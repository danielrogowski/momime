package momime.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.CombatAreaAffectsPlayersID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatAreaEffectSkillBonus;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.HeroItemType;
import momime.common.database.NegatedBySkill;
import momime.common.database.Pick;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitSpellEffect;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

/**
 * Simple unit lookups of basic skill, attribute and upkeep values
 */
public final class UnitUtilsImpl implements UnitUtils
{
	/**
	 * The majority of skills, if we don't have the skill at all, then bonuses don't apply to it.
	 * e.g. Settlers have no melee attack - just because they might gain 20 exp doesn't mean they start attacking with their pitchforks.
	 * e.g. Units with no ranged attack don't suddenly gain one.
	 * e.g. Phantom Warriors have no defence, but this is in the nature of the type of unit, and I think it makes sense to not allow them to gain a defence thru bonuses.
	 * 
	 * Movement speed, HP and Resistance are N/A here, because all units MUST define a value for those (see ServerDatabaseExImpl.consistencyChecks ())
	 * So the two that are left, that we must treat differently, are + to hit and + to block.  Most units don't have those values defined, but bonuses definitely still apply.
	 */
	private static final String [] SKILLS_WHERE_BONUSES_APPLY_EVEN_IF_NO_BASIC_SKILL = new String []
		{CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK};
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @return Unit with requested URN, or null if not found
	 */
	@Override
	public final MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units)
	{
		MemoryUnit result = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if (thisUnit.getUnitURN () == unitURN)
				result = thisUnit;
		}

		return result;
	}

	/**
	 * @param unitURN Unit URN to search for
	 * @param units List of units to search through
	 * @param caller The routine that was looking for the value
	 * @return Unit with requested URN
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Override
	public final MemoryUnit findUnitURN (final int unitURN, final List<MemoryUnit> units, final String caller)
		throws RecordNotFoundException
	{
		final MemoryUnit result = findUnitURN (unitURN, units);

		if (result == null)
			throw new RecordNotFoundException (MemoryUnit.class, unitURN, caller);

		return result;
	}

	/**
	 * @param unitURN Unit URN to remove
	 * @param units List of units to search through
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Override
	public final void removeUnitURN (final int unitURN, final List<MemoryUnit> units)
		throws RecordNotFoundException
	{
		boolean found = false;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((!found) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();
			if (thisUnit.getUnitURN () == unitURN)
			{
				iter.remove ();
				found = true;
			}
		}

		if (!found)
			throw new RecordNotFoundException (MemoryUnit.class, unitURN, "removeUnitURN");
	}

	/**
	 * Populates a unit's list of skills after creation - this is the equivalent of the TMomAvailableUnit.CreateAvailableUnit constructor in Delphi.
	 * The client will never use this on real units - the server always sends them will all info already populated; but the client does
	 * need this for initializing skills of sample units, e.g. when drawing units on the change construction screen.
	 * 
	 * @param unit Unit that has just been created
	 * @param startingExperience Initial experience; if -1 or null then experience won't be added into skill list, which is used when server sends units to client since they already have exp skill in list
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 * @return Unit definition
	 */
	@Override
	public final UnitEx initializeUnitSkills (final AvailableUnit unit, final Integer startingExperience, final CommonDatabase db) throws RecordNotFoundException
	{
		final UnitEx unitDefinition = db.findUnit (unit.getUnitID (), "initializeUnitSkills");

		// Check whether this type of unit gains experience (summoned units do not)
		// Also when sending heroes from the server to the client, experience is sent in amongst the rest of the skill list, so we don't need to
		// handle it separately here - in this case, experience will be -1 or null
		if ((startingExperience != null) && (startingExperience >= 0))
		{
			final String unitTypeID = db.findPick (unitDefinition.getUnitMagicRealm (), "initializeUnitSkills").getUnitTypeID ();
			final UnitType unitType = db.findUnitType (unitTypeID, "initializeUnitSkills");

			if (unitType.getExperienceLevel ().size () > 0)
			{
				final UnitSkillAndValue exp = new UnitSkillAndValue ();
				exp.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
				exp.setUnitSkillValue (startingExperience);
				unit.getUnitHasSkill ().add (exp);
			}
		}

		// Copy skills from DB
		for (final UnitSkillAndValue srcSkill : unitDefinition.getUnitHasSkill ())
		{
			final UnitSkillAndValue destSkill = new UnitSkillAndValue ();
			destSkill.setUnitSkillID (srcSkill.getUnitSkillID ());
			destSkill.setUnitSkillValue (srcSkill.getUnitSkillValue ());
			unit.getUnitHasSkill ().add (destSkill);
		}

		return unitDefinition;
	}

	/**
	 * Calculates and stores all derived skill, upkeep and other values for a particular unit and stores them for easy and quick lookup.
	 * Note the calculated values depend in part on which unit(s) we're in combat with and if we're calculating stats for purposes of defending an incoming attack.
	 * e.g. the +2 defence from Long Range will only be included vs incoming ranged attacks; the +3 bonus from Resist Elements will only be included vs incoming Chaos+Nature based attacks
	 * So must bear this in mind that we need to recalculate unit stats again if the circumstances we are calculating them for change, even if the unit itself has not changed.
	 * 
	 * @param unit Unit to expand skill list for
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills granted from other skills and skills granted from spells merged into the list
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final ExpandedUnitDetails expandUnitDetails (final AvailableUnit unit,
		final List<ExpandedUnitDetails> enemyUnits, final String attackFromSkillID, final String attackFromMagicRealmID,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// STEP 1 - Get a list of all units in the same stack as this one - we need this to look to see if anyone has skills like Holy Bonus or Resistance to All
		final List<AvailableUnit> unitStack = new ArrayList<AvailableUnit> ();
		final List<Integer> unitStackUnitURNs = new ArrayList<Integer> ();

		final MapCoordinates3DEx unitCombatLocation = (unit instanceof MemoryUnit) ? (MapCoordinates3DEx) ((MemoryUnit) unit).getCombatLocation () : null;
		if (unit.getUnitLocation () != null)
			for (final MemoryUnit thisUnit : mem.getUnit ())
				if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () == unit.getOwningPlayerID ()) &&
					(unit.getUnitLocation ().equals (thisUnit.getUnitLocation ())) &&
					(((unitCombatLocation == null) && (thisUnit.getCombatLocation () == null)) ||
					((unitCombatLocation != null) && (unitCombatLocation.equals (thisUnit.getCombatLocation ())))))
				{
					unitStack.add (thisUnit);
					unitStackUnitURNs.add (thisUnit.getUnitURN ());
				}
		
		if (!unitStack.contains (unit))
		{
			unitStack.add (unit);
			if (unit instanceof MemoryUnit)
				unitStackUnitURNs.add (((MemoryUnit) unit).getUnitURN ());
		}
		
		// STEP 2 - First just copy the skills from the unit into a map
		// NB. can't just use Collectors.toMap () because this throws an exception if you have any null values (which we often will)
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		unit.getUnitHasSkill ().forEach (s -> basicSkillValues.put (s.getUnitSkillID (), s.getUnitSkillValue ()));
		
		// For unit stack skills, want the highest value of each, and ignore any that are nulls
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		unitStack.forEach (u -> u.getUnitHasSkill ().stream ().filter (s -> (s.getUnitSkillValue () != null)).forEach (s ->
		{
			Integer skillValue = unitStackSkills.get (s.getUnitSkillID ());
			skillValue = (skillValue == null) ? s.getUnitSkillValue () : Math.max (skillValue, s.getUnitSkillValue ());
			unitStackSkills.put (s.getUnitSkillID (), skillValue);
		}));
		
		// STEP 3 - Add in skills from spells - we must do this next since some skills from spells may grant other skills, e.g. Invulerability spell effect grants Weapon Immunity
		for (final MemoryMaintainedSpell thisSpell : mem.getMaintainedSpell ())
			if ((thisSpell.getUnitURN () != null) && (unitStackUnitURNs.contains (thisSpell.getUnitURN ())) && (thisSpell.getUnitSkillID () != null))
			{
				// See if the spell definition defines a strength - this is for things like casting Immolation on a unit - we have to know that it is "Immolation 4"
				final Spell spellDef = db.findSpell (thisSpell.getSpellID (), "expandUnitDetails");
				boolean found = false;
				Integer strength = null;
				final Iterator<UnitSpellEffect> iter = spellDef.getUnitSpellEffect ().iterator ();
				while ((!found) && (iter.hasNext ()))
				{
					final UnitSpellEffect effect = iter.next ();
					if (effect.getUnitSkillID ().equals (thisSpell.getUnitSkillID ()))
					{
						found = true;
						strength = effect.getUnitSkillValue ();
					}
				}
				
				if (!found)
					throw new RecordNotFoundException (UnitSpellEffect.class.getName (), thisSpell.getUnitSkillID (), "expandUnitDetails");
				
				// Is it the unit we're calculating skills for?
				if ((unit instanceof MemoryUnit) && (((MemoryUnit) unit).getUnitURN () == thisSpell.getUnitURN ()))
					basicSkillValues.put (thisSpell.getUnitSkillID (), strength);
				
				// List the skill in the unit stack skills?
				if (strength != null)
				{
					Integer skillValue = unitStackSkills.get (thisSpell.getUnitSkillID ());
					skillValue = (skillValue == null) ? strength : Math.max (skillValue, strength);
					unitStackSkills.put (thisSpell.getUnitSkillID (), skillValue);
				}
			}
		
		// STEP 4 - Add in valueless skills from hero items - again need to do this early since some may grant other skills, e.g. Invulnerability imbued on a hero item grants Weapon Immunity
		// We deal with numeric bonuses from hero items much lower down
		if (unit instanceof MemoryUnit)
			for (final MemoryUnitHeroItemSlot slot : ((MemoryUnit) unit).getHeroItemSlot ())
				if (slot.getHeroItem () != null)
				{
					// Natural effects of the item type, e.g. +2 defence from plate mail
					final HeroItemType heroItemType = db.findHeroItemType (slot.getHeroItem ().getHeroItemTypeID (), "expandUnitDetails");
					for (final UnitSkillAndValue basicStat : heroItemType.getHeroItemTypeBasicStat ())
						if ((basicStat.getUnitSkillValue () == null) && (!basicSkillValues.containsKey (basicStat.getUnitSkillID ())))
							basicSkillValues.put (basicStat.getUnitSkillID (), null);
					
					// Bonuses imbued on the item
					for (final String bonusID : slot.getHeroItem ().getHeroItemChosenBonus ())
						for (final HeroItemBonusStat bonusStat : db.findHeroItemBonus (bonusID, "expandUnitDetails").getHeroItemBonusStat ())
							if ((bonusStat.getUnitSkillValue () == null) && (!basicSkillValues.containsKey (bonusStat.getUnitSkillID ())))

								// Some bonuses only apply if the attackFromSkillID matches the kind of item they're imbued in
								if ((bonusStat.isAppliesOnlyToAttacksAppropriateForTypeOfHeroItem () == null) || (!bonusStat.isAppliesOnlyToAttacksAppropriateForTypeOfHeroItem ()) ||
									((attackFromSkillID != null) && (heroItemType.getHeroItemTypeAttackType ().stream ().anyMatch (t -> t.equals (attackFromSkillID)))))
								
									basicSkillValues.put (bonusStat.getUnitSkillID (), null);
				}
		
		// STEP 5 - Now check all skills to see if any grant other skills
		// This is N/A for unit stack skills since granted skills always have a null skill strength value
		final List<String> skillsLeftToCheck = basicSkillValues.keySet ().stream ().collect (Collectors.toList ());
		while (skillsLeftToCheck.size () > 0)
		{
			final UnitSkillEx skillDef = db.findUnitSkill (skillsLeftToCheck.get (0), "expandUnitDetails");
			skillsLeftToCheck.remove (0);
			
			skillDef.getGrantsSkill ().stream ().forEach (s ->
			{
				basicSkillValues.put (s, null);
				skillsLeftToCheck.add (s);
			});
		}
		
		// STEP 6 - Create new map, eliminating from it skills that are negated
		// If we have the skill, before we go any further see if anything negates it
		final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved = new HashMap<String, Integer> ();
		final List<String> changedMagicRealmLifeformTypeIDs = new ArrayList<String> ();
		
		for (final Entry<String, Integer> skill : basicSkillValues.entrySet ())
		{
			boolean negated = false;
			final UnitSkillEx skillDef = db.findUnitSkill (skill.getKey (), "expandUnitDetails");
			final Iterator<NegatedBySkill> iter = skillDef.getNegatedBySkill ().iterator ();
			while ((!negated) && (iter.hasNext ()))
			{
				final NegatedBySkill negation = iter.next ();
				switch (negation.getNegatedByUnitID ())
				{
					case OUR_UNIT:
						if (basicSkillValues.containsKey (negation.getNegatedBySkillID ()))
							negated = true;
						break;
						
					case ENEMY_UNIT:
						if (enemyUnits != null)
							negated = enemyUnits.stream ().anyMatch (e -> e.hasModifiedSkill (negation.getNegatedBySkillID ()));
						break;
						
					default:
						throw new MomException ("expandUnitDetails doesn't know what to do with negatedByUnitID value of " +
							negation.getNegatedByUnitID () + " when determining value of skill " + skill.getKey ());
				}
			}
			
			if (!negated)
			{
				basicSkillValuesWithNegatedSkillsRemoved.put (skill.getKey (), skill.getValue ());
				
				// For any that are not negated, while we already have the skill definition found, see if the skill modifies the units magic realm/lifeform type
				if (skillDef.getChangesUnitToMagicRealm () != null)
					changedMagicRealmLifeformTypeIDs.add (skillDef.getChangesUnitToMagicRealm ());
			}
		}
		
		// STEP 7 - Do simple lookups
		final UnitEx unitDef = db.findUnit (unit.getUnitID (), "expandUnitDetails");
		final PlayerPublicDetails owningPlayer = (unit.getOwningPlayerID () == 0) ? null : getMultiplayerSessionUtils ().findPlayerWithID (players, unit.getOwningPlayerID (), "expandUnitDetails");
		final List<PlayerPick> picks = (owningPlayer == null) ? null : ((MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ()).getPick ();
		
		final WeaponGrade weaponGrade = (unit.getWeaponGrade () == null) ? null : db.findWeaponGrade (unit.getWeaponGrade (), "expandUnitDetails");
		final RangedAttackTypeEx rangedAttackType = (unitDef.getRangedAttackType () == null) ? null : db.findRangedAttackType (unitDef.getRangedAttackType (), "expandUnitDetails");
		
		final String unitTypeID = db.findPick (unitDef.getUnitMagicRealm (), "expandUnitDetails").getUnitTypeID ();
		final UnitType unitType = db.findUnitType (unitTypeID, "expandUnitDetails");
		
		// STEP 8 - Work out the units magic realm/lifeform type
		// We made a list of overrides to it above already - now what we do depends on how many modifications we found
		final Pick magicRealmLifeformType;
		
		// No modifications - use value from unit definition, unaltered
		if (changedMagicRealmLifeformTypeIDs.size () == 0)
			magicRealmLifeformType = db.findPick (unitDef.getUnitMagicRealm (), "expandUnitDetails");

		// Exactly one modification - use the value set by that skill (i.e. unit is Undead or Chaos Channeled)
		else if (changedMagicRealmLifeformTypeIDs.size () == 1)
			magicRealmLifeformType = db.findPick (changedMagicRealmLifeformTypeIDs.get (0), "expandUnitDetails");
		
		// Multiple - look for a magic realm whose merge list matches our list (i.e. unit is Undead AND Chaos Channeled)
		else
		{
			final Iterator<Pick> iter = db.getPick ().iterator ();
			Pick match = null;
			while ((match == null) && (iter.hasNext ()))
			{
				final Pick pick = iter.next ();
				
				if ((pick.getMergedFromPick ().size () == changedMagicRealmLifeformTypeIDs.size ()) &&
					(pick.getMergedFromPick ().stream ().allMatch (m -> changedMagicRealmLifeformTypeIDs.contains (m))))
					
					match = pick;
			}
			
			if (match != null)
				magicRealmLifeformType = match;
			else
			{
				// Not found - make the error message useful enough so we'll know how to fix it
				final StringBuilder msg = new StringBuilder ();
				changedMagicRealmLifeformTypeIDs.forEach (s ->
				{
					if (msg.length () > 0)
						msg.append (", ");
					
					msg.append (s);
				});
				
				throw new MomException ("No magic realm/lifeform type (Pick) found that merges lifeform types: " + msg);
			}
		}
		
		// STEP 9 - Find the unit's experience level
		// Experience can never be increased by spells, combat area effects, weapon grades, etc. etc. therefore safe to do this from the basic skill value on the unmerged list
		final Integer experienceSkillValue = basicSkillValuesWithNegatedSkillsRemoved.get (CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		final ExperienceLevel basicExpLvl;
		final ExperienceLevel modifiedExpLvl;
		if (experienceSkillValue == null)
		{
			basicExpLvl = null;		// This type of unit doesn't gain experience (e.g. summoned)
			modifiedExpLvl = null;
		}
		else
		{
			// Check all experience levels defined under the unit type
			// This checks them all so we aren't relying on them being defined in the correct orer
			ExperienceLevel levelFromExperience = null;
			for (final ExperienceLevel experienceLevel : unitType.getExperienceLevel ())

				// Careful - getExperienceRequired () can be null, for normal units' Ultra-Elite and Champion statuses
				// Levels that actually require 0 experience must state a 0 in the XML rather than omit the field
				if ((experienceLevel.getExperienceRequired () != null) && (experienceSkillValue >= experienceLevel.getExperienceRequired ()) &&
					((levelFromExperience == null) || (levelFromExperience.getLevelNumber () < experienceLevel.getLevelNumber ())))
						levelFromExperience = experienceLevel;

			// Check we got one
			if (levelFromExperience == null)
				throw new MomException ("Unit " + unit.getUnitID () + " of type " + unitTypeID + " with " + experienceSkillValue + " experience cannot find any appropriate experience level");

			// Now we've found the level that we're at due to actual experience, see if we get any level bonuses, i.e. Warlord retort or Crusade spell
			basicExpLvl = levelFromExperience;
			int levelIncludingBonuses = levelFromExperience.getLevelNumber ();

			// Does the player have the Warlord retort?
			if ((picks != null) && (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_WARLORD) > 0))
				levelIncludingBonuses++;

			// Does the player have the Crusade CAE?
			if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (mem.getCombatAreaEffect (), null, CommonDatabaseConstants.COMBAT_AREA_EFFECT_CRUSADE, unit.getOwningPlayerID ()) != null)
				levelIncludingBonuses++;

			// Now we have to ensure that the level we've attained actually exists, this is fine for units but a hero might reach Demi-God naturally,
			// then giving them +1 level on top of that will move them to an undefined level
			do
			{
				levelFromExperience = UnitTypeUtils.findExperienceLevel (unitType, levelIncludingBonuses);
				levelIncludingBonuses--;
			} while (levelFromExperience == null);

			modifiedExpLvl = levelFromExperience;
		}
		
		// STEP 10 - Ensure we have complete list of all skills that we need to fully calculate all the components for
		// The majority of skills, if we don't have the skill at all, then bonuses don't apply to it.
		// e.g. Settlers have no melee attack - just because they might gain 20 exp doesn't mean they start attacking with their pitchforks.
		// e.g. Units with no ranged attack don't suddenly gain one.
		// e.g. Phantom Warriors have no defence, but this is in the nature of the type of unit, and I think it makes sense to not allow them to gain a defence thru bonuses.
		// Movement speed, HP and Resistance are N/A here, because all units MUST define a value for those (see ServerDatabaseExImpl.consistencyChecks ())
		// So the two that are left, that we must treat differently, are + to hit and + to block.  Most units don't have those values defined, but bonuses definitely still apply.
		// So if they aren't in the list already then add them, and use "0" rather than "null" so we don't skip calculating the component breakdown.
		// If we fail to actually find any bonuses to either of these, then we strip them back out of modifiedSkillValues lower down.
		for (final String unitSkillID : SKILLS_WHERE_BONUSES_APPLY_EVEN_IF_NO_BASIC_SKILL)
			if (!basicSkillValuesWithNegatedSkillsRemoved.containsKey (unitSkillID))
				basicSkillValuesWithNegatedSkillsRemoved.put (unitSkillID, 0);
		
		// STEP 11 - Copy across basic skill values
		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues = new HashMap<String, Map<UnitSkillComponent, Integer>> ();
		for (final Entry<String, Integer> basicSkill : basicSkillValuesWithNegatedSkillsRemoved.entrySet ())
			
			// Skills with no value just get copied in as-is with no breakdown
			if (basicSkill.getValue () == null)
				modifiedSkillValues.put (basicSkill.getKey (), null);
			else
			{
				// Start a map of components of the breakdown
				final Map<UnitSkillComponent, Integer> components = new HashMap<UnitSkillComponent, Integer> ();
				if (basicSkill.getValue () > 0)
					components.put (UnitSkillComponent.BASIC, basicSkill.getValue ());
				
				modifiedSkillValues.put (basicSkill.getKey (), components);
			}
		
		// STEP 12 - Add bonuses from weapon grades
		if (weaponGrade != null)
			for (final UnitSkillAndValue bonus : weaponGrade.getWeaponGradeSkillBonus ())
			{
				final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (bonus.getUnitSkillID ());
				if ((components != null) && (bonus.getUnitSkillValue () != null))
				{
					// Only certain types of ranged attack get bonuses from Mithril and Adamantium weapons - e.g. bows do, magical blasts do not
					final boolean weaponGradeBonusApplies;
					if (bonus.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK))
						weaponGradeBonusApplies = (rangedAttackType == null) ? false : rangedAttackType.isMithrilAndAdamantiumVersions ();
					else
						weaponGradeBonusApplies = true;
					
					if (weaponGradeBonusApplies)
						components.put (UnitSkillComponent.WEAPON_GRADE, bonus.getUnitSkillValue ());
				}
			}

		// STEP 13 - Add bonuses from experience
		if (modifiedExpLvl != null)
			for (final UnitSkillAndValue bonus : modifiedExpLvl.getExperienceSkillBonus ())
			{
				final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (bonus.getUnitSkillID ());
				if ((components != null) && (bonus.getUnitSkillValue () != null))
					components.put (UnitSkillComponent.EXPERIENCE, bonus.getUnitSkillValue ());
			}
		
		// STEP 14 - Add bonuses from combat area effects
		for (final MemoryCombatAreaEffect thisCAE : mem.getCombatAreaEffect ())
			if (doesCombatAreaEffectApplyToUnit (unit, thisCAE, db))
			{
				// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites
				// So this means all the skill bonuses apply, except we still need to do the magic realm
				// check since some effects have different components which apply to different lifeform types, e.g. True Light and Darkness
				for (final CombatAreaEffectSkillBonus bonus : db.findCombatAreaEffect (thisCAE.getCombatAreaEffectID (), "expandUnitDetails").getCombatAreaEffectSkillBonus ())
				{
					// Magic realm/lifeform type can be blank for effects that apply to all types of unit (e.g. Prayer)
					final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (bonus.getUnitSkillID ());
					if ((components != null) && (bonus.getUnitSkillValue () != null) &&
						((bonus.getEffectMagicRealm () == null) || (bonus.getEffectMagicRealm ().equals (magicRealmLifeformType.getPickID ()))))
					{
						// There might be more than one CAE giving a bonus to the same skill, so this isn't a simple "put"
						Integer bonusValue = components.get (UnitSkillComponent.COMBAT_AREA_EFFECTS);
						bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonus.getUnitSkillValue ();
						components.put (UnitSkillComponent.COMBAT_AREA_EFFECTS, bonusValue);
					}
				}
			}
		
		// STEP 15 - Skills that add to other skills (hero skills, and skills like Large Shield adding +2 defence, and bonuses to the whole stack like Resistance to All)
		for (final UnitSkillEx skillDef : db.getUnitSkills ())
			for (final AddsToSkill addsToSkill : skillDef.getAddsToSkill ())
			{
				final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (addsToSkill.getAddsToSkillID ());
				if ((components != null) && (addsToSkill.isAffectsEntireStack () ? unitStackSkills : modifiedSkillValues).containsKey (skillDef.getUnitSkillID ()))
				{
					// If we have no info about the kind of attack being made, or this isn't in reference to an attack at all, then discount the bonus
					// if it has any restrictions that depend on the kind of incoming attack, even if we match those restrictions.
					// This is to stop the bonus from Large Shield showing on the unit info screen.
					if ((attackFromSkillID == null) && (attackFromMagicRealmID == null) &&
						((addsToSkill.getOnlyVersusAttacksFromSkillID () != null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null)))
					{
						// Ignore
					}
					else
					{
						// Deal with negative checks first, so the "if" below doesn't get too complicated; the value here is irrelevant if onlyVersusAttacksFromSkillID is null
						// NB. attackFromSkillID of "null" (i.e. spell based attacks) are considered as not matching the required skill ID
						// which is what we want - so Large Shield DOES give its bonus against incoming spell attacks such as Fire Bolt
						boolean onlyVersusAttacksFromSkillIDCheckPasses = (addsToSkill.getOnlyVersusAttacksFromSkillID () != null) &&
							(addsToSkill.getOnlyVersusAttacksFromSkillID ().equals (attackFromSkillID));
						if ((addsToSkill.isNegateOnlyVersusAttacksFromSkillID () != null) && (addsToSkill.isNegateOnlyVersusAttacksFromSkillID ()))
							onlyVersusAttacksFromSkillIDCheckPasses = !onlyVersusAttacksFromSkillIDCheckPasses;
						
						// Does the skill only apply to particular ranged attack types, and incoming skill IDs or incoming attacks of only particular magic realms?
						// (This deals with all conditional bonuses, such as Resist Elements, Large Shield or Flame Blade)
						if (((addsToSkill.getRangedAttackTypeID () == null) || (addsToSkill.getRangedAttackTypeID ().equals (unitDef.getRangedAttackType ()))) &&
							((addsToSkill.getOnlyVersusAttacksFromSkillID () == null) || (onlyVersusAttacksFromSkillIDCheckPasses)) &&
							((addsToSkill.getOnlyVersusAttacksFromMagicRealmID () == null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID ().equals (attackFromMagicRealmID))))
						{
							// How is the bonus calculated - fixed value, value from the skill, etc - firstly, defining both isn't valid
							UnitSkillComponent component = addsToSkill.isAffectsEntireStack () ? UnitSkillComponent.SPELL_EFFECTS_STACK : UnitSkillComponent.SPELL_EFFECTS;
							final int bonus;
							if ((addsToSkill.getAddsToSkillDivisor () != null) && (addsToSkill.getAddsToSkillFixed () != null))
								throw new MomException ("Unit skill " + skillDef.getUnitSkillID () + " adds to skill " + addsToSkill.getAddsToSkillID () +
									" but specifies both a level divisor and a fixed amount");

							// Any fixed bonuses from one skill to another?  e.g. Holy Armour gives +2 to defence
							else if (addsToSkill.getAddsToSkillFixed () != null)
								bonus = addsToSkill.getAddsToSkillFixed ();
							
							else
							{
								// For other kinds, we must have a value for the skill - e.g. Constitution 1 or Holy Bonus 2
								int multiplier;
								if (addsToSkill.isAffectsEntireStack ())
									multiplier = unitStackSkills.get (skillDef.getUnitSkillID ());
								else
								{
									multiplier = 0;
									final Map<UnitSkillComponent, Integer> totalComponents = modifiedSkillValues.get (skillDef.getUnitSkillID ());
									if (totalComponents != null)
										for (final Entry<UnitSkillComponent, Integer> c : totalComponents.entrySet ())
											if (c.getValue () == null)
												throw new MomException ("expandUnitDetails on " + unit.getUnitID () + " trying to sum addsFromSkill ID " + skillDef.getUnitSkillID () + " but the " + c.getKey () + " component is null");
											else
												multiplier = multiplier + c.getValue ();
								}
								
								if (multiplier <= 0)
									bonus = 0;
								
								// Any bonuses from hero skills? (Might gives +melee, Constitution gives +hit points, Agility gives +defence, and so on)
								else if (addsToSkill.getAddsToSkillDivisor () != null)
								{
									component = UnitSkillComponent.HERO_SKILLS;
									if (modifiedExpLvl != null)
									{
										// Multiplier will either equal 1 or 2, indicating whether we have the regular or super version of the skill - change this to be 2 for regular or 3 for super
										multiplier++;
										
										// Some skills take more than 1 level to gain 1 attribute point, so get this value
										final int divisor = (addsToSkill.getAddsToSkillDivisor () == null) ? 1 : addsToSkill.getAddsToSkillDivisor ();
										
										// Now can do the calculation
										bonus = ((modifiedExpLvl.getLevelNumber () + 1) * multiplier) / (divisor*2);
									}
									else
										bonus = 0;
								}
								
								
								// Neither divisor nor fixed value specified, so the value must come from the skill itself
								else
									bonus = multiplier;
							}
							
							if (bonus > 0)
							{
								Integer bonusValue = components.get (component);
								bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonus;
								components.put (component, bonusValue);
							}
						}
					}
				}
			}
		
		// STEP 16 - Hero items - numeric bonuses (dealt with valueless skills above)
		if (unit instanceof MemoryUnit)
			for (final MemoryUnitHeroItemSlot slot : ((MemoryUnit) unit).getHeroItemSlot ())
				if (slot.getHeroItem () != null)
				{
					// Natural effects of the item type, e.g. +2 defence from plate mail
					final HeroItemType heroItemType = db.findHeroItemType (slot.getHeroItem ().getHeroItemTypeID (), "expandUnitDetails");
					for (final UnitSkillAndValue basicStat : heroItemType.getHeroItemTypeBasicStat ())
						if (basicStat.getUnitSkillValue () != null)
						{
							final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (basicStat.getUnitSkillID ());
							if ((components != null) && (basicStat.getUnitSkillValue () != null))
								components.put (UnitSkillComponent.HERO_ITEMS, basicStat.getUnitSkillValue ());
						}
					
					// Bonuses imbued on the item
					for (final String bonusID : slot.getHeroItem ().getHeroItemChosenBonus ())
						for (final UnitSkillAndValue bonusStat : db.findHeroItemBonus (bonusID, "expandUnitDetails").getHeroItemBonusStat ())
							if (bonusStat.getUnitSkillValue () == null)
							{
								// Just want bonuses with a numeric value
							}
							
							// +Attack bonus - these apply to one or more specific skills, depending on the type of the item.
							// This deals with that e.g. swords don't add to "Thrown Weapons" skill but axes do.
							else if (bonusStat.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_ATTACK_APPROPRIATE_FOR_TYPE_OF_HERO_ITEM))
							{
								for (final String attackSkillID : heroItemType.getHeroItemTypeAttackType ())
								{
									final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (attackSkillID);
									if (components != null)
									{
										// This might coincide with a basic stat, e.g. plate mail (+2 defence) with another +4 defence imbued onto it
										Integer bonusValue = components.get (UnitSkillComponent.HERO_ITEMS);
										bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonusStat.getUnitSkillValue ();
										components.put (UnitSkillComponent.HERO_ITEMS, bonusValue);
									}
								}
							}
		
							// Regular bonus
							else
							{
								final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (bonusStat.getUnitSkillID ());
								if ((components != null) && (bonusStat.getUnitSkillValue () != null))
								{
									// This might coincide with a basic stat, e.g. plate mail (+2 defence) with another +4 defence imbued onto it
									Integer bonusValue = components.get (UnitSkillComponent.HERO_ITEMS);
									bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonusStat.getUnitSkillValue ();
									components.put (UnitSkillComponent.HERO_ITEMS, bonusValue);
								}
							}
				}
		
		// STEP 17 - If we falied to find any + to hit / + to block values and we have no basic value then there's no point keeping it in the list
		// We know the entry has to exist and have a valid map in it from the code above, but it may be an empty map
		for (final String unitSkillID : SKILLS_WHERE_BONUSES_APPLY_EVEN_IF_NO_BASIC_SKILL)
			if (modifiedSkillValues.get (unitSkillID).isEmpty ())
				modifiedSkillValues.remove (unitSkillID);
		
		// STEP 18 - Basic upkeep values - just copy from the unit definition
		final Map<String, Integer> basicUpkeepValues = unitDef.getUnitUpkeep ().stream ().collect
			(Collectors.toMap (u -> u.getProductionTypeID (), u -> u.getUndoubledProductionValue ()));
		
		// STEP 19 - Modify upkeep values
		final Map<String, Integer> modifiedUpkeepValues;

		// Upkeep for undead is zeroed for normal units and adds +50% for summoned creatures
		final int unitTypeUpkeepPercentage = modifiedSkillValues.containsKey (CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD) ? unitType.getUndeadUpkeepPercentage () : 100;
		if ((unitTypeUpkeepPercentage <= 0) || (basicUpkeepValues.isEmpty ()))
			modifiedUpkeepValues = new HashMap<String, Integer> ();		// Empty map
		else
		{
			// Reduce upkeep for Summoner retort?
			// Get reduction as a percentage - note we use the special "unit upkeep" production type, not "Mana"
			final int percentageReduction = (picks == null) ? 0 : getPlayerPickUtils ().totalProductionBonus
				(CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, unitType.getUnitTypeID (), picks, db);

			// Now copy and modify each basic skill value
			modifiedUpkeepValues = basicUpkeepValues.entrySet ().stream ().collect (Collectors.toMap (u -> u.getKey (), u ->
			{
				final int baseUpkeepValue = (u.getValue () * unitTypeUpkeepPercentage) / 100;

				// Calculate actual amount of reduction, rounding down
				final int amountReduction = (baseUpkeepValue * percentageReduction) / 100;
				
				// Note its impossible to actually get zero here since we round the reduction down, unless percentageReduction reached 100 which will never happen
				return baseUpkeepValue - amountReduction;				
			}));
		}
		
		// Finally can build the unit object
		final ExpandedUnitDetailsImpl container = new ExpandedUnitDetailsImpl (unit, unitDef, unitType, owningPlayer, magicRealmLifeformType,
			weaponGrade, rangedAttackType, basicExpLvl, modifiedExpLvl, basicSkillValues, modifiedSkillValues, basicUpkeepValues, modifiedUpkeepValues, this);
		return container;
	}

	/**
	 * Since Available Units cannot be in combat, this is quite a bit simpler than the MomUnit version
	 *
	 * The unit has to:
	 * 1) Be in the right location (or it be a global CAE)
	 * 2) Belong to the right player (either the CAE applies to all players, or just the caster)
	 *
	 * CAEs with player code B or O can never apply to Available Units, since these require the unit to be in combat
	 * The only way CAEs with player code C can apply is if they're global (e.g. Holy Arms)
	 *
	 * @param unit Unit to test
	 * @param effect The combat area effect to test
	 * @param db Lookup lists built over the XML database
	 * @return True if this combat area effect affects this unit
	 * @throws RecordNotFoundException If we can't find the definition for the CAE
	 */
	@Override
	public final boolean doesCombatAreaEffectApplyToUnit (final AvailableUnit unit, final MemoryCombatAreaEffect effect, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// Check if unit is in combat (available units can never be in combat)
		final MapCoordinates3DEx combatLocation;
		if (unit instanceof MemoryUnit)
		{
			final MemoryUnit mu = (MemoryUnit) unit;
			if ((mu.getCombatLocation () != null) && (mu.getCombatPosition () != null) && (mu.getCombatHeading () != null) && (mu.getCombatSide () != null))
				combatLocation = (MapCoordinates3DEx) mu.getCombatLocation ();
			else
				combatLocation = null;
		}
		else
			combatLocation = null;

		// Check location
		final boolean locationOk;
		if (effect.getMapLocation () == null)
		{
			// Area effect covering the whole map, so must apply
			locationOk = true;
		}
		else if (combatLocation != null)
		{
			// If unit is in combat, then the effect must be located at the combat
			locationOk = effect.getMapLocation ().equals (combatLocation);
		}
		else
		{
			// Area effect in one map location only, so we have to be in the right place
			locationOk = effect.getMapLocation ().equals (unit.getUnitLocation ());
		}

		// Check which player(s) this CAE affects
		final boolean applies;
		if (!locationOk)
			applies = false;
		else
		{
			final CombatAreaEffect combatAreaEffect = db.findCombatAreaEffect (effect.getCombatAreaEffectID (), "doesCombatAreaEffectApplyToUnit");

			// Check player - if this is blank, its a combat area effect that doesn't provide unit bonuses/penalties, e.g. Call Lightning, so we can just return false
			if (combatAreaEffect.getCombatAreaAffectsPlayers () == null)
				applies = false;

			// All is easy, either its a global CAE that affects everyone (like Chaos Surge or Eternal Night) or its a CAE in a
			// specific location that affects everyone, whether or not they're in combat (like Node Auras)
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.ALL_EVEN_NOT_IN_COMBAT))
				applies = true;

			// Spells that apply only to the caster only apply to available units if they're global (like Holy Arms)
			// Localised spells that apply only to the caster (like Prayer or Mass Invisibility) only apply to units in combat
			// NDG 19/6/2011 - On looking at this again I think that's highly debatable - if there was a caster only buff CAE then why
			// wouldn't it help units in the city defend against overland attacks like Call the Void?  However I've checked, there's no
			// City Enchantments in the original MoM that grant these kind of bonuses so its pretty irrelevant, so leaving it as it is
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.CASTER_ONLY))
				applies = ((effect.getCastingPlayerID () == unit.getOwningPlayerID ()) && ((combatLocation != null) || (effect.getMapLocation () == null)));

			// 'Both' CAEs (like Darkness) apply only to units in combat
			// If the unit is in a combat at the right location, then by definition it is one of the two players (either attacker or defender) and so the CAE applies
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.BOTH_PLAYERS_IN_COMBAT))
				applies = (combatLocation != null);

			// Similarly we must be in combat for 'Opponent' CAEs to apply, and to be an 'Opponent' CAE the CAE must be in combat at the same
			// location we are... so this simply needs us to check that we're not the caster
			else if (combatAreaEffect.getCombatAreaAffectsPlayers ().equals (CombatAreaAffectsPlayersID.COMBAT_OPPONENT))
				applies = ((effect.getCastingPlayerID () != unit.getOwningPlayerID ()) && (combatLocation != null));

			// 'Both' CAEs (like Darkness) and 'Opponent' CAEs (like Black Prayer) can only apply to units in combat, which Available Units can never be
			else
				applies = false;
		}

		return applies;
	}

	/**
	 * @param units Unit stack
	 * @return Comma delimited list of their unit URNs, for debug messages
	 * @throws MomException If the list includes something other than MemoryUnits or ExpandedUnitDetails
	 */
	@Override
	public String listUnitURNs (@SuppressWarnings ("rawtypes") final List units) throws MomException
	{
		final StringBuilder list = new StringBuilder ();
		if (units != null)
			for (final Object thisUnit : units)
			{
				if (list.length () > 0)
					list.append (", ");

				if (thisUnit instanceof MemoryUnit)
					list.append (((MemoryUnit) thisUnit).getUnitURN ());
				else if (thisUnit instanceof ExpandedUnitDetails)
				{
					final ExpandedUnitDetails xu = (ExpandedUnitDetails) thisUnit;
					if (xu.isMemoryUnit ())
						list.append (xu.getUnitURN ());
				}
				else
					throw new MomException ("listUnitURNs got an object of type " + thisUnit.getClass ());
			}

		return "(" + list + ")";
	}

	/**
	 * @param units List of units to check (usually movingPlayer's memory)
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to test if there are units *at all* at this location)
	 * @return First unit we find at the requested location who belongs to someone other than the specified player
	 */
	@Override
	public final MemoryUnit findFirstAliveEnemyAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID)
	{
		// The reason this is done separately from countAliveEnemiesAtLocation is because this routine can exit as
		// soon as it finds the first matching unit, whereas countAliveEnemiesAtLocation always has to run over the entire list

		MemoryUnit found = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();

			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () != exceptPlayerID) && (thisUnit.getUnitLocation () != null) &&
				(thisUnit.getUnitLocation ().getX () == x) && (thisUnit.getUnitLocation ().getY () == y) && (thisUnit.getUnitLocation ().getZ () == plane))

				found = thisUnit;
		}

		return found;
	}

	/**
	 * @param units List of units to check
	 * @param x X coordinate of location to check
	 * @param y Y coordinate of location to check
	 * @param plane Plane to check
	 * @param exceptPlayerID Player who's units to not consider (can pass in 0 to count *all* units at this location)
	 * @return Number of units that we find at the requested location who belongs to someone other than the specified player
	 */
	@Override
	public final int countAliveEnemiesAtLocation (final List<MemoryUnit> units, final int x, final int y, final int plane, final int exceptPlayerID)
	{
		int count = 0;
		for (final MemoryUnit thisUnit : units)
		{
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () != exceptPlayerID) && (thisUnit.getUnitLocation () != null) &&
				(thisUnit.getUnitLocation ().getX () == x) && (thisUnit.getUnitLocation ().getY () == y) && (thisUnit.getUnitLocation ().getZ () == plane))

				count++;
		}

		return count;
	}

	/**
	 * Clears up any references to the specified unit from under the FogOfWarMemory structure, because the unit has just been killed
	 * This is used even if the unit is not actually being freed, e.g. could be dismissing a hero or just setting a unit in combat to 'dead' but not actually freeing the unit
	 * 
	 * @param mem Fog of war memory structure to remove references from; can be player's memory or the true map on the server
	 * @param unitURN Unit about to be killed
	 */
	@Override
	public final void beforeKillingUnit (final FogOfWarMemory mem, final int unitURN)
	{
		final Iterator<MemoryMaintainedSpell> iter = mem.getMaintainedSpell ().iterator ();
		while (iter.hasNext ())
		{
			final MemoryMaintainedSpell spell = iter.next ();
			if ((spell.getUnitURN () != null) && (spell.getUnitURN () == unitURN))
				iter.remove ();
		}
	}

	/**
	 * @param units List of units to check
	 * @param combatLocation Location on overland map where the combat is taking place
	 * @param combatPosition Position within the combat map to look at
	 * @return Unit at this position, or null if there isn't one
	 */
	@Override
	public final MemoryUnit findAliveUnitInCombatAt (final List<MemoryUnit> units, final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition)
	{
		MemoryUnit found = null;
		final Iterator<MemoryUnit> iter = units.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MemoryUnit thisUnit = iter.next ();

			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (combatPosition.equals (thisUnit.getCombatPosition ())) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null))

				found = thisUnit;
		}

		return found;
	}
	
	/**
	 * Performs a deep copy (i.e. creates copies of every sub object rather than copying the references) of every field value from one unit to another
	 * @param source Unit to copy values from
	 * @param dest Unit to copy values to
	 * @param includeMovementFields Only the player who owns a unit can see its movement remaining and special orders
	 */
	@Override
	public final void copyUnitValues (final MemoryUnit source, final MemoryUnit dest, final boolean includeMovementFields)
	{
		// Destination values for a couple of movement related fields depend on input param
		final int newDoubleOverlandMovesLeft = includeMovementFields ? source.getDoubleOverlandMovesLeft () : 0;
		final Integer newDoubleCombatMovesLeft = includeMovementFields ? source.getDoubleCombatMovesLeft () : null;
		final UnitSpecialOrder newSpecialOrder = includeMovementFields ? source.getSpecialOrder () : null;
		
		// AvailableUnit fields
		dest.setOwningPlayerID (source.getOwningPlayerID ());
		dest.setUnitID (source.getUnitID ());
		dest.setWeaponGrade (source.getWeaponGrade ());

		if (source.getUnitLocation () == null)
			dest.setUnitLocation (null);
		else
			dest.setUnitLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) source.getUnitLocation ()));

		// AvailableUnit - skills list
		dest.getUnitHasSkill ().clear ();
		for (final UnitSkillAndValue srcSkill : source.getUnitHasSkill ())
		{
			final UnitSkillAndValue destSkill = new UnitSkillAndValue ();
			destSkill.setUnitSkillID (srcSkill.getUnitSkillID ());
			destSkill.setUnitSkillValue (srcSkill.getUnitSkillValue ());
			dest.getUnitHasSkill ().add (destSkill);
		}

		// MemoryUnit fields
		dest.setUnitURN (source.getUnitURN ());
		dest.setHeroNameID (source.getHeroNameID ());
		dest.setUnitName (source.getUnitName ());
		dest.setAmmoRemaining (source.getAmmoRemaining ());
		
		dest.setManaRemaining (source.getManaRemaining ());
		dest.setDoubleOverlandMovesLeft (newDoubleOverlandMovesLeft);
		dest.setSpecialOrder (newSpecialOrder);
		dest.setStatus (source.getStatus ());
		dest.setWasSummonedInCombat (source.isWasSummonedInCombat ());
		dest.setCombatHeading (source.getCombatHeading ());
		dest.setCombatSide (source.getCombatSide ());
		dest.setDoubleCombatMovesLeft (newDoubleCombatMovesLeft);

		dest.getFixedSpellsRemaining ().clear ();
		dest.getFixedSpellsRemaining ().addAll (source.getFixedSpellsRemaining ());
		
		dest.getHeroItemSpellChargesRemaining ().clear ();
		dest.getHeroItemSpellChargesRemaining ().addAll (source.getHeroItemSpellChargesRemaining ());
		
		if (source.getCombatLocation () == null)
			dest.setCombatLocation (null);
		else
			dest.setCombatLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) source.getCombatLocation ()));

		if (source.getCombatPosition () == null)
			dest.setCombatPosition (null);
		else
			dest.setCombatPosition (new MapCoordinates2DEx ((MapCoordinates2DEx) source.getCombatPosition ()));
		
		// MemoryUnit - hero item slots list
		dest.getHeroItemSlot ().clear ();
		
		source.getHeroItemSlot ().forEach (srcItemSlot ->
		{
			final MemoryUnitHeroItemSlot destItemSlot = new MemoryUnitHeroItemSlot ();
			if (srcItemSlot.getHeroItem () != null)
			{
				final NumberedHeroItem srcItem = srcItemSlot.getHeroItem ();
				final NumberedHeroItem destItem = new NumberedHeroItem ();
				
				destItem.setHeroItemURN (srcItem.getHeroItemURN ());
				destItem.setHeroItemName (srcItem.getHeroItemName ());
				destItem.setHeroItemTypeID (srcItem.getHeroItemTypeID ());
				destItem.setHeroItemImageNumber (srcItem.getHeroItemImageNumber ());
				destItem.setSpellID (srcItem.getSpellID ());
				destItem.setSpellChargeCount (srcItem.getSpellChargeCount ());
				
				destItem.getHeroItemChosenBonus ().addAll (srcItem.getHeroItemChosenBonus ());
				
				destItemSlot.setHeroItem (destItem);
			}
			dest.getHeroItemSlot ().add (destItemSlot);
		});
		
		// Memory unit - damage
		dest.getUnitDamage ().clear ();
		source.getUnitDamage ().forEach (srcDamage ->
		{
			final UnitDamage destDamage = new UnitDamage ();
			destDamage.setDamageType (srcDamage.getDamageType ());
			destDamage.setDamageTaken (srcDamage.getDamageTaken ());
			dest.getUnitDamage ().add (destDamage);
		});
	}
	
	/**
	 * @param damages List of types of unit damage
	 * @return Total damage taken across all types
	 */
	@Override
	public final int getTotalDamageTaken (final List<UnitDamage> damages)
	{
		final int total = damages.stream ().mapToInt (d -> d.getDamageTaken ()).sum ();
		return total;
	}
	
	/**
	 * @param damages List of types of unit damage
	 * @return Total damage taken across all types, excluding PERMANENT
	 */
	@Override
	public final int getHealableDamageTaken (final List<UnitDamage> damages)
	{
		final int total = damages.stream ().filter (d -> d.getDamageType () != StoredDamageTypeID.PERMANENT).mapToInt (d -> d.getDamageTaken ()).sum ();
		return total;
	}
	
	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}
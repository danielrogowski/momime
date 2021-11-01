package momime.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.AddsToSkillValueType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.HeroItemType;
import momime.common.database.Pick;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpellEffect;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;

/**
 * expandUnitDetails is such a key method and called from so many places, it needs to be in its own class so everything else can mock it.
 */
public final class ExpandUnitDetailsImpl implements ExpandUnitDetails
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
	private final static String [] SKILLS_WHERE_BONUSES_APPLY_EVEN_IF_NO_BASIC_SKILL = new String []
		{CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK};
	
	/** Methods for working out minimal unit details */
	private UnitDetailsUtils unitDetailsUtils;

	/** Sections broken out from the big expandUnitDetails method to make it more manageable */
	private ExpandUnitDetailsUtils expandUnitDetailsUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
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
		final List<MinimalUnitDetails> unitStack = new ArrayList<MinimalUnitDetails> ();
		final List<Integer> unitStackUnitURNs = new ArrayList<Integer> ();
		MinimalUnitDetails mu = null;

		final MapCoordinates3DEx unitCombatLocation = (unit instanceof MemoryUnit) ? (MapCoordinates3DEx) ((MemoryUnit) unit).getCombatLocation () : null;
		if (unit.getUnitLocation () != null)
			for (final MemoryUnit thisUnit : mem.getUnit ())
				if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () == unit.getOwningPlayerID ()) &&
					(unit.getUnitLocation ().equals (thisUnit.getUnitLocation ())) &&
					(((unitCombatLocation == null) && (thisUnit.getCombatLocation () == null)) ||
					((unitCombatLocation != null) && (unitCombatLocation.equals (thisUnit.getCombatLocation ())))))
				{
					final MinimalUnitDetails thisMU = getUnitDetailsUtils ().expandMinimalUnitDetails (thisUnit, players, mem, db);		
					unitStack.add (thisMU);
					unitStackUnitURNs.add (thisMU.getUnitURN ());
					
					if (thisUnit == unit)
						mu = thisMU;
				}
		
		final boolean isInCombat = (unitCombatLocation != null);
		
		if (mu == null)
		{
			mu = getUnitDetailsUtils ().expandMinimalUnitDetails (unit, players, mem, db);
			unitStack.add (mu);
			if (mu.isMemoryUnit ())
				unitStackUnitURNs.add (mu.getUnitURN ());
		}
		
		// STEP 2 - First just copy the basic skills from the minimal details
		final Map<String, Integer> basicSkillValues = new HashMap<String, Integer> ();
		for (final String unitSkillID : mu.listBasicSkillIDs ())
			basicSkillValues.put (unitSkillID, mu.getBasicSkillValue (unitSkillID));
		
		// For unit stack skills, want the highest value of each, and ignore any that are nulls
		final Map<String, Integer> unitStackSkills = new HashMap<String, Integer> ();
		for (final MinimalUnitDetails thisMU : unitStack)
			for (final String unitSkillID : thisMU.listBasicSkillIDs ())
			{
				final Integer thisSkillValue = thisMU.getBasicOrHeroSkillValue (unitSkillID);
				if (thisSkillValue != null)
				{
					Integer bestSkillValue = unitStackSkills.get (unitSkillID);
					bestSkillValue = (bestSkillValue == null) ? thisSkillValue : Math.max (bestSkillValue, thisSkillValue);
					unitStackSkills.put (unitSkillID, bestSkillValue);
				}
			}
		
		// STEP 3 - Add in skills from spells - we must do this next since some skills from spells may grant other skills, e.g. Invulerability spell effect grants Weapon Immunity
		Integer confusionSpellOwner = null;
		Integer possessionSpellOwner = null;
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
						strength = ((effect.isStoreSkillValueAsVariableDamage () != null) && (effect.isStoreSkillValueAsVariableDamage ())) ?
							thisSpell.getVariableDamage () : effect.getUnitSkillValue ();
					}
				}
				
				// Its possible we don't find the effect at all, in some unusual cases like Stasis
				
				// Is it the unit we're calculating skills for?
				if ((unit instanceof MemoryUnit) && (((MemoryUnit) unit).getUnitURN () == thisSpell.getUnitURN ()))
				{
					basicSkillValues.put (thisSpell.getUnitSkillID (), strength);
					
					// If we find a confusion spell cast on the unit, remember who cast it
					if (thisSpell.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION))
						confusionSpellOwner = thisSpell.getCastingPlayerID ();

					if (thisSpell.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_POSSESSION))
						possessionSpellOwner = thisSpell.getCastingPlayerID ();
				}
				
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
			if (!getUnitUtils ().isSkillNegated (skill.getKey (), basicSkillValues, enemyUnits, db))
			{
				basicSkillValuesWithNegatedSkillsRemoved.put (skill.getKey (), skill.getValue ());
				
				// For any that are not negated, while we already have the skill definition found, see if the skill modifies the units magic realm/lifeform type
				final UnitSkillEx skillDef = db.findUnitSkill (skill.getKey (), "expandUnitDetails");
				if (skillDef.getChangesUnitToMagicRealm () != null)
					changedMagicRealmLifeformTypeIDs.add (skillDef.getChangesUnitToMagicRealm ());
			}
		}
		
		// STEP 7 - Do simple lookups that weren't done in minimal details
		final List<PlayerPick> picks = (mu.getOwningPlayer () == null) ? null : ((MomPersistentPlayerPublicKnowledge) mu.getOwningPlayer ().getPersistentPlayerPublicKnowledge ()).getPick ();
		
		final WeaponGrade weaponGrade = (unit.getWeaponGrade () == null) ? null : db.findWeaponGrade (unit.getWeaponGrade (), "expandUnitDetails");
		final RangedAttackTypeEx rangedAttackType = (mu.getUnitDefinition ().getRangedAttackType () == null) ? null : db.findRangedAttackType (mu.getUnitDefinition ().getRangedAttackType (), "expandUnitDetails");
		
		final String unitTypeID = db.findPick (mu.getUnitDefinition ().getUnitMagicRealm (), "expandUnitDetails").getUnitTypeID ();
		final UnitType unitType = db.findUnitType (unitTypeID, "expandUnitDetails");
		
		// STEP 8 - Work out the units magic realm/lifeform type
		// We made a list of overrides to it above already - now what we do depends on how many modifications we found
		final Pick magicRealmLifeformType;
		
		// No modifications - use value from unit definition, unaltered
		if (changedMagicRealmLifeformTypeIDs.size () == 0)
			magicRealmLifeformType = db.findPick (mu.getUnitDefinition ().getUnitMagicRealm (), "expandUnitDetails");

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
		
		// STEP 9 - Ensure we have complete list of all skills that we need to fully calculate all the components for
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
		
		// STEP 10 - Copy across basic skill values
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		for (final Entry<String, Integer> basicSkill : basicSkillValuesWithNegatedSkillsRemoved.entrySet ())
		{
			final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
			
			// Start a map of components of the breakdown
			if ((basicSkill.getValue () != null) && (basicSkill.getValue () > 0))
				breakdown.getComponents ().put (UnitSkillComponent.BASIC, basicSkill.getValue ());
				
			modifiedSkillValues.put (basicSkill.getKey (), breakdown);
		}
		
		// STEP 11 - If we're a boat, see who has Wind Mastery cast
		if (basicSkillValues.containsKey (CommonDatabaseConstants.UNIT_SKILL_ID_SAILING))
		{
			boolean ourWindMastery = false;
			boolean enemyWindMastery = false;
			
			for (final MemoryMaintainedSpell thisSpell : mem.getMaintainedSpell ())
				if (thisSpell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_WIND_MASTERY))
					if (thisSpell.getCastingPlayerID () == unit.getOwningPlayerID ())
						ourWindMastery = true;
					else
						enemyWindMastery = true;
			
			// If there is both, they just cancel each other out and there's no effect
			int bonus = 0;
			if ((ourWindMastery) && (!enemyWindMastery))
				bonus = basicSkillValues.get (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED) / 2;

			else if ((enemyWindMastery) && (!ourWindMastery))
				bonus = -(basicSkillValues.get (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED) / 2);
			
			if (bonus != 0)
			{
				final UnitSkillComponent component = UnitSkillComponent.SPELL_EFFECTS;
				final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED);
				
				Integer bonusValue = breakdown.getComponents ().get (component);
				bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonus;
				breakdown.getComponents ().put (component, bonusValue);
			}
		}
		
		// STEP 12 - Add bonuses from weapon grades
		if (weaponGrade != null)
			for (final AddsToSkill addsToSkill : weaponGrade.getAddsToSkill ())
				getExpandUnitDetailsUtils ().addSkillBonus (mu, null, addsToSkill, UnitSkillComponent.WEAPON_GRADE, modifiedSkillValues, unitStackSkills,
					attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformType.getPickID ());

		// STEP 13 - Add bonuses from experience
		if (mu.getModifiedExperienceLevel () != null)
			for (final UnitSkillAndValue bonus : mu.getModifiedExperienceLevel ().getExperienceSkillBonus ())
			{
				final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (bonus.getUnitSkillID ());
				if ((breakdown != null) && (bonus.getUnitSkillValue () != null))
					breakdown.getComponents ().put (UnitSkillComponent.EXPERIENCE, bonus.getUnitSkillValue ());
			}
		
		// STEP 14 - Add bonuses from combat area effects
		final List<String> skillsGrantedFromCombatAreaEffects = new ArrayList<String> ();
		for (final MemoryCombatAreaEffect thisCAE : mem.getCombatAreaEffect ())
			if (getUnitUtils ().doesCombatAreaEffectApplyToUnit (unit, thisCAE, db))
				
				// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites
				// So this means all the skill bonuses apply, except we still need to do the magic realm
				// check since some effects have different components which apply to different lifeform types, e.g. True Light and Darkness
				for (final String grantedSkillID : db.findCombatAreaEffect (thisCAE.getCombatAreaEffectID (), "expandUnitDetails").getCombatAreaEffectGrantsSkill ())
					
					// Adds this skill if we don't already have it (like Mass Invisibility granting Invisibility); these are all valueless
					if ((!modifiedSkillValues.containsKey (grantedSkillID)) && (!getUnitUtils ().isSkillNegated (grantedSkillID, modifiedSkillValues, enemyUnits, db)))
					{
						modifiedSkillValues.put (grantedSkillID, new UnitSkillValueBreakdown (UnitSkillComponent.COMBAT_AREA_EFFECTS));
						skillsGrantedFromCombatAreaEffects.add (grantedSkillID);
					}
		
		// STEP 15 - For any skills added from CAEs, recheck if they are negated - this is because High Prayer is added after Prayer
		for (final String unitSkillID : skillsGrantedFromCombatAreaEffects)
			if (getUnitUtils ().isSkillNegated (unitSkillID, modifiedSkillValues, enemyUnits, db))
				modifiedSkillValues.remove (unitSkillID);
		
		// STEP 16 - Skills that add to other skills (hero skills, and skills like Large Shield adding +2 defence, and bonuses to the whole stack like Resistance to All)
		for (final UnitSkillEx skillDef : db.getUnitSkills ())
			for (final AddsToSkill addsToSkill : skillDef.getAddsToSkill ())
			{
				final boolean haveRequiredSkill;
				UnitSkillComponent overrideComponent = null;
				
				if ((addsToSkill.isPenaltyToEnemy () != null) && (addsToSkill.isPenaltyToEnemy ()))
					haveRequiredSkill = (enemyUnits != null) && (enemyUnits.size () == 1) && (enemyUnits.get (0).hasModifiedSkill (skillDef.getUnitSkillID ()));
				
				else if (addsToSkill.isAffectsEntireStack ())
					haveRequiredSkill = unitStackSkills.containsKey (skillDef.getUnitSkillID ());
				
				else
				{
					final UnitSkillValueBreakdown requiredSkill = modifiedSkillValues.get (skillDef.getUnitSkillID ());
					haveRequiredSkill = (requiredSkill != null);
					if ((requiredSkill != null) && (requiredSkill.getSource () == UnitSkillComponent.COMBAT_AREA_EFFECTS))		// So still show in green on unit details
						overrideComponent = requiredSkill.getSource ();
				}
					
				if (haveRequiredSkill)
					getExpandUnitDetailsUtils ().addSkillBonus (mu, skillDef.getUnitSkillID (), addsToSkill, overrideComponent, modifiedSkillValues, unitStackSkills,
						attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformType.getPickID ());
			}
		
		// STEP 17 - Hero items - numeric bonuses (dealt with valueless skills above)
		if (unit instanceof MemoryUnit)
			for (final MemoryUnitHeroItemSlot slot : ((MemoryUnit) unit).getHeroItemSlot ())
				if (slot.getHeroItem () != null)
				{
					// Natural effects of the item type, e.g. +2 defence from plate mail
					final HeroItemType heroItemType = db.findHeroItemType (slot.getHeroItem ().getHeroItemTypeID (), "expandUnitDetails");
					for (final UnitSkillAndValue basicStat : heroItemType.getHeroItemTypeBasicStat ())
						if (basicStat.getUnitSkillValue () != null)
						{
							final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (basicStat.getUnitSkillID ());
							if ((breakdown != null) && (basicStat.getUnitSkillValue () != null))
								breakdown.getComponents ().put (UnitSkillComponent.HERO_ITEMS, basicStat.getUnitSkillValue ());
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
									final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (attackSkillID);
									if (breakdown != null)
									{
										// This might coincide with a basic stat, e.g. plate mail (+2 defence) with another +4 defence imbued onto it
										Integer bonusValue = breakdown.getComponents ().get (UnitSkillComponent.HERO_ITEMS);
										bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonusStat.getUnitSkillValue ();
										breakdown.getComponents ().put (UnitSkillComponent.HERO_ITEMS, bonusValue);
									}
								}
							}
		
							// Regular bonus - if we don't have the skill then add it; there's very few of these (Holy Avenger and Stoning; also -Spell Save)
							// This a bit different that bonuses from spells or CAEs, as those won't add valued skills to stop settlers gaining an attack or phantom warriors gaining defence
							// but every hero has at least 1 attack, 1 defence, 1 resistance, and so on 
							else
							{
								UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (bonusStat.getUnitSkillID ());
								if (breakdown == null)
								{
									breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.HERO_ITEMS);
									modifiedSkillValues.put (bonusStat.getUnitSkillID (), breakdown);
								}
								
								// This might coincide with a basic stat, e.g. plate mail (+2 defence) with another +4 defence imbued onto it
								Integer bonusValue = breakdown.getComponents ().get (UnitSkillComponent.HERO_ITEMS);
								bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonusStat.getUnitSkillValue ();
								breakdown.getComponents ().put (UnitSkillComponent.HERO_ITEMS, bonusValue);
							}
				}
		
		// STEP 18 - If we falied to find any + to hit / + to block values and we have no basic value then there's no point keeping it in the list
		// We know the entry has to exist and have a valid map in it from the code above, but it may be an empty map
		for (final String unitSkillID : SKILLS_WHERE_BONUSES_APPLY_EVEN_IF_NO_BASIC_SKILL)
			if (modifiedSkillValues.get (unitSkillID).getComponents ().isEmpty ())
				modifiedSkillValues.remove (unitSkillID);
		
		// STEP 19 - Apply any skill adjustments that set to a fixed value (shatter), divide by a value (warp creature) or multiply by a value (berserk) 
		for (final UnitSkillEx skillDef : db.getUnitSkills ())
			for (final AddsToSkill addsToSkill : skillDef.getAddsToSkill ())
			{
				final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (addsToSkill.getAddsToSkillID ());
				
				// Note the value != null check is here, isn't in the bonuses block above, because we assume we won't get skills like "shatter 1" vs "shatter 2"
				// which set the level of penalty
				if ((breakdown != null) && (addsToSkill.getAddsToSkillValue () != null) && ((addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.LOCK) ||
					(addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.DIVIDE) || (addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.MULTIPLY)))
				{
					final boolean haveRequiredSkill;
					if ((addsToSkill.isPenaltyToEnemy () != null) && (addsToSkill.isPenaltyToEnemy ()))
						haveRequiredSkill = (enemyUnits != null) && (enemyUnits.size () == 1) && (enemyUnits.get (0).hasModifiedSkill (skillDef.getUnitSkillID ()));
					else
						haveRequiredSkill = (addsToSkill.isAffectsEntireStack () ? unitStackSkills : modifiedSkillValues).containsKey (skillDef.getUnitSkillID ());
					
					if (haveRequiredSkill)
					{
						// If we have no info about the kind of attack being made, or this isn't in reference to an attack at all, then discount the bonus
						// if it has any restrictions that depend on the kind of incoming attack, even if we match those restrictions.
						// This is to stop the bonus from Large Shield showing on the unit info screen.
						if ((attackFromSkillID == null) && (attackFromMagicRealmID == null) &&
							((addsToSkill.getOnlyVersusAttacksFromSkillID () != null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null)))
						{
							// Ignore
						}
						
						// If the bonus only applies in combat, and we aren't in combat, then ignore it.
						else if ((addsToSkill.isOnlyInCombat () != null) && (addsToSkill.isOnlyInCombat ()) && (!isInCombat))
						{
							// Ignore
						}
						
						// If the bonus only applies to specific magic realms then ignore it if doesn't match
						else if ((addsToSkill.getOnlyAppliesToMagicRealmID () != null) && (!addsToSkill.getOnlyAppliesToMagicRealmID ().equals (magicRealmLifeformType.getPickID ())))
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
							if (((addsToSkill.getRangedAttackTypeID () == null) || (addsToSkill.getRangedAttackTypeID ().equals (mu.getUnitDefinition ().getRangedAttackType ()))) &&
								((addsToSkill.getOnlyVersusAttacksFromSkillID () == null) || (onlyVersusAttacksFromSkillIDCheckPasses)) &&
								((addsToSkill.getOnlyVersusAttacksFromMagicRealmID () == null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID ().equals (attackFromMagicRealmID))))
							{
								// How is the bonus calculated - fixed value, value from the skill, etc
								UnitSkillComponent component;
								if ((addsToSkill.isPenaltyToEnemy () != null) && (addsToSkill.isPenaltyToEnemy ()))
									component = UnitSkillComponent.PENALTIES;
								else
									component = addsToSkill.isAffectsEntireStack () ? UnitSkillComponent.STACK : UnitSkillComponent.SPELL_EFFECTS;
								
								// Set to a fixed value?
								int currentSkillValue = 0;
								for (final Entry<UnitSkillComponent, Integer> c : breakdown.getComponents ().entrySet ())
									if (c.getValue () == null)
										throw new MomException ("expandUnitDetails on " + unit.getUnitID () + " trying to sum addsFromSkill ID " + addsToSkill.getAddsToSkillID () + " for penalties but the " + c.getKey () + " component is null");
									else
										currentSkillValue = currentSkillValue + c.getValue ();
								
								final int newValue;
								if (addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.LOCK)
									newValue = addsToSkill.getAddsToSkillValue ();
								
								// Divide by a value?
								else if (addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.DIVIDE)
									newValue = currentSkillValue / addsToSkill.getAddsToSkillValue ();
								
								// Multiply by a value?
								else
									newValue = currentSkillValue * addsToSkill.getAddsToSkillValue ();

								// LOCK and DIVIDE are used as penalties, so never allow these to improve an already bad stat
								final int bonus;
								if ((currentSkillValue < newValue) && (addsToSkill.getAddsToSkillValueType () != AddsToSkillValueType.MULTIPLY))
									bonus = 0;
								else
									bonus = newValue - currentSkillValue;
								
								if (bonus != 0)
								{
									Integer bonusValue = breakdown.getComponents ().get (component);
									bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonus;
									breakdown.getComponents ().put (component, bonusValue);
								}
							}
						}
					}
				}
			}
		
		// STEP 20 - Basic upkeep values - just copy from the unit definition
		final Map<String, Integer> basicUpkeepValues = mu.getUnitDefinition ().getUnitUpkeep ().stream ().collect
			(Collectors.toMap (u -> u.getProductionTypeID (), u -> u.getUndoubledProductionValue ()));
		
		// STEP 21 - Modify upkeep values
		final Map<String, Integer> modifiedUpkeepValues;

		// Upkeep for undead is zeroed for normal units and adds +50% for summoned creatures
		final int unitTypeUpkeepPercentage = modifiedSkillValues.containsKey (CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD) ? unitType.getUndeadUpkeepPercentage () : 100;
		if ((unitTypeUpkeepPercentage <= 0) || (basicUpkeepValues.isEmpty ()))
			modifiedUpkeepValues = new HashMap<String, Integer> ();		// Empty map
		else
		{
			// Noble replaces usual gold consumption with gain of +10 gold
			final boolean isNoble = modifiedSkillValues.containsKey (CommonDatabaseConstants.UNIT_SKILL_ID_NOBLE);
			
			// Reduce upkeep for Summoner retort?
			// Get reduction as a percentage - note we use the special "unit upkeep" production type, not "Mana"
			final int percentageReduction = (picks == null) ? 0 : getPlayerPickUtils ().totalProductionBonus
				(CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, unitType.getUnitTypeID (), picks, db);

			// Now copy and modify each basic skill value
			modifiedUpkeepValues = basicUpkeepValues.entrySet ().stream ().collect (Collectors.toMap (u -> u.getKey (), u ->
			{
				final int modifiedUpkeepValue;
				if ((isNoble) && (u.getKey ().equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)))
					modifiedUpkeepValue = -10;	// negative upkeep, so actually gives +10
				else
				{
					final int baseUpkeepValue = (u.getValue () * unitTypeUpkeepPercentage) / 100;
	
					// Calculate actual amount of reduction, rounding down
					final int amountReduction = (baseUpkeepValue * percentageReduction) / 100;
					
					// Note its impossible to actually get zero here since we round the reduction down, unless percentageReduction reached 100 which will never happen
					modifiedUpkeepValue = baseUpkeepValue - amountReduction;
				}
				return modifiedUpkeepValue;
			}));
		}
		
		// STEP 22 - Work out who has control of the unit at the moment
		int controllingPlayerID = unit.getOwningPlayerID ();
		if ((unit instanceof MemoryUnit) && (((MemoryUnit) unit).getConfusionEffect () == ConfusionEffect.CASTER_CONTROLLED) && (confusionSpellOwner != null))
			controllingPlayerID = confusionSpellOwner;
		
		else if (possessionSpellOwner != null)
			controllingPlayerID = possessionSpellOwner;
		
		// Finally can build the unit object
		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, mu.getUnitDefinition (), unitType, mu.getOwningPlayer (), magicRealmLifeformType,
			weaponGrade, rangedAttackType, mu.getBasicExperienceLevel (), mu.getModifiedExperienceLevel (), controllingPlayerID,
			basicSkillValues, modifiedSkillValues, basicUpkeepValues, modifiedUpkeepValues, getUnitUtils ());
		return xu;
	}

	/**
	 * @return Methods for working out minimal unit details
	 */
	public final UnitDetailsUtils getUnitDetailsUtils ()
	{
		return unitDetailsUtils;
	}

	/**
	 * @param u Methods for working out minimal unit details
	 */
	public final void setUnitDetailsUtils (final UnitDetailsUtils u)
	{
		unitDetailsUtils = u;
	}

	/**
	 * @return Sections broken out from the big expandUnitDetails method to make it more manageable
	 */
	public final ExpandUnitDetailsUtils getExpandUnitDetailsUtils ()
	{
		return expandUnitDetailsUtils;
	}

	/**
	 * @param u Sections broken out from the big expandUnitDetails method to make it more manageable
	 */
	public final void setExpandUnitDetailsUtils (final ExpandUnitDetailsUtils u)
	{
		expandUnitDetailsUtils = u;
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
}
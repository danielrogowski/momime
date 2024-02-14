package momime.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.AddsToSkillValueType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.HeroItemType;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpellEffect;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;

/**
 * Sections broken out from the big expandUnitDetails method to make it more manageable
 */
public final class ExpandUnitDetailsUtilsImpl implements ExpandUnitDetailsUtils
{
	/** Methods for working out minimal unit details */
	private UnitDetailsUtils unitDetailsUtils;

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * Searches for other units we might get unit stack bonuses like Pathfinding or Holy Bonus from.
	 * If we're in combat, then the unit stack means all the other units who are in combat with us.
	 * If we aren't in combat, then the unit stack means the other units in the same overland map square as us.  
	 * 
	 * @param unit Unit we are calculating stats for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of minimal computed details for all units stacked with the unit whose stats we're calculating
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final List<MinimalUnitDetails> buildUnitStackMinimalDetails (final AvailableUnit unit,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final List<MinimalUnitDetails> unitStack = new ArrayList<MinimalUnitDetails> ();
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
					
					if (thisUnit == unit)
						mu = thisMU;
				}
		
		if (mu == null)
		{
			mu = getUnitDetailsUtils ().expandMinimalUnitDetails (unit, players, mem, db);
			unitStack.add (mu);
		}
		
		return unitStack;
	}
	
	/**
	 * @param unitStack All units in the unit stack
	 * @return List of Unit URNs of the stacked units that are MemoryUnits
	 * @throws MomException If a unit claims to be a MemoryUnit but then won't provide its UnitURN
	 */
	@Override
	public final List<Integer> buildUnitStackUnitURNs (final List<MinimalUnitDetails> unitStack) throws MomException
	{
		final List<Integer> unitStackUnitURNs = new ArrayList<Integer> ();
		for (final MinimalUnitDetails muStack : unitStack)
			if (muStack.isMemoryUnit ())
				unitStackUnitURNs.add (muStack.getUnitURN ());
		
		return unitStackUnitURNs;
	}
	
	/**
	 * @param unitStack All units in the unit stack
	 * @return Map of all valued skills in the stack, picking out the highest value of each skill
	 * @throws MomException If we try to get the value of a skill the unit does not have
	 */
	@Override
	public final Map<String, Integer> getHighestSkillsInUnitStack (final List<MinimalUnitDetails> unitStack) throws MomException
	{
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
		
		return unitStackSkills;
	}
	
	/**
	 * @param mu Unit we are calculating stats for
	 * @param spells List of known spells
	 * @param unitStackUnitURNs List of unit URNs in the stack
	 * @param basicSkillValues Map of all skills the unit we are calculating has; will be added to
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill; will be added to
	 * @param db Lookup lists built over the XML database
	 * @return Map of unitSkillIDs granted by spells, keyed by skill ID with the value being the ID of the player who cast it on the unit
	 * @throws RecordNotFoundException If we encounter a spell we can't find the definition for
	 * @throws MomException If a unit claims to be a MemoryUnit but then won't provide its UnitURN
	 */
	@Override
	public final Map<String, Integer> addSkillsFromSpells (final MinimalUnitDetails mu, final List<MemoryMaintainedSpell> spells, final List<Integer> unitStackUnitURNs,
		final Map<String, Integer> basicSkillValues, final Map<String, Integer> unitStackSkills, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = new HashMap<String, Integer> ();
		
		for (final MemoryMaintainedSpell thisSpell : spells)
			if ((thisSpell.getUnitURN () != null) && (unitStackUnitURNs.contains (thisSpell.getUnitURN ())) && (thisSpell.getUnitSkillID () != null))
			{
				// See if the spell definition defines a strength - this is for things like casting Immolation on a unit - we have to know that it is "Immolation 4"
				final Spell spellDef = db.findSpell (thisSpell.getSpellID (), "addSkillsFromSpells");
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
				if ((mu.isMemoryUnit ()) && (mu.getUnitURN () == thisSpell.getUnitURN ()))
				{
					basicSkillValues.put (thisSpell.getUnitSkillID (), strength);
					skillsFromSpellsCastOnThisUnit.put (thisSpell.getUnitSkillID (), thisSpell.getCastingPlayerID ());
				}
				
				// List the skill in the unit stack skills?
				if (strength != null)
				{
					Integer skillValue = unitStackSkills.get (thisSpell.getUnitSkillID ());
					skillValue = (skillValue == null) ? strength : Math.max (skillValue, strength);
					unitStackSkills.put (thisSpell.getUnitSkillID (), skillValue);
				}
			}
		
		return skillsFromSpellsCastOnThisUnit;
	}
	
	/**
	 * Searches for valueless skills granted either from hero basic item types (Shields grant Large Shield skill) or from bonuses added to the hero item
	 * 
	 * @param slots The item slots on the unit, which may have items in them
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param basicSkillValues Map of all skills the unit we are calculating has; will be added to
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a hero item type or bonus not found in the database
	 */
	@Override
	public final void addValuelessSkillsFromHeroItems (final List<MemoryUnitHeroItemSlot> slots, final String attackFromSkillID,
		final Map<String, Integer> basicSkillValues, final CommonDatabase db)
		throws RecordNotFoundException
	{
		for (final MemoryUnitHeroItemSlot slot : slots)
			if (slot.getHeroItem () != null)
			{
				// Natural effects of the item type - as far as valueless skills go, this is the Large Shield skill you get automatically from using shield items
				final HeroItemType heroItemType = db.findHeroItemType (slot.getHeroItem ().getHeroItemTypeID (), "addValuelessSkillsFromHeroItems");
				for (final UnitSkillAndValue basicStat : heroItemType.getHeroItemTypeBasicStat ())
					if ((basicStat.getUnitSkillValue () == null) && (!basicSkillValues.containsKey (basicStat.getUnitSkillID ())))
						basicSkillValues.put (basicStat.getUnitSkillID (), null);
				
				// Bonuses imbued on the item
				for (final String bonusID : slot.getHeroItem ().getHeroItemChosenBonus ())
					for (final HeroItemBonusStat bonusStat : db.findHeroItemBonus (bonusID, "addValuelessSkillsFromHeroItems").getHeroItemBonusStat ())
						if ((bonusStat.getUnitSkillValue () == null) && (!basicSkillValues.containsKey (bonusStat.getUnitSkillID ())))

							// Some bonuses only apply if the attackFromSkillID matches the kind of item they're imbued in
							if ((bonusStat.isAppliesOnlyToAttacksAppropriateForTypeOfHeroItem () == null) || (!bonusStat.isAppliesOnlyToAttacksAppropriateForTypeOfHeroItem ()) ||
								((attackFromSkillID != null) && (heroItemType.getHeroItemTypeAttackType ().stream ().anyMatch (t -> t.equals (attackFromSkillID)))))
							
								basicSkillValues.put (bonusStat.getUnitSkillID (), null);
			}
	}
	
	/**
	 * Checks over all skills we already have to see if any of those skills grant others, and in turn if those skills grant more, and so on.
	 * This is for things like Invulnerability granting Weapon Immunity, or Undead granting a whole pile of immunities.
	 * 
	 * @param basicSkillValues Map of all skills the unit we are calculating has; will be added to
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a skill not found in the database
	 */
	@Override
	public final void addSkillsGrantedFromOtherSkills (final Map<String, Integer> basicSkillValues, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final List<String> skillsLeftToCheck = basicSkillValues.keySet ().stream ().collect (Collectors.toList ());
		while (skillsLeftToCheck.size () > 0)
		{
			final UnitSkillEx skillDef = db.findUnitSkill (skillsLeftToCheck.get (0), "addSkillsGrantedFromOtherSkills");
			skillsLeftToCheck.remove (0);
			
			skillDef.getGrantsSkill ().stream ().forEach (s ->
			{
				if (!basicSkillValues.containsKey (s))
				{
					basicSkillValues.put (s, null);
					skillsLeftToCheck.add (s);
				}
			});
		}
	}
	
	/**
	 * @param basicSkillValues Map of all skills the unit we are calculating has
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param db Lookup lists built over the XML database
	 * @return Copy of basicSkillValues, but with negated skills removed
	 * @throws RecordNotFoundException If we can't find a skill definition
	 * @throws MomException If a skill definition has an unknown negatedByUnitID value
	 */
	@Override
	public final Map<String, Integer> copySkillValuesRemovingNegatedSkills (final Map<String, Integer> basicSkillValues,
		final List<ExpandedUnitDetails> enemyUnits, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved = new HashMap<String, Integer> ();
		
		for (final Entry<String, Integer> skill : basicSkillValues.entrySet ())
			if (!getUnitUtils ().isSkillNegated (skill.getKey (), basicSkillValues, enemyUnits, db))
				basicSkillValuesWithNegatedSkillsRemoved.put (skill.getKey (), skill.getValue ());
		
		return basicSkillValuesWithNegatedSkillsRemoved;
	}
	
	/**
	 * Skills like Undead and Chaos Channels can modify a unit's magic realm/lifeform type.  So this
	 * method works out what the unit's modified magic realm/lifeform type is.
	 * 
	 * @param defaultMagicRealmID The default magic realm/lifeform type that will be output if no overrides are found
	 * @param basicSkillValuesWithNegatedSkillsRemoved Map of all skills the unit we are calculating has, with negated skills already removed
	 * @param db Lookup lists built over the XML database
	 * @return Modified magic realm/lifeform
	 * @throws RecordNotFoundException If we can't find one of the unit skills or picks involved
	 * @throws MomException If we find override(s), but no magic realm/lifeform is defined for that combination of override(s)
	 */
	@Override
	public final Pick determineModifiedMagicRealmLifeformType (final String defaultMagicRealmID,
		final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		final Set<String> changedMagicRealmLifeformTypeIDs = new HashSet<String> ();
		for (final String unitSkillID : basicSkillValuesWithNegatedSkillsRemoved.keySet ())
		{
			final UnitSkillEx skillDef = db.findUnitSkill (unitSkillID, "determineModifiedMagicRealmLifeformType");
			if (skillDef.getChangesUnitToMagicRealm () != null)
				changedMagicRealmLifeformTypeIDs.add (skillDef.getChangesUnitToMagicRealm ());
		}
		
		final Pick magicRealmLifeformType;
		
		// No modifications - use value from unit definition, unaltered
		if (changedMagicRealmLifeformTypeIDs.size () == 0)
			magicRealmLifeformType = db.findPick (defaultMagicRealmID, "determineModifiedMagicRealmLifeformType");

		// Exactly one modification - use the value set by that skill (i.e. unit is Undead or Chaos Channeled)
		else if (changedMagicRealmLifeformTypeIDs.size () == 1)
			magicRealmLifeformType = db.findPick (changedMagicRealmLifeformTypeIDs.iterator ().next (), "determineModifiedMagicRealmLifeformType");
		
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
		
		return magicRealmLifeformType;
	}
	
	/**
	 * @param basicSkillValuesWithNegatedSkillsRemoved Map of all skills the unit we are calculating has, with negated skills already removed
	 * @return Initial skill breakdown map, with basic components added
	 */
	@Override
	public final Map<String, UnitSkillValueBreakdown> buildInitialBreakdownFromBasicSkills
		(final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved)
	{
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = new HashMap<String, UnitSkillValueBreakdown> ();
		for (final Entry<String, Integer> basicSkill : basicSkillValuesWithNegatedSkillsRemoved.entrySet ())
		{
			final UnitSkillValueBreakdown breakdown = new UnitSkillValueBreakdown (UnitSkillComponent.BASIC);
			
			// Start a map of components of the breakdown
			if ((basicSkill.getValue () != null) && (basicSkill.getValue () > 0))
				breakdown.getComponents ().put (UnitSkillComponent.BASIC, basicSkill.getValue ());
				
			modifiedSkillValues.put (basicSkill.getKey (), breakdown);
		}
		
		return modifiedSkillValues;
	}
	
	/**
	 * Boats sail 50% faster if we have Wind Mastery cast, or 50% slower if someone else does.
	 * 
	 * @param basicSkillValues Map of all skills the unit we are calculating has
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitOwnerPlayerID Player who owns 
	 * @param spells List of known spells
	 */
	@Override
	public final void adjustMovementSpeedForWindMastery (final Map<String, Integer> basicSkillValues,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final int unitOwnerPlayerID, final List<MemoryMaintainedSpell> spells)
	{
		if (basicSkillValues.containsKey (CommonDatabaseConstants.UNIT_SKILL_ID_SAILING))
		{
			boolean ourWindMastery = false;
			boolean enemyWindMastery = false;
			
			for (final MemoryMaintainedSpell thisSpell : spells)
				if (thisSpell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_WIND_MASTERY))
					if (thisSpell.getCastingPlayerID () == unitOwnerPlayerID)
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
	}
	
	/**
	 * @param mu Unit we are calculating stats for
	 * @param weaponGrade Weapon grade to add bonuses from
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void addBonusesFromWeaponGrade (final MinimalUnitDetails mu, final WeaponGrade weaponGrade,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final Map<String, Integer> unitStackSkills,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException
	{
		for (final AddsToSkill addsToSkill : weaponGrade.getAddsToSkill ())
			getUnitDetailsUtils ().addSkillBonus (mu, null, addsToSkill, UnitSkillComponent.WEAPON_GRADE, modifiedSkillValues, unitStackSkills,
				attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformTypeID);
	}
	
	/**
	 * @param expLvl Experience level to add bonuses from
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 */
	@Override
	public final void addBonusesFromExperienceLevel (final ExperienceLevel expLvl, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues)
	{
		for (final UnitSkillAndValue bonus : expLvl.getExperienceSkillBonus ())
		{
			final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (bonus.getUnitSkillID ());
			if ((breakdown != null) && (bonus.getUnitSkillValue () != null))
				breakdown.getComponents ().put (UnitSkillComponent.EXPERIENCE, bonus.getUnitSkillValue ());
		}
	}
	
	/**
	 * Adds skills granted by CAEs.  Note this are only ever valueless skills.  CAEs can't directly add value bonuses, but do so by
	 * granting a unit skill and then that unit skill defines the bonuses.
	 * 
	 * @param unit Unit we are calculating stats for
	 * @param combatAreaEffects List of combat area effects to add bonuses from
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param db Lookup lists built over the XML database
	 * @return List of skills added that we didn't have before
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If a skill definition has an unknown negatedByUnitID value
	 */
	@Override
	public final List<String> addSkillsFromCombatAreaEffects (final AvailableUnit unit, final List<MemoryCombatAreaEffect> combatAreaEffects,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final List<ExpandedUnitDetails> enemyUnits,
		final String magicRealmLifeformTypeID, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		final List<String> skillsGrantedFromCombatAreaEffects = new ArrayList<String> ();
		for (final MemoryCombatAreaEffect thisCAE : combatAreaEffects)
			if (getUnitUtils ().doesCombatAreaEffectApplyToUnit (unit, thisCAE, db))
				
				// Found a combat area effect whose location matches this unit, as well as any player or other pre-requisites
				// So this means all the skill bonuses apply, except we still need to do the magic realm
				// check since some effects have different components which apply to different lifeform types, e.g. True Light and Darkness
				for (final String grantedSkillID : db.findCombatAreaEffect (thisCAE.getCombatAreaEffectID (), "addSkillsFromCombatAreaEffects").getCombatAreaEffectGrantsSkill ())
					
					// Adds this skill if we don't already have it (like Mass Invisibility granting Invisibility); these are all valueless
					if ((!modifiedSkillValues.containsKey (grantedSkillID)) && (!getUnitUtils ().isSkillNegated (grantedSkillID, modifiedSkillValues, enemyUnits, db)))
					{
						// Look to see if we can find a spell that grants the same skill
						final Spell spellThatGrantsSameSkill = db.getSpell ().stream ().filter (s -> s.getUnitSpellEffect ().stream ().anyMatch
							(e -> e.getUnitSkillID ().equals (grantedSkillID))).findAny ().orElse (null);
						
						// Has the spell got any restrictions on what it can be cast on?  This is to stop Holy Arms giving Holy Weapon to summoned creatures or undead
						boolean passesLifeformCheck = true;
						if ((spellThatGrantsSameSkill != null) && (spellThatGrantsSameSkill.getSpellValidUnitTarget ().size () > 0))
							passesLifeformCheck = spellThatGrantsSameSkill.getSpellValidUnitTarget ().stream ().anyMatch
								(t -> t.getTargetMagicRealmID ().equals (magicRealmLifeformTypeID));
						
						if (passesLifeformCheck)
						{
							modifiedSkillValues.put (grantedSkillID, new UnitSkillValueBreakdown (UnitSkillComponent.COMBAT_AREA_EFFECTS));
							skillsGrantedFromCombatAreaEffects.add (grantedSkillID);
						}
					}
		
		return skillsGrantedFromCombatAreaEffects;
	}
	
	/**
	 * Even though above method checks for skills granted from CAEs being negated before adding them, if combat has both Prayer and High Prayer
	 * cast, Prayer will be added first (at which point it isn't negated) and High Prayer after.  So now we need to go over the list again rechecking them.
	 * 
	 * @param skillsGrantedFromCombatAreaEffects List of skills granted from combat area effects
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If a skill definition has an unknown negatedByUnitID value
	 */
	@Override
	public final void removeNegatedSkillsAddedFromCombatAreaEffects (final List<String> skillsGrantedFromCombatAreaEffects,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final List<ExpandedUnitDetails> enemyUnits, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		for (final String unitSkillID : skillsGrantedFromCombatAreaEffects)
			if (getUnitUtils ().isSkillNegated (unitSkillID, modifiedSkillValues, enemyUnits, db))
				modifiedSkillValues.remove (unitSkillID);
	}

	/**
	 * Adds bonuses from skills which grant bonuses to other skills, for example Flame Blade granting +2 to melee attack.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void addBonusesFromOtherSkills (final MinimalUnitDetails mu, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final Map<String, Integer> unitStackSkills, final List<ExpandedUnitDetails> enemyUnits,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID, final CommonDatabase db)
		throws MomException
	{
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
					getUnitDetailsUtils ().addSkillBonus (mu, skillDef.getUnitSkillID (), addsToSkill, overrideComponent, modifiedSkillValues, unitStackSkills,
						attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformTypeID);
			}
	}
	
	/**
	 * Searches for bonuses granted either from hero basic item types (Plate Mail grants +2 defence) or from bonuses added to the hero item
	 * 
	 * @param slots The item slots on the unit, which may have items in them
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final void addBonusesFromHeroItems (final List<MemoryUnitHeroItemSlot> slots,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final String attackFromSkillID, final CommonDatabase db)
		throws RecordNotFoundException
	{
		for (final MemoryUnitHeroItemSlot slot : slots)
			if (slot.getHeroItem () != null)
			{
				// Natural effects of the item type, e.g. +2 defence from plate mail
				final HeroItemType heroItemType = db.findHeroItemType (slot.getHeroItem ().getHeroItemTypeID (), "addBonusesFromHeroItems");
				for (final UnitSkillAndValue basicStat : heroItemType.getHeroItemTypeBasicStat ())
					if (basicStat.getUnitSkillValue () != null)
					{
						final UnitSkillValueBreakdown breakdown = modifiedSkillValues.get (basicStat.getUnitSkillID ());
						if ((breakdown != null) && (basicStat.getUnitSkillValue () != null))
						{
							Integer bonusValue = breakdown.getComponents ().get (UnitSkillComponent.HERO_ITEMS);
							bonusValue = ((bonusValue == null) ? 0 : bonusValue) + basicStat.getUnitSkillValue ();
							breakdown.getComponents ().put (UnitSkillComponent.HERO_ITEMS, bonusValue);
						}
					}
				
				// Bonuses imbued on the item
				for (final String bonusID : slot.getHeroItem ().getHeroItemChosenBonus ())
					for (final HeroItemBonusStat bonusStat : db.findHeroItemBonus (bonusID, "addBonusesFromHeroItems").getHeroItemBonusStat ())
						// Just want bonuses with a numeric value
						if (bonusStat.getUnitSkillValue () == null)
						{
						}
						
						// Some bonuses only apply if the attackFromSkillID matches the kind of item they're imbued in
						else if ((bonusStat.isAppliesOnlyToAttacksAppropriateForTypeOfHeroItem () != null) && (bonusStat.isAppliesOnlyToAttacksAppropriateForTypeOfHeroItem ()) &&
							((attackFromSkillID == null) || (heroItemType.getHeroItemTypeAttackType ().stream ().noneMatch (t -> t.equals (attackFromSkillID)))))
						{
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
	}

	/**
	 * For now this is somewhat hard coded and just for the Chaos attribute, which halves the strength of attacks appropriate for the item type.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void addPenaltiesFromHeroItems (final MinimalUnitDetails mu, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		for (final MemoryUnitHeroItemSlot slot : mu.getMemoryUnit ().getHeroItemSlot ())
			if (slot.getHeroItem () != null)
			{
				final HeroItemType heroItemType = db.findHeroItemType (slot.getHeroItem ().getHeroItemTypeID (), "addPenaltiesFromHeroItems");
				
				// Penalties imbued on the item
				for (final String bonusID : slot.getHeroItem ().getHeroItemChosenBonus ())
					for (final HeroItemBonusStat bonusStat : db.findHeroItemBonus (bonusID, "addPenaltiesFromHeroItems").getHeroItemBonusStat ())
						if (bonusStat.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_DOOM_ATTACK))
							for (final String attackSkillID : heroItemType.getHeroItemTypeAttackType ())
							{
								final AddsToSkill addsToSkill = new AddsToSkill ();
								addsToSkill.setAddsToSkillID (attackSkillID);
								addsToSkill.setAddsToSkillValueType (AddsToSkillValueType.DIVIDE);
								addsToSkill.setAddsToSkillValue (2);
								
								getUnitDetailsUtils ().addSkillPenalty (mu, addsToSkill, null,
									modifiedSkillValues, attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformTypeID);
							}
			}
	}

	/**
	 * Adds penalties from skills which lock/divide/multiply other skills late in the calculation, for example Shatter locking melee attack at 1
	 * or Warp Creature dividing melee attack by 2.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param unitStackSkills Map of all valued skills in the stack, picking out the highest value of each skill
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Unit's modified magic realm/lifeform type
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void addPenaltiesFromOtherSkills (final MinimalUnitDetails mu, final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final Map<String, Integer> unitStackSkills, final List<ExpandedUnitDetails> enemyUnits,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID, final CommonDatabase db)
		throws MomException
	{
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
					getUnitDetailsUtils ().addSkillPenalty (mu, addsToSkill, overrideComponent,
						modifiedSkillValues, attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformTypeID);
			}
	}
	
	/**
	 * Modifies basic upkeep values for the unit according to skills like Noble and retorts like Summoner which may reduce it, or Undead which may increase or zero it.
	 * 
	 * @param mu Unit we are calculating stats for
	 * @param basicUpkeepValues Basic upkeep values, copied directly from the unit definition in the XML
	 * @param modifiedSkillValues Detailed breakdown of calculation of skill values
	 * @param db Lookup lists built over the XML database
	 * @return Modified upkeep values
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final Map<String, Integer> buildModifiedUpkeepValues (final MinimalUnitDetails mu, final Map<String, Integer> basicUpkeepValues,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final CommonDatabase db)
		throws RecordNotFoundException
	{
		// Upkeep for undead is zeroed for normal units and adds +50% for summoned creatures
		final int unitTypeUpkeepPercentage = modifiedSkillValues.containsKey
			(CommonDatabaseConstants.UNIT_SKILL_ID_UNDEAD) ? mu.getUnitType ().getUndeadUpkeepPercentage () : 100;
		
		final Map<String, Integer> modifiedUpkeepValues;
		if ((unitTypeUpkeepPercentage <= 0) || (basicUpkeepValues.isEmpty ()))
			modifiedUpkeepValues = new HashMap<String, Integer> ();		// Empty map
		else
		{
			final List<PlayerPick> picks = (mu.getOwningWizard () == null) ? null : mu.getOwningWizard ().getPick ();

			// Noble replaces usual gold consumption with gain of +10 gold
			final boolean isNoble = modifiedSkillValues.containsKey (CommonDatabaseConstants.UNIT_SKILL_ID_NOBLE);
			
			// Reduce upkeep for Summoner retort?
			// Get reduction as a percentage - note we use the special "unit upkeep" production type, not "Mana"
			final int percentageReduction = (picks == null) ? 0 : getPlayerPickUtils ().totalProductionBonus
				(CommonDatabaseConstants.PRODUCTION_TYPE_ID_UNIT_UPKEEP_REDUCTION, mu.getUnitType ().getUnitTypeID (), picks, db);

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
		
		return modifiedUpkeepValues;
	}
	
	/**
	 * @param mu Unit we are calculating stats for
	 * @param skillsFromSpellsCastOnThisUnit Map of unitSkillIDs granted by spells, keyed by skill ID with the value being the ID of the player who cast it on the unit
	 * @return Which player controls this unit in combat this round
	 * @throws MomException If a unit claims to be a MemoryUnit but then fails to typecast itself
	 */
	@Override
	public final int determineControllingPlayerID (final MinimalUnitDetails mu, final Map<String, Integer> skillsFromSpellsCastOnThisUnit)
		throws MomException
	{
		final Integer confusionSpellOwner = skillsFromSpellsCastOnThisUnit.get (CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION);
		
		Integer possessionSpellOwner = null;		
		for (final String possessionSkillID : CommonDatabaseConstants.UNIT_SKILL_IDS_POSSESSION)
		{
			final Integer thisPossessionSpellOwner = skillsFromSpellsCastOnThisUnit.get (possessionSkillID);
			if (thisPossessionSpellOwner != null)
				possessionSpellOwner = thisPossessionSpellOwner;
		}
		
		final int controllingPlayerID;
		if ((mu.isMemoryUnit ()) && (mu.getMemoryUnit ().getConfusionEffect () == ConfusionEffect.CASTER_CONTROLLED) && (confusionSpellOwner != null))
			controllingPlayerID = confusionSpellOwner;
		
		else if (possessionSpellOwner != null)
			controllingPlayerID = possessionSpellOwner;
		
		else
			controllingPlayerID = mu.getOwningPlayerID ();
		
		return controllingPlayerID;
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
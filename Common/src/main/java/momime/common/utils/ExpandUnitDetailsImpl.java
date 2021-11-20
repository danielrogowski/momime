package momime.common.utils;

import java.util.List;
import java.util.Map;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Pick;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;

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
	
	/** Sections broken out from the big expandUnitDetails method to make it more manageable */
	private ExpandUnitDetailsUtils expandUnitDetailsUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
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
		final List<MinimalUnitDetails> unitStack = getExpandUnitDetailsUtils ().buildUnitStackMinimalDetails (unit, players, mem, db);
		final List<Integer> unitStackUnitURNs = getExpandUnitDetailsUtils ().buildUnitStackUnitURNs (unitStack);
		
		final MinimalUnitDetails mu = unitStack.stream ().filter (u -> u.getUnit () == unit).findAny ().orElse (null);
		if (mu == null)
			throw new MomException ("The unit we are calculating stats for isn't in the unit stack");
		
		// STEP 2 - First just copy the basic skills from the minimal details
		final Map<String, Integer> basicSkillValues = mu.getBasicSkillValues ();
		
		// For unit stack skills, want the highest value of each, and ignore any that are nulls
		final Map<String, Integer> unitStackSkills = getExpandUnitDetailsUtils ().getHighestSkillsInUnitStack (unitStack);
		
		// STEP 3 - Add in skills from spells - we must do this next since some skills from spells may grant other skills, e.g. Invulerability spell effect grants Weapon Immunity
		final Map<String, Integer> skillsFromSpellsCastOnThisUnit = getExpandUnitDetailsUtils ().addSkillsFromSpells
			(mu, mem.getMaintainedSpell (), unitStackUnitURNs, basicSkillValues, unitStackSkills, db);
		
		// STEP 4 - Add in valueless skills from hero items - again need to do this early since some may grant other skills, e.g. Invulnerability imbued on a hero item grants Weapon Immunity
		// We deal with numeric bonuses from hero items much lower down
		if (mu.isMemoryUnit ())
			getExpandUnitDetailsUtils ().addValuelessSkillsFromHeroItems (mu.getMemoryUnit ().getHeroItemSlot (), attackFromSkillID, basicSkillValues, db);
		
		// STEP 5 - Now check all skills to see if any grant other skills
		// This is N/A for unit stack skills since granted skills always have a null skill strength value
		getExpandUnitDetailsUtils ().addSkillsGrantedFromOtherSkills (basicSkillValues, db);
		
		// STEP 6 - Create new map, eliminating from it skills that are negated
		// If we have the skill, before we go any further see if anything negates it
		final Map<String, Integer> basicSkillValuesWithNegatedSkillsRemoved = getExpandUnitDetailsUtils ().copySkillValuesRemovingNegatedSkills
			(basicSkillValues, enemyUnits, db);
		
		// STEP 7 - Work out the units magic realm/lifeform type
		// Make a list of overrides to it - after that what we do depends on how many modifications we found
		final Pick magicRealmLifeformType = getExpandUnitDetailsUtils ().determineModifiedMagicRealmLifeformType
			(mu.getUnitDefinition ().getUnitMagicRealm (), basicSkillValuesWithNegatedSkillsRemoved, db);
		
		// STEP 8 - Ensure we have complete list of all skills that we need to fully calculate all the components for
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
		
		// STEP 9 - Copy across basic skill values
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues = getExpandUnitDetailsUtils ().buildInitialBreakdownFromBasicSkills
			(basicSkillValuesWithNegatedSkillsRemoved);
		
		// STEP 10 - If we're a boat, see who has Wind Mastery cast
		getExpandUnitDetailsUtils ().adjustMovementSpeedForWindMastery (basicSkillValues, modifiedSkillValues, unit.getOwningPlayerID (), mem.getMaintainedSpell ());
		
		// STEP 11 - Add bonuses from weapon grades
		final WeaponGrade weaponGrade = (unit.getWeaponGrade () == null) ? null : db.findWeaponGrade (unit.getWeaponGrade (), "expandUnitDetails");
		if (weaponGrade != null)
			getExpandUnitDetailsUtils ().addBonusesFromWeaponGrade (mu, weaponGrade, modifiedSkillValues, unitStackSkills,
				attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformType.getPickID ());

		// STEP 12 - Add bonuses from experience
		if (mu.getModifiedExperienceLevel () != null)
			getExpandUnitDetailsUtils ().addBonusesFromExperienceLevel (mu.getModifiedExperienceLevel (), modifiedSkillValues);
		
		// STEP 13 - Add bonuses from combat area effects
		final List<String> skillsGrantedFromCombatAreaEffects = getExpandUnitDetailsUtils ().addSkillsFromCombatAreaEffects
			(unit, mem.getCombatAreaEffect (), modifiedSkillValues, enemyUnits, magicRealmLifeformType.getPickID (), db);
		
		// Small point here but, since CAEs can add skills we didn't previously have (which isn't the case with other kinds of bonuses), if the
		// added skills are defined to grant further skills (which we did back in step 5) that won't happen.  But I don't think this is really needed.
		
		// STEP 14 - For any skills added from CAEs, recheck if they are negated - this is because High Prayer is added after Prayer
		getExpandUnitDetailsUtils ().removeNegatedSkillsAddedFromCombatAreaEffects (skillsGrantedFromCombatAreaEffects, modifiedSkillValues, enemyUnits, db);
		
		// STEP 15 - Skills that add to other skills (hero skills, and skills like Large Shield adding +2 defence, and bonuses to the whole stack like Resistance to All)
		getExpandUnitDetailsUtils ().addBonusesFromOtherSkills (mu, modifiedSkillValues, unitStackSkills, enemyUnits,
			attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformType.getPickID (), db);
		
		// STEP 16 - Hero items - numeric bonuses (dealt with valueless skills above)
		if (mu.isMemoryUnit ())
			getExpandUnitDetailsUtils ().addBonusesFromHeroItems (mu.getMemoryUnit ().getHeroItemSlot (), modifiedSkillValues, db);
		
		// STEP 17 - If we falied to find any + to hit / + to block values and we have no basic value then there's no point keeping it in the list
		// We know the entry has to exist and have a valid map in it from the code above, but it may be an empty map
		for (final String unitSkillID : SKILLS_WHERE_BONUSES_APPLY_EVEN_IF_NO_BASIC_SKILL)
			if (modifiedSkillValues.get (unitSkillID).getComponents ().isEmpty ())
				modifiedSkillValues.remove (unitSkillID);
		
		// STEP 18 - Apply any skill adjustments that set to a fixed value (shatter), divide by a value (warp creature) or multiply by a value (berserk)
		getExpandUnitDetailsUtils ().addPenaltiesFromOtherSkills (mu, modifiedSkillValues, unitStackSkills, enemyUnits,
			attackFromSkillID, attackFromMagicRealmID, magicRealmLifeformType.getPickID (), db);
		
		// STEP 19 - Basic upkeep values - just copy from the unit definition
		final Map<String, Integer> basicUpkeepValues = mu.getBasicUpeepValues ();
		
		// STEP 20 - Modify upkeep values
		final Map<String, Integer> modifiedUpkeepValues = getExpandUnitDetailsUtils ().buildModifiedUpkeepValues (mu, basicUpkeepValues, modifiedSkillValues, db);
		
		// STEP 21 - Work out who has control of the unit at the moment
		final int controllingPlayerID = getExpandUnitDetailsUtils ().determineControllingPlayerID (mu, skillsFromSpellsCastOnThisUnit);
		
		// STEP 22 - Finally can build the unit object
		final RangedAttackTypeEx rangedAttackType = (mu.getUnitDefinition ().getRangedAttackType () == null) ? null : db.findRangedAttackType (mu.getUnitDefinition ().getRangedAttackType (), "expandUnitDetails");

		final ExpandedUnitDetailsImpl xu = new ExpandedUnitDetailsImpl (unit, mu.getUnitDefinition (), mu.getUnitType (), mu.getOwningPlayer (), magicRealmLifeformType,
			weaponGrade, rangedAttackType, mu.getBasicExperienceLevel (), mu.getModifiedExperienceLevel (), controllingPlayerID,
			basicSkillValues, modifiedSkillValues, basicUpkeepValues, modifiedUpkeepValues, getUnitUtils ());
		return xu;
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
}
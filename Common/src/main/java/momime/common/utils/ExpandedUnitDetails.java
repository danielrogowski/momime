package momime.common.utils;

import java.util.Set;

import momime.common.MomException;
import momime.common.database.DamageType;
import momime.common.database.Pick;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.WeaponGrade;

/**
 * Stores all derived skill, upkeep and other values for a particular unit and stores them for easy and quick lookup.  
 * Build this object by calling getUnitUtils ().expandUnitDetails (), or unit tests can just mock the interface.
 */
public interface ExpandedUnitDetails extends MinimalUnitDetails
{
	/**
	 * @return True magic realm/lifeform type of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead)
	 */
	public Pick getModifiedUnitMagicRealmLifeformType ();

	/**
	 * @return Weapon grade this unit has, or null for summoned units and heroes
	 */
	public WeaponGrade getWeaponGrade ();
	
	/**
	 * @return Ranged attack type this unit has, or null if it has none
	 */
	public RangedAttackTypeEx getRangedAttackType ();
	
	/**
	 * @param unitSkillID Unit skill ID to check
	 * @return Whether or not the unit has this skill, after negations
	 */
	public boolean hasModifiedSkill (final String unitSkillID);
	
	/**
	 * This totals across all the breakdown components.  This is the only value the server is ever interested in.
	 * 
	 * @param unitSkillID Unit skill ID to check
	 * @return Modified value of this skill, or null for valueless skills such as movement skills
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasModifiedSkill (); also if it has any null components
	 */
	public Integer getModifiedSkillValue (final String unitSkillID) throws MomException;

	/**
	 * Filters only specific breakdown components, for displaying attributes in the unit info panel where we want to colour the skill icons
	 * differently according to their component, and shading out negated skill points.
	 * 
	 * @param unitSkillID Unit skill ID to check
	 * @param component Which component(s) to include in the total
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @return Portion of the modified value of this skill that matches the requested filters; 0 for valued skills where no filters matched; null for valueless skills such as movement skills
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasModifiedSkill (); also if it has any null components
	 */
	public Integer filterModifiedSkillValue (final String unitSkillID, final UnitSkillComponent component, final UnitSkillPositiveNegative positiveNegative) throws MomException;
	
	/**
	 * @return Set of all modified skills this unit has
	 */
	public Set<String> listModifiedSkillIDs ();
	
	/**
	 * @param productionTypeID Production type we want to look up the base upkeep for
	 * @return Base upkeep value, before any reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 */
	public int getBasicUpkeepValue (final String productionTypeID);

	/**
	 * @return Set of all basic upkeeps this unit has
	 */
	public Set<String> listBasicUpkeepProductionTypeIDs ();
	
	/**
	 * @param productionTypeID Production type we want to look up the modified upkeep for
	 * @return Upkeep value, modified by reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 */
	public int getModifiedUpkeepValue (final String productionTypeID);

	/**
	 * @return Set of all modified upkeeps this unit has
	 */
	public Set<String> listModifiedUpkeepProductionTypeIDs ();
	
	/**
	 * @return Total damage taken by this unit across all types
	 * @throws MomException Won't happen, since we return 0 for AvailableUnits 
	 */
	public int getTotalDamageTaken () throws MomException;
	
	/**
	 * @return How many hit points the unit as a whole has left
	 * @throws MomException If we hit any problems reading unit skill values
	 */
	public int calculateHitPointsRemaining () throws MomException;
	
	/**
	 * First figure will take full damage before the second figure takes any damage
	 * 
	 * @return Number of figures left alive in this unit
	 * @throws MomException If we hit any problems reading unit skill values
	 */
	public int calculateAliveFigureCount () throws MomException;
	
	/**
	 * @return How many hit points the first figure in this unit has left
	 * @throws MomException If we hit any problems reading unit skill values
	 */
	public int calculateHitPointsRemainingOfFirstFigure () throws MomException;
	
	/**
	 * @param damageType Type of damage they are being hit by
	 * @return Whether or not the unit is completely immune to this type of damage - so getting a boost to e.g. 50 shields still returns false
	 */
	public boolean isUnitImmuneToDamageType (final DamageType damageType);

	/**
	 * @return How much ranged ammo this unit has when fully loaded
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill (); also if it has any null components
	 */
	public int calculateFullRangedAttackAmmo () throws MomException;

	/**
	 * @return How much mana the unit has total, before any is spent in combat
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill (); also if it has any null components
	 */
	public int calculateManaTotal () throws MomException;
}
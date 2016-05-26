package momime.common.utils;

import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.ExperienceLevel;
import momime.common.database.RangedAttackType;
import momime.common.database.Unit;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;

/**
 * Stores all derived skill, upkeep and other values for a particular unit and stores them for easy and quick lookup.  
 * Build this object by calling getUnitUtils ().expandUnitDetails (), or unit tests can just mock the interface.
 */
public interface ExpandedUnitDetails
{
	/**
	 * @return The unit whose details we are storing
	 */
	public AvailableUnit getUnit ();

	/**
	 * @return Whether this is a MemoryUnit or not 
	 */
	public boolean isMemoryUnit ();
	
	/**
	 * @return The unit whose details we are storing if it is a MemoryUnit; null if it is an AvailableUnit 
	 */
	public MemoryUnit getMemoryUnit ();
	
	/**
	 * @return Definition for this unit from the XML database
	 */
	public Unit getUnitDefinition ();
	
	/**
	 * @return Unit type (normal, hero or summoned)
	 */
	public UnitType getUnitType ();

	/**
	 * @return Details about the player who owns the unit
	 */
	public PlayerPublicDetails getOwningPlayer ();
	
	/**
	 * @return True magic realm/lifeform type ID of this unit, taking into account skills/spells that may modify the value (e.g. Chaos Channels, Undead)
	 */
	public String getModifiedUnitMagicRealmLifeformTypeID ();

	/**
	 * @return Weapon grade this unit has, or null for summoned units and heroes
	 */
	public WeaponGrade getWeaponGrade ();
	
	/**
	 * @return Ranged attack type this unit has, or null if it has none
	 */
	public RangedAttackType getRangedAttackType ();
	
	/**
	 * @return Experience level of this unit (0-5 for regular units, 0-8 for heroes) excluding bonuses from Warlord/Crusade; for units that don't gain experience (e.g. summoned), returns null
	 */
	public ExperienceLevel getBasicExperienceLevel ();

	/**
	 * @return Experience level of this unit (0-5 for regular units, 0-8 for heroes) including bonuses from Warlord/Crusade; for units that don't gain experience (e.g. summoned), returns null
	 */
	public ExperienceLevel getModifiedExperienceLevel ();
	
	/**
	 * @param unitSkillID Unit skill ID to check
	 * @return Whether or not the unit has this skill, prior to negations
	 */
	public boolean hasBasicSkill (final String unitSkillID);

	/**
	 * @param unitSkillID Unit skill ID to check
	 * @return Basic unmodified value of this skill, or null for valueless skills such as movement skills
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill ()
	 */
	public Integer getBasicSkillValue (final String unitSkillID) throws MomException;

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
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill (); also if it has any null components
	 */
	public Integer getModifiedSkillValue (final String unitSkillID) throws MomException;
}
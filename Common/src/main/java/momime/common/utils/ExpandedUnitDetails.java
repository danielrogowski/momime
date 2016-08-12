package momime.common.utils;

import java.util.List;
import java.util.Set;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.DamageType;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

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
	 * @return The unit whose details we are storing
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public MemoryUnit getMemoryUnit () throws MomException;
	
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
	 * @return Whether or not the unit is a hero
	 */
	public boolean isHero ();
	
	/**
	 * @return Whether or not the unit is a summoned creature
	 */
	public boolean isSummoned ();
	
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
	 * @return Set of all basic skills this unit has
	 */
	public Set<String> listBasicSkillIDs ();
	
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
	 * @return Number of figures in this unit before it takes any damage
	 */
	public int getFullFigureCount ();

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
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit has a skill with the "ignoreCombatTerrain" flag
	 * @throws RecordNotFoundException If one of the unit skills is not found in the database
	 */
	public boolean unitIgnoresCombatTerrain (final CommonDatabase db) throws RecordNotFoundException;

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

	/**
	 * @return String identifiying this unit, suitable for including in debug messages
	 */
	public String getDebugIdentifier ();
	
	// Properties that directly delegate to methods on AvailableUnit
	
	/**
	 * @return Unit definition identifier, e.g. UN001
	 */
	public String getUnitID ();

	/**
	 * @return PlayerID of the player who owns this unit 
	 */
	public int getOwningPlayerID ();
	
	/**
	 * @return Location of the unit on the overland map
	 */
	public MapCoordinates3DEx getUnitLocation ();
	
	// Properties that directly delegate to methods on MemoryUnit
	
	/**
	 * @return Unit URN
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public int getUnitURN () throws MomException;
	
	/**
	 * @return Current status of this unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public UnitStatusID getStatus () throws MomException;
	
	/**
	 * @return Location on the overland map of the combat this unit is involved in; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public MapCoordinates3DEx getCombatLocation () throws MomException;

	/**
	 * @return Location within the combat map where this unit is standing; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public MapCoordinates2DEx getCombatPosition () throws MomException;

	/**
	 * @param coords Location within the combat map where this unit is standing; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setCombatPosition (final MapCoordinates2DEx coords) throws MomException;
	
	/**
	 * @return Direction within the combat map that the unit is facing; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public Integer getCombatHeading () throws MomException;

	/**
	 * @return Whether the unit is part of the attacking or defending side in combat; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public UnitCombatSideID getCombatSide () throws MomException;
	
	/**
	 * @return The number of moves remaining for this unit this combat turn; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public Integer getDoubleCombatMovesLeft () throws MomException;

	/**
	 * @param moves The number of moves remaining for this unit this combat turn; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setDoubleCombatMovesLeft (final Integer moves) throws MomException;
	
	/**
	 * @return The number of moves remaining for this unit this overland turn
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public int getDoubleOverlandMovesLeft () throws MomException;

	/**
	 * @param moves The number of moves remaining for this unit this overland turn
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setDoubleOverlandMovesLeft (final int moves) throws MomException;
	
	/**
	 * @return The number of ranged shots this unit can still fire in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public int getAmmoRemaining () throws MomException;

	/**
	 * @return The amount of MP this unit can still spend on casting spells in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public int getManaRemaining () throws MomException;

	/**
	 * @return Any special order this unit is currently on, or null if none
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public UnitSpecialOrder getSpecialOrder () throws MomException;
	
	/**
	 * @param o Any special order this unit is currently on, or null if none
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setSpecialOrder (final UnitSpecialOrder o) throws MomException;
	
	/**
	 * @return List of damage this unit has taken
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public List<UnitDamage> getUnitDamage () throws MomException;
}
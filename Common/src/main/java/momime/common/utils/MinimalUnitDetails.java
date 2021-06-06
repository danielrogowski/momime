package momime.common.utils;

import java.util.List;
import java.util.Set;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSpecialOrder;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;

/**
 * Stores minimal unit details that can be derived quickly without examining the whole unit stack.
 * This is mainly here to calculate experience levels for Leadership skill which must be known prior to calculating full stats for any unit in the stack.
 */
public interface MinimalUnitDetails
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
	public UnitEx getUnitDefinition ();
	
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
	 * @return Basic unmodified value of this skill, or null for valueless skills such as movement skills; if unit is a hero then skill is multiplied up by their level
	 * @throws MomException If we call this on a skill that the unit does not have - must verify that the unit has the skill first by calling hasBasicSkill ()
	 */
	public Integer getBasicOrHeroSkillValue (final String unitSkillID) throws MomException;
	
	/**
	 * @return Set of all basic skills this unit has
	 */
	public Set<String> listBasicSkillIDs ();
	
	/**
	 * @return Number of figures in this unit before it takes any damage
	 */
	public int getFullFigureCount ();

	/**
	 * @param db Lookup lists built over the XML database
	 * @return True if the unit has a skill with the "ignoreCombatTerrain" flag
	 * @throws RecordNotFoundException If one of the unit skills is not found in the database
	 */
	public boolean unitIgnoresCombatTerrain (final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @return How much fame a player loses when this unit dies
	 */
	public int calculateFameLostForUnitDying ();
	
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
	 * @param coords Location on the overland map of the combat this unit is involved in; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setCombatLocation (final MapCoordinates3DEx coords) throws MomException;

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
	 * @param heading Direction within the combat map that the unit is facing; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setCombatHeading (final Integer heading) throws MomException;
	
	/**
	 * @return Whether the unit is part of the attacking or defending side in combat; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public UnitCombatSideID getCombatSide () throws MomException;

	/**
	 * @param side Whether the unit is part of the attacking or defending side in combat; null if the unit isn't currently in combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setCombatSide (final UnitCombatSideID side) throws MomException;
	
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
	 * @param ammo The number of ranged shots this unit can still fire in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setAmmoRemaining (final int ammo) throws MomException;
	
	/**
	 * @return The amount of MP this unit can still spend on casting spells in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public int getManaRemaining () throws MomException;

	/**
	 * @param mana The amount of MP this unit can still spend on casting spells in the current combat
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public void setManaRemaining (final int mana) throws MomException;
	
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
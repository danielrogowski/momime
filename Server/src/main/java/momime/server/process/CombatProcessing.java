package momime.server.process;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.MoveUnitInCombatReason;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.movement.CombatMovementType;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;

/**
 * Routines dealing with initiating and progressing combats, as well as moving and attacking
 */
public interface CombatProcessing
{
	/**
	 * Sets units into combat (e.g. sets their combatLocation to put them into combat, and sets their position, heading and side within that combat)
	 * and adds the info to the StartCombatMessage to inform the the two clients involved to do the same.
	 * One call does units on one side (attacking or defending) of the combat.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param startCombatMessage Message being built up ready to send to human participants; if combat is between two AI players can pass this in as null
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param currentLocation The overland map location where the units being put into combat are standing - so attackers are 1 tile away from the actual combatLocation
	 * @param startX X coordinate within the combat map to centre the units around
	 * @param startY Y coordinate within the combat map to centre the units around
	 * @param maxRows Number of rows the units need to be arranged into in order to fit
	 * @param unitHeading Heading the units should be set to face
	 * @param combatSide Side the units should be set to be on
	 * @param onlyUnitURNs If null, all units at currentLocation will be put into combat; if non-null, only the listed Unit URNs will be put into combat
	 * 	This is because the attacker may elect to not attack with every single unit in their stack
	 * @param combatMap Combat scenery we are placing the units onto (important because some tiles will be impassable to some types of unit)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Details about units that were positioned
	 * @throws MomException If there is a logic failure, e.g. not enough space to fit all the units
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns one of the units
	 */
	public PositionCombatUnitsSummary positionCombatUnits (final MapCoordinates3DEx combatLocation, final StartCombatMessage startCombatMessage,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final CoordinateSystem combatMapCoordinateSystem,
		final MapCoordinates3DEx currentLocation, final int startX, final int startY, final int maxRows, final int unitHeading,
		final UnitCombatSideID combatSide, final List<Integer> onlyUnitURNs, final MapAreaOfCombatTiles combatMap, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException;
	
	/**
	 * Progresses the combat happening at the specified location.
	 * Will cycle through playing out AI players' combat turns until either the combat ends or it is a human players turn, at which
	 * point it will send a message to them to tell them to take their turn and then exit.
	 * 
	 * @param combatLocation Where the combat is taking place
	 * @param initialFirstTurn True if this is the initial call from startCombat; false if it is being called by a human playing ending their combat turn or turning auto on
	 * @param initialAutoControlHumanPlayer True if it is being called by a human player turning auto on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void progressCombat (final MapCoordinates3DEx combatLocation, final boolean initialFirstTurn,
		final boolean initialAutoControlHumanPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException;

	/**
	 * Searches for units that died in the specified combat, but their side won, and they can regenerate.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param winningPlayer The player who won the combat
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return The number of dead units that were brought back to life
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public int regenerateUnits (final MapCoordinates3DEx combatLocation, final PlayerServerDetails winningPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Searches for units that died in the specified combat mainly to Life Stealing damage owned by the losing player,
	 * and converts them into undead owned by the winning player.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param newLocation The location the undead should be moved to on the overland map
	 * @param winningPlayer The player who won the combat
	 * @param losingPlayer The player who lost the combat
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return The true units that were converted into undead
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public List<MemoryUnit> createUndead (final MapCoordinates3DEx combatLocation, final MapCoordinates3DEx newLocation,
		final PlayerServerDetails winningPlayer, final PlayerServerDetails losingPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Searches for normal units that died in the specified combat where the winning player has Zombie Mastery cast,
	 * and converts them into zombies ownd by the winning player.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param newLocation The location the undead should be moved to on the overland map
	 * @param winningPlayer The player who won the combat
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return The true units that were converted into undead
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public List<MemoryUnit> createZombies (final MapCoordinates3DEx combatLocation, final MapCoordinates3DEx newLocation,
		final PlayerServerDetails winningPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Because of createUndead above creating units from life stealing attacks, its possible that after combat we can end up with more than 9 units
	 * in a map cell and need to go back and kill off some of the undead to get us back within the maximum.  Its too complicated to work out up front
	 * that there will end up being too many units in a cell, its easier to just allow them to be converted to undead and kill them off later like this.
	 * 
	 * @param unitLocation Location where the units are; if attackers won a combat then they will already have been advanced to the combat location after winning
	 * @param unitsToRemove The units we can potentially kill off (this is the list returned from createUndead above)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void killUnitsIfTooManyInMapCell (final MapCoordinates3DEx unitLocation, final List<MemoryUnit> unitsToRemove, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;
	
	/**
	 * Regular units who die in combat are only set to status=DEAD - they are not actually freed immediately in case someone wants to cast Animate Dead on them.
	 * After a combat ends, this routine properly frees them both on the server and all clients.
	 * 
	 * Heroes who die in combat are set to musDead on the server and the client who owns them - they're freed immediately on the other clients.
	 * This means we don't have to touch dead heroes here, just leave them as they are.
	 * 
	 * It also removes combat summons, e.g. Phantom Warriors, even if they are not dead.
	 * 
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void purgeDeadUnitsAndCombatSummonsFromCombat (final MapCoordinates3DEx combatLocation,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;
	
	/**
	 * Units are put into combat initially as part of the big StartCombat message rather than here.
	 * This routine is used a) for units added after a combat starts (e.g. summoning fire elementals or phantom warriors) and
	 * b) taking units out of combat when it ends.
	 * 
	 * Logic for taking units out of combat at the end very much mirrors what's in purgeDeadUnitsAndCombatSummonsFromCombat, e.g. don't
	 * try to take a monster in a lair out of combat in player's memory on server or client because purgeDeadUnitsAndCombatSummonsFromCombat will
	 * already have killed it off.
	 * 
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param trueUnit The true unit being put into or taken out of combat
	 * @param terrainLocation The location the combat is taking place
	 * @param combatLocation For putting unit into combat, is the location the combat is taking place (i.e. = terrainLocation), for taking unit out of combat will be null
	 * @param combatPosition For putting unit into combat, is the starting position the unit is standing in on the battlefield, for taking unit out of combat will be null
	 * @param combatHeading For putting unit into combat, is the direction the the unit is heading on the battlefield, for taking unit out of combat will be null
	 * @param combatSide For putting unit into combat, specifies which side they're on, for taking unit out of combat will be null
	 * @param summonedBySpellID For summoning new units directly into combat (e.g. fire elementals) gives the spellID they were summoned with; otherwise null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 */
	public void setUnitIntoOrTakeUnitOutOfCombat (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MemoryUnit trueUnit, final MapCoordinates3DEx terrainLocation, final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final Integer combatHeading, final UnitCombatSideID combatSide, final String summonedBySpellID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException;
	
	/**
	 * At the end of a combat, takes all units out of combat by nulling out all their combat values
	 * 
	 * @param attackingPlayer Player who is attacking
	 * @param defendingPlayer Player who is defending - may be null if taking an empty lair, or a "walk in without a fight" in simultaneous turns games
	 * @param combatLocation The location the combat took place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 */
	public void removeUnitsFromCombat (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException;
	
	/**
	 * Once we have mapped out the directions and distances around the combat map and verified that our desired destination is
	 * fine, this routine actually handles sending the movement animations, performing the updates, and resolving any attacks.
	 *  
	 * This is separate so that it can be called directly by the AI.
	 *  
	 * @param tu The unit being moved
	 * @param moveTo The position within the combat map that the unit wants to move to (or attack)
	 * @param reason What caused the movement
	 * @param movementDirections The map of movement directions generated by calculateCombatMovementDistances
	 * @param movementTypes The map of movement types generated by calculateCombatMovementDistances
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the attack resulted in the combat ending
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean okToMoveUnitInCombat (final ExpandedUnitDetails tu, final MapCoordinates2DEx moveTo, final MoveUnitInCombatReason reason,
		final int [] [] movementDirections, final CombatMovementType [] [] movementTypes, final MomSessionVariables mom)
		throws MomException, PlayerNotFoundException, RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * Rechecks that transports have sufficient space to hold all units for whom the terrain is impassable.
	 * This is used after naval combats where some of the transports may have died, to kill off any surviving units who now have no transport,
	 * or perhaps a unit had Flight cast on it which was dispelled during combat.
	 * 
	 * @param combatLocation The combatLocation where the units need to be rechecked
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void recheckTransportCapacityAfterCombat (final MapCoordinates3DEx combatLocation, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;
}
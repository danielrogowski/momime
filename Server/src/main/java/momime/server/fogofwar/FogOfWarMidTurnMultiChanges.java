package momime.server.fogofwar;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.PendingMovement;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.MomSessionVariables;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.process.OneCellPendingMovement;

/**
 * This contains methods for updating multiple mid turn changes at once, e.g. remove all spells in a location.
 * Movement methods are also here, since movement paths are calculated by repeatedly calling the other methods.
 * Separating this from the single changes mean the single changes can be mocked out in the unit tests for the multi change methods.
 */
public interface FogOfWarMidTurnMultiChanges
{
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param combatLocation Location of combat that just ended
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void switchOffMaintainedSpellsCastInCombatLocation_OnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx combatLocation, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param combatLocation Location of combat that just ended
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx combatLocation, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location to turn spells off from
	 * @param castingPlayerID Which player's spells to turn off; 0 = everybodys 
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void switchOffMaintainedSpellsInLocationOnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx cityLocation, final int castingPlayerID,
		final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param mapLocation Location the combat is taking place
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void removeCombatAreaEffectsFromLocalisedSpells (final FogOfWarMemory trueMap, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final CommonDatabase db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location of the city to remove the building from
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void destroyAllBuildingsInLocationOnServerAndClients (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx cityLocation,
		final MomSessionDescription sd, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;
	
	/**
	 * @param trueUnits True list of units to heal/gain experience
	 * @param onlyOnePlayerID If zero, will heal/exp units belonging to all players; if specified will heal/exp only units belonging to the specified player
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @param overlandMapCoordinateSystem Overland map coordinate system
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public void healUnitsAndGainExperience (final List<MemoryUnit> trueUnits, final int onlyOnePlayerID, final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings, final CoordinateSystem overlandMapCoordinateSystem)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * When a unit dies in combat, all the units on the opposing side gain 1 exp. 
	 * 
	 * @param combatLocation The location where the combat is taking place
	 * @param combatSide Which side is to gain 1 exp
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public void grantExperienceToUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Moves a unit stack from one location to another; the two locations are assumed to be adjacent map cells.
	 * It deals with all the resulting knock on effects, namely:
	 * 1) Checking if the units come into view for any players, if so adds the units into the player's memory and sends them to the client
	 * 2) Checking if the units go out of sight for any players, if so removes the units from the player's memory and removes them from the client
	 * 3) Checking what the units can see from their new location
	 * 4) Updating any cities the units are moving out of or into - normal units calm rebels in cities, so by moving the number of rebels may change
	 *
	 * @param unitStack The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param moveFrom Location to move from
	 *
	 * @param moveTo Location to move to
	 * 		moveTo.getPlane () needs some special discussion.  The calling routine must have set moveTo.getPlane () correctly, i.e. so if we're on Myrror
	 *			moving onto a tower, moveTo.getPlane () = 0 - you can't just assume moveTo.getPlane () = moveFrom.getPlane ().
	 *			Also moveTo.getPlane () cannot be calculated simply from checking if the map cell at moveTo is a tower - we might be
	 *			in a tower (on plane 0) moving to a map cell on Myrror - in this case the only way to know the correct value
	 *			of moveTo.getPlane () is by what map cell the player clicked on in the UI.
	 *
	 * @param players List of players in the session
	 * @param gsk Server knowledge structure
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void moveUnitStackOneCellOnServerAndClients (final List<MemoryUnit> unitStack, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo, final List<PlayerServerDetails> players,
		final MomGeneralServerKnowledge gsk, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException;

	/**
	 * Client has requested that we try move a stack of their units to a certain location - that location may be on the other
	 * end of the map, and we may not have seen it or the intervening terrain yet, so we basically move one tile at a time
	 * and re-evaluate *everthing* based on the knowledge we learn of the terrain from our new location before we make the next move
	 *
	 * @param selectedUnits The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param processCombats If true will allow combats to be started; if false any move that would initiate a combat will be cancelled and ignored.
	 *		This is used when executing pending movements at the start of one-player-at-a-time game turns.
	 * @param originalMoveFrom Location to move from
	 *
	 * @param moveTo Location to move to
	 * 		Note about moveTo.getPlane () - the same comment as moveUnitStackOneCellOnServerAndClients *doesn't apply*, moveTo.getPlane ()
	 *			will be whatever the player clicked on - if they click on a tower on Myrror, moveTo.getPlane () will be set to 1; the routine
	 *			sorts the correct destination plane out for each cell that the unit stack moves
	 *
	 * @param forceAsPendingMovement If true, forces all generated moves to be added as pending movements rather than occurring immediately (used for simultaneous turns games)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the move resulted in a combat being started in a one-player-at-a-time game (and thus the player's turn should halt while the combat is played out)
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public boolean moveUnitStack (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner, final boolean processCombats,
		final MapCoordinates3DEx originalMoveFrom, final MapCoordinates3DEx moveTo,
		final boolean forceAsPendingMovement, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException;
	
	/**
	 * This follows the same logic as moveUnitStack, except that it only works out what the first cell of movement will be,
	 * and doesn't actually perform the movement.
	 * 
	 * @param selectedUnits The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param pendingMovement The pending move we're determining one step of
	 * @param doubleMovementRemaining The lowest movement remaining of any unit in the stack
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws PlayerNotFoundException If we cannot find the player who a unit in the same location as our transports
	 * @throws MomException If there is a problem with any of the calculations
	 * @return null if the location is unreachable; otherwise object holding the details of the move, the one step we'll take first, and whether it initiates a combat
	 */
	public OneCellPendingMovement determineOneCellPendingMovement (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner,
		final PendingMovement pendingMovement,  final int doubleMovementRemaining, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException;
	
	/**
	 * This follows the same logic as moveUnitStack, except that it only works out what the movement path will be,
	 * and doesn't actually perform the movement.
	 * 
	 * @param selectedUnits The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param moveFrom Location to move from
	 * @param moveTo Location to move to
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If there is a problem with any of the calculations
	 * @return null if the location is unreachable; otherwise object holding the details of the move, the one step we'll take first, and whether it initiates a combat
	 */
	public List<Integer> determineMovementPath (final List<ExpandedUnitDetails> selectedUnits, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException;

	/**
	 * Gives all units full movement back again overland
	 *
	 * @param onlyOnePlayerID If zero, will reset movmenet for units belonging to all players; if specified will reset movement only for units belonging to the specified player
	 * @param players Players list
	 * @param trueMap True terrain, list of units and so on
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void resetUnitOverlandMovement (final int onlyOnePlayerID, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final FogOfWarSetting fogOfWarSettings, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}
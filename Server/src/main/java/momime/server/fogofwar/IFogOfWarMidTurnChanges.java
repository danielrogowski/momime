package momime.server.fogofwar;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.FogOfWarSettingData;
import momime.common.database.newgame.v0_9_4.FogOfWarValue;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.database.ServerDatabaseEx;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * This contains all methods that allow changes in the server's true memory to be replicated into each player's memory and send update messages to each client
 * i.e. methods for when the true values change (or are added or removed) but the visible area that each player can see does not change
 */
public interface IFogOfWarMidTurnChanges
{
	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the terrain that has been updated
	 * @param terrainAndNodeAurasSetting Terrain and Node Auras FOW setting from session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void updatePlayerMemoryOfTerrain (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final OverlandMapCoordinates coords,
		final FogOfWarValue terrainAndNodeAurasSetting, final Logger debugLogger)
		throws JAXBException, XMLStreamException;

	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the city that has been updated
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public void updatePlayerMemoryOfCity (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final OverlandMapCoordinates coords, final FogOfWarSettingData fogOfWarSettings, final Logger debugLogger)
		throws JAXBException, XMLStreamException;

	/**
	 * Adds a unit to the server's true memory, and checks who can see it - sending a message to update the client of any human players who can see it
	 *
	 * Heroes are added using this method during game startup - at which point they're added only on the server and they're off
	 * the map (null location) - so heroes are NEVER sent to the client using this method, meaning that we don't need to worry about sending skill lists with this method
	 *
	 * @param gsk Server knowledge structure to add the unit to
	 * @param unitID Type of unit to create
	 * @param locationToAddUnit Location to add the new unit; can be null for adding heroes that haven't been summoned yet
	 * @param buildingsLocation Location the unit was built - might be different from locationToAddUnit if the city is full and the unit got bumped to an adjacent tile; passed as null for units not built in cities such as summons
	 * @param combatLocation The location of the combat that this unit is being summoned into; null for anything other than combat summons
	 * @param unitOwner Player who will own the new unit
	 * @param initialStatus Initial status of the unit, typically ALIVE
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return Newly created unit
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public MemoryUnit addUnitOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final String unitID, final OverlandMapCoordinates locationToAddUnit, final OverlandMapCoordinates buildingsLocation, final OverlandMapCoordinates combatLocation,
		final PlayerServerDetails unitOwner, final UnitStatusID initialStatus, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;

	/**
	 * When we summon a hero, we don't 'add' it, the unit object already exists - but we still need to perform similar updates
	 * to addUnitOnServerAndClients to set the unit's location, see the area it can see, status and tell clients to update the same
	 *
	 * Although not written yet, this will also be used for bringing dead regular units back to life with the Raise Dead spell
	 *
	 * @param trueUnit The unit to set to alive
	 * @param locationToAddUnit Location to add the new unit, must be filled in
	 * @param unitOwner Player who will own the new unit, note the reason this has to be passed in separately is because the players list is allowed to be null
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void updateUnitStatusToAliveOnServerAndClients (final MemoryUnit trueUnit, final OverlandMapCoordinates locationToAddUnit,
		final PlayerServerDetails unitOwner, final List<PlayerServerDetails> players, final FogOfWarMemory trueMap,
		final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;

	/**
	 * Kills off a unit, on the server and usually also on whichever clients can see the unit
	 * There are a number of different possibilities for how we need to handle this, depending on how the unit died and whether it is a regular/summoned unit or a hero
	 *
	 * @param trueUnit The unit to set to alive
	 * @param action Method by which the unit is being killed
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void killUnitOnServerAndClients (final MemoryUnit trueUnit, final KillUnitActionID action,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException;

	/**
	 * Checks who can see a maintained spell that already exists on the server, adding it into the memory
	 * of anyone who can see it and also sending a message to update the client
	 *
	 * The reason we need this separate from addMaintainedSpellOnServerAndClients is for casting
	 * overland city/unit spells for which we have to pick a target before the casting of the spell is "complete"
	 *
	 * Spells in this "cast-but-not-targetted" state exist on the server but not in player's memory or on clients, so when their target has been set, this method is then called
	 *
	 * @param trueSpell True spell to add
	 * @param players List of players in the session
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void addExistingTrueMaintainedSpellToClients (final MemoryMaintainedSpell trueSpell, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final ServerDatabaseEx db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * @param gsk Server knowledge structure to add the spell to
	 * @param castingPlayerID Player who cast the spell
	 * @param spellID Which spell it is
	 * @param unitURN Indicates which unit the spell is cast on; null for spells not cast on units
	 * @param unitSkillID If a spell cast on a unit, indicates the specific skill that this spell grants the unit
	 * @param castInCombat Whether this spell was cast in combat or not
	 * @param cityLocation Indicates which city the spell is cast on; null for spells not cast on cities
	 * @param citySpellEffectID If a spell cast on a city, indicates the specific effect that this spell grants the city
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void addMaintainedSpellOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final OverlandMapCoordinates cityLocation, final String citySpellEffectID, final List<PlayerServerDetails> players,
		final ServerDatabaseEx db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param castingPlayerID Player who cast the spell
	 * @param spellID Which spell it is
	 * @param unitURN Indicates which unit the spell is cast on; null for spells not cast on units
	 * @param unitSkillID If a spell cast on a unit, indicates the specific skill that this spell grants the unit
	 * @param castInCombat Whether this spell was cast in combat or not
	 * @param cityLocation Indicates which city the spell is cast on; null for spells not cast on cities
	 * @param citySpellEffectID If a spell cast on a city, indicates the specific effect that this spell grants the city
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void switchOffMaintainedSpellOnServerAndClients (final FogOfWarMemory trueMap,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final OverlandMapCoordinates cityLocation, final String citySpellEffectID, final List<PlayerServerDetails> players,
		final ServerDatabaseEx db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * @param gsk Server knowledge structure to add the CAE to
	 * @param combatAreaEffectID Which CAE is it
	 * @param castingPlayerID Player who cast the CAE if it was created via a spell; null for natural CAEs (like node auras)
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void addCombatAreaEffectOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final String combatAreaEffectID, final Integer castingPlayerID, final OverlandMapCoordinates mapLocation,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param combatAreaEffectID Which CAE is it
	 * @param castingPlayerID Player who cast the CAE if it was created via a spell; null for natural CAEs (like node auras)
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void removeCombatAreaEffectFromServerAndClients (final FogOfWarMemory trueMap,
		final String combatAreaEffectID, final Integer castingPlayerID, final OverlandMapCoordinates mapLocation,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException;

	/**
	 * @param gsk Server knowledge structure to add the building(s) to
	 * @param players List of players in the session
	 * @param cityLocation Location of the city to add the building(s) to
	 * @param firstBuildingID First building ID to create, or can be null
	 * @param secondBuildingID Second building ID to create; this is usually null, it is mainly here for casting Move Fortress, which creates both a Fortress + Summoning circle at the same time
	 * @param buildingCreatedFromSpellID The spell that resulted in the creation of this building (e.g. casting Wall of Stone creates City Walls); null if building was constructed in the normal way
	 * @param buildingCreationSpellCastByPlayerID The player who cast the spell that resulted in the creation of this building; null if building was constructed in the normal way
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void addBuildingOnServerAndClients (final MomGeneralServerKnowledge gsk, final List<PlayerServerDetails> players,
		final OverlandMapCoordinates cityLocation, final String firstBuildingID, final String secondBuildingID,
		final String buildingCreatedFromSpellID, final Integer buildingCreationSpellCastByPlayerID,
		final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location of the city to remove the building from
	 * @param buildingID Which building to remove
	 * @param updateBuildingSoldThisTurn If true, tells client to update the buildingSoldThisTurn flag, which will prevents this city from selling a 2nd building this turn
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void destroyBuildingOnServerAndClients (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final OverlandMapCoordinates cityLocation, final String buildingID, final boolean updateBuildingSoldThisTurn,
		final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException;

	/**
	 * @param trueUnits True list of units to heal/gain experience
	 * @param onlyOnePlayerID If zero, will heal/exp units belonging to all players; if specified will heal/exp only units belonging to the specified player
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public void healUnitsAndGainExperience (final List<MemoryUnit> trueUnits, final int onlyOnePlayerID, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd, final Logger debugLogger)
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
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void moveUnitStackOneCellOnServerAndClients (final List<MemoryUnit> unitStack, final PlayerServerDetails unitStackOwner,
		final OverlandMapCoordinates moveFrom, final OverlandMapCoordinates moveTo, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException;

	/**
	 * Client has requested that we try move a stack of their units to a certain location - that location may be on the other
	 * end of the map, and we may not have seen it or the intervening terrain yet, so we basically move one tile at a time
	 * and re-evaluate *everthing* based on the knowledge we learn of the terrain from our new location before we make the next move
	 *
	 * @param unitStack The units we want to move (true unit versions)
	 * @param unitStackOwner The player who owns the units
	 * @param originalMoveFrom Location to move from
	 *
	 * @param moveTo Location to move to
	 * 		Note about moveTo.getPlane () - the same comment as moveUnitStackOneCellOnServerAndClients *doesn't apply*, moveTo.getPlane ()
	 *			will be whatever the player clicked on - if they click on a tower on Myrror, moveTo.getPlane () will be set to 1; the routine
	 *			sorts the correct destination plane out for each cell that the unit stack moves
	 *
	 * @param forceAsPendingMovement If true, forces all generated moves to be added as pending movements rather than occurring immediately (used for simultaneous turns games)
	 * @param players List of players in the session
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void moveUnitStack (final List<MemoryUnit> unitStack, final PlayerServerDetails unitStackOwner,
		final OverlandMapCoordinates originalMoveFrom, final OverlandMapCoordinates moveTo,
		final boolean forceAsPendingMovement, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException;
}

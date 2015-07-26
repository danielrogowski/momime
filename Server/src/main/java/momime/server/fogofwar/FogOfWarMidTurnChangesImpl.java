package momime.server.fogofwar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.UntransmittedKillUnitActionID;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeID;
import momime.common.database.FogOfWarSetting;
import momime.common.database.FogOfWarValue;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddBuildingMessage;
import momime.common.messages.servertoclient.AddCombatAreaEffectMessage;
import momime.common.messages.servertoclient.AddMaintainedSpellMessage;
import momime.common.messages.servertoclient.AddUnitMessage;
import momime.common.messages.servertoclient.ApplyDamageMessage;
import momime.common.messages.servertoclient.ApplyDamageMessageUnit;
import momime.common.messages.servertoclient.CancelCombatAreaEffectMessage;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.messages.servertoclient.KillUnitActionID;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.UpdateCityMessage;
import momime.common.messages.servertoclient.UpdateCityMessageData;
import momime.common.messages.servertoclient.UpdateDamageTakenAndExperienceMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessageData;
import momime.common.messages.servertoclient.UpdateUnitNameMessage;
import momime.common.messages.servertoclient.UpdateUnitToAliveMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.calculations.FogOfWarCalculations;
import momime.server.database.CitySpellEffectSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.utils.UnitServerUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * This contains all methods that allow changes in the server's true memory to be replicated into each player's memory and send update messages to each client
 * i.e. methods for when the true values change (or are added or removed) but the visible area that each player can see does not change
 */
public final class FogOfWarMidTurnChangesImpl implements FogOfWarMidTurnChanges
{
	/** Class logger */
	private final Log log = LogFactory.getLog (FogOfWarMidTurnChangesImpl.class);
	
	/** Single cell FOW calculations */
	private FogOfWarCalculations fogOfWarCalculations;
	
	/** FOW visibility checks */
	private FogOfWarMidTurnVisibility fogOfWarMidTurnVisibility;

	/** FOW duplication utils */
	private FogOfWarDuplication fogOfWarDuplication;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the terrain that has been updated
	 * @param terrainAndNodeAurasSetting Terrain and Node Auras FOW setting from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void updatePlayerMemoryOfTerrain (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx coords,
		final FogOfWarValue terrainAndNodeAurasSetting)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering updatePlayerMemoryOfTerrain: " + coords);

		final MemoryGridCell tc = trueTerrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

		// First build the message
		final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
		terrainMsg.setMapLocation (coords);
		terrainMsg.setTerrainData (tc.getTerrainData ());

		final UpdateTerrainMessage terrainMsgContainer = new UpdateTerrainMessage ();
		terrainMsgContainer.setData (terrainMsg);

		// Check which players can see the terrain
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

			if (getFogOfWarCalculations ().canSeeMidTurn (state, terrainAndNodeAurasSetting))
			{
				// Update player's memory on server
				final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
				if (getFogOfWarDuplication ().copyTerrainAndNodeAura (tc, mc))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
						thisPlayer.getConnection ().sendMessageToClient (terrainMsgContainer);
			}
		}

		log.trace ("Exiting updatePlayerMemoryOfTerrain");
	}

	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the city that has been updated
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @param newlyAddedCity Whether the reason we're seeing this city is because it has just been built (used so that client asks the player who built it to name the city)
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void updatePlayerMemoryOfCity (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx coords, final FogOfWarSetting fogOfWarSettings, final boolean newlyAddedCity)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering updatePlayerMemoryOfCity: " + coords);

		final MemoryGridCell tc = trueTerrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

		// First build the message
		final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
		cityMsg.setMapLocation (coords);

		final UpdateCityMessage cityMsgContainer = new UpdateCityMessage ();
		cityMsgContainer.setData (cityMsg);

		// Check which players can see the city
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

			if (getFogOfWarCalculations ().canSeeMidTurn (state, fogOfWarSettings.getCitiesSpellsAndCombatAreaEffects ()))
			{
				// Update player's memory on server
				final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

				final boolean includeCurrentlyConstructing;
				final boolean includeProductionSoFar;
				if (tc.getCityData () == null)
				{
					includeCurrentlyConstructing = false;
					includeProductionSoFar = false;
				}
				else
				{
					includeCurrentlyConstructing = (thisPlayer.getPlayerDescription ().getPlayerID () == tc.getCityData ().getCityOwnerID ()) ||
						(fogOfWarSettings.isSeeEnemyCityConstruction ());
					
					includeProductionSoFar = thisPlayer.getPlayerDescription ().getPlayerID () == tc.getCityData ().getCityOwnerID ();
				}

				if (getFogOfWarDuplication ().copyCityData (tc, mc, includeCurrentlyConstructing, includeProductionSoFar))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
					{
						// Note unlike the terrain msg which we can build once and send to each applicable player, the data for the city msg
						// needs to be reset for each player, since their visibility of the currentlyConstructing value may be different
						cityMsg.setCityData (mc.getCityData ());
						cityMsg.setAskForCityName (newlyAddedCity && (thisPlayer.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ())));
						thisPlayer.getConnection ().sendMessageToClient (cityMsgContainer);
					}
			}
		}

		log.trace ("Exiting updatePlayerMemoryOfCity");
	}

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
	 * @return Newly created unit
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final MemoryUnit addUnitOnServerAndClients (final MomGeneralServerKnowledgeEx gsk,
		final String unitID, final MapCoordinates3DEx locationToAddUnit, final MapCoordinates3DEx buildingsLocation, final MapCoordinates3DEx combatLocation,
		final PlayerServerDetails unitOwner, final UnitStatusID initialStatus, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.trace ("Entering addUnitOnServerAndClients: Player ID " + unitOwner.getPlayerDescription ().getPlayerID () + ", " + unitID);

		// There's a bunch of other unit statuses that don't make sense to use here - so worth checking this
		if ((initialStatus != UnitStatusID.NOT_GENERATED) && (initialStatus != UnitStatusID.ALIVE))
			throw new MomException ("addUnitOnServerAndClients: Invalid initial status of " + initialStatus);

		final MomPersistentPlayerPublicKnowledge unitOwnerPPK = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();

		// Check how much experience this unit should have
		// Note the reason we pass in buildingsLocation separately from locationToAddUnit is in case a city is full and a unit gets bumped
		// to the outside - it still needs to get the bonus from the buildings back in the city
		final int startingExperience;
		final Integer weaponGrade;
		if (buildingsLocation != null)
		{
			startingExperience = getMemoryBuildingUtils ().experienceFromBuildings (gsk.getTrueMap ().getBuilding (), buildingsLocation, db);
			weaponGrade = getUnitCalculations ().calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
				(gsk.getTrueMap ().getBuilding (), gsk.getTrueMap ().getMap (), buildingsLocation, unitOwnerPPK.getPick (), sd.getOverlandMapSize (), db);
		}
		else
		{
			startingExperience = 0;
			weaponGrade = null;
		}

		// Add on server
		// Even for heroes, we load in their default skill list - this is how heroes default skills are loaded during game startup
		final MemoryUnit newUnit = getUnitServerUtils ().createMemoryUnit (unitID, gsk.getNextFreeUnitURN (), weaponGrade, startingExperience, db);
		newUnit.setOwningPlayerID (unitOwner.getPlayerDescription ().getPlayerID ());
		newUnit.setCombatLocation (combatLocation);

		gsk.setNextFreeUnitURN (gsk.getNextFreeUnitURN () + 1);
		gsk.getTrueMap ().getUnit ().add (newUnit);

		if (initialStatus == UnitStatusID.ALIVE)
			updateUnitStatusToAliveOnServerAndClients (newUnit, locationToAddUnit, unitOwner, players, gsk.getTrueMap (), sd, db);

		log.trace ("Exiting addUnitOnServerAndClients = Unit URN " + newUnit.getUnitURN ());
		return newUnit;
	}

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
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void updateUnitStatusToAliveOnServerAndClients (final MemoryUnit trueUnit, final MapCoordinates3DEx locationToAddUnit,
		final PlayerServerDetails unitOwner, final List<PlayerServerDetails> players, final FogOfWarMemory trueMap,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.trace ("Entering updateUnitStatusToAliveOnServerAndClients: Unit URN " + trueUnit.getUnitURN ());

		// Update on server
		final MapCoordinates3DEx unitLocation = new MapCoordinates3DEx (locationToAddUnit);

		trueUnit.setUnitLocation (unitLocation);
		trueUnit.setStatus (UnitStatusID.ALIVE);

		// What can the new unit see? (it may expand the unit owner's vision to see things that they couldn't previously)
		if ((players != null) && (trueUnit.getCombatLocation () == null))
			getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, unitOwner, players, false, "updateUnitStatusToAliveOnServerAndClients", sd, db);

		// Tell clients?
		// Player list can be null, we use this for pre-adding units to the map before the fog of war has even been set up
		if (players != null)
		{
			final AddUnitMessage addMsg = new AddUnitMessage ();
			addMsg.setMemoryUnit (trueUnit);

			final UpdateUnitToAliveMessage updateMsg = new UpdateUnitToAliveMessage ();
			updateMsg.setUnitLocation (unitLocation);
			updateMsg.setUnitURN (trueUnit.getUnitURN ());

			for (final PlayerServerDetails player : players)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (trueUnit, trueMap.getMap (), player, db, sd.getFogOfWarSetting ()))
				{
					// Does the player already have the unit in their memory?
					if (getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit ()) == null)
					{
						// Player doesn't know about unit, so add it
						if (getFogOfWarDuplication ().copyUnit (trueUnit, priv.getFogOfWarMemory ().getUnit ()))
							if (player.getPlayerDescription ().isHuman ())
								player.getConnection ().sendMessageToClient (addMsg);
					}
					else
					{
						// Player already knows about unit, so just update it to alive
						if (getFogOfWarDuplication ().copyUnit (trueUnit, priv.getFogOfWarMemory ().getUnit ()))
							if (player.getPlayerDescription ().isHuman ())
								player.getConnection ().sendMessageToClient (updateMsg);
					}
				}
			}
		}

		log.trace ("Exiting updateUnitStatusToAliveOnServerAndClients");
	}

	/**
	 * Kills off a unit, on the server and usually also on whichever clients can see the unit
	 * There are a number of different possibilities for how we need to handle this, depending on how the unit died and whether it is a regular/summoned unit or a hero
	 *
	 * @param trueUnit The unit to set to alive
	 * @param transmittedAction Method by which the unit is being killed, out of possible values that are sent to clients; null if untransmittedAction is filled in
	 * @param untransmittedAction Method by which the unit is being killed, out of possible values that are not sent to clients; null if transmittedAction is filled in
	 * @param players List of players in this session, this can be passed in null for when units are being added to the map pre-game
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void killUnitOnServerAndClients (final MemoryUnit trueUnit, final KillUnitActionID transmittedAction, final UntransmittedKillUnitActionID untransmittedAction,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final FogOfWarSetting fogOfWarSettings, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.trace ("Entering killUnitOnServerAndClients: Unit URN " + trueUnit.getUnitURN ());

		// Build the message ready to send it to whoever could see the unit
		// Action has to be set per player depending on who can see it
		final KillUnitMessage msg = new KillUnitMessage ();
		msg.setUnitURN (trueUnit.getUnitURN ());

		// Check which players could see the unit
		for (final PlayerServerDetails player : players)
		{
			if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (trueUnit, trueMap.getMap (), player, db, fogOfWarSettings))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
				
				// Remove unit from players' memory on server - this doesn't suffer from the issue described below so we can just do it
				getPendingMovementUtils ().removeUnitFromAnyPendingMoves (trans.getPendingMovement (), trueUnit.getUnitURN ());
				getUnitUtils ().beforeKillingUnit (priv.getFogOfWarMemory (), trueUnit.getUnitURN ());	// Removes spells cast on unit
				
				// If still in combat, only set to DEAD in player's memory on server, rather than removing entirely
				if (untransmittedAction == UntransmittedKillUnitActionID.COMBAT_DAMAGE)
					getUnitUtils ().findUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "killUnitOnServerAndClients").setStatus (UnitStatusID.DEAD);
				else
					getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit ());

				if ((transmittedAction != null) && (player.getPlayerDescription ().isHuman ()))
				{
					// Edit the action appropriately for the client - the only reason this isn't always FREE is that
					// units killed or dismissed by lack of production need to go to a special status on
					// the owning client so they are freed once their name has been recorded in the NTM
				
					// For other players and statuses, they are just killed outright
					if ((trueUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ().intValue ()) &&
						((transmittedAction == KillUnitActionID.HERO_LACK_OF_PRODUCTION) || (transmittedAction == KillUnitActionID.UNIT_LACK_OF_PRODUCTION)))

						msg.setKillUnitActionID (transmittedAction);
					else
						msg.setKillUnitActionID (KillUnitActionID.FREE);
				
					player.getConnection ().sendMessageToClient (msg);
				}
			}
		}

		// Update the true copy of the unit as appropriate
		getUnitUtils ().beforeKillingUnit (trueMap, trueUnit.getUnitURN ());	// Removes spells cast on unit
		
		if (transmittedAction != null)
			switch (transmittedAction)
			{
				// Dismissed heroes go back to Generated
				// Heroes dismissed by lack of production go back to Generated
				case HERO_DIMISSED_VOLUNTARILY:
				case HERO_LACK_OF_PRODUCTION:
					log.debug ("Setting hero with unit URN " + trueUnit.getUnitURN () + " back to generated");
					trueUnit.setStatus (UnitStatusID.GENERATED);
					break;

					// Units killed by lack of production are simply killed off
				case FREE:
				case UNIT_LACK_OF_PRODUCTION:
					log.debug ("Permanently removing unit URN " + trueUnit.getUnitURN ());
					getUnitUtils ().removeUnitURN (trueUnit.getUnitURN (), trueMap.getUnit ());
					break;

					// VISIBLE_AREA_CHANGED is only ever used in msgs sent to clients and should never be passed into this method

				default:
					throw new MomException ("killUnitOnServerAndClients doesn't know what to do with true units when transmittedAction = " + transmittedAction);
			}
		
		if (untransmittedAction != null)
			switch (untransmittedAction)
			{
				// Killed by taking damage in combat.
				// All units killed by combat damage are kept around for the moment, since one of the players in the combat may Raise Dead them.
				// Heroes are kept at musDead even after the combat ends, in case the player resurrects them.
				case COMBAT_DAMAGE:
					log.debug ("Setting unit with unit URN " + trueUnit.getUnitURN () + " to dead");
					trueUnit.setStatus (UnitStatusID.DEAD);
					break;
				
				default:
					throw new MomException ("killUnitOnServerAndClients doesn't know what to do with true units when untransmittedAction = " + untransmittedAction);
			}

		log.trace ("Exiting killUnitOnServerAndClients");
	}

	/**
	 * Checks who can see a maintained spell that already exists on the server, adding it into the memory
	 * of anyone who can see it and also sending a message to update the client
	 *
	 * The reason we need this separate from addMaintainedSpellOnServerAndClients is for casting
	 * overland city/unit spells for which we have to pick a target before the casting of the spell is "complete"
	 *
	 * Spells in this "cast-but-not-targetted" state exist on the server but not in player's memory or on clients, so when their target has been set, this method is then called
	 *
	 * @param gsk Server knowledge structure
	 * @param trueSpell True spell to add
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addExistingTrueMaintainedSpellToClients (final MomGeneralServerKnowledgeEx gsk,
		final MemoryMaintainedSpell trueSpell, final List<PlayerServerDetails> players,
		final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering addExistingTrueMaintainedSpellToClients: Player ID " + trueSpell.getCastingPlayerID () + ", " + trueSpell.getSpellID ());

		// Build the message ready to send it to whoever can see the spell
		final AddMaintainedSpellMessage msg = new AddMaintainedSpellMessage ();
		msg.setMaintainedSpell (trueSpell);
		msg.setNewlyCast (true);		// Spells added via this method must be new, or just being targetted, so either way from the client's point of view they must be newly cast

		// Check which players can see the spell
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, gsk.getTrueMap ().getMap (), gsk.getTrueMap ().getUnit (), player, db, sd.getFogOfWarSetting ()))
			{
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))

					// Update on client
					if (player.getPlayerDescription ().isHuman ())
						player.getConnection ().sendMessageToClient (msg);
			}
		}
		
		// Does the spell generate a CAE? e.g. Heavenly Light and Cloud of Shadow; if so then add it
		if (trueSpell.getCitySpellEffectID () != null)
		{
			final CitySpellEffectSvr citySpellEffect = db.findCitySpellEffect (trueSpell.getCitySpellEffectID (), "addExistingTrueMaintainedSpellToClients");
			if (citySpellEffect.getCombatAreaEffectID () != null)
				addCombatAreaEffectOnServerAndClients (gsk, citySpellEffect.getCombatAreaEffectID (), trueSpell.getSpellID (), trueSpell.getCastingPlayerID (),
					(MapCoordinates3DEx) trueSpell.getCityLocation (), players, db, sd);
		}

		// The new spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
		// While it may seem a bit odd to do this here (since the spell already existed on the server before calling this),
		// the spell would have been in an untargetted state so we wouldn't know what city to apply it to, so this is definitely the right place to do this
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (players, trueSpell.getCastingPlayerID (), "addExistingTrueMaintainedSpellToClients");
		getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), castingPlayer, players, false, "addExistingTrueMaintainedSpellToClients", sd, db);

		log.trace ("Exiting addExistingTrueMaintainedSpellToClients");
	}

	/**
	 * @param gsk Server knowledge structure to add the spell to
	 * @param castingPlayerID Player who cast the spell
	 * @param spellID Which spell it is
	 * @param unitURN Indicates which unit the spell is cast on; null for spells not cast on units
	 * @param unitSkillID If a spell cast on a unit, indicates the specific skill that this spell grants the unit
	 * @param castInCombat Whether this spell was cast in combat or not
	 * @param cityLocation Indicates which city the spell is cast on; null for spells not cast on cities
	 * @param citySpellEffectID If a spell cast on a city, indicates the specific effect that this spell grants the city
	 * @param players List of players in the session, this can be passed in null for when spells that require a target are added initially only on the server
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addMaintainedSpellOnServerAndClients (final MomGeneralServerKnowledgeEx gsk,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final MapCoordinates3DEx cityLocation, final String citySpellEffectID, final List<PlayerServerDetails> players,
		final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering addMaintainedSpellOnServerAndClients: Player ID " + castingPlayerID + ", " + spellID);

		// First add on server
		final MapCoordinates3DEx spellLocation;
		if (cityLocation == null)
			spellLocation = null;
		else
			spellLocation = new MapCoordinates3DEx (cityLocation);

		final MemoryMaintainedSpell trueSpell = new MemoryMaintainedSpell ();
		trueSpell.setCastingPlayerID (castingPlayerID);
		trueSpell.setSpellID (spellID);
		trueSpell.setUnitURN (unitURN);
		trueSpell.setUnitSkillID (unitSkillID);
		trueSpell.setCastInCombat (castInCombat);
		trueSpell.setCityLocation (spellLocation);
		trueSpell.setCitySpellEffectID (citySpellEffectID);
		trueSpell.setSpellURN (gsk.getNextFreeSpellURN ());

		gsk.setNextFreeSpellURN (gsk.getNextFreeSpellURN () + 1);
		gsk.getTrueMap ().getMaintainedSpell ().add (trueSpell);

		// Then let the other routine deal with updating player memory and the clients
		if (players != null)
			addExistingTrueMaintainedSpellToClients (gsk, trueSpell, players, db, sd);

		log.trace ("Exiting addMaintainedSpellOnServerAndClients");
	}

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param spellURN Which spell it is
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void switchOffMaintainedSpellOnServerAndClients (final FogOfWarMemory trueMap, final int spellURN,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering switchOffMaintainedSpellOnServerAndClients: Spell URN " + spellURN);

		// Get the spell details before we remove it
		final MemoryMaintainedSpell trueSpell = getMemoryMaintainedSpellUtils ().findSpellURN (spellURN, trueMap.getMaintainedSpell (), "switchOffMaintainedSpellOnServerAndClients");
		
		// Switch off on server
		getMemoryMaintainedSpellUtils ().removeSpellURN (spellURN, trueMap.getMaintainedSpell ());

		// Build the message ready to send it to whoever could see the spell
		final SwitchOffMaintainedSpellMessage msg = new SwitchOffMaintainedSpellMessage ();
		msg.setSpellURN (spellURN);

		// Check which players could see the spell
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeSpellMidTurn (trueSpell, trueMap.getMap (), trueMap.getUnit (), player, db, sd.getFogOfWarSetting ()))
			{
				// Update player's memory on server
				getMemoryMaintainedSpellUtils ().removeSpellURN (spellURN, priv.getFogOfWarMemory ().getMaintainedSpell ());

				// Update on client
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
			}
		}
		
		// Does the spell generate a CAE? e.g. Heavenly Light and Cloud of Shadow; if so then remove it
		if (trueSpell.getCitySpellEffectID () != null)
		{
			final CitySpellEffectSvr citySpellEffect = db.findCitySpellEffect (trueSpell.getCitySpellEffectID (), "switchOffMaintainedSpellOnServerAndClients");
			if (citySpellEffect.getCombatAreaEffectID () != null)
			{
				final MemoryCombatAreaEffect trueCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
					(trueMap.getCombatAreaEffect (), (MapCoordinates3DEx) trueSpell.getCityLocation (), citySpellEffect.getCombatAreaEffectID (), trueSpell.getCastingPlayerID ());
				
				if (trueCAE != null)
					removeCombatAreaEffectFromServerAndClients (trueMap, trueCAE.getCombatAreaEffectURN (), players, db, sd);
			}
		}

		// The removed spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (players, trueSpell.getCastingPlayerID (), "switchOffMaintainedSpellOnServerAndClients");
		getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, castingPlayer, players, false, "switchOffMaintainedSpellOnServerAndClients", sd, db);

		log.trace ("Exiting switchOffMaintainedSpellOnServerAndClients");
	}

	/**
	 * @param gsk Server knowledge structure to add the CAE to
	 * @param combatAreaEffectID Which CAE is it
	 * @param spellID Which spell was cast to produce this CAE; null for CAEs that aren't from spells, like node auras
	 * @param castingPlayerID Player who cast the CAE if it was created via a spell; null for natural CAEs (like node auras)
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
	 * @param players List of players in the session, this can be passed in null for when CAEs are being added to the map pre-game
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void addCombatAreaEffectOnServerAndClients (final MomGeneralServerKnowledgeEx gsk,
		final String combatAreaEffectID, final String spellID, final Integer castingPlayerID, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering addCombatAreaEffectOnServerAndClients: " + combatAreaEffectID);

		// First add on server
		final MapCoordinates3DEx caeLocation;
		if (mapLocation == null)
			caeLocation = null;
		else
			caeLocation = new MapCoordinates3DEx (mapLocation);

		final MemoryCombatAreaEffect trueCAE = new MemoryCombatAreaEffect ();
		trueCAE.setCombatAreaEffectID (combatAreaEffectID);
		trueCAE.setCastingPlayerID (castingPlayerID);
		trueCAE.setMapLocation (caeLocation);
		trueCAE.setCombatAreaEffectURN (gsk.getNextFreeCombatAreaEffectURN ());

		gsk.setNextFreeCombatAreaEffectURN (gsk.getNextFreeCombatAreaEffectURN () + 1);
		gsk.getTrueMap ().getCombatAreaEffect ().add (trueCAE);

		// Build the message ready to send it to whoever can see the CAE
		final AddCombatAreaEffectMessage msg = new AddCombatAreaEffectMessage ();
		msg.setMemoryCombatAreaEffect (trueCAE);
		msg.setSpellID (spellID);

		// Check which players can see the CAE
		if (players != null)
			for (final PlayerServerDetails player : players)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				if (getFogOfWarMidTurnVisibility ().canSeeCombatAreaEffectMidTurn (trueCAE, priv.getFogOfWar (), sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
				{
					// Update player's memory on server
					if (getFogOfWarDuplication ().copyCombatAreaEffect (trueCAE, priv.getFogOfWarMemory ().getCombatAreaEffect ()))
	
						// Update on client
						if (player.getPlayerDescription ().isHuman ())
							player.getConnection ().sendMessageToClient (msg);
				}
			}

		log.trace ("Exiting addCombatAreaEffectOnServerAndClients");
	}

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param combatAreaEffectURN Which CAE is it
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void removeCombatAreaEffectFromServerAndClients (final FogOfWarMemory trueMap,
		final int combatAreaEffectURN, final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering removeCombatAreaEffectFromServerAndClients: CAE URN " + combatAreaEffectURN);

		// Get the CAE's details before we remove it
		final MemoryCombatAreaEffect trueCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffectURN (combatAreaEffectURN, trueMap.getCombatAreaEffect (), "removeCombatAreaEffectFromServerAndClients");
		
		// Remove on server
		getMemoryCombatAreaEffectUtils ().removeCombatAreaEffectURN (combatAreaEffectURN, trueMap.getCombatAreaEffect ());

		// Build the message ready to send it to whoever can see the CAE
		final CancelCombatAreaEffectMessage msg = new CancelCombatAreaEffectMessage ();
		msg.setCombatAreaEffectURN (combatAreaEffectURN);

		// Check which players can see the CAE
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeCombatAreaEffectMidTurn (trueCAE, priv.getFogOfWar (), sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))		// Cheating a little passing msgData as the CAE details, but we know they're correct
			{
				// Update player's memory on server
				getMemoryCombatAreaEffectUtils ().removeCombatAreaEffectURN (combatAreaEffectURN, priv.getFogOfWarMemory ().getCombatAreaEffect ());

				// Update on client
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
			}
		}

		log.trace ("Exiting removeCombatAreaEffectFromServerAndClients");
	}
	
	/**
	 * @param gsk Server knowledge structure to add the building(s) to
	 * @param players List of players in the session, this can be passed in null for when buildings are being added to the map pre-game
	 * @param cityLocation Location of the city to add the building(s) to
	 * @param firstBuildingID First building ID to create, mandatory
	 * @param secondBuildingID Second building ID to create; this is usually null, it is mainly here for casting Move Fortress, which creates both a Fortress + Summoning circle at the same time
	 * @param buildingCreatedFromSpellID The spell that resulted in the creation of this building (e.g. casting Wall of Stone creates City Walls); null if building was constructed in the normal way
	 * @param buildingCreationSpellCastByPlayerID The player who cast the spell that resulted in the creation of this building; null if building was constructed in the normal way
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addBuildingOnServerAndClients (final MomGeneralServerKnowledgeEx gsk, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx cityLocation, final String firstBuildingID, final String secondBuildingID,
		final String buildingCreatedFromSpellID, final Integer buildingCreationSpellCastByPlayerID,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering addBuildingOnServerAndClients: " + cityLocation + ", " + firstBuildingID + ", " + secondBuildingID + ", " + buildingCreatedFromSpellID);

		// First add on server
		final MemoryBuilding firstTrueBuilding;
		if (firstBuildingID == null)
			firstTrueBuilding = null;
		else
		{
			firstTrueBuilding = new MemoryBuilding ();
			firstTrueBuilding.setCityLocation (new MapCoordinates3DEx (cityLocation));
			firstTrueBuilding.setBuildingID (firstBuildingID);
			firstTrueBuilding.setBuildingURN (gsk.getNextFreeBuildingURN ());
			
			gsk.setNextFreeBuildingURN (gsk.getNextFreeBuildingURN () + 1);
			gsk.getTrueMap ().getBuilding ().add (firstTrueBuilding);
		}

		final MemoryBuilding secondTrueBuilding;
		if (secondBuildingID == null)
			secondTrueBuilding = null;
		else
		{
			secondTrueBuilding = new MemoryBuilding ();
			secondTrueBuilding.setCityLocation (new MapCoordinates3DEx (cityLocation));
			secondTrueBuilding.setBuildingID (secondBuildingID);
			secondTrueBuilding.setBuildingURN (gsk.getNextFreeBuildingURN ());

			gsk.setNextFreeBuildingURN (gsk.getNextFreeBuildingURN () + 1);
			gsk.getTrueMap ().getBuilding ().add (secondTrueBuilding);
		}

		// Build the message ready to send it to whoever can see the building
		// This is done here rather in a method on FogOfWarDuplication because its a bit weird where we can have two buildings but both are optional
		final AddBuildingMessage msg = new AddBuildingMessage ();
		msg.setFirstBuilding (firstTrueBuilding);
		msg.setSecondBuilding (secondTrueBuilding);
		msg.setBuildingCreatedFromSpellID (buildingCreatedFromSpellID);
		msg.setBuildingCreationSpellCastByPlayerID (buildingCreationSpellCastByPlayerID);

		// Check which players can see the building
		if (players != null)
		{
			for (final PlayerServerDetails player : players)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
				if (getFogOfWarCalculations ().canSeeMidTurn (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
				{
					// Add into player's memory on server
					if (firstTrueBuilding != null)
						getFogOfWarDuplication ().copyBuilding (firstTrueBuilding, priv.getFogOfWarMemory ().getBuilding ());
	
					if (secondTrueBuilding != null)
						getFogOfWarDuplication ().copyBuilding (secondTrueBuilding, priv.getFogOfWarMemory ().getBuilding ());
	
					// Send to client
					if (player.getPlayerDescription ().isHuman ())
						player.getConnection ().sendMessageToClient (msg);
				}
			}

			// The new building might be an Oracle, so recalculate fog of war
			// Buildings added at the start of the game are added straight to the TrueMap without using this
			// routine, so this doesn't cause a bunch of FOW recalculations before the game starts
			final OverlandMapCityData cityData = gsk.getTrueMap ().getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (players, cityData.getCityOwnerID (), "addBuildingOnServerAndClients");
			getFogOfWarProcessing ().updateAndSendFogOfWar (gsk.getTrueMap (), cityOwner, players, false, "addBuildingOnServerAndClients", sd, db);
		}

		log.trace ("Exiting addBuildingOnServerAndClients");
	}

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param buildingURN Which building to remove
	 * @param updateBuildingSoldThisTurn If true, tells client to update the buildingSoldThisTurn flag, which will prevents this city from selling a 2nd building this turn
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void destroyBuildingOnServerAndClients (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final int buildingURN, final boolean updateBuildingSoldThisTurn,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering destroyBuildingOnServerAndClients: Building URN " + buildingURN);

		// Get the building details before we remove it
		final MemoryBuilding trueBuilding = getMemoryBuildingUtils ().findBuildingURN (buildingURN, trueMap.getBuilding (), "destroyBuildingOnServerAndClients");
		final MapCoordinates3DEx cityLocation = (MapCoordinates3DEx) trueBuilding.getCityLocation ();
		
		// Destroy on server
		getMemoryBuildingUtils ().removeBuildingURN (buildingURN, trueMap.getBuilding ());

		// Build the message ready to send it to whoever can see the building
		final DestroyBuildingMessage msg = new DestroyBuildingMessage ();
		msg.setBuildingURN (buildingURN);
		msg.setUpdateBuildingSoldThisTurn (updateBuildingSoldThisTurn);

		// Check which players could see the building
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
			if (getFogOfWarCalculations ().canSeeMidTurn (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
			{
				// Remove from player's memory on server
				getMemoryBuildingUtils ().removeBuildingURN (buildingURN, priv.getFogOfWarMemory ().getBuilding ());

				// Send to client
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
			}
		}

		// The destroyed building might be an Oracle, so recalculate fog of war
		final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (players, cityData.getCityOwnerID (), "destroyBuildingOnServerAndClients");
		getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, cityOwner, players, false, "destroyBuildingOnServerAndClients", sd, db);

		log.trace ("Exiting destroyBuildingOnServerAndClients");
	}

	/**
	 * Informs clients who can see this unit of its damage taken & experience
	 *
	 * @param tu True unit details
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void updatePlayerMemoryOfUnit_DamageTakenAndExperience (final MemoryUnit tu, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering updatePlayerMemoryOfUnit_DamageTakenAndExperience: Unit URN " + tu.getUnitURN ());

		// First build the message
		final UpdateDamageTakenAndExperienceMessage msg = new UpdateDamageTakenAndExperienceMessage ();
		msg.setUnitURN (tu.getUnitURN ());
		msg.setDamageTaken (tu.getDamageTaken ());
		msg.setExperience (getUnitUtils ().getBasicSkillValue (tu.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));

		// Check which players can see the unit
		// Note it isn't enough to say "Is the Unit URN in the player's memory" - maybe they've seen the unit before and are remembering
		// what they saw, but cannot see it now - in that case they shouldn't receive any updates about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tu, trueTerrain, thisPlayer, db, fogOfWarSettings))
			{
				// Update player's memory on server
				final MemoryUnit mu = getUnitUtils ().findUnitURN (tu.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "updatePlayerMemoryOfUnit_DamageTakenAndExperience");
				mu.setDamageTaken (msg.getDamageTaken ());
				
				if (msg.getExperience () >= 0)
					getUnitUtils ().setBasicSkillValue (mu, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, msg.getExperience ());

				// Update player's memory on client
				if (thisPlayer.getPlayerDescription ().isHuman ())
					thisPlayer.getConnection ().sendMessageToClient (msg);
			}
		}

		log.trace ("Exiting updatePlayerMemoryOfUnit_DamageTakenAndExperience");
	}

	/**
	 * After setting new unit name on server, updates player memories and clients who can see the unit
	 *
	 * @param tu True unit details
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or the player should be able to see the unit but it isn't in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	@Override
	public final void updatePlayerMemoryOfUnit_UnitName (final MemoryUnit tu, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final FogOfWarSetting fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering updatePlayerMemoryOfUnit_UnitName: Unit URN " + tu.getUnitURN ());

		// First build the message
		final UpdateUnitNameMessage msg = new UpdateUnitNameMessage ();
		msg.setUnitURN (tu.getUnitURN ());
		msg.setUnitName (tu.getUnitName ());

		// Check which players can see the unit
		// Note it isn't enough to say "Is the Unit URN in the player's memory" - maybe they've seen the unit before and are remembering
		// what they saw, but cannot see it now - in that case they shouldn't receive any updates about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tu, trueTerrain, thisPlayer, db, fogOfWarSettings))
			{
				// Update player's memory on server
				final MemoryUnit mu = getUnitUtils ().findUnitURN (tu.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "updatePlayerMemoryOfUnit_UnitName");
				mu.setUnitName (msg.getUnitName ());

				// Update player's memory on client
				if (thisPlayer.getPlayerDescription ().isHuman ())
					thisPlayer.getConnection ().sendMessageToClient (msg);
			}
		}

		log.trace ("Exiting updatePlayerMemoryOfUnit_UnitName");
	}
	
	/**
	 * Informs clients who can see any of the units involved about how much combat damage an attacker and/or defender(s) have taken.
	 * The two players in combat use this to show the animation of the attack, be it a melee, ranged or spell attack.
	 * If the damage is enough to kill off the unit, the client will take care of this - we don't need to send a separate KillUnitMessage.
	 * 
	 * @param tuAttacker Server's true memory of unit that made the attack; or null if the attack isn't coming from a unit
	 * @param attackerPlayerID Player owning tuAttacker unit; supplied in case tuAttacker is null
	 * @param tuDefenders Server's true memory of unit(s) that got hit
	 * @param attackSkillID Skill used to make the attack, e.g. for gaze or breath attacks
	 * @param attackAttributeID Attribute used to make the attack, for regular melee or ranged attacks
	 * @param attackSpellID Spell used to make the attack
	 * @param damageType Type of damage done to defenders
	 * @param players List of players in the session
	 * @param trueTerrain True terrain map
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found, or a player should know about one of the units but we can't find it in their memory
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendCombatDamageToClients (final MemoryUnit tuAttacker, final int attackerPlayerID, final List<MemoryUnit> tuDefenders,
		final String attackSkillID, final String attackAttributeID, final String attackSpellID, final DamageTypeID damageType,
		final List<PlayerServerDetails> players, final MapVolumeOfMemoryGridCells trueTerrain,
		final ServerDatabaseEx db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		if (log.isTraceEnabled ())
		{
			String msg = "Entering sendCombatDamageToClients: Attacking unit URN " + ((tuAttacker != null) ? new Integer (tuAttacker.getUnitURN ()).toString () : "N/A") +
				", Defending unit URN(s) ";
			
			for (final MemoryUnit tuDefender : tuDefenders)
				msg = msg + tuDefender.getUnitURN () + ", ";
			
			log.trace (msg);
		}

		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			
			// We might get the interesting situation where an outside observer not involved in the combat can see one
			// unit stack but not the other - and so know about one unit but not the other.
			// We handle this by leaving one of the UnitURNs as null, but this means we have to build the message separately for each client.
			final ApplyDamageMessage msg;
			if (thisPlayer.getPlayerDescription ().isHuman ())
			{
				msg = new ApplyDamageMessage ();
				msg.setAttackerPlayerID (attackerPlayerID);
				msg.setDamageType (damageType);
			}
			else
				msg = null;
			
			// Attacking unit
			if ((tuAttacker != null) && (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tuAttacker, trueTerrain, thisPlayer, db, fogOfWarSettings)))
			{
				// Update player's memory of attacker on server
				final MemoryUnit muAttacker = getUnitUtils ().findUnitURN (tuAttacker.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "sendCombatDamageToClients-a");
				muAttacker.setDamageTaken (tuAttacker.getDamageTaken ());
				muAttacker.setCombatHeading (tuAttacker.getCombatHeading ());
				muAttacker.setDoubleCombatMovesLeft (tuAttacker.getDoubleCombatMovesLeft ());

				// Update player's memory of attacker on client
				if (msg != null)
				{
					msg.setAttackerUnitURN (tuAttacker.getUnitURN ());
					msg.setAttackerDamageTaken (tuAttacker.getDamageTaken ());
					msg.setAttackerDirection (tuAttacker.getCombatHeading ());
					msg.setAttackerDoubleCombatMovesLeft (tuAttacker.getDoubleCombatMovesLeft ());
				}
			}
			
			// Defending unit(s)
			for (final MemoryUnit tuDefender : tuDefenders)
				if (getFogOfWarMidTurnVisibility ().canSeeUnitMidTurn (tuDefender, trueTerrain, thisPlayer, db, fogOfWarSettings))
				{
					// Update player's memory of defender on server
					final MemoryUnit muDefender = getUnitUtils ().findUnitURN (tuDefender.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "sendCombatDamageToClients-d");
					muDefender.setDamageTaken (tuDefender.getDamageTaken ());
					muDefender.setCombatHeading (tuDefender.getCombatHeading ());
	
					// Update player's memory of defender on client
					if (msg != null)
					{
						final ApplyDamageMessageUnit msgUnit = new ApplyDamageMessageUnit ();
						msgUnit.setDefenderUnitURN (tuDefender.getUnitURN ());
						msgUnit.setDefenderDamageTaken (tuDefender.getDamageTaken ());
						msgUnit.setDefenderDirection (tuDefender.getCombatHeading ());
						msg.getDefenderUnit ().add (msgUnit);
					}
				}
			
			// Do we have a message to send?
			if (msg != null)
				if ((msg.getAttackerUnitURN () != null) || (msg.getDefenderUnit ().size () > 0))
				{
					msg.setAttackSkillID (attackSkillID);
					msg.setAttackAttributeID (attackAttributeID);
					msg.setAttackSpellID (attackSpellID);
					thisPlayer.getConnection ().sendMessageToClient (msg);
				}
		}		

		log.trace ("Exiting sendCombatDamageToClients");
	}

	/**
	 * Copies all of the listed units from source to destination, along with any unit spells (enchantments like Flame Blade or curses like Weakness)
	 * and then sends them to the client; this is used when a unit stack comes into view when it is moving

	 * @param unitStack List of units to copy
	 * @param trueSpells True spell details held on server
	 * @param player Player to copy details into
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 * @throws JAXBException If there is a problem sending a message to the client
	 * @throws XMLStreamException If there is a problem sending a message to the client
	 */
	@Override
	public final void addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (final List<MemoryUnit> unitStack,
		final List<MemoryMaintainedSpell> trueSpells, final PlayerServerDetails player, final ServerDatabaseEx db)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient: " +
			unitStack + ", Player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Units
		final List<Integer> unitURNs = new ArrayList<Integer> ();
		for (final MemoryUnit tu : unitStack)
		{
			unitURNs.add (tu.getUnitURN ());

			if (getFogOfWarDuplication ().copyUnit (tu, priv.getFogOfWarMemory ().getUnit ()))
				if (player.getPlayerDescription ().isHuman ())
				{
					final AddUnitMessage msg = new AddUnitMessage ();
					msg.setMemoryUnit (tu);
					player.getConnection ().sendMessageToClient (msg);
				}
		}

		// Spells cast on those units
		for (final MemoryMaintainedSpell trueSpell : trueSpells)
			if (trueSpell.getUnitURN () != null)
				if (unitURNs.contains (trueSpell.getUnitURN ()))
					if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))
						if (player.getPlayerDescription ().isHuman ())
						{
							final AddMaintainedSpellMessage msg = new AddMaintainedSpellMessage ();
							msg.setMaintainedSpell (trueSpell);
							player.getConnection ().sendMessageToClient (msg);
						}

		log.trace ("Exiting addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient");
	}

	/**
	 * Similar to the above routine, this removed all the listed units from the player's memory on the server, along with any unit spells
	 * Unlike the above routine, it sends no messages to inform the client
	 *
	 * @param unitStack List of unit URNs to remove
	 * @param player Player to remove them from
	 */
	@Override
	public final void freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (final List<Integer> unitStack, final PlayerServerDetails player)
	{
		log.trace ("Entering freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly: " +
			unitStack + ", Player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Units
		final Iterator<MemoryUnit> units = priv.getFogOfWarMemory ().getUnit ().iterator ();
		while (units.hasNext ())
		{
			final MemoryUnit thisUnit = units.next ();
			if (unitStack.contains (thisUnit.getUnitURN ()))
				units.remove ();
		}

		// Spells
		final Iterator<MemoryMaintainedSpell> spells = priv.getFogOfWarMemory ().getMaintainedSpell ().iterator ();
		while (spells.hasNext ())
		{
			final MemoryMaintainedSpell thisSpell = spells.next ();
			if (thisSpell.getUnitURN () != null)
				if (unitStack.contains (thisSpell.getUnitURN ()))
					spells.remove ();
		}

		log.trace ("Exiting freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly");
	}

	/**
	 * Finds the direction to make a one cell move in, when trying to get from moveFrom to moveTo
	 * This is based on the directions calculated by the movement routine so will take into account everything known about the terrain, so the direction
	 * chosen will try to make use of roads etc. and take into account the types of units moving rather than just being in a straight line
	 *
	 * @param moveFrom Location to move from
	 * @param moveTo Location to determine direction to
	 * @param movementDirections Movement directions from moveFrom to every location on the map
	 * @param sys Overland map coordinate system
	 * @return Direction to make one cell move in
	 * @throws MomException If we can't find a route from moveFrom to moveTo
	 */
	@Override
	public final int determineMovementDirection (final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo,
		final int [] [] [] movementDirections, final CoordinateSystem sys) throws MomException
	{
		log.trace ("Entering determineMovementDirection: " + moveFrom + ", " + moveTo);

		// The value at each cell of the directions grid is the direction we need to have come FROM to get there
		// So we need to start at the destinationand follow backwards down the movement path until we
		// get back to the From location, and the direction we want is the one that led us to the From location
		final MapCoordinates3DEx coords = new MapCoordinates3DEx (moveTo);

		int direction = -1;
		while ((coords.getX () != moveFrom.getX () || (coords.getY () != moveFrom.getY ())))
		{
			direction = movementDirections [coords.getZ ()] [coords.getY ()] [coords.getX ()];
			if (!getCoordinateSystemUtils ().move3DCoordinates (sys, coords, getCoordinateSystemUtils ().normalizeDirection (sys.getCoordinateSystemType (), direction + 4)))
				throw new MomException ("determineMovementDirection: Server map tracing moved to a cell off the map");
		}

		if (direction < 0)
			throw new MomException ("determineMovementDirection: Failed to trace a route from the movement target back to the movement origin");

		log.trace ("Exiting determineMovementDirection = " + direction);
		return direction;
	}

	/**
	 * Reduces the amount of remaining movement that all units in this stack have left by the amount that it costs them to enter a grid cell of the specified type
	 *
	 * @param unitStack The unit stack that is moving
	 * @param unitStackSkills All the skills that any units in the stack have
	 * @param tileTypeID Tile type being moved onto
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 */
	@Override
	public final void reduceMovementRemaining (final List<MemoryUnit> unitStack, final List<String> unitStackSkills, final String tileTypeID,
		final List<MemoryMaintainedSpell> spells, final ServerDatabaseEx db)
	{
		log.trace ("Entering reduceMovementRemaining: " + unitStack.size () + ", " + tileTypeID);

		for (final MemoryUnit thisUnit : unitStack)
		{
			// Don't just work out the distance it took for the whole stack to get here - if e.g. one unit can fly it might be able to
			// move into mountains taking only 1 MP whereas another unit in the same stack might take 3 MP
			Integer doubleMovementCost = getUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, tileTypeID, spells, db);
			
			// The only way we can get impassable here as a valid move is if we're loading onto a transport, in which case force movement spent to 2
			if (doubleMovementCost == null)
				doubleMovementCost = 2;

			// Entirely valid for doubleMovementCost to be > doubleOverlandMovesLeft, if e.g. a spearmen with 1 MP is moving onto mountains which cost 3 MP
			if (doubleMovementCost > thisUnit.getDoubleOverlandMovesLeft ())
				thisUnit.setDoubleOverlandMovesLeft (0);
			else
				thisUnit.setDoubleOverlandMovesLeft (thisUnit.getDoubleOverlandMovesLeft () - doubleMovementCost);
		}

		log.trace ("Exiting reduceMovementRemaining");
	}
	
	/**
	 * @return Single cell FOW calculations
	 */
	public final FogOfWarCalculations getFogOfWarCalculations ()
	{
		return fogOfWarCalculations;
	}

	/**
	 * @param calc Single cell FOW calculations
	 */
	public final void setFogOfWarCalculations (final FogOfWarCalculations calc)
	{
		fogOfWarCalculations = calc;
	}

	/**
	 * @return FOW visibility checks
	 */
	public final FogOfWarMidTurnVisibility getFogOfWarMidTurnVisibility ()
	{
		return fogOfWarMidTurnVisibility;
	}

	/**
	 * @param vis FOW visibility checks
	 */
	public final void setFogOfWarMidTurnVisibility (final FogOfWarMidTurnVisibility vis)
	{
		fogOfWarMidTurnVisibility = vis;
	}
	
	/**
	 * @return FOW duplication utils
	 */
	public final FogOfWarDuplication getFogOfWarDuplication ()
	{
		return fogOfWarDuplication;
	}

	/**
	 * @param dup FOW duplication utils
	 */
	public final void setFogOfWarDuplication (final FogOfWarDuplication dup)
	{
		fogOfWarDuplication = dup;
	}

	/**
	 * @return Main FOW update routine
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Main FOW update routine
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
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
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return MemoryBuilding utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}

	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}
}
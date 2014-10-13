package momime.server.fogofwar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_5.FogOfWarSettingData;
import momime.common.database.newgame.v0_9_5.FogOfWarValue;
import momime.common.messages.servertoclient.v0_9_5.AddBuildingMessage;
import momime.common.messages.servertoclient.v0_9_5.AddBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_5.AddCombatAreaEffectMessage;
import momime.common.messages.servertoclient.v0_9_5.AddMaintainedSpellMessage;
import momime.common.messages.servertoclient.v0_9_5.AddUnitMessage;
import momime.common.messages.servertoclient.v0_9_5.ApplyDamageMessage;
import momime.common.messages.servertoclient.v0_9_5.CancelCombatAreaEffectMessage;
import momime.common.messages.servertoclient.v0_9_5.CancelCombatAreaEffectMessageData;
import momime.common.messages.servertoclient.v0_9_5.DestroyBuildingMessage;
import momime.common.messages.servertoclient.v0_9_5.DestroyBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_5.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_5.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_5.KillUnitMessageData;
import momime.common.messages.servertoclient.v0_9_5.MoveUnitStackOverlandMessage;
import momime.common.messages.servertoclient.v0_9_5.PendingMovementMessage;
import momime.common.messages.servertoclient.v0_9_5.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.v0_9_5.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.v0_9_5.SwitchOffMaintainedSpellMessageData;
import momime.common.messages.servertoclient.v0_9_5.UpdateCityMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateCityMessageData;
import momime.common.messages.servertoclient.v0_9_5.UpdateDamageTakenAndExperienceMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateOverlandMovementRemainingMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateOverlandMovementRemainingUnit;
import momime.common.messages.servertoclient.v0_9_5.UpdateTerrainMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateTerrainMessageData;
import momime.common.messages.servertoclient.v0_9_5.UpdateUnitNameMessage;
import momime.common.messages.servertoclient.v0_9_5.UpdateUnitToAliveMessage;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.FogOfWarStateID;
import momime.common.messages.v0_9_5.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.PendingMovement;
import momime.common.messages.v0_9_5.TurnSystem;
import momime.common.messages.v0_9_5.UnitCombatSideID;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomFogOfWarCalculations;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_5.Plane;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;
import momime.server.process.CombatProcessing;
import momime.server.process.CombatScheduler;

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
	private MomFogOfWarCalculations fogOfWarCalculations;

	/** FOW duplication utils */
	private FogOfWarDuplication fogOfWarDuplication;
	
	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Unit calculations */
	private MomUnitCalculations unitCalculations;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;

	/** Server-only city calculations */
	private MomServerCityCalculations serverCityCalculations;
	
	/** Server-only unit calculations */
	private MomServerUnitCalculations serverUnitCalculations;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Simultaneous turns combat scheduler */
	private CombatScheduler combatScheduler;

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
		final List<PlayerServerDetails> players, final MapCoordinates3DEx coords, final FogOfWarSettingData fogOfWarSettings, final boolean newlyAddedCity)
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

			if (getFogOfWarCalculations ().canSeeMidTurn (state, fogOfWarSettings.getTerrainAndNodeAuras ()))
			{
				// Update player's memory on server
				final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

				final boolean includeCurrentlyConstructing;
				if (tc.getCityData () == null)
					includeCurrentlyConstructing = false;
				else
					includeCurrentlyConstructing = (thisPlayer.getPlayerDescription ().getPlayerID () == tc.getCityData ().getCityOwnerID ()) ||
						(fogOfWarSettings.isSeeEnemyCityConstruction ());

				if (getFogOfWarDuplication ().copyCityData (tc, mc, includeCurrentlyConstructing))

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
	 * @param unit True unit to test
	 * @param trueTerrain True terrain map
	 * @param player The player we are testing whether they can see the unit
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @return True if player can see this unit
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	final boolean canSeeUnitMidTurn (final MemoryUnit unit, final MapVolumeOfMemoryGridCells trueTerrain, final PlayerServerDetails player,
		final ServerDatabaseEx db, final FogOfWarSettingData fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean canSee;

		// Firstly we only know abouts that are alive
		if (unit.getStatus () != UnitStatusID.ALIVE)
			canSee = false;
		else
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			/*
			 * This is basically
			 * canSee = (fogOfWarArea.get (unit.getCurrentLocation ()) == FogOfWarStateID.CAN_SEE)
			 *
			 * Towers of Wizardry add a complication - if the unit is standing in a Tower of Wizardry then they'll be on plane 0, but perhaps we can see the
			 * tower on plane 1... so what this breaks down to is that we'll know about the unit providing we can see it on ANY plane
			 */
			canSee = getFogOfWarCalculations ().canSeeMidTurnOnAnyPlaneIfTower
				((MapCoordinates3DEx) unit.getUnitLocation (), fogOfWarSettings.getUnits (), trueTerrain, priv.getFogOfWar (), db);
		}

		return canSee;
	}

	/**
	 * @param spell True spell to test
	 * @param trueTerrain True terrain map
	 * @param trueUnits True list of units
	 * @param player The player we are testing whether they can see the spell
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of war settings from session description
	 * @return True if player can see this spell
	 * @throws RecordNotFoundException If the unit that the spell is cast on, or tile type or map feature IDs cannot be found
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	final boolean canSeeSpellMidTurn (final MemoryMaintainedSpell spell,
		final MapVolumeOfMemoryGridCells trueTerrain, final List<MemoryUnit> trueUnits, final PlayerServerDetails player,
		final ServerDatabaseEx db, final FogOfWarSettingData fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean canSee;

		// Unit spell?
		if (spell.getUnitURN () != null)
		{
			final MemoryUnit unit = getUnitUtils ().findUnitURN (spell.getUnitURN (), trueUnits, "canSeeSpellMidTurn");
			canSee = canSeeUnitMidTurn (unit,  trueTerrain, player, db, fogOfWarSettings);
		}

		// City spell?
		else if (spell.getCityLocation () != null)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			
			canSee = getFogOfWarCalculations ().canSeeMidTurn (priv.getFogOfWar ().getPlane ().get (spell.getCityLocation ().getZ ()).getRow ().get
				(spell.getCityLocation ().getY ()).getCell ().get (spell.getCityLocation ().getX ()), fogOfWarSettings.getCitiesSpellsAndCombatAreaEffects ());
		}

		// Overland enchantment
		else
			canSee = true;

		return canSee;
	}

	/**
	 * @param cae True CAE to test
	 * @param fogOfWarArea Area the player can/can't see, outside of FOW recalc
	 * @param setting FOW CAE setting, from session description
	 * @return True if player can see this CAE
	 */
	final boolean canSeeCombatAreaEffectMidTurn (final MemoryCombatAreaEffect cae,
		final MapVolumeOfFogOfWarStates fogOfWarArea, final FogOfWarValue setting)
	{
		final boolean canSee;

		// This is a lot simpler than the spell version, since CAEs can't be targetted on specific units, only on a map cell or globally

		// Localized CAE?
		if (cae.getMapLocation () != null)
			canSee = getFogOfWarCalculations ().canSeeMidTurn (fogOfWarArea.getPlane ().get (cae.getMapLocation ().getZ ()).getRow ().get
				(cae.getMapLocation ().getY ()).getCell ().get (cae.getMapLocation ().getX ()), setting);

		// Global CAE - so can see it everywhere
		else
			canSee = true;

		return canSee;
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
	public final MemoryUnit addUnitOnServerAndClients (final MomGeneralServerKnowledge gsk,
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
				(gsk.getTrueMap ().getBuilding (), gsk.getTrueMap ().getMap (), buildingsLocation, unitOwnerPPK.getPick (), sd.getMapSize (), db);
		}
		else
		{
			startingExperience = 0;
			weaponGrade = null;
		}

		// Add on server
		// Even for heroes, we load in their default skill list - this is how heroes default skills are loaded during game startup
		final MemoryUnit newUnit = getUnitUtils ().createMemoryUnit (unitID, gsk.getNextFreeUnitURN (), weaponGrade, startingExperience, true, db);
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
			addMsg.setData (getFogOfWarDuplication ().createAddUnitMessage (trueUnit));

			final UpdateUnitToAliveMessage updateMsg = new UpdateUnitToAliveMessage ();
			updateMsg.setUnitLocation (unitLocation);
			updateMsg.setUnitURN (trueUnit.getUnitURN ());

			for (final PlayerServerDetails player : players)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				if (canSeeUnitMidTurn (trueUnit, trueMap.getMap (), player, db, sd.getFogOfWarSetting ()))
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
		final FogOfWarSettingData fogOfWarSettings, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.trace ("Entering killUnitOnServerAndClients: Unit URN " + trueUnit.getUnitURN ());

		// Build the message ready to send it to whoever could see the unit
		// Action has to be set per player depending on who can see it
		final KillUnitMessageData msgData = new KillUnitMessageData ();
		msgData.setUnitURN (trueUnit.getUnitURN ());

		final KillUnitMessage msg = new KillUnitMessage ();
		msg.setData (msgData);

		// Check which players could see the unit
		for (final PlayerServerDetails player : players)
		{
			if (canSeeUnitMidTurn (trueUnit, trueMap.getMap (), player, db, fogOfWarSettings))
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

						msgData.setKillUnitActionID (transmittedAction);
					else
						msgData.setKillUnitActionID (KillUnitActionID.FREE);
				
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
	 * @param trueSpell True spell to add
	 * @param players List of players in the session
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void addExistingTrueMaintainedSpellToClients (final MemoryMaintainedSpell trueSpell, final List<PlayerServerDetails> players, final FogOfWarMemory trueMap,
		final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering addExistingTrueMaintainedSpellToClients: Player ID " + trueSpell.getCastingPlayerID () + ", " + trueSpell.getSpellID ());

		// Build the message ready to send it to whoever can see the spell
		final AddMaintainedSpellMessage msg = new AddMaintainedSpellMessage ();
		msg.setData (getFogOfWarDuplication ().createAddSpellMessage (trueSpell));

		// Check which players can see the spell
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (canSeeSpellMidTurn (trueSpell, trueMap.getMap (), trueMap.getUnit (), player, db, sd.getFogOfWarSetting ()))
			{
				// Update player's memory on server
				if (getFogOfWarDuplication ().copyMaintainedSpell (trueSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))

					// Update on client
					if (player.getPlayerDescription ().isHuman ())
						player.getConnection ().sendMessageToClient (msg);
			}
		}

		// The new spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
		// While it may seem a bit odd to do this here (since the spell already existed on the server before calling this),
		// the spell would have been in an untargetted state so we wouldn't know what city to apply it to, so this is definitely the right place to do this
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (players, trueSpell.getCastingPlayerID (), "addExistingTrueMaintainedSpellToClients");
		getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, castingPlayer, players, false, "addExistingTrueMaintainedSpellToClients", sd, db);

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
	public final void addMaintainedSpellOnServerAndClients (final MomGeneralServerKnowledge gsk,
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

		gsk.getTrueMap ().getMaintainedSpell ().add (trueSpell);

		// Then let the other routine deal with updating player memory and the clients
		addExistingTrueMaintainedSpellToClients (trueSpell, players, gsk.getTrueMap (), db, sd);

		log.trace ("Exiting addMaintainedSpellOnServerAndClients");
	}

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
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void switchOffMaintainedSpellOnServerAndClients (final FogOfWarMemory trueMap,
		final int castingPlayerID, final String spellID, final Integer unitURN, final String unitSkillID,
		final boolean castInCombat, final MapCoordinates3DEx cityLocation, final String citySpellEffectID, final List<PlayerServerDetails> players,
		final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering switchOffMaintainedSpellOnServerAndClients: Player ID " + castingPlayerID + ", " + spellID);

		// First switch off on server
		getMemoryMaintainedSpellUtils ().switchOffMaintainedSpell (trueMap.getMaintainedSpell (), castingPlayerID, spellID, unitURN, unitSkillID, cityLocation, citySpellEffectID);

		// Build the message ready to send it to whoever could see the spell
		final SwitchOffMaintainedSpellMessageData msgData = new SwitchOffMaintainedSpellMessageData ();
		msgData.setCastingPlayerID (castingPlayerID);
		msgData.setSpellID (spellID);
		msgData.setUnitURN (unitURN);
		msgData.setUnitSkillID (unitSkillID);
		msgData.setCastInCombat (castInCombat);
		msgData.setCityLocation (cityLocation);
		msgData.setCitySpellEffectID (citySpellEffectID);

		final SwitchOffMaintainedSpellMessage msg = new SwitchOffMaintainedSpellMessage ();
		msg.setData (msgData);

		// Check which players could see the spell
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (canSeeSpellMidTurn (msgData, trueMap.getMap (), trueMap.getUnit (), player,
				db, sd.getFogOfWarSetting ()))		// Cheating a little passing msgData as the spell details, but we know they're correct
			{
				// Update player's memory on server
				getMemoryMaintainedSpellUtils ().switchOffMaintainedSpell (priv.getFogOfWarMemory ().getMaintainedSpell (), castingPlayerID, spellID, unitURN, unitSkillID, cityLocation, citySpellEffectID);

				// Update on client
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
			}
		}

		// The removed spell might be Awareness, Nature Awareness, Nature's Eye, or a curse on an enemy city, so might affect the fog of war of the player who cast it
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (players, castingPlayerID, "switchOffMaintainedSpellOnServerAndClients");
		getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, castingPlayer, players, false, "switchOffMaintainedSpellOnServerAndClients", sd, db);

		log.trace ("Exiting switchOffMaintainedSpellOnServerAndClients");
	}

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
	@Override
	public final void switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx combatLocation, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients: " + combatLocation);
		
		// Copy the list, since we'll be removing spells from it as we go
		final List<MemoryMaintainedSpell> copyOfTrueSpells = new ArrayList<MemoryMaintainedSpell> ();
		copyOfTrueSpells.addAll (trueMap.getMaintainedSpell ());
		
		for (final MemoryMaintainedSpell trueSpell : copyOfTrueSpells)
			if ((trueSpell.isCastInCombat ()) && (trueSpell.getUnitURN () != null))
			{
				// Find the unit that the spell is cast on, to see whether they're in this particular combat
				final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (trueSpell.getUnitURN (), trueMap.getUnit (), "switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients");
				if (combatLocation.equals (thisUnit.getCombatLocation ()))
					
					switchOffMaintainedSpellOnServerAndClients (trueMap,
						trueSpell.getCastingPlayerID (), trueSpell.getSpellID (), trueSpell.getUnitURN (), trueSpell.getUnitSkillID (),
						trueSpell.isCastInCombat (), (MapCoordinates3DEx) trueSpell.getCityLocation (), trueSpell.getCitySpellEffectID (),
						players, db, sd);
			}
		
		log.trace ("Exiting switchOffMaintainedSpellsCastOnUnitsInCombat_OnServerAndClients");
	}
	
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
	@Override
	public final void switchOffMaintainedSpellsInLocationOnServerAndClients (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MapCoordinates3DEx cityLocation, final int castingPlayerID,
		final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering switchOffMaintainedSpellsInLocationOnServerAndClients: " + cityLocation);

		// Copy the list, since we'll be removing spells from it as we go
		final List<MemoryMaintainedSpell> copyOfTrueSpells = new ArrayList<MemoryMaintainedSpell> ();
		copyOfTrueSpells.addAll (trueMap.getMaintainedSpell ());
		
		for (final MemoryMaintainedSpell trueSpell : copyOfTrueSpells)
			if ((cityLocation.equals (trueSpell.getCityLocation ())) &&
				(castingPlayerID == 0) || (trueSpell.getCastingPlayerID () == castingPlayerID))

				switchOffMaintainedSpellOnServerAndClients (trueMap,
					trueSpell.getCastingPlayerID (), trueSpell.getSpellID (), trueSpell.getUnitURN (), trueSpell.getUnitSkillID (),
					trueSpell.isCastInCombat (), (MapCoordinates3DEx) trueSpell.getCityLocation (), trueSpell.getCitySpellEffectID (),
					players, db, sd);
		
		log.trace ("Exiting switchOffMaintainedSpellsInLocationOnServerAndClients");
	}
	
	/**
	 * @param gsk Server knowledge structure to add the CAE to
	 * @param combatAreaEffectID Which CAE is it
	 * @param castingPlayerID Player who cast the CAE if it was created via a spell; null for natural CAEs (like node auras)
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
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
	public final void addCombatAreaEffectOnServerAndClients (final MomGeneralServerKnowledge gsk,
		final String combatAreaEffectID, final Integer castingPlayerID, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
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

		gsk.getTrueMap ().getCombatAreaEffect ().add (trueCAE);

		// Build the message ready to send it to whoever can see the CAE
		final AddCombatAreaEffectMessage msg = new AddCombatAreaEffectMessage ();
		msg.setData (getFogOfWarDuplication ().createAddCombatAreaEffectMessage (trueCAE));

		// Check which players can see the CAE
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (canSeeCombatAreaEffectMidTurn (trueCAE, priv.getFogOfWar (), sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
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
	 * @param combatAreaEffectID Which CAE is it
	 * @param castingPlayerID Player who cast the CAE if it was created via a spell; null for natural CAEs (like node auras)
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
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
		final String combatAreaEffectID, final Integer castingPlayerID, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering removeCombatAreaEffectFromServerAndClients: " + combatAreaEffectID);

		// First remove on server
		getMemoryCombatAreaEffectUtils ().cancelCombatAreaEffect (trueMap.getCombatAreaEffect (), mapLocation, combatAreaEffectID, castingPlayerID);

		// Build the message ready to send it to whoever can see the CAE
		final CancelCombatAreaEffectMessageData msgData = new CancelCombatAreaEffectMessageData ();
		msgData.setCastingPlayerID (castingPlayerID);
		msgData.setCombatAreaEffectID (combatAreaEffectID);
		msgData.setMapLocation (mapLocation);

		final CancelCombatAreaEffectMessage msg = new CancelCombatAreaEffectMessage ();
		msg.setData (msgData);

		// Check which players can see the CAE
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			if (canSeeCombatAreaEffectMidTurn (msgData, priv.getFogOfWar (), sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))		// Cheating a little passing msgData as the CAE details, but we know they're correct
			{
				// Update player's memory on server
				getMemoryCombatAreaEffectUtils ().cancelCombatAreaEffect (priv.getFogOfWarMemory ().getCombatAreaEffect (), mapLocation, combatAreaEffectID, castingPlayerID);

				// Update on client
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
			}
		}

		log.trace ("Exiting removeCombatAreaEffectFromServerAndClients");
	}
	
	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param mapLocation Indicates which city the CAE is cast on; null for CAEs not cast on cities
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
	public final void removeCombatAreaEffectsFromLocalisedSpells (final FogOfWarMemory trueMap, final MapCoordinates3DEx mapLocation,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering removeCombatAreaEffectsFromLocalisedSpells: " + mapLocation);
		
		// Better copy the list of CAEs, since we'll be removing them as we go along
		final List<MemoryCombatAreaEffect> copyOftrueCAEs = new ArrayList<MemoryCombatAreaEffect> ();
		copyOftrueCAEs.addAll (trueMap.getCombatAreaEffect ());
	
		// CAE must be localised at this combat location (so we don't remove global enchantments like Crusade) and must be owned by a player (so we don't remove node auras)
		for (final MemoryCombatAreaEffect trueCAE : copyOftrueCAEs)
			if ((mapLocation.equals (trueCAE.getMapLocation ())) && (trueCAE.getCastingPlayerID () != null))
				removeCombatAreaEffectFromServerAndClients (trueMap, trueCAE.getCombatAreaEffectID (), trueCAE.getCastingPlayerID (), mapLocation, players, db, sd);
		
		log.trace ("Exiting removeCombatAreaEffectsFromLocalisedSpells");
	}

	/**
	 * @param gsk Server knowledge structure to add the building(s) to
	 * @param players List of players in the session
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
	public final void addBuildingOnServerAndClients (final MomGeneralServerKnowledge gsk, final List<PlayerServerDetails> players,
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
			gsk.getTrueMap ().getBuilding ().add (secondTrueBuilding);
		}

		// Build the message ready to send it to whoever can see the building
		// This is done here rather in a method on FogOfWarDuplication because its a bit weird where we can have two buildings but both are optional
		final AddBuildingMessageData msgData = new AddBuildingMessageData ();
		msgData.setFirstBuildingID (firstBuildingID);
		msgData.setSecondBuildingID (secondBuildingID);
		msgData.setCityLocation (cityLocation);
		msgData.setBuildingCreatedFromSpellID (buildingCreatedFromSpellID);
		msgData.setBuildingCreationSpellCastByPlayerID (buildingCreationSpellCastByPlayerID);

		final AddBuildingMessage msg = new AddBuildingMessage ();
		msg.setData (msgData);

		// Check which players can see the building
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

		log.trace ("Exiting addBuildingOnServerAndClients");
	}

	/**
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param cityLocation Location of the city to remove the building from
	 * @param buildingID Which building to remove
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
		final List<PlayerServerDetails> players, final MapCoordinates3DEx cityLocation, final String buildingID, final boolean updateBuildingSoldThisTurn,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering destroyBuildingOnServerAndClients: " + cityLocation + ", " + buildingID);

		// First destroy on server
		getMemoryBuildingUtils ().destroyBuilding (trueMap.getBuilding (), cityLocation, buildingID);

		// Build the message ready to send it to whoever can see the building
		final DestroyBuildingMessageData msgData = new DestroyBuildingMessageData ();
		msgData.setBuildingID (buildingID);
		msgData.setCityLocation (cityLocation);
		msgData.setUpdateBuildingSoldThisTurn (updateBuildingSoldThisTurn);

		final DestroyBuildingMessage msg = new DestroyBuildingMessage ();
		msg.setData (msgData);

		// Check which players could see the building
		for (final PlayerServerDetails player : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ());
			if (getFogOfWarCalculations ().canSeeMidTurn (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
			{
				// Remove from player's memory on server
				getMemoryBuildingUtils ().destroyBuilding (priv.getFogOfWarMemory ().getBuilding (), cityLocation, buildingID);

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
	@Override
	public final void destroyAllBuildingsInLocationOnServerAndClients (final FogOfWarMemory trueMap,
		final List<PlayerServerDetails> players, final MapCoordinates3DEx cityLocation,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering destroyBuildingOnServerAndClients: " + cityLocation);
		
		// Better copy the list of buildings, since we'll be removing them as we go along
		final List<MemoryBuilding> copyOfBuildingsList = new ArrayList<MemoryBuilding> ();
		copyOfBuildingsList.addAll (trueMap.getBuilding ());
		for (final MemoryBuilding trueBuilding : copyOfBuildingsList)
			if (cityLocation.equals (trueBuilding.getCityLocation ()))
				destroyBuildingOnServerAndClients (trueMap, players, cityLocation, trueBuilding.getBuildingID (), false, sd, db);
		
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
	private final void updatePlayerMemoryOfUnit_DamageTakenAndExperience (final MemoryUnit tu, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final FogOfWarSettingData fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering updatePlayerMemoryOfUnit_DamageTakenAndExperience: Unit URN " + tu.getUnitURN ());

		// First build the message
		final UpdateDamageTakenAndExperienceMessage msg = new UpdateDamageTakenAndExperienceMessage ();
		msg.setUnitURN (tu.getUnitURN ());
		msg.setDamageTaken (tu.getDamageTaken ());
		msg.setExperience (getUnitUtils ().getBasicSkillValue (tu.getUnitHasSkill (), CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE));

		// Check which players can see the unit
		// Note it isn't enough to say "Is the Unit URN in the player's memory" - maybe they've seen the unit before and are remembering
		// what they saw, but cannot see it now - in that case they shouldn't receive any updates about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			if (canSeeUnitMidTurn (tu, trueTerrain, thisPlayer, db, fogOfWarSettings))
			{
				// Update player's memory on server
				final MemoryUnit mu = getUnitUtils ().findUnitURN (tu.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "updatePlayerMemoryOfUnit_DamageTakenAndExperience");
				mu.setDamageTaken (msg.getDamageTaken ());
				getUnitUtils ().setBasicSkillValue (mu, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, msg.getExperience ());

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
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final FogOfWarSettingData fogOfWarSettings)
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
			if (canSeeUnitMidTurn (tu, trueTerrain, thisPlayer, db, fogOfWarSettings))
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
	 * @param trueUnits True list of units to heal/gain experience
	 * @param onlyOnePlayerID If zero, will heal/exp units belonging to all players; if specified will heal/exp only units belonging to the specified player
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
	public final void healUnitsAndGainExperience (final List<MemoryUnit> trueUnits, final int onlyOnePlayerID, final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db, final FogOfWarSettingData fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering healUnitsAndGainExperience: Player ID " + onlyOnePlayerID);

		for (final MemoryUnit thisUnit : trueUnits)
			if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && ((onlyOnePlayerID == 0) || (onlyOnePlayerID == thisUnit.getOwningPlayerID ())))
			{
				boolean sendMsg = false;

				// Heal?
				if (thisUnit.getDamageTaken () > 0)
				{
					thisUnit.setDamageTaken (thisUnit.getDamageTaken () - 1);
					sendMsg = true;
				}

				// Experience?
				final int exp = getUnitUtils ().getBasicSkillValue (thisUnit.getUnitHasSkill (), CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
				if (exp >= 0)
				{
					getUnitUtils ().setBasicSkillValue (thisUnit, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, exp + 1);
					sendMsg = true;
				}

				// Inform any clients who know about this unit
				if (sendMsg)
					updatePlayerMemoryOfUnit_DamageTakenAndExperience (thisUnit, trueTerrain, players, db, fogOfWarSettings);
			}

		log.trace ("Exiting healUnitsAndGainExperience");
	}
	
	/**
	 * Informs clients who can see either unit of how much combat damage two units have taken - the two players in combat use this to show the animation of the attack.
	 * If the damage is enough to kill off the unit, the client will take care of this - we don't need to send a separate KillUnitMessage.
	 * 
	 * @param tuAttacker Server's true memory of unit that made the attack
	 * @param tuDefender Server's true memory of unit that got hit
	 * @param isRangedAttack True if ranged attack; False if melee
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
	public final void sendCombatDamageToClients (final MemoryUnit tuAttacker, final MemoryUnit tuDefender,
		final boolean isRangedAttack, final List<PlayerServerDetails> players, final MapVolumeOfMemoryGridCells trueTerrain,
		final ServerDatabaseEx db, final FogOfWarSettingData fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering sendCombatDamageToClients: Unit URN " + tuAttacker.getUnitURN () +
			", Unit URN " + tuDefender.getUnitURN ());

		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			
			// We might get the interesting situation where an outside observer not involved in the combat can see one
			// unit stack but not the other - and so know about one unit but not the other.
			// We handle this by setting one of the UnitURNs to zero, but this means we have to build the message separately for each client.
			final ApplyDamageMessage msg;
			if (thisPlayer.getPlayerDescription ().isHuman ())
				msg = new ApplyDamageMessage ();
			else
				msg = null;
			
			// Attacking unit
			if (canSeeUnitMidTurn (tuAttacker, trueTerrain, thisPlayer, db, fogOfWarSettings))
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
			
			// Defending unit
			if (canSeeUnitMidTurn (tuDefender, trueTerrain, thisPlayer, db, fogOfWarSettings))
			{
				// Update player's memory of defender on server
				final MemoryUnit muDefender = getUnitUtils ().findUnitURN (tuDefender.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "sendCombatDamageToClients-d");
				muDefender.setDamageTaken (tuDefender.getDamageTaken ());
				muDefender.setCombatHeading (tuDefender.getCombatHeading ());

				// Update player's memory of defender on client
				if (msg != null)
				{
					msg.setDefenderUnitURN (tuDefender.getUnitURN ());
					msg.setDefenderDamageTaken (tuDefender.getDamageTaken ());
					msg.setDefenderDirection (tuDefender.getCombatHeading ());
				}
			}
			
			// Do we have a message to send?
			if (msg != null)
				if ((msg.getAttackerUnitURN () != null) || (msg.getDefenderUnitURN () != null))
				{
					msg.setRangedAttack (isRangedAttack);
					thisPlayer.getConnection ().sendMessageToClient (msg);
				}
		}		

		log.trace ("Exiting sendCombatDamageToClients");
	}

	/**
	 * When a unit dies in combat, all the units on the opposing side gain 1 exp. 
	 * 
	 * @param combatLocation The location where the combat is taking place
	 * @param combatSide Which side is to gain 1 exp
	 * @param trueTerrain True terrain map
	 * @param trueUnits True units list
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
	public final void grantExperienceToUnitsInCombat (final MapCoordinates3DEx combatLocation, final UnitCombatSideID combatSide,
		final MapVolumeOfMemoryGridCells trueTerrain, final List<MemoryUnit> trueUnits, final List<PlayerServerDetails> players,
		final ServerDatabaseEx db, final FogOfWarSettingData fogOfWarSettings)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering grantExperienceToUnitsInCombat: " + combatLocation + ", " + combatSide);
		
		for (final MemoryUnit trueUnit : trueUnits)
			if ((trueUnit.getStatus () == UnitStatusID.ALIVE) && (combatLocation.equals (trueUnit.getCombatLocation ())) &&
				(trueUnit.getCombatSide () == combatSide) && (trueUnit.getCombatPosition () != null))
			{
				final int exp = getUnitUtils ().getBasicSkillValue (trueUnit.getUnitHasSkill (), CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);
				if (exp >= 0)
				{
					getUnitUtils ().setBasicSkillValue (trueUnit, CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE, exp+1);
					
					// This updates both the player memories on the server, and sends messages out to the clients, as needed
					updatePlayerMemoryOfUnit_DamageTakenAndExperience (trueUnit, trueTerrain, players, db, fogOfWarSettings);
				}				
			}

		log.trace ("Exiting grantExperienceToUnitsInCombat");
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
	private final void addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (final List<MemoryUnit> unitStack,
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
					msg.setData (getFogOfWarDuplication ().createAddUnitMessage (tu));
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
							msg.setData (getFogOfWarDuplication ().createAddSpellMessage (trueSpell));
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
	private final void freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (final List<Integer> unitStack, final PlayerServerDetails player)
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
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void moveUnitStackOneCellOnServerAndClients (final List<MemoryUnit> unitStack, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering moveUnitStackOneCellOnServerAndClients: " + unitStack.size () + ", Player ID " +
			unitStackOwner.getPlayerDescription ().getPlayerID () + ", " + moveFrom + ", " + moveTo);

		// We need a list of the unit URNs
		final List<Integer> unitURNList = new ArrayList<Integer> ();
		for (final MemoryUnit tu : unitStack)
			unitURNList.add (tu.getUnitURN ());
		
		// Check each player in turn
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final boolean thisPlayerCanSee;
			final boolean canSeeAfterMove;

			// If this is the player who owns the units, then obviously he can see them!
			if (thisPlayer == unitStackOwner)
			{
				thisPlayerCanSee = true;
				canSeeAfterMove = true;
			}
			else
			{
				// It isn't enough to check whether the unit URNs moving are in the player's memory - they may be
				// remembering a location that they previously saw the units at, but can't see where they're moving from/to now
				final boolean couldSeeBeforeMove = getFogOfWarCalculations ().canSeeMidTurnOnAnyPlaneIfTower
					(moveFrom, sd.getFogOfWarSetting ().getUnits (), trueMap.getMap (), priv.getFogOfWar (), db);
				canSeeAfterMove = getFogOfWarCalculations ().canSeeMidTurnOnAnyPlaneIfTower
					(moveTo, sd.getFogOfWarSetting ().getUnits (), trueMap.getMap (), priv.getFogOfWar (), db);

				// Deal with clients who could not see this unit stack before this move, but now can
				if ((!couldSeeBeforeMove) && (canSeeAfterMove))
				{
					// The unit stack doesn't exist yet in the player's memory or on the client, so before they can move, we have to send all the unit details
					addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient (unitStack, trueMap.getMaintainedSpell (), thisPlayer, db);
					thisPlayerCanSee = true;
				}

				// Can this player see the units in their current location?
				else if (couldSeeBeforeMove)
				{
					thisPlayerCanSee = true;

					// If we're losing sight of the unit stack, then we need to forget the units and any spells they have cast on them in the player's memory on the server
					// Unlike the add above, we *don't* have to do this on the client, it does it itself via the freeAfterMoving flag after it finishes displaying the animation
					if (!canSeeAfterMove)
						freeUnitStackIncludingSpellsFromServerPlayerMemoryOnly (unitURNList, thisPlayer);
				}

				// This player can't see the units before, during or after their move
				else
					thisPlayerCanSee = false;
			}

			// Any updates to make for this player?
			if (thisPlayerCanSee)
			{
				// Move units in player's memory on server; N/A if we can't see them after the move - they'd have been freed above already
				if (canSeeAfterMove)
					for (final MemoryUnit thisUnit : unitStack)
						getUnitUtils ().findUnitURN (thisUnit.getUnitURN (), priv.getFogOfWarMemory ().getUnit (), "moveUnitStackOneCellOnServerAndClients").setUnitLocation (new MapCoordinates3DEx (moveTo));
				
				// Move units on client
				if (thisPlayer.getPlayerDescription ().isHuman ())
				{
					// Create a new message each time; reusing the same message messes up unit tests because the FreeAfterMoving flag changes each time 
					final MoveUnitStackOverlandMessage movementUnitMessage = new MoveUnitStackOverlandMessage ();
					movementUnitMessage.setMoveFrom (moveFrom);
					movementUnitMessage.setMoveTo (moveTo);
					movementUnitMessage.setFreeAfterMoving (!canSeeAfterMove);

					for (final MemoryUnit tu : unitStack)
						movementUnitMessage.getUnitURN ().add (tu.getUnitURN ());
					
					thisPlayer.getConnection ().sendMessageToClient (movementUnitMessage);
				}
			}
		}

		// Move units on true map - this has to be done after updating the players' memories above so that any calls to
		// addUnitStackIncludingSpellsToServerPlayerMemoryAndSendToClient that might take place add the units at their old location, THEN show them moving to the new location
		for (final MemoryUnit thisUnit : unitStack)
			thisUnit.setUnitLocation (new MapCoordinates3DEx (moveTo));

		// See what the units can see from their new location
		getFogOfWarProcessing ().updateAndSendFogOfWar (trueMap, unitStackOwner, players, false, "moveUnitStackOneCellOnServerAndClients", sd, db);

		// If we moved out of or into a city, then need to recalc rebels, production, because the units may now be (or may now no longer be) helping ease unrest.
		// Note this doesn't deal with capturing cities - attacking even an empty city is treated as a combat, so we can pick Capture/Raze.
		final MapCoordinates3DEx [] cityLocations = new MapCoordinates3DEx [] {moveFrom, moveTo};
		for (final MapCoordinates3DEx cityLocation : cityLocations)
		{
			final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
			if ((cityData != null) && (cityData.getCityPopulation () != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityPopulation () > 0))
			{
				final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (players, cityData.getCityOwnerID (), "moveUnitStackOneCellOnServerAndClients");
				final MomPersistentPlayerPrivateKnowledge cityOwnerPriv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();

				cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels (players, trueMap.getMap (), trueMap.getUnit (), trueMap.getBuilding (),
					cityLocation, cityOwnerPriv.getTaxRateID (), db).getFinalTotal ());

				getServerCityCalculations ().ensureNotTooManyOptionalFarmers (cityData);

				updatePlayerMemoryOfCity (trueMap.getMap (), players, cityLocation, sd.getFogOfWarSetting (), false);
			}
		}

		// If we captured a monster lair, temple, etc. then remove it from the map (don't remove nodes or towers of course).
		// This is now part of movement, rather than part of cleaning up after a combat, because capturing empty lairs no longer even initiates a combat.
		// If the monsters in a lair are killed in a combat, then the attackers advance after the combat using this same routine, so that also works.
		final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
		if ((tc.getTerrainData ().getMapFeatureID () != null) &&
			(db.findMapFeature (tc.getTerrainData ().getMapFeatureID (), "moveUnitStackOneCellOnServerAndClients").getMapFeatureMagicRealm ().size () > 0) &&
			(!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ())))
		{
			log.debug ("Removing lair at " + moveTo);
			tc.getTerrainData ().setMapFeatureID (null);
			updatePlayerMemoryOfTerrain (trueMap.getMap (), players, moveTo, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
		}					
		
		// If we captured a tower of wizardry, then turn the light on
		else if (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY.equals (tc.getTerrainData ().getMapFeatureID ()))
		{
			for (final Plane plane : db.getPlane ())
			{
				final MapCoordinates3DEx towerCoords = new MapCoordinates3DEx (moveTo.getX (), moveTo.getY (), plane.getPlaneNumber ());
				log.debug ("Turning light on in tower at " + towerCoords);
				
				trueMap.getMap ().getPlane ().get (towerCoords.getZ ()).getRow ().get (towerCoords.getY ()).getCell ().get
					(towerCoords.getX ()).getTerrainData ().setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY);
				updatePlayerMemoryOfTerrain (trueMap.getMap (), players, towerCoords, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
			}
		}
		
		log.trace ("Exiting moveUnitStackOneCellOnServerAndClients");
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
	final int determineMovementDirection (final MapCoordinates3DEx moveFrom, final MapCoordinates3DEx moveTo,
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
	final void reduceMovementRemaining (final List<MemoryUnit> unitStack, final List<String> unitStackSkills, final String tileTypeID,
		final List<MemoryMaintainedSpell> spells, final ServerDatabaseEx db)
	{
		log.trace ("Entering reduceMovementRemaining: " + unitStack.size () + ", " + tileTypeID);

		for (final MemoryUnit thisUnit : unitStack)
		{
			// Don't just work out the distance it took for the whole stack to get here - if e.g. one unit can fly it might be able to
			// move into mountains taking only 1 MP whereas another unit in the same stack might take 3 MP
			final int doubleMovementCost = getServerUnitCalculations ().calculateDoubleMovementToEnterTileType (thisUnit, unitStackSkills, tileTypeID, spells, db);

			// Entirely valid for doubleMovementCost to be > doubleOverlandMovesLeft, if e.g. a spearmen with 1 MP is moving onto mountains which cost 3 MP
			if (doubleMovementCost > thisUnit.getDoubleOverlandMovesLeft ())
				thisUnit.setDoubleOverlandMovesLeft (0);
			else
				thisUnit.setDoubleOverlandMovesLeft (thisUnit.getDoubleOverlandMovesLeft () - doubleMovementCost);
		}

		log.trace ("Exiting reduceMovementRemaining");
	}

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
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void moveUnitStack (final List<MemoryUnit> unitStack, final PlayerServerDetails unitStackOwner,
		final MapCoordinates3DEx originalMoveFrom, final MapCoordinates3DEx moveTo,
		final boolean forceAsPendingMovement, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering moveUnitStack: " + unitStack.size () + ", Player ID " +
			unitStackOwner.getPlayerDescription ().getPlayerID () + ", " + originalMoveFrom.toString () + ", " + moveTo.toString ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) unitStackOwner.getPersistentPlayerPrivateKnowledge ();
		final List<String> unitStackSkills = getServerUnitCalculations ().listAllSkillsInUnitStack (unitStack, priv.getFogOfWarMemory ().getMaintainedSpell (), mom.getServerDB ());

		// Have to define a lot of these out here so they can be used after the loop
		boolean keepGoing = true;
		boolean validMoveFound = false;
		int doubleMovementRemaining = 0;
		int [] [] [] movementDirections = null;

		MapCoordinates3DEx moveFrom = originalMoveFrom;
		boolean combatInitiated = false;

		while (keepGoing)
		{
			// What's the lowest movement remaining of any unit in the stack
			doubleMovementRemaining = Integer.MAX_VALUE;
			for (final MemoryUnit thisUnit : unitStack)
				if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
					doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();

			// Find distances and route from our start point to every location on the map
			final int [] [] [] doubleMovementDistances			= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			movementDirections											= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
			final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];

			getServerUnitCalculations ().calculateOverlandMovementDistances (moveFrom.getX (), moveFrom.getY (), moveFrom.getZ (), unitStackOwner.getPlayerDescription ().getPlayerID (),
				priv.getFogOfWarMemory (), unitStack, doubleMovementRemaining,
				doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, mom.getSessionDescription (), mom.getServerDB ());

			// Is there a route to where we want to go?
			validMoveFound = (doubleMovementDistances [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] >= 0);

			// Make 1 move as long as there is a valid move, and we're not allocating movement in a simultaneous turns game
			if ((validMoveFound) && (!forceAsPendingMovement))
			{
				// Get the direction to make our 1 move in
				final int movementDirection = determineMovementDirection (moveFrom, moveTo, movementDirections, mom.getSessionDescription ().getMapSize ());

				// Work out where this moves us to
				final MapCoordinates3DEx oneStep = new MapCoordinates3DEx (moveFrom);
				getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getMapSize (), oneStep, movementDirection);

				final MemoryGridCell oneStepTrueTile = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(oneStep.getZ ()).getRow ().get (oneStep.getY ()).getCell ().get (oneStep.getX ());

				// Does this initiate a combat?
				combatInitiated = movingHereResultsInAttack [oneStep.getZ ()] [oneStep.getY ()] [oneStep.getX ()];

				// Update the movement remaining for each unit
				if (!combatInitiated)
					reduceMovementRemaining (unitStack, unitStackSkills, oneStepTrueTile.getTerrainData ().getTileTypeID (), priv.getFogOfWarMemory ().getMaintainedSpell (), mom.getServerDB ());
				else
				{
					// Attacking uses up all movement
					for (final MemoryUnit thisUnit : unitStack)
						thisUnit.setDoubleOverlandMovesLeft (0);
				}

				// Tell the client how much movement each unit has left, while we're at it recheck the lowest movement remaining of anyone in the stack
				final UpdateOverlandMovementRemainingMessage movementRemainingMsg = new UpdateOverlandMovementRemainingMessage ();
				doubleMovementRemaining = Integer.MAX_VALUE;

				for (final MemoryUnit thisUnit : unitStack)
				{
					final UpdateOverlandMovementRemainingUnit msgUnit = new UpdateOverlandMovementRemainingUnit ();
					msgUnit.setUnitURN (thisUnit.getUnitURN ());
					msgUnit.setDoubleMovesLeft (thisUnit.getDoubleOverlandMovesLeft ());
					movementRemainingMsg.getUnit ().add (msgUnit);

					if (thisUnit.getDoubleOverlandMovesLeft () < doubleMovementRemaining)
						doubleMovementRemaining = thisUnit.getDoubleOverlandMovesLeft ();
				}
				unitStackOwner.getConnection ().sendMessageToClient (movementRemainingMsg);

				// Make our 1 movement?
				if (!combatInitiated)
				{
					// Adjust move to plane if moving onto a tower
					if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (oneStepTrueTile.getTerrainData ()))
						oneStep.setZ (0);

					// Actually move the units
					moveUnitStackOneCellOnServerAndClients (unitStack, unitStackOwner, moveFrom, oneStep, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());

					// Prepare for next loop
					moveFrom = oneStep;
				}
			}

			// Check whether to loop again
			keepGoing = (!forceAsPendingMovement) && (validMoveFound) && (!combatInitiated) && (doubleMovementRemaining > 0) &&
				((moveFrom.getX () != moveTo.getX ()) || (moveFrom.getY () != moveTo.getY ()));
		}

		// If the unit stack failed to reach its destination this turn, create a pending movement object so they'll continue their movement next turn
		if ((!combatInitiated) && ((moveFrom.getX () != moveTo.getX ()) || (moveFrom.getY () != moveTo.getY ())))
		{
			// Unless ForceAsPendingMovement is on, we'll have made at least one move so should recalc the
			// best path again based on what else we learned about the terrain in our last move
			if (!forceAsPendingMovement)
			{
				final int [] [] [] doubleMovementDistances			= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
				movementDirections											= new int [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
				final boolean [] [] [] canMoveToInOneTurn			= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];
				final boolean [] [] [] movingHereResultsInAttack	= new boolean [mom.getServerDB ().getPlane ().size ()] [mom.getSessionDescription ().getMapSize ().getHeight ()] [mom.getSessionDescription ().getMapSize ().getWidth ()];

				getServerUnitCalculations ().calculateOverlandMovementDistances (moveFrom.getX (), moveFrom.getY (), moveFrom.getZ (), unitStackOwner.getPlayerDescription ().getPlayerID (),
					priv.getFogOfWarMemory (), unitStack, doubleMovementRemaining,
					doubleMovementDistances, movementDirections, canMoveToInOneTurn, movingHereResultsInAttack, mom.getSessionDescription (), mom.getServerDB ());

				validMoveFound = (doubleMovementDistances [moveTo.getZ ()] [moveTo.getY ()] [moveTo.getX ()] >= 0);
			}

			if (validMoveFound)
			{
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) unitStackOwner.getTransientPlayerPrivateKnowledge ();
				final PendingMovement pending = new PendingMovement ();
				pending.setMoveFrom (moveFrom);
				pending.setMoveTo (moveTo);

				for (final MemoryUnit thisUnit : unitStack)
					pending.getUnitURN ().add (thisUnit.getUnitURN ());

				// Record the movement path
				final MapCoordinates3DEx coords = new MapCoordinates3DEx (moveTo);
				while ((coords.getX () != moveFrom.getX () || (coords.getY () != moveFrom.getY ())))
				{
					final int direction = movementDirections [coords.getZ ()] [coords.getY ()] [coords.getX ()];

					pending.getPath ().add (direction);

					if (!getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getMapSize (), coords, getCoordinateSystemUtils ().normalizeDirection
						(mom.getSessionDescription ().getMapSize ().getCoordinateSystemType (), direction + 4)))
						
						throw new MomException ("moveUnitStack: Server map tracing moved to a cell off the map");
				}

				trans.getPendingMovement ().add (pending);

				// Send the pending movement to the client
				final PendingMovementMessage pendingMsg = new PendingMovementMessage ();
				pendingMsg.setPendingMovement (pending);
				unitStackOwner.getConnection ().sendMessageToClient (pendingMsg);
			}
		}

		// Deal with any combat initiated
		if (!combatInitiated)
		{
			// No combat, so tell the client to ask for the next unit to move
			unitStackOwner.getConnection ().sendMessageToClient (new SelectNextUnitToMoveOverlandMessage ());
		}
		else
		{
			// What plane will the monsters/defenders be on?
			final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(moveTo.getZ ()).getRow ().get (moveTo.getY ()).getCell ().get (moveTo.getX ());
			
			final int towerPlane;
			if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (tc.getTerrainData ()))
				towerPlane = 0;
			else
				towerPlane = moveTo.getZ ();
			
			// Find a defending unit
			final MemoryUnit defUnit = getUnitUtils ().findFirstAliveEnemyAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), moveTo.getX (), moveTo.getY (), towerPlane, 0);
			final PlayerServerDetails defPlayer = (defUnit == null) ? null : getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), defUnit.getOwningPlayerID (), "moveUnitStack-DP");
			
			// Do we need to send a unit ID and/or player with the combat setup?
			final PlayerServerDetails playerBeingAttacked;
			if (defUnit != null)
			{
				// Player unit stack or defended city
				playerBeingAttacked = defPlayer;
			}
			else
			{
				// We're attacking *something* or typeOfCombatInitiated would not have been set to YES, yet there's no units, so it must be an unoccupied city
				// in which case the defending player is the city owner
				if ((tc.getCityData () == null) || (tc.getCityData ().getCityOwnerID () == null) || (tc.getCityData ().getCityPopulation () == null) ||
					(tc.getCityData ().getCityPopulation () <= 0))
					
					throw new MomException ("moveUnitStack has combat set to yes but isn't attacking an empty city");
				
				playerBeingAttacked = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), tc.getCityData ().getCityOwnerID (), "moveUnitStack-CO");
			}
			
			// Scheduled the combat or start it immediately
			final MapCoordinates3DEx defendingLocation = new MapCoordinates3DEx (moveTo.getX (), moveTo.getY (), towerPlane);

			final List<Integer> attackingUnitURNs = new ArrayList<Integer> ();
			for (final MemoryUnit tu : unitStack)
				attackingUnitURNs.add (tu.getUnitURN ());

			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
			{
				getCombatScheduler ().addScheduledCombatGeneratedURN (mom.getGeneralServerKnowledge (),
					defendingLocation, moveFrom, playerBeingAttacked, unitStackOwner, attackingUnitURNs);
			}
			else
			{
				getCombatProcessing ().initiateCombat (defendingLocation, moveFrom, null, unitStackOwner, attackingUnitURNs, mom);
			}
		}

		log.trace ("Exiting moveUnitStack");
	}

	/**
	 * @return Single cell FOW calculations
	 */
	public final MomFogOfWarCalculations getFogOfWarCalculations ()
	{
		return fogOfWarCalculations;
	}

	/**
	 * @param calc Single cell FOW calculations
	 */
	public final void setFogOfWarCalculations (final MomFogOfWarCalculations calc)
	{
		fogOfWarCalculations = calc;
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
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}
	
	/**
	 * @return Unit calculations
	 */
	public final MomUnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final MomUnitCalculations calc)
	{
		unitCalculations = calc;
	}
	
	/**
	 * @return City calculations
	 */
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
	{
		cityCalculations = calc;
	}

	/**
	 * @return Server-only city calculations
	 */
	public final MomServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final MomServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}

	/**
	 * @return Server-only unit calculations
	 */
	public final MomServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final MomServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
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
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
	}

	/**
	 * @return Simultaneous turns combat scheduler
	 */
	public final CombatScheduler getCombatScheduler ()
	{
		return combatScheduler;
	}

	/**
	 * @param scheduler Simultaneous turns combat scheduler
	 */
	public final void setCombatScheduler (final CombatScheduler scheduler)
	{
		combatScheduler = scheduler;
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
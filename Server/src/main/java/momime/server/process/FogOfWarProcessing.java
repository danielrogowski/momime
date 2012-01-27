package momime.server.process;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.FogOfWarValue;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.MemoryCombatAreaEffectUtils;
import momime.common.messages.MemoryGridCellUtils;
import momime.common.messages.MemoryMaintainedSpellUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.AddBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_4.AddCombatAreaEffectMessageData;
import momime.common.messages.servertoclient.v0_9_4.AddMaintainedSpellMessageData;
import momime.common.messages.servertoclient.v0_9_4.AddUnitMessageData;
import momime.common.messages.servertoclient.v0_9_4.CancelCombatAreaEffectMessageData;
import momime.common.messages.servertoclient.v0_9_4.DestroyBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_4.FogOfWarStateMessageData;
import momime.common.messages.servertoclient.v0_9_4.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessageData;
import momime.common.messages.servertoclient.v0_9_4.SwitchOffMaintainedSpellMessageData;
import momime.common.messages.servertoclient.v0_9_4.UpdateCityMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateCityMessageData;
import momime.common.messages.servertoclient.v0_9_4.UpdateNodeLairTowerUnitIDMessageData;
import momime.common.messages.servertoclient.v0_9_4.UpdateTerrainMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateTerrainMessageData;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.calculations.MomFogOfWarCalculations;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Plane;
import momime.server.messages.ServerMemoryGridCellUtils;
import momime.server.utils.CombatAreaEffectServerUtils;
import momime.server.utils.CompareUtils;
import momime.server.utils.MaintainedSpellServerUtils;
import momime.server.utils.UnitServerUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates;
import com.ndg.map.SquareMapDirection;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Methods for processing and updating fog of war
 */
public final class FogOfWarProcessing
{
	/**
	 * Copies all the terrain and node aura related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	final static boolean copyTerrainAndNodeAura (final MemoryGridCell source, final MemoryGridCell destination)
	{
		final OverlandMapTerrainData sourceData = source.getTerrainData ();
		OverlandMapTerrainData destinationData = destination.getTerrainData ();

		final boolean updateRequired = (destinationData == null) ||
			(!CompareUtils.safeStringCompare (sourceData.getTileTypeID (), destinationData.getTileTypeID ())) ||
			(!CompareUtils.safeStringCompare (sourceData.getMapFeatureID (), destinationData.getMapFeatureID ())) ||
			(!CompareUtils.safeStringCompare (sourceData.getRiverDirections (), destinationData.getRiverDirections ())) ||
			(!CompareUtils.safeIntegerCompare (sourceData.getNodeOwnerID (), destinationData.getNodeOwnerID ()));

		if (updateRequired)
		{
			if (destinationData == null)
			{
				destinationData = new OverlandMapTerrainData ();
				destination.setTerrainData (destinationData);
			}

			destinationData.setTileTypeID (sourceData.getTileTypeID ());
			destinationData.setMapFeatureID (sourceData.getMapFeatureID ());
			destinationData.setRiverDirections (sourceData.getRiverDirections ());
			destinationData.setNodeOwnerID (sourceData.getNodeOwnerID ());
		}

		return updateRequired;
	}

	/**
	 * Wipes all memory of the terrain at this location
	 * @param destination Map cell from player's memorized map
	 * @return True if an actual update was made; false if the player already knew nothing
	 */
	final static boolean blankTerrainAndNodeAura (final MemoryGridCell destination)
	{
		final OverlandMapTerrainData destinationData = destination.getTerrainData ();

		final boolean updateRequired = (destinationData != null) &&
			((destinationData.getTileTypeID () != null) || (destinationData.getMapFeatureID () != null) ||
			 (destinationData.getRiverDirections () != null) || (destinationData.getNodeOwnerID () != null));

		destination.setTerrainData (null);

		return updateRequired;
	}

	/**
	 * Copies all the city related data items from source to destination
	 * @param source The map cell to copy from
	 * @param destination The map cell to copy to
	 * @param includeCurrentlyConstructing Whether to copy currentlyConstructing from source to destination or null it out
	 * @return Whether any update actually happened (i.e. false if source and destination already had the same info)
	 */
	final static boolean copyCityData (final MemoryGridCell source, final MemoryGridCell destination, final boolean includeCurrentlyConstructing)
	{
		// Careful, may not even be a city here and hence source.getCityData () may be null
		// That's a valid scenario - maybe last time we saw this location there was a city here, but since then someone captured and razed it
		// In that case we're better to use the other routine
		final boolean updateRequired;

		final OverlandMapCityData sourceData = source.getCityData ();
		if (sourceData == null)
			updateRequired = blankCityData (destination);
		else
		{
			OverlandMapCityData destinationData = destination.getCityData ();

			final String newCurrentlyConstructing;
			if (includeCurrentlyConstructing)
				newCurrentlyConstructing = source.getCityData ().getCurrentlyConstructingBuildingOrUnitID ();
			else
				newCurrentlyConstructing = null;

			updateRequired = (destinationData == null) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getCityPopulation (), destinationData.getCityPopulation ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getMinimumFarmers (),  destinationData.getMinimumFarmers ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getOptionalFarmers (), destinationData.getOptionalFarmers ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getNumberOfRebels (), destinationData.getNumberOfRebels ())) ||
				(!CompareUtils.safeIntegerCompare (sourceData.getCityOwnerID (), destinationData.getCityOwnerID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCityRaceID (), destinationData.getCityRaceID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCitySizeID (), destinationData.getCitySizeID ())) ||
				(!CompareUtils.safeStringCompare (sourceData.getCityName (), destinationData.getCityName ())) ||
				(!CompareUtils.safeStringCompare (newCurrentlyConstructing, destinationData.getCurrentlyConstructingBuildingOrUnitID ()));

			if (updateRequired)
			{
				if (destinationData == null)
				{
					destinationData = new OverlandMapCityData ();
					destination.setCityData (destinationData);
				}

				destinationData.setCityPopulation (sourceData.getCityPopulation ());
				destinationData.setMinimumFarmers (sourceData.getMinimumFarmers ());
				destinationData.setOptionalFarmers (sourceData.getOptionalFarmers ());
				destinationData.setNumberOfRebels (sourceData.getNumberOfRebels ());
				destinationData.setCityOwnerID (sourceData.getCityOwnerID ());
				destinationData.setCityRaceID (sourceData.getCityRaceID ());
				destinationData.setCitySizeID (sourceData.getCitySizeID ());
				destinationData.setCityName (sourceData.getCityName ());
				destinationData.setCurrentlyConstructingBuildingOrUnitID (newCurrentlyConstructing);
			}
		}

		return updateRequired;
	}

	/**
	 * Wipes all memory of the city at this location
	 * @param destination Map cell from player's memorized map
	 * @return True if an actual update was made; false if the player already knew nothing
	 */
	final static boolean blankCityData (final MemoryGridCell destination)
	{
		final OverlandMapCityData destinationData = destination.getCityData ();

		final boolean updateRequired = (destinationData != null) &&
			((destinationData.getCityPopulation () != null) || (destinationData.getMinimumFarmers () != null) || (destinationData.getOptionalFarmers () != null) ||
			 (destinationData.getNumberOfRebels () != null) || (destinationData.getCityOwnerID () != null) || (destinationData.getCityRaceID () != null) ||
			 (destinationData.getCitySizeID () != null) || (destinationData.getCityName () != null) || (destinationData.getCurrentlyConstructingBuildingOrUnitID () != null));

		destination.setCityData (null);

		return updateRequired;
	}

	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the terrain that has been updated
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public final static void updatePlayerMemoryOfTerrain (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final OverlandMapCoordinates coords, final MomSessionDescription sd, final Logger debugLogger)
		throws JAXBException, XMLStreamException
	{
		debugLogger.entering (FogOfWarProcessing.class.getName (), "updatePlayerMemoryOfTerrain", CoordinatesUtils.overlandMapCoordinatesToString (coords));

		final MemoryGridCell tc = trueTerrain.getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

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
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

			if (MomFogOfWarCalculations.canSeeMidTurn (state, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ()))
			{
				// Update player's memory on server
				final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
				if (copyTerrainAndNodeAura (tc, mc))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
						thisPlayer.getConnection ().sendMessageToClient (terrainMsgContainer);
			}
		}

		debugLogger.exiting (FogOfWarProcessing.class.getName (), "updatePlayerMemoryOfTerrain");
	}

	/**
	 * After setting the various terrain values in the True Map, this routine copies and sends the new value to players who can see it
	 * i.e. the caller must update the True Map value themselves before calling this
	 *
	 * @param trueTerrain True terrain map
	 * @param players List of players in the session
	 * @param coords Location of the city that has been updated
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	public final static void updatePlayerMemoryOfCity (final MapVolumeOfMemoryGridCells trueTerrain,
		final List<PlayerServerDetails> players, final OverlandMapCoordinates coords, final MomSessionDescription sd, final Logger debugLogger)
		throws JAXBException, XMLStreamException
	{
		debugLogger.entering (FogOfWarProcessing.class.getName (), "updatePlayerMemoryOfCity", CoordinatesUtils.overlandMapCoordinatesToString (coords));

		final MemoryGridCell tc = trueTerrain.getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

		// First build the message
		final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
		cityMsg.setMapLocation (coords);

		final UpdateCityMessage cityMsgContainer = new UpdateCityMessage ();
		cityMsgContainer.setData (cityMsg);

		// Check which players can see the city
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

			if (MomFogOfWarCalculations.canSeeMidTurn (state, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ()))
			{
				// Update player's memory on server
				final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (coords.getPlane ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());

				final boolean includeCurrentlyConstructing;
				if (tc.getCityData () == null)
					includeCurrentlyConstructing = false;
				else
					includeCurrentlyConstructing = (thisPlayer.getPlayerDescription ().getPlayerID () == tc.getCityData ().getCityOwnerID ()) ||
						(sd.getFogOfWarSetting ().isSeeEnemyCityConstruction ());

				if (copyCityData (tc, mc, includeCurrentlyConstructing))

					// Update player's memory on client
					if (thisPlayer.getPlayerDescription ().isHuman ())
					{
						// Note unlike the terrain msg which we can build once and send to each applicable player, the data for the city msg
						// needs to be reset for each player, since their visibility of the currentlyConstructing value may be different
						cityMsg.setCityData (mc.getCityData ());
						thisPlayer.getConnection ().sendMessageToClient (cityMsgContainer);
					}
			}
		}

		debugLogger.exiting (FogOfWarProcessing.class.getName (), "updatePlayerMemoryOfCity");
	}

	/**
	 * Marks that we can see a particular cell
	 * @param fogOfWarArea Player's fog of war area
	 * @param x X coordinate of map cell to update
	 * @param y Y coordinate of map cell to update
	 * @param plane Plane of map cell to update
	 */
	final static void canSee (final MapVolumeOfFogOfWarStates fogOfWarArea, final int x, final int y, final int plane)
	{
		final List<FogOfWarStateID> row = fogOfWarArea.getPlane ().get (plane).getRow ().get (y).getCell ();

		switch (row.get (x))
		{
			// Never seen this cell before - now we can
			case NEVER_SEEN:
				row.set (x, FogOfWarStateID.TEMP_CAN_NOW_SEE);
				break;

			// Saw this cell once before but have only been remembering what is there ever since - now we can see it properly again
			case HAVE_SEEN:
				row.set (x, FogOfWarStateID.TEMP_CAN_NOW_SEE);
				break;

			// Could see this cell last turn and still can
			case CAN_SEE:
				row.set (x, FogOfWarStateID.TEMP_CAN_STILL_SEE);
				break;
		}
	}

	/**
	 * Marks that we can see all cells within a particular radius
	 * @param fogOfWarArea Player's fog of war area
	 * @param sys Overland map coordinate system
	 * @param x X coordinate of map cell to update
	 * @param y Y coordinate of map cell to update
	 * @param plane Plane of map cell to update
	 * @param radius Visible radius (negative = do nothing, 0 = this cell only, 1 = 1 ring around this cell, and so on)
	 */
	final static void canSeeRadius (final MapVolumeOfFogOfWarStates fogOfWarArea, final CoordinateSystem sys, final int x, final int y, final int plane, final int radius)
	{
		// First the centre square }
		canSee (fogOfWarArea, x, y, plane);

		// Then around the each square 'ring'
		final MapCoordinates coords = new MapCoordinates ();
		coords.setX (x);
		coords.setY (y);

		for (int ringNumber = 1; ringNumber <= radius; ringNumber++)
		{
			// Move down-left
			CoordinateSystemUtils.moveCoordinates (sys, coords, SquareMapDirection.SOUTHWEST.getDirectionID ());

			// Go around the ring
			for (int directionChk = 0; directionChk < 4; directionChk++)
			{
				final int d = (directionChk * 2) + 1;
				for (int traceSide = 0; traceSide < ringNumber * 2; traceSide++)
					if (CoordinateSystemUtils.moveCoordinates (sys, coords, d))
						canSee (fogOfWarArea, coords.getX (), coords.getY (), plane);
			}
		}
	}

	/**
	 * This is used in the middle of the updateAndSendFogOfWar () method, while the player's FOW area is set to the 5 state values
	 * indicating not only what the player can see but how the area they can see has changed during this call.
	 *
	 * It adjusts fog of war values depending on the options chosen on the new game form in order to work out what action
	 * we need to take for updating the players' memory of this location, if any.
	 *
	 * Note this *doesn't* need to worry about "what if the map cell, list of buildings, etc. has changed since last turn" - those
	 * types of changes are dealt with by all the other methods in on MomTrueMap - this only needs to deal with
	 * working out updates when the visible area changes, but the true values remain the same
	 *
	 * @param state The visibility of a particular map cell part way through a visible area change update
	 * @param setting FOW setting applicable for what we're testing whether we can see (e.g. use unit value to test if we can see a unit)
	 * @return Action the server needs to take to update the players' memory when the area the player can see changes
	 */
	final static FogOfWarUpdateAction determineVisibleAreaChangedUpdateAction (final FogOfWarStateID state, final FogOfWarValue setting)
	{
		final FogOfWarUpdateAction action;

		// This is basically the ADJUST_FOG_OF_WAR array in FogOfWarArea.pas, except that all the values
		// which don't result in an update being performed have all been merged into one NONE value

		// If we've just gained visibility of a cell then we get to see an updated copy of it regardless of our FOW setting
		if (state == FogOfWarStateID.TEMP_CAN_NOW_SEE)
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE;		// = fowCanNowSee in the Delphi code

		// If the middle of updateAndSendFogOfWar (), FOG_OF_WAR_CAN_SEE means *COULD* see but no longer can
		// So if the FOW setting is to forget what was seen then do so
		else if ((state == FogOfWarStateID.CAN_SEE) && (setting == FogOfWarValue.FORGET))
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET;		// = fowCanSee in the Delphi code

		// We can continually see the location, and so whenever any value there changes the other routines in MomTrueMap will update the player's memory of the change, so nothing to do here
		// No actual update required - however needToAddUnitOnClient () and needToRemoveUnitFromClient () need this
		else if ((state == FogOfWarStateID.TEMP_CAN_STILL_SEE) ||
			((setting == FogOfWarValue.ALWAYS_SEE_ONCE_SEEN) && (state != FogOfWarStateID.NEVER_SEEN)))

			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_ALREADY_VISIBLE;	// = fowCanStillSee in the Delphi code

		// All other combinations mean either:
		// We've seen the location before, but because our FOW setting is not "forget", we continue to remember what we saw there but don't get a fresh update, so nothing to do here
		// We've never seen the location, so nothing to do here
		else
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE;

		return action;
	}

	/**
	 * @param unit True unit to test
	 * @param players List of players in the session
	 * @param trueTerrain True terrain map
	 * @param fogOfWarArea Area the player can/can't see, during FOW recalc
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @return True if we need to add this unit to the player's memory
	 * @throws RecordNotFoundException If we can't find the player who owns the unit
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	static final boolean needToAddUnitOnClient (final MemoryUnit unit, final List<PlayerServerDetails> players,
		final MapVolumeOfMemoryGridCells trueTerrain, final MapVolumeOfFogOfWarStates fogOfWarArea,
		final ServerDatabaseLookup db, final MomSessionDescription sd)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean needToAdd;

		// Firstly we only know abouts that are alive
		if (unit.getStatus () != UnitStatusID.ALIVE)
			needToAdd = false;
		else
		{
			final OverlandMapTerrainData unitLocationTerrain = trueTerrain.getPlane ().get (unit.getUnitLocation ().getPlane ()).getRow ().get
				(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ()).getTerrainData ();

			/*
			 * For regular (i.e. player/raider) units in cities or walking around the map, this is basically
			 * needToAdd = (determineVisibleAreaChangedUpdateAction (fogOfWarArea.get (unit.getCurrentLocation ())) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)
			 *
			 * Towers of Wizardry add one complication - if the unit is standing in a Tower of Wizardry then they'll be on plane 0, but perhaps we can see the
			 * tower on plane 1... so what this breaks down to is that we need to add the unit if the tower has just come into view on ANY plane
			 * but wasn't visible on ANY plane last turn (if we scout the tower on Arcanus, then scout the same tower on Myrror, we don't want to send the same units twice)
			 *
			 * Second complication is monsters in nodes/lairs/towers - we have to specifically 'attack' the node/lair/tower in order to scout it - simply
			 * seeing the map cell isn't enough - the way we handle this is to say that the client NEVER knows about units belonging to the 'Monster' player
			 * (except rampaging monsters wandering around the map) - if the player attacks the node/lair/tower, we send them the details of the units
			 * there as we start the combat, and it loses reference to those units as soon as the combat is over }
			 *
			 * So first, see whether this unit belongs to the 'monster' player
			 */
			final PlayerServerDetails unitOwner = MultiplayerSessionServerUtils.findPlayerWithID (players, unit.getOwningPlayerID (), "needToAddUnitOnClient");
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();
			if (ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS))
			{
				// We only know about monsters if they're outside of a node/lair/tower and we can see that cell
				// NB. By virtue of them being outside of a tower, we don't have to worry about complications in handling units in towers of wizardry
				final FogOfWarStateID state = fogOfWarArea.getPlane ().get (unit.getUnitLocation ().getPlane ()).getRow ().get
					(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ());

				needToAdd = ((!ServerMemoryGridCellUtils.isNodeLairTower (unitLocationTerrain, db)) &&
					(determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getUnits ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE));
			}
			else
			{
				// Regular unit - is it in a tower of wizardy?
				if (MemoryGridCellUtils.isFeatureTowerOfWizardry (unitLocationTerrain.getMapFeatureID ()))
				{
					// Check all planes
					boolean towerJustCameIntoViewOnAtLeastOnePlane = false;
					boolean towerVisibleOnAnyPlaneLastTurn = false;

					for (final Plane plane : db.getPlanes ())
					{
						final FogOfWarStateID state = fogOfWarArea.getPlane ().get (plane.getPlaneNumber ()).getRow ().get
							(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ());

						switch (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getUnits ()))
						{
							case FOG_OF_WAR_ACTION_UPDATE:
								towerJustCameIntoViewOnAtLeastOnePlane = true;
								break;

							case FOG_OF_WAR_ACTION_FORGET:
							case FOG_OF_WAR_ACTION_ALREADY_VISIBLE:
								towerVisibleOnAnyPlaneLastTurn = true;
								break;
						}
					}

					needToAdd = (towerJustCameIntoViewOnAtLeastOnePlane) && (!towerVisibleOnAnyPlaneLastTurn);
				}
				else
				{
					// Regular unit not in a tower of wizardry, i.e. the standard scenario
					final FogOfWarStateID state = fogOfWarArea.getPlane ().get (unit.getUnitLocation ().getPlane ()).getRow ().get
						(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ());

					needToAdd = (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getUnits ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE);
				}
			}
		}

		return needToAdd;
	}

	/**
	 * Needs to be declared separately since its also used when checking for spells coming into/out of view
	 * @param unit Player's memory of unit to test
	 * @param players List of players in the session
	 * @param trueTerrain True terrain map
	 * @param fogOfWarArea Area the player can/can't see, during FOW recalc
	 * @param db Lookup lists built over the XML database
	 * @param sessionDescription Session description
	 * @return True if we need to remove this unit from the player's memory
	 * @throws RecordNotFoundException If we can't find the player who owns the unit
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	static final boolean needToRemoveUnitFromClient (final MemoryUnit unit, final List<PlayerServerDetails> players,
		final MapVolumeOfMemoryGridCells trueTerrain, final MapVolumeOfFogOfWarStates fogOfWarArea,
		final ServerDatabaseLookup db, final MomSessionDescription sessionDescription)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean needToRemove;

		// Firstly we only know abouts that are alive
		if (unit.getStatus () != UnitStatusID.ALIVE)
			needToRemove = false;
		else
		{
			final OverlandMapTerrainData unitLocationTerrain = trueTerrain.getPlane ().get (unit.getUnitLocation ().getPlane ()).getRow ().get
				(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ()).getTerrainData ();

			/*
			 * For regular (i.e. player/raider) units in cities or walking around the map, this is basically
			 * needToRemove = (determineVisibleAreaChangedUpdateAction (fogOfWarArea.get (unit.getCurrentLocation ())) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET)
			 *
			 * Towers of Wizardry add one complication - if the unit is standing in a Tower of Wizardry then they'll be on plane 0, but perhaps we can see the
			 * tower on plane 1... so what this breaks down to is that we need to remove the unit providing we lost sight of the tower on at least one plane and cannot still see it on any other planes
			 *
			 * Second complication is monsters in nodes/lairs/towers - we have to specifically 'attack' the node/lair/tower in order to scout it - simply
			 * seeing the map cell isn't enough - the way we handle this is to say that the client NEVER knows about units belonging to the 'Monster' player
			 * (except rampaging monsters wandering around the map) - if the player attacks the node/lair/tower, we send them the details of the units
			 * there as we start the combat, and it loses reference to those units as soon as the combat is over
			 *
			 * So first, see whether this unit belongs to the 'monster' player
			 */
			final PlayerServerDetails unitOwner = MultiplayerSessionServerUtils.findPlayerWithID (players, unit.getOwningPlayerID (), "needToRemoveUnitFromClient");
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();
			if (ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS))
			{
				// We only know about monsters if they're outside of a node/lair/tower and we can see that cell
				// NB. By virtue of them being outside of a tower, we don't have to worry about complications in handling units in towers of wizardry
				final FogOfWarStateID state = fogOfWarArea.getPlane ().get (unit.getUnitLocation ().getPlane ()).getRow ().get
					(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ());

				needToRemove = ((!ServerMemoryGridCellUtils.isNodeLairTower (unitLocationTerrain, db)) &&
					(determineVisibleAreaChangedUpdateAction (state, sessionDescription.getFogOfWarSetting ().getUnits ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET));
			}
			else
			{
				// Regular unit - is it in a tower of wizardy?
				if (MemoryGridCellUtils.isFeatureTowerOfWizardry (unitLocationTerrain.getMapFeatureID ()))
				{
					// Check all planes
					boolean towerJustGoneOutOfSightOnAtLeastOnePlane = false;
					boolean towerVisibleOnAnyPlane = false;

					for (final Plane plane : db.getPlanes ())
					{
						final FogOfWarStateID state = fogOfWarArea.getPlane ().get (plane.getPlaneNumber ()).getRow ().get
							(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ());

						switch (determineVisibleAreaChangedUpdateAction (state, sessionDescription.getFogOfWarSetting ().getUnits ()))
						{
							case FOG_OF_WAR_ACTION_FORGET:
								towerJustGoneOutOfSightOnAtLeastOnePlane = true;
								break;

							case FOG_OF_WAR_ACTION_ALREADY_VISIBLE:
							case FOG_OF_WAR_ACTION_UPDATE:
								towerVisibleOnAnyPlane = true;
								break;
						}
					}

					needToRemove = (towerJustGoneOutOfSightOnAtLeastOnePlane) && (!towerVisibleOnAnyPlane);
				}
				else
				{
					// Regular unit not in a tower of wizardry, i.e. the standard scenario
					final FogOfWarStateID state = fogOfWarArea.getPlane ().get (unit.getUnitLocation ().getPlane ()).getRow ().get
						(unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ());

					needToRemove = (determineVisibleAreaChangedUpdateAction (state, sessionDescription.getFogOfWarSetting ().getUnits ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET);
				}
			}
		}

		return needToRemove;
	}

	/**
	 * @param spell True spell to test
	 * @param players List of players in the session
	 * @param playerMemorySpells Player's memory of maintained spells
	 * @param trueUnits True list of units
	 * @param trueTerrain True terrain map
	 * @param fogOfWarArea Area the player can/can't see, during FOW recalc
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True if we need to add this spell to the player's memory
	 * @throws RecordNotFoundException If we can't find the player who owns the unit
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	static final boolean needToAddSpellOnClient (final MemoryMaintainedSpell spell, final List<PlayerServerDetails> players, final List<MemoryMaintainedSpell> playerMemorySpells,
		final List<MemoryUnit> trueUnits, final MapVolumeOfMemoryGridCells trueTerrain, final MapVolumeOfFogOfWarStates fogOfWarArea,
		final ServerDatabaseLookup db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean needToAdd;

		// Unit spell
		if (spell.getUnitURN () != null)
		{
			final MemoryUnit unit = UnitUtils.findUnitURN (spell.getUnitURN (), trueUnits, "needToAddSpellOnClient", debugLogger);
			needToAdd = needToAddUnitOnClient (unit, players, trueTerrain, fogOfWarArea, db, sd);
		}

		// Location (city or combat) spell
		// Must check here that we don't already have the spell - we could have scouted a city, seen the spells there, lost and then regained sight of it
		else if (spell.getCityLocation () != null)
		{
			final FogOfWarStateID state = fogOfWarArea.getPlane ().get (spell.getCityLocation ().getPlane ()).getRow ().get (spell.getCityLocation ().getY ()).getCell ().get (spell.getCityLocation ().getX ());

			needToAdd = (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
				(MemoryMaintainedSpellUtils.findMaintainedSpell (playerMemorySpells, spell.getCastingPlayerID (), spell.getSpellID (), spell.getUnitURN (), spell.getUnitSkillID (), spell.getCityLocation (), spell.getCitySpellEffectID (), debugLogger) == null);
		}

		// Global enchantment - would have known about it last turn
		else
			needToAdd = false;

		return needToAdd;
	}

	/**
	 * @param spell Player's memory of spell to test
	 * @param players List of players in the session
	 * @param playerMemoryUnits Player's memory of all units
	 * @param trueSpells True list of maintained spells
	 * @param trueTerrain True terrain map
	 * @param fogOfWarArea Area the player can/can't see, during FOW recalc
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True if we need to remove this spell from the player's memory
	 * @throws RecordNotFoundException If we can't find the player who owns the unit
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	static final boolean needToRemoveSpellFromClient (final MemoryMaintainedSpell spell, final List<PlayerServerDetails> players, final List<MemoryUnit> playerMemoryUnits,
		final List<MemoryMaintainedSpell> trueSpells, final MapVolumeOfMemoryGridCells trueTerrain, final MapVolumeOfFogOfWarStates fogOfWarArea,
		final ServerDatabaseLookup db, final MomSessionDescription sd, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException
	{
		final boolean needToRemove;

		// Unit spell
		if (spell.getUnitURN () != null)
		{
			// Careful - future code in needToRemoveUnitFromClient likely will require that we pass in the player's memory of where they last remember the unit being, not the true version
			final MemoryUnit unit = UnitUtils.findUnitURN (spell.getUnitURN (), playerMemoryUnits, "needToRemoveSpellFromClient", debugLogger);
			needToRemove = needToRemoveUnitFromClient (unit, players, trueTerrain, fogOfWarArea, db, sd);
		}

		// Location (city or combat) spell
		// This can happen one of two ways:
		// 1) We need to forget what's at that location (FOW setting = forget, and we've walked away)
		// 2) We've remembered the spells that used to be at a city, but in between scouting it last time and it coming into view now, some of those spells have been cancelled (i.e. no longer in true spell list)
		else if (spell.getCityLocation () != null)
		{
			final FogOfWarStateID state = fogOfWarArea.getPlane ().get (spell.getCityLocation ().getPlane ()).getRow ().get (spell.getCityLocation ().getY ()).getCell ().get (spell.getCityLocation ().getX ());

			needToRemove = (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) &&
				(MemoryMaintainedSpellUtils.findMaintainedSpell (trueSpells, spell.getCastingPlayerID (), spell.getSpellID (), spell.getUnitURN (), spell.getUnitSkillID (), spell.getCityLocation (), spell.getCitySpellEffectID (), debugLogger) == null);
		}

		// Global enchantment - never lose sight of these
		else
			needToRemove = false;

		return needToRemove;
	}

	/**
	 * @param cae True CAE to test
	 * @param playerMemoryCAEs Player's memory of all CAEs
	 * @param fogOfWarArea Area the player can/can't see, during FOW recalc
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True if we need to add this CAE to the player's memory
	 */
	static final boolean needToAddCombatAreaEffectOnClient (final MemoryCombatAreaEffect cae, final List<MemoryCombatAreaEffect> playerMemoryCAEs,
		final MapVolumeOfFogOfWarStates fogOfWarArea, final MomSessionDescription sd, final Logger debugLogger)
	{
		final boolean needToAdd;

		// Global CAE - so we must have been able to see it last turn
		if (cae.getMapLocation () == null)
			needToAdd = false;
		else
		{
			// Localized CAE - can only see it if we can see the location
			// Must check here that we don't already have the CAE - we could have scouted an area, seen the CAEs there, lost and then regained sight of it
			final FogOfWarStateID state = fogOfWarArea.getPlane ().get (cae.getMapLocation ().getPlane ()).getRow ().get (cae.getMapLocation ().getY ()).getCell ().get (cae.getMapLocation ().getX ());

			needToAdd = (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
				(!MemoryCombatAreaEffectUtils.findCombatAreaEffect (playerMemoryCAEs, cae.getMapLocation (), cae.getCombatAreaEffectID (), cae.getCastingPlayerID (), debugLogger));
		}

		return needToAdd;
	}

	/**
	 * @param cae Player's memory of CAE to test
	 * @param trueCAEs True list of CAEs
	 * @param fogOfWarArea Area the player can/can't see, during FOW recalc
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True if we need to add this CAE to the player's memory
	 */
	static final boolean needToRemoveCombatAreaEffectFromClient (final MemoryCombatAreaEffect cae, final List<MemoryCombatAreaEffect> trueCAEs,
		final MapVolumeOfFogOfWarStates fogOfWarArea, final MomSessionDescription sd, final Logger debugLogger)
	{
		final boolean needToRemove;

		// Global CAE - can never lose sight of
		if (cae.getMapLocation () == null)
			needToRemove = false;
		else
		{
			// Localized CAE
			// This can happen one of two ways:
			// 1) We need to forget what's at that location (FOW setting = forget, and we've walked away)
			// 2) We've remembered CAEs that used to be at a city, but in between scouting it last time and it coming into view now, some of those CAEs have been cancelled (i.e. no longer in true CAE list)
			final FogOfWarStateID state = fogOfWarArea.getPlane ().get (cae.getMapLocation ().getPlane ()).getRow ().get (cae.getMapLocation ().getY ()).getCell ().get (cae.getMapLocation ().getX ());

			needToRemove = (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) &&
				(!MemoryCombatAreaEffectUtils.findCombatAreaEffect (trueCAEs, cae.getMapLocation (), cae.getCombatAreaEffectID (), cae.getCastingPlayerID (), debugLogger));
		}

		return needToRemove;
	}

	/**
	 * @param coordinateList List of coordinates to check
	 * @param coordinates Coordinates to look for
	 * @return True if coordinates are already in the list
	 */
	final static boolean areCoordinatesIncludedInMessage (final List<UpdateNodeLairTowerUnitIDMessageData> coordinateList, final OverlandMapCoordinates coordinates)
	{
		boolean result = false;
		final Iterator<UpdateNodeLairTowerUnitIDMessageData> iter = coordinateList.iterator ();
		while ((!result) && (iter.hasNext ()))
		{
			final UpdateNodeLairTowerUnitIDMessageData theseCoords = iter.next ();
			if (CoordinatesUtils.overlandMapCoordinatesEqual (theseCoords.getNodeLairTowerLocation (), coordinates))
				result = true;
		}

		return result;
	}

	/**
	 * This is THE routine which does all the work for fog of war, it:
	 * 1) Checks what the player can now see
	 * 2) Compares the area against what the player could see before
	 * 3) Checks which map cells, buildings, spells, CAEs and units the player can either now see that they couldn't see before, or now can't see that they could see before
	 * 4) Updates the server copy of their memory accordingly
	 * 5) If a human player, sends messages to the client to update their memory there accordingly
	 *
	 * Note this *doesn't* need to worry about "what if the map cell, list of buildings, etc. has changed since last turn" - those
	 * types of changes are dealt with by all the other methods in on MomTrueMap - this only needs to deal with
	 * working out updates when the visible area changes, but the true values remain the same
	 *
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param player The player whose FOW we are recalculating
	 * @param players List of players in the session
	 * @param nameCitiesAtStartOfGame Set only for the first time this is called during game startup, and tells all the clients to ask for names for their starting cities
	 * @param triggeredFrom What caused the change in visible area - this is only used for debug messages on the client
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public final static void updateAndSendFogOfWar (final FogOfWarMemory trueMap, final PlayerServerDetails player,
		final List<PlayerServerDetails> players, final boolean nameCitiesAtStartOfGame,
		final String triggeredFrom, final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		debugLogger.entering (FogOfWarProcessing.class.getName (), "updateAndSendFogOfWar", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Nature Awareness allows us to see the whole map, in which case no point checking each city or unit
		if (((sd.isDisableFogOfWar () != null) && (sd.isDisableFogOfWar ())) ||
			MemoryMaintainedSpellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (),
				ServerDatabaseValues.VALUE_SPELL_ID_NATURE_AWARENESS, null, null, null, null, debugLogger) != null)
		{
			for (final Plane plane : db.getPlanes ())
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
						canSee (priv.getFogOfWar (), x, y, plane.getPlaneNumber ());
		}
		else
		{
			// Check if we have regular Awareness cast, so we don't have to check individually for every city
			final boolean awareness = (MemoryMaintainedSpellUtils.findMaintainedSpell (trueMap.getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (),
				ServerDatabaseValues.VALUE_SPELL_ID_AWARENESS, null, null, null, null, debugLogger) != null);

			// Check what areas we can see because we have cities there
			for (final Plane plane : db.getPlanes ())
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
					{
						final OverlandMapCityData trueCity = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
						if ((trueCity != null) && (trueCity.getCityPopulation () > 0))
						{
							final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
							coords.setX (x);
							coords.setY (y);
							coords.setPlane (plane.getPlaneNumber ());

							// Our city
							if (trueCity.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ())
							{
								// Most cities can 'see' the same pattern as their resource range, but some special buildings can extend this
								// This does not handle the "Nature's Eye" spell - this is done with the spells below
								final int scoutingRange = MomServerCityCalculations.calculateCityScoutingRange (trueMap.getBuilding (), coords, db, debugLogger);
								if (scoutingRange >= 0)
									canSeeRadius (priv.getFogOfWar (), sd.getMapSize (), x, y, plane.getPlaneNumber (), scoutingRange);
								else
								{
									// Standard city pattern
									for (final SquareMapDirection direction : MomCityCalculations.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
										if (CoordinateSystemUtils.moveCoordinates (sd.getMapSize (), coords, direction.getDirectionID ()))
											canSee (priv.getFogOfWar (), coords.getX (), coords.getY (), coords.getPlane ());
								}
							}

							// Enemy city - we can see a small area around it if we either have Awareness cast or a curse cast on the city
							else if ((awareness) || (MemoryMaintainedSpellUtils.findMaintainedSpell
								(trueMap.getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), null, null, null, coords, null, debugLogger) != null))

								canSeeRadius (priv.getFogOfWar (), sd.getMapSize (), x, y, plane.getPlaneNumber (), 1);
						}
					}

			// Check what areas we can see because we have units there
			for (final MemoryUnit thisUnit : trueMap.getUnit ())
				if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()))
				{
					final int scoutingRange = MomServerUnitCalculations.calculateUnitScoutingRange
						(thisUnit, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db, debugLogger);

					// If standing in a tower, can see both planes
					if (MemoryGridCellUtils.isFeatureTowerOfWizardry (trueMap.getMap ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
						(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ().getMapFeatureID ()))
					{
						for (final Plane plane : db.getPlanes ())
							canSeeRadius (priv.getFogOfWar (), sd.getMapSize (), thisUnit.getUnitLocation ().getX (), thisUnit.getUnitLocation ().getY (), plane.getPlaneNumber (), scoutingRange);
					}
					else
						// Can see single plane only
						canSeeRadius (priv.getFogOfWar (), sd.getMapSize (), thisUnit.getUnitLocation ().getX (), thisUnit.getUnitLocation ().getY (), thisUnit.getUnitLocation ().getPlane (), scoutingRange);
				}

			// Check what areas we can see because of visibility spells
			// This is mainly for Nature's Eye, but is handled separately from cities to allow Earth Lore to be a maintained spell so it works sensibly with "Forget" FOW settings
			for (final MemoryMaintainedSpell thisSpell : trueMap.getMaintainedSpell ())
				if ((thisSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisSpell.getCityLocation () != null))
				{
					// See if this spell has a scouting range
					final Integer scoutingRange = db.findSpell (thisSpell.getSpellID (), "updateAndSendFogOfWar").getSpellScoutingRange ();
					if (scoutingRange != null)
						canSeeRadius (priv.getFogOfWar (), sd.getMapSize (), thisSpell.getCityLocation ().getX (),
							thisSpell.getCityLocation ().getY (), thisSpell.getCityLocation ().getPlane (), scoutingRange);
				}
		}

		// Start off the big message, if a human player
		final FogOfWarVisibleAreaChangedMessage msg;
		if (player.getPlayerDescription ().isHuman ())
		{
			msg = new FogOfWarVisibleAreaChangedMessage ();
			msg.setTriggeredFrom (triggeredFrom);
		}
		else
			msg = null;

		for (final Plane plane : db.getPlanes ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
					final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);

					final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
					coords.setX (x);
					coords.setY (y);
					coords.setPlane (plane.getPlaneNumber ());

					// Check for changes in the terrain
					switch (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ()))
					{
						// Couldn't see this cell before (either we'd never seen it before, or we'd seen it earlier and have been remembering it) - now we can see it
						case FOG_OF_WAR_ACTION_UPDATE:
							if (copyTerrainAndNodeAura (tc, mc))
								if (msg != null)
								{
									final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
									terrainMsg.setMapLocation (coords);
									terrainMsg.setTerrainData (mc.getTerrainData ());
									msg.getTerrainUpdate ().add (terrainMsg);
								}
							break;

						// Could see this cell before but now we need to forget what we saw
						case FOG_OF_WAR_ACTION_FORGET:
							if (blankTerrainAndNodeAura (mc))
								if (msg != null)
								{
									final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
									terrainMsg.setMapLocation (coords);
									terrainMsg.setTerrainData (mc.getTerrainData ());
									msg.getTerrainUpdate ().add (terrainMsg);
								}
							break;
					}

					// Check for changes in cities
					switch (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
					{
						// Couldn't see this cell before (either we'd never seen it before, or we'd seen it earlier and have been remembering it) - now we can see it
						case FOG_OF_WAR_ACTION_UPDATE:
							// Careful, may not even be a city here and hence tc.getCityData () may be null
							final int cityOwnerID = (tc.getCityData () == null) ? 0 : tc.getCityData ().getCityOwnerID ();

							if (copyCityData (tc, mc, (cityOwnerID == player.getPlayerDescription ().getPlayerID ()) ||
																(sd.getFogOfWarSetting ().isSeeEnemyCityConstruction ())))
								if (msg != null)
								{
									final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
									cityMsg.setMapLocation (coords);
									cityMsg.setCityData (mc.getCityData ());
									cityMsg.setAskForCityName ((nameCitiesAtStartOfGame) && (tc.getCityData ().getCityOwnerID ().equals (player.getPlayerDescription ().getPlayerID ())));
									msg.getCityUpdate ().add (cityMsg);
								}
							break;

						// Could see this cell before but now we need to forget what we saw
						case FOG_OF_WAR_ACTION_FORGET:
							if (blankCityData (mc))
								if (msg != null)
								{
									final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
									cityMsg.setMapLocation (coords);
									cityMsg.setCityData (mc.getCityData ());
									msg.getCityUpdate ().add (cityMsg);
								}
						break;
					}
				}

		// Check to see what buildings we need to add (runs down the true list of buildings)
		for (final MemoryBuilding thisBuilding : trueMap.getBuilding ())
		{
			// Cell must have just come into view, and we must not already have this building
			// (We might have previously scouted this city, then moved away, now be re-scouting it)
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisBuilding.getCityLocation ().getPlane ()).getRow ().get
				(thisBuilding.getCityLocation ().getY ()).getCell ().get (thisBuilding.getCityLocation ().getX ());

			if ((determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
				(!MemoryBuildingUtils.findBuilding (priv.getFogOfWarMemory ().getBuilding (), thisBuilding.getCityLocation (), thisBuilding.getBuildingID (), debugLogger)))
			{
				// Copy building into player's memory
				final OverlandMapCoordinates memBuildingCoords = new OverlandMapCoordinates ();
				memBuildingCoords.setX (thisBuilding.getCityLocation ().getX ());
				memBuildingCoords.setY (thisBuilding.getCityLocation ().getY ());
				memBuildingCoords.setPlane (thisBuilding.getCityLocation ().getPlane ());

				final MemoryBuilding memBuilding = new MemoryBuilding ();
				memBuilding.setBuildingID (thisBuilding.getBuildingID ());
				memBuilding.setCityLocation (memBuildingCoords);

				priv.getFogOfWarMemory ().getBuilding ().add (memBuilding);

				if (msg != null)
				{
					final AddBuildingMessageData buildingMsg = new AddBuildingMessageData ();
					buildingMsg.setFirstBuildingID (memBuilding.getBuildingID ());
					buildingMsg.setCityLocation (memBuilding.getCityLocation ());
					msg.getAddBuilding ().add (buildingMsg);
				}
			}
		}

		// Check to see what buildings we need to remove (runs down our memorized list of buildings)
		final Iterator<MemoryBuilding> buildingsIter = priv.getFogOfWarMemory ().getBuilding ().iterator ();
		while (buildingsIter.hasNext ())
		{
			final MemoryBuilding thisBuilding = buildingsIter.next ();

			// This can happen one of two ways:
			// 1) We need to forget what's at that location (FOW setting = forget, and we've walked away)
			// 2) We've remembered the buildings that used to be at a city, but in between scouting it last time and it coming into view now, some of those buildings have been destroyed
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisBuilding.getCityLocation ().getPlane ()).getRow ().get
				(thisBuilding.getCityLocation ().getY ()).getCell ().get (thisBuilding.getCityLocation ().getX ());

			final FogOfWarUpdateAction action = determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ());

			if ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) || ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
				(!MemoryBuildingUtils.findBuilding (trueMap.getBuilding (), thisBuilding.getCityLocation (), thisBuilding.getBuildingID (), debugLogger))))
			{
				if (msg != null)
				{
					final DestroyBuildingMessageData buildingMsg = new DestroyBuildingMessageData ();
					buildingMsg.setBuildingID (thisBuilding.getBuildingID ());
					buildingMsg.setCityLocation (thisBuilding.getCityLocation ());
					msg.getDestroyBuilding ().add (buildingMsg);
				}

				buildingsIter.remove ();
			}
		}

		// Check to see what units we need to add (runs down the true list of units)
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if (needToAddUnitOnClient (thisUnit, players, trueMap.getMap (), priv.getFogOfWar (), db, sd))
			{
				debugLogger.finest ("UnitURN " + thisUnit.getUnitURN () + " has come into view for player " + player.getPlayerDescription ().getPlayerID () + ", sending details as part of VAC");

				// Copy unit into player's memory
				priv.getFogOfWarMemory ().getUnit ().add (UnitServerUtils.duplicateMemoryUnit (thisUnit));

				if (msg != null)
				{
					final AddUnitMessageData unitMsg = new AddUnitMessageData ();
					unitMsg.setUnitURN (thisUnit.getUnitURN ());
					unitMsg.setOwningPlayerID (thisUnit.getOwningPlayerID ());
					unitMsg.setUnitLocation (thisUnit.getUnitLocation ());
					unitMsg.setWeaponGrade (thisUnit.getWeaponGrade ());
					unitMsg.setUnitID (thisUnit.getUnitID ());
					unitMsg.setHeroNameID (thisUnit.getHeroNameID ());

					if (db.findUnit (thisUnit.getUnitID (), "updateAndSendFogOfWar").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
					{
						// Include skills in message; experience will be included in the skills list
						unitMsg.setReadSkillsFromXML (false);
						unitMsg.getUnitHasSkill ().addAll (thisUnit.getUnitHasSkill ());
					}
					else
					{
						// Tell client to read skills from XML file; include experience
						final int experience = UnitUtils.getBasicSkillValue (thisUnit.getUnitHasSkill (), CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_EXPERIENCE);

						unitMsg.setReadSkillsFromXML (true);
						if (experience >= 0)
							unitMsg.setExperience (experience);
					}

					msg.getAddUnit ().add (unitMsg);
				}

				// If this is a unit standing on a node or tower, then that proves that the node or tower has been cleared of monsters
				final OverlandMapTerrainData terrainData = trueMap.getMap ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
					(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ();

				final String nodeLairTowerKnownUnitID = priv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
					(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ());

				if ((ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)) && (!CompareUtils.safeStringCompare (nodeLairTowerKnownUnitID, "")))	// know it to be empty
				{
					priv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
						(thisUnit.getUnitLocation ().getY ()).getCell ().set (thisUnit.getUnitLocation ().getX (), "");

					if (msg != null)
						if (!areCoordinatesIncludedInMessage (msg.getUpdateNodeLairTowerUnitID (), thisUnit.getUnitLocation ()))
						{
							final OverlandMapCoordinates scoutMsgCoords = new OverlandMapCoordinates ();
							scoutMsgCoords.setX (thisUnit.getUnitLocation ().getX ());
							scoutMsgCoords.setY (thisUnit.getUnitLocation ().getY ());
							scoutMsgCoords.setPlane (thisUnit.getUnitLocation ().getPlane ());

							final UpdateNodeLairTowerUnitIDMessageData scoutMsg = new UpdateNodeLairTowerUnitIDMessageData ();
							scoutMsg.setNodeLairTowerLocation (scoutMsgCoords);
							scoutMsg.setMonsterUnitID ("");

							msg.getUpdateNodeLairTowerUnitID ().add (scoutMsg);
						}
				}
			}

		// Check to see what units we could see but now can't (this runs down our local memory of the units)
		final Iterator<MemoryUnit> unitsIter = priv.getFogOfWarMemory ().getUnit ().iterator ();
		while (unitsIter.hasNext ())
		{
			final MemoryUnit thisUnit = unitsIter.next ();

			if (needToRemoveUnitFromClient (thisUnit, players, trueMap.getMap (), priv.getFogOfWar (), db, sd))
			{
				debugLogger.finest ("UnitURN " + thisUnit.getUnitURN () + " has gone out of view for player " + player.getPlayerDescription ().getPlayerID () + ", sending kill as part of VAC");

				if (msg != null)
				{
					final KillUnitMessageData killMsg = new KillUnitMessageData ();
					killMsg.setUnitURN (thisUnit.getUnitURN ());
					killMsg.setKillUnitActionID (KillUnitActionID.VISIBLE_AREA_CHANGED);
					msg.getKillUnit ().add (killMsg);
				}

				unitsIter.remove ();
			}
		}

		// Check to see what maintained spells we couldn't see but now can (this runs down the true list of spells)
		for (final MemoryMaintainedSpell thisSpell : trueMap.getMaintainedSpell ())
			if (needToAddSpellOnClient (thisSpell, players, priv.getFogOfWarMemory ().getMaintainedSpell (), trueMap.getUnit (), trueMap.getMap (), priv.getFogOfWar (), db, sd, debugLogger))
			{
				// Copy spell into player's memory
				priv.getFogOfWarMemory ().getMaintainedSpell ().add (MaintainedSpellServerUtils.duplicateMemoryMaintainedSpell (thisSpell));

				if (msg != null)
				{
					final AddMaintainedSpellMessageData spellMsg = new AddMaintainedSpellMessageData ();
					spellMsg.setCastingPlayerID (thisSpell.getCastingPlayerID ());
					spellMsg.setSpellID (thisSpell.getSpellID ());
					spellMsg.setUnitURN (thisSpell.getUnitURN ());
					spellMsg.setUnitSkillID (thisSpell.getUnitSkillID ());
					spellMsg.setCastInCombat (thisSpell.isCastInCombat ());
					spellMsg.setCityLocation (thisSpell.getCityLocation ());
					spellMsg.setCitySpellEffectID (thisSpell.getCitySpellEffectID ());
					msg.getAddMaintainedSpell ().add (spellMsg);
				}
			}

		// Check to see what maintained spells we could see but now can't (this runs down our local memory of the spells)
		final Iterator<MemoryMaintainedSpell> spellsIter = priv.getFogOfWarMemory ().getMaintainedSpell ().iterator ();
		while (spellsIter.hasNext ())
		{
			final MemoryMaintainedSpell thisSpell = spellsIter.next ();

			if (needToRemoveSpellFromClient (thisSpell, players, priv.getFogOfWarMemory ().getUnit (), trueMap.getMaintainedSpell (), trueMap.getMap (), priv.getFogOfWar (), db, sd, debugLogger))
			{
				if (msg != null)
				{
					final SwitchOffMaintainedSpellMessageData spellMsg = new SwitchOffMaintainedSpellMessageData ();
					spellMsg.setCastingPlayerID (thisSpell.getCastingPlayerID ());
					spellMsg.setSpellID (thisSpell.getSpellID ());
					spellMsg.setUnitURN (thisSpell.getUnitURN ());
					spellMsg.setUnitSkillID (thisSpell.getUnitSkillID ());
					spellMsg.setCastInCombat (thisSpell.isCastInCombat ());
					spellMsg.setCityLocation (thisSpell.getCityLocation ());
					spellMsg.setCitySpellEffectID (thisSpell.getCitySpellEffectID ());
					msg.getSwitchOffMaintainedSpell ().add (spellMsg);
				}

				spellsIter.remove ();
			}
		}

		// Check to see what combat area effects we couldn't see but now can (this runs down the true list of CAEs)
		for (final MemoryCombatAreaEffect thisCAE : trueMap.getCombatAreaEffect ())
			if (needToAddCombatAreaEffectOnClient (thisCAE, priv.getFogOfWarMemory ().getCombatAreaEffect (), priv.getFogOfWar (), sd, debugLogger))
			{
				// Copy spell into player's memory
				priv.getFogOfWarMemory ().getCombatAreaEffect ().add (CombatAreaEffectServerUtils.duplicateMemoryCombatAreaEffect (thisCAE));

				if (msg != null)
				{
					final AddCombatAreaEffectMessageData caeMsg = new AddCombatAreaEffectMessageData ();
					caeMsg.setCombatAreaEffectID (thisCAE.getCombatAreaEffectID ());
					caeMsg.setMapLocation (thisCAE.getMapLocation ());
					caeMsg.setCastingPlayerID (thisCAE.getCastingPlayerID ());
					msg.getAddCombatAreaEffect ().add (caeMsg);
				}
			}

		// Check to see what combat area effects we need to remove from our memory (this runs down our local memory of the CAEs)
		final Iterator<MemoryCombatAreaEffect> caeIter = priv.getFogOfWarMemory ().getCombatAreaEffect ().iterator ();
		while (spellsIter.hasNext ())
		{
			final MemoryCombatAreaEffect thisCAE = caeIter.next ();

			if (needToRemoveCombatAreaEffectFromClient (thisCAE, trueMap.getCombatAreaEffect (), priv.getFogOfWar (), sd, debugLogger))
			{
				if (msg != null)
				{
					final CancelCombatAreaEffectMessageData caeMsg = new CancelCombatAreaEffectMessageData ();
					caeMsg.setCombatAreaEffectID (thisCAE.getCombatAreaEffectID ());
					caeMsg.setMapLocation (thisCAE.getMapLocation ());
					caeMsg.setCastingPlayerID (thisCAE.getCastingPlayerID ());
					msg.getCancelCombaAreaEffect ().add (caeMsg);
				}

				spellsIter.remove ();
			}
		}

		// Lastly send the client details of the changes in the fog of war area itself
		// Also sets the values on the server back normal
		for (final Plane plane : db.getPlanes ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final List<FogOfWarStateID> row = priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ();

					final OverlandMapCoordinates coords = new OverlandMapCoordinates ();
					coords.setX (x);
					coords.setY (y);
					coords.setPlane (plane.getPlaneNumber ());

					switch (row.get (x))
					{
						// Could see this cell before - but now we can't
						case CAN_SEE:
							row.set (x, FogOfWarStateID.HAVE_SEEN);
							if (msg != null)
							{
								final FogOfWarStateMessageData fowMsg = new FogOfWarStateMessageData ();
								fowMsg.setMapLocation (coords);
								fowMsg.setState (FogOfWarStateID.HAVE_SEEN);
								msg.getFogOfWarUpdate ().add (fowMsg);
							}
							break;

						// Couldn't see this cell before (either we'd never seen it before, or we'd seen it earlier and have been remembering it) - now we can see it
						case TEMP_CAN_NOW_SEE:
							row.set (x, FogOfWarStateID.CAN_SEE);
							if (msg != null)
							{
								final FogOfWarStateMessageData fowMsg = new FogOfWarStateMessageData ();
								fowMsg.setMapLocation (coords);
								fowMsg.setState (FogOfWarStateID.CAN_SEE);
								msg.getFogOfWarUpdate ().add (fowMsg);
							}
							break;

						// Could see this cell last turn and still can - Just reset cell back to the normal value - no need to send this
						case TEMP_CAN_STILL_SEE:
							row.set (x, FogOfWarStateID.CAN_SEE);
							break;
					}
				}

		// Send the completed message
		if (msg != null)
			player.getConnection ().sendMessageToClient (msg);

		debugLogger.exiting (FogOfWarProcessing.class.getName (), "updateAndSendFogOfWar");
	}

	/**
	 * Prevent instantiation
	 */
	private FogOfWarProcessing ()
	{
	}
}

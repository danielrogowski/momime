package momime.server.fogofwar;

import java.util.ArrayList;
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
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.AddBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_4.AddCombatAreaEffectMessageData;
import momime.common.messages.servertoclient.v0_9_4.CancelCombatAreaEffectMessageData;
import momime.common.messages.servertoclient.v0_9_4.DestroyBuildingMessageData;
import momime.common.messages.servertoclient.v0_9_4.FogOfWarStateMessageData;
import momime.common.messages.servertoclient.v0_9_4.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessageData;
import momime.common.messages.servertoclient.v0_9_4.SwitchOffMaintainedSpellMessageData;
import momime.common.messages.servertoclient.v0_9_4.UpdateCityMessageData;
import momime.common.messages.servertoclient.v0_9_4.UpdateNodeLairTowerUnitIDMessageData;
import momime.common.messages.servertoclient.v0_9_4.UpdateTerrainMessageData;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.FogOfWarStateID;
import momime.common.messages.v0_9_4.MapVolumeOfFogOfWarStates;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.CompareUtils;
import momime.common.utils.IMemoryBuildingUtils;
import momime.common.utils.IMemoryCombatAreaEffectUtils;
import momime.common.utils.IMemoryGridCellUtils;
import momime.common.utils.IMemoryMaintainedSpellUtils;
import momime.common.utils.IUnitUtils;
import momime.server.calculations.IMomServerCityCalculations;
import momime.server.calculations.IMomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.Plane;
import momime.server.messages.ServerMemoryGridCellUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates;
import com.ndg.map.SquareMapDirection;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * This contains the methods that recheck what areas of the map a specific player can see, and depending which areas
 * have come into sight, or are going out of sight, and depending on the chosen FOW settings on the new game form,
 * works out all the terrain, cities, buildings, units, and so on that need to be updated or removed from the player's memory
 * and builds the appropriate update message to inform the client of those changes
 *
 * i.e. methods for when the true values remain the same but the visible area changes
 */
public class FogOfWarProcessing implements IFogOfWarProcessing
{
	/** Class logger */
	private final Logger log = Logger.getLogger (FogOfWarProcessing.class.getName ());
	
	/** FOW duplication utils */
	private IFogOfWarDuplication fogOfWarDuplication;

	/** Unit utils */
	private IUnitUtils unitUtils;
	
	/** MemoryBuilding utils */
	private IMemoryBuildingUtils memoryBuildingUtils;
	
	/** Memory CAE utils */
	private IMemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** MemoryMaintainedSpell utils */
	private IMemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** MemoryGridCell utils */
	private IMemoryGridCellUtils memoryGridCellUtils;
	
	/** Server-only city calculations */
	private IMomServerCityCalculations serverCityCalculations;
	
	/** Server-only unit calculations */
	private IMomServerUnitCalculations serverUnitCalculations;
	
	/**
	 * Marks that we can see a particular cell
	 * @param fogOfWarArea Player's fog of war area
	 * @param x X coordinate of map cell to update
	 * @param y Y coordinate of map cell to update
	 * @param plane Plane of map cell to update
	 */
	final void canSee (final MapVolumeOfFogOfWarStates fogOfWarArea, final int x, final int y, final int plane)
	{
		final List<FogOfWarStateID> row = fogOfWarArea.getPlane ().get (plane).getRow ().get (y).getCell ();

		switch (row.get (x))
		{
			// Never seen this cell before - now we can
			case NEVER_SEEN:
				row.set (x, FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME);
				break;

			// Saw this cell once before but have only been remembering what is there ever since - now we can see it properly again
			case HAVE_SEEN:
				row.set (x, FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT);
				break;

			// Could see this cell last turn and still can
			case CAN_SEE:
				row.set (x, FogOfWarStateID.TEMP_CAN_STILL_SEE);
				break;
				
			// If its any of the TEMP values, then means we can see that cell more than once, which has no further effect
			case TEMP_SEEING_IT_FOR_FIRST_TIME:
			case TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT:
			case TEMP_CAN_STILL_SEE:
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
	final void canSeeRadius (final MapVolumeOfFogOfWarStates fogOfWarArea, final CoordinateSystem sys, final int x, final int y, final int plane, final int radius)
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
	 * This calls canSee () for every cell the map that the player can see for any reason (because they have units there, cities, awareness, etc.)
	 * It is part of updateAndSendFogOfWar () but just declared separately so we can run a JUnit test against it
	 *
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param player The player whose FOW we are recalculating
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final void markVisibleArea (final FogOfWarMemory trueMap, final PlayerServerDetails player,
		final List<PlayerServerDetails> players, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		log.entering (FogOfWarProcessing.class.getName (), "markVisibleArea", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Nature Awareness allows us to see the whole map, in which case no point checking each city or unit
		if (((sd.isDisableFogOfWar () != null) && (sd.isDisableFogOfWar ())) ||
			getMemoryMaintainedSpellUtils ().findMaintainedSpell (trueMap.getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (),
				ServerDatabaseValues.VALUE_SPELL_ID_NATURE_AWARENESS, null, null, null, null) != null)
		{
			for (final Plane plane : db.getPlane ())
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
						canSee (priv.getFogOfWar (), x, y, plane.getPlaneNumber ());
		}
		else
		{
			// Check if we have regular Awareness cast, so we don't have to check individually for every city
			final boolean awareness = (getMemoryMaintainedSpellUtils ().findMaintainedSpell (trueMap.getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (),
				ServerDatabaseValues.VALUE_SPELL_ID_AWARENESS, null, null, null, null) != null);

			// Check what areas we can see because we have cities there
			for (final Plane plane : db.getPlane ())
				for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
					for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
					{
						final OverlandMapCityData trueCity = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
						if ((trueCity != null) && (trueCity.getCityPopulation () != null) && (trueCity.getCityPopulation () > 0))
						{
							final OverlandMapCoordinatesEx coords = new OverlandMapCoordinatesEx ();
							coords.setX (x);
							coords.setY (y);
							coords.setPlane (plane.getPlaneNumber ());

							// Our city
							if ((trueCity.getCityOwnerID () != null) && (trueCity.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
							{
								// Most cities can 'see' the same pattern as their resource range, but some special buildings can extend this
								// This does not handle the "Nature's Eye" spell - this is done with the spells below
								final int scoutingRange = getServerCityCalculations ().calculateCityScoutingRange (trueMap.getBuilding (), coords, db);
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
							else if ((awareness) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
								(trueMap.getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), null, null, null, coords, null) != null))

								canSeeRadius (priv.getFogOfWar (), sd.getMapSize (), x, y, plane.getPlaneNumber (), 1);
						}
					}

			// Check what areas we can see because we have units there
			for (final MemoryUnit thisUnit : trueMap.getUnit ())
				if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()))
				{
					final int scoutingRange = getServerUnitCalculations ().calculateUnitScoutingRange
						(thisUnit, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db);

					// If standing in a tower, can see both planes
					if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (trueMap.getMap ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
						(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ()))
					{
						for (final Plane plane : db.getPlane ())
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
					final Integer scoutingRange = db.findSpell (thisSpell.getSpellID (), "markVisibleArea").getSpellScoutingRange ();
					if (scoutingRange != null)
						canSeeRadius (priv.getFogOfWar (), sd.getMapSize (), thisSpell.getCityLocation ().getX (),
							thisSpell.getCityLocation ().getY (), thisSpell.getCityLocation ().getPlane (), scoutingRange);
				}
		}

		log.exiting (FogOfWarProcessing.class.getName (), "markVisibleArea");
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
	final FogOfWarUpdateAction determineVisibleAreaChangedUpdateAction (final FogOfWarStateID state, final FogOfWarValue setting)
	{
		final FogOfWarUpdateAction action;

		// If we've never seen the location obviously we don't get an update
		if (state == FogOfWarStateID.NEVER_SEEN)
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE;

		// If we're only seeing it for the first time then clearly we need to update it
		else if (state == FogOfWarStateID.TEMP_SEEING_IT_FOR_FIRST_TIME)
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE;

		// With those two out of the way, every other state means that we've seen the location at some prior time
		// and just now its coming in view, out of view, still in view - if we're on "always see once seen" none of that matters, we've
		// still been able to see the location the whole time so know that our info about it must already be up to date
		else if ((setting == FogOfWarValue.ALWAYS_SEE_ONCE_SEEN) || (state == FogOfWarStateID.TEMP_CAN_STILL_SEE))
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF;

		// If we've saw it, lost sight of it, and are now seeing it again then usually means we need to update it
		else if (state == FogOfWarStateID.TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT)
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE;		// = fowCanNowSee in the Delphi code

		// If the middle of updateAndSendFogOfWar (), FOG_OF_WAR_CAN_SEE means *COULD* see but no longer can
		// So if the FOW setting is to forget what was seen then do so
		else if ((state == FogOfWarStateID.CAN_SEE) && (setting == FogOfWarValue.FORGET))
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET;		// = fowCanSee in the Delphi code

		else
			action = FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NONE;

		return action;
	}

	/**
	 * I think now that there is a OverlandMapCoordinatesEx.equals () method, that this could just be done with coordinateList.contains (), but leaving it for now
	 * 
	 * @param coordinateList List of coordinates to check
	 * @param coordinates Coordinates to look for
	 * @return True if coordinates are already in the list
	 */
	final boolean areCoordinatesIncludedInMessage (final List<UpdateNodeLairTowerUnitIDMessageData> coordinateList, final OverlandMapCoordinatesEx coordinates)
	{
		boolean result = false;
		final Iterator<UpdateNodeLairTowerUnitIDMessageData> iter = coordinateList.iterator ();
		while ((!result) && (iter.hasNext ()))
		{
			final UpdateNodeLairTowerUnitIDMessageData theseCoords = iter.next ();
			if (theseCoords.getNodeLairTowerLocation ().equals (coordinates))
				result = true;
		}

		return result;
	}

	/**
	 * This routine handles when the area that a player can see changes; it:
	 * 1) Checks what the player can now see
	 * 2) Compares the area against what the player could see before
	 * 3) Checks which map cells, buildings, spells, CAEs and units the player can either now see that they couldn't see before, or now can't see that they could see before
	 * 4) Updates the server copy of their memory accordingly
	 * 5) If a human player, sends messages to the client to update their memory there accordingly
	 *
	 * Note this *doesn't* need to worry about "what if the map cell, list of buildings, etc. has changed since last turn" - those
	 * types of changes are dealt with by all the methods in FogOfWarMidTurnChanges - this only needs to deal with
	 * working out updates when the visible area changes, but the true values remain the same
	 *
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param player The player whose FOW we are recalculating
	 * @param players List of players in the session
	 * @param nameCitiesAtStartOfGame Set only for the first time this is called during game startup, and tells all the clients to ask for names for their starting cities
	 * @param triggeredFrom What caused the change in visible area - this is only used for debug messages on the client
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void updateAndSendFogOfWar (final FogOfWarMemory trueMap, final PlayerServerDetails player,
		final List<PlayerServerDetails> players, final boolean nameCitiesAtStartOfGame,
		final String triggeredFrom, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (FogOfWarProcessing.class.getName (), "updateAndSendFogOfWar", player.getPlayerDescription ().getPlayerID ());

		markVisibleArea (trueMap, player, players, sd, db);

		// Start off the big message, if a human player
		final FogOfWarVisibleAreaChangedMessage msg;
		if (player.getPlayerDescription ().isHuman ())
		{
			msg = new FogOfWarVisibleAreaChangedMessage ();
			msg.setTriggeredFrom (triggeredFrom);
		}
		else
			msg = null;

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
					final MemoryGridCell tc = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);

					final OverlandMapCoordinatesEx coords = new OverlandMapCoordinatesEx ();
					coords.setX (x);
					coords.setY (y);
					coords.setPlane (plane.getPlaneNumber ());

					// Check for changes in the terrain
					switch (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ()))
					{
						// Couldn't see this cell before (either we'd never seen it before, or we'd seen it earlier and have been remembering it) - now we can see it
						case FOG_OF_WAR_ACTION_UPDATE:
						{
							if (getFogOfWarDuplication ().copyTerrainAndNodeAura (tc, mc))
								if (msg != null)
								{
									final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
									terrainMsg.setMapLocation (coords);
									terrainMsg.setTerrainData (mc.getTerrainData ());
									msg.getTerrainUpdate ().add (terrainMsg);
								}
							break;
						}

						// Could see this cell before but now we need to forget what we saw
						case FOG_OF_WAR_ACTION_FORGET:
						{
							if (getFogOfWarDuplication ().blankTerrainAndNodeAura (mc))
								if (msg != null)
								{
									final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
									terrainMsg.setMapLocation (coords);
									terrainMsg.setTerrainData (mc.getTerrainData ());
									msg.getTerrainUpdate ().add (terrainMsg);
								}
							break;
						}

						// No action required, because we could see it before and still can, so our info about it will already be up to date
						case FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF:
						{
							break;
						}

						// No action required, because we couldn't see it before and still can't
						case FOG_OF_WAR_ACTION_NONE:
						{
							break;
						}
					}

					// Check for changes in cities
					switch (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
					{
						// Couldn't see this cell before (either we'd never seen it before, or we'd seen it earlier and have been remembering it) - now we can see it
						case FOG_OF_WAR_ACTION_UPDATE:
						{
							// Careful, may not even be a city here and hence tc.getCityData () may be null
							final int cityOwnerID = (tc.getCityData () == null) ? 0 : tc.getCityData ().getCityOwnerID ();

							if (getFogOfWarDuplication ().copyCityData (tc, mc, (cityOwnerID == player.getPlayerDescription ().getPlayerID ()) ||
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
						}

						// Could see this cell before but now we need to forget what we saw
						case FOG_OF_WAR_ACTION_FORGET:
						{
							if (getFogOfWarDuplication ().blankCityData (mc))
								if (msg != null)
								{
									final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
									cityMsg.setMapLocation (coords);
									cityMsg.setCityData (mc.getCityData ());
									msg.getCityUpdate ().add (cityMsg);
								}
							break;
						}
						
						// No action required, because we could see it before and still can, so our info about it will already be up to date
						case FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF:
						{
							break;
						}
							
						// No action required, because we couldn't see it before and still can't
						case FOG_OF_WAR_ACTION_NONE:
						{
							break;
						}
					}
				}

		// Check to see what buildings we need to add
		// This must run down the true list of buildings so it can spot any that we don't know about yet
		for (final MemoryBuilding thisBuilding : trueMap.getBuilding ())
		{
			// This can happen one of two ways:
			// 1) We remember nothing about the location and are seeing it for the first time
			// 2) We remember the location but walked away from it (FOW setting = remember as last seen), its now coming back into view and since
			//		we last saw it, the city there has constructed new buildings
			//
			// In both cases action will be 'update', but that isn't enough to go on, since in (2) just because action=update doesn't mean that we don't
			// still have the building in our list from remembering it from last time we saw it
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisBuilding.getCityLocation ().getPlane ()).getRow ().get
				(thisBuilding.getCityLocation ().getY ()).getCell ().get (thisBuilding.getCityLocation ().getX ());

			if (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)
				if (getFogOfWarDuplication ().copyBuilding (thisBuilding, priv.getFogOfWarMemory ().getBuilding ()))
					if (msg != null)
					{
						final AddBuildingMessageData buildingMsg = new AddBuildingMessageData ();
						buildingMsg.setFirstBuildingID (thisBuilding.getBuildingID ());
						buildingMsg.setCityLocation (thisBuilding.getCityLocation ());
						msg.getAddBuilding ().add (buildingMsg);
					}
		}

		// Check to see what buildings we need to remove
		// This must run down our memorized list of buildings, because some of those may not exist in the true buildings list, if we remember the location
		// but walked away from it (FOW setting = remember as last seen), its now coming back into view and since we last saw it, the city there has lost some buildings
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
				(!getMemoryBuildingUtils ().findBuilding (trueMap.getBuilding (), (OverlandMapCoordinatesEx) thisBuilding.getCityLocation (), thisBuilding.getBuildingID ()))))
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
		final List<Integer> updatedUnitURNs = new ArrayList<Integer> ();
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if (thisUnit.getStatus () == UnitStatusID.ALIVE)
			{
				// Monsters in nodes/lairs/towers have to be scouted to see them, we can never see them by FOW coming into view (this routine)
				// See corresponding code in FogOfWarMidTurnChanges.canSeeUnitMidTurn ()
				final PlayerServerDetails unitOwner = MultiplayerSessionServerUtils.findPlayerWithID (players, thisUnit.getOwningPlayerID (), "canSeeUnitMidTurn");
				final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();
				
				final OverlandMapTerrainData terrainData = trueMap.getMap ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
					(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ();
				
				if ((ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) && (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)))
				{
					// Can't be seen by FOW change
				}
				else
				{
					// This can happen one of two ways:
					// 1) We remember nothing about the unit and are seeing it for the first time
					// 2) We remember the unit from it seeing it before (FOW setting = remember as last seen), its now coming back into view either in
					//		the same or a different location
					//
					// In both cases action will be 'update', but that isn't enough to go on, since in (2) just because action=update doesn't mean that we don't
					// still have the unit in our list from remembering it from last time we saw it
					//
					// Towers are a complication - all units in towers are set as being on plane 0, but the tile on plane 0 may be e.g. NEVER_SEEN
					// The fact that we can see the terrain on plane 1 also allows us to see the unit, so have multiple states and multiple update actions to consider
					final List<FogOfWarStateID> states = new ArrayList<FogOfWarStateID> ();

					if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
					{
						// In a tower, consider all planes
						for (final Plane plane : db.getPlane ())
							states.add (priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get
								(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));
					}
					else
						// Outside of a tower - only one plane to consider
						states.add (priv.getFogOfWar ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
							(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));

					// Convert to update actions
					final List<FogOfWarUpdateAction> actions = new ArrayList<FogOfWarUpdateAction> ();
					for (final FogOfWarStateID state : states)
						actions.add (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getUnits ()));

					// NONE can mean "none because we can't see it", or "none because it never left our sight so the info we remember must already be up to date"
					// So NONE,UPDATE is a tower coming into view on one plane that we can't see at all on the other plane so we need to do an update, whereas
					// NEVER_LOST_SIGHT_OF,UPDATE is a tower just coming into view on one plane that we have had a city next to on the other plane the whole time, so nothing to do
					if ((actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)) &&
						(!actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF)))
					{
						final boolean unitChanged = getFogOfWarDuplication ().copyUnit (thisUnit, priv.getFogOfWarMemory ().getUnit ());
						updatedUnitURNs.add (thisUnit.getUnitURN ());

						log.finest ("UnitURN " + thisUnit.getUnitURN () + " has come into view for player " + player.getPlayerDescription ().getPlayerID () + " as part of VAC, unitChanged=" + unitChanged);
						if ((unitChanged) && (msg != null))
							msg.getAddUnit ().add (getFogOfWarDuplication ().createAddUnitMessage (thisUnit, db));

						// If this is a unit standing on a node or tower, then that proves that the node or tower has been cleared of monsters
						final String nodeLairTowerKnownUnitID = priv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
							(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ());

						if ((ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)) && (!CompareUtils.safeStringCompare (nodeLairTowerKnownUnitID, "")))	// know it to be empty
						{
							priv.getNodeLairTowerKnownUnitIDs ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
								(thisUnit.getUnitLocation ().getY ()).getCell ().set (thisUnit.getUnitLocation ().getX (), "");

							if (msg != null)
								if (!areCoordinatesIncludedInMessage (msg.getUpdateNodeLairTowerUnitID (), (OverlandMapCoordinatesEx) thisUnit.getUnitLocation ()))
								{
									final OverlandMapCoordinatesEx scoutMsgCoords = new OverlandMapCoordinatesEx ();
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
				}
			}

		// Check to see what units we could see but now can't (this runs down our local memory of the units)
		final List<Integer> removedUnitURNs = new ArrayList<Integer> ();
		final Iterator<MemoryUnit> unitsIter = priv.getFogOfWarMemory ().getUnit ().iterator ();
		while (unitsIter.hasNext ())
		{
			final MemoryUnit thisUnit = unitsIter.next ();
			if (thisUnit.getStatus () == UnitStatusID.ALIVE)
			{
				// Monsters in nodes/lairs/towers have to be scouted to see them, we can never see them by FOW coming into view (this routine)
				// See corresponding code in FogOfWarMidTurnChanges.canSeeUnitMidTurn ()
				final PlayerServerDetails unitOwner = MultiplayerSessionServerUtils.findPlayerWithID (players, thisUnit.getOwningPlayerID (), "canSeeUnitMidTurn");
				final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();

				final OverlandMapTerrainData terrainData = trueMap.getMap ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
					(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ();

				if ((ppk.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) && (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)))
				{
					// Can't be seen (or lose sight of it) by FOW change
				}
				else
				{
					// This can happen one of two ways:
					// 1) We need to forget what's at the location we saw the unit at (FOW setting = forget, and we've walked away)
					// 2) We've remembered that we saw a unit at a particular location, but in between scouting it last time and it coming into view now, the unit is no longer there anymore
					//		(it could either be dead, or just moved to somewhere that we can't see, from the player's point of view they can't tell the difference)
					//
					// Towers are a complication - all units in towers are set as being on plane 0, but the tile on plane 0 may be e.g. NEVER_SEEN
					// The fact that we can see the terrain on plane 1 also allows us to see the unit, so have multiple states and multiple update actions to consider
					final List<FogOfWarStateID> states = new ArrayList<FogOfWarStateID> ();

					if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData))
					{
						// In a tower, consider all planes
						for (final Plane plane : db.getPlane ())
							states.add (priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get
								(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));
					}
					else
						// Outside of a tower - only one plane to consider
						states.add (priv.getFogOfWar ().getPlane ().get (thisUnit.getUnitLocation ().getPlane ()).getRow ().get
							(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));

					// Convert to update actions
					final List<FogOfWarUpdateAction> actions = new ArrayList<FogOfWarUpdateAction> ();
					for (final FogOfWarStateID state : states)
						actions.add (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getUnits ()));

					// NONE can mean "none because we can't see it", or "none because it never left our sight so the info we remember must already be up to date"
					// So NONE,FORGET is a tower going out of view on one plane that we can't see at all on the other plane so we need to forget it, whereas
					// NEVER_LOST_SIGHT_OF,FORGET is a tower going out of view on one plane that we have had a city next to on the other plane the whole time, so nothing to do
					// Also NEVER_LOST_SIGHT_OF,UPDATE is a tower going out of view on one plane at the same time as it is coming into view on the other plane, so again nothing to do
					final boolean needToRemoveUnit;
					if ((actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET)) &&
						(!actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF)) &&
						(!actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)))

						needToRemoveUnit = true;

					else if (actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE))
					{
						final MemoryUnit trueUnit = getUnitUtils ().findUnitURN (thisUnit.getUnitURN (), trueMap.getUnit ());
						if (trueUnit == null)
							needToRemoveUnit = true;
						else
							// We don't need to worry about checking whether or not we can see where the unit has moved to - we already know that we can't
							// because if we could see it, we'd have already dealt with it and updated our memory of the unit in the 'add' section above
							// The only way the code can drop into this block is if the unit has moved to somewhere that we can't see it
							needToRemoveUnit = !trueUnit.getUnitLocation ().equals (thisUnit.getUnitLocation ());
					}
					else
						needToRemoveUnit = false;

					if (needToRemoveUnit)
					{
						log.finest ("UnitURN " + thisUnit.getUnitURN () + " has gone out of view for player " + player.getPlayerDescription ().getPlayerID () + ", sending kill as part of VAC");
						removedUnitURNs.add (thisUnit.getUnitURN ());

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
			}
		}

		// Check to see what maintained spells we couldn't see but now can (this runs down the true list of spells)
		for (final MemoryMaintainedSpell thisSpell : trueMap.getMaintainedSpell ())

			// Ignore global enchantments, we can see those all the time, so they cannot come into and out of view as the FOW visible area changes
			if ((thisSpell.getUnitURN () != null) || (thisSpell.getCityLocation () != null))
			{
				// If a unit has come into view, then any spells cast on it also come into view - we recorded a list of all units that
				// came into view above, so that we don't have to work it out again here
				final boolean needToUpdate;
				if (thisSpell.getUnitURN () != null)
					needToUpdate = updatedUnitURNs.contains (thisSpell.getUnitURN ());
				else
				{
					// This can happen one of two ways:
					// 1) We remember nothing about the location and are seeing it for the first time
					// 2) We remember the location but walked away from it (FOW setting = remember as last seen), its now coming back into view and since
					//		we last saw it, there's now spells cast there
					//
					// In both cases action will be 'update', but that isn't enough to go on, since in (2) just because action=update doesn't mean that we don't
					// still have the spell in our list from remembering it from last time we saw it
					final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisSpell.getCityLocation ().getPlane ()).getRow ().get
						(thisSpell.getCityLocation ().getY ()).getCell ().get (thisSpell.getCityLocation ().getX ());

					needToUpdate = (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE);
				}

				if (needToUpdate)

					// Copy spell into player's memory
					if (getFogOfWarDuplication ().copyMaintainedSpell (thisSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))
						if (msg != null)
							msg.getAddMaintainedSpell ().add (getFogOfWarDuplication ().createAddSpellMessage (thisSpell));
			}

		// Check to see what maintained spells we could see but now can't (this runs down our local memory of the spells)
		final Iterator<MemoryMaintainedSpell> spellsIter = priv.getFogOfWarMemory ().getMaintainedSpell ().iterator ();
		while (spellsIter.hasNext ())
		{
			final MemoryMaintainedSpell thisSpell = spellsIter.next ();

			// Ignore global enchantments, we can see those all the time, so they cannot come into and out of view as the FOW visible area changes
			if ((thisSpell.getUnitURN () != null) || (thisSpell.getCityLocation () != null))
			{
				// If a unit has come into view, then any spells cast on it also come into view - we recorded a list of all units that
				// came into view above, so that we don't have to work it out again here
				final boolean needToRemoveSpell;
				if (thisSpell.getUnitURN () != null)
					needToRemoveSpell = removedUnitURNs.contains (thisSpell.getUnitURN ());
				else
				{
					// This can happen one of two ways:
					// 1) We need to forget what's at that location (FOW setting = forget, and we've walked away)
					// 2) We've remembered a CAE that used to be here, but in between scouting it last time and it coming into view now, some of those CAEs have been switched off
					final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisSpell.getCityLocation ().getPlane ()).getRow ().get
						(thisSpell.getCityLocation ().getY ()).getCell ().get (thisSpell.getCityLocation ().getX ());

					final FogOfWarUpdateAction action = determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ());

					needToRemoveSpell = ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) || ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
						(getMemoryMaintainedSpellUtils ().findMaintainedSpell (trueMap.getMaintainedSpell (), thisSpell.getCastingPlayerID (), thisSpell.getSpellID (),
							thisSpell.getUnitURN (), thisSpell.getUnitSkillID (), (OverlandMapCoordinatesEx) thisSpell.getCityLocation (), thisSpell.getCitySpellEffectID ()) == null)));
				}

				if (needToRemoveSpell)
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
		}

		// Check to see what combat area effects we couldn't see but now can (this runs down the true list of CAEs)
		for (final MemoryCombatAreaEffect thisCAE : trueMap.getCombatAreaEffect ())

			// Ignore global CAEs, we can see those all the time, so they cannot come into and out of view as the FOW visible area changes
			if (thisCAE.getMapLocation () != null)
			{
				// This can happen one of two ways:
				// 1) We remember nothing about the location and are seeing it for the first time
				// 2) We remember the location but walked away from it (FOW setting = remember as last seen), its now coming back into view and since
				//		we last saw it, there's now a CAE there
				//
				// In both cases action will be 'update', but that isn't enough to go on, since in (2) just because action=update doesn't mean that we don't
				// still have the CAE in our list from remembering it from last time we saw it
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisCAE.getMapLocation ().getPlane ()).getRow ().get
					(thisCAE.getMapLocation ().getY ()).getCell ().get (thisCAE.getMapLocation ().getX ());

				if (determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)
					if (getFogOfWarDuplication ().copyCombatAreaEffect (thisCAE, priv.getFogOfWarMemory ().getCombatAreaEffect ()))
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
		while (caeIter.hasNext ())
		{
			final MemoryCombatAreaEffect thisCAE = caeIter.next ();

			// Ignore global CAEs, we can see those all the time, so they cannot come into and out of view as the FOW visible area changes
			if (thisCAE.getMapLocation () != null)
			{
				// This can happen one of two ways:
				// 1) We need to forget what's at that location (FOW setting = forget, and we've walked away)
				// 2) We've remembered a CAE that used to be here, but in between scouting it last time and it coming into view now, some of those CAEs have been switched off
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisCAE.getMapLocation ().getPlane ()).getRow ().get
					(thisCAE.getMapLocation ().getY ()).getCell ().get (thisCAE.getMapLocation ().getX ());

				final FogOfWarUpdateAction action = determineVisibleAreaChangedUpdateAction (state, sd.getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ());

				if ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) || ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
					(!getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (trueMap.getCombatAreaEffect (),
						(OverlandMapCoordinatesEx) thisCAE.getMapLocation (), thisCAE.getCombatAreaEffectID (), thisCAE.getCastingPlayerID ()))))
				{
					if (msg != null)
					{
						final CancelCombatAreaEffectMessageData caeMsg = new CancelCombatAreaEffectMessageData ();
						caeMsg.setCombatAreaEffectID (thisCAE.getCombatAreaEffectID ());
						caeMsg.setMapLocation (thisCAE.getMapLocation ());
						caeMsg.setCastingPlayerID (thisCAE.getCastingPlayerID ());
						msg.getCancelCombaAreaEffect ().add (caeMsg);
					}

					caeIter.remove ();
				}
			}
		}

		// Lastly send the client details of the changes in the fog of war area itself
		// Also sets the values on the server back normal
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final List<FogOfWarStateID> row = priv.getFogOfWar ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ();

					final OverlandMapCoordinatesEx coords = new OverlandMapCoordinatesEx ();
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
						case TEMP_SEEING_IT_FOR_FIRST_TIME:
						case TEMP_SEEING_AFTER_LOST_SIGHT_OF_IT:
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
							
						// Could see this cell before and it still didn't come back into view - nothing to do
						case HAVE_SEEN:
							
						// Never seen this cell and still haven't - nothing to do
						case NEVER_SEEN:
					}
				}

		// Send the completed message
		if (msg != null)
			player.getConnection ().sendMessageToClient (msg);

		log.exiting (FogOfWarProcessing.class.getName (), "updateAndSendFogOfWar");
	}

	/**
	 * @return FOW duplication utils
	 */
	public final IFogOfWarDuplication getFogOfWarDuplication ()
	{
		return fogOfWarDuplication;
	}

	/**
	 * @param dup FOW duplication utils
	 */
	public final void setFogOfWarDuplication (final IFogOfWarDuplication dup)
	{
		fogOfWarDuplication = dup;
	}

	/**
	 * @return Unit utils
	 */
	public final IUnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final IUnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return MemoryBuilding utils
	 */
	public final IMemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final IMemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final IMemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final IMemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final IMemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final IMemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final IMemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final IMemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}

	/**
	 * @return Server-only city calculations
	 */
	public final IMomServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final IMomServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}

	/**
	 * @return Server-only unit calculations
	 */
	public final IMomServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final IMomServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}
}

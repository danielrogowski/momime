package momime.server.fogofwar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarValue;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.FogOfWarStateMessageData;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.UpdateCityMessageData;
import momime.common.messages.servertoclient.UpdateTerrainMessageData;
import momime.common.movement.MovementUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.utils.KnownWizardServerUtils;

/**
 * This contains the methods that recheck what areas of the map a specific player can see, and depending which areas
 * have come into sight, or are going out of sight, and depending on the chosen FOW settings on the new game form,
 * works out all the terrain, cities, buildings, units, and so on that need to be updated or removed from the player's memory
 * and builds the appropriate update message to inform the client of those changes
 *
 * i.e. methods for when the true values remain the same but the visible area changes
 */
public final class FogOfWarProcessingImpl implements FogOfWarProcessing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (FogOfWarProcessingImpl.class);
	
	/** FOW duplication utils */
	private FogOfWarDuplication fogOfWarDuplication;

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
	
	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Movement utils */
	private MovementUtils movementUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/**
	 * Marks that we can see a particular cell
	 * @param fogOfWarArea Player's fog of war area
	 * @param trueTerrain True overland map terrain
	 * @param x X coordinate of map cell to update
	 * @param y Y coordinate of map cell to update
	 * @param plane Plane of map cell to update
	 */
	final void canSee (final MapVolumeOfFogOfWarStates fogOfWarArea, final MapVolumeOfMemoryGridCells trueTerrain, final int x, final int y, final int plane)
	{
		// If there's a tower here, then we can see through it to the other plane
		final List<Integer> planes = new ArrayList<Integer> ();
		if (!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (trueTerrain.getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getTerrainData ()))
			planes.add (plane);
		else
			for (int n = 0; n < trueTerrain.getPlane ().size (); n++)
				planes.add (n);
		
		for (final Integer thisPlane : planes)
		{
			final List<FogOfWarStateID> row = fogOfWarArea.getPlane ().get (thisPlane).getRow ().get (y).getCell ();
	
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
	}

	/**
	 * Marks that we can see all cells within a particular radius
	 * @param fogOfWarArea Player's fog of war area
	 * @param trueTerrain True overland map terrain
	 * @param sys Overland map coordinate system
	 * @param x X coordinate of map cell to update
	 * @param y Y coordinate of map cell to update
	 * @param plane Plane of map cell to update
	 * @param radius Visible radius (negative = do nothing, 0 = this cell only, 1 = 1 ring around this cell, and so on)
	 */
	@Override
	public final void canSeeRadius (final MapVolumeOfFogOfWarStates fogOfWarArea, final MapVolumeOfMemoryGridCells trueTerrain,
		final CoordinateSystem sys, final int x, final int y, final int plane, final int radius)
	{
		// First the centre square }
		canSee (fogOfWarArea, trueTerrain, x, y, plane);

		// Then around the each square 'ring'
		final MapCoordinates2DEx coords = new MapCoordinates2DEx (x, y);
		
		for (int ringNumber = 1; ringNumber <= radius; ringNumber++)
		{
			// Move down-left
			getCoordinateSystemUtils ().move2DCoordinates (sys, coords, SquareMapDirection.SOUTHWEST.getDirectionID ());

			// Go around the ring
			for (int directionChk = 0; directionChk < 4; directionChk++)
			{
				final int d = (directionChk * 2) + 1;
				for (int traceSide = 0; traceSide < ringNumber * 2; traceSide++)
					if (getCoordinateSystemUtils ().move2DCoordinates (sys, coords, d))
						canSee (fogOfWarArea, trueTerrain, coords.getX (), coords.getY (), plane);
			}
		}
	}

	/**
	 * This calls canSee () for every cell the map that the player can see for any reason (because they have units there, cities, awareness, etc.)
	 * It is part of updateAndSendFogOfWar () but just declared separately so we can run a JUnit test against it
	 *
	 * @param player The player whose FOW we are recalculating
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final void markVisibleArea (final PlayerServerDetails player, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "markVisibleArea");
		
		// Nature Awareness allows us to see the whole map, in which case no point checking each city or unit
		if ((CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (wizardDetails.getWizardID ())) ||
			((mom.getSessionDescription ().isDisableFogOfWar () != null) && (mom.getSessionDescription ().isDisableFogOfWar ())) ||
			(getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (),
				ServerDatabaseValues.SPELL_ID_NATURE_AWARENESS, null, null, null, null) != null))
		{
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						canSee (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), x, y, z);
		}
		else
		{
			// Check if we have regular Awareness cast, so we don't have to check individually for every city
			final boolean awareness = (getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				player.getPlayerDescription ().getPlayerID (), ServerDatabaseValues.SPELL_ID_AWARENESS, null, null, null, null) != null);

			// Check what areas we can see because we have cities there
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final OverlandMapCityData trueCity = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
						if (trueCity != null)
						{
							final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);

							// Our city
							if (trueCity.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ())
							{
								// Most cities can 'see' the same pattern as their resource range, but some special buildings can extend this
								// This does not handle the "Nature's Eye" spell - this is done with the spells below
								final int scoutingRange = getServerCityCalculations ().calculateCityScoutingRange (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), coords, mom.getServerDB ());
								if (scoutingRange >= 0)
									canSeeRadius (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize (), x, y, z, scoutingRange);
								else
								{
									// Standard city pattern
									for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
										if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
											canSee (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), coords.getX (), coords.getY (), coords.getZ ());
								}
							}

							// Enemy city - we can see a small area around it if we either have Awareness cast or a curse cast on the city
							else if ((awareness) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
								(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), null, null, null, coords, null) != null))

								canSeeRadius (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize (), x, y, z, 1);
						}
					}

			// Check what areas we can see because we have units there
			for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()))
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					final int scoutingRange = getServerUnitCalculations ().calculateUnitScoutingRange (xu, mom.getServerDB ());

					// If standing in a tower, can see both planes
					if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(thisUnit.getUnitLocation ().getZ ()).getRow ().get (thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ()))
					{
						for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
							canSeeRadius (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize (),
								thisUnit.getUnitLocation ().getX (), thisUnit.getUnitLocation ().getY (), z, scoutingRange);
					}
					else
						// Can see single plane only
						canSeeRadius (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize (),
							thisUnit.getUnitLocation ().getX (), thisUnit.getUnitLocation ().getY (), thisUnit.getUnitLocation ().getZ (), scoutingRange);
				}

			// Check what areas we can see because of visibility spells
			// This is mainly for Nature's Eye, but is handled separately from cities to allow Earth Lore to be a maintained spell so it works sensibly with "Forget" FOW settings
			for (final MemoryMaintainedSpell thisSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
				if ((thisSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisSpell.getCityLocation () != null))
				{
					// See if this spell has a scouting range
					final Spell spellDef = mom.getServerDB ().findSpell (thisSpell.getSpellID (), "markVisibleArea");
					if ((spellDef.getSpellRadius () != null) && (spellDef.getTileTypeID () == null))
						canSeeRadius (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getSessionDescription ().getOverlandMapSize (), thisSpell.getCityLocation ().getX (),
							thisSpell.getCityLocation ().getY (), thisSpell.getCityLocation ().getZ (), spellDef.getSpellRadius ());
				}
			
			// Astral Gate lets us see 1 cell on the opposite plane, so movement routines know whether its safe to cross
			final Set<MapCoordinates2DEx> astralGates = getMovementUtils ().findAstralGates (player.getPlayerDescription ().getPlayerID (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
			for (final MapCoordinates2DEx astralGate : astralGates)
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					canSee (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), astralGate.getX (), astralGate.getY (), z);
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
	 * @param player The player whose FOW we are recalculating
	 * @param triggeredFrom What caused the change in visible area - this is only used for debug messages on the client
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether anything interesting changed (a unit, city, building, terrain, anything new or changed we didn't know about before, besides just the visible area changing)
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean updateAndSendFogOfWar (final PlayerServerDetails player, final String triggeredFrom, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		markVisibleArea (player, mom);
		
		// Were any changes made, besides just the visible area we can see?  Check everything EXCEPT msg.getFogOfWarUpdate ()
		// But we have to figure this out WITHOUT looking at the msg, as that will be null for AI players
		boolean somethingInterestingChanged = false;

		// Start off the big message, if a human player
		final FogOfWarVisibleAreaChangedMessage msg;
		if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			msg = new FogOfWarVisibleAreaChangedMessage ();
			msg.setTriggeredFrom (triggeredFrom);
		}
		else
			msg = null;

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);

					final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);

					// Check for changes in the terrain
					switch (determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ()))
					{
						// Couldn't see this cell before (either we'd never seen it before, or we'd seen it earlier and have been remembering it) - now we can see it
						case FOG_OF_WAR_ACTION_UPDATE:
						{
							if (tc.getTerrainData ().getNodeOwnerID () != null)
								getKnownWizardServerUtils ().meetWizard (tc.getTerrainData ().getNodeOwnerID (), player.getPlayerDescription ().getPlayerID (), true, mom);
							
							if (getFogOfWarDuplication ().copyTerrainAndNodeAura (tc, mc))
							{
								somethingInterestingChanged = true;
								
								if (msg != null)
								{
									final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
									terrainMsg.setMapLocation (coords);
									terrainMsg.setTerrainData (mc.getTerrainData ());
									msg.getTerrainUpdate ().add (terrainMsg);
								}
							}
							break;
						}

						// Could see this cell before but now we need to forget what we saw
						case FOG_OF_WAR_ACTION_FORGET:
						{
							if (getFogOfWarDuplication ().blankTerrainAndNodeAura (mc))
							{
								somethingInterestingChanged = true;

								if (msg != null)
								{
									final UpdateTerrainMessageData terrainMsg = new UpdateTerrainMessageData ();
									terrainMsg.setMapLocation (coords);
									terrainMsg.setTerrainData (mc.getTerrainData ());
									msg.getTerrainUpdate ().add (terrainMsg);
								}
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
					switch (determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()))
					{
						// Couldn't see this cell before (either we'd never seen it before, or we'd seen it earlier and have been remembering it) - now we can see it
						case FOG_OF_WAR_ACTION_UPDATE:
						{
							// Careful, may not even be a city here and hence tc.getCityData () may be null
							if (tc.getCityData () != null)
								getKnownWizardServerUtils ().meetWizard (tc.getCityData ().getCityOwnerID (), player.getPlayerDescription ().getPlayerID (), true, mom);
							
							final int cityOwnerID = (tc.getCityData () == null) ? 0 : tc.getCityData ().getCityOwnerID ();
							
							if (getFogOfWarDuplication ().copyCityData (tc, mc,
								(cityOwnerID == player.getPlayerDescription ().getPlayerID ()) || (mom.getSessionDescription ().getFogOfWarSetting ().isSeeEnemyCityConstruction ()),
								cityOwnerID == player.getPlayerDescription ().getPlayerID ()))
							{
								somethingInterestingChanged = true;

								if (msg != null)
								{
									final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
									cityMsg.setMapLocation (coords);
									cityMsg.setCityData (mc.getCityData ());
									msg.getCityUpdate ().add (cityMsg);
								}
							}
							break;
						}

						// Could see this cell before but now we need to forget what we saw
						case FOG_OF_WAR_ACTION_FORGET:
						{
							if (getFogOfWarDuplication ().blankCityData (mc))
							{
								somethingInterestingChanged = true;

								if (msg != null)
								{
									final UpdateCityMessageData cityMsg = new UpdateCityMessageData ();
									cityMsg.setMapLocation (coords);
									cityMsg.setCityData (mc.getCityData ());
									msg.getCityUpdate ().add (cityMsg);
								}
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
		for (final MemoryBuilding thisBuilding : mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ())
		{
			// This can happen one of two ways:
			// 1) We remember nothing about the location and are seeing it for the first time
			// 2) We remember the location but walked away from it (FOW setting = remember as last seen), its now coming back into view and since
			//		we last saw it, the city there has constructed new buildings
			//
			// In both cases action will be 'update', but that isn't enough to go on, since in (2) just because action=update doesn't mean that we don't
			// still have the building in our list from remembering it from last time we saw it
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisBuilding.getCityLocation ().getZ ()).getRow ().get
				(thisBuilding.getCityLocation ().getY ()).getCell ().get (thisBuilding.getCityLocation ().getX ());

			if (determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)
				if (getFogOfWarDuplication ().copyBuilding (thisBuilding, priv.getFogOfWarMemory ().getBuilding ()))
				{
					somethingInterestingChanged = true;

					if (msg != null)
						msg.getAddBuilding ().add (thisBuilding);
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
			final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisBuilding.getCityLocation ().getZ ()).getRow ().get
				(thisBuilding.getCityLocation ().getY ()).getCell ().get (thisBuilding.getCityLocation ().getX ());

			final FogOfWarUpdateAction action = determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ());

			if ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) || ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
				(getMemoryBuildingUtils ().findBuildingURN (thisBuilding.getBuildingURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ()) == null)))
			{
				somethingInterestingChanged = true;

				if (msg != null)
					msg.getDestroyBuilding ().add (thisBuilding.getBuildingURN ());

				buildingsIter.remove ();
			}
		}

		// Check to see what units we need to add (runs down the true list of units)
		final List<Integer> updatedUnitURNs = new ArrayList<Integer> ();
		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if (thisUnit.getStatus () == UnitStatusID.ALIVE)
			{
				final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (thisUnit.getUnitLocation ().getZ ()).getRow ().get
					(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ();
				
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
					for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
						states.add (priv.getFogOfWar ().getPlane ().get (z).getRow ().get
							(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));
				}
				else
					// Outside of a tower - only one plane to consider
					states.add (priv.getFogOfWar ().getPlane ().get (thisUnit.getUnitLocation ().getZ ()).getRow ().get
						(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));

				// Convert to update actions
				final List<FogOfWarUpdateAction> actions = new ArrayList<FogOfWarUpdateAction> ();
				for (final FogOfWarStateID state : states)
					actions.add (determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getUnits ()));

				// NONE can mean "none because we can't see it", or "none because it never left our sight so the info we remember must already be up to date"
				// So NONE,UPDATE is a tower coming into view on one plane that we can't see at all on the other plane so we need to do an update, whereas
				// NEVER_LOST_SIGHT_OF,UPDATE is a tower just coming into view on one plane that we have had a city next to on the other plane the whole time, so nothing to do
				if ((actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)) &&
					(!actions.contains (FogOfWarUpdateAction.FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF)))
				{
					getKnownWizardServerUtils ().meetWizard (thisUnit.getOwningPlayerID (), player.getPlayerDescription ().getPlayerID (), true, mom);
					
					final boolean unitChanged = getFogOfWarDuplication ().copyUnit (thisUnit, priv.getFogOfWarMemory ().getUnit (),
						thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ());
					updatedUnitURNs.add (thisUnit.getUnitURN ());

					log.debug ("UnitURN " + thisUnit.getUnitURN () + " has come into view for player " + player.getPlayerDescription ().getPlayerID () + " as part of VAC, unitChanged=" + unitChanged);
					if (unitChanged)
					{
						somethingInterestingChanged = true;

						if (msg != null)
							msg.getAddOrUpdateUnit ().add (thisUnit);
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
				final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (thisUnit.getUnitLocation ().getZ ()).getRow ().get
					(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()).getTerrainData ();

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
					for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
						states.add (priv.getFogOfWar ().getPlane ().get (z).getRow ().get
							(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));
				}
				else
					// Outside of a tower - only one plane to consider
					states.add (priv.getFogOfWar ().getPlane ().get (thisUnit.getUnitLocation ().getZ ()).getRow ().get
						(thisUnit.getUnitLocation ().getY ()).getCell ().get (thisUnit.getUnitLocation ().getX ()));

				// Convert to update actions
				final List<FogOfWarUpdateAction> actions = new ArrayList<FogOfWarUpdateAction> ();
				for (final FogOfWarStateID state : states)
					actions.add (determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getUnits ()));

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
					final MemoryUnit trueUnit = getUnitUtils ().findUnitURN (thisUnit.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
					if (trueUnit == null)
						needToRemoveUnit = true;
					
					// Heroes exist in the true units list even if they've been dismissed and gone back to generated, so we still want to remove the unit if the status is anything other than "alive"
					else if (trueUnit.getStatus () != UnitStatusID.ALIVE)
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
					log.debug ("UnitURN " + thisUnit.getUnitURN () + " has gone out of view for player " + player.getPlayerDescription ().getPlayerID () + ", sending kill as part of VAC");
					removedUnitURNs.add (thisUnit.getUnitURN ());

					somethingInterestingChanged = true;
					
					if (msg != null)
						msg.getKillUnit ().add (thisUnit.getUnitURN ());

					unitsIter.remove ();
				}
			}
		}

		// Check to see what maintained spells we couldn't see but now can (this runs down the true list of spells)
		for (final MemoryMaintainedSpell thisSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())

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
					final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisSpell.getCityLocation ().getZ ()).getRow ().get
						(thisSpell.getCityLocation ().getY ()).getCell ().get (thisSpell.getCityLocation ().getX ());

					needToUpdate = (determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE);
				}

				if (needToUpdate)

					// Copy spell into player's memory
					if (getFogOfWarDuplication ().copyMaintainedSpell (thisSpell, priv.getFogOfWarMemory ().getMaintainedSpell ()))
					{
						somethingInterestingChanged = true;
						
						if (msg != null)
							msg.getAddMaintainedSpell ().add (thisSpell);
					}
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
				// came into view above, so that we don't have to work it out again here.
				
				// Other thing that can happen here is we previously saw a unit with spells cast on it - then it moved away, lost some of those spells while we couldn't
				// see it happen so we still remember those old spells, and now we're seeing the unit again.
				// So the fact that we can see an updated version of the unit may mean we need to remove some spells.
				final boolean needToRemoveSpell;
				if (thisSpell.getUnitURN () != null)
					needToRemoveSpell = (removedUnitURNs.contains (thisSpell.getUnitURN ())) ||
						((updatedUnitURNs.contains (thisSpell.getUnitURN ())) && (getMemoryMaintainedSpellUtils ().findSpellURN
							(thisSpell.getSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ()) == null));
				else
				{
					// This can happen one of two ways:
					// 1) We need to forget what's at that location (FOW setting = forget, and we've walked away)
					// 2) We've remembered a CAE that used to be here, but in between scouting it last time and it coming into view now, some of those CAEs have been switched off
					final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisSpell.getCityLocation ().getZ ()).getRow ().get
						(thisSpell.getCityLocation ().getY ()).getCell ().get (thisSpell.getCityLocation ().getX ());

					final FogOfWarUpdateAction action = determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ());

					needToRemoveSpell = ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) || ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
						(getMemoryMaintainedSpellUtils ().findSpellURN (thisSpell.getSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ()) == null)));
				}

				if (needToRemoveSpell)
				{
					somethingInterestingChanged = true;
					
					if (msg != null)
						msg.getSwitchOffMaintainedSpell ().add (thisSpell.getSpellURN ());

					spellsIter.remove ();
				}
			}
		}

		// Check to see what combat area effects we couldn't see but now can (this runs down the true list of CAEs)
		for (final MemoryCombatAreaEffect thisCAE : mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect ())

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
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisCAE.getMapLocation ().getZ ()).getRow ().get
					(thisCAE.getMapLocation ().getY ()).getCell ().get (thisCAE.getMapLocation ().getX ());

				if (determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ()) == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE)
					if (getFogOfWarDuplication ().copyCombatAreaEffect (thisCAE, priv.getFogOfWarMemory ().getCombatAreaEffect ()))
					{
						somethingInterestingChanged = true;

						if (msg != null)
							msg.getAddCombatAreaEffect ().add (thisCAE);
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
				final FogOfWarStateID state = priv.getFogOfWar ().getPlane ().get (thisCAE.getMapLocation ().getZ ()).getRow ().get
					(thisCAE.getMapLocation ().getY ()).getCell ().get (thisCAE.getMapLocation ().getX ());

				final FogOfWarUpdateAction action = determineVisibleAreaChangedUpdateAction (state, mom.getSessionDescription ().getFogOfWarSetting ().getCitiesSpellsAndCombatAreaEffects ());

				if ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_FORGET) || ((action == FogOfWarUpdateAction.FOG_OF_WAR_ACTION_UPDATE) &&
					(getMemoryCombatAreaEffectUtils ().findCombatAreaEffectURN (thisCAE.getCombatAreaEffectURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect ()) == null)))
				{
					somethingInterestingChanged = true;

					if (msg != null)
						msg.getCancelCombaAreaEffect ().add (thisCAE.getCombatAreaEffectURN ());

					caeIter.remove ();
				}
			}
		}

		// Lastly send the client details of the changes in the fog of war area itself
		// Also sets the values on the server back normal
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final List<FogOfWarStateID> row = priv.getFogOfWar ().getPlane ().get (z).getRow ().get (y).getCell ();

					final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);
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

		// Send the completed message, as long as there's actually something to send (its null for AI players)
		if ((msg != null) && ((somethingInterestingChanged) || (!msg.getFogOfWarUpdate ().isEmpty ())))
			player.getConnection ().sendMessageToClient (msg);
		
		return somethingInterestingChanged;
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
	 * @return Server-only city calculations
	 */
	public final ServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final ServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}

	/**
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
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

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}

	/**
	 * @return Movement utils
	 */
	public final MovementUtils getMovementUtils ()
	{
		return movementUtils;
	}

	/**
	 * @param u Movement utils
	 */
	public final void setMovementUtils (final MovementUtils u)
	{
		movementUtils = u;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}

	/**
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}
}
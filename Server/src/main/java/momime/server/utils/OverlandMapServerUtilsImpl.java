package momime.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.v0_9_4.AvailableUnit;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.utils.UnitUtils;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.CityNameContainer;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.messages.v0_9_4.ServerGridCell;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates;
import com.ndg.map.areas.StringMapArea2DArray;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

/**
 * Server side only helper methods for dealing with the overland map
 */
public final class OverlandMapServerUtilsImpl implements OverlandMapServerUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (OverlandMapServerUtilsImpl.class.getName ());
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Server-only pick utils */
	private PlayerPickServerUtils playerPickServerUtils;
	
	/**
	 * Sets the race for all land squares connected to x, y
	 * @param map True terrain
	 * @param continentalRace Map area listing the continental race ID at each location
	 * @param x X coordinate of starting location
	 * @param y Y coordinate of starting location
	 * @param plane Plane of starting location
	 * @param raceID Race ID to set
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	final void setContinentalRace (final MapVolumeOfMemoryGridCells map, final List<StringMapArea2DArray> continentalRace,
		final int x, final int y, final int plane, final String raceID, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.entering (OverlandMapServerUtilsImpl.class.getName (), "setContinentalRace",
			new String [] {new Integer (x).toString (), new Integer (y).toString (), new Integer (plane).toString (), raceID});

		final CoordinateSystem sys = continentalRace.get (plane).getCoordinateSystem ();

		// Set centre tile
		continentalRace.get (plane).set (x, y, raceID);

		// Now branch out in every direction from here
		for (int d = 1; d <= CoordinateSystemUtils.getMaxDirection (sys.getCoordinateSystemType ()); d++)
		{
			final MapCoordinates coords = new MapCoordinates ();
			coords.setX (x);
			coords.setY (y);

			if (CoordinateSystemUtils.moveCoordinates (sys, coords, d))
			{
				final OverlandMapTerrainData terrain = map.getPlane ().get (plane).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();

				// NB. IsLand is Boolean so could be null, but should be set for all tile types produced by the map generator
				// the only tile types for which it is null are those which are special references for the movement tables, e.g. road tiles
				if ((db.findTileType (terrain.getTileTypeID (), "setContinentalRace").isIsLand ()) && (continentalRace.get (plane).get (coords) == null))
					setContinentalRace (map, continentalRace, coords.getX (), coords.getY (), plane, raceID, db);
			}
		}

		log.exiting (OverlandMapServerUtilsImpl.class.getName (), "setContinentalRace");
	}

	/**
	 * Sets the continental race (mostly likely race raiders cities at each location will choose) for every land tile on the map
	 *
	 * @param map Known terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Generated area of race IDs
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 */
	@Override
	public final List<StringMapArea2DArray> decideAllContinentalRaces (final MapVolumeOfMemoryGridCells map,
		final CoordinateSystem sys, final ServerDatabaseEx db) throws RecordNotFoundException, MomException
	{
		log.entering (OverlandMapServerUtilsImpl.class.getName (), "decideAllContinentalRaces");

		// Allocate a race to each continent of land for raider cities
		final List<StringMapArea2DArray> continentalRace = new ArrayList<StringMapArea2DArray> ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
			continentalRace.add (new StringMapArea2DArray (sys));

		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sys.getWidth (); x++)
				for (int y = 0; y < sys.getHeight (); y++)
				{
					final OverlandMapTerrainData terrain = map.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ();

					// NB. IsLand is Boolean so could be null, but should be set for all tile types produced by the map generator
					// the only tile types for which it is null are those which are special references for the movement tables, e.g. road tiles
					if ((db.findTileType (terrain.getTileTypeID (), "decideAllContinentalRaces").isIsLand ()) &&
						(continentalRace.get (plane.getPlaneNumber ()).get (x, y) == null))

						setContinentalRace (map, continentalRace, x, y, plane.getPlaneNumber (),
							getPlayerPickServerUtils ().chooseRandomRaceForPlane (plane.getPlaneNumber (), db), db);
				}

		log.exiting (OverlandMapServerUtilsImpl.class.getName (), "decideAllContinentalRaces", continentalRace);
		return continentalRace;
	}

	/**
	 * NB. This will always return names unique from those names it has generated before - but if human players happen to rename their cities
	 * to a name that the generator hasn't produced yet, it won't avoid generating that name
	 *
	 * @param gsk Server knowledge data structure
	 * @param race The race who is creating a new city
	 * @return Auto generated city name
	 */
	@Override
	public final String generateCityName (final MomGeneralServerKnowledge gsk, final Race race)
	{
		final List<String> possibleChoices = new ArrayList<String> ();

		// Try increasing suffixes, (none), II, III, IV, V, and so on... eventually we have to generate a city name that's not been used!
		String chosenCityName = null;
		int numeral = 1;

		while (chosenCityName == null)
		{
			// Test each name to see if it has been used before
			possibleChoices.clear ();
			for (final CityNameContainer thisCache : race.getCityName ())
			{
				String thisName = thisCache.getCityName ();
				if (numeral > 1)
					thisName = thisName + " " + RomanNumerals.intToRoman (numeral);

				if (!gsk.getUsedCityName ().contains (thisName))
					possibleChoices.add (thisName);
			}

			// If any names are left then pick one, if not then increase the roman numeral suffix
			if (possibleChoices.size () > 0)
				chosenCityName = possibleChoices.get (getRandomUtils ().nextInt (possibleChoices.size ()));
			else
				numeral++;
		}

		gsk.getUsedCityName ().add (chosenCityName);

		return chosenCityName;
	}
	
	/**
	 * A spirit attempts to capture a node
	 * The only way it can fail is if the node is already owned by another player, in which case we only have a chance of success
	 * 
	 * @param attackingSpirit The Magic or Guardian spirit attempting to take the node; its location tells us where the node is
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @param players List of players in this session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a map feature, building or pick that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void attemptToMeldWithNode (final MemoryUnit attackingSpirit, final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException, JAXBException, XMLStreamException, PlayerNotFoundException
	{
		log.entering (OverlandMapServerUtilsImpl.class.getName (), "attemptToMeldWithNode", new String []
			{attackingSpirit.getUnitID (), new Integer (attackingSpirit.getOwningPlayerID ()).toString (), new Integer (attackingSpirit.getUnitURN ()).toString ()});

		final ServerGridCell tc = (ServerGridCell) trueMap.getMap ().getPlane ().get
			(attackingSpirit.getUnitLocation ().getPlane ()).getRow ().get (attackingSpirit.getUnitLocation ().getY ()).getCell ().get (attackingSpirit.getUnitLocation ().getX ());
		
		// Succeed?
		final boolean successful;
		if (tc.getTerrainData ().getNodeOwnerID () == null)
			successful = true;
		else
		{
			final int attackingStrength = getUnitUtils ().getModifiedSkillValue (attackingSpirit, attackingSpirit.getUnitHasSkill (),
				CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_MELD_WITH_NODE, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db);
			
			// Create test unit
			final AvailableUnit defendingSpirit = new AvailableUnit ();
			defendingSpirit.setUnitID (tc.getNodeSpiritUnitID ());
			getUnitUtils ().initializeUnitSkills (defendingSpirit, -1, true, db);

			final int defendingStrength = getUnitUtils ().getModifiedSkillValue (defendingSpirit, defendingSpirit.getUnitHasSkill (),
				CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_MELD_WITH_NODE, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db);
			
			// Decide who wins
			successful = (getRandomUtils ().nextInt (defendingStrength + attackingStrength) < attackingStrength);
		}
		
		// Check if successful
		if (successful)
		{
			// Add messages about it
			final PlayerServerDetails attackingPlayer = MultiplayerSessionServerUtils.findPlayerWithID (players, attackingSpirit.getOwningPlayerID (), "attemptToMeldWithNode (a)");
			if (attackingPlayer.getPlayerDescription ().isHuman ())
			{
				final NewTurnMessageData msg = new NewTurnMessageData ();
				msg.setMsgType (NewTurnMessageTypeID.NODE_CAPTURED);
				msg.setLocation (attackingSpirit.getUnitLocation ());
				msg.setBuildingOrUnitID (attackingSpirit.getUnitID ());
				msg.setOtherUnitID (tc.getNodeSpiritUnitID ());
				msg.setOtherPlayerID (tc.getTerrainData ().getNodeOwnerID ());
				
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) attackingPlayer.getTransientPlayerPrivateKnowledge ();
				trans.getNewTurnMessage ().add (msg);
			}
			
			if (tc.getTerrainData ().getNodeOwnerID () != null)
			{
				final PlayerServerDetails defendingPlayer = MultiplayerSessionServerUtils.findPlayerWithID (players, tc.getTerrainData ().getNodeOwnerID (), "attemptToMeldWithNode (d)");
				if (defendingPlayer.getPlayerDescription ().isHuman ())
				{
					final NewTurnMessageData msg = new NewTurnMessageData ();
					msg.setMsgType (NewTurnMessageTypeID.NODE_LOST);
					msg.setLocation (attackingSpirit.getUnitLocation ());
					msg.setBuildingOrUnitID (tc.getNodeSpiritUnitID ());
					msg.setOtherUnitID (attackingSpirit.getUnitID ());
					msg.setOtherPlayerID (attackingSpirit.getOwningPlayerID ());
					
					final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) defendingPlayer.getTransientPlayerPrivateKnowledge ();
					trans.getNewTurnMessage ().add (msg);
				}
			}
			
			// Set the type of spirit that captured it
			tc.setNodeSpiritUnitID (attackingSpirit.getUnitID ());
			
			// Resolve the node ownership out across the full area, updating the true map as well as players' memory of who can see each cell and informing the clients too
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
					for (final Plane plane : db.getPlane ())
					{
						final ServerGridCell aura = (ServerGridCell) trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x);
						if (attackingSpirit.getUnitLocation ().equals (aura.getAuraFromNode ()))
						{
							// Update true map
							aura.getTerrainData ().setNodeOwnerID (attackingSpirit.getOwningPlayerID ());
							
							// Update players' memory and clients
							final OverlandMapCoordinatesEx auraLocation = new OverlandMapCoordinatesEx ();
							auraLocation.setX (x);
							auraLocation.setY (y);
							auraLocation.setPlane (plane.getPlaneNumber ());
							
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (trueMap.getMap (), players, auraLocation,
								sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
						}
					}
		}
		
		// Kill off the spirit
		getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (attackingSpirit, KillUnitActionID.FREE, null, trueMap, players, sd, db);

		log.exiting (OverlandMapServerUtilsImpl.class.getName (), "attemptToMeldWithNode", successful);
	}

	/**
	 * @param map Known terrain
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param playerID Player to check for
	 * @param db Lookup lists built over the XML database
	 * @return Total population this player has across all their cities
	 */
	@Override
	public final int totalPlayerPopulation (final MapVolumeOfMemoryGridCells map, final int playerID, final CoordinateSystem overlandMapCoordinateSystem, final ServerDatabaseEx db)
	{
		log.entering (OverlandMapServerUtilsImpl.class.getName (), "totalPlayerPopulation", playerID);
		
		int total = 0;
		for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				for (final Plane plane : db.getPlane ())
				{
					final OverlandMapCityData cityData = map.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityOwnerID () == playerID) &&
						(cityData.getCityPopulation () != null))
						
						total = total + cityData.getCityPopulation ();
				}
		
		log.exiting (OverlandMapServerUtilsImpl.class.getName (), "totalPlayerPopulation", total);
		return total;
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Server-only pick utils
	 */
	public final PlayerPickServerUtils getPlayerPickServerUtils ()
	{
		return playerPickServerUtils;
	}

	/**
	 * @param utils Server-only pick utils
	 */
	public final void setPlayerPickServerUtils (final PlayerPickServerUtils utils)
	{
		playerPickServerUtils = utils;
	}
}

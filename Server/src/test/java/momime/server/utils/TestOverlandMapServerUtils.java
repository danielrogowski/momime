package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.CityNameContainer;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.messages.v0_9_4.ServerGridCell;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.StringMapArea2DArray;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the OverlandMapServerUtils class
 */
public class TestOverlandMapServerUtils
{
	/**
	 * Tests the chooseRandomRaceForPlane method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
 	 * @throws MomException If no races are defined with the requested plane
 	 * @throws RecordNotFoundException If we can't find the returned race ID in the database
	 */
	@Test
	public final void testChooseRandomRaceForPlane () throws IOException, JAXBException, MomException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Set up object to test
		final OverlandMapServerUtils utils = new OverlandMapServerUtils ();
		
		// Pick a random race for each plane
		for (final Plane plane : db.getPlane ())
		{
			final String raceID = utils.chooseRandomRaceForPlane (plane.getPlaneNumber (), db);

			// Verify race inhabits the correct plane
			assertEquals (plane.getPlaneNumber (), db.findRace (raceID, "testChooseRandomRaceForPlane").getNativePlane ());
		}
	}

	/**
	 * Tests the setContinentalRace method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
	 */
	@Test
	public final void testSetContinentalRace () throws IOException, JAXBException, RecordNotFoundException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);

		// Fill map with ocean
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);

					cell.setTerrainData (terrain);
				}

		// Create area to output into
		final List<StringMapArea2DArray> continentalRace = new ArrayList<StringMapArea2DArray> ();
		for (int plane = 0; plane < db.getPlane ().size (); plane++)
			continentalRace.add (new StringMapArea2DArray (sys));

		// Set up object to test
		final OverlandMapServerUtils utils = new OverlandMapServerUtils ();
		
		// One cell of land
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		utils.setContinentalRace (map, continentalRace, 20, 10, 1, "RC01", db);
		assertEquals (1, continentalRace.get (1).countCellsEqualTo ("RC01"));

		// Mark a city radius around the tile - just to test a non-square area
		final MapCoordinates coords = new MapCoordinates ();
		coords.setX (20);
		coords.setY (10);

		for (final SquareMapDirection d : MomCityCalculations.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (CoordinateSystemUtils.moveCoordinates (sys, coords, d.getDirectionID ()))
				map.getPlane ().get (1).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);

		utils.setContinentalRace (map, continentalRace, 20, 10, 1, "RC02", db);
		assertEquals (21, continentalRace.get (1).countCellsEqualTo ("RC02"));
	}

	/**
	 * Tests the decideAllContinentalRaces method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
 	 * @throws MomException If no races are defined for a particular plane
	 */
	@Test
	public final void testDecideAllContinentalRaces () throws IOException, JAXBException, RecordNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);

		// Fill map with ocean
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);

					cell.setTerrainData (terrain);
				}

		// Set up object to test
		final OverlandMapServerUtils utils = new OverlandMapServerUtils ();
		
		// Run method
		final List<StringMapArea2DArray> continentalRace = utils.decideAllContinentalRaces (map, sys, db);

		// Check results, we should have
		// null at every sea tile
		// race at every land tile, that inhabits the correct plane
		// same race at any adjacent land tile
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sys.getWidth (); x++)
				for (int y = 0; y < sys.getHeight (); y++)
				{
					final OverlandMapTerrainData terrain = map.getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ();
					final String raceID = continentalRace.get (plane.getPlaneNumber ()).get (x, y);

					if (db.findTileType (terrain.getTileTypeID (), "testDecideAllContinentalRaces").isIsLand ())
					{
						// Land tile
						assertEquals (plane.getPlaneNumber (), db.findRace (raceID, "testDecideAllContinentalRaces").getNativePlane ());

						// Check adjacent tiles are the same race
						for (int d = 1; d <= CoordinateSystemUtils.getMaxDirection (sys.getCoordinateSystemType ()); d++)
						{
							final MapCoordinates coords = new MapCoordinates ();
							coords.setX (x);
							coords.setY (y);

							if (CoordinateSystemUtils.moveCoordinates (sys, coords, d))
							{
								final String adjacentRaceID = continentalRace.get (plane.getPlaneNumber ()).get (coords);
								if (adjacentRaceID != null)
									assertEquals (raceID, adjacentRaceID);
							}
						}
					}
					else
						// Sea tile
						assertNull (raceID);
				}
	}

	/**
	 * Tests the generateCityName method
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we can't find the requested race
	 */
	@Test
	public final void testGenerateCityName () throws JAXBException, RecordNotFoundException
	{
		// If we have 4 possible names
		final String names [] = new String [] {"Hanna", "Emily", "Mara", "Rose"};

		final Race race = new Race ();
		for (final String thisName : names)
		{
			final CityNameContainer cont = new CityNameContainer ();
			cont.setCityName (thisName);
			race.getCityName ().add (cont);
		}

		// But 3 of them are already used
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.getUsedCityName ().add ("Hanna");
		gsk.getUsedCityName ().add ("Emily");
		gsk.getUsedCityName ().add ("Rose");

		// Set up object to test
		final OverlandMapServerUtils utils = new OverlandMapServerUtils ();
		
		// Then there's only one possible outcome
		assertEquals ("Mara", utils.generateCityName (gsk, race));

		// Now all 4 are used, the next 4 we generate will be suffixed with II
		for (int n = 0; n < 4; n++)
		{
			final String name = utils.generateCityName (gsk, race);
			if (!name.endsWith (" II"))
				fail ("Second round of city names not suffixed with II, got \"" + name + "\"");
		}

		// And the next 4 we generate will be suffixed with III
		for (int n = 0; n < 4; n++)
		{
			final String name = utils.generateCityName (gsk, race);
			if (!name.endsWith (" III"))
				fail ("Third round of city names not suffixed with III, got \"" + name + "\"");
		}
	}
	
	/**
	 * Tests the attemptToMeldWithNode method when there's no defender
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttemptToMeldWithNode_Undefended () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Node location
		final OverlandMapCoordinatesEx nodeLocation = new OverlandMapCoordinatesEx ();
		nodeLocation.setX (20);
		nodeLocation.setY (10);
		nodeLocation.setPlane (1);
		
		// Node
		final ServerGridCell nodeCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		final OverlandMapTerrainData nodeData = new OverlandMapTerrainData ();
		nodeCell.setTerrainData (nodeData);
		
		// Node aura - its a small node, the centre tile and the one map square to the right of it :)
		nodeCell.setAuraFromNode (nodeLocation);
		final ServerGridCell adjacentCell = (ServerGridCell) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (21);
		final OverlandMapTerrainData adjacentData = new OverlandMapTerrainData ();
		adjacentCell.setTerrainData (adjacentData);
		adjacentCell.setAuraFromNode (nodeLocation);		
		
		// Players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final PlayerDescription attackerPd = new PlayerDescription ();
		attackerPd.setPlayerID (2);
		attackerPd.setHuman (true);
		
		final MomTransientPlayerPrivateKnowledge attackerTrans = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails attacker = new PlayerServerDetails (attackerPd, null, null, null, attackerTrans);
		players.add (attacker);
		
		// Units
		final MemoryUnit attackingSpirit = new MemoryUnit ();
		attackingSpirit.setOwningPlayerID (attacker.getPlayerDescription ().getPlayerID ());
		attackingSpirit.setUnitLocation (nodeLocation);
		attackingSpirit.setUnitID ("UN177");	// Guardian spirit
		attackingSpirit.setStatus (UnitStatusID.ALIVE);
		
		// Set up object to test
		final IFogOfWarMidTurnChanges fogOfWarMidTurnChanges = mock (IFogOfWarMidTurnChanges.class);
		
		final OverlandMapServerUtils utils = new OverlandMapServerUtils ();
		utils.setFogOfWarMidTurnChanges (fogOfWarMidTurnChanges);
		
		// Run method
		utils.attemptToMeldWithNode (attackingSpirit, trueMap, players, sd, db);
		
		// Check results
		assertEquals ("UN177", nodeCell.getNodeSpiritUnitID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), nodeData.getNodeOwnerID ());
		assertEquals (attacker.getPlayerDescription ().getPlayerID (), adjacentData.getNodeOwnerID ());
		
		assertEquals (1, attackerTrans.getNewTurnMessage ().size ());
		final NewTurnMessageData attackerMsg = attackerTrans.getNewTurnMessage ().get (0);
		assertEquals (NewTurnMessageTypeID.NODE_CAPTURED, attackerMsg.getMsgType ());
		assertEquals (nodeLocation, attackerMsg.getLocation ());
		assertEquals ("UN177", attackerMsg.getBuildingOrUnitID ());
		assertNull (attackerMsg.getOtherUnitID ());
		assertNull (attackerMsg.getOtherPlayerID ());

		verify (fogOfWarMidTurnChanges).killUnitOnServerAndClients (attackingSpirit, KillUnitActionID.FREE, trueMap, players, sd, db);
		verify (fogOfWarMidTurnChanges).updatePlayerMemoryOfTerrain (trueTerrain, players, nodeLocation, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());

		final OverlandMapCoordinatesEx adjacentLocation = new OverlandMapCoordinatesEx ();
		adjacentLocation.setX (21);
		adjacentLocation.setY (10);
		adjacentLocation.setPlane (1);

		verify (fogOfWarMidTurnChanges).updatePlayerMemoryOfTerrain (trueTerrain, players, adjacentLocation, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
	}
}

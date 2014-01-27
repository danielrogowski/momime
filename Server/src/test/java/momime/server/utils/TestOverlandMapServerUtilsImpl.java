package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import momime.common.calculations.MomCityCalculationsImpl;
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
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.CityNameContainer;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
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
import com.ndg.random.RandomUtils;

/**
 * Tests the OverlandMapServerUtils class
 */
public final class TestOverlandMapServerUtilsImpl
{
	/**
	 * Tests the setContinentalRace method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetContinentalRace () throws Exception
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
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		
		// One cell of land
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		utils.setContinentalRace (map, continentalRace, 20, 10, 1, "RC01", db);
		assertEquals (1, continentalRace.get (1).countCellsEqualTo ("RC01"));

		// Mark a city radius around the tile - just to test a non-square area
		final MapCoordinates coords = new MapCoordinates ();
		coords.setX (20);
		coords.setY (10);

		for (final SquareMapDirection d : MomCityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
			if (CoordinateSystemUtils.moveCoordinates (sys, coords, d.getDirectionID ()))
				map.getPlane ().get (1).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);

		utils.setContinentalRace (map, continentalRace, 20, 10, 1, "RC02", db);
		assertEquals (21, continentalRace.get (1).countCellsEqualTo ("RC02"));
	}

	/**
	 * Tests the decideAllContinentalRaces method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDecideAllContinentalRaces () throws Exception
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
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		
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

		// But 2 of them is already used
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.getUsedCityName ().add ("Hanna");
		gsk.getUsedCityName ().add ("Emily");
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (2)).thenReturn (1);
		when (random.nextInt (1)).thenReturn (0);

		when (random.nextInt (4)).thenReturn (1);
		when (random.nextInt (3)).thenReturn (2);
		
		// Set up object to test
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
		utils.setRandomUtils (random);
		
		// Go through the first two remaining choices without a suffix
		assertEquals ("Rose", utils.generateCityName (gsk, race));
		assertEquals ("Mara", utils.generateCityName (gsk, race));

		// Now all 4 are used, the next 4 we generate will be suffixed with II
		assertEquals ("Emily II", utils.generateCityName (gsk, race));
		assertEquals ("Rose II", utils.generateCityName (gsk, race));
		assertEquals ("Mara II", utils.generateCityName (gsk, race));
		assertEquals ("Hanna II", utils.generateCityName (gsk, race));

		// And the next 4 we generate will be suffixed with III
		assertEquals ("Emily III", utils.generateCityName (gsk, race));
		assertEquals ("Rose III", utils.generateCityName (gsk, race));
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
		final FogOfWarMidTurnChanges fogOfWarMidTurnChanges = mock (FogOfWarMidTurnChanges.class);
		
		final OverlandMapServerUtilsImpl utils = new OverlandMapServerUtilsImpl ();
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

		verify (fogOfWarMidTurnChanges).killUnitOnServerAndClients (attackingSpirit, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fogOfWarMidTurnChanges).updatePlayerMemoryOfTerrain (trueTerrain, players, nodeLocation, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());

		final OverlandMapCoordinatesEx adjacentLocation = new OverlandMapCoordinatesEx ();
		adjacentLocation.setX (21);
		adjacentLocation.setY (10);
		adjacentLocation.setPlane (1);

		verify (fogOfWarMidTurnChanges).updatePlayerMemoryOfTerrain (trueTerrain, players, adjacentLocation, sd.getFogOfWarSetting ().getTerrainAndNodeAuras ());
	}
	
	/**
	 * Tests the totalPlayerPopulation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testTotalPlayerPopulation () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sys);
		
		final OverlandMapCityData ourCity1 = new OverlandMapCityData ();
		ourCity1.setCityOwnerID (2);
		ourCity1.setCityPopulation (2000);
		map.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setCityData (ourCity1);

		final OverlandMapCityData ourCity2 = new OverlandMapCityData ();
		ourCity2.setCityOwnerID (2);
		ourCity2.setCityPopulation (3000);
		map.getPlane ().get (1).getRow ().get (15).getCell ().get (20).setCityData (ourCity2);

		final OverlandMapCityData ourCity3 = new OverlandMapCityData ();
		ourCity3.setCityOwnerID (2);
		map.getPlane ().get (1).getRow ().get (15).getCell ().get (25).setCityData (ourCity3);

		final OverlandMapCityData theirCity = new OverlandMapCityData ();
		theirCity.setCityOwnerID (3);
		theirCity.setCityPopulation (4000);
		map.getPlane ().get (0).getRow ().get (5).getCell ().get (20).setCityData (theirCity);

		final OverlandMapCityData nobodysCity = new OverlandMapCityData ();
		nobodysCity.setCityPopulation (5000);
		map.getPlane ().get (0).getRow ().get (5).getCell ().get (25).setCityData (nobodysCity);
		
		// Run method
		assertEquals (5000, new OverlandMapServerUtilsImpl ().totalPlayerPopulation (map, 2, sys, db));
	}
}

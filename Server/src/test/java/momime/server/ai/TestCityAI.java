package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CityAI class
 */
public final class TestCityAI
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEServerUnitTests");

	/**
	 * Tests the chooseCityLocation method
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
	 * @throws MomException If the food production values from the XML database aren't multiples of 2
	 */
	@Test
	public final void testChooseCityLocation () throws JAXBException, RecordNotFoundException, MomException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (serverDB, "60x40", "LP03", "NS03", "DL05");
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sd.getMapSize ());
		final int totalFoodBonusFromBuildings = MomServerCityCalculations.calculateTotalFoodBonusFromBuildings (db, debugLogger);

		// Fill map with ocean, then we can't build a city anywhere
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);

					cell.setTerrainData (terrain);
				}

		final OverlandMapCoordinates ocean = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertNull (ocean);

		// Fill map with tundra, then we can build a city anywhere but none of them are very good
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					cell.getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_TUNDRA);

		final OverlandMapCoordinates tundra = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (0, tundra.getX ());
		assertEquals (0, tundra.getY ());
		assertEquals (0, tundra.getPlane ());

		// If we put 3 dots of grass, there's only one exact spot where the city radius will include all of them
		// Also set the entire other plane to grass, to prove that it doesn't get considered
		map.getPlane ().get (0).getRow ().get (13).getCell ().get (20).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		map.getPlane ().get (0).getRow ().get (13).getCell ().get (24).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);
		map.getPlane ().get (0).getRow ().get (10).getCell ().get (22).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);

		for (final MapRowOfMemoryGridCells row : map.getPlane ().get (1).getRow ())
			for (final MemoryGridCell cell : row.getCell ())
				cell.getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_GRASS);

		final OverlandMapCoordinates grass = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (22, grass.getX ());
		assertEquals (12, grass.getY ());
		assertEquals (0, grass.getPlane ());

		// Putting some gems there is great
		map.getPlane ().get (0).getRow ().get (12).getCell ().get (22).getTerrainData ().setMapFeatureID ("MF01");

		final OverlandMapCoordinates gems = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (22, gems.getX ());
		assertEquals (12, gems.getY ());
		assertEquals (0, gems.getPlane ());

		// Putting a lair there instead means we can't build a city there
		// Note there's no longer a spot where can include all 3 grass tiles, so it picks the first coordinates that it encounters that includes two of the grass tiles
		map.getPlane ().get (0).getRow ().get (12).getCell ().get (22).getTerrainData ().setMapFeatureID ("MF13");

		final OverlandMapCoordinates lair = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (20, lair.getX ());
		assertEquals (11, lair.getY ());
		assertEquals (0, lair.getPlane ());

		// Put a river just to the right of the city - so it would be included in the previous radius, so we get the food from it anyway
		// But we don't get the 20% gold bonus from it unless we move the city to that location, so this proves that the gold bonus is taken into account
		map.getPlane ().get (0).getRow ().get (11).getCell ().get (21).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_RIVER);

		final OverlandMapCoordinates river = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (21, river.getX ());
		assertEquals (11, river.getY ());
		assertEquals (0, river.getPlane ());

		// Mountains produce no food and no gold, just like tundra, but they go give a 5% production bonus
		// This carefully places 5 mountain tiles just along underneath where the city was previously chosen, totally 25% bonus so it then
		// moves the city to include this 25% in preference to the 20% gold bonus from the river tile
		map.getPlane ().get (0).getRow ().get (13).getCell ().get (19).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		map.getPlane ().get (0).getRow ().get (14).getCell ().get (20).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		map.getPlane ().get (0).getRow ().get (14).getCell ().get (21).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		map.getPlane ().get (0).getRow ().get (14).getCell ().get (22).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		map.getPlane ().get (0).getRow ().get (13).getCell ().get (23).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);

		final OverlandMapCoordinates mountain = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (21, mountain.getX ());
		assertEquals (12, mountain.getY ());
		assertEquals (0, mountain.getPlane ());

		// Iron ore has a +4 quality rating, so putting some on the row that just *isn't* included if we take those mountains means it will still choose
		// the 25% bonus from the mountains rather than the 20% + 4 from the river and iron ore
		map.getPlane ().get (0).getRow ().get (9).getCell ().get (21).getTerrainData ().setMapFeatureID ("MF04");

		final OverlandMapCoordinates ironOre = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (21, ironOre.getX ());
		assertEquals (12, ironOre.getY ());
		assertEquals (0, ironOre.getPlane ());

		// Coal has a +6 quality rating, so putting some on the row that just *isn't* included if we take those mountains means it will now go
		// back to the 20% + 6 from the river and coal rather than the 25% from the mountains
		map.getPlane ().get (0).getRow ().get (9).getCell ().get (21).getTerrainData ().setMapFeatureID ("MF05");

		final OverlandMapCoordinates coal = CityAI.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db, debugLogger);
		assertEquals (21, coal.getX ());
		assertEquals (11, coal.getY ());
		assertEquals (0, coal.getPlane ());
	}

	/**
	 * Tests the findWorkersToConvertToFarmers method
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If there is a building that cannot be found in the DB
	 * @throws MomException If a city's race has no farmers defined or those farmers have no ration production defined
	 */
	@Test
	public final void testFindWorkersToConvertToFarmers () throws JAXBException, RecordNotFoundException, MomException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (serverDB, "60x40", "LP03", "NS03", "DL05");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// If we want trade goods cities and there are none, no updates will take place
		for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
		{
			final OverlandMapCityData cityData = new OverlandMapCityData ();
			cityData.setCityOwnerID (2);
			cityData.setCityPopulation (4000);
			cityData.setMinimumFarmers (1);
			cityData.setNumberOfRebels (1);
			cityData.setOptionalFarmers (0);
			cityData.setCityRaceID ("RC05");		// High men (standard ration production)
			cityData.setCurrentlyConstructingBuildingOrUnitID (CommonDatabaseConstants.VALUE_BUILDING_HOUSING);
			trueTerrain.getPlane ().get (0).getRow ().get (20).getCell ().get (x).setCityData (cityData);
		}

		assertEquals (10, CityAI.findWorkersToConvertToFarmers (10, true, trueMap, player, db, sd, debugLogger));

		for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
			assertEquals (0, trueTerrain.getPlane ().get (0).getRow ().get (20).getCell ().get (x).getCityData ().getOptionalFarmers ().intValue ());

		// In the situation where we have 62 cities - 2 building trade goods and 60 building something else - and we need 5 rations, there are
		// only 2 possible outcomes - use 2 farmers in city A and 1 in city B, or use 1 in city A and 2 in city B
		// Those 3 farmers will then produce 6 rations, so 1 leftover, hence -2 result
		for (int x = 0; x < 2; x++)
		{
			final OverlandMapCityData cityData = new OverlandMapCityData ();
			cityData.setCityOwnerID (2);
			cityData.setCityPopulation (4000);
			cityData.setMinimumFarmers (1);
			cityData.setNumberOfRebels (1);
			cityData.setOptionalFarmers (0);
			cityData.setCityRaceID ("RC05");		// High men (standard ration production)
			cityData.setCurrentlyConstructingBuildingOrUnitID (CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS);
			trueTerrain.getPlane ().get (0).getRow ().get (10).getCell ().get (x).setCityData (cityData);
		}

		assertEquals (-2, CityAI.findWorkersToConvertToFarmers (10, true, trueMap, player, db, sd, debugLogger));

		for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
			assertEquals (0, trueTerrain.getPlane ().get (0).getRow ().get (20).getCell ().get (x).getCityData ().getOptionalFarmers ().intValue ());

		switch (trueTerrain.getPlane ().get (0).getRow ().get (10).getCell ().get (0).getCityData ().getOptionalFarmers ())
		{
			case 1:
				assertEquals (2, trueTerrain.getPlane ().get (0).getRow ().get (10).getCell ().get (1).getCityData ().getOptionalFarmers ().intValue ());
				break;

			case 2:
				assertEquals (1, trueTerrain.getPlane ().get (0).getRow ().get (10).getCell ().get (1).getCityData ().getOptionalFarmers ().intValue ());
				break;

			default:
				fail ("Optional farmers in city A must be 1 or 2");
		}
	}
}
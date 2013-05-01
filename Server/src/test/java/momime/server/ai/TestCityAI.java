package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapRowOfMemoryGridCells;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.calculations.MomServerCityCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the CityAI class
 */
public final class TestCityAI
{
	/**
	 * Tests the chooseCityLocation method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we request an entry that can't be found in the database
	 * @throws MomException If the food production values from the XML database aren't multiples of 2
	 */
	@Test
	public final void testChooseCityLocation () throws IOException, JAXBException, RecordNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells map = ServerTestData.createOverlandMap (sd.getMapSize ());
		final int totalFoodBonusFromBuildings = new MomServerCityCalculations ().calculateTotalFoodBonusFromBuildings (db);

		// Fill map with ocean, then we can't build a city anywhere
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
				{
					final OverlandMapTerrainData terrain = new OverlandMapTerrainData ();
					terrain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);

					cell.setTerrainData (terrain);
				}

		// Set up test object
		final CityAI ai = new CityAI ();
		ai.setCityCalculations (new MomCityCalculations ());
		
		final OverlandMapCoordinates ocean = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
		assertNull (ocean);

		// Fill map with tundra, then we can build a city anywhere but none of them are very good
		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					cell.getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_TUNDRA);

		final OverlandMapCoordinates tundra = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
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

		final OverlandMapCoordinates grass = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
		assertEquals (22, grass.getX ());
		assertEquals (12, grass.getY ());
		assertEquals (0, grass.getPlane ());

		// Putting some gems there is great
		map.getPlane ().get (0).getRow ().get (12).getCell ().get (22).getTerrainData ().setMapFeatureID ("MF01");

		final OverlandMapCoordinates gems = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
		assertEquals (22, gems.getX ());
		assertEquals (12, gems.getY ());
		assertEquals (0, gems.getPlane ());

		// Putting a lair there instead means we can't build a city there
		// Note there's no longer a spot where can include all 3 grass tiles, so it picks the first coordinates that it encounters that includes two of the grass tiles
		map.getPlane ().get (0).getRow ().get (12).getCell ().get (22).getTerrainData ().setMapFeatureID ("MF13");

		final OverlandMapCoordinates lair = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
		assertEquals (20, lair.getX ());
		assertEquals (11, lair.getY ());
		assertEquals (0, lair.getPlane ());

		// Put a river just to the right of the city - so it would be included in the previous radius, so we get the food from it anyway
		// But we don't get the 20% gold bonus from it unless we move the city to that location, so this proves that the gold bonus is taken into account
		map.getPlane ().get (0).getRow ().get (11).getCell ().get (21).getTerrainData ().setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_RIVER);

		final OverlandMapCoordinates river = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
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

		final OverlandMapCoordinates mountain = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
		assertEquals (21, mountain.getX ());
		assertEquals (12, mountain.getY ());
		assertEquals (0, mountain.getPlane ());

		// Iron ore has a +4 quality rating, so putting some on the row that just *isn't* included if we take those mountains means it will still choose
		// the 25% bonus from the mountains rather than the 20% + 4 from the river and iron ore
		map.getPlane ().get (0).getRow ().get (9).getCell ().get (21).getTerrainData ().setMapFeatureID ("MF04");

		final OverlandMapCoordinates ironOre = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
		assertEquals (21, ironOre.getX ());
		assertEquals (12, ironOre.getY ());
		assertEquals (0, ironOre.getPlane ());

		// Coal has a +6 quality rating, so putting some on the row that just *isn't* included if we take those mountains means it will now go
		// back to the 20% + 6 from the river and coal rather than the 25% from the mountains
		map.getPlane ().get (0).getRow ().get (9).getCell ().get (21).getTerrainData ().setMapFeatureID ("MF05");

		final OverlandMapCoordinates coal = ai.chooseCityLocation (map, 0, sd, totalFoodBonusFromBuildings, db);
		assertEquals (21, coal.getX ());
		assertEquals (11, coal.getY ());
		assertEquals (0, coal.getPlane ());
	}

	/**
	 * Tests the findWorkersToConvertToFarmers method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If there is a building that cannot be found in the DB
	 * @throws MomException If a city's race has no farmers defined or those farmers have no ration production defined
	 */
	@Test
	public final void testFindWorkersToConvertToFarmers () throws IOException, JAXBException, RecordNotFoundException, MomException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);

		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);

		// Set up test object
		final MomServerCityCalculations serverCityCalculations = new MomServerCityCalculations ();
		serverCityCalculations.setMemoryBuildingUtils (new MemoryBuildingUtils ());
		
		final CityAI ai = new CityAI ();
		ai.setServerCityCalculations (serverCityCalculations);
		
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

		assertEquals (10, ai.findWorkersToConvertToFarmers (10, true, trueMap, player, db, sd));

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

		assertEquals (-2, ai.findWorkersToConvertToFarmers (10, true, trueMap, player, db, sd));

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

	/**
	 * Tests the decideWhatToBuild method
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testDecideWhatToBuild () throws IOException, JAXBException
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Map
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final List<MemoryBuilding> trueBuildings = new ArrayList<MemoryBuilding> ();

		// Need certain types of terrain in order to be able to construct all building types
		final OverlandMapTerrainData forest = new OverlandMapTerrainData ();
		forest.setTileTypeID (CommonDatabaseConstants.VALUE_TILE_TYPE_FOREST);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (19).setTerrainData (forest);

		final OverlandMapTerrainData mountain = new OverlandMapTerrainData ();
		mountain.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (20).setTerrainData (mountain);

		final OverlandMapTerrainData ocean = new OverlandMapTerrainData ();
		ocean.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_OCEAN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (21).setTerrainData (ocean);

		// Set up city
		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (20);
		cityLocation.setY (10);
		cityLocation.setPlane (1);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up test object
		final MemoryBuildingUtils memoryBuildingUtils = new MemoryBuildingUtils ();
		
		final MomServerCityCalculations serverCityCalculations = new MomServerCityCalculations ();
		serverCityCalculations.setCityCalculations (new MomCityCalculations ());
		serverCityCalculations.setMemoryBuildingUtils (memoryBuildingUtils);
		
		final CityAI ai = new CityAI ();
		ai.setMemoryBuildingUtils (memoryBuildingUtils);
		ai.setServerCityCalculations (serverCityCalculations);
		
		// Orcs can build absolutely everything
		// Sit in a loop adding every building that it decides upon until we get trade goods
		// Then check everything was built in the right order afterwards
		cityData.setCityRaceID ("RC09");
		while (!CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingOrUnitID ()))
		{
			ai.decideWhatToBuild (cityLocation, cityData, trueTerrain, trueBuildings, sd, db);
			if (!CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingOrUnitID ()))
			{
				final OverlandMapCoordinates buildingLocation = new OverlandMapCoordinates ();
				buildingLocation.setX (20);
				buildingLocation.setY (10);
				buildingLocation.setPlane (1);

				final MemoryBuilding building = new MemoryBuilding ();
				building.setCityLocation (buildingLocation);
				building.setBuildingID (cityData.getCurrentlyConstructingBuildingOrUnitID ());

				trueBuildings.add (building);
			}
		}

		assertEquals (31, trueBuildings.size ());
		assertEquals ("BL32", trueBuildings.get (0).getBuildingID ());		// Growth - Try to build a Granary but can't, Builders' Hall is a prerequisite of it
		assertEquals ("BL29", trueBuildings.get (1).getBuildingID ());		// Growth - Granary
		assertEquals ("BL08", trueBuildings.get (2).getBuildingID ());		// Growth - Try to build a Farmers' Market but can't, Marketplace is a prerequisite of it, and Blacksmith is a prerequisite of that
		assertEquals ("BL26", trueBuildings.get (3).getBuildingID ());		// Growth - Try to build a Farmers' Market but can't, Marketplace is a prerequisite of it
		assertEquals ("BL30", trueBuildings.get (4).getBuildingID ());		// Growth - Farmer's Market
		assertEquals ("BL15", trueBuildings.get (5).getBuildingID ());		// Production - Sawmill
		assertEquals ("BL31", trueBuildings.get (6).getBuildingID ());		// Production - Foresters' Guild
		assertEquals ("BL34", trueBuildings.get (7).getBuildingID ());		// Production - Miners' Guild
		assertEquals ("BL09", trueBuildings.get (8).getBuildingID ());		// Production - Try to build an Animsts' Guild but can't, Stables is a prerequisite of it
		assertEquals ("BL22", trueBuildings.get (9).getBuildingID ());		// Production - Try to build an Animsts' Guild but can't, Temple is a prerequisite of it, and Shrine is a prerequisite of that
		assertEquals ("BL23", trueBuildings.get (10).getBuildingID ());		// Production - Try to build an Animsts' Guild but can't, Temple is a prerequisite of it
		assertEquals ("BL10", trueBuildings.get (11).getBuildingID ());		// Production - Animsts' Guild
		assertEquals ("BL16", trueBuildings.get (12).getBuildingID ());		// Production - Try to build a Mechanicians' Guild but can't, Library - Sages' Guild - University is a prerequisite of it
		assertEquals ("BL17", trueBuildings.get (13).getBuildingID ());		// Production - Try to build a Mechanicians' Guild but can't, Sages' Guild - University is a prerequisite of it
		assertEquals ("BL20", trueBuildings.get (14).getBuildingID ());		// Production - Try to build a Mechanicians' Guild but can't, University is a prerequisite of it
		assertEquals ("BL33", trueBuildings.get (15).getBuildingID ());		// Production - Mechanicians' Guild
		assertEquals ("BL19", trueBuildings.get (16).getBuildingID ());		// Research - Try to build an Wizards' Guild but can't, Alchemists' Guild is a prerequisite of it
		assertEquals ("BL21", trueBuildings.get (17).getBuildingID ());		// Research - Wizards' Guild
		assertEquals ("BL24", trueBuildings.get (18).getBuildingID ());		// Unrest+Magic Power - Parthenon
		assertEquals ("BL25", trueBuildings.get (19).getBuildingID ());		// Unrest+Magic Power - Cathedral
		assertEquals ("BL27", trueBuildings.get (20).getBuildingID ());		// Gold - Bank
		assertEquals ("BL12", trueBuildings.get (21).getBuildingID ());		// Gold - Try to build an Merchants' Guild but can't, Ship Yard is a prerequisite of it, and Ship Wrights' Guild is a prerequisite of that
		assertEquals ("BL13", trueBuildings.get (22).getBuildingID ());		// Gold - Try to build an Merchants' Guild but can't, Ship Yard is a prerequisite of it
		assertEquals ("BL28", trueBuildings.get (23).getBuildingID ());		// Gold - Merchants' Guild
		assertEquals ("BL18", trueBuildings.get (24).getBuildingID ());		// Unrest without Magic Power - Oracle
		assertEquals ("BL03", trueBuildings.get (25).getBuildingID ());		// Units - Barracks
		assertEquals ("BL04", trueBuildings.get (26).getBuildingID ());		// Units - Armoury
		assertEquals ("BL05", trueBuildings.get (27).getBuildingID ());		// Units - Fighters' Guild
		assertEquals ("BL06", trueBuildings.get (28).getBuildingID ());		// Units - Armourers' Guild
		assertEquals ("BL07", trueBuildings.get (29).getBuildingID ());		// Units - War College
		assertEquals ("BL11", trueBuildings.get (30).getBuildingID ());		// Units - Fantastic Stables

		// Try again with Barbarians, who can't build Animsts' Guilds, Universities or Cathedrals
		trueBuildings.clear ();
		cityData.setCityRaceID ("RC01");
		cityData.setCurrentlyConstructingBuildingOrUnitID (null);
		while (!CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingOrUnitID ()))
		{
			ai.decideWhatToBuild (cityLocation, cityData, trueTerrain, trueBuildings, sd, db);
			if (!CommonDatabaseConstants.VALUE_BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingOrUnitID ()))
			{
				final OverlandMapCoordinates buildingLocation = new OverlandMapCoordinates ();
				buildingLocation.setX (20);
				buildingLocation.setY (10);
				buildingLocation.setPlane (1);

				final MemoryBuilding building = new MemoryBuilding ();
				building.setCityLocation (buildingLocation);
				building.setBuildingID (cityData.getCurrentlyConstructingBuildingOrUnitID ());

				trueBuildings.add (building);
			}
		}

		// The test here isn't so much *what* we now can't build, but rather the order we now choose to build them in and why, e.g.
		// We now construct a Shrine+Temple later on, based on their own merits as producers of magic power+unrest - Orcs built them just so that they could get an Animists' Guild
		// Even though we could build a Ship Wrights' Guild, there's no point in doing so, because we can't eventually get a Merchants' Guild from it because of the missing University+Bank
		assertEquals (18, trueBuildings.size ());
		assertEquals ("BL32", trueBuildings.get (0).getBuildingID ());		// Growth - Try to build a Granary but can't, Builders' Hall is a prerequisite of it
		assertEquals ("BL29", trueBuildings.get (1).getBuildingID ());		// Growth - Granary
		assertEquals ("BL08", trueBuildings.get (2).getBuildingID ());		// Growth - Try to build a Farmers' Market but can't, Marketplace is a prerequisite of it, and Blacksmith is a prerequisite of that
		assertEquals ("BL26", trueBuildings.get (3).getBuildingID ());		// Growth - Try to build a Farmers' Market but can't, Marketplace is a prerequisite of it
		assertEquals ("BL30", trueBuildings.get (4).getBuildingID ());		// Growth - Farmer's Market
		assertEquals ("BL15", trueBuildings.get (5).getBuildingID ());		// Production - Sawmill
		assertEquals ("BL31", trueBuildings.get (6).getBuildingID ());		// Production - Foresters' Guild
		assertEquals ("BL34", trueBuildings.get (7).getBuildingID ());		// Production - Miners' Guild
		assertEquals ("BL16", trueBuildings.get (8).getBuildingID ());		// Research - Library
		assertEquals ("BL17", trueBuildings.get (9).getBuildingID ());		// Research - Sages' Guild
		assertEquals ("BL22", trueBuildings.get (10).getBuildingID ());		// Unrest+Magic Power - Shrine
		assertEquals ("BL23", trueBuildings.get (11).getBuildingID ());		// Unrest+Magic Power - Temple
		assertEquals ("BL24", trueBuildings.get (12).getBuildingID ());		// Unrest+Magic Power - Parthenon
		assertEquals ("BL03", trueBuildings.get (13).getBuildingID ());		// Units - Barracks
		assertEquals ("BL04", trueBuildings.get (14).getBuildingID ());		// Units - Armoury
		assertEquals ("BL05", trueBuildings.get (15).getBuildingID ());		// Units - Fighters' Guild
		assertEquals ("BL06", trueBuildings.get (16).getBuildingID ());		// Units - Armourers' Guild
		assertEquals ("BL19", trueBuildings.get (17).getBuildingID ());		// Units - Alchemists' Guild
	}
}
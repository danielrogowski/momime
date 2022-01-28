package momime.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.areas.storage.MapArea2D;
import com.ndg.map.areas.storage.MapArea2DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.database.TileTypeEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.ServerCityCalculationsImpl;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the CityAI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCityAIImpl extends ServerTestData
{
	/**
	 * Tests the chooseCityLocation method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseCityLocation () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int x = 1; x <= 4; x++)
		{
			final TileTypeEx tileType = new TileTypeEx ();
			tileType.setCanBuildCity (x != 4);		// So 24, 10 is invalid because of tile type
			when (db.findTileType ("TT0" + x, "chooseCityLocation")).thenReturn (tileType);
		}		
		
		for (int x = 2; x <= 3; x++)
		{
			final MapFeatureEx mapFeature = new MapFeatureEx ();
			mapFeature.setCanBuildCity (x != 3);		// So 23, 10 is invalid because of map feature (e.g. lair)
			when (db.findMapFeature ("MF0" + x, "chooseCityLocation")).thenReturn (mapFeature);
		}		
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);
		
		// Maps
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (mapSize);
		final MapVolumeOfMemoryGridCells knownTerrain = createOverlandMap (mapSize);
		
		for (int x = 1; x <= 5; x++)
		{
			final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
			terrainData.setTileTypeID ("TT0" + x);
			if ((x >= 2) && (x <= 3))
				terrainData.setMapFeatureID ("MF0" + x);		// Chosen tile 22, 10 also has a map feature, but we can build a city on it (e.g. gold)
			
			knownTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20 + x).setTerrainData (terrainData);
		}
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (knownTerrain);
		
		// Area too close to other cities
		final MapArea2D<Boolean> withinExistingCityRadius = new MapArea2DArrayListImpl<Boolean> ();
		withinExistingCityRadius.setCoordinateSystem (mapSize);
		for (int y = 0; y < mapSize.getHeight (); y++)
			for (int x = 0; x < mapSize.getWidth (); x++)
				withinExistingCityRadius.set (x, y, (x == 25) && (y == 10));		// So 25, 10 is blocked even though it has the highest estimate 
		
		final CityCalculations cityCalc = mock (CityCalculations.class);
		when (cityCalc.markWithinExistingCityRadius (trueTerrain, knownTerrain, 1, mapSize)).thenReturn (withinExistingCityRadius);
		
		// Session variables
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Quality evaluations
		final AICityCalculations aiCityCalc = mock (AICityCalculations.class);
		for (int x = 1; x <= 2; x++)
			when (aiCityCalc.evaluateCityQuality (new MapCoordinates3DEx (20 + x, 10, 1), true, true, mem, mom)).thenReturn (x * 100);
		
		// Set up object to test
		final CityAIImpl ai = new CityAIImpl ();
		ai.setCityCalculations (cityCalc);
		ai.setAiCityCalculations (aiCityCalc);
		
		// Call method
		final MapCoordinates3DEx location = ai.chooseCityLocation (mem, 1, true, mom, "Unit test");
		
		// Check results
		assertEquals (22, location.getX ());
		assertEquals (10, location.getY ());
		assertEquals (1, location.getZ ());
	}

	/**
	 * Tests the decideWhatToBuild method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDecideWhatToBuild () throws Exception
	{
		final CommonDatabase db = loadServerDatabase ();

		// Map
		final MomSessionDescription sd = createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sd.getOverlandMapSize ());
		final List<MemoryBuilding> trueBuildings = new ArrayList<MemoryBuilding> ();

		// Need certain types of terrain in order to be able to construct all building types
		final OverlandMapTerrainData forest = new OverlandMapTerrainData ();
		forest.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_FOREST);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (19).setTerrainData (forest);

		final OverlandMapTerrainData mountain = new OverlandMapTerrainData ();
		mountain.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_MOUNTAIN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (20).setTerrainData (mountain);

		final OverlandMapTerrainData ocean = new OverlandMapTerrainData ();
		ocean.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_OCEAN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (21).setTerrainData (ocean);

		// Set up city
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardObjectiveID ("WO01");

		// Set up test object
		final MemoryBuildingUtilsImpl memoryBuildingUtils = new MemoryBuildingUtilsImpl ();
		
		final CityCalculationsImpl cityCalc = new CityCalculationsImpl ();
		cityCalc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		final ServerCityCalculationsImpl serverCityCalculations = new ServerCityCalculationsImpl ();
		serverCityCalculations.setCityCalculations (cityCalc);
		serverCityCalculations.setMemoryBuildingUtils (memoryBuildingUtils);
		
		final CityAIImpl ai = new CityAIImpl ();
		ai.setMemoryBuildingUtils (memoryBuildingUtils);
		ai.setServerCityCalculations (serverCityCalculations);
		
		// Orcs can build absolutely everything
		// Sit in a loop adding every building that it decides upon until we get trade goods
		// Then check everything was built in the right order afterwards
		cityData.setCityRaceID ("RC09");
		while (!CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))
		{
			ai.decideWhatToBuild (wizardDetails, cityLocation, cityData, 2, false, 0, null, null, trueTerrain, trueBuildings, sd, db);
			if (!CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))
			{
				final MapCoordinates3DEx buildingLocation = new MapCoordinates3DEx (20, 10, 1);

				final MemoryBuilding building = new MemoryBuilding ();
				building.setCityLocation (buildingLocation);
				building.setBuildingID (cityData.getCurrentlyConstructingBuildingID ());

				trueBuildings.add (building);
			}
		}

		assertEquals (33, trueBuildings.size ());
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
		assertEquals ("BL03", trueBuildings.get (24).getBuildingID ());		// Units - Barracks
		assertEquals ("BL04", trueBuildings.get (25).getBuildingID ());		// Units - Armoury
		assertEquals ("BL05", trueBuildings.get (26).getBuildingID ());		// Units - Fighters' Guild
		assertEquals ("BL06", trueBuildings.get (27).getBuildingID ());		// Units - Armourers' Guild
		assertEquals ("BL07", trueBuildings.get (28).getBuildingID ());		// Units - War College
		assertEquals ("BL11", trueBuildings.get (29).getBuildingID ());		// Units - Fantastic Stables
		assertEquals ("BL14", trueBuildings.get (30).getBuildingID ());		// Naval Units - Maritime Guild
		assertEquals ("BL18", trueBuildings.get (31).getBuildingID ());		// Unrest without Magic Power - Oracle
		assertEquals ("BL35", trueBuildings.get (32).getBuildingID ());		// Defence - City Walls

		// Try again with Barbarians, who can't build Animsts' Guilds, Universities or Cathedrals
		trueBuildings.clear ();
		cityData.setCityRaceID ("RC01");
		cityData.setCurrentlyConstructingBuildingID (null);
		while (!CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))
		{
			ai.decideWhatToBuild (wizardDetails, cityLocation, cityData, 2, false, 0, null, null, trueTerrain, trueBuildings, sd, db);
			if (!CommonDatabaseConstants.BUILDING_TRADE_GOODS.equals (cityData.getCurrentlyConstructingBuildingID ()))
			{
				final MemoryBuilding building = new MemoryBuilding ();
				building.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
				building.setBuildingID (cityData.getCurrentlyConstructingBuildingID ());

				trueBuildings.add (building);
			}
		}

		// The test here isn't so much *what* we now can't build, but rather the order we now choose to build them in and why, e.g.
		// We now construct a Shrine+Temple later on, based on their own merits as producers of magic power+unrest - Orcs built them just so that they could get an Animists' Guild
		// Even though we could build a Ship Wrights' Guild, there's no point in doing so, because we can't eventually get a Merchants' Guild from it because of the missing University+Bank
		assertEquals (23, trueBuildings.size ());
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
		assertEquals ("BL09", trueBuildings.get (17).getBuildingID ());		// Stables
		assertEquals ("BL19", trueBuildings.get (18).getBuildingID ());		// Units - Alchemists' Guild
		assertEquals ("BL12", trueBuildings.get (19).getBuildingID ());		// Naval Units - Ship Wrights' Guild
		assertEquals ("BL13", trueBuildings.get (20).getBuildingID ());		// Naval Units - Ship Yard
		assertEquals ("BL14", trueBuildings.get (21).getBuildingID ());		// Naval Units - Maritime Guild
		assertEquals ("BL35", trueBuildings.get (22).getBuildingID ());		// Defence - City Walls
	}
}
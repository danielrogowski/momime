package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.GenerateTestData;
import momime.common.database.OverlandMapSize;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.RaceEx;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the calculations in the CityProductionCalculationsImpl class
 */
public final class TestCityProductionCalculationsImpl
{
	/**
	 * Tests the calculateAllCityProductions method in normal mode of calculating the production of an existing city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateAllCityProductions () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final RaceEx race = new RaceEx ();
		race.setMineralBonusMultiplier (2);
		when (db.findRace ("RC01", "calculateAllCityProductions")).thenReturn (race);
		
		final PickType pickType = new PickType ();
		pickType.setPickTypeID ("X");
		when (db.getPickType ()).thenReturn (Arrays.asList (pickType));
		
		final Plane plane = new Plane ();
		when (db.findPlane (1, "calculateAllCityProductions")).thenReturn (plane);
		
		final List<Building> buildingDefs = new ArrayList<Building> ();
		for (int n = 1; n <= 5; n++)
		{
			final Building building = new Building ();
			building.setBuildingID ((n == 5) ? CommonDatabaseConstants.BUILDING_FORTRESS : "BL0" + n);
			buildingDefs.add (building);
		}
		
		when (db.getBuilding ()).thenReturn (buildingDefs);
				
		// Session description
		final OverlandMapSize mapSize = GenerateTestData.createOverlandMapSize ();
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);
		sd.setDifficultyLevel (difficultyLevel);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails cityOwner = new PlayerPublicDetails (null, pub, null);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 3, "calculateAllCityProductions")).thenReturn (cityOwner);
		
		// Picks
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.countPicksOfType (pub.getPick (), "X", true, db)).thenReturn (5);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (mapSize);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCityOwnerID (3);
		cityData.setCityPopulation (15483);
		cityData.setMinimumFarmers (3);
		cityData.setNumberOfRebels (3);
		cityData.setOptionalFarmers (2);		// So 7 workers
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), "BL01")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), "BL03")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), "BL04")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (new MemoryBuilding ());
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setBuildingIdSoldThisTurn ("BL04");

		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.addProductionAndConsumptionFromBuilding (any (CityProductionBreakdownsEx.class),
			eq (buildingDefs.get (0)), eq (null), eq (pub.getPick ()), eq (db))).thenReturn (1);
		when (cityCalculations.addProductionAndConsumptionFromBuilding (any (CityProductionBreakdownsEx.class),
			eq (buildingDefs.get (1)), eq (null), eq (pub.getPick ()), eq (db))).thenReturn (2);
		when (cityCalculations.addProductionAndConsumptionFromBuilding (any (CityProductionBreakdownsEx.class),
			eq (buildingDefs.get (2)), eq (null), eq (pub.getPick ()), eq (db))).thenReturn (3);
		when (cityCalculations.addProductionAndConsumptionFromBuilding (any (CityProductionBreakdownsEx.class),
			eq (buildingDefs.get (3)), eq (null), eq (pub.getPick ()), eq (db))).thenReturn (4);
		when (cityCalculations.addProductionAndConsumptionFromBuilding (any (CityProductionBreakdownsEx.class),
			eq (buildingDefs.get (4)), eq (null), eq (pub.getPick ()), eq (db))).thenReturn (5);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		for (int n = 1; n <= 3; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setCitySpellEffectID ("SE0" + n);
			
			if (n <= 2)
				spell.setCityLocation (new MapCoordinates3DEx (18 + n, 10, 1));
			
			spells.add (spell);
		}
		
		// Components of calculation done in other methods
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		when (cityCalculations.listCityFoodProductionFromTerrainTiles (map, new MapCoordinates3DEx (20, 10, 1), mapSize, db)).thenReturn (food);

		final CityProductionBreakdown production = new CityProductionBreakdown ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		when (cityCalculations.listCityProductionPercentageBonusesFromTerrainTiles (map, new MapCoordinates3DEx (20, 10, 1), mapSize, db)).thenReturn (production);

		final CityProductionBreakdown gold = new CityProductionBreakdown ();
		gold.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		when (cityCalculations.addGoldFromTaxes (cityData, "TR02", db)).thenReturn (gold);

		final CityProductionBreakdown rations = new CityProductionBreakdown ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		when (cityCalculations.addRationsEatenByPopulation (cityData)).thenReturn (rations);
		
		// Set up object to test
		final MemoryMaintainedSpellUtils memoryMaintainedSpellUtils = mock (MemoryMaintainedSpellUtils.class);
		
		final CityProductionCalculationsImpl calc = new CityProductionCalculationsImpl ();
		calc.setCityCalculations (cityCalculations);
		calc.setMultiplayerSessionUtils (multiplayerSessionUtils);
		calc.setMemoryBuildingUtils (memoryBuildingUtils);
		calc.setPlayerPickUtils (playerPickUtils);
		calc.setMemoryMaintainedSpellUtils (memoryMaintainedSpellUtils);
		
		// Call method
		final CityProductionBreakdownsEx productions = calc.calculateAllCityProductions
			(players, map, buildings, spells, new MapCoordinates3DEx (20, 10, 1), "TR02", sd, true, false, db);
		
		// Check results
		assertEquals (4, productions.getProductionType ().size ());
		assertSame (rations, productions.getProductionType ().get (0));
		assertSame (production, productions.getProductionType ().get (1));
		assertSame (gold, productions.getProductionType ().get (2));
		assertSame (food, productions.getProductionType ().get (3));
		
		verify (cityCalculations, times (1)).addProductionFromPopulation (productions, race,
			CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, 5, new MapCoordinates3DEx (20, 10, 1), buildings, db);
		verify (cityCalculations, times (1)).addProductionFromPopulation (productions, race,
			CommonDatabaseConstants.POPULATION_TASK_ID_WORKER, 7, new MapCoordinates3DEx (20, 10, 1), buildings, db);
		verify (cityCalculations, times (1)).addProductionFromPopulation (productions, race,
			CommonDatabaseConstants.POPULATION_TASK_ID_REBEL, 3, new MapCoordinates3DEx (20, 10, 1), buildings, db);
		
		verify (cityCalculations, times (1)).addProductionAndConsumptionFromBuilding (productions, buildingDefs.get (0), null, pub.getPick (), db);
		verify (cityCalculations, times (0)).addProductionAndConsumptionFromBuilding (productions, buildingDefs.get (1), null, pub.getPick (), db);		// This city does not have it
		verify (cityCalculations, times (1)).addProductionAndConsumptionFromBuilding (productions, buildingDefs.get (2), null, pub.getPick (), db);
		verify (cityCalculations, times (0)).addProductionAndConsumptionFromBuilding (productions, buildingDefs.get (3), null, pub.getPick (), db);		// Its sold
		verify (cityCalculations, times (0)).addProductionAndConsumptionFromBuilding (productions, buildingDefs.get (4), null, pub.getPick (), db);		// Fortress has special rules
		
		verify (cityCalculations, times (1)).addProductionFromFortressPickType (productions, pickType, 5, db);
		verify (cityCalculations, times (1)).addProductionFromFortressPlane (productions, plane, db);
		
		verify (cityCalculations, times (0)).addProductionFromSpell (productions, spells.get (0), 4, db);		// Wrong location
		verify (cityCalculations, times (1)).addProductionFromSpell (productions, spells.get (1), 4, db);
		verify (cityCalculations, times (0)).addProductionFromSpell (productions, spells.get (2), 4, db);		// No location (spell that's not targeted on a city)
		
		verify (cityCalculations, times (1)).addProductionFromMapFeatures (productions, map, new MapCoordinates3DEx (20, 10, 1), mapSize, db, 2, 0);
		
		verify (cityCalculations, times (1)).halveAddPercentageBonusAndCapProduction (cityOwner, rations, 0, difficultyLevel, db);
		verify (cityCalculations, times (1)).halveAddPercentageBonusAndCapProduction (cityOwner, production, 0, difficultyLevel, db);
		verify (cityCalculations, times (1)).halveAddPercentageBonusAndCapProduction (cityOwner, gold, 0, difficultyLevel, db);
		verify (cityCalculations, times (1)).halveAddPercentageBonusAndCapProduction (cityOwner, food, 0, difficultyLevel, db);
		
		verify (cityCalculations, times (1)).calculateGoldTradeBonus (gold, map, new MapCoordinates3DEx (20, 10, 1), null, mapSize, db);
	}	
}
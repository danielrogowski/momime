package momime.common.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the calculations in the CityProductionCalculationsImpl class
 */
@ExtendWith(MockitoExtension.class)
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
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (mapSize);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Players
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final PlayerPublicDetails cityOwnerPlayer = new PlayerPublicDetails (null, null, null);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 3, "calculateAllCityProductions")).thenReturn (cityOwnerPlayer);
		
		// Wizards
		final KnownWizardDetails cityOwnerWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 3, "calculateAllCityProductions")).thenReturn (cityOwnerWizard);
		
		// Picks
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.countPicksOfType (cityOwnerWizard.getPick (), "X", true, db)).thenReturn (5);
		
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
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), "BL01")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), "BL02")).thenReturn (null);
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), "BL03")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), "BL04")).thenReturn (new MemoryBuilding ());
		when (memoryBuildingUtils.findBuilding (mem.getBuilding (), new MapCoordinates3DEx (20, 10, 1), CommonDatabaseConstants.BUILDING_FORTRESS)).thenReturn (new MemoryBuilding ());
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setBuildingIdSoldThisTurn ("BL04");

		final CityCalculations cityCalculations = mock (CityCalculations.class);
		when (cityCalculations.addProductionAndConsumptionFromBuilding (any (CityProductionBreakdownsEx.class),
			eq (buildingDefs.get (0)), isNull (), eq (cityOwnerWizard.getPick ()), eq (db))).thenReturn (1);
		when (cityCalculations.addProductionAndConsumptionFromBuilding (any (CityProductionBreakdownsEx.class),
			eq (buildingDefs.get (2)), isNull (), eq (cityOwnerWizard.getPick ()), eq (db))).thenReturn (3);
		
		// Spells
		for (int n = 1; n <= 3; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setCitySpellEffectID ("SE0" + n);
			
			if (n <= 2)
				spell.setCityLocation (new MapCoordinates3DEx (18 + n, 10, 1));
			
			mem.getMaintainedSpell ().add (spell);
		}
		
		// Components of calculation done in other methods
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		when (cityCalculations.listCityFoodProductionFromTerrainTiles (map, new MapCoordinates3DEx (20, 10, 1), mapSize, db)).thenReturn (food);

		final CityProductionBreakdown production = new CityProductionBreakdown ();
		production.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
		when (cityCalculations.listCityProductionPercentageBonusesFromTerrainTiles (map, mem.getMaintainedSpell (),
			new MapCoordinates3DEx (20, 10, 1), mapSize, db)).thenReturn (production);

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
		calc.setKnownWizardUtils (knownWizardUtils);
		
		// Call method
		final CityProductionBreakdownsEx productions = calc.calculateAllCityProductions
			(players, mem, new MapCoordinates3DEx (20, 10, 1), "TR02", sd, null, true, false, db);
		
		// Check results
		assertEquals (4, productions.getProductionType ().size ());
		assertSame (rations, productions.getProductionType ().get (0));
		assertSame (production, productions.getProductionType ().get (1));
		assertSame (gold, productions.getProductionType ().get (2));
		assertSame (food, productions.getProductionType ().get (3));
		
		verify (cityCalculations).addProductionFromPopulation (productions, race,
			CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, 5, new MapCoordinates3DEx (20, 10, 1), mem.getBuilding (), db);
		verify (cityCalculations).addProductionFromPopulation (productions, race,
			CommonDatabaseConstants.POPULATION_TASK_ID_WORKER, 7, new MapCoordinates3DEx (20, 10, 1), mem.getBuilding (), db);
		verify (cityCalculations).addProductionFromPopulation (productions, race,
			CommonDatabaseConstants.POPULATION_TASK_ID_REBEL, 3, new MapCoordinates3DEx (20, 10, 1), mem.getBuilding (), db);
		
		verify (cityCalculations).addProductionAndConsumptionFromBuilding (productions, buildingDefs.get (0), null, cityOwnerWizard.getPick (), db);
		verify (cityCalculations).addProductionAndConsumptionFromBuilding (productions, buildingDefs.get (2), null, cityOwnerWizard.getPick (), db);
		
		verify (cityCalculations).addProductionFromFortressPickType (productions, pickType, 5, db);
		verify (cityCalculations).addProductionFromFortressPlane (productions, plane, db);
		
		verify (cityCalculations).addProductionFromSpell (productions, mem.getMaintainedSpell ().get (1), 4, db);
		
		verify (cityCalculations).addProductionFromMapFeatures (productions, map, new MapCoordinates3DEx (20, 10, 1), mapSize, db, 2, 0);
		
		verify (cityCalculations).halveAddPercentageBonusAndCapProduction (cityOwnerPlayer, cityOwnerWizard, rations, 0, difficultyLevel, db);
		verify (cityCalculations).halveAddPercentageBonusAndCapProduction (cityOwnerPlayer, cityOwnerWizard, production, 0, difficultyLevel, db);
		verify (cityCalculations).halveAddPercentageBonusAndCapProduction (cityOwnerPlayer, cityOwnerWizard, gold, 0, difficultyLevel, db);
		verify (cityCalculations).halveAddPercentageBonusAndCapProduction (cityOwnerPlayer, cityOwnerWizard, food, 0, difficultyLevel, db);
		
		verify (cityCalculations).calculateGoldTradeBonus (gold, map, new MapCoordinates3DEx (20, 10, 1), null, mapSize, db);
		verifyNoMoreInteractions (cityCalculations);
	}	
}
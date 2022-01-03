package momime.server.calculations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.Building;
import momime.common.database.CitySize;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.MemoryBuildingUtilsImpl;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseValues;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.CityServerUtils;

/**
 * Tests the ServerCityCalculations class
 */
@ExtendWith(MockitoExtension.class)
public final class TestServerCityCalculationsImpl extends ServerTestData
{
	/**
	 * Tests the calculateCitySizeIDAndMinimumFarmers method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCitySizeIDAndMinimumFarmers_Simple () throws Exception
	{
		// Mock database
		final CitySize smallCity = new CitySize ();
		smallCity.setCitySizeID ("CS01");
		smallCity.setCitySizeMaximum (3999);
		
		final CitySize mediumCity = new CitySize ();
		mediumCity.setCitySizeID ("CS02");
		mediumCity.setCitySizeMinimum (4000);
		mediumCity.setCitySizeMaximum (6999);
		
		final CitySize largeCity = new CitySize ();
		largeCity.setCitySizeID ("CS03");
		largeCity.setCitySizeMinimum (7000);
		
		final List<CitySize> citySizes = new ArrayList<CitySize> ();
		citySizes.add (smallCity);
		citySizes.add (mediumCity);
		citySizes.add (largeCity);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getCitySize ()).thenReturn (citySizes);
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");

		final PlayerServerDetails player = new PlayerServerDetails (null, null, priv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 2, "calculateCitySizeIDAndMinimumFarmers")).thenReturn (player);

		// City
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityRaceID ("RC01");
		cityData.setCityPopulation (6900);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Max city size and rations provided by buildings for free
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setProductionAmountPlusPercentageBonus (10);
		
		final CityProductionBreakdownsEx cityProductions = new CityProductionBreakdownsEx ();
		cityProductions.getProductionType ().add (food);
		
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		when (prod.calculateAllCityProductions (players, trueMap.getWizardDetails (), terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), priv.getTaxRateID (),
			sd, null, false, false, db)).thenReturn (cityProductions);
	
		// Rations produced per farmer
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.calculateDoubleFarmingRate (terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db)).thenReturn (4);

		// Session variables
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up object to test
		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		calc.setCityProductionCalculations (prod);
		calc.setCityServerUtils (cityServerUtils);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		calc.calculateCitySizeIDAndMinimumFarmers (new MapCoordinates3DEx (20, 10, 1), mom);
		
		// Check results
		assertEquals ("CS02", cityData.getCitySizeID ());
		assertEquals (3, cityData.getMinimumFarmers ());		// 6 pop, each farmer produces 2, so need 6 / 2 = 3 farmers
	}

	/**
	 * Tests the calculateCitySizeIDAndMinimumFarmers method when no city size is defined for the given population
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCitySizeIDAndMinimumFarmers_NoMatchingCitySize () throws Exception
	{
		// Mock database
		final CitySize smallCity = new CitySize ();
		smallCity.setCitySizeID ("CS01");
		smallCity.setCitySizeMaximum (3999);
		
		final CitySize mediumCity = new CitySize ();
		mediumCity.setCitySizeID ("CS02");
		mediumCity.setCitySizeMinimum (4000);
		mediumCity.setCitySizeMaximum (6999);
		
		final CitySize largeCity = new CitySize ();
		largeCity.setCitySizeID ("CS03");
		largeCity.setCitySizeMinimum (7000);
		mediumCity.setCitySizeMaximum (9999);
		
		final List<CitySize> citySizes = new ArrayList<CitySize> ();
		citySizes.add (smallCity);
		citySizes.add (mediumCity);
		citySizes.add (largeCity);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getCitySize ()).thenReturn (citySizes);
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");

		final PlayerServerDetails player = new PlayerServerDetails (null, null, priv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 2, "calculateCitySizeIDAndMinimumFarmers")).thenReturn (player);

		// City
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityRaceID ("RC01");
		cityData.setCityPopulation (16900);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Max city size and rations provided by buildings for free
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setProductionAmountPlusPercentageBonus (10);
		
		final CityProductionBreakdownsEx cityProductions = new CityProductionBreakdownsEx ();
		cityProductions.getProductionType ().add (food);
		
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		when (prod.calculateAllCityProductions (players, trueMap.getWizardDetails (), terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), priv.getTaxRateID (),
			sd, null, false, false, db)).thenReturn (cityProductions);
		
		// Rations produced per farmer
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.calculateDoubleFarmingRate (terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db)).thenReturn (4);

		// Session variables
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up object to test
		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		calc.setCityProductionCalculations (prod);
		calc.setCityServerUtils (cityServerUtils);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		calc.calculateCitySizeIDAndMinimumFarmers (new MapCoordinates3DEx (20, 10, 1), mom);
	}

	/**
	 * Tests the calculateCitySizeIDAndMinimumFarmers method where we have a Granary providing +2 rations, so we need 1 less farmer
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCitySizeIDAndMinimumFarmers_Granary () throws Exception
	{
		// Mock database
		final CitySize smallCity = new CitySize ();
		smallCity.setCitySizeID ("CS01");
		smallCity.setCitySizeMaximum (3999);
		
		final CitySize mediumCity = new CitySize ();
		mediumCity.setCitySizeID ("CS02");
		mediumCity.setCitySizeMinimum (4000);
		mediumCity.setCitySizeMaximum (6999);
		
		final CitySize largeCity = new CitySize ();
		largeCity.setCitySizeID ("CS03");
		largeCity.setCitySizeMinimum (7000);
		
		final List<CitySize> citySizes = new ArrayList<CitySize> ();
		citySizes.add (smallCity);
		citySizes.add (mediumCity);
		citySizes.add (largeCity);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getCitySize ()).thenReturn (citySizes);
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");

		final PlayerServerDetails player = new PlayerServerDetails (null, null, priv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 2, "calculateCitySizeIDAndMinimumFarmers")).thenReturn (player);

		// City
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityRaceID ("RC01");
		cityData.setCityPopulation (6900);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Max city size and rations provided by buildings for free
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setProductionAmountPlusPercentageBonus (10);
		
		final CityProductionBreakdown rations = new CityProductionBreakdown ();
		rations.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		rations.setProductionAmountToAddAfterPercentages (2);		// Not doubled
		
		final CityProductionBreakdownsEx cityProductions = new CityProductionBreakdownsEx ();
		cityProductions.getProductionType ().add (food);
		cityProductions.getProductionType ().add (rations);
		
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		when (prod.calculateAllCityProductions (players, trueMap.getWizardDetails (), terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), priv.getTaxRateID (),
			sd, null, false, false, db)).thenReturn (cityProductions);
		
		// Rations produced per farmer
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.calculateDoubleFarmingRate (terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db)).thenReturn (4);

		// Session variables
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up object to test
		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		calc.setCityProductionCalculations (prod);
		calc.setCityServerUtils (cityServerUtils);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		calc.calculateCitySizeIDAndMinimumFarmers (new MapCoordinates3DEx (20, 10, 1), mom);
		
		// Check results
		assertEquals ("CS02", cityData.getCitySizeID ());
		assertEquals (2, cityData.getMinimumFarmers ());		// 6 pop, but we get 2 rations for free; each farmer produces 2, so need 4 / 2 = 2 farmers
	}

	/**
	 * Tests the calculateCitySizeIDAndMinimumFarmers method when the overfarming rule applies
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCitySizeIDAndMinimumFarmers_Overfarming () throws Exception
	{
		// Mock database
		final CitySize smallCity = new CitySize ();
		smallCity.setCitySizeID ("CS01");
		smallCity.setCitySizeMaximum (3999);
		
		final CitySize mediumCity = new CitySize ();
		mediumCity.setCitySizeID ("CS02");
		mediumCity.setCitySizeMinimum (4000);
		mediumCity.setCitySizeMaximum (6999);
		
		final CitySize largeCity = new CitySize ();
		largeCity.setCitySizeID ("CS03");
		largeCity.setCitySizeMinimum (7000);
		
		final List<CitySize> citySizes = new ArrayList<CitySize> ();
		citySizes.add (smallCity);
		citySizes.add (mediumCity);
		citySizes.add (largeCity);
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getCitySize ()).thenReturn (citySizes);
		
		// Session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Player
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");

		final PlayerServerDetails player = new PlayerServerDetails (null, null, priv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, 2, "calculateCitySizeIDAndMinimumFarmers")).thenReturn (player);

		// City
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityRaceID ("RC01");
		cityData.setCityPopulation (14000);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (terrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Max city size and rations provided by buildings for free
		final CityProductionBreakdown food = new CityProductionBreakdown ();
		food.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		food.setProductionAmountPlusPercentageBonus (10);	// So the city is over max size
		
		final CityProductionBreakdownsEx cityProductions = new CityProductionBreakdownsEx ();
		cityProductions.getProductionType ().add (food);
		
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		when (prod.calculateAllCityProductions (players, trueMap.getWizardDetails (), terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), priv.getTaxRateID (),
			sd, null, false, false, db)).thenReturn (cityProductions);
	
		// Rations produced per farmer
		final CityServerUtils cityServerUtils = mock (CityServerUtils.class);
		when (cityServerUtils.calculateDoubleFarmingRate (terrain, buildings, spells, new MapCoordinates3DEx (20, 10, 1), db)).thenReturn (3);

		// Session variables
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getGeneralPublicKnowledge ()).thenReturn (gpk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Set up object to test
		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		calc.setCityProductionCalculations (prod);
		calc.setCityServerUtils (cityServerUtils);
		calc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		
		// Run method
		calc.calculateCitySizeIDAndMinimumFarmers (new MapCoordinates3DEx (20, 10, 1), mom);
		
		// Check results
		assertEquals ("CS03", cityData.getCitySizeID ());
		assertEquals (12, cityData.getMinimumFarmers ());		// 12 farmers each producing 1½ generate 18, but food from terrain is 10 so overfarming
																						// rule halves the excess and we get 14, which = the population so loop stops
	}
	
	/**
	 * Tests the ensureNotTooManyOptionalFarmers method in valid scenarios
	 * @throws MomException If minimum farmers + rebels > population
	 */
	@Test
	public final void testEnsureNotTooManyOptionalFarmers_Valid () throws MomException
	{
		final OverlandMapCityData city = new OverlandMapCityData ();

		city.setCityPopulation (8678);

		city.setMinimumFarmers (3);
		city.setNumberOfRebels (3);

		// Set up object to test
		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		
		// Lower optional farmers
		city.setOptionalFarmers (1);
		calc.ensureNotTooManyOptionalFarmers (city);
		assertEquals (1, city.getOptionalFarmers ());

		// Exact number of optional farmers
		city.setOptionalFarmers (2);
		calc.ensureNotTooManyOptionalFarmers (city);
		assertEquals (2, city.getOptionalFarmers ());

		// Too many optional farmers
		city.setOptionalFarmers (3);
		calc.ensureNotTooManyOptionalFarmers (city);
		assertEquals (2, city.getOptionalFarmers ());

		// Way too many optional farmers
		city.setNumberOfRebels (5);
		calc.ensureNotTooManyOptionalFarmers (city);
		assertEquals (0, city.getOptionalFarmers ());
	}

	/**
	 * Tests the ensureNotTooManyOptionalFarmers method when the minimum farmers and rebels are set incorrectly
	 * @throws MomException If minimum farmers + rebels > population
	 */
	@Test
	public final void testEnsureNotTooManyOptionalFarmers_Invalid () throws MomException
	{
		final OverlandMapCityData city = new OverlandMapCityData ();

		city.setCityPopulation (8678);
		city.setMinimumFarmers (3);
		city.setNumberOfRebels (6);
		city.setOptionalFarmers (0);

		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		
		assertThrows (MomException.class, () ->
		{
			calc.ensureNotTooManyOptionalFarmers (city);
		});
	}

	/**
	 * Tests the calculateCityScoutingRange cmethod
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateCityScoutingRange () throws Exception
	{
		final CommonDatabase db = loadServerDatabase ();

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Location
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (2, 2, 0);

		// Set up object to test
		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		
		// No buildings
		assertEquals (-1, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// City walls in wrong location
		final MemoryBuilding firstBuilding = new MemoryBuilding ();
		firstBuilding.setBuildingID ("BL35");
		firstBuilding.setCityLocation (new MapCoordinates3DEx (2, 3, 0));

		buildings.add (firstBuilding);
		assertEquals (-1, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// Irrelevant building in right location
		final MemoryBuilding secondBuilding = new MemoryBuilding ();
		secondBuilding.setBuildingID ("BL34");
		secondBuilding.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (secondBuilding);
		assertEquals (-1, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// City walls increase to 3
		final MemoryBuilding cityWalls = new MemoryBuilding ();
		cityWalls.setBuildingID ("BL35");
		cityWalls.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (cityWalls);
		assertEquals (3, calc.calculateCityScoutingRange (buildings, cityLocation, db));

		// Oracle increases to 4
		final MemoryBuilding oracle = new MemoryBuilding ();
		oracle.setBuildingID ("BL18");
		oracle.setCityLocation (new MapCoordinates3DEx (2, 2, 0));

		buildings.add (oracle);
		assertEquals (4, calc.calculateCityScoutingRange (buildings, cityLocation, db));
	}

	/**
	 * Tests the canEventuallyConstructBuilding method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanEventuallyConstructBuilding () throws Exception
	{
		final CommonDatabase db = loadServerDatabase ();

		final MomSessionDescription sd = createMomSessionDescription (db, "MS03", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sd.getOverlandMapSize ());

		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();

		// Need certain types of terrain in order to be able to construct all building types
		final OverlandMapTerrainData forest = new OverlandMapTerrainData ();
		forest.setTileTypeID (CommonDatabaseConstants.TILE_TYPE_FOREST);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (19).setTerrainData (forest);

		final OverlandMapTerrainData mountain = new OverlandMapTerrainData ();
		mountain.setTileTypeID (ServerDatabaseValues.TILE_TYPE_MOUNTAIN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (20).setTerrainData (mountain);

		final OverlandMapTerrainData ocean = new OverlandMapTerrainData ();
		ocean.setTileTypeID (ServerDatabaseValues.TILE_TYPE_OCEAN);
		trueTerrain.getPlane ().get (1).getRow ().get (9).getCell ().get (21).setTerrainData (ocean);

		// Set up city
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);

		final OverlandMapCityData cityData = new OverlandMapCityData ();
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		// Set up object to test
		final CityCalculationsImpl cityCalc = new CityCalculationsImpl ();
		cityCalc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		final ServerCityCalculationsImpl calc = new ServerCityCalculationsImpl ();
		calc.setMemoryBuildingUtils (new MemoryBuildingUtilsImpl ());
		calc.setCityCalculations (cityCalc);
		
		// Orcs can build absolutely everything
		cityData.setCityRaceID ("RC09");
		for (final Building building : db.getBuilding ())
			if ((!building.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)) &&
				(!building.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)))

				assertTrue (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation, building, sd.getOverlandMapSize (), db), building.getBuildingID ());

		// Barbarians can't build Universities
		cityData.setCityRaceID ("RC01");
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL20", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));

		// Barbarians can't build Banks, because they can't build Universities
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL27", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));

		// Barbarians can't build Merchants' Guilds, because they can't build Banks, because they can't build Universities
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));

		// Orcs can't build Ship Wrights' Guilds if there's no water
		cityData.setCityRaceID ("RC09");
		ocean.setTileTypeID (ServerDatabaseValues.TILE_TYPE_GRASS);
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL12", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));

		// Orcs can't build Ship Yards if there's no water, because they can't build a Ship Wrights' Guild
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL13", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));

		// Orcs can't build Merchants' Guilds if there's no water, because they can't build a Ship Yard, because they can't build a Ship Wrights' Guild
		assertFalse (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));

		// If we got a Ship Wrights' Guild and subsequently the water all dried up then we *can* then construct the other building types
		// (Ok bad example, but similar with Sawmills + forests disappearing is definitely possible)
		final MemoryBuilding shipWrightsGuild = new MemoryBuilding ();
		shipWrightsGuild.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		shipWrightsGuild.setBuildingID ("BL12");

		buildings.add (shipWrightsGuild);

		assertTrue (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL13", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));
		assertTrue (calc.canEventuallyConstructBuilding (trueTerrain, buildings, cityLocation,
			db.findBuilding ("BL28", "testCanEventuallyConstructBuilding"), sd.getOverlandMapSize (), db));
	}
}
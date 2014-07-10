package momime.client.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseExImpl;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeEx;
import momime.client.graphics.database.RaceEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.v0_9_5.ProductionTypeImage;
import momime.client.graphics.database.v0_9_5.RacePopulationTask;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Race;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.CityViewPanel;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.CalculateCityGrowthRateBreakdown;
import momime.common.calculations.CalculateCityProductionResult;
import momime.common.calculations.CalculateCityProductionResults;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.OverlandMapCityData;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the CityViewUI class
 */
public final class TestCityViewUI
{
	/**
	 * Tests the CityViewUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCityViewUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final RaceEx race = new RaceEx ();
		when (gfx.findRace ("RC01", "cityDataUpdated")).thenReturn (race);
		
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER, "/momime.client.graphics/races/barbarian/farmer.png"));
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_WORKER, "/momime.client.graphics/races/barbarian/worker.png"));
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_REBEL, "/momime.client.graphics/races/barbarian/rebel.png"));
		race.buildMap ();
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.setTileWidth (20);
		overlandMapTileSet.setTileHeight (18);
		when (gfx.findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "OverlandMapUI.init")).thenReturn (overlandMapTileSet);
		
		final ProductionTypeEx rations = new ProductionTypeEx ();
		rations.getProductionTypeImage ().add (createProductionTypeImage ("1", "/momime.client.graphics/production/rations/1.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("10", "/momime.client.graphics/production/rations/10.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("-1", "/momime.client.graphics/production/rations/-1.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("-10", "/momime.client.graphics/production/rations/-10.png"));
		rations.buildMap ();
		when (gfx.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS)).thenReturn (rations);
		
		final ProductionTypeEx gold = new ProductionTypeEx ();
		gold.getProductionTypeImage ().add (createProductionTypeImage ("1", "/momime.client.graphics/production/gold/1.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("10", "/momime.client.graphics/production/gold/10.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("-1", "/momime.client.graphics/production/gold/-1.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("-10", "/momime.client.graphics/production/gold/-10.png"));
		gold.buildMap ();
		when (gfx.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD)).thenReturn (gold);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmCity", "Resources")).thenReturn ("Resources");
		when (lang.findCategoryEntry ("frmCity", "Enchantments")).thenReturn ("Enchantments/Curses");
		when (lang.findCategoryEntry ("frmCity", "Terrain")).thenReturn ("Surrounding Terrain");
		when (lang.findCategoryEntry ("frmCity", "Buildings")).thenReturn ("Buildings");
		when (lang.findCategoryEntry ("frmCity", "Units")).thenReturn ("Units");
		when (lang.findCategoryEntry ("frmCity", "Production")).thenReturn ("Producing");

		when (lang.findCategoryEntry ("frmCity", "RushBuy")).thenReturn ("Buy");
		when (lang.findCategoryEntry ("frmCity", "ChangeConstruction")).thenReturn ("Change");
		when (lang.findCategoryEntry ("frmCity", "OK")).thenReturn ("OK");
		
		when (lang.findCitySizeName ("CS01")).thenReturn ("Test City of CITY_NAME");
		when (lang.findCategoryEntry ("frmCity", "MaxCitySize")).thenReturn ("maximum MAX_CITY_SIZE");
		when (lang.findCategoryEntry ("frmCity", "PopulationAndGrowth")).thenReturn ("Population: POPULATION (GROWTH_RATE)");

		final Race raceName = new Race ();
		raceName.setRaceName ("Barbarian");
		when (lang.findRace ("RC01")).thenReturn (raceName);
				
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Client DB
		final ClientDatabaseExImpl db = new ClientDatabaseExImpl ();
		
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setClientDatabase (db);
		
		// City data
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCitySizeID ("CS01");
		cityData.setCityName ("Blahdy Blah");
		cityData.setCityPopulation (7890);
		cityData.setMinimumFarmers (2);
		cityData.setOptionalFarmers (1);
		cityData.setNumberOfRebels (2);
		
		final MapSizeData mapSize = ClientTestData.createMapSizeData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (mapSize);
		terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20).setCityData (cityData);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		priv.setTaxRateID ("TR01");
		
		final MomClient client = new MomClient ();
		client.setOurPersistentPlayerPrivateKnowledge (priv);
		client.setGeneralPublicKnowledge (gpk);
		client.setSessionDescription (sd);
		
		// City production
		final MomCityCalculations calc = mock (MomCityCalculations.class);
		
		final int maxCitySize = 20;
		
		final CalculateCityProductionResult maxCitySizeProd = new CalculateCityProductionResult (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
		maxCitySizeProd.setBaseProductionAmount (maxCitySize);

		final CalculateCityProductionResult rationsProd = new CalculateCityProductionResult (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS);
		rationsProd.setConsumptionAmount (4);
		rationsProd.setBaseProductionAmount (18);
		
		final CalculateCityProductionResult goldProd = new CalculateCityProductionResult (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
		goldProd.setConsumptionAmount (18);
		goldProd.setBaseProductionAmount (4);
		
		final List<CalculateCityProductionResult> productions = new ArrayList<CalculateCityProductionResult> ();
		productions.add (maxCitySizeProd);
		productions.add (rationsProd);
		productions.add (goldProd);
		
		final CalculateCityProductionResults productionsContainer = mock (CalculateCityProductionResults.class);
		when (productionsContainer.iterator ()).thenReturn (productions.iterator ());
		when (productionsContainer.findProductionType (maxCitySizeProd.getProductionTypeID ())).thenReturn (maxCitySizeProd);
		
		when (calc.calculateAllCityProductions (client.getPlayers (), terrain, fow.getBuilding (), new MapCoordinates3DEx (20, 10, 0), "TR01", sd, true, db, false)).thenReturn (productionsContainer);
		
		final CalculateCityGrowthRateBreakdown cityGrowthBreakdown = new CalculateCityGrowthRateBreakdown (0, 0, null, 0, 0, null, 0, 0, 0, 0, 70);
		when (calc.calculateCityGrowthRate (terrain, fow.getBuilding (), new MapCoordinates3DEx (20, 10, 0), maxCitySize, db)).thenReturn (cityGrowthBreakdown);
		
		// Set up terrain panel
		final CityViewPanel panel = new CityViewPanel ();
		panel.setUtils (utils);
		panel.setGraphicsDB (gfx);
		
		// Set up form
		final CityViewUI cityView = new CityViewUI ();
		cityView.setUtils (utils);
		cityView.setLanguageHolder (langHolder);
		cityView.setLanguageChangeMaster (langMaster);
		cityView.setGraphicsDB (gfx);
		cityView.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		cityView.setOverlandMapUI (new OverlandMapUI ());
		cityView.setCityViewPanel (panel);
		cityView.setClient (client);
		cityView.setCityCalculations (calc);
		cityView.setTextUtils (new TextUtilsImpl ());
		cityView.setSmallFont (CreateFontsForTests.getSmallFont ());
		cityView.setMediumFont (CreateFontsForTests.getMediumFont ());
		cityView.setLargeFont (CreateFontsForTests.getLargeFont ());
	
		// Display form		
		cityView.setVisible (true);
		Thread.sleep (5000);
	}

	/**
	 * Shortcut method for creating RacePopulationTasks in unit tests
	 * 
	 * @param populationTaskID Farmer/Worker/Rebel
	 * @param filename Filename to locate the image
	 * @return New RacePopulationTask object
	 */
	private final RacePopulationTask createRacePopulationTaskImage (final String populationTaskID, final String filename)
	{
		final RacePopulationTask image = new RacePopulationTask ();
		image.setPopulationTaskID (populationTaskID);
		image.setCivilianImageFile (filename);
		return image;
	}
	
	/**
	 * Shortcut method for creating ProductionTypeImages in unit tests
	 * 
	 * @param value Value (10, 1, -1, -10)
	 * @param filename Filename to locate the image
	 * @return New ProductionTypeImage object
	 */
	private final ProductionTypeImage createProductionTypeImage (final String value, final String filename)
	{
		final ProductionTypeImage image = new ProductionTypeImage ();
		image.setProductionValue (value);
		image.setProductionImageFile (filename);
		return image;
	}
}
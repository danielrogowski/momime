package momime.client.ui.frames;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.ProductionTypeGfx;
import momime.client.graphics.database.ProductionTypeImageGfx;
import momime.client.graphics.database.RaceGfx;
import momime.client.graphics.database.RacePopulationTaskGfx;
import momime.client.graphics.database.TileSetGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.RaceLang;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.CityViewPanel;
import momime.client.ui.renderer.MemoryMaintainedSpellListCellRenderer;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.database.Building;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.OverlandMapSize;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;

import org.junit.Test;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the CityViewUI class
 */
public final class TestCityViewUI
{
	/**
	 * Tests the CityViewUI form
	 * 
	 * @param ourCity Whether the city is ours, or someone else's
	 * @param seeEnemyCityConstruction Whether we can see what enemy cities are constructing or not 
	 * @throws Exception If there is a problem
	 */
	private final void testCityViewUI (final boolean ourCity, final boolean seeEnemyCityConstruction) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final RaceGfx race = new RaceGfx ();
		when (gfx.findRace ("RC01", "cityDataChanged")).thenReturn (race);
		
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, "/momime.client.graphics/races/barbarian/farmer.png"));
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER, "/momime.client.graphics/races/barbarian/worker.png"));
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.POPULATION_TASK_ID_REBEL, "/momime.client.graphics/races/barbarian/rebel.png"));
		race.buildMap ();
		
		final TileSetGfx overlandMapTileSet = new TileSetGfx ();
		overlandMapTileSet.setTileWidth (20);
		overlandMapTileSet.setTileHeight (18);
		when (gfx.findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "OverlandMapUI.init")).thenReturn (overlandMapTileSet);
		
		final ProductionTypeGfx rations = new ProductionTypeGfx ();
		rations.getProductionTypeImage ().add (createProductionTypeImage ("1", "/momime.client.graphics/production/rations/1.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("10", "/momime.client.graphics/production/rations/10.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("-1", "/momime.client.graphics/production/rations/-1.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("-10", "/momime.client.graphics/production/rations/-10.png"));
		rations.buildMap ();
		when (gfx.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, "generateProductionImage")).thenReturn (rations);
		
		final ProductionTypeGfx gold = new ProductionTypeGfx ();
		gold.getProductionTypeImage ().add (createProductionTypeImage ("1", "/momime.client.graphics/production/gold/1.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("10", "/momime.client.graphics/production/gold/10.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("-1", "/momime.client.graphics/production/gold/-1.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("-10", "/momime.client.graphics/production/gold/-10.png"));
		gold.buildMap ();
		when (gfx.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "generateProductionImage")).thenReturn (gold);

		final ProductionTypeGfx food = new ProductionTypeGfx ();
		food.buildMap ();
		when (gfx.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, "generateProductionImage")).thenReturn (food);
		
		final CityViewElementGfx granaryGfx = new CityViewElementGfx ();
		granaryGfx.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		when (gfx.findBuilding (eq ("BL01"), anyString ())).thenReturn (granaryGfx);
		
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
		when (lang.findCategoryEntry ("frmCity", "Rename")).thenReturn ("Rename");
		
		when (lang.findCitySizeName ("CS01")).thenReturn ("Test City of CITY_NAME");
		when (lang.findCategoryEntry ("frmCity", "MaxCitySize")).thenReturn ("maximum MAX_CITY_SIZE");
		when (lang.findCategoryEntry ("frmCity", "PopulationAndGrowth")).thenReturn ("Population: POPULATION (GROWTH_RATE)");

		final RaceLang raceName = new RaceLang ();
		raceName.setRaceName ("Barbarian");
		when (lang.findRace ("RC01")).thenReturn (raceName);
				
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Building granary = new Building ();
		granary.setProductionCost (200);
		when (db.findBuilding (eq ("BL01"), anyString ())).thenReturn (granary);
		
		when (db.getMostExpensiveConstructionCost ()).thenReturn (1000);
		
		// City data
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityRaceID ("RC01");
		cityData.setCitySizeID ("CS01");
		cityData.setCityName ("Blahdy Blah");
		cityData.setCityPopulation (7890);
		cityData.setMinimumFarmers (2);
		cityData.setOptionalFarmers (1);
		cityData.setNumberOfRebels (2);
		cityData.setCurrentlyConstructingBuildingID ("BL01");
		cityData.setCityOwnerID (ourCity ? 1 : 2);
		cityData.setProductionSoFar (60);
		
		final OverlandMapSize overlandMapSize = ClientTestData.createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (overlandMapSize);
		final MemoryGridCell mc = terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20);
		mc.setCityData (cityData);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		priv.setTaxRateID ("TR01");
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setSeeEnemyCityConstruction (seeEnemyCityConstruction);
		sd.setFogOfWarSetting (fowSettings);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPlayerID ()).thenReturn (1);
		
		// City production
		final CityCalculations calc = mock (CityCalculations.class);
		
		final int maxCitySize = 20;
		
		final CityProductionBreakdown maxCitySizeProd = new CityProductionBreakdown ();
		maxCitySizeProd.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		maxCitySizeProd.setCappedProductionAmount (maxCitySize);

		final CityProductionBreakdown rationsProd = new CityProductionBreakdown ();
		rationsProd.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
		rationsProd.setConsumptionAmount (4);
		rationsProd.setCappedProductionAmount (18);
		
		final CityProductionBreakdown goldProd = new CityProductionBreakdown ();
		goldProd.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		goldProd.setConsumptionAmount (18);
		goldProd.setCappedProductionAmount (4);
		
		final CityProductionBreakdownsEx productions = new CityProductionBreakdownsEx ();
		productions.getProductionType ().add (maxCitySizeProd);
		productions.getProductionType ().add (rationsProd);
		productions.getProductionType ().add (goldProd);
		
		when (calc.calculateAllCityProductions (client.getPlayers (), terrain, fow.getBuilding (), new MapCoordinates3DEx (20, 10, 0), "TR01", sd, true, false, db)).thenReturn (productions);
		
		final CityGrowthRateBreakdown cityGrowthBreakdown = new CityGrowthRateBreakdown ();
		cityGrowthBreakdown.setFinalTotal (70);
		when (calc.calculateCityGrowthRate (terrain, fow.getBuilding (), new MapCoordinates3DEx (20, 10, 0), maxCitySize, db)).thenReturn (cityGrowthBreakdown);
		
		// Display at least some landscape
		final CityViewElementGfx landscape = new CityViewElementGfx ();
		landscape.setLocationX (0);
		landscape.setLocationY (0);
		landscape.setSizeMultiplier (2);
		landscape.setCityViewImageFile ("/momime.client.graphics/cityView/landscape/arcanus.png");
		
		final List<CityViewElementGfx> elements = new ArrayList<CityViewElementGfx> ();
		elements.add (landscape);
		when (gfx.getCityViewElements ()).thenReturn (elements);

		// Set up animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Set up terrain panel
		final CityViewPanel panel = new CityViewPanel ();
		panel.setUtils (utils);
		panel.setGraphicsDB (gfx);
		panel.setAnim (anim);
		panel.setClient (client);
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setGraphicsDB (gfx);
		resourceValueClientUtils.setUtils (utils);
		
		// Set up spells list renderer
		final MemoryMaintainedSpellListCellRenderer spellsRenderer = new MemoryMaintainedSpellListCellRenderer ();
		
		// Mock mini map generator
		final BufferedImage [] miniMapBitmaps = new BufferedImage [1];
		miniMapBitmaps [0] = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		
		final OverlandMapBitmapGenerator gen = mock (OverlandMapBitmapGenerator.class);
		when (gen.generateOverlandMapBitmaps (0, 20-3, 10-3, 7, 7)).thenReturn (miniMapBitmaps);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CityViewUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final CityViewUI cityView = new CityViewUI ();
		cityView.setUtils (utils);
		cityView.setLanguageHolder (langHolder);
		cityView.setLanguageChangeMaster (langMaster);
		cityView.setGraphicsDB (gfx);
		cityView.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		cityView.setCityViewPanel (panel);
		cityView.setClient (client);
		cityView.setCityCalculations (calc);
		cityView.setAnim (anim);
		cityView.setOverlandMapBitmapGenerator (gen);
		cityView.setTextUtils (new TextUtilsImpl ());
		cityView.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		cityView.setOverlandMapUI (new OverlandMapUI ());		// Just to read the zero anim counter
		cityView.setResourceValueClientUtils (resourceValueClientUtils);
		cityView.setMemoryMaintainedSpellListCellRenderer (spellsRenderer);
		cityView.setSmallFont (CreateFontsForTests.getSmallFont ());
		cityView.setMediumFont (CreateFontsForTests.getMediumFont ());
		cityView.setLargeFont (CreateFontsForTests.getLargeFont ());
		cityView.setCityViewLayout (layout);
	
		// Display form		
		cityView.setVisible (true);
		Thread.sleep (5000);
		cityView.setVisible (false);
	}

	/**
	 * Shortcut method for creating RacePopulationTasks in unit tests
	 * 
	 * @param populationTaskID Farmer/Worker/Rebel
	 * @param filename Filename to locate the image
	 * @return New RacePopulationTask object
	 */
	private final RacePopulationTaskGfx createRacePopulationTaskImage (final String populationTaskID, final String filename)
	{
		final RacePopulationTaskGfx image = new RacePopulationTaskGfx ();
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
	private final ProductionTypeImageGfx createProductionTypeImage (final String value, final String filename)
	{
		final ProductionTypeImageGfx image = new ProductionTypeImageGfx ();
		image.setProductionValue (value);
		image.setProductionImageFile (filename);
		return image;
	}

	/**
	 * Tests the CityViewUI form on one of our cities
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCityViewUI_Ours () throws Exception
	{
		testCityViewUI (true, true);
	}

	/**
	 * Tests the CityViewUI form on one of someone else's cities and we can see what they're constructing
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCityViewUI_NotOurs_CanSee () throws Exception
	{
		testCityViewUI (false, true);
	}
	
	/**
	 * Tests the CityViewUI form on one of someone else's cities and we can't see what they're constructing
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCityViewUI_NotOurs_CantSee () throws Exception
	{
		testCityViewUI (false, false);
	}
}
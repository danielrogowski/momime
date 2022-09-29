package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.CityScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.CityViewPanel;
import momime.client.ui.renderer.MemoryMaintainedSpellListCellRenderer;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.ResourceValueClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.CitySize;
import momime.common.database.CityViewElement;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.FogOfWarSetting;
import momime.common.database.Language;
import momime.common.database.OverlandMapSize;
import momime.common.database.ProductionTypeEx;
import momime.common.database.ProductionTypeImage;
import momime.common.database.RaceEx;
import momime.common.database.RacePopulationTask;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;

/**
 * Tests the CityViewUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCityViewUI extends ClientTestData
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

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final RaceEx race = new RaceEx ();
		race.getRaceNameSingular ().add (createLanguageText (Language.ENGLISH, "Barbarian"));
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, "/momime.client.graphics/races/barbarian/farmer.png"));
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER, "/momime.client.graphics/races/barbarian/worker.png"));
		race.getRacePopulationTask ().add (createRacePopulationTaskImage (CommonDatabaseConstants.POPULATION_TASK_ID_REBEL, "/momime.client.graphics/races/barbarian/rebel.png"));
		race.buildMap ();
		when (db.findRace (eq ("RC01"), anyString ())).thenReturn (race);
				
		final CitySize citySize = new CitySize ();
		citySize.getCitySizeNameIncludingOwner ().add (createLanguageText (Language.ENGLISH, "PLAYER_NAME's Test City of CITY_NAME"));
		when (db.findCitySize (eq ("CS01"), anyString ())).thenReturn (citySize);

		final ProductionTypeEx rations = new ProductionTypeEx ();
		rations.getProductionTypeImage ().add (createProductionTypeImage ("1", "/momime.client.graphics/production/rations/1.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("10", "/momime.client.graphics/production/rations/10.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("-1", "/momime.client.graphics/production/rations/-1.png"));
		rations.getProductionTypeImage ().add (createProductionTypeImage ("-10", "/momime.client.graphics/production/rations/-10.png"));
		rations.buildMap ();
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, "generateProductionImage")).thenReturn (rations);
		
		final ProductionTypeEx gold = new ProductionTypeEx ();
		gold.getProductionTypeImage ().add (createProductionTypeImage ("1", "/momime.client.graphics/production/gold/1.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("10", "/momime.client.graphics/production/gold/10.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("-1", "/momime.client.graphics/production/gold/-1.png"));
		gold.getProductionTypeImage ().add (createProductionTypeImage ("-10", "/momime.client.graphics/production/gold/-10.png"));
		gold.buildMap ();
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "generateProductionImage")).thenReturn (gold);

		final ProductionTypeEx food = new ProductionTypeEx ();
		food.buildMap ();
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD, "generateProductionImage")).thenReturn (food);
		
		final CityViewElement granaryGfx = new CityViewElement ();
		granaryGfx.setCityViewImageFile ("/momime.client.graphics/cityView/buildings/BL29.png");
		when (db.findCityViewElementBuilding (eq ("BL01"), anyString ())).thenReturn (granaryGfx);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final CityScreen cityScreenLang = new CityScreen ();
		cityScreenLang.getResources ().add (createLanguageText (Language.ENGLISH, "Resources"));
		cityScreenLang.getEnchantments ().add (createLanguageText (Language.ENGLISH, "Enchantments/Curses"));
		cityScreenLang.getTerrain ().add (createLanguageText (Language.ENGLISH, "Surrounding Terrain"));
		cityScreenLang.getBuildings ().add (createLanguageText (Language.ENGLISH, "Buildings"));
		cityScreenLang.getUnits ().add (createLanguageText (Language.ENGLISH, "Units"));
		cityScreenLang.getProduction ().add (createLanguageText (Language.ENGLISH, "Producing"));
		cityScreenLang.getProductionTurns ().add (createLanguageText (Language.ENGLISH, "NUMBER_OF_TURNS Turns"));
		
		cityScreenLang.getRushBuy ().add (createLanguageText (Language.ENGLISH, "Buy"));
		cityScreenLang.getChangeConstruction ().add (createLanguageText (Language.ENGLISH, "Change"));
		cityScreenLang.getRename ().add (createLanguageText (Language.ENGLISH, "Rename"));
		cityScreenLang.getMaxCitySize ().add (createLanguageText (Language.ENGLISH, "maximum MAX_CITY_SIZE"));
		cityScreenLang.getPopulationAndGrowth ().add (createLanguageText (Language.ENGLISH, "Population: POPULATION (GROWTH_RATE)"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getCityScreen ()).thenReturn (cityScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
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
		
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (overlandMapSize);
		final MemoryGridCell mc = terrain.getPlane ().get (0).getRow ().get (10).getCell ().get (20);
		mc.setCityData (cityData);

		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);

		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		priv.setTaxRateID ("TR01");
		
		// Players
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		if (ourCity)
		{
			final PlayerPublicDetails player1 = new PlayerPublicDetails (null, null, null);
			when (multiplayerSessionUtils.findPlayerWithID (players, 1)).thenReturn (player1);
			when (wizardClientUtils.getPlayerName (player1)).thenReturn ("Nigel");
		}		
		else
		{
			final PlayerPublicDetails player2 = new PlayerPublicDetails (null, null, null);
			when (multiplayerSessionUtils.findPlayerWithID (players, 2)).thenReturn (player2);
			when (wizardClientUtils.getPlayerName (player2)).thenReturn ("Jafar");
		}
				
		// Production cost
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		if (ourCity)
			when (prod.calculateProductionCost (players, fow, new MapCoordinates3DEx (20, 10, 0), "TR01", sd, null, db, null)).thenReturn (200);
		
		when (db.getMostExpensiveConstructionCost ()).thenReturn (1000);
		
		// Session description
		final FogOfWarSetting fowSettings = new FogOfWarSetting ();
		fowSettings.setSeeEnemyCityConstruction (seeEnemyCityConstruction);
		sd.setFogOfWarSetting (fowSettings);
		
		final DifficultyLevel difficultyLevel = new DifficultyLevel ();
		difficultyLevel.setAiWizardsPopulationGrowthRateMultiplier (300);
		sd.setDifficultyLevel (difficultyLevel);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPlayerID ()).thenReturn (1);
		when (client.getPlayers ()).thenReturn (players);
		
		// City production
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
		
		when (prod.calculateAllCityProductions (client.getPlayers (), fow, new MapCoordinates3DEx (20, 10, 0), "TR01", sd, null, true, false, db)).thenReturn (productions);

		// City calculations
		final CityCalculations calc = mock (CityCalculations.class);
		final CityGrowthRateBreakdown cityGrowthBreakdown = new CityGrowthRateBreakdown ();
		cityGrowthBreakdown.setCappedTotal (70);
		when (calc.calculateCityGrowthRate (players, fow, new MapCoordinates3DEx (20, 10, 0), maxCitySize, difficultyLevel, db)).thenReturn (cityGrowthBreakdown);
		
		final ClientCityCalculations clientCityCalculations = mock (ClientCityCalculations.class);
		if (ourCity)
			when (clientCityCalculations.calculateProductionTurnsRemaining (new MapCoordinates3DEx (20, 10, 0))).thenReturn (17);
		
		// Display at least some landscape
		final CityViewElement landscape = new CityViewElement ();
		landscape.setLocationX (0);
		landscape.setLocationY (0);
		landscape.setSizeMultiplier (2);
		landscape.setCityViewImageFile ("/momime.client.graphics/cityView/landscape/arcanus.png");
		
		final List<CityViewElement> elements = new ArrayList<CityViewElement> ();
		elements.add (landscape);
		when (db.getCityViewElement ()).thenReturn (elements);
		
		// Event
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);

		// Set up animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setUtils (utils);
		
		// Set up terrain panel
		final CityViewPanel panel = new CityViewPanel ();
		panel.setUtils (utils);
		panel.setAnim (anim);
		panel.setClient (client);
		
		// Set up production image generator
		final ResourceValueClientUtilsImpl resourceValueClientUtils = new ResourceValueClientUtilsImpl ();
		resourceValueClientUtils.setClient (client);
		resourceValueClientUtils.setUtils (utils);
		
		// Set up spells list renderer
		final MemoryMaintainedSpellListCellRenderer spellsRenderer = new MemoryMaintainedSpellListCellRenderer ();
		
		// Mock mini map generator
		final BufferedImage [] miniMapBitmaps = new BufferedImage [1];
		miniMapBitmaps [0] = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		
		final OverlandMapBitmapGenerator gen = mock (OverlandMapBitmapGenerator.class);
		when (gen.generateOverlandMapBitmaps (0, 20-3, 10-3, 7, 7)).thenReturn (miniMapBitmaps);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CityViewUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final CityViewUI cityView = new CityViewUI ();
		cityView.setUtils (utils);
		cityView.setLanguageHolder (langHolder);
		cityView.setLanguageChangeMaster (langMaster);
		cityView.setCityLocation (new MapCoordinates3DEx (20, 10, 0));
		cityView.setCityViewPanel (panel);
		cityView.setClient (client);
		cityView.setCityCalculations (calc);
		cityView.setClientCityCalculations (clientCityCalculations);
		cityView.setCityProductionCalculations (prod);
		cityView.setMultiplayerSessionUtils (multiplayerSessionUtils);
		cityView.setWizardClientUtils (wizardClientUtils);
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
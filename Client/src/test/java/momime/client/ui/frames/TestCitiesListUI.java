package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.MiniMapBitmapGenerator;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.CitiesListScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.CitiesListCellRenderer;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.OverlandMapSize;
import momime.common.database.RaceEx;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Tests the CitiesListUI class
 */
public final class TestCitiesListUI extends ClientTestData
{
	/**
	 * Tests the CitiesListUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCitiesListUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		for (int n = 1; n <= 3; n++)
		{
			final RaceEx race = new RaceEx ();
			race.getRaceNameSingular ().add (createLanguageText (Language.ENGLISH, "Race " + n));
			when (db.findRace (eq ("RC0" + n), anyString ())).thenReturn (race);
		}
		
		for (int n = 1; n <= 2; n++)
		{
			final Building building = new Building ();
			building.getBuildingName ().add (createLanguageText (Language.ENGLISH, "Building " + n));
			when (db.findBuilding (eq ("BL0" + n), anyString ())).thenReturn (building);
		}

		for (int n = 1; n <= 2; n++)
		{
			final UnitEx unitDef = new UnitEx ();
			unitDef.getUnitName ().add (createLanguageText (Language.ENGLISH, "Unit " + n));
			when (db.findUnit (eq ("UN00" + n), anyString ())).thenReturn (unitDef);
		}
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final CitiesListScreen citiesListScreenLang = new CitiesListScreen ();
		citiesListScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "The Cities of PLAYER_NAME"));
		citiesListScreenLang.getCityName ().add (createLanguageText (Language.ENGLISH, "Name"));
		citiesListScreenLang.getCityRace ().add (createLanguageText (Language.ENGLISH, "Race"));
		citiesListScreenLang.getCityPopulation ().add (createLanguageText (Language.ENGLISH, "Pop."));
		citiesListScreenLang.getCityRations ().add (createLanguageText (Language.ENGLISH, "Rat."));
		citiesListScreenLang.getCityGold ().add (createLanguageText (Language.ENGLISH, "Gold"));
		citiesListScreenLang.getCityProduction ().add (createLanguageText (Language.ENGLISH, "Prod."));
		citiesListScreenLang.getCityConstructing ().add (createLanguageText (Language.ENGLISH, "Constructing"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getCitiesListScreen ()).thenReturn (citiesListScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Player
		final PlayerPublicDetails ourPlayer = new PlayerPublicDetails (null, null, null);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1)).thenReturn (ourPlayer);
		
		// Player name
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (ourPlayer)).thenReturn ("Rjak");

		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);
		
		// Map
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (mapSize);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setTaxRateID ("TR01");
		priv.setFogOfWarMemory (fow);
		
		// Cities
		final OverlandMapCityData city1Data = new OverlandMapCityData ();
		city1Data.setCityName ("Capital");
		city1Data.setCityPopulation (1234);
		city1Data.setCityOwnerID (1);
		city1Data.setCityRaceID ("RC01");
		city1Data.setCurrentlyConstructingBuildingID ("BL01");

		final OverlandMapCityData city2Data = new OverlandMapCityData ();
		city2Data.setCityName ("Zerolle");
		city2Data.setCityPopulation (2345);
		city2Data.setCityOwnerID (1);
		city2Data.setCityRaceID ("RC03");
		city2Data.setCurrentlyConstructingBuildingID ("BL02");

		final OverlandMapCityData city3Data = new OverlandMapCityData ();
		city3Data.setCityName ("Weedy");
		city3Data.setCityPopulation (3557);
		city3Data.setCityOwnerID (1);
		city3Data.setCityRaceID ("RC02");
		city3Data.setCurrentlyConstructingUnitID ("UN001");

		final OverlandMapCityData city4Data = new OverlandMapCityData ();
		city4Data.setCityName ("Ymraag");
		city4Data.setCityPopulation (3456);
		city4Data.setCityOwnerID (1);
		city4Data.setCityRaceID ("RC01");
		city4Data.setCurrentlyConstructingUnitID ("UN002");
		
		final OverlandMapCityData city5Data = new OverlandMapCityData ();
		city5Data.setCityName ("Xylon");
		city5Data.setCityPopulation (1956);
		city5Data.setCityOwnerID (1);
		city5Data.setCityRaceID ("RC03");
		city5Data.setCurrentlyConstructingBuildingID ("BL02");
		
		for (final OverlandMapCityData cityData : new OverlandMapCityData [] {city1Data, city2Data, city3Data, city4Data, city5Data})
		{
			final int x = cityData.getCityPopulation () % 100;
			final int y = cityData.getCityPopulation () / 100;
			final int z = cityData.getCityName ().startsWith ("W") ? 1 : 0;
			
			terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).setCityData (cityData);
		}
		
		// Capital
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setCityLocation (new MapCoordinates3DEx (34, 12, 0));
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findCityWithBuilding (1, CommonDatabaseConstants.BUILDING_FORTRESS, terrain, fow.getBuilding ())).thenReturn (fortress);
		
		// Production
		final CityProductionCalculations prod = mock (CityProductionCalculations.class);
		
		final CityProductionBreakdownsEx cityProductions = new CityProductionBreakdownsEx ();
		when (prod.calculateAllCityProductions
			(eq (players), eq (terrain), eq (fow.getBuilding ()), eq (fow.getMaintainedSpell ()),
				any (MapCoordinates3DEx.class), eq ("TR01"), eq (sd), eq (true), eq (false), eq (db))).thenReturn (cityProductions);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPlayerID ()).thenReturn (1);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getClientDB ()).thenReturn (db);

		// Mock the minimap bitmaps provided by the RHP
		final MiniMapBitmapGenerator gen = mock (MiniMapBitmapGenerator.class);
		when (gen.generateMiniMapBitmap (0)).thenReturn (createSolidImage (mapSize.getWidth (), mapSize.getHeight (), 0x004000));
		when (gen.generateMiniMapBitmap (1)).thenReturn (createSolidImage (mapSize.getWidth (), mapSize.getHeight (), 0x402000));
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CitiesListUI.xml"));
		final XmlLayoutContainerEx rowLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CitiesListUI-Row.xml"));
		layout.buildMaps ();
		rowLayout.buildMaps ();

		// Renderer
		final CitiesListCellRenderer renderer = new CitiesListCellRenderer ();
		renderer.setLanguageHolder (langHolder);
		renderer.setClient (client);
		renderer.setUtils (utils);
		renderer.setCitiesListEntryLayout (rowLayout);
		renderer.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Set up form
		final CitiesListUI cities = new CitiesListUI ();
		cities.setCitiesListLayout (layout);
		cities.setUtils (utils);
		cities.setLanguageHolder (langHolder);
		cities.setLanguageChangeMaster (langMaster);
		cities.setLargeFont (CreateFontsForTests.getLargeFont ());
		cities.setSmallFont (CreateFontsForTests.getSmallFont ());
		cities.setClient (client);
		cities.setMultiplayerSessionUtils (multiplayerSessionUtils);
		cities.setWizardClientUtils (wizardClientUtils);
		cities.setMemoryBuildingUtils (memoryBuildingUtils);
		cities.setCityProductionCalculations (prod);
		cities.setMiniMapBitmapGenerator (gen);
		cities.setCitiesListCellRenderer (renderer);
		
		// Display form		
		cities.setVisible (true);
		Thread.sleep (5000);
		cities.setVisible (false);
	}
}
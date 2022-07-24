package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.CitiesListScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.renderer.CitiesListCellRenderer;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.UnitCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.OverlandMapSize;
import momime.common.database.RaceEx;
import momime.common.database.RacePopulationTask;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillWeaponGrade;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Tests the CitiesListUI class
 */
@ExtendWith(MockitoExtension.class)
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

		int r = 0;
		for (String raceName : new String [] {"barbarian", "gnoll", "halfling"})
		{
			r++;
			final RaceEx race = new RaceEx ();
			
			int t = 0;
			for (String taskImage : new String [] {"farmer", "worker", "rebel"})
			{
				t++;
				final RacePopulationTask task = new RacePopulationTask ();
				task.setPopulationTaskID ("PT0" + t);
				task.setCivilianImageFile ("/momime.client.graphics/races/" + raceName + "/" + taskImage + ".png");
				race.getRacePopulationTask ().add (task);
			}
			
			race.buildMap ();
			when (db.findRace (eq ("RC0" + r), anyString ())).thenReturn (race);
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
		
		final UnitSkillEx melee = new UnitSkillEx ();
		int w = 0;
		for (final String weaponGradeName : new String [] {"Normal", "Alchemy", "Mithril", "Adamantium"})
		{
			final UnitSkillWeaponGrade weaponGrade = new UnitSkillWeaponGrade ();
			weaponGrade.setWeaponGradeNumber (w);
			weaponGrade.setSkillImageFile ("/momime.client.graphics/unitSkills/melee" + weaponGradeName + ".png");
			melee.getUnitSkillWeaponGrade ().add (weaponGrade);
			
			w++;
		}
		melee.buildMap ();
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "refreshCitiesList")).thenReturn (melee);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final CitiesListScreen citiesListScreenLang = new CitiesListScreen ();
		citiesListScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "The Cities of PLAYER_NAME"));
		citiesListScreenLang.getCityName ().add (createLanguageText (Language.ENGLISH, "Name"));
		citiesListScreenLang.getCityPopulation ().add (createLanguageText (Language.ENGLISH, "Population"));
		citiesListScreenLang.getCityUnits ().add (createLanguageText (Language.ENGLISH, "Units"));
		citiesListScreenLang.getCityWeaponGrade ().add (createLanguageText (Language.ENGLISH, "Wep"));
		citiesListScreenLang.getCityEnchantments ().add (createLanguageText (Language.ENGLISH, "Ench"));
		citiesListScreenLang.getCitySell ().add (createLanguageText (Language.ENGLISH, "Sell"));
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
		
		// Wizard
		final KnownWizardDetails ourWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (fow.getWizardDetails (), 1, "refreshCitiesList")).thenReturn (ourWizard);
		
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
		city3Data.setCityPopulation (25000);
		city3Data.setMinimumFarmers (5);
		city3Data.setOptionalFarmers (3);
		city3Data.setNumberOfRebels (4);
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
		
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		int cityNumber = 0;
		for (final OverlandMapCityData cityData : new OverlandMapCityData [] {city1Data, city2Data, city3Data, city4Data, city5Data})
		{
			final int x = cityData.getCityPopulation () % 100;
			final int y = cityData.getCityPopulation () / 1000;
			final int z = cityData.getCityName ().startsWith ("W") ? 1 : 0;
			
			terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).setCityData (cityData);
			
			if (cityData.getCityName ().startsWith ("Z"))
				terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x).setBuildingIdSoldThisTurn ("X");
			
			when (unitCalculations.calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
				(fow.getBuilding (), fow.getMap (), new MapCoordinates3DEx (x, y, z), ourWizard.getPick (), mapSize, db)).thenReturn (cityNumber % 4); 
			
			// Enchantments and curses
			final int enchantmentCount = cityNumber / 2;
			final int curseCount = cityNumber % 2;
			
			for (int n = 0; n < enchantmentCount; n++)
			{
				final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
				spell.setCityLocation (new MapCoordinates3DEx (x, y, z));
				spell.setCastingPlayerID (1);
				fow.getMaintainedSpell ().add (spell);
			}
			
			for (int n = 0; n < curseCount; n++)
			{
				final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
				spell.setCityLocation (new MapCoordinates3DEx (x, y, z));
				spell.setCastingPlayerID (2);
				fow.getMaintainedSpell ().add (spell);
			}
			
			cityNumber++;
		}
		
		// Capital
		final MemoryBuilding fortress = new MemoryBuilding ();
		fortress.setCityLocation (new MapCoordinates3DEx (34, 12, 0));
		
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findCityWithBuilding (1, CommonDatabaseConstants.BUILDING_FORTRESS, terrain, fow.getBuilding ())).thenReturn (fortress);
		
		// Client		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPlayerID ()).thenReturn (1);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getClientDB ()).thenReturn (db);

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
		cities.setKnownWizardUtils (knownWizardUtils);
		cities.setMultiplayerSessionUtils (multiplayerSessionUtils);
		cities.setWizardClientUtils (wizardClientUtils);
		cities.setMemoryBuildingUtils (memoryBuildingUtils);
		cities.setUnitCalculations (unitCalculations);
		cities.setCitiesListCellRenderer (renderer);
		
		// Display form		
		cities.setVisible (true);
		Thread.sleep (5000);
		cities.setVisible (false);
	}
}
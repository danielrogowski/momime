package momime.client.ui.panels;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.areas.storage.MapArea2DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.MapRightHandBar;
import momime.client.languages.database.OverlandMapScreen;
import momime.client.languages.database.Simple;
import momime.client.languages.database.SurveyorTab;
import momime.client.newturnmessages.NewTurnMessageSpellEx;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Event;
import momime.common.database.Language;
import momime.common.database.MapFeatureEx;
import momime.common.database.OverlandMapSize;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellBookSection;
import momime.common.database.SpellBookSectionID;
import momime.common.database.TileTypeEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.TurnPhase;
import momime.common.messages.TurnSystem;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.ResourceValueUtils;

/**
 * Tests the OverlandMapRightHandPanel class
 */
@ExtendWith(MockitoExtension.class)
public final class TestOverlandMapRightHandPanel extends ClientTestData
{
	/**
	 * All the tests need to set the panel up in the same way, so do so via this common method
	 * 
	 * @param isSpellPanel Whether this is the test to show the spell panel, which requires a couple of extra mocks
	 * @return Panel to test with
	 * @throws Exception If there is a problem
	 */
	private final PanelAndFrame createPanel (final boolean isSpellPanel) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx goldProduction = new ProductionTypeEx ();
		goldProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Gold"));
		goldProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "GP"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD), anyString ())).thenReturn (goldProduction);
		
		final ProductionTypeEx rationsProduction = new ProductionTypeEx ();
		rationsProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Rations"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS), anyString ())).thenReturn (rationsProduction);
		
		final ProductionTypeEx foodProduction = new ProductionTypeEx ();
		foodProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Food"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD), anyString ())).thenReturn (foodProduction);
		
		final ProductionTypeEx productionProduction = new ProductionTypeEx ();
		productionProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Production"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION), anyString ())).thenReturn (productionProduction);
		
		final ProductionTypeEx manaProduction = new ProductionTypeEx ();
		manaProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Mana"));
		manaProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA), anyString ())).thenReturn (manaProduction);
		
		final ProductionTypeEx magicPowerProduction = new ProductionTypeEx ();
		magicPowerProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Magic Power"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER), anyString ())).thenReturn (magicPowerProduction);
		
		final TileTypeEx tileTypeLang = new TileTypeEx ();
		tileTypeLang.getTileTypeDescription ().add (createLanguageText (Language.ENGLISH, "The tile type"));
		tileTypeLang.getTileTypeCannotBuildCityDescription ().add (createLanguageText (Language.ENGLISH, "Can't build city (tile)"));
		when (db.findTileType (eq ("TT01"), anyString ())).thenReturn (tileTypeLang);
		
		final MapFeatureEx mapFeatureLang = new MapFeatureEx ();
		mapFeatureLang.getMapFeatureDescription ().add (createLanguageText (Language.ENGLISH, "The map feature"));
		mapFeatureLang.getMapFeatureMagicWeaponsDescription ().add (createLanguageText (Language.ENGLISH, "Builds +1 Magic Weapons"));
		when (db.findMapFeature (eq ("MF01"), anyString ())).thenReturn (mapFeatureLang);

		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		
		final MapRightHandBar mapRightHandBarLang = new MapRightHandBar ();
		mapRightHandBarLang.getDone ().add (createLanguageText (Language.ENGLISH, "Done"));
		mapRightHandBarLang.getPatrol ().add (createLanguageText (Language.ENGLISH, "Patrol"));
		mapRightHandBarLang.getWait ().add (createLanguageText (Language.ENGLISH, "Wait"));
		mapRightHandBarLang.getOnePlayerAtATimeCurrentPlayer ().add (createLanguageText (Language.ENGLISH, "Current player:"));
		mapRightHandBarLang.getSimultaneousTurnsWaitingLine1 ().add (createLanguageText (Language.ENGLISH, "Waiting for"));
		mapRightHandBarLang.getSimultaneousTurnsWaitingLine2 ().add (createLanguageText (Language.ENGLISH, "other players"));
		mapRightHandBarLang.getProductionPerTurn ().add (createLanguageText (Language.ENGLISH, "AMOUNT_PER_TURN PRODUCTION_TYPE per turn"));
		mapRightHandBarLang.getProductionPerTurnMagicPower ().add (createLanguageText (Language.ENGLISH, "Power Base AMOUNT_PER_TURN"));
		mapRightHandBarLang.getTargetSpell ().add (createLanguageText (Language.ENGLISH, "Target Spell"));
		
		final SurveyorTab surveyorTabLang = new SurveyorTab ();
		surveyorTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Surveyor"));
		surveyorTabLang.getCityResources ().add (createLanguageText (Language.ENGLISH, "City Resources"));
		surveyorTabLang.getFeatureProvidesSpellProtection ().add (createLanguageText (Language.ENGLISH, "Protects against spells"));
		surveyorTabLang.getCorrupted ().add (createLanguageText (Language.ENGLISH, "Corrupted"));
		surveyorTabLang.getCantBuildCityTooCloseToAnotherCity ().add (createLanguageText (Language.ENGLISH,
			"Cities cannot be built" + System.lineSeparator () + "within CITY_SEPARATION squares" + System.lineSeparator () + "of another city"));

		final OverlandMapScreen overlandMapScreenLang = new OverlandMapScreen ();
		overlandMapScreenLang.setMapRightHandBar (mapRightHandBarLang);
		overlandMapScreenLang.setSurveyorTab (surveyorTabLang);
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getOverlandMapScreen ()).thenReturn (overlandMapScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		if (isSpellPanel)
		{
			final SpellBookSection section = new SpellBookSection ();
			section.getSpellTargetPrompt ().add (createLanguageText (Language.ENGLISH, "Select a friendly city to cast your SPELL_NAME spell on"));
			when (db.findSpellBookSection (SpellBookSectionID.CITY_ENCHANTMENTS, "OverlandMapRightHandPanel")).thenReturn (section);
			
			final Spell spell = new Spell ();
			spell.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
			spell.getSpellName ().add (createLanguageText (Language.ENGLISH, "Heavenly Light"));
			
			when (db.findSpell ("SP001", "OverlandMapRightHandPanel")).thenReturn (spell);
		}

		final TileTypeEx tileType = new TileTypeEx ();
		tileType.setCanBuildCity (true);
		tileType.setDoubleFood (3);
		tileType.setProductionBonus (4);
		tileType.setGoldBonus (10);		
		when (db.findTileType ("TT01", "surveyorLocationOrLanguageChanged")).thenReturn (tileType);
		
		when (db.findTileType ("FOW", "surveyorLocationOrLanguageChanged")).thenReturn (new TileTypeEx ());
		
		final MapFeatureEx mapFeature = new MapFeatureEx ();
		mapFeature.setCanBuildCity (true);
		mapFeature.setFeatureMagicWeapons (3);
		mapFeature.setFeatureSpellProtection (true);
		when (db.findMapFeature ("MF01", "surveyorLocationOrLanguageChanged")).thenReturn (mapFeature);
		
		// Overland map
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MapVolumeOfMemoryGridCells map = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (map);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF01");
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Player private knowledge
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		when (client.getOurPlayerID ()).thenReturn (3);
		when (client.getClientDB ()).thenReturn (db);
		
		// General public knowledge
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setCurrentPlayerID (3);
		
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		sd.setOverlandMapSize (overlandMapSize);
		
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Players
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setPlayerType (PlayerType.HUMAN);
		
		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("FF8000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, null, trans1);
		when (wizardClientUtils.getPlayerName (player1)).thenReturn ("Mr. Blah");

		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (-1);
		pd2.setPlayerType (PlayerType.AI);
		
		final MomTransientPlayerPublicKnowledge trans2 = new MomTransientPlayerPublicKnowledge ();
		trans2.setFlagColour ("FF0080");
		
		final PlayerPublicDetails player2 = new PlayerPublicDetails (pd2, null, trans2);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		players.add (player2);
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Wizard
		final KnownWizardDetails wizard1 = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (eq (fow.getWizardDetails ()), eq (pd1.getPlayerID ()), anyString ())).thenReturn (wizard1);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd1.getPlayerID ()), anyString ())).thenReturn (player1);
		
		// Resource values
		final ResourceValueUtils resources = mock (ResourceValueUtils.class);
		when (resources.findAmountStoredForProductionType (ppk.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (99999);
		when (resources.findAmountStoredForProductionType (ppk.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (99999);
		
		// City info
		final CityCalculations cityCalc = mock (CityCalculations.class);

		final MapArea2DArrayListImpl<Boolean> invalidCityLocations = new MapArea2DArrayListImpl<Boolean> ();
		invalidCityLocations.setCoordinateSystem (overlandMapSize);
		
		for (int x = 0; x < overlandMapSize.getWidth (); x++)
			for (int y = 0; y < overlandMapSize.getHeight (); y++)
				invalidCityLocations.set (x, y, true);
		
		when (cityCalc.markWithinExistingCityRadius (map, null, 1, overlandMapSize)).thenReturn (invalidCityLocations);
		
		// Conjunction
		gpk.setConjunctionEventID ("EV01");
		
		final Event eventDef = new Event ();
		eventDef.getEventName ().add (createLanguageText (Language.ENGLISH, "Conjunction of Nature"));
		when (db.findEvent ("EV01", "updateConjunction")).thenReturn (eventDef);
		
		// Component factory
		final UIComponentFactory uiComponentFactory = mock (UIComponentFactory.class);
		when (uiComponentFactory.createSelectUnitButton ()).thenAnswer ((i) ->
		{
			final SelectUnitButton button = new SelectUnitButton ();
			button.setUtils (utils);
			return button;
		});
		
		// Layouts
		final XmlLayoutContainerEx rhpLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/OverlandMapRightHandPanel.xml"));
		rhpLayout.buildMaps ();

		final XmlLayoutContainerEx surveyorLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/OverlandMapRightHandPanel-Surveyor.xml"));
		surveyorLayout.buildMaps ();
		
		final XmlLayoutContainerEx economyLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/OverlandMapRightHandPanel-Economy.xml"));
		economyLayout.buildMaps ();
		
		// Set up panel
		final OverlandMapRightHandPanel panel = new OverlandMapRightHandPanel ();
		panel.setUtils (utils);
		panel.setClient (client);
		panel.setLanguageHolder (langHolder);
		panel.setLanguageChangeMaster (langMaster);
		panel.setResourceValueUtils (resources);
		panel.setCityCalculations (cityCalc);
		panel.setTextUtils (new TextUtilsImpl ());
		panel.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		panel.setUiComponentFactory (uiComponentFactory);
		panel.setMultiplayerSessionUtils (multiplayerSessionUtils);
		panel.setSmallFont (CreateFontsForTests.getSmallFont ());
		panel.setMediumFont (CreateFontsForTests.getMediumFont ());
		panel.setLargeFont (CreateFontsForTests.getLargeFont ());
		panel.setOverlandMapRightHandPanelLayout (rhpLayout);
		panel.setSurveyorLayout (surveyorLayout);
		panel.setEconomyLayout (economyLayout);
		panel.setWizardClientUtils (wizardClientUtils);
		panel.setKnownWizardUtils (knownWizardUtils);
		
		// Set up a dummy frame to display the panel
		final JFrame frame = new JFrame ("testOverlandMapRightHandPanel");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (panel.getPanel ());
		frame.pack ();
		frame.setLocationRelativeTo (null);
		frame.setVisible (true);

		// Have to do this after the frame is displayed
		panel.setSurveyorLocation (new MapCoordinates3DEx (20, 10, 1));
		panel.setIndexOfCurrentPlayer (1);
		
		final PanelAndFrame result = new PanelAndFrame ();
		result.panel = panel;
		result.frame = frame;
		return result;
	}
	
	/**
	 * Tests showing the units panel and special order buttons
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testUnitsPanel () throws Exception
	{
		final PanelAndFrame panel = createPanel (false);
		panel.panel.setTop (OverlandMapRightHandPanelTop.UNITS);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.SPECIAL_ORDERS);
		
		Thread.sleep (5000);
		panel.frame.setVisible (false);
	}

	/**
	 * Tests showing the surveyor panel and cancel button
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSurveyorPanel_Normal () throws Exception
	{
		final PanelAndFrame panel = createPanel (false);
		panel.panel.setTop (OverlandMapRightHandPanelTop.SURVEYOR);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.CANCEL);
		
		Thread.sleep (5000);
		panel.frame.setVisible (false);
	}

	/**
	 * Tests showing the surveyor panel on a corrupted land tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSurveyorPanel_Corrupted () throws Exception
	{
		final PanelAndFrame panel = createPanel (false);
		final MomPersistentPlayerPrivateKnowledge ppk = panel.panel.getClient ().getOurPersistentPlayerPrivateKnowledge ();
		ppk.getFogOfWarMemory ().getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).getTerrainData ().setCorrupted (1);
		
		// Have to force the surveyor info to update, since it has already been displayed
		panel.panel.setSurveyorLocation (new MapCoordinates3DEx (20, 10, 1));
		
		panel.panel.setTop (OverlandMapRightHandPanelTop.SURVEYOR);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.CANCEL);
		
		Thread.sleep (5000);
		panel.frame.setVisible (false);
	}

	/**
	 * Tests showing the target spell panel and cancel button
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testTargetSpellPanel () throws Exception
	{
		final PanelAndFrame panel = createPanel (true);
		panel.panel.setTop (OverlandMapRightHandPanelTop.TARGET_SPELL);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.CANCEL);
		
		final NewTurnMessageSpellEx ntm = new NewTurnMessageSpellEx ();
		ntm.setSpellID ("SP001");
		
		panel.panel.setTargetSpell (ntm);

		Thread.sleep (5000);
		panel.frame.setVisible (false);
	}

	/**
	 * Tests showing the economy panel and next turn button
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEconomyPanel_OurTurn () throws Exception
	{
		final PanelAndFrame panel = createPanel (false);
		panel.panel.setTop (OverlandMapRightHandPanelTop.ECONOMY);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.NEXT_TURN_BUTTON);
		
		Thread.sleep (5000);
		panel.frame.setVisible (false);
	}

	/**
	 * Tests showing the economy panel and current player in a one-player-at-a-time game
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEconomyPanel_OtherPlayersTurn_OnePlayerAtATime () throws Exception
	{
		final PanelAndFrame panel = createPanel (false);
		panel.panel.setTop (OverlandMapRightHandPanelTop.ECONOMY);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.PLAYER);

		Thread.sleep (5000);
		panel.frame.setVisible (false);
	}

	/**
	 * Tests showing the economy panel and current player in a simultaneous turns game
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEconomyPanel_OtherPlayersTurn_Simultaneous () throws Exception
	{
		final PanelAndFrame panel = createPanel (false);
		panel.panel.setTop (OverlandMapRightHandPanelTop.ECONOMY);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.PLAYER);
		
		panel.panel.getClient ().getSessionDescription ().setTurnSystem (TurnSystem.SIMULTANEOUS);
		panel.panel.getClient ().getGeneralPublicKnowledge ().setTurnPhase (TurnPhase.ALLOCATING_MOVES);
		panel.panel.turnSystemOrCurrentPlayerChanged ();

		Thread.sleep (5000);
		panel.frame.setVisible (false);
	}

	/** Common setup method needs to return both a panel and frame, so need small class to hold them both */
	private class PanelAndFrame
	{
		/** Panel created by createPanel method */ 
		private OverlandMapRightHandPanel panel;

		/** Frame created by createPanel method */ 
		private JFrame frame;
	}
}
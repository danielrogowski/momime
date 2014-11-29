package momime.client.ui.panels;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.database.MapFeature;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.SpellBookSection;
import momime.client.newturnmessages.NewTurnMessageSpellEx;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.TileType;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.TurnSystem;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.ResourceValueUtils;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ndg.map.areas.storage.MapArea2DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the OverlandMapRightHandPanel class
 */
public final class TestOverlandMapRightHandPanel
{
	/**
	 * All the tests need to set the panel up in the same way, so do so via this common method
	 * 
	 * @return Panel to test with
	 * @throws Exception If there is a problem
	 */
	private final PanelAndFrame createPanel () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmMapRightHandBar", "Cancel")).thenReturn ("Cancel");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "Done")).thenReturn ("Done");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "Patrol")).thenReturn ("Patrol");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "Wait")).thenReturn ("Wait");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "OnePlayerAtATimeCurrentPlayer")).thenReturn ("Current player:");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "SimultaneousTurnsLine1")).thenReturn ("Waiting for");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "SimultaneousTurnsLine2")).thenReturn ("other players");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "ProductionPerTurn")).thenReturn ("AMOUNT_PER_TURN PRODUCTION_TYPE per turn");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "ProductionPerTurnMagicPower")).thenReturn ("Power Base AMOUNT_PER_TURN");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "TargetSpell")).thenReturn ("Target Spell");
		
		when (lang.findCategoryEntry ("frmSurveyor", "Title")).thenReturn ("Surveyor");
		when (lang.findCategoryEntry ("frmSurveyor", "CityResources")).thenReturn ("City Resources");
		when (lang.findCategoryEntry ("frmSurveyor", "FeatureProvidesSpellProtection")).thenReturn ("Protects against spells");
		when (lang.findCategoryEntry ("frmSurveyor", "CantBuildCityTooCloseToAnotherCity")).thenReturn ("Cities cannot be built" + System.lineSeparator () + "within CITY_SEPARATION squares" + System.lineSeparator () + "of another city");
		
		final ProductionType goldProduction = new ProductionType ();
		goldProduction.setProductionTypeDescription ("Gold");
		goldProduction.setProductionTypeSuffix ("GP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD)).thenReturn (goldProduction);
		
		final ProductionType rationsProduction = new ProductionType ();
		rationsProduction.setProductionTypeDescription ("Rations");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS)).thenReturn (rationsProduction);
		
		final ProductionType foodProduction = new ProductionType ();
		foodProduction.setProductionTypeDescription ("Food");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD)).thenReturn (foodProduction);
		
		final ProductionType productionProduction = new ProductionType ();
		productionProduction.setProductionTypeDescription ("Production");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_PRODUCTION)).thenReturn (productionProduction);
		
		final ProductionType manaProduction = new ProductionType ();
		manaProduction.setProductionTypeDescription ("Mana");
		manaProduction.setProductionTypeSuffix ("MP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)).thenReturn (manaProduction);
		
		final SpellBookSection section = new SpellBookSection ();
		section.setSpellTargetPrompt ("Select a friendly city to cast your SPELL_NAME spell on");
		when (lang.findSpellBookSection (SpellBookSectionID.CITY_ENCHANTMENTS)).thenReturn (section);
		
		final momime.client.language.database.v0_9_5.Spell spellLang = new momime.client.language.database.v0_9_5.Spell ();
		spellLang.setSpellName ("Heavenly Light");
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		final momime.client.language.database.v0_9_5.TileType tileTypeLang = new momime.client.language.database.v0_9_5.TileType ();
		tileTypeLang.setTileTypeDescription ("The tile type");
		tileTypeLang.setTileTypeCannotBuildCityDescription ("Can't build city (tile)");
		when (lang.findTileType ("TT01")).thenReturn (tileTypeLang);
		
		final momime.client.language.database.v0_9_5.MapFeature mapFeatureLang = new momime.client.language.database.v0_9_5.MapFeature ();
		mapFeatureLang.setMapFeatureDescription ("The map feature");
		mapFeatureLang.setMapFeatureMagicWeaponsDescription ("Builds +1 Magic Weapons");
		when (lang.findMapFeature ("MF01")).thenReturn (mapFeatureLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from client DB
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		when (db.findSpell ("SP001", "OverlandMapRightHandPanel")).thenReturn (spell);

		final TileType tileType = new TileType ();
		tileType.setCanBuildCity (true);
		tileType.setDoubleFood (3);
		tileType.setProductionBonus (4);
		tileType.setGoldBonus (10);		
		when (db.findTileType ("TT01", "surveyorLocationOrLanguageChanged")).thenReturn (tileType);
		
		final MapFeature mapFeature = new MapFeature ();
		mapFeature.setCanBuildCity (true);
		mapFeature.setFeatureMagicWeapons (3);
		mapFeature.setFeatureSpellProtection (true);
		when (db.findMapFeature ("MF01", "surveyorLocationOrLanguageChanged")).thenReturn (mapFeature);
		
		// Overland map
		final MapSizeData mapSize = ClientTestData.createMapSizeData ();
		
		final MapVolumeOfMemoryGridCells map = ClientTestData.createOverlandMap (mapSize);
		
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
		sd.setMapSize (mapSize);
		
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Players
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		pd1.setPlayerName ("Mr. Blah");
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "updateAmountPerTurn")).thenReturn (player1);
		
		// Resource values
		final ResourceValueUtils resources = mock (ResourceValueUtils.class);
		when (resources.findAmountStoredForProductionType (ppk.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD)).thenReturn (99999);
		when (resources.findAmountStoredForProductionType (ppk.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)).thenReturn (99999);
		
		// City info
		final CityCalculations cityCalc = mock (CityCalculations.class);

		final MapArea2DArrayListImpl<Boolean> invalidCityLocations = new MapArea2DArrayListImpl<Boolean> ();
		invalidCityLocations.setCoordinateSystem (mapSize);
		
		for (int x = 0; x < mapSize.getWidth (); x++)
			for (int y = 0; y < mapSize.getHeight (); y++)
				invalidCityLocations.set (x, y, true);
		
		when (cityCalc.markWithinExistingCityRadius (map, 1, mapSize)).thenReturn (invalidCityLocations);
		
		// Component factory
		final UIComponentFactory uiComponentFactory = mock (UIComponentFactory.class);
		when (uiComponentFactory.createSelectUnitButton ()).thenAnswer (new Answer<SelectUnitButton> ()
		{
			@Override
			public final SelectUnitButton answer (final InvocationOnMock invocation) throws Throwable
			{
				final SelectUnitButton button = new SelectUnitButton ();
				button.setUtils (utils);
				return button;
			}
		});
		
		// Layout
		final XmlLayoutContainerEx surveyorLayout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.panels/OverlandMapRightHandPanel-Surveyor.xml"));
		surveyorLayout.buildMaps ();
		
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
		panel.setSurveyorLayout (surveyorLayout);
		
		// Set up a dummy frame to display the panel
		final JFrame frame = new JFrame ("testOverlandMapRightHandPanel");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane (panel.getPanel ());
		frame.pack ();
		frame.setLocationRelativeTo (null);
		frame.setVisible (true);

		// Have to do this after the frame is displayed
		panel.setSurveyorLocation (new MapCoordinates3DEx (20, 10, 1));
		
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
		final PanelAndFrame panel = createPanel ();
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
	public final void testSurveyorPanel () throws Exception
	{
		final PanelAndFrame panel = createPanel ();
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
		final PanelAndFrame panel = createPanel ();
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
		final PanelAndFrame panel = createPanel ();
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
		final PanelAndFrame panel = createPanel ();
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
		final PanelAndFrame panel = createPanel ();
		panel.panel.setTop (OverlandMapRightHandPanelTop.ECONOMY);
		panel.panel.setBottom (OverlandMapRightHandPanelBottom.PLAYER);
		
		panel.panel.getClient ().getSessionDescription ().setTurnSystem (TurnSystem.SIMULTANEOUS);
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
package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.config.MomImeClientConfig;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.MapButtonBar;
import momime.client.languages.database.MapRightHandBar;
import momime.client.languages.database.Month;
import momime.client.languages.database.OverlandMapScreen;
import momime.client.languages.database.Simple;
import momime.client.languages.database.SurveyorTab;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.OverlandMapSize;
import momime.common.database.ProductionTypeEx;
import momime.common.database.TileSetEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.TurnSystem;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;

/**
 * Tests the OverlandMapUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestOverlandMapUI extends ClientTestData
{
	/**
	 * Tests the OverlandMapUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOverlandMapUI () throws Exception
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

		final ProductionTypeEx manaProduction = new ProductionTypeEx ();
		manaProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Mana"));
		manaProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA), anyString ())).thenReturn (manaProduction);
		
		final ProductionTypeEx rationsProduction = new ProductionTypeEx ();
		rationsProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Rations"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS), anyString ())).thenReturn (rationsProduction);

		final ProductionTypeEx magicPowerProduction = new ProductionTypeEx ();
		magicPowerProduction.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Magic Power"));
		when (db.findProductionType (eq (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER), anyString ())).thenReturn (magicPowerProduction);
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.setAnimationSpeed (2.0);
		overlandMapTileSet.setAnimationFrameCount (3);
		when (db.findTileSet (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP, "OverlandMapUI.init")).thenReturn (overlandMapTileSet);
	
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		
		final MapButtonBar mapButtonBarLang = new MapButtonBar ();
		mapButtonBarLang.getGame ().add (createLanguageText (Language.ENGLISH, "Game"));
		mapButtonBarLang.getSpells ().add (createLanguageText (Language.ENGLISH, "Spells"));
		mapButtonBarLang.getArmies ().add (createLanguageText (Language.ENGLISH, "Armies"));
		mapButtonBarLang.getCities ().add (createLanguageText (Language.ENGLISH, "Cities"));
		mapButtonBarLang.getMagic ().add (createLanguageText (Language.ENGLISH, "Magic"));
		mapButtonBarLang.getPlane ().add (createLanguageText (Language.ENGLISH, "Plane"));
		mapButtonBarLang.getNewTurnMessages ().add (createLanguageText (Language.ENGLISH, "Msgs"));
		mapButtonBarLang.getChat ().add (createLanguageText (Language.ENGLISH, "Chat"));
		mapButtonBarLang.getTurn ().add (createLanguageText (Language.ENGLISH, "MONTH YEAR (Turn TURN)"));

		final Month month = new Month ();
		month.setMonthNumber (1);
		month.getName ().add (createLanguageText (Language.ENGLISH, "January"));
		
		final SurveyorTab surveyorTabLang = new SurveyorTab ();
		surveyorTabLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Surveyor"));

		// Language entries needed by the right hand panel
		final MapRightHandBar mapRightHandBarLang = new MapRightHandBar ();
		mapRightHandBarLang.getProductionPerTurn ().add (createLanguageText (Language.ENGLISH, "AMOUNT_PER_TURN PRODUCTION_TYPE per turn"));
		mapRightHandBarLang.getProductionPerTurnMagicPower ().add (createLanguageText (Language.ENGLISH, "Power Base AMOUNT_PER_TURN"));
		
		final OverlandMapScreen overlandMapScreenLang = new OverlandMapScreen ();
		overlandMapScreenLang.setMapButtonBar (mapButtonBarLang);
		overlandMapScreenLang.setMapRightHandBar (mapRightHandBarLang);
		overlandMapScreenLang.setSurveyorTab (surveyorTabLang);
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getOverlandMapScreen ()).thenReturn (overlandMapScreenLang);
		when (lang.getMonth ()).thenReturn (Arrays.asList (month));
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Set up session description
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (overlandMapSize);
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		final MomClient client = mock (MomClient.class);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getClientDB ()).thenReturn (db);
		
		// Set up FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// General public knowledge
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (1);
		gpk.setCurrentPlayerID (3);
		
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Player
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		
		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();
		
		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("800000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		when (wizardClientUtils.getPlayerName (player1)).thenReturn ("Mr. Blah");
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (3);
		
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);
		
		// Wizard
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);

		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");
		when (knownWizardUtils.findKnownWizardDetails (priv.getKnownWizardDetails (), pd1.getPlayerID (), "OverlandMapUI")).thenReturn (wizardDetails);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players) , eq (pd1.getPlayerID ()), anyString ())).thenReturn (player1);
		
		// Config
		final MomImeClientConfig config = new MomImeClientConfig (); 
		
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
		
		// Set up right hand panel
		final OverlandMapRightHandPanel rhp = new OverlandMapRightHandPanel ();
		rhp.setUtils (utils);
		rhp.setLanguageHolder (langHolder);
		rhp.setLanguageChangeMaster (langMaster);
		rhp.setClient (client);
		rhp.setResourceValueUtils (mock (ResourceValueUtils.class));
		rhp.setUiComponentFactory (uiComponentFactory);
		rhp.setMultiplayerSessionUtils (multiplayerSessionUtils);
		rhp.setTextUtils (new TextUtilsImpl ());
		rhp.setMemoryGridCellUtils (new MemoryGridCellUtilsImpl ());
		rhp.setSmallFont (CreateFontsForTests.getSmallFont ());
		rhp.setMediumFont (CreateFontsForTests.getMediumFont ());
		rhp.setLargeFont (CreateFontsForTests.getLargeFont ());
		rhp.setOverlandMapRightHandPanelLayout (rhpLayout);
		rhp.setSurveyorLayout (surveyorLayout);
		rhp.setEconomyLayout (economyLayout);
		rhp.setWizardClientUtils (wizardClientUtils);

		// Give it some dummy images for the terrain
		final BufferedImage [] overlandMapBitmaps = new BufferedImage [overlandMapTileSet.getAnimationFrameCount ()];
		for (int n = 0; n < overlandMapBitmaps.length; n++)
			overlandMapBitmaps [n] = createSolidImage (60 * 20, 40 * 18, (new int [] {0x200000, 0x002000, 0x000020}) [n]);
		
		final OverlandMapBitmapGenerator gen = mock (OverlandMapBitmapGenerator.class);
		when (gen.generateOverlandMapBitmaps (0, 0, 0, overlandMapSize.getWidth (), overlandMapSize.getHeight ())).thenReturn (overlandMapBitmaps);
		
		// Set up form
		final OverlandMapUI map = new OverlandMapUI ();
		map.setUtils (utils);
		map.setGraphicsDB (gfx);
		map.setLanguageHolder (langHolder);
		map.setLanguageChangeMaster (langMaster);
		map.setClient (client);
		map.setClientConfig (config);
		map.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		map.setKnownWizardUtils (knownWizardUtils);
		map.setOverlandMapRightHandPanel (rhp);
		map.setSmallFont (CreateFontsForTests.getSmallFont ());

		map.setOverlandMapBitmapGenerator (gen);
		map.regenerateOverlandMapBitmaps ();
		
		// Display form		
		map.setVisible (true);
		map.updateTurnLabelText ();		// Must do this after .init (), or the label we're updating won't exist yet
		Thread.sleep (5000);
		map.setVisible (false);
	}
}
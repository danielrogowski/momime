package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.TurnSystem;
import momime.common.utils.MemoryGridCellUtilsImpl;
import momime.common.utils.ResourceValueUtils;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the OverlandMapUI class
 */
public final class TestOverlandMapUI
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
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final TileSetEx overlandMapTileSet = new TileSetEx ();
		overlandMapTileSet.setAnimationSpeed (2.0);
		overlandMapTileSet.setAnimationFrameCount (3);
		when (gfx.findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "OverlandMapUI.init")).thenReturn (overlandMapTileSet);
	
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmMapButtonBar", "Game")).thenReturn ("Game");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Spells")).thenReturn ("Spells");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Armies")).thenReturn ("Armies");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Cities")).thenReturn ("Cities");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Magic")).thenReturn ("Magic");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Plane")).thenReturn ("Plane");
		when (lang.findCategoryEntry ("frmMapButtonBar", "NewTurnMessages")).thenReturn ("Msgs");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Chat")).thenReturn ("Chat");

		when (lang.findCategoryEntry ("Months", "MNTH01")).thenReturn ("January");
		when (lang.findCategoryEntry ("frmMapButtonBar", "Turn")).thenReturn ("MONTH YEAR (Turn TURN)");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Language entries needed by the right hand panel
		when (lang.findCategoryEntry ("frmMapRightHandBar", "ProductionPerTurn")).thenReturn ("AMOUNT_PER_TURN PRODUCTION_TYPE per turn");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "ProductionPerTurnMagicPower")).thenReturn ("Power Base AMOUNT_PER_TURN");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "GoldStored")).thenReturn ("AMOUNT_STORED GP");
		when (lang.findCategoryEntry ("frmMapRightHandBar", "ManaStored")).thenReturn ("AMOUNT_STORED MP");
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Set up session description
		final MapSizeData mapSize = ClientTestData.createMapSizeData ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (mapSize);
		sd.setTurnSystem (TurnSystem.ONE_PLAYER_AT_A_TIME);
		
		final MomClient client = mock (MomClient.class);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Set up FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// General public knowledge
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (1);
		gpk.setCurrentPlayerID (3);
		
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Player
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (3);
		pd1.setHuman (true);
		pd1.setPlayerName ("Mr. Blah");
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (3);
		when (client.getOurTransientPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd1.getPlayerID (), "updateAmountPerTurn")).thenReturn (player1);
		
		// Config
		final MomImeClientConfig config = new MomImeClientConfig (); 
		
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
		rhp.setSurveyorLayout (surveyorLayout);

		// Give it some dummy images for the terrain
		final BufferedImage [] overlandMapBitmaps = new BufferedImage [overlandMapTileSet.getAnimationFrameCount ()];
		for (int n = 0; n < overlandMapBitmaps.length; n++)
		{
			final BufferedImage bitmap = new BufferedImage (60 * 20, 40 * 18, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = bitmap.createGraphics ();
			try
			{
				switch (n)
				{
					case 0:
						g.setColor (new Color (0x200000));
						break;
						
					case 1:
						g.setColor (new Color (0x002000));
						break;
						
					case 2:
						g.setColor (new Color (0x000020));
						break;
				}
				
				g.fillRect (0, 0, 60 * 20, 40 * 18);
				
				// Draw a white outline so we can see the wrapping borders
				g.setColor (Color.WHITE);
				g.drawRect (0, 0, (60 * 20) - 1, (40 * 18) - 1);
			}
			finally
			{
				g.dispose ();
			}
			overlandMapBitmaps [n] = bitmap;
		}
		
		final OverlandMapBitmapGenerator gen = mock (OverlandMapBitmapGenerator.class);
		when (gen.generateOverlandMapBitmaps (0, 0, 0, mapSize.getWidth (), mapSize.getHeight ())).thenReturn (overlandMapBitmaps);
		
		// Set up form
		final OverlandMapUI map = new OverlandMapUI ();
		map.setUtils (utils);
		map.setGraphicsDB (gfx);
		map.setLanguageHolder (langHolder);
		map.setLanguageChangeMaster (langMaster);
		map.setClient (client);
		map.setClientConfig (config);
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
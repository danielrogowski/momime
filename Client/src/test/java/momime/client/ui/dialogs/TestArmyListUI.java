package momime.client.ui.dialogs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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
import momime.client.calculations.MiniMapBitmapGenerator;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.ArmyListScreen;
import momime.client.languages.database.Simple;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.renderer.ArmyListCellRenderer;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.OverlandMapSize;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Tests the ArmyListUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestArmyListUI extends ClientTestData
{
	/**
	 * Tests the ArmyListUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testArmyListUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the client database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitID ("UN001");
		unitDef.setUnitOverlandImageFile ("/momime.client.graphics/units/UN040/overland.png");
		when (db.findUnit (eq ("UN001"), anyString ())).thenReturn (unitDef);
		
		final List<UnitEx> unitDefs = new ArrayList<UnitEx> ();
		unitDefs.add (unitDef);
		doReturn (unitDefs).when (db).getUnits ();
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final ArmyListScreen armyListScreenLang = new ArmyListScreen ();
		armyListScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "The Armies of PLAYER_NAME"));
		armyListScreenLang.getHeroItems ().add (createLanguageText (Language.ENGLISH, "Items"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getArmyListScreen ()).thenReturn (armyListScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Session description
		final OverlandMapSize mapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (mapSize);

		// Player
		final PlayerPublicDetails ourPlayer = new PlayerPublicDetails (null, null, null);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1)).thenReturn (ourPlayer);
		
		// Player name
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (ourPlayer)).thenReturn ("Rjak");
		
		// Units
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);

		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		for (int x = 0; x <= 10; x++)
			for (int y = 0; y <= 10; y++)
				for (int z = 0; z <= mapSize.getDepth (); z++)
					for (int n = 0; n < x+y; n++)
					{
						final MemoryUnit thisUnit = new MemoryUnit ();
						thisUnit.setStatus (UnitStatusID.ALIVE);
						thisUnit.setOwningPlayerID (1);
						thisUnit.setUnitLocation (new MapCoordinates3DEx (x, y, z));
						thisUnit.setUnitID ("UN001");
						fow.getUnit ().add (thisUnit);
						
						final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
						when (expand.expandUnitDetails (thisUnit, null, null, null, players, fow, db)).thenReturn (xu);
					}
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPlayerID ()).thenReturn (1);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getSessionDescription ()).thenReturn (sd);
		when (client.getClientDB ()).thenReturn (db);

		// Image generator
		final PlayerColourImageGenerator generator = mock (PlayerColourImageGenerator.class);
		when (generator.getOverlandUnitImage (unitDef, 1, false)).thenReturn (utils.loadImage (unitDef.getUnitOverlandImageFile ()));
		
		// Renderer
		final ArmyListCellRenderer renderer = new ArmyListCellRenderer ();
		renderer.setUtils (utils);
		renderer.setClient (client);
		renderer.setPlayerColourImageGenerator (generator);
		
		// Mock the minimap bitmaps provided by the RHP
		final MiniMapBitmapGenerator gen = mock (MiniMapBitmapGenerator.class);
		when (gen.generateMiniMapBitmap (0)).thenReturn (createSolidImage (mapSize.getWidth (), mapSize.getHeight (), 0x004000));
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/ArmyListUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final ArmyListUI army = new ArmyListUI ();
		army.setArmyListLayout (layout);
		army.setUtils (utils);
		army.setExpandUnitDetails (expand);
		army.setLanguageHolder (langHolder);
		army.setLanguageChangeMaster (langMaster);
		army.setLargeFont (CreateFontsForTests.getLargeFont ());
		army.setMediumFont (CreateFontsForTests.getMediumFont ());
		army.setClient (client);
		army.setMultiplayerSessionUtils (multiplayerSessionUtils);
		army.setWizardClientUtils (wizardClientUtils);
		army.setMiniMapBitmapGenerator (gen);
		army.setArmyListCellRenderer (renderer);
		army.setOverlandMapProcessing (mock (OverlandMapProcessing.class));
		army.setOverlandMapUI (mock (OverlandMapUI.class));
		
		// Display form		
		army.setModal (false);
		army.setVisible (true);
		Thread.sleep (5000);
		army.setVisible (false);
	}
}
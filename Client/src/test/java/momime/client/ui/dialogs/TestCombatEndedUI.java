package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.messages.process.CombatEndedMessageImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the CombatEndedUI class
 */
public final class TestCombatEndedUI
{
	/**
	 * Tests the CombatEndedUI form
	 * @throws Exception If there is a problem
	 */
	private final void testCombatEndedUI (final int winningPlayerID) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmCombatEnded", "Defeat")).thenReturn ("You have been defeated");
		when (lang.findCategoryEntry ("frmCombatEnded", "Victory")).thenReturn ("You are triumphant");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPlayerID ()).thenReturn (1);
		
		// Overland map
		final CoordinateSystem sys = ClientTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = ClientTestData.createOverlandMap (sys);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (terrain);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Message
		final CombatEndedMessageImpl msg = new CombatEndedMessageImpl ();
		msg.setWinningPlayerID (winningPlayerID);
		msg.setCombatLocation (new MapCoordinates3DEx (20, 10, 0));

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/CombatEndedUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final CombatEndedUI combatEnded = new CombatEndedUI ();
		combatEnded.setUtils (utils);
		combatEnded.setClient (client);
		combatEnded.setMessage (msg);
		combatEnded.setLanguageHolder (langHolder);
		combatEnded.setLanguageChangeMaster (langMaster);
		combatEnded.setCombatEndedLayout (layout);
		combatEnded.setSmallFont (CreateFontsForTests.getSmallFont ());

		// Display form
		combatEnded.setModal (false);
		combatEnded.setVisible (true);
		Thread.sleep (5000);
	}

	/**
	 * Tests the CombatEndedUI form when we won
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEndedUI_Won () throws Exception
	{
		testCombatEndedUI (1);
	}
	
	/**
	 * Tests the CombatEndedUI form when we lost
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCombatEndedUI_Lost () throws Exception
	{
		testCombatEndedUI (2);
	}
}
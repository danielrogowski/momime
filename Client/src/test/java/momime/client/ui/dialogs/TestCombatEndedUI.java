package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.CombatEndedScreen;
import momime.client.messages.process.CombatEndedMessageImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Language;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;

/**
 * Tests the CombatEndedUI class
 */
public final class TestCombatEndedUI extends ClientTestData
{
	/**
	 * Tests the CombatEndedUI form
	 * 
	 * @param winningPlayerID The player who won the combat
	 * @throws Exception If there is a problem
	 */
	private final void testCombatEndedUI (final int winningPlayerID) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final CombatEndedScreen combatEndedScreenLang = new CombatEndedScreen ();
		combatEndedScreenLang.getDefeat ().add (createLanguageText (Language.ENGLISH, "You have been defeated"));
		combatEndedScreenLang.getVictory ().add (createLanguageText (Language.ENGLISH, "You are triumphant"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getCombatEndedScreen ()).thenReturn (combatEndedScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPlayerID ()).thenReturn (1);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = createOverlandMap (sys);
		
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
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/CombatEndedUI.xml"));
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
		combatEnded.setVisible (false);
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
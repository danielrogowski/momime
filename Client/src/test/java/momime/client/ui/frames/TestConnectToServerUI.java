package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.KnownServer;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the ConnectToServerUI class
 */
public final class TestConnectToServerUI
{
	/**
	 * Tests the ConnectToServerUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testConnectToServerUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmConnectToServer", "Title")).thenReturn ("Connect to Server");
		when (lang.findCategoryEntry ("frmConnectToServer", "PickFromList")).thenReturn ("Select a server from the list");
		when (lang.findCategoryEntry ("frmConnectToServer", "EnterServer")).thenReturn ("or enter a server IP address below");
		when (lang.findCategoryEntry ("frmConnectToServer", "PlayerName")).thenReturn ("Player name:");
		when (lang.findCategoryEntry ("frmConnectToServer", "Password")).thenReturn ("Password:");
		when (lang.findCategoryEntry ("frmConnectToServer", "CreateAccount")).thenReturn ("This is a new account");
		when (lang.findCategoryEntry ("frmConnectToServer", "KickExistingConnection")).thenReturn ("Kick existing connection using my account");
		when (lang.findCategoryEntry ("frmConnectToServer", "Cancel")).thenReturn ("Cancel");
		when (lang.findCategoryEntry ("frmConnectToServer", "OK")).thenReturn ("OK");
		
		final KnownServer localhost = new KnownServer ();
		localhost.setKnownServerDescription ("localhost");
		localhost.setKnownServerIP ("127.0.0.1");

		final KnownServer another = new KnownServer ();
		another.setKnownServerDescription ("Some other host");
		another.setKnownServerIP ("123.45.67.89");
		
		final List<KnownServer> servers = new ArrayList<KnownServer> ();
		servers.add (localhost);
		servers.add (another);
		when (lang.getKnownServer ()).thenReturn (servers);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Set up form
		final ConnectToServerUI connect = new ConnectToServerUI ();
		connect.setUtils (utils);
		connect.setLanguageHolder (langHolder);
		connect.setLanguageChangeMaster (langMaster);
		connect.setMainMenuUI (new MainMenuUI ());
		connect.setSmallFont (CreateFontsForTests.getSmallFont ());
		connect.setMediumFont (CreateFontsForTests.getMediumFont ());
		connect.setLargeFont (CreateFontsForTests.getLargeFont ());
	
		// Display form		
		connect.setVisible (true);
		Thread.sleep (5000);
	}
}
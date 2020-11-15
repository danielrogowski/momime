package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.ConnectToServerScreen;
import momime.client.languages.database.KnownServer;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Language;

/**
 * Tests the ConnectToServerUI class
 */
public final class TestConnectToServerUI extends ClientTestData
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
		final Simple simpleLang = new Simple ();
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final ConnectToServerScreen connectToServerScreenLang = new ConnectToServerScreen ();
		connectToServerScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Connect to Server"));
		connectToServerScreenLang.getPickFromList ().add (createLanguageText (Language.ENGLISH, "Select a server from the list"));
		connectToServerScreenLang.getEnterServer ().add (createLanguageText (Language.ENGLISH, "or enter a server IP address below"));
		connectToServerScreenLang.getPlayerName ().add (createLanguageText (Language.ENGLISH, "Player name:"));
		connectToServerScreenLang.getPassword ().add (createLanguageText (Language.ENGLISH, "Password:"));
		connectToServerScreenLang.getCreateAccount ().add (createLanguageText (Language.ENGLISH, "This is a new account"));
		connectToServerScreenLang.getKickExistingConnection ().add (createLanguageText (Language.ENGLISH, "Kick existing connection using my account"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getConnectToServerScreen ()).thenReturn (connectToServerScreenLang);
		
		final KnownServer localhost = new KnownServer ();
		localhost.setKnownServerID ("SRV01");
		localhost.setKnownServerIP ("127.0.0.1");
		localhost.getKnownServerDescription ().add (createLanguageText (Language.ENGLISH, "Local PC"));
		when (lang.findKnownServer (localhost.getKnownServerID ())).thenReturn (localhost);

		final KnownServer another = new KnownServer ();
		another.setKnownServerID ("SRV02");
		another.setKnownServerIP ("123.45.67.89");
		another.getKnownServerDescription ().add (createLanguageText (Language.ENGLISH, "Some other server"));
		when (lang.findKnownServer (another.getKnownServerID ())).thenReturn (another);
		
		final List<KnownServer> servers = new ArrayList<KnownServer> ();
		servers.add (localhost);
		servers.add (another);
		when (lang.getKnownServer ()).thenReturn (servers);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Set up form
		final ConnectToServerUI connect = new ConnectToServerUI ();
		connect.setUtils (utils);
		connect.setLanguageHolder (langHolder);
		connect.setLanguageChangeMaster (langMaster);
		connect.setSmallFont (CreateFontsForTests.getSmallFont ());
		connect.setMediumFont (CreateFontsForTests.getMediumFont ());
		connect.setLargeFont (CreateFontsForTests.getLargeFont ());
	
		// Display form		
		connect.setVisible (true);
		Thread.sleep (5000);
		connect.setVisible (false);
	}
}
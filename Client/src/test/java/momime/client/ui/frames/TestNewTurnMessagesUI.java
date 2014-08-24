package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the NewTurnMessagesUI class
 */
public final class TestNewTurnMessagesUI
{
	/**
	 * Tests the NewTurnMessagesUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testNewTurnMessagesUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Set up form
		final NewTurnMessagesUI scroll = new NewTurnMessagesUI ();
		scroll.setUtils (utils);
		scroll.setLanguageHolder (langHolder);
		scroll.setLanguageChangeMaster (langMaster);
		
		// Display form		
		scroll.setVisible (true);
		Thread.sleep (50000);
	}
}
package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;

import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the ChooseLanguageUI class
 */
public final class TestChooseLanguageUI
{
	/**
	 * Tests the ChooseLanguageUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseLanguageUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmChooseLanguage", "Title")).thenReturn ("Choose Language");
		when (lang.findCategoryEntry ("frmChooseLanguage", "Cancel")).thenReturn ("Cancel");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Find the test folder containing empty language files
		String path = getClass ().getResource ("/momime.client.language/Lang 1.Master of Magic Language.xml").toString ();
		path = URLDecoder.decode (path, "UTF-8").substring (6);
		path = path.substring (0, path.length () - 35);
		
		// Set up form
		final ChooseLanguageUI choose = new ChooseLanguageUI ();
		choose.setUtils (utils);
		choose.setLanguageHolder (langHolder);
		choose.setLanguageChangeMaster (langMaster);
		choose.setPathToLanguageXmlFiles (path);
		choose.setSmallFont (CreateFontsForTests.getSmallFont ());
		choose.setLargeFont (CreateFontsForTests.getLargeFont ());

		// Display form		
		choose.setVisible (true);
		Thread.sleep (5000);
	}
}
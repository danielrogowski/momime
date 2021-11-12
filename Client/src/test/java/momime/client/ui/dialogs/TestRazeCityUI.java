package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.RazeCityScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Language;

/**
 * Tests the RazeCityUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestRazeCityUI extends ClientTestData
{
	/**
	 * Tests the RazeCityUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testRazeCityUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getNo ().add (createLanguageText (Language.ENGLISH, "No"));
		
		final RazeCityScreen razeCityScreenLang = new RazeCityScreen ();
		razeCityScreenLang.getRaze ().add (createLanguageText (Language.ENGLISH, "Raze"));
		razeCityScreenLang.getText ().add (createLanguageText (Language.ENGLISH, "Do you wish to completely destroy this city?"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getRazeCityScreen ()).thenReturn (razeCityScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/RazeCityUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final RazeCityUI box = new RazeCityUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		box.setRazeCityLayout (layout);
		
		// Display form
		box.setModal (false);
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}
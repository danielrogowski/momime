package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the RazeCityUI class
 */
public final class TestRazeCityUI
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmRazeCity", "Raze")).thenReturn ("Raze");
		when (lang.findCategoryEntry ("frmRazeCity", "No")).thenReturn ("No");
		when (lang.findCategoryEntry ("frmRazeCity", "Text")).thenReturn ("Do you wish to completely destroy this city?");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/RazeCityUI.xml"));
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
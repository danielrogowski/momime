package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the ScheduledCombatsUI class
 */
public final class TestScheduledCombatsUI
{
	/**
	 * Tests the ScheduledCombatsUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testScheduledCombatsUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("ScheduledCombats", "Title")).thenReturn ("Combats");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewTurnMessagesUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final ScheduledCombatsUI combats = new ScheduledCombatsUI ();
		combats.setNewTurnMessagesLayout (layout);
		combats.setUtils (utils);
		combats.setLanguageHolder (langHolder);
		combats.setLanguageChangeMaster (langMaster);

		// Display form		
		combats.setVisible (true);
		Thread.sleep (5000);
		combats.setVisible (false);
	}
}
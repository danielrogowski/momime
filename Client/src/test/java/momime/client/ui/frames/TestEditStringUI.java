package momime.client.ui.frames;

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
 * Tests the EditStringUI class
 */
public final class TestEditStringUI extends ClientTestData
{
	/**
	 * Tests the EditStringUI form, with fixed text
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEditStringUI_FixedText () throws Exception
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
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/EditStringUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final EditStringUI box = new EditStringUI ();
		box.setEditStringLayout (layout);
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitle ("Edit box test using fixed text");
		box.setPrompt ("Fixed text edit prompt");
		box.setText ("Default value");
		box.setLargeFont (CreateFontsForTests.getLargeFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}

	/**
	 * Tests the EditStringUI form, with text read from the language XML
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEditStringUI_VariableText () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("TitleCat", "TitleEntry")).thenReturn ("Edit box test using variable text");
		when (lang.findCategoryEntry ("PromptCat", "PromptEntry")).thenReturn ("Variable text edit prompt");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/EditStringUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final EditStringUI box = new EditStringUI ();
		box.setEditStringLayout (layout);
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitleLanguageCategoryID ("TitleCat");
		box.setTitleLanguageEntryID ("TitleEntry");
		box.setPromptLanguageCategoryID ("PromptCat");
		box.setPromptLanguageEntryID ("PromptEntry");
		box.setText ("Default value");
		box.setLargeFont (CreateFontsForTests.getLargeFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}
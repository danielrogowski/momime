package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.messages.v0_9_5.MemoryUnit;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the MessageBoxUI class
 */
public final class TestMessageBoxUI
{
	/**
	 * Tests the MessageBoxUI form, with fixed text
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMessageBoxUI_FixedText () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmMessageBox", "OK")).thenReturn ("OK");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Set up form
		final MessageBoxUI box = new MessageBoxUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitle ("Message box test using fixed text");
		box.setText ("Here's some fixed text for the message box which is long enough to have to split over a couple of lines.");
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
	}

	/**
	 * Tests the MessageBoxUI form, with text read from the language XML
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMessageBoxUI_VariableText () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmMessageBox", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("TitleCat", "TitleEntry")).thenReturn ("Message box test using variable text");
		when (lang.findCategoryEntry ("TextCat", "TextEntry")).thenReturn ("Here's some variable text for the message box which is long enough to have to split over a couple of lines.");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Set up form
		final MessageBoxUI box = new MessageBoxUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitleLanguageCategoryID ("TitleCat");
		box.setTitleLanguageEntryID ("TitleEntry");
		box.setTextLanguageCategoryID ("TextCat");
		box.setTextLanguageEntryID ("TextEntry");
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
	}

	/**
	 * Tests the MessageBoxUI form, with long enough text that it needs a scroll bar
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMessageBoxUI_ScrollBar () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmMessageBox", "OK")).thenReturn ("OK");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Set up form
		final MessageBoxUI box = new MessageBoxUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitle ("Message box test using fixed text");
		box.setText ("Here's some fixed text\r\nthat includes\r\na bunch of carriage returns\r\nof lines to\r\nmake sure that\r\nwe need to display\r\na scroll bar\r\non the message box.");
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
	}

	/**
	 * Tests the MessageBoxUI form, with no/yes buttons rather than just OK
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMessageBoxUI_NoYes () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmMessageBox", "No")).thenReturn ("No");
		when (lang.findCategoryEntry ("frmMessageBox", "Yes")).thenReturn ("Yes");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Variable replacer
		final String title = "Message box test with no and yes buttons";
		final String text = "Here's some fixed text for the message box with no and yes buttons, which is long enough to have to split over a couple of lines.";

		final UnitStatsLanguageVariableReplacer replacer = mock (UnitStatsLanguageVariableReplacer.class);
		when (replacer.replaceVariables (title)).thenReturn (title);
		when (replacer.replaceVariables (text)).thenReturn (text);
		
		// Set up form
		final MessageBoxUI box = new MessageBoxUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitle (title);
		box.setText (text);
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		box.setUnitToDismiss (new MemoryUnit ());
		box.setUnitStatsReplacer (replacer);
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
	}
}
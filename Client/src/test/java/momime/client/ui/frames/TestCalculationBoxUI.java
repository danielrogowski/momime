package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the CalculationBoxUI class
 */
public final class TestCalculationBoxUI
{
	/**
	 * Tests the CalculationBoxUI form, with fixed text
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculationBoxUI_FixedText () throws Exception
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
		final CalculationBoxUI box = new CalculationBoxUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitle ("Message box test using fixed text");
		box.setText ("Here's some fixed text for the message box which is long enough to have to split over a couple of lines.");
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}

	/**
	 * Tests the CalculationBoxUI form, with text read from the language XML
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculationBoxUI_VariableText () throws Exception
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
		final CalculationBoxUI box = new CalculationBoxUI ();
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
		box.setVisible (false);
	}

	/**
	 * Tests the CalculationBoxUI form, with long enough text that it needs a scroll bar
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculationBoxUI_ScrollBar () throws Exception
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
		final CalculationBoxUI box = new CalculationBoxUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setTitle ("Message box test using fixed text");
		
		box.setText ("Here's some" + System.lineSeparator () + "fixed text" + System.lineSeparator () + "that includes" +
			System.lineSeparator () + "a bunch of" + System.lineSeparator () + "carriage returns" + System.lineSeparator () + "of lines to" +
			System.lineSeparator () + "make sure that" + System.lineSeparator () + "we need" + System.lineSeparator () + "to display" +
			System.lineSeparator () + "a scroll bar" + System.lineSeparator () + "on the" + System.lineSeparator () + "message box" +
			System.lineSeparator () + "blah blah" + System.lineSeparator () + "another line" + System.lineSeparator () + "more blah.");
		
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}
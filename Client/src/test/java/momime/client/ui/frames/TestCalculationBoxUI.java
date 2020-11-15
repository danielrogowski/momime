package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.Simple;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Language;
import momime.common.database.LanguageText;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the CalculationBoxUI class
 */
public final class TestCalculationBoxUI extends ClientTestData
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
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CalculationBoxUI.xml"));
		layout.buildMaps ();

		// Set up form
		final CalculationBoxUI box = new CalculationBoxUI ();
		box.setCalculationBoxLayout (layout);
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
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		final List<LanguageText> title = Arrays.asList (createLanguageText (Language.ENGLISH, "Message box test using variable text"));
		final List<LanguageText> text = Arrays.asList (createLanguageText (Language.ENGLISH,
			"Here's some variable text for the message box which is long enough to have to split over a couple of lines."));
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CalculationBoxUI.xml"));
		layout.buildMaps ();

		// Set up form
		final CalculationBoxUI box = new CalculationBoxUI ();
		box.setCalculationBoxLayout (layout);
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setLanguageTitle (title);
		box.setLanguageText (text);
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
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CalculationBoxUI.xml"));
		layout.buildMaps ();

		// Set up form
		final CalculationBoxUI box = new CalculationBoxUI ();
		box.setCalculationBoxLayout (layout);
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
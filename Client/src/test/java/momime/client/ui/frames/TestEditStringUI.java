package momime.client.ui.frames;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

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
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Language;
import momime.common.database.LanguageText;

/**
 * Tests the EditStringUI class
 */
@ExtendWith(MockitoExtension.class)
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
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
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
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		final List<LanguageText> title = Arrays.asList (createLanguageText (Language.ENGLISH, "Edit box test using variable text"));
		final List<LanguageText> prompt = Arrays.asList (createLanguageText (Language.ENGLISH, "Variable text edit prompt"));
		
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
		box.setLanguageTitle (title);
		box.setLanguagePrompt (prompt);
		box.setText ("Default value");
		box.setLargeFont (CreateFontsForTests.getLargeFont ());
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}
package momime.client.ui.frames;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;

import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;

/**
 * Tests the SpellBookNewUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellBookNewUI
{
	/**
	 * Tests the SpellBookNewUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellBookNewUI () throws Exception
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
		
		// Set up form
		final SpellBookNewUI book = new SpellBookNewUI ();
		book.setUtils (utils);
		book.setLanguageHolder (langHolder);
		book.setLanguageChangeMaster (langMaster);

		// Display form		
		book.setVisible (true);
		Thread.sleep (50000);
		book.setVisible (false);
	}	
}
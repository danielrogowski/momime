package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.AnimationFrame;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the SpellBookUI class
 */
public final class TestSpellBookUI
{
	/**
	 * Tests the SpellBookUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellBookUI () throws Exception
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
		
		// Mock entries from the graphics XML
		final AnimationEx pageTurn = new AnimationEx ();
		pageTurn.setAnimationSpeed (5);
		for (int n = 0; n < 4; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setFrameImageFile ("/momime.client.graphics/ui/spellBook/spellBookAnim-frame" + n + ".png");
			
			pageTurn.getFrame ().add (frame);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (SpellBookUI.ANIM_PAGE_TURN, "SpellBookUI")).thenReturn (pageTurn);
		
		// Set up form
		final SpellBookUI book = new SpellBookUI ();
		book.setUtils (utils);
		book.setLanguageHolder (langHolder);
		book.setLanguageChangeMaster (langMaster);
		book.setGraphicsDB (gfx);
		book.setSmallFont (CreateFontsForTests.getSmallFont ());
		book.setMediumFont (CreateFontsForTests.getMediumFont ());
		book.setLargeFont (CreateFontsForTests.getLargeFont ());

		// Display form		
		book.setVisible (true);
		Thread.sleep (50000);
	}	
}
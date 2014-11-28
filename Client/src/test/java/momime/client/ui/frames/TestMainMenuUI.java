package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.AnimationControllerImpl;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the MainMenuUI class
 */
public final class TestMainMenuUI
{
	/**
	 * Tests the MainMenuUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMainMenuUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmMainMenu", "LongTitle")).thenReturn ("Master of Magic - Implode's Multiplayer Edition - Client vVERSION");
		when (lang.findCategoryEntry ("frmMainMenu", "ShortTitle")).thenReturn ("Implode's Multiplayer Edition - Client");
		when (lang.findCategoryEntry ("frmMainMenu", "Version")).thenReturn ("version VERSION");
		when (lang.findCategoryEntry ("frmMainMenu", "OriginalCopyrightLine1")).thenReturn ("Original Master of Magic is Copyright");
		when (lang.findCategoryEntry ("frmMainMenu", "OriginalCopyrightLine2")).thenReturn ("Simtex Software and Microprose");

		when (lang.findCategoryEntry ("frmMainMenu", "ChangeLanguage")).thenReturn ("Change Language");
		when (lang.findCategoryEntry ("frmMainMenu", "ConnectToServer")).thenReturn ("Connect to Server");
		when (lang.findCategoryEntry ("frmMainMenu", "NewGame")).thenReturn ("New Game");
		when (lang.findCategoryEntry ("frmMainMenu", "JoinGame")).thenReturn ("Join Game");
		when (lang.findCategoryEntry ("frmMainMenu", "Options")).thenReturn ("Options");
		when (lang.findCategoryEntry ("frmMainMenu", "Exit")).thenReturn ("Exit");
		when (lang.findCategoryEntry ("frmMainMenu", "LanguageFileAuthor")).thenReturn ("Language file written by Blah");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from the graphics XML
		final AnimationEx title = new AnimationEx ();
		title.setAnimationSpeed (8);
		for (int n = 1; n <= 20; n++)
			title.getFrame ().add ("/momime.client.graphics/ui/mainMenu/title-frame" + ((n < 10) ? "0" : "") + n + ".png");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (MainMenuUI.ANIM_MAIN_MENU_TITLE, "registerRepaintTrigger")).thenReturn (title);
		
		// Set up animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Set up form
		final MomClient client = mock (MomClient.class);
		
		final MainMenuUI main = new MainMenuUI ();
		main.setUtils (utils);
		main.setLanguageHolder (langHolder);
		main.setLanguageChangeMaster (langMaster);
		main.setClient (client);
		main.setVersion ("9.9.9");
		main.setMusicPlayer (mock (AudioPlayer.class));
		main.setLargeFont (CreateFontsForTests.getLargeFont ());
		main.setMediumFont (CreateFontsForTests.getMediumFont ());
		main.setAnim (anim);

		// Display form		
		main.setVisible (true);
		Thread.sleep (5000);
		main.setVisible (false);
	}
}
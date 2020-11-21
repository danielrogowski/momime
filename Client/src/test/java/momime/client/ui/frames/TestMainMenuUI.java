package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.MainMenuScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.AnimationControllerImpl;
import momime.common.database.AnimationEx;
import momime.common.database.Language;

/**
 * Tests the MainMenuUI class
 */
public final class TestMainMenuUI extends ClientTestData
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
		final MainMenuScreen mainMenuScreenLang = new MainMenuScreen ();
		mainMenuScreenLang.getLongTitle ().add (createLanguageText (Language.ENGLISH, "Master of Magic - Implode's Multiplayer Edition - Client vVERSION"));
		mainMenuScreenLang.getShortTitle ().add (createLanguageText (Language.ENGLISH, "Implode's Multiplayer Edition - Client"));
		mainMenuScreenLang.getVersion ().add (createLanguageText (Language.ENGLISH, "version VERSION"));
		mainMenuScreenLang.getOriginalCopyrightLine1 ().add (createLanguageText (Language.ENGLISH, "Original Master of Magic is Copyright"));
		mainMenuScreenLang.getOriginalCopyrightLine2 ().add (createLanguageText (Language.ENGLISH, "Simtex Software and Microprose"));
		
		mainMenuScreenLang.getConnectToServer ().add (createLanguageText (Language.ENGLISH, "Connect to Server"));
		mainMenuScreenLang.getNewGame ().add (createLanguageText (Language.ENGLISH, "New Game"));
		mainMenuScreenLang.getJoinGame ().add (createLanguageText (Language.ENGLISH, "Join Game"));
		mainMenuScreenLang.getLoadGame ().add (createLanguageText (Language.ENGLISH, "Load Game"));
		mainMenuScreenLang.getOptions ().add (createLanguageText (Language.ENGLISH, "Options"));
		mainMenuScreenLang.getExit ().add (createLanguageText (Language.ENGLISH, "Exit"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getMainMenuScreen ()).thenReturn (mainMenuScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

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
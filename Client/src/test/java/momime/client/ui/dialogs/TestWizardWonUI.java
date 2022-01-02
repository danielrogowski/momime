package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.WizardWonScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.KnownWizardDetails;

/**
 * Tests the WizardWonUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestWizardWonUI extends ClientTestData
{
	/**
	 * Tests the WizardWonUI form
	 * 
	 * @param standardPhotoID Photo ID of the wizard who won
	 * @throws Exception If there is a problem
	 */
	private final void testWizardWonUI (final String standardPhotoID) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Client
		final MomClient client = mock (MomClient.class);
		if (standardPhotoID != null)
			when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);

		if (standardPhotoID != null)
		{
			final WizardEx winningWizardDef = new WizardEx ();
			winningWizardDef.setWorldHandsImageFile ("/momime.client.graphics/animations/worlds/hands-male-mid.png");
			winningWizardDef.setTalkingAnimation ("WIZARD_TALKING_01");
			when (db.findWizard ("WZ01", "WizardWonUI")).thenReturn (winningWizardDef);
		}
		
		// Animations
		final AnimationEx worldsAnim = new AnimationEx ();
		for (int n = 1; n <= 67; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 2)
				s = "0" + s;
			
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/animations/worlds/worlds-frame-" + s + ".png");
			worldsAnim.getFrame ().add (frame);
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_WON_WORLDS, "WizardWonUI (W)")).thenReturn (worldsAnim);

		final AnimationEx sparklesAnim = new AnimationEx ();
		for (int n = 1; n <= 25; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 2)
				s = "0" + s;
			
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/animations/worlds/sparkles-frame-" + s + ".png");
			sparklesAnim.getFrame ().add (frame);
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_WON_SPARKLES, "WizardWonUI (S)")).thenReturn (sparklesAnim);

		if (standardPhotoID != null)
		{
			final AnimationEx talkingAnim = new AnimationEx ();
			for (int n = 1; n <= 20; n++)
			{
				String s = Integer.valueOf (n).toString ();
				while (s.length () < 2)
					s = "0" + s;
				
				final AnimationFrame frame = new AnimationFrame ();
				frame.setImageFile ("/momime.client.graphics/wizards/WZ01-talk-frame-" + s + ".png");
				talkingAnim.getFrame ().add (frame);
			}
			
			when (db.findAnimation ("WIZARD_TALKING_01", "WizardWonUI (T)")).thenReturn (talkingAnim);
		}
		
		// Mock entries from the language XML
		final WizardWonScreen wizardWonScreenLang = new WizardWonScreen ();
		wizardWonScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "You have won"));
		wizardWonScreenLang.getLine1 ().add (createLanguageText (Language.ENGLISH, "Having conquered both the"));
		wizardWonScreenLang.getLine2 ().add (createLanguageText (Language.ENGLISH, "worlds of Arcanus and Myrror"));
		wizardWonScreenLang.getLine3 ().add (createLanguageText (Language.ENGLISH, "I and only I remain the one"));
		wizardWonScreenLang.getLine4 ().add (createLanguageText (Language.ENGLISH, "and true Master of Magic"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getWizardWonScreen ()).thenReturn (wizardWonScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Players
		final KnownWizardDetails winningWizard = new KnownWizardDetails ();
		winningWizard.setStandardPhotoID (standardPhotoID);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/WizardWonUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final WizardWonUI wizardWon = new WizardWonUI ();
		wizardWon.setWizardWonLayout (layout);
		wizardWon.setClient (client);
		wizardWon.setGraphicsDB (gfx);
		wizardWon.setUtils (utils);
		wizardWon.setLanguageHolder (langHolder);
		wizardWon.setLanguageChangeMaster (langMaster);
		wizardWon.setLargeFont (CreateFontsForTests.getLargeFont ());
		wizardWon.setWinningWizard (winningWizard);
		wizardWon.setMusicPlayer (mock (AudioPlayer.class));
		
		// Display form		
		wizardWon.setModal (false);
		wizardWon.setVisible (true);
		Thread.sleep (20000);
		wizardWon.setVisible (false);
	}

	/**
	 * Tests the WizardWonUI form with a wizard with a standard photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWizardWonUI_StandardPhoto () throws Exception
	{
		testWizardWonUI ("WZ01");
	}

	/**
	 * Tests the WizardWonUI form with a wizard with a custom photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWizardWonUI_CustomPhoto () throws Exception
	{
		testWizardWonUI (null);
	}
}
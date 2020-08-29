package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.WizardGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Tests the WizardWonUI class
 */
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
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);

		final WizardGfx winningWizardGfx = new WizardGfx ();
		winningWizardGfx.setWorldHandsImageFile ("/momime.client.graphics/animations/worlds/hands-male-mid.png");
		winningWizardGfx.setTalkingAnimation ("WIZARD_TALKING_01");
		when (gfx.findWizard ("WZ01", "WizardWonUI")).thenReturn (winningWizardGfx);
		
		// Animations
		final AnimationGfx worldsAnim = new AnimationGfx ();
		for (int n = 1; n <= 67; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 2)
				s = "0" + s;
			
			worldsAnim.getFrame ().add ("/momime.client.graphics/animations/worlds/worlds-frame-" + s + ".png");
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_WON_WORLDS, "WizardWonUI (W)")).thenReturn (worldsAnim);

		final AnimationGfx sparklesAnim = new AnimationGfx ();
		for (int n = 1; n <= 25; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 2)
				s = "0" + s;
			
			sparklesAnim.getFrame ().add ("/momime.client.graphics/animations/worlds/sparkles-frame-" + s + ".png");
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_WON_SPARKLES, "WizardWonUI (S)")).thenReturn (sparklesAnim);

		final AnimationGfx talkingAnim = new AnimationGfx ();
		for (int n = 1; n <= 20; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 2)
				s = "0" + s;
			
			talkingAnim.getFrame ().add ("/momime.client.graphics/wizards/WZ01-talk-frame-" + s + ".png");
		}
		
		when (gfx.findAnimation ("WIZARD_TALKING_01", "WizardWonUI (T)")).thenReturn (talkingAnim);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmWizardWon", "Title")).thenReturn ("You have won");
		when (lang.findCategoryEntry ("frmWizardWon", "Line1")).thenReturn ("Having conquered both the");
		when (lang.findCategoryEntry ("frmWizardWon", "Line2")).thenReturn ("worlds of Arcanus and Myrror");
		when (lang.findCategoryEntry ("frmWizardWon", "Line3")).thenReturn ("I and only I remain the one");
		when (lang.findCategoryEntry ("frmWizardWon", "Line4")).thenReturn ("and true Master of Magic");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Players
		final MomPersistentPlayerPublicKnowledge winningWizardPub = new MomPersistentPlayerPublicKnowledge ();
		winningWizardPub.setStandardPhotoID (standardPhotoID);
		
		final PlayerPublicDetails winningWizard = new PlayerPublicDetails (null, winningWizardPub, null);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/WizardWonUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final WizardWonUI wizardWon = new WizardWonUI ();
		wizardWon.setWizardWonLayout (layout);
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
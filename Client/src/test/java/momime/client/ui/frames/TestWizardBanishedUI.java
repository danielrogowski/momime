package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.swing.Timer;

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
import momime.client.utils.WizardClientUtils;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Tests the WizardBanishedUI class
 */
public final class TestWizardBanishedUI extends ClientTestData
{
	/** Used to mock the animation */
	private int tickNumber;

	/** Swing timer to display the animation */
	private Timer swingTimer;
	
	/**
	 * Tests the WizardBanishedUI form
	 * 
	 * @param isEnemyWizard Whether banished by an enemy wizard, or raiders
	 * @throws Exception If there is a problem
	 */
	private final void testWizardBanishedUI (final boolean isEnemyWizard) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);

		final WizardGfx banishedWizardGfx = new WizardGfx ();
		banishedWizardGfx.setStandingImageFile ("/momime.client.graphics/wizards/WZ01-standing.png");
		banishedWizardGfx.setEvaporatingAnimation ("WIZARD_EVAPORATING_01");
		when (gfx.findWizard ("WZ01", "WizardBanishedUI (A)")).thenReturn (banishedWizardGfx);

		final WizardGfx banishingWizardGfx = new WizardGfx ();
		banishingWizardGfx.setBanishingImageFile ("/momime.client.graphics/wizards/WZ03-banishing.png");
		banishingWizardGfx.setBanishingHandImageFile ("/momime.client.graphics/wizards/WZ03-banishing-hand.png");
		when (gfx.findWizard ("WZ03", "WizardBanishedUI (B)")).thenReturn (banishingWizardGfx);
		
		// Animations
		final AnimationGfx singleBlastAnim = new AnimationGfx ();
		for (int n = 1; n <= 4; n++)
			singleBlastAnim.getFrame ().add ("/momime.client.graphics/animations/wizardsLab/singleBlast-frame" + n + ".png");
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_SINGLE_BLAST, "WizardBanishedUI (C)")).thenReturn (singleBlastAnim);

		final AnimationGfx doubleBlastAnim = new AnimationGfx ();
		for (int n = 1; n <= 8; n++)
			doubleBlastAnim.getFrame ().add ("/momime.client.graphics/animations/wizardsLab/doubleBlast-frame" + n + ".png");
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_DOUBLE_BLAST, "WizardBanishedUI (D)")).thenReturn (doubleBlastAnim);

		final AnimationGfx evaporatingAnim = new AnimationGfx ();
		for (int n = 1; n <= 4; n++)
			evaporatingAnim.getFrame ().add ("/momime.client.graphics/wizards/WZ01-evaporating-frame" + n + ".png");
		
		when (gfx.findAnimation ("WIZARD_EVAPORATING_01", "WizardBanishedUI (E)")).thenReturn (evaporatingAnim);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmWizardBanished", "BanishedByWizard")).thenReturn ("BANISHING_WIZARD banishes BANISHED_WIZARD");
		when (lang.findCategoryEntry ("frmWizardBanished", "BanishedByRaiders")).thenReturn ("BANISHING_WIZARD banish BANISHED_WIZARD");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Players
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final MomPersistentPlayerPublicKnowledge banishedWizardPub = new MomPersistentPlayerPublicKnowledge ();
		banishedWizardPub.setStandardPhotoID ("WZ01");
		
		final PlayerPublicDetails banishedWizard = new PlayerPublicDetails (null, banishedWizardPub, null);
		when (wizardClientUtils.getPlayerName (banishedWizard)).thenReturn ("Merlin");

		final MomPersistentPlayerPublicKnowledge banishingWizardPub = new MomPersistentPlayerPublicKnowledge ();
		if (isEnemyWizard)
			banishingWizardPub.setStandardPhotoID ("WZ03");
		
		final PlayerPublicDetails banishingWizard = new PlayerPublicDetails (null, banishingWizardPub, null);
		when (wizardClientUtils.getPlayerName (banishingWizard)).thenReturn (isEnemyWizard ? "Kali" : "Raiders");
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/WizardBanishedUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final WizardBanishedUI wizardBanished = new WizardBanishedUI ();
		wizardBanished.setWizardBanishedLayout (layout);
		wizardBanished.setGraphicsDB (gfx);
		wizardBanished.setUtils (utils);
		wizardBanished.setWizardClientUtils (wizardClientUtils);
		wizardBanished.setLanguageHolder (langHolder);
		wizardBanished.setLanguageChangeMaster (langMaster);
		wizardBanished.setLargeFont (CreateFontsForTests.getLargeFont ());		
		wizardBanished.setBanishedWizard (banishedWizard);
		wizardBanished.setBanishingWizard (banishingWizard);
		wizardBanished.setMusicPlayer (mock (AudioPlayer.class));
		wizardBanished.setSoundPlayer (mock (AudioPlayer.class));
		
		// Display form		
		wizardBanished.setVisible (true);
		
		// Mock the animation
		tickNumber = 0;
		
		// I think should create a javax.swing.Timer if processMessagesOnSwingEventDispatcherThread is true and
		// a java.util.Timer if processMessagesOnSwingEventDispatcherThread is false, but since every app I'm
		// using this for at the moment is a Swing app, am not bothering to implement that yet
		swingTimer = new Timer ((int) (1000.0 * wizardBanished.getDuration () / wizardBanished.getTickCount ()), (ev) ->
		{
			tickNumber++;
			wizardBanished.tick (tickNumber);
			if (tickNumber >= wizardBanished.getTickCount ())
				swingTimer.stop ();
		});
		swingTimer.start ();
		
		// Wait for the anim to finish before allowing the test to finish
		while (swingTimer.isRunning ())
			Thread.sleep (1000);
		
		wizardBanished.setVisible (false);
	}

	/**
	 * Tests the WizardBanishedUI form, being banished by an enemy wizard
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWizardBanishedUI_Wizard () throws Exception
	{
		testWizardBanishedUI (true);
	}

	/**
	 * Tests the WizardBanishedUI form, being banished by raiders
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWizardBanishedUI_Raiders () throws Exception
	{
		testWizardBanishedUI (false);
	}
}
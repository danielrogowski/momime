package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.PlayerPublicDetails;
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
import momime.client.languages.database.WizardBanishedScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Tests the WizardBanishedUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestWizardBanishedUI extends ClientTestData
{
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
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final WizardEx banishedWizardDef = new WizardEx ();
		banishedWizardDef.setStandingImageFile ("/momime.client.graphics/wizards/WZ01-standing.png");
		banishedWizardDef.setEvaporatingAnimation ("WIZARD_EVAPORATING_01");
		when (db.findWizard ("WZ01", "WizardBanishedUI (A)")).thenReturn (banishedWizardDef);

		if (isEnemyWizard)
		{
			final WizardEx banishingWizardDef = new WizardEx ();
			banishingWizardDef.setBanishingImageFile ("/momime.client.graphics/wizards/WZ03-banishing.png");
			banishingWizardDef.setBanishingHandImageFile ("/momime.client.graphics/wizards/WZ03-banishing-hand.png");
			when (db.findWizard ("WZ03", "WizardBanishedUI (B)")).thenReturn (banishingWizardDef);
		}
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);

		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);

		// Animations
		final AnimationEx singleBlastAnim = new AnimationEx ();
		for (int n = 1; n <= 4; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/animations/wizardsLab/singleBlast-frame" + n + ".png");
			singleBlastAnim.getFrame ().add (frame);
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_SINGLE_BLAST, "WizardBanishedUI (C)")).thenReturn (singleBlastAnim);

		final AnimationEx doubleBlastAnim = new AnimationEx ();
		for (int n = 1; n <= 8; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/animations/wizardsLab/doubleBlast-frame" + n + ".png");
			doubleBlastAnim.getFrame ().add (frame);
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_WIZARD_BANISHED_DOUBLE_BLAST, "WizardBanishedUI (D)")).thenReturn (doubleBlastAnim);

		final AnimationEx evaporatingAnim = new AnimationEx ();
		for (int n = 1; n <= 4; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/wizards/WZ01-evaporating-frame" + n + ".png");
			evaporatingAnim.getFrame ().add (frame);
		}
		
		when (db.findAnimation ("WIZARD_EVAPORATING_01", "WizardBanishedUI (E)")).thenReturn (evaporatingAnim);
		
		// Mock entries from the language XML
		final WizardBanishedScreen wizardBanishedScreenLang = new WizardBanishedScreen ();
		wizardBanishedScreenLang.getBanishedByWizard ().add (createLanguageText (Language.ENGLISH, "BANISHING_WIZARD banishes BANISHED_WIZARD"));
		wizardBanishedScreenLang.getBanishedByRaiders ().add (createLanguageText (Language.ENGLISH, "BANISHING_WIZARD banish BANISHED_WIZARD"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getWizardBanishedScreen ()).thenReturn (wizardBanishedScreenLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
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
		wizardBanished.setClient (client);
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
		wizardBanished.setModal (false);
		wizardBanished.setVisible (true);
		Thread.sleep (8000);
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
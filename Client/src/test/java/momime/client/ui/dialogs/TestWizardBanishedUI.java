package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

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
import momime.common.messages.KnownWizardDetails;
import momime.common.utils.PlayerKnowledgeUtils;

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
	 * @param isEnemyAtAll Whether banished by anyone at all, or by their own stupid mistake
	 * @throws Exception If there is a problem
	 */
	private final void testWizardBanishedUI (final boolean isEnemyWizard, final boolean isEnemyAtAll) throws Exception
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
		
		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		if (isEnemyAtAll)
			when (playerKnowledgeUtils.isWizard ("WZ03")).thenReturn (isEnemyWizard);
		
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
		wizardBanishedScreenLang.getBanishedByNobody ().add (createLanguageText (Language.ENGLISH, "BANISHED_WIZARD is banished"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getWizardBanishedScreen ()).thenReturn (wizardBanishedScreenLang);

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Players
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final KnownWizardDetails banishedWizardDetails = new KnownWizardDetails ();
		banishedWizardDetails.setWizardID ("WZ01");
		banishedWizardDetails.setStandardPhotoID ("WZ01");
		
		final PlayerPublicDetails banishedWizard = new PlayerPublicDetails (null, null, null);
		when (wizardClientUtils.getPlayerName (banishedWizard)).thenReturn ("Merlin");
		
		PlayerPublicDetails banishingWizard = null;
		KnownWizardDetails banishingWizardDetails = null;
		if (isEnemyAtAll)
		{
			banishingWizard = new PlayerPublicDetails (null, null, null);
	
			when (wizardClientUtils.getPlayerName (banishingWizard)).thenReturn (isEnemyWizard ? "Kali" : "Raiders");
			
			banishingWizardDetails = new KnownWizardDetails ();
			banishingWizardDetails.setWizardID ("WZ03");
			if (isEnemyWizard)
				banishingWizardDetails.setStandardPhotoID ("WZ03");
		}
		
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
		wizardBanished.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		wizardBanished.setLanguageHolder (langHolder);
		wizardBanished.setLanguageChangeMaster (langMaster);
		wizardBanished.setLargeFont (CreateFontsForTests.getLargeFont ());		
		wizardBanished.setBanishedWizard (banishedWizard);
		wizardBanished.setBanishedWizardDetails (banishedWizardDetails);
		wizardBanished.setBanishingWizard (banishingWizard);
		wizardBanished.setBanishingWizardDetails (banishingWizardDetails);
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
		testWizardBanishedUI (true, true);
	}

	/**
	 * Tests the WizardBanishedUI form, being banished by raiders
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWizardBanishedUI_Raiders () throws Exception
	{
		testWizardBanishedUI (false, true);
	}

	/**
	 * Tests the WizardBanishedUI form, being banished by their own stupidity
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testWizardBanishedUI_Nobody () throws Exception
	{
		testWizardBanishedUI (false, false);
	}
}
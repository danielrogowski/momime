package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.random.RandomUtils;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.DiplomacyScreen;
import momime.client.ui.dialogs.OverlandEnchantmentsUI;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.SpellClientUtilsImpl;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.LanguageTextVariant;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RelationScore;
import momime.common.database.WizardEx;
import momime.common.database.WizardPersonality;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.KnownWizardUtils;

/**
 * Tests the DiplomacyUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestDiplomacyUI extends ClientTestData
{
	/**
	 * Tests the DiplomacyUI class
	 * @param anotherWizard True if custom photo; false for standard photo 
	 * @throws Exception If there is a problem
	 */
	public final void testDiplomacyUI (final boolean anotherWizard) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		if (!anotherWizard)
		{
			final WizardEx wizard = new WizardEx ();
			wizard.setPortraitImageFile ("/momime.client.graphics/wizards/WZ12.png");
			wizard.setDiplomacyAnimation ("TALKING_ANIM");
			when (db.findWizard ("WZ12", "DiplomacyUI")).thenReturn (wizard);
		}
		
		final RelationScore relationScore = new RelationScore ();
		relationScore.setEyesLeftImage ("/momime.client.graphics/ui/diplomacy/eyes-left-08.png");
		relationScore.setEyesRightImage ("/momime.client.graphics/ui/diplomacy/eyes-right-08.png");
		when (db.findRelationScore ("RS05", "DiplomacyUI")).thenReturn (relationScore);
		
		final WizardPersonality personality = new WizardPersonality ();
		when (db.getWizardPersonality ()).thenReturn (Arrays.asList (personality));
		
		final LanguageTextVariant phrase = new LanguageTextVariant ();
		personality.getInitialMeetingPhrase ().add (phrase);
		phrase.getTextVariant ().add (createLanguageText (Language.ENGLISH, "Greetings OUR_PLAYER_NAME, it is I, TALKING_PLAYER_NAME!"));
		
		// Mock entries from the graphics XML
		final AnimationEx fade = new AnimationEx ();
		fade.setAnimationSpeed (8);
		for (int n = 1; n <= 15; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/ui/mirror/mirror-fade-frame" + ((n < 10) ? "0" : "") + n + ".png");
			fade.getFrame ().add (frame);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (OverlandEnchantmentsUI.MIRROR_ANIM, "DiplomacyUI")).thenReturn (fade);
		
		// Mock entries from the language XML
		final DiplomacyScreen diplomacyScreenLang = new DiplomacyScreen ();
		diplomacyScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Diplomacy"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getDiplomacyScreen ()).thenReturn (diplomacyScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// FOW (just to add the spell into)
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		// Wizard we're talking to
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		if (!anotherWizard)
		{
			final DiplomacyWizardDetails wizardDetails1 = new DiplomacyWizardDetails ();
			wizardDetails1.setStandardPhotoID ("WZ12");
			when (knownWizardUtils.findKnownWizardDetails (eq (fow.getWizardDetails ()), eq (1), anyString ())).thenReturn (wizardDetails1);
			
			final AnimationEx talkingAnim = new AnimationEx ();
			for (int n = 1; n <= 25; n++)
			{
				String s = Integer.valueOf (n).toString ();
				while (s.length () < 2)
					s = "0" + s;
				
				final AnimationFrame frame = new AnimationFrame ();
				frame.setImageFile ("/momime.client.graphics/wizards/WZ12-diplomacy-frame-" + s + ".png");
				talkingAnim.getFrame ().add (frame);
			}
			when (db.findAnimation ("TALKING_ANIM", "DiplomacyUI")).thenReturn (talkingAnim);
		}
		else
		{
			final DiplomacyWizardDetails wizardDetails2 = new DiplomacyWizardDetails ();
			wizardDetails2.setCustomPhoto (Files.readAllBytes (Paths.get (getClass ().getResource ("/CustomWizardPhoto.png").toURI ())));
			when (knownWizardUtils.findKnownWizardDetails (eq (fow.getWizardDetails ()), eq (2), anyString ())).thenReturn (wizardDetails2);
		}

		// Our wizard
		final KnownWizardDetails ourWizardDetails = new KnownWizardDetails ();
		when (knownWizardUtils.findKnownWizardDetails (eq (fow.getWizardDetails ()), eq (3), anyString ())).thenReturn (ourWizardDetails);
		
		// Players
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final PlayerPublicDetails talkingWizard = new PlayerPublicDetails (null, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (anotherWizard ? 2 : 1), anyString ())).thenReturn (talkingWizard);
		when (wizardClientUtils.getPlayerName (talkingWizard)).thenReturn ("Ariel");

		final PlayerPublicDetails ourWizard = new PlayerPublicDetails (null, null, null);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (3), anyString ())).thenReturn (ourWizard); 
		when (wizardClientUtils.getPlayerName (ourWizard)).thenReturn ("Bob");
		
		// Production types
		final ProductionTypeEx gold = new ProductionTypeEx ();
		gold.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Gold"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "regenerateGoldOfferText")).thenReturn (gold);
		
		// Year
		final MomGeneralPublicKnowledge gpk = new MomGeneralPublicKnowledge ();
		gpk.setTurnNumber (10);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (3);
		when (client.getGeneralPublicKnowledge ()).thenReturn (gpk);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/DiplomacyUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final DiplomacyUI diplomacy = new DiplomacyUI ();
		diplomacy.setDiplomacyLayout (layout);
		diplomacy.setUtils (utils);
		diplomacy.setClient (client);
		diplomacy.setGraphicsDB (gfx);
		diplomacy.setMultiplayerSessionUtils (multiplayerSessionUtils);
		diplomacy.setMediumFont (CreateFontsForTests.getMediumFont ());
		diplomacy.setLanguageHolder (langHolder);
		diplomacy.setLanguageChangeMaster (langMaster);
		diplomacy.setKnownWizardUtils (knownWizardUtils);
		diplomacy.setWizardClientUtils (wizardClientUtils);
		diplomacy.setTalkingWizardID (anotherWizard ? 2 : 1);
		diplomacy.setVisibleRelationScoreID ("RS05");
		diplomacy.setSpellClientUtils (new SpellClientUtilsImpl ());
		diplomacy.setRandomUtils (mock (RandomUtils.class));
		diplomacy.setPortraitState (DiplomacyPortraitState.APPEARING);
		diplomacy.setTextState (DiplomacyTextState.INITIAL_CONTACT);
		diplomacy.setTextUtils (new TextUtilsImpl ());
		
		// Display form
		diplomacy.initializeTalkingWizard ();
		diplomacy.setVisible (true);
		Thread.sleep (10000);
		diplomacy.setVisible (false);
	}

	/**
	 * Tests the DiplomacyUI form using a standard photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDiplomacyUI_StandardPhoto () throws Exception
	{
		testDiplomacyUI (false);
	}

	/**
	 * Tests the DiplomacyUI form using a custom photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDiplomacyUI_CustomPhoto () throws Exception
	{
		testDiplomacyUI (true);
	}
}
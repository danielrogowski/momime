package momime.client.ui.dialogs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
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
import momime.client.languages.database.SpellOfMasteryEndScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.WizardState;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Tests the SpellOfMasteryEndUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellOfMasteryEndUI extends ClientTestData
{
	/**
	 * Tests the SpellOfMasteryEndUI class
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellOfMasteryEndUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 5; n++)
		{
			if ((n == 1) || (n == 2) || (n == 5))
			{
				final WizardEx castingWizardDef = new WizardEx ();
				castingWizardDef.setChantingAnimation ("WIZARD_CHANTING_0" + n);
				castingWizardDef.setBallAnimation ("WIZARD_BALL_0" + n);
				when (db.findWizard (eq ("WZ0" + n), anyString ())).thenReturn (castingWizardDef);
			}
			
			// Chanting anim
			if (n == 1)
			{
				final AnimationEx chantingAnim = new AnimationEx ();
				for (int f = 1; f <= 8; f++)
				{
					final int frameNumber = (f <= 5) ? f : (10 - f);
					
					final AnimationFrame frame = new AnimationFrame ();
					frame.setImageFile ("/momime.client.graphics/wizards/WZ0" + n + "-chanting-frame-" + frameNumber + ".png");
					chantingAnim.getFrame ().add (frame);
				}
				
				when (db.findAnimation (eq ("WIZARD_CHANTING_0" + n), anyString ())).thenReturn (chantingAnim);
			}
			
			// Ball anim
			if ((n == 2) || (n == 5))
			{
				final AnimationEx ballAnim = new AnimationEx ();
				for (int f = 1; f <= 60; f++)
				{
					String s = Integer.valueOf (f).toString ();
					while (s.length () < 2)
						s = "0" + s;
					
					final AnimationFrame frame = new AnimationFrame ();
					frame.setImageFile ("/momime.client.graphics/wizards/WZ0" + n + "-ball-frame-" + s + ".png");
					ballAnim.getFrame ().add (frame);
				}
			
				when (db.findAnimation (eq ("WIZARD_BALL_0" + n), anyString ())).thenReturn (ballAnim);
			}
		}
		
		// Client
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		final AnimationEx portalAnim = new AnimationEx ();
		for (int n = 1; n <= 22; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 2)
				s = "0" + s;
			
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/animations/wizardsLab/portal-frame-" + s + ".png");
			portalAnim.getFrame ().add (frame);
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_SPELL_OF_MASTERY_PORTAL, "SpellOfMasteryEndUI")).thenReturn (portalAnim);

		// Mock entries from the language XML
		final SpellOfMasteryEndScreen spellOfMasteryEndScreenLang = new SpellOfMasteryEndScreen ();
		spellOfMasteryEndScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Spell of Mastery"));
		spellOfMasteryEndScreenLang.getLine ().add (createLanguageText (Language.ENGLISH, "PLAYER_NAME has completed casting the Spell of Mastery"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSpellOfMasteryEndScreen ()).thenReturn (spellOfMasteryEndScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Casting wizard
		final PlayerDescription castingWizardPd = new PlayerDescription ();
		castingWizardPd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge castingWizardPub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails castingWizard = new PlayerPublicDetails (castingWizardPd, castingWizardPub, null);
		
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (castingWizard)).thenReturn ("Bob");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails castingWizardDetails = new KnownWizardDetails ();
		castingWizardDetails.setStandardPhotoID ("WZ01");
		castingWizardDetails.setWizardState (WizardState.ACTIVE);
		when (knownWizardUtils.findKnownWizardDetails (priv.getKnownWizardDetails (), castingWizardPd.getPlayerID (), "SpellOfMasteryEndUI (C)")).thenReturn (castingWizardDetails);
		
		// Banished wizards
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (castingWizard);
		
		for (int n = 2; n <= 5; n++)
		{
			final MomPersistentPlayerPublicKnowledge banishedWizardPub = new MomPersistentPlayerPublicKnowledge ();
			
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (n);
			
			final PlayerPublicDetails banishedWizard = new PlayerPublicDetails (pd, banishedWizardPub, null);
			players.add (banishedWizard);
			
			final KnownWizardDetails banishedWizardDetails = new KnownWizardDetails ();
			banishedWizardDetails.setWizardState ((n == 4) ? WizardState.DEFEATED : WizardState.ACTIVE);
			if (n != 3)
				banishedWizardDetails.setStandardPhotoID ("WZ0" + n);
			
			when (knownWizardUtils.findKnownWizardDetails (priv.getKnownWizardDetails (), n, "SpellOfMasteryEndUI (B)")).thenReturn (banishedWizardDetails);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		
		// Wizards
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard (null)).thenReturn (true);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/SpellOfMasteryEndUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final SpellOfMasteryEndUI spellOfMasteryEnd = new SpellOfMasteryEndUI ();
		spellOfMasteryEnd.setSpellOfMasteryEndLayout (layout);
		spellOfMasteryEnd.setClient (client);
		spellOfMasteryEnd.setGraphicsDB (gfx);
		spellOfMasteryEnd.setUtils (utils);
		spellOfMasteryEnd.setLanguageHolder (langHolder);
		spellOfMasteryEnd.setLanguageChangeMaster (langMaster);
		spellOfMasteryEnd.setLargeFont (CreateFontsForTests.getLargeFont ());
		spellOfMasteryEnd.setCastingWizard (castingWizard);
		spellOfMasteryEnd.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		spellOfMasteryEnd.setKnownWizardUtils (knownWizardUtils);
		spellOfMasteryEnd.setMusicPlayer (mock (AudioPlayer.class));
		spellOfMasteryEnd.setWizardClientUtils (wizardClientUtils);
		
		// Display form		
		spellOfMasteryEnd.setModal (false);
		spellOfMasteryEnd.setVisible (true);
		Thread.sleep (20000);
		spellOfMasteryEnd.setVisible (false);
	}
}
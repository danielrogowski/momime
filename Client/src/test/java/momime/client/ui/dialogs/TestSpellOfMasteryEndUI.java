package momime.client.ui.dialogs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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
import momime.client.languages.database.SpellOfMasteryEndScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.WizardEx;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.WizardState;

/**
 * Tests the SpellOfMasteryEndUI class
 */
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
			final WizardEx castingWizardDef = new WizardEx ();
			castingWizardDef.setChantingAnimation ("WIZARD_CHANTING_0" + n);
			castingWizardDef.setBallAnimation ("WIZARD_BALL_0" + n);
			when (db.findWizard (eq ("WZ0" + n), anyString ())).thenReturn (castingWizardDef);
			
			// Chanting anim
			final AnimationEx chantingAnim = new AnimationEx ();
			for (int f = 1; f <= 8; f++)
			{
				final int frameNumber = (f <= 5) ? f : (10 - f);
				
				final AnimationFrame frame = new AnimationFrame ();
				frame.setImageFile ("/momime.client.graphics/wizards/WZ0" + n + "-chanting-frame-" + frameNumber + ".png");
				chantingAnim.getFrame ().add (frame);
			}
			
			when (db.findAnimation (eq ("WIZARD_CHANTING_0" + n), anyString ())).thenReturn (chantingAnim);
			
			// Ball anim
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
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
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
		final MomPersistentPlayerPublicKnowledge castingWizardPub = new MomPersistentPlayerPublicKnowledge ();
		castingWizardPub.setStandardPhotoID ("WZ01");
		castingWizardPub.setWizardState (WizardState.ACTIVE);
		
		final PlayerPublicDetails castingWizard = new PlayerPublicDetails (null, castingWizardPub, null);
		
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (castingWizard)).thenReturn ("Bob");
		
		// Banished wizards
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (castingWizard);
		
		for (int n = 2; n <= 5; n++)
		{
			final MomPersistentPlayerPublicKnowledge banishedWizardPub = new MomPersistentPlayerPublicKnowledge ();
			
			if (n != 3)
				banishedWizardPub.setStandardPhotoID ("WZ0" + n);
			
			banishedWizardPub.setWizardState ((n == 4) ? WizardState.DEFEATED : WizardState.ACTIVE);
			
			final PlayerPublicDetails banishedWizard = new PlayerPublicDetails (null, banishedWizardPub, null);
			players.add (banishedWizard);
		}
		
		when (client.getPlayers ()).thenReturn (players);
		
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
		spellOfMasteryEnd.setMusicPlayer (mock (AudioPlayer.class));
		spellOfMasteryEnd.setWizardClientUtils (wizardClientUtils);
		
		// Display form		
		spellOfMasteryEnd.setModal (false);
		spellOfMasteryEnd.setVisible (true);
		Thread.sleep (20000);
		spellOfMasteryEnd.setVisible (false);
	}
}
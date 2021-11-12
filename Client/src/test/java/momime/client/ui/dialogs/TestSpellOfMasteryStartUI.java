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
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SpellOfMasteryStartScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.Language;

/**
 * Tests the SpellOfMasteryStartUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellOfMasteryStartUI extends ClientTestData
{
	/**
	 * Tests the SpellOfMasteryStartUI class
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellOfMasteryStartUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		final AnimationEx masteryAnim = new AnimationEx ();
		for (int n = 1; n <= 60; n++)
		{
			String s = Integer.valueOf (n).toString ();
			while (s.length () < 2)
				s = "0" + s;
			
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/animations/spellOfMasteryStart/spellOfMastery-frame-" + s + ".png");
			masteryAnim.getFrame ().add (frame);
		}
		
		when (gfx.findAnimation (GraphicsDatabaseConstants.ANIM_SPELL_OF_MASTERY_START, "SpellOfMasteryStartUI")).thenReturn (masteryAnim);

		// Mock entries from the language XML
		final SpellOfMasteryStartScreen spellOfMasteryStartScreenLang = new SpellOfMasteryStartScreen ();
		spellOfMasteryStartScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Spell of Mastery"));
		spellOfMasteryStartScreenLang.getLine ().add (createLanguageText (Language.ENGLISH, "PLAYER_NAME has started casting the Spell of Mastery"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSpellOfMasteryStartScreen ()).thenReturn (spellOfMasteryStartScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Casting wizard
		final PlayerPublicDetails castingWizard = new PlayerPublicDetails (null, null, null);
		
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		when (wizardClientUtils.getPlayerName (castingWizard)).thenReturn ("Bob");

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/SpellOfMasteryStartUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final SpellOfMasteryStartUI spellOfMasteryStart = new SpellOfMasteryStartUI ();
		spellOfMasteryStart.setSpellOfMasteryStartLayout (layout);
		spellOfMasteryStart.setGraphicsDB (gfx);
		spellOfMasteryStart.setUtils (utils);
		spellOfMasteryStart.setLanguageHolder (langHolder);
		spellOfMasteryStart.setLanguageChangeMaster (langMaster);
		spellOfMasteryStart.setLargeFont (CreateFontsForTests.getLargeFont ());
		spellOfMasteryStart.setCastingWizard (castingWizard);
		spellOfMasteryStart.setMusicPlayer (mock (AudioPlayer.class));
		spellOfMasteryStart.setWizardClientUtils (wizardClientUtils);
		
		// Display form		
		spellOfMasteryStart.setModal (false);
		spellOfMasteryStart.setVisible (true);
		Thread.sleep (20000);
		spellOfMasteryStart.setVisible (false);
	}
}
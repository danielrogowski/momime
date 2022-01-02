package momime.client.ui.dialogs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SpellCasting;
import momime.client.messages.process.AddOrUpdateMaintainedSpellMessageImpl;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.utils.WizardClientUtils;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.Spell;
import momime.common.database.WizardEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;

/**
 * Tests the OverlandEnchantmentsUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestOverlandEnchantmentsUI extends ClientTestData
{
	/**
	 * Tests the OverlandEnchantmentsUI form
	 * @param anotherWizard True if spell is being cast by another wizard, False if its ours 
	 * @throws Exception If there is a problem
	 */
	private final void testOverlandEnchantmentsUI (final boolean anotherWizard) throws Exception
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
			when (db.findWizard ("WZ01", "OverlandEnchantmentsUI")).thenReturn (wizard);
		}
		
		final Spell spellDef = new Spell ();
		spellDef.getSpellName ().add (createLanguageText (Language.ENGLISH, "Just Cause"));
		spellDef.setOverlandEnchantmentImageFile ("/momime.client.graphics/spells/SP127/overlandEnchantment.png");
		when (db.findSpell ("SP001", "OverlandEnchantmentsUI")).thenReturn (spellDef);		
		
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
		when (gfx.findAnimation (OverlandEnchantmentsUI.MIRROR_ANIM, "OverlandEnchantmentsUI")).thenReturn (fade);
		
		// Mock entries from the language XML
		final SpellCasting spellCastingLang = new SpellCasting ();
		spellCastingLang.getOurOverlandEnchantment ().add (createLanguageText (Language.ENGLISH, "You have completed casting..."));
		spellCastingLang.getEnemyOverlandEnchantment ().add (createLanguageText (Language.ENGLISH, "PLAYER_NAME has completed casting..."));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSpellCasting ()).thenReturn (spellCastingLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// FOW (just to add the spell into)
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		// Player
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);

		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("FF0000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, null, trans1);

		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);

		final MomTransientPlayerPublicKnowledge trans2 = new MomTransientPlayerPublicKnowledge ();
		trans2.setFlagColour ("FF0000");
		
		final PlayerPublicDetails player2 = new PlayerPublicDetails (pd2, null, trans2);
		if (anotherWizard)
			when (wizardClientUtils.getPlayerName (player2)).thenReturn ("Someone");
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		players.add (player2);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		
		if (!anotherWizard)
			when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd1.getPlayerID ()), anyString ())).thenReturn (player1);
		
		if (anotherWizard)
		{
			when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd2.getPlayerID ()), anyString ())).thenReturn (player2);
			when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd2.getPlayerID ()))).thenReturn (player2);
		}
		
		// Wizard
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		if (!anotherWizard)
		{
			final KnownWizardDetails wizardDetails1 = new KnownWizardDetails ();
			wizardDetails1.setStandardPhotoID ("WZ01");
			when (knownWizardUtils.findKnownWizardDetails (eq (priv.getKnownWizardDetails ()), eq (pd1.getPlayerID ()), anyString ())).thenReturn (wizardDetails1);
		}
		else
		{
			final KnownWizardDetails wizardDetails2 = new KnownWizardDetails ();
			wizardDetails2.setCustomPhoto (Files.readAllBytes (Paths.get (getClass ().getResource ("/CustomWizardPhoto.png").toURI ())));
			when (knownWizardUtils.findKnownWizardDetails (eq (priv.getKnownWizardDetails ()), eq (pd2.getPlayerID ()), anyString ())).thenReturn (wizardDetails2);
		}
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd1.getPlayerID ());
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Spell being shown
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setCastingPlayerID (anotherWizard ? 2 : 1);
		spell.setSpellID ("SP001");
		
		final AddOrUpdateMaintainedSpellMessageImpl spellMessage = new AddOrUpdateMaintainedSpellMessageImpl ();
		spellMessage.setMaintainedSpell (spell);
		spellMessage.setNewlyCast (true);

		// Using the real image generator is easier than mocking it out
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/OverlandEnchantmentsUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final OverlandEnchantmentsUI ench = new OverlandEnchantmentsUI ();
		ench.setUtils (utils);
		ench.setLanguageHolder (langHolder);
		ench.setLanguageChangeMaster (langMaster);
		ench.setClient (client);
		ench.setGraphicsDB (gfx);
		ench.setPlayerColourImageGenerator (gen);
		ench.setMultiplayerSessionUtils (multiplayerSessionUtils);
		ench.setLargeFont (CreateFontsForTests.getLargeFont ());
		ench.setOverlandEnchantmentsLayout (layout);
		ench.setAddSpellMessage (spellMessage);
		ench.setWizardClientUtils (wizardClientUtils);
		ench.setKnownWizardUtils (knownWizardUtils);
		ench.setMagicSlidersUI (new MagicSlidersUI ());
		ench.setMemoryMaintainedSpellUtils (mock (MemoryMaintainedSpellUtils.class));
		
		// Display form
		ench.setModal (false);
		ench.setVisible (true);
		Thread.sleep (10000);
		ench.setVisible (false);
	}

	/**
	 * Tests the OverlandEnchantmentsUI form when we cast an overland enchantment and we're using a standard photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOverlandEnchantmentsUI_StandardPhoto_OurSpell () throws Exception
	{
		testOverlandEnchantmentsUI (false);
	}

	/**
	 * Tests the OverlandEnchantmentsUI form when somebody else cast an overland enchantment and they're using a wizard with a custom photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOverlandEnchantmentsUI_CustomPhoto_OtherWizardSpell () throws Exception
	{
		testOverlandEnchantmentsUI (true);
	}
}
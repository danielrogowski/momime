package momime.client.ui.dialogs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.SpellGfx;
import momime.client.graphics.database.WizardGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.messages.process.AddMaintainedSpellMessageImpl;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Tests the OverlandEnchantmentsUI class
 */
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
		
		// Mock entries from the graphics XML
		final AnimationGfx fade = new AnimationGfx ();
		fade.setAnimationSpeed (8);
		for (int n = 1; n <= 15; n++)
			fade.getFrame ().add ("/momime.client.graphics/ui/mirror/mirror-fade-frame" + ((n < 10) ? "0" : "") + n + ".png");
		
		final WizardGfx wizard = new WizardGfx ();
		wizard.setPortraitImageFile ("/momime.client.graphics/wizards/WZ12.png");
		
		final SpellGfx spellGfx = new SpellGfx ();
		spellGfx.setOverlandEnchantmentImageFile ("/momime.client.graphics/spells/SP127/overlandEnchantment.png");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (OverlandEnchantmentsUI.MIRROR_ANIM, "OverlandEnchantmentsUI")).thenReturn (fade);
		when (gfx.findWizard ("WZ01", "OverlandEnchantmentsUI")).thenReturn (wizard);
		when (gfx.findSpell ("SP001", "OverlandEnchantmentsUI")).thenReturn (spellGfx);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("SpellCasting", "OurOverlandEnchantment")).thenReturn ("You have completed casting...");
		when (lang.findCategoryEntry ("SpellCasting", "EnemyOverlandEnchantment")).thenReturn ("PLAYER_NAME has completed casting...");
		
		final SpellLang spellLang = new SpellLang ();
		spellLang.setSpellName ("Just Cause");
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Player
		final WizardClientUtils wizardClientUtils = mock (WizardClientUtils.class);
		
		final PlayerDescription pd1 = new PlayerDescription ();
		pd1.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge pub1 = new MomPersistentPlayerPublicKnowledge ();
		pub1.setStandardPhotoID ("WZ01");
		
		final MomTransientPlayerPublicKnowledge trans1 = new MomTransientPlayerPublicKnowledge ();
		trans1.setFlagColour ("FF0000");
		
		final PlayerPublicDetails player1 = new PlayerPublicDetails (pd1, pub1, trans1);
		when (wizardClientUtils.getPlayerName (player1)).thenReturn ("Us");

		final PlayerDescription pd2 = new PlayerDescription ();
		pd2.setPlayerID (2);

		final MomPersistentPlayerPublicKnowledge pub2 = new MomPersistentPlayerPublicKnowledge ();
		pub2.setCustomPhoto (Files.readAllBytes (Paths.get (getClass ().getResource ("/CustomWizardPhoto.png").toURI ())));
		
		final MomTransientPlayerPublicKnowledge trans2 = new MomTransientPlayerPublicKnowledge ();
		trans2.setFlagColour ("FF0000");
		
		final PlayerPublicDetails player2 = new PlayerPublicDetails (pd2, pub2, trans2);
		when (wizardClientUtils.getPlayerName (player2)).thenReturn ("Someone");
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player1);
		players.add (player2);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd1.getPlayerID ());
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd1.getPlayerID ()), anyString ())).thenReturn (player1);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd1.getPlayerID ()))).thenReturn (player1);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd2.getPlayerID ()), anyString ())).thenReturn (player2);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd2.getPlayerID ()))).thenReturn (player2);
		
		// FOW (just to add the spell into)
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Spell being shown
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setCastingPlayerID (anotherWizard ? 2 : 1);
		spell.setSpellID ("SP001");
		
		final AddMaintainedSpellMessageImpl spellMessage = new AddMaintainedSpellMessageImpl ();
		spellMessage.setMaintainedSpell (spell);

		// Using the real image generator is easier than mocking it out
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
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
		ench.setMagicSlidersUI (new MagicSlidersUI ());
		
		// Display form
		ench.setModal (false);
		ench.setVisible (true);
		Thread.sleep (5000);
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
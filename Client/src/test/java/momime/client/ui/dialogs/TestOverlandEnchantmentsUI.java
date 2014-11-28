package momime.client.ui.dialogs;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.messages.process.AddMaintainedSpellMessageImpl;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.ui.frames.MagicSlidersUI;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the OverlandEnchantmentsUI class
 */
public final class TestOverlandEnchantmentsUI
{
	/**
	 * Tests the OverlandEnchantmentsUI form
	 * @param photo standardPhotoID, or filename to a custom wizard photo
	 * @param isCustomPhoto True if photo represents a custom image file, False if its a wizardID 
	 * @throws Exception If there is a problem
	 */
	private final void testOverlandEnchantmentsUI (final String photo, final boolean isCustomPhoto) throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the graphics XML
		final AnimationEx fade = new AnimationEx ();
		fade.setAnimationSpeed (8);
		for (int n = 1; n <= 15; n++)
			fade.getFrame ().add ("/momime.client.graphics/ui/mirror/mirror-fade-frame" + ((n < 10) ? "0" : "") + n + ".png");
		
		final Wizard wizard = new Wizard ();
		wizard.setPortraitFile ("/momime.client.graphics/wizards/WZ12.png");
		
		final momime.client.graphics.database.v0_9_5.Spell spellGfx = new momime.client.graphics.database.v0_9_5.Spell ();
		spellGfx.setOverlandEnchantmentImageFile ("/momime.client.graphics/spells/SP127/overlandEnchantment.png");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (OverlandEnchantmentsUI.MIRROR_ANIM, "OverlandEnchantmentsUI")).thenReturn (fade);
		when (gfx.findWizard ("WZ01", "OverlandEnchantmentsUI")).thenReturn (wizard);
		when (gfx.findSpell ("SP001", "OverlandEnchantmentsUI")).thenReturn (spellGfx);
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("SpellCasting", "OurOverlandEnchantment")).thenReturn ("You have completed casting...");
		
		final Spell spellLang = new Spell ();
		spellLang.setSpellName ("Just Cause");
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		if (isCustomPhoto)
			pub.setCustomPhoto (Files.readAllBytes (Paths.get (getClass ().getResource (photo).toURI ())));
		else
			pub.setStandardPhotoID (photo);
		
		final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
		trans.setFlagColour ("FF0000");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, trans);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID ());
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
		
		// FOW (just to add the spell into)
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Spell being shown
		final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
		spell.setCastingPlayerID (1);
		spell.setSpellID ("SP001");
		
		final AddMaintainedSpellMessageImpl spellMessage = new AddMaintainedSpellMessageImpl ();
		spellMessage.setMaintainedSpell (spell);

		// Using the real image generator is easier than mocking it out
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/OverlandEnchantmentsUI.xml"));
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
		ench.setMagicSlidersUI (new MagicSlidersUI ());
		
		// Display form
		ench.setModal (false);
		ench.setVisible (true);
		Thread.sleep (10000);
		ench.setVisible (false);
	}

	/**
	 * Tests the OverlandEnchantmentsUI form using a wizard with a standard photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOverlandEnchantmentsUI_StandardPhoto () throws Exception
	{
		testOverlandEnchantmentsUI ("WZ01", false);
	}

	/**
	 * Tests the OverlandEnchantmentsUI form using a wizard with a custom photo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOverlandEnchantmentsUI_CustomPhoto () throws Exception
	{
		testOverlandEnchantmentsUI ("/CustomWizardPhoto.png", true);
	}
}
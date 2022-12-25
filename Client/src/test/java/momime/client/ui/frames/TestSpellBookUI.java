package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SpellBookScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.SpellBookPage;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.Pick;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellBookSection;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.WizardState;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

/**
 * Tests the SpellBookUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellBookUI extends ClientTestData
{
	/**
	 * Tests the SpellBookUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellBookUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		int sectionNumber = 0;
		for (final String sectionName : new String [] {"Summoning spells", "Overland enchantments"})
		{
			sectionNumber++;
			
			final SpellBookSection section = new SpellBookSection ();
			section.getSpellBookSectionName ().add (createLanguageText (Language.ENGLISH, sectionName));
			when (db.findSpellBookSection (SpellBookSectionID.fromValue ("SC0" + sectionNumber), "SpellBookUI")).thenReturn (section);
		}
		
		final ProductionTypeEx research = new ProductionTypeEx ();
		research.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "RP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "SpellBookUI")).thenReturn (research);
		
		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "SpellBookUI")).thenReturn (mana);
		
		final Pick lifeSpells = new Pick ();
		lifeSpells.setPickBookshelfTitleColour ("FFFFFF");
		when (db.findPick ("MB01", "languageOrPageChanged")).thenReturn (lifeSpells);
		
		final Pick pinkSpells = new Pick ();
		pinkSpells.setPickBookshelfTitleColour ("EF75C8");
		when (db.findPick ("MB02", "languageOrPageChanged")).thenReturn (pinkSpells);
		
		// Mock entries from the language XML
		final SpellBookScreen spellBookScreenLang = new SpellBookScreen ();
		spellBookScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Spells"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSpellBookScreen ()).thenReturn (spellBookScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from the graphics XML
		final AnimationEx pageTurn = new AnimationEx ();
		pageTurn.setAnimationSpeed (5);
		for (int n = 1; n <= 4; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/ui/spellBook/spellBookAnim-frame" + n + ".png");
			pageTurn.getFrame ().add (frame);
		}
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findAnimation (SpellBookUI.ANIM_PAGE_TURN, "SpellBookUI")).thenReturn (pageTurn);
		
		// Session description
		final SpellSetting spellSettings = new SpellSetting ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		// Player
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);

		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final KnownWizardDetails ourWizard = new KnownWizardDetails ();
		ourWizard.setWizardState (WizardState.ACTIVE);
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2, "languageOrPageChanged")).thenReturn (ourWizard);
		
		// Research statuses - we know 0 of SP000 - SP009, 1 of SP010 - SP019 and so on
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);
		
		// Some example spells
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		final Spell magicSpirit = new Spell ();
		magicSpirit.setSpellID ("SP201");
		magicSpirit.getSpellName ().add (createLanguageText (Language.ENGLISH, "Magic Spirit"));
		magicSpirit.getSpellDescription ().add (createLanguageText (Language.ENGLISH, "Summons magic spirit to meld with nodes and generate magic power."));
		magicSpirit.setOverlandCastingCost (30);
		when (spellUtils.spellCanBeCastIn (magicSpirit, SpellCastType.OVERLAND)).thenReturn (true);
		when (spellUtils.getReducedOverlandCastingCost (magicSpirit, null, null, ourWizard.getPick (), mem.getMaintainedSpell (), spellSettings, db)).thenReturn (30);
		
		final Spell unicorns = new Spell ();
		unicorns.setSpellID ("SP136");
		unicorns.setSpellRealm ("MB01");
		unicorns.getSpellName ().add (createLanguageText (Language.ENGLISH, "Unicorns"));
		unicorns.getSpellDescription ().add (createLanguageText (Language.ENGLISH, "Summons a herd of unicorns. These lovely creatures can teleport to any square on the battlefield."));
		unicorns.setOverlandCastingCost (250);
		when (spellUtils.spellCanBeCastIn (unicorns, SpellCastType.OVERLAND)).thenReturn (true);
		when (spellUtils.getReducedOverlandCastingCost (unicorns, null, null, ourWizard.getPick (), mem.getMaintainedSpell (), spellSettings, db)).thenReturn (225);

		final Spell foo = new Spell ();
		foo.setSpellID ("SP250");
		foo.setSpellRealm ("MB02");
		foo.getSpellName ().add (createLanguageText (Language.ENGLISH, "Foo"));
		foo.getSpellDescription ().add (createLanguageText (Language.ENGLISH, "Isn't it amazing having an overland enchantment that can be cast in combat!"));
		foo.setOverlandCastingCost (80);
		foo.setCombatCastingCost (25);
		when (spellUtils.spellCanBeCastIn (foo, SpellCastType.OVERLAND)).thenReturn (true);
		when (spellUtils.getReducedOverlandCastingCost (foo, null, null, ourWizard.getPick (), mem.getMaintainedSpell (), spellSettings, db)).thenReturn (70);
		when (spellUtils.getReducedCombatCastingCost (foo, null, ourWizard.getPick (), mem.getMaintainedSpell (), spellSettings, db)).thenReturn (20);
		
		// Mock which sections and spells are on which pages
		final SpellBookPage summoningPage = new SpellBookPage ();
		summoningPage.setSectionID (SpellBookSectionID.SUMMONING);
		summoningPage.setFirstPageOfSection (true);
		summoningPage.getSpells ().add (magicSpirit);
		summoningPage.getSpells ().add (unicorns);

		final SpellBookPage overlandEnchantmentsPage = new SpellBookPage ();
		overlandEnchantmentsPage.setSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		overlandEnchantmentsPage.setFirstPageOfSection (true);
		overlandEnchantmentsPage.getSpells ().add (foo);
		
		final SpellBookPage researchPage = new SpellBookPage ();
		researchPage.setSectionID (SpellBookSectionID.RESEARCHABLE_NOW);
		researchPage.setFirstPageOfSection (true);
		
		final SpellClientUtils spellClientUtils = mock (SpellClientUtils.class);
		when (spellClientUtils.generateSpellBookPages (SpellCastType.OVERLAND)).thenReturn (Arrays.asList (summoningPage, overlandEnchantmentsPage, researchPage));
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getOurPlayerID ()).thenReturn (2);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Set up form
		final SpellBookUI book = new SpellBookUI ();
		book.setUtils (utils);
		book.setLanguageHolder (langHolder);
		book.setLanguageChangeMaster (langMaster);
		book.setGraphicsDB (gfx);
		book.setClient (client);
		book.setSpellUtils (spellUtils);
		book.setMultiplayerSessionUtils (multiplayerSessionUtils);
		book.setKnownWizardUtils (knownWizardUtils);
		book.setTextUtils (new TextUtilsImpl ());
		book.setSmallFont (CreateFontsForTests.getSmallFont ());
		book.setMediumFont (CreateFontsForTests.getMediumFont ());
		book.setLargeFont (CreateFontsForTests.getLargeFont ());
		book.setCombatUI (new CombatUI ());
		book.setSpellClientUtils (spellClientUtils);
		book.setCastType (SpellCastType.OVERLAND);

		// Display form		
		book.setVisible (true);
		Thread.sleep (5000);
		book.setVisible (false);
	}	
}
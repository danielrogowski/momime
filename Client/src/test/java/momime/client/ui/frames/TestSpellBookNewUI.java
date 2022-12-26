package momime.client.ui.frames;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SpellBookScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.SpellBookPage;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.TextUtilsImpl;
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
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.WizardState;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

/**
 * Tests the SpellBookNewUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellBookNewUI extends ClientTestData
{
	/**
	 * Tests the SpellBookNewUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellBookNewUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		int sectionNumber = 0;
		for (final String sectionName : new String [] {"Summoning spells", "Overland enchantments", "Research spells"})
		{
			sectionNumber++;
			
			final SpellBookSection section = new SpellBookSection ();
			section.getSpellBookSectionName ().add (createLanguageText (Language.ENGLISH, sectionName));
			
			if (sectionNumber > 2)
				lenient ().when (db.findSpellBookSection (SpellBookSectionID.fromValue ("SC98"), "SpellBookUI")).thenReturn (section);
			else
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

		// Session description
		final SpellSetting spellSettings = new SpellSetting ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final KnownWizardDetails ourWizard = new KnownWizardDetails ();
		ourWizard.setWizardState (WizardState.ACTIVE);

		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2, "languageOrPageChanged")).thenReturn (ourWizard);
		
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
		
		final Spell mastery = new Spell ();
		mastery.setSpellID ("SP213");
		mastery.getSpellName ().add (createLanguageText (Language.ENGLISH, "Spell of Mastery"));
		mastery.getSpellDescription ().add (createLanguageText (Language.ENGLISH, "Banishes all opponent wizards to Limbo. You win."));
		mastery.setOverlandCastingCost (5000);
		mastery.setResearchCost (60000);
		
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setRemainingResearchCost (59000);
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE_NOW);
		researchStatus.setSpellID ("SP213");
		lenient ().when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP213")).thenReturn (researchStatus);
		
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
		researchPage.getSpells ().add (mastery);
		
		final SpellClientUtils spellClientUtils = mock (SpellClientUtils.class);
		when (spellClientUtils.generateSpellBookPages (SpellCastType.OVERLAND)).thenReturn (Arrays.asList (summoningPage, overlandEnchantmentsPage, researchPage));
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getOurPlayerID ()).thenReturn (2);
		when (client.getSessionDescription ()).thenReturn (sd);

		// Layout
		final XmlLayoutContainerEx bookLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/SpellBookUI.xml"));
		final XmlLayoutContainerEx spellLayout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/SpellBookUI-Spell.xml"));
		bookLayout.buildMaps ();
		spellLayout.buildMaps ();
		
		// Set up form
		final SpellBookNewUI book = new SpellBookNewUI ();
		book.setSpellBookLayout (bookLayout);
		book.setSpellLayout (spellLayout);
		book.setUtils (utils);
		book.setLanguageHolder (langHolder);
		book.setClient (client);
		book.setSpellUtils (spellUtils);
		book.setLanguageChangeMaster (langMaster);
		book.setKnownWizardUtils (knownWizardUtils);
		book.setTextUtils (new TextUtilsImpl ());
		book.setLargeFont (CreateFontsForTests.getLargeFont ());
		book.setMediumFont (CreateFontsForTests.getMediumFont ());
		book.setSmallFont (CreateFontsForTests.getSmallFont ());
		book.setCombatUI (new CombatUI ());
		book.setSpellClientUtils (spellClientUtils);
		book.setCastType (SpellCastType.OVERLAND);

		// Display form		
		book.setVisible (true);
		Thread.sleep (20000);
		book.setVisible (false);
	}	
}
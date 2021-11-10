package momime.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ndg.swing.NdgUIUtils;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.HelpScreen;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CityViewElement;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.Pick;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidUnitTarget;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the SpellClientUtilsImpl class
 */
public final class TestSpellClientUtilsImpl extends ClientTestData
{
	/**
	 * Tests the listUpkeepsOfSpell method when the spell has no upkeeps
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListUpkeepsOfSpell_None () throws Exception
	{
		// Spell
		final Spell spell = new Spell ();
		
		// Player picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		
		// Run method
		assertNull (utils.listUpkeepsOfSpell (spell, picks));
	}

	/**
	 * Tests the listUpkeepsOfSpell method when the spell has a single normal upkeep
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListUpkeepsOfSpell_One () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Mana"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "listUpkeepsOfSpell")).thenReturn (mana);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellUpkeepWithoutChanneler ().add (createLanguageText (Language.ENGLISH, "UPKEEP_VALUE PRODUCTION_TYPE"));
		helpLang.getSpellUpkeepWithChanneler ().add (createLanguageText (Language.ENGLISH, "HALF_UPKEEP_VALUE PRODUCTION_TYPE (reduced from UPKEEP_VALUE PRODUCTION_TYPE)"));
		helpLang.getSpellUpkeepFixed ().add (createLanguageText (Language.ENGLISH, "Upkeep: UPKEEP_LIST per turn"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell
		final ProductionTypeAndUndoubledValue upkeep = new ProductionTypeAndUndoubledValue ();
		upkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		upkeep.setUndoubledProductionValue (5);
		
		final Spell spell = new Spell ();
		spell.getSpellUpkeep ().add (upkeep);
		
		// Player picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_CHANNELER)).thenReturn (0);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Upkeep: 5 Mana per turn", utils.listUpkeepsOfSpell (spell, picks));
	}

	/**
	 * Tests the listUpkeepsOfSpell method when the spell has a single normal upkeep, but have the channeler retort
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListUpkeepsOfSpell_Channeler () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Mana"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "listUpkeepsOfSpell")).thenReturn (mana);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellUpkeepWithoutChanneler ().add (createLanguageText (Language.ENGLISH, "UPKEEP_VALUE PRODUCTION_TYPE"));
		helpLang.getSpellUpkeepWithChanneler ().add (createLanguageText (Language.ENGLISH, "HALF_UPKEEP_VALUE PRODUCTION_TYPE (reduced from UPKEEP_VALUE PRODUCTION_TYPE)"));
		helpLang.getSpellUpkeepFixed ().add (createLanguageText (Language.ENGLISH, "Upkeep: UPKEEP_LIST per turn"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell
		final ProductionTypeAndUndoubledValue upkeep = new ProductionTypeAndUndoubledValue ();
		upkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		upkeep.setUndoubledProductionValue (5);
		
		final Spell spell = new Spell ();
		spell.getSpellUpkeep ().add (upkeep);
		
		// Player picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_CHANNELER)).thenReturn (1);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Upkeep: 2½ Mana (reduced from 5 Mana) per turn", utils.listUpkeepsOfSpell (spell, picks));
	}

	/**
	 * Tests the listUpkeepsOfSpell method when the spell has a more than one upkeep
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListUpkeepsOfSpell_Two () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Mana"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "listUpkeepsOfSpell")).thenReturn (mana);

		final ProductionTypeEx gold = new ProductionTypeEx ();
		gold.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Gold"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "listUpkeepsOfSpell")).thenReturn (gold);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellUpkeepWithoutChanneler ().add (createLanguageText (Language.ENGLISH, "UPKEEP_VALUE PRODUCTION_TYPE"));
		helpLang.getSpellUpkeepWithChanneler ().add (createLanguageText (Language.ENGLISH, "HALF_UPKEEP_VALUE PRODUCTION_TYPE (reduced from UPKEEP_VALUE PRODUCTION_TYPE)"));
		helpLang.getSpellUpkeepFixed ().add (createLanguageText (Language.ENGLISH, "Upkeep: UPKEEP_LIST per turn"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell
		final ProductionTypeAndUndoubledValue upkeep1 = new ProductionTypeAndUndoubledValue ();
		upkeep1.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		upkeep1.setUndoubledProductionValue (5);

		final ProductionTypeAndUndoubledValue upkeep2 = new ProductionTypeAndUndoubledValue ();
		upkeep2.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		upkeep2.setUndoubledProductionValue (2);
		
		final Spell spell = new Spell ();
		spell.getSpellUpkeep ().add (upkeep1);
		spell.getSpellUpkeep ().add (upkeep2);
		
		// Player picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_CHANNELER)).thenReturn (0);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Upkeep: 5 Mana, 2 Gold per turn", utils.listUpkeepsOfSpell (spell, picks));
	}
	
	/**
	 * Tests the listValidMagicRealmLifeformTypeTargetsOfSpell method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListValidMagicRealmLifeformTypeTargetsOfSpell () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Pick magicRealm1 = new Pick ();
		magicRealm1.getUnitMagicRealmPlural ().add (createLanguageText (Language.ENGLISH, "Chaos units"));
		when (db.findPick ("LT01", "listValidMagicRealmLifeformTypeTargetsOfSpell")).thenReturn (magicRealm1);

		final Pick magicRealm3 = new Pick ();
		magicRealm3.getUnitMagicRealmPlural ().add (createLanguageText (Language.ENGLISH, "Death units"));
		when (db.findPick ("LT02", "listValidMagicRealmLifeformTypeTargetsOfSpell")).thenReturn (magicRealm3);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();

		// Spell
		final Spell spell = new Spell ();
		
		for (int n = 1; n <= 2; n++)
		{
			final SpellValidUnitTarget target = new SpellValidUnitTarget ();
			target.setTargetMagicRealmID ("LT0" + n);
			
			spell.getSpellValidUnitTarget ().add (target);
		}

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		
		// Run method
		assertEquals (System.lineSeparator () + "Chaos units" + System.lineSeparator () + "Death units",
			utils.listValidMagicRealmLifeformTypeTargetsOfSpell (spell));
	}
	
	/**
	 * Tests the listSavingThrowsOfSpell method when there are no ValidUnitTarget records at all
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListSavingThrowsOfSpell_None () throws Exception
	{
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellBookNoSavingThrow ().add (createLanguageText (Language.ENGLISH, "None"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell details
		final Spell spell = new Spell ();

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		
		// Run method
		assertEquals ("None", utils.listSavingThrowsOfSpell (spell));
	}
	
	/**
	 * Tests the listSavingThrowsOfSpell method when there are ValidUnitTarget records defined, but none list a saving throw
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListSavingThrowsOfSpell_LimitedTargets () throws Exception
	{
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellBookNoSavingThrow ().add (createLanguageText (Language.ENGLISH, "None"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell details
		final Spell spell = new Spell ();
		
		final SpellValidUnitTarget target = new SpellValidUnitTarget ();
		target.setTargetMagicRealmID ("X");
		spell.getSpellValidUnitTarget ().add (target);

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		
		// Run method
		assertEquals ("None", utils.listSavingThrowsOfSpell (spell));
	}

	/**
	 * Tests the listSavingThrowsOfSpell method when there is a saving throw defined, with no modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListSavingThrowsOfSpell_BasicSavingThrow () throws Exception
	{
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellBookNoSavingThrowModifier ().add (createLanguageText (Language.ENGLISH, "Saves against Resistance"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setCombatBaseDamage (0);
		
		final SpellValidUnitTarget target = new SpellValidUnitTarget ();
		spell.getSpellValidUnitTarget ().add (target);

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		
		// Run method
		assertEquals ("Saves against Resistance", utils.listSavingThrowsOfSpell (spell));
	}
	
	/**
	 * Tests the listSavingThrowsOfSpell method when there is a saving throw defined, with no modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListSavingThrowsOfSpell_ModifiedSavingThrow_NoSpellValidUnitTargetRecords () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitSkillEx attr = new UnitSkillEx ();
		attr.getUnitSkillDescription ().add (createLanguageText (Language.ENGLISH, "Resistance"));
		when (db.findUnitSkill (eq ("UA01"), anyString ())).thenReturn (attr);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellBookSingleSavingThrowModifier ().add (createLanguageText (Language.ENGLISH, "Saves against Resistance at SAVING_THROW_MODIFIER"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
				
		// Spell details
		final Spell spell = new Spell ();
		spell.setCombatBaseDamage (5);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Saves against Resistance at -5", utils.listSavingThrowsOfSpell (spell));
	}

	/**
	 * Tests the listSavingThrowsOfSpell method when there is a saving throw defined, with no modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListSavingThrowsOfSpell_ModifiedSavingThrow_WithSpellValidUnitTargetRecords () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitSkillEx attr = new UnitSkillEx ();
		attr.getUnitSkillDescription ().add (createLanguageText (Language.ENGLISH, "Resistance"));
		when (db.findUnitSkill (eq ("UA01"), anyString ())).thenReturn (attr);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellBookSingleSavingThrowModifier ().add (createLanguageText (Language.ENGLISH, "Saves against Resistance at SAVING_THROW_MODIFIER"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
				
		// Spell details
		final Spell spell = new Spell ();
		spell.setCombatBaseDamage (5);
		
		// Spell has some spellValidUnitTarget records because it can only be targetted against creatures from certain magic realms
		// but this doesn't alter its saving throw modifier
		spell.getSpellValidUnitTarget ().add (new SpellValidUnitTarget ());
		spell.getSpellValidUnitTarget ().add (new SpellValidUnitTarget ());
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Saves against Resistance at -5", utils.listSavingThrowsOfSpell (spell));
	}

	/**
	 * Tests the listSavingThrowsOfSpell method when there are different saving throws for different magic realms
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListSavingThrowsOfSpell_MultipleSavingThrow () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitSkillEx attr = new UnitSkillEx ();
		attr.getUnitSkillDescription ().add (createLanguageText (Language.ENGLISH, "Resistance"));
		when (db.findUnitSkill (eq ("UA01"), anyString ())).thenReturn (attr);

		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the language XML
		final HelpScreen helpLang = new HelpScreen ();
		helpLang.getSpellBookMultipleSavingThrowModifiers ().add (createLanguageText (Language.ENGLISH, "Saves against Resistance from SAVING_THROW_MODIFIER_MINIMUM to SAVING_THROW_MODIFIER_MAXIMUM"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setCombatBaseDamage (3);
		
		final SpellValidUnitTarget target1 = new SpellValidUnitTarget ();
		target1.setMagicRealmAdditionalSavingThrowModifier (2);
		spell.getSpellValidUnitTarget ().add (target1);

		final SpellValidUnitTarget target2 = new SpellValidUnitTarget ();
		target2.setMagicRealmAdditionalSavingThrowModifier (3);
		spell.getSpellValidUnitTarget ().add (target2);

		final SpellValidUnitTarget target3 = new SpellValidUnitTarget ();
		spell.getSpellValidUnitTarget ().add (target3);

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setClient (client);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Saves against Resistance from -3 to -6", utils.listSavingThrowsOfSpell (spell));
	}
	
	/**
	 * Tests the findImageForSpell method on a spell that has no defined section (isn't supposed yet)
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_NoSection () throws IOException
	{
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		
		assertNull (utils.findImageForSpell ("SP001", null));
	}

	/**
	 * Tests the findImageForSpell method on a spell that has a section that isn't in the switch statement
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_ImagelessSection () throws IOException
	{
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.RESEARCHABLE);
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		
		assertNull (utils.findImageForSpell ("SP001", null));
	}

	/**
	 * Tests the findImageForSpell method on an overland enchantment, without the player colour mirror
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_OverlandEnchantment_NoPlayer () throws IOException
	{
		// Mock image
		final BufferedImage image = new BufferedImage (2, 2, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 2; x++)
			for (int y = 0; y < 2; y++)
				image.setRGB (x, y, Color.RED.getRGB ());
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("SP001.png")).thenReturn (image);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		spell.setOverlandEnchantmentImageFile ("SP001.png");
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertEquals (1, out.getWidth (null));
		assertEquals (1, out.getHeight (null));
		
		final BufferedImage dest = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = dest.createGraphics ();
		try
		{
			g.drawImage (out, 0, 0, null);
		}
		finally
		{
			g.dispose ();
		}
		assertEquals (Color.RED.getRGB (), dest.getRGB (0, 0));
	}

	/**
	 * Tests the findImageForSpell method on an overland enchantment, without the player colour mirror
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_OverlandEnchantment_WithPlayer () throws IOException
	{
		// Mock image
		final BufferedImage image = new BufferedImage (2, 2, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 2; x++)
			for (int y = 0; y < 2; y++)
				image.setRGB (x, y, Color.RED.getRGB ());
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("SP001.png")).thenReturn (image);
		
		// Mock border from mirror
		final BufferedImage mirror = new BufferedImage (6, 6, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 6; x++)
			for (int y = 0; y < 6; y++)
				mirror.setRGB (x, y, Color.BLUE.getRGB ());
		
		final PlayerColourImageGenerator gen = mock (PlayerColourImageGenerator.class);
		when (gen.getModifiedImage (GraphicsDatabaseConstants.OVERLAND_ENCHANTMENTS_MIRROR, true, null, null, null, 3, null)).thenReturn (mirror);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		spell.setOverlandEnchantmentImageFile ("SP001.png");
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		utils.setPlayerColourImageGenerator (gen);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", 3);
		
		// Check results
		assertEquals (3, out.getWidth (null));
		assertEquals (3, out.getHeight (null));
		
		final BufferedImage dest = new BufferedImage (3, 3, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = dest.createGraphics ();
		try
		{
			g.drawImage (out, 0, 0, null);
		}
		finally
		{
			g.dispose ();
		}
		
		// NB. IMAGE_MIRROR_X_OFFSET and IMAGE_MIRROR_Y_OFFSET are much bigger than our test images, so we get none of the red pixel
		for (int x = 0; x < 3; x++)
			for (int y = 0; y < 3; y++)
				assertEquals (Color.BLUE.getRGB (), dest.getRGB (x, y));
	}

	/**
	 * Tests the findImageForSpell method on a summoning spell that can only summon units with the same image
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_Summoning_One () throws IOException
	{
		// Mock image
		final BufferedImage maleImage = new BufferedImage (2, 2, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 2; x++)
			for (int y = 0; y < 2; y++)
				maleImage.setRGB (x, y, Color.BLUE.getRGB ());
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("Male.png")).thenReturn (maleImage);
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// So this creates units 1,3,5,7,9 all which use the same male summonining image
		for (int n = 1; n < 10; n++)
			if (n % 2 != 0)
			{
				final UnitEx unitDef = new UnitEx ();
				unitDef.setUnitSummonImageFile ("Male.png");
				when (db.findUnit ("UN00" + n, "findImageForSpell")).thenReturn (unitDef);
			}
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		for (int n = 1; n < 10; n++)
			if (n % 2 == 1)
				spell.getSummonedUnit ().add ("UN00" + n);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertEquals (1, out.getWidth (null));
		assertEquals (1, out.getHeight (null));
		
		final BufferedImage dest = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = dest.createGraphics ();
		try
		{
			g.drawImage (out, 0, 0, null);
		}
		finally
		{
			g.dispose ();
		}
		assertEquals (Color.BLUE.getRGB (), dest.getRGB (0, 0));
	}

	/**
	 * Tests the findImageForSpell method on a summoning spell that summon units with two kinds of images
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_Summoning_Two () throws IOException
	{
		// Mock image
		final BufferedImage maleImage = new BufferedImage (2, 2, BufferedImage.TYPE_INT_ARGB);
		final BufferedImage femaleImage = new BufferedImage (2, 2, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 2; x++)
			for (int y = 0; y < 2; y++)
			{
				maleImage.setRGB (x, y, Color.BLUE.getRGB ());
				femaleImage.setRGB (x, y, Color.RED.getRGB ());
			}
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("Male.png")).thenReturn (maleImage);
		when (uiUtils.loadImage ("Female.png")).thenReturn (femaleImage);
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// So this creates units 1,3,5,7,9 all which use the male summonining image, and 2,4,6,8 use the female image
		for (int n = 1; n < 10; n++)
		{
			final UnitEx unitDef = new UnitEx ();
			unitDef.setUnitSummonImageFile ((n % 2 != 0) ? "Male.png" : "Female.png");
			when (db.findUnit ("UN00" + n, "findImageForSpell")).thenReturn (unitDef);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		for (int n = 1; n < 10; n++)
			spell.getSummonedUnit ().add ("UN00" + n);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertEquals (3, out.getWidth (null));
		assertEquals (1, out.getHeight (null));
		
		final BufferedImage dest = new BufferedImage (3, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = dest.createGraphics ();
		try
		{
			g.drawImage (out, 0, 0, null);
		}
		finally
		{
			g.dispose ();
		}
		assertEquals (Color.BLUE.getRGB (), dest.getRGB (0, 0));
		assertEquals (MomUIConstants.TRANSPARENT.getRGB (), dest.getRGB (1, 0));
		assertEquals (Color.RED.getRGB (), dest.getRGB (2, 0));
	}

	/**
	 * Tests the findImageForSpell method on a unit enchantment that can only produce one type of benefit
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_UnitEnchantment_One () throws IOException
	{
		// Mock image
		final BufferedImage effect1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("US050.png")).thenReturn (effect1Image);
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skill = new UnitSkillEx ();
		skill.setUnitSkillImageFile ("US050.png");
		when (db.findUnitSkill ("US050", "findImageForSpell")).thenReturn (skill);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US050");
		spell.getUnitSpellEffect ().add (effect);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertSame (effect1Image, out);
	}

	/**
	 * Tests the findImageForSpell method on a unit enchantment that can produce two types of benefit
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_UnitEnchantment_Two () throws IOException
	{
		// Mock images
		final BufferedImage effect1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		effect1Image.setRGB (0, 0, Color.BLUE.getRGB ());

		final BufferedImage effect2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		effect2Image.setRGB (0, 0, Color.RED.getRGB ());
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("US050.png")).thenReturn (effect1Image);
		when (uiUtils.loadImage ("US051.png")).thenReturn (effect2Image);
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 50; n <= 51; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillImageFile ("US0" + n + ".png");
			when (db.findUnitSkill ("US0" + n, "findImageForSpell")).thenReturn (skill);
		}		
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		for (int n = 50; n <= 51; n++)
		{
			final UnitSpellEffect effect = new UnitSpellEffect ();
			effect.setUnitSkillID ("US0" + n);
			spell.getUnitSpellEffect ().add (effect);
		}
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertEquals (3, out.getWidth (null));
		assertEquals (1, out.getHeight (null));
		
		final BufferedImage dest = new BufferedImage (3, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = dest.createGraphics ();
		try
		{
			g.drawImage (out, 0, 0, null);
		}
		finally
		{
			g.dispose ();
		}
		assertEquals (Color.BLUE.getRGB (), dest.getRGB (0, 0));
		assertEquals (MomUIConstants.TRANSPARENT.getRGB (), dest.getRGB (1, 0));
		assertEquals (Color.RED.getRGB (), dest.getRGB (2, 0));
	}

	/**
	 * Tests the findImageForSpell method on a city enchantment that can only produce one type of benefit
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_CityEnchantment_One () throws IOException
	{
		// Mock image
		final BufferedImage effect1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("CSE050.png")).thenReturn (effect1Image);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);

		final CityViewElement element = new CityViewElement ();
		element.setCityViewAlternativeImageFile ("CSE050.png");
		when (db.findCityViewElementSpellEffect ("CSE050")).thenReturn (element);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		
		spell.getSpellHasCityEffect ().add ("CSE050");
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertSame (effect1Image, out);
	}

	/**
	 * Tests the findImageForSpell method on a city enchantment that can only produce two types of benefit
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_CityEnchantment_Two () throws IOException
	{
		// Mock images
		final BufferedImage effect1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		effect1Image.setRGB (0, 0, Color.BLUE.getRGB ());

		final BufferedImage effect2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		effect2Image.setRGB (0, 0, Color.RED.getRGB ());
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("CSE050.png")).thenReturn (effect1Image);
		when (uiUtils.loadImage ("CSE051.png")).thenReturn (effect2Image);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);

		for (int n = 50; n <= 51; n++)
		{
			final CityViewElement element = new CityViewElement ();
			if (n == 50)
			{
				// Prove alt image gets used in preference
				element.setCityViewImageFile ("ThisGetsIgnored.png");
				element.setCityViewAlternativeImageFile ("CSE050.png");
			}
			else
				element.setCityViewImageFile ("CSE051.png");
			
			when (db.findCityViewElementSpellEffect ("CSE0" + n)).thenReturn (element);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		
		for (int n = 50; n <= 51; n++)
			spell.getSpellHasCityEffect ().add ("CSE0" + n);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertEquals (3, out.getWidth (null));
		assertEquals (1, out.getHeight (null));
		
		final BufferedImage dest = new BufferedImage (3, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = dest.createGraphics ();
		try
		{
			g.drawImage (out, 0, 0, null);
		}
		finally
		{
			g.dispose ();
		}
		assertEquals (Color.BLUE.getRGB (), dest.getRGB (0, 0));
		assertEquals (MomUIConstants.TRANSPARENT.getRGB (), dest.getRGB (1, 0));
		assertEquals (Color.RED.getRGB (), dest.getRGB (2, 0));
	}

	/**
	 * Tests the findImageForSpell method on a city enchantment that can produces a building, and its animated
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_CityEnchantment_Building () throws IOException
	{
		// Mock image
		final BufferedImage buildingFrame1 = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		final BufferedImage buildingFrame2 = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		final BufferedImage buildingFrame3 = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("BL15-frame1.png")).thenReturn (buildingFrame1);
		when (uiUtils.loadImage ("BL15-frame2.png")).thenReturn (buildingFrame2);
		when (uiUtils.loadImage ("BL15-frame3.png")).thenReturn (buildingFrame3);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);

		final AnimationEx anim = new AnimationEx ();
		for (int n = 1; n <= 3; n++)
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("BL15-frame" + n + ".png");
			anim.getFrame ().add (frame);
		}
		
		when (db.findAnimation ("BL15-anim", "findImageForSpell")).thenReturn (anim);

		final CityViewElement element = new CityViewElement ();
		element.setCityViewAnimation ("BL15-anim");
		when (db.findCityViewElementBuilding ("BL15", "findImageForSpell")).thenReturn (element);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		spell.setBuildingID ("BL15");		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertSame (buildingFrame1, out);
	}

	/**
	 * Tests the findImageForSpell method on a combat enchantment that can only produce one type of benefit
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_CombatEnchantment_One () throws IOException
	{
		// Mock image
		final BufferedImage effect1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("CAE050.png")).thenReturn (effect1Image);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);

		final CombatAreaEffect cae = new CombatAreaEffect ();
		cae.setCombatAreaEffectImageFile ("CAE050.png");
		when (db.findCombatAreaEffect ("CAE050", "findImageForSpell")).thenReturn (cae);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		spell.getSpellHasCombatEffect ().add ("CAE050");
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertSame (effect1Image, out);
	}

	/**
	 * Tests the findImageForSpell method on a combat enchantment that can only produce two types of benefit
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_CombatEnchantment_Two () throws IOException
	{
		// Mock images
		final BufferedImage effect1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		effect1Image.setRGB (0, 0, Color.BLUE.getRGB ());

		final BufferedImage effect2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		effect2Image.setRGB (0, 0, Color.RED.getRGB ());
		
		final NdgUIUtils uiUtils = mock (NdgUIUtils.class);
		when (uiUtils.loadImage ("CAE050.png")).thenReturn (effect1Image);
		when (uiUtils.loadImage ("CAE051.png")).thenReturn (effect2Image);
		
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);

		for (int n = 50; n <= 51; n++)
		{
			final CombatAreaEffect cae = new CombatAreaEffect ();
			cae.setCombatAreaEffectImageFile ("CAE0" + n + ".png");
			when (db.findCombatAreaEffect ("CAE0" + n, "findImageForSpell")).thenReturn (cae);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		for (int n = 50; n <= 51; n++)
			spell.getSpellHasCombatEffect ().add ("CAE0" + n);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setUtils (uiUtils);
		
		// Run method
		final Image out = utils.findImageForSpell ("SP001", null);
		
		// Check results
		assertEquals (3, out.getWidth (null));
		assertEquals (1, out.getHeight (null));
		
		final BufferedImage dest = new BufferedImage (3, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics g = dest.createGraphics ();
		try
		{
			g.drawImage (out, 0, 0, null);
		}
		finally
		{
			g.dispose ();
		}
		assertEquals (Color.BLUE.getRGB (), dest.getRGB (0, 0));
		assertEquals (MomUIConstants.TRANSPARENT.getRGB (), dest.getRGB (1, 0));
		assertEquals (Color.RED.getRGB (), dest.getRGB (2, 0));
	}
}
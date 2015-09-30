package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.CombatAreaEffectGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.SpellGfx;
import momime.client.graphics.database.UnitGfx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.UnitMagicRealmLang;
import momime.client.language.database.UnitSkillLang;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellHasCityEffect;
import momime.common.database.SpellHasCombatEffect;
import momime.common.database.SpellUpkeep;
import momime.common.database.SpellValidUnitTarget;
import momime.common.database.SummonedUnit;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the SpellClientUtilsImpl class
 */
public final class TestSpellClientUtilsImpl
{
	/**
	 * Tests the listUpkeepsOfSpell method when the spell has no upkeeps
	 */
	@Test
	public final void testListUpkeepsOfSpell_None ()
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
	 */
	@Test
	public final void testListUpkeepsOfSpell_One ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepWithoutChanneler")).thenReturn ("UPKEEP_VALUE PRODUCTION_TYPE");
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepWithChanneler")).thenReturn ("HALF_UPKEEP_VALUE PRODUCTION_TYPE (reduced from UPKEEP_VALUE PRODUCTION_TYPE)");
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepFixed")).thenReturn ("Upkeep: UPKEEP_LIST per turn");
		
		final ProductionTypeLang mana = new ProductionTypeLang ();
		mana.setProductionTypeDescription ("Mana");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (mana);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Spell
		final SpellUpkeep upkeep = new SpellUpkeep ();
		upkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		upkeep.setUpkeepValue (5);
		
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
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Upkeep: 5 Mana per turn", utils.listUpkeepsOfSpell (spell, picks));
	}

	/**
	 * Tests the listUpkeepsOfSpell method when the spell has a single normal upkeep, but have the channeler retort
	 */
	@Test
	public final void testListUpkeepsOfSpell_Channeler ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepWithoutChanneler")).thenReturn ("UPKEEP_VALUE PRODUCTION_TYPE");
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepWithChanneler")).thenReturn ("HALF_UPKEEP_VALUE PRODUCTION_TYPE (reduced from UPKEEP_VALUE PRODUCTION_TYPE)");
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepFixed")).thenReturn ("Upkeep: UPKEEP_LIST per turn");
		
		final ProductionTypeLang mana = new ProductionTypeLang ();
		mana.setProductionTypeDescription ("Mana");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (mana);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Spell
		final SpellUpkeep upkeep = new SpellUpkeep ();
		upkeep.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		upkeep.setUpkeepValue (5);
		
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
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Upkeep: 2½ Mana (reduced from 5 Mana) per turn", utils.listUpkeepsOfSpell (spell, picks));
	}

	/**
	 * Tests the listUpkeepsOfSpell method when the spell has a more than one upkeep
	 */
	@Test
	public final void testListUpkeepsOfSpell_Two ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepWithoutChanneler")).thenReturn ("UPKEEP_VALUE PRODUCTION_TYPE");
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepWithChanneler")).thenReturn ("HALF_UPKEEP_VALUE PRODUCTION_TYPE (reduced from UPKEEP_VALUE PRODUCTION_TYPE)");
		when (lang.findCategoryEntry ("frmHelp", "SpellUpkeepFixed")).thenReturn ("Upkeep: UPKEEP_LIST per turn");
		
		final ProductionTypeLang mana = new ProductionTypeLang ();
		mana.setProductionTypeDescription ("Mana");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (mana);

		final ProductionTypeLang gold = new ProductionTypeLang ();
		gold.setProductionTypeDescription ("Gold");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)).thenReturn (gold);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Spell
		final SpellUpkeep upkeep1 = new SpellUpkeep ();
		upkeep1.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		upkeep1.setUpkeepValue (5);

		final SpellUpkeep upkeep2 = new SpellUpkeep ();
		upkeep2.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		upkeep2.setUpkeepValue (2);
		
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
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Upkeep: 5 Mana, 2 Gold per turn", utils.listUpkeepsOfSpell (spell, picks));
	}
	
	/**
	 * Tests the listValidMagicRealmLifeformTypeTargetsOfSpell method
	 */
	@Test
	public final void testListValidMagicRealmLifeformTypeTargetsOfSpell ()
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		
		final UnitMagicRealmLang magicRealm1 = new UnitMagicRealmLang ();
		magicRealm1.setUnitMagicRealmPlural ("Chaos units");
		when (lang.findUnitMagicRealm ("LT01")).thenReturn (magicRealm1);

		final UnitMagicRealmLang magicRealm3 = new UnitMagicRealmLang ();
		magicRealm3.setUnitMagicRealmPlural ("Death units");
		when (lang.findUnitMagicRealm ("LT03")).thenReturn (magicRealm3);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Spell
		final Spell spell = new Spell ();
		
		for (int n = 1; n <= 3; n++)
		{
			final SpellValidUnitTarget target = new SpellValidUnitTarget ();
			target.setTargetMagicRealmID ("LT0" + n);
			
			spell.getSpellValidUnitTarget ().add (target);
		}

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		
		// Run method
		assertEquals (System.lineSeparator () + "Chaos units" + System.lineSeparator () + "LT02" + System.lineSeparator () + "Death units",
			utils.listValidMagicRealmLifeformTypeTargetsOfSpell (spell));
	}
	
	/**
	 * Tests the listSavingThrowsOfSpell method when there are no ValidUnitTarget records at all
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 */
	@Test
	public final void testListSavingThrowsOfSpell_None () throws MomException
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellBookNoSavingThrow")).thenReturn ("None");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
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
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 */
	@Test
	public final void testListSavingThrowsOfSpell_LimitedTargets () throws MomException
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellBookNoSavingThrow")).thenReturn ("None");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
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
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 */
	@Test
	public final void testListSavingThrowsOfSpell_BasicSavingThrow () throws MomException
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellBookNoSavingThrowModifier")).thenReturn ("Saves against UNIT_SKILL");
		
		final UnitSkillLang attr = new UnitSkillLang ();
		attr.setUnitSkillDescription ("Resistance");
		when (lang.findUnitSkill ("UA01")).thenReturn (attr);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Spell details
		final Spell spell = new Spell ();
		
		final SpellValidUnitTarget target = new SpellValidUnitTarget ();
		target.setSavingThrowSkillID ("UA01");
		spell.getSpellValidUnitTarget ().add (target);

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		
		// Run method
		assertEquals ("Saves against Resistance", utils.listSavingThrowsOfSpell (spell));
	}
	
	/**
	 * Tests the listSavingThrowsOfSpell method when there is a saving throw defined, with no modifier
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 */
	@Test
	public final void testListSavingThrowsOfSpell_ModifiedSavingThrow () throws MomException
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellBookSingleSavingThrowModifier")).thenReturn ("Saves against UNIT_SKILL at SAVING_THROW_MODIFIER");
		
		final UnitSkillLang attr = new UnitSkillLang ();
		attr.setUnitSkillDescription ("Resistance");
		when (lang.findUnitSkill ("UA01")).thenReturn (attr);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Spell details
		final Spell spell = new Spell ();
		
		final SpellValidUnitTarget target = new SpellValidUnitTarget ();
		target.setSavingThrowSkillID ("UA01");
		target.setSavingThrowModifier (5);
		spell.getSpellValidUnitTarget ().add (target);

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Saves against Resistance at +5", utils.listSavingThrowsOfSpell (spell));
	}

	/**
	 * Tests the listSavingThrowsOfSpell method when there are different saving throws for different magic realms
	 * @throws MomException If there are multiple saving throws listed, but against different unit attributes
	 */
	@Test
	public final void testListSavingThrowsOfSpell_MultipleSavingThrow () throws MomException
	{
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "SpellBookMultipleSavingThrowModifiers")).thenReturn ("Saves against UNIT_SKILL from SAVING_THROW_MODIFIER_MINIMUM to SAVING_THROW_MODIFIER_MAXIMUM");
		
		final UnitSkillLang attr = new UnitSkillLang ();
		attr.setUnitSkillDescription ("Resistance");
		when (lang.findUnitSkill ("UA01")).thenReturn (attr);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Spell details
		final Spell spell = new Spell ();
		
		final SpellValidUnitTarget target1 = new SpellValidUnitTarget ();
		target1.setSavingThrowSkillID ("UA01");
		target1.setSavingThrowModifier (5);
		spell.getSpellValidUnitTarget ().add (target1);

		final SpellValidUnitTarget target2 = new SpellValidUnitTarget ();
		target2.setSavingThrowSkillID ("UA01");
		target2.setSavingThrowModifier (6);
		spell.getSpellValidUnitTarget ().add (target2);

		final SpellValidUnitTarget target3 = new SpellValidUnitTarget ();
		target3.setSavingThrowSkillID ("UA01");
		target3.setSavingThrowModifier (3);
		spell.getSpellValidUnitTarget ().add (target3);

		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setLanguageHolder (langHolder);
		utils.setTextUtils (new TextUtilsImpl ());
		
		// Run method
		assertEquals ("Saves against Resistance from +3 to +6", utils.listSavingThrowsOfSpell (spell));
	}
	
	/**
	 * Tests the findImageForSpell method on a spell that has no defined section (isn't supposed yet)
	 * @throws IOException If a necessary record or image is not found
	 */
	@Test
	public final void testFindImageForSpell_NoSection () throws IOException
	{
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
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
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
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
		
		// Mock entries from graphics DB
		final SpellGfx spellGfx = new SpellGfx ();
		spellGfx.setOverlandEnchantmentImageFile ("SP001.png");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findSpell ("SP001", "findImageForSpell")).thenReturn (spellGfx);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final SpellGfx spellGfx = new SpellGfx ();
		spellGfx.setOverlandEnchantmentImageFile ("SP001.png");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findSpell ("SP001", "findImageForSpell")).thenReturn (spellGfx);
		
		// Mock border from mirror
		final BufferedImage mirror = new BufferedImage (6, 6, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 6; x++)
			for (int y = 0; y < 6; y++)
				mirror.setRGB (x, y, Color.BLUE.getRGB ());
		
		final PlayerColourImageGenerator gen = mock (PlayerColourImageGenerator.class);
		when (gen.getOverlandEnchantmentMirror (3)).thenReturn (mirror);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);

		// So this creates units 1,3,5,7,9 all which use the same male summonining image
		for (int n = 1; n < 10; n++)
			if (n % 2 == 1)
			{
				final UnitGfx unitGfx = new UnitGfx ();
				unitGfx.setUnitSummonImageFile ("Male.png");
				when (gfx.findUnit ("UN00" + n, "findImageForSpell")).thenReturn (unitGfx);
			}
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		for (int n = 1; n < 10; n++)
			if (n % 2 == 1)
			{
				final SummonedUnit summoned = new SummonedUnit ();
				summoned.setSummonedUnitID ("UN00" + n);
				spell.getSummonedUnit ().add (summoned);
			}
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);

		// So this creates units 1,3,5,7,9 all which use the male summonining image, and 2,4,6,8 use the female image
		for (int n = 1; n < 10; n++)
		{
			final UnitGfx unitGfx = new UnitGfx ();
			unitGfx.setUnitSummonImageFile ((n % 2 == 1) ? "Male.png" : "Female.png");
			when (gfx.findUnit ("UN00" + n, "findImageForSpell")).thenReturn (unitGfx);
		}
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		for (int n = 1; n < 10; n++)
		{
			final SummonedUnit summoned = new SummonedUnit ();
			summoned.setSummonedUnitID ("UN00" + n);
			spell.getSummonedUnit ().add (summoned);
		}
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final UnitSkillGfx skill = new UnitSkillGfx ();
		skill.setUnitSkillImageFile ("US050.png");
		when (gfx.findUnitSkill ("US050", "findImageForSpell")).thenReturn (skill);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		final UnitSkillAndValue effect = new UnitSkillAndValue ();
		effect.setUnitSkillID ("US050");
		spell.getUnitSpellEffect ().add (effect);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		for (int n = 50; n <= 51; n++)
		{
			final UnitSkillGfx skill = new UnitSkillGfx ();
			skill.setUnitSkillImageFile ("US0" + n + ".png");
			when (gfx.findUnitSkill ("US0" + n, "findImageForSpell")).thenReturn (skill);
		}
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		
		for (int n = 50; n <= 51; n++)
		{
			final UnitSkillAndValue effect = new UnitSkillAndValue ();
			effect.setUnitSkillID ("US0" + n);
			spell.getUnitSpellEffect ().add (effect);
		}
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final CityViewElementGfx element = new CityViewElementGfx ();
		element.setCityViewAlternativeImageFile ("CSE050.png");
		when (gfx.findCitySpellEffect ("CSE050", "findImageForSpell")).thenReturn (element);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		
		final SpellHasCityEffect effect = new SpellHasCityEffect ();
		effect.setCitySpellEffectID ("CSE050");
		spell.getSpellHasCityEffect ().add (effect);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		for (int n = 50; n <= 51; n++)
		{
			final CityViewElementGfx element = new CityViewElementGfx ();
			if (n == 50)
			{
				// Prove alt image gets used in preference
				element.setCityViewImageFile ("ThisGetsIgnored.png");
				element.setCityViewAlternativeImageFile ("CSE050.png");
			}
			else
				element.setCityViewImageFile ("CSE051.png");
			
			when (gfx.findCitySpellEffect ("CSE0" + n, "findImageForSpell")).thenReturn (element);
		}
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		
		for (int n = 50; n <= 51; n++)
		{
			final SpellHasCityEffect effect = new SpellHasCityEffect ();
			effect.setCitySpellEffectID ("CSE0" + n);
			spell.getSpellHasCityEffect ().add (effect);
		}
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final AnimationGfx anim = new AnimationGfx ();
		for (int n = 1; n <= 3; n++)
			anim.getFrame ().add ("BL15-frame" + n + ".png");
		
		when (gfx.findAnimation ("BL15-anim", "findImageForSpell")).thenReturn (anim);

		final CityViewElementGfx element = new CityViewElementGfx ();
		element.setCityViewAnimation ("BL15-anim");
		when (gfx.findBuilding ("BL15", "findImageForSpell")).thenReturn (element);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
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
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final CombatAreaEffectGfx cae = new CombatAreaEffectGfx ();
		cae.setCombatAreaEffectImageFile ("CAE050.png");
		when (gfx.findCombatAreaEffect ("CAE050", "findImageForSpell")).thenReturn (cae);
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		
		final SpellHasCombatEffect effect = new SpellHasCombatEffect ();
		effect.setCombatAreaEffectID ("CAE050");
		spell.getSpellHasCombatEffect ().add (effect);
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		for (int n = 50; n <= 51; n++)
		{
			final CombatAreaEffectGfx cae = new CombatAreaEffectGfx ();
			cae.setCombatAreaEffectImageFile ("CAE0" + n + ".png");
			when (gfx.findCombatAreaEffect ("CAE0" + n, "findImageForSpell")).thenReturn (cae);
		}
		
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Spell details
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		for (int n = 50; n <= 51; n++)
		{
			final SpellHasCombatEffect effect = new SpellHasCombatEffect ();
			effect.setCombatAreaEffectID ("CAE0" + n);
			spell.getSpellHasCombatEffect ().add (effect);
		}
		
		when (db.findSpell ("SP001", "findImageForSpell")).thenReturn (spell);
		
		// Set up object to test
		final SpellClientUtilsImpl utils = new SpellClientUtilsImpl ();
		utils.setClient (client);
		utils.setGraphicsDB (gfx);
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
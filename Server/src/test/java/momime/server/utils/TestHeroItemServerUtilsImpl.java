package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.PickAndQuantity;
import momime.common.database.UnitSetting;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
import momime.server.database.HeroItemBonusSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;

/**
 * Tests the HeroItemServerUtilsImpl class
 */
public final class TestHeroItemServerUtilsImpl
{
	/**
	 * Tests the validateHeroItem on a trivial item with no bonuses at all
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_Trivial () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();		 
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertNull (utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem on a simple item with a valid bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_NormalBonus () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		for (int n = 1; n <= 1; n++)
		{
			final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
			bonus.setBonusCraftingCost (100 * n);
			bonus.setHeroItemBonusDescription ("B" + n);
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01"})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertNull (utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem exceeding the maximum number of bonuses allowed
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedBonusCount () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
			bonus.setBonusCraftingCost (100 * n);
			bonus.setHeroItemBonusDescription ("B" + n);
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemBonuses (1);

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Hero items may be imbued with a maximum of 1 bonuses", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem trying to pick the same bonus twice
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SameBonusTwice () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		for (int n = 1; n <= 1; n++)
		{
			final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
			bonus.setBonusCraftingCost (100 * n);
			bonus.setHeroItemBonusDescription ("B" + n);
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB01"})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Bonus B1 was chosen more than once", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem exceeding the maximum cost per bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedMaximumBonusCost () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
			bonus.setBonusCraftingCost (100 * n);
			bonus.setHeroItemBonusDescription ("B" + n);
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (100);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Bonus B2 exceeds the maximum cost of 100 per bonus", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem trying to pick a "special" bonus with no cost specified (i.e. spell charges), when we do have a maximum cost set
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedUnspecifiedBonusCost () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		for (int n = 1; n <= 1; n++)
		{
			final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
			bonus.setHeroItemBonusDescription ("B" + n);
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (100);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01"})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Bonus B1 exceeds the maximum cost of 100 per bonus", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem that requires a certain number of books that we don't have
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_DontHavePrerequisite () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
			bonus.setBonusCraftingCost (100 * n);
			bonus.setHeroItemBonusDescription ("B" + n);
			
			final PickAndQuantity prereq = new PickAndQuantity ();
			prereq.setPickID ("MB01");
			prereq.setQuantity (n);
			bonus.getHeroItemBonusPrerequisite ().add (prereq);
			
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, pub, null, null, null);
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (pub.getPick (), "MB01")).thenReturn (1);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		assertEquals ("Bonus B2 requires at least 2 picks in magic realm MB01", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem where two different bonuses both give a bonus to the same stat (pick Attack +1 and Attack +2)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SameStatTwice () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonusSvr bonus = new HeroItemBonusSvr ();
			bonus.setBonusCraftingCost (100 * n);
			bonus.setHeroItemBonusDescription ("B" + n);
			
			final HeroItemBonusStat stat = new HeroItemBonusStat ();
			stat.setUnitSkillID ("UA01");
			stat.setUnitSkillValue (n);
			bonus.getHeroItemBonusStat ().add (stat);
			
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			heroItem.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("More than one bonus was selected that gives a bonus to stat UA01", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem when we specify a spell ID without picking the Spell Charges bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SpellButDidntPick () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Hero items shouldn't specify a spell if the spell charges bonus was not picked", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem when we specify a spell count without picking the Spell Charges bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SpellCountButDidntPick () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellChargeCount (4);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Hero items shouldn't specify a number of spells if the spell charges bonus was not picked", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem when we pick spell charges, but not which spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_DidntSpecifySpell () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final HeroItemBonusSvr scDef = new HeroItemBonusSvr ();
		scDef.setHeroItemBonusDescription ("SC");
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		
		final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
		bonus.setHeroItemBonusID (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		heroItem.getHeroItemChosenBonus ().add (bonus);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Hero items must specify a spell if the spell charges bonus was picked", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem when we pick spell charges, but not how many charges
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_DidntSpecifySpellCount () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final HeroItemBonusSvr scDef = new HeroItemBonusSvr ();
		scDef.setHeroItemBonusDescription ("SC");
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		
		final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
		bonus.setHeroItemBonusID (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		heroItem.getHeroItemChosenBonus ().add (bonus);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Hero items must specify a number of spells if the spell charges bonus was picked", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem when we try to exceed the maximum allowed number of spell charges
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedSpellCount () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final HeroItemBonusSvr scDef = new HeroItemBonusSvr ();
		scDef.setHeroItemBonusDescription ("SC");
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (3);

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
		bonus.setHeroItemBonusID (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		heroItem.getHeroItemChosenBonus ().add (bonus);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		
		// Run method
		assertEquals ("Number of spell charges on the item was over the allowed maximum", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem when we try to imbue spell charges of a spell that we don't know
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_UnresearchedSpell () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final HeroItemBonusSvr scDef = new HeroItemBonusSvr ();
		scDef.setHeroItemBonusDescription ("SC");
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		final SpellSvr spellChargeDef = new SpellSvr ();
		when (db.findSpell ("SP001", "validateHeroItem")).thenReturn (spellChargeDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (4);

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.RESEARCHABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
		bonus.setHeroItemBonusID (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		heroItem.getHeroItemChosenBonus ().add (bonus);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertEquals ("You tried to imbue a spell that you haven't yet researched into a hero item", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem when we try to imbue spell charges of a non-combat spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_OverlandSpell () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final HeroItemBonusSvr scDef = new HeroItemBonusSvr ();
		scDef.setHeroItemBonusDescription ("SC");
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		final SpellSvr spellChargeDef = new SpellSvr ();
		when (db.findSpell ("SP001", "validateHeroItem")).thenReturn (spellChargeDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (4);

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		when (spellUtils.spellCanBeCastIn (spellChargeDef, SpellCastType.COMBAT)).thenReturn (false);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
		bonus.setHeroItemBonusID (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		heroItem.getHeroItemChosenBonus ().add (bonus);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertEquals ("You can only imbue combat spells into hero items", utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}

	/**
	 * Tests the validateHeroItem imbuing valid spell charges
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SpellCharges () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		final HeroItemBonusSvr scDef = new HeroItemBonusSvr ();
		scDef.setHeroItemBonusDescription ("SC");
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		final SpellSvr spellChargeDef = new SpellSvr ();
		when (db.findSpell ("SP001", "validateHeroItem")).thenReturn (spellChargeDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (4);

		// Crafting spell
		final SpellSvr spell = new SpellSvr ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		// Research status
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		when (spellUtils.spellCanBeCastIn (spellChargeDef, SpellCastType.COMBAT)).thenReturn (true);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
		bonus.setHeroItemBonusID (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		heroItem.getHeroItemChosenBonus ().add (bonus);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		
		// Run method
		assertNull (utils.validateHeroItem (player, spell, heroItem, unitSettings, db));
	}
}
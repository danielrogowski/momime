package momime.server.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemBonusStat;
import momime.common.database.PickAndQuantity;
import momime.common.database.Spell;
import momime.common.database.UnitSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the HeroItemServerUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestHeroItemServerUtilsImpl
{
	/**
	 * Tests the validateHeroItem on a trivial item with no bonuses at all
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_Trivial () throws Exception
	{
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();		 
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertNull (utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem on a simple item with a valid bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_NormalBonus () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 1; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setBonusCraftingCost (100 * n);
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01"})
			heroItem.getHeroItemChosenBonus ().add (bonusID);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertNull (utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem exceeding the maximum number of bonuses allowed
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedBonusCount () throws Exception
	{
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setBonusCraftingCost (100 * n);
		}
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Item settings
		unitSettings.setMaxHeroItemBonuses (1);

		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
			heroItem.getHeroItemChosenBonus ().add (bonusID);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Hero items may be imbued with a maximum of 1 bonuses", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem trying to pick the same bonus twice
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SameBonusTwice () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 1; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setHeroItemBonusID ("IB0" + n);
			bonus.setBonusCraftingCost (100 * n);
			when (db.findHeroItemBonus (bonus.getHeroItemBonusID (), "validateHeroItem")).thenReturn (bonus);
		}
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB01"})
			heroItem.getHeroItemChosenBonus ().add (bonusID);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Bonus IB01 was chosen more than once", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem exceeding the maximum cost per bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedMaximumBonusCost () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setHeroItemBonusID ("IB0" + n);
			bonus.setBonusCraftingCost (100 * n);
			when (db.findHeroItemBonus (bonus.getHeroItemBonusID (), "validateHeroItem")).thenReturn (bonus);
		}
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (100);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
			heroItem.getHeroItemChosenBonus ().add (bonusID);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Bonus IB02 exceeds the maximum cost of 100 per bonus", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem trying to pick a "special" bonus with no cost specified (i.e. spell charges), when we do have a maximum cost set
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedUnspecifiedBonusCost () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 1; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setHeroItemBonusID ("IB0" + n);
			when (db.findHeroItemBonus (bonus.getHeroItemBonusID (), "validateHeroItem")).thenReturn (bonus);
		}
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (100);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01"})
			heroItem.getHeroItemChosenBonus ().add (bonusID);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Bonus IB01 exceeds the maximum cost of 100 per bonus", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem that requires a certain number of books that we don't have
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_DontHavePrerequisite () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setHeroItemBonusID ("IB0" + n);
			bonus.setBonusCraftingCost (100 * n);
			
			final PickAndQuantity prereq = new PickAndQuantity ();
			prereq.setPickID ("MB01");
			prereq.setQuantity (n);
			bonus.getHeroItemBonusPrerequisite ().add (prereq);
			
			when (db.findHeroItemBonus (bonus.getHeroItemBonusID (), "validateHeroItem")).thenReturn (bonus);
		}
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (wizardDetails.getPick (), "MB01")).thenReturn (1);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
			heroItem.getHeroItemChosenBonus ().add (bonusID);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setPlayerPickUtils (playerPickUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Bonus IB02 requires at least 2 picks in magic realm MB01", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem where two different bonuses both give a bonus to the same stat (pick Attack +1 and Attack +2)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SameStatTwice () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		for (int n = 1; n <= 2; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setBonusCraftingCost (100 * n);
			
			final HeroItemBonusStat stat = new HeroItemBonusStat ();
			stat.setUnitSkillID ("UA01");
			stat.setUnitSkillValue (n);
			bonus.getHeroItemBonusStat ().add (stat);
			
			when (db.findHeroItemBonus ("IB0" + n, "validateHeroItem")).thenReturn (bonus);
		}
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (200);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		for (final String bonusID : new String [] {"IB01", "IB02"})
			heroItem.getHeroItemChosenBonus ().add (bonusID);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("More than one bonus was selected that gives a bonus to stat UA01", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem when we specify a spell ID without picking the Spell Charges bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SpellButDidntPick () throws Exception
	{
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Hero items shouldn't specify a spell if the spell charges bonus was not picked", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem when we specify a spell count without picking the Spell Charges bonus
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SpellCountButDidntPick () throws Exception
	{
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellChargeCount (4);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Hero items shouldn't specify a number of spells if the spell charges bonus was not picked", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem when we pick spell charges, but not which spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_DidntSpecifySpell () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemBonus scDef = new HeroItemBonus ();
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		
		heroItem.getHeroItemChosenBonus ().add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Hero items must specify a spell if the spell charges bonus was picked", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem when we pick spell charges, but not how many charges
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_DidntSpecifySpellCount () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemBonus scDef = new HeroItemBonus ();
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		// Session description
		final UnitSetting unitSettings = new UnitSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		
		heroItem.getHeroItemChosenBonus ().add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Hero items must specify a number of spells if the spell charges bonus was picked", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem when we try to exceed the maximum allowed number of spell charges
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_ExceedSpellCount () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemBonus scDef = new HeroItemBonus ();
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (3);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
		spell.setHeroItemBonusMaximumCraftingCost (0);
		
		// Casting player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (1);
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, null, null, null);
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		heroItem.getHeroItemChosenBonus ().add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("Number of spell charges on the item was over the allowed maximum", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem when we try to imbue spell charges of a spell that we don't know
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_UnresearchedSpell () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemBonus scDef = new HeroItemBonus ();
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		final Spell spellChargeDef = new Spell ();
		when (db.findSpell ("SP001", "validateHeroItem")).thenReturn (spellChargeDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (4);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
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
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		heroItem.getHeroItemChosenBonus ().add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("You tried to imbue a spell that you haven't yet researched into a hero item", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem when we try to imbue spell charges of a non-combat spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_OverlandSpell () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemBonus scDef = new HeroItemBonus ();
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		final Spell spellChargeDef = new Spell ();
		when (db.findSpell ("SP001", "validateHeroItem")).thenReturn (spellChargeDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (4);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
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
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		heroItem.getHeroItemChosenBonus ().add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Run method
		assertEquals ("You can only imbue combat spells into hero items", utils.validateHeroItem (player, spell, heroItem, mom));
	}

	/**
	 * Tests the validateHeroItem imbuing valid spell charges
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testValidateHeroItem_SpellCharges () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemBonus scDef = new HeroItemBonus ();
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "validateHeroItem")).thenReturn (scDef);
		
		final Spell spellChargeDef = new Spell ();
		when (db.findSpell ("SP001", "validateHeroItem")).thenReturn (spellChargeDef);
		
		// Item settings
		final UnitSetting unitSettings = new UnitSetting ();
		unitSettings.setMaxHeroItemSpellCharges (4);

		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSettings);
		
		// Crafting spell
		final Spell spell = new Spell ();
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
		
		// Casting wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();

		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (mem);
		
		final KnownWizardDetails knownWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), pd.getPlayerID (), "validateHeroItem")).thenReturn (knownWizard);
		
		// Item we want to create
		final HeroItem heroItem = new HeroItem ();
		heroItem.setSpellID ("SP001");
		heroItem.setSpellChargeCount (4);
		
		heroItem.getHeroItemChosenBonus ().add (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		
		// Set up object to test
		final HeroItemServerUtilsImpl utils = new HeroItemServerUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKnownWizardUtils (knownWizardUtils);

		// Run method
		assertNull (utils.validateHeroItem (player, spell, heroItem, mom));
	}
}
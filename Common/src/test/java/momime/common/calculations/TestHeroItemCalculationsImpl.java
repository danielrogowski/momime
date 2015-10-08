package momime.common.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemType;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the HeroItemCalculationsImpl class
 */
public final class TestHeroItemCalculationsImpl
{
	/**
	 * Tests the calculateCraftingCost method
	 * @throws RecordNotFoundException If the item type, one of the bonuses or spell charges can't be found in the XML
	 */
	@Test
	public final void testCalculateCraftingCost () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final HeroItemType itemType = new HeroItemType ();
		itemType.setBaseCraftingCost (100);
		itemType.setItemBonusCraftingCostMultiplier (2);
		when (db.findHeroItemType ("IT01", "calculateCraftingCost")).thenReturn (itemType);
		
		final Spell spell = new Spell ();
		spell.setCombatCastingCost (30);
		when (db.findSpell ("SP001", "calculateCraftingCost")).thenReturn (spell);
		
		final HeroItemBonus bonus1 = new HeroItemBonus ();
		bonus1.setCraftingCostMultiplierApplies (false);
		bonus1.setBonusCraftingCost (6);
		when (db.findHeroItemBonus ("IB01", "calculateCraftingCost")).thenReturn (bonus1);

		final HeroItemBonus bonus2 = new HeroItemBonus ();
		bonus2.setCraftingCostMultiplierApplies (false);
		bonus2.setBonusCraftingCost (8);
		when (db.findHeroItemBonus ("IB02", "calculateCraftingCost")).thenReturn (bonus2);
		
		final HeroItemBonus bonus3 = new HeroItemBonus ();
		bonus3.setCraftingCostMultiplierApplies (true);
		bonus3.setBonusCraftingCost (13);
		when (db.findHeroItemBonus ("IB03", "calculateCraftingCost")).thenReturn (bonus3);
		
		final HeroItemBonus spellCharges = new HeroItemBonus ();
		spellCharges.setCraftingCostMultiplierApplies (false);
		when (db.findHeroItemBonus (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES, "calculateCraftingCost")).thenReturn (spellCharges);
		
		// Set up sample item
		final HeroItem item = new HeroItem ();
		item.setHeroItemTypeID ("IT01");
		item.setSpellID ("SP001");
		item.setSpellChargeCount (3);
		
		for (int n = 0; n <= 3; n++)
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID ((n == 0) ? CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES : "IB0" + n);
			item.getHeroItemChosenBonus ().add (bonus);
		}
		
		// Set up object to test
		final HeroItemCalculationsImpl calc = new HeroItemCalculationsImpl ();
		
		// Run method
		assertEquals (100 + (30 * 20 * 3) + 6 + 8 + (13 * 2), calc.calculateCraftingCost (item, db));
	}
	
	/**
	 * Tests the haveRequiredBooksForBonus method
	 * @throws RecordNotFoundException If the bonuses can't be found in the XML 
	 */
	@Test
	public final void testHaveRequiredBooksForBonus () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final HeroItemBonus bonus = new HeroItemBonus ();
		when (db.findHeroItemBonus ("IB01", "haveRequiredBooksForBonus")).thenReturn (bonus);
		
		// Picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		final PlayerPickUtils playerPickUtils= mock (PlayerPickUtils.class);
		
		// Set up object to test
		final HeroItemCalculationsImpl calc = new HeroItemCalculationsImpl ();
		calc.setPlayerPickUtils (playerPickUtils);
		
		// Test bonus with no requirements
		assertTrue (calc.haveRequiredBooksForBonus ("IB01", picks, db));
		
		// Need 2 of a book
		final PickAndQuantity prereq1 = new PickAndQuantity ();
		prereq1.setPickID ("MB01");
		prereq1.setQuantity (2);
		bonus.getHeroItemBonusPrerequisite ().add (prereq1);

		assertFalse (calc.haveRequiredBooksForBonus ("IB01", picks, db));
		
		// We have 1 of it, and 2 of something else
		when (playerPickUtils.getQuantityOfPick (picks, "MB01")).thenReturn (1);
		when (playerPickUtils.getQuantityOfPick (picks, "MB02")).thenReturn (2);
		
		assertFalse (calc.haveRequiredBooksForBonus ("IB01", picks, db));
		
		// Now we have enough - more than enough
		when (playerPickUtils.getQuantityOfPick (picks, "MB01")).thenReturn (3);
		assertTrue (calc.haveRequiredBooksForBonus ("IB01", picks, db));
		
		// 2 requirements for the same book are fine
		final PickAndQuantity prereq2 = new PickAndQuantity ();
		prereq2.setPickID ("MB01");
		prereq2.setQuantity (3);
		bonus.getHeroItemBonusPrerequisite ().add (prereq2);

		assertTrue (calc.haveRequiredBooksForBonus ("IB01", picks, db));
		
		// Add requirement for different book
		final PickAndQuantity prereq3 = new PickAndQuantity ();
		prereq3.setPickID ("MB02");
		prereq3.setQuantity (4);
		bonus.getHeroItemBonusPrerequisite ().add (prereq3);

		assertFalse (calc.haveRequiredBooksForBonus ("IB01", picks, db));
		
		// Add the second books
		when (playerPickUtils.getQuantityOfPick (picks, "MB02")).thenReturn (5);
		assertTrue (calc.haveRequiredBooksForBonus ("IB01", picks, db));
	}
	
	/**
	 * Tests the haveRequiredBooksForItem method
	 * @throws RecordNotFoundException If the item type or one of the bonuses can't be found in the XML 
	 */
	@Test
	public final void testHaveRequiredBooksForItem () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final HeroItemBonus bonus1Def = new HeroItemBonus ();
		when (db.findHeroItemBonus ("IB01", "haveRequiredBooksForBonus")).thenReturn (bonus1Def);

		final PickAndQuantity prereq2 = new PickAndQuantity ();
		prereq2.setPickID ("MB01");
		prereq2.setQuantity (2);
		
		final HeroItemBonus bonus2Def = new HeroItemBonus ();
		bonus2Def.getHeroItemBonusPrerequisite ().add (prereq2);
		when (db.findHeroItemBonus ("IB02", "haveRequiredBooksForBonus")).thenReturn (bonus2Def);

		final PickAndQuantity prereq3 = new PickAndQuantity ();
		prereq3.setPickID ("MB02");
		prereq3.setQuantity (3);
		
		final HeroItemBonus bonus3Def = new HeroItemBonus ();
		bonus3Def.getHeroItemBonusPrerequisite ().add (prereq3);
		when (db.findHeroItemBonus ("IB03", "haveRequiredBooksForBonus")).thenReturn (bonus3Def);
		
		// Picks
		final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
		final PlayerPickUtils playerPickUtils= mock (PlayerPickUtils.class);

		// Set up object to test
		final HeroItemCalculationsImpl calc = new HeroItemCalculationsImpl ();
		calc.setPlayerPickUtils (playerPickUtils);
		
		// Item with no bonuses at all
		final HeroItem item = new HeroItem ();
		assertTrue (calc.haveRequiredBooksForItem (item, picks, db));
		
		// Add a bonus that doesn't have any requirements
		final HeroItemTypeAllowedBonus bonus1 = new HeroItemTypeAllowedBonus ();
		bonus1.setHeroItemBonusID ("IB01");
		item.getHeroItemChosenBonus ().add (bonus1);
		
		assertTrue (calc.haveRequiredBooksForItem (item, picks, db));
		
		// Add a bonus that needs some books
		final HeroItemTypeAllowedBonus bonus2 = new HeroItemTypeAllowedBonus ();
		bonus2.setHeroItemBonusID ("IB02");
		item.getHeroItemChosenBonus ().add (bonus2);
		
		assertFalse (calc.haveRequiredBooksForItem (item, picks, db));
		
		// Now actually have those books
		when (playerPickUtils.getQuantityOfPick (picks, "MB01")).thenReturn (2);
		assertTrue (calc.haveRequiredBooksForItem (item, picks, db));
		
		// Add another bonus that requires a different kind of book
		final HeroItemTypeAllowedBonus bonus3 = new HeroItemTypeAllowedBonus ();
		bonus3.setHeroItemBonusID ("IB03");
		item.getHeroItemChosenBonus ().add (bonus3);
		
		assertFalse (calc.haveRequiredBooksForItem (item, picks, db));
		
		// Now give us the other kind of book
		when (playerPickUtils.getQuantityOfPick (picks, "MB02")).thenReturn (5);
		assertTrue (calc.haveRequiredBooksForBonus ("IB01", picks, db));
	}
}
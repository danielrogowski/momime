package momime.common.calculations;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemType;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerPickUtils;

/**
 * Calculates stats about hero items
 */
public final class HeroItemCalculationsImpl implements HeroItemCalculations
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (HeroItemCalculationsImpl.class);
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * @param heroItem Hero item to calculate the cost for
	 * @param db Lookup lists built over the XML database
	 * @return Crafting cost - note this doesn't take the reductions from Artificer or Runemaster into account, because sometimes we need the raw value, e.g. when breaking on anvil
	 * @throws RecordNotFoundException If the item type, one of the bonuses or spell charges can't be found in the XML
	 * @throws MomException If we selected the Spell Charges bonus without picking the spell or number of charges
	 */
	@Override
	public final int calculateCraftingCost (final HeroItem heroItem, final CommonDatabase db) throws RecordNotFoundException, MomException
	{
		log.trace ("Entering calculateCraftingCost: " + heroItem.getHeroItemTypeID () + ", " + heroItem.getHeroItemName ());
		
		// Get base cost
		final HeroItemType itemType = db.findHeroItemType (heroItem.getHeroItemTypeID (), "calculateCraftingCost");
		int cost = itemType.getBaseCraftingCost ();
		
		// Add on cost of enchantments
		for (final HeroItemTypeAllowedBonus chosenBonus : heroItem.getHeroItemChosenBonus ())
		{
			final HeroItemBonus bonus = db.findHeroItemBonus (chosenBonus.getHeroItemBonusID (), "calculateCraftingCost");
			
			int bonusCost;
			if (!chosenBonus.getHeroItemBonusID ().equals (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES))
				bonusCost = bonus.getBonusCraftingCost ();
			else if ((heroItem.getSpellID () == null) || (heroItem.getSpellChargeCount () == null))
				throw new MomException ("Hero item \"" + heroItem.getHeroItemName () + "\" includes Spell Charges bonus, but doesn't specify which spell and/or the number of charges"); 
			else
				bonusCost = 20 * heroItem.getSpellChargeCount () * db.findSpell (heroItem.getSpellID (), "calculateCraftingCost").getCombatCastingCost ();
			
			if (bonus.isCraftingCostMultiplierApplies ())
				bonusCost = bonusCost * itemType.getItemBonusCraftingCostMultiplier ();
			
			cost = cost + bonusCost;
		}
		
		log.trace ("Exiting calculateCraftingCost = " + cost);
		return cost;
	}

	/**
	 * @param heroItemBonusID ID of the bonus that we want to get
	 * @param picks Our spell book picks
	 * @param db Lookup lists built over the XML database
	 * @return Whether we have the necessary picks to match the bonuses on this item
	 * @throws RecordNotFoundException If the bonuses can't be found in the XML 
	 */
	@Override
	public final boolean haveRequiredBooksForBonus (final String heroItemBonusID, final List<PlayerPick> picks, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering haveRequiredBooksForBonus: " + heroItemBonusID);
		
		final HeroItemBonus bonus = db.findHeroItemBonus (heroItemBonusID, "haveRequiredBooksForBonus");
			
		boolean haveRequiredBooks = true;
		final Iterator<PickAndQuantity> iter = bonus.getHeroItemBonusPrerequisite ().iterator ();
		while ((haveRequiredBooks) && (iter.hasNext ()))
		{
			final PickAndQuantity prereq = iter.next ();
			if (getPlayerPickUtils ().getQuantityOfPick (picks, prereq.getPickID ()) < prereq.getQuantity ())
				haveRequiredBooks = false;
		}
		
		log.trace ("Exiting haveRequiredBooksForBonus = " + haveRequiredBooks);
		return haveRequiredBooks;
	}
	
	/**
	 * @param heroItem Hero item we want to get
	 * @param picks Our spell book picks
	 * @param db Lookup lists built over the XML database
	 * @return Whether we have the necessary picks to match the bonuses on this item
	 * @throws RecordNotFoundException If the item type or one of the bonuses can't be found in the XML 
	 */
	@Override
	public final boolean haveRequiredBooksForItem (final HeroItem heroItem, final List<PlayerPick> picks, final CommonDatabase db) throws RecordNotFoundException
	{
		log.trace ("Entering haveRequiredBooksForItem: " + heroItem.getHeroItemTypeID () + ", " + heroItem.getHeroItemName ());
		
		boolean haveRequiredBooks = true;
		final Iterator<HeroItemTypeAllowedBonus> iter = heroItem.getHeroItemChosenBonus ().iterator ();
		while ((haveRequiredBooks) && (iter.hasNext ()))
			if (!haveRequiredBooksForBonus (iter.next ().getHeroItemBonusID (), picks, db))
				haveRequiredBooks = false;
		
		// NB. the Spell Charges bonus has no prerequisite books and you do not need to know (nor have enough
		// books to know) the spell in order to be able to obtain an item that casts it.
		
		log.trace ("Exiting haveRequiredBooksForItem = " + haveRequiredBooks);
		return haveRequiredBooks;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
}
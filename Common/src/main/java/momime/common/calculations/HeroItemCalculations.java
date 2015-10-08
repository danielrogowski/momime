package momime.common.calculations;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.HeroItem;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PlayerPick;

/**
 * Calculates stats about hero items
 */
public interface HeroItemCalculations
{
	/**
	 * @param heroItem Hero item to calculate the cost for
	 * @param db Lookup lists built over the XML database
	 * @return Crafting cost - note this doesn't take the reductions from Artificer or Runemaster into account, because sometimes we need the raw value, e.g. when breaking on anvil
	 * @throws RecordNotFoundException If the item type, one of the bonuses or spell charges can't be found in the XML
	 */
	public int calculateCraftingCost (final HeroItem heroItem, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param heroItemBonusID ID of the bonus that we want to get
	 * @param picks Our spell book picks
	 * @param db Lookup lists built over the XML database
	 * @return Whether we have the necessary picks to match the bonuses on this item
	 * @throws RecordNotFoundException If the bonuses can't be found in the XML 
	 */
	public boolean haveRequiredBooksForBonus (final String heroItemBonusID, final List<PlayerPick> picks, final CommonDatabase db) throws RecordNotFoundException;
	
	/**
	 * @param heroItem Hero item we want to get
	 * @param picks Our spell book picks
	 * @param db Lookup lists built over the XML database
	 * @return Whether we have the necessary picks to match the bonuses on this item
	 * @throws RecordNotFoundException If the item type or one of the bonuses can't be found in the XML 
	 */
	public boolean haveRequiredBooksForItem (final HeroItem heroItem, final List<PlayerPick> picks, final CommonDatabase db) throws RecordNotFoundException;
}
package momime.common.utils;

import java.security.InvalidParameterException;
import java.util.List;

import momime.common.database.ICommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Pick;
import momime.common.messages.v0_9_4.PlayerPick;

/**
 * Methods for working with list of PlayerPicks
 */
public interface IPlayerPickUtils
{
	/**
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return Total cost of all of this player's picks
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public int getTotalPickCost (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Delphi method used to be called 'FindPick'
	 * @param picks List of picks to check
	 * @param pickID The ID of the pick to search for
	 * @return The quantity of the requested pick that the player has; for spell books this is the number of books, for retorts it is always 1; if has none of requested pick, returns zero
	 */
	public int getQuantityOfPick (final List<PlayerPick> picks, final String pickID);

	/**
	 * Changes the number of a particular type of pick we have
	 * Will add a new pick to the list of we add a new pick ID that we didn't previously have
	 * Will remove picks from the list if we reduce their quantity to zero
	 *
	 * @param picks List of picks to update
	 * @param pickID The type of pick we want to change our quantity of
	 * @param changeInQuantity The amount of the pick to add or remove
	 */
	public void updatePickQuantity (final List<PlayerPick> picks, final String pickID, final int changeInQuantity);

	/**
	 * @param picks List of picks to check
	 * @param pickTypeID Type of pick to count - 'R' for retorts or 'B' for spell books
	 * @param original True to count only picks the player chose at the start of the game; false to include extras they've picked up from capturing monsters lairs and so on
	 * @param db Lookup lists built over the XML database
	 * @return Number of picks in list of the requested pick type, e.g. count total number of all spell books
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public int countPicksOfType (final List<PlayerPick> picks, final String pickTypeID, final boolean original, final ICommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Certain picks have pre-requisite other picks, e.g. to choose Divine Power you have to have 4 life books.  This tests if a player has the necessary pre-requisites to add a particular pick.
	 * Written as much as possible so that pre-requisite retorts will also work, even though the original MoM has none of these, e.g. could have Super Warlord with Warlord as a pre-requisite
	 * This is used for retorts - it notably doesn't check exclusivity (can't have both life + death books) because there's no exclusive retorts
	 * @param pick The pick that the player wants to add
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return True if player has the necessary pre-requisites for this pick
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public boolean meetsPickRequirements (final Pick pick, final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Tests if a certain pick can be removed, or if doing so will break some pre-requisite of another pick we already have
	 * For example you can't remove a life book if we've only got 4 of them and have Divine Power
	 *
	 * @param pickID The type of the pick the player wants to remove
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return True if the player can remove the pick without violating any pre-requisites of remaining picks
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public boolean canSafelyRemove (final String pickID, final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Tests if a certain pick can be added, or if we've another type of pick which is exclusive from it
	 * This is used to make sure we can't add a life book if we've got death books, and vice versa
	 * Ths is used for books - it notably doesn't check pre-requisites (other required picks) because no books have any
	 * @param pick The pick that the player wants to add
	 * @param picks List of picks to check
	 * @return True if the player can add the pick without violating exclusivity of any picks they already have
	 */
	public boolean canSafelyAdd (final Pick pick, final List<PlayerPick> picks);

	/**
	 * Some picks (the Alchemy retort) grant improved weapon grades for all Normal units.  This method finds the highest weapon grade granted by this set of picks.
	 * Basically returns 1 if the wizard has Alchemy, 0 if not
	 *
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return Best weapon grade granted by all the picks the player has
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public int getHighestWeaponGradeGrantedByPicks (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Since the only picks which gives this are the Divine & Infernal Power retorts, and since you can never have both, this will only ever be 0% or 50%
	 *
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return Total percentage bonus that any picks we have increase the effectiveness of religious buildings
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public int totalReligiousBuildingBonus (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Delphi code used to return this info in two 'var' parameters as part of 'totalReligiousBuildingBonus', but Java doesn't handle methods with multiple outputs, so split into a separate method
	 *
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return List of pick IDs that provide any percentage bonus to the effectiveness of religious building
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public List<String> pickIdsContributingToReligiousBuildingBonus (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * e.g. Archmage gives +50% to skill improvement 'production'
	 * Mana Focusing gives +25% to mana 'production'
	 *
	 * @param productionTypeID Production type we want to calculate the bonus for
	 * @param unitTypeID Some bonuses (Summoner unit cost reduction) apply only to a particular unit type ID and so need to provide this value
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return Total bonus to specified type of production from all picks
	 * @throws InvalidParameterException If we request a production type ID whose special bonus can't be calculated by this routine
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public int totalProductionBonus (final String productionTypeID, final String unitTypeID, final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException;
}

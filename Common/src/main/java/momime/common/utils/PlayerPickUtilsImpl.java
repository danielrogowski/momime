package momime.common.utils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ICommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.Pick;
import momime.common.database.v0_9_4.PickExclusiveFrom;
import momime.common.database.v0_9_4.PickPrerequisite;
import momime.common.database.v0_9_4.PickProductionBonus;
import momime.common.messages.v0_9_4.PlayerPick;

/**
 * Methods for working with list of PlayerPicks
 */
public final class PlayerPickUtilsImpl implements IPlayerPickUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (PlayerPickUtilsImpl.class.getName ());
	
	/**
	 * @param srcPicks List of picks to copy
	 * @return Deep copied list of picks
	 */
	private final List<PlayerPick> duplicatePlayerPicksList (final List<PlayerPick> srcPicks)
	{
		final List<PlayerPick> destPicks = new ArrayList<PlayerPick> ();
		for (final PlayerPick srcPick : srcPicks)
		{
			final PlayerPick destPick = new PlayerPick ();
			destPick.setPickID (srcPick.getPickID ());
			destPick.setQuantity (srcPick.getQuantity ());
			destPick.setOriginalQuantity (srcPick.getOriginalQuantity ());
			destPicks.add (destPick);
		}

		return destPicks;
	}

	/**
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return Total cost of all of this player's picks
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final int getTotalPickCost (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "getTotalPickCost");

		int result = 0;
		for (final PlayerPick thisPick : picks)
			result = result + (db.findPick (thisPick.getPickID (), "getTotalPickCost").getPickCost () * thisPick.getQuantity ());

		log.exiting (PlayerPickUtilsImpl.class.getName (), "getTotalPickCost", result);
		return result;
	}

	/**
	 * Delphi method used to be called 'FindPick'
	 * @param picks List of picks to check
	 * @param pickID The ID of the pick to search for
	 * @return The quantity of the requested pick that the player has; for spell books this is the number of books, for retorts it is always 1; if has none of requested pick, returns zero
	 */
	@Override
	public final int getQuantityOfPick (final List<PlayerPick> picks, final String pickID)
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "getQuantityOfPick", pickID);

		int result = 0;
		final Iterator<PlayerPick> iter = picks.iterator ();
		while ((result == 0) && (iter.hasNext ()))
		{
			final PlayerPick thisPick = iter.next ();
			if (thisPick.getPickID ().equals (pickID))
				result = thisPick.getQuantity ();
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "getQuantityOfPick", result);
		return result;
	}

	/**
	 * Changes the number of a particular type of pick we have
	 * Will add a new pick to the list of we add a new pick ID that we didn't previously have
	 * Will remove picks from the list if we reduce their quantity to zero
	 *
	 * @param picks List of picks to update
	 * @param pickID The type of pick we want to change our quantity of
	 * @param changeInQuantity The amount of the pick to add or remove
	 */
	@Override
	public final void updatePickQuantity (final List<PlayerPick> picks, final String pickID, final int changeInQuantity)
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "updatePickQuantity", new String [] {pickID, new Integer (changeInQuantity).toString ()});

		String result = null;

		// Search for existing pick entry to update
		final Iterator<PlayerPick> iter = picks.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final PlayerPick thisPick = iter.next ();
			if (thisPick.getPickID ().equals (pickID))
			{
				thisPick.setQuantity (thisPick.getQuantity () + changeInQuantity);
				if (thisPick.getQuantity () <= 0)
				{
					iter.remove ();
					result = "Existing entry found, change reduced to <= 0 so removed";
				}
				else
					result = "Existing entry found and updated";
			}
		}

		// If we didn't find an existing pick, add it
		if (result == null)
		{
			if (changeInQuantity > 0)
			{
				result = "No existing entry, so added";
				final PlayerPick newPick = new PlayerPick ();
				newPick.setPickID (pickID);
				newPick.setQuantity (changeInQuantity);
				picks.add (newPick);
			}
			else
				result = "No existing entry, but changeInQuantity <= 0, so done nothing";
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "updatePickQuantity", result);
	}

	/**
	 * @param picks List of picks to check
	 * @param pickTypeID Type of pick to count - 'R' for retorts or 'B' for spell books
	 * @param original True to count only picks the player chose at the start of the game; false to include extras they've picked up from capturing monsters lairs and so on
	 * @param db Lookup lists built over the XML database
	 * @return Number of picks in list of the requested pick type, e.g. count total number of all spell books
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final int countPicksOfType (final List<PlayerPick> picks, final String pickTypeID, final boolean original, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "countPicksOfType", pickTypeID);

		int result = 0;
		for (final PlayerPick thisPick : picks)
			if (db.findPick (thisPick.getPickID (), "countPicksOfType").getPickType ().equals (pickTypeID))
			{
				if (original)
					result = result + thisPick.getOriginalQuantity ();
				else
					result = result + thisPick.getQuantity ();
			}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "countPicksOfType", result);
		return result;
	}

	/**
	 * Where a pick has general pre-requisites based on pick type (e.g. "2 books in any 3 realms of magic") rather than specific picks (e.g. "4 life books"), this decides which type of book to use to satisfy the pre-requisite
	 * If pre-requisite is for, say, 4 books of any type then ideally we want to find books with exactly 4 books, if not then as few as possible over 4 books
	 * @param picks List of picks to check
	 * @param desiredPickTypeID Type of pick desired ("Book" or "Retort")
	 * @param desiredCount Quantity of pick desired
	 * @param db Lookup lists built over the XML database
	 * @return Most appropriate pick to use to satisfy pre-requisite, or null if we don't have enough books of any type
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
 	 */
	private final String findMostAppropriatePickToSatisfy (final List<PlayerPick> picks, final String desiredPickTypeID, final int desiredCount, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "findMostAppropriatePickToSatisfy", new String [] {desiredPickTypeID, new Integer (desiredCount).toString ()});

		String bestPick = null;
		int bestPickCount = Integer.MAX_VALUE;

		for (final PlayerPick thisPick : picks)

			// Make sure its the right pick type, i.e. if we're looking for "a book in any realm of magic" that this pick isn't a retort
			if (db.findPick (thisPick.getPickID (), "findMostAppropriatePickToSatisfy").getPickType ().equals (desiredPickTypeID))

				// Do we have enough of it, and is it better than the one we've already chosen?
				if ((thisPick.getQuantity () >= desiredCount) && (thisPick.getQuantity () < bestPickCount))
				{
					// Found one better than what we already have chosen
					bestPick = thisPick.getPickID ();
					bestPickCount = thisPick.getQuantity ();
				}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "findMostAppropriatePickToSatisfy", bestPick);
		return bestPick;
	}

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
	@Override
	public final boolean meetsPickRequirements (final Pick pick, final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "meetsPickRequirements", pick.getPickID ());

		boolean result = true;

		// Make a copy of all the picks we have, then as we use those picks to satisfy various pre-requisites, we can remove them from the list
		final List<PlayerPick> picksLeftToUse = duplicatePlayerPicksList (picks);

		// Deal with any pre-requisites for specific books
		for (final PickPrerequisite pickPrerequisite : pick.getPickPrerequisite ())
		{
			// Ensure this is a pre-requisite for a specific book
			if ((pickPrerequisite.getPrerequisiteID () != null) && (!pickPrerequisite.getPrerequisiteID ().equals ("")))
			{
				// Found one - see if we have it
				if (getQuantityOfPick (picksLeftToUse, pickPrerequisite.getPrerequisiteID ()) >= pickPrerequisite.getPrerequisiteCount ())
				{
					// Remove from list of pre-requisites we can still use
					updatePickQuantity (picksLeftToUse, pickPrerequisite.getPrerequisiteID (), -pickPrerequisite.getPrerequisiteCount ());
				}
				else
				{
					// Don't have the necessary pre-requisite
					result = false;
				}
			}
		}

		// Deal with any pre-requisites for a certain number of any book e.g. Sage Master requires 1 pick in any 2 realms of magic
		for (final PickPrerequisite pickPrerequisite : pick.getPickPrerequisite ())
		{
			// Ensure this is a general pre-requisite
			if ((pickPrerequisite.getPrerequisiteTypeID () != null) && (!pickPrerequisite.getPrerequisiteTypeID ().equals ("")))
			{
				// Found one - pick the best type of spell book to use to satisfy the requirement
				final String preRequisiteID = findMostAppropriatePickToSatisfy (picksLeftToUse, pickPrerequisite.getPrerequisiteTypeID (), pickPrerequisite.getPrerequisiteCount (), db);
				if (preRequisiteID != null)
				{
					// Remove from list of pre-requisites we can still use - set it all the way to zero so that we can't for example use 2 life books to satisfy a pre-requisite for 1 pick in 2 different realms of magic
					updatePickQuantity (picksLeftToUse, preRequisiteID, -getQuantityOfPick (picksLeftToUse, preRequisiteID));
				}
				else
				{
					// Don't have the necessary pre-requisite
					result = false;
				}
			}
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "meetsPickRequirements", result);
		return result;
	}

	/**
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return True if requirements of all picks in list are met, e.g. will return False if list includes Divine Power but doesn't include 4 Life Books
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	private final boolean allRequirementsMet (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "allRequirementsMet");

		boolean result = true;
		final Iterator<PlayerPick> iter = picks.iterator ();

		while ((result) && (iter.hasNext ()))
			if (!meetsPickRequirements (db.findPick (iter.next ().getPickID (), "allRequirementsMet"), picks, db))
				result = false;

		log.exiting (PlayerPickUtilsImpl.class.getName (), "allRequirementsMet", result);
		return result;
	}

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
	@Override
	public final boolean canSafelyRemove (final String pickID, final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "canSafelyRemove", pickID);

		// First check we actually have one of the pick to remove
		final boolean result;
		if (getQuantityOfPick (picks, pickID) <= 0)
			result = false;
		else
		{
			// We have one of the pick
			// Only way to reliably test this is to copy the whole list, remove the pick in question, and then re-check pre-requisites
			final List<PlayerPick> testList = duplicatePlayerPicksList (picks);

			// Take 1 of this pick out of the list
			updatePickQuantity (testList, pickID, -1);

			// Now test the list for validity
			result = allRequirementsMet (testList, db);
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "canSafelyRemove", result);
		return result;
	}

	/**
	 * Tests if a certain pick can be added, or if we've another type of pick which is exclusive from it
	 * This is used to make sure we can't add a life book if we've got death books, and vice versa
	 * Ths is used for books - it notably doesn't check pre-requisites (other required picks) because no books have any
	 * @param pick The pick that the player wants to add
	 * @param picks List of picks to check
	 * @return True if the player can add the pick without violating exclusivity of any picks they already have
	 */
	@Override
	public final boolean canSafelyAdd (final Pick pick, final List<PlayerPick> picks)
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "canSafelyAdd", pick.getPickID ());

		boolean result = true;
		for (final PickExclusiveFrom thisExclusive : pick.getPickExclusiveFrom ())
		{
			// Found an exclusivity - check if we have any of it
			if (getQuantityOfPick (picks, thisExclusive.getPickExclusiveFromID ()) > 0)

				// Mutually exclusive pick found, so we can't add this pick
				result = false;
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "canSafelyAdd", result);
		return result;
	}

	/**
	 * Some picks (the Alchemy retort) grant improved weapon grades for all Normal units.  This method finds the highest weapon grade granted by this set of picks.
	 * Basically returns 1 if the wizard has Alchemy, 0 if not
	 *
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return Best weapon grade granted by all the picks the player has
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final int getHighestWeaponGradeGrantedByPicks (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "getHighestWeaponGradeGrantedByPicks");

		int result = 0;
		for (final PlayerPick thisPick : picks)
		{
			final Integer thisValue = db.findPick (thisPick.getPickID (), "getHighestWeaponGradeGrantedByPicks").getPickMagicWeapons ();
			if (thisValue != null)
				result = Math.max (result, thisValue);
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "getHighestWeaponGradeGrantedByPicks", result);
		return result;
	}

	/**
	 * Since the only picks which gives this are the Divine & Infernal Power retorts, and since you can never have both, this will only ever be 0% or 50%
	 *
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return Total percentage bonus that any picks we have increase the effectiveness of religious buildings
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final int totalReligiousBuildingBonus (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "totalReligiousBuildingBonus");

		int result = 0;
		for (final PlayerPick thisPick : picks)
		{
			final Integer thisValue = db.findPick (thisPick.getPickID (), "totalReligiousBuildingBonus").getPickReligiousBuildingBonus ();
			if (thisValue != null)
				result = result + (thisValue * thisPick.getQuantity ());
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "totalReligiousBuildingBonus", result);
		return result;
	}

	/**
	 * Delphi code used to return this info in two 'var' parameters as part of 'totalReligiousBuildingBonus', but Java doesn't handle methods with multiple outputs, so split into a separate method
	 *
	 * @param picks List of picks to check
	 * @param db Lookup lists built over the XML database
	 * @return List of pick IDs that provide any percentage bonus to the effectiveness of religious building
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final List<String> pickIdsContributingToReligiousBuildingBonus (final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "pickIdsContributingToReligiousBuildingBonus");

		final List<String> result = new ArrayList<String> ();
		for (final PlayerPick thisPick : picks)
		{
			final Integer thisValue = db.findPick (thisPick.getPickID (), "pickIdsContributingToReligiousBuildingBonus").getPickReligiousBuildingBonus ();
			if ((thisValue != null) && (thisValue > 0))
				result.add (thisPick.getPickID ());
		}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "pickIdsContributingToReligiousBuildingBonus", result);
		return result;
	}

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
	@Override
	public final int totalProductionBonus (final String productionTypeID, final String unitTypeID, final List<PlayerPick> picks, final ICommonDatabase db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickUtilsImpl.class.getName (), "totalProductionBonus", new String [] {productionTypeID, unitTypeID});

		if ((productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH)) ||
			(productionTypeID.equals (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SPELL_COST_REDUCTION)))

			throw new InvalidParameterException (PlayerPickUtilsImpl.class.getName () + ".totalProductionBonus should never be used to calculate spell cost reductions or research bonuses since these have special rules for how the bonuses are combined together");

		int result = 0;
		for (final PlayerPick thisPick : picks)
			for (final PickProductionBonus bonus : db.findPick (thisPick.getPickID (), "totalProductionBonus").getPickProductionBonus ())

				// Bonus to right type of production?
				if (bonus.getProductionTypeID ().equals (productionTypeID))
				{
					// Does this production bonus only apply to a specifc unit type?  Blank = applies to all
					if ((bonus.getUnitTypeID () == null) || (bonus.getUnitTypeID ().equals ("")) || (bonus.getUnitTypeID ().equals (unitTypeID)))
						result = result + (bonus.getPercentageBonus () * thisPick.getQuantity ());
				}

		log.exiting (PlayerPickUtilsImpl.class.getName (), "totalProductionBonus", result);
		return result;
	}
}

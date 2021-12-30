package momime.server.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Pick;
import momime.common.database.PickAndQuantity;
import momime.common.database.PickType;
import momime.common.database.PickTypeCountContainer;
import momime.common.database.PickTypeGrantsSpells;
import momime.common.database.Plane;
import momime.common.database.RaceEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.WizardEx;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowRank;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.SpellAI;

/**
 * Server side only helper methods for dealing with picks
 */
public final class PlayerPickServerUtilsImpl implements PlayerPickServerUtils
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (PlayerPickServerUtilsImpl.class);
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** AI decisions about spells */
	private SpellAI spellAI;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/**
	 * @param picks Player's picks to count up
	 * @param db Lookup lists built over the XML database
	 * @return Initial skill wizard will start game with - 2 per book +10 if they chose Archmage
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final int getTotalInitialSkill (final List<PlayerPick> picks, final CommonDatabase db) throws RecordNotFoundException
	{
		int total = 0;
		for (final PlayerPick thisPick : picks)
		{
			final Pick thisPickRecord = db.findPick (thisPick.getPickID (), "getTotalInitialSkill");
			if (thisPickRecord.getPickInitialSkill () != null)
				total = total + (thisPickRecord.getPickInitialSkill () * thisPick.getQuantity ());
		}

		return total;
	}

	/**
	 * @param players List of players
	 * @param wizardID Wizard ID we want to pick
	 * @return Player with the specified wizard ID, or null if none are found
	 */
	@Override
	public final PlayerServerDetails findPlayerUsingWizard (final List<PlayerServerDetails> players, final String wizardID)
	{
		PlayerServerDetails result = null;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final PlayerServerDetails player = iter.next ();
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

			if ((ppk.getWizardID () != null) && (ppk.getWizardID ().equals (wizardID)))
				result = player;
		}

		return result;
	}

	/**
	 * @param players List of players
	 * @param standardPhotoID Standard photo ID we want to pick
	 * @return Player with the specified Standard photo ID, or null if none are found
	 */
	@Override
	public final PlayerServerDetails findPlayerUsingStandardPhoto (final List<PlayerServerDetails> players, final String standardPhotoID)
	{
		PlayerServerDetails result = null;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final PlayerServerDetails player = iter.next ();
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

			if ((ppk.getStandardPhotoID () != null) && (ppk.getStandardPhotoID ().equals (standardPhotoID)))
				result = player;
		}

		return result;
	}

	/**
	 * Checks whether a player's custom pick selection is valid
	 *
	 * @param player Player choosing custom picks
	 * @param picks The custom picks they have requested
	 * @param humanSpellPicks Number of picks that human players get at the start of the game, from the session description - difficulty level
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return null if choices are acceptable; message to send back to client if choices aren't acceptable
	 */
	@Override
	public final String validateCustomPicks (final PlayerServerDetails player, final List<PickAndQuantity> picks, final int humanSpellPicks, final MomSessionVariables mom)
	{
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		String msg = null;
		if (ppk.getWizardID () == null)
			msg = "You must choose a custom wizard or before trying to choose custom picks";

		else if (!ppk.getWizardID ().equals (""))
			msg = "You cannot choose custom picks when you have picked one of the pre-defined wizards";

		else if ((priv.isCustomPicksChosen () != null) && (priv.isCustomPicksChosen ()))
			msg = "You cannot choose custom picks more than once";

		else
		{
			// Make sure all the pick IDs are valid, and also total them up
			int totalPickCost = 0;
			try
			{
				for (final PickAndQuantity thisPick : picks)
				{
					final Pick pick = mom.getServerDB ().findPick (thisPick.getPickID (), "validateCustomPicks");
					totalPickCost = totalPickCost + (thisPick.getQuantity () * pick.getPickCost ());
				}

				// Right total?
				if (totalPickCost != humanSpellPicks)
					msg = "Incorrect number of custom picks chosen, please try again";
			}
			catch (final RecordNotFoundException e)
			{
				msg = "Requested a pick ID that doesn't exist";
			}
		}

		return msg;
	}

	/**
	 * Counts how many spells of each rank for the specified magic realm we still need to choose for free before starting the game
	 * e.g. if we have 10 life books, and so far have chosen 5 common life spells, will return that we still need to choose 4 more
	 *
	 * @param player Player we want to check to see if they need to pick free spells
	 * @param pick The pick we want to see if we get any free spells from
	 * @param db Lookup lists built over the XML database
	 * @return Message containing magic realm and counts of how many spells we need to pick of each rank - can be an empty list; null indicates that the quantity of pick we have doesn't grant any free spells
	 * @throws RecordNotFoundException If the pick ID can't be found in the database, or refers to a pick type ID that can't be found; or the player has a spell research status that isn't found
	 */
	final ChooseInitialSpellsNowMessage countFreeSpellsLeftToChoose (final PlayerServerDetails player, final PlayerPick pick, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// What type of pick is it - a book or retort?
		final PickType pickType = db.findPickType (db.findPick (pick.getPickID (), "countFreeSpellsLeftToChoose").getPickType (), "countFreeSpellsLeftToChoose");

		// Find the entry for how many of this number book we have
		PickTypeCountContainer pickTypeCount = null;
		final Iterator<PickTypeCountContainer> pickTypeCountIterator = pickType.getPickTypeCount ().iterator ();
		while ((pickTypeCount == null) && (pickTypeCountIterator.hasNext ()))
		{
			final PickTypeCountContainer thisPickTypeCount = pickTypeCountIterator.next ();
			if (thisPickTypeCount.getCount () == pick.getQuantity ())
				pickTypeCount = thisPickTypeCount;
		}

		// There might not be an entry - that just means having this number of this pick doesn't grant us any free spells
		final ChooseInitialSpellsNowMessage msg;
		if (pickTypeCount == null)
			msg = null;
		else
		{
			// Make a list of how many spells of each rank we still need to pick
			msg = new ChooseInitialSpellsNowMessage ();
			msg.setMagicRealmID (pick.getPickID ());

			for (final PickTypeGrantsSpells thisSpellRank : pickTypeCount.getSpellCount ())
			{
				final int freeSpellsAlreadySelected = getSpellUtils ().getSpellsForRealmRankStatus (priv.getSpellResearchStatus (),
					pick.getPickID (), thisSpellRank.getSpellRank (), SpellResearchStatusID.AVAILABLE, db).size ();

				if ((thisSpellRank.getSpellsFreeAtStart () != null) && (thisSpellRank.getSpellsFreeAtStart () > freeSpellsAlreadySelected))
				{
					// Yay, found one!
					final ChooseInitialSpellsNowRank msgRank = new ChooseInitialSpellsNowRank ();
					msgRank.setSpellRankID (thisSpellRank.getSpellRank ());
					msgRank.setFreeSpellCount (thisSpellRank.getSpellsFreeAtStart () - freeSpellsAlreadySelected);
					msg.getSpellRank ().add (msgRank);

					log.debug ("countFreeSpellsLeftToChoose: Human player " + player.getPlayerDescription ().getPlayerName () + " needs to choose " +
						msgRank.getFreeSpellCount () + " free spells of realm " + msg.getMagicRealmID () + " rank " + msgRank.getSpellRankID ());
				}
			}
		}

		return msg;
	}

	/**
	 * Checks each type of book this player has to see if it can find a type of book for which the player gets free spells, but has not yet chosen those free spells
	 *
	 * For human players, if there is a type of book that we need to pick free spells for then sends a message to the client to
	 * tell them to make their selection; if all free spells have been chosen then instructs the client to proceed to choosing a race
	 *
	 * For AI players, if there is a type of book that we need to pick free spells for then this calls the routine to make
	 * the AI player choose the free spells for this type of book and actually gives those spells to the AI player;
	 * if all free spells have been chosen, returns null and does nothing
	 *
	 * @param player Player we want to check to see if they need to pick free spells
	 * @param db Lookup lists built over the XML database
	 * @return Message containing magic realm and counts of how many spells we need to pick of each rank; or null if there are no more free spells we need to choose
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 * @throws RecordNotFoundException If the player has picks which we can't find in the cache, or the AI player chooses a spell which we can't then find in their list
	 */
	@Override
	public final ChooseInitialSpellsNowMessage findRealmIDWhereWeNeedToChooseFreeSpells (final PlayerServerDetails player, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Search through the types of book that this player has
		ChooseInitialSpellsNowMessage msg = null;

		final Iterator<PlayerPick> picksIterator  = ppk.getPick ().iterator ();
		while ((msg == null) && (picksIterator.hasNext ()))
		{
			final PlayerPick pick = picksIterator.next ();

			// Make a list of how many spells of each rank we still need to pick
			msg = countFreeSpellsLeftToChoose (player, pick, db);

			// If we found a type of book that we have enough of to get some free spells, but we've already chosen them, then wipe out the message and continue looking
			if ((msg != null) && (msg.getSpellRank ().size () == 0))
				msg = null;
		}

		// For human players at this point we're done, we either return null and then instruct them to proceed to choosing a race, or
		// we've selected a type of book and listed for them how many spells of each rank they need to choose
		// For AI players, we actually need to choose spells
		if ((msg != null) && (!player.getPlayerDescription ().isHuman ()))
			for (final ChooseInitialSpellsNowRank thisSpellRank : msg.getSpellRank ())
			{
				log.debug ("findRealmIDWhereWeNeedToChooseFreeSpells: AI player " + player.getPlayerDescription ().getPlayerName () + " about to choose " +
					thisSpellRank.getFreeSpellCount () + " free spells of realm " + msg.getMagicRealmID () + " rank " + thisSpellRank.getSpellRankID ());

				for (int aiSpellChoices = 0; aiSpellChoices < thisSpellRank.getFreeSpellCount (); aiSpellChoices++)
					getSpellAI ().chooseFreeSpellAI (priv.getSpellResearchStatus (), msg.getMagicRealmID (), thisSpellRank.getSpellRankID (), db).setStatus
						(SpellResearchStatusID.AVAILABLE);
			}

		return msg;
	}

	/**
	 * Validates whether the spell IDs the player wants to get for free are acceptable or not
	 *
	 * @param player Player who is trying to choose free spells
	 * @param pickID The magic realm of the spells they want to choose
	 * @param spellIDs Which spells they want to choose
	 * @param db Lookup lists built over the XML database
	 * @return null if choices are acceptable; message to send back to client if choices aren't acceptable
	 * @throws RecordNotFoundException If the pick ID can't be found in the database, or refers to a pick type ID that can't be found; or the player has a spell research status that isn't found
	 */
	@Override
	public final String validateInitialSpellSelection (final PlayerServerDetails player, final String pickID, final List<String> spellIDs, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

		// Find how many of this pick the player has
		PlayerPick pick = null;
		final Iterator<PlayerPick> picksIterator  = ppk.getPick ().iterator ();
		while ((pick == null) && (picksIterator.hasNext ()))
		{
			final PlayerPick thisPick = picksIterator.next ();
			if (thisPick.getPickID ().equals (pickID))
				pick = thisPick;
		}

		String msg;
		if (pick == null)
			msg = "You can't choose free spells for a magic realm which you have no books in";
		else
		{
			// First get a list of how many spells of each rank the player has left to choose in this magic realm
			final ChooseInitialSpellsNowMessage rankCounts = countFreeSpellsLeftToChoose (player, pick, db);
			if (rankCounts == null)
				msg = "Having " + pick.getQuantity () + " books doesn't grant any free spells";
			else
			{
				// Now go through the list of spells, subtracting off from the count for the relevant rank for each spell we've chosen
				msg = null;
				final Iterator<String> spellsIterator = spellIDs.iterator ();
				while ((msg == null) && (spellsIterator.hasNext ()))
				{
					final Spell thisSpell = db.findSpell (spellsIterator.next (), "validateInitialSpellSelection");
					if (!pickID.equals (thisSpell.getSpellRealm ()))
						msg = "You are trying to choose free spells that don't match the magic realm specified";

					boolean rankFound = false;
					for (final ChooseInitialSpellsNowRank thisRank : rankCounts.getSpellRank ())
					{
						if (thisRank.getSpellRankID ().equals (thisSpell.getSpellRank ()))
						{
							rankFound = true;
							if (thisRank.getFreeSpellCount () > 0)
								thisRank.setFreeSpellCount (thisRank.getFreeSpellCount () - 1);
							else
								msg = "You have chosen too many free spells";
						}
					}

					// Stop trying to choose spells that are of a rank that we get no free spells of, and so aren't even in the list
					if (!rankFound)
						msg = "You have chosen too many free spells";
				}
			}
		}

		return msg;
	}

	/**
	 * @param player Player who wants to choose a race
	 * @param raceID Race they wish to choose
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choice isn't acceptable
	 * @throws RecordNotFoundException If we choose a race whose native plane can't be found
	 */
	@Override
	public final String validateRaceChoice (final PlayerServerDetails player, final String raceID, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

		RaceEx race = null;
		try
		{
			race = db.findRace (raceID, "validateRaceChoice");
		}
		catch (final RecordNotFoundException e)
		{
			// Ignore it, trap this just by the fact that race is still null
		}

		final String msg;
		if (race == null)
			msg = "Race choice invalid, please try again";
		else
		{
			// Check pre-requisites, i.e. if we've picked a Myrran race we have to have the Myrran retort
			final String pickID = db.findPlane (race.getNativePlane (), "validateRaceChoice").getPrerequisitePickToChooseNativeRace ();
			if (pickID == null)
				msg = null;

			// Check if we have the pick
			else if (getPlayerPickUtils ().getQuantityOfPick (ppk.getPick (), pickID) > 0)
				msg = null;

			else
				msg = "You don''t have the necessary pick to choose this race";
		}

		return msg;
	}

	/**
	 * @param player Player to test
	 * @return True if this player has made all their pre-game selections and are waiting to start
	 */
	final boolean hasChosenAllDetails (final PlayerServerDetails player)
	{
		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		final boolean isCustomPicksChosen = (priv.isCustomPicksChosen () == null) ? false : priv.isCustomPicksChosen ();

		final boolean result = ((getPlayerKnowledgeUtils ().hasWizardBeenChosen (ppk.getWizardID ())) && (priv.getFirstCityRaceID () != null) &&
			((!getPlayerKnowledgeUtils ().isCustomWizard (ppk.getWizardID ())) || (isCustomPicksChosen)));

		return result;
	}

	/**
	 * Tests whether everyone has finished pre-game selections and is ready to start
	 * @param players List of players
	 * @param sd Session description
	 * @return True if all players have chosen all details to start game
	 */
	@Override
	public final boolean allPlayersHaveChosenAllDetails (final List<PlayerServerDetails> players, final MomSessionDescription sd)
	{
		// If not all players have joined, then not all have chosen
		boolean result = (players.size () == sd.getMaxPlayers () - sd.getAiPlayerCount () - 2);	// -2 for raiders & monsters

		// Check each player
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((result) && (iter.hasNext ()))
			if (!hasChosenAllDetails (iter.next ()))
				result = false;

		return result;
	}

	/**
	 * Used when reloading multiplayer games to test whether all necessary players have joined back into the session and it can continue
	 * @param players List of players
	 * @return True if all human players are connected
	 */
	@Override
	public final boolean allPlayersAreConnected (final List<PlayerServerDetails> players)
	{
		// Check each player
		boolean result = true;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((result) && (iter.hasNext ()))
		{
			final PlayerServerDetails player = iter.next ();
			if ((player.getPlayerDescription ().isHuman ()) && (player.getConnection () == null))
				result = false;
		}
		
		return result;
	}
	
	/**
	 * @param players List of players
	 * @param db Lookup lists built over the XML database
	 * @return List of wizards not used by human players - AI players will then pick randomly from this list
	 */
	@Override
	public final List<WizardEx> listWizardsForAIPlayers (final List<PlayerServerDetails> players, final CommonDatabase db)
	{
		// First get a list of all the available wizards
		final List<WizardEx> availableWizards = new ArrayList<WizardEx> ();
		for (final WizardEx thisWizard : db.getWizards ())
			if ((!thisWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) &&
				(!thisWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)) &&
				(findPlayerUsingStandardPhoto (players, thisWizard.getWizardID ()) == null))

				availableWizards.add (thisWizard);

		return availableWizards;
	}

	/**
	 * Checks which plane a wizard with a certain selection of picks should start on
	 * @param picks List of picks the wizard has
	 * @param db Lookup lists built over the XML database
	 * @return Plane the wizard should start on
	 */
	@Override
	public final int startingPlaneForWizard (final List<PlayerPick> picks, final CommonDatabase db)
	{
		int bestMatch = 0;		// Default to Arcanus
		int bestMatchPrerequisiteCount = 0;

		// Check each plane
		for (final Plane plane : db.getPlane ())
		{
			// Meet whatever pre-requisites are defined?
			if ((plane.getPrerequisitePickToChooseNativeRace () == null) || (getPlayerPickUtils ().getQuantityOfPick (picks, plane.getPrerequisitePickToChooseNativeRace ()) > 0))
			{
				// How many picks did we match on? (this is a bit of a fake way of doing it when you can only need a single pick, but, well it works)
				int thisMatchPrerequisiteCount = 0;
				if (plane.getPrerequisitePickToChooseNativeRace () != null)
					thisMatchPrerequisiteCount++;

				// Best match (most pre-requisites matched) so far?
				if (thisMatchPrerequisiteCount > bestMatchPrerequisiteCount)
				{
					bestMatch = plane.getPlaneNumber ();
					bestMatchPrerequisiteCount = thisMatchPrerequisiteCount;
				}
			}
		}

		return bestMatch;
	}

	/**
	 * @param planeNumber Plane we want to look for races for
	 * @param db Lookup lists built over the XML database
	 * @return Random race ID that inhabits this plane
	 * @throws MomException If there are no races defined in the database that inhabit this plane
	 */
	@Override
	public final String chooseRandomRaceForPlane (final int planeNumber, final CommonDatabase db)
		throws MomException
	{
		final List<String> possibleRaces = new ArrayList<String> ();

		for (final RaceEx thisRace : db.getRaces ())
			if (thisRace.getNativePlane () == planeNumber)
				possibleRaces.add (thisRace.getRaceID ());

		if (possibleRaces.size () == 0)
			throw new MomException ("chooseRandomRaceForPlane: No races defined for plane " + planeNumber);

		final String raceID = possibleRaces.get (getRandomUtils ().nextInt (possibleRaces.size ()));

		return raceID;
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

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return AI decisions about spells
	 */
	public final SpellAI getSpellAI ()
	{
		return spellAI;
	}

	/**
	 * @param ai AI decisions about spells
	 */
	public final void setSpellAI (final SpellAI ai)
	{
		spellAI = ai;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}
}
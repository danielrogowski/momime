package momime.server.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowRank;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.utils.IPlayerPickUtils;
import momime.common.utils.ISpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.server.ai.ISpellAI;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Pick;
import momime.server.database.v0_9_4.PickType;
import momime.server.database.v0_9_4.PickTypeCountContainer;
import momime.server.database.v0_9_4.PickTypeGrantsSpells;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Wizard;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Server side only helper methods for dealing with picks
 */
public final class PlayerPickServerUtils implements IPlayerPickServerUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (PlayerPickServerUtils.class.getName ());
	
	/** Player pick utils */
	private IPlayerPickUtils playerPickUtils;
	
	/** Spell utils */
	private ISpellUtils spellUtils;
	
	/** AI decisions about spells */
	private ISpellAI spellAI;
	
	/**
	 * @param picks Player's picks to count up
	 * @param db Lookup lists built over the XML database
	 * @return Initial skill wizard will start game with - 2 per book +10 if they chose Archmage
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	@Override
	public final int getTotalInitialSkill (final List<PlayerPick> picks, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.entering (PlayerPickServerUtils.class.getName (), "getTotalInitialSkill", picks.size ());

		int total = 0;
		for (final PlayerPick thisPick : picks)
		{
			final Pick thisPickRecord = db.findPick (thisPick.getPickID (), "getTotalInitialSkill");
			if (thisPickRecord.getPickInitialSkill () != null)
				total = total + (thisPickRecord.getPickInitialSkill () * thisPick.getQuantity ());
		}

		log.exiting (PlayerPickServerUtils.class.getName (), "getTotalInitialSkill", total);
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
		log.entering (PlayerPickServerUtils.class.getName (), "findPlayerUsingWizard", wizardID);

		PlayerServerDetails result = null;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final PlayerServerDetails player = iter.next ();
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

			if ((ppk.getWizardID () != null) && (ppk.getWizardID ().equals (wizardID)))
				result = player;
		}

		log.exiting (PlayerPickServerUtils.class.getName (), "findPlayerUsingWizard", (result == null) ? "null" : result.getPlayerDescription ().getPlayerID ());
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
		log.entering (PlayerPickServerUtils.class.getName (), "findPlayerUsingStandardPhoto", standardPhotoID);

		PlayerServerDetails result = null;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((result == null) && (iter.hasNext ()))
		{
			final PlayerServerDetails player = iter.next ();
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

			if ((ppk.getStandardPhotoID () != null) && (ppk.getStandardPhotoID ().equals (standardPhotoID)))
				result = player;
		}

		log.exiting (PlayerPickServerUtils.class.getName (), "findPlayerUsingStandardPhoto", (result == null) ? "null" : result.getPlayerDescription ().getPlayerID ());
		return result;
	}

	/**
	 * Checks whether a player's custom pick selection is valid
	 *
	 * @param player Player choosing custom picks
	 * @param picks The custom picks they have requested
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return null if choices are acceptable; message to send back to client if choices aren't acceptable
	 */
	@Override
	public final String validateCustomPicks (final PlayerServerDetails player, final List<WizardPick> picks, final MomSessionDescription sd, final ServerDatabaseEx db)
	{
		log.entering (PlayerPickServerUtils.class.getName (), "validateCustomPicks", new Integer [] {player.getPlayerDescription ().getPlayerID (), picks.size ()});

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
				for (final WizardPick thisPick : picks)
				{
					final Pick pick = db.findPick (thisPick.getPick (), "validateCustomPicks");
					totalPickCost = totalPickCost + (thisPick.getQuantity () * pick.getPickCost ());
				}

				// Right total?
				if (totalPickCost != sd.getDifficultyLevel ().getHumanSpellPicks ())
					msg = "Incorrect number of custom picks chosen, please try again";
			}
			catch (final RecordNotFoundException e)
			{
				msg = "Requested a pick ID that doesn't exist";
			}
		}

		log.exiting (PlayerPickServerUtils.class.getName (), "validateCustomPicks", msg);
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
	private final ChooseInitialSpellsNowMessage countFreeSpellsLeftToChoose (final PlayerServerDetails player, final PlayerPick pick, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickServerUtils.class.getName (), "countFreeSpellsLeftToChoose", player.getPlayerDescription ().getPlayerID ());

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

					log.finest (PlayerPickServerUtils.class.getName () + ".countFreeSpellsLeftToChoose: Human player " + player.getPlayerDescription ().getPlayerName () + " needs to choose " +
						msgRank.getFreeSpellCount () + " free spells of realm " + msg.getMagicRealmID () + " rank " + msgRank.getSpellRankID ());
				}
			}
		}

		log.exiting (PlayerPickServerUtils.class.getName (), "countFreeSpellsLeftToChoose", msg);
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
	public final ChooseInitialSpellsNowMessage findRealmIDWhereWeNeedToChooseFreeSpells (final PlayerServerDetails player, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{
		log.entering (PlayerPickServerUtils.class.getName (), "findRealmIDWhereWeNeedToChooseFreeSpells", player.getPlayerDescription ().getPlayerID ());

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
				log.finest (PlayerPickServerUtils.class.getName () + ".findRealmIDWhereWeNeedToChooseFreeSpells: AI player " + player.getPlayerDescription ().getPlayerName () + " about to choose " +
					thisSpellRank.getFreeSpellCount () + " free spells of realm " + msg.getMagicRealmID () + " rank " + thisSpellRank.getSpellRankID ());

				for (int aiSpellChoices = 0; aiSpellChoices < thisSpellRank.getFreeSpellCount (); aiSpellChoices++)
					getSpellAI ().chooseFreeSpellAI (priv.getSpellResearchStatus (), msg.getMagicRealmID (), thisSpellRank.getSpellRankID (), player.getPlayerDescription ().getPlayerName (),
						db).setStatus (SpellResearchStatusID.AVAILABLE);
			}

		log.exiting (PlayerPickServerUtils.class.getName (), "findRealmIDWhereWeNeedToChooseFreeSpells", (msg == null) ? "null" : msg.getMagicRealmID ());
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
	public final String validateInitialSpellSelection (final PlayerServerDetails player, final String pickID, final List<String> spellIDs, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickServerUtils.class.getName (), "validateInitialSpellSelection",
			new String [] {new Integer (player.getPlayerDescription ().getPlayerID ()).toString (), pickID});

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

		log.exiting (PlayerPickServerUtils.class.getName (), "validateInitialSpellSelection", msg);
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
	public final String validateRaceChoice (final PlayerServerDetails player, final String raceID, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.entering (PlayerPickServerUtils.class.getName (), "validateRaceChoice",
			new String [] {new Integer (player.getPlayerDescription ().getPlayerID ()).toString (), raceID});

		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

		Race race = null;
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

		log.exiting (PlayerPickServerUtils.class.getName (), "validateRaceChoice", msg);
		return msg;
	}

	/**
	 * @param player Player to test
	 * @return True if this player has made all their pre-game selections and are waiting to start
	 */
	final boolean hasChosenAllDetails (final PlayerServerDetails player)
	{
		log.entering (PlayerPickServerUtils.class.getName (), "hasChosenAllDetails", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		final boolean isCustomPicksChosen = (priv.isCustomPicksChosen () == null) ? false : priv.isCustomPicksChosen ();

		final boolean result = ((PlayerKnowledgeUtils.hasWizardBeenChosen (ppk.getWizardID ())) && (priv.getFirstCityRaceID () != null) &&
			((!PlayerKnowledgeUtils.isCustomWizard (ppk.getWizardID ())) || (isCustomPicksChosen)));

		log.exiting (PlayerPickServerUtils.class.getName (), "hasChosenAllDetails", result);
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
		log.entering (PlayerPickServerUtils.class.getName (), "allHumanPlayersHaveChosenAllDetails");

		// If not all players have joined, then not all have chosen
		boolean result = (players.size () == sd.getMaxPlayers () - sd.getAiPlayerCount () - 2);	// -2 for raiders & monsters

		// Check each player
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((result) && (iter.hasNext ()))
			if (!hasChosenAllDetails (iter.next ()))
				result = false;

		log.exiting (PlayerPickServerUtils.class.getName (), "allHumanPlayersHaveChosenAllDetails", result);
		return result;
	}

	/**
	 * @param players List of players
	 * @param db Lookup lists built over the XML database
	 * @return List of wizards not used by human players - AI players will then pick randomly from this list
	 */
	@Override
	public final List<Wizard> listWizardsForAIPlayers (final List<PlayerServerDetails> players, final ServerDatabaseEx db)
	{
		log.entering (PlayerPickServerUtils.class.getName (), "listWizardsForAIPlayers", players.size ());

		// First get a list of all the available wizards
		final List<Wizard> availableWizards = new ArrayList<Wizard> ();
		for (final Wizard thisWizard : db.getWizard ())
			if ((!thisWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) &&
				(!thisWizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)) &&
				(findPlayerUsingStandardPhoto (players, thisWizard.getWizardID ()) == null))

				availableWizards.add (thisWizard);

		log.exiting (PlayerPickServerUtils.class.getName (), "listWizardsForAIPlayers", availableWizards.size ());
		return availableWizards;
	}

	/**
	 * Checks which plane a wizard with a certain selection of picks should start on
	 * @param picks List of picks the wizard has
	 * @param db Lookup lists built over the XML database
	 * @return Plane the wizard should start on
	 */
	@Override
	public final int startingPlaneForWizard (final List<PlayerPick> picks, final ServerDatabaseEx db)
	{
		log.entering (PlayerPickServerUtils.class.getName (), "startingPlaneForWizard");

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

		log.exiting (PlayerPickServerUtils.class.getName (), "startingPlaneForWizard", bestMatch);
		return bestMatch;
	}

	/**
	 * @param planeNumber Plane we want to look for races for
	 * @param db Lookup lists built over the XML database
	 * @return Random race ID that inhabits this plane
	 * @throws MomException If there are no races defined in the database that inhabit this plane
	 */
	@Override
	public final String chooseRandomRaceForPlane (final int planeNumber, final ServerDatabaseEx db)
		throws MomException
	{
		log.entering (PlayerPickServerUtils.class.getName (), "chooseRandomRaceForPlane", planeNumber);

		final List<String> possibleRaces = new ArrayList<String> ();

		for (final Race thisRace : db.getRace ())
			if (thisRace.getNativePlane () == planeNumber)
				possibleRaces.add (thisRace.getRaceID ());

		if (possibleRaces.size () == 0)
			throw new MomException ("chooseRandomRaceForPlane: No races defined for plane " + planeNumber);

		final String raceID = possibleRaces.get (RandomUtils.getGenerator ().nextInt (possibleRaces.size ()));

		log.entering (PlayerPickServerUtils.class.getName (), "chooseRandomRaceForPlane", raceID);
		return raceID;
	}

	/**
	 * @return Player pick utils
	 */
	public final IPlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final IPlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Spell utils
	 */
	public final ISpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final ISpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return AI decisions about spells
	 */
	public final ISpellAI getSpellAI ()
	{
		return spellAI;
	}

	/**
	 * @param ai AI decisions about spells
	 */
	public final void setSpellAI (final ISpellAI ai)
	{
		spellAI = ai;
	}
}

package momime.server.utils;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.WizardEx;
import momime.common.messages.PlayerPick;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.server.MomSessionVariables;

/**
 * Server side only helper methods for dealing with picks
 */
public interface PlayerPickServerUtils
{
	/**
	 * @param picks Player's picks to count up
	 * @param db Lookup lists built over the XML database
	 * @return Initial skill wizard will start game with - 2 per book +10 if they chose Archmage
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public int getTotalInitialSkill (final List<PlayerPick> picks, final CommonDatabase db) throws RecordNotFoundException;

	/**
	 * @param players List of players
	 * @param standardPhotoID Standard photo ID we want to pick
	 * @return Player with the specified Standard photo ID, or null if none are found
	 */
	public PlayerServerDetails findPlayerUsingStandardPhoto (final List<PlayerServerDetails> players, final String standardPhotoID);

	/**
	 * Checks whether a player's custom pick selection is valid
	 *
	 * @param player Player choosing custom picks
	 * @param picks The custom picks they have requested
	 * @param humanSpellPicks Number of picks that human players get at the start of the game, from the session description - difficulty level
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return null if choices are acceptable; message to send back to client if choices aren't acceptable
	 * @throws RecordNotFoundException If the wizard isn't found in the list
	 */
	public String validateCustomPicks (final PlayerServerDetails player, final List<PickAndQuantity> picks, final int humanSpellPicks, final MomSessionVariables mom)
		throws RecordNotFoundException;

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
	public ChooseInitialSpellsNowMessage findRealmIDWhereWeNeedToChooseFreeSpells (final PlayerServerDetails player, final CommonDatabase db)
		throws MomException, RecordNotFoundException;

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
	public String validateInitialSpellSelection (final PlayerServerDetails player, final String pickID, final List<String> spellIDs, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * @param player Player who wants to choose a race
	 * @param raceID Race they wish to choose
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choice isn't acceptable
	 * @throws RecordNotFoundException If we choose a race whose native plane can't be found
	 */
	public String validateRaceChoice (final PlayerServerDetails player, final String raceID, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * Tests whether everyone has finished pre-game selections and is ready to start
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if all players have chosen all details to start game
	 * @throws RecordNotFoundException If one of the wizard isn't found in the list
	 */
	public boolean allPlayersHaveChosenAllDetails (final MomSessionVariables mom)
		throws RecordNotFoundException;

	/**
	 * Used when reloading multiplayer games to test whether all necessary players have joined back into the session and it can continue
	 * @param players List of players
	 * @return True if all human players are connected
	 */
	public boolean allPlayersAreConnected (final List<PlayerServerDetails> players);
	
	/**
	 * @param players List of players
	 * @param db Lookup lists built over the XML database
	 * @return List of wizards not used by human players - AI players will then pick randomly from this list
	 */
	public List<WizardEx> listWizardsForAIPlayers (final List<PlayerServerDetails> players, final CommonDatabase db);

	/**
	 * Checks which plane a wizard with a certain selection of picks should start on
	 * @param picks List of picks the wizard has
	 * @param db Lookup lists built over the XML database
	 * @return Plane the wizard should start on
	 */
	public int startingPlaneForWizard (final List<PlayerPick> picks, final CommonDatabase db);

	/**
	 * @param planeNumber Plane we want to look for races for
	 * @param db Lookup lists built over the XML database
	 * @return Random race ID that inhabits this plane
	 * @throws MomException If there are no races defined in the database that inhabit this plane
	 */
	public String chooseRandomRaceForPlane (final int planeNumber, final CommonDatabase db)
		throws MomException;
}
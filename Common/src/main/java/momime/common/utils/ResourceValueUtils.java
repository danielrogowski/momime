package momime.common.utils;

import java.util.List;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.PlayerPick;

/**
 * Methods for working with list of MomResourceValues
 */
public interface ResourceValueUtils
{
	/**
	 * Careful, there is a Delphi method named TMomPlayerResourceValues.FindAmountPerTurnForProductionType, but that does more than
	 * simply look the value up in the list, see calculateAmountPerTurnForProductionType method below
	 *
	 * This method is the equivalent of TMomPlayerResourceValues.FindProductionType, which *only* searches for the value in the list
	 *
	 * @param resourceList List of resources to search through
     * @param productionTypeID Type of production to look up
     * @return How much of this production type that this player generates per turn
     */
	public int findAmountPerTurnForProductionType (final List<MomResourceValue> resourceList, final String productionTypeID);

	/**
	 * @param resourceList List of resources to search through
     * @param productionTypeID Type of production to look up
     * @return How much of this production type that this player has stored
     */
	public int findAmountStoredForProductionType (final List<MomResourceValue> resourceList, final String productionTypeID);

	/**
	 * @param resourceList List of resources to update
	 * @param productionTypeID Which type of production we are modifing
	 * @param amountToAdd Amount to modify their per turn production by
	 */
	public void addToAmountPerTurn (final List<MomResourceValue> resourceList, final String productionTypeID, final int amountToAdd);

	/**
	 * @param resourceList List of resources to update
	 * @param productionTypeID Which type of production we are modifing
	 * @param amountToAdd Amount to modify their stored production by
	 */
	public void addToAmountStored (final List<MomResourceValue> resourceList, final String productionTypeID, final int amountToAdd);

	/**
	 * Note Delphi version could either erase the values for one player or all players
	 * Java version operates only on one player because each player now has their own resource list
	 *
	 * @param resourceList List of resources to update
	 */
	public void zeroAmountsPerTurn (final List<MomResourceValue> resourceList);

	/**
	 * Note Delphi version could either erase the values for one player or all players
	 * Java version operates only on one player because each player now has their own resource list
	 *
	 * @param resourceList List of resources to update
	 */
	public void zeroAmountsStored (final List<MomResourceValue> resourceList);

    /**
	 * @param resourceList List of resources to search through
     * @return The specified player's casting skill, in mana-points-spendable/turn instead of raw skill points
     */
	public int calculateBasicCastingSkill (final List<MomResourceValue> resourceList);

    /**
	 * @param resourceList List of resources to search through
	 * @param playerDetails Details about the player whose casting skill we want
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param includeBonusFromHeroesAtFortress Whether to add on bonus from heroes parked at the Wizard's Fortress, or only from Archmage
     * @return The specified player's casting skill, including bonuses from Archmage and heroes
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
     */
	public int calculateModifiedCastingSkill (final List<MomResourceValue> resourceList, final PlayerPublicDetails playerDetails,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db, final boolean includeBonusFromHeroesAtFortress)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * There isn't a "calculateBasicResearch" and "calculateModifiedResearch" since basic research is calculated with calculateAmountPerTurnForProductionType.
	 * So this method adds on the modified bonus onto basic research, for which the only source is from Sage heroes.
	 * 
	 * @param playerID Player we want to calculate modified research for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Research bonus from units
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public int calculateResearchFromUnits (final int playerID, final List<? extends PlayerPublicDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
     * This does include splitting magic power into mana/research/skill improvement, but does not include selling 2 rations to get 1 gold
     * Delphi method is TMomPlayerResourceValues.FindAmountPerTurnForProductionType
     *
     * @param privateInfo Private info of the player whose production amount we are calculating
     * @param picks Picks of the player whose production amount we are calculating
     * @param productionTypeID Type of production to calculate
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
     * @return How much of this production type that this player gets per turn
	 * @throws MomException If we find an invalid casting reduction type
	 * @throws RecordNotFoundException If we look for a particular record that we expect to be present in the XML file and we can't find it
     */
	public int calculateAmountPerTurnForProductionType (final MomPersistentPlayerPrivateKnowledge privateInfo, final List<PlayerPick> picks,
		final String productionTypeID, final SpellSetting spellSettings, final CommonDatabase db)
    	throws MomException, RecordNotFoundException;
}
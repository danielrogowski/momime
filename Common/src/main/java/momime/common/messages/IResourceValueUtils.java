package momime.common.messages;

import java.util.List;

import momime.common.MomException;
import momime.common.database.ICommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomResourceValue;
import momime.common.messages.v0_9_4.PlayerPick;

/**
 * Methods for working with list of MomResourceValues
 */
public interface IResourceValueUtils
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
	public int calculateCastingSkillOfPlayer (final List<MomResourceValue> resourceList);

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
		final String productionTypeID, final SpellSettingData spellSettings, final ICommonDatabase db)
    	throws MomException, RecordNotFoundException;
}

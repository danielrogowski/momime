package momime.server.ai;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.PlayerPick;
import momime.server.MomSessionVariables;

/**
 * For calculating relation scores between two wizards
 */
public interface RelationAI
{
	/**
	 * @param firstPicks First wizard's picks
	 * @param secondPicks Second wizard's picks
	 * @param db Lookup lists built over the XML database
	 * @return Natural relation between the two wizards based on their spell books (wiki calls this startingRelation)
	 */
	public int calculateBaseRelation (final List<PlayerPick> firstPicks, final List<PlayerPick> secondPicks, final CommonDatabase db);
	
	/**
	 * @param player AI player whose turn to take
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If a wizard who owns a city can't be found
	 */
	public void updateVisibleRelationDueToUnitsInOurBorder (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException;
	
	/**
	 * @param player AI player to move their visible relations a little bit back towards base relation
	 */
	public void slideTowardsBaseRelation (final PlayerServerDetails player);
	
	/**
	 * @param player AI player to verify all their visibleRelation values are within the capped range
	 */
	public void capVisibleRelations (final PlayerServerDetails player);
	
	/**
	 * @param wizardDetails Wizard to receive bonus
	 * @param bonus Amount of bonus
	 */
	public void bonusToVisibleRelation (final DiplomacyWizardDetails wizardDetails, final int bonus);
}
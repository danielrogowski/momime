package momime.server.utils;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.MomException;
import momime.common.database.HeroItem;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.NumberedHeroItem;
import momime.server.MomSessionVariables;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Server only helper methods for dealing with hero items
 */
public interface HeroItemServerUtils
{
	/**
	 * @param item Hero item stats
	 * @param gsk General server knowledge
	 * @return Hero item with allocated URN
	 */
	public NumberedHeroItem createNumberedHeroItem (final HeroItem item, final MomGeneralServerKnowledge gsk);

	/**
	 * @param player Player who wants to craft an item
	 * @param spell The spell being used to craft it (Enchant Item or Create Artifact)
	 * @param heroItem The details of the item they want to create
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Error message describing why they can't create the desired item, or null if all validation passes
	 * @throws RecordNotFoundException If player requested a bonus that doesn't exist
	 * @throws MomException If there any serious failures in logic
	 */
	public String validateHeroItem (final PlayerServerDetails player, final Spell spell, final HeroItem heroItem, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException;
}
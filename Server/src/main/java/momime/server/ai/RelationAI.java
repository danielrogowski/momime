package momime.server.ai;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.messages.PlayerPick;

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
}
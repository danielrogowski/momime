package momime.client.utils;

import momime.common.database.Pick;

/**
 * Client side only helper methods for dealing with players' pick choices
 */
public interface PlayerPickClientUtils
{
	/**
	 * @param pick Pick we want pre-requisites for
	 * @return Description of the pre-requisites for this pick (e.g. "2 Spell Books in any 3 Realms of Magic"), or null if it has no pre-requisites
	 */
	public String describePickPreRequisites (final Pick pick);
}
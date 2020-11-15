package momime.client.utils;

import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;

/**
 * Client side only helper methods for dealing with players' pick choices
 */
public interface PlayerPickClientUtils
{
	/**
	 * @param pick Pick we want pre-requisites for
	 * @return Description of the pre-requisites for this pick (e.g. "2 Spell Books in any 3 Realms of Magic"), or null if it has no pre-requisites
	 * @throws RecordNotFoundException If one of the prerequisite picks cannot be found
	 */
	public String describePickPreRequisites (final Pick pick) throws RecordNotFoundException;
	
	/**
	 * @param pick Magic realm we want a book image for
	 * @return Randomly selected image for this type of pick
	 * @throws RecordNotFoundException If this wizard has no combat music playlists defined
	 */
	public String chooseRandomBookImageFilename (final Pick pick) throws RecordNotFoundException;	
}
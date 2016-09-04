package momime.client.graphics.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.random.RandomUtils;

import momime.client.graphics.database.v0_9_8.Pick;
import momime.common.database.RecordNotFoundException;

/**
 * Provides method for picking a random book image, to minimize situations where the un-typecast BookImage class has to be referenced outside of the graphics DB package
 */
public final class PickGfx extends Pick
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (PickGfx.class);
	
	/** Random utils */
	private RandomUtils randomUtils;

	/**
	 * @return Randomly selected image for this type of pick
	 * @throws RecordNotFoundException If this wizard has no combat music playlists defined
	 */
	public final String chooseRandomBookImageFilename () throws RecordNotFoundException
	{
		log.trace ("Entering chooseRandomBookImageFilename");
		
		if (getBookImageFile ().size () == 0)
			throw new RecordNotFoundException ("BookImage", null, "chooseRandomBookImageFilename");
		
		final String filename = getBookImageFile ().get (getRandomUtils ().nextInt (getBookImageFile ().size ()));
		log.trace ("Exiting chooseRandomBookImageFilename = " + filename);
		return filename;
	}
	
	/**
	 * @return Random utils
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random utils
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}
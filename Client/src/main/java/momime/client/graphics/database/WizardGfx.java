package momime.client.graphics.database;

import momime.client.graphics.database.v0_9_5.Wizard;
import momime.client.graphics.database.v0_9_5.WizardCombatPlayList;
import momime.common.database.RecordNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.random.RandomUtils;

/**
 * Provides method for picking a random playlist, to minimize situations where the un-typecast BookImage class has to be referenced outside of the graphics DB package
 */
public final class WizardGfx extends Wizard
{
	/** Class logger */
	private final Log log = LogFactory.getLog (WizardGfx.class);
	
	/** Random utils */
	private RandomUtils randomUtils;
	
	/**
	 * @return Randomly selected playlist for this wizard's combat music
	 * @throws RecordNotFoundException If this wizard has no combat music playlists defined
	 */
	public final String chooseRandomCombatPlayListID () throws RecordNotFoundException
	{
		log.trace ("Entering chooseRandomCombatPlayListID");
		
		if (getCombatPlayList ().size () == 0)
			throw new RecordNotFoundException (WizardCombatPlayList.class, null, "chooseRandomCombatPlayListID");
		
		final String playListID = getCombatPlayList ().get (getRandomUtils ().nextInt (getCombatPlayList ().size ())).getPlayListID ();
		log.trace ("Exiting chooseRandomCombatPlayListID = " + playListID);
		return playListID;
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
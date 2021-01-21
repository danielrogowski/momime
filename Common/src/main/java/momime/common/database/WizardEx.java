package momime.common.database;

import com.ndg.random.RandomUtils;

/**
 * Provides method for picking a random playlist, to minimize situations where the un-typecast BookImage class has to be referenced outside of the graphics DB package
 */
public final class WizardEx extends Wizard
{
	/** Random utils */
	private RandomUtils randomUtils;
	
	/**
	 * @return Randomly selected playlist for this wizard's combat music
	 * @throws RecordNotFoundException If this wizard has no combat music playlists defined
	 */
	public final String chooseRandomCombatPlayListID () throws RecordNotFoundException
	{
		if (getCombatPlayList ().size () == 0)
			throw new RecordNotFoundException ("WizardCombatPlayList", null, "chooseRandomCombatPlayListID");
		
		final String playListID = getCombatPlayList ().get (getRandomUtils ().nextInt (getCombatPlayList ().size ()));
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
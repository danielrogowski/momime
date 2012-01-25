package momime.server.utils;

import java.util.Random;

/**
 * Random number generator used on server
 */
public final class RandomUtils
{
	/**
	 * the actual random number generator
	 */
	private static final Random generator = new Random(System.currentTimeMillis ());

	/**
	 * @return the generator
	 */
	public static final Random getGenerator()
	{
		return generator;
	}

	/**
	 * Prevent instantiation
	 */
	private RandomUtils ()
	{
	}
}
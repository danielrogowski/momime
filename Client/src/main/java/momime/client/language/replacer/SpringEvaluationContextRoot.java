package momime.client.language.replacer;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.database.CommonDatabase;

/**
 * Allows EL expressions in the help text to access both the common DB and graphics DB
 */
public final class SpringEvaluationContextRoot
{
	/** Graphics database */
	private final GraphicsDatabaseEx graphicsDB;

	/** Multiplayer client */
	private final MomClient client;
	
	/**
	 * This is a constructor rather than setter methods, as I don't want to expose a getter to the client from where some EL expression could access just about anything
	 * 
	 * @param aGraphicsDB Graphics database
	 * @param aClient Multiplayer client
	 */
	public SpringEvaluationContextRoot (final GraphicsDatabaseEx aGraphicsDB, final MomClient aClient)
	{
		graphicsDB = aGraphicsDB;
		client = aClient;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @return Client DB received from server when we joined a game
	 */
	public final CommonDatabase getClientDB ()
	{
		return client.getClientDB ();
	}
}
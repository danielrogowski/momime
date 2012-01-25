package momime.server.messages;

import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Pick;

/**
 * Server-only methods for working with list of PlayerPicks
 */
public final class ServerPlayerPickUtils
{
	/**
	 * @param picks Player's picks to count up
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger
	 * @return Initial skill wizard will start game with - 2 per book +10 if they chose Archmage
	 * @throws RecordNotFoundException If we have a pick in our list which can't be found in the db
	 */
	public final static int getTotalInitialSkill (final List<PlayerPick> picks, final ServerDatabaseLookup db, final Logger debugLogger) throws RecordNotFoundException
	{
		debugLogger.entering (ServerPlayerPickUtils.class.getName (), "getTotalInitialSkill", picks.size ());

		int total = 0;
		for (final PlayerPick thisPick : picks)
		{
			final Pick thisPickRecord = db.findPick (thisPick.getPickID (), "getTotalInitialSkill");
			if (thisPickRecord.getPickInitialSkill () != null)
				total = total + (thisPickRecord.getPickInitialSkill () * thisPick.getQuantity ());
		}

		debugLogger.exiting (ServerPlayerPickUtils.class.getName (), "getTotalInitialSkill", total);
		return total;
	}

	/**
	 * Prevent instantiation
	 */
	private ServerPlayerPickUtils ()
	{
	}
}

package momime.server.utils;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.messages.v0_9_4.MomTransientPlayerPublicKnowledge;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Server side only helper methods for dealing with players
 */
public final class PlayerServerUtilsImpl implements PlayerServerUtils
{
	/** Class logger */
	private final Logger log = Logger.getLogger (PlayerServerUtilsImpl.class.getName ());

	/**
	 * @param players List of players to check
	 * @param turnNo Turn number to check
	 * @return True if all players have allocated movement for the specified turn number; false if still waiting for some
	 */
	@Override
	public final boolean allPlayersFinishedAllocatingMovement (final List<PlayerServerDetails> players, final int turnNo)
	{
		log.entering (PlayerServerUtilsImpl.class.getName (), "allPlayersFinishedAllocatingMovement");
		
		boolean allAllocated = true;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((allAllocated) && (iter.hasNext ()))
		{
			final PlayerServerDetails thisPlayer = iter.next ();
			final MomTransientPlayerPublicKnowledge tpk = (MomTransientPlayerPublicKnowledge) thisPlayer.getTransientPlayerPublicKnowledge ();
			
			if (tpk.getMovementAllocatedForTurnNumber () < turnNo)
				allAllocated = false;
		}			

		log.exiting (PlayerServerUtilsImpl.class.getName (), "allPlayersFinishedAllocatingMovement", allAllocated);
		return allAllocated;
	}
}

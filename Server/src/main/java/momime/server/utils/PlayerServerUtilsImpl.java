package momime.server.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Server side only helper methods for dealing with players
 */
public final class PlayerServerUtilsImpl implements PlayerServerUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (PlayerServerUtilsImpl.class);

	/**
	 * @param players List of players to check
	 * @param turnNo Turn number to check
	 * @return True if all players have allocated movement for the specified turn number; false if still waiting for some
	 */
	@Override
	public final boolean allPlayersFinishedAllocatingMovement (final List<PlayerServerDetails> players, final int turnNo)
	{
		log.trace ("Entering allPlayersFinishedAllocatingMovement: Turn " + turnNo);
		
		boolean allAllocated = true;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((allAllocated) && (iter.hasNext ()))
		{
			final PlayerServerDetails thisPlayer = iter.next ();
			final MomTransientPlayerPublicKnowledge tpk = (MomTransientPlayerPublicKnowledge) thisPlayer.getTransientPlayerPublicKnowledge ();
			
			if (tpk.getMovementAllocatedForTurnNumber () < turnNo)
				allAllocated = false;
		}			

		log.trace ("Exiting allPlayersFinishedAllocatingMovement = " + allAllocated);
		return allAllocated;
	}
}

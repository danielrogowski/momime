package momime.server.utils;

import java.util.Iterator;
import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.messages.MomTransientPlayerPublicKnowledge;

/**
 * Server side only helper methods for dealing with players
 */
public final class PlayerServerUtilsImpl implements PlayerServerUtils
{
	/**
	 * @param players List of players to check
	 * @param turnNo Turn number to check
	 * @return True if all players have allocated movement for the specified turn number; false if still waiting for some
	 */
	@Override
	public final boolean allPlayersFinishedAllocatingMovement (final List<PlayerServerDetails> players, final int turnNo)
	{
		boolean allAllocated = true;
		final Iterator<PlayerServerDetails> iter = players.iterator ();
		while ((allAllocated) && (iter.hasNext ()))
		{
			final PlayerServerDetails thisPlayer = iter.next ();
			final MomTransientPlayerPublicKnowledge tpk = (MomTransientPlayerPublicKnowledge) thisPlayer.getTransientPlayerPublicKnowledge ();
			
			if (tpk.getMovementAllocatedForTurnNumber () < turnNo)
				allAllocated = false;
		}			

		return allAllocated;
	}
}
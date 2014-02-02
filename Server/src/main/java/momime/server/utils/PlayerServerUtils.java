package momime.server.utils;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Server side only helper methods for dealing with players
 */
public interface PlayerServerUtils
{
	/**
	 * @param players List of players to check
	 * @param turnNo Turn number to check
	 * @return True if all players have allocated movement for the specified turn number; false if still waiting for some
	 */
	public boolean allPlayersFinishedAllocatingMovement (final List<PlayerServerDetails> players, final int turnNo);
}

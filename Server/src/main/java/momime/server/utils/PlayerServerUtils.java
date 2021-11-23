package momime.server.utils;

import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.TurnSystem;

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
	
	/**
	 * Used to stop players taking action when its someone else's turn (one at a time turns) or after they hit Next Turn (simultaneous turns)
	 * 
	 * @param player Player trying to take action
	 * @param gpk Public knowledge structure
	 * @param turnSystem Turn system from session description
	 * @return Whether its currently this player's turn or not
	 */
	public boolean isPlayerTurn (final PlayerServerDetails player, final MomGeneralPublicKnowledge gpk, final TurnSystem turnSystem);
}
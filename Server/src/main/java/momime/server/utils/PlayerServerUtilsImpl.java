package momime.server.utils;

import java.util.Iterator;
import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.TurnSystem;

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
	
	/**
	 * Used to stop players taking action when its someone else's turn (one at a time turns) or after they hit Next Turn (simultaneous turns)
	 * 
	 * @param player Player trying to take action
	 * @param gpk Public knowledge structure
	 * @param turnSystem Turn system from session description
	 * @return Whether its currently this player's turn or not
	 */
	@Override
	public final boolean isPlayerTurn (final PlayerServerDetails player, final MomGeneralPublicKnowledge gpk, final TurnSystem turnSystem)
	{
		final boolean valid;
		
		if (turnSystem == TurnSystem.ONE_PLAYER_AT_A_TIME)
			valid = (gpk.getCurrentPlayerID () == player.getPlayerDescription ().getPlayerID ());
		else
		{
			final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
			valid = trans.getMovementAllocatedForTurnNumber () < gpk.getTurnNumber ();
		}

		return valid;		
	}
}
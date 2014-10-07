package momime.common.utils;

import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MomScheduledCombat;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Util methods for dealing with scheduled combats in simultaneous turns games
 */
public interface ScheduledCombatUtils
{
	/**
	 * @param combats List of scheduled combats to search
	 * @param scheduledCombatURN scheduledCombatURN to search for
	 * @return Combat with requested key; or null if not found
	 */
	public MomScheduledCombat findScheduledCombatURN (final List<MomScheduledCombat> combats, final Integer scheduledCombatURN);

	/**
	 * @param combats List of scheduled combats to search
	 * @param scheduledCombatURN scheduledCombatURN to search for
	 * @param caller The routine that was looking for the value
	 * @return Combat with requested key
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	public MomScheduledCombat findScheduledCombatURN (final List<MomScheduledCombat> combats, final Integer scheduledCombatURN, final String caller)
		throws RecordNotFoundException;
	
	/**
	 * @param combat Combat to check
	 * @param besidesWho Human player we already know about
	 * @param players List of known players
	 * @return The other human player involved in the combat; null if the other player is an AI player
	 * @throws PlayerNotFoundException If one of the players listed for the combat can't be found in the players list
	 */
	public PlayerPublicDetails determineOtherHumanPlayer (final MomScheduledCombat combat, final PlayerPublicDetails besidesWho, final List<? extends PlayerPublicDetails> players)
		throws PlayerNotFoundException;
}
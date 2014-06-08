package momime.common.utils;

import java.util.Iterator;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MomScheduledCombat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Util methods for dealing with scheduled combats in simultaneous turns games
 */
public final class ScheduledCombatUtilsImpl implements ScheduledCombatUtils
{
	/** Class logger */
	final Log log = LogFactory.getLog (ScheduledCombatUtilsImpl.class);

	/**
	 * @param combats List of scheduled combats to search
	 * @param scheduledCombatURN scheduledCombatURN to search for
	 * @return Combat with requested key; or null if not found
	 */
	@Override
	public final MomScheduledCombat findScheduledCombatURN (final List<MomScheduledCombat> combats, final Integer scheduledCombatURN)
	{
		log.trace ("Entering findScheduledCombatURN: " + scheduledCombatURN);

		MomScheduledCombat found = null;
		final Iterator<MomScheduledCombat> iter = combats.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MomScheduledCombat thisCombat = iter.next ();
			if (thisCombat.getScheduledCombatURN () == scheduledCombatURN)
				found = thisCombat;
		}
		
		log.trace ("Exiting findScheduledCombatURN = " + found);
		return found;
	}

	/**
	 * @param combats List of scheduled combats to search
	 * @param scheduledCombatURN scheduledCombatURN to search for
	 * @param caller The routine that was looking for the value
	 * @return Combat with requested key
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	@Override
	public final MomScheduledCombat findScheduledCombatURN (final List<MomScheduledCombat> combats, final Integer scheduledCombatURN, final String caller)
		throws RecordNotFoundException
	{
		log.trace ("Entering findScheduledCombatURN: " + scheduledCombatURN);

		final MomScheduledCombat found = findScheduledCombatURN (combats, scheduledCombatURN);
		
		if (found == null)
			throw new RecordNotFoundException ("ScheduledCombats", scheduledCombatURN, caller);
		
		log.trace ("Exiting findScheduledCombatURN = " + found);
		return found;
	}
	
	/**
	 * @param combat Combat to check
	 * @param besidesWho Human player we already know about
	 * @param players List of known players
	 * @return The other human player involved in the combat; null if there is no other player or its an AI player
	 * @throws PlayerNotFoundException If one of the players listed for the combat can't be found in the players list
	 */
	@Override
	public final PlayerPublicDetails determineOtherHumanPlayer (final MomScheduledCombat combat, final PlayerPublicDetails besidesWho, final List<? extends PlayerPublicDetails> players)
		throws PlayerNotFoundException
	{
		log.trace ("Entering determineOtherHumanPlayer: " + combat.getScheduledCombatURN () + ", Player ID " + besidesWho.getPlayerDescription ().getPlayerID ());
		
		final PlayerPublicDetails ohp;
		if (combat.isWalkInWithoutAFight ())
			ohp = null;
		
		else
		{
			final Integer otherPlayerID;
			if (besidesWho.getPlayerDescription ().getPlayerID ().intValue () == combat.getAttackingPlayerID ())
				otherPlayerID = combat.getDefendingPlayerID ();
			else if (besidesWho.getPlayerDescription ().getPlayerID ().equals (combat.getDefendingPlayerID ()))
				otherPlayerID = combat.getAttackingPlayerID ();
			else
				// besidesWho isn't either of the 2 players in the combat, so just return null
				otherPlayerID = null;
			
			if (otherPlayerID == null)
				ohp = null;
			else
			{
				final PlayerPublicDetails otherPlayer = MultiplayerSessionUtils.findPlayerWithID (players, otherPlayerID, "determineOtherHumanPlayer");
				ohp = (otherPlayer.getPlayerDescription ().isHuman ()) ? otherPlayer : null;
			}			
		}
		
		log.trace ("Exiting determineOtherHumanPlayer = " + ((ohp == null) ? "null" : ohp.getPlayerDescription ().getPlayerID ().toString ()));
		return ohp;
	}
}

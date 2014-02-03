package momime.common.utils;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MomScheduledCombat;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Util methods for dealing with scheduled combats in simultaneous turns games
 */
public final class ScheduledCombatUtilsImpl implements ScheduledCombatUtils
{
	/** Class logger */
	final Logger log = Logger.getLogger (ScheduledCombatUtilsImpl.class.getName ());

	/**
	 * @param combats List of scheduled combats to search
	 * @param scheduledCombatURN scheduledCombatURN to search for
	 * @return Combat with requested key; or null if not found
	 */
	@Override
	public final MomScheduledCombat findScheduledCombatURN (final List<MomScheduledCombat> combats, final Integer scheduledCombatURN)
	{
		log.entering (ScheduledCombatUtilsImpl.class.getName (), "findScheduledCombatURN", scheduledCombatURN);

		MomScheduledCombat found = null;
		final Iterator<MomScheduledCombat> iter = combats.iterator ();
		while ((found == null) && (iter.hasNext ()))
		{
			final MomScheduledCombat thisCombat = iter.next ();
			if (thisCombat.getScheduledCombatURN () == scheduledCombatURN)
				found = thisCombat;
		}
		
		log.exiting (ScheduledCombatUtilsImpl.class.getName (), "findScheduledCombatURN", found);
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
		log.entering (ScheduledCombatUtilsImpl.class.getName (), "findScheduledCombatURN", new String [] {new Integer (scheduledCombatURN).toString (), caller});

		final MomScheduledCombat found = findScheduledCombatURN (combats, scheduledCombatURN);
		
		if (found == null)
			throw new RecordNotFoundException ("ScheduledCombats", scheduledCombatURN, caller);
		
		log.exiting (ScheduledCombatUtilsImpl.class.getName (), "findScheduledCombatURN", found);
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
		log.entering (ScheduledCombatUtilsImpl.class.getName (), "determineOtherHumanPlayer",
			new Integer [] {combat.getScheduledCombatURN (), besidesWho.getPlayerDescription ().getPlayerID ()});
		
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
		
		log.exiting (ScheduledCombatUtilsImpl.class.getName (), "determineOtherHumanPlayer",
			(ohp == null) ? "null" : ohp.getPlayerDescription ().getPlayerID ().toString ());
		return ohp;
	}
}

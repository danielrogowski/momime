package momime.server.process;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.AddScheduledCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.PlayerCombatRequestStatusMessage;
import momime.common.messages.servertoclient.v0_9_4.ScheduledCombatWalkInWithoutAFightMessage;
import momime.common.messages.servertoclient.v0_9_4.ShowListAndOtherScheduledCombatsMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateOtherScheduledCombatsMessage;
import momime.common.messages.v0_9_4.MomScheduledCombat;
import momime.common.messages.v0_9_4.MomTransientPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MoveResultsInAttackTypeID;
import momime.common.utils.ScheduledCombatUtils;
import momime.server.MomSessionVariables;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;

import com.ndg.multiplayer.server.MultiplayerServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Combat scheduler is only used in Simultaneous turns games, where all the combats are queued up and processed in one go at the end of the turn.
 * The combat scheduled handles requests from clients about which order they wish to run combats in, and takes care of things like
 * when player A is busy in one combat, notifying all other clients that they cannot request a combat against player A until that one finishes.   
 */
public final class CombatSchedulerImpl implements CombatScheduler
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CombatSchedulerImpl.class.getName ());
	
	/** Server-side multiplayer utils */
	private MultiplayerServerUtils multiplayerServerUtils;
	
	/** Scheduled combat utils */
	private ScheduledCombatUtils scheduledCombatUtils; 
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/**
	 * Records details of a combat that needs to be added to the combat scheduler, allocating it a new scheduledCombatURN in the process
	 * 
	 * @param gsk Server knowledge structure to add the combat to
	 * @param defendingLocation Location being attacked
	 * @param attackingFrom Attacking from where
	 * @param defendingPlayer Player being attacked; may be null if attacking an empty node/lair/tower
	 * @param attackingPlayer Player owning the attacking units
	 * @param attackingUnitURNs Which specific units are attacking (may not be everybody standing at in attackingFrom)
	 * @param typeOfCombat Type of combat initiated
	 * @param monsterUnitID Type of unit scouted here; empty string if "known to be empty"
	 */
	@Override
	public final void addScheduledCombatGeneratedURN (final MomGeneralServerKnowledge gsk,
		final OverlandMapCoordinatesEx defendingLocation, final OverlandMapCoordinatesEx attackingFrom,
		final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer, final List<Integer> attackingUnitURNs,
		final MoveResultsInAttackTypeID typeOfCombat, final String monsterUnitID)
	{
		log.entering (CombatSchedulerImpl.class.getName (), "addScheduledCombatGeneratedURN");
		
		// Populate combat details
		final MomScheduledCombat combat = new MomScheduledCombat ();
		combat.setScheduledCombatURN (gsk.getNextFreeScheduledCombatURN ());
		combat.setTypeOfCombat (typeOfCombat);
		combat.setMonsterUnitID (monsterUnitID);
		combat.getAttackingUnitURN ().addAll (attackingUnitURNs);
		
		combat.setAttackingPlayerID (attackingPlayer.getPlayerDescription ().getPlayerID ());
		if (defendingPlayer != null)
			combat.setDefendingPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
		
	    // Copy locations into new objects, to be safe
		final OverlandMapCoordinatesEx defLoc = new OverlandMapCoordinatesEx ();
		defLoc.setX (defendingLocation.getX ());
		defLoc.setY (defendingLocation.getY ());
		defLoc.setZ (defendingLocation.getZ ());
		combat.setDefendingLocation (defLoc);
		
		final OverlandMapCoordinatesEx attFrom = new OverlandMapCoordinatesEx ();
		attFrom.setX (attackingFrom.getX ());
		attFrom.setY (attackingFrom.getY ());
		attFrom.setZ (attackingFrom.getZ ());
		combat.setAttackingFrom (attFrom);
	    
		// Add to list
		gsk.getScheduledCombat ().add (combat);
		
		// Record next URN to use
		gsk.setNextFreeScheduledCombatURN (gsk.getNextFreeScheduledCombatURN () + 1);
		
		log.exiting (CombatSchedulerImpl.class.getName (), "addScheduledCombatGeneratedURN", combat.getScheduledCombatURN ());
	}
	
	/**
	 * Updates whether a player is involved in a simultaneous turns combat or not, on both the server & all clients
	 * @param player Player who is now busy or not
	 * @param players List of players in session
	 * @param currentlyPlayingCombat Whether player is now busy or not
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void informClientsOfPlayerBusyInCombat (final PlayerServerDetails player, final List<PlayerServerDetails> players, final boolean currentlyPlayingCombat)
		throws JAXBException, XMLStreamException
	{
		log.entering (CombatSchedulerImpl.class.getName (), "informClientsOfPlayerBusyInCombat",
			new String [] {player.getPlayerDescription ().getPlayerID ().toString (), new Boolean (currentlyPlayingCombat).toString ()});
		
		// Update on server
		// Note if we're entering combat, then we no longer have a pending request and so clear out any requested combat ID
		// If we ending combat, then we also don't have a pending request and so clear out any requested combat ID
		final MomTransientPlayerPublicKnowledge pub = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
		pub.setCurrentlyPlayingCombat (currentlyPlayingCombat);
		pub.setScheduledCombatUrnRequested (null);
		
		// Update on clients
		final PlayerCombatRequestStatusMessage msg = new PlayerCombatRequestStatusMessage ();
		msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
		msg.setCurrentlyPlayingCombat (currentlyPlayingCombat);
		getMultiplayerServerUtils ().sendMessageToAllClients (players, msg);

		log.exiting (CombatSchedulerImpl.class.getName (), "informClientsOfPlayerBusyInCombat");
	}
	
	/**
	 * Sends to all human players details of the scheduled combats that they are and aren't involved in 
	 * 
	 * @param players List of players in session 
	 * @param combats List of all scheduled combats in session
	 * @param updateOthersCountOnly true will send to each player only an update to the number of combats they *aren't* involved in, with no details of the combats they *are* involved in
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendScheduledCombats (final List<PlayerServerDetails> players,
		final List<MomScheduledCombat> combats, final boolean updateOthersCountOnly)
		throws JAXBException, XMLStreamException
	{
		log.entering (CombatSchedulerImpl.class.getName (), "sendScheduledCombats");
		
		// For each human player, look through all scheduled combats to see which ones do and don't apply to them
		for (final PlayerServerDetails thisPlayer : players)
			if (thisPlayer.getPlayerDescription ().isHuman ())
			{
				// Count how many combats they *aren't* involved in
				int othersCount = 0;
				for (final MomScheduledCombat thisCombat : combats)
					if ((thisCombat.getAttackingPlayerID () == thisPlayer.getPlayerDescription ().getPlayerID ().intValue ()) ||
						(thisPlayer.getPlayerDescription ().getPlayerID ().equals (thisCombat.getDefendingPlayerID ())))
					{
						if (!updateOthersCountOnly)
						{
							final AddScheduledCombatMessage msg = new AddScheduledCombatMessage ();
							msg.setScheduledCombatData (thisCombat);
							thisPlayer.getConnection ().sendMessageToClient (msg);
						}
					}
					else
						othersCount++;
				
				// Tell player how many combats they aren't involved in
				// Two possible types of message for this, depending on whether we sent them their own combats as well
				if (updateOthersCountOnly)
				{
					final UpdateOtherScheduledCombatsMessage msg = new UpdateOtherScheduledCombatsMessage ();
					msg.setCombatCount (othersCount);
					thisPlayer.getConnection ().sendMessageToClient (msg);
				}
				else
				{
					final ShowListAndOtherScheduledCombatsMessage msg = new ShowListAndOtherScheduledCombatsMessage ();
					msg.setCombatCount (othersCount);
					thisPlayer.getConnection ().sendMessageToClient (msg);
				}
			}
		
		log.exiting (CombatSchedulerImpl.class.getName (), "sendScheduledCombats");
	}

	/**
	 * Handles tidying up after a scheduled combat
	 * 
	 * @param scheduledCombatURN The scheduled combat that ended
	 * @param winningPlayer Player who won; if they scouted a node/lair/tower but clicked No to not attack those Sky Drakes, this will be null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void processEndOfScheduledCombat (final int scheduledCombatURN, final PlayerServerDetails winningPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (CombatSchedulerImpl.class.getName (), "processEndOfScheduledCombat", scheduledCombatURN);
		
		final MomScheduledCombat combat = getScheduledCombatUtils ().findScheduledCombatURN 
			(mom.getGeneralServerKnowledge ().getScheduledCombat (), scheduledCombatURN, "processEndOfScheduledCombat");
		
		final PlayerServerDetails attackingPlayer = MultiplayerSessionServerUtils.findPlayerWithID (mom.getPlayers (), combat.getAttackingPlayerID (), "processEndOfScheduledCombat");
		
		// Any other combats where this player is attacking the same square, change from a combat into an optional movement without a fight
		if ((winningPlayer != null) && (combat.getAttackingPlayerID () == winningPlayer.getPlayerDescription ().getPlayerID ()))
			for (final MomScheduledCombat otherCombat : mom.getGeneralServerKnowledge ().getScheduledCombat ())
				if ((otherCombat != combat) &&
					(otherCombat.getAttackingPlayerID () == combat.getAttackingPlayerID ()) &&
					(otherCombat.getDefendingLocation ().equals (combat.getDefendingLocation ())))
				{
					// Update on server
					otherCombat.setWalkInWithoutAFight (true);
					
					// Update on client
					if (attackingPlayer.getPlayerDescription ().isHuman ())
					{
						final ScheduledCombatWalkInWithoutAFightMessage msg = new ScheduledCombatWalkInWithoutAFightMessage ();
						msg.setScheduledCombatURN (otherCombat.getScheduledCombatURN ());
						
						attackingPlayer.getConnection ().sendMessageToClient (msg);
					}
				}

		// Delete it list on server
		// Note that we don't have to tell the clients to remove the combat from their list - they know to do so from the mmCombatEnded message
		mom.getGeneralServerKnowledge ().getScheduledCombat ().remove (combat);
		
		// Any more combats left to play?
		if (mom.getGeneralServerKnowledge ().getScheduledCombat ().size () == 0)
			getPlayerMessageProcessing ().endPhase (mom, 0);
		else
			// Update counts on other clients so they know how many combats other players are involved in and they're still waiting for
			sendScheduledCombats (mom.getPlayers (), mom.getGeneralServerKnowledge ().getScheduledCombat (), true);
		
		log.exiting (CombatSchedulerImpl.class.getName (), "processEndOfScheduledCombat");
	}

	/**
	 * @return Server-side multiplayer utils
	 */
	public final MultiplayerServerUtils getMultiplayerServerUtils ()
	{
		return multiplayerServerUtils;
	}
	
	/**
	 * @param utils Server-side multiplayer utils
	 */
	public final void setMultiplayerServerUtils (final MultiplayerServerUtils utils)
	{
		multiplayerServerUtils = utils;
	}

	/**
	 * @return Scheduled combat utils
	 */
	public final ScheduledCombatUtils getScheduledCombatUtils ()
	{
		return scheduledCombatUtils;
	}

	/**
	 * @param utils Scheduled combat utils
	 */
	public final void setScheduledCombatUtils (final ScheduledCombatUtils utils)
	{
		scheduledCombatUtils = utils;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}
}

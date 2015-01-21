package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomScheduledCombat;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.RequestStartScheduledCombatMessage;
import momime.common.messages.servertoclient.PlayerCombatRequestStatusMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ScheduledCombatUtils;
import momime.server.MomSessionVariables;
import momime.server.process.CombatProcessing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client sends this to request to play a particular scheduled combat
 */
public final class RequestStartScheduledCombatMessageImpl extends RequestStartScheduledCombatMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (RequestStartScheduledCombatMessageImpl.class);

	/** Scheduled combat utils */
	private ScheduledCombatUtils scheduledCombatUtils; 
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;

	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
			throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID () + ", Combat URN " + getScheduledCombatURN ());

		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		final MomScheduledCombat combat = getScheduledCombatUtils ().findScheduledCombatURN
			(mom.getGeneralServerKnowledge ().getScheduledCombat (), getScheduledCombatURN (), "RequestStartScheduledCombatMessageImpl");
		
		// Who else is involved?
		final PlayerServerDetails ohp = (combat == null) ? null : (PlayerServerDetails) getScheduledCombatUtils ().determineOtherHumanPlayer
			(combat, sender.getPlayerDescription ().getPlayerID (), mom.getPlayers ());
		
		final MomTransientPlayerPublicKnowledge senderTpk = (MomTransientPlayerPublicKnowledge) sender.getTransientPlayerPublicKnowledge ();
		final MomTransientPlayerPublicKnowledge ohpTpk = (ohp == null) ? null : (MomTransientPlayerPublicKnowledge) ohp.getTransientPlayerPublicKnowledge ();
		
		// Now do some checks
		String error = null;
		if (combat == null)
			error = "Couldn't find the scheduled combat that you are requesting";
			
		else if ((sender.getPlayerDescription ().getPlayerID ().intValue () != combat.getAttackingPlayerID ()) &&
					(!sender.getPlayerDescription ().getPlayerID ().equals (combat.getDefendingPlayerID ())))
			
			error = "You tried to request a scheduled combat that you aren't involved in";
		
		else if ((senderTpk.isCurrentlyPlayingCombat ()) || (senderTpk.getScheduledCombatUrnRequested () != null))
			error = "You can only request to play one combat at a time";
		
		// Is the other player busy?
		// Unless they're 'busy' trying to start up the same combat we're requesting - that's fine
		else if ((ohpTpk != null) && ((ohpTpk.isCurrentlyPlayingCombat ()) ||
			((ohpTpk.getScheduledCombatUrnRequested () != null) && (getScheduledCombatURN () != ohpTpk.getScheduledCombatUrnRequested ()))))
			
			error = "The other player is busy playing another a combat, try again in a moment";
		
		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}

		// If another human player, check whether they've also requested the same combat.
		// If not, we can't kick the combat off yet, we have to let them know that we want to play this combat now and wait for them to accept.
		// And we send this to *everyone*, not just the other player, since we need them to know that we're busy and they can't request combats against us.
		else if ((ohpTpk != null) &&
			((ohpTpk.getScheduledCombatUrnRequested () == null) || (getScheduledCombatURN () != ohpTpk.getScheduledCombatUrnRequested ())))
		{
			// Remember on server
			senderTpk.setScheduledCombatUrnRequested (getScheduledCombatURN ());
			
			// Inform all clients
			final PlayerCombatRequestStatusMessage msg = new PlayerCombatRequestStatusMessage ();
			msg.setPlayerID (sender.getPlayerDescription ().getPlayerID ());
			msg.setRequestedScheduledCombatURN (getScheduledCombatURN ());
			
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
		}
		else
		{
			// The attacking player isn't necessarily the sender; if a human-vs-human combat then we're processing the message from
			// whoever was second to click on the combat; that might be the attacker or defender.
			// Also don't rely on comparing and picking either sender or ohp, because if the attacker is an AI player, they're neither of these.
			final PlayerServerDetails attackingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID
				(mom.getPlayers (), combat.getAttackingPlayerID (), "RequestStartScheduledCombatMessageImpl");
			
			// Actually start the combat (Or pop up the 'found node/lair/tower' window if applicable)
			getCombatProcessing ().initiateCombat ((MapCoordinates3DEx) combat.getDefendingLocation (), (MapCoordinates3DEx) combat.getAttackingFrom (),
				getScheduledCombatURN (), attackingPlayer, combat.getAttackingUnitURN (), mom);
		}
	
		log.trace ("Exiting process");
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
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}

	/**
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
	}
}
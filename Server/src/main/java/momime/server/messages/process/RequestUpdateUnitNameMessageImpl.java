package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.RequestUpdateUnitNameMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.server.IMomSessionVariables;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Message client sends to server to tell them what name we want to rename a hero to
 */
public final class RequestUpdateUnitNameMessageImpl extends RequestUpdateUnitNameMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (RequestUpdateUnitNameMessageImpl.class.getName ());

	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 * @throws RecordNotFoundException If the player has picks which we can't find in the cache, or the AI player chooses a spell which we can't then find in their list
	 * @throws PlayerNotFoundException If the player who owns the unit cannot be found
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException, PlayerNotFoundException
	{
		log.entering (RequestUpdateUnitNameMessageImpl.class.getName (), "process",
			new Integer [] {sender.getPlayerDescription ().getPlayerID (), getUnitURN ()});

		final IMomSessionVariables mom = (IMomSessionVariables) thread;
		
		// Find the unit being dismissed
		final MemoryUnit trueUnit = mom.getUnitUtils ().findUnitURN (getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

		// Validation
		String error = null;
		if (trueUnit == null)
			error = "Can't find the unit you tried to rename";
		
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (trueUnit.getOwningPlayerID ()))
			error = "You tried to dimiss a unit that you don't own";
		
		else
			error = null;
		
		if (error != null)
		{
			// Return error
			log.warning (DismissUnitMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Rename it on server
			trueUnit.setUnitName (getUnitName ());
			
			// Rename it in players' memories and on clients
			mom.getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit_UnitName (trueUnit,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
		}

		log.exiting (RequestUpdateUnitNameMessageImpl.class.getName (), "process");
	}
}

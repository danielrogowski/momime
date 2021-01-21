package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.clienttoserver.RequestRemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;

/**
 * Client sends this if player clicks a queued overland spell that they no longer want to cast
 */
public final class RequestRemoveQueuedSpellMessageImpl extends RequestRemoveQueuedSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestRemoveQueuedSpellMessageImpl.class);
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	@Override
	public final void process (@SuppressWarnings ("unused") final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		
		// Validate the request
		final String error;
		if ((getQueuedSpellIndex () >= 0) && (getQueuedSpellIndex () < priv.getQueuedSpell ().size ()))
		{
			final String spellID = priv.getQueuedSpell ().get (getQueuedSpellIndex ()).getQueuedSpellID ();
			if (spellID.equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN))
				error = "You cannot cancel casting the Spell of Return.";
			else
				error = null;
		}
		else
			error = "Cannot find the queued overland spell you are trying to cancel casting.";
		
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Remove on server
			priv.getQueuedSpell ().remove (getQueuedSpellIndex ());
			
			// Remove on client
			final RemoveQueuedSpellMessage msg = new RemoveQueuedSpellMessage ();
			msg.setQueuedSpellIndex (getQueuedSpellIndex ());
			sender.getConnection ().sendMessageToClient (msg);
			
			// If the spell we removed was the first one, and had some Mana spent on it already, then zero that out
			if ((getQueuedSpellIndex () == 0) && (priv.getManaSpentOnCastingCurrentSpell () > 0))
			{
				priv.setManaSpentOnCastingCurrentSpell (0);
				sender.getConnection ().sendMessageToClient (new UpdateManaSpentOnCastingCurrentSpellMessage ());
			}
		}
	}
}
package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.CombatAutoControlMessage;
import momime.server.MomSessionVariables;
import momime.server.knowledge.CombatDetails;
import momime.server.process.CombatProcessing;
import momime.server.utils.CombatMapServerUtils;

/**
 * Message client sends to server when they want the server to use its AI to move their combat units for this combat turn.
 * Client re-sends this each combat turn - the server has no persistent memory of whether a client has
 * Auto switched on or not - this makes it easier to allow the player to switch Auto back off again.
 */
public final class CombatAutoControlMessageImpl extends CombatAutoControlMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CombatAutoControlMessageImpl.class);
	
	/** Combat processing */
	private CombatProcessing combatProcessing;

	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@SuppressWarnings ("unused")
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		final CombatDetails combatDetails = getCombatMapServerUtils ().findCombatURN (mom.getCombatDetails (), getCombatURN ());
		if (combatDetails == null)
			log.warn (sender.getPlayerDescription ().getPlayerName () + " sent CombatAutoControlMessage for combat URN " + getCombatURN () + " which does not exist");
		else
			getCombatProcessing ().progressCombat (combatDetails.getCombatLocation (), false, true, mom);
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

	/**
	 * @return Methods dealing with combat maps that are only needed on the server
	 */
	public final CombatMapServerUtils getCombatMapServerUtils ()
	{
		return combatMapServerUtils;
	}

	/**
	 * @param u Methods dealing with combat maps that are only needed on the server
	 */
	public final void setCombatMapServerUtils (final CombatMapServerUtils u)
	{
		combatMapServerUtils = u;
	}
}
package momime.server.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.CaptureCityDecisionMessage;
import momime.server.MomSessionVariables;
import momime.server.process.CombatProcessing;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client sends this to tell the server whether they want to raze or capture a city they just took.
 */
public final class CaptureCityDecisionMessageImpl extends CaptureCityDecisionMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CaptureCityDecisionMessageImpl.class.getName ());

	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (CaptureCityDecisionMessageImpl.class.getName (), "process",
			new String [] {getCityLocation ().toString (), getCaptureCityDecision ().toString ()});
		
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		final PlayerServerDetails defendingPlayer = MultiplayerSessionServerUtils.findPlayerWithID (mom.getPlayers (), getDefendingPlayerID (), "CaptureCityDecisionMessageImpl");
		getCombatProcessing ().combatEnded ((OverlandMapCoordinatesEx) getCityLocation (), sender, defendingPlayer, sender, getCaptureCityDecision (), mom);

		log.exiting (CaptureCityDecisionMessageImpl.class.getName (), "process");
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

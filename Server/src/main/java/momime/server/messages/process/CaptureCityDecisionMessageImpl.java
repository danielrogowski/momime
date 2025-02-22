package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.clienttoserver.CaptureCityDecisionMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.server.MomSessionVariables;
import momime.server.knowledge.CombatDetails;
import momime.server.process.CombatStartAndEnd;
import momime.server.utils.CombatMapServerUtils;

/**
 * Client sends this to tell the server whether they want to raze or capture a city they just took.
 */
public final class CaptureCityDecisionMessageImpl extends CaptureCityDecisionMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CaptureCityDecisionMessageImpl.class);
	
	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
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
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		final CombatDetails combatDetails = getCombatMapServerUtils ().findCombatByLocation (mom.getCombatDetails (),
			(MapCoordinates3DEx) getCityLocation (), "CaptureCityDecisionMessageImpl");
		
		if ((getCaptureCityDecision () == CaptureCityDecisionID.CAPTURE) || (getCaptureCityDecision () == CaptureCityDecisionID.RAZE))
		{
			final PlayerServerDetails defendingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), getDefendingPlayerID (), "CaptureCityDecisionMessageImpl");
			getCombatStartAndEnd ().combatEnded (combatDetails, sender, defendingPlayer, sender, getCaptureCityDecision (), mom, true);
		}
		else
		{
			log.warn ("Received Capture City Decision message from " + sender.getPlayerDescription ().getPlayerName () + " who sent an invalid decision - " + getCaptureCityDecision ());

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("You can only pick Capture or Raze for the captured city.");
			sender.getConnection ().sendMessageToClient (reply);
		}
	}	

	/**
	 * @return Starting and ending combats
	 */
	public final CombatStartAndEnd getCombatStartAndEnd ()
	{
		return combatStartAndEnd;
	}

	/**
	 * @param cse Starting and ending combats
	 */
	public final void setCombatStartAndEnd (final CombatStartAndEnd cse)
	{
		combatStartAndEnd = cse;
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
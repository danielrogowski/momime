package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.clienttoserver.EndCombatTurnMessage;
import momime.server.MomSessionVariables;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.CombatEndTurn;
import momime.server.process.CombatProcessing;

/**
 * Message client sends to server when all units have been moved in combat
 */
public final class EndCombatTurnMessageImpl extends EndCombatTurnMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (EndCombatTurnMessageImpl.class);

	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Combat end of turn processing */
	private CombatEndTurn combatEndTurn;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(getCombatLocation ().getZ ()).getRow ().get (getCombatLocation ().getY ()).getCell ().get (getCombatLocation ().getX ());
		
		if (!sender.getPlayerDescription ().getPlayerID ().equals (tc.getCombatCurrentPlayerID ()))
			log.warn ("Received EndCombatTurnMessage from wrong player - ignored");
		else
		{
			getCombatEndTurn ().combatEndTurn ((MapCoordinates3DEx) getCombatLocation (), sender.getPlayerDescription ().getPlayerID (), mom);
			
			getCombatProcessing ().progressCombat ((MapCoordinates3DEx) getCombatLocation (), false, false, mom);
		}
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
	 * @return Combat end of turn processing
	 */
	public final CombatEndTurn getCombatEndTurn ()
	{
		return combatEndTurn;
	}

	/**
	 * @param c Combat end of turn processing
	 */
	public final void setCombatEndTurn (final CombatEndTurn c)
	{
		combatEndTurn = c;
	}
}
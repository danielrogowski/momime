package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_5.AttackNodeLairTowerMessage;
import momime.server.MomSessionVariables;
import momime.server.process.CombatStartAndEnd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client has clicked Yes, they want to attack those Sky Drakes :)
 * These will be the actual cells containing the units, so if either side is in a tower, then their plane will say 0.
 */
public final class AttackNodeLairTowerMessageImpl extends AttackNodeLairTowerMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AttackNodeLairTowerMessageImpl.class);

	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
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
		throws JAXBException, XMLStreamException, PlayerNotFoundException, MomException, RecordNotFoundException
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID () + ", " + getAttackingFrom () + ", " + getDefendingLocation () +
			", Combat URN " + scheduledCombatURN);

		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		getCombatStartAndEnd ().startCombat ((MapCoordinates3DEx) getDefendingLocation (), (MapCoordinates3DEx) getAttackingFrom (),
			getScheduledCombatURN (), sender, getUnitURN (), mom);

		log.trace ("Exiting process");
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
}
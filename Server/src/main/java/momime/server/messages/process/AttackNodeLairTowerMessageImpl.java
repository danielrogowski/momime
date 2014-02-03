package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.AttackNodeLairTowerMessage;
import momime.server.MomSessionVariables;
import momime.server.process.CombatProcessing;

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
	private final Logger log = Logger.getLogger (AttackNodeLairTowerMessageImpl.class.getName ());

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
		throws JAXBException, XMLStreamException, PlayerNotFoundException, MomException, RecordNotFoundException
	{
		log.entering (AttackNodeLairTowerMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getAttackingFrom ().toString (), getDefendingLocation ().toString (),
			(scheduledCombatURN == null) ? "NotSched" : scheduledCombatURN.toString ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		getCombatProcessing ().startCombat ((OverlandMapCoordinatesEx) getDefendingLocation (), (OverlandMapCoordinatesEx) getAttackingFrom (),
			getScheduledCombatURN (), sender, getUnitURN (), mom);

		log.exiting (AttackNodeLairTowerMessageImpl.class.getName (), "process");
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

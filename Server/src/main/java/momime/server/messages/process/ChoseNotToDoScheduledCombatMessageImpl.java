package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_5.ChoseNotToDoScheduledCombatMessage;
import momime.server.MomSessionVariables;
import momime.server.process.CombatScheduler;

/**
 * Client sends this if they've initiated a scheduled combat in the movement phase, but now decided they don't want to play it after all.
 * Since you can't choose not to do a combat that you initiated, the only circumstance this happens in is when you've scouted a node/lair/tower and
 * have second thoughts about attacking those sky drakes :)
 */
public final class ChoseNotToDoScheduledCombatMessageImpl extends ChoseNotToDoScheduledCombatMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChoseNotToDoScheduledCombatMessageImpl.class.getName ());

	/** Simultaneous turns combat scheduler */
	private CombatScheduler combatScheduler;

	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (ChoseNotToDoScheduledCombatMessageImpl.class.getName (), "process", getScheduledCombatURN ());

		final MomSessionVariables mom = (MomSessionVariables) thread;

		// This means the player isn't involved in a combat after all, so let everyone know that
		getCombatScheduler ().informClientsOfPlayerBusyInCombat (sender, mom.getPlayers (), false);
		
		// Tidy up combat
		getCombatScheduler ().processEndOfScheduledCombat (getScheduledCombatURN (), null, mom);

		log.exiting (ChoseNotToDoScheduledCombatMessageImpl.class.getName (), "process");
	}

	/**
	 * @return Simultaneous turns combat scheduler
	 */
	public final CombatScheduler getCombatScheduler ()
	{
		return combatScheduler;
	}

	/**
	 * @param scheduler Simultaneous turns combat scheduler
	 */
	public final void setCombatScheduler (final CombatScheduler scheduler)
	{
		combatScheduler = scheduler;
	}
}

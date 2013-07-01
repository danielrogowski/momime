package momime.server.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.RequestCastSpellMessage;
import momime.server.IMomSessionVariables;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Client sends this to request a spell being cast, in combat or overland.
 * 
 * If overland, all the combat elements are ommitted.  In this case we don't send which unit/location we're targetting it at just yet,
 * since it might take multiple turns to cast.
 * 
 * If in combat, and casting at a unit, then combatTargetUnitURN willspecify which unit
 * (overland spells, even unit enchantments, work differently via NTMs).
 * 
 * If in combat, and casting at a location (e.g. summoning), combatTargetLocation will specify the target location.
 */
public class RequestCastSpellMessageImpl extends RequestCastSpellMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (RequestCastSpellMessageImpl.class.getName ());
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws IOException For other types of failures
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (RequestCastSpellMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getSpellID ()});

		final IMomSessionVariables mom = (IMomSessionVariables) thread;

		mom.getSpellProcessing ().requestCastSpell (sender, getSpellID (),
			(OverlandMapCoordinatesEx) getCombatLocation (), (CombatMapCoordinatesEx) getCombatTargetLocation (), getCombatTargetUnitURN (), mom);

		log.exiting (RequestCastSpellMessageImpl.class.getName (), "process");
	}
}

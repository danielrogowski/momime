package momime.server.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.server.MomSessionVariables;
import momime.server.process.SpellQueueing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

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
public class RequestCastSpellMessageImpl extends RequestCastSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (RequestCastSpellMessageImpl.class);

	/** Spell queueing methods */
	private SpellQueueing spellQueueing;
	
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
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID () + ", "+ getSpellID ());

		final MomSessionVariables mom = (MomSessionVariables) thread;

		getSpellQueueing ().requestCastSpell (sender, getCombatCastingUnitURN (), getCombatCastingSlotNumber (), getSpellID (), getHeroItem (),
			(MapCoordinates3DEx) getCombatLocation (), (MapCoordinates2DEx) getCombatTargetLocation (), getCombatTargetUnitURN (), getVariableDamage (), mom);

		log.trace ("Exiting process");
	}

	/**
	 * @return Spell queueing methods
	 */
	public final SpellQueueing getSpellQueueing ()
	{
		return spellQueueing;
	}

	/**
	 * @param obj Spell queueing methods
	 */
	public final void setSpellQueueing (final SpellQueueing obj)
	{
		spellQueueing = obj;
	}
}
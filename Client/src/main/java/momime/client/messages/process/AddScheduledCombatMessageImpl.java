package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.common.messages.MomScheduledCombat;
import momime.common.messages.servertoclient.AddScheduledCombatMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to client to notify them of a combat they need to play out at the end of a simultaneous turns game
 */
public final class AddScheduledCombatMessageImpl extends AddScheduledCombatMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddScheduledCombatMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Scheduled combat URN " + getScheduledCombatData ().getScheduledCombatURN ());
		
		// Have to copy it, because this is an extension of the data structure sent from the server
		final MomScheduledCombat combat = new MomScheduledCombat ();
	    combat.setScheduledCombatURN			(getScheduledCombatData ().getScheduledCombatURN ());
	    combat.setDefendingLocation				(getScheduledCombatData ().getDefendingLocation ());
	    combat.setAttackingFrom					(getScheduledCombatData ().getAttackingFrom ());
	    combat.setDefendingPlayerID				(getScheduledCombatData ().getDefendingPlayerID ());
	    combat.setAttackingPlayerID				(getScheduledCombatData ().getAttackingPlayerID ());
	    combat.getAttackingUnitURN ().addAll	(getScheduledCombatData ().getAttackingUnitURN ());
		
		getClient ().getOurTransientPlayerPrivateKnowledge ().getScheduledCombat ().add (combat);		
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
}
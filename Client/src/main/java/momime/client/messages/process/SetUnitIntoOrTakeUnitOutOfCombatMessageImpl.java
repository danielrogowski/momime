package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.common.messages.servertoclient.v0_9_5.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to client when a combat is over to take those units out of combat.
 * For taking units out of combat, all the values will be omitted except for the unitURN.
 */
public final class SetUnitIntoOrTakeUnitOutOfCombatMessageImpl extends SetUnitIntoOrTakeUnitOutOfCombatMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SetUnitIntoOrTakeUnitOutOfCombatMessageImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
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
		log.trace ("Entering start: " + getUnitURN () + ", " + getCombatLocation () + ", " + getCombatLocation () + ", " + getCombatSide () + ", " + getSummonedBySpellID ()); 

		// Later this will need adding to so that units summoned into combat show the anim of them coming up out of the summoning circle
		final MemoryUnit unit = getUnitUtils ().findUnitURN (getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
		unit.setCombatPosition (getCombatPosition ());
		unit.setCombatLocation (getCombatLocation ());
		unit.setCombatHeading (getCombatHeading ());
		unit.setCombatSide (getCombatSide ());

		log.trace ("Exiting start");
	}
	
	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
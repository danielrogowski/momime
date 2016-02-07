package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.utils.UnitClientUtils;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to everyone to notify of dead units, except where it is already obvious from an Apply Damage message that a unit is dead
 */
public final class KillUnitMessageImpl extends KillUnitMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (KillUnitMessageImpl.class);

	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Unit URN " + getUnitURN () + ", " + getNewStatus ());

		final MemoryUnit unit = getUnitUtils ().findUnitURN (unitURN,
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "KillUnitMessageImpl");
		
		getUnitClientUtils ().killUnit (unit, getNewStatus ());
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
}
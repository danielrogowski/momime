package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.UpdateUnitNameMessage;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Message server sends to clients to tell them that a hero was renamed
 */
public final class UpdateUnitNameMessageImpl extends UpdateUnitNameMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UpdateUnitNameMessageImpl.class);

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
		log.trace ("Entering start: Unit URN " + getUnitURN () + ", \"" + getUnitName () + "\"");

		final MemoryUnit unit = getUnitUtils ().findUnitURN (getUnitURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "UpdateUnitNameMessageImpl");
		
		unit.setUnitName (getUnitName ());
		
		// Do we have a unit info screen open for this unit?
		final UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (getUnitURN ());
		if (unitInfo != null)
		{
			unitInfo.languageChanged ();
			unitInfo.getUnitInfoPanel ().showUnit (unit);
		}
		
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
package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.SetSpecialOrderMessage;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to the player owning the units to set a special order on them (not used for Patrolling)
 */
public final class SetSpecialOrderMessageImpl extends SetSpecialOrderMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SetSpecialOrderMessageImpl.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		for (final Integer thisUnitURN : getUnitURN ())
		{
			final MemoryUnit mu = getUnitUtils ().findUnitURN (thisUnitURN,
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "SetSpecialOrderMessageImpl");
			
			mu.setSpecialOrder (getSpecialOrder ());
			
			// Remove it from the wait list
			getOverlandMapProcessing ().removeUnitFromLeftToMoveOverland (mu);
			
			// Close down any unit info screen that may be open for it
			final UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (thisUnitURN);
			if (unitInfo != null)
				unitInfo.close ();
		}
		
		getOverlandMapProcessing ().selectNextUnitToMoveOverland ();
		
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

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
}
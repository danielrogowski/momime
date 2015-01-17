package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CityViewUI;
import momime.common.messages.servertoclient.PendingSaleMessage;
import momime.common.utils.MemoryBuildingUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this in a simultaneous turns game to inform the city owner *only* that a building will be sold at the end of the turn.
 * It can also be sent with buildingID omitted, to cancel selling anything at the end of the turn.
 */
public final class PendingSaleMessageImpl extends PendingSaleMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (PendingSaleMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getCityLocation () + ", Building URN " + getBuildingURN ());
		
		final String buildingID = (getBuildingURN () == null) ? null : getMemoryBuildingUtils ().findBuildingURN
			(getBuildingURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), "PendingSaleMessageImpl").getBuildingID ();
		
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get (getCityLocation ().getZ ()).getRow ().get
			(getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).setBuildingIdSoldThisTurn (buildingID);
		
		// Likely there's a city screen displaying this, for us to have requested/cancelled the pending sale
		final CityViewUI cityView = getClient ().getCityViews ().get (getCityLocation ().toString ());
		if (cityView != null)
			cityView.getCityViewPanel ().repaint ();

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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}
}
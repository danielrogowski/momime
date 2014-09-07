package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.CityViewUI;
import momime.common.messages.servertoclient.v0_9_5.UpdateProductionSoFarMessage;
import momime.common.messages.v0_9_5.MemoryGridCell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to the owner of a city to tell them how many production points they've put into the current construction project so far.
 * Only the owner of the city gets this - so you cannot tell how much production has been generated from cities that you don't own.
 */
public final class UpdateProductionSoFarMessageImpl extends UpdateProductionSoFarMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (UpdateProductionSoFarMessageImpl.class);

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
		log.trace ("Entering start");
		
		final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		gc.setProductionSoFar (getProductionSoFar ());
		
		// If that city screen is open, need to update the production coins
		final CityViewUI cityView = getClient ().getCityViews ().get (getCityLocation ().toString ());
		if (cityView != null)
		{
			cityView.productionSoFarChanged ();
			cityView.recheckRushBuyEnabled ();
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
}
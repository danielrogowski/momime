package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.ui.dialogs.RazeCityUI;
import momime.common.messages.servertoclient.AskForCaptureCityDecisionMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to players who capture a city, so they show the "Capture or Raze" form (the decision will be blank at this point)
 */
public final class AskForCaptureCityDecisionMessageImpl extends AskForCaptureCityDecisionMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChooseYourRaceNowMessageImpl.class);

	/** Raze city UI */
	private RazeCityUI razeCityUI;

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");

		getRazeCityUI ().setCityLocation ((MapCoordinates3DEx) getCityLocation ());
		getRazeCityUI ().setDefendingPlayerID (getDefendingPlayerID ());
		getRazeCityUI ().setVisible (true);
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Raze city UI
	 */
	public final RazeCityUI getRazeCityUI ()
	{
		return razeCityUI;
	}		

	/**
	 * @param ui Raze city UI
	 */
	public final void setRazeCityUI (final RazeCityUI ui)
	{
		razeCityUI = ui;
	}	
}
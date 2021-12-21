package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.ui.dialogs.RazeCityUI;
import momime.common.messages.servertoclient.AskForCaptureCityDecisionMessage;

/**
 * Server sends this to players who capture a city, so they show the "Capture or Raze" form (the decision will be blank at this point)
 */
public final class AskForCaptureCityDecisionMessageImpl extends AskForCaptureCityDecisionMessage implements BaseServerToClientMessage
{
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
		getRazeCityUI ().setCityLocation ((MapCoordinates3DEx) getCityLocation ());
		getRazeCityUI ().setDefendingPlayerID (getDefendingPlayerID ());
		getRazeCityUI ().setVisible (true);
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
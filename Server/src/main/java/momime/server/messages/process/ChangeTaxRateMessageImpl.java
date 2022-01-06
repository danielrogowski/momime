package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.clienttoserver.ChangeTaxRateMessage;
import momime.server.MomSessionVariables;
import momime.server.process.CityProcessing;

/**
 * Clients send this to the server to request a change in their global tax rate
 */
public final class ChangeTaxRateMessageImpl extends ChangeTaxRateMessage implements PostSessionClientToServerMessage
{
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		getCityProcessing ().changeTaxRate (sender, getTaxRateID (), mom);
	}

	/**
	 * @return City processing methods
	 */
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
	}
}
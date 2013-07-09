package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.ChangeCityConstructionMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Clients send this to set what they want to build in a city
 */
public final class ChangeCityConstructionMessageImpl extends ChangeCityConstructionMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChangeCityConstructionMessageImpl.class.getName ());
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.entering (ChangeCityConstructionMessageImpl.class.getName (), "process",
			new String [] {(getCityLocation () == null) ? "null" : getCityLocation ().toString (), getBuildingOrUnitID ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;

		final String error = mom.getCityServerUtils ().validateCityConstruction (sender, mom.getGeneralServerKnowledge ().getTrueMap (),
			(OverlandMapCoordinatesEx) getCityLocation (), getBuildingOrUnitID (), mom.getSessionDescription (), mom.getServerDB ());

		if (error != null)
		{
			// Return error
			log.warning (ChangeCityConstructionMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Update construction on true map
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(getCityLocation ().getPlane ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
			cityData.setCurrentlyConstructingBuildingOrUnitID (getBuildingOrUnitID ());

			// Send update to clients
			mom.getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), (OverlandMapCoordinatesEx) getCityLocation (), mom.getSessionDescription ().getFogOfWarSetting (), false);
		}

		log.exiting (ChangeCityConstructionMessageImpl.class.getName (), "process");
	}
}

package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.clienttoserver.v0_9_4.ChangeOptionalFarmersMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.server.MomSessionThread;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.process.FogOfWarProcessing;
import momime.server.utils.CityServerUtils;

import com.ndg.multiplayer.server.IProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Clients send this to set what they want to build in a city
 */
public final class ChangeOptionalFarmersMessageImpl extends ChangeOptionalFarmersMessage implements IProcessableClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (ChangeOptionalFarmersMessageImpl.class.getName (), "process",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (getCityLocation ()), new Integer (getOptionalFarmers ()).toString ()});

		final MomSessionThread mom = (MomSessionThread) thread;

		final String error = CityServerUtils.validateOptionalFarmers (sender, mom.getGeneralServerKnowledge ().getTrueMap (),
			getCityLocation (), getOptionalFarmers (), mom.getSessionDescription (), mom.getServerDBLookup (), debugLogger);

		if (error != null)
		{
			// Return error
			debugLogger.warning (ChangeOptionalFarmersMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Update construction on true map
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(getCityLocation ().getPlane ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
			cityData.setOptionalFarmers (getOptionalFarmers ());

			// Send update to clients
			FogOfWarProcessing.updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), getCityLocation (), mom.getSessionDescription (), debugLogger);

			// Tell the player how this will affect their global production
			MomServerResourceCalculations.recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDBLookup (), debugLogger);
		}

		debugLogger.exiting (ChangeOptionalFarmersMessageImpl.class.getName (), "process");
	}
}

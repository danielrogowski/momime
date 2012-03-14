package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.clienttoserver.v0_9_4.ChooseCityNameMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.server.MomSessionThread;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import com.ndg.multiplayer.server.IProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Message client sends to server to tell them what name we chose for a city
 */
public final class ChooseCityNameMessageImpl extends ChooseCityNameMessage implements IProcessableClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender, final Logger debugLogger)
		throws JAXBException, XMLStreamException
	{
		debugLogger.entering (ChooseCityNameMessageImpl.class.getName (), "process", sender.getPlayerDescription ().getPlayerID ());

		final MomSessionThread mom = (MomSessionThread) thread;

		// Check and update true map cell
		final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (getCityLocation ().getPlane ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		if (sender.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ()))
		{
			tc.getCityData ().setCityName (getCityName ());

			// Then send the change to all players who can see the city
			FogOfWarMidTurnChanges.updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getPlayers (), getCityLocation (), mom.getSessionDescription (), debugLogger);
		}
		else
		{
			debugLogger.warning ("Received City Name message from " + sender.getPlayerDescription ().getPlayerName () + " who isn't the city owner");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("You tried to name a city which isn''t yours - change ignored.");
			sender.getConnection ().sendMessageToClient (reply);
		}

		debugLogger.exiting (ChooseCityNameMessageImpl.class.getName (), "process");
	}
}

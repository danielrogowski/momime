package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.clienttoserver.v0_9_5.ChooseCityNameMessage;
import momime.common.messages.servertoclient.v0_9_5.TextPopupMessage;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Message client sends to server to tell them what name we chose for a city
 */
public final class ChooseCityNameMessageImpl extends ChooseCityNameMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChooseCityNameMessageImpl.class);

	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException
	{
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID () + ", " + getCityLocation ());

		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Check and update true map cell
		final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		if (sender.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ()))
		{
			tc.getCityData ().setCityName (getCityName ());

			// Then send the change to all players who can see the city
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), (MapCoordinates3DEx) getCityLocation (), mom.getSessionDescription ().getFogOfWarSetting (), false);
		}
		else
		{
			log.warn ("Received City Name message from " + sender.getPlayerDescription ().getPlayerName () + " who isn't the city owner");

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText ("You tried to name a city which isn''t yours - change ignored.");
			sender.getConnection ().sendMessageToClient (reply);
		}

		log.trace ("Exiting process");
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}
}
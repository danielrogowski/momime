package momime.server.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.clienttoserver.ChangeOptionalFarmersMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.CityServerUtils;

/**
 * Clients send this to set what they want to build in a city
 */
public final class ChangeOptionalFarmersMessageImpl extends ChangeOptionalFarmersMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ChangeOptionalFarmersMessageImpl.class);

	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
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

		final String error = getCityServerUtils ().validateOptionalFarmers (sender, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
			(MapCoordinates3DEx) getCityLocation (), getOptionalFarmers ());

		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Update construction on true map
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
			cityData.setOptionalFarmers (getOptionalFarmers ());

			// Send update to clients
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), (MapCoordinates3DEx) getCityLocation (), mom.getSessionDescription ().getFogOfWarSetting ());

			// Tell the player how this will affect their global production
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
	}

	/**
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
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
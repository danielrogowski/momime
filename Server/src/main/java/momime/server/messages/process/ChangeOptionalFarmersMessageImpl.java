package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.ChangeOptionalFarmersMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.CityServerUtils;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Clients send this to set what they want to build in a city
 */
public final class ChangeOptionalFarmersMessageImpl extends ChangeOptionalFarmersMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChangeOptionalFarmersMessageImpl.class.getName ());

	/** Server-only city utils */
	private CityServerUtils cityServerUtils;
	
	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (ChangeOptionalFarmersMessageImpl.class.getName (), "process",
			new String [] {(getCityLocation () == null) ? "null" : getCityLocation ().toString (), new Integer (getOptionalFarmers ()).toString ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;

		final String error = getCityServerUtils ().validateOptionalFarmers (sender, mom.getGeneralServerKnowledge ().getTrueMap (),
			(OverlandMapCoordinatesEx) getCityLocation (), getOptionalFarmers (), mom.getSessionDescription (), mom.getServerDB ());

		if (error != null)
		{
			// Return error
			log.warning (ChangeOptionalFarmersMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

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
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), (OverlandMapCoordinatesEx) getCityLocation (), mom.getSessionDescription ().getFogOfWarSetting (), false);

			// Tell the player how this will affect their global production
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}

		log.exiting (ChangeOptionalFarmersMessageImpl.class.getName (), "process");
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
	public final MomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final MomServerResourceCalculations calc)
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

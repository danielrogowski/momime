package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.SellBuildingMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.TurnSystem;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.v0_9_4.Building;
import momime.server.process.CityProcessing;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Clients send this when they want to sell a building from a city, or cancel a pending sale
 */
public final class SellBuildingMessageImpl extends SellBuildingMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (SellBuildingMessageImpl.class.getName ());

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;

	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (SellBuildingMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (),
			(getCityLocation () == null) ? "null" : getCityLocation ().toString (), getBuildingID ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;

		final MemoryGridCell tc;
		if (getCityLocation () == null)
			tc = null;
		else
			tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		
		final Building building;
		final int goldFromSellingBuilding;
		if (getBuildingID () == null)
		{
			building = null;
			goldFromSellingBuilding = 0;
		}
		else
		{
			building = mom.getServerDB ().findBuilding (getBuildingID (), "SellBuildingMessageImpl");
			goldFromSellingBuilding = getMemoryBuildingUtils ().goldFromSellingBuilding (building);
		}
		
		// Check the owner is who we're expecting & other validation
		final String msg;
		if (tc == null)
			msg = "You didn't provide the location of the city where you want to sell a building.";
		
		else if (tc.getCityData () == null)
			msg = "You tried to sell a building in a location that isn't a city.";
		
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ()))
		{
			if (getBuildingID () != null)
				msg = "You tried to sell a building in a city that you don't own.";
			else
				msg = "You tried to cancel selling a building in a city that you don't own.";
		}
		
		else if ((getBuildingID () != null) && (!getMemoryBuildingUtils ().findBuilding
			(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), (OverlandMapCoordinatesEx) getCityLocation (), getBuildingID ())))
			msg = "This city doesn't have one of those buildings to sell.";
		
		else if ((getBuildingID () != null) && (goldFromSellingBuilding <= 0))
			msg = "You tried to sell a building that has no value.";
		
		else if ((getBuildingID () != null) && (tc.getBuildingIdSoldThisTurn () != null) &&
			(mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME))
			msg = "You can only sell back one building each turn.";
		
		else if ((getBuildingID () != null) && (getMemoryBuildingUtils ().doAnyBuildingsDependOn
			(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), (OverlandMapCoordinatesEx) getCityLocation (), getBuildingID (), mom.getServerDB ()) != null))
			msg = "You cannot sell back this building because it is required by other buildings that you must sell first.";
		
		else
			msg = null;
		
		if (msg != null)
		{
			// Return error
			log.warning (SellBuildingMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + msg);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (msg);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// All ok - use second routine now all validation is done
			getCityProcessing ().sellBuilding (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), (OverlandMapCoordinatesEx) getCityLocation (),
				getBuildingID (), (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS), true, mom.getSessionDescription (), mom.getServerDB ());
			
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.exiting (SellBuildingMessageImpl.class.getName (), "process");
	}

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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

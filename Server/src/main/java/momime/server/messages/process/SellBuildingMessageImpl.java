package momime.server.messages.process;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.TurnSystem;
import momime.common.messages.clienttoserver.SellBuildingMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.process.CityProcessing;

/**
 * Clients send this when they want to sell a building from a city, or cancel a pending sale
 */
public final class SellBuildingMessageImpl extends SellBuildingMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SellBuildingMessageImpl.class);

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

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
		final MomSessionVariables mom = (MomSessionVariables) thread;

		final MemoryGridCell tc;
		if (getCityLocation () == null)
			tc = null;
		else
			tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		
		Building building = null;
		int goldFromSellingBuilding = 0;
		if (getBuildingURN () != null)
		{
			final MemoryBuilding trueBuilding = getMemoryBuildingUtils ().findBuildingURN (getBuildingURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
			if (trueBuilding != null)
			{
				building = mom.getServerDB ().findBuilding (trueBuilding.getBuildingID (), "SellBuildingMessageImpl");
				goldFromSellingBuilding = getMemoryBuildingUtils ().goldFromSellingBuilding (building);
			}
		}
		
		// Check the owner is who we're expecting & other validation
		final String msg;
		if (tc == null)
			msg = "You didn't provide the location of the city where you want to sell a building.";
		
		else if (tc.getCityData () == null)
			msg = "You tried to sell a building in a location that isn't a city.";
		
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ()))
		{
			if (getBuildingURN () != null)
				msg = "You tried to sell a building in a city that you don't own.";
			else
				msg = "You tried to cancel selling a building in a city that you don't own.";
		}
		
		else if ((getBuildingURN () != null) && (building == null))
			msg = "Cannot find the building that you're trying to sell.";
		
		else if ((getBuildingURN () != null) && (goldFromSellingBuilding <= 0))
			msg = "You tried to sell a building that has no value.";
		
		else if ((getBuildingURN () != null) && (tc.getBuildingIdSoldThisTurn () != null) &&
			(mom.getSessionDescription ().getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME))
			msg = "You can only sell back one building each turn.";
		
		else if ((building != null) && (getMemoryBuildingUtils ().doAnyBuildingsDependOn
			(mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (), (MapCoordinates3DEx) getCityLocation (), building.getBuildingID (), mom.getServerDB ()) != null))
			msg = "You cannot sell back this building because it is required by other buildings that you must sell first.";
		
		else
			msg = null;
		
		if (msg != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + msg);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (msg);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// All ok - use second routine now all validation is done
			getCityProcessing ().sellBuilding ((MapCoordinates3DEx) getCityLocation (), getBuildingURN (),
				(mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS), true, mom);
			
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
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
package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.RushBuyMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateProductionSoFarMessage;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.Unit;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Message client sends to server when they want to rush buy the current construction project in a particular city
 */
public final class RushBuyMessageImpl extends RushBuyMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (RushBuyMessageImpl.class.getName ());

	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		log.entering (RushBuyMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (),
			(getCityLocation () == null) ? "null" : getCityLocation ().toString ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		Integer productionCost = null;
		final MemoryGridCell tc;
		if (getCityLocation () == null)
			tc = null;
		else
		{
			tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		
			// Check if we're constructing a building or a unit
			final String buildingOrUnitID = (tc.getCityData () == null) ? null : tc.getCityData ().getCurrentlyConstructingBuildingOrUnitID ();  
			if (buildingOrUnitID != null)
			{
				try
				{
					final Building building = mom.getServerDB ().findBuilding (buildingOrUnitID, "RushBuyMessageImpl");
					productionCost = building.getProductionCost ();
				}
				catch (final RecordNotFoundException e)
				{
					// Ignore, maybe its a unit
				}

				try
				{
					final Unit unit = mom.getServerDB ().findUnit (buildingOrUnitID, "RushBuyMessageImpl");
					productionCost = unit.getProductionCost ();
				}
				catch (final RecordNotFoundException e)
				{
					// Ignore, maybe its a building
				}
			}
		}
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		
		int rushBuyCost = 0;
		final String msg;
		if (tc == null)
			msg = "You didn't provide the location of the city where you want to rush buy.";
		
		else if (tc.getCityData () == null)
			msg = "You tried to rush buy in a location that isn't a city.";
		
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ()))
			msg = "You tried to rush buy the construction of a city which isn't yours.";
		
		else if (productionCost == null)
			msg = "Couldn't find the building or unit that you're trying to rush buy, or it has no production cost defined.";
		
		else
		{
			// Check if we have enough gold
			rushBuyCost = getCityCalculations ().goldToRushBuy (productionCost, tc.getProductionSoFar ());
			
			if (getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD) < rushBuyCost)
				msg = "You cannot afford to rush buy the construction project in this city.";
			else
				msg = null;			
		}

		if (msg != null)
		{
			// Return error
			log.warning (RushBuyMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + msg);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (msg);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// All ok - deduct money & send to client
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, -rushBuyCost);
			getServerResourceCalculations ().sendGlobalProductionValues (sender, 0);
			
			// Finish construction & send to client
			// We don't actually construct the building/unit here - that only happens when we end turn, same as in the original MoM
			tc.setProductionSoFar (productionCost);
			
			final UpdateProductionSoFarMessage reply = new UpdateProductionSoFarMessage ();
			reply.setCityLocation (getCityLocation ());
			reply.setProductionSoFar (productionCost);
			sender.getConnection ().sendMessageToClient (reply);
		}
		
		log.exiting (RushBuyMessageImpl.class.getName (), "process");
	}

	/**
	 * @return City calculations
	 */
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
	{
		cityCalculations = calc;
	}
	
	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
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
}

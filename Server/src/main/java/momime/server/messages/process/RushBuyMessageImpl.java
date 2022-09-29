package momime.server.messages.process;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.clienttoserver.RushBuyMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

/**
 * Message client sends to server when they want to rush buy the current construction project in a particular city
 */
public final class RushBuyMessageImpl extends RushBuyMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RushBuyMessageImpl.class);

	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		
		Integer productionCost = null;
		final MemoryGridCell tc;
		if (getCityLocation () == null)
			tc = null;
		else
		{
			tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		
			// Check if we're constructing a building or a unit
			productionCost = getCityProductionCalculations ().calculateProductionCost
				(mom.getPlayers (), priv.getFogOfWarMemory (), (MapCoordinates3DEx) getCityLocation (), priv.getTaxRateID (),
					mom.getSessionDescription (), mom.getGeneralPublicKnowledge ().getConjunctionEventID (), mom.getServerDB (), null);
		}
		
		int rushBuyCost = 0;
		final String msg;
		if (tc == null)
			msg = "You didn't provide the location of the city where you want to rush buy.";
		
		else if (tc.getCityData () == null)
			msg = "You tried to rush buy in a location that isn't a city.";
		
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (tc.getCityData ().getCityOwnerID ()))
			msg = "You tried to rush buy the construction of a city which isn't yours.";
		
		else if (tc.getCityData ().getCityPopulation () < 1000)
			msg = "You must wait for an Outpost to reach 1,000 population and grow into a Hamlet before you can rush buy its construction"; 
		
		else if (productionCost == null)
			msg = "Couldn't find the building or unit that you're trying to rush buy, or it has no production cost defined.";
		
		else
		{
			// Check if we have enough gold
			final int productionSoFar = (tc.getCityData ().getProductionSoFar () == null) ? 0 : tc.getCityData ().getProductionSoFar ();
			rushBuyCost = getCityCalculations ().goldToRushBuy (productionCost, productionSoFar);
			
			if (getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD) < rushBuyCost)
				msg = "You cannot afford to rush buy the construction project in this city.";
			else
				msg = null;			
		}

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
			// All ok - deduct money & send to client
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -rushBuyCost);
			getServerResourceCalculations ().sendGlobalProductionValues (sender, null, false);
			
			// Finish construction & send to client
			// We don't actually construct the building/unit here - that only happens when we end turn, same as in the original MoM
			tc.getCityData ().setProductionSoFar (productionCost);

			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), (MapCoordinates3DEx) getCityLocation (), mom.getSessionDescription ().getFogOfWarSetting ());
		}
	}

	/**
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
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

	/**
	 * @return City production calculations
	 */
	public final CityProductionCalculations getCityProductionCalculations ()
	{
		return cityProductionCalculations;
	}

	/**
	 * @param c City production calculations
	 */
	public final void setCityProductionCalculations (final CityProductionCalculations c)
	{
		cityProductionCalculations = c;
	}
}
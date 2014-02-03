package momime.server.process.resourceconsumer;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.server.MomSessionVariables;
import momime.server.process.CityProcessing;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Building that consumes a particular type of resource
 */
public final class MomResourceConsumerBuilding implements MomResourceConsumer
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomResourceConsumerBuilding.class.getName ());
	
	/** True map building that is consuming resources */
	private MemoryBuilding building;

	/** The player who's resources are being consumed */
	private PlayerServerDetails player;

	/** The type of resources being consumed */
	private String productionTypeID;

	/** The amount of production being consumed */
	private int consumptionAmount;

	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/**
	 * @return The player who's resources are being consumed
	 */
	@Override
	public final PlayerServerDetails getPlayer ()
	{
		return player;
	}

	/**
	 * @param aPlayer The player who's resources are being consumed
	 */
	public final void setPlayer (final PlayerServerDetails aPlayer)
	{
		player = aPlayer;
	}
	
	/**
	 * @return The type of resources being consumed
	 */
	@Override
	public final String getProductionTypeID ()
	{
		return productionTypeID;
	}

	/**
	 * @param aProductionTypeID The type of resources being consumed
	 */
	public final void setProductionTypeID (final String aProductionTypeID)
	{
		productionTypeID = aProductionTypeID;
	}
	
	/**
	 * @return The amount of production being consumed
	 */
	@Override
	public final int getConsumptionAmount ()
	{
		return consumptionAmount;
	}

	/**
	 * @param amount The amount of production being consumed
	 */
	public final void setConsumptionAmount (final int amount)
	{
		consumptionAmount = amount;
	}
	
	/**
	 * @return True map building that is consuming resources
	 */
	public final MemoryBuilding getBuilding ()
	{
		return building;
	}

	/**
	 * @param aBuilding True map building that is consuming resources
	 */
	public final void setBuilding (final MemoryBuilding aBuilding)
	{
		building = aBuilding;
	}
	
	/**
	 * Sells this building to get some gold back and conserve resources
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void kill (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (MomResourceConsumerBuilding.class.getName (), "kill",
			new String [] {getBuilding ().getCityLocation ().toString (), getBuilding ().getBuildingID ()});

		getCityProcessing ().sellBuilding (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (),
			(OverlandMapCoordinatesEx) getBuilding ().getCityLocation (), getBuilding ().getBuildingID (), false, false, mom.getSessionDescription (), mom.getServerDB ());

		if (getPlayer ().getPlayerDescription ().isHuman ())
		{
			final NewTurnMessageData buildingSold = new NewTurnMessageData ();
			buildingSold.setMsgType (NewTurnMessageTypeID.BUILDING_LACK_OF_PRODUCTION);
			buildingSold.setLocation (getBuilding ().getCityLocation ());
			buildingSold.setBuildingOrUnitID (getBuilding ().getBuildingID ());
			buildingSold.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (buildingSold);
		}

		log.exiting (MomResourceConsumerBuilding.class.getName (), "kill");
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

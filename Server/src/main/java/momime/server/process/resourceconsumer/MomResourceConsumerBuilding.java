package momime.server.process.resourceconsumer;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageBuildingSoldFromLackOfProduction;
import momime.common.messages.NewTurnMessageTypeID;
import momime.server.MomSessionVariables;
import momime.server.process.CityProcessing;

/**
 * Building that consumes a particular type of resource
 */
public final class MomResourceConsumerBuilding implements MomResourceConsumer
{
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
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void kill (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		getCityProcessing ().sellBuilding ((MapCoordinates3DEx) getBuilding ().getCityLocation (), getBuilding ().getBuildingURN (), false, false, mom);

		if (getPlayer ().getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final NewTurnMessageBuildingSoldFromLackOfProduction buildingSold = new NewTurnMessageBuildingSoldFromLackOfProduction ();
			buildingSold.setMsgType (NewTurnMessageTypeID.BUILDING_LACK_OF_PRODUCTION);
			buildingSold.setCityLocation (getBuilding ().getCityLocation ());
			buildingSold.setBuildingID (getBuilding ().getBuildingID ());
			buildingSold.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (buildingSold);
		}
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
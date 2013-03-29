package momime.server.process.resourceconsumer;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CoordinatesUtils;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.server.database.ServerDatabaseEx;
import momime.server.process.CityProcessing;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Building that consumes a particular type of resource
 */
public final class MomResourceConsumerBuilding implements IMomResourceConsumer
{
	/** True map building that is consuming resources */
	private final MemoryBuilding building;

	/** The player who's resources are being consumed */
	private final PlayerServerDetails player;

	/** The type of resources being consumed */
	private final String productionTypeID;

	/** The amount of production being consumed */
	private final int consumptionAmount;

	/**
	 * @param aPlayer The player who's resources are being consumed
	 * @param aProductionTypeID The type of resources being consumed
	 * @param aConsumptionAmount The amount of production being consumed
	 * @param aBuilding True map building that is consuming resources
	 */
	public MomResourceConsumerBuilding (final PlayerServerDetails aPlayer, final String aProductionTypeID, final int aConsumptionAmount, final MemoryBuilding aBuilding)
	{
		super ();

		player = aPlayer;
		productionTypeID = aProductionTypeID;
		consumptionAmount = aConsumptionAmount;
		building = aBuilding;
	}

	/**
	 * @return The player who's resources are being consumed
	 */
	@Override
	public final PlayerServerDetails getPlayer ()
	{
		return player;
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
	 * @return The amount of production being consumed
	 */
	@Override
	public final int getConsumptionAmount ()
	{
		return consumptionAmount;
	}

	/**
	 * @return True map building that is consuming resources
	 */
	public final MemoryBuilding getBuilding ()
	{
		return building;
	}

	/**
	 * Sells this building to get some gold back and conserve resources
	 *
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param db Lookup lists built over the XML database
	 * @param sd Session description
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void kill (final FogOfWarMemory trueMap, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		debugLogger.entering (MomResourceConsumerBuilding.class.getName (), "kill",
			new String [] {CoordinatesUtils.overlandMapCoordinatesToString (getBuilding ().getCityLocation ()), getBuilding ().getBuildingID ()});

		CityProcessing.sellBuilding (trueMap, players, getBuilding ().getCityLocation (), getBuilding ().getBuildingID (), false, false, sd, db, debugLogger);

		if (getPlayer ().getPlayerDescription ().isHuman ())
		{
			final NewTurnMessageData buildingSold = new NewTurnMessageData ();
			buildingSold.setMsgType (NewTurnMessageTypeID.BUILDING_LACK_OF_PRODUCTION);
			buildingSold.setLocation (getBuilding ().getCityLocation ());
			buildingSold.setBuildingOrUnitID (getBuilding ().getBuildingID ());
			buildingSold.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (buildingSold);
		}

		debugLogger.exiting (MomResourceConsumerBuilding.class.getName (), "kill");
	}
}

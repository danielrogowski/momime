package momime.server.process.resourceconsumer;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.NewTurnMessageUnitKilledFromLackOfProduction;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.KillUnitActionID;

/**
 * Unit that consumes a particular type of resource
 */
public final class MomResourceConsumerUnit implements MomResourceConsumer
{
	/** True map unit that is consuming resources */
	private MemoryUnit unit;

	/** The player who's resources are being consumed */
	private PlayerServerDetails player;

	/** The type of resources being consumed */
	private String productionTypeID;

	/** The amount of production being consumed */
	private int consumptionAmount;

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
	 * @return True map unit that is consuming resources
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}

	/**
	 * @param aUnit True map unit that is consuming resources
	 */
	public final void setUnit (final MemoryUnit aUnit)
	{
		unit = aUnit;
	}
	
	/**
	 * Disbands this unit to conserve resources
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
		mom.getWorldUpdates ().killUnit (getUnit ().getUnitURN (), KillUnitActionID.LACK_OF_PRODUCTION);
		mom.getWorldUpdates ().process (mom);

		if (getPlayer ().getPlayerDescription ().isHuman ())
		{
			final NewTurnMessageUnitKilledFromLackOfProduction unitKilled = new NewTurnMessageUnitKilledFromLackOfProduction ();
			unitKilled.setMsgType (NewTurnMessageTypeID.UNIT_LACK_OF_PRODUCTION);
			unitKilled.setUnitURN (getUnit ().getUnitURN ());
			unitKilled.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (unitKilled);
		}
	}
}
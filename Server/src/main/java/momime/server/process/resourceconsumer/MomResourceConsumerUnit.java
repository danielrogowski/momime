package momime.server.process.resourceconsumer;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Unit that consumes a particular type of resource
 */
public final class MomResourceConsumerUnit implements MomResourceConsumer
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomResourceConsumerUnit.class.getName ());
	
	/** True map unit that is consuming resources */
	private final MemoryUnit unit;

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
	 * @param aUnit True map unit that is consuming resources
	 */
	public MomResourceConsumerUnit (final PlayerServerDetails aPlayer, final String aProductionTypeID, final int aConsumptionAmount, final MemoryUnit aUnit)
	{
		super ();

		player = aPlayer;
		productionTypeID = aProductionTypeID;
		consumptionAmount = aConsumptionAmount;
		unit = aUnit;
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
	 * @return True map unit that is consuming resources
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
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
		log.entering (MomResourceConsumerUnit.class.getName (), "kill", getUnit ().getUnitURN ());

		// Action needs to depend on the type of unit
		final KillUnitActionID action;
		if (mom.getServerDB ().findUnit (getUnit ().getUnitID (), "MomResourceConsumerUnit").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
			action = KillUnitActionID.HERO_LACK_OF_PRODUCTION;
		else
			action = KillUnitActionID.UNIT_LACK_OF_PRODUCTION;

		mom.getFogOfWarMidTurnChanges ().killUnitOnServerAndClients (getUnit (), action, null,
			mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

		if (getPlayer ().getPlayerDescription ().isHuman ())
		{
			final NewTurnMessageData unitKilled = new NewTurnMessageData ();
			unitKilled.setMsgType (NewTurnMessageTypeID.UNIT_LACK_OF_PRODUCTION);
			unitKilled.setUnitURN (getUnit ().getUnitURN ());
			unitKilled.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (unitKilled);
		}

		log.exiting (MomResourceConsumerUnit.class.getName (), "kill");
	}
}

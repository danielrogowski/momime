package momime.server.process.resourceconsumer;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.server.database.ServerDatabaseLookup;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Unit that consumes a particular type of resource
 */
public final class MomResourceConsumerUnit implements IMomResourceConsumer
{
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
		final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		debugLogger.entering (MomResourceConsumerUnit.class.getName (), "kill", getUnit ().getUnitURN ());

		// Action needs to depend on the type of unit
		final KillUnitActionID action;
		if (db.findUnit (getUnit ().getUnitID (), "MomResourceConsumerUnit").getUnitMagicRealm ().equals (CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
			action = KillUnitActionID.HERO_LACK_OF_PRODUCTION;
		else
			action = KillUnitActionID.UNIT_LACK_OF_PRODUCTION;

		FogOfWarMidTurnChanges.killUnitOnServerAndClients (getUnit (), action, trueMap, players, sd, db, debugLogger);

		if (getPlayer ().getPlayerDescription ().isHuman ())
		{
			final NewTurnMessageData unitKilled = new NewTurnMessageData ();
			unitKilled.setMsgType (NewTurnMessageTypeID.UNIT_LACK_OF_PRODUCTION);
			unitKilled.setUnitURN (getUnit ().getUnitURN ());
			unitKilled.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (unitKilled);
		}

		debugLogger.exiting (MomResourceConsumerUnit.class.getName (), "kill");
	}
}

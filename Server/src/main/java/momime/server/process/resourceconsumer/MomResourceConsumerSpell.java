package momime.server.process.resourceconsumer;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.server.MomSessionVariables;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Spell that consumes a particular type of resource
 */
public final class MomResourceConsumerSpell implements MomResourceConsumer
{
	/** Class logger */
	private final Logger log = Logger.getLogger (MomResourceConsumerSpell.class.getName ());
	
	/** True map spell that is consuming resources */
	private final MemoryMaintainedSpell spell;

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
	 * @param aSpell True map spell that is consuming resources
	 */
	public MomResourceConsumerSpell (final PlayerServerDetails aPlayer, final String aProductionTypeID, final int aConsumptionAmount, final MemoryMaintainedSpell aSpell)
	{
		super ();

		player = aPlayer;
		productionTypeID = aProductionTypeID;
		consumptionAmount = aConsumptionAmount;
		spell = aSpell;
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
	 * @return True map spell that is consuming resources
	 */
	public final MemoryMaintainedSpell getSpell ()
	{
		return spell;
	}

	/**
	 * Switches off this spell to conserve resources
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
		log.entering (MomResourceConsumerSpell.class.getName (), "kill", getSpell ().getSpellID ());

		mom.getSpellProcessing ().switchOffSpell (mom.getGeneralServerKnowledge ().getTrueMap (),
			getSpell ().getCastingPlayerID (), getSpell ().getSpellID (), getSpell ().getUnitURN (), getSpell ().getUnitSkillID (),
			getSpell ().isCastInCombat (), (OverlandMapCoordinatesEx) getSpell ().getCityLocation (), getSpell ().getCitySpellEffectID (),
			mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());

		if (getPlayer ().getPlayerDescription ().isHuman ())
		{
			final NewTurnMessageData spellSwitchedOff = new NewTurnMessageData ();
			spellSwitchedOff.setMsgType (NewTurnMessageTypeID.SPELL_LACK_OF_PRODUCTION);
			spellSwitchedOff.setSpellID (getSpell ().getSpellID ());
			spellSwitchedOff.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (spellSwitchedOff);
		}

		log.exiting (MomResourceConsumerSpell.class.getName (), "kill");
	}
}

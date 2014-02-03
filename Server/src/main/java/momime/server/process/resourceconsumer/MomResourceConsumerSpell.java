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
import momime.server.process.SpellProcessing;

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
	private MemoryMaintainedSpell spell;

	/** The player who's resources are being consumed */
	private PlayerServerDetails player;

	/** The type of resources being consumed */
	private String productionTypeID;

	/** The amount of production being consumed */
	private int consumptionAmount;

	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
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
	 * @return True map spell that is consuming resources
	 */
	public final MemoryMaintainedSpell getSpell ()
	{
		return spell;
	}

	/**
	 * @param aSpell True map spell that is consuming resources
	 */
	public final void setSpell (final MemoryMaintainedSpell aSpell)
	{
		spell = aSpell;
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

		getSpellProcessing ().switchOffSpell (mom.getGeneralServerKnowledge ().getTrueMap (),
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

	/**
	 * @return Spell processing methods
	 */
	public final SpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final SpellProcessing obj)
	{
		spellProcessing = obj;
	}
}

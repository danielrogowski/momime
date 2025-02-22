package momime.server.process.resourceconsumer;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageSpellSwitchedOffFromLackOfProduction;
import momime.common.messages.NewTurnMessageTypeID;
import momime.server.MomSessionVariables;

/**
 * Spell that consumes a particular type of resource
 */
public final class MomResourceConsumerSpell implements MomResourceConsumer
{
	/** True map spell that is consuming resources */
	private MemoryMaintainedSpell spell;

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
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void kill (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		mom.getWorldUpdates ().switchOffSpell (getSpell ().getSpellURN (), false);
		mom.getWorldUpdates ().process (mom);

		if (getPlayer ().getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final NewTurnMessageSpellSwitchedOffFromLackOfProduction spellSwitchedOff = new NewTurnMessageSpellSwitchedOffFromLackOfProduction ();
			spellSwitchedOff.setMsgType (NewTurnMessageTypeID.SPELL_LACK_OF_PRODUCTION);
			spellSwitchedOff.setSpellID (getSpell ().getSpellID ());
			spellSwitchedOff.setProductionTypeID (getProductionTypeID ());

			((MomTransientPlayerPrivateKnowledge) getPlayer ().getTransientPlayerPrivateKnowledge ()).getNewTurnMessage ().add (spellSwitchedOff);
		}
	}
}
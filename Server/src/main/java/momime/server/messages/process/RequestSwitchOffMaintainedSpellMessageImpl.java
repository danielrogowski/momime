package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.clienttoserver.RequestSwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.process.SpellProcessing;

/**
 * Client sends this when they want to switch off a maintained spell (overland, unit or city).
 * Note because spells don't get allocated any kind of spellURN, we basically have to pass the full details of the spell to the server to make sure that it can find it.
 */
public final class RequestSwitchOffMaintainedSpellMessageImpl extends RequestSwitchOffMaintainedSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestSwitchOffMaintainedSpellMessageImpl.class);

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, MomException, PlayerNotFoundException, RecordNotFoundException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Look for the spell
		final MemoryMaintainedSpell trueSpell = getMemoryMaintainedSpellUtils ().findSpellURN (getSpellURN (),
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());

		final Spell spellDef = (trueSpell == null) ? null : mom.getServerDB ().findSpell (trueSpell.getSpellID (), "RequestSwitchOffMaintainedSpellMessageImpl");
		
		// Do some checks
		final String error;
		if (trueSpell == null)
			error = "Couldn't find the spell you wanted to switch off";
		else if (!sender.getPlayerDescription ().getPlayerID ().equals (trueSpell.getCastingPlayerID ()))
			error = "You cannot switch off another wizard's spells!";
		else if ((spellDef.isPermanent () != null) && (spellDef.isPermanent ()))
			error = "You cannot switch off permanent spells";
		else
			error = null;
		
		// All ok?
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Switch off spell + associated CAEs
			getSpellProcessing ().switchOffSpell (getSpellURN (), mom);
			
			// Spell no longer using mana
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
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
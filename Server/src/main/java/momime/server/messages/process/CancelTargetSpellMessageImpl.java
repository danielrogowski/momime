package momime.server.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.clienttoserver.v0_9_5.CancelTargetSpellMessage;
import momime.common.messages.servertoclient.v0_9_5.TextPopupMessage;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerResourceCalculations;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client sends this to specify that, after bothering to finish casting an overland spell, they don't want to pick a target for it after all
 */
public final class CancelTargetSpellMessageImpl extends CancelTargetSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CancelTargetSpellMessageImpl.class.getName ());

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the client
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the client
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (CancelTargetSpellMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getSpellID ()});
		
		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		// Spell should already exist, but not targetted
		final MemoryMaintainedSpell maintainedSpell = getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
				sender.getPlayerDescription ().getPlayerID (), getSpellID (), null, null, null, null);
	
		// Do all the checks
		final String error;
		if (maintainedSpell == null)
			error = "Can't find an instance of this spell awaiting targetting";
		else
			error = null;
	
		// All ok?
		if (error != null)
		{
			// Return error
			log.warning (CancelTargetSpellMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Remove it
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().remove (maintainedSpell);
			
			// Cancelled spell will probably use up some mana maintenance
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.exiting (CancelTargetSpellMessageImpl.class.getName (), "process");
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
	public final MomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final MomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}
}

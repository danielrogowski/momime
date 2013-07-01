package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.clienttoserver.v0_9_4.RequestSwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.server.IMomSessionVariables;

import com.ndg.multiplayer.server.ProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client sends this when they want to switch off a maintained spell (overland, unit or city).
 * Note because spells don't get allocated any kind of spellURN, we basically have to pass the full details of the spell to the server to make sure that it can find it.
 */
public final class RequestSwitchOffMaintainedSpellMessageImpl extends RequestSwitchOffMaintainedSpellMessage implements ProcessableClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (RequestSwitchOffMaintainedSpellMessageImpl.class.getName ());

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
		log.entering (RequestSwitchOffMaintainedSpellMessageImpl.class.getName (), "process", sender.getPlayerDescription ().getPlayerID ());

		final IMomSessionVariables mom = (IMomSessionVariables) thread;

		// Look for the spell
		final MemoryMaintainedSpell trueSpell = mom.getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), getCastingPlayerID (), getSpellID (),
			getUnitURN (), getUnitSkillID (), (OverlandMapCoordinatesEx) getCityLocation (), getCitySpellEffectID ());
		
		// Do some checks
		final String error;
		if (!sender.getPlayerDescription ().getPlayerID ().equals (getCastingPlayerID ()))
			error = "You cannot switch off another wizard's spells!";
		else if (trueSpell == null)
			error = "Couldn't find the spell you wanted to switch off";
		else
			error = null;
		
		// All ok?
		if (error != null)
		{
			// Return error
			log.warning (RequestSwitchOffMaintainedSpellMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Switch off spell + associated CAEs
			mom.getSpellProcessing ().switchOffSpell (mom.getGeneralServerKnowledge ().getTrueMap (), trueSpell.getCastingPlayerID (), trueSpell.getSpellID (),
				trueSpell.getUnitURN (), trueSpell.getUnitSkillID (), trueSpell.isCastInCombat (), (OverlandMapCoordinatesEx) trueSpell.getCityLocation (),
				trueSpell.getCitySpellEffectID (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// Spell no longer using mana
			mom.getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}

		log.exiting (RequestSwitchOffMaintainedSpellMessageImpl.class.getName (), "process");
	}
}

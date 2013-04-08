package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.clienttoserver.v0_9_4.ChooseCustomPicksMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.PlayerPick;
import momime.server.IMomSessionVariables;
import momime.server.utils.PlayerPickServerUtils;

import com.ndg.multiplayer.server.IProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Client telling Server about all the custom picks they've chosen
 */
public final class ChooseCustomPicksMessageImpl extends ChooseCustomPicksMessage implements IProcessableClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 * @throws RecordNotFoundException If the player has picks which we can't find in the cache, or the AI player chooses a spell which we can't then find in their list
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender, final Logger debugLogger)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException
	{
		debugLogger.entering (ChooseCustomPicksMessageImpl.class.getName (), "process", new Integer [] {sender.getPlayerDescription ().getPlayerID (), getPick ().size ()});

		final IMomSessionVariables mom = (IMomSessionVariables) thread;

		// Validate the requested picks
		final String error = PlayerPickServerUtils.validateCustomPicks (sender, getPick (), mom.getSessionDescription (), mom.getServerDB (), debugLogger);
		if (error != null)
		{
			// Return error
			debugLogger.warning (ChooseCustomPicksMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Record custom picks on server
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
			final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) sender.getTransientPlayerPrivateKnowledge ();
			priv.setCustomPicksChosen (true);

			for (final WizardPick srcPick : getPick ())
			{
				final PlayerPick destPick = new PlayerPick ();
				destPick.setPickID (srcPick.getPick ());
				destPick.setQuantity (srcPick.getQuantity ());
				destPick.setOriginalQuantity (srcPick.getQuantity ());
				ppk.getPick ().add (destPick);
			}

			// Commenting this out because don't think client needs this info yet
			// Send picks
			// if (player.getPlayerDescription ().isHuman ())
			// sendPicksToPlayer (players, player, false, false, debugLogger);

			// Tell client to either pick free starting spells or pick a race, depending on whether the pre-defined wizard chosen has >1 of any kind of book
			// Its fine to do this before we confirm to the client that their wizard choice was OK by the mmChosenWizard message sent below
			debugLogger.finest (ChooseCustomPicksMessageImpl.class.getName () + ".process: About to search for first realm (if any) where human player " + sender.getPlayerDescription ().getPlayerName () + " gets free spells");
			final ChooseInitialSpellsNowMessage chooseSpellsMsg = PlayerPickServerUtils.findRealmIDWhereWeNeedToChooseFreeSpells (sender, mom.getServerDB (), debugLogger);
			if (chooseSpellsMsg != null)
				sender.getConnection ().sendMessageToClient (chooseSpellsMsg);
			else
				sender.getConnection ().sendMessageToClient (new ChooseYourRaceNowMessage ());
		}

		debugLogger.exiting (ChooseCustomPicksMessageImpl.class.getName (), "process");
	}
}

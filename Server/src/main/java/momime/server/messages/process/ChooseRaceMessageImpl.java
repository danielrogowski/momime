package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.v0_9_4.ChooseRaceMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.servertoclient.v0_9_4.YourRaceIsOkMessage;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.server.MomSessionThread;
import momime.server.process.PlayerMessageProcessing;
import momime.server.utils.PlayerPickServerUtils;

import com.ndg.multiplayer.server.IProcessableClientToServerMessage;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Message we send to the server when we choose which race we want to be
 */
public final class ChooseRaceMessageImpl extends ChooseRaceMessage implements IProcessableClientToServerMessage
{
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If there is a problem in any game logic or data
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws PlayerNotFoundException If we encounter players that we cannot find in the list
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender, final Logger debugLogger)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException, PlayerNotFoundException
	{
		debugLogger.entering (ChooseRaceMessageImpl.class.getName (), "process", new String [] {new Integer (sender.getPlayerDescription ().getPlayerID ()).toString (), getRaceID ()});

		final MomSessionThread mom = (MomSessionThread) thread;

		final String error = PlayerPickServerUtils.validateRaceChoice (sender, getRaceID (), mom.getServerDB (), debugLogger);
		if (error != null)
		{
			// Return error
			debugLogger.warning (ChooseRaceMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Remember choice on the server
			final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) sender.getTransientPlayerPrivateKnowledge ();
			priv.setFirstCityRaceID (getRaceID ());

			// Tell player choice was OK
			sender.getConnection ().sendMessageToClient (new YourRaceIsOkMessage ());

			// If all players have chosen then start the game
			PlayerMessageProcessing.checkIfCanStartGame (mom, debugLogger);
		}

		debugLogger.exiting (ChooseRaceMessageImpl.class.getName (), "process", error);
	}
}

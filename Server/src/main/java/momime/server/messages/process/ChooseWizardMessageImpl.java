package momime.server.messages.process;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.ChooseWizardMessage;
import momime.server.MomSessionVariables;
import momime.server.process.PlayerMessageProcessing;

/**
 * Message we send to the server when we choose which wizard we want to be
 */
public final class ChooseWizardMessageImpl extends ChooseWizardMessage implements PostSessionClientToServerMessage
{
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		getPlayerMessageProcessing ().chooseWizard (getWizardID (), sender, mom);
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}
}
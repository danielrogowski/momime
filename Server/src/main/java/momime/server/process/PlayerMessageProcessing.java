package momime.server.process;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.TurnSystem;
import momime.server.MomSessionVariables;

/**
 * Methods for any significant message processing to do with game startup and the turn system that isn't done in the message implementations
 */
public interface PlayerMessageProcessing
{
	/**
	 * Message we send to the server when we choose which wizard we want to be; AI players also call this to do their wizard, picks and spells setup
	 * which is why this isn't all just in ChooseWizardMessageImpl
	 *
	 * @param wizardID wizard ID the player wants to choose
	 * @param player Player who sent the message
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 */
	public void chooseWizard (final String wizardID, final PlayerServerDetails player, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException;

	/**
	 * If all players have chosen their wizards and, if necessary, custom picks, then sends message to tell everyone to start
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending any messages to the clients
	 * @throws XMLStreamException If there is a problem sending any messages to the clients
	 * @throws IOException If there are any other kinds of faults
	 */
	public void checkIfCanStartGame (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * After reloading a saved game, checks whether all human players have joined back in, and if so then starts the game back up again
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending any messages to the clients
	 * @throws XMLStreamException If there is a problem sending any messages to the clients
	 * @throws MomException If there is a problem in any game logic or data
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws PlayerNotFoundException If we encounter players that we cannot find in the list
	 */
	public void checkIfCanStartLoadedGame (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException, PlayerNotFoundException;
	
	/**
	 * In a one-player-at-a-time game, this gets called when a player clicks the Next Turn button to tell everyone whose turn it is now
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param loadingSavedGame True if the turn is being started immediately after loading a saved game
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If an expected element cannot be found
	 * @throws PlayerNotFoundException If the player who owns a unit, or the previous or next player cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public void switchToNextPlayer (final MomSessionVariables mom, final boolean loadingSavedGame)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Kicks off a new turn in an everybody-allocate-movement-then-move-simultaneously game
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param loadingSavedGame True if the turn is being started immediately after loading a saved game
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws RecordNotFoundException If an expected element cannot be found
	 * @throws PlayerNotFoundException If the player who owns a unit, or the previous or next player cannot be found
	 * @throws MomException If the player's unit doesn't have the experience skill
	 */
	public void kickOffSimultaneousTurn (final MomSessionVariables mom, final boolean loadingSavedGame)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Sends all new turn messages queued up on the server to each player, then clears them from the server
	 * This is also used to trigger new turns or when it is a different player's turn
	 * 
	 * @param gpk Public knowledge structure; can pass this as null if messageType = null
	 * @param players List of players in this session
	 * @param messageType Type of message to send according to the turn system being used; null = just send messages, don't start a new turn
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the value of messageType isn't recognized
	 */
	public void sendNewTurnMessages (final MomGeneralPublicKnowledge gpk, final List<PlayerServerDetails> players,
		final TurnSystem messageType) throws JAXBException, XMLStreamException, MomException;
	
	/**
	 * Human player has clicked the next turn button, or AI player's turn has finished
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param player Player who hit the next turn button
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void nextTurnButton (final MomSessionVariables mom, final PlayerServerDetails player)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Processes the 'end phase' (for want of something better to call it), which happens at the end of each player's turn
	 *
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process start phase for all players; if specified will process start phase only for the specified player
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public void endPhase (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Checks to see if anyone has won the game, by every other wizard being defeated
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws PlayerNotFoundException If the requested playerID cannot be found
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void checkIfWonGame (final MomSessionVariables mom) throws PlayerNotFoundException, JAXBException, XMLStreamException;
}
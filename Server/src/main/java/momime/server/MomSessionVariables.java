package momime.server;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.ServerToClientSessionConnection;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.JoinSuccessfulReason;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabase;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.server.ai.DiplomacyProposal;
import momime.server.knowledge.CombatDetails;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.worldupdates.WorldUpdates;

/**
 * Container for all the values held against a session
 */
public interface MomSessionVariables
{
	/**
	 * @return Session description, typecasted to MoM specific type
	 */
	public MomSessionDescription getSessionDescription ();

	/**
	 * @return Server XML in use for this session
	 */
	public CommonDatabase getServerDB ();

	/**
	 * @return Server general knowledge, typecasted to MoM specific type
	 */
	public MomGeneralServerKnowledge getGeneralServerKnowledge ();
	
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	public MomGeneralPublicKnowledge getGeneralPublicKnowledge ();
	
	/**
	 * @return All information about all players in this session - some may be AI players or disconnected
	 */
	public List<PlayerServerDetails> getPlayers ();
	
	/**
	 * @return Overland map generator for this session
	 */
	public OverlandMapGenerator getOverlandMapGenerator ();
	
	/**
	 * This is used for all cases where players join or rejoin sessions, so for newSession, joinSession, loadGame, adding both human and AI players.
	 * Note when it is being used by newSession and loadGame, we are breaking our rule by allowing it to be ran from another thread,
	 * and it accesses and updates the player list - however this is fine, because in that situation it is being ran prior to
	 * this thread even starting, so is still safe.
	 * 
	 * @param pd Description for joining player; this must have been completely populated including the human/AI flag and the playerID.
	 * 	The only exception is that AI players may be added with a null playerID, in which a playerID will be allocated to them.
	 * @param connection Connection to player who requested new session; only applicable if newPlayer.isHuman () is true, and even then, can be left null
	 * 	(as will be the case when reloading multiplayer games - the connection for the secondary players will initially be null until they join the session).
	 * @param reason The type of session the player joined into; ignored and can be null if connection is null OR sendMessages is false
	 * @param sendMessages Whether to send JoinSuccessful & AdditionalPlayerJoined messages
	 * @param existingPlayerDetails If the player has existing details (i.e. persistent/transient public/private details), pass them in here.
	 * 	The details will be added to the player list if not already present (for rejoins they'll already be in the list, for loading saved games they won't be).
	 * @throws JAXBException If there is a problem converting the reply into XML
	 * @throws XMLStreamException If there is a problem writing the reply to the XML stream
	 * @throws IOException If there is a problem sending any reply back to the client
	 * @return Newly created player
	 */
	public PlayerServerDetails addPlayer (final PlayerDescription pd, final ServerToClientSessionConnection connection, final JoinSuccessfulReason reason,
		final boolean sendMessages, final PlayerServerDetails existingPlayerDetails)
		throws JAXBException, XMLStreamException, IOException;

	/**
	 * Records a save game file of this session's state
	 * @param identifier Identifier of the sequence of saved game files for this session, such as a turn number; can be left null
	 * @throws IOException If saved games are not enabled on this server, or there is some error saving the file
	 * @throws JAXBException If there is an XML error trying to save the file
	 */
	public void saveGame (final String identifier) throws IOException, JAXBException;

	/**
	 * Searches for all save points of the current session and deletes the oldest ones, keeping only the number of latest save points as specified
	 * @param keepCount Number of save points to keep
	 * @return Number that were deleted
	 */
	public int deleteOldestSavePoints (final int keepCount);
	
	/**
	 * @param playerID Human player to convert to AI player
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void updateHumanPlayerToAI (final int playerID)
		throws IOException, JAXBException, XMLStreamException;

	/**
	 * @return Engine for updating server's true copy of the game world
	 */
	public WorldUpdates getWorldUpdates ();

	/**
	 * @return Combat details storage
	 */
	public List<CombatDetails> getCombatDetails ();
	
	/**
	 * @return List of diplomacy proposals an AI player is waiting to send to a human player
	 */
	public List<DiplomacyProposal> getPendingDiplomacyProposals ();
	
	/**
	 * @return Set to true if the thread is running a game consisting of only AI players
	 */
	public boolean isAiGame ();
}
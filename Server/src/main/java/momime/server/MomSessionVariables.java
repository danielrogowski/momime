package momime.server;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.server.database.ServerDatabaseEx;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;
import momime.server.mapgenerator.OverlandMapGenerator;

import org.apache.commons.logging.Log;

import com.ndg.multiplayer.server.ServerToClientSessionConnection;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

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
	public ServerDatabaseEx getServerDB ();

	/**
	 * @return Server general knowledge, typecasted to MoM specific type
	 */
	public MomGeneralServerKnowledgeEx getGeneralServerKnowledge ();
	
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	public MomGeneralPublicKnowledge getGeneralPublicKnowledge ();
	
	/**
	 * @return All information about all players in this session - some may be AI players or disconnected
	 */
	public List<PlayerServerDetails> getPlayers ();
	
	/**
	 * @return Logger for logging key messages relating to this session
	 */
	public Log getSessionLogger ();
	
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
	 * @param sendMessages Whether to send JoinSuccessful & AdditionalPlayerJoined messages
	 * @param existingPlayerDetails If the player has existing details (i.e. persistent/transient public/private details), pass them in here.
	 * 	The details will be added to the player list if not already present (for rejoins they'll already be in the list, for loading saved games they won't be).
	 * @throws JAXBException If there is a problem converting the reply into XML
	 * @throws XMLStreamException If there is a problem writing the reply to the XML stream
	 * @return Newly created player
	 */
	public PlayerServerDetails addPlayer (final PlayerDescription pd, final ServerToClientSessionConnection connection,
		final boolean sendMessages, final PlayerServerDetails existingPlayerDetails)
		throws JAXBException, XMLStreamException;
}
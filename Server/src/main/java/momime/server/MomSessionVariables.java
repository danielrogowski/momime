package momime.server;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.v0_9_5.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.server.database.ServerDatabaseEx;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;

import com.ndg.map.CoordinateSystem;
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
	 * @return Combat map coordinate system, expect this to be merged into session desc once client is also in Java
	 */
	public CoordinateSystem getCombatMapCoordinateSystem ();

	/**
	 * @return Server XML in use for this session
	 */
	public ServerDatabaseEx getServerDB ();

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
	 * @return Logger for logging key messages relating to this session
	 */
	public Logger getSessionLogger ();
	
	/**
	 * @return Overland map generator for this session
	 */
	public OverlandMapGenerator getOverlandMapGenerator ();
	
	/**
	 * Adds an AI player
	 *
	 * @param newPlayer Details of player who requested new session
	 * @return Newly created player
	 * @throws JAXBException If there is a problem converting the reply into XML
	 * @throws XMLStreamException If there is a problem writing the reply to the XML stream
	 */
	public PlayerServerDetails addComputerPlayer (final PlayerDescription newPlayer)
		throws JAXBException, XMLStreamException;
}

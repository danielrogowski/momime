package momime.server;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.messages.IPlayerPickUtils;
import momime.common.messages.IResourceValueUtils;
import momime.common.messages.ISpellUtils;
import momime.common.messages.IUnitUtils;
import momime.common.messages.v0_9_4.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.calculations.IMomServerResourceCalculations;
import momime.server.calculations.IMomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.IFogOfWarMidTurnChanges;
import momime.server.messages.v0_9_4.MomGeneralServerKnowledge;
import momime.server.process.ICityProcessing;
import momime.server.process.IPlayerMessageProcessing;
import momime.server.process.ISpellProcessing;
import momime.server.utils.ICityServerUtils;
import momime.server.utils.IPlayerPickServerUtils;
import momime.server.utils.ISpellServerUtils;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Container for all the values held against a session
 * Declared as an interface mainly so test methods can mock this without needing to actually create instances of MomSessionThread
 */
public interface IMomSessionVariables
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
	 * @return Methods for updating true map + players' memory
	 */
	public IFogOfWarMidTurnChanges getFogOfWarMidTurnChanges ();

	/**
	 * @return Methods for dealing with player msgs
	 */
	public IPlayerMessageProcessing getPlayerMessageProcessing ();
	
	/**
	 * @return Spell processing methods
	 */
	public ISpellProcessing getSpellProcessing ();

	/**
	 * @return City processing methods
	 */
	public ICityProcessing getCityProcessing ();

	/**
	 * @return Resource calculations
	 */
	public IMomServerResourceCalculations getServerResourceCalculations ();
	
	/**
	 * @return Resource value utils
	 */
	public IResourceValueUtils getResourceValueUtils ();
	
	/**
	 * @return Player pick utils
	 */
	public IPlayerPickUtils getPlayerPickUtils ();
	
	/**
	 * @return Unit utils
	 */
	public IUnitUtils getUnitUtils ();
	
	/**
	 * @return Spell utils
	 */
	public ISpellUtils getSpellUtils ();
	
	/**
	 * @return Server-only city utils
	 */
	public ICityServerUtils getCityServerUtils ();
	
	/**
	 * @return Server-only pick utils
	 */
	public IPlayerPickServerUtils getPlayerPickServerUtils ();
	
	/**
	 * @return Server-only spell utils
	 */
	public ISpellServerUtils getSpellServerUtils ();
	
	/**
	 * @return Server-only unit calculations
	 */
	public IMomServerUnitCalculations getServerUnitCalculations ();
	
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

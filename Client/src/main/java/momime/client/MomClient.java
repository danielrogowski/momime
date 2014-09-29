package momime.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.database.ClientDatabaseEx;
import momime.client.database.v0_9_5.NewGameDatabase;
import momime.client.ui.frames.ChangeConstructionUI;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.messages.v0_9_5.MomGeneralPublicKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.MomTransientPlayerPrivateKnowledge;

import com.ndg.multiplayer.base.client.ClientToServerConnection;
import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * A lot of data structures, especially the db, are accessed from the client, but in ways that can't easily be
 * mocked out in unit tests.  So instead this interface exists as the reference point for any places that
 * obtain data structures from the client so we don't have to use the real MomClientImpl in unit tests.
 */
public interface MomClient
{
	// Methods implemented in MultiplayerBaseClient
	
	/**
	 * @return Connection to server
	 */
	public ClientToServerConnection getServerConnection ();
	
	/**
	 * @param addr IP or DNS name of the server
	 */
	public void setServerAddress (final String addr);
	
	/**
	 * Initiate connection to the server
	 * @throws IOException If there is a problem making the connection
	 * @throws InterruptedException If there is a problem waiting for the thread to start up
	 * @throws JAXBException If there is a problem sending something to connecting client
	 * @throws XMLStreamException If there is a problem sending something to connecting client
	 */
	public void connect () throws IOException, InterruptedException, JAXBException, XMLStreamException;
	
	/**
	 * Finishes a custom duration message, or an animated message with isFinishAfterDuration = false
	 * 
	 * @param msg Message to finish
	 * @throws IOException If we are unable to process the message
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 */
	public void finishCustomDurationMessage (final CustomDurationServerToClientMessage msg) throws IOException, JAXBException, XMLStreamException;
	
	// Methods implemented in MultiplayerSessionClient
	
	/**
	 * @return Publicly available information about all players in our current session; this is how we access our own public details too, by searching for our player ID
	 */
	public List<PlayerPublicDetails> getPlayers ();

	/**
	 * @return The player ID we're currently logged as; null if not currently logged in
	 */
	public Integer getOurPlayerID ();
	
	// Methods implemented in MomClientImpl
	
	/**
	 * @return Name that we logged in using
	 */
	public String getOurPlayerName ();
	
	/**
	 * @param name Name that we logged in using
	 */
	public void setOurPlayerName (final String name);
	
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	public MomGeneralPublicKnowledge getGeneralPublicKnowledge ();

	/**
	 * @return Client XML in use for this session
	 */
	public ClientDatabaseEx getClientDB ();
	
	/**
	 * @return Session description, typecasted to MoM specific type
	 */
	public MomSessionDescription getSessionDescription ();
	
	/**
	 * @return Private knowledge about our player that is persisted to save game files,  typecasted to MoM specific type
	 */
	public MomPersistentPlayerPrivateKnowledge getOurPersistentPlayerPrivateKnowledge ();
	
	/**
	 * @return Private knowledge about our player that is not persisted to save game files,  typecasted to MoM specific type
	 */
	public MomTransientPlayerPrivateKnowledge getOurTransientPlayerPrivateKnowledge ();

	/**
	 * @return List of all city views currently open, keyed by coordinates.toString ()
	 */
	public Map<String, CityViewUI> getCityViews ();
	
	/**
	 * @return List of all change constructions currently open, keyed by coordinates.toString ()
	 */
	public Map<String, ChangeConstructionUI> getChangeConstructions ();
	
	/**
	 * @return List of all unit info screens currently open, keyed by Unit URN
	 */
	public Map<Integer, UnitInfoUI> getUnitInfos ();

	/**
	 * @return Info we need in order to create games; sent from server
	 */
	public NewGameDatabase getNewGameDatabase ();

	/**
	 * @param db Info we need in order to create games; sent from server
	 */
	public void setNewGameDatabase (final NewGameDatabase db);
}
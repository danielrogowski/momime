package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.ConnectToServerUI;
import momime.common.messages.servertoclient.v0_9_5.NewGameDatabaseMessage;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Message server sends to clients as they connect, to tell them what databases and pre-defined settings are available
 */
public final class NewGameDatabaseMessageImpl extends NewGameDatabaseMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (NewGameDatabaseMessageImpl.class.getName ());
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Connect to server UI */
	private ConnectToServerUI connectToServerUI;
	
	/**
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (NewGameDatabaseMessageImpl.class.getName (), "process");

		// Record it
		getClient ().setNewGameDatabase (getNewGameDatabase ());
		
		// Now we're connected, log in, or create account then log in
		getConnectToServerUI ().afterConnected ();
		
		log.exiting (NewGameDatabaseMessageImpl.class.getName (), "process");
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Connect to server UI
	 */
	public final ConnectToServerUI getConnectToServerUI ()
	{
		return connectToServerUI;
	}

	/**
	 * @param ui Connect to server UI
	 */
	public final void setConnectToServerUI (final ConnectToServerUI ui)
	{
		connectToServerUI = ui;
	}
}

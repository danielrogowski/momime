package momime.client.messages.process;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.messages.servertoclient.v0_9_5.ChosenStandardPhotoMessage;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Message server sends to players to tell them that a player is using a standard photo
 * (either because they're using a standard wizard, or a custom wizard with a standard photo)
 */
public final class ChosenStandardPhotoMessageImpl extends ChosenStandardPhotoMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (ChosenStandardPhotoMessageImpl.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
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
		log.entering (ChosenStandardPhotoMessageImpl.class.getName (), "process", new String []
			{new Integer (getPlayerID ()).toString (), getPhotoID ()});

		final PlayerPublicDetails player = MultiplayerSessionUtils.findPlayerWithID (getClient ().getPlayers (), getPlayerID (), "ChosenStandardPhotoMessageImpl");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();

		// Store photo ID
		pub.setStandardPhotoID (getPhotoID ());
		
		// Store flag colour
		trans.setFlagColour (getGraphicsDB ().findWizard (getPhotoID (), "ChosenStandardPhotoMessageImpl").getFlagColour ());
		
		log.exiting (ChosenStandardPhotoMessageImpl.class.getName (), "process");
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
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}
}

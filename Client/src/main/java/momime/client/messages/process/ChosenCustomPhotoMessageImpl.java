package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.servertoclient.ChosenCustomPhotoMessage;

/**
 * Message server sends to players to tell them that a player chose a custom wizard with a custom photo and flag colour
 */
public final class ChosenCustomPhotoMessageImpl extends ChosenCustomPhotoMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getPlayerID (), "ChosenCustomPhotoMessageImpl");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();

		// Store image data
		pub.setCustomPhoto (getNdgBmpImage ());
		
		// Store flag colour
		pub.setCustomFlagColour (getFlagColour ());
		trans.setFlagColour (getFlagColour ());
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
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}
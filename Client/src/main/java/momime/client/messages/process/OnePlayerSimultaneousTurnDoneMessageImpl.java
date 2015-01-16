package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.servertoclient.OnePlayerSimultaneousTurnDoneMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Server sends this to all clients to notify that one player has finished allocating simultaneous movement
 * (so the client can show a 'tick' next to them in the turn bar)
 */
public final class OnePlayerSimultaneousTurnDoneMessageImpl extends OnePlayerSimultaneousTurnDoneMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (StartSimultaneousTurnMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Player ID " + getPlayerID ());
		
		final PlayerPublicDetails donePlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getPlayerID (), "OnePlayerSimultaneousTurnDoneMessageImpl");
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) donePlayer.getTransientPlayerPublicKnowledge ();
		trans.setMovementAllocatedForTurnNumber (getClient ().getGeneralPublicKnowledge ().getTurnNumber ());
		
		getOverlandMapRightHandPanel ().repaintColourPatches ();
		
		log.trace ("Exiting start");
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

	/**
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}
}
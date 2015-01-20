package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.scheduledcombatmessages.ScheduledCombatMessageProcessing;
import momime.client.ui.frames.ScheduledCombatsUI;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.servertoclient.PlayerCombatRequestStatusMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Server sends this to inform clients whether players are are busy fighting a combat or have
 * requested a particular combat (in either situation, we can't request to play a combat against them)
 */
public final class PlayerCombatRequestStatusMessageImpl extends PlayerCombatRequestStatusMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (PlayerCombatRequestStatusMessageImpl.class);

	/** Scheduled combats list */
	private ScheduledCombatsUI scheduledCombatsUI;
	
	/** Scheduled combat message processing */
	private ScheduledCombatMessageProcessing scheduledCombatMessageProcessing;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: Player ID " + getPlayerID ());

		// Update whether this player is in combat
		final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getPlayerID (), "PlayerCombatRequestStatusMessageImpl");
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
		
		trans.setCurrentlyPlayingCombat (isCurrentlyPlayingCombat ());
		trans.setScheduledCombatUrnRequested (getRequestedScheduledCombatURN ());
		
		// Only update the UI if it is already being displayed (since this gets sent after starting a subsequent turn)
		if (getScheduledCombatsUI ().isVisible ())
			getScheduledCombatsUI ().setCombatMessages (getScheduledCombatMessageProcessing ().sortAndAddCategories ());
		
		log.trace ("Exiting start");
	}
	
	/**
	 * @return Scheduled combats list
	 */
	public final ScheduledCombatsUI getScheduledCombatsUI ()
	{
		return scheduledCombatsUI;
	}

	/**
	 * @param ui Scheduled combats list
	 */
	public final void setScheduledCombatsUI (final ScheduledCombatsUI ui)
	{
		scheduledCombatsUI = ui;
	}

	/**
	 * @return Scheduled combat message processing
	 */
	public final ScheduledCombatMessageProcessing getScheduledCombatMessageProcessing ()
	{
		return scheduledCombatMessageProcessing;
	}

	/**
	 * @param proc Scheduled combat message processing
	 */
	public final void setScheduledCombatMessageProcessing (final ScheduledCombatMessageProcessing proc)
	{
		scheduledCombatMessageProcessing = proc;
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
}
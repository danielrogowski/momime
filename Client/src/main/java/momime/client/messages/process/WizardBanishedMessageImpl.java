package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;

import momime.client.MomClient;
import momime.client.ui.frames.WizardBanishedUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.common.messages.servertoclient.WizardBanishedMessage;

/**
 * Server announces to everybody when a wizard gets banished, so the clients can show the animation for it
 */
public final class WizardBanishedMessageImpl extends WizardBanishedMessage implements AnimatedServerToClientMessage 
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (WizardBanishedMessageImpl.class);

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** UI dialog created to show this message */
	private WizardBanishedUI wizardBanishedUI;

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		wizardBanishedUI = getPrototypeFrameCreator ().createWizardBanished ();
		wizardBanishedUI.setBanishedWizard (getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getBanishedPlayerID (), "WizardBanishedMessageImpl (A)"));
		wizardBanishedUI.setBanishingWizard (getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getBanishingPlayerID (), "WizardBanishedMessageImpl (B)"));
		wizardBanishedUI.setDefeated (isDefeated ());
		wizardBanishedUI.setVisible (true);
		
		log.trace ("Exiting start");
	}
	
	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return wizardBanishedUI.getDuration ();
	}
	
	/**
	 * @return Number of ticks that the duration is divided into
	 */
	@Override
	public final int getTickCount ()
	{
		return wizardBanishedUI.getTickCount ();
	}
	
	/**
	 * @param tickNumber How many ticks have occurred, from 1..tickCount
	 */
	@Override
	public final void tick (final int tickNumber)
	{
		wizardBanishedUI.tick (tickNumber);
	}
	
	/**
	 * Controls whether the animation finishes automatically after duration (tickCount calls to the tick method) have executed, or has
	 * custom finishing conditions, in which case the application must take care of finishing the animation in the
	 * manner described on CustomDurationServerToClientMessage.
	 * 
	 * @return True if the multiplayer layer finishes the animation automatically after duration has elaped; False if the application takes care of finishing the animation
	 */
	@Override
	public final boolean isFinishAfterDuration ()
	{
		return true;
	}
	
	/**
	 * Close out the UI when the animation finishes
	 */
	@Override
	public final void finish ()
	{
		wizardBanishedUI.finish ();
	}
	
	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
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
package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.ui.dialogs.WizardWonUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.common.messages.servertoclient.PlayAnimationMessage;

/**
 * Server telling the client to play an animation about a significant game event, like casting Spell of Mastery.
 * Unlike updateWizardStateMessage, the animation has no bearing on the game and pushes no data updates whatsoever, the client just plays the anim and that's it.
 */
public final class PlayAnimationMessageImpl extends PlayAnimationMessage implements CustomDurationServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (PlayAnimationMessageImpl.class);

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
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
		final PlayerPublicDetails player = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getPlayerID (), "PlayAnimationMessageImpl");
		
		switch (getAnimationID ())
		{
			case WON:
				final WizardWonUI wizardWonUI = getPrototypeFrameCreator ().createWizardWon ();
				wizardWonUI.setWinningWizard (player);
				wizardWonUI.setPlayAnimationMessage (this);
				wizardWonUI.setVisible (true);
				break;
				
			default:
				// Its just an animation, don't throw a major exception for it
				log.error ("PlayAnimationMessageImpl doesn't know how to play animation \"" + getAnimationID () + "\"");
				getClient ().finishCustomDurationMessage (this);
		}
	}
	
	/**
	 * Nothing to do here when the message completes, because its all handled by whichever animation UI we display
	 */
	@Override
	public final void finish ()
	{
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
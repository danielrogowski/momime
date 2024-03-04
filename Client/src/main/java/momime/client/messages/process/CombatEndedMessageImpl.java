package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.graphics.AnimationContainer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.dialogs.CombatEndedUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.SpellBookNewUI;
import momime.client.utils.AnimationController;
import momime.common.messages.servertoclient.CombatEndedMessage;
import momime.common.utils.SpellCastType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;

/**
 * Server sends this to client to say who won a combat, and whether a city was captured or razed as a result
 */
public final class CombatEndedMessageImpl extends CombatEndedMessage implements CustomDurationServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CombatEndedMessageImpl.class);
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Combat UI */
	private CombatUI combatUI;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Music player */
	private MomAudioPlayer musicPlayer;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Spell book */
	private SpellBookNewUI spellBookUI;
	
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
		final CombatEndedUI ui = getPrototypeFrameCreator ().createCombatEnded ();
		ui.setMessage (this);
		ui.setVisible (true);
	}

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void finish () throws JAXBException, XMLStreamException, IOException
	{
		// Close down the UI
		getCombatUI ().setVisible (false);
		
		// Switch spell book to showing overland spells
		getSpellBookUI ().setCastType (SpellCastType.OVERLAND);
		
		// Go back to the overland music
		try
		{
			getMusicPlayer ().setShuffle (true);
			getMusicPlayer ().playPlayList (GraphicsDatabaseConstants.PLAY_LIST_OVERLAND_MUSIC, AnimationContainer.GRAPHICS_XML);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Prompt for next overland movement
		getOverlandMapProcessing ().selectNextUnitToMoveOverland ();
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
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}

	/**
	 * @return Animation controller
	 */
	public final AnimationController getAnim ()
	{
		return anim;
	}

	/**
	 * @param controller Animation controller
	 */
	public final void setAnim (final AnimationController controller)
	{
		anim = controller;
	}

	/**
	 * @return Music player
	 */
	public final MomAudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final MomAudioPlayer player)
	{
		musicPlayer = player;
	}

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}

	/**
	 * @return Spell book
	 */
	public final SpellBookNewUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookNewUI ui)
	{
		spellBookUI = ui;
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
package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.process.OverlandMapProcessing;
import momime.client.scheduledcombatmessages.ScheduledCombatMessageProcessing;
import momime.client.ui.dialogs.CombatEndedUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.ScheduledCombatsUI;
import momime.client.ui.frames.SpellBookUI;
import momime.client.utils.AnimationController;
import momime.common.messages.MomScheduledCombat;
import momime.common.messages.servertoclient.CombatEndedMessage;
import momime.common.utils.ScheduledCombatUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;

/**
 * Server sends this to client to say who won a combat, and whether a city was captured or razed as a result
 */
public final class CombatEndedMessageImpl extends CombatEndedMessage implements CustomDurationServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChosenWizardMessageImpl.class);
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Combat UI */
	private CombatUI combatUI;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Music player */
	private AudioPlayer musicPlayer;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/** Scheduled combat utils */
	private ScheduledCombatUtils scheduledCombatUtils; 
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Scheduled combats list */
	private ScheduledCombatsUI scheduledCombatsUI;
	
	/** Scheduled combat message processing */
	private ScheduledCombatMessageProcessing scheduledCombatMessageProcessing;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getCombatLocation ());
		
		final CombatEndedUI ui = getPrototypeFrameCreator ().createCombatEnded ();
		ui.setMessage (this);
		ui.setVisible (true);
		
		log.trace ("Exiting start: " + getCombatLocation ());
	}

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void finish () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering finish: " + getCombatLocation ());
		
		// Close down the UI
		getCombatUI ().setVisible (false);
		
		// Switch spell book to showing overland spells
		getSpellBookUI ().languageOrPageChanged ();
		
		// Remove scheduled combat
		if (getScheduledCombatURN () != null)
		{
			final MomScheduledCombat combat = getScheduledCombatUtils ().findScheduledCombatURN (getClient ().getOurTransientPlayerPrivateKnowledge ().getScheduledCombat (),
				getScheduledCombatURN (), "CombatEndedMessageImpl");
			getClient ().getOurTransientPlayerPrivateKnowledge ().getScheduledCombat ().remove (combat);
			
			getScheduledCombatsUI ().setCombatMessages (getScheduledCombatMessageProcessing ().sortAndAddCategories ());
			getScheduledCombatsUI ().setVisible (true);
		}
		
		// Go back to the overland music
		try
		{
			getMusicPlayer ().setShuffle (true);
			getMusicPlayer ().playPlayList (GraphicsDatabaseConstants.PLAY_LIST_OVERLAND_MUSIC);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		// Prompt for next overland movement
		getOverlandMapProcessing ().selectNextUnitToMoveOverland ();
		
		log.trace ("Exiting finish: " + getCombatLocation ());
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
	public final AudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final AudioPlayer player)
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
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
	}

	/**
	 * @return Scheduled combat utils
	 */
	public final ScheduledCombatUtils getScheduledCombatUtils ()
	{
		return scheduledCombatUtils;
	}

	/**
	 * @param utils Scheduled combat utils
	 */
	public final void setScheduledCombatUtils (final ScheduledCombatUtils utils)
	{
		scheduledCombatUtils = utils;
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
}
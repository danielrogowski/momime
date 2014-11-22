package momime.client.messages.process;

import java.awt.Color;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.ui.frames.CombatUI;
import momime.common.database.Spell;
import momime.common.messages.servertoclient.AddCombatAreaEffectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

/**
 * Server sends this to notify clients of new CAEs, or those that have newly come into view.
 * Besides the info we remember, the client also needs the spell ID for animation purposes
 */
public final class AddCombatAreaEffectMessageImpl extends AddCombatAreaEffectMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AddCombatAreaEffectMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Whether adding this CAE is showing an animation or not */
	private boolean animated;
	
	/** Magic realm colour to flash the screen */
	private Color flashColour;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getMemoryCombatAreaEffect ().getMapLocation () + ", " + getMemoryCombatAreaEffect ().getCombatAreaEffectID ());

		// First add the CAE
		getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect ().add (getMemoryCombatAreaEffect ());
		
		// Check out whether we need an animation
		animated = (getMemoryCombatAreaEffect ().getCastingPlayerID () != null) && (getCombatUI ().isVisible ()) &&
			(getCombatUI ().getCombatLocation ().equals (getMemoryCombatAreaEffect ().getMapLocation ()));
		
		// Look up what colour this spell should flash
		flashColour = Color.WHITE;
		if (animated)
		{
			final Spell spell = getClient ().getClientDB ().findSpell (getSpellID (), "AddCombatAreaEffectMessageImpl");
			if (spell.getSpellRealm () != null)
			{
				// Now look up the magic realm in the graphics XML file
				final Pick magicRealm = getGraphicsDB ().findPick (spell.getSpellRealm (), "AddCombatAreaEffectMessageImpl");
				flashColour = new Color (Integer.parseInt (magicRealm.getPickBookshelfTitleColour (), 16));
			}
		}
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return animated ? 1 : 0;
	}
	
	/**
	 * @return Number of ticks that the duration is divided into
	 */
	@Override
	public final int getTickCount ()
	{
		return animated ? 30 : 0;
	}
	
	/**
	 * @param tickNumber How many ticks have occurred, from 1..tickCount
	 */
	@Override
	public final void tick (final int tickNumber)
	{
		// Work out value between 0..1 for how much we are flashed up or down
		final double v;
		if (tickNumber < getTickCount () / 2)
			v = ((double) tickNumber) / (getTickCount () / 2);
		else
			v = ((double) (getTickCount () - tickNumber)) / (getTickCount () / 2);
		
		// Work out colour
		getCombatUI ().setFlashColour (new Color (flashColour.getRed (), flashColour.getGreen (), flashColour.getBlue (), (int) (v * 200)));
	}
	
	/**
	 * @return True, because the anim ends automatically when the flash completes
	 */
	@Override
	public final boolean isFinishAfterDuration ()
	{
		return true;
	}
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void finish () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering finish");
		
		// If there's a combat in progress, the icon for this CAE might need to be added to it
		if (getCombatUI ().isVisible ())
			getCombatUI ().generateCombatAreaEffectIcons ();
		
		// Make sure the combat screen isn't showing any colour
		getCombatUI ().setFlashColour (CombatUI.NO_FLASH_COLOUR);

		log.trace ("Exiting finish");
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
package momime.client.messages.process;

import java.awt.Color;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.database.Pick;
import momime.common.database.Spell;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.servertoclient.AddOrUpdateCombatAreaEffectMessage;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to notify clients of new CAEs, or those that have newly come into view.
 * Besides the info we remember, the client also needs the spell ID for animation purposes
 */
public final class AddOrUpdateCombatAreaEffectMessageImpl extends AddOrUpdateCombatAreaEffectMessage implements AnimatedServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Unit utils */
	private UnitUtils unitUtils;

	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
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
		// First add or update the CAE
		final MemoryCombatAreaEffect oldCAE = getMemoryCombatAreaEffectUtils ().findCombatAreaEffectURN (getMemoryCombatAreaEffect ().getCombatAreaEffectURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect ());
		if (oldCAE != null)
		{
			oldCAE.setCastingCost (getMemoryCombatAreaEffect ().getCastingCost ());
			oldCAE.setCastingPlayerID (getMemoryCombatAreaEffect ().getCastingPlayerID ());
		}
		else
		{
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect ().add (getMemoryCombatAreaEffect ());

			// Check out whether we need an animation
			animated = (getMemoryCombatAreaEffect ().getCastingPlayerID () != null) && (getCombatUI ().isVisible ()) &&
				(getCombatUI ().getCombatLocation ().equals (getMemoryCombatAreaEffect ().getMapLocation ()));
		}
		
		// Look up what colour this spell should flash
		flashColour = Color.WHITE;
		if (animated)
		{
			final Spell spell = getClient ().getClientDB ().findSpell (getSpellID (), "AddOrUpdateCombatAreaEffectMessageImpl");
			if (spell.getSpellRealm () != null)
			{
				// Now look up the magic realm in the graphics XML file
				final Pick magicRealm = getClient ().getClientDB ().findPick (spell.getSpellRealm (), "AddOrUpdateCombatAreaEffectMessageImpl");
				flashColour = new Color (Integer.parseInt (magicRealm.getPickBookshelfTitleColour (), 16));
			}
		}
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
		// If there's a combat in progress, the icon for this CAE might need to be added to it
		if (getCombatUI ().isVisible ())
			getCombatUI ().generateCombatAreaEffectIcons ();
		
		// Make sure the combat screen isn't showing any colour
		getCombatUI ().setFlashColour (CombatUI.NO_FLASH_COLOUR);
		
		// If any open unit info screen are affected by this CAE, then redraw their attributes
		for (final UnitInfoUI unitInfo : getClient ().getUnitInfos ().values ())
			if (getUnitUtils ().doesCombatAreaEffectApplyToUnit (unitInfo.getUnit (), getMemoryCombatAreaEffect (), getClient ().getClientDB ()))
				unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}
}
package momime.client.messages.process;

import java.awt.Color;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.ui.dialogs.OverlandEnchantmentsUI;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.CombatUICastAnimation;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.common.database.AnimationEx;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.Pick;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.TileSetEx;
import momime.common.messages.servertoclient.ShowSpellAnimationMessage;
import momime.common.utils.UnitUtils;

/**
 * Tells the client to display a spell animation.  There are no other side effects, so whatever
 * damage or updates to the game world take place as a result of the spell must be sent separately.
 */
public final class ShowSpellAnimationMessageImpl extends ShowSpellAnimationMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ShowSpellAnimationMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Combat UI */
	private CombatUI combatUI;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Sound effects player */
	private AudioPlayer soundPlayer;

	/** Bitmap generator includes routines for calculating pixel coords */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Animation to display; null to just flash a colour or process message instantly, or if animation is being handled by another frame */
	private AnimationEx anim;
	
	/** Magic realm colour to flash the screen; null if have an actual animation to display or to process message instantly */
	private Color flashColour;
	
	/** True for overland enchantments */
	private boolean animatedByOtherFrame;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		final Spell spell = getClient ().getClientDB ().findSpell (getSpellID (), "ShowSpellAnimationMessageImpl");
		
		anim = null;
		flashColour = null;
		animatedByOtherFrame = false;
		
		if (isCastInCombat ())
		{
			if ((spell.getCombatCastAnimation () != null) && ((getCombatTargetUnitURN () != null) || getCombatTargetLocation () != null))
			{
				// Figure out the location to display it
				final MapCoordinates2DEx targetPosition = (MapCoordinates2DEx) ((getCombatTargetLocation () != null) ? getCombatTargetLocation () :
					getUnitUtils ().findUnitURN (getCombatTargetUnitURN (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "ShowSpellAnimationMessageImpl").getCombatPosition ());
	
				anim = getClient ().getClientDB ().findAnimation (spell.getCombatCastAnimation (), "ShowSpellAnimationMessageImpl");
	
				// Show anim on CombatUI
				final TileSetEx combatMapTileSet = getClient ().getClientDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "ShowSpellAnimationMessageImpl");
				
				final int adjustX = (anim.getCombatCastOffsetX () == null) ? 0 : 2 * anim.getCombatCastOffsetX ();
				final int adjustY = (anim.getCombatCastOffsetY () == null) ? 0 : 2 * anim.getCombatCastOffsetY ();

				final CombatUICastAnimation castAnim = new CombatUICastAnimation ();
				castAnim.setAnim (anim);
				castAnim.setFrameCount (anim.getFrame ().size ());
				castAnim.setInFront (true);
				castAnim.setPositionX (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX (targetPosition.getX (), targetPosition.getY (), combatMapTileSet));
				castAnim.setPositionY (adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY (targetPosition.getX (), targetPosition.getY (), combatMapTileSet));
				
				getCombatUI ().getCombatCastAnimations ().add (castAnim);
			}
			
			// Disenchant Area / True flash the screen white/blue just like adding a CAE
			// Maybe need to loosen these conditions if we need to use this for other spells later on
			else if ((spell.getAttackSpellCombatTarget () == AttackSpellTargetID.ALL_UNITS) && (spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS))
			{
				if (spell.getSpellRealm () != null)
				{
					// Now look up the magic realm in the graphics XML file
					final Pick magicRealm = getClient ().getClientDB ().findPick (spell.getSpellRealm (), "ShowSpellAnimationMessageImpl");
					flashColour = new Color (Integer.parseInt (magicRealm.getPickBookshelfTitleColour (), 16));
				}
				else
					flashColour = Color.WHITE;
			}
		}
		else
		{
			// Special global attack spells that animate like overland enchantments
			animatedByOtherFrame = true;
			
			final OverlandEnchantmentsUI overlandEnchantmentsPopup = getPrototypeFrameCreator ().createOverlandEnchantments ();
			overlandEnchantmentsPopup.setShowSpellAnimationMessage (this);
			overlandEnchantmentsPopup.setVisible (true);
		}

		// See if there's a sound effect defined in the graphics XML file
		if (spell.getSpellSoundFile () != null)
			try
			{
				getSoundPlayer ().playAudioFile (spell.getSpellSoundFile ());
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
	}

	/**
	 * @return Number of ticks that the duration is divided into
	 */
	@Override
	public final int getTickCount ()
	{
		final int tickCount;
		if (anim != null)
			tickCount = anim.getFrame ().size ();
		else if (flashColour != null)
			tickCount = 30;
		else
			tickCount = 0;
		
		return tickCount;
	}
	
	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		final double duration;
		if (anim != null)
			duration = anim.getFrame ().size () / anim.getAnimationSpeed ();
		else if (flashColour != null)
			duration = 1;
		else
			duration = 0;
		
		return duration;
	}
	
	/**
	 * @param tickNumber How many ticks have occurred, from 1..tickCount
	 */
	@Override
	public final void tick (final int tickNumber)
	{
		if (isCastInCombat ())
		{
			if (anim != null)
			{
				for (final CombatUICastAnimation castAnim : getCombatUI ().getCombatCastAnimations ())
					castAnim.setFrameNumber (tickNumber - 1);
			}
			else if (flashColour != null)
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
		}
	}
	
	/**
	 * @return False for city anims and overland enchantments which have to be clicked on to close their window 
	 */
	@Override
	public final boolean isFinishAfterDuration ()
	{
		return !animatedByOtherFrame;
	}
	
	/**
	 * Clean up the animation when it completes
	 */
	@Override
	public final void finish ()
	{
		if (isCastInCombat ())
		{
			// Remove the anim
			if (anim != null)
				getCombatUI ().getCombatCastAnimations ().clear ();
			
			// Make sure the combat screen isn't showing any colour
			else if (flashColour != null)
				getCombatUI ().setFlashColour (CombatUI.NO_FLASH_COLOUR);
		}
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
	 * @return Sound effects player
	 */
	public final AudioPlayer getSoundPlayer ()
	{
		return soundPlayer;
	}

	/**
	 * @param player Sound effects player
	 */
	public final void setSoundPlayer (final AudioPlayer player)
	{
		soundPlayer = player;
	}

	/**
	 * @return Bitmap generator includes routines for calculating pixel coords
	 */
	public final CombatMapBitmapGenerator getCombatMapBitmapGenerator ()
	{
		return combatMapBitmapGenerator;
	}

	/**
	 * @param gen Bitmap generator includes routines for calculating pixel coords
	 */
	public final void setCombatMapBitmapGenerator (final CombatMapBitmapGenerator gen)
	{
		combatMapBitmapGenerator = gen;
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
}
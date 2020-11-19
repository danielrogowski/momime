package momime.client.messages.process;

import java.awt.Point;
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
import momime.client.ui.frames.CombatUI;
import momime.common.database.AnimationGfx;
import momime.common.database.Spell;
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
	private static final Log log = LogFactory.getLog (ShowSpellAnimationMessageImpl.class);

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

	/** Animation to display; null to process message instantly, or if animation is being handled by another frame */
	private AnimationGfx anim;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getSpellID ());
		
		final Spell spell = getClient ().getClientDB ().findSpell (getSpellID (), "ShowSpellAnimationMessageImpl");
		
		anim = null;
		if ((spell.getCombatCastAnimation () != null) && (isCastInCombat ()) && ((getCombatTargetUnitURN () != null) || getCombatTargetLocation () != null))
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
			
			getCombatUI ().getCombatCastAnimationPositions ().add (new Point
				(adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX (targetPosition.getX (), targetPosition.getY (), combatMapTileSet),
				adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY (targetPosition.getX (), targetPosition.getY (), combatMapTileSet)));

			getCombatUI ().setCombatCastAnimationFrame (0);
			getCombatUI ().setCombatCastAnimation (anim);
			getCombatUI ().setCombatCastAnimationInFront (true);
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
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Number of ticks that the duration is divided into
	 */
	@Override
	public final int getTickCount ()
	{
		return (anim == null) ? 0 : anim.getFrame ().size ();
	}
	
	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return (anim == null) ? 0 : (anim.getFrame ().size () / anim.getAnimationSpeed ());
	}
	
	/**
	 * @param tickNumber How many ticks have occurred, from 1..tickCount
	 */
	@Override
	public final void tick (final int tickNumber)
	{
		if (isCastInCombat ())
			getCombatUI ().setCombatCastAnimationFrame (tickNumber - 1);
	}
	
	/**
	 * @return True to finish the message as soon as the animation finishes 
	 */
	@Override
	public final boolean isFinishAfterDuration ()
	{
		return true;
	}
	
	/**
	 * Clean up the animation when it completes
	 */
	@Override
	public final void finish ()
	{
		// Remove the anim
		if (anim != null)
		{
			if (isCastInCombat ())
			{
				getCombatUI ().setCombatCastAnimation (null);
				getCombatUI ().getCombatCastAnimationPositions ().clear ();
			}
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
}
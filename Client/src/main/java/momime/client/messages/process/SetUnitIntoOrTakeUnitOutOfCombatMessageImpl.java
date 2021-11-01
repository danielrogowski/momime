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
import momime.client.process.CombatMapProcessing;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.common.database.AnimationEx;
import momime.common.database.Spell;
import momime.common.database.TileSetEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;

/**
 * Server sends this to client when a combat is over to take those units out of combat.
 * For taking units out of combat, all the values will be omitted except for the unitURN.
 */
public final class SetUnitIntoOrTakeUnitOutOfCombatMessageImpl extends SetUnitIntoOrTakeUnitOutOfCombatMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SetUnitIntoOrTakeUnitOutOfCombatMessageImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Bitmap generator includes routines for calculating pixel coords */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;

	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Sound effects player */
	private AudioPlayer soundPlayer;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** The unit being updated */
	private MemoryUnit unit;
	
	/** Animation to display, or null to process message instantly */
	private AnimationEx anim;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		unit = getUnitUtils ().findUnitURN (getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
		
		// See if there is an animation defined in the graphics XML file
		anim = null;
		if (getSummonedBySpellID () != null)
		{
			final Spell spell = getClient ().getClientDB ().findSpell (getSummonedBySpellID (), "SetUnitIntoOrTakeUnitOutOfCombatMessageImpl");
			if (spell.getCombatCastAnimation () != null)
			{
				final MapCoordinates2DEx animPosition = (MapCoordinates2DEx) ((getCombatPosition () != null) ? getCombatPosition () : unit.getCombatPosition ());

				anim = getClient ().getClientDB ().findAnimation (spell.getCombatCastAnimation (), "SetUnitIntoOrTakeUnitOutOfCombatMessageImpl");
				
				final TileSetEx combatMapTileSet = getClient ().getClientDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "SetUnitIntoOrTakeUnitOutOfCombatMessageImpl");
				
				final int adjustX = (anim.getCombatCastOffsetX () == null) ? 0 : 2 * anim.getCombatCastOffsetX ();
				final int adjustY = (anim.getCombatCastOffsetY () == null) ? 0 : 2 * anim.getCombatCastOffsetY ();
				
				getCombatUI ().getCombatCastAnimationPositions ().add (new Point (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX
					(animPosition.getX (), animPosition.getY (), combatMapTileSet),
				adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY
					(animPosition.getX (), animPosition.getY (), combatMapTileSet)));

				getCombatUI ().setCombatCastAnimationFrame (0);
				getCombatUI ().setCombatCastAnimation (anim);
				
				// Summoning circle anim appears behind; recall hero anim appears in front
				getCombatUI ().setCombatCastAnimationInFront (getCombatPosition () == null);
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
		getCombatUI ().setCombatCastAnimationFrame (tickNumber - 1);
	}
	
	/**
	 * @return True, because the animation ends as soon as we finish displaying it
	 */
	@Override
	public final boolean isFinishAfterDuration ()
	{
		return true;
	}

	/**
	 * Remove the animation when it completes
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void finish () throws JAXBException, XMLStreamException, IOException
	{
		// Remove the anim
		if (anim != null)
		{
			getCombatUI ().setCombatCastAnimation (null);
			getCombatUI ().getCombatCastAnimationPositions ().clear ();
		}
		
		if (getCombatPosition () == null)
		{
			// Stop drawing the unit (possible the unit was never in combat in the first place, if we are capturing an empty city)
			if (unit.getCombatPosition () != null)
			{
				getCombatUI ().setUnitToDrawAtLocation (unit.getCombatPosition ().getX (), unit.getCombatPosition ().getY (), null);
				getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (unit);
				getCombatMapProcessing ().selectNextUnitToMoveCombat ();
			}
		}
		else
		{
			// Show the unit
			final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (unit, null, null, null,
				getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
			
			getCombatUI ().setUnitToDrawAtLocation (getCombatPosition ().getX (), getCombatPosition ().getY (), xu);
	
			// Give it movement this turn
			unit.setDoubleCombatMovesLeft (2 * xu.getMovementSpeed ());
		}

		// Finally just update the values
		unit.setCombatPosition (getCombatPosition ());
		unit.setCombatLocation (getCombatLocation ());
		unit.setCombatHeading (getCombatHeading ());
		unit.setCombatSide (getCombatSide ());

		// Prompt for it to move
		if (getCombatPosition () != null)
			getCombatMapProcessing ().moveToFrontOfList (unit);
		
		// Update any unit info screen that may be open
		UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (unit.getUnitURN ());
		if (unitInfo != null)
			unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
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
	 * @return Combat map processing
	 */
	public final CombatMapProcessing getCombatMapProcessing ()
	{
		return combatMapProcessing;
	}

	/**
	 * @param proc Combat map processing
	 */
	public final void setCombatMapProcessing (final CombatMapProcessing proc)
	{
		combatMapProcessing = proc;
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}
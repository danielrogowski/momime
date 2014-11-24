package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.v0_9_5.Spell;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.frames.CombatUI;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

/**
 * Server sends this to client when a combat is over to take those units out of combat.
 * For taking units out of combat, all the values will be omitted except for the unitURN.
 */
public final class SetUnitIntoOrTakeUnitOutOfCombatMessageImpl extends SetUnitIntoOrTakeUnitOutOfCombatMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SetUnitIntoOrTakeUnitOutOfCombatMessageImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Bitmap generator includes routines for calculating pixel coords */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;

	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Sound effects player */
	private AudioPlayer soundPlayer;

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
		log.trace ("Entering start: " + getUnitURN () + ", " + getCombatLocation () + ", " + getCombatLocation () + ", " + getCombatSide () + ", " + getSummonedBySpellID ()); 

		// First just add it
		unit = getUnitUtils ().findUnitURN (getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
		unit.setCombatPosition (getCombatPosition ());
		unit.setCombatLocation (getCombatLocation ());
		unit.setCombatHeading (getCombatHeading ());
		unit.setCombatSide (getCombatSide ());
		
		// See if there is an animation defined in the graphics XML file
		anim = null;
		if (getSummonedBySpellID () != null)
		{
			final Spell spell = getGraphicsDB ().findSpell (getSummonedBySpellID (), "SetUnitIntoOrTakeUnitOutOfCombatMessageImpl");
			if (spell.getCombatCastAnimation () != null)
			{
				anim = getGraphicsDB ().findAnimation (spell.getCombatCastAnimation (), "SetUnitIntoOrTakeUnitOutOfCombatMessageImpl");
				
				final TileSetEx combatMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_COMBAT_MAP, "SetUnitIntoOrTakeUnitOutOfCombatMessageImpl");
				
				final int adjustX = (spell.getCombatCastOffsetX () == null) ? 0 : 2 * spell.getCombatCastOffsetX ();
				final int adjustY = (spell.getCombatCastOffsetY () == null) ? 0 : 2 * spell.getCombatCastOffsetY ();
				
				getCombatUI ().setCombatCastAnimationX (adjustX + getCombatMapBitmapGenerator ().combatCoordinatesX
					(getCombatPosition ().getX (), getCombatPosition ().getY (), combatMapTileSet));
				getCombatUI ().setCombatCastAnimationY (adjustY + getCombatMapBitmapGenerator ().combatCoordinatesY
					(getCombatPosition ().getX (), getCombatPosition ().getY (), combatMapTileSet));

				getCombatUI ().setCombatCastAnimationFrame (0);
				getCombatUI ().setCombatCastAnimation (anim);
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
		log.trace ("Entering finish");

		if (getCombatPosition () != null)
		{
			// Remove the anim
			getCombatUI ().setCombatCastAnimation (null);
		
			// Show the unit
			getCombatUI ().getUnitToDrawAtEachLocation () [getCombatPosition ().getY ()] [getCombatPosition ().getX ()] = unit;
	
			// Give it movement this turn
			unit.setDoubleCombatMovesLeft (getClient ().getClientDB ().findUnit (unit.getUnitID (), "SetUnitIntoOrTakeUnitOutOfCombatMessageImpl").getDoubleMovement ());
			
			// Prompt for it to move
			getCombatMapProcessing ().moveToFrontOfList (unit);
		}
		
		log.trace ("Exiting finish");
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
}
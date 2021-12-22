package momime.client.messages.process;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;
import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.frames.CombatUI;
import momime.client.utils.UnitClientUtils;
import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AnimationEx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.TileSetEx;
import momime.common.database.UnitSkillEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.MoveUnitInCombatMessage;
import momime.common.messages.servertoclient.MoveUnitInCombatReason;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;

/**
 * Server breaks down client move requests into a series of directions and sends them back to the client
 */
public final class MoveUnitInCombatMessageImpl extends MoveUnitInCombatMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MoveUnitStackOverlandMessageImpl.class);

	/** Time, in seconds, a unit takes to walk from tile to tile in combat */
	private final static double COMBAT_WALK_TIMING = 1;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Bitmap generator for the static terrain */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;
	
	/** Sound effects player */
	private AudioPlayer soundPlayer;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Work the duration out once only */
	private double duration;

	/** Number of animation ticks */
	private int tickCount;
	
	/** The unit that is moving */
	private ExpandedUnitDetails unit;
	
	/** The location (in combat map cells) that we're moving to */
	private MapCoordinates2DEx moveTo;
	
	/** Need to know size of combat map tiles to figure out spacing of the move */
	private TileSetEx combatMapTileSet;
	
	/** Current position of this unit on the combat map, in pixels */
	private int currentX;
	
	/** Current position of this unit on the combat map, in pixels */
	private int currentY;
	
	/** Current base zOrder of this unit on the combat map */
	private int currentZOrder;
	
	/** Overlays to draw on top of the unit that's moving */
	private List<BufferedImage> overlays;
	
	/** Animations to draw on top of the unit that's moving */
	private List<AnimationEx> animations;
	
	/** List of shading colours to apply to the image */
	private List<String> shadingColours;
	
	/** How much "dug into the ground" the unit should appear; null/0 means draw normally, 1 will draw nothing at all */
	private Double mergingRatio;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		// Find the unit that's moving
		final MemoryUnit mu = getUnitUtils ().findUnitURN (getUnitURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "MoveUnitInCombatMessageImpl.start");
		
		if (!getMoveFrom ().equals (mu.getCombatPosition ()))
			log.warn ("MoveUnitInCombatMessageImpl is trying to move Unit URN " + getUnitURN () + " but its previous location stated in the message (" + getMoveFrom () +
				") isn't what we expected (" + mu.getCombatPosition () + ")");
		
		unit = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
			getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
		
		// See if we need to draw any overlays or animations that move with the unit, e.g. Web or Confusion
		overlays = new ArrayList<BufferedImage> ();
		animations = new ArrayList<AnimationEx> ();
		shadingColours = new ArrayList<String> ();
		
		for (final String unitSkillID : unit.listModifiedSkillIDs ())
		{
			final UnitSkillEx unitSkillDef = getClient ().getClientDB ().findUnitSkill (unitSkillID, "MoveUnitInCombatMessageImpl.start");
			if ((unitSkillDef.getUnitSkillCombatOverlay () != null) || (unitSkillDef.getUnitSkillCombatAnimation () != null))
			{
				// Do we need a certain skill value to draw this?
				final boolean drawOverlay;
				if (unitSkillDef.getUnitSkillCombatOverlayMinimumValue () == null)
					drawOverlay = true;
				else
				{
					final Integer testSkillValue = unit.getModifiedSkillValue (unitSkillID);
					if (testSkillValue == null)
						drawOverlay = false;
					else
						drawOverlay = (testSkillValue >= unitSkillDef.getUnitSkillCombatOverlayMinimumValue ());
				}
				
				if (drawOverlay)
				{
					if (unitSkillDef.getUnitSkillCombatOverlay () != null)
						overlays.add (getUtils ().loadImage (unitSkillDef.getUnitSkillCombatOverlay ()));

					if (unitSkillDef.getUnitSkillCombatAnimation () != null)
						animations.add (getClient ().getClientDB ().findAnimation (unitSkillDef.getUnitSkillCombatAnimation (), "MoveUnitInCombatMessageImpl.start"));
				}
			}
			
			if (unitSkillDef.getUnitSkillCombatColour () != null)
				shadingColours.add (unitSkillDef.getUnitSkillCombatColour ());
		}
		
		// Remove the unit from the map cell it is leaving so the regular drawing routine stops drawing this unit
		getCombatUI ().clearUnitToDrawFromLocation (mu.getCombatPosition ().getX (), mu.getCombatPosition ().getY (), mu.getUnitID ());
		
		// Don't draw unit moving if we can't see it
		if (getUnitUtils ().canSeeUnitInCombat (unit, getClient ().getOurPlayerID (), getClient ().getPlayers (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB (),
			getClient ().getSessionDescription ().getCombatMapSize ()))
			
			getCombatUI ().setUnitMoving (this);

		// Can't blank this out until check above is done
		mu.setCombatPosition (null);
		
		// We need this repeatedly so just work it out once
		combatMapTileSet = getClient ().getClientDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "MoveUnitInCombatMessageImpl.start");
		tickCount = Math.max (combatMapTileSet.getTileWidth (), combatMapTileSet.getTileHeight ()) * 2;
		
		currentX = getCombatMapBitmapGenerator ().combatCoordinatesX (getMoveFrom ().getX (), getMoveFrom ().getY (), combatMapTileSet);
		currentY = getCombatMapBitmapGenerator ().combatCoordinatesY (getMoveFrom ().getX (), getMoveFrom ().getY (), combatMapTileSet);
		currentZOrder = getMoveFrom ().getY ();
		
		// Work the duration out once only
		duration = COMBAT_WALK_TIMING * 0.8d;

		// Work out new position
		if (getDirection () != null)
		{
			moveTo = new MapCoordinates2DEx ((MapCoordinates2DEx) getMoveFrom ());
			getCoordinateSystemUtils ().move2DCoordinates (getClient ().getSessionDescription ().getCombatMapSize (), moveTo, getDirection ());
		}
		else if (getTeleportTo () != null)
		{
			moveTo = (MapCoordinates2DEx) getTeleportTo ();
			
			// Is it teleporting (Unicorns) or merging (Great Wyrm) - need to control the animation differently in either case
			if (unit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_MERGING))
				setMergingRatio (0d);
			else
				shadingColours.add ("FFFFFFFF");
		}
		else
			throw new MomException ("MoveUnitInCombatMessageImpl: Neither direction nor teleportTo was supplied");
		
		// Kick off animation
		if ((getDirection () != null) || (getMergingRatio () != null))
		{
			if (getDirection () != null)
				mu.setCombatHeading (getDirection ());
		
			final String movingActionID = getUnitCalculations ().determineCombatActionID (unit, true, getClient ().getClientDB ());
			
			// Play walking sound effect
			getUnitClientUtils ().playCombatActionSound (mu, movingActionID);
		}
		else
			try
			{
				// Play teleporting sound effect
				getSoundPlayer ().playAudioFile ("/momime.client.sounds/SOUNDFX_011_000 - Teleport.mp3");
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
	}
	
	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return duration;
	}
	
	/**
	 * @return Number of ticks that the duration is divided into
	 */
	@Override
	public final int getTickCount ()
	{
		return tickCount;
	}
	
	/**
	 * @param tickNumber How many ticks have occurred, from 1..tickCount
	 */
	@Override
	public final void tick (final int tickNumber)
	{
		final double ratio = (double) tickNumber / tickCount;
		
		// Get from/to pixel coordinates
		final int moveFromX = getCombatMapBitmapGenerator ().combatCoordinatesX (getMoveFrom ().getX (), getMoveFrom ().getY (), combatMapTileSet);
		final int moveFromY = getCombatMapBitmapGenerator ().combatCoordinatesY (getMoveFrom ().getX (), getMoveFrom ().getY (), combatMapTileSet);

		final int moveToX = getCombatMapBitmapGenerator ().combatCoordinatesX (moveTo.getX (), moveTo.getY (), combatMapTileSet);
		final int moveToY = getCombatMapBitmapGenerator ().combatCoordinatesY (moveTo.getX (), moveTo.getY (), combatMapTileSet);
		
		// Work out current position
		if (getDirection () != null)
		{
			currentX = moveFromX + (int) ((moveToX - moveFromX) * ratio);
			currentY = moveFromY + (int) ((moveToY - moveFromY) * ratio);
			currentZOrder = (getMoveFrom ().getY () * 50) + (int) ((moveTo.getY () - getMoveFrom ().getY ()) * ratio * 50d);
		}
		else if (tickNumber < (tickCount / 2))
		{
			currentX = moveFromX;
			currentY = moveFromY;
			currentZOrder = moveFromY * 50;
			
			final double alphaRatio = (double) tickNumber / (tickCount / 2);
			if (getMergingRatio () == null)
			{
				// Make Unicorns fade out
				final int alpha = (int) ((1d - alphaRatio) * 255);
				String alphaString = Integer.toHexString (alpha).toUpperCase ();
				while (alphaString.length () < 2)
					alphaString = "0" + alphaString;
				
				shadingColours.remove (shadingColours.size () - 1);
				shadingColours.add (alphaString + "FFFFFF");
			}
			else
			{
				// Make Great Wyrm sink down
				setMergingRatio (alphaRatio);
			}
		}
		else
		{
			currentX = moveToX;
			currentY = moveToY;
			currentZOrder = moveToY * 50;

			final double alphaRatio = (double) (tickNumber - (tickCount / 2)) / (tickCount / 2);
			if (getMergingRatio () == null)
			{
				// Make Unicorns fade in
				final int alpha = (int) (alphaRatio * 255);
				String alphaString = Integer.toHexString (alpha).toUpperCase ();
				while (alphaString.length () < 2)
					alphaString = "0" + alphaString;
				
				shadingColours.remove (shadingColours.size () - 1);
				shadingColours.add (alphaString + "FFFFFF");
			}
			else
			{
				// Make Great Wyrm rise up
				setMergingRatio (1d - alphaRatio);
			}
		}
	}
	
	/**
	 * @return True, because the animation auto completes as soon as the unit reaches the destination cell
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
		// Set the unit into the new map cell so the regular drawing routine takes over drawing this unit again
		unit.setCombatPosition (moveTo);
		getCombatUI ().setUnitToDrawAtLocation (moveTo.getX (), moveTo.getY (), unit);
		getCombatUI ().setUnitMoving (null);
		
		// Update remaining movement
		unit.setDoubleCombatMovesLeft (getDoubleCombatMovesLeft ());
		
		// Jump to the next unit to move, unless we're a unit who still has some movement left.
		// This routine will then ignore the request if we're not the current player.
		if (unit.getDoubleCombatMovesLeft () <= 0)
			getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (unit.getMemoryUnit ());
		
		if (getReason () == MoveUnitInCombatReason.MANUAL)
			getCombatMapProcessing ().selectNextUnitToMoveCombat ();
	}
	
	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}
	
	/**
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
	}

	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param u Unit utils
	 */
	public final void setUnitUtils (final UnitUtils u)
	{
		unitUtils = u;
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
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param csu Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils csu)
	{
		coordinateSystemUtils = csu;
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
	 * @return Bitmap generator for the static terrain
	 */
	public final CombatMapBitmapGenerator getCombatMapBitmapGenerator ()
	{
		return combatMapBitmapGenerator;
	}

	/**
	 * @param gen Bitmap generator for the static terrain
	 */
	public final void setCombatMapBitmapGenerator (final CombatMapBitmapGenerator gen)
	{
		combatMapBitmapGenerator = gen;
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
	
	/**
	 * @return The unit that is moving
	 */
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return Current position of this unit on the combat map, in pixels
	 */
	public final int getCurrentX ()
	{
		return currentX;
	}

	/**
	 * @return Current position of this unit on the combat map, in pixels
	 */
	public final int getCurrentY ()
	{
		return currentY;
	}

	/**
	 * @return Current base zOrder of this unit on the combat map
	 */
	public final int getCurrentZOrder ()
	{
		return currentZOrder;
	}

	/**
	 * @return Overlays to draw on top of the unit that's moving
	 */
	public final List<BufferedImage> getOverlays ()
	{
		return overlays;
	}
	
	/**
	 * @return Animations to draw on top of the unit that's moving
	 */
	public final List<AnimationEx> getAnimations ()
	{
		return animations;
	}

	/**
	 * @return List of shading colours to apply to the image
	 */
	public final List<String> getShadingColours ()
	{
		return shadingColours;
	}

	/**
	 * @return How much "dug into the ground" the unit should appear; null/0 means draw normally, 1 will draw nothing at all
	 */
	public final Double getMergingRatio ()
	{
		return mergingRatio;
	}

	/**
	 * @param r How much "dug into the ground" the unit should appear; null/0 means draw normally, 1 will draw nothing at all
	 */
	public final void setMergingRatio (final Double r)
	{
		mergingRatio = r;
	}
}
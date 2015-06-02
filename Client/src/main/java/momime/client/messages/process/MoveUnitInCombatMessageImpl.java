package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetGfx;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.frames.CombatUI;
import momime.client.utils.UnitClientUtils;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.MoveUnitInCombatMessage;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

/**
 * Server breaks down client move requests into a series of directions and sends them back to the client
 */
public final class MoveUnitInCombatMessageImpl extends MoveUnitInCombatMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MoveUnitStackOverlandMessageImpl.class);

	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;

	/** Client unit calculations */
	private ClientUnitCalculations clientUnitCalculations;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;
	
	/** Bitmap generator for the static terrain */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;
	
	/** Work the duration out once only */
	private double duration;

	/** Number of animation ticks */
	private int tickCount;
	
	/** The unit that is moving */
	private MemoryUnit unit;
	
	/** The location (in combat map cells) that we're moving to */
	private MapCoordinates2DEx moveTo;
	
	/** Need to know size of combat map tiles to figure out spacing of the move */
	private TileSetGfx combatMapTileSet;
	
	/** Current position of this unit on the combat map, in pixels */
	private int currentX;
	
	/** Current position of this unit on the combat map, in pixels */
	private int currentY;
	
	/** Current base zOrder of this unit on the combat map */
	private int currentZOrder;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		// Find the unit that's moving
		unit = getUnitUtils ().findUnitURN (getUnitURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "MoveUnitInCombatMessageImpl.start");
		
		if (!getMoveFrom ().equals (unit.getCombatPosition ()))
			log.warn ("MoveUnitInCombatMessageImpl is trying to move Unit URN " + getUnitURN () + " but its previous location stated in the message (" + getMoveFrom () +
				") isn't what we expected (" + unit.getCombatPosition () + ")"); 
		
		// Remove the unit from the map cell it is leaving so the regular drawing routine stops drawing this unit
		getCombatUI ().getUnitToDrawAtEachLocation () [unit.getCombatPosition ().getY ()] [unit.getCombatPosition ().getX ()] = null;
		unit.setCombatPosition (null);
		getCombatUI ().setUnitMoving (this);

		// We need this repeatedly so just work it out once
		combatMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "MoveUnitInCombatMessageImpl.start");
		tickCount = Math.max (combatMapTileSet.getTileWidth (), combatMapTileSet.getTileHeight ()) * 2;
		
		currentX = getCombatMapBitmapGenerator ().combatCoordinatesX (getMoveFrom ().getX (), getMoveFrom ().getY (), combatMapTileSet);
		currentY = getCombatMapBitmapGenerator ().combatCoordinatesY (getMoveFrom ().getX (), getMoveFrom ().getY (), combatMapTileSet);
		currentZOrder = getMoveFrom ().getY ();
		
		// Work the duration out once only
		duration = getUnitClientUtils ().calculateWalkTiming (unit) * 0.8d;

		// Work out new position
		moveTo = new MapCoordinates2DEx ((MapCoordinates2DEx) getMoveFrom ());
		getCoordinateSystemUtils ().move2DCoordinates (getClient ().getSessionDescription ().getCombatMapSize (), moveTo, getDirection ());
		
		// Kick off animation
		unit.setCombatHeading (getDirection ());
		final String movingActionID = getClientUnitCalculations ().determineCombatActionID (unit, true);
		
		// Play walking sound effect
		getUnitClientUtils ().playCombatActionSound (unit, movingActionID);
		
		log.trace ("Exiting start");
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
		currentX = moveFromX + (int) ((moveToX - moveFromX) * ratio);
		currentY = moveFromY + (int) ((moveToY - moveFromY) * ratio);
		currentZOrder = (getMoveFrom ().getY () * 50) + (int) ((moveTo.getY () - getMoveFrom ().getY ()) * ratio * 50d);
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
		log.trace ("Entering finish");
		
		// Set the unit into the new map cell so the regular drawing routine takes over drawing this unit again
		unit.setCombatPosition (moveTo);
		getCombatUI ().getUnitToDrawAtEachLocation () [moveTo.getY ()] [moveTo.getX ()] = unit;
		getCombatUI ().setUnitMoving (null);
		
		// Update remaining movement
		unit.setDoubleCombatMovesLeft (getDoubleCombatMovesLeft ());
		
		// Jump to the next unit to move, unless we're a unit who still has some movement left.
		// This routine will then ignore the request if we're not the current player.
		if (unit.getDoubleCombatMovesLeft () <= 0)
			getCombatMapProcessing ().removeUnitFromLeftToMoveCombat (unit);
		
		getCombatMapProcessing ().selectNextUnitToMoveCombat ();
		
		log.trace ("Exiting finish");
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
	 * @return Client unit calculations
	 */
	public final ClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final ClientUnitCalculations calc)
	{
		clientUnitCalculations = calc;
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
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
	 * @return The unit that is moving
	 */
	public final MemoryUnit getUnit ()
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
}
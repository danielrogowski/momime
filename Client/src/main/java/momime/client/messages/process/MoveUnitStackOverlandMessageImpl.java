package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.utils.UnitClientUtils;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.KillUnitActionID;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

/**
 * Server breaks down client move requests into a series of directions and sends them back to the client
 */
public final class MoveUnitStackOverlandMessageImpl extends MoveUnitStackOverlandMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MoveUnitStackOverlandMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Overland map tile set */
	private TileSetEx overlandMapTileSet;
	
	/** Number of animation ticks */
	private int tickCount;
	
	/** Unit to draw */
	private MemoryUnit unitToDraw;
	
	/** Location to draw unit stack at */
	private int currentX;

	/** Location to draw unit stack at */
	private int currentY;
	
	/** Delta being moved in X direction */
	private int directionX;
	
	/** Delta being moved in Y direction */
	private int directionY;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");

		// Work out the direction of travel
		final int direction = getCoordinateSystemUtils ().determineDirectionTo (getClient ().getSessionDescription ().getMapSize (),
			getMoveFrom ().getX (), getMoveFrom ().getY (), getMoveTo ().getX (), getMoveTo ().getY ());
		
		// Split the direction into X and Y components
		switch (direction)
		{
			case 2:
			case 3:
			case 4:
				directionX = 1;
				break;
				
			case 6:
			case 7:
			case 8:
				directionX = -1;
				break;
			
			default:
				directionX = 0;
		}
		
		switch (direction)
		{
			case 8:
			case 1:
			case 2:
				directionY = -1;
				break;
				
			case 4:
			case 5:
			case 6:
				directionY = 1;
				break;
			
			default:
				directionY = 0;
		}
		
		// Remove the units from the map cell they're leaving
		for (final int thisUnitURN : getUnitURN ())
		{
			final MemoryUnit u = getUnitUtils ().findUnitURN (thisUnitURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "MoveUnitStackOverlandMessageImpl.start");
			u.setUnitLocation (null);
			
			if (unitToDraw == null)
				unitToDraw = u;
		}
		
		// If we've got the city screen open for the map cell the units just left then we need to update it to remove their select unit buttons
		final CityViewUI cityView = getClient ().getCityViews ().get (getMoveFrom ().toString ());
		if (cityView != null)
			cityView.unitsChanged ();
		
		// We need this repeatedly so just work it out once
		overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "MoveUnitStackOverlandMessageImpl.start");
		tickCount = Math.max (overlandMapTileSet.getTileWidth (), overlandMapTileSet.getTileHeight ());
		
		// Start at the starting cell - note it being at the start point means it looks no different, so is why we don't do a repaint yet
		currentX = getMoveFrom ().getX () * overlandMapTileSet.getTileWidth ();
		currentY = getMoveFrom ().getY () * overlandMapTileSet.getTileHeight ();
		
		getOverlandMapUI ().setUnitStackMoving (this);
		
		log.trace ("Exiting start");
	}

	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return 1;
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
		currentX = (int) ((getMoveFrom ().getX () + (directionX * ratio)) * overlandMapTileSet.getTileWidth ());
		currentY = (int) ((getMoveFrom ().getY () + (directionY * ratio)) * overlandMapTileSet.getTileHeight ());
		
		getOverlandMapUI ().repaintSceneryPanel ();
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
		
		if (isFreeAfterMoving ())
			getMemoryMaintainedSpellUtils ().removeSpellsCastOnUnitStack (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), getUnitURN ());
		
		// Put the units into their new map cell
		for (final int thisUnitURN : getUnitURN ())
		{
			final MemoryUnit u = getUnitUtils ().findUnitURN (thisUnitURN, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "MoveUnitStackOverlandMessageImpl.finish");

			// Free after moving is used when an enemy unit is walking out of the area we can see, so we see them move and then they disappear
			if (isFreeAfterMoving ())
				getUnitClientUtils ().killUnit (thisUnitURN, KillUnitActionID.FREE, null);
			else
				u.setUnitLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) getMoveTo ()));
		}
		
		getOverlandMapUI ().setUnitStackMoving (null);
		getOverlandMapUI ().repaintSceneryPanel ();
		
		// If we've got the city screen open for the map cell the units just moved into then we need to update it to add their select unit buttons
		final CityViewUI cityView = getClient ().getCityViews ().get (getMoveTo ().toString ());
		if (cityView != null)
			cityView.unitsChanged ();
		
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
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
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
	 * @return Unit to draw
	 */
	public final MemoryUnit getUnitToDraw ()
	{
		return unitToDraw;
	}
	
	/**
	 * @return Location to draw unit stack at
	 */
	public final int getCurrentX ()
	{
		return currentX;
	}

	/**
	 * @return Location to draw unit stack at
	 */
	public final int getCurrentY ()
	{
		return currentY;
	}
}
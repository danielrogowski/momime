package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.base.client.AnimatedServerToClientMessage;

import momime.client.MomClient;
import momime.client.config.MomImeClientConfigEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetGfx;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.utils.UnitClientUtils;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.UnitUtils;

/**
 * Server breaks down client move requests into a series of directions and sends them back to the client
 */
public final class MoveUnitStackOverlandMessageImpl extends MoveUnitStackOverlandMessage implements AnimatedServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MoveUnitStackOverlandMessageImpl.class);

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
	
	/** Client config, containing the scale setting */
	private MomImeClientConfigEx clientConfig;
	
	/** Overland map tile set */
	private TileSetGfx overlandMapTileSet;
	
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
	
	/** Show as animation or not? */
	private boolean anim;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		// If the move is only 1 cell, show as an animation; if its a further move (like Recall Hero) then just do it instantly
		anim = (getClientConfig ().isOverlandAnimateUnitsMoving ()) &&
			(getCoordinateSystemUtils ().findDistanceBetweenXCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), getMoveFrom ().getX (), getMoveTo ().getX ()) <= 1) &&
			(getCoordinateSystemUtils ().findDistanceBetweenYCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), getMoveFrom ().getY (), getMoveTo ().getY ()) <= 1);

		if (anim)
		{
			// Work out the direction of travel
			final int direction = getCoordinateSystemUtils ().determineDirectionTo (getClient ().getSessionDescription ().getOverlandMapSize (),
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
		
		if (anim)
		{
			// We need this repeatedly so just work it out once
			overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "MoveUnitStackOverlandMessageImpl.start");
			tickCount = Math.max (overlandMapTileSet.getTileWidth (), overlandMapTileSet.getTileHeight ());
			
			// Start at the starting cell - note it being at the start point means it looks no different, so is why we don't do a repaint yet
			currentX = getMoveFrom ().getX () * overlandMapTileSet.getTileWidth ();
			currentY = getMoveFrom ().getY () * overlandMapTileSet.getTileHeight ();
			
			getOverlandMapUI ().setUnitStackMoving (this);
		}
		else
			tickCount = 0;
		
		log.trace ("Exiting start");
	}
	
	/**
	 * @return Plane which the overland map must be viewing to see the unit stack moving
	 */
	public final int getAnimationPlane ()
	{
		return Math.max (getMoveFrom ().getZ (), getMoveTo ().getZ ());
	}

	/**
	 * @return Number of seconds that the animation takes to display
	 */
	@Override
	public final double getDuration ()
	{
		return anim ? 1 : 0;
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
				getUnitClientUtils ().killUnit (u, null);
			else
				u.setUnitLocation (new MapCoordinates3DEx ((MapCoordinates3DEx) getMoveTo ()));
		}
		
		getOverlandMapUI ().setUnitStackMoving (null);
		getOverlandMapUI ().repaintSceneryPanel ();
		
		// If we've got the city screen open for the map cell the units just moved into then we need to update it to add their select unit buttons
		final CityViewUI cityView = getClient ().getCityViews ().get (getMoveTo ().toString ());
		if (cityView != null)
			cityView.unitsChanged ();

		// Update all unit info screens that we have open, because the attributes may be affected as units move around the map
		// in and out of CAEs that affect it, and also if the unit being moved has a skill like Resistance to All or Holy Bonus, then
		// every other unit in the cell it moved from or the cell it moved to all need updating (maybe we could make this more
		// clever and only update units in either the moveFrom or moveTo locations, but just leave it safe / simple for now)
		for (final UnitInfoUI unitInfo : getClient ().getUnitInfos ().values ())
			unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
		
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
	 * @return Client config, containing the scale setting
	 */
	public final MomImeClientConfigEx getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing the scale setting
	 */
	public final void setClientConfig (final MomImeClientConfigEx config)
	{
		clientConfig = config;
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
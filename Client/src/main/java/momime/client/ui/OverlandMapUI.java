package momime.client.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import momime.client.MomClient;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.MapFeatureEx;
import momime.client.graphics.database.SmoothedTileTypeEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.v0_9_5.SmoothedTile;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.MapSizeData;
import momime.common.messages.v0_9_5.MemoryGridCell;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Screen for displaying the overland map, including the buttons and side panels and so on that appear in the same frame
 */
public final class OverlandMapUI
{
	/** Class logger */
	private final Logger log = Logger.getLogger (OverlandMapUI.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private MomUIUtils utils;
	
	/** Smoothed tiles to display at every map cell */
	private SmoothedTile [] [] [] smoothedTiles;
	
	/** Bitmaps for each animation frame of the overland map */
	private BufferedImage [] overlandMapBitmaps;
	
	/** The plane that the UI is currently displaying */
	private int mapViewPlane = 0;
	
	/**
	 * Creates the smoothedTiles array as the correct size
	 */
	public final void afterJoinedSession ()
	{
		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		smoothedTiles = new SmoothedTile [mapSize.getDepth ()] [mapSize.getHeight ()] [mapSize.getWidth ()];
	}
	
	/**
	 * Converts the tile types sent by the server into actual tile numbers, smoothing the edges of various terrain types in the process
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for; or can supply null to resmooth everything
	 * @throws RecordNotFoundException If required entries in the graphics XML cannot be found
	 */
	public final void smoothMapTerrain (final MapArea3D<Boolean> areaToSmooth) throws RecordNotFoundException
	{
		log.entering (OverlandMapUI.class.getName (), "smoothMapTerrain", areaToSmooth);

		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		final int maxDirection = getCoordinateSystemUtils ().getMaxDirection (mapSize.getCoordinateSystemType ());
		
		// Choose the appropriate tile set
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "smoothMapTerrain");
		
		// Now check each map cell
		for (int planeNo = 0; planeNo < mapSize.getDepth (); planeNo++) 
			for (int y = 0; y < mapSize.getHeight (); y++) 
				for (int x = 0; x < mapSize.getWidth (); x++)
					if ((areaToSmooth == null) || ((areaToSmooth.get (x, y, planeNo) != null) && (areaToSmooth.get (x, y, planeNo))))
					{
						final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
							(planeNo).getRow ().get (y).getCell ().get (x);
						
						// Have we ever seen this tile?
						final String tileTypeID = (gc.getTerrainData () == null) ? null : gc.getTerrainData ().getTileTypeID ();
						if (tileTypeID == null)
							smoothedTiles [planeNo] [y] [x] = null;
						else
						{
							final SmoothedTileTypeEx smoothedTileType = overlandMapTileSet.findSmoothedTileType (tileTypeID, mapViewPlane, null);
							
							// If this is ticked then fix the bitmask
							// If a land based tile, want to assume grass in every direction (e.g. for mountains, draw a single mountain), so want 11111111
							
							// But for a sea tile, this looks really daft - you get a 'sea' of lakes surrounded by grass!  So we have to force these to 00000000 instead
							// to make it look remotely sensible
							
							// Rather than hard coding which tile types need 00000000 and which need 11111111, the graphics XML file has a special
							// entry under every tile for the image to use for 'NoSmooth' = No Smoothing
							final StringBuffer bitmask = new StringBuffer ();
							
							// --- Leaving this 'if' out for now since there's no options screen yet via which to turn smoothing off, but I did prove that it works ---
							// bitmask.append (GraphicsDatabaseConstants.VALUE_TILE_BITMASK_NO_SMOOTHING);
							
							{
								// 3 possibilities for how we create the bitmask
								// 0 = force 00000000
								// 1 = use 0 for this type of tile, 1 for anything else (assume grass)
								// 2 = use 0 for this type of tile, 1 for anything else (assume grass), 2 for rivers (in a joining direction)
								final int maxValueInEachDirection = overlandMapTileSet.findSmoothingSystem
									(smoothedTileType.getSmoothingSystemID (), "smoothMapTerrain").getMaxValueEachDirection ();
								
								if (maxValueInEachDirection == 0)
								{
									for (int d = 1; d <= maxDirection; d++)
										bitmask.append ("0");
								}
								
								// If a river tile, decide whether to treat this direction as a river based on the RiverDirections FROM this tile, not by looking at adjoining tiles
								// NB. This is only inland rivers - oceanside river mouths are just special shore/ocean tiles
								else if ((maxValueInEachDirection == 1) && (gc.getTerrainData ().getRiverDirections () != null))
								{
									for (int d = 1; d <= maxDirection; d++)
										if (gc.getTerrainData ().getRiverDirections ().contains (new Integer (d).toString ()))
											bitmask.append ("0");
										else
											bitmask.append ("1");
								}
								
								// Normal type of smoothing
								else
								{
									for (int d = 1; d <= maxDirection; d++)
										
										// Want rivers? i.e. is this an ocean tile
										if ((maxValueInEachDirection == 2) && (gc.getTerrainData ().getRiverDirections () != null) &&
											(gc.getTerrainData ().getRiverDirections ().contains (new Integer (d).toString ())))
											
											bitmask.append ("2");
										else
										{
											final MapCoordinates3DEx coords = new MapCoordinates3DEx ();
											coords.setX (x);
											coords.setY (y);
											coords.setZ (planeNo);
											if (getCoordinateSystemUtils ().move3DCoordinates (mapSize, coords, d))
											{
												final MemoryGridCell otherGc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
													(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
												final String otherTileTypeID = (otherGc.getTerrainData () == null) ? null : otherGc.getTerrainData ().getTileTypeID ();
												
												if ((otherTileTypeID == null) || (otherTileTypeID.equals (tileTypeID)) ||
													(otherTileTypeID.equals (smoothedTileType.getSecondaryTileTypeID ())) ||
													(otherTileTypeID.equals (smoothedTileType.getTertiaryTileTypeID ())))
													
													bitmask.append ("0");
												else
													bitmask.append ("1");
											}
											else
												bitmask.append ("0");
										}
								}
							}

							// The cache works directly on unsmoothed bitmasks so no reduction to do
							smoothedTiles [planeNo] [y] [x] = smoothedTileType.getRandomImage (bitmask.toString ());
						}
					}
		
		log.exiting (OverlandMapUI.class.getName (), "smoothMapTerrain");
	}
	
	/**
	 * Generates big bitmaps of the entire overland map in each frame of animation
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateOverlandMapBitmaps () throws IOException
	{
		log.entering (OverlandMapUI.class.getName (), "regenerateOverlandMapBitmaps", mapViewPlane);

		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		
		// We need the tile set so we know how many animation frames there are
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "regenerateOverlandMapBitmaps");
		
		// Create the set of empty bitmaps
		overlandMapBitmaps = new BufferedImage [overlandMapTileSet.getAnimationFrameCount ()];
		final Graphics2D [] g = new Graphics2D [overlandMapTileSet.getAnimationFrameCount ()];
		for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
		{
			overlandMapBitmaps [frameNo] = new BufferedImage
				(mapSize.getWidth () * overlandMapTileSet.getTileWidth (), mapSize.getHeight () * overlandMapTileSet.getTileHeight (), BufferedImage.TYPE_INT_ARGB);
			
			g [frameNo] = overlandMapBitmaps [frameNo].createGraphics ();
		}
		
		// Run through each tile
		for (int y = 0; y < mapSize.getHeight (); y++) 
			for (int x = 0; x < mapSize.getWidth (); x++)
			{
				// Terrain
				final SmoothedTile tile = smoothedTiles [mapViewPlane] [y] [x];
				if (tile != null)
				{
					if (tile.getTileFile () != null)
					{
						// Use same image for all frames
						final BufferedImage image = getUtils ().loadImage (tile.getTileFile ());
						for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
							g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
					}
					else if (tile.getTileAnimation () != null)
					{
						// Copy each animation frame over to each bitmap
						final AnimationEx anim = getGraphicsDB ().findAnimation (tile.getTileAnimation (), "regenerateOverlandMapBitmaps");
						for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
						{
							final BufferedImage image = getUtils ().loadImage (anim.getFrame ().get (frameNo).getFrameImageFile ());
							g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
						}
					}
				}
				
				// Map feature
				final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(mapViewPlane).getRow ().get (y).getCell ().get (x);
				final String mapFeatureID = (gc.getTerrainData () == null) ? null : gc.getTerrainData ().getMapFeatureID ();
				if (mapFeatureID != null)
				{
					final MapFeatureEx mapFeature = getGraphicsDB ().findMapFeature (mapFeatureID, "regenerateOverlandMapBitmaps");
					final BufferedImage image = getUtils ().loadImage (mapFeature.getOverlandMapImageFile ());

					// Use same image for all frames
					for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
						g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
				}
			}
		
		// Clean up the drawing contexts 
		for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
			g [frameNo].dispose ();
		
		// For debug purposes for now
		for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
			ImageIO.write (overlandMapBitmaps [frameNo], "png", new File ("F:\\MoMIMEClient overland map frame " + frameNo + ".png"));
		
		log.exiting (OverlandMapUI.class.getName (), "regenerateOverlandMapBitmaps"); 
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
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final MomUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final MomUIUtils util)
	{
		utils = util;
	}
}

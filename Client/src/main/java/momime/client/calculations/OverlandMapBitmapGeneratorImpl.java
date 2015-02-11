package momime.client.calculations;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import momime.client.MomClient;
import momime.client.config.MomImeClientConfigEx;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.CityImageGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.MapFeatureGfx;
import momime.client.graphics.database.SmoothedTileGfx;
import momime.client.graphics.database.SmoothedTileTypeGfx;
import momime.client.graphics.database.TileSetGfx;
import momime.client.ui.PlayerColourImageGenerator;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MemoryGridCell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;

/**
 * Generates large bitmap images showing the current state of the entire overland map and fog of war.  This includes terrain (tiles + map features)
 * and cities, but not elements that are dynamically drawn over the top of the map (units).
 * 
 * This is moved out from OverlandMapUI to allow the bitmap generation and the screen layout to be more independantly unit testable.
 */
public final class OverlandMapBitmapGeneratorImpl implements OverlandMapBitmapGenerator
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OverlandMapBitmapGeneratorImpl.class);

	/** $50000000 matches the alpha value on the partial border images */
	private final static Color PARTIAL_FOW_COLOUR = new Color (0, 0, 0, 0x50);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;
	
	/** Client config, containing various overland map settings */
	private MomImeClientConfigEx clientConfig;
	
	/** Smoothed tiles to display at every map cell */
	private SmoothedTileGfx [] [] [] smoothedTiles;

	/**
	 * Creates the smoothedTiles array as the correct size
	 * Can't just do this at the start of init (), because the server sends the first FogOfWarVisibleAreaChanged prior to the overland map being displayed,
	 * so we can prepare the map image before displaying it - so we have to create the area for it to prepare it into
	 */
	@Override
	public final void afterJoinedSession ()
	{
		log.trace ("Entering afterJoinedSession");

		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		smoothedTiles = new SmoothedTileGfx [mapSize.getDepth ()] [mapSize.getHeight ()] [mapSize.getWidth ()];

		log.trace ("Exiting afterJoinedSession");
	}	
	
	/**
	 * Converts the tile types sent by the server into actual tile numbers, smoothing the edges of various terrain types in the process
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for; or can supply null to resmooth everything
	 * @throws RecordNotFoundException If required entries in the graphics XML cannot be found
	 */
	@Override
	public final void smoothMapTerrain (final MapArea3D<Boolean> areaToSmooth) throws RecordNotFoundException
	{
		log.trace ("Entering smoothMapTerrain: " + areaToSmooth);

		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		final int maxDirection = getCoordinateSystemUtils ().getMaxDirection (mapSize.getCoordinateSystemType ());
		
		// Choose the appropriate tile set
		final TileSetGfx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "smoothMapTerrain");
		
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
							final SmoothedTileTypeGfx smoothedTileType = overlandMapTileSet.findSmoothedTileType (tileTypeID, planeNo, null);
							
							// If this is ticked then fix the bitmask
							// If a land based tile, want to assume grass in every direction (e.g. for mountains, draw a single mountain), so want 11111111
							
							// But for a sea tile, this looks really daft - you get a 'sea' of lakes surrounded by grass!  So we have to force these to 00000000 instead
							// to make it look remotely sensible
							
							// Rather than hard coding which tile types need 00000000 and which need 11111111, the graphics XML file has a special
							// entry under every tile for the image to use for 'NoSmooth' = No Smoothing
							final StringBuffer bitmask = new StringBuffer ();
							if (!getClientConfig ().isOverlandSmoothTerrain ())
								bitmask.append (GraphicsDatabaseConstants.TILE_BITMASK_NO_SMOOTHING);
							else							
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
											final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, planeNo);
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
		
		log.trace ("Exiting smoothMapTerrain");
	}
	
	/**
	 * Generates big bitmaps of the entire overland map in each frame of animation.
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView.
	 * 
	 * Generated bitmaps will all be 20x countX by 18x countY pixels in size.
	 * 
	 * @param mapViewPlane Which plane to generate bitmaps for
	 * @param startX Map coordinate of the cell to draw at the left edge of the bitmaps
	 * @param startY Map coordinate of the cell to draw at the top edge of the bitmaps
	 * @param countX Width of the bitmap to generate, in number of map cells
	 * @param countY Height of the bitmap to generate, in number of map cells
	 * @return Array of overland map bitmaps
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Override
	public final BufferedImage [] generateOverlandMapBitmaps (final int mapViewPlane, final int startX, final int startY, final int countX, final int countY) throws IOException
	{
		log.trace ("Entering generateOverlandMapBitmaps: " + mapViewPlane);

		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		
		// We need the tile set so we know how many animation frames there are
		final TileSetGfx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "generateOverlandMapBitmaps");
		
		// Create the set of empty bitmaps
		final BufferedImage [] overlandMapBitmaps = new BufferedImage [overlandMapTileSet.getAnimationFrameCount ()];
		final Graphics2D [] g = new Graphics2D [overlandMapTileSet.getAnimationFrameCount ()];
		for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
		{
			overlandMapBitmaps [frameNo] = new BufferedImage
				(countX * overlandMapTileSet.getTileWidth (), countY * overlandMapTileSet.getTileHeight (), BufferedImage.TYPE_INT_ARGB);
			
			g [frameNo] = overlandMapBitmaps [frameNo].createGraphics ();
		}
		
		// Run through each tile
		final MapCoordinates2DEx mapCoords = new MapCoordinates2DEx (startX, startY);
		for (int x = 0; x < countX; x++)
		{
			mapCoords.setY (startY);
			for (int y = 0; y < countY; y++)
			{
				// If close to a non-wrapping edge (possible to put a city on the top/bottom row of tundra on the map), we may move off the end of the map
				if (getCoordinateSystemUtils ().areCoordinatesWithinRange (mapSize, mapCoords))
				{
					// Terrain
					final SmoothedTileGfx tile = smoothedTiles [mapViewPlane] [mapCoords.getY ()] [mapCoords.getX ()];
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
							final AnimationGfx anim = getGraphicsDB ().findAnimation (tile.getTileAnimation (), "generateOverlandMapBitmaps");
							for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
							{
								final BufferedImage image = getUtils ().loadImage (anim.getFrame ().get (frameNo));
								g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
							}
						}
					}
					
					// Map feature
					final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(mapViewPlane).getRow ().get (mapCoords.getY ()).getCell ().get (mapCoords.getX ());
					final String mapFeatureID = (gc.getTerrainData () == null) ? null : gc.getTerrainData ().getMapFeatureID ();
					if (mapFeatureID != null)
					{
						final MapFeatureGfx mapFeature = getGraphicsDB ().findMapFeature (mapFeatureID, "generateOverlandMapBitmaps");
						final BufferedImage image = getUtils ().loadImage (mapFeature.getOverlandMapImageFile ());
	
						// Use same image for all frames
						for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
							g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
					}
				}
				
				// Use proper routine to move map coordinates so it correctly handles wrapping edges (startX might = 58 on a width 60 map)
				getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.SOUTH.getDirectionID ());
			}
			getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.EAST.getDirectionID ());
		}
		
		// Cities have to be drawn in a 2nd pass, since they're larger than the terrain tiles
		mapCoords.setX (startX);
		for (int x = 0; x < countX; x++)
		{
			mapCoords.setY (startY);
			for (int y = 0; y < countY; y++)
			{
				if (getCoordinateSystemUtils ().areCoordinatesWithinRange (mapSize, mapCoords))
				{
					final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(mapViewPlane).getRow ().get (mapCoords.getY ()).getCell ().get (mapCoords.getX ());
					final String citySizeID = (gc.getCityData () == null) ? null : gc.getCityData ().getCitySizeID ();
					if (citySizeID != null)
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (mapCoords.getX (), mapCoords.getY (), mapViewPlane);
						
						final CityImageGfx cityImage = getGraphicsDB ().findBestCityImage (citySizeID, cityLocation,
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), "generateOverlandMapBitmaps");
						final BufferedImage image = getUtils ().loadImage (cityImage.getCityImageFile ());
						
						final int xpos = (x * overlandMapTileSet.getTileWidth ()) - ((image.getWidth () - overlandMapTileSet.getTileWidth ()) / 2);
						final int ypos = (y * overlandMapTileSet.getTileHeight ()) - ((image.getHeight () - overlandMapTileSet.getTileHeight ()) / 2);
	
						// Use same image for all frames
						final BufferedImage cityFlagImage = getPlayerColourImageGenerator ().getCityFlagImage (gc.getCityData ().getCityOwnerID ());
						for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
						{
							g [frameNo].drawImage (image, xpos, ypos, null);
							g [frameNo].drawImage (cityFlagImage, xpos + cityImage.getFlagOffsetX (), ypos + cityImage.getFlagOffsetY (), null);
						}
					}
				}
				
				getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.SOUTH.getDirectionID ());
			}
			getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.EAST.getDirectionID ());
		}
		
		// Node auras have to be drawn in a 3rd pass, so they appear over the top of cities
		mapCoords.setX (startX);
		for (int x = 0; x < countX; x++)
		{
			mapCoords.setY (startY);
			for (int y = 0; y < countY; y++)
			{
				if (getCoordinateSystemUtils ().areCoordinatesWithinRange (mapSize, mapCoords))
				{
					final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(mapViewPlane).getRow ().get (mapCoords.getY ()).getCell ().get (mapCoords.getX ());
					
					if ((mc.getTerrainData () != null) && (mc.getTerrainData ().getNodeOwnerID () != null))
						
						// Copy each animation frame over to each bitmap
						for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
						{
							final BufferedImage nodeAuraImage = getPlayerColourImageGenerator ().getNodeAuraImage (frameNo, mc.getTerrainData ().getNodeOwnerID ());
	
							g [frameNo].drawImage (nodeAuraImage, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
						}
				}
				
				getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.SOUTH.getDirectionID ());
			}
			getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.EAST.getDirectionID ());
		}
		
		// Clean up the drawing contexts 
		for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
			g [frameNo].dispose ();
		
		log.trace ("Exiting generateOverlandMapBitmaps");
		return overlandMapBitmaps;
	}
	
	/**
	 * Generates big bitmap of the smoothed edges of blackness that obscure the edges
	 * of the outermost tiles we can see, so that the edges aren't just a solid black line.
	 * 
	 * Generated bitmap will be 20x countX by 18x countY pixels in size.
	 * 
	 * @param mapViewPlane Which plane to generate FOW bitmap for
	 * @param startX Map coordinate of the cell to draw at the left edge of the bitmaps
	 * @param startY Map coordinate of the cell to draw at the top edge of the bitmaps
	 * @param countX Width of the bitmap to generate, in number of map cells
	 * @param countY Height of the bitmap to generate, in number of map cells
	 * @return Fog of war bitmap
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Override
	public final BufferedImage generateFogOfWarBitmap (final int mapViewPlane, final int startX, final int startY, final int countX, final int countY) throws IOException
	{
		log.trace ("Entering generateFogOfWarBitmap: " + mapViewPlane);

		// Depending on options, we may not need to do anything at all
		final BufferedImage fogOfWarBitmap;
		if ((!getClientConfig ().isOverlandShowPartialFogOfWar ()) && (!getClientConfig ().isOverlandSmoothFogOfWar ()))
			fogOfWarBitmap = null;
		else
		{
			final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
			final int maxDirection = getCoordinateSystemUtils ().getMaxDirection (mapSize.getCoordinateSystemType ());

			// Choose the appropriate tile sets
			final TileSetGfx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "generateFogOfWarBitmap");
			final SmoothedTileTypeGfx fullFogOfWar = overlandMapTileSet.findSmoothedTileType (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR, null, null);
			final SmoothedTileTypeGfx partialFogOfWar = overlandMapTileSet.findSmoothedTileType (CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR_HAVE_SEEN, null, null);

			// Create the empty bitmap
			fogOfWarBitmap = new BufferedImage
				(countX * overlandMapTileSet.getTileWidth (), countY * overlandMapTileSet.getTileHeight (), BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = fogOfWarBitmap.createGraphics ();

			final MapCoordinates2DEx mapCoords = new MapCoordinates2DEx (startX, startY);
			for (int x = 0; x < countX; x++)
			{
				mapCoords.setY (startY);
				for (int y = 0; y < countY; y++)
				{
					// If close to a non-wrapping edge (possible to put a city on the top/bottom row of tundra on the map), we may move off the end of the map
					if (getCoordinateSystemUtils ().areCoordinatesWithinRange (mapSize, mapCoords))
					{
						final FogOfWarStateID state = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar ().getPlane ().get
							(mapViewPlane).getRow ().get (mapCoords.getY ()).getCell ().get (mapCoords.getX ());
						
						// First deal with the "full" fog of war, i.e. the border between areas we either
						// can or have seen, & haven't seen, which is a totally black smoothing border
						if ((state != FogOfWarStateID.NEVER_SEEN) && (getClientConfig ().isOverlandSmoothFogOfWar ()))
						{
							// Generate the bitmask for this map cell
							final StringBuffer bitmask = new StringBuffer ();
							for (int d = 1; d <= maxDirection; d++)
							{
								final MapCoordinates3DEx coords = new MapCoordinates3DEx (mapCoords.getX (), mapCoords.getY (), mapViewPlane);
								if (getCoordinateSystemUtils ().move3DCoordinates (mapSize, coords, d))
								{
									final FogOfWarStateID otherState = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar ().getPlane ().get
										(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
									
									if (otherState == FogOfWarStateID.NEVER_SEEN)
										bitmask.append ("0");
									else
										bitmask.append ("1");
								}
								else
									bitmask.append ("1");
							}
		
							// If this tile has no Fog of War anywhere around it, then we don't need to obscure its edges in any way
							final String bitmaskString = bitmask.toString ();
							if (!bitmaskString.equals ("11111111"))
							{
								final BufferedImage image = getUtils ().loadImage (fullFogOfWar.getRandomImage (bitmaskString).getTileFile ());
								g.drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
							}							 
						}
						
						if (getClientConfig ().isOverlandShowPartialFogOfWar ())
						{
							// Borders at the edge of partial fog of war (i.e. what we have seen)
							if ((state == FogOfWarStateID.CAN_SEE) && (getClientConfig ().isOverlandSmoothFogOfWar ()))
							{
								// Generate the bitmask for this map cell
								final StringBuffer bitmask = new StringBuffer ();
								for (int d = 1; d <= maxDirection; d++)
								{
									final MapCoordinates3DEx coords = new MapCoordinates3DEx (mapCoords.getX (), mapCoords.getY (), mapViewPlane);
									if (getCoordinateSystemUtils ().move3DCoordinates (mapSize, coords, d))
									{
										final FogOfWarStateID otherState = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar ().getPlane ().get
											(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
										
										if (otherState == FogOfWarStateID.HAVE_SEEN)
											bitmask.append ("0");
										else
											bitmask.append ("1");
									}
									else
										bitmask.append ("1");
								}
			
								// If this tile has no Fog of War anywhere around it, then we don't need to obscure its edges in any way
								final String bitmaskString = bitmask.toString ();
								if (!bitmaskString.equals ("11111111"))
								{
									final BufferedImage image = getUtils ().loadImage (partialFogOfWar.getRandomImage (bitmaskString).getTileFile ());
									g.drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
								}							 
							}
							
							// Solid area we have seen, but can no longer see
							else if (state == FogOfWarStateID.HAVE_SEEN)
							{
								g.setColor (PARTIAL_FOW_COLOUR);
								g.fillRect (x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), overlandMapTileSet.getTileWidth (), overlandMapTileSet.getTileHeight ());
							}
						}
					}
					
					// Use proper routine to move map coordinates so it correctly handles wrapping edges (startX might = 58 on a width 60 map)
					getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.SOUTH.getDirectionID ());
				}
				getCoordinateSystemUtils ().move2DCoordinates (mapSize, mapCoords, SquareMapDirection.EAST.getDirectionID ());
			}
			
			g.dispose ();
		}
		
		log.trace ("Exiting generateFogOfWarBitmap = " + fogOfWarBitmap);
		return fogOfWarBitmap;
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
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param gen Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator gen)
	{
		playerColourImageGenerator = gen;
	}

	/**
	 * @return Client config, containing various overland map settings
	 */	
	public final MomImeClientConfigEx getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing various overland map settings
	 */
	public final void setClientConfig (final MomImeClientConfigEx config)
	{
		clientConfig = config;
	}
}
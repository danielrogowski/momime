package momime.server.mapgenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_4.MapRowOfCombatTiles;
import momime.common.messages.v0_9_4.MemoryGridCell;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.MomCombatTileLayer;
import momime.common.utils.ICombatMapUtils;
import momime.common.utils.IMemoryBuildingUtils;
import momime.common.utils.IMemoryMaintainedSpellUtils;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.CombatMapElement;
import momime.server.database.v0_9_4.CombatTileType;
import momime.server.database.v0_9_4.TileType;
import momime.server.messages.v0_9_4.ServerGridCell;
import momime.server.utils.RandomUtils;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemType;
import com.ndg.map.MapCoordinates;


/**
 * Server only class which contains all the code for generating a random combat map
 * 
 * NB. This is a singleton generator class - each method call generates and returns a new combat map
 * This is in contrast to the OverlandMapGenerator which has one generator created per session and is tied to the overland map being created
 */
public final class CombatMapGenerator
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CombatMapGenerator.class.getName ());

	/** Combat map size - hard coded for now */
	public final static int COMBAT_MAP_WIDTH = 12;
	
	/** Combat map size - hard coded for now */
	public final static int COMBAT_MAP_HEIGHT = 25;
	
	/** How many generation zones the map is split up into to randomize the height map - hard coded for now */
	private final static int COMBAT_MAP_ZONES_HORIZONTALLY = 10;

	/** How many generation zones the map is split up into to randomize the height map - hard coded for now */
	private final static int COMBAT_MAP_ZONES_VERTICALLY = 8;
	
	/** MemoryBuilding utils */
	private IMemoryBuildingUtils memoryBuildingUtils;

	/** MemoryMaintainedSpell utils */
	private IMemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Combat map utils */
	private ICombatMapUtils combatMapUtils;
	
	/**
	 * @param db Server database XML
	 * @param trueTerrain Details of the overland map, buildings and so on
	 * @param combatMapLocation The location that the map is being generated for (we need this in order to look for buildings, etc)
	 * @return Newly generated combat map
	 * @throws RecordNotFoundException If one of the elements that meets the conditions specifies a combatTileTypeID that doesn't exist in the database
	 */
	public final MapAreaOfCombatTiles generateCombatMap (final ServerDatabaseEx db, final FogOfWarMemory trueTerrain, final OverlandMapCoordinatesEx combatMapLocation)
		throws RecordNotFoundException
	{
		log.entering (CombatMapGenerator.class.getName (), "generateCombatMap");
		
		// What tileType is the map cell we're generating a combat map for?
		final ServerGridCell mc = (ServerGridCell) trueTerrain.getMap ().getPlane ().get (combatMapLocation.getPlane ()).getRow ().get (combatMapLocation.getY ()).getCell ().get (combatMapLocation.getX ());
		final TileType tileType = db.findTileType (mc.getTerrainData ().getTileTypeID (), "generateCombatMap");

		// Start map generation, this initializes all the map cells
		final MapAreaOfCombatTiles map = setAllToGrass ();
		
		// Not sure if will have to move this out of here later
		final CoordinateSystem sys = new CoordinateSystem ();
		sys.setWidth (COMBAT_MAP_WIDTH);
		sys.setHeight (COMBAT_MAP_HEIGHT);
		sys.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		
		// Generate height-based scenery
		final HeightMapGenerator heightMap = new HeightMapGenerator (sys, COMBAT_MAP_ZONES_HORIZONTALLY, COMBAT_MAP_ZONES_VERTICALLY, 0);
		heightMap.generateHeightMap ();
		
		// Set troughs and hills
		setLowestTiles (heightMap, map, ServerDatabaseValues.VALUE_COMBAT_TILE_TYPE_DARK, tileType.getCombatDarkTiles ());
		setHighestTiles (heightMap, map, ServerDatabaseValues.VALUE_COMBAT_TILE_TYPE_RIDGE, tileType.getCombatRidgeTiles ());
		
		// Place trees/rocks randomly
		setTerrainFeaturesRandomly (map, ServerDatabaseValues.VALUE_COMBAT_TILE_TERRAIN_FEATURE, tileType.getCombatTerrainFeatures ());
		
		// Place walls, buildings, houses, nodes, towers and anything else defined in the combat map elements in the server XML
		// Purposefully do this 2nd, so if there happens to be a tree right where we need to put the Wizards' Fortress, the tree will be overwritten
		placeCombatMapElements (map, db, trueTerrain, combatMapLocation);
		
		// Store the map against the grid cell on the server
		mc.setCombatMap (map);
		
		log.exiting (CombatMapGenerator.class.getName (), "generateCombatMap");
		return map;
	}
	
	/**
	 * @return Newly created map initialized with all cells set to grass; also sets all of the offMapEdge values
	 */
	final MapAreaOfCombatTiles setAllToGrass ()
	{
		log.entering (CombatMapGenerator.class.getName (), "setAllToGrass");

		// Create empty map
		final MapAreaOfCombatTiles map = new MapAreaOfCombatTiles ();

		// Create all the map cells
		for (int y = 0; y < COMBAT_MAP_HEIGHT; y++)
		{
			final MapRowOfCombatTiles row = new MapRowOfCombatTiles ();
			for (int x = 0; x < COMBAT_MAP_WIDTH; x++)
			{
				// Create grass tile
				final MomCombatTileLayer layer = new MomCombatTileLayer ();
				layer.setLayer (CombatMapLayerID.TERRAIN);
				layer.setCombatTileTypeID (ServerDatabaseValues.VALUE_COMBAT_TILE_TYPE_GRASS);
				
				final MomCombatTile cell = new MomCombatTile ();
				cell.getTileLayer ().add (layer);
				
				// Set the areas that are off the edge of the map
				// See "MoMIMEMiscellaneous\MoM IME screen layouts\Combat map.psp" for why these boundaries are like this
				final boolean canWalkOn = (x >= 1) && (y >= 2) &&
					(y <= COMBAT_MAP_HEIGHT - 3) &&
					(((y % 2 == 0) && (x <= COMBAT_MAP_WIDTH - 2)) ||
					((y % 2 == 1) && (x <= COMBAT_MAP_WIDTH - 3)));
				
				cell.setOffMapEdge (!canWalkOn);
				
				// Add it
				row.getCell ().add (cell);
			}

			map.getRow ().add (row);
		}

		log.exiting (CombatMapGenerator.class.getName (), "setAllToGrass");
		return map;
	}
	
	/**
	 * Finds the height in the heightmap for which as close as possible to DesiredTileCount tiles are above that height and the remainder are below it
	 * Then sets all tiles above this height to have scenery combatTileTypeID
	 *
	 * @param heightMap Height map generated for this plane
	 * @param map Map to update tiles
	 * @param combatTileTypeID The tile type to set the highest tiles to
	 * @param desiredTileCount How many tiles to set to this tile type
	 */
	private final void setHighestTiles (final HeightMapGenerator heightMap, final MapAreaOfCombatTiles map, final String combatTileTypeID, final int desiredTileCount)
	{
		log.entering (CombatMapGenerator.class.getName (), "setHighestTiles", new String [] {combatTileTypeID, new Integer (desiredTileCount).toString ()});

		heightMap.setHighestTiles (desiredTileCount, new ProcessTileCallback ()
		{
			@Override
			public final void process (final int x, final int y)
			{
				// We can assume the terrain layer will always be the first/only one in the list, since this gets called right after setAllToGrass
				map.getRow ().get (y).getCell ().get (x).getTileLayer ().get (0).setCombatTileTypeID (combatTileTypeID);
			}
		});

		log.exiting (CombatMapGenerator.class.getName (), "setHighestTiles");
	}

	/**
	 * Finds the height in the heightmap for which as close as possible to DesiredTileCount tiles are below that height and the remainder are above it
	 * Then sets all tiles above this height to have scenery combatTileTypeID
	 *
	 * @param heightMap Height map generated for this plane
	 * @param map Map to update tiles
	 * @param combatTileTypeID The tile type to set the highest tiles to
	 * @param desiredTileCount How many tiles to set to this tile type
	 */
	private final void setLowestTiles (final HeightMapGenerator heightMap, final MapAreaOfCombatTiles map, final String combatTileTypeID, final int desiredTileCount)
	{
		log.entering (CombatMapGenerator.class.getName (), "setLowestTiles", new String [] {combatTileTypeID, new Integer (desiredTileCount).toString ()});

		heightMap.setLowestTiles (desiredTileCount, new ProcessTileCallback ()
		{
			@Override
			public final void process (final int x, final int y)
			{
				// We can assume the terrain layer will always be the first/only one in the list, since this gets called right after setAllToGrass
				map.getRow ().get (y).getCell ().get (x).getTileLayer ().get (0).setCombatTileTypeID (combatTileTypeID);
			}
		});

		log.exiting (CombatMapGenerator.class.getName (), "setLowestTiles");
	}
	
	/**
	 * Sets rocks and trees in random locations in the building layer
	 * @param map Map to add trees and rocks to
	 * @param combatTileTypeID Tile type to use for trees and rocks
	 * @param featureTileCount How many trees and rocks to add
	 */
	final void setTerrainFeaturesRandomly (final MapAreaOfCombatTiles map, final String combatTileTypeID, final int featureTileCount)
	{
		log.exiting (CombatMapGenerator.class.getName (), "setTerrainFeaturesRandomly");

		// Make a list of all the possible locations
		// Since there's nothing in the building layer at this point - that means everywhere
		final List<MapCoordinates> possibleLocations = new ArrayList<MapCoordinates> ();
		for (int x = 0; x < COMBAT_MAP_WIDTH; x++)
			for (int y = 0; y < COMBAT_MAP_HEIGHT; y++)
			{
				final MapCoordinates coords = new MapCoordinates ();
				coords.setX (x);
				coords.setY (y);
				possibleLocations.add (coords);
			}
		
		// Now add features
		int featuresAdded = 0;
		while ((featuresAdded < featureTileCount) && (possibleLocations.size () > 0))
		{
			final int index = RandomUtils.getGenerator ().nextInt (possibleLocations.size ());
			final MapCoordinates coords = possibleLocations.get (index);
			possibleLocations.remove (index);
			featuresAdded++;
			
			// Terrain features go in the building layer, which we know won't exist at this point, so we can just add it without checking if its already there
			final MomCombatTileLayer layer = new MomCombatTileLayer ();
			layer.setLayer (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES);
			layer.setCombatTileTypeID (combatTileTypeID);
			
			map.getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTileLayer ().add (layer);
		}

		log.exiting (CombatMapGenerator.class.getName (), "setTerrainFeaturesRandomly");
	}
	
	/**
	 * Places walls, buildings, houses, nodes, towers and anything else defined in the combat map elements in the server XML
	 * @param map Map to add elements to
	 * @param db Server database XML
	 * @param trueTerrain Details of the overland map, buildings and so on
	 * @param combatMapLocation The location that the map is being generated for (we need this in order to look for buildings, etc)
	 * @throws RecordNotFoundException If one of the elements that meets the conditions specifies a combatTileTypeID that doesn't exist in the database
	 */
	final void placeCombatMapElements (final MapAreaOfCombatTiles map, final ServerDatabaseEx db, final FogOfWarMemory trueTerrain, final OverlandMapCoordinatesEx combatMapLocation)
		throws RecordNotFoundException
	{
		log.entering (CombatMapGenerator.class.getName (), "placeCombatMapElements");
		
		// Find the map cell
		final MemoryGridCell mc = trueTerrain.getMap ().getPlane ().get (combatMapLocation.getPlane ()).getRow ().get (combatMapLocation.getY ()).getCell ().get (combatMapLocation.getX ());
		
		// Check each element
		for (final CombatMapElement element : db.getCombatMapElement ())
		{
			// Check conditions
			final String tileTypeID;
			final String mapFeatureID;
			if (mc.getTerrainData () == null)
			{
				tileTypeID = null;
				mapFeatureID = null;
			}
			else
			{
				tileTypeID = mc.getTerrainData ().getTileTypeID ();
				mapFeatureID = mc.getTerrainData ().getMapFeatureID ();
			}
			
			final int cityPopulation;
			if (mc.getCityData () == null)
				cityPopulation = -1;
			else if (mc.getCityData ().getCityPopulation () == null)
				cityPopulation = -1;
			else
				cityPopulation = mc.getCityData ().getCityPopulation ();
			
			if (((element.getTileTypeID () == null) || (element.getTileTypeID ().equals (tileTypeID))) &&
				((element.getMapFeatureID () == null) || (element.getMapFeatureID ().equals (mapFeatureID))) &&
				((element.getMinimumPopulation () == null) || (cityPopulation >= element.getMinimumPopulation ())) &&
				((element.getMaximumPopulation () == null) || (cityPopulation <= element.getMaximumPopulation ())) &&
				((element.getBuildingID () == null) || (getMemoryBuildingUtils ().findBuilding (trueTerrain.getBuilding (), combatMapLocation, element.getBuildingID ()))) &&
				((element.getSpellID () == null) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
					(trueTerrain.getMaintainedSpell (), null, element.getSpellID (), null, null, combatMapLocation, null) != null)))
			{
				// Get the cell to update
				final MomCombatTile combatTile = map.getRow ().get (element.getLocationY ()).getCell ().get (element.getLocationX ());
				
				// Place combat tile
				if (element.getCombatTileTypeID () != null)
				{
					final CombatTileType ctt = db.findCombatTileType (element.getCombatTileTypeID (), "placeCombatMapElements");
					getCombatMapUtils ().setCombatTileTypeForLayer (combatTile, ctt.getCombatMapLayer (), element.getCombatTileTypeID ());
				}
				
				// Place borders
				if ((element.getCombatTileBorderDirections () != null) && (element.getCombatTileBorderID () != null))
				{
					combatTile.getBorderID ().add (element.getCombatTileBorderID ());
					combatTile.setBorderDirections (element.getCombatTileBorderDirections ());
				}
			}			
		}
		
		log.exiting (CombatMapGenerator.class.getName (), "placeCombatMapElements");
	}

	/**
	 * @return MemoryBuilding utils
	 */
	public final IMemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final IMemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final IMemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final IMemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}
	
	/**
	 * @return Combat map utils
	 */
	public final ICombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param utils Combat map utils
	 */
	public final void setCombatMapUtils (final ICombatMapUtils utils)
	{
		combatMapUtils = utils;
	}
}
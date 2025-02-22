package momime.server.mapgenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.MapCoordinates2D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.map.generator.HeightMapGenerator;
import com.ndg.map.generator.HeightMapGeneratorImpl;
import com.ndg.utils.random.RandomUtils;

import momime.common.database.CombatMapElement;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileTypeEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapRowOfCombatTiles;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomCombatTileLayer;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.CombatMapServerUtils;

/**
 * Server only class which contains all the code for generating a random combat map
 * 
 * NB. This is a singleton generator class - each method call generates and returns a new combat map
 * This is in contrast to the OverlandMapGenerator which has one generator created per session and is tied to the overland map being created
 */
public final class CombatMapGeneratorImpl implements CombatMapGenerator
{
	/** MemoryBuilding utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/**
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Server database XML
	 * @param trueTerrain Details of the overland map, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param combatMapLocation The location that the map is being generated for (we need this in order to look for buildings, etc)
	 * @return Newly generated combat map
	 * @throws RecordNotFoundException If one of the elements that meets the conditions specifies a combatTileTypeID that doesn't exist in the database
	 */
	@Override
	public final MapAreaOfCombatTiles generateCombatMap (final CombatMapSize combatMapCoordinateSystem,
		final CommonDatabase db, final FogOfWarMemory trueTerrain, final CoordinateSystem overlandMapCoordinateSystem, final MapCoordinates3DEx combatMapLocation)
		throws RecordNotFoundException
	{
		// What tileType is the map cell we're generating a combat map for?
		final ServerGridCellEx mc = (ServerGridCellEx) trueTerrain.getMap ().getPlane ().get (combatMapLocation.getZ ()).getRow ().get (combatMapLocation.getY ()).getCell ().get (combatMapLocation.getX ());
		final TileTypeEx tileType = db.findTileType (mc.getTerrainData ().getTileTypeID (), "generateCombatMap");

		// Start map generation, this initializes all the map cells
		final MapAreaOfCombatTiles map = setAllToDefaultTerrain (combatMapCoordinateSystem, tileType);
		
		// Generate height-based scenery
		final HeightMapGenerator heightMap = new HeightMapGeneratorImpl ();
		heightMap.setCoordinateSystem (combatMapCoordinateSystem);
		heightMap.setZoneWidth (combatMapCoordinateSystem.getZoneWidth ());
		heightMap.setZoneHeight (combatMapCoordinateSystem.getZoneHeight ());
		heightMap.getNumberOfRowsFromMapEdgeToAvoid ().add (7);		// This was baked into the map generator before, so keep it the same
		heightMap.setRandomUtils (getRandomUtils ());
		heightMap.generateHeightMap ();
		
		// Set troughs and hills
		if (tileType.getCombatDarkTiles () != null)
			setLowestTiles (heightMap, map, CommonDatabaseConstants.COMBAT_TILE_TYPE_DARK, tileType.getCombatDarkTiles ());
		
		if (tileType.getCombatRidgeTiles () != null)
			setHighestTiles (heightMap, map, CommonDatabaseConstants.COMBAT_TILE_TYPE_RIDGE, tileType.getCombatRidgeTiles ());

		// Special handling for Flying Fortress.  Could just put all the clouds in the XML but there'd be so much of it...
		final boolean flyingFortress = (getMemoryMaintainedSpellUtils ().findMaintainedSpell (trueTerrain.getMaintainedSpell (), null,
			CommonDatabaseConstants.SPELL_ID_FLYING_FORTRESS, null, null, combatMapLocation, null) != null);
			
		// Place trees/rocks randomly
		if ((!flyingFortress) && (tileType.getCombatTerrainFeatures () != null))
			setTerrainFeaturesRandomly (map, combatMapCoordinateSystem, CommonDatabaseConstants.COMBAT_TILE_TERRAIN_FEATURE, tileType.getCombatTerrainFeatures ());
		
		// Place walls, buildings, houses, nodes, towers and anything else defined in the combat map elements in the server XML
		// Purposefully do this 2nd, so if there happens to be a tree right where we need to put the Wizards' Fortress, the tree will be overwritten
		placeCombatMapElements (map, db, trueTerrain, overlandMapCoordinateSystem, combatMapLocation);

		if (flyingFortress)
			placeClouds (map, db);
		
		return map;
	}
	
	/**
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param tileType Overland map tile type we are generating a combat map for
	 * @return Newly created map initialized with all cells set to default (usually grass); also sets all of the offMapEdge values
	 */
	final MapAreaOfCombatTiles setAllToDefaultTerrain (final CoordinateSystem combatMapCoordinateSystem, final TileTypeEx tileType)
	{
		// Create empty map
		final MapAreaOfCombatTiles map = new MapAreaOfCombatTiles ();

		// Create all the map cells
		for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
		{
			final MapRowOfCombatTiles row = new MapRowOfCombatTiles ();
			for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
			{
				// Create grass tile
				final MomCombatTileLayer layer = new MomCombatTileLayer ();
				layer.setLayer (CombatMapLayerID.TERRAIN);
				layer.setCombatTileTypeID (tileType.getCombatTileTypeID ());
				
				final MomCombatTile cell = new MomCombatTile ();
				cell.getTileLayer ().add (layer);
				
				// Set the areas that are off the edge of the map
				// See "MoMIMEMiscellaneous\MoM IME screen layouts\Combat map.psp" for why these boundaries are like this
				final boolean canWalkOn = (x >= 1) && (y >= 2) &&
					(y <= combatMapCoordinateSystem.getHeight () - 3) &&
					(((y % 2 == 0) && (x <= combatMapCoordinateSystem.getWidth () - 2)) ||
					((y % 2 != 0) && (x <= combatMapCoordinateSystem.getWidth () - 3)));
				
				cell.setOffMapEdge (!canWalkOn);
				
				// Add it
				row.getCell ().add (cell);
			}

			map.getRow ().add (row);
		}

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
		heightMap.setHighestTiles (desiredTileCount, (x, y) ->
		
			// We can assume the terrain layer will always be the first/only one in the list, since this gets called right after setAllToGrass
			map.getRow ().get (y).getCell ().get (x).getTileLayer ().get (0).setCombatTileTypeID (combatTileTypeID));
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
		heightMap.setLowestTiles (desiredTileCount, (x, y) ->

			// We can assume the terrain layer will always be the first/only one in the list, since this gets called right after setAllToGrass
			map.getRow ().get (y).getCell ().get (x).getTileLayer ().get (0).setCombatTileTypeID (combatTileTypeID));
	}
	
	/**
	 * Sets rocks and trees in random locations in the building layer
	 * @param map Map to add trees and rocks to
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param combatTileTypeID Tile type to use for trees and rocks
	 * @param featureTileCount How many trees and rocks to add
	 */
	final void setTerrainFeaturesRandomly (final MapAreaOfCombatTiles map, final CoordinateSystem combatMapCoordinateSystem,
		final String combatTileTypeID, final int featureTileCount)
	{
		// Make a list of all the possible locations
		// Since there's nothing in the building layer at this point - that means everywhere
		final List<MapCoordinates2D> possibleLocations = new ArrayList<MapCoordinates2D> ();
		for (int x = 0; x < combatMapCoordinateSystem.getWidth (); x++)
			for (int y = 0; y < combatMapCoordinateSystem.getHeight (); y++)
			{
				final MapCoordinates2D coords = new MapCoordinates2D ();
				coords.setX (x);
				coords.setY (y);
				possibleLocations.add (coords);
			}
		
		// Now add features
		int featuresAdded = 0;
		while ((featuresAdded < featureTileCount) && (possibleLocations.size () > 0))
		{
			final int index = getRandomUtils ().nextInt (possibleLocations.size ());
			final MapCoordinates2D coords = possibleLocations.get (index);
			possibleLocations.remove (index);
			featuresAdded++;
			
			// Terrain features go in the building layer, which we know won't exist at this point, so we can just add it without checking if its already there
			final MomCombatTileLayer layer = new MomCombatTileLayer ();
			layer.setLayer (CombatMapLayerID.BUILDINGS_AND_TERRAIN_FEATURES);
			layer.setCombatTileTypeID (combatTileTypeID);
			
			map.getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTileLayer ().add (layer);
		}
	}
	
	/**
	 * Places walls, buildings, houses, nodes, towers and anything else defined in the combat map elements in the server XML
	 * @param map Map to add elements to
	 * @param db Server database XML
	 * @param trueTerrain Details of the overland map, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param combatMapLocation The location that the map is being generated for (we need this in order to look for buildings, etc)
	 * @throws RecordNotFoundException If one of the elements that meets the conditions specifies a combatTileTypeID that doesn't exist in the database
	 */
	final void placeCombatMapElements (final MapAreaOfCombatTiles map, final CommonDatabase db, final FogOfWarMemory trueTerrain,
		final CoordinateSystem overlandMapCoordinateSystem, final MapCoordinates3DEx combatMapLocation)
		throws RecordNotFoundException
	{
		// Find the map cell
		final MemoryGridCell mc = trueTerrain.getMap ().getPlane ().get (combatMapLocation.getZ ()).getRow ().get (combatMapLocation.getY ()).getCell ().get (combatMapLocation.getX ());
		
		// Get a list of all road tile types in adjacent map cells, but NOT including the map cell in the centre (where the combat actually is)
		final Set<String> adjacentRoadTileTypeIDs = new HashSet<String> ();
		for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (overlandMapCoordinateSystem.getCoordinateSystemType ()); d++)
		{
			final MapCoordinates3DEx newCoords = new MapCoordinates3DEx (combatMapLocation);
			if (getCoordinateSystemUtils ().move3DCoordinates (overlandMapCoordinateSystem, newCoords, d))
			{
				final OverlandMapTerrainData ringTerrain = trueTerrain.getMap ().getPlane ().get (newCoords.getZ ()).getRow ().get (newCoords.getY ()).getCell ().get (newCoords.getX ()).getTerrainData ();
				if ((ringTerrain != null) && (ringTerrain.getRoadTileTypeID () != null) && (!adjacentRoadTileTypeIDs.contains (ringTerrain.getRoadTileTypeID ())))
					adjacentRoadTileTypeIDs.add (ringTerrain.getRoadTileTypeID ());
			}
		}
		
		// Check each element
		for (final CombatMapElement element : db.getCombatMapElement ())
		{
			// Check conditions
			final String tileTypeID;
			final String mapFeatureID;
			final String roadTileTypeID;
			if (mc.getTerrainData () == null)
			{
				tileTypeID = null;
				mapFeatureID = null;
				roadTileTypeID = null;
			}
			else
			{
				tileTypeID = mc.getTerrainData ().getTileTypeID ();
				mapFeatureID = mc.getTerrainData ().getMapFeatureID ();
				roadTileTypeID = mc.getTerrainData ().getRoadTileTypeID ();
			}
			
			final int cityPopulation = (mc.getCityData () == null) ? -1 : mc.getCityData ().getCityPopulation ();
			
			if (((element.getTileTypeID () == null) || (element.getTileTypeID ().equals (tileTypeID))) &&
				((element.getRoadTileTypeID () == null) || (element.getRoadTileTypeID ().equals (roadTileTypeID))) &&
				((element.getAdjacentRoadTileTypeID () == null) || (adjacentRoadTileTypeIDs.contains (element.getAdjacentRoadTileTypeID ()))) &&
				((element.getMapFeatureID () == null) || (element.getMapFeatureID ().equals (mapFeatureID))) &&
				((element.getMinimumPopulation () == null) || (cityPopulation >= element.getMinimumPopulation ())) &&
				((element.getMaximumPopulation () == null) || (cityPopulation <= element.getMaximumPopulation ())) &&
				((element.getBuildingID () == null) || (getMemoryBuildingUtils ().findBuilding (trueTerrain.getBuilding (), combatMapLocation, element.getBuildingID ()) != null)) &&
				((element.getSpellID () == null) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
					(trueTerrain.getMaintainedSpell (), null, element.getSpellID (), null, null, combatMapLocation, null) != null)) &&
				((element.getExcludeSpellID () == null) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
					(trueTerrain.getMaintainedSpell (), null, element.getExcludeSpellID (), null, null, combatMapLocation, null) == null)))
			{
				// Get the cell to update
				final MomCombatTile combatTile = map.getRow ().get (element.getLocationY ()).getCell ().get (element.getLocationX ());
				
				// Place combat tile
				if (element.getCombatTileTypeID () != null)
				{
					final CombatTileType ctt = db.findCombatTileType (element.getCombatTileTypeID (), "placeCombatMapElements");
					getCombatMapServerUtils ().setCombatTileTypeForLayer (combatTile, ctt.getCombatMapLayer (), element.getCombatTileTypeID ());
				}
				
				// Place borders
				if ((element.getCombatTileBorderDirections () != null) && (element.getCombatTileBorderID () != null))
				{
					combatTile.getBorderID ().add (element.getCombatTileBorderID ());
					combatTile.setBorderDirections (element.getCombatTileBorderDirections ());
				}
			}			
		}
	}

	/**
	 * Many elements of a combat map are random, e.g. placement of ridges, dark areas, rocks and trees.  So when a city enchantment
	 * (Wall of Fire/Darkness) is cast during a combat, we want to regenerate only the tile borders to show the new enchantment
	 * without regenerating the random elements of the combat map.
	 *  
	 * @param map Map to renegerate the tile borders of
	 * @param db Server database XML
	 * @param trueTerrain Details of the overland map, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param combatMapLocation The location that the map is being regenerated for (we need this in order to look for buildings, etc)
	 * @throws RecordNotFoundException If one of the elements that meets the conditions specifies a combatTileTypeID that doesn't exist in the database
	 */
	@Override
	public final void regenerateCombatTileBorders (final MapAreaOfCombatTiles map, final CommonDatabase db, final FogOfWarMemory trueTerrain,
		final CoordinateSystem overlandMapCoordinateSystem, final MapCoordinates3DEx combatMapLocation)
		throws RecordNotFoundException
	{
		// Scrub out all the old borders
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile tile : row.getCell ())
			{
				tile.getBorderID ().clear ();
				tile.setBorderDirections (null);
			}
		
		// Technically this re-places tiles set from map elements too, but since they're all fixed there's no real harm in doing so
		placeCombatMapElements (map, db, trueTerrain, overlandMapCoordinateSystem, combatMapLocation);
	}

	/**
	 * Places cloud tiles everywhere except the city
	 * 
	 * @param map Map to place cloud tiles in
	 * @param db Server database XML
	 */
	final void placeClouds (final MapAreaOfCombatTiles map, final CommonDatabase db)
	{
		final List<String> cityTiles = db.getCombatTileType ().stream ().filter
			(t -> (t.isInsideCity () != null) && (t.isInsideCity ())).map (t -> t.getCombatTileTypeID ()).collect (Collectors.toList ());
			
		for (final MapRowOfCombatTiles row : map.getRow ())
			for (final MomCombatTile tile : row.getCell ())
				if (!tile.getTileLayer ().stream ().anyMatch (l -> cityTiles.contains (l.getCombatTileTypeID ())))
					tile.getTileLayer ().get (0).setCombatTileTypeID (CommonDatabaseConstants.COMBAT_TILE_TYPE_CLOUD);
	}
	
	/**
	 * @return MemoryBuilding utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils MemoryBuilding utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}
	
	/**
	 * @return Methods dealing with combat maps that are only needed on the server
	 */
	public final CombatMapServerUtils getCombatMapServerUtils ()
	{
		return combatMapServerUtils;
	}

	/**
	 * @param u Methods dealing with combat maps that are only needed on the server
	 */
	public final void setCombatMapServerUtils (final CombatMapServerUtils u)
	{
		combatMapServerUtils = u;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}
}
package momime.client.calculations;

import momime.common.database.CombatMapLayerID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SmoothedTileTypeEx;
import momime.common.messages.MapAreaOfCombatTiles;

/**
 * Tile sets have their tiles indexed by a bitmask with a value corresponding to each adjacent tile,
 * so 00110000 might represent grass everywhere except mountains right and down-right.
 * 
 * This class is responsible for building those bitmasks.  Moved out of OverlandMapBitmapGenerator and
 * CombatMapBitmapGenerator to make it more unit testable.
 */
public interface TileSetBitmaskGenerator
{
	/**
	 * @param smoothedTileType Smoothed tile type to generate bitmask for 
	 * @param riverDirections List of directions in which a river runs from this tile, e.g. a tile with a river heading off the tile downwards = 5
	 * @param x X coordinate of overland map tile we want to generate bitmask for
	 * @param y Y coordinate of overland map tile we want to generate bitmask for
	 * @param planeNo Plane of overland map tile we want to generate bitmask for
	 * @return Bitmask describing adjacent tiles to use to select correct tile image to draw
	 * @throws RecordNotFoundException If the tile set or smoothing system can't be found 
	 */
	public String generateOverlandMapBitmask (final SmoothedTileTypeEx smoothedTileType, final String riverDirections, final int x, final int y, final int planeNo)
		throws RecordNotFoundException;
	
	/**
	 * @param combatTerrain Details of combat map terrain
	 * @param smoothedTileType Smoothed tile type to generate bitmask for 
	 * @param x X coordinate of combat map tile we want to generate bitmask for
	 * @param y Y coordinate of combat map tile we want to generate bitmask for
	 * @param layer Combat map layer of map tile we want to generate bitmask for
	 * @return Bitmask describing adjacent tiles to use to select correct tile image to draw
	 * @throws RecordNotFoundException If the tile set or smoothing system can't be found 
	 */
	public String generateCombatMapBitmask (final MapAreaOfCombatTiles combatTerrain,
		final SmoothedTileTypeEx smoothedTileType, final CombatMapLayerID layer, final int x, final int y)
		throws RecordNotFoundException;
}
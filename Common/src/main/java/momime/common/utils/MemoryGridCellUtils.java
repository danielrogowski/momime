package momime.common.utils;

import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.OverlandMapTerrainData;

/**
 * Helper methods for dealing with MemoryGridCell objects
 *
 * There are also specific ClientMemoryGridCellUtils and ServerMemoryGridCellUtils classes, because they need to
 * provide client and server specific implementations of
 *
 * public final static boolean isNodeLairTower (final OverlandMapTerrainData terrainData)
 */
public interface MemoryGridCellUtils
{
	/**
	 * This is used because the data structures hold blank for an unknown tile type that we can't see, but the XML files
	 * define movement rate rules and descriptions for tiles within the fog of war, and these can't be defined as null so are defined as "FOW"
	 *
	 * So whenever looking up a tile type ID that may be out of visible range in one of the XML files, we have to convert the value first
	 *
	 * The Delphi code has a separate property on a MemoryGridCell called TileTypeID_FOWforBlank for this
	 *
	 * @param terrainData Terrain data to check
	 * @return Tile type ID input, with null converted to FOW
	 */
	public String convertNullTileTypeToFOW (final OverlandMapTerrainData terrainData);

	/**
	 * @param terrainData Terrain data to check
	 * @return True if this map feature represents a Tower of Wizardy (cleared or uncleared)
	 */
	public boolean isTerrainTowerOfWizardry (final OverlandMapTerrainData terrainData);

	/**
	 * Nulls out the building sold this turn value in every map cell
	 * @param map Map to wipe buildings sold from
	 */
	public void blankBuildingsSoldThisTurn (final MapVolumeOfMemoryGridCells map);
}
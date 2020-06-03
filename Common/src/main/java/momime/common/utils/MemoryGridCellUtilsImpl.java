package momime.common.utils;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.OverlandMapTerrainData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper methods for dealing with MemoryGridCell objects
 *
 * There are also specific ClientMemoryGridCellUtils and ServerMemoryGridCellUtils classes, because they need to
 * provide client and server specific implementations of
 *
 * public final static boolean isNodeLairTower (final OverlandMapTerrainData terrainData)
 */
public final class MemoryGridCellUtilsImpl implements MemoryGridCellUtils
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MemoryGridCellUtilsImpl.class);
	
	/**
	 * This is used because the data structures hold blank for an unknown tile type that we can't see, but the XML files
	 * define movement rate rules and descriptions for tiles within the fog of war, and these can't be defined as null so are defined as "FOW"
	 *
	 * So whenever looking up a tile type ID that may be out of visible range in one of the XML files, we have to convert the value first
	 *
	 * The Delphi code has a separate property on a MemoryGridCell called TileTypeID_FOWforBlank for this
	 *
	 * @param terrainData Terrain data to check
	 * @param considerRoad If true and there is road in the tile, then the road will be returned instead of the underlying tile
	 * @return Tile type ID input, with null converted to FOW
	 */
	@Override
	public final String convertNullTileTypeToFOW (final OverlandMapTerrainData terrainData, final boolean considerRoad)
	{
		final String tileTypeID;
		if (terrainData == null)
			tileTypeID = CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR;
		else if ((considerRoad) && (terrainData.getRoadTileTypeID () != null))
			tileTypeID = terrainData.getRoadTileTypeID ();
		else if (terrainData.getTileTypeID () == null)
			tileTypeID = CommonDatabaseConstants.TILE_TYPE_FOG_OF_WAR;
		else
			tileTypeID = terrainData.getTileTypeID ();

		return tileTypeID;
	}

	/**
	 * @param terrainData Terrain data to check
	 * @return True if this map feature represents a Tower of Wizardy (cleared or uncleared)
	 */
	@Override
	public final boolean isTerrainTowerOfWizardry (final OverlandMapTerrainData terrainData)
	{
		final boolean tower;
		if (terrainData == null)
			tower = false;
		else if (terrainData.getMapFeatureID () == null)
			tower = false;
		else
			tower = (terrainData.getMapFeatureID ().equals (CommonDatabaseConstants.FEATURE_UNCLEARED_TOWER_OF_WIZARDRY)) ||
			 	(terrainData.getMapFeatureID ().equals (CommonDatabaseConstants.FEATURE_CLEARED_TOWER_OF_WIZARDRY));

		return tower;
	}

	/**
	 * Nulls out the building sold this turn value in every map cell
	 * @param map Map to wipe buildings sold from
	 */
	@Override
	public final void blankBuildingsSoldThisTurn (final MapVolumeOfMemoryGridCells map)
	{
		log.trace ("Entering blankBuildingsSoldThisTurn");

		for (final MapAreaOfMemoryGridCells plane : map.getPlane ())
			for (final MapRowOfMemoryGridCells row : plane.getRow ())
				for (final MemoryGridCell cell : row.getCell ())
					cell.setBuildingIdSoldThisTurn (null);

		log.trace ("Exiting blankBuildingsSoldThisTurn");
	}
}
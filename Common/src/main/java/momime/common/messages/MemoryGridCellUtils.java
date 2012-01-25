package momime.common.messages;

import momime.common.database.CommonDatabaseConstants;

/**
 * Helper methods for dealing with MemoryGridCell objects
 *
 * There are also specific ClientMemoryGridCellUtils and ServerMemoryGridCellUtils classes, because they need to
 * provide client and server specific implementations of
 *
 * public final static boolean isNodeLairTower (final OverlandMapTerrainData terrainData)
 */
public final class MemoryGridCellUtils
{
	/**
	 * This is used because the data structures hold blank for an unknown tile type that we can't see, but the XML files
	 * define movement rate rules and descriptions for tiles within the fog of war, and these can't be defined as null so are defined as "FOW"
	 *
	 * So whenever looking up a tile type ID that may be out of visible range in one of the XML files, we have to convert the value first
	 *
	 * The Delphi code has a separate property on a MemoryGridCell called TileTypeID_FOWforBlank for this
	 *
	 * @param tileTypeID Tile type ID
	 * @return Tile type ID input, with null converted to FOW
	 */
	public final static String convertNullTileTypeToFOW (final String tileTypeID)
	{
		return (tileTypeID == null) ? CommonDatabaseConstants.VALUE_TILE_TYPE_FOG_OF_WAR : tileTypeID;
	}

	/**
	 * @param mapFeatureID Map feature ID
	 * @return True if this map feature represents a Tower of Wizardy (cleared or uncleared)
	 */
	public final static boolean isFeatureTowerOfWizardry (final String mapFeatureID)
	{
		return ((mapFeatureID != null) &&
			((mapFeatureID.equals (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY)) ||
			 (mapFeatureID.equals (CommonDatabaseConstants.VALUE_FEATURE_CLEARED_TOWER_OF_WIZARDRY))));
	}

	/**
	 * Prevent instantiation
	 */
	private MemoryGridCellUtils ()
	{
	}
}

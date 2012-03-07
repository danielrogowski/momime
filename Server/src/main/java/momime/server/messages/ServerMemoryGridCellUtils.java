package momime.server.messages;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.database.ServerDatabaseLookup;

/**
 * Client specific helper methods for dealing with MemoryGridCell objects
 */
public final class ServerMemoryGridCellUtils
{
	/**
	 * @param terrainData Our knowledge of the terrain at this location
	 * @param db Lookup lists built over the XML database
	 * @return True if we know there's a Node, Lair or Tower of Wizardy (cleared or uncleared) here, false if there isn't or we have no knowledge of the location
	 * @throws RecordNotFoundException If the tile type or map feature IDs cannot be found
	 */
	public final static boolean isNodeLairTower (final OverlandMapTerrainData terrainData, final ServerDatabaseLookup db)
		throws RecordNotFoundException
	{
		final boolean result;
		if (terrainData == null)
			result = false;
		else
			result = ((terrainData.getTileTypeID () != null) && (db.findTileType (terrainData.getTileTypeID (), "isNodeLairTower").getMagicRealmID () != null)) ||

			// Lairs & Towers
			((terrainData.getMapFeatureID () != null) && (db.findMapFeature (terrainData.getMapFeatureID (), "isNodeLairTower").getMapFeatureMagicRealm ().size () > 0));

		return result;
	}

	/**
	 * Prevent instantiation
	 */
	private ServerMemoryGridCellUtils ()
	{
	}
}

package momime.common.utils;

import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.v0_9_4.MomCombatTile;

/**
 * Helper utils for dealing with combat maps
 */
public interface CombatMapUtils
{
	/**
	 * @param tile Tile to search
	 * @param layer Layer to look for
	 * @return combatTileTypeID at this layer, or null if this layer doesn't exist
	 */
	public String getCombatTileTypeForLayer (final MomCombatTile tile, final CombatMapLayerID layer);
	
	/**
	 * Checks to see if the specified layer exists already for the specified tile; if it does then it updates it, if it doesn't then it adds it
	 * 
	 * @param tile Tile to update
	 * @param layer Layer to update
	 * @param combatTileTypeID New tile type to use for that layer
	 */
	public void setCombatTileTypeForLayer (final MomCombatTile tile, final CombatMapLayerID layer, final String combatTileTypeID);
}

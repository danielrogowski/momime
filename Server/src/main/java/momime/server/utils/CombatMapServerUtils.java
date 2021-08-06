package momime.server.utils;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;

/**
 * Methods dealing with combat maps that are only needed on the server
 */
public interface CombatMapServerUtils
{
	/**
	 * Checks to see if the specified layer exists already for the specified tile; if it does then it updates it, if it doesn't then it adds it
	 * 
	 * @param tile Tile to update
	 * @param layer Layer to update
	 * @param combatTileTypeID New tile type to use for that layer
	 */
	public void setCombatTileTypeForLayer (final MomCombatTile tile, final CombatMapLayerID layer, final String combatTileTypeID);
		
	/**
	 * @param playerID Player whose units to count
	 * @param combatLocation Combat units must be in
	 * @param units List of units
	 * @return Number of alive units belonging to this player at this location
	 */
	public int countPlayersAliveUnitsAtCombatLocation (final int playerID, final MapCoordinates3DEx combatLocation, final List<MemoryUnit> units);

	/**
	 * @param combatLocation Location where the combat is taking place
	 * @param combatPosition Location of the unit within the combat map
	 * @param combatMap Combat scenery
	 * @param trueSpells True spell details held on server
	 * @param db Lookup lists built over the XML database
	 * @return Whether the specified location is within wall of fire (if there even is a wall of fire here)
	 */
	public boolean isWithinWallOfFire (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final MapAreaOfCombatTiles combatMap, final List<MemoryMaintainedSpell> trueSpells, final CommonDatabase db);
}
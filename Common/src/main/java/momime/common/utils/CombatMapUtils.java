package momime.common.utils;

import java.util.List;

import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomCombatTile;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

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
	
	/**
	 * Rechecks whether both sides in combat still have units left alive, and if so who the two players are.
	 * Delphi method used to be called RecheckCountsOfUnitsInCombat_MustLockUL, and returned a true/false for whether
	 * both values had been found.  Equivalent here is to call .bothFound () on the returned obj.
	 *  
	 * @param combatLocation Overland map coordinates where combat is taking place
	 * @param units List of known units
	 * @param players Players list
	 * @return Who the attacking and defending players are
	 * @throws PlayerNotFoundException If we determine the attacking or defending player ID, but that ID then can't be found in the players list
	 */
	public CombatPlayers determinePlayersInCombatFromLocation (final MapCoordinates3DEx combatLocation,
		final List<MemoryUnit> units, final List<? extends PlayerPublicDetails> players) throws PlayerNotFoundException;
	
	/**
	 * @param playerID Player whose units to count
	 * @param combatLocation Combat units must be in
	 * @param units List of units
	 * @return Number of alive units belonging to this player at this location
	 */
	public int countPlayersAliveUnitsAtCombatLocation (final int playerID, final MapCoordinates3DEx combatLocation, final List<MemoryUnit> units);
}

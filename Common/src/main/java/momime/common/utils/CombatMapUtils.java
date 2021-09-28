package momime.common.utils;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;

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
	 * @param combatLocation Location where the combat is taking place
	 * @param combatPosition Location of the unit within the combat map
	 * @param combatMap Combat scenery
	 * @param trueBuildings True list of buildings
	 * @param db Lookup lists built over the XML database
	 * @return Whether the specified location is within city walls (if there even are any)
	 */
	public boolean isWithinCityWalls (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final MapAreaOfCombatTiles combatMap, final List<MemoryBuilding> trueBuildings, final CommonDatabase db);
	
	/**
	 * @param combatLocation Location where the combat is taking place
	 * @param combatPosition Location of the unit within the combat map
	 * @param combatMap Combat scenery
	 * @param trueSpells True list of spells
	 * @param db Lookup lists built over the XML database
	 * @return Whether the specified location is within wall of darkness (if there even is a wall of darkness here)
	 */
	public boolean isWithinWallOfDarkness (final MapCoordinates3DEx combatLocation, final MapCoordinates2DEx combatPosition,
		final MapAreaOfCombatTiles combatMap, final List<MemoryMaintainedSpell> trueSpells, final CommonDatabase db);
}
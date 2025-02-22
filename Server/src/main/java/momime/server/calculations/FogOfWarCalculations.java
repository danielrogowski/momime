package momime.server.calculations;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CommonDatabase;
import momime.common.database.FogOfWarValue;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;

/**
 * Isolated methods used in fog of war routines by MomTrueMap
 * In the Delphi code most of these are on MomServerPlayer, but that doesn't make sense here since we have to pass in the pre-locked FOW area
 */
public interface FogOfWarCalculations
{
	/**
	 * This is used outside of updateAndSendFogOfWar () method, while the player's FOW area is set to the normal 3 state values indicating what the player can see.
	 *
	 * It adjusts fog of war values depending on the options chosen on the new game form in order to work out
	 * whether the player can see a value that's changed on the true map.
	 *
	 * Note this *doesn't* need to worry about "what if the area the player can see has changed since last turn" - those
	 * types of changes are dealt with by updateAndSendFogOfWar () - this only needs to deal with
	 * working out updates when the visible area remains the same, but the true values change
	 *
	 * @param state The visibility of a particular map cell
	 * @param setting FOW setting applicable for what we're testing whether we can see (e.g. use unit value to test if we can see a unit)
	 * @return Whether the player can see the updated value
	 */
	public boolean canSeeMidTurn (final FogOfWarStateID state, final FogOfWarValue setting);

	/**
	 * The FOW areas generated by updateAndSendFogOfWar don't resolve across towers - so if we can see a tower on Myrror, it doesn't mark the same location on plane 0 as visible
	 * When checking to see if we can see units at a location, we can see the units in a tower if we can see the tower on EITHER plane
	 *
	 * @param location Location to check
	 * @param setting FOW setting applicable for what we're testing whether we can see (e.g. use unit value to test if we can see a unit)
	 * @param trueTerrain True terrain map (we can't use memorized - the player might not be able to see whether it is a tower or not!)
	 * @param fogOfWarArea The areas of the map the player can and cannot see
	 * @param db Lookup lists built over the XML database
	 * @return If a tower, returns true if we can see the location on either plane; if not a tower, does a regular check
	 */
	public boolean canSeeMidTurnOnAnyPlaneIfTower (final MapCoordinates3DEx location, final FogOfWarValue setting,
		final MapVolumeOfMemoryGridCells trueTerrain, final MapVolumeOfFogOfWarStates fogOfWarArea, final CommonDatabase db);
}
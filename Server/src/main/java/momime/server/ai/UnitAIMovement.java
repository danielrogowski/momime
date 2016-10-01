package momime.server.ai;

import java.util.List;

import com.ndg.map.CoordinateSystem;

import momime.common.messages.MapVolumeOfMemoryGridCells;

/**
 * Provides a method for processing each movement code that the AI uses to decide where to send units overland.
 * 
 * Each of these methods must be able to operate in "test" mode, so we just test to see if the unit has something
 * to do using that code, without actually doing it.
 */
public interface UnitAIMovement
{
	/**
	 * AI tries to move units to any location that lacks defence or can be captured without a fight.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param underdefendedLocations Locations which are either ours (cities/towers) but lack enough defence, or not ours but can be freely captured (empty lairs/cities/etc)
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_Reinforce (final int [] [] [] doubleMovementDistances,
		final List<AIDefenceLocation> underdefendedLocations);

	/**
	 * AI tries to move units to scout any unknown terrain.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	public AIMovementDecision considerUnitMovement_ScoutAll (final int [] [] [] doubleMovementDistances,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys);
}
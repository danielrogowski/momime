package momime.client.process;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Methods dealing with the turn sequence and overland movement that are too big to leave in
 * message implementations, or are used multiple times. 
 */
public interface OverlandMapProcessing
{
	/**
	 * At the start of a turn, once all our movement has been reset and the server has sent any continuation moves to us, this gets called.
	 * It builds a list of units we need to give movement orders to i.e. all those units which have movement left and are not patrolling.
	 */
	public void buildUnitsLeftToMoveList ();
	
	/**
	 * Selects and centres the map on the next unit which we need to give a movement order to
	 */
	public void selectNextUnitToMoveOverland ();
	
	/**
	 * Sets the select unit boxes appropriately for the units we have in the specified cell
	 * @param unitLocation Location of the unit stack to move; null means we're moving nothing so just remove all old unit selection buttons
	 */
	public void showSelectUnitBoxes (final MapCoordinates3DEx unitLocation);

	/**
	 * To be able to build cities and perform other special orders, there are a number of checks we need to do
	 * @param unitLocation Location of the unit stack to move
	 */
	public void enableOrDisableSpecialOrderButtons (final MapCoordinates3DEx unitLocation);
	
	/**
	 * Updates the indicator for how much movement the current unit stack has left
	 */
	public void updateMovementRemaining ();

	/**
	 * @return Whether we're in the middle of the server processing and sending us pending moves
	 */
	public boolean isProcessingContinuedMovement ();
	
	/**
	 * @param cont Whether we're in the middle of the server processing and sending us pending moves
	 */
	public void setProcessingContinuedMovement (final boolean cont);
}
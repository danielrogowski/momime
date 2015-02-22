package momime.server.process;

import momime.common.messages.PendingMovement;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * In simultaneous turns games, movement is broken down and processed one cell at a time.
 * This class stores the details about a one cell pending movement that might be done next.
 */
public final class OneCellPendingMovement
{
	/** The player who owns the units that are trying to move */
	private final PlayerServerDetails unitStackOwner;
	
	/** Details of the full movement */
	private final PendingMovement pendingMovement;
	
	/** The first cell this unit stack will move to */
	private final MapCoordinates3DEx oneStep;
	
	/** Whether this move initiates a combat */
	private final boolean combatInitiated;

	/**
	 * @param aUnitStackOwner The player who owns the units that are trying to move
	 * @param aPendingMovement Details of the full movement
	 * @param aOneStep The first cell this unit stack will move to
	 * @param aCombatInitiated Whether this move initiates a combat
	 */
	public OneCellPendingMovement (final PlayerServerDetails aUnitStackOwner, final PendingMovement aPendingMovement,
		final MapCoordinates3DEx aOneStep, final boolean aCombatInitiated)
	{
		super ();
		unitStackOwner = aUnitStackOwner;
		pendingMovement = aPendingMovement;
		oneStep = aOneStep;
		combatInitiated = aCombatInitiated;
	}
	
	/**
	 * @return String representation of class values, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return "Player ID " + getUnitStackOwner ().getPlayerDescription ().getPlayerID () + ", " + getPendingMovement ().getUnitURN ().size () + " units, from " +
			getPendingMovement ().getMoveFrom () + " to " + getPendingMovement ().getMoveTo () + ", first step is " + getOneStep () + ", combat = " + isCombatInitiated ();
	}

	/**
	 * @return The player who owns the units that are trying to move
	 */
	public final PlayerServerDetails getUnitStackOwner ()
	{
		return unitStackOwner;
	}
	
	/**
	 * @return Details of the full movement
	 */
	public final PendingMovement getPendingMovement ()
	{
		return pendingMovement;
	}
	
	/**
	 * @return The first cell this unit stack will move to
	 */
	public final MapCoordinates3DEx getOneStep ()
	{
		return oneStep;
	}

	/**
	 * @return Whether this move initiates a combat
	 */
	public final boolean isCombatInitiated ()
	{
		return combatInitiated;
	}
}
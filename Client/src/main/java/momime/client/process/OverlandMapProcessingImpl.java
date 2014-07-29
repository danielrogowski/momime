package momime.client.process;

import java.util.ArrayList;
import java.util.List;

import momime.client.MomClient;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.PendingMovementUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Methods dealing with the turn sequence and overland movement that are too big to leave in
 * message implementations, or are used multiple times. 
 */
public final class OverlandMapProcessingImpl implements OverlandMapProcessing
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OverlandMapProcessingImpl.class);
	
	/** Ordered list of units that we have to give orders to this turn */
	private final List<MemoryUnit> unitsLeftToMoveOverland = new ArrayList<MemoryUnit> ();
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;

	/** Whether we're in the middle of the server processing and sending us pending moves */
	private boolean processingContinuedMovement;
	
	/** The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack */
	private MapCoordinates3DEx unitMoveFrom;
		
	/**
	 * At the start of a turn, once all our movement has been reset and the server has sent any continuation moves to us, this gets called.
	 * It builds a list of units we need to give movement orders to i.e. all those units which have movement left and are not patrolling.
	 */
	@Override
	public final void buildUnitsLeftToMoveList ()
	{
		log.trace ("Entering buildUnitsLeftToMoveList");
		
		// Rebuild the list
		unitsLeftToMoveOverland.clear ();
		for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
			if ((mu.getOwningPlayerID () == getClient ().getOurPlayerID ()) && (mu.getDoubleOverlandMovesLeft () > 0) &&
				(mu.getSpecialOrder () == null) && (mu.getStatus () == UnitStatusID.ALIVE) &&
				(getPendingMovementUtils ().findPendingMoveForUnit (getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement (), mu.getUnitURN ()) == null))
				
				unitsLeftToMoveOverland.add (mu);
		
		// On every other turn, prompt to move units in the reverse order
		if ((getClient ().getGeneralPublicKnowledge ().getTurnNumber () % 2 == 0) && (unitsLeftToMoveOverland.size () > 1))
		{
			final List<MemoryUnit> listCopy = new ArrayList<MemoryUnit> ();
			for (int n = 0; n < unitsLeftToMoveOverland.size (); n++)
				listCopy.add (unitsLeftToMoveOverland.get (unitsLeftToMoveOverland.size () - 1 - n));
			
			unitsLeftToMoveOverland.clear ();
			unitsLeftToMoveOverland.addAll (listCopy);
		}
		
		// Ask for movement orders for the first unit
		selectNextUnitToMoveOverland ();
		
		log.trace ("Exiting buildUnitsLeftToMoveList");
	}
	
	/**
	 * Selects and centres the map on the next unit which we need to give a movement order to
	 */
	@Override
	public final void selectNextUnitToMoveOverland ()
	{
		log.trace ("Entering selectNextUnitToMoveOverland");
		
		if (unitsLeftToMoveOverland.size () > 0)
		{
			final MemoryUnit unitToMove = unitsLeftToMoveOverland.get (0);
			
			// Select this unit stack
			showSelectUnitBoxes (new MapCoordinates3DEx ((MapCoordinates3DEx) unitToMove.getUnitLocation ()));
			
			// Shift the map to be centred on this location
		}
		else
		{
			// Get rid of any old select unit buttons, then use the regular routine to sort the panels and buttons out
			showSelectUnitBoxes (null);
		}
		
		log.trace ("Exiting selectNextUnitToMoveOverland");
	}

	/**
	 * Sets the select unit boxes appropriately for the units we have in the specified cell
	 * @param unitLocation Location of the unit stack to move; null means we're moving nothing so just remove all old unit selection buttons
	 */
	@Override
	public final void showSelectUnitBoxes (final MapCoordinates3DEx unitLocation)
	{
		log.trace ("Entering showSelectUnitBoxes: " + unitLocation);
		
		// Get rid of any old dynamically created buttons
		
		// Create new buttons
		
		// Enable or disable the special order buttons like build city, purify, etc.
		if (unitLocation != null)
			enableOrDisableSpecialOrderButtons (unitLocation);
		
		// Even if we auto selected zero units, we still have to set these, since the player might then decide to select one of the units
		// manually to move it, in which case we need to know where its moving from

        // This is the single only place unitMoveFrom is ever set
		unitMoveFrom = unitLocation;
		updateMovementRemaining ();
		
		log.trace ("Exiting showSelectUnitBoxes");
	}
	
	/**
	 * To be able to build cities and perform other special orders, there are a number of checks we need to do
	 * @param unitLocation Location of the unit stack to move
	 */
	@Override
	public final void enableOrDisableSpecialOrderButtons (final MapCoordinates3DEx unitLocation)
	{
		log.trace ("Entering enableOrDisableSpecialOrderButtons: " + unitLocation);
		log.trace ("Exiting enableOrDisableSpecialOrderButtons");
	}
	
	/**
	 * Updates the indicator for how much movement the current unit stack has left
	 */
	@Override
	public final void updateMovementRemaining ()
	{
		log.trace ("Entering updateMovementRemaining");
		log.trace ("Exiting updateMovementRemaining");
	}
	
	/**
	 * @return Whether we're in the middle of the server processing and sending us pending moves
	 */
	@Override
	public final boolean isProcessingContinuedMovement ()
	{
		return processingContinuedMovement;
	}
	
	/**
	 * @param cont Whether we're in the middle of the server processing and sending us pending moves
	 */
	@Override
	public final void setProcessingContinuedMovement (final boolean cont)
	{
		processingContinuedMovement = cont;
	}
	
	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
	}
}
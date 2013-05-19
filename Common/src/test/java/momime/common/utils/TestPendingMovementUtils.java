package momime.common.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import momime.common.messages.v0_9_4.PendingMovement;

import org.junit.Test;

/**
 * Tests the PendingMovementUtils class
 */
public final class TestPendingMovementUtils
{
	/**
	 * Tests the removeUnitFromAnyPendingMoves method
	 */
	@Test
	public final void testRemoveUnitFromAnyPendingMoves ()
	{
		// Build up test list
		final List<PendingMovement> moves = new ArrayList<PendingMovement> ();
		
		final String testData = "1,4,2 1,3,4 3,5 4 1,2";
		for (final String src : testData.split (" "))
		{
			final PendingMovement move = new PendingMovement ();
			for (final String unit : src.split (","))
				move.getUnitURN ().add (Integer.parseInt (unit));
			
			moves.add (move);
		}
		
		// Run method
		new PendingMovementUtils ().removeUnitFromAnyPendingMoves (moves, 4);
		
		// Check results
		assertEquals (4, moves.size ());
		
		assertEquals (1, moves.get (0).getUnitURN ().get (0).intValue ());
		assertEquals (2, moves.get (0).getUnitURN ().get (1).intValue ());

		assertEquals (1, moves.get (1).getUnitURN ().get (0).intValue ());
		assertEquals (3, moves.get (1).getUnitURN ().get (1).intValue ());

		assertEquals (3, moves.get (2).getUnitURN ().get (0).intValue ());
		assertEquals (5, moves.get (2).getUnitURN ().get (1).intValue ());
		
		assertEquals (1, moves.get (3).getUnitURN ().get (0).intValue ());
		assertEquals (2, moves.get (3).getUnitURN ().get (1).intValue ());
	}
}

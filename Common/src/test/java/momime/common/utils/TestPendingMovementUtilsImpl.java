package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import momime.common.messages.PendingMovement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the PendingMovementUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestPendingMovementUtilsImpl
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
		new PendingMovementUtilsImpl ().removeUnitFromAnyPendingMoves (moves, 4);
		
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
	
	/**
	 * Tests the removeAnyPendingMovesThatIncludeUnit method
	 */
	@Test
	public final void testRemoveAnyPendingMovesThatIncludeUnit ()
	{
		// Use same test list as above, to show the difference between the two methods
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
		new PendingMovementUtilsImpl ().removeAnyPendingMovesThatIncludeUnit (moves, 4);
		
		// Check results
		assertEquals (2, moves.size ());
		
		assertEquals (3, moves.get (0).getUnitURN ().get (0).intValue ());
		assertEquals (5, moves.get (0).getUnitURN ().get (1).intValue ());
		
		assertEquals (1, moves.get (1).getUnitURN ().get (0).intValue ());
		assertEquals (2, moves.get (1).getUnitURN ().get (1).intValue ());
	}
	
	/**
	 * Tests the findPendingMoveForUnit method
	 */
	@Test
	public final void testFindPendingMoveForUnit ()
	{
		// Set up test data
		final PendingMovement moveOne = new PendingMovement ();
		moveOne.getUnitURN ().add (2);
		moveOne.getUnitURN ().add (5);
		moveOne.getUnitURN ().add (8);
		
		final PendingMovement moveTwo = new PendingMovement ();
		moveTwo.getUnitURN ().add (4);
		moveTwo.getUnitURN ().add (1);
		moveTwo.getUnitURN ().add (7);

		final List<PendingMovement> moves = new ArrayList<PendingMovement> ();
		moves.add (moveOne);
		moves.add (moveTwo);
		
		// Set up object to test
		final PendingMovementUtilsImpl obj = new PendingMovementUtilsImpl ();
		
		// Run method
		assertNull (obj.findPendingMoveForUnit (moves, 3));
		assertSame (moveTwo, obj.findPendingMoveForUnit (moves, 1));
	}
}
package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the CombatTileUnitRelativeScaleGfx class
 */
public final class TestCombatTileUnitRelativeScaleGfx
{
	/**
	 * Tests the findFigureCount method to look for a figure count that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindFigureCount_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final CombatTileUnitRelativeScaleGfx scale = new CombatTileUnitRelativeScaleGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileFigurePositionsGfx positions = new CombatTileFigurePositionsGfx ();
			positions.setFigureCount (n);
			
			scale.getCombatTileFigurePositions ().add (positions);
		}
		
		scale.buildMap ();
		
		// Run tests
		assertEquals (2, scale.findFigureCount (2, "testFindFigureCount_Exists").getFigureCount ());
	}

	/**
	 * Tests the findFigureCount method to look for a figure count that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindFigureCount_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final CombatTileUnitRelativeScaleGfx scale = new CombatTileUnitRelativeScaleGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final CombatTileFigurePositionsGfx positions = new CombatTileFigurePositionsGfx ();
			positions.setFigureCount (n);
			
			scale.getCombatTileFigurePositions ().add (positions);
		}
		
		scale.buildMap ();
		
		// Run tests
		scale.findFigureCount (4, "testFindFigureCount_NotExists");
	}
}
package momime.client.graphics.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import momime.common.database.RecordNotFoundException;

import org.junit.jupiter.api.Test;

/**
 * Tests the CombatTileFigurePositionsGfx class
 */
public final class TestCombatTileFigurePositionsGfx
{
	/**
	 * Tests the findFigureNumber method to look for a figure number that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindFigureNumber_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final CombatTileFigurePositionsGfx positions = new CombatTileFigurePositionsGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final FigurePositionsForFigureCount figures = new FigurePositionsForFigureCount ();
			figures.setFigureNumber (n);
			figures.setTileRelativeX (-n);
			
			positions.getFigurePositionsForFigureCount ().add (figures);
		}
		
		positions.buildMap ();
		
		// Run tests
		assertEquals (-2, positions.findFigureNumber (2, "testFindFigureNumber_Exists").getTileRelativeX ());
	}

	/**
	 * Tests the findFigureNumber method to look for a figure number that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindFigureNumber_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final CombatTileFigurePositionsGfx positions = new CombatTileFigurePositionsGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final FigurePositionsForFigureCount figures = new FigurePositionsForFigureCount ();
			figures.setFigureNumber (n);
			figures.setTileRelativeX (-n);
			
			positions.getFigurePositionsForFigureCount ().add (figures);
		}
		
		positions.buildMap ();
		
		// Run tests
		positions.findFigureNumber (4, "testFindFigureNumber_NotExists");
	}
}
package momime.client.graphics.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.database.RecordNotFoundException;

/**
 * Tests the CombatTileFigurePositionsGfx class
 */
@ExtendWith(MockitoExtension.class)
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
	@Test
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
		assertThrows (RecordNotFoundException.class, () ->
		{
			positions.findFigureNumber (4, "testFindFigureNumber_NotExists");
		});
	}
}
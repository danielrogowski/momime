package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.CombatTileFigurePositions;
import momime.client.graphics.database.v0_9_5.FigurePositionsForFigureCount;
import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over the figure numbers, so we can look up their coordinates faster
 */
public final class CombatTileFigurePositionsEx extends CombatTileFigurePositions
{
	/** Map of figure numbers to figure positions */
	private Map<Integer, FigurePositionsForFigureCount> figureNumbersMap;

	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		figureNumbersMap = new HashMap<Integer, FigurePositionsForFigureCount> ();
		for (final FigurePositionsForFigureCount positions : getFigurePositionsForFigureCount ())
			figureNumbersMap.put (positions.getFigureNumber (), positions);
	}
	
	/**
	 * @param figureNumber Which figure number is being drawn
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Coordinates to draw the figure at
	 * @throws RecordNotFoundException If the figureNumber doesn't exist
	 */
	public final FigurePositionsForFigureCount findFigureNumber (final int figureNumber, final String caller) throws RecordNotFoundException
	{
		final FigurePositionsForFigureCount found = figureNumbersMap.get (figureNumber);

		if (found == null)
			throw new RecordNotFoundException (FigurePositionsForFigureCount.class, figureNumber, caller);
		
		return found;
	}
}
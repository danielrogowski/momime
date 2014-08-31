package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.CombatTileFigurePositions;
import momime.client.graphics.database.v0_9_5.CombatTileUnitRelativeScale;
import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over the figure counts, so we can look up their coordinates faster
 */
public final class CombatTileUnitRelativeScaleEx extends CombatTileUnitRelativeScale
{
	/** Map of figure counts to figure positions */
	private Map<Integer, CombatTileFigurePositionsEx> figureCountsMap;

	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		figureCountsMap = new HashMap<Integer, CombatTileFigurePositionsEx> ();
		for (final CombatTileFigurePositions positions : getCombatTileFigurePositions ())
		{
			final CombatTileFigurePositionsEx posex = (CombatTileFigurePositionsEx) positions;
			posex.buildMap ();
			figureCountsMap.put (posex.getFigureCount (), posex);
		}
	}
	
	/**
	 * @param figureCount Full number of units in the figure being drawn, before it takes any damage
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return List of the coordinates to draw each figure at
	 * @throws RecordNotFoundException If the figureCount doesn't exist
	 */
	public final CombatTileFigurePositionsEx findFigureCount (final int figureCount, final String caller) throws RecordNotFoundException
	{
		final CombatTileFigurePositionsEx found = figureCountsMap.get (figureCount);

		if (found == null)
			throw new RecordNotFoundException (CombatTileFigurePositions.class, figureCount, caller);
		
		return found;
	}
}
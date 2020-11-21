package momime.client.graphics.database;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over the figure counts, so we can look up their coordinates faster
 */
public final class CombatTileUnitRelativeScaleGfx extends CombatTileUnitRelativeScale
{
	/** Map of figure counts to figure positions */
	private Map<Integer, CombatTileFigurePositionsGfx> figureCountsMap;

	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		figureCountsMap = getCombatTileFigurePositionsGfx ().stream ().collect (Collectors.toMap (p -> p.getFigureCount (), p -> p));
		
		getCombatTileFigurePositionsGfx ().forEach (p -> p.buildMap ());
	}
	
	/**
	 * @return List of figure counts
	 */
	@SuppressWarnings ("unchecked")
	public final List<CombatTileFigurePositionsGfx> getCombatTileFigurePositionsGfx ()
	{
		return (List<CombatTileFigurePositionsGfx>) (List<?>) getCombatTileFigurePositions ();
	}
	
	/**
	 * @param figureCount Full number of units in the figure being drawn, before it takes any damage
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return List of the coordinates to draw each figure at
	 * @throws RecordNotFoundException If the figureCount doesn't exist
	 */
	public final CombatTileFigurePositionsGfx findFigureCount (final int figureCount, final String caller) throws RecordNotFoundException
	{
		final CombatTileFigurePositionsGfx found = figureCountsMap.get (figureCount);

		if (found == null)
			throw new RecordNotFoundException (CombatTileFigurePositions.class, figureCount, caller);
		
		return found;
	}
}
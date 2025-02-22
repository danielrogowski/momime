package momime.common.database;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adds a map over directions, so we can find their images faster
 */
public final class UnitCombatActionEx extends UnitCombatAction
{
	/** Map of directions to combat images */
	private Map<Integer, UnitCombatImage> directionsMap;

	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		directionsMap = getUnitCombatImage ().stream ().collect (Collectors.toMap (i -> i.getDirection (), i -> i));
	}

	/**
	 * @param direction Which direction to draw the unit facing
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Image/anim details to show this unit facing in the requested direction
	 * @throws RecordNotFoundException If the direction doesn't exist
	 */
	public final UnitCombatImage findDirection (final int direction, final String caller) throws RecordNotFoundException
	{
		final UnitCombatImage found = directionsMap.get (direction);

		if (found == null)
			throw new RecordNotFoundException (UnitCombatImage.class, direction, caller);

		return found;
	}
}
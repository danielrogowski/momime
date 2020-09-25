package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_9.UnitCombatAction;
import momime.client.graphics.database.v0_9_9.UnitCombatImage;
import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over directions, so we can find their images faster
 */
public final class UnitCombatActionGfx extends UnitCombatAction
{
	/** Map of directions to combat images */
	private Map<Integer, UnitCombatImageGfx> directionsMap;

	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		directionsMap = new HashMap<Integer, UnitCombatImageGfx> ();
		for (final UnitCombatImage image : getUnitCombatImage ())
			directionsMap.put (image.getDirection (), (UnitCombatImageGfx) image);
	}
	
	/**
	 * @param direction Which direction to draw the unit facing
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Image/anim details to show this unit facing in the requested direction
	 * @throws RecordNotFoundException If the direction doesn't exist
	 */
	public final UnitCombatImageGfx findDirection (final int direction, final String caller) throws RecordNotFoundException
	{
		final UnitCombatImageGfx found = directionsMap.get (direction);

		if (found == null)
			throw new RecordNotFoundException (UnitCombatImage.class, direction, caller);
		
		return found;
	}
}
package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.Unit;
import momime.client.graphics.database.v0_9_5.UnitCombatAction;
import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over combat actions, so we can find their images faster
 */
public final class UnitEx extends Unit
{
	/** Map of combat actions to combat images */
	private Map<String, UnitCombatActionEx> combatActionsMap;

	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		combatActionsMap = new HashMap<String, UnitCombatActionEx> ();
		for (final UnitCombatAction action : getUnitCombatAction ())
		{
			final UnitCombatActionEx actionEx = (UnitCombatActionEx) action;
			combatActionsMap.put (actionEx.getCombatActionID (), actionEx);
		}
	}
	
	/**
	 * @param combatActionID What action to draw the unit doing, e.g. STAND, WALK
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Set of combat images for this unit taking the requested action
	 * @throws RecordNotFoundException If the action doesn't exist
	 */
	public final UnitCombatActionEx findCombatAction (final String combatActionID, final String caller) throws RecordNotFoundException
	{
		final UnitCombatActionEx found = combatActionsMap.get (combatActionID);
	
		if (found == null)
			throw new RecordNotFoundException (UnitCombatAction.class, combatActionID, caller);
		
		return found;
	}
}
package momime.common.database;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Defines all the units available in the game
 */
public final class UnitEx extends Unit
{
	/** Map of hero name IDs to plane hero name objects */
	private Map<String, HeroName> heroNamesMap;

	/** Map of combat actions to combat images */
	private Map<String, UnitCombatActionEx> combatActionsMap;
	
	/**
	 * @return Typecasted list of combat actions
	 */
	@SuppressWarnings ("unchecked")
	public final List<UnitCombatActionEx> getUnitCombatActions ()
	{
		return (List<UnitCombatActionEx>) (List<?>) getUnitCombatAction ();
	}
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		// Build lower levels maps
		getUnitCombatActions ().forEach (a -> a.buildMap ());

		// Create maps
		heroNamesMap = getHeroName ().stream ().collect (Collectors.toMap (n -> n.getHeroNameID (), n -> n));
		combatActionsMap = getUnitCombatActions ().stream ().collect (Collectors.toMap (a -> a.getCombatActionID (), a -> a));
	}

	/**
	 * @param heroNameID Hero name to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Hero name object
	 * @throws RecordNotFoundException If the hero name doesn't exist
	 */
	public final HeroName findHeroName (final String heroNameID, final String caller) throws RecordNotFoundException
	{
		final HeroName found = heroNamesMap.get (heroNameID);
		if (found == null)
			throw new RecordNotFoundException (HeroName.class, heroNameID, caller);

		return found;
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
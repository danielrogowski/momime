package momime.common.utils;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;

/**
 * Methods dealing with checking whether we can see units or not
 */
public interface UnitVisibilityUtils
{
	/**
	 * Whether a unit can be seen *at all* in combat.  So this isn't simply asking whether it has the Invisibility skill and whether we have
	 * True Sight or Immunity to Illusions to negate it.  Even if a unit is invisible, we can still see it if we have one of our units adjacent to it.
	 * 
	 * So for a unit to be completely hidden in combat it must:
	 * 1) not be ours AND
	 * 2) be invisible (either natively, from Invisibility spell, or from Mass Invisible CAE) AND
	 * 3) we must have no unit with True Sight or Immunity to Illusions AND
	 * 4) we must have no unit adjacent to it
	 * 
	 * @param xu Unit present on the combat map
	 * @param ourPlayerID Our player ID
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return Whether we can see it or its completely hidden
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public boolean canSeeUnitInCombat (final ExpandedUnitDetails xu, final int ourPlayerID,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db,
		final CoordinateSystem combatMapCoordinateSystem)
		throws MomException, RecordNotFoundException, PlayerNotFoundException;
	
	/**
	 * Needed to test whether to draw units on the overland map.  Calling expandUnitDetails continually is too
	 * expensive so need a quicker way to check whether units are invisible or not.
	 * 
	 * @param mu Unit to test
	 * @param ourPlayerID Our player ID
	 * @param spells Known spells
	 * @param db Lookup lists built over the XML database
	 * @return Whether the unit should be visible on the overland map
	 */
	public boolean canSeeUnitOverland (final MemoryUnit mu, final int ourPlayerID, final List<MemoryMaintainedSpell> spells, final CommonDatabase db);
}
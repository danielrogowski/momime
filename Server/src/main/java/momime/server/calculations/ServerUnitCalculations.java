package momime.server.calculations;

import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Server only calculations pertaining to units, e.g. calculations relating to fog of war
 */
public interface ServerUnitCalculations
{
	/**
	 * @param unit The unit to check
	 * @param db Lookup lists built over the XML database
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int calculateUnitScoutingRange (final ExpandedUnitDetails unit, final CommonDatabase db) throws RecordNotFoundException, MomException;

	/**
	 * Non-magical ranged attack incurr a -10% to hit penalty for each 3 tiles distance between the attacking and defending unit on the combat map.
	 * This is loosely explained in the manual and strategy guide, but the info on the MoM wiki is clearer.
	 * 
	 * @param attacker Unit firing the ranged attack
	 * @param defender Unit being shot
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @return To hit penalty incurred from the distance between the attacker and defender, NB. this is not capped in any way so may get very high values here
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int calculateRangedAttackDistancePenalty (final ExpandedUnitDetails attacker, final ExpandedUnitDetails defender,
		final CombatMapSize combatMapCoordinateSystem) throws MomException;
	
	/**
	 * Gets a list of all the units a summoning spell might summon if we cast it.  That's straightforward for normal summoning spells, but heroes can only be
	 * hired once and if killed are never available to summon again.  Plus some heroes are restricted depending on what our spell book picks are.
	 * 
	 * @param spell Summoning spell
	 * @param wizardDetails Wizard casting the spell
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of units this spell might summon if we cast it; list can be empty if we're already summoned and killed all heroes for example
	 * @throws RecordNotFoundException If one of the summoned unit IDs can't be found in the DB
	 */
	public List<UnitEx> listUnitsSpellMightSummon (final Spell spell, final KnownWizardDetails wizardDetails, final List<MemoryUnit> trueUnits, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Similar to listUnitsSpellMightSummon, except lists all heroes who haven't been killed, and who we have the necessary spell book picks for. 
	 * 
	 * @param wizardDetails Wizard recruiting heroes
	 * @param trueUnits List of true units
	 * @param db Lookup lists built over the XML database
	 * @return List of heroes available to us
	 */
	public List<UnitEx> listHeroesForHire (final KnownWizardDetails wizardDetails, final List<MemoryUnit> trueUnits, final CommonDatabase db);
}
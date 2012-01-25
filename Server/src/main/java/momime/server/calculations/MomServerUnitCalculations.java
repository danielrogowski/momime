package momime.server.calculations;

import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.calculations.UnitHasSkillMergedList;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.ServerDatabaseValues;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server only calculations pertaining to units, e.g. calculations relating to fog of war
 */
public final class MomServerUnitCalculations
{
	/**
	 * @param unit The unit to check
	 * @param players Pre-locked players list
	 * @param spells Known spells (flight spell might increase scouting range)
	 * @param combatAreaEffects Known combat area effects (because theoretically, you could define a CAE which bumped up the scouting skill...)
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return How many squares this unit can see; by default = 1, flying units automatically get 2, and the Scouting unit skill can push this even higher
	 * @throws RecordNotFoundException If we can't find the player who owns the unit, or the unit has a skill that we can't find in the cache
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public static final int calculateUnitScoutingRange (final MemoryUnit unit, final List<PlayerServerDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final ServerDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (MomServerUnitCalculations.class.getName (), "calculateUnitScoutingRange",
			new String [] {new Integer (unit.getUnitURN ()).toString (), unit.getUnitID ()});

		int scoutingRange = 1;

		// Make sure we only bother to do this once
		final UnitHasSkillMergedList mergedSkills = UnitUtils.mergeSpellEffectsIntoSkillList (spells, unit, debugLogger);

		// Actual scouting skill
		scoutingRange = Math.max (scoutingRange, UnitUtils.getModifiedSkillValue
			(unit, mergedSkills, ServerDatabaseValues.VALUE_UNIT_SKILL_ID_SCOUTING, players, spells, combatAreaEffects, db, debugLogger));

		// Scouting range granted by other skills (i.e. flight skills)
		for (final UnitHasSkill thisSkill : mergedSkills)
		{
			final Integer unitSkillScoutingRange = db.findUnitSkill (thisSkill.getUnitSkillID (), "calculateUnitScoutingRange").getUnitSkillScoutingRange ();
			if (unitSkillScoutingRange != null)
				scoutingRange = Math.max (scoutingRange, unitSkillScoutingRange);
		}

		debugLogger.exiting (MomServerUnitCalculations.class.getName (), "calculateUnitScoutingRange", scoutingRange);
		return scoutingRange;
	}
}

package momime.common.utils;

import java.util.List;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;

/**
 * Calculates modified values over and above basic skill, attribute and upkeep values
 */
public interface UnitSkillUtils
{
	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param unitSkillID Unique identifier for this skill
	 * @param enemyUnits List of enemy units who may have skills that negate the skill we're checking for; typically this is the unit we're engaging in an attack with; in some
	 * 	cases such as Invisibility, it may be ALL units we're in combat with; for situations not involved in combats or specific attacks, just pass null here
	 * @param component Which component(s) making up this attribute to calculate
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer);
	 * 	Returns -1 if the unit doesn't have the skill
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit; or a bonus applies that we cannot determine the amount of
	 */
	public int getModifiedSkillValue (final AvailableUnit unit, final List<UnitSkillAndValue> skills, final String unitSkillID, final List<MemoryUnit> enemyUnits,
		final UnitSkillComponent component, final UnitSkillPositiveNegative positiveNegative, final String attackFromSkillID, final String attackFromMagicRealmID,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}
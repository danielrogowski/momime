package momime.common.utils;

import java.util.List;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitHasSkill;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;

/**
 * Calculates modified values over and above basic skill, attribute and upkeep values
 */
public interface UnitSkillUtils
{
	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param unitSkillID Unique identifier for this skill
	 * @param component Which component(s) making up this attribute to calculate
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer);
	 * 	Returns -1 if the unit doesn't have the skill
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit; or a bonus applies that we cannot determine the amount of
	 */
	public int getModifiedSkillValue (final AvailableUnit unit, final List<UnitHasSkill> skills, final String unitSkillID,
		final UnitSkillComponent component, final UnitSkillPositiveNegative positiveNegative, final List<? extends PlayerPublicDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * @param unit Unit to look up the base upkeep for
	 * @param productionTypeID Production type we want to look up the modified upkeep for
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
	 * @return Upkeep value, modified by reductions such as the Summoner retort reducing upkeep for summoned units; 0 if this unit has no upkeep of this type
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public int getModifiedUpkeepValue (final AvailableUnit unit, final String productionTypeID, final List<? extends PlayerPublicDetails> players,
		final CommonDatabase db)
		throws PlayerNotFoundException, RecordNotFoundException;
}
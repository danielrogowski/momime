package momime.common.utils;

import java.util.List;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.database.UnitHasSkill;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Calculates modified values over and above basic skill, attribute and upkeep values
 */
public interface UnitSkillUtils
{
	/**
	 * @param unit Unit we want to check
	 * @param skills List of skills the unit has, either just unit.getUnitHasSkill () or can pre-merge with spell skill list by calling mergeSpellEffectsIntoSkillList
	 * @param unitSkillID Unique identifier for this skill
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Value of the specified skill - base value can be improved by weapon grades, experience or CAEs (e.g. Node Auras or Prayer), or can be reduced by curses or enemy CAEs (e.g. Black Prayer); skills granted from spells currently always return zero but this is likely to change
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	public int getModifiedSkillValue (final AvailableUnit unit, final List<UnitHasSkill> skills, final String unitSkillID, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * NB. The reason there is no getBasicAttributeValue method is because this can be achieved by passing in MomUnitAttributeComponent.BASIC
	 * 
	 * @param unit Unit to calculate attribute value for
	 * @param unitAttributeID Unique identifier for this attribute
	 * @param component Which component(s) making up this attribute to calculate
	 * @param positiveNegative Whether to only include positive effects, only negative effects, or both
	 * @param players Players list
	 * @param spells Known spells
	 * @param combatAreaEffects Known combat area effects
	 * @param db Lookup lists built over the XML database
	 * @return Calculated unit attribute (e.g. swords/shields/hearts); 0 if attribute is N/A for this unit (unlike skills, which return -1)
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	public int getModifiedAttributeValue (final AvailableUnit unit, final String unitAttributeID, final UnitAttributeComponent component,
		final UnitAttributePositiveNegative positiveNegative, final List<? extends PlayerPublicDetails> players,
		final List<MemoryMaintainedSpell> spells, final List<MemoryCombatAreaEffect> combatAreaEffects, final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException;
	
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
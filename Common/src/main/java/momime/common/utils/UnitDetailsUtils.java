package momime.common.utils;

import java.util.List;
import java.util.Map;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;

/**
 * Methods for working out minimal unit details
 */
public interface UnitDetailsUtils
{
	/**
	 * Calculates minimal unit details that can be derived quickly without examining the whole unit stack.
	 * 
	 * @param unit Unit to expand skill list for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return List of all skills this unit has, with skills granted from other skills and skills granted from spells merged into the list
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public MinimalUnitDetails expandMinimalUnitDetails (final AvailableUnit unit,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Adds on a bonus to a skill value, if it passes suitable checks.  Assumes the main check, that this bonus applies as a whole, has already passed.
	 * So for example if this bonus is from another skill, e.g. +2 defence from Holy Armour, that we have already checked that the unit does actually have Holy Armour;
	 * or if it this bonus is from a weapon grade, that we have checked that the unit does actually have that weapon grade. 
	 * 
	 * @param mu Minimal details for the unit calculated so far
	 * @param unitSkillID If this bonus is being added because one skill gives a bonus to another skill, this is the skill the bonus is being granted FROM
	 * 	null if this bonus is being granted from something other than a skill, e.g. a weapon grade 
	 * @param addsToSkill The details of the skill the bonus is applied TO and any associated conditions
	 * @param overrideComponent Component to add these bonuses as; null means work it out based on whether its a + or - and whether it affects whole stack or not
	 * @param modifiedSkillValues Map of skill values calculated for the unit so far
	 * @param unitStackSkills List of all skills that any unit stacked with the unit we are calculating has; if a numeric skill then indicates the highest value from the stack
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Calculated lifeform type for this unit, e.g. regular unit, or it has been chaos channeled
	 * @return Value describing whether the bonus was added or not, and if not, why not - just used for unit test
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public AddSkillBonusResult addSkillBonus (final MinimalUnitDetails mu, final String unitSkillID, final AddsToSkill addsToSkill, final UnitSkillComponent overrideComponent,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues, final Map<String, Integer> unitStackSkills,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException;
	
	/**
	 * Adds on a penalty to a skill value, if it passes suitable checks.  Assumes the main check, that this penalty applies as a whole, has already passed.
	 * Strictly speaking, these are not so much split in terms of bonus+penalty as much as the type of modification done.  This is really just handling
	 * adjustments that must be done late in the calculation, so for example if one modification adds +2 to a value but another then locks it at 1,
	 * the locking must be done late otherwise we might end up with 3.  The addSkillBonus method is also used to add penalties like ADD_FIXED with value -2,
	 * they are just a different kind of penalty.
	 * 
	 * @param mu Minimal details for the unit calculated so far
	 * @param addsToSkill The details of the skill the bonus is applied TO and any associated conditions
	 * @param overrideComponent Component to add these bonuses as; null means work it out based on whether its a + or - and whether it affects whole stack or not
	 * @param modifiedSkillValues Map of skill values calculated for the unit so far
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Calculated lifeform type for this unit, e.g. regular unit, or it has been chaos channeled
	 * @return Value describing whether the penalty was added or not, and if not, why not - just used for unit test
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public AddSkillBonusResult addSkillPenalty (final MinimalUnitDetails mu, final AddsToSkill addsToSkill, final UnitSkillComponent overrideComponent,
		final Map<String, UnitSkillValueBreakdown> modifiedSkillValues,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException;
}
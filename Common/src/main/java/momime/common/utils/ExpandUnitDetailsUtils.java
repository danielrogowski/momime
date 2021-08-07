package momime.common.utils;

import java.util.Map;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.UnitSkillComponent;

/**
 * Sections broken out from the big expandUnitDetails method to make it more manageable
 */
public interface ExpandUnitDetailsUtils
{
	/**
	 * Adds on a bonus to a skill value, if it passes suitable checks.  Assumes the main check, that this bonus applies as a whole, has already passed.
	 * So for example if this bonus is from another skill, e.g. +2 defence from Holy Armour, that we have already checked that the unit does actually have Holy Armour;
	 * or if it this bonus is from a weapon grade, that we have checked that the unit does actually have that weapon grade. 
	 * 
	 * @param mu Minimal details for the unit calculated so far
	 * @param unitSkillID If this bonus is being added because one skill gives a bonus to another skill, this is the skill the bonus is being granted FROM
	 * 	null if this bonus is being granted from something other than a skill, e.g. a weapon grade 
	 * @param addsToSkill The details of the skill the bonus is applied TO and any associated conditions
	 * @param modifiedSkillValues Map of skill values calculated for the unit so far
	 * @param unitStackSkills List of all skills that any unit stacked with the unit we are calculating has; if a numeric skill then indicates the highest value from the stack
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Calculated lifeform type for this unit, e.g. regular unit, or it has been chaos channeled
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public void addSkillBonus (final MinimalUnitDetails mu, final String unitSkillID, final AddsToSkill addsToSkill,
		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues, final Map<String, Integer> unitStackSkills,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException;
}
package momime.common.utils;

import java.util.Map;
import java.util.Map.Entry;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.MomException;
import momime.common.database.AddsToSkill;
import momime.common.database.AddsToSkillValueType;
import momime.common.database.UnitSkillComponent;

/**
 * Sections broken out from the big expandUnitDetails method to make it more manageable
 */
public final class ExpandUnitDetailsUtilsImpl implements ExpandUnitDetailsUtils
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
	 * @param overrideComponent Component to add these bonuses as; null means work it out based on whether its a + or - and whether it affects whole stack or not
	 * @param modifiedSkillValues Map of skill values calculated for the unit so far
	 * @param unitStackSkills List of all skills that any unit stacked with the unit we are calculating has; if a numeric skill then indicates the highest value from the stack
	 * @param attackFromSkillID The skill ID of the incoming attack, e.g. bonus from Long Range only activates vs ranged attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param attackFromMagicRealmID The magic realm of the incoming attack, e.g. bonus from Bless only activates vs Death and Chaos-based attacks;
	 *		null will only count bonuses that apply regardless of the kind of attack being defended against
	 * @param magicRealmLifeformTypeID Calculated lifeform type for this unit, e.g. regular unit, or it has been chaos channeled
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void addSkillBonus (final MinimalUnitDetails mu, final String unitSkillID, final AddsToSkill addsToSkill, final UnitSkillComponent overrideComponent,
		final Map<String, Map<UnitSkillComponent, Integer>> modifiedSkillValues, final Map<String, Integer> unitStackSkills,
		final String attackFromSkillID, final String attackFromMagicRealmID, final String magicRealmLifeformTypeID)
		throws MomException
	{
		final Map<UnitSkillComponent, Integer> components = modifiedSkillValues.get (addsToSkill.getAddsToSkillID ());
		if ((components != null) && ((addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_FIXED) ||
			(addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_DIVISOR)))
		{
			final MapCoordinates3DEx unitCombatLocation = mu.isMemoryUnit () ? mu.getCombatLocation () : null;
			final boolean isInCombat = (unitCombatLocation != null);
			
			// If we have no info about the kind of attack being made, or this isn't in reference to an attack at all, then discount the bonus
			// if it has any restrictions that depend on the kind of incoming attack, even if we match those restrictions.
			// This is to stop the bonus from Large Shield showing on the unit info screen.
			if ((attackFromSkillID == null) && (attackFromMagicRealmID == null) &&
				((addsToSkill.getOnlyVersusAttacksFromSkillID () != null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID () != null)))
			{
				// Ignore
			}
			
			// If the bonus only applies in combat, and we aren't in combat, then ignore it.
			else if ((addsToSkill.isOnlyInCombat () != null) && (addsToSkill.isOnlyInCombat ()) && (!isInCombat))
			{
				// Ignore
			}
			
			// If the bonus only applies to specific magic realms then ignore it if doesn't match
			else if ((addsToSkill.getOnlyAppliesToMagicRealmID () != null) && (!addsToSkill.getOnlyAppliesToMagicRealmID ().equals (magicRealmLifeformTypeID)))
			{
				// Ignore
			}
			
			else
			{
				// Deal with negative checks first, so the "if" below doesn't get too complicated; the value here is irrelevant if onlyVersusAttacksFromSkillID is null
				// NB. attackFromSkillID of "null" (i.e. spell based attacks) are considered as not matching the required skill ID
				// which is what we want - so Large Shield DOES give its bonus against incoming spell attacks such as Fire Bolt
				boolean onlyVersusAttacksFromSkillIDCheckPasses = (addsToSkill.getOnlyVersusAttacksFromSkillID () != null) &&
					(addsToSkill.getOnlyVersusAttacksFromSkillID ().equals (attackFromSkillID));
				if ((addsToSkill.isNegateOnlyVersusAttacksFromSkillID () != null) && (addsToSkill.isNegateOnlyVersusAttacksFromSkillID ()))
					onlyVersusAttacksFromSkillIDCheckPasses = !onlyVersusAttacksFromSkillIDCheckPasses;
				
				// Does the skill only apply to particular ranged attack types, and incoming skill IDs or incoming attacks of only particular magic realms?
				// (This deals with all conditional bonuses, such as Resist Elements, Large Shield or Flame Blade)
				if (((addsToSkill.getRangedAttackTypeID () == null) || (addsToSkill.getRangedAttackTypeID ().equals (mu.getUnitDefinition ().getRangedAttackType ()))) &&
					((addsToSkill.getOnlyVersusAttacksFromSkillID () == null) || (onlyVersusAttacksFromSkillIDCheckPasses)) &&
					((addsToSkill.getOnlyVersusAttacksFromMagicRealmID () == null) || (addsToSkill.getOnlyVersusAttacksFromMagicRealmID ().equals (attackFromMagicRealmID))))
				{
					// How is the bonus calculated - fixed value, value from the skill, etc
					UnitSkillComponent component;
					if (overrideComponent != null)
						component = overrideComponent;
					else if ((addsToSkill.isPenaltyToEnemy () != null) && (addsToSkill.isPenaltyToEnemy ()))
						component = UnitSkillComponent.PENALTIES;
					else
						component = addsToSkill.isAffectsEntireStack () ? UnitSkillComponent.STACK : UnitSkillComponent.SPELL_EFFECTS;
					
					// Any fixed bonuses from one skill to another?  e.g. Holy Armour gives +2 to defence
					final int bonus;
					if ((addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_FIXED) && (addsToSkill.getAddsToSkillValue () != null))
						bonus = addsToSkill.getAddsToSkillValue ();
					
					// Divisors are only applicable when adding a value based on another skill
					else if (unitSkillID == null)
						bonus = 0;
					
					else
					{
						// For other kinds, we must have a value for the skill - e.g. Constitution 1 or Holy Bonus 2
						int multiplier;
						if (addsToSkill.isAffectsEntireStack ())
							multiplier = unitStackSkills.get (unitSkillID);
						else
						{
							multiplier = 0;
							final Map<UnitSkillComponent, Integer> totalComponents = modifiedSkillValues.get (unitSkillID);
							if (totalComponents != null)
								for (final Entry<UnitSkillComponent, Integer> c : totalComponents.entrySet ())
									if (c.getValue () == null)
										throw new MomException ("expandUnitDetails on " + mu.getUnitID () + " trying to sum addsFromSkill ID " + unitSkillID + " for bonuses but the " + c.getKey () + " component is null");
									else
										multiplier = multiplier + c.getValue ();
						}
						
						if (multiplier <= 0)
							bonus = 0;
						
						// Use already calculated highest value from the stack
						else if ((addsToSkill.isAffectsEntireStack ()) && (addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_DIVISOR) &&
							(addsToSkill.getAddsToSkillValue () != null))
						{
							component = UnitSkillComponent.STACK;
							
							// Multiplier has already been set to highest (level + 1) * (normal=2 or super=3) for highest value in the stack, see getBasicOrHeroSkillValue
							bonus = multiplier / (addsToSkill.getAddsToSkillValue () * 2);
						}
						
						// Any bonuses from hero skills? (Might gives +melee, Constitution gives +hit points, Agility gives +defence, and so on)
						else if ((addsToSkill.getAddsToSkillValueType () == AddsToSkillValueType.ADD_DIVISOR) && (addsToSkill.getAddsToSkillValue () != null))
						{
							component = UnitSkillComponent.HERO_SKILLS;
							if (mu.getModifiedExperienceLevel () != null)
							{
								// Multiplier will either equal 1 or 2, indicating whether we have the regular or super version of the skill - change this to be 2 for regular or 3 for super
								multiplier++;
								
								// Now can do the calculation
								bonus = ((mu.getModifiedExperienceLevel ().getLevelNumber () + 1) * multiplier) / (addsToSkill.getAddsToSkillValue () * 2);
							}
							else
								bonus = 0;
						}
						
						
						// Neither divisor nor fixed value specified, so the value must come from the skill itself
						else
							bonus = multiplier;
					}
					
					if (bonus != 0)
					{
						Integer bonusValue = components.get (component);
						bonusValue = ((bonusValue == null) ? 0 : bonusValue) + bonus;
						components.put (component, bonusValue);
					}
				}
			}
		}
	}
}
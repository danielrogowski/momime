package momime.server.process;

import momime.common.database.AttackResolutionStep;
import momime.common.database.UnitCombatSideID;
import momime.server.calculations.AttackDamage;

/**
 * Holds details about one step that must be executed in the process of an attack.
 * 
 * These get stored in a List<List<step>>, so the inner list is all steps that are processed simultaneously,
 * for example a unit making a regular attack and the enemy unit counterattacking, and at the end of the inner list
 * of steps, the damage from the step as a whole is actually applied to the units.  So that the enemy unit counterattacking
 * always does so at full strength, even if it took damage from the first hit.
 * 
 * The outer list-of-lists then breaks the steps down into those where damage is applied between each step,
 * for example a unit with first strike attacking an enemy unit has its damage applied before the enemy unit counterattacks.
 *
 * Each step can either be an attack from a unit skill (such as "regular melee attack" or "lightning breath" or "thrown weapons")
 * or an attack from a spell.  The database only models the first of these, so to get unit skills and spells intermingled
 * into the same lists requires this container class.
 */
public final class AttackResolutionStepContainer
{
	/** Details of step where an attack is made using a unit skill */
	private final AttackResolutionStep unitSkillStep;
	
	/** Details of step where an attack is made from a spell, cast either by a unit or the wizard */
	private final AttackDamage spellStep;
	
	/**
	 * @param aUnitSkillStep Details of step where an attack is made using a unit skill
	 */
	public AttackResolutionStepContainer (final AttackResolutionStep aUnitSkillStep)
	{
		unitSkillStep = aUnitSkillStep;
		spellStep = null;
	}

	/**
	 * @param aSpellStep Details of step where an attack is made from a spell, cast either by a unit or the wizard
	 */
	public AttackResolutionStepContainer (final AttackDamage aSpellStep)
	{
		unitSkillStep = null;
		spellStep = aSpellStep;
	}
	
	/**
	 * @return Which unit is making the attack (attacker = the unit that is making the attack, not necessarily who started the combat)
	 */
	public final UnitCombatSideID getCombatSide ()
	{
		return (getUnitSkillStep () != null) ? getUnitSkillStep ().getCombatSide () : UnitCombatSideID.ATTACKER;
	}

	/**
	 * @return Number of times this step gets executed
	 */
	public final int getRepetitions ()
	{
		return ((getUnitSkillStep () != null) && (getUnitSkillStep ().getRepetitions () != null)) ? getUnitSkillStep ().getRepetitions () : 1;
	}

	/**
	 * @return Details of step where an attack is made using a unit skill
	 */
	public final AttackResolutionStep getUnitSkillStep ()
	{
		return unitSkillStep;
	}

	/**
	 * @return Details of step where an attack is made from a spell, cast either by a unit or the wizard
	 */
	public final AttackDamage getSpellStep ()
	{
		return spellStep;
	}

	/**
	 * Needed to make matchers in unit tests work correctly
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean result;
		if (o instanceof AttackResolutionStepContainer)
		{
			final AttackResolutionStepContainer a = (AttackResolutionStepContainer) o;
			result = ((getUnitSkillStep () != null) && (getSpellStep () == null) && (a.getUnitSkillStep () != null) && (a.getSpellStep () == null) &&
					(getUnitSkillStep ().equals (a.getUnitSkillStep ()))) ||
					
				((getUnitSkillStep () == null) && (getSpellStep () != null) && (a.getUnitSkillStep () == null) && (a.getSpellStep () != null) &&
					(getSpellStep ().equals (a.getSpellStep ())));
		}
		else
			result = super.equals (o);
		
		return result;
	}
}
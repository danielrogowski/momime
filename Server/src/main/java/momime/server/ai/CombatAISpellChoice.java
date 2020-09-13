package momime.server.ai;

import momime.common.utils.ExpandedUnitDetails;
import momime.server.database.SpellSvr;

/**
 * One possible spell and target that the AI could potentially cast in combat.  These all get put in a list so it can then pick from the list.
 */
final class CombatAISpellChoice
{
	/** The spell to cast */
	private final SpellSvr spell;
	
	/** The unit to target the spell on, if it is aimed at a unit - could be ours or theirs, depending on the type of spell */
	private final ExpandedUnitDetails targetUnit;
	
	/** For spells that his multiple targets (e.g. Flame Strike or Mass Healing), how many suitable targets will be hit */ 
	private final Integer targetCount;
	
	/**
	 * @param aSpell The spell to cast
	 * @param aTargetUnit The unit to target the spell on, if it is aimed at a unit - could be ours or theirs, depending on the type of spell
	 * @param aTargetCount For spells that his multiple targets (e.g. Flame Strike or Mass Healing), how many suitable targets will be hit
	 */
	CombatAISpellChoice (final SpellSvr aSpell, final ExpandedUnitDetails aTargetUnit, final Integer aTargetCount)
	{
		spell = aSpell;
		targetUnit = aTargetUnit;
		targetCount = aTargetCount;
	}

	/**
	 * @return The spell to cast
	 */
	public final SpellSvr getSpell ()
	{
		return spell;
	}
	
	/**
	 * @return The unit to target the spell on, if it is aimed at a unit - could be ours or theirs, depending on the type of spell
	 */
	public final ExpandedUnitDetails getTargetUnit ()
	{
		return targetUnit;
	}

	/**
	 * @return For spells that his multiple targets (e.g. Flame Strike or Mass Healing), how many suitable targets will be hit
	 */ 
	public final Integer getTargetCount ()
	{
		return targetCount;
	}
}
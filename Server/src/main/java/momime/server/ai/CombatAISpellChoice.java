package momime.server.ai;

import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.utils.ExpandedUnitDetails;

/**
 * One possible spell and target that the AI could potentially cast in combat.  These all get put in a list so it can then pick from the list.
 */
final class CombatAISpellChoice
{
	/** The spell to cast */
	private final Spell spell;
	
	/** The unit to target the spell on, if it is aimed at a unit - could be ours or theirs, depending on the type of spell */
	private final ExpandedUnitDetails targetUnit;
	
	/** For spells that his multiple targets (e.g. Flame Strike or Mass Healing), how many suitable targets will be hit */ 
	private final Integer targetCount;
	
	/** For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null */
	private final Integer combatCastingFixedSpellNumber;
	
	/** For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null */
	private final Integer combatCastingSlotNumber;
	
	/**
	 * @param aSpell The spell to cast
	 * @param aTargetUnit The unit to target the spell on, if it is aimed at a unit - could be ours or theirs, depending on the type of spell
	 * @param aTargetCount For spells that his multiple targets (e.g. Flame Strike or Mass Healing), how many suitable targets will be hit
	 * @param aCombatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number
	 * @param aCombatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2)
	 */
	CombatAISpellChoice (final Spell aSpell, final ExpandedUnitDetails aTargetUnit, final Integer aTargetCount,
		final Integer aCombatCastingFixedSpellNumber, final Integer aCombatCastingSlotNumber) 
	{
		spell = aSpell;
		targetUnit = aTargetUnit;
		targetCount = aTargetCount;
		combatCastingFixedSpellNumber = aCombatCastingFixedSpellNumber;
		combatCastingSlotNumber = aCombatCastingSlotNumber;
	}
	
	/**
	 * @return Estimate of how useful this spell will be
	 */
	public final int getWeighting ()
	{
		int value;

		// Rate multiple target spells according to how many targets they have (2..18)
		if (targetCount != null)
			value = targetCount * 2;
		
		// Rate other kinds of spells according to their type
		else if (spell.getSpellBookSectionID () == SpellBookSectionID.COMBAT_ENCHANTMENTS)
			value = 7;

		else if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
			value = 5;
		
		else
			value = 3;

		// Assume more expensive spells are better.
		// Note multiplier here is purposefully small so really expensive spells will bump over the initial value ratings from above and move themselves higher up the list.
		value = (value * 20) + (spell.getCombatCastingCost () == null ? 0 : spell.getCombatCastingCost ());
		
		// Also pay more attention to heroes (both ours and theirs)
		if ((getTargetUnit () != null) && (getTargetUnit ().isHero ()))
			value = value + 25;
		
		return value;
	}

	/**
	 * @return The spell to cast
	 */
	public final Spell getSpell ()
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

	/**
	 * @return For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 */
	public final Integer getCombatCastingFixedSpellNumber ()
	{
		return combatCastingFixedSpellNumber;
	}
	
	/**
	 * @return For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 */
	public final Integer getCombatCastingSlotNumber ()
	{
		return combatCastingSlotNumber;
	}
}
package momime.server.ai;

import momime.common.messages.MemoryUnit;

/**
 * Stores a link to a unit together with its combat AI ordering, so that we don't have to keep recalculating it
 */
final class MemoryUnitAndCombatAIOrder implements Comparable<MemoryUnitAndCombatAIOrder>
{
	/** Unit being positioned into combat */
	private final MemoryUnit unit;
	
	/** Ordering that AI will use units in combat (see notes in CombatAIImpl) */
	private final int combatAIOrder;
	
	/**
	 * @param aUnit Unit being positioned into combat
	 * @param aCombatAIOrder Ordering that AI will use units in combat (see notes in CombatAIImpl)
	 */
	MemoryUnitAndCombatAIOrder (final MemoryUnit aUnit, final int aCombatAIOrder)
	{
		unit = aUnit;
		combatAIOrder = aCombatAIOrder;
	}

	/**
	 * @return Value to sort units by 'combat AI order'
	 */
	@Override
	public final int compareTo (final MemoryUnitAndCombatAIOrder o)
	{
		return getCombatAIOrder () - o.getCombatAIOrder ();
	}

	/**
	 * @return Unit being positioned into combat
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return Ordering that AI will use units in combat (see notes in CombatAIImpl)
	 */
	public final int getCombatAIOrder ()
	{
		return combatAIOrder;
	}
}
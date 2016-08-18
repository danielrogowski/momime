package momime.server.process;

import momime.common.utils.ExpandedUnitDetails;

/**
 * Stores a link to a unit together with its combat class, so that we don't have to keep recalculating it
 */
final class MemoryUnitAndCombatClass implements Comparable<MemoryUnitAndCombatClass>
{
	/** Unit being positioned into combat */
	private final ExpandedUnitDetails unit;
	
	/** Combat class (see notes in CombatProcessingImpl) */
	private final int combatClass;
	
	/**
	 * @param aUnit Unit being positioned into combat
	 * @param aCombatClass Combat class (see notes in CombatProcessingImpl)
	 */
	MemoryUnitAndCombatClass (final ExpandedUnitDetails aUnit, final int aCombatClass)
	{
		unit = aUnit;
		combatClass = aCombatClass;
	}

	/**
	 * @return Value to sort units by 'combat class'
	 */
	@Override
	public final int compareTo (final MemoryUnitAndCombatClass o)
	{
		return getCombatClass () - o.getCombatClass ();
	}

	/**
	 * @return Unit being positioned into combat
	 */
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return Combat class (see notes in CombatProcessingImpl)
	 */
	public final int getCombatClass ()
	{
		return combatClass;
	}
}
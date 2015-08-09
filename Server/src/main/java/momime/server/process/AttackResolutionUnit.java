package momime.server.process;

import momime.common.messages.MemoryUnit;

/**
 * Some special attributes need to be tracked from one attack resolution step to the next, so the units involved
 * are both wrapped in this object to keep track of any other associated details.
 * 
 * One the attack resolution is resolved between the two units involved, this wrapper is discarded.
 */
public final class AttackResolutionUnit
{
	/** The unit invovled in the attack */
	private final MemoryUnit unit;
	
	/** The number of figures in this unit that are frozen in fear and hence cannot initiate any further attacks */ 
	private int figuresFrozenInFear;
	
	/**
	 * @param aUnit The unit invovled in the attack
	 */
	public AttackResolutionUnit (final MemoryUnit aUnit)
	{
		unit = aUnit;
	}

	/**
	 * @return The unit invovled in the attack
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return The number of figures in this unit that are frozen in fear and hence cannot initiate any further attacks
	 */ 
	public final int getFiguresFrozenInFear ()
	{
		return figuresFrozenInFear;
	}

	/**
	 * @param figureCount The number of figures in this unit that are frozen in fear and hence cannot initiate any further attacks
	 */ 
	public final void setFiguresFrozenInFear (final int figureCount)
	{
		figuresFrozenInFear = figureCount;
	}

	/**
	 * Needed to make matchers in unit tests work correctly
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean result;
		if (o instanceof AttackResolutionUnit)
		{
			final AttackResolutionUnit a = (AttackResolutionUnit) o; 
			result = (getUnit ().equals (a.getUnit ())) && (getFiguresFrozenInFear () == a.getFiguresFrozenInFear ());
		}
		else
			result = super.equals (o);
		
		return result;
	}
}
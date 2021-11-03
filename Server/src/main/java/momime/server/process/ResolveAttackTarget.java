package momime.server.process;

import momime.common.database.Spell;
import momime.common.messages.MemoryUnit;

/**
 * Stores details about one defender being attacked in a call to resolveAttack
 */
public final class ResolveAttackTarget
{
	/** Unit being attacked */
	private final MemoryUnit defender;
	
	/** Overrides the default spell being used to attack the units */
	private Spell spellOverride;
	
	/**
	 * @param aDefender Unit being attacked
	 */
	public ResolveAttackTarget (final MemoryUnit aDefender)
	{
		defender = aDefender;
	}

	/** 
	 * @return Unit being attacked
	 */
	public final MemoryUnit getDefender ()
	{
		return defender;
	}

	/**
	 * @return Overrides the default spell being used to attack the units
	 */
	public final Spell getSpellOverride ()
	{
		return spellOverride;
	}

	/**
	 * @param s Overrides the default spell being used to attack the units
	 */
	public final void setSpellOverride (final Spell s)
	{
		spellOverride = s;
	}
	
	/**
	 * Needed to make matchers in unit tests work correctly
	 */
	@Override
	public final boolean equals (final Object o)
	{
		final boolean result;
		if (o instanceof ResolveAttackTarget)
		{
			final ResolveAttackTarget a = (ResolveAttackTarget) o; 
			result = (getDefender ().equals (a.getDefender ())) &&
				(((getSpellOverride () == null) && (a.getSpellOverride () == null)) ||
					((getSpellOverride () != null) && (a.getSpellOverride () != null) && (getSpellOverride ().equals (a.getSpellOverride ()))));
		}
		else
			result = super.equals (o);
		
		return result;
	}
}
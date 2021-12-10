package momime.server.process;

/**
 * Structure to hold return values from calling resolveAttack
 */
public final class ResolveAttackResult
{
	/** Whether the attack wiped out one side in a combat and ended it (combatEnded will already have been called) */
	private final boolean combatEnded;
	
	/** Number of units belonging to the attacking player that were killed */
	private final int attackingPlayerUnitsKilled;
	
	/** Number of units belongong to the defending player that were killed */
	private final int defendingPlayerUnitsKilled;
	
	/**
	 * @param aCombatEnded Whether the attack wiped out one side in a combat and ended it (combatEnded will already have been called)
	 * @param anAttackingPlayerUnitsKilled Number of units belonging to the attacking player that were killed
	 * @param aDefendingPlayerUnitsKilled Number of units belongong to the defending player that were killed
	 */
	ResolveAttackResult (final boolean aCombatEnded, final int anAttackingPlayerUnitsKilled, final int aDefendingPlayerUnitsKilled)
	{
		combatEnded = aCombatEnded;
		attackingPlayerUnitsKilled = anAttackingPlayerUnitsKilled;
		defendingPlayerUnitsKilled = aDefendingPlayerUnitsKilled;
	}

	/**
	 * @return Whether the attack wiped out one side in a combat and ended it (combatEnded will already have been called)
	 */
	public final boolean isCombatEnded ()
	{
		return combatEnded;
	}
	
	/**
	 * @return Number of units belonging to the attacking player that were killed
	 */
	public final int getAttackingPlayerUnitsKilled ()
	{
		return attackingPlayerUnitsKilled;
	}
	
	/**
	 * @return Number of units belongong to the defending player that were killed
	 */
	public final int getDefendingPlayerUnitsKilled ()
	{
		return defendingPlayerUnitsKilled;
	}
}
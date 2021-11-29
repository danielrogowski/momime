package momime.server.worldupdates;

/**
 * Interface to allow creating world update objects with spring dependencies injected
 */
public interface WorldUpdateFactory
{
	/**
	 * @return New object with spring dependencies injected
	 */
	public KillUnitUpdate createKillUnitUpdate ();
	
	/**
	 * @return New object with spring dependencies injected
	 */
	public RemoveCombatAreaEffectUpdate createRemoveCombatAreaEffectUpdate ();
	
	/**
	 * @return New object with spring dependencies injected
	 */
	public SwitchOffSpellUpdate createSwitchOffSpellUpdate ();
	
	/**
	 * @return New object with spring dependencies injected
	 */
	public RecheckTransportCapacityUpdate createRecheckTransportCapacityUpdate ();
	
	/**
	 * @return New object with spring dependencies injected
	 */
	public RecalculateCityUpdate createRecalculateCityUpdate ();
	
	/**
	 * @return New object with spring dependencies injected
	 */
	public RecalculateProductionUpdate createRecalculateProductionUpdate ();
}
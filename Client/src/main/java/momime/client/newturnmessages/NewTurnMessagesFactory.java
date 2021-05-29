package momime.client.newturnmessages;

/**
 * All of the NTMs need things like the font and language file injected, so need to be created from prototypes defined in spring
 */
public interface NewTurnMessagesFactory
{
	/**
	 * @return NTM cateogry with injected dependencies
	 */
	public NewTurnMessageCategory createNewTurnMessageCategory ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageSpellSwitchedOffFromLackOfProductionEx createNewTurnMessageSpellSwitchedOffFromLackOfProduction ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageBuildingSoldFromLackOfProductionEx createNewTurnMessageBuildingSoldFromLackOfProduction ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageConstructBuildingEx createNewTurnMessageConstructBuilding ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageConstructUnitEx createNewTurnMessageConstructUnit ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageCreateArtifactEx createNewTurnMessageCreateArtifact ();
	
	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageSummonUnitEx createNewTurnMessageSummonUnit ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessagePopulationChangeEx createNewTurnMessagePopulationChange ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageUnitKilledFromLackOfProductionEx createNewTurnMessageUnitKilledFromLackOfProduction ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageSpellEx createNewTurnMessageSpell ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageNodeEx createNewTurnMessageNode ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageHeroGainedALevelEx createNewTurnMessageHeroGainedALevel ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageOfferHeroEx createNewTurnMessageOfferHero ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageOfferUnitsEx createNewTurnMessageOfferUnits ();

	/**
	 * @return NTM with injected dependencies
	 */
	public NewTurnMessageOfferItemEx createNewTurnMessageOfferItem ();
}
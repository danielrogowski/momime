package momime.client.messages.process;

import momime.client.calculations.damage.DamageCalculationAttackDataEx;
import momime.client.calculations.damage.DamageCalculationDefenceDataEx;
import momime.client.calculations.damage.DamageCalculationHeaderDataEx;

/**
 * Factory interface for creating every possible type of server to client message
 */
public interface ServerToClientMessagesFactory
{
	/**
	 * @return Newly created message
	 */
	public UpdateRemainingResearchCostMessageImpl createUpdateRemainingResearchCostMessage ();

	/**
	 * @return Newly created message
	 */
	public SelectNextUnitToMoveOverlandMessageImpl createSelectNextUnitToMoveOverlandMessage ();

	/**
	 * @return Newly created message
	 */
	public TextPopupMessageImpl createTextPopupMessage ();

	/**
	 * @return Newly created message
	 */
	public NewGameDatabaseMessageImpl createNewGameDatabaseMessage ();

	/**
	 * @return Newly created message
	 */
	public StartCombatMessageImpl createStartCombatMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateCombatMapMessageImpl createUpdateCombatMapMessage ();

	/**
	 * @return Newly created message
	 */
	public PendingMovementMessageImpl createPendingMovementMessage ();

	/**
	 * @return Newly created message
	 */
	public FullSpellListMessageImpl createFullSpellListMessage ();

	/**
	 * @return Newly created message
	 */
	public FogOfWarVisibleAreaChangedMessageImpl createFogOfWarVisibleAreaChangedMessage ();

	/**
	 * @return Newly created message
	 */
	public StartGameMessageImpl createStartGameMessage ();

	/**
	 * @return Newly created message
	 */
	public ErasePendingMovementsMessageImpl createErasePendingMovementsMessage ();

	/**
	 * @return Newly created message
	 */
	public PendingSaleMessageImpl createPendingSaleMessage ();

	/**
	 * @return Newly created message
	 */
	public DamageCalculationMessageImpl createDamageCalculationMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateGlobalEconomyMessageImpl createUpdateGlobalEconomyMessage ();

	/**
	 * @return Newly created message
	 */
	public StartSimultaneousTurnMessageImpl createStartSimultaneousTurnMessage ();

	/**
	 * @return Newly created message
	 */
	public AddNewTurnMessagesMessageImpl createAddNewTurnMessagesMessage ();

	/**
	 * @return Newly created message
	 */
	public TaxRateChangedMessageImpl createTaxRateChangedMessage ();

	/**
	 * @return Newly created message
	 */
	public MoveUnitStackOverlandMessageImpl createMoveUnitStackOverlandMessage ();

	/**
	 * @return Newly created message
	 */
	public YourPhotoIsOkMessageImpl createYourPhotoIsOkMessage ();

	/**
	 * @return Newly created message
	 */
	public AddOrUpdateCombatAreaEffectMessageImpl createAddOrUpdateCombatAreaEffectMessage ();

	/**
	 * @return Newly created message
	 */
	public AddOrUpdateMaintainedSpellMessageImpl createAddOrUpdateMaintainedSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateCityMessageImpl createUpdateCityMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseInitialSpellsNowMessageImpl createChooseInitialSpellsNowMessage ();

	/**
	 * @return Newly created message
	 */
	public ChosenStandardPhotoMessageImpl createChosenStandardPhotoMessage ();

	/**
	 * @return Newly created message
	 */
	public ReplacePicksMessageImpl createReplacePicksMessage ();

	/**
	 * @return Newly created message
	 */
	public SetCombatPlayerMessageImpl createSetCombatPlayerMessage ();

	/**
	 * @return Newly created message
	 */
	public KillUnitMessageImpl createKillUnitMessage ();

	/**
	 * @return Newly created message
	 */
	public ChosenCustomPhotoMessageImpl createChosenCustomPhotoMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseYourRaceNowMessageImpl createChooseYourRaceNowMessage ();

	/**
	 * @return Newly created message
	 */
	public YourRaceIsOkMessageImpl createYourRaceIsOkMessage ();

	/**
	 * @return Newly created message
	 */
	public ApplyDamageMessageImpl createApplyDamageMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateTerrainMessageImpl createUpdateTerrainMessage ();

	/**
	 * @return Newly created message
	 */
	public OverlandCastQueuedMessageImpl createOverlandCastQueuedMessage ();

	/**
	 * @return Newly created message
	 */
	public AddOrUpdateUnitMessageImpl createAddOrUpdateUnitMessage ();

	/**
	 * @return Newly created message
	 */
	public AddBuildingMessageImpl createAddBuildingMessage ();

	/**
	 * @return Newly created message
	 */
	public DestroyBuildingMessageImpl createDestroyBuildingMessage ();

	/**
	 * @return Newly created message
	 */
	public EndOfContinuedMovementMessageImpl createEndOfContinuedMovementMessage ();

	/**
	 * @return Newly created message
	 */
	public SetUnitIntoOrTakeUnitOutOfCombatMessageImpl createSetUnitIntoOrTakeUnitOutOfCombatMessage ();

	/**
	 * @return Newly created message
	 */
	public SpellResearchChangedMessageImpl createSpellResearchChangedMessage ();

	/**
	 * @return Newly created message
	 */
	public SwitchOffMaintainedSpellMessageImpl createSwitchOffMaintainedSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public RemoveQueuedSpellMessageImpl createRemoveQueuedSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public MoveUnitInCombatMessageImpl createMoveUnitInCombatMessage ();

	/**
	 * @return Newly created message
	 */
	public CombatEndedMessageImpl createCombatEndedMessage ();

	/**
	 * @return Newly created message
	 */
	public ChosenWizardMessageImpl createChosenWizardMessage ();

	/**
	 * @return Newly created message
	 */
	public OnePlayerSimultaneousTurnDoneMessageImpl createOnePlayerSimultaneousTurnDoneMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateManaSpentOnCastingCurrentSpellMessageImpl createUpdateManaSpentOnCastingCurrentSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public BroadcastChatMessageImpl createBroadcastChatMessage ();

	/**
	 * @return Newly created message
	 */
	public SetCurrentPlayerMessageImpl createSetCurrentPlayerMessage ();

	/**
	 * @return Newly created message
	 */
	public CancelCombatAreaEffectMessageImpl createCancelCombatAreaEffectMessage ();

	/**
	 * @return Newly created message
	 */
	public AskForCaptureCityDecisionMessageImpl createAskForCaptureCityDecisionMessage ();
	
	/**
	 * @return Newly created message
	 */
	public AddUnassignedHeroItemMessageImpl createAddUnassignedHeroItemMessage ();

	/**
	 * @return Newly created message
	 */
	public RemoveUnassignedHeroItemMessageImpl createRemoveUnassignedHeroItemMessage ();
	
	/**
	 * @return Damage breakdown line with with injected dependencies
	 */
	public DamageCalculationHeaderDataEx createDamageCalculationHeaderData ();

	/**
	 * @return Damage breakdown line with with injected dependencies
	 */
	public DamageCalculationAttackDataEx createDamageCalculationAttackData ();

	/**
	 * @return Damage breakdown line with with injected dependencies
	 */
	public DamageCalculationDefenceDataEx createDamageCalculationDefenceData ();
	
	/**
	 * @return Newly created message
	 */
	public TreasureRewardMessageImpl createTreasureRewardMessage ();
	
	/**
	 * @return Newly created message
	 */
	public DispelMagicResultsMessageImpl createDispelMagicResultsMessage ();
	
	/**
	 * @return Newly created message
	 */
	public ShowSpellAnimationMessageImpl createShowSpellAnimationMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateWizardStateMessageImpl createUpdateWizardStateMessage ();

	/**
	 * @return Newly created message
	 */
	public AddPowerBaseHistoryMessageImpl createAddPowerBaseHistoryMessage ();

	/**
	 * @return Newly created message
	 */
	public PlayAnimationMessageImpl createPlayAnimationMessage ();
}
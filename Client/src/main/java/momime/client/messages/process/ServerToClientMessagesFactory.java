package momime.client.messages.process;

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
	public UpdateNodeLairTowerUnitIDMessageImpl createUpdateNodeLairTowerUnitIDMessage ();

	/**
	 * @return Newly created message
	 */
	public StartCombatMessageImpl createStartCombatMessage ();

	/**
	 * @return Newly created message
	 */
	public PendingMovementMessageImpl createPendingMovementMessage ();

	/**
	 * @return Newly created message
	 */
	public PlayerCombatRequestStatusMessageImpl createPlayerCombatRequestStatusMessage ();

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
	public StartGameProgressMessageImpl createStartGameProgressMessage ();

	/**
	 * @return Newly created message
	 */
	public PendingSaleMessageImpl createPendingSaleMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateOverlandMovementRemainingMessageImpl createUpdateOverlandMovementRemainingMessage ();

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
	public ScheduledCombatWalkInWithoutAFightMessageImpl createScheduledCombatWalkInWithoutAFightMessage ();

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
	public FoundLairNodeTowerMessageImpl createFoundLairNodeTowerMessage ();

	/**
	 * @return Newly created message
	 */
	public AddCombatAreaEffectMessageImpl createAddCombatAreaEffectMessage ();

	/**
	 * @return Newly created message
	 */
	public AddMaintainedSpellMessageImpl createAddMaintainedSpellMessage ();

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
	public ShowListAndOtherScheduledCombatsMessageImpl createShowListAndOtherScheduledCombatsMessage ();

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
	public SetSpecialOrderMessageImpl createSetSpecialOrderMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseYourRaceNowMessageImpl createChooseYourRaceNowMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateOtherScheduledCombatsMessageImpl createUpdateOtherScheduledCombatsMessage ();

	/**
	 * @return Newly created message
	 */
	public YourRaceIsOkMessageImpl createYourRaceIsOkMessage ();

	/**
	 * @return Newly created message
	 */
	public AddScheduledCombatMessageImpl createAddScheduledCombatMessage ();

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
	public UpdateDamageTakenAndExperienceMessageImpl createUpdateDamageTakenAndExperienceMessage ();

	/**
	 * @return Newly created message
	 */
	public AddUnitMessageImpl createAddUnitMessage ();

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
	public UpdateUnitNameMessageImpl createUpdateUnitNameMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateProductionSoFarMessageImpl createUpdateProductionSoFarMessage ();

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
	public OverlandMovementTypesMessageImpl createOverlandMovementTypesMessage ();

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
	public UpdateUnitToAliveMessageImpl createUpdateUnitToAliveMessage ();
}

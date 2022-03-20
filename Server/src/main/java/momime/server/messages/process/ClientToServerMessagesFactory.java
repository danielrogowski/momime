package momime.server.messages.process;

/**
 * Factory interface for creating every possible type of client to server message
 */
public interface ClientToServerMessagesFactory
{
	/**
	 * @return Newly created message
	 */
	public ChooseInitialSpellsMessageImpl createChooseInitialSpellsMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseCustomFlagColourMessageImpl createChooseCustomFlagColourMessage ();

	/**
	 * @return Newly created message
	 */
	public NextTurnButtonMessageImpl createNextTurnButtonMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestMoveOverlandUnitStackMessageImpl createRequestMoveOverlandUnitStackMessage ();

	/**
	 * @return Newly created message
	 */
	public AlchemyMessageImpl createAlchemyMessage ();

	/**
	 * @return Newly created message
	 */
	public SellBuildingMessageImpl createSellBuildingMessage ();

	/**
	 * @return Newly created message
	 */
	public UpdateMagicPowerDistributionMessageImpl createUpdateMagicPowerDistributionMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseCustomPicksMessageImpl createChooseCustomPicksMessage ();

	/**
	 * @return Newly created message
	 */
	public SpecialOrderButtonMessageImpl createSpecialOrderButtonMessage ();

	/**
	 * @return Newly created message
	 */
	public CaptureCityDecisionMessageImpl createCaptureCityDecisionMessage ();

	/**
	 * @return Newly created message
	 */
	public ChatMessageImpl createChatMessage ();

	/**
	 * @return Newly created message
	 */
	public RushBuyMessageImpl createRushBuyMessage ();

	/**
	 * @return Newly created message
	 */
	public DismissUnitMessageImpl createDismissUnitMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseRaceMessageImpl createChooseRaceMessage ();

	/**
	 * @return Newly created message
	 */
	public ChangeTaxRateMessageImpl createChangeTaxRateMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestUpdateUnitNameMessageImpl createRequestUpdateUnitNameMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseCityNameMessageImpl createChooseCityNameMessage ();

	/**
	 * @return Newly created message
	 */
	public CancelTargetSpellMessageImpl createCancelTargetSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestSwitchOffMaintainedSpellMessageImpl createRequestSwitchOffMaintainedSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestResearchSpellMessageImpl createRequestResearchSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public UploadCustomPhotoMessageImpl createUploadCustomPhotoMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseStandardPhotoMessageImpl createChooseStandardPhotoMessage ();

	/**
	 * @return Newly created message
	 */
	public ChangeOptionalFarmersMessageImpl createChangeOptionalFarmersMessage ();

	/**
	 * @return Newly created message
	 */
	public CancelPendingMovementAndSpecialOrdersMessageImpl createCancelPendingMovementAndSpecialOrdersMessage ();

	/**
	 * @return Newly created message
	 */
	public TargetSpellMessageImpl createTargetSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestCastSpellMessageImpl createRequestCastSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public EndCombatTurnMessageImpl createEndCombatTurnMessage ();

	/**
	 * @return Newly created message
	 */
	public CombatAutoControlMessageImpl createCombatAutoControlMessage ();

	/**
	 * @return Newly created message
	 */
	public ChooseWizardMessageImpl createChooseWizardMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestMoveCombatUnitMessageImpl createRequestMoveCombatUnitMessage ();

	/**
	 * @return Newly created message
	 */
	public ChangeCityConstructionMessageImpl createChangeCityConstructionMessage ();
	
	/**
	 * @return Newly created message
	 */
	public RequestMoveHeroItemMessageImpl createRequestMoveHeroItemMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestRemoveQueuedSpellMessageImpl createRequestRemoveQueuedSpellMessage ();

	/**
	 * @return Newly created message
	 */
	public RequestAcceptOfferMessageImpl createRequestAcceptOfferMessage ();
	
	/**
	 * @return Newly created message
	 */
	public RequestDiplomacyMessageImpl createRequestDiplomacyMessage ();
}
package momime.server.messages.process;

import javax.xml.bind.annotation.XmlRegistry;

import momime.common.messages.clienttoserver.AlchemyMessage;
import momime.common.messages.clienttoserver.CancelPendingMovementAndSpecialOrdersMessage;
import momime.common.messages.clienttoserver.CancelTargetSpellMessage;
import momime.common.messages.clienttoserver.CaptureCityDecisionMessage;
import momime.common.messages.clienttoserver.ChangeCityConstructionMessage;
import momime.common.messages.clienttoserver.ChangeOptionalFarmersMessage;
import momime.common.messages.clienttoserver.ChangeTaxRateMessage;
import momime.common.messages.clienttoserver.ChatMessage;
import momime.common.messages.clienttoserver.ChooseCityNameMessage;
import momime.common.messages.clienttoserver.ChooseCustomFlagColourMessage;
import momime.common.messages.clienttoserver.ChooseCustomPicksMessage;
import momime.common.messages.clienttoserver.ChooseInitialSpellsMessage;
import momime.common.messages.clienttoserver.ChooseRaceMessage;
import momime.common.messages.clienttoserver.ChooseStandardPhotoMessage;
import momime.common.messages.clienttoserver.ChooseWizardMessage;
import momime.common.messages.clienttoserver.CombatAutoControlMessage;
import momime.common.messages.clienttoserver.DismissUnitMessage;
import momime.common.messages.clienttoserver.EndCombatTurnMessage;
import momime.common.messages.clienttoserver.NextTurnButtonMessage;
import momime.common.messages.clienttoserver.ObjectFactory;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.common.messages.clienttoserver.RequestMoveCombatUnitMessage;
import momime.common.messages.clienttoserver.RequestMoveHeroItemMessage;
import momime.common.messages.clienttoserver.RequestMoveOverlandUnitStackMessage;
import momime.common.messages.clienttoserver.RequestOverlandMovementDistancesMessage;
import momime.common.messages.clienttoserver.RequestRemoveQueuedSpellMessage;
import momime.common.messages.clienttoserver.RequestResearchSpellMessage;
import momime.common.messages.clienttoserver.RequestSwitchOffMaintainedSpellMessage;
import momime.common.messages.clienttoserver.RequestUpdateUnitNameMessage;
import momime.common.messages.clienttoserver.RushBuyMessage;
import momime.common.messages.clienttoserver.SellBuildingMessage;
import momime.common.messages.clienttoserver.SpecialOrderButtonMessage;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.messages.clienttoserver.UpdateMagicPowerDistributionMessage;
import momime.common.messages.clienttoserver.UploadCustomPhotoMessage;

/**
 * On the server, make sure we create the special versions of each message that include the processing interface.
 * This just delegates all message creation to the factory interface which creates messages from prototype bean definitions.
 * I tried just putting @XmlRegistry straight on the interface and using it, but that didn't work
 */
@XmlRegistry
public final class ClientToServerMessagesObjectFactory extends ObjectFactory
{
	/** Factory for creating prototype message beans from spring */
	private ClientToServerMessagesFactory factory;
	
	/**
	 * @return Newly created ChooseWizardMessage
	 */
	@Override
	public final ChooseWizardMessage createChooseWizardMessage ()
	{
        return getFactory ().createChooseWizardMessage ();
	}

	/**
	 * @return Newly created UploadCustomPhotoMessage
	 */
	@Override
	public final UploadCustomPhotoMessage createUploadCustomPhotoMessage ()
	{
		return getFactory ().createUploadCustomPhotoMessage ();
	}

	/**
	 * @return Newly created ChooseInitialSpellsMessage
	 */
	@Override
	public final ChooseInitialSpellsMessage createChooseInitialSpellsMessage ()
	{
		return getFactory ().createChooseInitialSpellsMessage ();
	}

	/**
	 * @return Newly created ChooseCityNameMessage
	 */
	@Override
	public final ChooseCityNameMessage createChooseCityNameMessage ()
	{
		return getFactory ().createChooseCityNameMessage ();
	}

	/**
	 * @return Newly created ChooseStandardPhotoMessage
	 */
	@Override
	public final ChooseStandardPhotoMessage createChooseStandardPhotoMessage ()
	{
		return getFactory ().createChooseStandardPhotoMessage ();
	}

	/**
	 * @return Newly created ChooseCustomFlagColourMessage
	 */
	@Override
	public final ChooseCustomFlagColourMessage createChooseCustomFlagColourMessage ()
	{
		return getFactory ().createChooseCustomFlagColourMessage ();
	}

	/**
	 * @return Newly created ChooseCustomPicksMessage
	 */
	@Override
	public final ChooseCustomPicksMessage createChooseCustomPicksMessage ()
	{
		return getFactory ().createChooseCustomPicksMessage ();
	}

	/**
	 * @return Newly created ChooseRaceMessage
	 */
	@Override
	public final ChooseRaceMessage createChooseRaceMessage ()
	{
		return getFactory ().createChooseRaceMessage ();
	}

	/**
	 * @return Newly created ChangeCityConstructionMessage
	 */
	@Override
	public final ChangeCityConstructionMessage createChangeCityConstructionMessage ()
	{
		return getFactory ().createChangeCityConstructionMessage ();
	}

	/**
	 * @return Newly created ChangeOptionalFarmersMessage
	 */
	@Override
	public final ChangeOptionalFarmersMessage createChangeOptionalFarmersMessage ()
	{
		return getFactory ().createChangeOptionalFarmersMessage ();
	}

	/**
	 * @return Newly created UpdateMagicPowerDistributionMessage
	 */
	@Override
	public final UpdateMagicPowerDistributionMessage createUpdateMagicPowerDistributionMessage ()
	{
		return getFactory ().createUpdateMagicPowerDistributionMessage ();
	}

	/**
	 * @return Newly created AlchemyMessage
	 */
	@Override
	public final AlchemyMessage createAlchemyMessage ()
	{
		return getFactory ().createAlchemyMessage ();
	}

	/**
	 * @return Newly created RequestOverlandMovementDistancesMessage
	 */
	@Override
	public final RequestOverlandMovementDistancesMessage createRequestOverlandMovementDistancesMessage ()
	{
		return getFactory ().createRequestOverlandMovementDistancesMessage ();
	}

	/**
	 * @return Newly created RequestMoveOverlandUnitStackMessage
	 */
	@Override
	public final RequestMoveOverlandUnitStackMessage createRequestMoveOverlandUnitStackMessage ()
	{
		return getFactory ().createRequestMoveOverlandUnitStackMessage ();
	}

	/**
	 * @return Newly created RequestResearchSpellMessage
	 */
	@Override
	public final RequestResearchSpellMessage createRequestResearchSpellMessage ()
	{
		return getFactory ().createRequestResearchSpellMessage ();
	}

	/**
	 * @return Newly created NextTurnButtonMessage
	 */
	@Override
	public final NextTurnButtonMessage createNextTurnButtonMessage ()
	{
		return getFactory ().createNextTurnButtonMessage ();
	}

	/**
	 * @return Newly created RequestCastSpellMessage
	 */
	@Override
	public final RequestCastSpellMessage createRequestCastSpellMessage ()
	{
		return getFactory ().createRequestCastSpellMessage ();
	}

	/**
	 * @return Newly created TargetSpellMessage
	 */
	@Override
	public final TargetSpellMessage createTargetSpellMessage ()
	{
		return getFactory ().createTargetSpellMessage ();
	}

	/**
	 * @return Newly created CancelTargetSpellMessage
	 */
	@Override
	public final CancelTargetSpellMessage createCancelTargetSpellMessage ()
	{
		return getFactory ().createCancelTargetSpellMessage ();
	}
	
	/**
	 * @return Newly created DismissUnitMessage
	 */
	@Override
	public final DismissUnitMessage createDismissUnitMessage ()
	{
		return getFactory ().createDismissUnitMessage ();
	}

	/**
	 * @return Newly created ChatMessage
	 */
	@Override
	public final ChatMessage createChatMessage ()
	{
		return getFactory ().createChatMessage ();
	}

	/**
	 * @return Newly created ChangeTaxRateMessage
	 */
	@Override
	public final ChangeTaxRateMessage createChangeTaxRateMessage ()
	{
		return getFactory ().createChangeTaxRateMessage ();
	}

	/**
	 * @return Newly created RushBuyMessage
	 */
	@Override
	public final RushBuyMessage createRushBuyMessage ()
	{
		return getFactory ().createRushBuyMessage ();
	}
	
	/**
	 * @return Newly created SellBuildingMessage
	 */
	@Override
	public final SellBuildingMessage createSellBuildingMessage ()
	{
		return getFactory ().createSellBuildingMessage ();
	}

	/**
	 * @return Newly created SpecialOrderButtonMessage
	 */
	@Override
	public final SpecialOrderButtonMessage createSpecialOrderButtonMessage ()
	{
		return getFactory ().createSpecialOrderButtonMessage ();
	}

	/**
	 * @return Newly created CancelPendingMovementAndSpecialOrdersMessage
	 */
	@Override
	public final CancelPendingMovementAndSpecialOrdersMessage createCancelPendingMovementAndSpecialOrdersMessage ()
	{
		return getFactory ().createCancelPendingMovementAndSpecialOrdersMessage ();
	}

	/**
	 * @return Newly created RequestSwitchOffMaintainedSpellMessage
	 */
	@Override
	public final RequestSwitchOffMaintainedSpellMessage createRequestSwitchOffMaintainedSpellMessage ()
	{
		return getFactory ().createRequestSwitchOffMaintainedSpellMessage ();
	}

	/**
	 * @return Newly created RequestUpdateUnitNameMessage
	 */
	@Override
	public final RequestUpdateUnitNameMessage createRequestUpdateUnitNameMessage ()
	{
		return getFactory ().createRequestUpdateUnitNameMessage ();
	}

	/**
	 * @return Newly created EndCombatTurnMessage
	 */
	@Override
	public final EndCombatTurnMessage createEndCombatTurnMessage ()
	{
		return getFactory ().createEndCombatTurnMessage ();
	}

	/**
	 * @return Newly created CombatAutoControlMessage
	 */
	@Override
	public final CombatAutoControlMessage createCombatAutoControlMessage ()
	{
		return getFactory ().createCombatAutoControlMessage ();
	}

	/**
	 * @return Newly created RequestMoveCombatUnitMessage
	 */
	@Override
	public final RequestMoveCombatUnitMessage createRequestMoveCombatUnitMessage ()
	{
		return getFactory ().createRequestMoveCombatUnitMessage ();
	}
	
	/**
	 * @return Newly created CaptureCityDecisionMessage
	 */
	@Override
	public final CaptureCityDecisionMessage createCaptureCityDecisionMessage ()
	{
		return getFactory ().createCaptureCityDecisionMessage ();
	}

	/**
	 * @return Newly created RequestMoveHeroItemMessage
	 */
	@Override
	public final RequestMoveHeroItemMessage createRequestMoveHeroItemMessage ()
	{
		return getFactory ().createRequestMoveHeroItemMessage ();
	}

	/**
	 * @return Newly created RequestMoveHeroItemMessage
	 */
	@Override
	public final RequestRemoveQueuedSpellMessage createRequestRemoveQueuedSpellMessage ()
	{
		return getFactory ().createRequestRemoveQueuedSpellMessage ();
	}
	
	/**
	 * @return Factory for creating prototype message beans from spring
	 */
	public final ClientToServerMessagesFactory getFactory ()
	{
		return factory;
	}

	/**
	 * @param fac Factory for creating prototype message beans from spring
	 */
	public final void setFactory (final ClientToServerMessagesFactory fac)
	{
		factory = fac;
	}
}
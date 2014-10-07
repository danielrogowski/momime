package momime.server.messages.process;

import javax.xml.bind.annotation.XmlRegistry;

import momime.common.messages.clienttoserver.v0_9_5.AlchemyMessage;
import momime.common.messages.clienttoserver.v0_9_5.CancelPendingMovementAndSpecialOrdersMessage;
import momime.common.messages.clienttoserver.v0_9_5.CancelTargetSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.CaptureCityDecisionMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChangeCityConstructionMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChangeOptionalFarmersMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChangeTaxRateMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChatMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChooseCityNameMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChooseCustomFlagColourMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChooseCustomPicksMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChooseInitialSpellsMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChooseRaceMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChooseStandardPhotoMessage;
import momime.common.messages.clienttoserver.v0_9_5.ChooseWizardMessage;
import momime.common.messages.clienttoserver.v0_9_5.CombatAutoControlMessage;
import momime.common.messages.clienttoserver.v0_9_5.DismissUnitMessage;
import momime.common.messages.clienttoserver.v0_9_5.EndCombatTurnMessage;
import momime.common.messages.clienttoserver.v0_9_5.NextTurnButtonMessage;
import momime.common.messages.clienttoserver.v0_9_5.ObjectFactory;
import momime.common.messages.clienttoserver.v0_9_5.RequestCastSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestMoveCombatUnitMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestMoveOverlandUnitStackMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestOverlandMovementDistancesMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestResearchSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestStartScheduledCombatMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestSwitchOffMaintainedSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestUpdateUnitNameMessage;
import momime.common.messages.clienttoserver.v0_9_5.RushBuyMessage;
import momime.common.messages.clienttoserver.v0_9_5.SellBuildingMessage;
import momime.common.messages.clienttoserver.v0_9_5.SpecialOrderButtonMessage;
import momime.common.messages.clienttoserver.v0_9_5.TargetSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.UnrequestStartScheduledCombatMessage;
import momime.common.messages.clienttoserver.v0_9_5.UpdateMagicPowerDistributionMessage;
import momime.common.messages.clienttoserver.v0_9_5.UploadCustomPhotoMessage;

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
	 * @return Newly created UnrequestStartScheduledCombatMessage
	 */
	@Override
	public final UnrequestStartScheduledCombatMessage createUnrequestStartScheduledCombatMessage ()
	{
		return getFactory ().createUnrequestStartScheduledCombatMessage ();
	}

	/**
	 * @return Newly created RequestStartScheduledCombatMessage
	 */
	@Override
	public final RequestStartScheduledCombatMessage createRequestStartScheduledCombatMessage ()
	{
		return getFactory ().createRequestStartScheduledCombatMessage ();
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
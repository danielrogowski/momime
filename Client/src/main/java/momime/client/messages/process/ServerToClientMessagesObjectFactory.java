package momime.client.messages.process;

import javax.xml.bind.annotation.XmlRegistry;

import momime.common.messages.servertoclient.AddBuildingMessage;
import momime.common.messages.servertoclient.AddOrUpdateCombatAreaEffectMessage;
import momime.common.messages.servertoclient.AddOrUpdateMaintainedSpellMessage;
import momime.common.messages.servertoclient.AddNewTurnMessagesMessage;
import momime.common.messages.servertoclient.AddOrUpdateUnitMessage;
import momime.common.messages.servertoclient.AddPowerBaseHistoryMessage;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.ApplyDamageMessage;
import momime.common.messages.servertoclient.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.BroadcastChatMessage;
import momime.common.messages.servertoclient.CancelCombatAreaEffectMessage;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.ChosenCustomPhotoMessage;
import momime.common.messages.servertoclient.ChosenStandardPhotoMessage;
import momime.common.messages.servertoclient.ChosenWizardMessage;
import momime.common.messages.servertoclient.CombatEndedMessage;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.messages.servertoclient.DamageCalculationHeaderData;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DestroyBuildingMessage;
import momime.common.messages.servertoclient.DispelMagicResultsMessage;
import momime.common.messages.servertoclient.EndOfContinuedMovementMessage;
import momime.common.messages.servertoclient.ErasePendingMovementsMessage;
import momime.common.messages.servertoclient.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.messages.servertoclient.MoveUnitInCombatMessage;
import momime.common.messages.servertoclient.MoveUnitStackOverlandMessage;
import momime.common.messages.servertoclient.NewGameDatabaseMessage;
import momime.common.messages.servertoclient.ObjectFactory;
import momime.common.messages.servertoclient.OnePlayerSimultaneousTurnDoneMessage;
import momime.common.messages.servertoclient.OverlandCastQueuedMessage;
import momime.common.messages.servertoclient.PendingMovementMessage;
import momime.common.messages.servertoclient.PendingSaleMessage;
import momime.common.messages.servertoclient.PlayAnimationMessage;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.RemoveUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.ReplacePicksMessage;
import momime.common.messages.servertoclient.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.SetCombatPlayerMessage;
import momime.common.messages.servertoclient.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.servertoclient.ShowSpellAnimationMessage;
import momime.common.messages.servertoclient.SpellResearchChangedMessage;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.messages.servertoclient.StartGameMessage;
import momime.common.messages.servertoclient.StartSimultaneousTurnMessage;
import momime.common.messages.servertoclient.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.TaxRateChangedMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.TreasureRewardMessage;
import momime.common.messages.servertoclient.UpdateCityMessage;
import momime.common.messages.servertoclient.UpdateCombatMapMessage;
import momime.common.messages.servertoclient.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.messages.servertoclient.UpdateRemainingResearchCostMessage;
import momime.common.messages.servertoclient.UpdateTerrainMessage;
import momime.common.messages.servertoclient.UpdateWizardStateMessage;
import momime.common.messages.servertoclient.YourPhotoIsOkMessage;
import momime.common.messages.servertoclient.YourRaceIsOkMessage;

/**
 * On the client, make sure we create the special versions of each message that include the processing interface.
 * This just delegates all message creation to the factory interface which creates messages from prototype bean definitions.
 * I tried just putting @XmlRegistry straight on the interface and using it, but that didn't work
 */
@XmlRegistry
public final class ServerToClientMessagesObjectFactory extends ObjectFactory
{
	/** Factory for creating prototype message beans from spring */
	private ServerToClientMessagesFactory factory;
	
	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateRemainingResearchCostMessage createUpdateRemainingResearchCostMessage ()
	{
		return getFactory ().createUpdateRemainingResearchCostMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final SelectNextUnitToMoveOverlandMessage createSelectNextUnitToMoveOverlandMessage ()
	{
		return getFactory ().createSelectNextUnitToMoveOverlandMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final TextPopupMessage createTextPopupMessage ()
	{
		return getFactory ().createTextPopupMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final NewGameDatabaseMessage createNewGameDatabaseMessage ()
	{
		return getFactory ().createNewGameDatabaseMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final StartCombatMessage createStartCombatMessage ()
	{
		return getFactory ().createStartCombatMessage ();
	}
	
	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateCombatMapMessage createUpdateCombatMapMessage ()
	{
		return getFactory ().createUpdateCombatMapMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final PendingMovementMessage createPendingMovementMessage ()
	{
		return getFactory ().createPendingMovementMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final FullSpellListMessage createFullSpellListMessage ()
	{
		return getFactory ().createFullSpellListMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final FogOfWarVisibleAreaChangedMessage createFogOfWarVisibleAreaChangedMessage ()
	{
		return getFactory ().createFogOfWarVisibleAreaChangedMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final StartGameMessage createStartGameMessage ()
	{
		return getFactory ().createStartGameMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ErasePendingMovementsMessage createErasePendingMovementsMessage ()
	{
		return getFactory ().createErasePendingMovementsMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final PendingSaleMessage createPendingSaleMessage ()
	{
		return getFactory ().createPendingSaleMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final DamageCalculationMessage createDamageCalculationMessage ()
	{
		return getFactory ().createDamageCalculationMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateGlobalEconomyMessage createUpdateGlobalEconomyMessage ()
	{
		return getFactory ().createUpdateGlobalEconomyMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final StartSimultaneousTurnMessage createStartSimultaneousTurnMessage ()
	{
		return getFactory ().createStartSimultaneousTurnMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddNewTurnMessagesMessage createAddNewTurnMessagesMessage ()
	{
		return getFactory ().createAddNewTurnMessagesMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final TaxRateChangedMessage createTaxRateChangedMessage ()
	{
		return getFactory ().createTaxRateChangedMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final MoveUnitStackOverlandMessage createMoveUnitStackOverlandMessage ()
	{
		return getFactory ().createMoveUnitStackOverlandMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final YourPhotoIsOkMessage createYourPhotoIsOkMessage ()
	{
		return getFactory ().createYourPhotoIsOkMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddOrUpdateCombatAreaEffectMessage createAddOrUpdateCombatAreaEffectMessage ()
	{
		return getFactory ().createAddOrUpdateCombatAreaEffectMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddOrUpdateMaintainedSpellMessage createAddOrUpdateMaintainedSpellMessage ()
	{
		return getFactory ().createAddOrUpdateMaintainedSpellMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateCityMessage createUpdateCityMessage ()
	{
		return getFactory ().createUpdateCityMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ChooseInitialSpellsNowMessage createChooseInitialSpellsNowMessage ()
	{
		return getFactory ().createChooseInitialSpellsNowMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ChosenStandardPhotoMessage createChosenStandardPhotoMessage ()
	{
		return getFactory ().createChosenStandardPhotoMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ReplacePicksMessage createReplacePicksMessage ()
	{
		return getFactory ().createReplacePicksMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final SetCombatPlayerMessage createSetCombatPlayerMessage ()
	{
		return getFactory ().createSetCombatPlayerMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final KillUnitMessage createKillUnitMessage ()
	{
		return getFactory ().createKillUnitMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ChosenCustomPhotoMessage createChosenCustomPhotoMessage ()
	{
		return getFactory ().createChosenCustomPhotoMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ChooseYourRaceNowMessage createChooseYourRaceNowMessage ()
	{
		return getFactory ().createChooseYourRaceNowMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final YourRaceIsOkMessage createYourRaceIsOkMessage ()
	{
		return getFactory ().createYourRaceIsOkMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ApplyDamageMessage createApplyDamageMessage ()
	{
		return getFactory ().createApplyDamageMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateTerrainMessage createUpdateTerrainMessage ()
	{
		return getFactory ().createUpdateTerrainMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final OverlandCastQueuedMessage createOverlandCastQueuedMessage ()
	{
		return getFactory ().createOverlandCastQueuedMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddOrUpdateUnitMessage createAddOrUpdateUnitMessage ()
	{
		return getFactory ().createAddOrUpdateUnitMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddBuildingMessage createAddBuildingMessage ()
	{
		return getFactory ().createAddBuildingMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final DestroyBuildingMessage createDestroyBuildingMessage ()
	{
		return getFactory ().createDestroyBuildingMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final EndOfContinuedMovementMessage createEndOfContinuedMovementMessage ()
	{
		return getFactory ().createEndOfContinuedMovementMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final SetUnitIntoOrTakeUnitOutOfCombatMessage createSetUnitIntoOrTakeUnitOutOfCombatMessage ()
	{
		return getFactory ().createSetUnitIntoOrTakeUnitOutOfCombatMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final SpellResearchChangedMessage createSpellResearchChangedMessage ()
	{
		return getFactory ().createSpellResearchChangedMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final SwitchOffMaintainedSpellMessage createSwitchOffMaintainedSpellMessage ()
	{
		return getFactory ().createSwitchOffMaintainedSpellMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final RemoveQueuedSpellMessage createRemoveQueuedSpellMessage ()
	{
		return getFactory ().createRemoveQueuedSpellMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final MoveUnitInCombatMessage createMoveUnitInCombatMessage ()
	{
		return getFactory ().createMoveUnitInCombatMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final CombatEndedMessage createCombatEndedMessage ()
	{
		return getFactory ().createCombatEndedMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ChosenWizardMessage createChosenWizardMessage ()
	{
		return getFactory ().createChosenWizardMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final OnePlayerSimultaneousTurnDoneMessage createOnePlayerSimultaneousTurnDoneMessage ()
	{
		return getFactory ().createOnePlayerSimultaneousTurnDoneMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateManaSpentOnCastingCurrentSpellMessage createUpdateManaSpentOnCastingCurrentSpellMessage ()
	{
		return getFactory ().createUpdateManaSpentOnCastingCurrentSpellMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final BroadcastChatMessage createBroadcastChatMessage ()
	{
		return getFactory ().createBroadcastChatMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final SetCurrentPlayerMessage createSetCurrentPlayerMessage ()
	{
		return getFactory ().createSetCurrentPlayerMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final CancelCombatAreaEffectMessage createCancelCombatAreaEffectMessage ()
	{
		return getFactory ().createCancelCombatAreaEffectMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AskForCaptureCityDecisionMessage createAskForCaptureCityDecisionMessage ()
	{
		return getFactory ().createAskForCaptureCityDecisionMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public AddUnassignedHeroItemMessage createAddUnassignedHeroItemMessage ()
	{
		return getFactory ().createAddUnassignedHeroItemMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public RemoveUnassignedHeroItemMessage createRemoveUnassignedHeroItemMessage ()
	{
		return getFactory ().createRemoveUnassignedHeroItemMessage ();
	}

	/**
	 * @return Newly created damage calculation breakdown line
	 */
	@Override
	public final DamageCalculationHeaderData createDamageCalculationHeaderData ()
	{
		return getFactory ().createDamageCalculationHeaderData ();
	}

	/**
	 * @return Newly created damage calculation breakdown line
	 */
	@Override
	public final DamageCalculationAttackData createDamageCalculationAttackData ()
	{
		return getFactory ().createDamageCalculationAttackData ();
	}

	/**
	 * @return Newly created damage calculation breakdown line
	 */
	@Override
	public final DamageCalculationDefenceData createDamageCalculationDefenceData ()
	{
		return getFactory ().createDamageCalculationDefenceData ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final TreasureRewardMessage createTreasureRewardMessage ()
	{
		return getFactory ().createTreasureRewardMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final DispelMagicResultsMessage createDispelMagicResultsMessage ()
	{
		return getFactory ().createDispelMagicResultsMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final ShowSpellAnimationMessage createShowSpellAnimationMessage ()
	{
		return getFactory ().createShowSpellAnimationMessage ();
	}
	
	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateWizardStateMessage createUpdateWizardStateMessage ()
	{
		return getFactory ().createUpdateWizardStateMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddPowerBaseHistoryMessage createAddPowerBaseHistoryMessage ()
	{
		return getFactory ().createAddPowerBaseHistoryMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final PlayAnimationMessage createPlayAnimationMessage ()
	{
		return getFactory ().createPlayAnimationMessage ();
	}
	
	/**
	 * @return Factory for creating prototype message beans from spring
	 */
	public final ServerToClientMessagesFactory getFactory ()
	{
		return factory;
	}

	/**
	 * @param fac Factory for creating prototype message beans from spring
	 */
	public final void setFactory (final ServerToClientMessagesFactory fac)
	{
		factory = fac;
	}
}
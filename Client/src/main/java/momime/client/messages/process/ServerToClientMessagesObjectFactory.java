package momime.client.messages.process;

import javax.xml.bind.annotation.XmlRegistry;

import momime.common.messages.servertoclient.v0_9_4.AddBuildingMessage;
import momime.common.messages.servertoclient.v0_9_4.AddCombatAreaEffectMessage;
import momime.common.messages.servertoclient.v0_9_4.AddMaintainedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.AddNewTurnMessagesMessage;
import momime.common.messages.servertoclient.v0_9_4.AddScheduledCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.AddUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.ApplyDamageMessage;
import momime.common.messages.servertoclient.v0_9_4.AskForCaptureCityDecisionMessage;
import momime.common.messages.servertoclient.v0_9_4.BroadcastChatMessage;
import momime.common.messages.servertoclient.v0_9_4.CancelCombatAreaEffectMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenCustomPhotoMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenStandardPhotoMessage;
import momime.common.messages.servertoclient.v0_9_4.ChosenWizardMessage;
import momime.common.messages.servertoclient.v0_9_4.CombatEndedMessage;
import momime.common.messages.servertoclient.v0_9_4.DamageCalculationMessage;
import momime.common.messages.servertoclient.v0_9_4.DestroyBuildingMessage;
import momime.common.messages.servertoclient.v0_9_4.EndOfContinuedMovementMessage;
import momime.common.messages.servertoclient.v0_9_4.ErasePendingMovementsMessage;
import momime.common.messages.servertoclient.v0_9_4.FogOfWarVisibleAreaChangedMessage;
import momime.common.messages.servertoclient.v0_9_4.FoundLairNodeTowerMessage;
import momime.common.messages.servertoclient.v0_9_4.FullSpellListMessage;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.MoveUnitInCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.MoveUnitStackOverlandMessage;
import momime.common.messages.servertoclient.v0_9_4.NewGameDatabaseMessage;
import momime.common.messages.servertoclient.v0_9_4.ObjectFactory;
import momime.common.messages.servertoclient.v0_9_4.OnePlayerSimultaneousTurnDoneMessage;
import momime.common.messages.servertoclient.v0_9_4.OverlandCastQueuedMessage;
import momime.common.messages.servertoclient.v0_9_4.OverlandMovementTypesMessage;
import momime.common.messages.servertoclient.v0_9_4.PendingMovementMessage;
import momime.common.messages.servertoclient.v0_9_4.PendingSaleMessage;
import momime.common.messages.servertoclient.v0_9_4.PlayerCombatRequestStatusMessage;
import momime.common.messages.servertoclient.v0_9_4.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.ReplacePicksMessage;
import momime.common.messages.servertoclient.v0_9_4.ScheduledCombatWalkInWithoutAFightMessage;
import momime.common.messages.servertoclient.v0_9_4.SelectNextUnitToMoveOverlandMessage;
import momime.common.messages.servertoclient.v0_9_4.SetCombatPlayerMessage;
import momime.common.messages.servertoclient.v0_9_4.SetCurrentPlayerMessage;
import momime.common.messages.servertoclient.v0_9_4.SetSpecialOrderMessage;
import momime.common.messages.servertoclient.v0_9_4.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.ShowListAndOtherScheduledCombatsMessage;
import momime.common.messages.servertoclient.v0_9_4.SpellResearchChangedMessage;
import momime.common.messages.servertoclient.v0_9_4.StartCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.StartGameMessage;
import momime.common.messages.servertoclient.v0_9_4.StartGameProgressMessage;
import momime.common.messages.servertoclient.v0_9_4.StartSimultaneousTurnMessage;
import momime.common.messages.servertoclient.v0_9_4.SwitchOffMaintainedSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.TaxRateChangedMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateCityMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateDamageTakenAndExperienceMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateNodeLairTowerUnitIDMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateOtherScheduledCombatsMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateOverlandMovementRemainingMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateProductionSoFarMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateRemainingResearchCostMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateTerrainMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateUnitNameMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateUnitToAliveMessage;
import momime.common.messages.servertoclient.v0_9_4.YourPhotoIsOkMessage;
import momime.common.messages.servertoclient.v0_9_4.YourRaceIsOkMessage;

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
	public final UpdateNodeLairTowerUnitIDMessage createUpdateNodeLairTowerUnitIDMessage ()
	{
		return getFactory ().createUpdateNodeLairTowerUnitIDMessage ();
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
	public final PendingMovementMessage createPendingMovementMessage ()
	{
		return getFactory ().createPendingMovementMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final PlayerCombatRequestStatusMessage createPlayerCombatRequestStatusMessage ()
	{
		return getFactory ().createPlayerCombatRequestStatusMessage ();
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
	public final StartGameProgressMessage createStartGameProgressMessage ()
	{
		return getFactory ().createStartGameProgressMessage ();
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
	public final UpdateOverlandMovementRemainingMessage createUpdateOverlandMovementRemainingMessage ()
	{
		return getFactory ().createUpdateOverlandMovementRemainingMessage ();
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
	public final ScheduledCombatWalkInWithoutAFightMessage createScheduledCombatWalkInWithoutAFightMessage ()
	{
		return getFactory ().createScheduledCombatWalkInWithoutAFightMessage ();
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
	public final FoundLairNodeTowerMessage createFoundLairNodeTowerMessage ()
	{
		return getFactory ().createFoundLairNodeTowerMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddCombatAreaEffectMessage createAddCombatAreaEffectMessage ()
	{
		return getFactory ().createAddCombatAreaEffectMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddMaintainedSpellMessage createAddMaintainedSpellMessage ()
	{
		return getFactory ().createAddMaintainedSpellMessage ();
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
	public final ShowListAndOtherScheduledCombatsMessage createShowListAndOtherScheduledCombatsMessage ()
	{
		return getFactory ().createShowListAndOtherScheduledCombatsMessage ();
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
	public final SetSpecialOrderMessage createSetSpecialOrderMessage ()
	{
		return getFactory ().createSetSpecialOrderMessage ();
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
	public final UpdateOtherScheduledCombatsMessage createUpdateOtherScheduledCombatsMessage ()
	{
		return getFactory ().createUpdateOtherScheduledCombatsMessage ();
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
	public final AddScheduledCombatMessage createAddScheduledCombatMessage ()
	{
		return getFactory ().createAddScheduledCombatMessage ();
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
	public final UpdateDamageTakenAndExperienceMessage createUpdateDamageTakenAndExperienceMessage ()
	{
		return getFactory ().createUpdateDamageTakenAndExperienceMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final AddUnitMessage createAddUnitMessage ()
	{
		return getFactory ().createAddUnitMessage ();
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
	public final UpdateUnitNameMessage createUpdateUnitNameMessage ()
	{
		return getFactory ().createUpdateUnitNameMessage ();
	}

	/**
	 * @return Newly created message
	 */
	@Override
	public final UpdateProductionSoFarMessage createUpdateProductionSoFarMessage ()
	{
		return getFactory ().createUpdateProductionSoFarMessage ();
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
	public final OverlandMovementTypesMessage createOverlandMovementTypesMessage ()
	{
		return getFactory ().createOverlandMovementTypesMessage ();
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
	public final UpdateUnitToAliveMessage createUpdateUnitToAliveMessage ()
	{
		return getFactory ().createUpdateUnitToAliveMessage ();
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

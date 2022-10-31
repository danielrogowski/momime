package momime.server.process;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.DiplomacyAction;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.PactType;
import momime.common.messages.servertoclient.DiplomacyMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.DiplomacyAIConstants;
import momime.server.ai.RelationAI;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.utils.KnownWizardServerUtils;

/**
 * Methods for processing agreed diplomatic actions.  Attempting to keep this consistent for all situations,
 * so in all methods the proposer could be a human or AI player, and so could the agreer (and so could the thirdParty, if applicable).
 */
public final class DiplomacyProcessingImpl implements DiplomacyProcessing
{
	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/**
	 * @param proposer Player who proposed the pact
	 * @param agreer Player who agreed to the pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param pactType What type of pact it is
	 * @param action Diplomacy action to send back to the proposer, if they are a human player
	 * @param relationBonus Bonus to each player's opinion of each other
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void agreePact (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom,
		final PactType pactType, final DiplomacyAction action, final int relationBonus)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge proposerPriv = (MomPersistentPlayerPrivateKnowledge) proposer.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge agreerPriv = (MomPersistentPlayerPrivateKnowledge) agreer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails proposersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(proposerPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "agreePact (A)");
		final DiplomacyWizardDetails agreersOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(agreerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "agreePact (P)");
		
		// Update pacts
		getKnownWizardServerUtils ().updatePact (proposer.getPlayerDescription ().getPlayerID (), agreer.getPlayerDescription ().getPlayerID (), pactType, mom);
		getKnownWizardServerUtils ().updatePact (agreer.getPlayerDescription ().getPlayerID (), proposer.getPlayerDescription ().getPlayerID (), pactType, mom);
		
		// Both players like each other for establishing the pact.  It doesn't matter who proposed it and who agreed,
		// but this is only relevant to AI players as we don't try to know what a human player's opinion of another player is.
		if ((proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!proposersOpinionOfAgreer.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().bonusToVisibleRelation (proposersOpinionOfAgreer, relationBonus);
		
		if ((agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!agreersOpinionOfProposer.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().bonusToVisibleRelation (agreersOpinionOfProposer, relationBonus);
		
		// If the proposer was a human player, notify them that the agreer accepted the pact
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setAction (action);

			// Show them that the AI player's opinion of them improved because of the pact
			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (agreersOpinionOfProposer.getVisibleRelation (), "agreePact").getRelationScoreID ());
			
			proposer.getConnection ().sendMessageToClient (msg);
		}
	}

	/**
	 * @param proposer Player who proposed the wizard pact
	 * @param agreer Player who agreed to the wizard pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void agreeWizardPact (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		agreePact (proposer, agreer, mom, PactType.WIZARD_PACT, DiplomacyAction.ACCEPT_WIZARD_PACT, DiplomacyAIConstants.RELATION_BONUS_FORM_WIZARD_PACT);
	}
	
	/**
	 * @param proposer Player who proposed the alliance
	 * @param agreer Player who agreed to the alliance
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void agreeAlliance (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		agreePact (proposer, agreer, mom, PactType.ALLIANCE, DiplomacyAction.ACCEPT_ALLIANCE, DiplomacyAIConstants.RELATION_BONUS_FORM_ALLIANCE);
	}
	
	/**
	 * @param proposer Player who proposed the peace treaty
	 * @param agreer Player who agreed to the peace treaty
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void agreePeaceTreaty (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		agreePact (proposer, agreer, mom, null, DiplomacyAction.ACCEPT_PEACE_TREATY, DiplomacyAIConstants.RELATION_BONUS_FORM_PEACE_TREATY);
	}
	
	/**
	 * @param proposer Player who proposed the pact
	 * @param rejecter Player who rejected the pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param action Diplomacy rejection to send back to the proposer, if they are a human player
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void rejectPact (final PlayerServerDetails proposer, final PlayerServerDetails rejecter, final MomSessionVariables mom, final DiplomacyAction action)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge rejecterPriv = (MomPersistentPlayerPrivateKnowledge) rejecter.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails rejectersOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(rejecterPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "rejectPact (A)");
		
		// If the proposer was a human player, notify them that the agreer accepted the pact
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (rejecter.getPlayerDescription ().getPlayerID ());
			msg.setAction (action);

			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (rejecter.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (rejectersOpinionOfProposer.getVisibleRelation (), "rejectPact").getRelationScoreID ());
			
			proposer.getConnection ().sendMessageToClient (msg);
		}
	}
	
	/**
	 * @param proposer Player who proposed the wizard pact
	 * @param rejecter Player who rejected the wizard pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectWizardPact (final PlayerServerDetails proposer, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPact (proposer, rejecter, mom, DiplomacyAction.REJECT_WIZARD_PACT);
	}
	
	/**
	 * @param proposer Player who proposed the alliance
	 * @param rejecter Player who rejected the alliance
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectAlliance (final PlayerServerDetails proposer, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPact (proposer, rejecter, mom, DiplomacyAction.REJECT_ALLIANCE);
	}
	
	/**
	 * @param proposer Player who proposed the peace treaty
	 * @param rejecter Player who rejected the peace treaty
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectPeaceTreaty (final PlayerServerDetails proposer, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPact (proposer, rejecter, mom, DiplomacyAction.REJECT_PEACE_TREATY);
	}
	
	/**
	 * @param proposer Player who proposed the pact
	 * @param agreer Player who agreed to the pact
	 * @param other Player who pact was made with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param pactType What type of pact it is
	 * @param action Diplomacy action to send back to the proposer, if they are a human player
	 * @param otherAction Diplomacy action to send back to the 3rd party player, if they are a human player
	 * @param positiveRelationBonus How much proposer likes agreer for agreeing to this
	 * @param negativeRelationBonus How much 3rd party wizard dislikes both of them for conspiring against them
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void agreePactWithThirdParty (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other, final MomSessionVariables mom,
		final PactType pactType, final DiplomacyAction action, final DiplomacyAction otherAction, final int positiveRelationBonus, final int negativeRelationBonus)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge proposerPriv = (MomPersistentPlayerPrivateKnowledge) proposer.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge agreerPriv = (MomPersistentPlayerPrivateKnowledge) agreer.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge otherPriv = (MomPersistentPlayerPrivateKnowledge) other.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails proposersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(proposerPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "agreePactWithThirdParty (A)");
		final DiplomacyWizardDetails agreersOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(agreerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "agreePactWithThirdParty (P)");
		final DiplomacyWizardDetails othersOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(otherPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "agreePactWithThirdParty (T)");
		final DiplomacyWizardDetails othersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(otherPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "agreePactWithThirdParty (O)");
		
		// Update pacts
		getKnownWizardServerUtils ().updatePact (other.getPlayerDescription ().getPlayerID (), agreer.getPlayerDescription ().getPlayerID (), pactType, mom);
		getKnownWizardServerUtils ().updatePact (agreer.getPlayerDescription ().getPlayerID (), other.getPlayerDescription ().getPlayerID (), pactType, mom);
		
		// Inform the 3rd party wizard, who isn't involved in the conversation
		if (other.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setOtherPlayerID (proposer.getPlayerDescription ().getPlayerID ());
			msg.setAction (otherAction);
			msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (CommonDatabaseConstants.MIN_RELATION_SCORE, "agreePactWithThirdParty").getRelationScoreID ());
			
			other.getConnection ().sendMessageToClient (msg);
		}
		
		// agreer already dislikes proposer by -10 for them making this request, this was dealt with during the "propose" phase because it applies even if the proposal was rejected.
		// But now it was actually agreed to, the proposer likes them for agreeing to it, and the 3rd party who got declared war on hates them both for picking on them.
		if ((proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!proposersOpinionOfAgreer.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().bonusToVisibleRelation (proposersOpinionOfAgreer, positiveRelationBonus);
		
		if ((other.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!othersOpinionOfProposer.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().penaltyToVisibleRelation (othersOpinionOfProposer, negativeRelationBonus);

		if ((other.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!othersOpinionOfAgreer.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().penaltyToVisibleRelation (othersOpinionOfAgreer, negativeRelationBonus);
		
		// If the proposer was a human player, notify them that the agreer accepted the proposal
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setOtherPlayerID (proposer.getPlayerDescription ().getPlayerID ());
			msg.setAction (action);

			// Show them that the AI player's opinion of them improved because of the pact
			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (agreersOpinionOfProposer.getVisibleRelation (), "agreePactWithThirdParty").getRelationScoreID ());
			
			proposer.getConnection ().sendMessageToClient (msg);
		}
	}

	/**
	 * @param proposer Player who proposed that one wizard declare war on another
	 * @param agreer Player who agreed to declare war
	 * @param other Player who war was declared on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void agreeDeclareWarOnOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		agreePactWithThirdParty (proposer, agreer, other, mom, PactType.WAR, DiplomacyAction.DECLARE_WAR_ON_YOU_BECAUSE_OF_OTHER_WIZARD,
			DiplomacyAction.ACCEPT_DECLARE_WAR_ON_OTHER_WIZARD, DiplomacyAIConstants.RELATION_BONUS_FORM_AGREEING_TO_DECLARE_WAR,
			DiplomacyAIConstants.RELATION_PENALTY_FOR_DECLARING_WAR);
	}
	
	/**
	 * @param proposer Player who proposed that one wizard break their alliance with another
	 * @param agreer Player who agreed to break their alliance
	 * @param other Player who they had the alliance with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void agreeBreakAllianceWithOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		agreePactWithThirdParty (proposer, agreer, other, mom, null, DiplomacyAction.BREAK_ALLIANCE_WITH_YOU_BECAUSE_OF_OTHER_WIZARD,
			DiplomacyAction.ACCEPT_BREAK_ALLIANCE_WITH_OTHER_WIZARD, DiplomacyAIConstants.RELATION_BONUS_FORM_AGREEING_TO_BREAK_ALLIANCE,
			DiplomacyAIConstants.RELATION_PENALTY_FOR_BREAKING_ALLIANCE_NICELY);
	}

	/**
	 * @param proposer Player who proposed the pact
	 * @param agreer Player who rejected the pact
	 * @param other Player who the pact was in reference to
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param action Diplomacy rejection to send back to the proposer, if they are a human player
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void rejectPactWithThirdParty (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other,
		final MomSessionVariables mom, final DiplomacyAction action)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge agreerPriv = (MomPersistentPlayerPrivateKnowledge) agreer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails agreersOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(agreerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "rejectPactWithThirdParty");
		
		// If the proposer was a human player, notify them that the agreer accepted the pact
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setOtherPlayerID (other.getPlayerDescription ().getPlayerID ());
			msg.setAction (action);

			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (agreersOpinionOfProposer.getVisibleRelation (), "rejectPactWithThirdParty").getRelationScoreID ());
			
			proposer.getConnection ().sendMessageToClient (msg);
		}
	}
	
	/**
	 * @param proposer Player who proposed the declaration of war
	 * @param agreer Player who rejected the declaration of war
	 * @param other Player who they wanted war declared on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectDeclareWarOnOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other,
		final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPactWithThirdParty (proposer, agreer, other, mom, DiplomacyAction.REJECT_DECLARE_WAR_ON_OTHER_WIZARD);
	}

	/**
	 * @param proposer Player who proposed the breaking allance
	 * @param agreer Player who rejected breaking their alliance
	 * @param other Player who they have an alliance with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectBreakAllianceWithOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other,
		final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPactWithThirdParty (proposer, agreer, other, mom, DiplomacyAction.REJECT_BREAK_ALLIANCE_WITH_OTHER_WIZARD);
	}

	/**
	 * @param giver Player who is giving gold
	 * @param receiver Player who is receiving gold
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param offerGoldTier Gold offer tier 1..4
	 * @param giverAction Diplomacy action to send back to the giver, if they are a human player
	 * @param receiverAction Diplomacy action to send to the receiver, if they are a human player
	 * @param relationBonusPerTier Bonus to the receiver's opinion of the giver, multiplied up by the tier
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void giveGoldInternal (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom,
		final int offerGoldTier, final DiplomacyAction giverAction, final DiplomacyAction receiverAction, final int relationBonusPerTier)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge giverPriv = (MomPersistentPlayerPrivateKnowledge) giver.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge receiverPriv = (MomPersistentPlayerPrivateKnowledge) receiver.getPersistentPlayerPrivateKnowledge ();

		final DiplomacyWizardDetails giversOpinionOfReceiver = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(giverPriv.getFogOfWarMemory ().getWizardDetails (), receiver.getPlayerDescription ().getPlayerID (), "giveGoldInternal (R)");
		final DiplomacyWizardDetails receiversOpinionOfGiver = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(receiverPriv.getFogOfWarMemory ().getWizardDetails (), giver.getPlayerDescription ().getPlayerID (), "giveGoldInternal (G)");
		
		// Convert tier to actual gold amount
		final int offerGoldAmount = getKnownWizardUtils ().convertGoldOfferTierToAmount (giversOpinionOfReceiver.getMaximumGoldTribute (), offerGoldTier);
		
		// Give gold				
		getResourceValueUtils ().addToAmountStored (giverPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -offerGoldAmount);
		getServerResourceCalculations ().sendGlobalProductionValues (giver, null, false);
		
		getResourceValueUtils ().addToAmountStored (receiverPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, offerGoldAmount);
		getServerResourceCalculations ().sendGlobalProductionValues (receiver, null, false);
		
		// Further gold offers will be more expensive (there's no message for this - client triggers same update from the ACCEPT_GOLD msg sent below)
		giversOpinionOfReceiver.setMaximumGoldTribute (giversOpinionOfReceiver.getMaximumGoldTribute () + offerGoldAmount);
		
		// Improved relation from the donation
		if ((relationBonusPerTier > 0) && (receiver.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!receiversOpinionOfGiver.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().bonusToVisibleRelation (receiversOpinionOfGiver, offerGoldTier * relationBonusPerTier);
		
		// Tell the receiver they were given some gold
		if (receiver.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (receiver.getPlayerDescription ().getPlayerID ());
			msg.setAction (receiverAction);
			msg.setOfferGoldAmount (offerGoldAmount);

			if (giver.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (giversOpinionOfReceiver.getVisibleRelation (), "giveGoldInternal").getRelationScoreID ());
			
			receiver.getConnection ().sendMessageToClient (msg);
		}
		
		// If the giver was a human player, tell them thanks
		if (giver.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (receiver.getPlayerDescription ().getPlayerID ());
			msg.setAction (giverAction);
			msg.setOfferGoldAmount (offerGoldAmount);

			if (receiver.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (receiversOpinionOfGiver.getVisibleRelation (), "giveGoldInternal").getRelationScoreID ());
			
			giver.getConnection ().sendMessageToClient (msg);
		}
	}

	/**
	 * @param giver Player who is giving gold
	 * @param receiver Player who is receiving gold
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param offerGoldTier Gold offer tier 1..4
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void giveGold (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final int offerGoldTier)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		giveGoldInternal (giver, receiver, mom, offerGoldTier, DiplomacyAction.ACCEPT_GOLD, DiplomacyAction.GIVE_GOLD,
			DiplomacyAIConstants.RELATION_BONUS_FOR_GOLD_DONATION_PER_TIER);
	}
	
	/**
	 * @param giver Player who is giving gold
	 * @param receiver Player who threatened them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param offerGoldTier Gold offer tier 1..4
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void giveGoldBecauseThreatened (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final int offerGoldTier)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		giveGoldInternal (giver, receiver, mom, offerGoldTier, DiplomacyAction.ACCEPT_GOLD_BECAUSE_THREATENED, DiplomacyAction.GIVE_GOLD_BECAUSE_THREATENED, 0);
	}
	
	/**
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}

	/**
	 * @return For calculating relation scores between two wizards
	 */
	public final RelationAI getRelationAI ()
	{
		return relationAI;
	}

	/**
	 * @param ai For calculating relation scores between two wizards
	 */
	public final void setRelationAI (final RelationAI ai)
	{
		relationAI = ai;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}
}
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
import momime.server.MomSessionVariables;
import momime.server.ai.DiplomacyAIConstants;
import momime.server.ai.RelationAI;
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
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "agreePact").getRelationScoreID ());
			
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
	 * @param agreer Player who rejected the pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param action Diplomacy rejection to send back to the proposer, if they are a human player
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void rejectPact (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom, final DiplomacyAction action)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge proposerPriv = (MomPersistentPlayerPrivateKnowledge) proposer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails proposersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(proposerPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "rejectPact (A)");
		
		// If the proposer was a human player, notify them that the agreer accepted the pact
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setAction (action);

			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "rejectPact").getRelationScoreID ());
			
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
		final MomPersistentPlayerPrivateKnowledge otherPriv = (MomPersistentPlayerPrivateKnowledge) other.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails proposersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(proposerPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "agreePactWithThirdParty (A)");
		final DiplomacyWizardDetails othersOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(otherPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "agreePactWithThirdParty (P)");
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
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "agreePactWithThirdParty").getRelationScoreID ());
			
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
		final MomPersistentPlayerPrivateKnowledge proposerPriv = (MomPersistentPlayerPrivateKnowledge) proposer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails proposersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(proposerPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "rejectPactWithThirdParty (A)");
		
		// If the proposer was a human player, notify them that the agreer accepted the pact
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setOtherPlayerID (other.getPlayerDescription ().getPlayerID ());
			msg.setAction (action);

			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "rejectPactWithThirdParty").getRelationScoreID ());
			
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
}
package momime.server.process;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.RelationScore;
import momime.common.database.Spell;
import momime.common.database.SpellRank;
import momime.common.messages.DiplomacyAction;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.PactType;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.servertoclient.DiplomacyMessage;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.DiplomacyAIConstants;
import momime.server.ai.RelationAI;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.utils.KnownWizardServerUtils;

/**
 * Methods for processing agreed diplomatic actions.  Attempting to keep this consistent for all situations,
 * so in all methods the proposer could be a human or AI player, and so could the agreer (and so could the thirdParty, if applicable).
 */
public final class DiplomacyProcessingImpl implements DiplomacyProcessing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (DiplomacyProcessingImpl.class);
	
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
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;

	/**
	 * @param humanPlayer Human player we want to talk to
	 * @param aiPlayer AI player who wants to talk to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void requestTalking (final PlayerServerDetails humanPlayer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("AI Player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " requesting to talk to human player ID " + humanPlayer.getPlayerDescription ().getPlayerID ());
		
		// What's our opinion of the human player?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfHumanPlayer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), humanPlayer.getPlayerDescription ().getPlayerID (), "requestTalking");
		
		final RelationScore relationScore = mom.getServerDB ().findRelationScoreForValue (aiPlayerOpinionOfHumanPlayer.getVisibleRelation (), "requestTalking");
		
		// Send request
		final DiplomacyMessage msg = new DiplomacyMessage ();
		msg.setTalkFromPlayerID (aiPlayer.getPlayerDescription ().getPlayerID ());
		msg.setAction (DiplomacyAction.INITIATE_TALKING);
		msg.setVisibleRelationScoreID (relationScore.getRelationScoreID ());
		
		humanPlayer.getConnection ().sendMessageToClient (msg);
	}
	
	
	/**
	 * @param requester Player who wanted to talk
	 * @param agreer Player who agreed to talk to them
	 * @param patienceRunningOut Whether agreeing reluctantly and have limited patience to talk to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void agreeTalking (final PlayerServerDetails requester, final PlayerServerDetails agreer, final boolean patienceRunningOut, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " agreed to talk to " + requester.getPlayerDescription ().getPlayerType () +
			" player ID " + requester.getPlayerDescription ().getPlayerID ());
		
		if (requester.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			// What's our opinion of the requester?
			final MomPersistentPlayerPrivateKnowledge agreerPriv = (MomPersistentPlayerPrivateKnowledge) agreer.getPersistentPlayerPrivateKnowledge ();
			
			final DiplomacyWizardDetails rejecterOpinionOfRequester = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
				(agreerPriv.getFogOfWarMemory ().getWizardDetails (), requester.getPlayerDescription ().getPlayerID (), "rejectTalking");

			final RelationScore relationScore = mom.getServerDB ().findRelationScoreForValue (rejecterOpinionOfRequester.getVisibleRelation (), "rejectTalking");

			// Send message
			final DiplomacyMessage msg = new DiplomacyMessage ();
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setAction (patienceRunningOut ? DiplomacyAction.ACCEPT_TALKING_IMPATIENT : DiplomacyAction.ACCEPT_TALKING);
			msg.setVisibleRelationScoreID (relationScore.getRelationScoreID ());
			
			requester.getConnection ().sendMessageToClient (msg);
		}
	}

	/**
	 * @param requester Player who wanted to talk
	 * @param rejecter Player who is fed up talking to them
	 * @param action Kind of rejection action to send
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void nagging (final PlayerServerDetails requester, final PlayerServerDetails rejecter, final DiplomacyAction action, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the requester?
		final MomPersistentPlayerPrivateKnowledge rejecterPriv = (MomPersistentPlayerPrivateKnowledge) rejecter.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails rejecterOpinionOfRequester = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(rejecterPriv.getFogOfWarMemory ().getWizardDetails (), requester.getPlayerDescription ().getPlayerID (), "nagging");

		// Penalty for nagging
		getRelationAI ().penaltyToVisibleRelation (rejecterOpinionOfRequester, DiplomacyAIConstants.RELATION_PENALTY_FOR_NAGGING);
		
		if (requester.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final RelationScore relationScore = mom.getServerDB ().findRelationScoreForValue (rejecterOpinionOfRequester.getVisibleRelation (), "nagging");
			
			final DiplomacyMessage msg = new DiplomacyMessage ();
			msg.setTalkFromPlayerID (rejecter.getPlayerDescription ().getPlayerID ());
			msg.setAction (action);
			msg.setVisibleRelationScoreID (relationScore.getRelationScoreID ());
			
			requester.getConnection ().sendMessageToClient (msg);
		}
	}
	
	/**
	 * @param requester Player who wanted to talk
	 * @param rejecter Player who refused to talk to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectTalking (final PlayerServerDetails requester, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + rejecter.getPlayerDescription ().getPlayerID () + " refused to talk to " + requester.getPlayerDescription ().getPlayerType () +
			" player ID " + requester.getPlayerDescription ().getPlayerID ());
		
		nagging (requester, rejecter, DiplomacyAction.REJECT_TALKING, mom);
	}
	
	/**
	 * @param requester Player who wanted to talk
	 * @param rejecter Player who is fed up talking to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void grownImpatient (final PlayerServerDetails requester, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + rejecter.getPlayerDescription ().getPlayerID () + " was talking to " + requester.getPlayerDescription ().getPlayerType () +
			" player ID " + requester.getPlayerDescription ().getPlayerID () + " but grew impatient so ended the conversation");
		
		nagging (requester, rejecter, DiplomacyAction.GROWN_IMPATIENT, mom);
	}
	
	/**
	 * @param proposer Player who proposed the pact
	 * @param agreer Player who agreed to the pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param pactType What type of pact it is
	 * @param proposerAction Diplomacy action to send back to the proposer, if they are a human player
	 * @param agreerAction Diplomacy action to send back to the agreer, if they are a human player
	 * @param relationBonus Bonus to each player's opinion of each other
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void agreePact (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom,
		final PactType pactType, final DiplomacyAction proposerAction, final DiplomacyAction agreerAction, final int relationBonus)
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
		if ((relationBonus > 0) && (proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!proposersOpinionOfAgreer.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().bonusToVisibleRelation (proposersOpinionOfAgreer, relationBonus);
		
		if ((relationBonus > 0) && (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!agreersOpinionOfProposer.isEverStartedCastingSpellOfMastery ()))
			getRelationAI ().bonusToVisibleRelation (agreersOpinionOfProposer, relationBonus);
		
		// If they're breaking a pact, only the player breaking it gets a penalty
		if ((relationBonus < 0) && (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN))
			getRelationAI ().penaltyToVisibleRelation (agreersOpinionOfProposer, -relationBonus);
		
		// If the proposer was a human player, notify them that the agreer accepted the pact
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setAction (proposerAction);

			// Show them that the AI player's opinion of them improved because of the pact
			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (agreersOpinionOfProposer.getVisibleRelation (), "agreePact").getRelationScoreID ());
			
			proposer.getConnection ().sendMessageToClient (msg);
		}

		if ((agreerAction != null) && (agreer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN))
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (proposer.getPlayerDescription ().getPlayerID ());
			msg.setAction (agreerAction);

			if (proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "agreePact").getRelationScoreID ());
			
			agreer.getConnection ().sendMessageToClient (msg);
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
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " agreed to a wizard pact with " + proposer.getPlayerDescription ().getPlayerType () +
			" player ID " + proposer.getPlayerDescription ().getPlayerID ());
		
		agreePact (proposer, agreer, mom, PactType.WIZARD_PACT, DiplomacyAction.ACCEPT_WIZARD_PACT, DiplomacyAction.AFTER_WIZARD_PACT,
			DiplomacyAIConstants.RELATION_BONUS_FORM_WIZARD_PACT);
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
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " agreed to an alliance with " + proposer.getPlayerDescription ().getPlayerType () +
			" player ID " + proposer.getPlayerDescription ().getPlayerID ());
		
		agreePact (proposer, agreer, mom, PactType.ALLIANCE, DiplomacyAction.ACCEPT_ALLIANCE, DiplomacyAction.AFTER_ALLIANCE,
			DiplomacyAIConstants.RELATION_BONUS_FORM_ALLIANCE);
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
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " agreed to a peace treaty with " + proposer.getPlayerDescription ().getPlayerType () +
			" player ID " + proposer.getPlayerDescription ().getPlayerID ());
		
		agreePact (proposer, agreer, mom, null, DiplomacyAction.ACCEPT_PEACE_TREATY, DiplomacyAction.AFTER_PEACE_TREATY,
			DiplomacyAIConstants.RELATION_BONUS_FORM_PEACE_TREATY);
	}

	/**
	 * @param proposer Player who is breaking the wizard pact
	 * @param agreer Player who they had the wizard pact with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void breakWizardPactNicely (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + proposer.getPlayerDescription ().getPlayerID () + " is breaking their wizard pact with Player ID " + agreer.getPlayerDescription ().getPlayerType ());
		
		agreePact (proposer, agreer, mom, null, DiplomacyAction.BROKEN_WIZARD_PACT_NICELY, DiplomacyAction.BREAK_WIZARD_PACT_NICELY,
			-DiplomacyAIConstants.RELATION_PENALTY_FOR_BREAKING_WIZARD_PACT_NICELY);
	}
	
	/**
	 * @param proposer Player who is breaking the alliance
	 * @param agreer Player who they had the alliance with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void breakAllianceNicely (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + proposer.getPlayerDescription ().getPlayerID () + " is breaking their alliance with Player ID " + agreer.getPlayerDescription ().getPlayerType ());
		
		agreePact (proposer, agreer, mom, null, DiplomacyAction.BROKEN_ALLIANCE_NICELY, DiplomacyAction.BREAK_ALLIANCE_NICELY,
			-DiplomacyAIConstants.RELATION_PENALTY_FOR_BREAKING_ALLIANCE_NICELY);
	}
	
	/**
	 * This works just like agreeing a pact, just "agreeing" to declare war on each other...
	 * 
	 * @param declarer Player who declared war
	 * @param threatener Player who threatened them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void declareWarBecauseThreatened (final PlayerServerDetails declarer, final PlayerServerDetails threatener, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Note players are reversed on purpose, its the threatener who gets a message back to say what the response to their threat was
		// There's no need to send a message back to the declarer, as the threatener's opinion of someone doesn't change when they threaten them
		log.debug ("Player ID " + threatener.getPlayerDescription ().getPlayerID () + " threatened Player ID " + declarer.getPlayerDescription ().getPlayerType () +
			", so they declared war on them for it");
		
		agreePact (threatener, declarer, mom, PactType.WAR, DiplomacyAction.DECLARE_WAR_BECAUSE_THREATENED, null, 0);
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
		log.debug ("Player ID " + rejecter.getPlayerDescription ().getPlayerID () + " rejected a wizard pact with " + proposer.getPlayerDescription ().getPlayerType () +
			" player ID " + proposer.getPlayerDescription ().getPlayerID ());
		
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
		log.debug ("Player ID " + rejecter.getPlayerDescription ().getPlayerID () + " rejected an alliance with " + proposer.getPlayerDescription ().getPlayerType () +
			" player ID " + proposer.getPlayerDescription ().getPlayerID ());
		
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
		log.debug ("Player ID " + rejecter.getPlayerDescription ().getPlayerID () + " rejected a peace treaty with " + proposer.getPlayerDescription ().getPlayerType () +
			" player ID " + proposer.getPlayerDescription ().getPlayerID ());
		
		rejectPact (proposer, rejecter, mom, DiplomacyAction.REJECT_PEACE_TREATY);
	}
	
	/**
	 * @param proposer Player who proposed the pact
	 * @param agreer Player who agreed to the pact
	 * @param other Player who pact was made with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param pactType What type of pact it is
	 * @param proposerAction Diplomacy action to send back to the proposer, if they are a human player
	 * @param agreerAction Diplomacy action to send back to the agreer, if they are a human player
	 * @param otherAction Diplomacy action to send back to the 3rd party player, if they are a human player
	 * @param positiveRelationBonus How much proposer likes agreer for agreeing to this
	 * @param negativeRelationBonus How much 3rd party wizard dislikes both of them for conspiring against them
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void agreePactWithThirdParty (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other, final MomSessionVariables mom,
		final PactType pactType, final DiplomacyAction proposerAction, final DiplomacyAction agreerAction, final DiplomacyAction otherAction,
		final int positiveRelationBonus, final int negativeRelationBonus)
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
		
		if (other.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
			getRelationAI ().penaltyToVisibleRelation (othersOpinionOfProposer, negativeRelationBonus);

		if (other.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
			getRelationAI ().penaltyToVisibleRelation (othersOpinionOfAgreer, negativeRelationBonus);
		
		// If the proposer was a human player, notify them that the agreer accepted the proposal
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setOtherPlayerID (other.getPlayerDescription ().getPlayerID ());
			msg.setAction (proposerAction);

			// Show them that the AI player's opinion of them improved because of the pact
			// If it is two human players, leave it null and the UI will keep showing the same value from when diplomacy started
			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (agreersOpinionOfProposer.getVisibleRelation (), "agreePactWithThirdParty").getRelationScoreID ());
			
			proposer.getConnection ().sendMessageToClient (msg);
		}

		if (agreer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (proposer.getPlayerDescription ().getPlayerID ());
			msg.setOtherPlayerID (other.getPlayerDescription ().getPlayerID ());
			msg.setAction (agreerAction);

			if (proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "agreePactWithThirdParty").getRelationScoreID ());
			
			agreer.getConnection ().sendMessageToClient (msg);
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
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " agreed to player ID " + proposer.getPlayerDescription ().getPlayerType () +
			"'s request to declare war on player ID " + other.getPlayerDescription ().getPlayerID ());
		
		agreePactWithThirdParty (proposer, agreer, other, mom, PactType.WAR, DiplomacyAction.DECLARE_WAR_ON_YOU_BECAUSE_OF_OTHER_WIZARD,
			DiplomacyAction.ACCEPT_DECLARE_WAR_ON_OTHER_WIZARD, DiplomacyAction.AFTER_DECLARE_WAR_ON_OTHER_WIZARD,
			DiplomacyAIConstants.RELATION_BONUS_FORM_AGREEING_TO_DECLARE_WAR, DiplomacyAIConstants.RELATION_PENALTY_FOR_DECLARING_WAR);
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
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " agreed to player ID " + proposer.getPlayerDescription ().getPlayerType () +
			"'s request to break their alliance with player ID " + other.getPlayerDescription ().getPlayerID ());
		
		agreePactWithThirdParty (proposer, agreer, other, mom, null, DiplomacyAction.BREAK_ALLIANCE_WITH_YOU_BECAUSE_OF_OTHER_WIZARD,
			DiplomacyAction.ACCEPT_BREAK_ALLIANCE_WITH_OTHER_WIZARD, DiplomacyAction.AFTER_BREAK_ALLIANCE_WITH_OTHER_WIZARD,
			DiplomacyAIConstants.RELATION_BONUS_FORM_AGREEING_TO_BREAK_ALLIANCE, DiplomacyAIConstants.RELATION_PENALTY_FOR_BREAKING_ALLIANCE_NICELY);
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
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " refused player ID " + proposer.getPlayerDescription ().getPlayerType () +
			"'s request to declare war on player ID " + other.getPlayerDescription ().getPlayerID ());
		
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
		log.debug ("Player ID " + agreer.getPlayerDescription ().getPlayerID () + " refused player ID " + proposer.getPlayerDescription ().getPlayerType () +
			"'s request to break their alliance with player ID " + other.getPlayerDescription ().getPlayerID ());
		
		rejectPactWithThirdParty (proposer, agreer, other, mom, DiplomacyAction.REJECT_BREAK_ALLIANCE_WITH_OTHER_WIZARD);
	}

	/**
	 * @param giver Player who is giving gold
	 * @param receiver Player who is receiving gold
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param offerGoldTier Gold offer tier 1..4
	 * @param giverAction Diplomacy action to send back to the giver, if they are a human player; can be null
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
			msg.setTalkFromPlayerID (giver.getPlayerDescription ().getPlayerID ());
			msg.setAction (receiverAction);
			msg.setOfferGoldAmount (offerGoldAmount);

			if (giver.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (giversOpinionOfReceiver.getVisibleRelation (), "giveGoldInternal").getRelationScoreID ());
			
			receiver.getConnection ().sendMessageToClient (msg);
		}
		
		// If the giver was a human player, tell them thanks
		if ((giverAction != null) && (giver.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN))
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
		log.debug ("Player ID " + giver.getPlayerDescription ().getPlayerID () + " is giving player ID " + receiver.getPlayerDescription ().getPlayerType () + " gold donation at tier " + offerGoldTier);
		
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
		log.debug ("Player ID " + giver.getPlayerDescription ().getPlayerID () + " is giving player ID " + receiver.getPlayerDescription ().getPlayerType () + " gold donation at tier " + offerGoldTier +
			" in response to being threatened");
		
		giveGoldInternal (giver, receiver, mom, offerGoldTier, DiplomacyAction.ACCEPT_GOLD, DiplomacyAction.GIVE_GOLD_BECAUSE_THREATENED, 0);
	}
	
	/**
	 * @param giver Player who is giving a spell
	 * @param receiver Player who is receiving a spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param spellID Spell being given
	 * @param giverAction Diplomacy action to send back to the giver, if they are a human player; can be null
	 * @param receiverAction Diplomacy action to send to the receiver, if they are a human player
	 * @param relationBonus Whether the receiver's opinion of the giver improves for them giving the spell (if true, the amount it improves depends on the spell rank and comes from the DB)
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	private final void giveSpellInternal (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom,
		final String spellID, final DiplomacyAction giverAction, final DiplomacyAction receiverAction, final boolean relationBonus)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge giverPriv = (MomPersistentPlayerPrivateKnowledge) giver.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge receiverPriv = (MomPersistentPlayerPrivateKnowledge) receiver.getPersistentPlayerPrivateKnowledge ();

		final DiplomacyWizardDetails giversOpinionOfReceiver = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(giverPriv.getFogOfWarMemory ().getWizardDetails (), receiver.getPlayerDescription ().getPlayerID (), "giveSpellInternal (R)");
		final DiplomacyWizardDetails receiversOpinionOfGiver = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(receiverPriv.getFogOfWarMemory ().getWizardDetails (), giver.getPlayerDescription ().getPlayerID (), "giveSpellInternal (G)");
		
		// Learn spell
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (receiverPriv.getSpellResearchStatus (), spellID);
		researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		// Just in case the donated spell was one of the 8 spells available to research now
		getServerSpellCalculations ().randomizeSpellsResearchableNow (receiverPriv.getSpellResearchStatus (), mom.getServerDB ());
		
		if (receiver.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
			spellsMsg.getSpellResearchStatus ().addAll (receiverPriv.getSpellResearchStatus ());
			receiver.getConnection ().sendMessageToClient (spellsMsg);
		}
		
		// Improved relation from the donation
		if ((relationBonus) && (receiver.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!receiversOpinionOfGiver.isEverStartedCastingSpellOfMastery ()))
		{
			final Spell spellDef = mom.getServerDB ().findSpell (spellID, "giveSpellInternal");
			final SpellRank spellRank = mom.getServerDB ().findSpellRank (spellDef.getSpellRank (), "giveSpellInternal");
			if (spellRank.getSpellTributeRelationBonus () != null)
				getRelationAI ().bonusToVisibleRelation (receiversOpinionOfGiver, spellRank.getSpellTributeRelationBonus ());
		}
		
		// Tell the receiver they were given some a spell
		if (receiver.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (giver.getPlayerDescription ().getPlayerID ());
			msg.setAction (receiverAction);
			msg.setOfferSpellID (spellID);

			if (giver.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (giversOpinionOfReceiver.getVisibleRelation (), "giveSpellInternal").getRelationScoreID ());
			
			receiver.getConnection ().sendMessageToClient (msg);
		}
		
		// If the giver was a human player, tell them thanks
		if ((giverAction != null) && (giver.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN))
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (receiver.getPlayerDescription ().getPlayerID ());
			msg.setAction (giverAction);
			msg.setOfferSpellID (spellID);

			if (receiver.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (receiversOpinionOfGiver.getVisibleRelation (), "giveSpellInternal").getRelationScoreID ());
			
			giver.getConnection ().sendMessageToClient (msg);
		}
	}
	
	/**
	 * @param giver Player who is giving a spell
	 * @param receiver Player who is receiving a spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param spellID Spell being given
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void giveSpell (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final String spellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + giver.getPlayerDescription ().getPlayerID () + " is giving player ID " + receiver.getPlayerDescription ().getPlayerType () + " spell ID " + spellID);
		
		giveSpellInternal (giver, receiver, mom, spellID, DiplomacyAction.ACCEPT_SPELL, DiplomacyAction.GIVE_SPELL, true);
	}

	/**
	 * @param giver Player who is giving a spell
	 * @param receiver Player who threatened them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param spellID Spell being given
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void giveSpellBecauseThreatened (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final String spellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + giver.getPlayerDescription ().getPlayerID () + " is giving player ID " + receiver.getPlayerDescription ().getPlayerType () + " spell ID " + spellID +
			" in response to being threatened");
		
		giveSpellInternal (giver, receiver, mom, spellID, DiplomacyAction.ACCEPT_SPELL, DiplomacyAction.GIVE_SPELL_BECAUSE_THREATENED, false);
	}

	/**
	 * @param proposer Player who proposed trading spells
	 * @param agreer Player who agreed to trade spells
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param proposerWantsSpellID The spell the proposer asked for
	 * @param agreerWantsSpellID The spell the agreer wants in return
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void agreeTradeSpells (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom,
		final String proposerWantsSpellID, final String agreerWantsSpellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + proposer.getPlayerDescription ().getPlayerID () + " is getting spell ID " + proposerWantsSpellID +
			" from player ID " + agreer.getPlayerDescription ().getPlayerType () + " in exchange for spell ID " + agreerWantsSpellID);
		
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge proposerPriv = (MomPersistentPlayerPrivateKnowledge) proposer.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge agreerPriv = (MomPersistentPlayerPrivateKnowledge) agreer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails proposersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(proposerPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "agreeTradeSpells (A)");
		final DiplomacyWizardDetails agreersOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(agreerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "agreeTradeSpells (P)");
		
		// Learn the spells
		final SpellResearchStatus agreerResearchStatus = getSpellUtils ().findSpellResearchStatus (agreerPriv.getSpellResearchStatus (), agreerWantsSpellID);
		final SpellResearchStatus proposerResearchStatus = getSpellUtils ().findSpellResearchStatus (proposerPriv.getSpellResearchStatus (), proposerWantsSpellID);
		agreerResearchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		proposerResearchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
		
		// Just in case the donated spell was one of the 8 spells available to research now
		getServerSpellCalculations ().randomizeSpellsResearchableNow (agreerPriv.getSpellResearchStatus (), mom.getServerDB ());
		getServerSpellCalculations ().randomizeSpellsResearchableNow (proposerPriv.getSpellResearchStatus (), mom.getServerDB ());
		
		if (agreer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
			spellsMsg.getSpellResearchStatus ().addAll (agreerPriv.getSpellResearchStatus ());
			agreer.getConnection ().sendMessageToClient (spellsMsg);
		}

		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
			spellsMsg.getSpellResearchStatus ().addAll (proposerPriv.getSpellResearchStatus ());
			proposer.getConnection ().sendMessageToClient (spellsMsg);
		}
		
		// Improved relations from the trade
		if ((agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!agreersOpinionOfProposer.isEverStartedCastingSpellOfMastery ()))
		{
			final Spell spellDef = mom.getServerDB ().findSpell (agreerWantsSpellID, "agreeTradeSpells");
			final SpellRank spellRank = mom.getServerDB ().findSpellRank (spellDef.getSpellRank (), "agreeTradeSpells");
			if (spellRank.getSpellTributeRelationBonus () != null)
				getRelationAI ().bonusToVisibleRelation (agreersOpinionOfProposer, spellRank.getSpellTributeRelationBonus ());
		}

		if ((proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN) && (!proposersOpinionOfAgreer.isEverStartedCastingSpellOfMastery ()))
		{
			final Spell spellDef = mom.getServerDB ().findSpell (proposerWantsSpellID, "agreeTradeSpells");
			final SpellRank spellRank = mom.getServerDB ().findSpellRank (spellDef.getSpellRank (), "agreeTradeSpells");
			if (spellRank.getSpellTributeRelationBonus () != null)
				getRelationAI ().bonusToVisibleRelation (proposersOpinionOfAgreer, spellRank.getSpellTributeRelationBonus ());
		}

		// Tell the receiver they were given some a spell
		if (agreer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (proposer.getPlayerDescription ().getPlayerID ());
			msg.setAction (DiplomacyAction.ACCEPT_EXCHANGE_SPELL);
			msg.setOfferSpellID (agreerWantsSpellID);
			msg.setRequestSpellID (proposerWantsSpellID);

			if (proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "agreeTradeSpells").getRelationScoreID ());
			
			agreer.getConnection ().sendMessageToClient (msg);
		}
		
		// If the proposer was a human player, tell them thanks
		if (proposer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (agreer.getPlayerDescription ().getPlayerID ());
			msg.setAction (DiplomacyAction.AFTER_EXCHANGE_SPELL);
			msg.setOfferSpellID (proposerWantsSpellID);		// // Note these are reversed, depending who the msg is going to
			msg.setRequestSpellID (agreerWantsSpellID);

			if (agreer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (agreersOpinionOfProposer.getVisibleRelation (), "tradeSpells").getRelationScoreID ());
			
			proposer.getConnection ().sendMessageToClient (msg);
		}
	}	
	
	/**
	 * @param proposer Player who proposed trading spells, but ultimately rejected the proposal when the agreer requested an unreasonable spell in return 
	 * @param agreer Player who agreed to trade spells
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param proposerWantsSpellID The spell the proposer asked for
	 * @param agreerWantsSpellID The spell the agreer wants in return
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectTradeSpells (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom,
		final String proposerWantsSpellID, final String agreerWantsSpellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge proposerPriv = (MomPersistentPlayerPrivateKnowledge) proposer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails proposersOpinionOfAgreer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(proposerPriv.getFogOfWarMemory ().getWizardDetails (), agreer.getPlayerDescription ().getPlayerID (), "rejectTradeSpells (A)");

		if (agreer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (proposer.getPlayerDescription ().getPlayerID ());
			msg.setAction (DiplomacyAction.REJECT_EXCHANGE_SPELL);
			msg.setOfferSpellID (agreerWantsSpellID);
			msg.setRequestSpellID (proposerWantsSpellID);

			if (proposer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (proposersOpinionOfAgreer.getVisibleRelation (), "rejectTradeSpells").getRelationScoreID ());
			
			agreer.getConnection ().sendMessageToClient (msg);
		}
	}
	
	/**
	 * @param ignorer Player who ignored the threat
	 * @param threatener Player who threatened them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void ignoreThreat (final PlayerServerDetails ignorer, final PlayerServerDetails threatener, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		log.debug ("Player ID " + threatener.getPlayerDescription ().getPlayerID () + " threatened Player ID " + ignorer.getPlayerDescription ().getPlayerType () + " and got ignored");
		
		// Find the two wizards' opinions of each other
		final MomPersistentPlayerPrivateKnowledge ignorerPriv = (MomPersistentPlayerPrivateKnowledge) ignorer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails ignorersOpinionOfThreatener = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(ignorerPriv.getFogOfWarMemory ().getWizardDetails (), threatener.getPlayerDescription ().getPlayerID (), "ignoreThreat");

		if (threatener.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final DiplomacyMessage msg = new DiplomacyMessage ();	
			msg.setTalkFromPlayerID (ignorer.getPlayerDescription ().getPlayerID ());
			msg.setAction (DiplomacyAction.IGNORE_THREAT);

			if (ignorer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
				msg.setVisibleRelationScoreID (mom.getServerDB ().findRelationScoreForValue (ignorersOpinionOfThreatener.getVisibleRelation (), "ignoreThreat").getRelationScoreID ());
			
			threatener.getConnection ().sendMessageToClient (msg);
		}
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

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return Server-only spell calculations
	 */
	public final ServerSpellCalculations getServerSpellCalculations ()
	{
		return serverSpellCalculations;
	}

	/**
	 * @param calc Server-only spell calculations
	 */
	public final void setServerSpellCalculations (final ServerSpellCalculations calc)
	{
		serverSpellCalculations = calc;
	}
}
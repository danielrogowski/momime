package momime.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.WizardPersonality;
import momime.common.messages.DiplomacyAction;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.messages.process.RequestDiplomacyMessageImpl;
import momime.server.process.DiplomacyProcessing;

/**
 * Methods for AI making decisions about diplomacy with other wizards
 */
public final class DiplomacyAIImpl implements DiplomacyAI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (DiplomacyAIImpl.class);
	
	/** Methods for processing agreed diplomatic actions */
	private DiplomacyProcessing diplomacyProcessing; 
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/**
	 * @param requester Player who wants to talk to us
	 * @param aiPlayer Player who is considering agreeing talking to them or not (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we agree to talk to them or not (note exactly what the caller needs to do based on this depends on whether the requester is a human or AI player)\
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final boolean decideWhetherWillTalkTo (final PlayerServerDetails requester, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the requester?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfRequester = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), requester.getPlayerDescription ().getPlayerID (), "decideWhetherWillTalkTo");

		// Have they been bugging us with requests too often?
		final int maximumRequests = getRelationAI ().decideMaximumRequests (aiPlayerOpinionOfRequester.getVisibleRelation ());
		final boolean accept = (aiPlayerOpinionOfRequester.getImpatienceLevel () < maximumRequests);
		if (accept)
		{
			final boolean patienceRunningOut = (aiPlayerOpinionOfRequester.getImpatienceLevel () + 1 >= maximumRequests);
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided they would " +
				(patienceRunningOut ? "reluctantly " : "") + "talk with Player ID " + requester.getPlayerDescription ().getPlayerID ());
			
			getDiplomacyProcessing ().agreeTalking (requester, aiPlayer, patienceRunningOut, mom);
		}
		else
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " refused to talk with Player ID " + requester.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().rejectTalking (requester, aiPlayer, mom);
		}
		
		return accept;
	}
	
	/**
	 * Decision is made the same as decideWhetherWillTalkTo, the only difference is how we handle the accept or reject 
	 * 
	 * @param requester Player who wants to make a proposal to us
	 * @param aiPlayer Player who is listening, or maybe lost patience to listen (us)
	 * @param request What type of request they are trying to make (some can't be ignored)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we agree to listen to their proposal or not (no action/msgs are taken for this, its up to the caller to act on a "true" result accordingly) 
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final boolean willListenToRequest (final PlayerServerDetails requester, final PlayerServerDetails aiPlayer, final DiplomacyAction request, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the requester?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfRequester = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), requester.getPlayerDescription ().getPlayerID (), "willListenToRequest");

		// Some requests reduce patience, but can't be ignored
		final boolean accept;
		if (DiplomacyAIConstants.CANNOT_IGNORE_REQUESTS.contains (request))
		{
			accept = true;
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " has no choice but listen to proposal " + request + " from Player ID " +
				requester.getPlayerDescription ().getPlayerID () + " because of the type of request");
		}
		else
		{
			final int maximumRequests = getRelationAI ().decideMaximumRequests (aiPlayerOpinionOfRequester.getVisibleRelation ());
			accept = (aiPlayerOpinionOfRequester.getImpatienceLevel () < maximumRequests);
			if (accept)
				log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " agreed to listen to proposal " + request + " from Player ID " + requester.getPlayerDescription ().getPlayerID ());
			else
			{
				log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " grew impatient and refused to listen to proposal " + request + " from Player ID " +
					requester.getPlayerDescription ().getPlayerID ());
				getDiplomacyProcessing ().grownImpatient (requester, aiPlayer, mom);
			}
		}
		
		// The fact that they keep bugging still reduces patience regardless of whether we listen or don't
		aiPlayerOpinionOfRequester.setImpatienceLevel (aiPlayerOpinionOfRequester.getImpatienceLevel () + 1);
		
		return accept;
	}
	
	/**
	 * @param proposer Player who proposed the wizard pact
	 * @param aiPlayer Player who is considering accepting or rejecting the wizard pact (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void considerWizardPact (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the proposer?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "considerWizardPact (P)");
		
		// What's our personality?
		final KnownWizardDetails aiWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), aiPlayer.getPlayerDescription ().getPlayerID (), "considerWizardPact");
		final WizardPersonality aiPersonality = mom.getServerDB ().findWizardPersonality (aiWizard.getWizardPersonalityID (), "considerWizardPact (W)");
		
		// Make decision
		if (aiPlayerOpinionOfProposer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_AGREE_TO_WIZARD_PACT + aiPersonality.getHostilityModifier ()))
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to accept a wizard pact with Player ID " + proposer.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().agreeWizardPact (proposer, aiPlayer, mom);
		}
		else
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to reject a wizard pact with Player ID " + proposer.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().rejectWizardPact (proposer, aiPlayer, mom);
		}
	}
	
	/**
	 * @param proposer Player who proposed the alliance
	 * @param aiPlayer Player who is considering accepting or rejecting the alliance (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void considerAlliance (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the proposer?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "considerAlliance (P)");
		
		// What's our personality?
		final KnownWizardDetails aiWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), aiPlayer.getPlayerDescription ().getPlayerID (), "considerAlliance");
		final WizardPersonality aiPersonality = mom.getServerDB ().findWizardPersonality (aiWizard.getWizardPersonalityID (), "considerAlliance (W)");
		
		// Make decision
		if (aiPlayerOpinionOfProposer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_AGREE_TO_ALLIANCE + aiPersonality.getHostilityModifier ()))
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to accept an alliance with Player ID " + proposer.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().agreeAlliance (proposer, aiPlayer, mom);
		}
		else
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to reject an alliance with Player ID " + proposer.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().rejectAlliance (proposer, aiPlayer, mom);
		}
	}

	/**
	 * @param proposer Player who proposed the peace treaty
	 * @param aiPlayer Player who is considering accepting or rejecting the peace treaty (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void considerPeaceTreaty (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the proposer?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "considerPeaceTreaty (P)");
		
		// What's our personality?
		final KnownWizardDetails aiWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), aiPlayer.getPlayerDescription ().getPlayerID (), "considerPeaceTreaty");
		final WizardPersonality aiPersonality = mom.getServerDB ().findWizardPersonality (aiWizard.getWizardPersonalityID (), "considerPeaceTreaty (W)");
		
		// Make decision
		if (aiPlayerOpinionOfProposer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_AGREE_TO_PEACE_TREATY + aiPersonality.getHostilityModifier ()))
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to accept a peace treaty with Player ID " + proposer.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().agreePeaceTreaty (proposer, aiPlayer, mom);
		}
		else
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to reject a peace treaty with Player ID " + proposer.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().rejectPeaceTreaty (proposer, aiPlayer, mom);
		}
	}

	/**
	 * @param proposer Player who proposed that we declare war on another wizard
	 * @param aiPlayer Player who is considering declaring war on another wizard as requeted (us)
	 * @param other The player we are being asked to declare war on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void considerDeclareWarOnOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the proposer?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "considerDeclareWarOnOtherWizard (P)");
		
		// What's our personality?
		final KnownWizardDetails aiWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), aiPlayer.getPlayerDescription ().getPlayerID (), "considerDeclareWarOnOtherWizard");
		final WizardPersonality aiPersonality = mom.getServerDB ().findWizardPersonality (aiWizard.getWizardPersonalityID (), "considerDeclareWarOnOtherWizard (W)");
		
		// Make decision
		if (aiPlayerOpinionOfProposer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_AGREE_TO_DECLARE_WAR + aiPersonality.getHostilityModifier ()))
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to agree to Player ID " + proposer.getPlayerDescription ().getPlayerID () +
				"'s request to declare war on Player ID " + other.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().agreeDeclareWarOnOtherWizard (proposer, aiPlayer, other, mom);
		}
		else
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to reject Player ID " + proposer.getPlayerDescription ().getPlayerID () +
				"'s request to declare war on Player ID " + other.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().rejectDeclareWarOnOtherWizard (proposer, aiPlayer, other, mom);
		}
	}
	
	/**
	 * @param proposer Player who proposed that we declare war on another wizard
	 * @param aiPlayer Player who is considering declaring war on another wizard as requeted (us)
	 * @param other The player we are being asked to declare war on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void considerBreakAllianceWithOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the proposer?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfProposer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), proposer.getPlayerDescription ().getPlayerID (), "considerBreakAllianceWithOtherWizard (P)");
		
		// What's our personality?
		final KnownWizardDetails aiWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), aiPlayer.getPlayerDescription ().getPlayerID (), "considerBreakAllianceWithOtherWizard");
		final WizardPersonality aiPersonality = mom.getServerDB ().findWizardPersonality (aiWizard.getWizardPersonalityID (), "considerBreakAllianceWithOtherWizard (W)");
		
		// Make decision
		if (aiPlayerOpinionOfProposer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_AGREE_TO_BREAK_ALLIANCE + aiPersonality.getHostilityModifier ()))
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to agree to Player ID " + proposer.getPlayerDescription ().getPlayerID () +
				"'s request to break their alliance with Player ID " + other.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().agreeBreakAllianceWithOtherWizard (proposer, aiPlayer, other, mom);
		}
		else
		{
			log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " decided to reject Player ID " + proposer.getPlayerDescription ().getPlayerID () +
				"'s request to break their alliance with Player ID " + other.getPlayerDescription ().getPlayerID ());
			getDiplomacyProcessing ().rejectBreakAllianceWithOtherWizard (proposer, aiPlayer, other, mom);
		}
	}
	
	
	/**
	 * @param threatener Player who threatened the AI player
	 * @param aiPlayer Player who has to choose how respond to the threat (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void respondToThreat (final PlayerServerDetails threatener, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// What's our opinion of the wizard threatening us?
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge threatenerPriv = (MomPersistentPlayerPrivateKnowledge) threatener.getPersistentPlayerPrivateKnowledge ();
		
		final DiplomacyWizardDetails aiPlayerOpinionOfThreatener = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), threatener.getPlayerDescription ().getPlayerID (), "respondToThreat (P)");

		// Dislike them for threatening us, regardless of how we respond
		getRelationAI ().penaltyToVisibleRelation (aiPlayerOpinionOfThreatener, DiplomacyAIConstants.RELATION_PENALTY_FOR_THREATENING);

		// Make a list of valid responses
		final List<DiplomacyAction> responses = new ArrayList<DiplomacyAction> ();
		responses.add (DiplomacyAction.IGNORE_THREAT);
		responses.add (DiplomacyAction.DECLARE_WAR_BECAUSE_THREATENED);
		
		// Do we have enough gold to offer?
		final int tier = 1;	// Always be cheap
		final int goldAmount = getResourceValueUtils ().findAmountStoredForProductionType (aiPlayerPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		final int goldOffer = getKnownWizardUtils ().convertGoldOfferTierToAmount (aiPlayerOpinionOfThreatener.getMaximumGoldTribute (), tier + 1);
		if (goldAmount >= goldOffer)
			responses.add (DiplomacyAction.GIVE_GOLD_BECAUSE_THREATENED);
		
		// Do we have a tradeable spell to offer?
		final List<String> spellIDsWeCanOffer = getServerSpellCalculations ().findCheapestSpells (getServerSpellCalculations ().listTradeableSpells
			(aiPlayerPriv.getSpellResearchStatus (), threatenerPriv.getSpellResearchStatus ()), RequestDiplomacyMessageImpl.MAXIMUM_TRADEABLE_SPELLS, mom.getServerDB ());
		if (!spellIDsWeCanOffer.isEmpty ())
			responses.add (DiplomacyAction.GIVE_SPELL_BECAUSE_THREATENED);
		
		// Pick random response
		log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " is being threatened by Player ID " + threatener.getPlayerDescription ().getPlayerID () +
			" and has following choices: " + responses.stream ().map (r -> r.toString ()).collect (Collectors.joining (", ")));
		
		final DiplomacyAction response = responses.get (getRandomUtils ().nextInt (responses.size ()));
		log.debug ("AI player ID " + aiPlayer.getPlayerDescription ().getPlayerID () + " is being threatened by Player ID " + threatener.getPlayerDescription ().getPlayerID () +
			" and made choice " + response);
		
		switch (response)
		{
			case DECLARE_WAR_BECAUSE_THREATENED:
				getDiplomacyProcessing ().declareWarBecauseThreatened (aiPlayer, threatener, mom);
				break;
				
			case GIVE_GOLD_BECAUSE_THREATENED:
				getDiplomacyProcessing ().giveGoldBecauseThreatened (aiPlayer, threatener, mom, tier);
				break;
				
			case GIVE_SPELL_BECAUSE_THREATENED:
				getDiplomacyProcessing ().giveSpellBecauseThreatened (aiPlayer, threatener, mom, spellIDsWeCanOffer.get (0));
				break;

			// Do nothing
			default:
		}
	}

	/**
	 * @param spellIDsWeCanRequest Spells we can request
	 * @param db Lookup lists built over the XML database
	 * @return Spell to request
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	@Override
	public final String chooseSpellToRequest (final List<String> spellIDsWeCanRequest, final CommonDatabase db)
		throws RecordNotFoundException
	{
		String bestSpellID = null;
		Integer bestResearchCost = null;
		
		for (final String spellID : spellIDsWeCanRequest)
		{
			final int thisResearchCost = db.findSpell (spellID, "chooseSpellToRequest").getResearchCost ();
			if ((bestResearchCost == null) || (thisResearchCost > bestResearchCost))
			{
				bestSpellID = spellID;
				bestResearchCost = thisResearchCost;
			}
		}
		
		return bestSpellID;
	}
	
	/**
	 * @param requestSpellID The spell the other wizard wants from us
	 * @param spellIDsWeCanOffer Spells we can request in return
	 * @param db Lookup lists built over the XML database
	 * @return Spell to request in return if there's one we like, or null if all of them would be a bad trade
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	@Override
	public final String chooseSpellToRequestInReturn (final String requestSpellID, final List<String> spellIDsWeCanOffer, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final int requestSpellResearchCost = db.findSpell (requestSpellID, "chooseSpellToRequestInReturn").getResearchCost ();
		
		// Don't request anything cheaper - assume the requester wants the spell they asked for badly enough that we aren't going to give it to them easily
		// but don't request anything ridiculously more expensive either, or they'll never agree to it
		final int minimumResearchCost = requestSpellResearchCost;
		final int maximumResearchCost = requestSpellResearchCost * 2;
		
		// Check each possible spell
		String bestSpellID = null;
		Integer bestResearchCost = null;
		
		for (final String spellID : spellIDsWeCanOffer)
		{
			final int thisResearchCost = db.findSpell (spellID, "chooseSpellToRequestInReturn").getResearchCost ();
			if ((thisResearchCost >= minimumResearchCost) && (thisResearchCost <= maximumResearchCost) &&
				((bestResearchCost == null) || (thisResearchCost > bestResearchCost)))
			{
				bestSpellID = spellID;
				bestResearchCost = thisResearchCost;
			}
		}
		
		return bestSpellID;
	}

	/**
	 * @param weRequestedSpellID Spell ID we requested
	 * @param theyRequestedSpellID Spell ID they want in return
	 * @param db Lookup lists built over the XML database
	 * @return Whether we accept the trade, or false if they requested something way too expensive in return
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	@Override
	public final boolean considerSpellTrade (final String weRequestedSpellID, final String theyRequestedSpellID, final CommonDatabase db) 
		throws RecordNotFoundException
	{
		final int weRequestedResearchCost = db.findSpell (weRequestedSpellID, "considerSpellTrade").getResearchCost ();
		final int theyRequestedResearchCost = db.findSpell (theyRequestedSpellID, "considerSpellTrade").getResearchCost ();
		
		// Accept it as long as it isn't more than 2x as expensive as what we requested
		final boolean accept = (theyRequestedResearchCost <= (weRequestedResearchCost * 2));
		
		if (accept)
			log.debug ("AI player requested spell ID " + weRequestedSpellID + " and agreed to give spell ID " + theyRequestedSpellID + " for it"); 
		else
			log.debug ("AI player requested spell ID " + weRequestedSpellID + " but refused to give spell ID " + theyRequestedSpellID + " for it"); 
			
		return accept;
	}
	
	/**
	 * @return Methods for processing agreed diplomatic actions
	 */
	public final DiplomacyProcessing getDiplomacyProcessing ()
	{
		return diplomacyProcessing;
	}
	
	/**
	 * @param p Methods for processing agreed diplomatic actions
	 */
	public final void setDiplomacyProcessing (final DiplomacyProcessing p)
	{
		diplomacyProcessing = p;
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

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}
}
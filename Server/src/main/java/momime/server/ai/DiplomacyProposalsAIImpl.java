package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.WizardPersonality;
import momime.common.messages.DiplomacyAction;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.PactType;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.messages.process.RequestDiplomacyMessageImpl;

/**
 * During an AI player's turn, works out what diplomacy proposals they may want to initiate to other wizards
 */
public final class DiplomacyProposalsAIImpl implements DiplomacyProposalsAI
{
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;
	
	/** Methods for AI making decisions about diplomacy with other wizards */
	private DiplomacyAI diplomacyAI;
	
	/**
	 * @param aiPlayer AI player whose turn to take
	 * @param talkToPlayer Player they are considering talking to
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of proposals we'd like to make to talkToPlayer, so an empty list if there's nothing we want to talk to them about
	 * @throws RecordNotFoundException If it turns out we don't even know talkToPlayer  
	 * @throws MomException For any other unexpected situations
	 */
	@Override
	public final List<DiplomacyProposal> generateProposals (final PlayerServerDetails aiPlayer, final PlayerServerDetails talkToPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException
	{
		final List<DiplomacyProposal> proposals = new ArrayList<DiplomacyProposal> ();
		
		final MomPersistentPlayerPrivateKnowledge aiPlayerPriv = (MomPersistentPlayerPrivateKnowledge) aiPlayer.getPersistentPlayerPrivateKnowledge ();

		// What's our personality?
		final KnownWizardDetails aiWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), aiPlayer.getPlayerDescription ().getPlayerID (), "generateProposals");
		final WizardPersonality aiPersonality = mom.getServerDB ().findWizardPersonality (aiWizard.getWizardPersonalityID (), "generateProposals");
		
		// Find what pact we currently have with the wizard we're talking to
		final PactType pactType = getKnownWizardUtils ().findPactWith (aiWizard.getPact (), talkToPlayer.getPlayerDescription ().getPlayerID ());
		
		// Find our opinion of the wizard we're talking to
		final DiplomacyWizardDetails aiPlayerOpinionOfTalkToPlayer = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(aiPlayerPriv.getFogOfWarMemory ().getWizardDetails (), talkToPlayer.getPlayerDescription ().getPlayerID (), "generateProposals");
		
		// Compare visibleRelation and our current pact to see if we should try to change it
		if (pactType == null)
		{
			// Should we ask for a wizard pact or an alliance or threaten to declare war on them?
			if (aiPlayerOpinionOfTalkToPlayer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_PROPOSE_ALLIANCE + aiPersonality.getHostilityModifier ()))
				proposals.add (new DiplomacyProposal (DiplomacyAction.PROPOSE_ALLIANCE, null, null));
			else if (aiPlayerOpinionOfTalkToPlayer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_PROPOSE_WIZARD_PACT + aiPersonality.getHostilityModifier ()))
				proposals.add (new DiplomacyProposal (DiplomacyAction.PROPOSE_WIZARD_PACT, null, null));
			else if (aiPlayerOpinionOfTalkToPlayer.getVisibleRelation () <= (DiplomacyAIConstants.MAXIMUM_RELATION_TO_THREATEN + aiPersonality.getHostilityModifier ()))
				proposals.add (new DiplomacyProposal (DiplomacyAction.THREATEN, null, null));
		}
		else
			switch (pactType)
			{
				// If we now dislike the wizard, should we break the alliance?
				case ALLIANCE:
					if (aiPlayerOpinionOfTalkToPlayer.getVisibleRelation () <= (DiplomacyAIConstants.MAXIMUM_RELATION_TO_BREAK_ALLIANCE + aiPersonality.getHostilityModifier ()))
						proposals.add (new DiplomacyProposal (DiplomacyAction.BREAK_ALLIANCE_NICELY, null, null));
					break;

				// If we now dislike the wizard, should we break the wizard pact?
				// Or if we now really like them, should we ask about upgrading to an alliance?
				case WIZARD_PACT:
					if (aiPlayerOpinionOfTalkToPlayer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_PROPOSE_ALLIANCE + aiPersonality.getHostilityModifier ()))
						proposals.add (new DiplomacyProposal (DiplomacyAction.PROPOSE_ALLIANCE, null, null));
					else if (aiPlayerOpinionOfTalkToPlayer.getVisibleRelation () <= (DiplomacyAIConstants.MAXIMUM_RELATION_TO_BREAK_WIZARD_PACT + aiPersonality.getHostilityModifier ()))
						proposals.add (new DiplomacyProposal (DiplomacyAction.BREAK_WIZARD_PACT_NICELY, null, null));
					break;

				// If we now like the wizard, should we ask for a peace treaty?
				case WAR:
					if (aiPlayerOpinionOfTalkToPlayer.getVisibleRelation () >= (DiplomacyAIConstants.MINIMUM_RELATION_TO_PROPOSE_PEACE_TREATY + aiPersonality.getHostilityModifier ()))
						proposals.add (new DiplomacyProposal (DiplomacyAction.PROPOSE_PEACE_TREATY, null, null));
					break;
					
				default:
					throw new MomException ("generateProposals doesn't know what to do with a pact of type " + pactType); 
			}
		
		// If we aren't trying to change pact, and we aren't at war with them, see if we can trade any spells
		if ((proposals.isEmpty ()) && (pactType != PactType.WAR))
		{
			final MomPersistentPlayerPrivateKnowledge talkToPlayerPriv = (MomPersistentPlayerPrivateKnowledge) talkToPlayer.getPersistentPlayerPrivateKnowledge ();
			
			final List<String> spellIDsWeCanOffer = getServerSpellCalculations ().findCheapestSpells (getServerSpellCalculations ().listTradeableSpells
				(aiPlayerPriv.getSpellResearchStatus (), talkToPlayerPriv.getSpellResearchStatus ()), RequestDiplomacyMessageImpl.MAXIMUM_TRADEABLE_SPELLS, mom.getServerDB ());

			final List<String> spellIDsWeCanRequest = getServerSpellCalculations ().findCheapestSpells (getServerSpellCalculations ().listTradeableSpells
				(talkToPlayerPriv.getSpellResearchStatus (), aiPlayerPriv.getSpellResearchStatus ()), RequestDiplomacyMessageImpl.MAXIMUM_TRADEABLE_SPELLS, mom.getServerDB ());
			
			if ((!spellIDsWeCanOffer.isEmpty ()) && (!spellIDsWeCanRequest.isEmpty ()))
			{
				final String requestSpellID = getDiplomacyAI ().chooseSpellToRequest (spellIDsWeCanRequest, mom.getServerDB ());
				proposals.add (new DiplomacyProposal (DiplomacyAction.PROPOSE_EXCHANGE_SPELL, null, requestSpellID));
			}
		}
		
		return proposals;
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
	 * @return Methods for AI making decisions about diplomacy with other wizards
	 */
	public final DiplomacyAI getDiplomacyAI ()
	{
		return diplomacyAI;
	}

	/**
	 * @param ai Methods for AI making decisions about diplomacy with other wizards
	 */
	public final void setDiplomacyAI (final DiplomacyAI ai)
	{
		diplomacyAI = ai;
	}
}
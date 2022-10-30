package momime.server.ai;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.WizardPersonality;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;
import momime.server.process.DiplomacyProcessing;

/**
 * Methods for AI making decisions about diplomacy with other wizards
 */
public final class DiplomacyAIImpl implements DiplomacyAI
{
	/** Methods for processing agreed diplomatic actions */
	private DiplomacyProcessing diplomacyProcessing; 
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
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
			getDiplomacyProcessing ().agreeWizardPact (proposer, aiPlayer, mom);
		else
			getDiplomacyProcessing ().rejectWizardPact (proposer, aiPlayer, mom);
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
			getDiplomacyProcessing ().agreeAlliance (proposer, aiPlayer, mom);
		else
			getDiplomacyProcessing ().rejectAlliance (proposer, aiPlayer, mom);
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
			getDiplomacyProcessing ().agreePeaceTreaty (proposer, aiPlayer, mom);
		else
			getDiplomacyProcessing ().rejectPeaceTreaty (proposer, aiPlayer, mom);
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
			getDiplomacyProcessing ().agreeDeclareWarOnOtherWizard (proposer, aiPlayer, other, mom);
		else
			getDiplomacyProcessing ().rejectDeclareWarOnOtherWizard (proposer, aiPlayer, other, mom);
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
			getDiplomacyProcessing ().agreeBreakAllianceWithOtherWizard (proposer, aiPlayer, other, mom);
		else
			getDiplomacyProcessing ().rejectBreakAllianceWithOtherWizard (proposer, aiPlayer, other, mom);
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
}
package momime.server.process;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
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
	 * @param agreer Player who rejected the wizard pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectWizardPact (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPact (proposer, agreer, mom, DiplomacyAction.REJECT_WIZARD_PACT);
	}
	
	/**
	 * @param proposer Player who proposed the alliance
	 * @param agreer Player who rejected the alliance
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectAlliance (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPact (proposer, agreer, mom, DiplomacyAction.REJECT_ALLIANCE);
	}
	
	/**
	 * @param proposer Player who proposed the peace treaty
	 * @param agreer Player who rejected the peace treaty
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void rejectPeaceTreaty (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		rejectPact (proposer, agreer, mom, DiplomacyAction.REJECT_PEACE_TREATY);
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
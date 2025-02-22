package momime.server.process;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Methods for processing agreed diplomatic actions.  Attempting to keep this consistent for all situations,
 * so in all methods the proposer could be a human or AI player, and so could the agreer (and so could the thirdParty, if applicable).
 */
public interface DiplomacyProcessing
{
	/**
	 * @param humanPlayer Human player we want to talk to
	 * @param aiPlayer AI player who wants to talk to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void requestTalking (final PlayerServerDetails humanPlayer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param requester Player who wanted to talk
	 * @param agreer Player who agreed to talk to them
	 * @param patienceRunningOut Whether agreeing reluctantly and have limited patience to talk to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void agreeTalking (final PlayerServerDetails requester, final PlayerServerDetails agreer, final boolean patienceRunningOut, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param requester Player who wanted to talk
	 * @param rejecter Player who refused to talk to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void rejectTalking (final PlayerServerDetails requester, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param requester Player who wanted to talk
	 * @param rejecter Player who is fed up talking to them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void grownImpatient (final PlayerServerDetails requester, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the wizard pact
	 * @param agreer Player who agreed to the wizard pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void agreeWizardPact (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the alliance
	 * @param agreer Player who agreed to the alliance
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void agreeAlliance (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the peace treaty
	 * @param agreer Player who agreed to the peace treaty
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void agreePeaceTreaty (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who is breaking the wizard pact
	 * @param agreer Player who they had the wizard pact with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void breakWizardPactNicely (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who is breaking the alliance
	 * @param agreer Player who they had the alliance with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void breakAllianceNicely (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
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
	public void declareWarBecauseThreatened (final PlayerServerDetails declarer, final PlayerServerDetails threatener, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the wizard pact
	 * @param rejecter Player who rejected the wizard pact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void rejectWizardPact (final PlayerServerDetails proposer, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the alliance
	 * @param rejecter Player who rejected the alliance
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void rejectAlliance (final PlayerServerDetails proposer, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the peace treaty
	 * @param rejecter Player who rejected the peace treaty
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void rejectPeaceTreaty (final PlayerServerDetails proposer, final PlayerServerDetails rejecter, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param proposer Player who proposed that one wizard declare war on another
	 * @param agreer Player who agreed to declare war
	 * @param other Player who war was declared on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void agreeDeclareWarOnOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
		
	/**
	 * @param proposer Player who proposed that one wizard break their alliance with another
	 * @param agreer Player who agreed to break their alliance
	 * @param other Player who they had the alliance with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void agreeBreakAllianceWithOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param proposer Player who proposed the declaration of war
	 * @param agreer Player who rejected the declaration of war
	 * @param other Player who they wanted war declared on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void rejectDeclareWarOnOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other,
		final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param proposer Player who proposed the breaking allance
	 * @param agreer Player who rejected breaking their alliance
	 * @param other Player who they have an alliance with
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void rejectBreakAllianceWithOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final PlayerServerDetails other,
		final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param giver Player who is giving gold
	 * @param receiver Player who is receiving gold
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param offerGoldTier Gold offer tier 1..4
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void giveGold (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final int offerGoldTier)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param giver Player who is giving gold
	 * @param receiver Player who threatened them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param offerGoldTier Gold offer tier 1..4
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void giveGoldBecauseThreatened (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final int offerGoldTier)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param giver Player who is giving a spell
	 * @param receiver Player who is receiving a spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param spellID Spell being given
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void giveSpell (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final String spellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param giver Player who is giving a spell
	 * @param receiver Player who threatened them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param spellID Spell being given
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void giveSpellBecauseThreatened (final PlayerServerDetails giver, final PlayerServerDetails receiver, final MomSessionVariables mom, final String spellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

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
	public void agreeTradeSpells (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom,
		final String proposerWantsSpellID, final String agreerWantsSpellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

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
	public void rejectTradeSpells (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom,
		final String proposerWantsSpellID, final String agreerWantsSpellID)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param ignorer Player who ignored the threat
	 * @param threatener Player who threatened them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void ignoreThreat (final PlayerServerDetails ignorer, final PlayerServerDetails threatener, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;	
}
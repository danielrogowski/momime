package momime.server.process;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.DiplomacyAction;
import momime.server.MomSessionVariables;

/**
 * Methods for processing agreed diplomatic actions.  Attempting to keep this consistent for all situations,
 * so in all methods the proposer could be a human or AI player, and so could the agreer (and so could the thirdParty, if applicable).
 */
public interface DiplomacyProcessing
{
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
	 * @param proposerAction Diplomacy action to send back to the proposer, if they are a human player
	 * @param agreerAction Diplomacy action to send back to the agreer, if they are a human player
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void tradeSpells (final PlayerServerDetails proposer, final PlayerServerDetails agreer, final MomSessionVariables mom,
		final String proposerWantsSpellID, final String agreerWantsSpellID, final DiplomacyAction proposerAction, final DiplomacyAction agreerAction)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
}
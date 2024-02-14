package momime.server.ai;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.DiplomacyAction;
import momime.server.MomSessionVariables;

/**
 * Methods for AI making decisions about diplomacy with other wizards
 */
public interface DiplomacyAI
{
	/**
	 * @param requester Player who wants to talk to us
	 * @param aiPlayer Player who is considering agreeing talking to them or not (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we agree to talk to them or not (note exactly what the caller needs to do based on this depends on whether the requester is a human or AI player)\
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public boolean decideWhetherWillTalkTo (final PlayerServerDetails requester, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
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
	public boolean willListenToRequest (final PlayerServerDetails requester, final PlayerServerDetails aiPlayer, final DiplomacyAction request, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the wizard pact
	 * @param aiPlayer Player who is considering accepting or rejecting the wizard pact (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void considerWizardPact (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed the alliance
	 * @param aiPlayer Player who is considering accepting or rejecting the alliance (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void considerAlliance (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param proposer Player who proposed the peace treaty
	 * @param aiPlayer Player who is considering accepting or rejecting the peace treaty (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void considerPeaceTreaty (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed that we declare war on another wizard
	 * @param aiPlayer Player who is considering declaring war on another wizard as requeted (us)
	 * @param other The player we are being asked to declare war on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void considerDeclareWarOnOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param proposer Player who proposed that we declare war on another wizard
	 * @param aiPlayer Player who is considering declaring war on another wizard as requeted (us)
	 * @param other The player we are being asked to declare war on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void considerBreakAllianceWithOtherWizard (final PlayerServerDetails proposer, final PlayerServerDetails aiPlayer, final PlayerServerDetails other, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param threatener Player who threatened the AI player
	 * @param aiPlayer Player who has to choose how respond to the threat (us)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void respondToThreat (final PlayerServerDetails threatener, final PlayerServerDetails aiPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;

	/**
	 * @param spellIDsWeCanRequest Spells we can request
	 * @param db Lookup lists built over the XML database
	 * @return Spell to request
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	public String chooseSpellToRequest (final List<String> spellIDsWeCanRequest, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * @param requestSpellID The spell the other wizard wants from us
	 * @param spellIDsWeCanOffer Spells we can request in return
	 * @param db Lookup lists built over the XML database
	 * @return Spell to request in return if there's one we like, or null if all of them would be a bad trade
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	public String chooseSpellToRequestInReturn (final String requestSpellID, final List<String> spellIDsWeCanOffer, final CommonDatabase db)
		throws RecordNotFoundException;

	/**
	 * @param weRequestedSpellID Spell ID we requested
	 * @param theyRequestedSpellID Spell ID they want in return
	 * @param db Lookup lists built over the XML database
	 * @return Whether we accept the trade, or false if they requested something way too expensive in return
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	public boolean considerSpellTrade (final String weRequestedSpellID, final String theyRequestedSpellID, final CommonDatabase db) 
		throws RecordNotFoundException;
}
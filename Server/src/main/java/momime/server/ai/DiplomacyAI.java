package momime.server.ai;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Methods for AI making decisions about diplomacy with other wizards
 */
public interface DiplomacyAI
{
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
	 * @param requestSpellID The spell the other wizard wants from us
	 * @param spellIDsWeCanOffer Spells we can request in return
	 * @param db Lookup lists built over the XML database
	 * @return Spell to request in return if there's one we like, or null if all of them would be a bad trade
	 * @throws RecordNotFoundException If we can't find one of the spell IDs in the database
	 */
	public String chooseSpellToRequestInReturn (final String requestSpellID, final List<String> spellIDsWeCanOffer, final CommonDatabase db)
		throws RecordNotFoundException;
}
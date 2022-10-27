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
}
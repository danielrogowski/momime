package momime.server.ai;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * During an AI player's turn, works out what diplomacy proposals they may want to initiate to other wizards
 */
public interface DiplomacyProposalsAI
{
	/**
	 * @param aiPlayer AI player whose turn to take
	 * @param talkToPlayer Player they are considering talking to
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of proposals we'd like to make to talkToPlayer, so an empty list if there's nothing we want to talk to them about
	 * @throws RecordNotFoundException If it turns out we don't even know talkToPlayer  
	 * @throws MomException For any other unexpected situations
	 */
	public List<DiplomacyProposal> generateProposals (final PlayerServerDetails aiPlayer, final PlayerServerDetails talkToPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException;
	
	/**
	 * @param aiPlayer AI player whose turn it is
	 * @param talkToPlayer Player they are talking to
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void sendNextProposal (final PlayerServerDetails aiPlayer, final PlayerServerDetails talkToPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}
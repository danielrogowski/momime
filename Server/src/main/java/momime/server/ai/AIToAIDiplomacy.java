package momime.server.ai;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Methods for processing diplomacy requests from AI player to another AI player
 */
public interface AIToAIDiplomacy
{
	/**
	 * @param player AI player whose turn it is, who is making diplomatic proposals
	 * @param talkToPlayer AI player they are making proposals to
	 * @param proposals List of proposals to make
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If we run into a proposal there is no code for
	 */
	public void submitProposals (final PlayerServerDetails player, final PlayerServerDetails talkToPlayer, final List<DiplomacyProposal> proposals, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException;
}
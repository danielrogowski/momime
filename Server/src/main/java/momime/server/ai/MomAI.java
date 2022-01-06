package momime.server.ai;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.messages.NewTurnMessageOffer;
import momime.server.MomSessionVariables;

/**
 * Overall AI strategy + control
 */
public interface MomAI
{
	/**
	 *
	 * @param player AI player whose turn to take
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether AI turn was fully completed or not; false if we the AI initated a combat in a one-player-at-a-time game and must resume their turn after the combat ends 
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	public boolean aiPlayerTurn (final PlayerServerDetails player, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * AI player decides whether to accept an offer.  Assumes we've already validated that they can afford it.
	 * 
	 * @param player Player who is accepting an offer
	 * @param offer Offer being accepted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	public void decideOffer (final PlayerServerDetails player, final NewTurnMessageOffer offer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}
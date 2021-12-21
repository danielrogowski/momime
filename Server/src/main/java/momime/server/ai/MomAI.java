package momime.server.ai;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
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
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public boolean aiPlayerTurn (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * AI player decides whether to accept an offer.  Assumes we've already validated that they can afford it.
	 * 
	 * @param player Player who is accepting an offer
	 * @param offer Offer being accepted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If there is a validation problem
	 */
	public void decideOffer (final PlayerServerDetails player, final NewTurnMessageOffer offer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException;
}
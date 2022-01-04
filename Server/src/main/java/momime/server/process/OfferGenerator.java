package momime.server.process;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.NewTurnMessageOffer;
import momime.common.messages.NewTurnMessageOfferHero;
import momime.common.messages.NewTurnMessageOfferItem;
import momime.common.messages.NewTurnMessageOfferUnits;
import momime.server.MomSessionVariables;

/**
 * Generates offers to hire heroes, mercenaries and buy hero items
 */
public interface OfferGenerator
{
	/**
	 * Tests to see if the player has any heroes they can get, and if so rolls a chance that one offers to join them this turn (for a fee).
	 * 
	 * @param player Player to check for hero offer for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return The offer that was generate if there was one, otherwise null
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public NewTurnMessageOfferHero generateHeroOffer (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
	
	/**
	 * Randomly checks if the player gets an offer to hire mercenary units.
	 * 
	 * @param player Player to check for units offer for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return The offer that was generate if there was one, otherwise null
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public NewTurnMessageOfferUnits generateUnitsOffer (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Randomly checks if the player gets an offer to buy a hero item.
	 * 
	 * @param player Player to check for item offer for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return The offer that was generate if there was one, otherwise null
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public NewTurnMessageOfferItem generateItemOffer (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Processes accepting an offer.  Assumes we've already validated that the offer is genuine (the client didn't make it up) and that they can afford it.
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
	public void acceptOffer (final PlayerServerDetails player, final NewTurnMessageOffer offer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException;
}
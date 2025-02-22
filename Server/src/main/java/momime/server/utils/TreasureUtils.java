package momime.server.utils;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.TreasureRewardMessage;
import momime.server.MomSessionVariables;

/**
 * Methods dealing with choosing treasure to reward to a player for capturing a lair/node/tower
 */
public interface TreasureUtils
{
	/**
	 * Awards treasure when a node/lair/tower is captured.  See very lengthy description about the process on the method impl.
	 * 
	 * NB. This routine will grant whatever bonuses it awards, i.e. it will add books/retorts to the player's picks, it will mark spells as available,
	 * it will add hero items to their bank and gold/mana to their resources.  But it doesn't generate messages to inform the player of that.
	 * It makes things too complicated if we *don't* grant the bonuses here, because then on the next loop we'd have to be considering
	 * treasure already awarded.  e.g. we get a life book first, then another special - when consdiering whether we can get a death book or
	 * Divine Power, we'd have to consider both the picks the player already has, plus picks already awarded.  So just awarding them
	 * immediately simplies this.
	 * 
	 * The exception to this is that prisoners are added via the regular FOW routines and so the client does get notified of the unit update.
	 * 
	 * The other thing it will not do is, should the rewards include any additional spell books, it won't go through the process of marking
	 * which additional spells the wizard now has available to research, or available to win from further treasure rewards.  If a spell is rewarded that
	 * was previously "researchable now", it will also not pick which new spell becomes "researchable now" to get us back to 8 choices.
	 * Since special/pick rewards are allocated last, we don't have the complication to worry about whereby we might e.g. get a life book
	 * as a first pick, and then as a second pick get a life spell that is only available to us because of the book we just got.
	 *
	 * @param treasureValue Amount of treasure to award
	 * @param player Player who captured the lair/node/tower
	 * @param lairNodeTowerLocation The location of where the lair/node/tower was
	 * @param tileTypeID The tile type that the lair/node/tower was, before it was possibly altered/removed by capturing it
	 * @param mapFeatureID The map feature that the lair/node/tower was, before it was possibly altered/removed by capturing it (will be null for nodes/towers)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Details of all rewards given (pre-built message ready to send back to client)
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	public TreasureRewardMessage rollTreasureReward (final int treasureValue, final PlayerServerDetails player,
		final MapCoordinates3DEx lairNodeTowerLocation, final String tileTypeID, final String mapFeatureID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;

	/**
	 * When Rampaging Monsters ruin a city, the gold the player lost is waiting in the ruin to be reclaimed.  So then they get the gold
	 * as a fixed treasure reward rather than the usual treasure rolling method above.
	 * 
	 * @param goldAmount Amount of gold to award
	 * @param player Player who recaptured the ruin
	 * @param lairNodeTowerLocation The location of where the ruin was
	 * @param tileTypeID The tile type that the ruin was, before it was possibly altered/removed by capturing it
	 * @param mapFeatureID The map feature that the ruin was, before it was possibly altered/removed by capturing it (will be null for nodes/towers)
	 * @return Details of all rewards given (pre-built message ready to send back to client)
	 */
	public TreasureRewardMessage giveGoldInRuin (final int goldAmount, final PlayerServerDetails player,
		final MapCoordinates3DEx lairNodeTowerLocation, final String tileTypeID, final String mapFeatureID);
	
	/**
	 * Sends the reward info to the client, including all separate messages to e.g. add hero items and so on.
	 * 
	 * @param reward Details of treasure reward to send
	 * @param player Player who earned the reward
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	public void sendTreasureReward (final TreasureRewardMessage reward, final PlayerServerDetails player, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException;
}
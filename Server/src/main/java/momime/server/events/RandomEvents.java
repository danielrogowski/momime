package momime.server.events;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.servertoclient.AttackCitySpellResult;
import momime.server.MomSessionVariables;

/**
 * Rolls random events
 */
public interface RandomEvents
{
	/**
	 * Rolls to see if server should trigger a random event this turn 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 * @throws IOException If there is another kind of problem
	 */
	public void rollRandomEvent (final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * Rolls to see if an active event with a duration should end 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	public void rollToEndRandomEvents (final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
	
	/**
	 * @param eventID Which kind of event it is
	 * @param targetPlayerID If its an event that targets a wizard, then who was targeted
	 * @param citySizeID If its an event that targets a city, then the size of the city (since all players receiving the message may not be able to see the city)
	 * @param cityName If its an event that targets a city, then the name of the city (since all players receiving the message may not be able to see the city)
	 * @param mapFeatureID If its an event that targets a city mineral deposit, then which kind of mineral it is
	 * @param heroItemName If its an event that grants a hero item, then the name of the item
	 * @param goldAmount If its an event that takes or gives gold, then how much gold
	 * @param conjunction Tells the client to update their conjunctionEventID
	 * @param ending Whether we're broadcasting the start or end of the event
	 * @param players List of players in the session
	 * @param attackCitySpellResult Counts of how many units, buildings and population were killed by Earthquake or Great Meteor 
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	public void sendRandomEventMessage (final String eventID, final Integer targetPlayerID, final String citySizeID, final String cityName,
		final String mapFeatureID, final String heroItemName, final Integer goldAmount, final boolean conjunction, final boolean ending,
		final List<PlayerServerDetails> players, final AttackCitySpellResult attackCitySpellResult)
		throws JAXBException, XMLStreamException;
}
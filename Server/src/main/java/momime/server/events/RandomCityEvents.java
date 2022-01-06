package momime.server.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Deals with random events which pick a city target:
 * Great Meteor, Earthquake, Plague, Rebellion, Depletion, New Minerals, Population Boom
 * Also Diplomatic Marriage, but here we're picking a raider city rather than one owned by a wizard
 */
public interface RandomCityEvents
{
	/**
	 * Can only call this on events that are targeted at cities
	 * 
	 * @param event Event we want to find a target for
	 * @param cityLocation Location we are considering for the event
	 * @param cityOwnerID Will only consider cities with the specified owner
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the city is a valid target for the event or not
	 * @throws RecordNotFoundException If we can't find the definition for a unit stationed in the city
	 */
	public boolean isCityValidTargetForEvent (final Event event, final MapCoordinates3DEx cityLocation, final int cityOwnerID, final MomSessionVariables mom)
		throws RecordNotFoundException;
	
	/**
	 * @param event Event to trigger
	 * @param targetWizard Wizard the event is being triggered for
	 * @param cityLocation City the event is being triggered for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 * @throws IOException If there is another kind of problem
	 */
	public void triggerCityEvent (final Event event, final PlayerServerDetails targetWizard, final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
	
	/**
	 * @param event Event to switch off
	 * @param cityLocation City the event is being switched off for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	public void cancelCityEvent (final Event event, final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException;
}
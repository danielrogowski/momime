package momime.server.events;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.server.MomSessionVariables;

/**
 * Deals with random events which pick a wizard target:
 * Great Meteor, The Gift, Diplomatic Marriage, Earthquake, Piracy, Plague, Rebellion, Donation, Depletion, New Minerals, Population Boom
 */
public interface RandomWizardEvents
{
	/**
	 * Can only call this on events that are targeted at wizards
	 * 
	 * @param event Event we want to find a target for
	 * @param player Wizard we are considering for the event
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the wizard is a valid target for the event or not
	 * @throws RecordNotFoundException If we can't find the definition for a unit stationed in one of the wizard's cities
	 */
	public boolean isWizardValidTargetForEvent (final Event event, final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException;
	
	/**
	 * @param event Event to trigger
	 * @param targetWizard Wizard the event is being triggered for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 * @throws IOException If there is another kind of problem
	 */
	public void triggerWizardEvent (final Event event, final PlayerServerDetails targetWizard, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException;
}
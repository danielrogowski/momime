package momime.server.utils;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PlayerPick;
import momime.common.messages.WizardState;
import momime.server.MomSessionVariables;

/**
 * Process for making sure one wizard has met another wizard
 */
public interface KnownWizardServerUtils
{
	/**
	 * @param metWizardID The wizard who has become known
	 * @param meetingWizardID The wizard who now knows them; if null then everybody now knows them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param showAnimation Whether to show animation popup of wizard announcing themselves to you
	 * @throws RecordNotFoundException If we can't find the wizard we are meeting
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void meetWizard (final int metWizardID, final Integer meetingWizardID, final boolean showAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * @param src List of picks to copy from
	 * @param dest List of picks to copy to
	 */
	public void copyPickList (final List<PlayerPick> src, final List<PlayerPick> dest);
	
	/**
	 * Updates all copies of a wizard state on the server.  Does not notify clients of the change.
	 * 
	 * @param playerID Player whose state changed
	 * @param newState New state
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the player in the server's true wizard details
	 */
	public void updateWizardState (final int playerID, final WizardState newState, final MomSessionVariables mom)
		throws RecordNotFoundException;

	/**
	 * Picks have been updated in server's true memory.  Now they need copying to the player memory of each player who knows that wizard, and sending to the clients.
	 * 
	 * @param playerID Player whose picks changed.
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the player in the server's true wizard details
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void copyAndSendUpdatedPicks (final int playerID, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException,XMLStreamException;
}
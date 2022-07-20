package momime.server.utils;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.session.PlayerNotFoundException;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.PactType;
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
	 * @throws PlayerNotFoundException If we can't find the player we are meeting
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public void meetWizard (final int metWizardID, final Integer meetingWizardID, final boolean showAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException;
	
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

	/**
	 * During the start phase, when resources are recalculated, this stores the power base of each wizard, which is public info to every player via the Historian screen.
	 * 
	 * @param onlyOnePlayerID If zero, will record power base for all players; if specified will record power base only for the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If one of the wizard isn't found in the list
	 * @throws PlayerNotFoundException If we can't find one of the players to send the messages out to
	 */
	public void storePowerBaseHistory (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException;
	
	/**
	 * This only updates the pact of the specified player; since pacts are two-way, the caller
	 * must therefore always call this method twice, switching the player params around.
	 * 
	 * @param updatePlayerID Player whose pact list is being updated
	 * @param pactPlayerID Who they have the pact with
	 * @param pactType New type of pact; null is fine and just means previous pact is now cancelled
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	public void updatePact (final int updatePlayerID, final int pactPlayerID, final PactType pactType, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException;
	
	/**
	 * Sets flag everywhere in server side memory
	 * 
	 * @param castingPlayerID Player who started casting Spell of Mastery
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 */
	public void setEverStartedCastingSpellOfMastery (final int castingPlayerID, final MomSessionVariables mom)
		throws RecordNotFoundException; 
}
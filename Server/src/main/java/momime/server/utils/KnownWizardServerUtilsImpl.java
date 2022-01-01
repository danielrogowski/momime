package momime.server.utils;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.servertoclient.MeetWizardMessage;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;

/**
 * Process for making sure one wizard has met another wizard
 */
public final class KnownWizardServerUtilsImpl implements KnownWizardServerUtils
{
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @param metWizardID The wizard who has become known
	 * @param meetingWizardID The wizard who now knows them; if null then everybody now knows them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param showAnimation Whether to show animation popup of wizard announcing themselves to you
	 * @throws RecordNotFoundException If we can't find the wizard we are meeting
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void meetWizard (final int metWizardID, final Integer meetingWizardID, final boolean showAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		final KnownWizardDetails metWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueWizardDetails (), metWizardID, "meetWizard");		
		
		// Go through each player who gets to meet them
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((meetingWizardID == null) || (meetingWizardID.equals (player.getPlayerDescription ().getPlayerID ())))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				
				// Do they already know them?
				if (getKnownWizardUtils ().findKnownWizardDetails (priv.getKnownWizardDetails (), metWizardID) == null)
				{
					// On the server, remember these wizard have now met; make a separate copy of the object
					final KnownWizardDetails knownWizardDetails = new KnownWizardDetails ();
					knownWizardDetails.setPlayerID (metWizardID);
					knownWizardDetails.setWizardID (metWizard.getWizardID ());

					priv.getKnownWizardDetails ().add (knownWizardDetails);
					
					// Tell the player the wizard they chose was OK; in that way they get their copy of their own KnownWizardDetails record
					if (player.getPlayerDescription ().isHuman ())
					{
						final MeetWizardMessage meet = new MeetWizardMessage ();
						meet.setKnownWizardDetails (knownWizardDetails);
						
						if (showAnimation)
							meet.setShowAnimation (true);
						
						player.getConnection ().sendMessageToClient (meet);
					}
				}
			}
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}
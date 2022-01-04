package momime.server.utils;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.MeetWizardMessage;
import momime.common.messages.servertoclient.ReplacePicksMessage;
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
		final KnownWizardDetails metWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), metWizardID, "meetWizard");		
		
		// Go through each player who gets to meet them
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((meetingWizardID == null) || (meetingWizardID.equals (player.getPlayerDescription ().getPlayerID ())))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				
				// Do they already know them?
				if (getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), metWizardID) == null)
				{
					// On the server, remember these wizard have now met; make a separate copy of the object
					final KnownWizardDetails knownWizardDetails = new KnownWizardDetails ();
					knownWizardDetails.setPlayerID (metWizardID);
					knownWizardDetails.setWizardID (metWizard.getWizardID ());
					knownWizardDetails.setStandardPhotoID (metWizard.getStandardPhotoID ());
					knownWizardDetails.setCustomPhoto (metWizard.getCustomPhoto ());
					knownWizardDetails.setCustomFlagColour (metWizard.getCustomFlagColour ());
					knownWizardDetails.setWizardState (metWizard.getWizardState ());
					copyPickList (metWizard.getPick (), knownWizardDetails.getPick ());

					priv.getFogOfWarMemory ().getWizardDetails ().add (knownWizardDetails);
					
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
	 * @param src List of picks to copy from
	 * @param dest List of picks to copy to
	 */
	@Override
	public final void copyPickList (final List<PlayerPick> src, final List<PlayerPick> dest)
	{
		dest.clear ();
		for (final PlayerPick srcPick : src)
		{
			final PlayerPick destPick = new PlayerPick ();
			destPick.setPickID (srcPick.getPickID ());
			destPick.setQuantity (srcPick.getQuantity ());
			destPick.setOriginalQuantity (srcPick.getOriginalQuantity ());
			dest.add (destPick);
		}					
	}
	
	/**
	 * Updates all copies of a wizard state on the server.  Does not notify clients of the change.
	 * 
	 * @param playerID Player whose state changed
	 * @param newState New state
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the player in the server's true wizard details
	 */
	@Override
	public final void updateWizardState (final int playerID, final WizardState newState, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		// True memory
		getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), playerID, "updateWizardState").setWizardState (newState);
		
		// Each player who knows them
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), playerID);
			if (wizardDetails != null)
				wizardDetails.setWizardState (newState);
		}
	}
	
	/**
	 * Picks have been updated in server's true memory.  Now they need copying to the player memory of each player who knows that wizard, and sending to the clients.
	 * 
	 * @param playerID Player whose picks changed.
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the player in the server's true wizard details
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void copyAndSendUpdatedPicks (final int playerID, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException,XMLStreamException
	{
		// True memory
		final KnownWizardDetails trueWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), playerID, "copyAndSendUpdatedPicks");
		
		// Build message - resend all of them, not just the new ones
		final ReplacePicksMessage msg = new ReplacePicksMessage ();
		msg.setPlayerID (playerID);
		msg.getPick ().addAll (trueWizard.getPick ());
		
		// Each player who knows them
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), playerID);
			if (wizardDetails != null)
			{
				copyPickList (trueWizard.getPick (), wizardDetails.getPick ());
				if (player.getPlayerDescription ().isHuman ())
					player.getConnection ().sendMessageToClient (msg);
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
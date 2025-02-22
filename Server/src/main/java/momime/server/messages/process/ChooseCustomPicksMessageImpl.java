package momime.server.messages.process;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.clienttoserver.ChooseCustomPicksMessage;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;
import momime.server.utils.KnownWizardServerUtils;
import momime.server.utils.PlayerPickServerUtils;

/**
 * Client telling Server about all the custom picks they've chosen
 */
public final class ChooseCustomPicksMessageImpl extends ChooseCustomPicksMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ChooseCustomPicksMessageImpl.class);
	
	/** Server-only pick utils */
	private PlayerPickServerUtils playerPickServerUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 * @throws RecordNotFoundException If the player has picks which we can't find in the cache, or the AI player chooses a spell which we can't then find in their list
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, MomException, RecordNotFoundException
	{
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Validate the requested picks
		final String error = getPlayerPickServerUtils ().validateCustomPicks (sender, getPick (), mom.getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks (), mom);
		if (error != null)
		{
			// Return error
			log.warn ("process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Record custom picks on server
			final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) sender.getTransientPlayerPrivateKnowledge ();
			priv.setCustomPicksChosen (true);
			
			final KnownWizardDetails trueWizardDetails = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
				sender.getPlayerDescription ().getPlayerID (), "ChooseCustomPicksMessageImpl");

			for (final PickAndQuantity srcPick : getPick ())
			{
				final PlayerPick destPick = new PlayerPick ();
				destPick.setPickID (srcPick.getPickID ());
				destPick.setQuantity (srcPick.getQuantity ());
				destPick.setOriginalQuantity (srcPick.getQuantity ());
				trueWizardDetails.getPick ().add (destPick);
			}

			// Send picks to the player - they need to know their own picks so they know whether they're allowed to pick a Myrran race not
			getKnownWizardServerUtils ().copyAndSendUpdatedPicks (sender.getPlayerDescription ().getPlayerID (), mom);

			// Tell client to either pick free starting spells or pick a race, depending on whether the pre-defined wizard chosen has >1 of any kind of book
			// Its fine to do this before we confirm to the client that their wizard choice was OK by the mmChosenWizard message sent below
			log.debug ("process: About to search for first realm (if any) where human player " + sender.getPlayerDescription ().getPlayerName () + " gets free spells");
			final ChooseInitialSpellsNowMessage chooseSpellsMsg = getPlayerPickServerUtils ().findRealmIDWhereWeNeedToChooseFreeSpells (sender, mom);
			if (chooseSpellsMsg != null)
				sender.getConnection ().sendMessageToClient (chooseSpellsMsg);
			else
				sender.getConnection ().sendMessageToClient (new ChooseYourRaceNowMessage ());
		}
	}

	/**
	 * @return Server-only pick utils
	 */
	public final PlayerPickServerUtils getPlayerPickServerUtils ()
	{
		return playerPickServerUtils;
	}

	/**
	 * @param utils Server-only pick utils
	 */
	public final void setPlayerPickServerUtils (final PlayerPickServerUtils utils)
	{
		playerPickServerUtils = utils;
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

	/**
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}
}
package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.WizardPick;
import momime.common.messages.clienttoserver.v0_9_5.ChooseCustomPicksMessage;
import momime.common.messages.servertoclient.v0_9_5.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.v0_9_5.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.v0_9_5.ReplacePicksMessage;
import momime.common.messages.servertoclient.v0_9_5.TextPopupMessage;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.PlayerPick;
import momime.server.MomSessionVariables;
import momime.server.utils.PlayerPickServerUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client telling Server about all the custom picks they've chosen
 */
public final class ChooseCustomPicksMessageImpl extends ChooseCustomPicksMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChooseCustomPicksMessageImpl.class);
	
	/** Server-only pick utils */
	private PlayerPickServerUtils playerPickServerUtils;
	
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
		log.trace ("Entering process: Player ID " + sender.getPlayerDescription ().getPlayerID () + ", " + getPick ().size ());

		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Validate the requested picks
		final String error = getPlayerPickServerUtils ().validateCustomPicks (sender, getPick (), mom.getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks (), mom.getServerDB ());
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
			final MomPersistentPlayerPublicKnowledge ppk = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
			final MomTransientPlayerPrivateKnowledge priv = (MomTransientPlayerPrivateKnowledge) sender.getTransientPlayerPrivateKnowledge ();
			priv.setCustomPicksChosen (true);

			for (final WizardPick srcPick : getPick ())
			{
				final PlayerPick destPick = new PlayerPick ();
				destPick.setPickID (srcPick.getPick ());
				destPick.setQuantity (srcPick.getQuantity ());
				destPick.setOriginalQuantity (srcPick.getQuantity ());
				ppk.getPick ().add (destPick);
			}

			// Send picks to the player - they need to know their own picks so they know whether they're allowed to pick a Myrran race not
			// We don't send picks to other players until the game is starting up
			if (sender.getPlayerDescription ().isHuman ())
			{
				final ReplacePicksMessage picksMsg = new ReplacePicksMessage ();
				picksMsg.setPlayerID (sender.getPlayerDescription ().getPlayerID ());
				picksMsg.getPick ().addAll (ppk.getPick ());
				sender.getConnection ().sendMessageToClient (picksMsg);
			}

			// Tell client to either pick free starting spells or pick a race, depending on whether the pre-defined wizard chosen has >1 of any kind of book
			// Its fine to do this before we confirm to the client that their wizard choice was OK by the mmChosenWizard message sent below
			log.debug ("process: About to search for first realm (if any) where human player " + sender.getPlayerDescription ().getPlayerName () + " gets free spells");
			final ChooseInitialSpellsNowMessage chooseSpellsMsg = getPlayerPickServerUtils ().findRealmIDWhereWeNeedToChooseFreeSpells (sender, mom.getServerDB ());
			if (chooseSpellsMsg != null)
				sender.getConnection ().sendMessageToClient (chooseSpellsMsg);
			else
				sender.getConnection ().sendMessageToClient (new ChooseYourRaceNowMessage ());
		}

		log.trace ("Exiting process");
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
}
package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.clienttoserver.ChooseInitialSpellsMessage;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowMessage;
import momime.common.messages.servertoclient.ChooseYourRaceNowMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.server.MomSessionVariables;
import momime.server.utils.PlayerPickServerUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Tells server which spell choices we made, e.g. that 11 books grants 1 free rare spell.
 */
public final class ChooseInitialSpellsMessageImpl extends ChooseInitialSpellsMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ChooseInitialSpellsMessageImpl.class);
	
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
		final MomSessionVariables mom = (MomSessionVariables) thread;

		final String error = getPlayerPickServerUtils ().validateInitialSpellSelection (sender, getPickID (), getSpell (), mom.getServerDB ());
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
			// Grant free spells
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
			for (final SpellResearchStatus thisSpell : priv.getSpellResearchStatus ())
				if (getSpell ().contains (thisSpell.getSpellID ()))
					thisSpell.setStatus (SpellResearchStatusID.AVAILABLE);

			// Does player have to pick any further free spells or are they done
			log.debug ("process: " + sender.getPlayerDescription ().getPlayerName () + " made valid selections, checking to see if need to choose more free spells");

			final ChooseInitialSpellsNowMessage chooseSpellsMsg = getPlayerPickServerUtils ().findRealmIDWhereWeNeedToChooseFreeSpells (sender, mom.getServerDB ());
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
}
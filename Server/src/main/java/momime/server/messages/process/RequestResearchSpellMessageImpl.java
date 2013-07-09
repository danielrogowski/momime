package momime.server.messages.process;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SwitchResearch;
import momime.common.messages.clienttoserver.v0_9_4.RequestResearchSpellMessage;
import momime.common.messages.servertoclient.v0_9_4.SpellResearchChangedMessage;
import momime.common.messages.servertoclient.v0_9_4.TextPopupMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateRemainingResearchCostMessage;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.server.MomSessionVariables;
import momime.server.database.v0_9_4.Spell;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

/**
 * Client sends this when they want to pick which spell they want to research
 */
public final class RequestResearchSpellMessageImpl extends RequestResearchSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (RequestResearchSpellMessageImpl.class.getName ());
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.entering (RequestResearchSpellMessageImpl.class.getName (), "process",
			new String [] {sender.getPlayerDescription ().getPlayerID ().toString (), getSpellID ()});

		final MomSessionVariables mom = (MomSessionVariables) thread;
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();

		// Validate the requested picks
		final String error = mom.getSpellServerUtils ().validateResearch (sender, spellID, mom.getSessionDescription ().getSpellSetting ().getSwitchResearch (), mom.getServerDB ());
		if (error != null)
		{
			// Return error
			log.warning (ChooseCustomPicksMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			// Do they lose research towards current spell?
			if ((mom.getSessionDescription ().getSpellSetting ().getSwitchResearch () == SwitchResearch.LOSE_CURRENT_RESEARCH) && (priv.getSpellIDBeingResearched () != null))
			{
				// Lose on server
				final Spell spellPreviouslyBeingResearched = mom.getServerDB ().findSpell (priv.getSpellIDBeingResearched (), "RequestResearchSpellMessageImpl");
				final SpellResearchStatus spellPreviouslyBeingResearchedStatus = mom.getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched ());

				spellPreviouslyBeingResearchedStatus.setRemainingResearchCost (spellPreviouslyBeingResearched.getResearchCost ());

				// Lose on client
				final UpdateRemainingResearchCostMessage msg = new UpdateRemainingResearchCostMessage ();
				msg.setSpellID (priv.getSpellIDBeingResearched ());
				msg.setRemainingResearchCost (spellPreviouslyBeingResearched.getResearchCost ());
				sender.getConnection ().sendMessageToClient (msg);
			}

			// Change on server
			priv.setSpellIDBeingResearched (getSpellID ());

			// Change on client
			final SpellResearchChangedMessage msg = new SpellResearchChangedMessage ();
			msg.setSpellID (getSpellID ());
			sender.getConnection ().sendMessageToClient (msg);
		}

		log.exiting (RequestResearchSpellMessageImpl.class.getName (), "process");
	}
}

package momime.server.messages.process;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;

import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SwitchResearch;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.WizardState;
import momime.common.messages.clienttoserver.RequestResearchSpellMessage;
import momime.common.messages.servertoclient.SpellResearchChangedMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.messages.servertoclient.UpdateRemainingResearchCostMessage;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.utils.SpellServerUtils;

/**
 * Client sends this when they want to pick which spell they want to research
 */
public final class RequestResearchSpellMessageImpl extends RequestResearchSpellMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestResearchSpellMessageImpl.class);

	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Server-side only spell utils */
	private SpellServerUtils spellServerUtils;
	
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
		final MomSessionVariables mom = (MomSessionVariables) thread;
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) sender.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();

		// Validate the requested picks
		final String error;
		if (pub.getWizardState () != WizardState.ACTIVE)
			error = "You cannot research spells while you are banished.";
		else
			error = getSpellServerUtils ().validateResearch (sender, spellID, mom.getSessionDescription ().getSpellSetting ().getSwitchResearch (), mom.getServerDB ());
		
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
			// Do they lose research towards current spell?
			if ((mom.getSessionDescription ().getSpellSetting ().getSwitchResearch () == SwitchResearch.LOSE_CURRENT_RESEARCH) && (priv.getSpellIDBeingResearched () != null))
			{
				// Lose on server
				final Spell spellPreviouslyBeingResearched = mom.getServerDB ().findSpell (priv.getSpellIDBeingResearched (), "RequestResearchSpellMessageImpl");
				final SpellResearchStatus spellPreviouslyBeingResearchedStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched ());

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
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}
	
	/**
	 * @return Server-side only spell utils
	 */
	public final SpellServerUtils getSpellServerUtils ()
	{
		return spellServerUtils;
	}

	/**
	 * @param utils Server-side only spell utils
	 */
	public final void setSpellServerUtils (final SpellServerUtils utils)
	{
		spellServerUtils = utils;
	}
}
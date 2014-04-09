package momime.client.messages.process;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.common.messages.servertoclient.v0_9_5.FullSpellListMessage;
import momime.common.messages.v0_9_5.SpellResearchStatus;
import momime.common.messages.v0_9_5.SpellResearchStatusID;
import momime.common.utils.SpellUtils;

import com.ndg.multiplayer.client.MultiplayerServerConnection;
import com.ndg.multiplayer.client.SessionServerToClientMessage;

/**
 * Server sends this to client to tell them the status of every spell in the game
 */
public final class FullSpellListMessageImpl extends FullSpellListMessage implements SessionServerToClientMessage
{
	/** Class logger */
	private final Logger log = Logger.getLogger (FullSpellListMessageImpl.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/**
	 * @param sender Connection to the server
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void process (final MultiplayerServerConnection sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.entering (FullSpellListMessageImpl.class.getName (), "process", getSpellResearchStatus ().size ());

		// Check got the right number
		final List<SpellResearchStatus> ourSpells = getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus ();
		if (ourSpells.size () != getSpellResearchStatus ().size ())
			throw new IOException ("Server sent updated spell status for a different number of spells (" +
				getSpellResearchStatus ().size () + ") than expected (" + ourSpells.size () + ")");
		
		// Accept the new data
		ourSpells.clear ();
		ourSpells.addAll (getSpellResearchStatus ());
		
		// If finished researching current spell then we need to choose a new spell to start researching
		if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () != null)
		{
			final SpellResearchStatus spellBeingResearched = getSpellUtils ().findSpellResearchStatus
				(ourSpells, getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
			
			if (spellBeingResearched.getStatus () == SpellResearchStatusID.AVAILABLE)
			{
				getClient ().getOurPersistentPlayerPrivateKnowledge ().setSpellIDBeingResearched (null);
				
				// The spell we're currently researching was just set to Available, so need to regen the production icons on the next turn
				// button depending on whether we've got another spell left to research or not
				
				// However no need to do so explicitly, since the only ways that this message is ever sent is during game start or during GPV accumulation
				
				// In either of those situations, the server will be sending us updated GPVs shortly which will trigger the icons to be regen'd at that stage				
			}
		}
		
		log.exiting (FullSpellListMessageImpl.class.getName (), "process");
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
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
}

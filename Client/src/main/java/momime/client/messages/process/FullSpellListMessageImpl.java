package momime.client.messages.process;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.SpellBookUI;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.utils.SpellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to client to tell them the status of every spell in the game
 */
public final class FullSpellListMessageImpl extends FullSpellListMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (FullSpellListMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getSpellResearchStatus ().size ());

		// Check got the right number
		final List<SpellResearchStatus> ourSpells = getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus ();
		if (ourSpells.size () != getSpellResearchStatus ().size ())
			throw new IOException ("Server sent updated spell status for a different number of spells (" +
				getSpellResearchStatus ().size () + ") than expected (" + ourSpells.size () + ")");
		
		// Accept the new data
		ourSpells.clear ();
		ourSpells.addAll (getSpellResearchStatus ());
		
		// Update the UI
		getSpellBookUI ().updateSpellBook ();
		
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
		
		log.trace ("Exiting start");
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

	/**
	 * @return Spell book
	 */
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
	}
}
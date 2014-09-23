package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.SpellBookUI;
import momime.common.messages.servertoclient.v0_9_5.SpellResearchChangedMessage;

/**
 * Server sends this back to a client who requested a change in research to let them know the change was OK.
 * This isn't used to set research to 'nothing', so safe to assume that SpellID is non-blank.
 */
public final class SpellResearchChangedMessageImpl extends SpellResearchChangedMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SpellResearchChangedMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");

		// Store new value
		getClient ().getOurPersistentPlayerPrivateKnowledge ().setSpellIDBeingResearched (getSpellID ());
		
		// Colour spell differently in the spell book
		getSpellBookUI ().languageOrPageChanged ();
		
		// Update the label on the magic screen
		getMagicSlidersUI ().updateProductionLabels ();
		
		// There's probably a new turn message telling us that we had to choose a spell to research
		getNewTurnMessagesUI ().languageChanged ();
		
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

	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}

	/**
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
	}
}
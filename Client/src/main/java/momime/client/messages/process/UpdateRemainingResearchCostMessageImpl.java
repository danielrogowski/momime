package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.SpellBookUI;
import momime.common.messages.servertoclient.UpdateRemainingResearchCostMessage;
import momime.common.utils.SpellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Server sends this to client to update the number of research points they have left to spend before getting a particular spell.
 * This isn't used to set RemainingResearchCost = 0 when research is completed, because when we complete researching a spell, the server also has to
 * randomly pick the 8 further choices of what to research next.  So in that situation we just send the whole fullSpellListMessage again.
 */
public final class UpdateRemainingResearchCostMessageImpl extends UpdateRemainingResearchCostMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UpdateRemainingResearchCostMessageImpl.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
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
		getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (),
			getSpellID ()).setRemainingResearchCost (getRemainingResearchCost ());
		
		// Show the reduced value in the spell book research pages
		getSpellBookUI ().languageOrPageChanged ();
		
		// Update the progress bar on the magic screen
		getMagicSlidersUI ().updateProductionLabels ();
		
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
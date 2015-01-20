package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.scheduledcombatmessages.ScheduledCombatMessageProcessing;
import momime.client.ui.frames.ScheduledCombatsUI;
import momime.common.messages.servertoclient.ShowListAndOtherScheduledCombatsMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

/**
 * Notifies clients about how many combats they *aren't* involved in, and instructs
 * them that all scheduled combats have now been sent so they can show the list.
 */
public final class ShowListAndOtherScheduledCombatsMessageImpl extends ShowListAndOtherScheduledCombatsMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ShowListAndOtherScheduledCombatsMessageImpl.class);

	/** Scheduled combats list */
	private ScheduledCombatsUI scheduledCombatsUI;
	
	/** Scheduled combat message processing */
	private ScheduledCombatMessageProcessing scheduledCombatMessageProcessing;

	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start: " + getCombatCount ());
	
		getScheduledCombatMessageProcessing ().setScheduledCombatsNotInvolvedIn (getCombatCount ());
		getScheduledCombatsUI ().setCombatMessages (getScheduledCombatMessageProcessing ().sortAndAddCategories ());
		getScheduledCombatsUI ().setVisible (true);
		
		log.trace ("Exiting start");
	}
	
	/**
	 * @return Scheduled combats list
	 */
	public final ScheduledCombatsUI getScheduledCombatsUI ()
	{
		return scheduledCombatsUI;
	}

	/**
	 * @param ui Scheduled combats list
	 */
	public final void setScheduledCombatsUI (final ScheduledCombatsUI ui)
	{
		scheduledCombatsUI = ui;
	}

	/**
	 * @return Scheduled combat message processing
	 */
	public final ScheduledCombatMessageProcessing getScheduledCombatMessageProcessing ()
	{
		return scheduledCombatMessageProcessing;
	}

	/**
	 * @param proc Scheduled combat message processing
	 */
	public final void setScheduledCombatMessageProcessing (final ScheduledCombatMessageProcessing proc)
	{
		scheduledCombatMessageProcessing = proc;
	}
}
package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.HistoryUI;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.servertoclient.AddPowerBaseHistoryMessage;
import momime.common.messages.servertoclient.PowerBaseHistoryPlayer;
import momime.common.utils.KnownWizardUtils;

/**
 * Server broadcasts history of all wizards' power base each turn to show on the Historian screen
 */
public final class AddPowerBaseHistoryMessageImpl extends AddPowerBaseHistoryMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** UI for screen showing power base history for each wizard */
	private HistoryUI historyUI;

	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		for (final PowerBaseHistoryPlayer value : getPlayer ())
		{
			final KnownWizardDetails thisWizard = getKnownWizardUtils ().findKnownWizardDetails
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), value.getPlayerID (), "AddPowerBaseHistoryMessageImpl");
			
			for (int n = 0; n < value.getZeroCount (); n++)
				thisWizard.getPowerBaseHistory ().add (0);
			
			thisWizard.getPowerBaseHistory ().add (value.getPowerBase ());
		}
		
		getHistoryUI ().redrawChart ();
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
	 * @return UI for screen showing power base history for each wizard
	 */
	public final HistoryUI getHistoryUI ()
	{
		return historyUI;
	}

	/**
	 * @param h UI for screen showing power base history for each wizard
	 */
	public final void setHistoryUI (final HistoryUI h)
	{
		historyUI = h;
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
}
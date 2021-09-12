package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.ui.frames.WizardsUI;
import momime.common.messages.servertoclient.OverlandCastingInfo;
import momime.common.messages.servertoclient.OverlandCastingInfoMessage;

/**
 * Server telling us what spells all wizards are currently casting overland
 */
public final class OverlandCastingInfoMessageImpl extends OverlandCastingInfoMessage implements BaseServerToClientMessage
{
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getWizardsUI ().getOverlandCastingInfo ().clear ();
		
		for (final OverlandCastingInfo info : getOverlandCastingInfo ())
			getWizardsUI ().getOverlandCastingInfo ().put (info.getPlayerID (), info);
		
		getWizardsUI ().setVisible (true);
		getWizardsUI ().updateWizards (false);
	}

	/**
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
	}
}
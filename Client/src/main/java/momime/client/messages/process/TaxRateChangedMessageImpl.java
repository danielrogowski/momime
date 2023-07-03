package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.CityViewUI;
import momime.client.ui.frames.TaxRateUI;
import momime.common.messages.servertoclient.TaxRateChangedMessage;

/**
 * Server sends this back to clients who request a tax rate change to acknowledge that their request was OK
 */
public final class TaxRateChangedMessageImpl extends TaxRateChangedMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** Tax rate UI */
	private TaxRateUI taxRateUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getClient ().getOurPersistentPlayerPrivateKnowledge ().setTaxRateID (getTaxRateID ());

		for (final CityViewUI cityView : getClient ().getCityViews ().values ())
			cityView.cityDataChanged ();
		
		// Move the * showing the current tax rate
		getTaxRateUI ().languageChanged ();
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
	 * @return Tax rate UI
	 */
	public final TaxRateUI getTaxRateUI ()
	{
		return taxRateUI;
	}

	/**
	 * @param ui Tax rate UI
	 */
	public final void setTaxRateUI (final TaxRateUI ui)
	{
		taxRateUI = ui;
	}
}
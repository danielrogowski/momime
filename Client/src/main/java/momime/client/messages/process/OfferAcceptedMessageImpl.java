package momime.client.messages.process;

import java.io.IOException;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.newturnmessages.NewTurnMessageOfferEx;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OfferUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.servertoclient.OfferAcceptedMessage;

/**
 * Client requested to accept an offer to hire a hero, units or buy an item, and server processed it OK.
 * The actual new unit(s) or hero item will be sent separately.
 */
public final class OfferAcceptedMessageImpl extends OfferAcceptedMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;
	
	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		final NewTurnMessageOfferEx offer = getClient ().getOurTransientPlayerPrivateKnowledge ().getNewTurnMessage ().stream ().filter
			(ntm -> ntm instanceof NewTurnMessageOfferEx).map (ntm -> (NewTurnMessageOfferEx) ntm).filter
			(o -> o.getOfferURN () == getOfferURN ()).findAny ().orElse (null);
		
		if (offer != null)
		{
			offer.setOfferAccepted (true);
			getNewTurnMessagesUI ().languageChanged ();
			getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
			
			final OfferUI ui = getClient ().getOffers ().get (getOfferURN ());
			ui.close ();
		}
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

	/**
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}
}
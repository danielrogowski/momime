package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import momime.client.MomClient;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.servertoclient.UpdateTurnPhaseMessage;

/**
 * Server sends this to all clients to notify when the turn reaches a new phase
 */
public final class UpdateTurnPhaseMessageImpl extends UpdateTurnPhaseMessage implements BaseServerToClientMessage
{
	/** Multiplayer client */
	private MomClient client;

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
		getClient ().getGeneralPublicKnowledge ().setTurnPhase (getTurnPhase ());
		getOverlandMapRightHandPanel ().turnSystemOrCurrentPlayerChanged ();
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
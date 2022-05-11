package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.ui.dialogs.DiplomacyTextState;
import momime.client.ui.dialogs.DiplomacyUI;
import momime.common.messages.servertoclient.EndDiplomacyMessage;

/**
 * Another player (may be human or AI player) either refuses to talk to us up front, or we've been talking to them and now they lost patience and telling us to go away.
 */
public final class EndDiplomacyMessageImpl extends EndDiplomacyMessage implements BaseServerToClientMessage
{
	/** Diplomacy UI */
	private DiplomacyUI diplomacyUI;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getDiplomacyUI ().setTalkingWizardID (getTalkFromPlayerID ());
		getDiplomacyUI ().setTextState (DiplomacyTextState.REFUSED_TALK);
		getDiplomacyUI ().initializeText ();
	}
	
	/**
	 * @return Diplomacy UI
	 */
	public final DiplomacyUI getDiplomacyUI ()
	{
		return diplomacyUI;
	}

	/**
	 * @param ui Diplomacy UI
	 */
	public final void setDiplomacyUI (final DiplomacyUI ui)
	{
		diplomacyUI = ui;
	}
}
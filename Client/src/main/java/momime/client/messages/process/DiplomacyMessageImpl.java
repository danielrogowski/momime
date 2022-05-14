package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.ui.dialogs.DiplomacyPortraitState;
import momime.client.ui.dialogs.DiplomacyTextState;
import momime.client.ui.dialogs.DiplomacyUI;
import momime.common.messages.servertoclient.DiplomacyMessage;

/**
 * Notifying a player of a proposal, offer or demand to another wizard, the exact nature of which is set by the action value.
 */
public final class DiplomacyMessageImpl extends DiplomacyMessage implements BaseServerToClientMessage
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
		getDiplomacyUI ().setDiplomacyAction (getAction ());

		switch (getAction ())
		{
			case INITIATE_TALKING:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.APPEARING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.NONE);		// Text doesn't appear until the animation showing the wizard appearing completes
				getDiplomacyUI ().setVisibleRelationScoreID (getVisibleRelationScoreID ());
				getDiplomacyUI ().setVisible (true);
				break;
				
			case ACCEPT_TALKING:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.APPEARING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.NONE);		// Text doesn't appear until the animation showing the wizard appearing completes
				getDiplomacyUI ().setVisibleRelationScoreID (getVisibleRelationScoreID ());
				getDiplomacyUI ().updateRelationScore ();
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case REJECT_TALKING:
				getDiplomacyUI ().setTextState (DiplomacyTextState.REFUSED_TALK);
				getDiplomacyUI ().initializeText ();
				break;
				
			default:
				throw new IOException ("Client doesn't know how to handle diplomacy action " + getAction ());
		}
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
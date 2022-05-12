package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.ui.dialogs.DiplomacyPortraitState;
import momime.client.ui.dialogs.DiplomacyTextState;
import momime.client.ui.dialogs.DiplomacyUI;
import momime.common.messages.servertoclient.RequestAudienceMessage;

/**
 * Another player (may be human or AI player) wants to talk to us.  We have to pick whether we will talk to them or refuse.
 */
public final class RequestAudienceMessageImpl extends RequestAudienceMessage implements BaseServerToClientMessage
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
		getDiplomacyUI ().setRequestAudienceMessage (this);
		getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.APPEARING);
		getDiplomacyUI ().setTextState (DiplomacyTextState.NONE);		// Text doesn't appear until the animation showing the wizard appearing completes
		getDiplomacyUI ().setVisibleRelationScoreID (getVisibleRelationScoreID ());
		
		if (isInitiatingRequest ())
			getDiplomacyUI ().setVisible (true);
		else
		{
			getDiplomacyUI ().updateRelationScore ();
			getDiplomacyUI ().initializeText ();
			getDiplomacyUI ().initializePortrait ();
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
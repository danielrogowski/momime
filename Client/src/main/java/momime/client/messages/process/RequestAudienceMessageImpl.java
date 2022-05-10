package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.ui.dialogs.DiplomacyPortraitState;
import momime.client.ui.dialogs.DiplomacyTextState;
import momime.client.ui.dialogs.DiplomacyUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.common.messages.servertoclient.RequestAudienceMessage;

/**
 * Another player (may be human or AI player) wants to talk to us.  We have to pick whether we will talk to them or refuse.
 */
public final class RequestAudienceMessageImpl extends RequestAudienceMessage implements CustomDurationServerToClientMessage
{
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		final DiplomacyUI diplomacy = getPrototypeFrameCreator ().createDiplomacy ();
		diplomacy.setTalkingWizardID (getTalkFromPlayerID ());
		diplomacy.setRequestAudienceMessage (this);
		diplomacy.setPortraitState (DiplomacyPortraitState.APPEARING);
		diplomacy.setTextState (DiplomacyTextState.NONE);		// Text doesn't appear until the animation showing the wizard appearing completes
		diplomacy.setVisibleRelationScoreID (getVisibleRelationScoreID ());
		diplomacy.setVisible (true);
	}
	
	/**
	 * Nothing to do here when the message completes, because DiplomacyUI already closed itself
	 */
	@Override
	public final void finish ()
	{
	}
	
	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
}
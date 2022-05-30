package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.ui.frames.DiplomacyPortraitState;
import momime.client.ui.frames.DiplomacyTextState;
import momime.client.ui.frames.DiplomacyUI;
import momime.common.messages.servertoclient.DiplomacyMessage;

/**
 * Notifying a player of a proposal, offer or demand to another wizard, the exact nature of which is set by the action value.
 */
public final class DiplomacyMessageImpl extends DiplomacyMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AddBuildingMessageImpl.class);
	
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
		log.debug ("Received diplomacy action " + getAction () + " from player ID " + getTalkFromPlayerID ());
		
		getDiplomacyUI ().setTalkingWizardID (getTalkFromPlayerID ());
		getDiplomacyUI ().setDiplomacyAction (getAction ());
		getDiplomacyUI ().setMeetWizardMessage (null);

		switch (getAction ())
		{
			case INITIATE_TALKING:
				getDiplomacyUI ().initializeTalkingWizard ();
				getDiplomacyUI ().setProposingWizardID (getTalkFromPlayerID ());
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.APPEARING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.NONE);		// Text doesn't appear until the animation showing the wizard appearing completes
				getDiplomacyUI ().setVisibleRelationScoreID (getVisibleRelationScoreID ());
				getDiplomacyUI ().updateRelationScore ();
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				getDiplomacyUI ().setVisible (true);
				break;
				
			case ACCEPT_TALKING:
			case ACCEPT_TALKING_IMPATIENT:
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

			case REJECT_WIZARD_PACT:
			case REJECT_ALLIANCE:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.GENERIC_REFUSE);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case PROPOSE_WIZARD_PACT:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_WIZARD_PACT);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case PROPOSE_ALLIANCE:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_ALLIANCE);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case ACCEPT_WIZARD_PACT:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.ACCEPT_WIZARD_PACT);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case ACCEPT_ALLIANCE:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.ACCEPT_ALLIANCE);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
			
			case END_CONVERSATION:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.DISAPPEARING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.NONE);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;

			case GROWN_IMPATIENT:
				getDiplomacyUI ().setTextState (DiplomacyTextState.GROWN_IMPATIENT);
				getDiplomacyUI ().initializeText ();
				break;
				
			default:
				throw new IOException ("Client doesn't know how to handle diplomacy action " + getAction ());
		}
		
		log.debug ("Done with diplomacy action " + getAction () + " from player ID " + getTalkFromPlayerID ());
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
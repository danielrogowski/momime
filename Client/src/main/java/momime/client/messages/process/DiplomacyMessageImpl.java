package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.ui.frames.DiplomacyPortraitState;
import momime.client.ui.frames.DiplomacyTextState;
import momime.client.ui.frames.DiplomacyUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.servertoclient.DiplomacyMessage;
import momime.common.utils.KnownWizardUtils;

/**
 * Notifying a player of a proposal, offer or demand to another wizard, the exact nature of which is set by the action value.
 */
public final class DiplomacyMessageImpl extends DiplomacyMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (DiplomacyMessageImpl.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Diplomacy UI */
	private DiplomacyUI diplomacyUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
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
		log.debug ("Received diplomacy action " + getAction () + " from player ID " + getTalkFromPlayerID ());
		
		getDiplomacyUI ().setTalkingWizardID (getTalkFromPlayerID ());
		getDiplomacyUI ().setOtherWizardID (getOtherPlayerID ());
		getDiplomacyUI ().setDiplomacyAction (getAction ());
		getDiplomacyUI ().setOfferGoldAmount (getOfferGoldAmount ());
		getDiplomacyUI ().setRequestSpellID (getRequestSpellID ());
		getDiplomacyUI ().setOfferSpellID (getOfferSpellID ());
		getDiplomacyUI ().setCityName (getCityName ());
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
				getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
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
			case REJECT_PEACE_TREATY:
			case REJECT_DECLARE_WAR_ON_OTHER_WIZARD:
			case REJECT_BREAK_ALLIANCE_WITH_OTHER_WIZARD:
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

			case PROPOSE_PEACE_TREATY:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_PEACE_TREATY);
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
			
			case ACCEPT_PEACE_TREATY:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.ACCEPT_PEACE_TREATY);
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
				
			case GIVE_GOLD:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.GIVEN_GOLD);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case GIVE_SPELL:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.GIVEN_SPELL);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case ACCEPT_GOLD:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.THANKS_FOR_GOLD);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				
				// Further gold offers will be more expensive
				final DiplomacyWizardDetails talkToWizard = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getTalkFromPlayerID (), "DiplomacyMessageImpl");
				talkToWizard.setMaximumGoldTribute (talkToWizard.getMaximumGoldTribute () + getOfferGoldAmount ());
				break;
				
			case ACCEPT_SPELL:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.THANKS_FOR_SPELL);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case PROPOSE_EXCHANGE_SPELL:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_EXCHANGE_SPELL);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case NO_SPELLS_TO_EXCHANGE:
			case REFUSE_EXCHANGE_SPELL:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.REFUSE_EXCHANGE_SPELL);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case REJECT_EXCHANGE_SPELL:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.REJECT_EXCHANGE_SPELL);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;

			case ACCEPT_EXCHANGE_SPELL:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.THANKS_FOR_EXCHANGING_SPELL);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;

			case BROKEN_WIZARD_PACT_CITY:
			case BROKEN_ALLIANCE_CITY:
			case BROKEN_ALLIANCE_UNITS:
			case DECLARE_WAR_CITY:
			case DECLARE_WAR_ON_YOU_BECAUSE_OF_OTHER_WIZARD:
			case BREAK_ALLIANCE_WITH_YOU_BECAUSE_OF_OTHER_WIZARD:
				getDiplomacyUI ().initializeTalkingWizard ();
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.APPEARING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.NONE);		// Text doesn't appear until the animation showing the wizard appearing completes
				getDiplomacyUI ().setVisibleRelationScoreID (getVisibleRelationScoreID ());
				getDiplomacyUI ().updateRelationScore ();
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				getDiplomacyUI ().setVisible (true);
				break;
				
			case BREAK_WIZARD_PACT_NICELY:
			case BREAK_ALLIANCE_NICELY:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.BREAK_WIZARD_PACT_OR_ALLIANCE);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case BROKEN_WIZARD_PACT_NICELY:
			case BROKEN_ALLIANCE_NICELY:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.BROKEN_WIZARD_PACT_OR_ALLIANCE);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case PROPOSE_DECLARE_WAR_ON_OTHER_WIZARD:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_DECLARE_WAR_ON_OTHER_WIZARD);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case PROPOSE_BREAK_ALLIANCE_WITH_OTHER_WIZARD:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_BREAK_ALLIANCE_WITH_OTHER_WIZARD);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;

			case CANNOT_DECLARE_WAR_ON_UNKNOWN_WIZARD:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.CANNOT_DECLARE_WAR_ON_UNKNOWN_WIZARD);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case ACCEPT_DECLARE_WAR_ON_OTHER_WIZARD:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.ACCEPT_DECLARE_WAR_ON_OTHER_WIZARD);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			case ACCEPT_BREAK_ALLIANCE_WITH_OTHER_WIZARD:
				getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
				getDiplomacyUI ().setTextState (DiplomacyTextState.ACCEPT_BREAK_ALLIANCE_WITH_OTHER_WIZARD);
				getDiplomacyUI ().initializeText ();
				getDiplomacyUI ().initializePortrait ();
				break;
				
			default:
				throw new IOException ("Client doesn't know how to handle diplomacy action " + getAction ());
		}
		
		log.debug ("Done with diplomacy action " + getAction () + " from player ID " + getTalkFromPlayerID ());
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
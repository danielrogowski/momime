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
import momime.common.messages.DiplomacyAction;
import momime.common.messages.servertoclient.TradeableSpellsMessage;

/**
 * During diplomacy, we can try to give a spell to another player, or try to trade spells with another player.
 * This message tells the client which spells they know and which the other wizard can learn.
 */
public final class TradeableSpellsMessageImpl extends TradeableSpellsMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (TradeableSpellsMessageImpl.class);

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
		log.debug ("Received tradeable spell list for diplomacy action " + getAction () + " from player ID " + getTalkFromPlayerID () + ": " + getTradeableSpellID ().size () + " spells");
		
		getDiplomacyUI ().setTalkingWizardID (getTalkFromPlayerID ());
		getDiplomacyUI ().setDiplomacyAction (getAction ());
		getDiplomacyUI ().setOfferGoldAmount (null);
		getDiplomacyUI ().setMeetWizardMessage (null);
		getDiplomacyUI ().setRequestSpellID (getRequestSpellID ());
		getDiplomacyUI ().setOfferSpellID (null);
		getDiplomacyUI ().getTradeableSpellIDs ().clear ();
		getDiplomacyUI ().getTradeableSpellIDs ().addAll (getTradeableSpellID ());
		
		if (getAction () == DiplomacyAction.GIVE_SPELL)
			getDiplomacyUI ().setTextState (DiplomacyTextState.GIVE_SPELL);
		else if (getRequestSpellID () == null)
			getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_EXCHANGE_SPELL_THEIRS);
		else
			getDiplomacyUI ().setTextState (DiplomacyTextState.PROPOSE_EXCHANGE_SPELL_OURS);
		
		getDiplomacyUI ().setPortraitState (DiplomacyPortraitState.TALKING);
		getDiplomacyUI ().initializeText ();
		getDiplomacyUI ().initializePortrait ();
		
		log.debug ("Done with tradeable spell list for diplomacy action " + getAction () + " from player ID " + getTalkFromPlayerID ());
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
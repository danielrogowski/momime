package momime.server.messages.process;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.DiplomacyAction;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.PactType;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.clienttoserver.RequestDiplomacyMessage;
import momime.common.messages.servertoclient.DiplomacyMessage;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.TradeableSpellsMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.utils.KnownWizardServerUtils;

/**
 * We make a proposal, offer or demand to another wizard, the exact nature of which is set by the action value.
 */
public final class RequestDiplomacyMessageImpl extends RequestDiplomacyMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestDiplomacyMessageImpl.class);
	
	/** Amount of space in the UI to list tradeable spells */
	private final static int MAXIMUM_TRADEABLE_SPELLS = 4;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;

	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, IOException
	{
		log.debug ("Received diplomacy action " + getAction () + " from player ID " + sender.getPlayerDescription ().getPlayerID () + " sending to player ID " + getTalkToPlayerID ());
		
		final MomSessionVariables mom = (MomSessionVariables) thread;

		// Have to deal here with the fact that maybe we know the wizard we want to talk to, but they don't know us.  Perhaps we
		// only know them because they cast an overland enchantment or banished someone.  In this case we don't show the
		// animation for meeting, as this is superceeded by the animation for us wanting to talk to them.
		// Don't need to test whether we need to do this.  If wizard already knows us then the routine will simply do nothing and exit.
		getKnownWizardServerUtils ().meetWizard (sender.getPlayerDescription ().getPlayerID (), getTalkToPlayerID (), false, mom);
		
		final PlayerServerDetails talkToPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), getTalkToPlayerID (), "RequestDiplomacyMessageImpl");

		final MomPersistentPlayerPrivateKnowledge senderPriv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge talkToPlayerPriv = (MomPersistentPlayerPrivateKnowledge) talkToPlayer.getPersistentPlayerPrivateKnowledge ();

		final DiplomacyWizardDetails talkToWizard = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(senderPriv.getFogOfWarMemory ().getWizardDetails (), getTalkToPlayerID (), "RequestDiplomacyMessageImpl");
		
		// Convert gold tier to the actual amount, because the sender knows their relation to the other wizard, but the receiver won't
		final Integer offerGoldAmount = (getOfferGoldTier () == null) ? null :
			getKnownWizardUtils ().convertGoldOfferTierToAmount (talkToWizard.getMaximumGoldTribute (), getOfferGoldTier ());
		
		// Any updates needed on the server because of the action?
		boolean proceed = true;
		switch (getAction ())
		{
			case ACCEPT_WIZARD_PACT:
				getKnownWizardServerUtils ().updatePact (sender.getPlayerDescription ().getPlayerID (), getTalkToPlayerID (), PactType.WIZARD_PACT, mom);
				getKnownWizardServerUtils ().updatePact (getTalkToPlayerID (), sender.getPlayerDescription ().getPlayerID (), PactType.WIZARD_PACT, mom);
				break;
				
			case ACCEPT_ALLIANCE:
				getKnownWizardServerUtils ().updatePact (sender.getPlayerDescription ().getPlayerID (), getTalkToPlayerID (), PactType.ALLIANCE, mom);
				getKnownWizardServerUtils ().updatePact (getTalkToPlayerID (), sender.getPlayerDescription ().getPlayerID (), PactType.ALLIANCE, mom);
				break;
				
			case ACCEPT_PEACE_TREATY:
				getKnownWizardServerUtils ().updatePact (sender.getPlayerDescription ().getPlayerID (), getTalkToPlayerID (), null, mom);
				getKnownWizardServerUtils ().updatePact (getTalkToPlayerID (), sender.getPlayerDescription ().getPlayerID (), null, mom);
				break;
				
			// Tributes send an automated reply without even waiting for the recipient to click anything
			case GIVE_GOLD:
			{
				final DiplomacyMessage msg = new DiplomacyMessage ();	
				msg.setTalkFromPlayerID (getTalkToPlayerID ());
				msg.setAction (DiplomacyAction.ACCEPT_GOLD);
				msg.setOtherPlayerID (getOtherPlayerID ());
				msg.setOfferGoldAmount (offerGoldAmount);			
				
				sender.getConnection ().sendMessageToClient (msg);
				
				// Give gold				
				getResourceValueUtils ().addToAmountStored (senderPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -offerGoldAmount);
				getServerResourceCalculations ().sendGlobalProductionValues (sender, null, false);
				
				getResourceValueUtils ().addToAmountStored (talkToPlayerPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, offerGoldAmount);
				getServerResourceCalculations ().sendGlobalProductionValues (talkToPlayer, null, false);
				
				// Further gold offers will be more expensive (there's no message for this - client triggers same update from the ACCEPT_GOLD msg sent above)
				talkToWizard.setMaximumGoldTribute (talkToWizard.getMaximumGoldTribute () + offerGoldAmount);
				break;
			}
			
			// Breaking a wizard pact / alliance send an automated reply without even waiting for the recipient to click anything
			case BREAK_WIZARD_PACT_NICELY:
			case BREAK_ALLIANCE_NICELY:
			{
				final DiplomacyMessage msg = new DiplomacyMessage ();	
				msg.setTalkFromPlayerID (getTalkToPlayerID ());
				msg.setAction ((getAction () == DiplomacyAction.BREAK_WIZARD_PACT_NICELY) ? DiplomacyAction.BROKEN_WIZARD_PACT_NICELY : DiplomacyAction.BROKEN_ALLIANCE_NICELY);
				msg.setOtherPlayerID (getOtherPlayerID ());
				msg.setOfferGoldAmount (offerGoldAmount);			
				
				sender.getConnection ().sendMessageToClient (msg);
				
				// Remove pact
				getKnownWizardServerUtils ().updatePact (sender.getPlayerDescription ().getPlayerID (), getTalkToPlayerID (), null, mom);
				getKnownWizardServerUtils ().updatePact (getTalkToPlayerID (), sender.getPlayerDescription ().getPlayerID (), null, mom);
				break;
			}
			
			// Generate list of tradeable spells
			case GIVE_SPELL:
			case GIVE_SPELL_BECAUSE_THREATENED:
			case PROPOSE_EXCHANGE_SPELL:
				if (getOfferSpellID () == null)
				{
					proceed = false;
					
					// Do we need the list of spells they can trade to us, or we can trade to them, or both (so we can verify there's even a possible trade to do)?
					final List<String> spellIDsWeCanOffer = getServerSpellCalculations ().findCheapestSpells (getServerSpellCalculations ().listTradeableSpells
						(senderPriv.getSpellResearchStatus (), talkToPlayerPriv.getSpellResearchStatus ()), MAXIMUM_TRADEABLE_SPELLS, mom.getServerDB ());

					final List<String> spellIDsWeCanRequest = ((getAction () == DiplomacyAction.PROPOSE_EXCHANGE_SPELL) && (getRequestSpellID () == null)) ?
						getServerSpellCalculations ().findCheapestSpells (getServerSpellCalculations ().listTradeableSpells
							(talkToPlayerPriv.getSpellResearchStatus (), senderPriv.getSpellResearchStatus ()), MAXIMUM_TRADEABLE_SPELLS, mom.getServerDB ()) : null;
					
					if (((spellIDsWeCanRequest != null) && ((spellIDsWeCanRequest.isEmpty ()) || (spellIDsWeCanOffer.isEmpty ()))) ||
						((spellIDsWeCanRequest == null) && (spellIDsWeCanOffer.isEmpty ())))
					{
						// No valid trade, so don't even send the request to the other player
						final DiplomacyMessage msg = new DiplomacyMessage ();	
						msg.setTalkFromPlayerID (getTalkToPlayerID ());
						msg.setAction (DiplomacyAction.NO_SPELLS_TO_EXCHANGE);		// There isn't a separate "no spells to give" for tributes
						msg.setOtherPlayerID (getOtherPlayerID ());
						
						sender.getConnection ().sendMessageToClient (msg);
					}
					else
					{
						final TradeableSpellsMessage msg = new TradeableSpellsMessage ();
						msg.setTalkFromPlayerID (getTalkToPlayerID ());
						msg.setAction (getAction ());
						msg.setRequestSpellID (getRequestSpellID ());
						msg.getTradeableSpellID ().addAll ((spellIDsWeCanRequest != null) ? spellIDsWeCanRequest : spellIDsWeCanOffer);
						
						if ((getAction () == DiplomacyAction.PROPOSE_EXCHANGE_SPELL) && (getRequestSpellID () != null))
						{
							// Other player now needs to respond with which spell they'd like in return
							msg.setTalkFromPlayerID (sender.getPlayerDescription ().getPlayerID ());
							talkToPlayer.getConnection ().sendMessageToClient (msg);
						}
						else
							sender.getConnection ().sendMessageToClient (msg);
					}
				}
				else if (getAction () == DiplomacyAction.ACCEPT_SPELL)
				{
					final DiplomacyMessage msg = new DiplomacyMessage ();	
					msg.setTalkFromPlayerID (getTalkToPlayerID ());
					msg.setAction ((getAction () == DiplomacyAction.PROPOSE_EXCHANGE_SPELL) ? DiplomacyAction.PROPOSE_EXCHANGE_SPELL : DiplomacyAction.ACCEPT_SPELL);
					msg.setOtherPlayerID (getOtherPlayerID ());
					msg.setOfferSpellID (getOfferSpellID ());
					msg.setRequestSpellID (getRequestSpellID ());
					
					sender.getConnection ().sendMessageToClient (msg);
					
					// Learn the spell
					final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (talkToPlayerPriv.getSpellResearchStatus (), getOfferSpellID ());
					researchStatus.setStatus (SpellResearchStatusID.AVAILABLE);
					
					// Just in case the donated spell was one of the 8 spells available to research now
					getServerSpellCalculations ().randomizeSpellsResearchableNow (talkToPlayerPriv.getSpellResearchStatus (), mom.getServerDB ());
					
					final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
					spellsMsg.getSpellResearchStatus ().addAll (talkToPlayerPriv.getSpellResearchStatus ());
					talkToPlayer.getConnection ().sendMessageToClient (spellsMsg);
				}
				break;
				
			// Exchange spells
			case ACCEPT_EXCHANGE_SPELL:
			{
				// Learn the spells
				final SpellResearchStatus researchStatus1 = getSpellUtils ().findSpellResearchStatus (talkToPlayerPriv.getSpellResearchStatus (), getOfferSpellID ());
				final SpellResearchStatus researchStatus2 = getSpellUtils ().findSpellResearchStatus (senderPriv.getSpellResearchStatus (), getRequestSpellID ());
				researchStatus1.setStatus (SpellResearchStatusID.AVAILABLE);
				researchStatus2.setStatus (SpellResearchStatusID.AVAILABLE);
				
				// Just in case the donated spell was one of the 8 spells available to research now
				getServerSpellCalculations ().randomizeSpellsResearchableNow (talkToPlayerPriv.getSpellResearchStatus (), mom.getServerDB ());
				getServerSpellCalculations ().randomizeSpellsResearchableNow (senderPriv.getSpellResearchStatus (), mom.getServerDB ());
				
				final FullSpellListMessage spellsMsg1 = new FullSpellListMessage ();
				spellsMsg1.getSpellResearchStatus ().addAll (talkToPlayerPriv.getSpellResearchStatus ());
				talkToPlayer.getConnection ().sendMessageToClient (spellsMsg1);

				final FullSpellListMessage spellsMsg2 = new FullSpellListMessage ();
				spellsMsg2.getSpellResearchStatus ().addAll (senderPriv.getSpellResearchStatus ());
				sender.getConnection ().sendMessageToClient (spellsMsg2);

				break;
			}
				
			default:
				// This is fine, most diplomacy actions don't trigger updates 
		}
		
		// Forward the action onto human players
		if (proceed)
		{
			if (talkToPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
			{
				final DiplomacyMessage msg = new DiplomacyMessage ();	
				msg.setTalkFromPlayerID (sender.getPlayerDescription ().getPlayerID ());
				msg.setAction (getAction ());
				msg.setVisibleRelationScoreID (getVisibleRelationScoreID ());
				msg.setOtherPlayerID (getOtherPlayerID ());
				msg.setOfferSpellID (getOfferSpellID ());
				msg.setRequestSpellID (getRequestSpellID ());
				msg.setOfferGoldAmount (offerGoldAmount);			
				
				talkToPlayer.getConnection ().sendMessageToClient (msg);
			}
			else
				switch (getAction ())
				{
					default:
						throw new IOException ("AI does not know how to respond to Diplomacy action " + getAction ());
				}
		}
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}
	
	/**
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
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

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Server-only spell calculations
	 */
	public final ServerSpellCalculations getServerSpellCalculations ()
	{
		return serverSpellCalculations;
	}

	/**
	 * @param calc Server-only spell calculations
	 */
	public final void setServerSpellCalculations (final ServerSpellCalculations calc)
	{
		serverSpellCalculations = calc;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}
}
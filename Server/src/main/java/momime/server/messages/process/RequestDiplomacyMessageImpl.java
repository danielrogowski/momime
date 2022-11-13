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
import momime.common.database.RelationScore;
import momime.common.messages.DiplomacyAction;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.clienttoserver.RequestDiplomacyMessage;
import momime.common.messages.servertoclient.DiplomacyMessage;
import momime.common.messages.servertoclient.TradeableSpellsMessage;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.DiplomacyAI;
import momime.server.ai.RelationAI;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.process.DiplomacyProcessing;
import momime.server.utils.KnownWizardServerUtils;

/**
 * We make a proposal, offer or demand to another wizard, the exact nature of which is set by the action value.
 */
public final class RequestDiplomacyMessageImpl extends RequestDiplomacyMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RequestDiplomacyMessageImpl.class);
	
	/** Amount of space in the UI to list tradeable spells */
	public final static int MAXIMUM_TRADEABLE_SPELLS = 4;

	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;

	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;
	
	/** Methods for AI making decisions about diplomacy with other wizards */
	private DiplomacyAI diplomacyAI;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/** Methods for processing agreed diplomatic actions */
	private DiplomacyProcessing diplomacyProcessing; 
	
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
		final PlayerServerDetails otherPlayer = (getOtherPlayerID () == null) ? null : getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), getOtherPlayerID (), "RequestDiplomacyMessageImpl");

		// In context of player-to-player diplomacy, sender is the player agreeing to / rejecting the request which usually means talkToPlayer is the one who initiated it in the first place
		// In context of player-to-AI diplomacy, sender is the human player making the request and talkToPlayer is the AI player who is responding  
		final MomPersistentPlayerPrivateKnowledge senderPriv = (MomPersistentPlayerPrivateKnowledge) sender.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge talkToPlayerPriv = (MomPersistentPlayerPrivateKnowledge) talkToPlayer.getPersistentPlayerPrivateKnowledge ();

		// Wizards' opinions of each other
		final DiplomacyWizardDetails talkToWizard = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(senderPriv.getFogOfWarMemory ().getWizardDetails (), getTalkToPlayerID (), "RequestDiplomacyMessageImpl (T)");
		final DiplomacyWizardDetails senderWizard = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
			(talkToPlayerPriv.getFogOfWarMemory ().getWizardDetails (), sender.getPlayerDescription ().getPlayerID (), "RequestDiplomacyMessageImpl (S)");

		// Keep track of the last time we tried to talk to this wizard; have to do this as a special block as AI players might reject talking just below here
		// and we want to update this even if they completely refused to talk to us
		if (getAction () == DiplomacyAction.INITIATE_TALKING)
			senderWizard.setLastTurnTalkedTo (mom.getGeneralPublicKnowledge ().getTurnNumber ());
		
		// See if we're ran out patience to hear their requests - only applicable to AI players - human players can respond however they wish
		boolean proceed = true;
		if (talkToPlayer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
			switch (getAction ())
			{
				// This is a bit of a special case because
				// 1) It has a different response (reject talking) rather than the standard (grown impatient)
				// 2) A successful pass of the patience check doesn't increase impatience by +1
				case INITIATE_TALKING:
					getDiplomacyAI ().decideWhetherWillTalkTo (sender, talkToPlayer, mom);
					proceed = false;
					break;
				
				// Requests that need to check patience level
				case BREAK_WIZARD_PACT_NICELY:
				case BREAK_ALLIANCE_NICELY:
				case PROPOSE_WIZARD_PACT:
				case PROPOSE_ALLIANCE:
				case PROPOSE_PEACE_TREATY:
				case PROPOSE_DECLARE_WAR_ON_OTHER_WIZARD:
				case PROPOSE_BREAK_ALLIANCE_WITH_OTHER_WIZARD:
				case PROPOSE_EXCHANGE_SPELL:
				case THREATEN:
				{
					// PROPOSE_EXCHANGE_SPELL goes through a few stages; only do this if we're at the very initial stage
					if (((getAction () == DiplomacyAction.PROPOSE_EXCHANGE_SPELL) && ((getRequestSpellID () == null) && (getOfferSpellID () == null))) ||
						(getAction () != DiplomacyAction.PROPOSE_EXCHANGE_SPELL))
					{
						// If true, will fall into the rest of the code below; if false then method sends GROWN_IMPATIENT back to the client
						if (!getDiplomacyAI ().willListenToRequest (sender, talkToPlayer, getAction (), mom))
							proceed = false;
					}
				}
				break;
	
				// Not all actions increase impatience (requests tend to, final actions don't)
				default:
			}
		
		// Any updates needed on the server because of the action?
		if (proceed)
			switch (getAction ())
			{
				case ACCEPT_WIZARD_PACT:
					getDiplomacyProcessing ().agreeWizardPact (talkToPlayer, sender, mom);
					proceed = false;
					break;
					
				case ACCEPT_ALLIANCE:
					getDiplomacyProcessing ().agreeAlliance (talkToPlayer, sender, mom);
					proceed = false;
					break;
					
				case ACCEPT_PEACE_TREATY:
					getDiplomacyProcessing ().agreePeaceTreaty (talkToPlayer, sender, mom);
					proceed = false;
					break;

				case REJECT_WIZARD_PACT:
					getDiplomacyProcessing ().rejectWizardPact (talkToPlayer, sender, mom);
					proceed = false;
					break;
					
				case REJECT_ALLIANCE:
					getDiplomacyProcessing ().rejectAlliance (talkToPlayer, sender, mom);
					proceed = false;
					break;
					
				case REJECT_PEACE_TREATY:
					getDiplomacyProcessing ().rejectPeaceTreaty (talkToPlayer, sender, mom);
					proceed = false;
					break;
					
				case DECLARE_WAR_BECAUSE_THREATENED:
					getDiplomacyProcessing ().declareWarBecauseThreatened (sender, talkToPlayer, mom);
					proceed = false;
					break;
					
				// Player 1 asking player 2 to declare war on player 3 has no idea whether player 2 even knows who player 3 is.
				// So if they don't know each other, send back a special reply.
				case PROPOSE_DECLARE_WAR_ON_OTHER_WIZARD:
				{
					if (talkToPlayer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
						getRelationAI ().penaltyToVisibleRelation (senderWizard, 10);
					
					if (getKnownWizardUtils ().findKnownWizardDetails (talkToPlayerPriv.getFogOfWarMemory ().getWizardDetails (), getOtherPlayerID ()) == null)
					{
						final DiplomacyMessage msg = new DiplomacyMessage ();	
						msg.setTalkFromPlayerID (getTalkToPlayerID ());
						msg.setAction (DiplomacyAction.CANNOT_DECLARE_WAR_ON_UNKNOWN_WIZARD);
						msg.setOtherPlayerID (getOtherPlayerID ());
						
						if (talkToPlayer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
						{
							final RelationScore relationScore = mom.getServerDB ().findRelationScoreForValue (senderWizard.getVisibleRelation (), "RequestDiplomacyMessageImpl");
							msg.setVisibleRelationScoreID (relationScore.getRelationScoreID ());
						}
						
						sender.getConnection ().sendMessageToClient (msg);
						proceed = false;
					}
					
					break;
				}
					
				case ACCEPT_DECLARE_WAR_ON_OTHER_WIZARD:
					getDiplomacyProcessing ().agreeDeclareWarOnOtherWizard (talkToPlayer, sender, otherPlayer, mom);
					proceed = false;
					break;
				
				// Just putting this here so its consistent with PROPOSE_DECLARE_WAR_ON_OTHER_WIZARD
				case PROPOSE_BREAK_ALLIANCE_WITH_OTHER_WIZARD:
					if (talkToPlayer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN)
						getRelationAI ().penaltyToVisibleRelation (senderWizard, 10);
					break;
	
				case ACCEPT_BREAK_ALLIANCE_WITH_OTHER_WIZARD:
					getDiplomacyProcessing ().agreeBreakAllianceWithOtherWizard (talkToPlayer, sender, otherPlayer, mom);
					proceed = false;
					break;
				
				// Tributes send an automated reply without even waiting for the recipient to click anything
				case GIVE_GOLD:
					getDiplomacyProcessing ().giveGold (sender, talkToPlayer, mom, getOfferGoldTier ());
					proceed = false;
					break;

				case GIVE_GOLD_BECAUSE_THREATENED:
					getDiplomacyProcessing ().giveGoldBecauseThreatened (sender, talkToPlayer, mom, getOfferGoldTier ());
					proceed = false;
					break;
				
				case BREAK_WIZARD_PACT_NICELY:
					getDiplomacyProcessing ().breakWizardPactNicely (sender, talkToPlayer, mom);
					proceed = false;
					break;
					
				case BREAK_ALLIANCE_NICELY:
					getDiplomacyProcessing ().breakAllianceNicely (sender, talkToPlayer, mom);
					proceed = false;
					break;
				
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
							msg.setAction ((getAction () == DiplomacyAction.GIVE_SPELL_BECAUSE_THREATENED) ? DiplomacyAction.NO_SPELLS_TO_EXCHANGE_BECAUSE_THREATENED : DiplomacyAction.NO_SPELLS_TO_EXCHANGE);
							msg.setOtherPlayerID (getOtherPlayerID ());
							
							sender.getConnection ().sendMessageToClient (msg);
						}
						else if ((getAction () == DiplomacyAction.PROPOSE_EXCHANGE_SPELL) && (getRequestSpellID () != null) && (talkToPlayer.getPlayerDescription ().getPlayerType () != PlayerType.HUMAN))
						{
							// Player asked AI player for a spell that they know, now AI player needs to choose which spell they'd like in return
							final String requestSpellIDInReturn = getDiplomacyAI ().chooseSpellToRequestInReturn (getRequestSpellID (), spellIDsWeCanOffer, mom.getServerDB ());
							final RelationScore relationScore = mom.getServerDB ().findRelationScoreForValue (senderWizard.getVisibleRelation (), "RequestDiplomacyMessageImpl");
							
							final DiplomacyMessage msg = new DiplomacyMessage ();
							msg.setTalkFromPlayerID (getTalkToPlayerID ());
							msg.setAction ((requestSpellIDInReturn == null) ? DiplomacyAction.REFUSE_EXCHANGE_SPELL : DiplomacyAction.PROPOSE_EXCHANGE_SPELL);
							msg.setVisibleRelationScoreID (relationScore.getRelationScoreID ());
							msg.setRequestSpellID (getRequestSpellID ());
							msg.setOfferSpellID (requestSpellIDInReturn);
							
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
					
					// Proceed with spell donation
					else if (getAction () != DiplomacyAction.PROPOSE_EXCHANGE_SPELL)
					{
						proceed = false;
						if (getAction () == DiplomacyAction.GIVE_SPELL)
							getDiplomacyProcessing ().giveSpell (sender, talkToPlayer, mom, getOfferSpellID ());
						else
							getDiplomacyProcessing ().giveSpellBecauseThreatened (sender, talkToPlayer, mom, getOfferSpellID ());
					}
					break;
					
				// Exchange spells
				case ACCEPT_EXCHANGE_SPELL:
					getDiplomacyProcessing ().tradeSpells (sender, talkToPlayer, mom, getRequestSpellID (), getOfferSpellID ());
					proceed = false;
					break;
					
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
				
				// Convert gold tier to the actual amount, because the sender knows their relation to the other wizard, but the receiver won't
				if (getOfferGoldTier () != null)
					msg.setOfferGoldAmount (getKnownWizardUtils ().convertGoldOfferTierToAmount (talkToWizard.getMaximumGoldTribute (), getOfferGoldTier ()));
				
				talkToPlayer.getConnection ().sendMessageToClient (msg);
			}
			else
				switch (getAction ())
				{
					case PROPOSE_WIZARD_PACT:
						getDiplomacyAI ().considerWizardPact (sender, talkToPlayer, mom);
						break;

					case PROPOSE_ALLIANCE:
						getDiplomacyAI ().considerAlliance (sender, talkToPlayer, mom);
						break;
					
					case PROPOSE_PEACE_TREATY:
						getDiplomacyAI ().considerPeaceTreaty (sender, talkToPlayer, mom);
						break;
					
					case PROPOSE_DECLARE_WAR_ON_OTHER_WIZARD:
						getDiplomacyAI ().considerDeclareWarOnOtherWizard (sender, talkToPlayer, otherPlayer, mom);
						break;
					
					case PROPOSE_BREAK_ALLIANCE_WITH_OTHER_WIZARD:
						getDiplomacyAI ().considerBreakAllianceWithOtherWizard (sender, talkToPlayer, otherPlayer, mom);
						break;
					
					case THREATEN:
						getDiplomacyAI ().respondToThreat (sender, talkToPlayer, mom);
						break;
					
					// Ignore, AI doesn't need to respond to these
					case END_CONVERSATION:
					case GIVE_GOLD:
					case GIVE_SPELL:
					case ACCEPT_EXCHANGE_SPELL:
						
					// Already handled above
					case BREAK_WIZARD_PACT_NICELY:
					case BREAK_ALLIANCE_NICELY:
					case INITIATE_TALKING:
						break;
					
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
	 * @return Methods for AI making decisions about diplomacy with other wizards
	 */
	public final DiplomacyAI getDiplomacyAI ()
	{
		return diplomacyAI;
	}

	/**
	 * @param ai Methods for AI making decisions about diplomacy with other wizards
	 */
	public final void setDiplomacyAI (final DiplomacyAI ai)
	{
		diplomacyAI = ai;
	}

	/**
	 * @return For calculating relation scores between two wizards
	 */
	public final RelationAI getRelationAI ()
	{
		return relationAI;
	}

	/**
	 * @param ai For calculating relation scores between two wizards
	 */
	public final void setRelationAI (final RelationAI ai)
	{
		relationAI = ai;
	}

	/**
	 * @return Methods for processing agreed diplomatic actions
	 */
	public final DiplomacyProcessing getDiplomacyProcessing ()
	{
		return diplomacyProcessing;
	}
	
	/**
	 * @param p Methods for processing agreed diplomatic actions
	 */
	public final void setDiplomacyProcessing (final DiplomacyProcessing p)
	{
		diplomacyProcessing = p;
	}
}
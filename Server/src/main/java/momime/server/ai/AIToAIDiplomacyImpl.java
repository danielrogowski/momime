package momime.server.ai;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.messages.process.RequestDiplomacyMessageImpl;
import momime.server.process.DiplomacyProcessing;

/**
 * Methods for processing diplomacy requests from AI player to another AI player
 */
public final class AIToAIDiplomacyImpl implements AIToAIDiplomacy
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (AIToAIDiplomacyImpl.class);
	
	/** Methods for AI making decisions about diplomacy with other wizards */
	private DiplomacyAI diplomacyAI;

	/** Methods for processing agreed diplomatic actions */
	private DiplomacyProcessing diplomacyProcessing; 
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;
	
	/**
	 * @param player AI player whose turn it is, who is making diplomatic proposals
	 * @param talkToPlayer AI player they are making proposals to
	 * @param proposals List of proposals to make
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If we run into a proposal there is no code for
	 */
	@Override
	public final void submitProposals (final PlayerServerDetails player, final PlayerServerDetails talkToPlayer, final List<DiplomacyProposal> proposals, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException
	{
		boolean keepGoing = true;
		for (final DiplomacyProposal proposal : proposals)
			if (keepGoing)
				if (getDiplomacyAI ().willListenToRequest (player, talkToPlayer, proposal.getAction (), mom))
				{
					log.debug ("AI Player " + player.getPlayerDescription ().getPlayerID () + " making proposal " + proposal + " to AI player " + talkToPlayer.getPlayerDescription ().getPlayerID ());
					
					switch (proposal.getAction ())
					{
						case PROPOSE_WIZARD_PACT:
							getDiplomacyAI ().considerWizardPact (player, talkToPlayer, mom);
							break;

						case PROPOSE_ALLIANCE:
							getDiplomacyAI ().considerAlliance (player, talkToPlayer, mom);
							break;
						
						case PROPOSE_PEACE_TREATY:
							getDiplomacyAI ().considerPeaceTreaty (player, talkToPlayer, mom);
							break;
						
						case THREATEN:
							getDiplomacyAI ().respondToThreat (player, talkToPlayer, mom);
							break;
							
						// These 2 aren't really "proposals" since the recipient doesn't have the choice to agree or reject it, it just happens
						case BREAK_WIZARD_PACT_NICELY:
							getDiplomacyProcessing ().breakWizardPactNicely (player, talkToPlayer, mom);
							break;
							
						case BREAK_ALLIANCE_NICELY:
							getDiplomacyProcessing ().breakAllianceNicely (player, talkToPlayer, mom);
							break;
							
						// We know what spell they asked for, now have to find what spell we'll request in return
						case PROPOSE_EXCHANGE_SPELL:
							final MomPersistentPlayerPrivateKnowledge playerPriv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
							final MomPersistentPlayerPrivateKnowledge talkToPlayerPriv = (MomPersistentPlayerPrivateKnowledge) talkToPlayer.getPersistentPlayerPrivateKnowledge ();
							
							final List<String> spellIDsWeCanOffer = getServerSpellCalculations ().findCheapestSpells (getServerSpellCalculations ().listTradeableSpells
								(playerPriv.getSpellResearchStatus (), talkToPlayerPriv.getSpellResearchStatus ()), RequestDiplomacyMessageImpl.MAXIMUM_TRADEABLE_SPELLS, mom.getServerDB ());
							final String requestSpellIDInReturn = getDiplomacyAI ().chooseSpellToRequestInReturn (proposal.getRequestSpellID (), spellIDsWeCanOffer, mom.getServerDB ());
							if (requestSpellIDInReturn == null)
								log.debug ("AI Player " + player.getPlayerDescription ().getPlayerID () + " wanted spell ID " + proposal.getRequestSpellID () + " from AI player " +
									talkToPlayer.getPlayerDescription ().getPlayerID () + ", but there's nothing they want in return");
							else
							{
								log.debug ("AI Player " + player.getPlayerDescription ().getPlayerID () + " wants spell ID " + proposal.getRequestSpellID () + " from AI player " +
									talkToPlayer.getPlayerDescription ().getPlayerID () + ", and they want spell ID " + requestSpellIDInReturn + " in return");
	
								// chooseSpellToRequestInReturn makes sure we'll never generate a too unreasonable demand in return, but putting it here anyway for consistency with AI-to-human diplomacy
								if (getDiplomacyAI ().considerSpellTrade (proposal.getRequestSpellID (), requestSpellIDInReturn, mom.getServerDB ()))
									getDiplomacyProcessing ().agreeTradeSpells (player, talkToPlayer, mom, proposal.getRequestSpellID (), requestSpellIDInReturn);
							}
							
							break;
						
						default:
							throw new MomException ("AI player " + player.getPlayerDescription ().getPlayerID () + " trying to submit proposal " + proposal + " to AI player " +
								talkToPlayer.getPlayerDescription ().getPlayerID () + " but no code to handle it");
					}
				}
				else
				{
					// Lost patience, so stop making requests (this is like sending GROWN_IMPATIENT to the client, which ends the conversation before we can complete any more proposals)
					keepGoing = false;
					log.debug ("AI Player " + player.getPlayerDescription ().getPlayerID () + " tried to make proposal " + proposal + " to AI player " + talkToPlayer.getPlayerDescription ().getPlayerID () +
						" but they got impatient and ended the conversation");
				}
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
}
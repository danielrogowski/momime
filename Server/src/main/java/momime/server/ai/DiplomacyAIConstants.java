package momime.server.ai;

import java.util.Arrays;
import java.util.List;

import momime.common.messages.DiplomacyAction;

/**
 * Amounts that AI players' opinions of other players alter by under various circumstances
 */
public final class DiplomacyAIConstants
{
	/** Types of request that reduce patience, but can't be ignored so can't reply GROWN_IMPATIENT to them */
	public final static List<DiplomacyAction> CANNOT_IGNORE_REQUESTS = Arrays.asList
		(DiplomacyAction.BREAK_ALLIANCE_NICELY, DiplomacyAction.BREAK_WIZARD_PACT_NICELY, DiplomacyAction.THREATEN);
	
	// miscellaneous constants
	
	/** How often wizards are allowed to try to talk to each other */
	public final static int MINIMUM_TURNS_BETWEEN_TALKING = 8;
	
	// positive factors
	
	/** Bonus for forming an alliance; both players like each other for the pact, no matter who proposed or agreed to it */
	public final static int RELATION_BONUS_FORM_ALLIANCE = 20;

	/** Bonus for agreeing to a request to declare war on another wizard */
	public final static int RELATION_BONUS_FORM_AGREEING_TO_DECLARE_WAR = 15;

	/** Bonus for agreeing to a request to break alliance with another wizard */
	public final static int RELATION_BONUS_FORM_AGREEING_TO_BREAK_ALLIANCE = 15;
	
	/** Bonus for forming a wizard pact; both players like each other for the pact, no matter who proposed or agreed to it */
	public final static int RELATION_BONUS_FORM_WIZARD_PACT = 10;

	/** Bonus for forming a peace treaty; both players like each other for the treaty, no matter who proposed or agreed to it */
	public final static int RELATION_BONUS_FORM_PEACE_TREATY = 10;

	/** Bonus for forming a gold donation, multiplied by the tier 1..4 */
	public final static int RELATION_BONUS_FOR_GOLD_DONATION_PER_TIER = 5;
	
	// negative factors

	/** Penalty for breaking a wizard pact nicely via diplomacy screen */
	public final static int RELATION_PENALTY_FOR_BREAKING_WIZARD_PACT_NICELY = 10;
	
	/** Penalty for breaking an alliance nicely via diplomacy screen (in the case of a wizard asking another wizard to break their alliance with a 3rd wizard, the 3rd wizard hates them both by this amount for it) */
	public final static int RELATION_PENALTY_FOR_BREAKING_ALLIANCE_NICELY = 20;

	/** Penalty for spamming us with diplomatic proposals when we've told them to go away */
	public final static int RELATION_PENALTY_FOR_NAGGING = 20;
	
	/** Penalty for declaring war (in the case of a wizard asking another wizard to declare war on a 3rd wizard, the 3rd wizard hates both the wizards by this amount for it) */
	public final static int RELATION_PENALTY_FOR_DECLARING_WAR = 30;

	/** Penalty for threatening a wizard (regardless of how they respond to the threat) */
	public final static int RELATION_PENALTY_FOR_THREATENING = 30;
	
	// necessary relation scores for various negative actions

	/** Relation below which an AI player will threaten another player, modified by their personality type */
	public final static int MAXIMUM_RELATION_TO_THREATEN = -50;

	/** Relation below which an AI player will break a wizard pact, modified by their personality type */
	public final static int MAXIMUM_RELATION_TO_BREAK_WIZARD_PACT = -10;
	
	/** Relation below which an AI player will break an alliance, modified by their personality type */
	public final static int MAXIMUM_RELATION_TO_BREAK_ALLIANCE = 10;

	// necessary relation scores for various positive actions
	
	/** Minimum relation for an AI player to agree to a peace treaty, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_PEACE_TREATY = 0;

	/** Minimum relation for an AI player to propose a peace treaty, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_PROPOSE_PEACE_TREATY = 10;
	
	/** Minimum relation for an AI player to agree to a wizard pact, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_WIZARD_PACT = 20;

	/** Minimum relation for an AI player to propose a wizard pact, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_PROPOSE_WIZARD_PACT = 30;

	/** Minimum relation for an AI player to agree to an alliance, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_ALLIANCE = 40;

	/** Minimum relation for an AI player to propose an alliance, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_PROPOSE_ALLIANCE = 50;
	
	/** Minimum relation for an AI player to agree to declaring war on another wizard, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_DECLARE_WAR = 60;
	
	/** Minimum relation for an AI player to agree to breaking an alliance with another wizard, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_BREAK_ALLIANCE = 60;
	
}
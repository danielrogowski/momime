package momime.server.ai;

/**
 * Amounts that AI players' opinions of other players alter by under various circumstances
 */
public final class DiplomacyAIConstants
{
	// positive factors
	
	/** Bonus for forming a wizard pact; both players like each other for the pact, no matter who proposed or agreed to it */
	public final static int RELATION_BONUS_FORM_WIZARD_PACT = 10;

	/** Bonus for forming an alliance; both players like each other for the pact, no matter who proposed or agreed to it */
	public final static int RELATION_BONUS_FORM_ALLIANCE = 20;

	/** Bonus for forming a peace treaty; both players like each other for the treaty, no matter who proposed or agreed to it */
	public final static int RELATION_BONUS_FORM_PEACE_TREATY = 10;
	
	// necessary relation scores for various actions

	/** Minimum relation for an AI player to agree to a peace treaty, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_PEACE_TREATY = 0;

	/** Minimum relation for an AI player to agree to a wizard pact, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_WIZARD_PACT = 20;

	/** Minimum relation for an AI player to agree to an alliance, modified by their personality type */
	public final static int MINIMUM_RELATION_TO_AGREE_TO_ALLIANCE = 40;
}
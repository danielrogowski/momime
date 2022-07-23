package momime.client.ui.frames;

/**
 * Different text to generate for the diplomacy screen
 */
public enum DiplomacyTextState
{
	/** No text at all */
	NONE,
	
	/** When we first come in contact with a wizard, the diplomacy screen opens automatically with no need to
	 * accept or refuse just to show a greeting message, then closes after a mouse click, so no option to actually do anything */
	INITIAL_CONTACT,
	
	/** We requested another wizard talk to us and waiting to see if they accept or refuse; portrait state will be none (MIRROR) */
	WAITING_FOR_ACCEPT,
	
	/** Another wizard wants to talk to us; we get choice to accept or refuse */
	ACCEPT_OR_REFUSE_TALK,
	
	/** We requested another wizard talk to us and they refused; portrait state will be none (MIRROR) */
	REFUSED_TALK,

	/** We requested another wizard talk to us and they accepted; we get a greeting message to click once prior to getting main choices */
	ACCEPT_TALK,
	
	/** We requested another wizard talk to us and they accepted; now we get a list of choices like trading spells and making/breaking pacts to choose from and passing control of the conversation */
	MAIN_CHOICES,
	
	/** Waiting for other wizard to pick one of MAIN_CHOICES; in meantime we have nothing we can do or pick at all except we can get impatient and end the conversation */
	WAITING_FOR_CHOICE,

	/** Waiting for other wizard to accept or reject a proposal we made to them */
	WAITING_FOR_RESPONSE,

	/** General "no" message in response to some proposal */
	GENERIC_REFUSE,
	
	/** Pick a type of treaty to propose */
	PROPOSE_TREATY,

	/** Other wizard proposes a wizard pact with us */
	PROPOSE_WIZARD_PACT,
	
	/** Other wizard proposes an alliance with us */
	PROPOSE_ALLIANCE,
	
	/** Other wizard proposes a peace treaty to us */
	PROPOSE_PEACE_TREATY,

	/** Accept wizard pact */
	ACCEPT_WIZARD_PACT,
	
	/** Accept alliance */
	ACCEPT_ALLIANCE,
	
	/** Accept peace treaty */
	ACCEPT_PEACE_TREATY,

	/** Pick a type of treaty to break*/
	BREAK_TREATY,
	
	/** Other wizard broke telling us they are breaking our wizard pact or alliance nicely, via diplomacy screen */
	BREAK_WIZARD_PACT_OR_ALLIANCE,
	
	/** We broke our wizard pact or alliance nicely via diplomacy screen, other wizard is conveying their displeasure back to us */
	BROKEN_WIZARD_PACT_OR_ALLIANCE,
	
	/** Other wizard got fed up of us making proposals to them and ended the conversation */ 
	GROWN_IMPATIENT,
	
	/** Pick a type of tribute to offer */
	OFFER_TRIBUTE,
	
	/** Other wizard gave us gold */
	GIVEN_GOLD,
	
	/** Other wizard gave us a spell */
	GIVEN_SPELL,
	
	/** Other wizard saying thanks for gold we gave them */
	THANKS_FOR_GOLD,

	/** Other wizard saying thanks for spell we gave them */
	THANKS_FOR_SPELL,
	
	/** Pick a spell to give to other wizard */
	GIVE_SPELL,
	
	/** Pick a spell we want from the other wizard */
	PROPOSE_EXCHANGE_SPELL_THEIRS,
	
	/** Pick a spell to give to other wizard */
	PROPOSE_EXCHANGE_SPELL_OURS,
	
	/** Other wizard offering us a spell exchange */
	PROPOSE_EXCHANGE_SPELL,
	
	/** We requested a spell from the other wizard, but had nothing good to offer in return so they immedaitely declined */
	REFUSE_EXCHANGE_SPELL,
	
	/** Other wizard is willing to give us the spell we want, but made an unreasonable demand in return so we rejected it */
	REJECT_EXCHANGE_SPELL,

	/** Other wizard saying thanks for thr trade */
	THANKS_FOR_EXCHANGING_SPELL,
	
	/** Broken a wizard pact or alliance by attacking their units or a city, so they're mad about it */
	BROKEN_PACT_UNITS_OR_CITY,

	/** List of other wizards we can ask the wizard we're talking to to declare war on */
	CHOOSE_OTHER_WIZARD_TO_DECLARE_WAR_ON,
	
	/** Suggesting that we declare war on another wizard */
	PROPOSE_DECLARE_WAR_ON_OTHER_WIZARD,
	
	/** They agreed to declare war on another wizard like we asked */
	ACCEPT_DECLARE_WAR_ON_OTHER_WIZARD,
	
	/** List of other wizards we can ask the wizard we're talking to to break their alliance with */
	CHOOSE_OTHER_WIZARD_TO_BREAK_ALLIANCE_WITH,

	/** Suggesting that we break our alliance with another wizard */
	PROPOSE_BREAK_ALLIANCE_WITH_OTHER_WIZARD,

	/** We asked player 2 to declare war on player 3, but player 2 doesn't even know player 3 */
	CANNOT_DECLARE_WAR_ON_UNKNOWN_WIZARD,
	
	/** They agreed to break their alliance with another wizard */
	ACCEPT_BREAK_ALLIANCE_WITH_OTHER_WIZARD,
	
	/** Declaring war on you because another wizard asked us to */
	DECLARE_WAR_ON_YOU_BECAUSE_OF_OTHER_WIZARD,
	
	/** Breaking alliance with you because another wizard asked us to */
	BREAK_ALLIANCE_WITH_YOU_BECAUSE_OF_OTHER_WIZARD;
}
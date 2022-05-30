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

	/** Accept wizard pact */
	ACCEPT_WIZARD_PACT,
	
	/** Accept alliance */
	ACCEPT_ALLIANCE,
	
	/** Other wizard got fed up of us making proposals to them and ended the conversation */ 
	GROWN_IMPATIENT;
}
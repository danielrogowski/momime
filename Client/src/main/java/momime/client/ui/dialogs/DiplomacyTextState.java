package momime.client.ui.dialogs;

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
	WAITING_FOR_CHOICE;
}
package momime.editors.client.language;

/**
 * Names of elements in language XML
 */
public final class LanguageEditorDatabaseConstants
{
	/** Wizard agrees to talk to us */
	public final static String TAG_ENTITY_NORMAL_GREETING_PHRASE = "normalGreetingPhrase";

	/** Wizard agrees to talk to us, but impatient */
	public final static String TAG_ENTITY_IMPATIENT_GREETING_PHRASE = "impatientGreetingPhrase";

	/** Wizard refuses to talk to us */
	public final static String TAG_ENTITY_REFUSE_GREETING_PHRASE = "refuseGreetingPhrase";
	
	/** We make a proposal and other wizard says no; used for many different kinds of proposals */
	public final static String TAG_ENTITY_GENERIC_REFUSE_PHRASE = "genericRefusePhrase";
	
	/** Propose a wizard pact */
	public final static String TAG_ENTITY_PROPOSE_WIZARD_PACT_PHRASE = "proposeWizardPactPhrase";
	
	/** Accept a wizard pact */
	public final static String TAG_ENTITY_ACCEPT_WIZARD_PACT_PHRASE = "acceptWizardPactPhrase";
	
	/** Propose an alliance */
	public final static String TAG_ENTITY_PROPOSE_ALLIANCE_PHRASE = "proposeAlliancePhrase";
	
	/** Accept an alliance */
	public final static String TAG_ENTITY_ACCEPT_ALLIANCE_PHRASE = "acceptAlliancePhrase";
	
	/** Break a pact (we're telling other wizard thnot just a wizard pact but any kind of pact) */
	public final static String TAG_ENTITY_BREAK_PACT_PHRASE = "breakPactPhrase";
	
	/** Broken a pact (not just a wizard pact but any kind of pact) */
	public final static String TAG_ENTITY_BROKEN_PACT_PHRASE = "brokenPactPhrase";

	/** Other wizard got fed up of us making proposals to them and ended the conversation */
	public final static String TAG_ENTITY_GROWN_IMPATIENT_PHRASE = "grownImpatientPhrase";
	
	/** Other wizard saying thanks for gold we gave them */
	public final static String TAG_THANKS_FOR_GOLD_PHRASE = "thanksForGoldPhrase";
	
	/** Other wizard saying thanks for spell we gave them */
	public final static String TAG_THANKS_FOR_SPELL_PHRASE = "thanksForSpellPhrase";
	
	/**
	 * Prevent instatiation of this class
	 */
	private LanguageEditorDatabaseConstants ()
	{
	}
}
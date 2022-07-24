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
	
	/** Propose a peace treaty */
	public final static String TAG_ENTITY_PROPOSE_PEACE_TREATY_PHRASE = "proposePeaceTreatyPhrase";
	
	/** Accept a peace treaty */
	public final static String TAG_ENTITY_ACCEPT_PEACE_TREATY_PHRASE = "acceptPeaceTreatyPhrase";
	
	/** Break a pact (we're telling other wizard; not just a wizard pact but any kind of pact) */
	public final static String TAG_ENTITY_BREAK_PACT_PHRASE = "breakPactPhrase";
	
	/** Broken a pact (not just a wizard pact but any kind of pact) */
	public final static String TAG_ENTITY_BROKEN_PACT_PHRASE = "brokenPactPhrase";

	/** We broke an alliance by attacking other wizard's units */
	public final static String TAG_PACT_BROKEN_UNITS_PHRASE = "pactBrokenUnitsPhrase";

	/** We broke an alliance or wizard pact by attacking other wizard's city */
	public final static String TAG_PACT_BROKEN_CITY_PHRASE = "pactBrokenCityPhrase";
	
	/** They declared war on us because we attacked their city */
	public final static String TAG_DECLARE_WAR_CITY_PHRASE = "declareWarCityPhrase";
	
	/** Other wizard got fed up of us making proposals to them and ended the conversation */
	public final static String TAG_ENTITY_GROWN_IMPATIENT_PHRASE = "grownImpatientPhrase";
	
	/** Other wizard saying thanks for gold we gave them */
	public final static String TAG_THANKS_FOR_GOLD_PHRASE = "thanksForGoldPhrase";
	
	/** Other wizard saying thanks for spell we gave them */
	public final static String TAG_THANKS_FOR_SPELL_PHRASE = "thanksForSpellPhrase";
	
	/** Other wizard wants one of our spells; what will we ask for in return? */
	public final static String TAG_EXCHANGE_SPELL_OURS_PHRASE = "exchangeSpellOursPhrase";
	
	/** We requested a spell from the other wizard, and they said forget it OR they offered a lame spell in return and we said forget it */
	public final static String TAG_REFUSE_EXCHANGE_SPELL_PHRASE = "refuseExchangeSpellPhrase";

	/** Propose that they declare war on another wizard */
	public final static String TAG_PROPOSE_DECLARE_WAR_ON_ANOTHER_WIZARD_PHRASE = "proposeDeclareWarOnAnotherWizardPhrase";

	/** Accept that we should declare war on another wizard */
	public final static String TAG_ACCEPT_DECLARE_WAR_ON_ANOTHER_WIZARD_PHRASE = "acceptDeclareWarOnAnotherWizardPhrase";

	/** Propose that they break alliance with another wizard */
	public final static String TAG_PROPOSE_BREAK_ALLIANCE_WITH_ANOTHER_WIZARD_PHRASE = "proposeBreakAllianceWithAnotherWizardPhrase";

	/** Accept that we should break alliance with another wizard */
	public final static String TAG_ACCEPT_BREAK_ALLIANCE_WITH_ANOTHER_WIZARD_PHRASE = "acceptBreakAllianceWithAnotherWizardPhrase";

	/** Respond to threat - ignore */
	public final static String TAG_RESPOND_TO_THREAT_IGNORE_PHRASE = "respondToThreatIgnorePhrase";
	
	/** Respond to threat - give gold */
	public final static String TAG_RESPOND_TO_THREAT_GOLD_PHRASE = "respondToThreatGoldPhrase";
	
	/** Respond to threat - give spell */
	public final static String TAG_RESPOND_TO_THREAT_SPELL_PHRASE = "respondToThreatSpellPhrase";
	
	/** Respond to threat - declare war */
	public final static String TAG_RESPOND_TO_THREAT_WAR_PHRASE = "respondToThreatWarPhrase";
	
	/**
	 * Prevent instatiation of this class
	 */
	private LanguageEditorDatabaseConstants ()
	{
	}
}
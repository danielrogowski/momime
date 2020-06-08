package momime.server.ai;

/**
 * AI classifies units as some entry from this enum, so it knows what to do with each one.  This roughly corresponds to UnitSpecialOrder with the addition of TRANSPORT.
 */
enum AIUnitType
{
	/** Settlers who can make a new city */
	BUILD_CITY,
	
	/** Engineers who can pave road */
	BUILD_ROAD,
	
	/** Magic and Guardian spirits who can meld with nodes */
	MELD_WITH_NODE,
	
	/** Priests who can purify corrupted tiles */
	PURIFY,
	
	/** Any unit that can transport others, usually boats but also Floating Island */
	TRANSPORT,
	
	/** Any unit that isn't one of the above */
	COMBAT_UNIT;
}
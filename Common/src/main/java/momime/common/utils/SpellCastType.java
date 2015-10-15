package momime.common.utils;

/**
 * Used internally for calculating casting costs and so on
 */
public enum SpellCastType
{
	/** Overland map spell */
	OVERLAND,
	
	/** Combat map spell */
	COMBAT,
	
	/** Special setting used on the client only, when using the spell book to select the spell charges to imbue into a hero item */
	SPELL_CHARGES;
}
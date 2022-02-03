package momime.client.ui.dialogs;

/**
 * Different ways we can draw the wizard portrait in the diplomacy screen
 */
public enum DiplomacyPortraitState
{
	/** Animate wizard appearing in the mirror */
	APPEARING,
	
	/** Animate wizard fading out in the mirror */
	DISAPPEARING,
	
	/** Don't show the wizard at all, just the mirror (after fully disappeared) */
	MIRROR,
	
	/** Standard wizard portrait */
	NORMAL,
	
	/** Happy wizard portrait */
	HAPPY,

	/** Mad wizard portrait */
	MAD,

	/** Animation of wizard talking */
	TALKING;
}
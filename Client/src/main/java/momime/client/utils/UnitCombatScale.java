package momime.client.utils;

/**
 * The units in the original MoM were draw so at full screen in 320x200 they looked a sensible size.  The diamond shaped combat tiles are drawn in MoM IME
 * at double the size so that combat maps become 640x400 and are still the same number of tiles across/down, and while by default we can just also
 * double the size of the unit images, the other option is to bump up the number of figures but keep the units the same number of pixels - which gives
 * some really neat looking units with 24 or 32 figures in them.
 * 
 * This is a visual effect only though, the game mechanics still work as if the unit has 6 or 8 figures, otherwise it would unbalance things too badly
 * giving archer 32 arrows to fire instead of 8, and so on.
 */
public enum UnitCombatScale
{
	/** Draw all units double the number of pixels in size */
	DOUBLE_SIZE_UNITS,
	
	/** Draw all units with 4x the number of figures they really have.  Heroes are shows with an entourage of 4 cavalry. */
	FOUR_TIMES_FIGURES,
	
	/** Draw single summoned units (like Sky Drakes) still as single units, but everything else with 4x the number of figures they really have */
	FOUR_TIMES_FIGURES_EXCEPT_SINGLE_SUMMONED;
}
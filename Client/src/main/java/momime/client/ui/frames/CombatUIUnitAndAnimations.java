package momime.client.ui.frames;

import java.util.List;

import momime.client.graphics.database.AnimationGfx;
import momime.common.messages.MemoryUnit;

/**
 * The combatUI caches which unit is at each location on the combat map so it doesn't have to keep rechecking at every redraw.
 * Also it caches which animations need to be drawn for each unit due to skills (spell effects) they have, e.g. Confusion.
 * So this small storage class is held at each combat map cell to keep this info.
 */
final class CombatUIUnitAndAnimations
{
	/** Unit to draw at this combat map cell */
	private final MemoryUnit unit;
	
	/** Animations to draw on top of the unit */
	private final List<AnimationGfx> animations;
	
	/**
	 * @param aUnit Unit to draw at this combat map cell
	 * @param anAnimations Animations to draw on top of the unit
	 */
	CombatUIUnitAndAnimations (final MemoryUnit aUnit, final List<AnimationGfx> anAnimations)
	{
		unit = aUnit;
		animations = anAnimations;
	}

	/**
	 * @return Unit to draw at this combat map cell
	 */
	public final MemoryUnit getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return Animations to draw on top of the unit
	 */
	public final List<AnimationGfx> getAnimations ()
	{
		return animations;
	}
}
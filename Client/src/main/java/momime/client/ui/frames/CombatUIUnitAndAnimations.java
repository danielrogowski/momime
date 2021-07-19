package momime.client.ui.frames;

import java.awt.image.BufferedImage;
import java.util.List;

import momime.common.database.AnimationEx;
import momime.common.utils.ExpandedUnitDetails;

/**
 * The combatUI caches which unit is at each location on the combat map so it doesn't have to keep rechecking at every redraw.
 * Also it caches which animations need to be drawn for each unit due to skills (spell effects) they have, e.g. Confusion.
 * So this small storage class is held at each combat map cell to keep this info.
 */
final class CombatUIUnitAndAnimations
{
	/** Unit to draw at this combat map cell */
	private final ExpandedUnitDetails unit;
	
	/** Images to draw on top of the unit */
	private final List<BufferedImage> overlays;
	
	/** Animations to draw on top of the unit */
	private final List<AnimationEx> animations;
	
	/** List of shading colours to apply to the image */
	private final List<String> shadingColours;
	
	/**
	 * @param aUnit Unit to draw at this combat map cell
	 * @param anOverlays Images to draw on top of the unit
	 * @param anAnimations Animations to draw on top of the unit
	 * @param aShadingColours List of shading colours to apply to the image
	 */
	CombatUIUnitAndAnimations (final ExpandedUnitDetails aUnit, final List<BufferedImage> anOverlays, final List<AnimationEx> anAnimations,
		final List<String> aShadingColours)
	{
		unit = aUnit;
		overlays = anOverlays;
		animations = anAnimations;
		shadingColours = aShadingColours;
	}

	/**
	 * @return Unit to draw at this combat map cell
	 */
	public final ExpandedUnitDetails getUnit ()
	{
		return unit;
	}

	/**
	 * @return Images to draw on top of the unit
	 */
	public final List<BufferedImage> getOverlays ()
	{
		return overlays;
	}
	
	/**
	 * @return Animations to draw on top of the unit
	 */
	public final List<AnimationEx> getAnimations ()
	{
		return animations;
	}

	/**
	 * @return List of shading colours to apply to the image
	 */
	public final List<String> getShadingColours ()
	{
		return shadingColours;
	}
}
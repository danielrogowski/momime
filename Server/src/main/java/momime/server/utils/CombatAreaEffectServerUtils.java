package momime.server.utils;

import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Server side only helper methods for dealing with CAEs
 */
public final class CombatAreaEffectServerUtils
{
	/**
	 * Used for copying spells from true map into player's memory
	 * @param src Spell to copy
	 * @return Deep copy of spell
	 */
	public final static MemoryCombatAreaEffect duplicateMemoryCombatAreaEffect (final MemoryCombatAreaEffect src)
	{
		final MemoryCombatAreaEffect dest = new MemoryCombatAreaEffect ();

		dest.setCombatAreaEffectID (src.getCombatAreaEffectID ());
		dest.setCastingPlayerID (src.getCastingPlayerID ());

		if (src.getMapLocation () == null)
			dest.setMapLocation (null);
		else
		{
			final OverlandMapCoordinates mapLocation = new OverlandMapCoordinates ();
			mapLocation.setX (src.getMapLocation ().getX ());
			mapLocation.setY (src.getMapLocation ().getY ());
			mapLocation.setPlane (src.getMapLocation ().getPlane ());
			dest.setMapLocation (mapLocation);
		}

		return dest;
	}

	/**
	 * Prevent instantiation
	 */
	private CombatAreaEffectServerUtils ()
	{
	}
}

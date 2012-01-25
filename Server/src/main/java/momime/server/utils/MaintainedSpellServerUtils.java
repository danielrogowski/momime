package momime.server.utils;

import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

/**
 * Server side only helper methods for dealing with maintained spells
 */
public final class MaintainedSpellServerUtils
{
	/**
	 * Used for copying spells from true map into player's memory
	 * @param src Spell to copy
	 * @return Deep copy of spell
	 */
	public final static MemoryMaintainedSpell duplicateMemoryMaintainedSpell (final MemoryMaintainedSpell src)
	{
		final MemoryMaintainedSpell dest = new MemoryMaintainedSpell ();

		dest.setCastingPlayerID (src.getCastingPlayerID ());
		dest.setSpellID (src.getSpellID ());
		dest.setUnitURN (src.getUnitURN ());
		dest.setUnitSkillID (src.getUnitSkillID ());
		dest.setCastInCombat (src.isCastInCombat ());
		dest.setCitySpellEffectID (src.getCitySpellEffectID ());

		if (src.getCityLocation () == null)
			dest.setCityLocation (null);
		else
		{
			final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
			cityLocation.setX (src.getCityLocation ().getX ());
			cityLocation.setY (src.getCityLocation ().getY ());
			cityLocation.setPlane (src.getCityLocation ().getPlane ());
			dest.setCityLocation (cityLocation);
		}

		return dest;
	}

	/**
	 * Prevent instantiation
	 */
	private MaintainedSpellServerUtils ()
	{
	}
}

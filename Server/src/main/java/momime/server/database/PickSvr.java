package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_9.Pick;

/**
 * Just for typecasting the list of free spells
 */
public final class PickSvr extends Pick
{
	/**
	 * @return List of free spells that this pick gives (used for Artificer giving Create Item and Create Artifact for free)
	 */
	@SuppressWarnings ("unchecked")
	public final List<PickFreeSpellSvr> getPickFreeSpells ()
	{
		return (List<PickFreeSpellSvr>) (List<?>) getPickFreeSpell ();
	}
}
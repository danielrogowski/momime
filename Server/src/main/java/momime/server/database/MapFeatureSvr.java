package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_8.MapFeature;

/**
 * Typecast sub lists, so that majority of code doesn't need to reference a package that changes between versions
 */
public final class MapFeatureSvr extends MapFeature
{
	/**
	 * @return List of chances for what types of creature will inhabit a particular map feature
	 */
	@SuppressWarnings ("unchecked")
	public final List<MapFeatureMagicRealmSvr> getMapFeatureMagicRealms ()
	{
		return (List<MapFeatureMagicRealmSvr>) (List<?>) getMapFeatureMagicRealm ();
	}

	/**
	 * @return List of chances for what types of creature will inhabit a particular map feature
	 */
	@SuppressWarnings ("unchecked")
	public final List<MapFeatureTreasureBookRewardSvr> getMapFeatureTreasureBookRewards ()
	{
		return (List<MapFeatureTreasureBookRewardSvr>) (List<?>) getMapFeatureTreasureBookReward ();
	}
}
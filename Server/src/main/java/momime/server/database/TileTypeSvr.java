package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_7.TileType;

/**
 * Just for typecasting various lists
 */
public final class TileTypeSvr extends TileType
{
	/**
	 * @return Chances of each kind of map feature appearing on this tile type
	 */
	@SuppressWarnings ("unchecked")
	public final List<TileTypeFeatureChanceSvr> getTileTypeFeatureChances ()
	{
		return (List<TileTypeFeatureChanceSvr>) (List<?>) getTileTypeFeatureChance ();
	}
	
	/**
	 * @return List of effects (i.e. node auras) generated from a particular tile type 
	 */
	@SuppressWarnings ("unchecked")
	public final List<TileTypeAreaEffectSvr> getTileTypeAreaEffects ()
	{
		return (List<TileTypeAreaEffectSvr>) (List<?>) getTileTypeAreaEffect ();
	}
}
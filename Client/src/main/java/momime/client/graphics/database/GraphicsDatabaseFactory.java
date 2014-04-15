package momime.client.graphics.database;

/**
 * Factory interface for creating graphics database related objects from prototypes defined in the spring XML
 */
public interface GraphicsDatabaseFactory
{
	/**
	 * @return Newly created smoothed tile type
	 */
	public SmoothedTileTypeEx createSmoothedTileType ();
	
	/**
	 * @return Newly created tile set
	 */
	public TileSetEx createTileSet ();

	/**
	 * @return Newly created map feature
	 */
	public MapFeatureEx createMapFeature ();
	
	/**
	 * @return Newly created animation
	 */
	public AnimationEx createAnimation ();
}

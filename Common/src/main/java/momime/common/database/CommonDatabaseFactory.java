package momime.common.database;

/**
 * Factory interface for creating common database objects from prototypes defined in the spring XML
 */
public interface CommonDatabaseFactory
{
	/**
	 * @return Database with spring injections
	 */
	public CommonDatabaseImpl createDatabase ();

	/**
	 * @return Newly created wizard
	 */
	public WizardEx createWizard ();
	
	/**
	 * @return Newly created map feature
	 */
	public MapFeatureEx createMapFeature ();

	/**
	 * @return Newly created smoothed tile type
	 */
	public SmoothedTileTypeEx createSmoothedTileType ();
	
	/**
	 * @return Newly created tile set
	 */
	public TileSetEx createTileSet ();
	
	/**
	 * @return Newly created animation
	 */
	public AnimationEx createAnimation ();
}
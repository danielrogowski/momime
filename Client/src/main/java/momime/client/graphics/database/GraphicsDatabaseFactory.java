package momime.client.graphics.database;

/**
 * Factory interface for creating graphics database related objects from prototypes defined in the spring XML
 */
public interface GraphicsDatabaseFactory
{
	/**
	 * @return Newly created smoothed tile type
	 */
	public SmoothedTileTypeGfx createSmoothedTileType ();
	
	/**
	 * @return Newly created tile set
	 */
	public TileSetGfx createTileSet ();

	/**
	 * @return Newly created map feature
	 */
	public MapFeatureGfx createMapFeature ();
	
	/**
	 * @return Newly created animation
	 */
	public AnimationGfx createAnimation ();
	
	/**
	 * @return Newly created pick
	 */
	public PickGfx createPick ();
	
	/**
	 * @return Newly created wizard
	 */
	public WizardGfx createWizard ();
}
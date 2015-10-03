package momime.server.mapgenerator;

/**
 * Interface to perform some operation from the setHighestTiles/setLowestTiles methods 
 */
@FunctionalInterface
public interface ProcessTileCallback
{
	/**
	 * @param x X coordinate of tile to process
	 * @param y Y coordinate of tile to process
	 */
	public void process (final int x, final int y);
}
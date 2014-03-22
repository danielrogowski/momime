package momime.client.graphics.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.ObjectFactory;
import momime.client.graphics.database.v0_9_5.SmoothedTileType;
import momime.client.graphics.database.v0_9_5.SmoothingSystem;
import momime.client.graphics.database.v0_9_5.TileSet;

/**
 * Creates our custom extended GraphicsDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class GraphicsDatabaseFactory extends ObjectFactory
{
	/**
	 * @return Custom extended GraphicsDatabase 
	 */
	@Override
	public final GraphicsDatabase createGraphicsDatabase ()
	{
		return new GraphicsDatabaseExImpl ();
	}

	/**
	 * @return Custom extended TileSet 
	 */
	@Override
	public final TileSet createTileSet ()
	{
		return new TileSetEx ();
	}

	/**
	 * @return Custom extended SmoothedTileType 
	 */
	@Override
	public final SmoothedTileType createSmoothedTileType ()
	{
		return new SmoothedTileTypeEx ();
	}

	/**
	 * @return Custom extended SmoothingSystem 
	 */
	@Override
	public final SmoothingSystem createSmoothingSystem ()
	{
		return new SmoothingSystemEx ();
	}
}

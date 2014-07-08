package momime.client.graphics.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.MapFeature;
import momime.client.graphics.database.v0_9_5.ObjectFactory;
import momime.client.graphics.database.v0_9_5.ProductionType;
import momime.client.graphics.database.v0_9_5.Race;
import momime.client.graphics.database.v0_9_5.SmoothedTileType;
import momime.client.graphics.database.v0_9_5.SmoothingSystem;
import momime.client.graphics.database.v0_9_5.TileSet;

/**
 * Creates our custom extended GraphicsDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class GraphicsDatabaseObjectFactory extends ObjectFactory
{
	/** Factory for creating prototype message beans from spring */
	private GraphicsDatabaseFactory factory;
	
	/**
	 * @return Custom extended GraphicsDatabase 
	 */
	@Override
	public final GraphicsDatabase createGraphicsDatabase ()
	{
		return new GraphicsDatabaseExImpl ();
	}

	/**
	 * @return Custom extended Race
	 */
	@Override
	public final Race createRace ()
	{
		return new RaceEx ();
	}
	
	/**
	 * @return Custom extended TileSet 
	 */
	@Override
	public final TileSet createTileSet ()
	{
		return getFactory ().createTileSet ();
	}

	/**
	 * @return Custom extended SmoothedTileType 
	 */
	@Override
	public final SmoothedTileType createSmoothedTileType ()
	{
		return getFactory ().createSmoothedTileType ();
	}

	/**
	 * @return Custom extended SmoothingSystem 
	 */
	@Override
	public final SmoothingSystem createSmoothingSystem ()
	{
		return new SmoothingSystemEx ();
	}
	
	/**
	 * @return Custom extended MapFeature 
	 */
	@Override
	public final MapFeature createMapFeature ()
	{
		return getFactory ().createMapFeature ();
	}

	/**
	 * @return Custom extended Animation 
	 */
	@Override
	public final Animation createAnimation ()
	{
		return getFactory ().createAnimation ();
	}
	
	/**
	 * @return Custom extended Production type 
	 */
	@Override
	public final ProductionType createProductionType ()
	{
		return new ProductionTypeEx ();
	}

	/**
	 * @return Factory for creating prototype message beans from spring
	 */
	public final GraphicsDatabaseFactory getFactory ()
	{
		return factory;
	}

	/**
	 * @param fac Factory for creating prototype message beans from spring
	 */
	public final void setFactory (final GraphicsDatabaseFactory fac)
	{
		factory = fac;
	}
}
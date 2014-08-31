package momime.client.graphics.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.CombatTileFigurePositions;
import momime.client.graphics.database.v0_9_5.CombatTileUnitRelativeScale;
import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.MapFeature;
import momime.client.graphics.database.v0_9_5.ObjectFactory;
import momime.client.graphics.database.v0_9_5.ProductionType;
import momime.client.graphics.database.v0_9_5.Race;
import momime.client.graphics.database.v0_9_5.RangedAttackType;
import momime.client.graphics.database.v0_9_5.SmoothedTileType;
import momime.client.graphics.database.v0_9_5.SmoothingSystem;
import momime.client.graphics.database.v0_9_5.TileSet;
import momime.client.graphics.database.v0_9_5.Unit;
import momime.client.graphics.database.v0_9_5.UnitAttribute;
import momime.client.graphics.database.v0_9_5.UnitCombatAction;
import momime.client.graphics.database.v0_9_5.UnitType;

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
	 * @return Custom extended Unit attribute
	 */
	@Override
	public final UnitAttribute createUnitAttribute ()
	{
		return new UnitAttributeEx ();
	}
	
	/**
	 * @return Custom extended Ranged attack type
	 */
	@Override
	public final RangedAttackType createRangedAttackType ()
	{
		return new RangedAttackTypeEx ();
	}
	
	/**
	 * @return Custom extended Unit type
	 */
	@Override
	public final UnitType createUnitType ()
	{
		return new UnitTypeEx ();
	}

	/**
	 * @return Custom extended Unit
	 */
	@Override
	public final Unit createUnit ()
	{
		return new UnitEx ();
	}

	/**
	 * @return Custom extended scale
	 */
	@Override
	public final CombatTileUnitRelativeScale createCombatTileUnitRelativeScale ()
	{
		return new CombatTileUnitRelativeScaleEx ();
	}

	/**
	 * @return Custom extended Unit positions
	 */
	@Override
	public final CombatTileFigurePositions createCombatTileFigurePositions ()
	{
		return new CombatTileFigurePositionsEx ();
	}

	/**
	 * @return Custom extended Unit action
	 */
	@Override
	public final UnitCombatAction createUnitCombatAction ()
	{
		return new UnitCombatActionEx ();
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
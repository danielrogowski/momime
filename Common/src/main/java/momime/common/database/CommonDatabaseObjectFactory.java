package momime.common.database;

import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Creates our custom extended database when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class CommonDatabaseObjectFactory extends ObjectFactory
{
	/** Factory for creating prototype message beans from spring */
	private CommonDatabaseFactory factory;
	
	/**
	 * @return Custom extended database 
	 */
	@Override
	public final MomDatabase createMomDatabase ()
	{
		return getFactory ().createDatabase ();
	}

	/**
	 * @return Custom extended wizard 
	 */
	@Override
	public final Wizard createWizard ()
	{
		return getFactory ().createWizard ();
	}
	
	/**
	 * @return Custom extended unit 
	 */
	@Override
	public final Unit createUnit ()
	{
		return new UnitEx ();
	}

	/**
	 * @return Custom extended race 
	 */
	@Override
	public final Race createRace ()
	{
		return new RaceEx ();
	}

	/**
	 * @return Custom extended production type
	 */
	@Override
	public final ProductionType createProductionType ()
	{
		return new ProductionTypeEx ();
	}

	/**
	 * @return Custom extended ranged attack type
	 */
	@Override
	public final RangedAttackType createRangedAttackType ()
	{
		return new RangedAttackTypeEx ();
	}

	/**
	 * @return Custom extended unit skill 
	 */
	@Override
	public final UnitSkill createUnitSkill ()
	{
		return new UnitSkillEx ();
	}

	/**
	 * @return Custom extended unit combat action 
	 */
	@Override
	public final UnitCombatAction createUnitCombatAction ()
	{
		return new UnitCombatActionEx ();
	}

	/**
	 * @return Custom extended map feature 
	 */
	@Override
	public final MapFeature createMapFeature ()
	{
		return getFactory ().createMapFeature ();
	}
	
	/**
	 * @return Custom extended tile type 
	 */
	@Override
	public final TileType createTileType ()
	{
		return new TileTypeEx ();
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
	 * @return Custom extended Animation 
	 */
	@Override
	public final Animation createAnimation ()
	{
		return getFactory ().createAnimation ();
	}
	
	/**
	 * @return Factory for creating prototype message beans from spring
	 */
	public final CommonDatabaseFactory getFactory ()
	{
		return factory;
	}
	
	/**
	 * @param fac Factory for creating prototype message beans from spring
	 */
	public final void setFactory (final CommonDatabaseFactory fac)
	{
		factory = fac;
	}
}
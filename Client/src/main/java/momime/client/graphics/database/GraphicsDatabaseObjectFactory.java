package momime.client.graphics.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.graphics.database.v0_9_8.Animation;
import momime.client.graphics.database.v0_9_8.CityImage;
import momime.client.graphics.database.v0_9_8.CityViewElement;
import momime.client.graphics.database.v0_9_8.CombatAction;
import momime.client.graphics.database.v0_9_8.CombatAreaEffect;
import momime.client.graphics.database.v0_9_8.CombatTileBorderImage;
import momime.client.graphics.database.v0_9_8.CombatTileFigurePositions;
import momime.client.graphics.database.v0_9_8.CombatTileUnitRelativeScale;
import momime.client.graphics.database.v0_9_8.FigurePositionsForFigureCount;
import momime.client.graphics.database.v0_9_8.GraphicsDatabase;
import momime.client.graphics.database.v0_9_8.HeroItemSlotType;
import momime.client.graphics.database.v0_9_8.HeroItemType;
import momime.client.graphics.database.v0_9_8.MapFeature;
import momime.client.graphics.database.v0_9_8.ObjectFactory;
import momime.client.graphics.database.v0_9_8.Pick;
import momime.client.graphics.database.v0_9_8.PlayList;
import momime.client.graphics.database.v0_9_8.ProductionType;
import momime.client.graphics.database.v0_9_8.Race;
import momime.client.graphics.database.v0_9_8.RangedAttackType;
import momime.client.graphics.database.v0_9_8.RangedAttackTypeCombatImage;
import momime.client.graphics.database.v0_9_8.SmoothedTile;
import momime.client.graphics.database.v0_9_8.SmoothedTileType;
import momime.client.graphics.database.v0_9_8.SmoothingSystem;
import momime.client.graphics.database.v0_9_8.Spell;
import momime.client.graphics.database.v0_9_8.TileSet;
import momime.client.graphics.database.v0_9_8.TileType;
import momime.client.graphics.database.v0_9_8.TileTypeRoad;
import momime.client.graphics.database.v0_9_8.Unit;
import momime.client.graphics.database.v0_9_8.UnitCombatAction;
import momime.client.graphics.database.v0_9_8.UnitCombatImage;
import momime.client.graphics.database.v0_9_8.UnitSkill;
import momime.client.graphics.database.v0_9_8.UnitSkillComponentImage;
import momime.client.graphics.database.v0_9_8.UnitSpecialOrderImage;
import momime.client.graphics.database.v0_9_8.UnitType;
import momime.client.graphics.database.v0_9_8.WeaponGrade;
import momime.client.graphics.database.v0_9_8.Wizard;

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
		return new RaceGfx ();
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
		return new SmoothingSystemGfx ();
	}
	
	/**
	 * @return Custom extended TileType
	 */
	@Override
	public final TileType createTileType ()
	{
		return new TileTypeGfx ();
	}

	/**
	 * @return Custom extended road
	 */
	@Override
	public final TileTypeRoad createTileTypeRoad ()
	{
		return new TileTypeRoadGfx ();
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
		return new ProductionTypeGfx ();
	}
	
	/**
	 * @return Custom extended Ranged attack type
	 */
	@Override
	public final RangedAttackType createRangedAttackType ()
	{
		return new RangedAttackTypeGfx ();
	}
	
	/**
	 * @return Custom extended Unit type
	 */
	@Override
	public final UnitType createUnitType ()
	{
		return new UnitTypeGfx ();
	}

	/**
	 * @return Custom extended Unit
	 */
	@Override
	public final Unit createUnit ()
	{
		return new UnitGfx ();
	}

	/**
	 * @return Custom extended scale
	 */
	@Override
	public final CombatTileUnitRelativeScale createCombatTileUnitRelativeScale ()
	{
		return new CombatTileUnitRelativeScaleGfx ();
	}

	/**
	 * @return Custom extended Unit positions
	 */
	@Override
	public final CombatTileFigurePositions createCombatTileFigurePositions ()
	{
		return new CombatTileFigurePositionsGfx ();
	}

	/**
	 * @return Custom extended Unit action
	 */
	@Override
	public final UnitCombatAction createUnitCombatAction ()
	{
		return new UnitCombatActionGfx ();
	}
	
	/**
	 * @return Custom extended Pick
	 */
	@Override
	public final Pick createPick ()
	{
		return factory.createPick ();
	}

	/**
	 * @return Custom extended Spell
	 */
	@Override
	public final Spell createSpell ()
	{
		return new SpellGfx ();
	}

	/**
	 * @return Custom extended Wizard
	 */
	@Override
	public final Wizard createWizard ()
	{
		return factory.createWizard ();
	}
	
	/**
	 * @return Custom extended City view element
	 */
	@Override
	public final CityViewElement createCityViewElement ()
	{
		return new CityViewElementGfx ();
	}

	/**
	 * @return Custom extended Combat action
	 */
	@Override
	public final CombatAction createCombatAction ()
	{
		return new CombatActionGfx ();
	}
	
	/**
	 * @return Custom extended Unit skill
	 */
	@Override
	public final UnitSkill createUnitSkill ()
	{
		return new UnitSkillGfx ();
	}
	
	/**
	 * @return Custom extended Unit combat image
	 */
	@Override
	public final UnitCombatImage createUnitCombatImage ()
	{
		return new UnitCombatImageGfx ();
	}

	/**
	 * @return Custom extended RAT combat image
	 */
	@Override
	public final RangedAttackTypeCombatImage createRangedAttackTypeCombatImage ()
	{
		return new RangedAttackTypeCombatImageGfx ();
	}

	/**
	 * @return Custom extended Figure positions for Figure count
	 */
	@Override
	public final FigurePositionsForFigureCount createFigurePositionsForFigureCount ()
	{
		return new FigurePositionsForFigureCountGfx ();
	}

	/**
	 * @return Custom extended Combat area effect
	 */
	@Override
	public final CombatAreaEffect createCombatAreaEffect ()
	{
		return new CombatAreaEffectGfx ();
	}

	/**
	 * @return Custom extended Play list
	 */
	@Override
	public final PlayList createPlayList ()
	{
		return new PlayListGfx ();
	}

	/**
	 * @return Custom extended Smoothed tile
	 */
	@Override
	public final SmoothedTile createSmoothedTile ()
	{
		return new SmoothedTileGfx ();
	}

	/**
	 * @return Custom extended City image
	 */
	@Override
	public final CityImage createCityImage ()
	{
		return new CityImageGfx ();
	}

	/**
	 * @return Custom extended Weapon grade
	 */
	@Override
	public final WeaponGrade createWeaponGrade ()
	{
		return new WeaponGradeGfx ();
	}
	
	/**
	 * @return Custom extended Unit attribute component images
	 */
	@Override
	public final UnitSkillComponentImage createUnitSkillComponentImage ()
	{
		return new UnitSkillComponentImageGfx ();
	}

	/**
	 * @return Custom extended Unit special order images
	 */
	@Override
	public final UnitSpecialOrderImage createUnitSpecialOrderImage ()
	{
		return new UnitSpecialOrderImageGfx ();
	}

	/**
	 * @return Custom extended Combat tile border image
	 */
	@Override
	public final CombatTileBorderImage createCombatTileBorderImage ()
	{
		return new CombatTileBorderImageGfx ();
	}
	
	/**
	 * @return Custom extended Hero item type
	 */
	@Override
	public final HeroItemType createHeroItemType ()
	{
		return new HeroItemTypeGfx ();
	}

	/**
	 * @return Custom extended Hero item slot type
	 */
	@Override
	public final HeroItemSlotType createHeroItemSlotType ()
	{
		return new HeroItemSlotTypeGfx ();
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
package momime.server.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.server.database.v0_9_7.AttackResolution;
import momime.server.database.v0_9_7.AttackResolutionCondition;
import momime.server.database.v0_9_7.AttackResolutionStep;
import momime.server.database.v0_9_7.Building;
import momime.server.database.v0_9_7.CityNameContainer;
import momime.server.database.v0_9_7.CitySize;
import momime.server.database.v0_9_7.CitySpellEffect;
import momime.server.database.v0_9_7.CombatAreaEffect;
import momime.server.database.v0_9_7.CombatMapElement;
import momime.server.database.v0_9_7.CombatTileBorder;
import momime.server.database.v0_9_7.CombatTileType;
import momime.server.database.v0_9_7.DifficultyLevel;
import momime.server.database.v0_9_7.FogOfWarSetting;
import momime.server.database.v0_9_7.HeroItemBonus;
import momime.server.database.v0_9_7.LandProportion;
import momime.server.database.v0_9_7.MapFeature;
import momime.server.database.v0_9_7.MapFeatureMagicRealm;
import momime.server.database.v0_9_7.NodeStrength;
import momime.server.database.v0_9_7.ObjectFactory;
import momime.server.database.v0_9_7.OverlandMapSize;
import momime.server.database.v0_9_7.Pick;
import momime.server.database.v0_9_7.PickFreeSpell;
import momime.server.database.v0_9_7.PickType;
import momime.server.database.v0_9_7.PickTypeCountContainer;
import momime.server.database.v0_9_7.PickTypeGrantsSpells;
import momime.server.database.v0_9_7.Plane;
import momime.server.database.v0_9_7.ProductionType;
import momime.server.database.v0_9_7.Race;
import momime.server.database.v0_9_7.RangedAttackType;
import momime.server.database.v0_9_7.ServerDatabase;
import momime.server.database.v0_9_7.Spell;
import momime.server.database.v0_9_7.SpellRank;
import momime.server.database.v0_9_7.SpellSetting;
import momime.server.database.v0_9_7.TileType;
import momime.server.database.v0_9_7.TileTypeAreaEffect;
import momime.server.database.v0_9_7.TileTypeFeatureChance;
import momime.server.database.v0_9_7.Unit;
import momime.server.database.v0_9_7.UnitSetting;
import momime.server.database.v0_9_7.UnitSkill;
import momime.server.database.v0_9_7.UnitType;
import momime.server.database.v0_9_7.WeaponGrade;
import momime.server.database.v0_9_7.Wizard;
import momime.server.database.v0_9_7.WizardPickCount;

/**
 * Creates our custom extended ServerDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class ServerDatabaseObjectFactory extends ObjectFactory
{
	/** Factory for creating prototype message beans from spring */
	private ServerDatabaseFactory factory;
	
	/**
	 * @return Custom extended ServerDatabase 
	 */
	@Override
	public final ServerDatabase createServerDatabase ()
	{
		return getFactory ().createDatabase ();
	}

	/**
	 * @return Custom extended Plane 
	 */
	@Override
	public final Plane createPlane ()
	{
		return new PlaneSvr ();
	}

	/**
	 * @return Custom extended ProductionType 
	 */
	@Override
	public final ProductionType createProductionType ()
	{
		return new ProductionTypeSvr ();
	}

	/**
	 * @return Custom extended MapFeature 
	 */
	@Override
	public final MapFeature createMapFeature ()
	{
		return new MapFeatureSvr ();
	}

	/**
	 * @return Custom extended TileType 
	 */
	@Override
	public final TileType createTileType ()
	{
		return new TileTypeSvr ();
	}

	/**
	 * @return Custom extended PickType 
	 */
	@Override
	public final PickType createPickType ()
	{
		return new PickTypeSvr ();
	}

	/**
	 * @return Custom extended Pick 
	 */
	@Override
	public final Pick createPick ()
	{
		return new PickSvr ();
	}

	/**
	 * @return Custom extended Wizard 
	 */
	@Override
	public final Wizard createWizard ()
	{
		return new WizardSvr ();
	}

	/**
	 * @return Custom extended Race 
	 */
	@Override
	public final Race createRace ()
	{
		return new RaceSvr ();
	}

	/**
	 * @return Custom extended Building 
	 */
	@Override
	public final Building createBuilding ()
	{
		return new BuildingSvr ();
	}

	/**
	 * @return Custom extended CitySize 
	 */
	@Override
	public final CitySize createCitySize ()
	{
		return new CitySizeSvr ();
	}

	/**
	 * @return Custom extended UnitType 
	 */
	@Override
	public final UnitType createUnitType ()
	{
		return new UnitTypeSvr ();
	}

	/**
	 * @return Custom extended UnitSkill 
	 */
	@Override
	public final UnitSkill createUnitSkill ()
	{
		return new UnitSkillSvr ();
	}

	/**
	 * @return Custom extended RangedAttackType 
	 */
	@Override
	public final RangedAttackType createRangedAttackType ()
	{
		return new RangedAttackTypeSvr ();
	}

	/**
	 * @return Custom extended Unit 
	 */
	@Override
	public final Unit createUnit ()
	{
		return new UnitSvr ();
	}

	/**
	 * @return Custom extended WeaponGrade 
	 */
	@Override
	public final WeaponGrade createWeaponGrade ()
	{
		return new WeaponGradeSvr ();
	}

	/**
	 * @return Custom extended CombatAreaEffect 
	 */
	@Override
	public final CombatAreaEffect createCombatAreaEffect ()
	{
		return new CombatAreaEffectSvr ();
	}

	/**
	 * @return Custom extended Spell 
	 */
	@Override
	public final Spell createSpell ()
	{
		return new SpellSvr ();
	}

	/**
	 * @return Custom extended OverlandMapSize 
	 */
	@Override
	public final OverlandMapSize createOverlandMapSize ()
	{
		return new OverlandMapSizeSvr ();
	}

	/**
	 * @return Custom extended LandProportion 
	 */
	@Override
	public final LandProportion createLandProportion ()
	{
		return new LandProportionSvr ();
	}

	/**
	 * @return Custom extended NodeStrength 
	 */
	@Override
	public final NodeStrength createNodeStrength ()
	{
		return new NodeStrengthSvr ();
	}

	/**
	 * @return Custom extended DifficultyLevel 
	 */
	@Override
	public final DifficultyLevel createDifficultyLevel ()
	{
		return new DifficultyLevelSvr ();
	}

	/**
	 * @return Custom extended FogOfWarSetting 
	 */
	@Override
	public final FogOfWarSetting createFogOfWarSetting ()
	{
		return new FogOfWarSettingSvr ();
	}

	/**
	 * @return Custom extended UnitSetting 
	 */
	@Override
	public final UnitSetting createUnitSetting ()
	{
		return new UnitSettingSvr ();
	}

	/**
	 * @return Custom extended SpellSetting 
	 */
	@Override
	public final SpellSetting createSpellSetting ()
	{
		return new SpellSettingSvr ();
	}

	/**
	 * @return Custom extended CombatTileType 
	 */
	@Override
	public final CombatTileType createCombatTileType ()
	{
		return new CombatTileTypeSvr ();
	}

	/**
	 * @return Custom extended CombatTileBorder 
	 */
	@Override
	public final CombatTileBorder createCombatTileBorder ()
	{
		return new CombatTileBorderSvr ();
	}

	/**
	 * @return Custom extended CombatMapElement 
	 */
	@Override
	public final CombatMapElement createCombatMapElement ()
	{
		return new CombatMapElementSvr ();
	}

	/**
	 * @return Custom extended PickTypeCountContainer 
	 */
	@Override
	public final PickTypeCountContainer createPickTypeCountContainer ()
	{
		return new PickTypeCountContainerSvr ();
	}

	/**
	 * @return Custom extended WizardPickCount 
	 */
	@Override
	public final WizardPickCount createWizardPickCount ()
	{
		return new WizardPickCountSvr ();
	}

	/**
	 * @return Custom extended CityNameContainer 
	 */
	@Override
	public final CityNameContainer createCityNameContainer ()
	{
		return new CityNameContainerSvr ();
	}

	/**
	 * @return Custom extended TileTypeFeatureChance
	 */
	@Override
	public final TileTypeFeatureChance createTileTypeFeatureChance ()
	{
		return new TileTypeFeatureChanceSvr ();
	}

	/**
	 * @return Custom extended TileTypeAreaEffect
	 */
	@Override
	public final TileTypeAreaEffect createTileTypeAreaEffect ()
	{
		return new TileTypeAreaEffectSvr ();
	}

	/**
	 * @return Custom extended PickTypeGrantsSpells
	 */
	@Override
	public final PickTypeGrantsSpells createPickTypeGrantsSpells ()
	{
		return new PickTypeGrantsSpellsSvr ();
	}

	/**
	 * @return Custom extended MapFeatureMagicRealm
	 */
	@Override
	public final MapFeatureMagicRealm createMapFeatureMagicRealm ()
	{
		return new MapFeatureMagicRealmSvr ();
	}

	/**
	 * @return Custom extended PickFreeSpell
	 */
	@Override
	public final PickFreeSpell createPickFreeSpell ()
	{
		return new PickFreeSpellSvr ();
	}

	/**
	 * @return Custom extended CitySpellEffect
	 */
	@Override
	public final CitySpellEffect createCitySpellEffect ()
	{
		return new CitySpellEffectSvr ();
	}

	/**
	 * @return Custom extended AttackResolution
	 */
	@Override
	public final AttackResolution createAttackResolution ()
	{
		return new AttackResolutionSvr ();
	}

	/**
	 * @return Custom extended AttackResolutionCondition
	 */
	@Override
	public final AttackResolutionCondition createAttackResolutionCondition ()
	{
		return new AttackResolutionConditionSvr ();
	}

	/**
	 * @return Custom extended AttackResolutionStep
	 */
	@Override
	public final AttackResolutionStep createAttackResolutionStep ()
	{
		return new AttackResolutionStepSvr ();
	}

	/**
	 * @return Custom extended HeroItemBonus
	 */
	@Override
	public final HeroItemBonus createHeroItemBonus ()
	{
		return new HeroItemBonusSvr ();
	}

	/**
	 * @return Custom extended SpellRank
	 */
	@Override
	public final SpellRank createSpellRank ()
	{
		return new SpellRankSvr ();
	}

	/**
	 * @return Factory for creating prototype message beans from spring
	 */
	public final ServerDatabaseFactory getFactory ()
	{
		return factory;
	}
	
	/**
	 * @param fac Factory for creating prototype message beans from spring
	 */
	public final void setFactory (final ServerDatabaseFactory fac)
	{
		factory = fac;
	}
}
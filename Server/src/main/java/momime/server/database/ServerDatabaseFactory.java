package momime.server.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.server.database.v0_9_6.Building;
import momime.server.database.v0_9_6.CityNameContainer;
import momime.server.database.v0_9_6.CitySize;
import momime.server.database.v0_9_6.CitySpellEffect;
import momime.server.database.v0_9_6.CombatAreaEffect;
import momime.server.database.v0_9_6.CombatMapElement;
import momime.server.database.v0_9_6.CombatTileBorder;
import momime.server.database.v0_9_6.CombatTileType;
import momime.server.database.v0_9_6.DifficultyLevel;
import momime.server.database.v0_9_6.FogOfWarSetting;
import momime.server.database.v0_9_6.LandProportion;
import momime.server.database.v0_9_6.MapFeature;
import momime.server.database.v0_9_6.MapFeatureMagicRealm;
import momime.server.database.v0_9_6.MovementRateRule;
import momime.server.database.v0_9_6.NodeStrength;
import momime.server.database.v0_9_6.ObjectFactory;
import momime.server.database.v0_9_6.OverlandMapSize;
import momime.server.database.v0_9_6.Pick;
import momime.server.database.v0_9_6.PickFreeSpell;
import momime.server.database.v0_9_6.PickType;
import momime.server.database.v0_9_6.PickTypeCountContainer;
import momime.server.database.v0_9_6.PickTypeGrantsSpells;
import momime.server.database.v0_9_6.Plane;
import momime.server.database.v0_9_6.ProductionType;
import momime.server.database.v0_9_6.Race;
import momime.server.database.v0_9_6.RangedAttackType;
import momime.server.database.v0_9_6.ServerDatabase;
import momime.server.database.v0_9_6.Spell;
import momime.server.database.v0_9_6.SpellSetting;
import momime.server.database.v0_9_6.TileType;
import momime.server.database.v0_9_6.TileTypeAreaEffect;
import momime.server.database.v0_9_6.TileTypeFeatureChance;
import momime.server.database.v0_9_6.Unit;
import momime.server.database.v0_9_6.UnitAttribute;
import momime.server.database.v0_9_6.UnitMagicRealm;
import momime.server.database.v0_9_6.UnitSetting;
import momime.server.database.v0_9_6.UnitSkill;
import momime.server.database.v0_9_6.UnitType;
import momime.server.database.v0_9_6.WeaponGrade;
import momime.server.database.v0_9_6.Wizard;
import momime.server.database.v0_9_6.WizardPickCount;

/**
 * Creates our custom extended ServerDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class ServerDatabaseFactory extends ObjectFactory
{
	/**
	 * @return Custom extended ServerDatabase 
	 */
	@Override
	public final ServerDatabase createServerDatabase ()
	{
		return new ServerDatabaseExImpl ();
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
	 * @return Custom extended UnitAttribute 
	 */
	@Override
	public final UnitAttribute createUnitAttribute ()
	{
		return new UnitAttributeSvr ();
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
	 * @return Custom extended MovementRateRule 
	 */
	@Override
	public final MovementRateRule createMovementRateRule ()
	{
		return new MovementRateRuleSvr ();
	}

	/**
	 * @return Custom extended UnitMagicRealm 
	 */
	@Override
	public final UnitMagicRealm createUnitMagicRealm ()
	{
		return new UnitMagicRealmSvr ();
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
}
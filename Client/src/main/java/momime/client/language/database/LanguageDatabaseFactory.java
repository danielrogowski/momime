package momime.client.language.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.language.database.v0_9_9.Building;
import momime.client.language.database.v0_9_9.CitySize;
import momime.client.language.database.v0_9_9.CitySpellEffect;
import momime.client.language.database.v0_9_9.CombatAreaEffect;
import momime.client.language.database.v0_9_9.DamageType;
import momime.client.language.database.v0_9_9.DifficultyLevel;
import momime.client.language.database.v0_9_9.FogOfWarSetting;
import momime.client.language.database.v0_9_9.Hero;
import momime.client.language.database.v0_9_9.HeroItemBonus;
import momime.client.language.database.v0_9_9.HeroItemSlotType;
import momime.client.language.database.v0_9_9.HeroItemType;
import momime.client.language.database.v0_9_9.KnownServer;
import momime.client.language.database.v0_9_9.LandProportion;
import momime.client.language.database.v0_9_9.LanguageCategory;
import momime.client.language.database.v0_9_9.LanguageDatabase;
import momime.client.language.database.v0_9_9.MapFeature;
import momime.client.language.database.v0_9_9.NodeStrength;
import momime.client.language.database.v0_9_9.ObjectFactory;
import momime.client.language.database.v0_9_9.OverlandMapSize;
import momime.client.language.database.v0_9_9.Pick;
import momime.client.language.database.v0_9_9.PickType;
import momime.client.language.database.v0_9_9.Plane;
import momime.client.language.database.v0_9_9.PopulationTask;
import momime.client.language.database.v0_9_9.ProductionType;
import momime.client.language.database.v0_9_9.Race;
import momime.client.language.database.v0_9_9.RangedAttackType;
import momime.client.language.database.v0_9_9.ShortcutKey;
import momime.client.language.database.v0_9_9.Spell;
import momime.client.language.database.v0_9_9.SpellBookSection;
import momime.client.language.database.v0_9_9.SpellRank;
import momime.client.language.database.v0_9_9.SpellSetting;
import momime.client.language.database.v0_9_9.TileType;
import momime.client.language.database.v0_9_9.Unit;
import momime.client.language.database.v0_9_9.UnitSetting;
import momime.client.language.database.v0_9_9.UnitSkill;
import momime.client.language.database.v0_9_9.UnitType;
import momime.client.language.database.v0_9_9.Wizard;

/**
 * Creates our custom extended LanguageDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class LanguageDatabaseFactory extends ObjectFactory
{
	/**
	 * @return Custom extended LanguageDatabase 
	 */
	@Override
	public final LanguageDatabase createLanguageDatabase ()
	{
		return new LanguageDatabaseExImpl ();
	}

	/**
	 * @return Custom extended LanguageCategory 
	 */
	@Override
	public final LanguageCategory createLanguageCategory ()
	{
		return new LanguageCategoryEx ();
	}

	/**
	 * @return Custom extended UnitType
	 */
	@Override
	public final UnitType createUnitType ()
	{
		return new UnitTypeLang ();
	}

	/**
	 * @return Custom extended Plane
	 */
	@Override
	public final Plane createPlane ()
	{
		return new PlaneLang ();
	}

	/**
	 * @return Custom extended ProductionType
	 */
	@Override
	public final ProductionType createProductionType ()
	{
		return new ProductionTypeLang ();
	}

	/**
	 * @return Custom extended MapFeature
	 */
	@Override
	public final MapFeature createMapFeature ()
	{
		return new MapFeatureLang ();
	}

	/**
	 * @return Custom extended TileType
	 */
	@Override
	public final TileType createTileType ()
	{
		return new TileTypeLang ();
	}

	/**
	 * @return Custom extended PickType
	 */
	@Override
	public final PickType createPickType ()
	{
		return new PickTypeLang ();
	}

	/**
	 * @return Custom extended Pick
	 */
	@Override
	public final Pick createPick ()
	{
		return new PickLang ();
	}

	/**
	 * @return Custom extended PopulationTask
	 */
	@Override
	public final PopulationTask createPopulationTask ()
	{
		return new PopulationTaskLang ();
	}

	/**
	 * @return Custom extended Race
	 */
	@Override
	public final Race createRace ()
	{
		return new RaceLang ();
	}

	/**
	 * @return Custom extended Building
	 */
	@Override
	public final Building createBuilding ()
	{
		return new BuildingLang ();
	}

	/**
	 * @return Custom extended Unit
	 */
	@Override
	public final Unit createUnit ()
	{
		return new UnitLang ();
	}

	/**
	 * @return Custom extended Spell
	 */
	@Override
	public final Spell createSpell ()
	{
		return new SpellLang ();
	}

	/**
	 * @return Custom extended CombatAreaEffect
	 */
	@Override
	public final CombatAreaEffect createCombatAreaEffect ()
	{
		return new CombatAreaEffectLang ();
	}

	/**
	 * @return Custom extended SpellBookSection
	 */
	@Override
	public final SpellBookSection createSpellBookSection ()
	{
		return new SpellBookSectionLang ();
	}

	/**
	 * @return Custom extended CitySpellEffect
	 */
	@Override
	public final CitySpellEffect createCitySpellEffect ()
	{
		return new CitySpellEffectLang ();
	}

	/**
	 * @return Custom extended ShortcutKey
	 */
	@Override
	public final ShortcutKey createShortcutKey ()
	{
		return new ShortcutKeyLang ();
	}

	/**
	 * @return Custom extended UnitSkill
	 */
	@Override
	public final UnitSkill createUnitSkill ()
	{
		return new UnitSkillLang ();
	}
	
	/**
	 * @return Custom extended KnownServer
	 */
	@Override
	public final KnownServer createKnownServer ()
	{
		return new KnownServerLang ();
	}

	/**
	 * @return Custom extended Wizard
	 */
	@Override
	public final Wizard createWizard ()
	{
		return new WizardLang ();
	}

	/**
	 * @return Custom extended RAT
	 */
	@Override
	public final RangedAttackType createRangedAttackType ()
	{
		return new RangedAttackTypeLang ();
	}

	/**
	 * @return Custom extended Hero
	 */
	@Override
	public final Hero createHero ()
	{
		return new HeroLang ();
	}

	/**
	 * @return Custom extended CitySize
	 */
	@Override
	public final CitySize createCitySize ()
	{
		return new CitySizeLang ();
	}

	/**
	 * @return Custom extended SpellRank
	 */
	@Override
	public final SpellRank createSpellRank ()
	{
		return new SpellRankLang ();
	}

	/**
	 * @return Custom extended HeroItemType
	 */
	@Override
	public final HeroItemType createHeroItemType ()
	{
		return new HeroItemTypeLang ();
	}

	/**
	 * @return Custom extended HeroItemSlotType
	 */
	@Override
	public final HeroItemSlotType createHeroItemSlotType ()
	{
		return new HeroItemSlotTypeLang ();
	}

	/**
	 * @return Custom extended HeroItemBonus
	 */
	@Override
	public final HeroItemBonus createHeroItemBonus ()
	{
		return new HeroItemBonusLang ();
	}

	/**
	 * @return Custom extended OverlandMapSize
	 */
	@Override
	public final OverlandMapSize createOverlandMapSize ()
	{
		return new OverlandMapSizeLang ();
	}

	/**
	 * @return Custom extended LandProportion
	 */
	@Override
	public final LandProportion createLandProportion ()
	{
		return new LandProportionLang ();
	}

	/**
	 * @return Custom extended NodeStrength
	 */
	@Override
	public final NodeStrength createNodeStrength ()
	{
		return new NodeStrengthLang ();
	}

	/**
	 * @return Custom extended DifficultyLevel
	 */
	@Override
	public final DifficultyLevel createDifficultyLevel ()
	{
		return new DifficultyLevelLang ();
	}

	/**
	 * @return Custom extended UnitSetting
	 */
	@Override
	public final UnitSetting createUnitSetting ()
	{
		return new UnitSettingLang ();
	}

	/**
	 * @return Custom extended SpellSetting
	 */
	@Override
	public final SpellSetting createSpellSetting ()
	{
		return new SpellSettingLang ();
	}

	/**
	 * @return Custom extended FogOfWarSetting
	 */
	@Override
	public final FogOfWarSetting createFogOfWarSetting ()
	{
		return new FogOfWarSettingLang ();
	}

	/**
	 * @return Custom extended DamageType
	 */
	@Override
	public final DamageType createDamageType ()
	{
		return new DamageTypeLang ();
	}
}
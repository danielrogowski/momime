package momime.client.language.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.language.database.v0_9_6.Building;
import momime.client.language.database.v0_9_6.CitySpellEffect;
import momime.client.language.database.v0_9_6.CombatAreaEffect;
import momime.client.language.database.v0_9_6.KnownServer;
import momime.client.language.database.v0_9_6.LanguageCategory;
import momime.client.language.database.v0_9_6.LanguageDatabase;
import momime.client.language.database.v0_9_6.MapFeature;
import momime.client.language.database.v0_9_6.ObjectFactory;
import momime.client.language.database.v0_9_6.Pick;
import momime.client.language.database.v0_9_6.PickType;
import momime.client.language.database.v0_9_6.Plane;
import momime.client.language.database.v0_9_6.PopulationTask;
import momime.client.language.database.v0_9_6.ProductionType;
import momime.client.language.database.v0_9_6.Race;
import momime.client.language.database.v0_9_6.ShortcutKey;
import momime.client.language.database.v0_9_6.Spell;
import momime.client.language.database.v0_9_6.SpellBookSection;
import momime.client.language.database.v0_9_6.TileType;
import momime.client.language.database.v0_9_6.Unit;
import momime.client.language.database.v0_9_6.UnitAttribute;
import momime.client.language.database.v0_9_6.UnitSkill;
import momime.client.language.database.v0_9_6.UnitType;


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
	 * @return Custom extended UnitAttribute
	 */
	@Override
	public final UnitAttribute createUnitAttribute ()
	{
		return new UnitAttributeLang ();
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
}
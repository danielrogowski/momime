package momime.client.language.database;

import java.util.List;

import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.KnownServer;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.language.database.v0_9_5.Plane;
import momime.client.language.database.v0_9_5.PopulationTask;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.Race;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.language.database.v0_9_5.SpellBookSection;
import momime.client.language.database.v0_9_5.TileType;
import momime.client.language.database.v0_9_5.Unit;
import momime.client.language.database.v0_9_5.UnitAttribute;
import momime.client.language.database.v0_9_5.UnitSkill;
import momime.common.database.SpellBookSectionID;

/**
 * Describes operations that we need to support over the language XML file
 */
public interface LanguageDatabaseEx
{
	/**
	 * @param planeNumber Plane number to search for
	 * @return Plane descriptions object; or null if not found
	 */
	public Plane findPlane (final int planeNumber);

	/**
	 * @param productionTypeID Production type ID to search for
	 * @return Production type descriptions object; or null if not found
	 */
	public ProductionType findProductionType (final String productionTypeID);

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @return Map feature descriptions object; or null if not found
	 */
	public MapFeature findMapFeature (final String mapFeatureID);

	/**
	 * @param tileTypeID Tile type ID to search for
	 * @return Tile type descriptions object; or null if not found
	 */
	public TileType findTileType (final String tileTypeID);

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @return Pick type description; or replays back the ID if no description exists
	 */
	public String findPickTypeDescription (final String pickTypeID);
	
	/**
	 * @param pickID Pick ID to search for
	 * @return Pick descriptions object; or null if not found
	 */
	public Pick findPick (final String pickID);
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @return Wizard name; or replays back the ID if no name exists
	 */
	public String findWizardName (final String wizardID);

	/**
	 * @param populationTaskID Population task ID to search for
	 * @return Population task descriptions object; or null if not found
	 */
	public PopulationTask findPopulationTask (final String populationTaskID);
	
	/**
	 * @param raceID Race ID to search for
	 * @return Race descriptions object; or null if not found
	 */
	public Race findRace (final String raceID);

	/**
	 * @param buildingID Building ID to search for
	 * @return Building descriptions object; or null if not found
	 */
	public Building findBuilding (final String buildingID);
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @return Unit type descriptions object; or null if not found
	 */
	public UnitTypeEx findUnitType (final String unitTypeID);
	
	/**
	 * @param unitAttributeID Unit attribute ID to search for
	 * @return Unit attribute descriptions object; or null if not found
	 */
	public UnitAttribute findUnitAttribute (final String unitAttributeID);

	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @return Unit skill descriptions object; or null if not found
	 */
	public UnitSkill findUnitSkill (final String unitSkillID);
	
	/**
	 * @param rangedAttackTypeID Ranged attack type ID to search for
	 * @return Ranged attack type description; or replays back the ID if no description exists
	 */
	public String findRangedAttackTypeDescription (final String rangedAttackTypeID);
	
	/**
	 * @param unitID Unit ID to search for
	 * @return Unit descriptions object; or null if not found
	 */
	public Unit findUnit (final String unitID);

	/**
	 * @param heroNameID Hero name ID to search for
	 * @return Hero name; or replays back the ID if no description exists
	 */
	public String findHeroName (final String heroNameID);
	
	/**
	 * @param citySizeID City size ID to search for
	 * @return City size name; or replays back the ID if no description exists
	 */
	public String findCitySizeName (final String citySizeID);
	
	/**
	 * @param spellRankID Spell rank ID to search for
	 * @return Spell rank description; or replays back the ID if no description exists
	 */
	public String findSpellRankDescription (final String spellRankID);
	
	/**
	 * @param spellBookSectionID Spell book section ID to search for
	 * @return Spell book section descriptions object; or null if not found
	 */
	public SpellBookSection findSpellBookSection (final SpellBookSectionID spellBookSectionID);
	
	/**
	 * @param spellID Spell ID to search for
	 * @return Spell descriptions object; or null if not found
	 */
	public Spell findSpell (final String spellID);
	
	/**
	 * @param mapSizeID Map size ID to search for
	 * @return Map size description; or replays back the ID if no description exists
	 */
	public String findMapSizeDescription (final String mapSizeID);
	
	/**
	 * @param landProportionID Land proportion ID to search for
	 * @return Land proportion description; or replays back the ID if no description exists
	 */
	public String findLandProportionDescription (final String landProportionID);
	
	/**
	 * @param nodeStrengthID Node strength ID to search for
	 * @return Node strength description; or replays back the ID if no description exists
	 */
	public String findNodeStrengthDescription (final String nodeStrengthID);
	
	/**
	 * @param difficultyLevelID Difficulty level ID to search for
	 * @return Difficulty level description; or replays back the ID if no description exists
	 */
	public String findDifficultyLevelDescription (final String difficultyLevelID);
	
	/**
	 * @param fogOfWarSettingID Fog of War setting ID to search for
	 * @return Fog of War setting description; or replays back the ID if no description exists
	 */
	public String findFogOfWarSettingDescription (final String fogOfWarSettingID);
	
	/**
	 * @param unitSettingID Unit setting ID to search for
	 * @return Unit setting description; or replays back the ID if no description exists
	 */
	public String findUnitSettingDescription (final String unitSettingID);
	
	/**
	 * @param spellSettingID Spell setting ID to search for
	 * @return Spell setting description; or replays back the ID if no description exists
	 */
	public String findSpellSettingDescription (final String spellSettingID);
	
	/**
	 * @param languageCategoryID Category ID to search for
	 * @param languageEntryID Entry ID to search for
	 * @return Text of the requested language entry; or replays the key back if the category or entry doesn't exist
	 */
	public String findCategoryEntry (final String languageCategoryID, final String languageEntryID);
	
	/**
	 * @return List of all known servers
	 */
	public List<KnownServer> getKnownServer ();
}
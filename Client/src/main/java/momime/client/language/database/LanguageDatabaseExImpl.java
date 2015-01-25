package momime.client.language.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.CitySize;
import momime.client.language.database.v0_9_5.DifficultyLevel;
import momime.client.language.database.v0_9_5.FogOfWarSetting;
import momime.client.language.database.v0_9_5.Hero;
import momime.client.language.database.v0_9_5.LandProportion;
import momime.client.language.database.v0_9_5.LanguageCategory;
import momime.client.language.database.v0_9_5.LanguageDatabase;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.MapSize;
import momime.client.language.database.v0_9_5.NodeStrength;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.language.database.v0_9_5.PickType;
import momime.client.language.database.v0_9_5.Plane;
import momime.client.language.database.v0_9_5.PopulationTask;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.Race;
import momime.client.language.database.v0_9_5.RangedAttackType;
import momime.client.language.database.v0_9_5.Shortcut;
import momime.client.language.database.v0_9_5.ShortcutKey;
import momime.client.language.database.v0_9_5.Spell;
import momime.client.language.database.v0_9_5.SpellBookSection;
import momime.client.language.database.v0_9_5.SpellRank;
import momime.client.language.database.v0_9_5.SpellSetting;
import momime.client.language.database.v0_9_5.TileType;
import momime.client.language.database.v0_9_5.Unit;
import momime.client.language.database.v0_9_5.UnitAttribute;
import momime.client.language.database.v0_9_5.UnitSetting;
import momime.client.language.database.v0_9_5.UnitSkill;
import momime.client.language.database.v0_9_5.UnitType;
import momime.client.language.database.v0_9_5.Wizard;
import momime.common.database.SpellBookSectionID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of language XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class LanguageDatabaseExImpl extends LanguageDatabase implements LanguageDatabaseEx
{
	/** Class logger */
	private final Log log = LogFactory.getLog (LanguageDatabaseExImpl.class);
	
	/** Map of plane IDs to plane objects */
	private Map<Integer, Plane> planesMap;

	/** Map of production type IDs to production type objects */
	private Map<String, ProductionType> productionTypesMap;

	/** Map of map feature IDs to map feature objects */
	private Map<String, MapFeature> mapFeaturesMap;

	/** Map of tile type IDs to tile type objects */
	private Map<String, TileType> tileTypesMap;
	
	/** Map of pick type IDs to pick type objects */
	private Map<String, PickType> pickTypesMap;
	
	/** Map of pick IDs to pick objects */
	private Map<String, Pick> picksMap;

	/** Map of wizard IDs to wizard objects */
	private Map<String, Wizard> wizardsMap;

	/** Map of population task IDs to population task objects */
	private Map<String, PopulationTask> populationTasksMap;
	
	/** Map of race IDs to race objects */
	private Map<String, Race> racesMap;
	
	/** Map of building IDs to building objects */
	private Map<String, Building> buildingsMap;

	/** Map of unit type IDs to unit type objects */
	private Map<String, UnitTypeEx> unitTypesMap;
	
	/** Map of unit attribute IDs to unit attribute objects */
	private Map<String, UnitAttribute> unitAttributesMap;
	
	/** Map of unit skill IDs to unit skill objects */
	private Map<String, UnitSkill> unitSkillsMap;
	
	/** Map of ranged attack type IDs to unit skill objects */
	private Map<String, RangedAttackType> rangedAttackTypesMap;
	
	/** Map of unit IDs to unit objects */
	private Map<String, Unit> unitsMap;

	/** Map of hero name IDs to hero name objects */
	private Map<String, Hero> heroNamesMap;
	
	/** Map of city size IDs to city size objects */
	private Map<String, CitySize> citySizesMap;
	
	/** Map of spell rank IDs to spell rank objects */
	private Map<String, SpellRank> spellRanksMap;
	
	/** Map of spell book section IDs to spell book section objects */
	private Map<SpellBookSectionID, SpellBookSection> spellBookSectionsMap;

	/** Map of spell IDs to spell objects */
	private Map<String, Spell> spellsMap;
	
	/** Map of map size IDs to map size objects */
	private Map<String, MapSize> mapSizesMap;
	
	/** Map of land proportion IDs to land proportion objects */
	private Map<String, LandProportion> landProportionsMap;
	
	/** Map of node strength IDs to node strength objects */
	private Map<String, NodeStrength> nodeStrengthsMap;
	
	/** Map of difficulty level IDs to difficulty level objects */
	private Map<String, DifficultyLevel> difficultyLevelsMap;
	
	/** Map of fog of war setting IDs to fog of war settings objects */
	private Map<String, FogOfWarSetting> fogOfWarSettingsMap;
	
	/** Map of unit setting IDs to unit settings objects */
	private Map<String, UnitSetting> unitSettingsMap;
	
	/** Map of spell setting IDs to spell settings objects */
	private Map<String, SpellSetting> spellSettingsMap;

	/** Map of category IDs to category objects */
	private Map<String, LanguageCategoryEx> categoriesMap;
	
	/** Map of shortcuts to shortcut keys objects */
	private Map<Shortcut, ShortcutKey> shortcutsMap;

	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		log.trace ("Entering buildMaps");
		
		// Create planes map
		planesMap = new HashMap<Integer, Plane> ();
		for (final Plane thisPlane : getPlane ())
			planesMap.put (thisPlane.getPlaneNumber (), thisPlane);

		// Create production types map
		productionTypesMap = new HashMap<String, ProductionType> ();
		for (final ProductionType thisProductionType : getProductionType ())
			productionTypesMap.put (thisProductionType.getProductionTypeID (), thisProductionType);

		// Create map features map
		mapFeaturesMap = new HashMap<String, MapFeature> ();
		for (final MapFeature thisMapFeature : getMapFeature ())
			mapFeaturesMap.put (thisMapFeature.getMapFeatureID (), thisMapFeature);
		
		// Create tile types map
		tileTypesMap = new HashMap<String, TileType> ();
		for (final TileType thisTileType : getTileType ())
			tileTypesMap.put (thisTileType.getTileTypeID (), thisTileType);
		
		// Create pick types map
		pickTypesMap = new HashMap<String, PickType> ();
		for (final PickType thisPickType : getPickType ())
			pickTypesMap.put (thisPickType.getPickTypeID (), thisPickType);
		
		// Create picks map
		picksMap = new HashMap<String, Pick> ();
		for (final Pick thisPick : getPick ())
			picksMap.put (thisPick.getPickID (), thisPick);

		// Create wizards map
		wizardsMap = new HashMap<String, Wizard> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), thisWizard);

		// Create population tasks map
		populationTasksMap = new HashMap<String, PopulationTask> ();
		for (final PopulationTask thisPopulationTask : getPopulationTask ())
			populationTasksMap.put (thisPopulationTask.getPopulationTaskID (), thisPopulationTask);
		
		// Create races map
		racesMap = new HashMap<String, Race> ();
		for (final Race thisRace : getRace ())
			racesMap.put (thisRace.getRaceID (), thisRace);
		
		// Create buildings map
		buildingsMap = new HashMap<String, Building> ();
		for (final Building thisBuilding : getBuilding ())
			buildingsMap.put (thisBuilding.getBuildingID (), thisBuilding);

		// Create unit types map
		unitTypesMap = new HashMap<String, UnitTypeEx> ();
		for (final UnitType thisUnitType : getUnitType ())
		{
			final UnitTypeEx utex = (UnitTypeEx) thisUnitType;
			utex.buildMap ();
			unitTypesMap.put (utex.getUnitTypeID (), utex);
		}
		
		// Create unit attributes map
		unitAttributesMap = new HashMap<String, UnitAttribute> ();
		for (final UnitAttribute thisUnitAttribute : getUnitAttribute ())
			unitAttributesMap.put (thisUnitAttribute.getUnitAttributeID (), thisUnitAttribute);

		// Create unit skills map
		unitSkillsMap = new HashMap<String, UnitSkill> ();
		for (final UnitSkill thisUnitSkill : getUnitSkill ())
			unitSkillsMap.put (thisUnitSkill.getUnitSkillID (), thisUnitSkill);

		// Create ranged attack types map
		rangedAttackTypesMap = new HashMap<String, RangedAttackType> ();
		for (final RangedAttackType thisRangedAttackType : getRangedAttackType ())
			rangedAttackTypesMap.put (thisRangedAttackType.getRangedAttackTypeID (), thisRangedAttackType);
		
		// Create units map
		unitsMap = new HashMap<String, Unit> ();
		for (final Unit thisUnit : getUnit ())
			unitsMap.put (thisUnit.getUnitID (), thisUnit);
		
		// Create hero names map
		heroNamesMap = new HashMap<String, Hero> ();
		for (final Hero thisHeroName : getHero ())
			heroNamesMap.put (thisHeroName.getHeroNameID (), thisHeroName);
		
		// Create city sizes map
		citySizesMap = new HashMap<String, CitySize> ();
		for (final CitySize thisCitySize : getCitySize ())
			citySizesMap.put (thisCitySize.getCitySizeID (), thisCitySize);
		
		// Create spell ranks map
		spellRanksMap = new HashMap<String, SpellRank> ();
		for (final SpellRank thisSpellRank : getSpellRank ())
			spellRanksMap.put (thisSpellRank.getSpellRankID (), thisSpellRank);

		// Create spell book sections map
		spellBookSectionsMap = new HashMap<SpellBookSectionID, SpellBookSection> ();
		for (final SpellBookSection thisSection : getSpellBookSection ())
			spellBookSectionsMap.put (thisSection.getSpellBookSectionID (), thisSection);
		
		// Create spells map
		spellsMap = new HashMap<String, Spell> ();
		for (final Spell thisSpell : getSpell ())
			spellsMap.put (thisSpell.getSpellID (), thisSpell);
		
		// Create map sizes map
		mapSizesMap = new HashMap<String, MapSize> ();
		for (final MapSize thisMapSize : getMapSize ())
			mapSizesMap.put (thisMapSize.getMapSizeID (), thisMapSize);
		
		// Create land proportions map
		landProportionsMap = new HashMap<String, LandProportion> ();
		for (final LandProportion thisLandProportion : getLandProportion ())
			landProportionsMap.put (thisLandProportion.getLandProportionID (), thisLandProportion);

		// Create node strengths map
		nodeStrengthsMap = new HashMap<String, NodeStrength> ();
		for (final NodeStrength thisNodeStrength : getNodeStrength ())
			nodeStrengthsMap.put (thisNodeStrength.getNodeStrengthID (), thisNodeStrength);
		
		// Create difficulty levels map
		difficultyLevelsMap = new HashMap<String, DifficultyLevel> ();
		for (final DifficultyLevel thisDifficultyLevel : getDifficultyLevel ())
			difficultyLevelsMap.put (thisDifficultyLevel.getDifficultyLevelID (), thisDifficultyLevel);

		// Create fog of war settings map
		fogOfWarSettingsMap = new HashMap<String, FogOfWarSetting> ();
		for (final FogOfWarSetting thisFogOfWarSetting : getFogOfWarSetting ())
			fogOfWarSettingsMap.put (thisFogOfWarSetting.getFogOfWarSettingID (), thisFogOfWarSetting);
		
		// Create unit settings map
		unitSettingsMap = new HashMap<String, UnitSetting> ();
		for (final UnitSetting thisUnitSetting : getUnitSetting ())
			unitSettingsMap.put (thisUnitSetting.getUnitSettingID (), thisUnitSetting);
		
		// Create spell settings map
		spellSettingsMap = new HashMap<String, SpellSetting> ();
		for (final SpellSetting thisSpellSetting : getSpellSetting ())
			spellSettingsMap.put (thisSpellSetting.getSpellSettingID (), thisSpellSetting);
		
		// Create categories map
		categoriesMap = new HashMap<String, LanguageCategoryEx> ();
		for (final LanguageCategory thisCategory : getLanguageCategory ())
		{
			final LanguageCategoryEx catEx = (LanguageCategoryEx) thisCategory;
			catEx.buildMap ();
			categoriesMap.put (thisCategory.getLanguageCategoryID (), catEx);
		}
		
		// Create shortcuts map
		shortcutsMap = new HashMap<Shortcut, ShortcutKey> ();
		for (final ShortcutKey thisShortcut : getShortcutKey ())
			shortcutsMap.put (thisShortcut.getShortcut (), thisShortcut);

		log.trace ("Exiting buildMaps");
	}	

	/**
	 * @param planeNumber Plane number to search for
	 * @return Plane descriptions object; or null if not found
	 */
	@Override
	public final Plane findPlane (final int planeNumber)
	{
		return planesMap.get (planeNumber);
	}

	/**
	 * @param productionTypeID Production type ID to search for
	 * @return Production type descriptions object; or null if not found
	 */
	@Override
	public final ProductionType findProductionType (final String productionTypeID)
	{
		return productionTypesMap.get (productionTypeID);
	}

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @return Map feature descriptions object; or null if not found
	 */
	@Override
	public final MapFeature findMapFeature (final String mapFeatureID)
	{
		return mapFeaturesMap.get (mapFeatureID);
	}

	/**
	 * @param tileTypeID Tile type ID to search for
	 * @return Tile type descriptions object; or null if not found
	 */
	@Override
	public final TileType findTileType (final String tileTypeID)
	{
		return tileTypesMap.get (tileTypeID);
	}

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @return Pick type description; or replays back the ID if no description exists
	 */
	@Override
	public final String findPickTypeDescription (final String pickTypeID)
	{
		final PickType thisPickType = pickTypesMap.get (pickTypeID);
		final String desc = (thisPickType != null) ? thisPickType.getPickTypeDescription () : null;
		return (desc != null) ? desc : pickTypeID;
	}
	
	/**
	 * @param pickID Pick ID to search for
	 * @return Pick descriptions object; or null if not found
	 */
	@Override
	public final Pick findPick (final String pickID)
	{
		return picksMap.get (pickID);
	}
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @return Wizard name; or replays back the ID if no name exists
	 */
	@Override
	public final String findWizardName (final String wizardID)
	{
		final Wizard thisWizard = wizardsMap.get (wizardID);
		final String wizardName = (thisWizard != null) ? thisWizard.getWizardName () : null;
		return (wizardName != null) ? wizardName : wizardID;
	}

	/**
	 * @param populationTaskID Population task ID to search for
	 * @return Population task descriptions object; or null if not found
	 */
	@Override
	public final PopulationTask findPopulationTask (final String populationTaskID)
	{
		return populationTasksMap.get (populationTaskID);
	}
	
	/**
	 * @param raceID Race ID to search for
	 * @return Race descriptions object; or null if not found
	 */
	@Override
	public final Race findRace (final String raceID)
	{
		return racesMap.get (raceID);
	}
	
	/**
	 * @param buildingID Building ID to search for
	 * @return Building descriptions object; or null if not found
	 */
	@Override
	public final Building findBuilding (final String buildingID)
	{
		return buildingsMap.get (buildingID);
	}
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @return Unit type descriptions object; or null if not found
	 */
	@Override
	public final UnitTypeEx findUnitType (final String unitTypeID)
	{
		return unitTypesMap.get (unitTypeID);
	}
	
	/**
	 * @param unitAttributeID Unit attribute ID to search for
	 * @return Unit attribute descriptions object; or null if not found
	 */
	@Override
	public final UnitAttribute findUnitAttribute (final String unitAttributeID)
	{
		return unitAttributesMap.get (unitAttributeID);
	}
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @return Unit skill descriptions object; or null if not found
	 */
	@Override
	public final UnitSkill findUnitSkill (final String unitSkillID)
	{
		return unitSkillsMap.get (unitSkillID);
	}

	/**
	 * @param rangedAttackTypeID Ranged attack type ID to search for
	 * @return Ranged attack type description; or replays back the ID if no description exists
	 */
	@Override
	public final String findRangedAttackTypeDescription (final String rangedAttackTypeID)
	{
		final RangedAttackType rat = rangedAttackTypesMap.get (rangedAttackTypeID);
		final String desc = (rat != null) ? rat.getRangedAttackTypeDescription () : null; 
		return (desc != null) ? desc : rangedAttackTypeID;
	}
	
	/**
	 * @param unitID Unit ID to search for
	 * @return Unit descriptions object; or null if not found
	 */
	@Override
	public final Unit findUnit (final String unitID)
	{
		return unitsMap.get (unitID);
	}
	
	/**
	 * @param heroNameID Hero name ID to search for
	 * @return Hero name; or replays back the ID if no description exists
	 */
	@Override
	public final String findHeroName (final String heroNameID)
	{
		final Hero thisHeroName = heroNamesMap.get (heroNameID);
		final String hn = (thisHeroName != null) ? thisHeroName.getHeroName () : null;
		return (hn != null) ? hn : heroNameID;
	}
	
	/**
	 * @param citySizeID City size ID to search for
	 * @return City size name; or replays back the ID if no description exists
	 */
	@Override
	public final String findCitySizeName (final String citySizeID)
	{
		final CitySize thisCitySize = citySizesMap.get (citySizeID);
		final String citySizeName = (thisCitySize != null) ? thisCitySize.getCitySizeName () : null; 
		return (citySizeName != null) ? citySizeName : citySizeID;
	}
	
	/**
	 * @param spellRankID Spell rank ID to search for
	 * @return Spell rank description; or replays back the ID if no description exists
	 */
	@Override
	public final String findSpellRankDescription (final String spellRankID)
	{
		final SpellRank thisSpellRank = spellRanksMap.get (spellRankID);
		final String desc = (thisSpellRank != null) ? thisSpellRank.getSpellRankDescription () : null; 
		return (desc != null) ? desc : spellRankID;
	}

	/**
	 * @param spellBookSectionID Spell book section ID to search for
	 * @return Spell book section descriptions object; or null if not found
	 */
	@Override
	public final SpellBookSection findSpellBookSection (final SpellBookSectionID spellBookSectionID)
	{
		return spellBookSectionsMap.get (spellBookSectionID);
	}
	
	/**
	 * @param spellID Spell ID to search for
	 * @return Spell descriptions object; or null if not found
	 */
	@Override
	public final Spell findSpell (final String spellID)
	{
		return spellsMap.get (spellID);
	}
	
	/**
	 * @param mapSizeID Map size ID to search for
	 * @return Map size description; or replays back the ID if no description exists
	 */
	@Override
	public final String findMapSizeDescription (final String mapSizeID)
	{
		final MapSize thisMapSize = mapSizesMap.get (mapSizeID);
		final String desc = (thisMapSize != null) ? thisMapSize.getMapSizeDescription () : null;
		return (desc != null) ? desc : mapSizeID;
	}
	
	/**
	 * @param landProportionID Land proportion ID to search for
	 * @return Land proportion description; or replays back the ID if no description exists
	 */
	@Override
	public final String findLandProportionDescription (final String landProportionID)
	{
		final LandProportion thisLandProportion = landProportionsMap.get (landProportionID);
		final String desc = (thisLandProportion != null) ? thisLandProportion.getLandProportionDescription () : null;
		return (desc != null) ? desc : landProportionID;
	}
	
	/**
	 * @param nodeStrengthID Node strength ID to search for
	 * @return Node strength description; or replays back the ID if no description exists
	 */
	@Override
	public final String findNodeStrengthDescription (final String nodeStrengthID)
	{
		final NodeStrength thisNodeStrength = nodeStrengthsMap.get (nodeStrengthID);
		final String desc = (thisNodeStrength != null) ? thisNodeStrength.getNodeStrengthDescription () : null;
		return (desc != null) ? desc : nodeStrengthID;
	}
	
	/**
	 * @param difficultyLevelID Difficulty level ID to search for
	 * @return Difficulty level description; or replays back the ID if no description exists
	 */
	@Override
	public final String findDifficultyLevelDescription (final String difficultyLevelID)
	{
		final DifficultyLevel thisDifficultyLevel = difficultyLevelsMap.get (difficultyLevelID);
		final String desc = (thisDifficultyLevel != null) ? thisDifficultyLevel.getDifficultyLevelDescription () : null;
		return (desc != null) ? desc : difficultyLevelID;
	}
	
	/**
	 * @param fogOfWarSettingID Fog of War setting ID to search for
	 * @return Fog of War setting description; or replays back the ID if no description exists
	 */
	@Override
	public final String findFogOfWarSettingDescription (final String fogOfWarSettingID)
	{
		final FogOfWarSetting thisFogOfWarSetting = fogOfWarSettingsMap.get (fogOfWarSettingID);
		final String desc = (thisFogOfWarSetting != null) ? thisFogOfWarSetting.getFogOfWarSettingDescription () : null;
		return (desc != null) ? desc : fogOfWarSettingID;
	}
	
	/**
	 * @param unitSettingID Unit setting ID to search for
	 * @return Unit setting description; or replays back the ID if no description exists
	 */
	@Override
	public final String findUnitSettingDescription (final String unitSettingID)
	{
		final UnitSetting thisUnitSetting = unitSettingsMap.get (unitSettingID);
		final String desc = (thisUnitSetting != null) ? thisUnitSetting.getUnitSettingDescription () : null;
		return (desc != null) ? desc : unitSettingID;
	}
	
	/**
	 * @param spellSettingID Spell setting ID to search for
	 * @return Spell setting description; or replays back the ID if no description exists
	 */
	@Override
	public final String findSpellSettingDescription (final String spellSettingID)
	{
		final SpellSetting thisSpellSetting = spellSettingsMap.get (spellSettingID);
		final String desc = (thisSpellSetting != null) ? thisSpellSetting.getSpellSettingDescription () : null;
		return (desc != null) ? desc : spellSettingID;
	}
	
	/**
	 * @param languageCategoryID Category ID to search for
	 * @param languageEntryID Entry ID to search for
	 * @return Text of the requested language entry; or replays the key back if the category or entry doesn't exist
	 */
	@Override
	public final String findCategoryEntry (final String languageCategoryID, final String languageEntryID)
	{
		final LanguageCategoryEx cat = categoriesMap.get (languageCategoryID);
		final String entry = (cat == null) ? null : cat.findEntry (languageEntryID);
		return (entry == null) ? (languageCategoryID + "/" + languageEntryID) : entry;
	}

	/**
	 * @param shortcut Game shortcut that we're looking to see if there is a key defined for it
	 * @return Details of the key that should activate this shortcut, or null if none is defined
	 */
	@Override
	public final ShortcutKey findShortcutKey (final Shortcut shortcut)
	{
		return shortcutsMap.get (shortcut);
	}
}
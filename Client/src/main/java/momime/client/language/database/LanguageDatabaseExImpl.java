package momime.client.language.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.language.database.v0_9_7.Building;
import momime.client.language.database.v0_9_7.CitySize;
import momime.client.language.database.v0_9_7.CitySpellEffect;
import momime.client.language.database.v0_9_7.CombatAreaEffect;
import momime.client.language.database.v0_9_7.DifficultyLevel;
import momime.client.language.database.v0_9_7.FogOfWarSetting;
import momime.client.language.database.v0_9_7.Hero;
import momime.client.language.database.v0_9_7.HeroItemBonus;
import momime.client.language.database.v0_9_7.HeroItemSlotType;
import momime.client.language.database.v0_9_7.HeroItemType;
import momime.client.language.database.v0_9_7.LandProportion;
import momime.client.language.database.v0_9_7.LanguageCategory;
import momime.client.language.database.v0_9_7.LanguageDatabase;
import momime.client.language.database.v0_9_7.MapFeature;
import momime.client.language.database.v0_9_7.NodeStrength;
import momime.client.language.database.v0_9_7.OverlandMapSize;
import momime.client.language.database.v0_9_7.Pick;
import momime.client.language.database.v0_9_7.PickType;
import momime.client.language.database.v0_9_7.Plane;
import momime.client.language.database.v0_9_7.PopulationTask;
import momime.client.language.database.v0_9_7.ProductionType;
import momime.client.language.database.v0_9_7.Race;
import momime.client.language.database.v0_9_7.RangedAttackType;
import momime.client.language.database.v0_9_7.ShortcutKey;
import momime.client.language.database.v0_9_7.Spell;
import momime.client.language.database.v0_9_7.SpellBookSection;
import momime.client.language.database.v0_9_7.SpellRank;
import momime.client.language.database.v0_9_7.SpellSetting;
import momime.client.language.database.v0_9_7.TileType;
import momime.client.language.database.v0_9_7.Unit;
import momime.client.language.database.v0_9_7.UnitSetting;
import momime.client.language.database.v0_9_7.UnitSkill;
import momime.client.language.database.v0_9_7.UnitType;
import momime.client.language.database.v0_9_7.Wizard;
import momime.common.database.Shortcut;
import momime.common.database.SpellBookSectionID;

/**
 * Implementation of language XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class LanguageDatabaseExImpl extends LanguageDatabase implements LanguageDatabaseEx
{
	/** Class logger */
	private final Log log = LogFactory.getLog (LanguageDatabaseExImpl.class);
	
	/** Map of plane IDs to plane objects */
	private Map<Integer, PlaneLang> planesMap;

	/** Map of production type IDs to production type objects */
	private Map<String, ProductionTypeLang> productionTypesMap;

	/** Map of map feature IDs to map feature objects */
	private Map<String, MapFeatureLang> mapFeaturesMap;

	/** Map of tile type IDs to tile type objects */
	private Map<String, TileTypeLang> tileTypesMap;
	
	/** Map of pick type IDs to pick type objects */
	private Map<String, PickTypeLang> pickTypesMap;
	
	/** Map of pick IDs to pick objects */
	private Map<String, PickLang> picksMap;

	/** Map of wizard IDs to wizard objects */
	private Map<String, WizardLang> wizardsMap;

	/** Map of population task IDs to population task objects */
	private Map<String, PopulationTaskLang> populationTasksMap;
	
	/** Map of race IDs to race objects */
	private Map<String, RaceLang> racesMap;
	
	/** Map of building IDs to building objects */
	private Map<String, BuildingLang> buildingsMap;

	/** Map of unit type IDs to unit type objects */
	private Map<String, UnitTypeLang> unitTypesMap;
	
	/** Map of unit skill IDs to unit skill objects */
	private Map<String, UnitSkillLang> unitSkillsMap;
	
	/** Map of ranged attack type IDs to unit skill objects */
	private Map<String, RangedAttackTypeLang> rangedAttackTypesMap;
	
	/** Map of unit IDs to unit objects */
	private Map<String, UnitLang> unitsMap;

	/** Map of hero name IDs to hero name objects */
	private Map<String, HeroLang> heroNamesMap;
	
	/** Map of city size IDs to city size objects */
	private Map<String, CitySizeLang> citySizesMap;

	/** Map of city spell effect IDs to unit objects */
	private Map<String, CitySpellEffectLang> citySpellEffectsMap;
	
	/** Map of combat area effect IDs to unit objects */
	private Map<String, CombatAreaEffectLang> combatAreaEffectsMap;

	/** Map of spell rank IDs to spell rank objects */
	private Map<String, SpellRankLang> spellRanksMap;
	
	/** Map of spell book section IDs to spell book section objects */
	private Map<SpellBookSectionID, SpellBookSectionLang> spellBookSectionsMap;

	/** Map of spell IDs to spell objects */
	private Map<String, SpellLang> spellsMap;
	
	/** Map of hero item type IDs to hero item type objects */
	private Map<String, HeroItemTypeLang> heroItemTypesMap;
	
	/** Map of hero item slot type IDs to hero item slot type objects */
	private Map<String, HeroItemSlotTypeLang> heroItemSlotTypesMap;
	
	/** Map of hero item bonus IDs to hero item bonus objects */
	private Map<String, HeroItemBonusLang> heroItemBonusesMap;
	
	/** Map of overland map size IDs to overland map size objects */
	private Map<String, OverlandMapSizeLang> overlandMapSizesMap;
	
	/** Map of land proportion IDs to land proportion objects */
	private Map<String, LandProportionLang> landProportionsMap;
	
	/** Map of node strength IDs to node strength objects */
	private Map<String, NodeStrengthLang> nodeStrengthsMap;
	
	/** Map of difficulty level IDs to difficulty level objects */
	private Map<String, DifficultyLevelLang> difficultyLevelsMap;
	
	/** Map of fog of war setting IDs to fog of war settings objects */
	private Map<String, FogOfWarSettingLang> fogOfWarSettingsMap;
	
	/** Map of unit setting IDs to unit settings objects */
	private Map<String, UnitSettingLang> unitSettingsMap;
	
	/** Map of spell setting IDs to spell settings objects */
	private Map<String, SpellSettingLang> spellSettingsMap;

	/** Map of category IDs to category objects */
	private Map<String, LanguageCategoryEx> categoriesMap;
	
	/** Map of shortcuts to shortcut keys objects */
	private Map<Shortcut, ShortcutKeyLang> shortcutsMap;

	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		log.trace ("Entering buildMaps");
		
		// Create planes map
		planesMap = new HashMap<Integer, PlaneLang> ();
		for (final Plane thisPlane : getPlane ())
			planesMap.put (thisPlane.getPlaneNumber (), (PlaneLang) thisPlane);

		// Create production types map
		productionTypesMap = new HashMap<String, ProductionTypeLang> ();
		for (final ProductionType thisProductionType : getProductionType ())
			productionTypesMap.put (thisProductionType.getProductionTypeID (), (ProductionTypeLang) thisProductionType);

		// Create map features map
		mapFeaturesMap = new HashMap<String, MapFeatureLang> ();
		for (final MapFeature thisMapFeature : getMapFeature ())
			mapFeaturesMap.put (thisMapFeature.getMapFeatureID (), (MapFeatureLang) thisMapFeature);
		
		// Create tile types map
		tileTypesMap = new HashMap<String, TileTypeLang> ();
		for (final TileType thisTileType : getTileType ())
			tileTypesMap.put (thisTileType.getTileTypeID (), (TileTypeLang) thisTileType);
		
		// Create pick types map
		pickTypesMap = new HashMap<String, PickTypeLang> ();
		for (final PickType thisPickType : getPickType ())
			pickTypesMap.put (thisPickType.getPickTypeID (), (PickTypeLang) thisPickType);
		
		// Create picks map
		picksMap = new HashMap<String, PickLang> ();
		for (final Pick thisPick : getPick ())
			picksMap.put (thisPick.getPickID (), (PickLang) thisPick);

		// Create wizards map
		wizardsMap = new HashMap<String, WizardLang> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), (WizardLang) thisWizard);

		// Create population tasks map
		populationTasksMap = new HashMap<String, PopulationTaskLang> ();
		for (final PopulationTask thisPopulationTask : getPopulationTask ())
			populationTasksMap.put (thisPopulationTask.getPopulationTaskID (), (PopulationTaskLang) thisPopulationTask);
		
		// Create races map
		racesMap = new HashMap<String, RaceLang> ();
		for (final Race thisRace : getRace ())
			racesMap.put (thisRace.getRaceID (), (RaceLang) thisRace);
		
		// Create buildings map
		buildingsMap = new HashMap<String, BuildingLang> ();
		for (final Building thisBuilding : getBuilding ())
			buildingsMap.put (thisBuilding.getBuildingID (), (BuildingLang) thisBuilding);

		// Create unit types map
		unitTypesMap = new HashMap<String, UnitTypeLang> ();
		for (final UnitType thisUnitType : getUnitType ())
		{
			final UnitTypeLang utex = (UnitTypeLang) thisUnitType;
			utex.buildMap ();
			unitTypesMap.put (utex.getUnitTypeID (), utex);
		}
		
		// Create unit skills map
		unitSkillsMap = new HashMap<String, UnitSkillLang> ();
		for (final UnitSkill thisUnitSkill : getUnitSkill ())
			unitSkillsMap.put (thisUnitSkill.getUnitSkillID (), (UnitSkillLang) thisUnitSkill);
		
		// Create ranged attack types map
		rangedAttackTypesMap = new HashMap<String, RangedAttackTypeLang> ();
		for (final RangedAttackType thisRangedAttackType : getRangedAttackType ())
			rangedAttackTypesMap.put (thisRangedAttackType.getRangedAttackTypeID (), (RangedAttackTypeLang) thisRangedAttackType);
		
		// Create units map
		unitsMap = new HashMap<String, UnitLang> ();
		for (final Unit thisUnit : getUnit ())
			unitsMap.put (thisUnit.getUnitID (), (UnitLang) thisUnit);
		
		// Create hero names map
		heroNamesMap = new HashMap<String, HeroLang> ();
		for (final Hero thisHeroName : getHero ())
			heroNamesMap.put (thisHeroName.getHeroNameID (), (HeroLang) thisHeroName);
		
		// Create city sizes map
		citySizesMap = new HashMap<String, CitySizeLang> ();
		for (final CitySize thisCitySize : getCitySize ())
			citySizesMap.put (thisCitySize.getCitySizeID (), (CitySizeLang) thisCitySize);

		// Create city spell effects map
		citySpellEffectsMap = new HashMap<String, CitySpellEffectLang> ();
		for (final CitySpellEffect effect : getCitySpellEffect ())
			citySpellEffectsMap.put (effect.getCitySpellEffectID (), (CitySpellEffectLang) effect);
		
		// Create combat area effects map
		combatAreaEffectsMap = new HashMap<String, CombatAreaEffectLang> ();
		for (final CombatAreaEffect cae : getCombatAreaEffect ())
			combatAreaEffectsMap.put (cae.getCombatAreaEffectID (), (CombatAreaEffectLang) cae);
		
		// Create spell ranks map
		spellRanksMap = new HashMap<String, SpellRankLang> ();
		for (final SpellRank thisSpellRank : getSpellRank ())
			spellRanksMap.put (thisSpellRank.getSpellRankID (), (SpellRankLang) thisSpellRank);

		// Create spell book sections map
		spellBookSectionsMap = new HashMap<SpellBookSectionID, SpellBookSectionLang> ();
		for (final SpellBookSection thisSection : getSpellBookSection ())
			spellBookSectionsMap.put (thisSection.getSpellBookSectionID (), (SpellBookSectionLang) thisSection);
		
		// Create spells map
		spellsMap = new HashMap<String, SpellLang> ();
		for (final Spell thisSpell : getSpell ())
			spellsMap.put (thisSpell.getSpellID (), (SpellLang) thisSpell);
		
		// Create hero item types map
		heroItemTypesMap = new HashMap<String, HeroItemTypeLang> ();
		for (final HeroItemType thisHeroItemType : getHeroItemType ())
			heroItemTypesMap.put (thisHeroItemType.getHeroItemTypeID (), (HeroItemTypeLang) thisHeroItemType);
		
		// Create hero item slot types map
		heroItemSlotTypesMap = new HashMap<String, HeroItemSlotTypeLang> ();
		for (final HeroItemSlotType thisHeroItemSlotType : getHeroItemSlotType ())
			heroItemSlotTypesMap.put (thisHeroItemSlotType.getHeroItemSlotTypeID (), (HeroItemSlotTypeLang) thisHeroItemSlotType);
		
		// Create hero item bonuses map
		heroItemBonusesMap = new HashMap<String, HeroItemBonusLang> ();
		for (final HeroItemBonus thisHeroItemBonus : getHeroItemBonus ())
			heroItemBonusesMap.put (thisHeroItemBonus.getHeroItemBonusID (), (HeroItemBonusLang) thisHeroItemBonus);
		
		// Create overland map sizes map
		overlandMapSizesMap = new HashMap<String, OverlandMapSizeLang> ();
		for (final OverlandMapSize thisOverlandMapSize : getOverlandMapSize ())
			overlandMapSizesMap.put (thisOverlandMapSize.getOverlandMapSizeID (), (OverlandMapSizeLang) thisOverlandMapSize);
		
		// Create land proportions map
		landProportionsMap = new HashMap<String, LandProportionLang> ();
		for (final LandProportion thisLandProportion : getLandProportion ())
			landProportionsMap.put (thisLandProportion.getLandProportionID (), (LandProportionLang) thisLandProportion);

		// Create node strengths map
		nodeStrengthsMap = new HashMap<String, NodeStrengthLang> ();
		for (final NodeStrength thisNodeStrength : getNodeStrength ())
			nodeStrengthsMap.put (thisNodeStrength.getNodeStrengthID (), (NodeStrengthLang) thisNodeStrength);
		
		// Create difficulty levels map
		difficultyLevelsMap = new HashMap<String, DifficultyLevelLang> ();
		for (final DifficultyLevel thisDifficultyLevel : getDifficultyLevel ())
			difficultyLevelsMap.put (thisDifficultyLevel.getDifficultyLevelID (), (DifficultyLevelLang) thisDifficultyLevel);

		// Create fog of war settings map
		fogOfWarSettingsMap = new HashMap<String, FogOfWarSettingLang> ();
		for (final FogOfWarSetting thisFogOfWarSetting : getFogOfWarSetting ())
			fogOfWarSettingsMap.put (thisFogOfWarSetting.getFogOfWarSettingID (), (FogOfWarSettingLang) thisFogOfWarSetting);
		
		// Create unit settings map
		unitSettingsMap = new HashMap<String, UnitSettingLang> ();
		for (final UnitSetting thisUnitSetting : getUnitSetting ())
			unitSettingsMap.put (thisUnitSetting.getUnitSettingID (), (UnitSettingLang) thisUnitSetting);
		
		// Create spell settings map
		spellSettingsMap = new HashMap<String, SpellSettingLang> ();
		for (final SpellSetting thisSpellSetting : getSpellSetting ())
			spellSettingsMap.put (thisSpellSetting.getSpellSettingID (), (SpellSettingLang) thisSpellSetting);
		
		// Create categories map
		categoriesMap = new HashMap<String, LanguageCategoryEx> ();
		for (final LanguageCategory thisCategory : getLanguageCategory ())
		{
			final LanguageCategoryEx catEx = (LanguageCategoryEx) thisCategory;
			catEx.buildMap ();
			categoriesMap.put (thisCategory.getLanguageCategoryID (), catEx);
		}
		
		// Create shortcuts map
		shortcutsMap = new HashMap<Shortcut, ShortcutKeyLang> ();
		for (final ShortcutKey thisShortcut : getShortcutKey ())
			shortcutsMap.put (thisShortcut.getShortcut (), (ShortcutKeyLang) thisShortcut);

		log.trace ("Exiting buildMaps");
	}	

	/**
	 * @param planeNumber Plane number to search for
	 * @return Plane descriptions object; or null if not found
	 */
	@Override
	public final PlaneLang findPlane (final int planeNumber)
	{
		return planesMap.get (planeNumber);
	}

	/**
	 * @param productionTypeID Production type ID to search for
	 * @return Production type descriptions object; or null if not found
	 */
	@Override
	public final ProductionTypeLang findProductionType (final String productionTypeID)
	{
		return productionTypesMap.get (productionTypeID);
	}

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @return Map feature descriptions object; or null if not found
	 */
	@Override
	public final MapFeatureLang findMapFeature (final String mapFeatureID)
	{
		return mapFeaturesMap.get (mapFeatureID);
	}

	/**
	 * @param tileTypeID Tile type ID to search for
	 * @return Tile type descriptions object; or null if not found
	 */
	@Override
	public final TileTypeLang findTileType (final String tileTypeID)
	{
		return tileTypesMap.get (tileTypeID);
	}

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @return Pick type descriptions object; or null if not found
	 */
	@Override
	public final PickTypeLang findPickType (final String pickTypeID)
	{
		return pickTypesMap.get (pickTypeID);
	}
	
	/**
	 * @param pickID Pick ID to search for
	 * @return Pick descriptions object; or null if not found
	 */
	@Override
	public final PickLang findPick (final String pickID)
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
		final WizardLang thisWizard = wizardsMap.get (wizardID);
		final String wizardName = (thisWizard != null) ? thisWizard.getWizardName () : null;
		return (wizardName != null) ? wizardName : wizardID;
	}

	/**
	 * @param populationTaskID Population task ID to search for
	 * @return Population task descriptions object; or null if not found
	 */
	@Override
	public final PopulationTaskLang findPopulationTask (final String populationTaskID)
	{
		return populationTasksMap.get (populationTaskID);
	}
	
	/**
	 * @param raceID Race ID to search for
	 * @return Race descriptions object; or null if not found
	 */
	@Override
	public final RaceLang findRace (final String raceID)
	{
		return racesMap.get (raceID);
	}
	
	/**
	 * @param buildingID Building ID to search for
	 * @return Building descriptions object; or null if not found
	 */
	@Override
	public final BuildingLang findBuilding (final String buildingID)
	{
		return buildingsMap.get (buildingID);
	}
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @return Unit type descriptions object; or null if not found
	 */
	@Override
	public final UnitTypeLang findUnitType (final String unitTypeID)
	{
		return unitTypesMap.get (unitTypeID);
	}
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @return Unit skill descriptions object; or null if not found
	 */
	@Override
	public final UnitSkillLang findUnitSkill (final String unitSkillID)
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
		final RangedAttackTypeLang rat = rangedAttackTypesMap.get (rangedAttackTypeID);
		final String desc = (rat != null) ? rat.getRangedAttackTypeDescription () : null; 
		return (desc != null) ? desc : rangedAttackTypeID;
	}
	
	/**
	 * @param unitID Unit ID to search for
	 * @return Unit descriptions object; or null if not found
	 */
	@Override
	public final UnitLang findUnit (final String unitID)
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
		final HeroLang thisHeroName = heroNamesMap.get (heroNameID);
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
		final CitySizeLang thisCitySize = citySizesMap.get (citySizeID);
		final String citySizeName = (thisCitySize != null) ? thisCitySize.getCitySizeName () : null; 
		return (citySizeName != null) ? citySizeName : citySizeID;
	}
	
	/**
	 * @param citySpellEffectID City spell effect ID to search for
	 * @return City spell effect descriptions object; or null if not found
	 */
	@Override
	public final CitySpellEffectLang findCitySpellEffect (final String citySpellEffectID)
	{
		return citySpellEffectsMap.get (citySpellEffectID);
	}

	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @return Combat area effect descriptions object; or null if not found
	 */
	@Override
	public final CombatAreaEffectLang findCombatAreaEffect (final String combatAreaEffectID)
	{
		return combatAreaEffectsMap.get (combatAreaEffectID);
	}
	
	/**
	 * @param spellRankID Spell rank ID to search for
	 * @return Spell rank description; or replays back the ID if no description exists
	 */
	@Override
	public final String findSpellRankDescription (final String spellRankID)
	{
		final SpellRankLang thisSpellRank = spellRanksMap.get (spellRankID);
		final String desc = (thisSpellRank != null) ? thisSpellRank.getSpellRankDescription () : null; 
		return (desc != null) ? desc : spellRankID;
	}

	/**
	 * @param spellBookSectionID Spell book section ID to search for
	 * @return Spell book section descriptions object; or null if not found
	 */
	@Override
	public final SpellBookSectionLang findSpellBookSection (final SpellBookSectionID spellBookSectionID)
	{
		return spellBookSectionsMap.get (spellBookSectionID);
	}
	
	/**
	 * @param spellID Spell ID to search for
	 * @return Spell descriptions object; or null if not found
	 */
	@Override
	public final SpellLang findSpell (final String spellID)
	{
		return spellsMap.get (spellID);
	}
	
	/**
	 * @param heroItemTypeID Hero item type ID to search for
	 * @return Hero item type description; or replays back the ID if no description exists
	 */
	@Override
	public final String findHeroItemTypeDescription (final String heroItemTypeID)
	{
		final HeroItemTypeLang thisHeroItemType = heroItemTypesMap.get (heroItemTypeID);
		final String desc = (thisHeroItemType != null) ? thisHeroItemType.getHeroItemTypeDescription () : null;
		return (desc != null) ? desc : heroItemTypeID;
	}
	
	/**
	 * @param heroItemSlotTypeID Hero item slot type ID to search for
	 * @return Hero item slot type description; or replays back the ID if no description exists
	 */
	@Override
	public final String findHeroItemSlotTypeDescription (final String heroItemSlotTypeID)
	{
		final HeroItemSlotTypeLang thisHeroItemSlotType = heroItemSlotTypesMap.get (heroItemSlotTypeID);
		final String desc = (thisHeroItemSlotType != null) ? thisHeroItemSlotType.getSlotTypeDescription () : null;
		return (desc != null) ? desc : heroItemSlotTypeID;
	}
	
	/**
	 * @param heroItemBonusID Hero item bonus ID to search for
	 * @return Hero item bonus description; or replays back the ID if no description exists
	 */
	@Override
	public final String findHeroItemBonusDescription (final String heroItemBonusID)
	{
		final HeroItemBonusLang thisHeroItemBonus = heroItemBonusesMap.get (heroItemBonusID);
		final String desc = (thisHeroItemBonus != null) ? thisHeroItemBonus.getHeroItemBonusDescription () : null;
		return (desc != null) ? desc : heroItemBonusID;
	}
	
	/**
	 * @param overlandMapSizeID Overland map size ID to search for
	 * @return Overland map size description; or replays back the ID if no description exists
	 */
	@Override
	public final String findOverlandMapSizeDescription (final String overlandMapSizeID)
	{
		final OverlandMapSizeLang thisMapSize = overlandMapSizesMap.get (overlandMapSizeID);
		final String desc = (thisMapSize != null) ? thisMapSize.getOverlandMapSizeDescription () : null;
		return (desc != null) ? desc : overlandMapSizeID;
	}
	
	/**
	 * @param landProportionID Land proportion ID to search for
	 * @return Land proportion description; or replays back the ID if no description exists
	 */
	@Override
	public final String findLandProportionDescription (final String landProportionID)
	{
		final LandProportionLang thisLandProportion = landProportionsMap.get (landProportionID);
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
		final NodeStrengthLang thisNodeStrength = nodeStrengthsMap.get (nodeStrengthID);
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
		final DifficultyLevelLang thisDifficultyLevel = difficultyLevelsMap.get (difficultyLevelID);
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
		final FogOfWarSettingLang thisFogOfWarSetting = fogOfWarSettingsMap.get (fogOfWarSettingID);
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
		final UnitSettingLang thisUnitSetting = unitSettingsMap.get (unitSettingID);
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
		final SpellSettingLang thisSpellSetting = spellSettingsMap.get (spellSettingID);
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
	 * @return List of all known servers
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<KnownServerLang> getKnownServers ()
	{
		return (List<KnownServerLang>) (List<?>) getKnownServer ();
	}
	
	/**
	 * @param shortcut Game shortcut that we're looking to see if there is a key defined for it
	 * @return Details of the key that should activate this shortcut, or null if none is defined
	 */
	@Override
	public final ShortcutKeyLang findShortcutKey (final Shortcut shortcut)
	{
		return shortcutsMap.get (shortcut);
	}
}
package momime.common.database;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.MomException;
import momime.common.messages.MemoryBuilding;

/**
 * Interface describing lookups that are needed by the utils in MoMIMECommon 
 * Both the server and client databases then provide implementations of these
 */
public interface CommonDatabase
{
	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public void buildMaps ();
	
	/**
	 * Derives values from the received database
	 * @throws MomException If any of the consistency checks fail
	 */
	public void consistencyChecks () throws MomException;
	
	/**
	 * Consistency checks that can only be ran on the client
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public void clientConsistencyChecks () throws IOException;
	
	/**
	 * @return Complete list of all pre-defined overland map sizes
	 */
	public List<OverlandMapSize> getOverlandMapSize ();

	/**
	 * @return Complete list of all pre-defined land proportions
	 */
	public List<LandProportion> getLandProportion ();

	/**
	 * @return Complete list of all pre-defined node strengths
	 */
	public List<NodeStrength> getNodeStrength ();

	/**
	 * @return Complete list of all pre-defined difficulty levels
	 */
	public List<DifficultyLevel> getDifficultyLevel ();

	/**
	 * @return Complete list of all pre-defined fog of war settings
	 */
	public List<FogOfWarSetting> getFogOfWarSetting ();

	/**
	 * @return Complete list of all pre-defined unit settings
	 */
	public List<UnitSetting> getUnitSetting ();

	/**
	 * @return Complete list of all pre-defined spell settings
	 */
	public List<SpellSetting> getSpellSetting ();

	/**
	 * @return Complete list of all pre-defined hero item settings
	 */
	public List<HeroItemSetting> getHeroItemSetting ();
	
	/**
	 * @return Default map size, difficulty level and so on to preselect on the new game screen
	 */
	public NewGameDefaults getNewGameDefaults ();
	
	/**
	 * @return Complete list of all planes in game
	 */
	public List<Plane> getPlane ();

	/**
	 * @param planeNumber Plane number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Plane object
	 * @throws RecordNotFoundException If the plane number doesn't exist
	 */
	public Plane findPlane (final int planeNumber, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all map features in game
	 */
	public List<MapFeatureEx> getMapFeatures ();

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	public MapFeatureEx findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all tile types in game
	 */
	public List<TileTypeEx> getTileTypes ();

	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	public TileTypeEx findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all production types in game
	 */
	public List<ProductionTypeEx> getProductionTypes ();

	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	public ProductionTypeEx findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all pick types in game
	 */
	public List<PickType> getPickType ();

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	public PickType findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all picks in game
	 */
	public List<Pick> getPick ();
	
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	public Pick findPick (final String pickID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all wizards in game
	 */
	public List<WizardEx> getWizards ();

	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	public WizardEx findWizard (final String wizardID, final String caller) throws RecordNotFoundException;

	/**
	 * @param combatActionID Combat action ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Combat action object
	 * @throws RecordNotFoundException If the combatActionID doesn't exist
	 */
	public CombatAction findCombatAction (final String combatActionID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all unit types in game
	 */
	public List<UnitTypeEx> getUnitTypes ();
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	public UnitTypeEx findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all units in game
	 */
	public List<UnitEx> getUnits ();

	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public UnitEx findUnit (final String unitID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all unit skills in game
	 */
	public List<UnitSkillEx> getUnitSkills ();

	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	public UnitSkillEx findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all weapon grades in game
	 */
	public List<WeaponGrade> getWeaponGrade ();
	
	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weapon grade number doesn't exist
	 */
	public WeaponGrade findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all ranged attack types in game
	 */
	public List<RangedAttackTypeEx> getRangedAttackTypes ();
	
	/**
	 * @param rangedAttackTypeID RAT ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return RAT object
	 * @throws RecordNotFoundException If the RAT ID doesn't exist
	 */
	public RangedAttackTypeEx findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param populationTaskID Population task ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Population task object
	 * @throws RecordNotFoundException If the population task ID doesn't exist
	 */
	public PopulationTask findPopulationTask (final String populationTaskID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param citySizeID City size ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return City size object
	 * @throws RecordNotFoundException If the city size ID doesn't exist
	 */
	public CitySize findCitySize (final String citySizeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all races in game
	 */
	public List<RaceEx> getRaces ();

	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	public RaceEx findRace (final String raceID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all tax rates in game
	 */
	public List<TaxRate> getTaxRate ();
	
	/**
	 * @param taxRateID Tax rate ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tax rate object
	 * @throws RecordNotFoundException If the tax rateID doesn't exist
	 */
	public TaxRate findTaxRate (final String taxRateID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all buildings in game
	 */
	public List<Building> getBuilding ();

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	public Building findBuilding (final String buildingID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all city spell effects in game
	 */
	public List<CitySpellEffect> getCitySpellEffect ();
	
	/**
	 * @param citySpellEffectID City spell effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CitySpellEffect object
	 * @throws RecordNotFoundException If the city spell effect ID doesn't exist
	 */
	public CitySpellEffect findCitySpellEffect (final String citySpellEffectID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all city sizes in game
	 */
	public List<CitySize> getCitySize ();
	
	/**
	 * Note this isn't straightforward like the other lookups, since one citySizeID can have multiple entries in the graphics XML,
	 * some with specialised graphics showing particular buildings.  So this must pick the most appropriate entry.
	 * 
	 * @param citySizeID City size ID to search for
	 * @param cityLocation Location of the city, so we can check what buildings it has
	 * @param buildings List of known buildings
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return City size object
	 * @throws RecordNotFoundException If no city size entries match the requested citySizeID
	 */
	public CityImage findBestCityImage (final String citySizeID, final MapCoordinates3DEx cityLocation,
		final List<MemoryBuilding> buildings, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all city view elements
	 */
	public List<CityViewElement> getCityViewElement ();
	
	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object; note buildings in the graphics XML are just a special case of city view elements
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	public CityViewElement findCityViewElementBuilding (final String buildingID, final String caller) throws RecordNotFoundException;

	/**
	 * Note some city spell effects have more than one city view element defined, one for arcanus and one for myrror.  In this case this will return the arcanus entry in preference.
	 * 
	 * @param citySpellEffectID City spell effect ID to search for
	 * @return City spell effect object, or null if not found (e.g. Pestilence has no image)
	 */
	public CityViewElement findCityViewElementSpellEffect (final String citySpellEffectID);
		
	/**
	 * @return Complete list of all combat map elements in game
	 */
	public List<CombatMapElement> getCombatMapElement ();
	
	/**
	 * @return Complete list of all spell ranks in game
	 */
	public List<SpellRank> getSpellRank ();

	/**
	 * @param spellRankID Spell rank ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell rank object
	 * @throws RecordNotFoundException If the spellRankID doesn't exist
	 */
	public SpellRank findSpellRank (final String spellRankID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param sectionID Spell book section ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell book section object
	 * @throws RecordNotFoundException If the sectionID doesn't exist
	 */
	public SpellBookSection findSpellBookSection (final SpellBookSectionID sectionID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all spells in game
	 */
	public List<Spell> getSpell ();

	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	public Spell findSpell (final String spellID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all CAEs in game
	 */
	public List<CombatAreaEffect> getCombatAreaEffect ();
	
	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatAreaEffect object
	 * @throws RecordNotFoundException If the combat area effect ID doesn't exist
	 */
	public CombatAreaEffect findCombatAreaEffect (final String combatAreaEffectID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all combat tile types in game
	 */
	public List<CombatTileType> getCombatTileType ();
	
	/**
	 * @param combatTileTypeID Combat tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileType object
	 * @throws RecordNotFoundException If the combat tile type ID doesn't exist
	 */
	public CombatTileType findCombatTileType (final String combatTileTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all combat tile borders in game
	 */
	public List<CombatTileBorder> getCombatTileBorder ();
	
	/**
	 * @param combatTileBorderID Combat tile border ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileBorder object
	 * @throws RecordNotFoundException If the combat tile border ID doesn't exist
	 */
	public CombatTileBorder findCombatTileBorder (final String combatTileBorderID, final String caller) throws RecordNotFoundException;

    /**
     * @param combatTileBorderID Combat tile border ID to search for
     * @param directions Border directions to search for
     * @param frontOrBack Whether to look for the front or back image
     * @return Image details if found; null if not found
     */
    public CombatTileBorderImage findCombatTileBorderImages (final String combatTileBorderID, final String directions, final FrontOrBack frontOrBack);
	
	/**
	 * @return Complete list of all movement rate rules in game
	 */
	public List<MovementRateRule> getMovementRateRule ();

	/**
	 * @return Complete list of all hero item slot types in game
	 */
	public List<HeroItemSlotType> getHeroItemSlotType ();
	
	/**
	 * @param heroItemSlotTypeID Hero item slot type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemSlotType object
	 * @throws RecordNotFoundException If the hero item slot type ID doesn't exist
	 */
	public HeroItemSlotType findHeroItemSlotType (final String heroItemSlotTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all hero item types in game
	 */
	public List<HeroItemType> getHeroItemType ();
	
	/**
	 * @param heroItemTypeID Hero item type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemType object
	 * @throws RecordNotFoundException If the hero item type ID doesn't exist
	 */
	public HeroItemType findHeroItemType (final String heroItemTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all hero item bonuses in game
	 */
	public List<HeroItemBonus> getHeroItemBonus ();
	
	/**
	 * @param heroItemBonusID Hero item bonus ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemBonus object
	 * @throws RecordNotFoundException If the hero item bonus ID doesn't exist
	 */
	public HeroItemBonus findHeroItemBonus (final String heroItemBonusID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all pre-defined hero items
	 */
	public List<HeroItem> getHeroItem ();
	
	/**
	 * @return Complete list of all damage types in game
	 */
	public List<DamageType> getDamageType ();
	
	/**
	 * @param damageTypeID Damage type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return DamageType object
	 * @throws RecordNotFoundException If the damage type ID doesn't exist
	 */
	public DamageType findDamageType (final String damageTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all AI unit categories in game
	 */
	public List<AiUnitCategory> getAiUnitCategory ();

	/**
	 * @return Complete list of all tile sets in game
	 */
	public List<TileSetEx> getTileSets ();
	
	/**
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	public TileSetEx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all animations in game
	 */
	public List<AnimationEx> getAnimations ();

	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	public AnimationEx findAnimation (final String animationID, final String caller) throws RecordNotFoundException;

	/**
	 * @param playListID Play list ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Play list object
	 * @throws RecordNotFoundException If the playListID doesn't exist
	 */
	public PlayList findPlayList (final String playListID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of random events
	 */
	public List<Event> getEvent ();
	
	/**
	 * @param eventID Random event ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Random event object
	 * @throws RecordNotFoundException If the eventID doesn't exist
	 */
	public Event findEvent (final String eventID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of wizard personalities
	 */
	public List<WizardPersonality> getWizardPersonality ();
	
	/**
	 * @param personalityID Wizard personality ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard personality object
	 * @throws RecordNotFoundException If the personalityID doesn't exist
	 */
	public WizardPersonality findWizardPersonality (final String personalityID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of wizard objectives
	 */
	public List<WizardObjective> getWizardObjective ();

	/**
	 * @param objectiveID Wizard objective ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard objective object
	 * @throws RecordNotFoundException If the objectiveID doesn't exist
	 */
	public WizardObjective findWizardObjective (final String objectiveID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param score Relation score to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Relation score object
	 * @throws RecordNotFoundException If no object is defined with the specified score in its range
	 */
	public RelationScore findRelationScore (final int score, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Cost to construct the most expensive unit or building in the database
	 */
	public int getMostExpensiveConstructionCost ();
	
	/**
	 * NB. This will find the largest width and the largest height separately, so its possible this may return a dimension
	 * which no building actually has, if e.g. the widest is 50x25 and the tallest is 20x40 then it would return 50x40.
	 * 
	 * @return Size of the largest building image that can be constructed
	 */
	public Dimension getLargestBuildingSize ();
	
	/**
	 * @return Hero item bonus ID that grants invisibility
	 */
	public String getInvisibilityHeroItemBonusID ();

	/**
	 * @return City walls building ID
	 */
	public String getCityWallsBuildingID ();
	
	/**
	 * @return List of unit IDs that have the special unit skill that they can move through other units and other units can move through them (Magic Vortex) 
	 */
	public List<String> getUnitsThatMoveThroughOtherUnits ();

	/**
	 * @return Map of unit skill IDs to the damage reduction they give
	 */
	public Map<String, Integer> getDamageReductionSkills ();
}
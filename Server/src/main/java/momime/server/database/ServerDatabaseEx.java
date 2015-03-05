package momime.server.database;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.NewGameDefaults;
import momime.common.database.RecordNotFoundException;

/**
 * Describes operations that we need to support over the server XML file
 * This overrides a lot of the methods declared on CommonDatabase to return the server-specific versions
 */
public interface ServerDatabaseEx extends CommonDatabase
{
	/**
	 * @return Default map size, difficulty level and so on to preselect on the new game screen
	 */
	public NewGameDefaults getNewGameDefaults ();
	
	/**
	 * @return Complete list of all planes in game
	 */
	@Override
	public List<PlaneSvr> getPlanes ();
	
	/**
	 * @return Complete list of all map features in game
	 */
	@Override
	public List<MapFeatureSvr> getMapFeatures ();

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public MapFeatureSvr findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all tile types in game
	 */
	@Override
	public List<TileTypeSvr> getTileTypes ();
	
	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	@Override
	public TileTypeSvr findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all production types in game
	 */
	@Override
	public List<ProductionTypeSvr> getProductionTypes ();
	
	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public ProductionTypeSvr findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all pick types in game
	 */
	@Override
	public List<PickTypeSvr> getPickTypes ();
	
	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	@Override
	public PickTypeSvr findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	@Override
	public PickSvr findPick (final String pickID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all wizards in game
	 */
	@Override
	public List<WizardSvr> getWizards ();
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public WizardSvr findWizard (final String wizardID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all units in game
	 */
	@Override
	public List<UnitSvr> getUnits ();
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public UnitSvr findUnit (final String unitID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all unit skills in game
	 */
	@Override
	public List<UnitSkillSvr> getUnitSkills ();
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	@Override
	public UnitSkillSvr findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all races in game
	 */
	@Override
	public List<RaceSvr> getRaces ();
	
	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	@Override
	public RaceSvr findRace (final String raceID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all buildings in game
	 */
	@Override
	public List<BuildingSvr> getBuildings ();
	
	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public BuildingSvr findBuilding (final String buildingID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all spells in game
	 */
	@Override
	public List<SpellSvr> getSpells ();
	
	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	@Override
	public SpellSvr findSpell (final String spellID, final String caller) throws RecordNotFoundException;

	/**
	 * @param combatTileTypeID Combat tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileType object
	 * @throws RecordNotFoundException If the combat tile type ID doesn't exist
	 */
	@Override
	public CombatTileTypeSvr findCombatTileType (final String combatTileTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @param citySpellEffectID City spell effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CitySpellEffect object
	 * @throws RecordNotFoundException If the city spell effect ID doesn't exist
	 */
	public CitySpellEffectSvr findCitySpellEffect (final String citySpellEffectID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all picks in game
	 */
	@Override
	public List<PickSvr> getPicks ();

	/**
	 * @return Complete list of all city sizes in game
	 */
	public List<CitySizeSvr> getCitySizes ();

	/**
	 * @return Complete list of all unit magic realms in game
	 */
	public List<UnitMagicRealmSvr> getUnitMagicRealms ();

	/**
	 * @return Complete list of all unit types in game
	 */
	public List<UnitTypeSvr> getUnitTypes ();
	
	/**
	 * @return Complete list of all unit attributes in game
	 */
	@Override
	public List<UnitAttributeSvr> getUnitAttributes ();

	/**
	 * @return Complete list of all ranged attack types in game
	 */
	public List<RangedAttackTypeSvr> getRangedAttackTypes ();

	/**
	 * @return Complete list of all weapon grades in game
	 */
	public List<WeaponGradeSvr> getWeaponGrades ();

	/**
	 * @return Complete list of all movement rate rules in game
	 */
	public List<MovementRateRuleSvr> getMovementRateRules ();

	/**
	 * @return Complete list of all combat map elements in game
	 */
	public List<CombatMapElementSvr> getCombatMapElements ();

	/**
	 * @return Complete list of all CAEs in game
	 */
	public List<CombatAreaEffectSvr> getCombatAreaEffects ();

	/**
	 * @return Complete list of all combat tile types in game
	 */
	public List<CombatTileTypeSvr> getCombatTileTypes ();

	/**
	 * @return Complete list of all combat tile borders in game
	 */
	public List<CombatTileBorderSvr> getCombatTileBorders ();
	
	/**
	 * @return Complete list of all pre-defined map sizes
	 */
	public List<MapSizeSvr> getMapSizes ();

	/**
	 * @return Complete list of all pre-defined land proportions
	 */
	public List<LandProportionSvr> getLandProportions ();

	/**
	 * @return Complete list of all pre-defined node strengths
	 */
	public List<NodeStrengthSvr> getNodeStrengths ();

	/**
	 * @return Complete list of all pre-defined difficulty levels
	 */
	public List<DifficultyLevelSvr> getDifficultyLevels ();

	/**
	 * @return Complete list of all pre-defined fog of war settings
	 */
	public List<FogOfWarSettingSvr> getFogOfWarSettings ();

	/**
	 * @return Complete list of all pre-defined unit settings
	 */
	public List<UnitSettingSvr> getUnitSettings ();

	/**
	 * @return Complete list of all pre-defined spell settings
	 */
	public List<SpellSettingSvr> getSpellSettings ();
}
package momime.server.database;

import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.TaxRate;
import momime.server.database.v0_9_5.Building;
import momime.server.database.v0_9_5.CitySize;
import momime.server.database.v0_9_5.CombatAreaEffect;
import momime.server.database.v0_9_5.CombatMapElement;
import momime.server.database.v0_9_5.CombatTileBorder;
import momime.server.database.v0_9_5.CombatTileType;
import momime.server.database.v0_9_5.DifficultyLevel;
import momime.server.database.v0_9_5.FogOfWarSetting;
import momime.server.database.v0_9_5.LandProportion;
import momime.server.database.v0_9_5.MapFeature;
import momime.server.database.v0_9_5.MapSize;
import momime.server.database.v0_9_5.MovementRateRule;
import momime.server.database.v0_9_5.NodeStrength;
import momime.server.database.v0_9_5.Pick;
import momime.server.database.v0_9_5.PickType;
import momime.server.database.v0_9_5.Plane;
import momime.server.database.v0_9_5.ProductionType;
import momime.server.database.v0_9_5.Race;
import momime.server.database.v0_9_5.RangedAttackType;
import momime.server.database.v0_9_5.Spell;
import momime.server.database.v0_9_5.SpellSetting;
import momime.server.database.v0_9_5.TileType;
import momime.server.database.v0_9_5.Unit;
import momime.server.database.v0_9_5.UnitAttribute;
import momime.server.database.v0_9_5.UnitMagicRealm;
import momime.server.database.v0_9_5.UnitSetting;
import momime.server.database.v0_9_5.UnitSkill;
import momime.server.database.v0_9_5.UnitType;
import momime.server.database.v0_9_5.WeaponGrade;
import momime.server.database.v0_9_5.Wizard;

/**
 * Describes operations that we need to support over the server XML file
 * This overrides a lot of the methods declared on CommonDatabase to return the server-specific versions
 */
public interface ServerDatabaseEx extends CommonDatabase
{
	/**
	 * @return Complete list of all planes in game
	 */
	@Override
	public List<Plane> getPlane ();
	
	/**
	 * @return Complete list of all map features in game
	 */
	@Override
	public List<MapFeature> getMapFeature ();

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public MapFeature findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all tile types in game
	 */
	@Override
	public List<TileType> getTileType ();
	
	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	@Override
	public TileType findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all production types in game
	 */
	@Override
	public List<ProductionType> getProductionType ();
	
	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public ProductionType findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all pick types in game
	 */
	@Override
	public List<PickType> getPickType ();
	
	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	@Override
	public PickType findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	@Override
	public Pick findPick (final String pickID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all wizards in game
	 */
	@Override
	public List<Wizard> getWizard ();
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all units in game
	 */
	@Override
	public List<Unit> getUnit ();
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public Unit findUnit (final String unitID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all unit skills in game
	 */
	@Override
	public List<UnitSkill> getUnitSkill ();
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	@Override
	public UnitSkill findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all races in game
	 */
	@Override
	public List<Race> getRace ();
	
	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	@Override
	public Race findRace (final String raceID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all buildings in game
	 */
	@Override
	public List<Building> getBuilding ();
	
	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public Building findBuilding (final String buildingID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all spells in game
	 */
	@Override
	public List<Spell> getSpell ();
	
	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	@Override
	public Spell findSpell (final String spellID, final String caller) throws RecordNotFoundException;

	/**
	 * @param combatTileTypeID Combat tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileType object
	 * @throws RecordNotFoundException If the combat tile type ID doesn't exist
	 */
	@Override
	public CombatTileType findCombatTileType (final String combatTileTypeID, final String caller) throws RecordNotFoundException;
	
	// New methods that aren't overrides from CommonDatabase

	/**
	 * @return Complete list of all picks in game
	 */
	public List<Pick> getPick ();

	/**
	 * @return Complete list of all tax rates in game
	 */
	public List<TaxRate> getTaxRate ();
	
	/**
	 * @return Complete list of all city sizes in game
	 */
	public List<CitySize> getCitySize ();

	/**
	 * @return Complete list of all unit magic realms in game
	 */
	public List<UnitMagicRealm> getUnitMagicRealm ();

	/**
	 * @return Complete list of all unit types in game
	 */
	public List<UnitType> getUnitType ();
	
	/**
	 * @return Complete list of all unit attributes in game
	 */
	@Override
	public List<UnitAttribute> getUnitAttribute ();

	/**
	 * @return Complete list of all ranged attack types in game
	 */
	public List<RangedAttackType> getRangedAttackType ();

	/**
	 * @return Complete list of all weapon grades in game
	 */
	public List<WeaponGrade> getWeaponGrade ();

	/**
	 * @return Complete list of all movement rate rules in game
	 */
	public List<MovementRateRule> getMovementRateRule ();

	/**
	 * @return Complete list of all combat map elements in game
	 */
	public List<CombatMapElement> getCombatMapElement ();

	/**
	 * @return Complete list of all CAEs in game
	 */
	public List<CombatAreaEffect> getCombatAreaEffect ();

	/**
	 * @return Complete list of all combat tile types in game
	 */
	public List<CombatTileType> getCombatTileType ();

	/**
	 * @return Complete list of all combat tile borders in game
	 */
	public List<CombatTileBorder> getCombatTileBorder ();
	
	/**
	 * @return Complete list of all pre-defined map sizes
	 */
	public List<MapSize> getMapSize ();

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
}

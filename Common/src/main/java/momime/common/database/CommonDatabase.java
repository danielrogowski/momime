package momime.common.database;

import java.util.List;

import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.CombatAreaEffect;
import momime.common.database.v0_9_5.CombatTileBorder;
import momime.common.database.v0_9_5.CombatTileType;
import momime.common.database.v0_9_5.MapFeature;
import momime.common.database.v0_9_5.Pick;
import momime.common.database.v0_9_5.PickType;
import momime.common.database.v0_9_5.Plane;
import momime.common.database.v0_9_5.ProductionType;
import momime.common.database.v0_9_5.Race;
import momime.common.database.v0_9_5.RangedAttackType;
import momime.common.database.v0_9_5.Spell;
import momime.common.database.v0_9_5.TaxRate;
import momime.common.database.v0_9_5.TileType;
import momime.common.database.v0_9_5.Unit;
import momime.common.database.v0_9_5.UnitAttribute;
import momime.common.database.v0_9_5.UnitMagicRealm;
import momime.common.database.v0_9_5.UnitSkill;
import momime.common.database.v0_9_5.UnitType;
import momime.common.database.v0_9_5.WeaponGrade;
import momime.common.database.v0_9_5.Wizard;

/**
 * Interface describing lookups that are needed by the utils in MoMIMECommon 
 * Both the server and client databases then provide implementations of these
 */
public interface CommonDatabase
{
	/**
	 * @return Complete list of all planes in game
	 */
	public List<? extends Plane> getPlane ();

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
	public List<? extends MapFeature> getMapFeature ();

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	public MapFeature findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all tile types in game
	 */
	public List<? extends TileType> getTileType ();

	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	public TileType findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all production types in game
	 */
	public List<? extends ProductionType> getProductionType ();

	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	public ProductionType findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all pick types in game
	 */
	public List<? extends PickType> getPickType ();

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	public PickType findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException;

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
	public List<? extends Wizard> getWizard ();

	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	public Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException;

	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	public UnitType findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @param unitMagicRealmID Unit magic realm ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit magic realm object
	 * @throws RecordNotFoundException If the unitMagicRealmID doesn't exist
	 */
	public UnitMagicRealm findUnitMagicRealm (final String unitMagicRealmID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all units in game
	 */
	public List<? extends Unit> getUnit ();

	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public Unit findUnit (final String unitID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all unit skills in game
	 */
	public List<? extends UnitSkill> getUnitSkill ();

	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	public UnitSkill findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all unit attributes in game
	 */
	public List<? extends UnitAttribute> getUnitAttribute ();
	
	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weapon grade number doesn't exist
	 */
	public WeaponGrade findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException;

	/**
	 * @param rangedAttackTypeID RAT ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return RAT object
	 * @throws RecordNotFoundException If the RAT ID doesn't exist
	 */
	public RangedAttackType findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return Complete list of all races in game
	 */
	public List<? extends Race> getRace ();

	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	public Race findRace (final String raceID, final String caller) throws RecordNotFoundException;

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
	public List<? extends Building> getBuilding ();

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	public Building findBuilding (final String buildingID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all spells in game
	 */
	public List<? extends Spell> getSpell ();

	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	public Spell findSpell (final String spellID, final String caller) throws RecordNotFoundException;

	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatAreaEffect object
	 * @throws RecordNotFoundException If the combat area effect ID doesn't exist
	 */
	public CombatAreaEffect findCombatAreaEffect (final String combatAreaEffectID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param combatTileTypeID Combat tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileType object
	 * @throws RecordNotFoundException If the combat tile type ID doesn't exist
	 */
	public CombatTileType findCombatTileType (final String combatTileTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @param  combatTileBorderID Combat tile border ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileBorder object
	 * @throws RecordNotFoundException If the combat tile border ID doesn't exist
	 */
	public CombatTileBorder findCombatTileBorder (final String combatTileBorderID, final String caller) throws RecordNotFoundException;
}

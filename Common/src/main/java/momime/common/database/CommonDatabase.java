package momime.common.database;

import java.util.List;

/**
 * Interface describing lookups that are needed by the utils in MoMIMECommon 
 * Both the server and client databases then provide implementations of these
 */
public interface CommonDatabase
{
	/**
	 * @return Complete list of all planes in game
	 */
	public List<? extends Plane> getPlanes ();

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
	public List<? extends MapFeature> getMapFeatures ();

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
	public List<? extends TileType> getTileTypes ();

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
	public List<? extends ProductionType> getProductionTypes ();

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
	public List<? extends PickType> getPickTypes ();

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
	public List<? extends Pick> getPicks ();
	
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
	public List<? extends Wizard> getWizards ();

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
	 * @return Complete list of all units in game
	 */
	public List<? extends Unit> getUnits ();

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
	public List<? extends UnitSkill> getUnitSkills ();

	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	public UnitSkill findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException;

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
	public List<? extends Race> getRaces ();

	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	public Race findRace (final String raceID, final String caller) throws RecordNotFoundException;

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
	public List<? extends Building> getBuildings ();

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
	public List<? extends Spell> getSpells ();

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
	 * @param combatTileBorderID Combat tile border ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileBorder object
	 * @throws RecordNotFoundException If the combat tile border ID doesn't exist
	 */
	public CombatTileBorder findCombatTileBorder (final String combatTileBorderID, final String caller) throws RecordNotFoundException;

	/**
	 * @return Complete list of all movement rate rules in game
	 */
	public List<MovementRateRule> getMovementRateRule ();

	/**
	 * @return Complete list of all hero item slot types in game
	 */
	public List<? extends HeroItemSlotType> getHeroItemSlotType ();
	
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
	public List<? extends HeroItemType> getHeroItemType ();
	
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
	public List<? extends HeroItemBonus> getHeroItemBonus ();
	
	/**
	 * @param heroItemBonusID Hero item bonus ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemBonus object
	 * @throws RecordNotFoundException If the hero item bonus ID doesn't exist
	 */
	public HeroItemBonus findHeroItemBonus (final String heroItemBonusID, final String caller) throws RecordNotFoundException;

	/**
	 * @param damageTypeID Damage type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return DamageType object
	 * @throws RecordNotFoundException If the damage type ID doesn't exist
	 */
	public DamageType findDamageType (final String damageTypeID, final String caller) throws RecordNotFoundException;
}
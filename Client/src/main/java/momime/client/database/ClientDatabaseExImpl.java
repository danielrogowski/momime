package momime.client.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.database.Building;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CombatTileBorder;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageType;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemSlotType;
import momime.common.database.HeroItemType;
import momime.common.database.Pick;
import momime.common.database.PickType;
import momime.common.database.Plane;
import momime.common.database.ProductionType;
import momime.common.database.Race;
import momime.common.database.RangedAttackType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.TaxRate;
import momime.common.database.TileType;
import momime.common.database.Unit;
import momime.common.database.UnitSkill;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;

/**
 * Adds client-side specific extensions to the common database lookup class
 */
public final class ClientDatabaseExImpl extends ClientDatabase implements ClientDatabaseEx
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (ClientDatabaseExImpl.class);
	
	/** Map of plane numbers to plane XML objects */
	private Map<Integer, Plane> planesMap;

	/** Map of map feature IDs to map feature XML objects */
	private Map<String, MapFeature> mapFeaturesMap;

	/** Map of tile type IDs to file type XML objects */
	private Map<String, TileType> tileTypesMap;

	/** Map of production type IDs to production type XML objects */
	private Map<String, ProductionType> productionTypesMap;

	/** Map of pick type IDs to pick XML objects */
	private Map<String, PickType> pickTypesMap;

	/** Map of pick IDs to pick XML objects */
	private Map<String, Pick> picksMap;

	/** Map of wizard IDs to wizard XML objects */
	private Map<String, Wizard> wizardsMap;

	/** Map of unit type IDs to unit type XML objects */
	private Map<String, UnitType> unitTypesMap;

	/** Map of unit IDs to unit XML objects */
	private Map<String, Unit> unitsMap;

	/** Map of unit skill IDs to unit skill XML objects */
	private Map<String, UnitSkill> unitSkillsMap;

	/** Map of weapon grade numbers to weapon grade XML objects */
	private Map<Integer, WeaponGrade> weaponGradesMap;

	/** Map of ranged attack type IDs to ranged attack type XML objects */
	private Map<String, RangedAttackType> rangedAttackTypesMap;
	
	/** Map of race IDs to race XML objects */
	private Map<String, Race> racesMap;

	/** Map of tax rate IDs to tax rate XML objects */
	private Map<String, TaxRate> taxRatesMap;

	/** Map of building IDs to building XML objects */
	private Map<String, Building> buildingsMap;

	/** Map of spell IDs to spell XML objects */
	private Map<String, Spell> spellsMap;

	/** Map of combat area effect IDs to combat area effect objects */
	private Map<String, CombatAreaEffect> combatAreaEffectsMap;
	
	/** Map of combat tile type IDs to combat tile type objects */
	private Map<String, CombatTileType> combatTileTypesMap;

	/** Map of combat tile border IDs to combat tile border objects */
	private Map<String, CombatTileBorder> combatTileBordersMap;
	
	/** Map of hero item slot type IDs to hero item slot type objects */
	private Map<String, HeroItemSlotType> heroItemSlotTypesMap;

	/** Map of hero item type IDs to hero item type objects */
	private Map<String, HeroItemType> heroItemTypesMap;

	/** Map of hero item bonus IDs to hero item bonus objects */
	private Map<String, HeroItemBonus> heroItemBonusesMap;
	
	/** Map of damage type IDs to damage type objects */
	private Map<String, DamageType> damageTypesMap;
	
	/** Cost to construct the most expensive unit or building in the database */
	private int mostExpensiveConstructionCost;
	
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

		// Create map features map
		mapFeaturesMap = new HashMap<String, MapFeature> ();
		for (final MapFeature thisMapFeature : getMapFeature ())
			mapFeaturesMap.put (thisMapFeature.getMapFeatureID (), thisMapFeature);

		// Create tile types map
		tileTypesMap = new HashMap<String, TileType> ();
		for (final TileType thisTileType : getTileType ())
			tileTypesMap.put (thisTileType.getTileTypeID (), thisTileType);

		// Create production types map
		productionTypesMap = new HashMap<String, ProductionType> ();
		for (final ProductionType thisProductionType : getProductionType ())
			productionTypesMap.put (thisProductionType.getProductionTypeID (), thisProductionType);

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

		// Create unit types map
		unitTypesMap = new HashMap<String, UnitType> ();
		for (final UnitType thisUnitType : getUnitType ())
			unitTypesMap.put (thisUnitType.getUnitTypeID (), thisUnitType);

		// Create units map
		unitsMap = new HashMap<String, Unit> ();
		for (final Unit thisUnit : getUnit ())
			unitsMap.put (thisUnit.getUnitID (), thisUnit);

		// Create unit skills map
		unitSkillsMap = new HashMap<String, UnitSkill> ();
		for (final UnitSkill thisUnitSkill : getUnitSkill ())
			unitSkillsMap.put (thisUnitSkill.getUnitSkillID (), thisUnitSkill);

		// Create weaponGrades map
		weaponGradesMap = new HashMap<Integer, WeaponGrade> ();
		for (final WeaponGrade thisWeaponGrade : getWeaponGrade ())
			weaponGradesMap.put (thisWeaponGrade.getWeaponGradeNumber (), thisWeaponGrade);

		// Create rangedAttackTypes map
		rangedAttackTypesMap = new HashMap<String, RangedAttackType> ();
		for (final RangedAttackType thisRangedAttackType : getRangedAttackType ())
			rangedAttackTypesMap.put (thisRangedAttackType.getRangedAttackTypeID (), thisRangedAttackType);
		
		// Create races map
		racesMap = new HashMap<String, Race> ();
		for (final Race thisRace : getRace ())
			racesMap.put (thisRace.getRaceID (), thisRace);

		// Create tax rates map
		taxRatesMap = new HashMap<String, TaxRate> ();
		for (final TaxRate thisTaxRate : getTaxRate ())
			taxRatesMap.put (thisTaxRate.getTaxRateID (), thisTaxRate);

		// Create buildings map
		buildingsMap = new HashMap<String, Building> ();
		for (final Building thisBuilding : getBuilding ())
			buildingsMap.put (thisBuilding.getBuildingID (), thisBuilding);

		// Create spells map
		spellsMap = new HashMap<String, Spell> ();
		for (final Spell thisSpell : getSpell ())
			spellsMap.put (thisSpell.getSpellID (), thisSpell);

		// Create combat area effects map
		combatAreaEffectsMap = new HashMap<String, CombatAreaEffect> ();
		for (final CombatAreaEffect thisCombatAreaEffect : getCombatAreaEffect ())
			combatAreaEffectsMap.put (thisCombatAreaEffect.getCombatAreaEffectID (), thisCombatAreaEffect);

		// Combat tile types map
		combatTileTypesMap = new HashMap<String, CombatTileType> ();
		for (final CombatTileType thisCombatTileType : getCombatTileType ())
			combatTileTypesMap.put (thisCombatTileType.getCombatTileTypeID (), thisCombatTileType);

		// Combat tile borders map
		combatTileBordersMap = new HashMap<String, CombatTileBorder> ();
		for (final CombatTileBorder thisCombatTileBorder : getCombatTileBorder ())
			combatTileBordersMap.put (thisCombatTileBorder.getCombatTileBorderID (), thisCombatTileBorder);

		// Create hero item slot types map
		heroItemSlotTypesMap = new HashMap<String, HeroItemSlotType> ();
		for (final HeroItemSlotType thisHeroItemSlotType : getHeroItemSlotType ())
			heroItemSlotTypesMap.put (thisHeroItemSlotType.getHeroItemSlotTypeID (), thisHeroItemSlotType);

		// Create hero item types map
		heroItemTypesMap = new HashMap<String, HeroItemType> ();
		for (final HeroItemType thisHeroItemType : getHeroItemType ())
			heroItemTypesMap.put (thisHeroItemType.getHeroItemTypeID (), thisHeroItemType);

		// Create hero item bonuses map
		heroItemBonusesMap = new HashMap<String, HeroItemBonus> ();
		for (final HeroItemBonus thisHeroItemBonus : getHeroItemBonus ())
			heroItemBonusesMap.put (thisHeroItemBonus.getHeroItemBonusID (), thisHeroItemBonus);
		
		// Create damage types map
		damageTypesMap = new HashMap<String, DamageType> ();
		for (final DamageType thisDamageType : getDamageType ())
			damageTypesMap.put (thisDamageType.getDamageTypeID (), thisDamageType);
		
		log.trace ("Exiting buildMaps");
	}

	/**
	 * Derives values from the received database
	 */
	public final void consistencyChecks ()
	{
		log.trace ("Entering consistencyChecks");
		log.info ("Processing client XML file");
		
		// Check all buildings and units to find the most expensive one
		mostExpensiveConstructionCost = 0;
		for (final Building thisBuilding : getBuilding ())
			if (thisBuilding.getProductionCost () != null)
				mostExpensiveConstructionCost = Math.max (mostExpensiveConstructionCost, thisBuilding.getProductionCost ());

		for (final Unit thisUnit : getUnit ())
			if ((CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL.equals (thisUnit.getUnitMagicRealm ())) &&
				(thisUnit.getProductionCost () != null))
				
				mostExpensiveConstructionCost = Math.max (mostExpensiveConstructionCost, thisUnit.getProductionCost ());
		
		log.info ("Most expensive construction project is " + mostExpensiveConstructionCost);

		log.trace ("Exiting consistencyChecks");
	}
	
	/**
	 * Method triggered when we receive the database from the server
	 */
	public final void buildMapsAndRunConsistencyChecks ()
	{
		log.trace ("Entering buildMapsAndRunConsistencyChecks");

		buildMaps ();
		consistencyChecks ();

		log.trace ("Exiting buildMapsAndRunConsistencyChecks");
	}
	
	/**
	 * @return Complete list of all planes in game
	 */
	@Override
	public final List<Plane> getPlanes ()
	{
		return getPlane ();
	}
	
	/**
	 * @param planeNumber Plane number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Plane object
	 * @throws RecordNotFoundException If the plane number doesn't exist
	 */
	@Override
	public final Plane findPlane (final int planeNumber, final String caller) throws RecordNotFoundException
	{
		final Plane found = planesMap.get (planeNumber);
		if (found == null)
			throw new RecordNotFoundException (Plane.class, planeNumber, caller);

		return found;
	}

	/**
	 * @return Complete list of all map features in game
	 */
	@Override
	public final List<MapFeature> getMapFeatures ()
	{
		return getMapFeature ();
	}
	
	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public final MapFeature findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException
	{
		final MapFeature found = mapFeaturesMap.get (mapFeatureID);
		if (found == null)
			throw new RecordNotFoundException (MapFeature.class, mapFeatureID, caller);

		return found;
	}

	/**
	 * @return Complete list of all tile types in game
	 */
	@Override
	public final List<TileType> getTileTypes ()
	{
		return getTileType ();
	}
	
	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	@Override
	public final TileType findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException
	{
		final TileType found = tileTypesMap.get (tileTypeID);
		if (found == null)
			throw new RecordNotFoundException (TileType.class, tileTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all production types in game
	 */
	@Override
	public final List<ProductionType> getProductionTypes ()
	{
		return getProductionType ();
	}
	
	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public final ProductionType findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException
	{
		final ProductionType found = productionTypesMap.get (productionTypeID);
		if (found == null)
			throw new RecordNotFoundException (ProductionType.class, productionTypeID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all pick types in game
	 */
	@Override
	public final List<PickType> getPickTypes ()
	{
		return getPickType ();
	}

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	@Override
	public final PickType findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException
	{
		final PickType found = pickTypesMap.get (pickTypeID);
		if (found == null)
			throw new RecordNotFoundException (PickType.class, pickTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all picks in game
	 */
	@Override
	public final List<Pick> getPicks ()
	{
		return getPick ();
	}
	
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	@Override
	public final Pick findPick (final String pickID, final String caller) throws RecordNotFoundException
	{
		final Pick found = picksMap.get (pickID);
		if (found == null)
			throw new RecordNotFoundException (Pick.class, pickID, caller);

		return found;
	}

	/**
	 * @return Complete list of all wizards in game
	 */
	@Override
	public final List<Wizard> getWizards ()
	{
		return getWizard ();
	}
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public final Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException
	{
		final Wizard found = wizardsMap.get (wizardID);
		if (found == null)
			throw new RecordNotFoundException (Wizard.class, wizardID, caller);

		return found;
	}

	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	@Override
	public final UnitType findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException
	{
		final UnitType found = unitTypesMap.get (unitTypeID);
		if (found == null)
			throw new RecordNotFoundException (UnitType.class, unitTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all units in game
	 */
	@Override
	public final List<Unit> getUnits ()
	{
		return getUnit ();
	}
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final Unit findUnit (final String unitID, final String caller) throws RecordNotFoundException
	{
		final Unit found = unitsMap.get (unitID);
		if (found == null)
			throw new RecordNotFoundException (Unit.class, unitID, caller);

		return found;
	}

	/**
	 * @return Complete list of all unit skills in game
	 */
	@Override
	public final List<UnitSkill> getUnitSkills ()
	{
		return getUnitSkill ();
	}
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	@Override
	public final UnitSkill findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException
	{
		final UnitSkill found = unitSkillsMap.get (unitSkillID);
		if (found == null)
			throw new RecordNotFoundException (UnitSkill.class, unitSkillID, caller);

		return found;
	}

	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weapon grade number doesn't exist
	 */
	@Override
	public final WeaponGrade findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException
	{
		final WeaponGrade found = weaponGradesMap.get (weaponGradeNumber);
		if (found == null)
			throw new RecordNotFoundException (WeaponGrade.class, weaponGradeNumber, caller);

		return found;
	}

	/**
	 * @param rangedAttackTypeID RAT ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return RAT object
	 * @throws RecordNotFoundException If the RAT ID doesn't exist
	 */
	@Override
	public final RangedAttackType findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException
	{
		final RangedAttackType found = rangedAttackTypesMap.get (rangedAttackTypeID);
		if (found == null)
			throw new RecordNotFoundException (RangedAttackType.class, rangedAttackTypeID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all races in game
	 */
	@Override
	public final List<Race> getRaces ()
	{
		return getRace ();
	}
	
	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	@Override
	public final Race findRace (final String raceID, final String caller) throws RecordNotFoundException
	{
		final Race found = racesMap.get (raceID);
		if (found == null)
			throw new RecordNotFoundException (Race.class, raceID, caller);

		return found;
	}

	/**
	 * @param taxRateID Tax rate ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tax rate object
	 * @throws RecordNotFoundException If the tax rateID doesn't exist
	 */
	@Override
	public final TaxRate findTaxRate (final String taxRateID, final String caller) throws RecordNotFoundException
	{
		final TaxRate found = taxRatesMap.get (taxRateID);
		if (found == null)
			throw new RecordNotFoundException (TaxRate.class, taxRateID, caller);

		return found;
	}

	/**
	 * @return Complete list of all buildings in game
	 */
	@Override
	public final List<Building> getBuildings ()
	{
		return getBuilding ();
	}
	
	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public final Building findBuilding (final String buildingID, final String caller) throws RecordNotFoundException
	{
		final Building found = buildingsMap.get (buildingID);
		if (found == null)
			throw new RecordNotFoundException (Building.class, buildingID, caller);

		return found;
	}

	/**
	 * @return Complete list of all spells in game
	 */
	@Override
	public final List<Spell> getSpells ()
	{
		return getSpell ();
	}
	
	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	@Override
	public final Spell findSpell (final String spellID, final String caller) throws RecordNotFoundException
	{
		final Spell found = spellsMap.get (spellID);
		if (found == null)
			throw new RecordNotFoundException (Spell.class, spellID, caller);

		return found;
	}

	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatAreaEffect object
	 * @throws RecordNotFoundException If the combat area effect ID doesn't exist
	 */
	@Override
	public final CombatAreaEffect findCombatAreaEffect (final String combatAreaEffectID, final String caller) throws RecordNotFoundException
	{
		final CombatAreaEffect found = combatAreaEffectsMap.get (combatAreaEffectID);
		if (found == null)
			throw new RecordNotFoundException (CombatAreaEffect.class, combatAreaEffectID, caller);

		return found;
	}

	/**
	 * @param combatTileTypeID Combat tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileType object
	 * @throws RecordNotFoundException If the combat tile type ID doesn't exist
	 */
	@Override
	public final CombatTileType findCombatTileType (final String combatTileTypeID, final String caller) throws RecordNotFoundException
	{
		final CombatTileType found = combatTileTypesMap.get (combatTileTypeID);
		if (found == null)
			throw new RecordNotFoundException (CombatTileType.class, combatTileTypeID, caller);

		return found;
	}

	/**
	 * @param combatTileBorderID Combat tile border ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileBorder object
	 * @throws RecordNotFoundException If the combat tile border ID doesn't exist
	 */
	@Override
	public final CombatTileBorder findCombatTileBorder (final String combatTileBorderID, final String caller) throws RecordNotFoundException
	{
		final CombatTileBorder found = combatTileBordersMap.get (combatTileBorderID);
		if (found == null)
			throw new RecordNotFoundException (CombatTileBorder.class, combatTileBorderID, caller);

		return found;
	}

	/**
	 * @param heroItemSlotTypeID Hero item slot type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemSlotType object
	 * @throws RecordNotFoundException If the hero item slot type ID doesn't exist
	 */
	@Override
	public final HeroItemSlotType findHeroItemSlotType (final String heroItemSlotTypeID, final String caller) throws RecordNotFoundException
	{
		final HeroItemSlotType found = heroItemSlotTypesMap.get (heroItemSlotTypeID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemSlotType.class, heroItemSlotTypeID, caller);

		return found;
	}
	
	/**
	 * @param heroItemTypeID Hero item type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemType object
	 * @throws RecordNotFoundException If the hero item type ID doesn't exist
	 */
	@Override
	public final HeroItemType findHeroItemType (final String heroItemTypeID, final String caller) throws RecordNotFoundException
	{
		final HeroItemType found = heroItemTypesMap.get (heroItemTypeID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemType.class, heroItemTypeID, caller);

		return found;
	}

	/**
	 * @param heroItemBonusID Hero item bonus ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return HeroItemBonus object
	 * @throws RecordNotFoundException If the hero item bonus ID doesn't exist
	 */
	@Override
	public final HeroItemBonus findHeroItemBonus (final String heroItemBonusID, final String caller) throws RecordNotFoundException
	{
		final HeroItemBonus found = heroItemBonusesMap.get (heroItemBonusID);
		if (found == null)
			throw new RecordNotFoundException (HeroItemBonus.class, heroItemBonusID, caller);

		return found;
	}

	/**
	 * @param damageTypeID Damage type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return DamageType object
	 * @throws RecordNotFoundException If the damage type ID doesn't exist
	 */
	@Override
	public final DamageType findDamageType (final String damageTypeID, final String caller) throws RecordNotFoundException
	{
		final DamageType found = damageTypesMap.get (damageTypeID);
		if (found == null)
			throw new RecordNotFoundException (DamageType.class, damageTypeID, caller);

		return found;
	}
	
	/**
	 * @return Cost to construct the most expensive unit or building in the database
	 */
	@Override
	public final int getMostExpensiveConstructionCost ()
	{
		return mostExpensiveConstructionCost;
	}
}
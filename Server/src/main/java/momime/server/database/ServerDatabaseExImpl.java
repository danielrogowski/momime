package momime.server.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TaxRate;
import momime.server.database.v0_9_7.Building;
import momime.server.database.v0_9_7.CitySpellEffect;
import momime.server.database.v0_9_7.CombatAreaEffect;
import momime.server.database.v0_9_7.CombatTileBorder;
import momime.server.database.v0_9_7.CombatTileType;
import momime.server.database.v0_9_7.DamageType;
import momime.server.database.v0_9_7.HeroItemBonus;
import momime.server.database.v0_9_7.HeroItemSlotType;
import momime.server.database.v0_9_7.HeroItemType;
import momime.server.database.v0_9_7.MapFeature;
import momime.server.database.v0_9_7.Pick;
import momime.server.database.v0_9_7.PickType;
import momime.server.database.v0_9_7.Plane;
import momime.server.database.v0_9_7.ProductionType;
import momime.server.database.v0_9_7.Race;
import momime.server.database.v0_9_7.RangedAttackType;
import momime.server.database.v0_9_7.ServerDatabase;
import momime.server.database.v0_9_7.Spell;
import momime.server.database.v0_9_7.TileType;
import momime.server.database.v0_9_7.Unit;
import momime.server.database.v0_9_7.UnitSkill;
import momime.server.database.v0_9_7.UnitType;
import momime.server.database.v0_9_7.WeaponGrade;
import momime.server.database.v0_9_7.Wizard;
import momime.server.utils.UnitSkillDirectAccess;

/**
 * Adds maps for faster key lookups over the server-side database read in via JAXB
 */
public final class ServerDatabaseExImpl extends ServerDatabase implements ServerDatabaseEx
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ServerDatabaseExImpl.class);
	
	/** Unit skill values direct access */
	private UnitSkillDirectAccess unitSkillDirectAccess;
	
	/** Map of plane numbers to plane XML objects */
	private Map<Integer, Plane> planesMap;

	/** Map of map feature IDs to map feature XML objects */
	private Map<String, MapFeatureSvr> mapFeaturesMap;

	/** Map of tile type IDs to file type XML objects */
	private Map<String, TileTypeSvr> tileTypesMap;

	/** Map of production type IDs to production type XML objects */
	private Map<String, ProductionTypeSvr> productionTypesMap;

	/** Map of pick type IDs to pick XML objects */
	private Map<String, PickTypeSvr> pickTypesMap;

	/** Map of pick IDs to pick XML objects */
	private Map<String, PickSvr> picksMap;

	/** Map of wizard IDs to wizard XML objects */
	private Map<String, WizardSvr> wizardsMap;

	/** Map of unit type IDs to unit type XML objects */
	private Map<String, UnitTypeSvr> unitTypesMap;

	/** Map of unit IDs to unit XML objects */
	private Map<String, UnitSvr> unitsMap;

	/** Map of unit skill IDs to unit skill XML objects */
	private Map<String, UnitSkillSvr> unitSkillsMap;

	/** Map of weapon grade numbers to weapon grade XML objects */
	private Map<Integer, WeaponGradeSvr> weaponGradesMap;

	/** Map of ranged attack type IDs to ranged attack type XML objects */
	private Map<String, RangedAttackTypeSvr> rangedAttackTypesMap;
	
	/** Map of race IDs to race XML objects */
	private Map<String, RaceSvr> racesMap;

	/** Map of tax rate IDs to tax rate XML objects */
	private Map<String, TaxRate> taxRatesMap;

	/** Map of building IDs to building XML objects */
	private Map<String, BuildingSvr> buildingsMap;

	/** Map of spell IDs to spell XML objects */
	private Map<String, SpellSvr> spellsMap;

	/** Map of combat area effect IDs to combat area effect objects */
	private Map<String, CombatAreaEffect> combatAreaEffectsMap;
	
	/** Map of combat tile type IDs to combat tile type objects */
	private Map<String, CombatTileTypeSvr> combatTileTypesMap;

	/** Map of combat tile border IDs to combat tile border objects */
	private Map<String, CombatTileBorder> combatTileBordersMap;
	
	/** Map of city spell effect IDs to city spell effect objects */
	private Map<String, CitySpellEffectSvr> citySpellEffectsMap;
	
	/** Map of hero item slot type IDs to hero item slot type objects */
	private Map<String, HeroItemSlotType> heroItemSlotTypesMap;

	/** Map of hero item type IDs to hero item type objects */
	private Map<String, HeroItemType> heroItemTypesMap;

	/** Map of hero item bonus IDs to hero item bonus objects */
	private Map<String, HeroItemBonusSvr> heroItemBonusesMap;
	
	/** Map of damage type IDs to damage type objects */
	private Map<String, DamageTypeSvr> damageTypesMap;
	
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
		mapFeaturesMap = new HashMap<String, MapFeatureSvr> ();
		for (final MapFeature thisMapFeature : getMapFeature ())
			mapFeaturesMap.put (thisMapFeature.getMapFeatureID (), (MapFeatureSvr) thisMapFeature);

		// Create tile types map
		tileTypesMap = new HashMap<String, TileTypeSvr> ();
		for (final TileType thisTileType : getTileType ())
			tileTypesMap.put (thisTileType.getTileTypeID (), (TileTypeSvr) thisTileType);

		// Create production types map
		productionTypesMap = new HashMap<String, ProductionTypeSvr> ();
		for (final ProductionType thisProductionType : getProductionType ())
			productionTypesMap.put (thisProductionType.getProductionTypeID (), (ProductionTypeSvr) thisProductionType);

		// Create pick types map
		pickTypesMap = new HashMap<String, PickTypeSvr> ();
		for (final PickType thisPickType : getPickType ())
			pickTypesMap.put (thisPickType.getPickTypeID (), (PickTypeSvr) thisPickType);

		// Create picks map
		picksMap = new HashMap<String, PickSvr> ();
		for (final Pick thisPick : getPick ())
			picksMap.put (thisPick.getPickID (), (PickSvr) thisPick);

		// Create wizards map
		wizardsMap = new HashMap<String, WizardSvr> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), (WizardSvr) thisWizard);

		// Create unit types map
		unitTypesMap = new HashMap<String, UnitTypeSvr> ();
		for (final UnitType thisUnitType : getUnitType ())
			unitTypesMap.put (thisUnitType.getUnitTypeID (), (UnitTypeSvr) thisUnitType);

		// Create units map
		unitsMap = new HashMap<String, UnitSvr> ();
		for (final Unit thisUnit : getUnit ())
			unitsMap.put (thisUnit.getUnitID (), (UnitSvr) thisUnit);

		// Create unit skills map
		unitSkillsMap = new HashMap<String, UnitSkillSvr> ();
		for (final UnitSkill thisUnitSkill : getUnitSkill ())
			unitSkillsMap.put (thisUnitSkill.getUnitSkillID (), (UnitSkillSvr) thisUnitSkill);

		// Create weaponGrades map
		weaponGradesMap = new HashMap<Integer, WeaponGradeSvr> ();
		for (final WeaponGrade thisWeaponGrade : getWeaponGrade ())
			weaponGradesMap.put (thisWeaponGrade.getWeaponGradeNumber (), (WeaponGradeSvr) thisWeaponGrade);

		// Create rangedAttackTypes map
		rangedAttackTypesMap = new HashMap<String, RangedAttackTypeSvr> ();
		for (final RangedAttackType thisRangedAttackType : getRangedAttackType ())
			rangedAttackTypesMap.put (thisRangedAttackType.getRangedAttackTypeID (), (RangedAttackTypeSvr) thisRangedAttackType);
		
		// Create races map
		racesMap = new HashMap<String, RaceSvr> ();
		for (final Race thisRace : getRace ())
			racesMap.put (thisRace.getRaceID (), (RaceSvr) thisRace);

		// Create tax rates map
		taxRatesMap = new HashMap<String, TaxRate> ();
		for (final TaxRate thisTaxRate : getTaxRate ())
			taxRatesMap.put (thisTaxRate.getTaxRateID (), thisTaxRate);

		// Create buildings map
		buildingsMap = new HashMap<String, BuildingSvr> ();
		for (final Building thisBuilding : getBuilding ())
			buildingsMap.put (thisBuilding.getBuildingID (), (BuildingSvr) thisBuilding);

		// Create spells map
		spellsMap = new HashMap<String, SpellSvr> ();
		for (final Spell thisSpell : getSpell ())
			spellsMap.put (thisSpell.getSpellID (), (SpellSvr) thisSpell);

		// Create combat area effects map
		combatAreaEffectsMap = new HashMap<String, CombatAreaEffect> ();
		for (final CombatAreaEffect thisCombatAreaEffect : getCombatAreaEffect ())
			combatAreaEffectsMap.put (thisCombatAreaEffect.getCombatAreaEffectID (), thisCombatAreaEffect);

		// Combat tile types map
		combatTileTypesMap = new HashMap<String, CombatTileTypeSvr> ();
		for (final CombatTileType thisCombatTileType : getCombatTileType ())
			combatTileTypesMap.put (thisCombatTileType.getCombatTileTypeID (), (CombatTileTypeSvr) thisCombatTileType);

		// Combat tile borders map
		combatTileBordersMap = new HashMap<String, CombatTileBorder> ();
		for (final CombatTileBorder thisCombatTileBorder : getCombatTileBorder ())
			combatTileBordersMap.put (thisCombatTileBorder.getCombatTileBorderID (), thisCombatTileBorder);
		
		// City spell effects map
		citySpellEffectsMap = new HashMap<String, CitySpellEffectSvr> ();
		for (final CitySpellEffect thisCitySpellEffect : getCitySpellEffect ())
			citySpellEffectsMap.put (thisCitySpellEffect.getCitySpellEffectID (), (CitySpellEffectSvr) thisCitySpellEffect);
		
		// Create hero item slot types map
		heroItemSlotTypesMap = new HashMap<String, HeroItemSlotType> ();
		for (final HeroItemSlotType thisHeroItemSlotType : getHeroItemSlotType ())
			heroItemSlotTypesMap.put (thisHeroItemSlotType.getHeroItemSlotTypeID (), thisHeroItemSlotType);

		// Create hero item types map
		heroItemTypesMap = new HashMap<String, HeroItemType> ();
		for (final HeroItemType thisHeroItemType : getHeroItemType ())
			heroItemTypesMap.put (thisHeroItemType.getHeroItemTypeID (), thisHeroItemType);

		// Create hero item bonuses map
		heroItemBonusesMap = new HashMap<String, HeroItemBonusSvr> ();
		for (final HeroItemBonus thisHeroItemBonus : getHeroItemBonus ())
			heroItemBonusesMap.put (thisHeroItemBonus.getHeroItemBonusID (), (HeroItemBonusSvr) thisHeroItemBonus);
		
		// Create damage types map
		damageTypesMap = new HashMap<String, DamageTypeSvr> ();
		for (final DamageType thisDamageType : getDamageType ())
			damageTypesMap.put (thisDamageType.getDamageTypeID (), (DamageTypeSvr) thisDamageType);
		
		log.trace ("Exiting buildMaps");
	}

	/**
	 * Derives values from the received database
	 * @throws MomException If any of the consistency checks fail
	 */
	public final void consistencyChecks () throws MomException
	{
		log.trace ("Entering consistencyChecks");
		
		// Check all units have an HP and double movement speed "skill" value defined
		for (final Unit unitDef : getUnit ())
		{
			if (getUnitSkillDirectAccess ().getDirectSkillValue (unitDef.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS) < 1)
				throw new MomException ("Unit " + unitDef.getUnitID () + " has no HP value defined");
			
			if (getUnitSkillDirectAccess ().getDirectSkillValue (unitDef.getUnitHasSkill (), CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE) < 1)
				throw new MomException ("Unit " + unitDef.getUnitID () + " has no resistance value defined");
			
			if (getUnitSkillDirectAccess ().getDirectSkillValue (unitDef.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED) < 1)
				throw new MomException ("Unit " + unitDef.getUnitID () + " has no movement speed defined");
		}

		log.trace ("Exiting consistencyChecks");
	}
	
	/**
	 * @return Complete list of all city sizes in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<CitySizeSvr> getCitySizes ()
	{
		return (List<CitySizeSvr>) (List<?>) getCitySize ();
	}
	
	/**
	 * @return Complete list of all planes in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<PlaneSvr> getPlanes ()
	{
		return (List<PlaneSvr>) (List<?>) getPlane ();
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
	@SuppressWarnings ("unchecked")
	public final List<MapFeatureSvr> getMapFeatures ()
	{
		return (List<MapFeatureSvr>) (List<?>) getMapFeature ();
	}
	
	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public final MapFeatureSvr findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException
	{
		final MapFeatureSvr found = mapFeaturesMap.get (mapFeatureID);
		if (found == null)
			throw new RecordNotFoundException (MapFeature.class, mapFeatureID, caller);

		return found;
	}

	/**
	 * @return Complete list of all tile types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<TileTypeSvr> getTileTypes ()
	{
		return (List<TileTypeSvr>) (List<?>) getTileType ();
	}
	
	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	@Override
	public final TileTypeSvr findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException
	{
		final TileTypeSvr found = tileTypesMap.get (tileTypeID);
		if (found == null)
			throw new RecordNotFoundException (TileType.class, tileTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all production types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<ProductionTypeSvr> getProductionTypes ()
	{
		return (List<ProductionTypeSvr>) (List<?>) getProductionType ();
	}
	
	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public final ProductionTypeSvr findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException
	{
		final ProductionTypeSvr found = productionTypesMap.get (productionTypeID);
		if (found == null)
			throw new RecordNotFoundException (ProductionType.class, productionTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all pick types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<PickTypeSvr> getPickTypes ()
	{
		return (List<PickTypeSvr>) (List<?>) getPickType ();
	}
	
	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	@Override
	public final PickTypeSvr findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException
	{
		final PickTypeSvr found = pickTypesMap.get (pickTypeID);
		if (found == null)
			throw new RecordNotFoundException (PickType.class, pickTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all picks in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<PickSvr> getPicks ()
	{
		return (List<PickSvr>) (List<?>) getPick ();
	}
	
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	@Override
	public final PickSvr findPick (final String pickID, final String caller) throws RecordNotFoundException
	{
		final PickSvr found = picksMap.get (pickID);
		if (found == null)
			throw new RecordNotFoundException (Pick.class, pickID, caller);

		return found;
	}

	/**
	 * @return Complete list of all wizards in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<WizardSvr> getWizards ()
	{
		return (List<WizardSvr>) (List<?>) getWizard ();
	}
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public final WizardSvr findWizard (final String wizardID, final String caller) throws RecordNotFoundException
	{
		final WizardSvr found = wizardsMap.get (wizardID);
		if (found == null)
			throw new RecordNotFoundException (Wizard.class, wizardID, caller);

		return found;
	}

	/**
	 * @return Complete list of all unit types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<UnitTypeSvr> getUnitTypes ()
	{
		return (List<UnitTypeSvr>) (List<?>) getUnitType ();
	}
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	@Override
	public final UnitTypeSvr findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException
	{
		final UnitTypeSvr found = unitTypesMap.get (unitTypeID);
		if (found == null)
			throw new RecordNotFoundException (UnitType.class, unitTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all units in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<UnitSvr> getUnits ()
	{
		return (List<UnitSvr>) (List<?>) getUnit ();
	}
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final UnitSvr findUnit (final String unitID, final String caller) throws RecordNotFoundException
	{
		final UnitSvr found = unitsMap.get (unitID);
		if (found == null)
			throw new RecordNotFoundException (Unit.class, unitID, caller);

		return found;
	}

	/**
	 * @return Complete list of all unit skills in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<UnitSkillSvr> getUnitSkills ()
	{
		return (List<UnitSkillSvr>) (List<?>) getUnitSkill ();
	}
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	@Override
	public final UnitSkillSvr findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException
	{
		final UnitSkillSvr found = unitSkillsMap.get (unitSkillID);
		if (found == null)
			throw new RecordNotFoundException (UnitSkill.class, unitSkillID, caller);

		return found;
	}

	/**
	 * @return Complete list of all weapon grades in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<WeaponGradeSvr> getWeaponGrades ()
	{
		return (List<WeaponGradeSvr>) (List<?>) getWeaponGrade ();
	}
	
	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weapon grade number doesn't exist
	 */
	@Override
	public final WeaponGradeSvr findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException
	{
		final WeaponGradeSvr found = weaponGradesMap.get (weaponGradeNumber);
		if (found == null)
			throw new RecordNotFoundException (WeaponGrade.class, weaponGradeNumber, caller);

		return found;
	}

	/**
	 * @return Complete list of all ranged attack types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<RangedAttackTypeSvr> getRangedAttackTypes ()
	{
		return (List<RangedAttackTypeSvr>) (List<?>) getRangedAttackType ();
	}
	
	/**
	 * @param rangedAttackTypeID RAT ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return RAT object
	 * @throws RecordNotFoundException If the RAT ID doesn't exist
	 */
	@Override
	public final RangedAttackTypeSvr findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException
	{
		final RangedAttackTypeSvr found = rangedAttackTypesMap.get (rangedAttackTypeID);
		if (found == null)
			throw new RecordNotFoundException (RangedAttackType.class, rangedAttackTypeID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all races in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<RaceSvr> getRaces ()
	{
		return (List<RaceSvr>) (List<?>) getRace ();
	}
	
	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	@Override
	public final RaceSvr findRace (final String raceID, final String caller) throws RecordNotFoundException
	{
		final RaceSvr found = racesMap.get (raceID);
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
	@SuppressWarnings ("unchecked")
	public final List<BuildingSvr> getBuildings ()
	{
		return (List<BuildingSvr>) (List<?>) getBuilding ();
	}
	
	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public final BuildingSvr findBuilding (final String buildingID, final String caller) throws RecordNotFoundException
	{
		final BuildingSvr found = buildingsMap.get (buildingID);
		if (found == null)
			throw new RecordNotFoundException (Building.class, buildingID, caller);

		return found;
	}

	/**
	 * @return Complete list of all spells in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<SpellSvr> getSpells ()
	{
		return (List<SpellSvr>) (List<?>) getSpell ();
	}
	
	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	@Override
	public final SpellSvr findSpell (final String spellID, final String caller) throws RecordNotFoundException
	{
		final SpellSvr found = spellsMap.get (spellID);
		if (found == null)
			throw new RecordNotFoundException (Spell.class, spellID, caller);

		return found;
	}

	/**
	 * @return Complete list of all CAEs in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<CombatAreaEffectSvr> getCombatAreaEffects ()
	{
		return (List<CombatAreaEffectSvr>) (List<?>) getCombatAreaEffect ();
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
	 * @return Complete list of all combat tile types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<CombatTileTypeSvr> getCombatTileTypes ()
	{
		return (List<CombatTileTypeSvr>) (List<?>) getCombatTileType ();
	}
	
	/**
	 * @param combatTileTypeID Combat tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatTileType object
	 * @throws RecordNotFoundException If the combat tile type ID doesn't exist
	 */
	@Override
	public final CombatTileTypeSvr findCombatTileType (final String combatTileTypeID, final String caller) throws RecordNotFoundException
	{
		final CombatTileTypeSvr found = combatTileTypesMap.get (combatTileTypeID);
		if (found == null)
			throw new RecordNotFoundException (CombatTileType.class, combatTileTypeID, caller);

		return found;
	}

	/**
	 * @return Complete list of all combat tile borders in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<CombatTileBorderSvr> getCombatTileBorders ()
	{
		return (List<CombatTileBorderSvr>) (List<?>) getCombatTileBorder ();
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
	 * @param citySpellEffectID City spell effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CitySpellEffect object
	 * @throws RecordNotFoundException If the city spell effect ID doesn't exist
	 */
	@Override
	public final CitySpellEffectSvr findCitySpellEffect (final String citySpellEffectID, final String caller) throws RecordNotFoundException
	{
		final CitySpellEffectSvr found = citySpellEffectsMap.get (citySpellEffectID);
		if (found == null)
			throw new RecordNotFoundException (CitySpellEffect.class, citySpellEffectID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all combat map elements in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<CombatMapElementSvr> getCombatMapElements ()
	{
		return (List<CombatMapElementSvr>) (List<?>) getCombatMapElement ();
	}

	/**
	 * @return Complete list of all pre-defined overland map sizes
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<OverlandMapSizeSvr> getOverlandMapSizes ()
	{
		return (List<OverlandMapSizeSvr>) (List<?>) getOverlandMapSize ();
	}
	
	/**
	 * @return Complete list of all pre-defined land proportions
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<LandProportionSvr> getLandProportions ()
	{
		return (List<LandProportionSvr>) (List<?>) getLandProportion ();
	}

	/**
	 * @return Complete list of all pre-defined node strengths
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<NodeStrengthSvr> getNodeStrengths ()
	{
		return (List<NodeStrengthSvr>) (List<?>) getNodeStrength ();
	}
	
	/**
	 * @return Complete list of all pre-defined difficulty levels
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<DifficultyLevelSvr> getDifficultyLevels ()
	{
		return (List<DifficultyLevelSvr>) (List<?>) getDifficultyLevel ();
	}

	/**
	 * @return Complete list of all pre-defined fog of war settings
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<FogOfWarSettingSvr> getFogOfWarSettings ()
	{
		return (List<FogOfWarSettingSvr>) (List<?>) getFogOfWarSetting ();
	}
	
	/**
	 * @return Complete list of all pre-defined unit settings
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<UnitSettingSvr> getUnitSettings ()
	{
		return (List<UnitSettingSvr>) (List<?>) getUnitSetting ();
	}
	
	/**
	 * @return Complete list of all pre-defined spell settings
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<SpellSettingSvr> getSpellSettings ()
	{
		return (List<SpellSettingSvr>) (List<?>) getSpellSetting ();
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
	public final HeroItemBonusSvr findHeroItemBonus (final String heroItemBonusID, final String caller) throws RecordNotFoundException
	{
		final HeroItemBonusSvr found = heroItemBonusesMap.get (heroItemBonusID);
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
	public final DamageTypeSvr findDamageType (final String damageTypeID, final String caller) throws RecordNotFoundException
	{
		final DamageTypeSvr found = damageTypesMap.get (damageTypeID);
		if (found == null)
			throw new RecordNotFoundException (DamageType.class, damageTypeID, caller);

		return found;
	}
	
	/**
	 * @return Complete list of all spell ranks in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<SpellRankSvr> getSpellRanks ()
	{
		return (List<SpellRankSvr>) (List<?>) getSpellRank ();
	}
	
	/**
	 * @return Complete list of all damage types in game
	 */
	@Override
	@SuppressWarnings ("unchecked")
	public final List<DamageTypeSvr> getDamageTypes ()
	{
		return (List<DamageTypeSvr>) (List<?>) getDamageType ();
	}
	
	/** 
	 * @return Unit skill values direct access
	 */
	public final UnitSkillDirectAccess getUnitSkillDirectAccess ()
	{
		return unitSkillDirectAccess;
	}

	/**
	 * @param direct Unit skill values direct access
	 */
	public final void setUnitSkillDirectAccess (final UnitSkillDirectAccess direct)
	{
		unitSkillDirectAccess = direct;
	}
}
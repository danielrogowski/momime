package momime.common.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import momime.common.database.v0_9_4.Building;
import momime.common.database.v0_9_4.CombatAreaEffect;
import momime.common.database.v0_9_4.MapFeature;
import momime.common.database.v0_9_4.Pick;
import momime.common.database.v0_9_4.PickType;
import momime.common.database.v0_9_4.Plane;
import momime.common.database.v0_9_4.ProductionType;
import momime.common.database.v0_9_4.Race;
import momime.common.database.v0_9_4.Spell;
import momime.common.database.v0_9_4.TaxRate;
import momime.common.database.v0_9_4.TileType;
import momime.common.database.v0_9_4.Unit;
import momime.common.database.v0_9_4.UnitMagicRealm;
import momime.common.database.v0_9_4.UnitSkill;
import momime.common.database.v0_9_4.UnitType;
import momime.common.database.v0_9_4.WeaponGrade;
import momime.common.database.v0_9_4.Wizard;

/**
 * The list classes generated from the XSD are inefficient for lookups - if we have over 216 spells, and have to find
 * spell SP200, then we have to check each one in turn until we find the right one
 *
 * We can't modify the classes generated from the XSD - instead, this is a wrapper around the generated classes
 * that provider speedier lookup functions
 */
public class CommonDatabaseLookup
{
	/** List of all available planes */
	private final List<? extends Plane> planesList;

	/** Map of plane numbers to plane XML objects */
	private final Map<Integer, Plane> planes;

	/** List of all available map features */
	private final List<? extends MapFeature> mapFeaturesList;

	/** Map of map feature IDs to map feature XML objects */
	private final Map<String, MapFeature> mapFeatures;

	/** List of all available tile types */
	private final List<? extends TileType> tileTypesList;

	/** Map of tile type IDs to file type XML objects */
	private final Map<String, TileType> tileTypes;

	/** List of all available production types */
	private final List<? extends ProductionType> productionTypesList;

	/** Map of production type IDs to production type XML objects */
	private final Map<String, ProductionType> productionTypes;

	/** List of all available pick types */
	private final List<? extends PickType> pickTypesList;

	/** Map of pick type IDs to pick XML objects */
	private final Map<String, PickType> pickTypes;

	/** Map of pick IDs to pick XML objects */
	private final Map<String, Pick> picks;

	/** List of all available wizards*/
	private final List<? extends Wizard> wizardsList;

	/** Map of wizard IDs to wizard XML objects */
	private final Map<String, Wizard> wizards;

	/** Map of unit type IDs to unit type XML objects */
	private final Map<String, UnitType> unitTypes;

	/** Map of unit magic realm IDs to unit magic realm XML objects */
	private final Map<String, UnitMagicRealm> unitMagicRealms;

	/** List of all available units */
	private final List<? extends Unit> unitsList;

	/** Map of unit IDs to unit XML objects */
	private final Map<String, Unit> units;

	/** List of all unit skills */
	private final List<? extends UnitSkill> unitSkillsList;

	/** Map of unit skill IDs to unit skill XML objects */
	private final Map<String, UnitSkill> unitSkills;

	/** Map of weapon grade numbers to weapon grade XML objects */
	private final Map<Integer, WeaponGrade> weaponGrades;

	/** List of all available races */
	private final List<? extends Race> racesList;

	/** Map of race IDs to race XML objects */
	private final Map<String, Race> races;

	/** Map of tax rate IDs to tax rate XML objects */
	private final Map<String, TaxRate> taxRates;

	/** List of all available buildings */
	private final List<? extends Building> buildingsList;

	/** Map of building IDs to building XML objects */
	private final Map<String, Building> buildings;

	/** List of all available spells */
	private final List<? extends Spell> spellsList;

	/** Map of spell IDs to spell XML objects */
	private final Map<String, Spell> spells;

	/** Map of combat area effect IDs to combat area effect objects */
	private final Map<String, CombatAreaEffect> combatAreaEffects;

	/**
	 * @param aPlanesList List of planes loaded from XML
	 * @param aMapFeaturesList List of map features loaded from XML
	 * @param aTileTypesList List of tile types loaded from XML
	 * @param aProductionTypesList List of production types loaded from XML
	 * @param aPickTypesList List of pick types loaded from XML
	 * @param picksList List of picks loaded from XML
	 * @param aWizardsList List of wizards loaded from XML
	 * @param unitTypesList List of units types loaded from XML
	 * @param unitMagicRealmsList List of unit magic realms loaded from XML
	 * @param aUnitsList List of units loaded from XML
	 * @param aUnitSkillsList List of unit skills loaded from XML
	 * @param weaponGradesList List of weapon grades loaded from XML
	 * @param aRacesList List of races loaded from XML
	 * @param taxRatesList List of tax rates loaded from XML
	 * @param aBuildingsList List of buildings loaded from XML
	 * @param aSpellsList List of spells loaded from XML
	 * @param combatAreaEffectsList List of combat area effects loaded from XML
	 */
	public CommonDatabaseLookup (final List<? extends Plane> aPlanesList, final List<? extends MapFeature> aMapFeaturesList, final List<? extends TileType> aTileTypesList,
		final List<? extends ProductionType> aProductionTypesList, final List<? extends PickType> aPickTypesList, final List<? extends Pick> picksList,
		final List<? extends Wizard> aWizardsList, final List<? extends UnitType> unitTypesList, final List<? extends UnitMagicRealm> unitMagicRealmsList,
		final List<? extends Unit> aUnitsList, final List<? extends UnitSkill> aUnitSkillsList, final List<? extends WeaponGrade> weaponGradesList,
		final List<? extends Race> aRacesList, final List<TaxRate> taxRatesList, final List<? extends Building> aBuildingsList,
		final List<? extends Spell> aSpellsList, final List<? extends CombatAreaEffect> combatAreaEffectsList)
	{
		super ();

		// Create planes map
		planesList = aPlanesList;
		planes = new HashMap<Integer, Plane> ();
		if (planesList != null)
			for (final Plane thisPlane : planesList)
				planes.put (thisPlane.getPlaneNumber (), thisPlane);

		// Create map features map
		mapFeaturesList = aMapFeaturesList;
		mapFeatures = new HashMap<String, MapFeature> ();
		if (mapFeaturesList != null)
			for (final MapFeature thismapFeature : mapFeaturesList)
				mapFeatures.put (thismapFeature.getMapFeatureID (), thismapFeature);

		// Create tile types map
		tileTypesList = aTileTypesList;
		tileTypes = new HashMap<String, TileType> ();
		if (tileTypesList != null)
			for (final TileType thistileType : tileTypesList)
				tileTypes.put (thistileType.getTileTypeID (), thistileType);

		// Create production types map
		productionTypesList = aProductionTypesList;
		productionTypes = new HashMap<String, ProductionType> ();
		if (productionTypesList != null)
			for (final ProductionType thisproductionType : productionTypesList)
				productionTypes.put (thisproductionType.getProductionTypeID (), thisproductionType);

		// Create pick types map
		pickTypesList = aPickTypesList;
		pickTypes = new HashMap<String, PickType> ();
		if (pickTypesList != null)
			for (final PickType thisPickType : pickTypesList)
				pickTypes.put (thisPickType.getPickTypeID (), thisPickType);

		// Create picks map
		picks = new HashMap<String, Pick> ();
		if (picksList != null)
			for (final Pick thisPick : picksList)
				picks.put (thisPick.getPickID (), thisPick);

		// Create wizards map
		wizardsList = aWizardsList;
		wizards = new HashMap<String, Wizard> ();
		if (wizardsList != null)
			for (final Wizard thisWizard : wizardsList)
				wizards.put (thisWizard.getWizardID (), thisWizard);

		// Create unit types map
		unitTypes = new HashMap<String, UnitType> ();
		if (unitTypesList != null)
			for (final UnitType thisUnitType : unitTypesList)
				unitTypes.put (thisUnitType.getUnitTypeID (), thisUnitType);

		// Create unit magic realms map
		unitMagicRealms = new HashMap<String, UnitMagicRealm> ();
		if (unitMagicRealmsList != null)
			for (final UnitMagicRealm thisUnitMagicRealm : unitMagicRealmsList)
				unitMagicRealms.put (thisUnitMagicRealm.getUnitMagicRealmID (), thisUnitMagicRealm);

		// Create units map
		unitsList = aUnitsList;
		units = new HashMap<String, Unit> ();
		if (unitsList != null)
			for (final Unit thisUnit : unitsList)
				units.put (thisUnit.getUnitID (), thisUnit);

		// Create unit skills map
		unitSkillsList = aUnitSkillsList;
		unitSkills = new HashMap<String, UnitSkill> ();
		if (unitSkillsList != null)
			for (final UnitSkill thisUnitSkill : unitSkillsList)
				unitSkills.put (thisUnitSkill.getUnitSkillID (), thisUnitSkill);

		// Create weaponGrades map
		weaponGrades = new HashMap<Integer, WeaponGrade> ();
		if (weaponGradesList != null)
			for (final WeaponGrade thisWeaponGrade : weaponGradesList)
				weaponGrades.put (thisWeaponGrade.getWeaponGradeNumber (), thisWeaponGrade);

		// Create races map
		racesList = aRacesList;
		races = new HashMap<String, Race> ();
		if (racesList != null)
			for (final Race thisRace : racesList)
				races.put (thisRace.getRaceID (), thisRace);

		// Create tax rates map
		taxRates = new HashMap<String, TaxRate> ();
		if (taxRatesList != null)
			for (final TaxRate thisTaxRate : taxRatesList)
				taxRates.put (thisTaxRate.getTaxRateID (), thisTaxRate);

		// Create buildings map
		buildingsList = aBuildingsList;
		buildings = new HashMap<String, Building> ();
		if (buildingsList != null)
			for (final Building thisBuilding : buildingsList)
				buildings.put (thisBuilding.getBuildingID (), thisBuilding);

		// Create spells map
		spellsList = aSpellsList;
		spells = new HashMap<String, Spell> ();
		if (spellsList != null)
			for (final Spell thisSpell : spellsList)
				spells.put (thisSpell.getSpellID (), thisSpell);

		// Create combat area effects map
		combatAreaEffects = new HashMap<String, CombatAreaEffect> ();
		if (combatAreaEffectsList != null)
			for (final CombatAreaEffect thisCombatAreaEffect : combatAreaEffectsList)
				combatAreaEffects.put (thisCombatAreaEffect.getCombatAreaEffectID (), thisCombatAreaEffect);
	}

	/**
	 * @return Complete list of all planes in game
	 */
	public List<? extends Plane> getPlanes ()
	{
		return planesList;
	}

	/**
	 * @param planeNumber Plane number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Plane object
	 * @throws RecordNotFoundException If the plane number doesn't exist
	 */
	public Plane findPlane (final int planeNumber, final String caller) throws RecordNotFoundException
	{
		final Plane plane = planes.get (planeNumber);
		if (plane == null)
			throw new RecordNotFoundException (Plane.class.getName (), planeNumber, caller);

		return plane;
	}

	/**
	 * @return Complete list of all map features in game
	 */
	public List<? extends MapFeature> getMapFeatures ()
	{
		return mapFeaturesList;
	}

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	public MapFeature findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException
	{
		final MapFeature mapFeature = mapFeatures.get (mapFeatureID);
		if (mapFeature == null)
			throw new RecordNotFoundException (MapFeature.class.getName (), mapFeatureID, caller);

		return mapFeature;
	}

	/**
	 * @return Complete list of all tile types in game
	 */
	public List<? extends TileType> getTileTypes ()
	{
		return tileTypesList;
	}

	/**
	 * @param tileTypeID Tile type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile type object
	 * @throws RecordNotFoundException If the tileTypeID doesn't exist
	 */
	public TileType findTileType (final String tileTypeID, final String caller) throws RecordNotFoundException
	{
		final TileType tileType = tileTypes.get (tileTypeID);
		if (tileType == null)
			throw new RecordNotFoundException (TileType.class.getName (), tileTypeID, caller);

		return tileType;
	}

	/**
	 * @return Complete list of all production types in game
	 */
	public List<? extends ProductionType> getProductionTypes ()
	{
		return productionTypesList;
	}

	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	public ProductionType findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException
	{
		final ProductionType productionType = productionTypes.get (productionTypeID);
		if (productionType == null)
			throw new RecordNotFoundException (ProductionType.class.getName (), productionTypeID, caller);

		return productionType;
	}

	/**
	 * @return Complete list of all pick types in game
	 */
	public final List<? extends PickType> getPickTypes ()
	{
		return pickTypesList;
	}

	/**
	 * @param pickTypeID Pick type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return PickType object
	 * @throws RecordNotFoundException If the pickTypeID doesn't exist
	 */
	public PickType findPickType (final String pickTypeID, final String caller) throws RecordNotFoundException
	{
		final PickType pickType = pickTypes.get (pickTypeID);
		if (pickType == null)
			throw new RecordNotFoundException (PickType.class.getName (), pickTypeID, caller);

		return pickType;
	}

	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	public Pick findPick (final String pickID, final String caller) throws RecordNotFoundException
	{
		final Pick pick = picks.get (pickID);
		if (pick == null)
			throw new RecordNotFoundException (Pick.class.getName (), pickID, caller);

		return pick;
	}

	/**
	 * @return Complete list of all wizards in game
	 */
	public List<? extends Wizard> getWizards ()
	{
		return wizardsList;
	}

	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	public Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException
	{
		final Wizard wizard = wizards.get (wizardID);
		if (wizard == null)
			throw new RecordNotFoundException (Wizard.class.getName (), wizardID, caller);

		return wizard;
	}

	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	public final UnitType findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException
	{
		final UnitType unitType = unitTypes.get (unitTypeID);
		if (unitType == null)
			throw new RecordNotFoundException (UnitType.class.getName (), unitTypeID, caller);

		return unitType;
	}

	/**
	 * @param unitMagicRealmID Unit magic realm ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit magic realm object
	 * @throws RecordNotFoundException If the unitMagicRealmID doesn't exist
	 */
	public final UnitMagicRealm findUnitMagicRealm (final String unitMagicRealmID, final String caller) throws RecordNotFoundException
	{
		final UnitMagicRealm unitMagicRealm = unitMagicRealms.get (unitMagicRealmID);
		if (unitMagicRealm == null)
			throw new RecordNotFoundException (UnitMagicRealm.class.getName (), unitMagicRealmID, caller);

		return unitMagicRealm;
	}

	/**
	 * @return Complete list of all units in game
	 */
	public List<? extends Unit> getUnits ()
	{
		return unitsList;
	}

	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public Unit findUnit (final String unitID, final String caller) throws RecordNotFoundException
	{
		final Unit unit = units.get (unitID);
		if (unit == null)
			throw new RecordNotFoundException (Unit.class.getName (), unitID, caller);

		return unit;
	}

	/**
	 * @return Complete list of all unit skills in game
	 */
	public List<? extends UnitSkill> getUnitSkills ()
	{
		return unitSkillsList;
	}

	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	public UnitSkill findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException
	{
		final UnitSkill unitSkill = unitSkills.get (unitSkillID);
		if (unitSkill == null)
			throw new RecordNotFoundException (UnitSkill.class.getName (), unitSkillID, caller);

		return unitSkill;
	}

	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weapon grade number doesn't exist
	 */
	public final WeaponGrade findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException
	{
		final WeaponGrade weaponGrade = weaponGrades.get (weaponGradeNumber);
		if (weaponGrade == null)
			throw new RecordNotFoundException (WeaponGrade.class.getName (), weaponGradeNumber, caller);

		return weaponGrade;
	}

	/**
	 * @return Complete list of all races in game
	 */
	public List<? extends Race> getRaces ()
	{
		return racesList;
	}

	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	public Race findRace (final String raceID, final String caller) throws RecordNotFoundException
	{
		final Race race = races.get (raceID);
		if (race == null)
			throw new RecordNotFoundException (Race.class.getName (), raceID, caller);

		return race;
	}

	/**
	 * @param taxRateID Tax rate ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tax rate object
	 * @throws RecordNotFoundException If the tax rateID doesn't exist
	 */
	public final TaxRate findTaxRate (final String taxRateID, final String caller) throws RecordNotFoundException
	{
		final TaxRate taxRate = taxRates.get (taxRateID);
		if (taxRate == null)
			throw new RecordNotFoundException (TaxRate.class.getName (), taxRateID, caller);

		return taxRate;
	}

	/**
	 * @return Complete list of all buildings in game
	 */
	public List<? extends Building> getBuildings ()
	{
		return buildingsList;
	}

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	public Building findBuilding (final String buildingID, final String caller) throws RecordNotFoundException
	{
		final Building building = buildings.get (buildingID);
		if (building == null)
			throw new RecordNotFoundException (Building.class.getName (), buildingID, caller);

		return building;
	}

	/**
	 * @return Complete list of all spells in game
	 */
	public final List<? extends Spell> getSpells ()
	{
		return spellsList;
	}

	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	public Spell findSpell (final String spellID, final String caller) throws RecordNotFoundException
	{
		final Spell spell = spells.get (spellID);
		if (spell == null)
			throw new RecordNotFoundException (Spell.class.getName (), spellID, caller);

		return spell;
	}

	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CombatAreaEffect object
	 * @throws RecordNotFoundException If the combat area effect ID doesn't exist
	 */
	public final CombatAreaEffect findCombatAreaEffect (final String combatAreaEffectID, final String caller) throws RecordNotFoundException
	{
		final CombatAreaEffect combatAreaEffect = combatAreaEffects.get (combatAreaEffectID);
		if (combatAreaEffect == null)
			throw new RecordNotFoundException (CombatAreaEffect.class.getName (), combatAreaEffectID, caller);

		return combatAreaEffect;
	}
}

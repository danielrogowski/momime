package momime.server.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import momime.common.database.CommonDatabaseLookup;
import momime.common.database.RecordNotFoundException;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.CitySize;
import momime.server.database.v0_9_4.MapFeature;
import momime.server.database.v0_9_4.MovementRateRule;
import momime.server.database.v0_9_4.Pick;
import momime.server.database.v0_9_4.PickType;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.ProductionType;
import momime.server.database.v0_9_4.Race;
import momime.server.database.v0_9_4.ServerDatabase;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.TileType;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.UnitSkill;
import momime.server.database.v0_9_4.Wizard;

/**
 * Adds client-side specific extensions to the common database lookup class
 */
public final class ServerDatabaseLookup extends CommonDatabaseLookup
{
	/** List of all available city sizes */
	private final List<CitySize> citySizesList;

	/** Map of city size IDs to city size XML objects */
	private final Map<String, CitySize> citySizes;

	/** List of movement rate rules */
	private final List<MovementRateRule> movementRateRules;

	/**
	 * @param db Client DB structure coverted from server XML
	 */
	public ServerDatabaseLookup (final ServerDatabase db)
	{
		super (db.getPlane (), db.getMapFeature (), db.getTileType (), db.getProductionType (),
			db.getPickType (), db.getPick (), db.getWizard (), db.getUnitType (), db.getUnitMagicRealm (),
			db.getUnit (), db.getUnitSkill (), db.getWeaponGrade (), db.getRace (), db.getTaxRate (),
			db.getBuilding (), db.getSpell (), db.getCombatAreaEffect ());

		// Create city sizes map
		citySizesList = db.getCitySize ();
		citySizes = new HashMap<String, CitySize> ();
		if (citySizesList != null)
			for (final CitySize thisCitySize : citySizesList)
				citySizes.put (thisCitySize.getCitySizeID (), thisCitySize);

		// Store list of movement rate rules, no need for a map since there'd never be a reason to want a single rule, only to iterate through them
		movementRateRules = db.getMovementRateRule ();
	}

	/**
	 * @return Complete list of all city sizes in game
	 */
	public final List<CitySize> getCitySizes ()
	{
		return citySizesList;
	}

	/**
	 * @param citySizeID City size ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return CitySize object
	 * @throws RecordNotFoundException If the citySizeID doesn't exist
	 */
	public final CitySize findCitySize (final String citySizeID, final String caller) throws RecordNotFoundException
	{
		final CitySize citySize = citySizes.get (citySizeID);
		if (citySize == null)
			throw new RecordNotFoundException (CitySize.class.getName (), citySizeID, caller);

		return citySize;
	}

	/**
	 * @return List of movement rate rules
	 */
	public final List<MovementRateRule> getMovementRateRules ()
	{
		return movementRateRules;
	}

	/**
	 * @return Complete list of all planes in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<Plane> getPlanes ()
	{
		return (List<Plane>) super.getPlanes ();
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
		return (Plane) super.findPlane (planeNumber, caller);
	}

	/**
	 * @return Complete list of all map features in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<MapFeature> getMapFeatures ()
	{
		return (List<MapFeature>) super.getMapFeatures ();
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
		return (MapFeature) super.findMapFeature (mapFeatureID, caller);
	}

	/**
	 * @return Complete list of all tile types in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<TileType> getTileTypes ()
	{
		return (List<TileType>) super.getTileTypes ();
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
		return (TileType) super.findTileType (tileTypeID, caller);
	}

	/**
	 * @return Complete list of all production types in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<ProductionType> getProductionTypes ()
	{
		return (List<ProductionType>) super.getProductionTypes ();
	}

	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public ProductionType findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException
	{
		return (ProductionType) super.findProductionType (productionTypeID, caller);
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
		return (PickType) super.findPickType (pickTypeID, caller);
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
		return (Pick) super.findPick (pickID, caller);
	}

	/**
	 * @return Complete list of all wizards in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<Wizard> getWizards ()
	{
		return (List<Wizard>) super.getWizards ();
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
		return (Wizard) super.findWizard (wizardID, caller);
	}

	/**
	 * @return Complete list of all units in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<Unit> getUnits ()
	{
		return (List<Unit>) super.getUnits ();
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
		return (Unit) super.findUnit (unitID, caller);
	}

	/**
	 * @return Complete list of all unit skills in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<UnitSkill> getUnitSkills ()
	{
		return (List<UnitSkill>) super.getUnitSkills ();
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
		return (UnitSkill) super.findUnitSkill (unitSkillID, caller);
	}

	/**
	 * @return Complete list of all races in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<Race> getRaces ()
	{
		return (List<Race>) super.getRaces ();
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
		return (Race) super.findRace (raceID, caller);
	}

	/**
	 * @return Complete list of all buildings in game
	 */
	@SuppressWarnings ("unchecked")
	@Override
	public final List<Building> getBuildings ()
	{
		return (List<Building>) super.getBuildings ();
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
		return (Building) super.findBuilding (buildingID, caller);
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
		return (Spell) super.findSpell (spellID, caller);
	}
}

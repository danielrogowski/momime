package momime.client.graphics.database;

import java.awt.Dimension;
import java.util.List;

import momime.client.graphics.database.v0_9_5.CityImage;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Unit;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.graphics.database.v0_9_5.WeaponGrade;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MemoryBuilding;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Describes operations that we need to support over the graphics XML file
 */
public interface GraphicsDatabaseEx
{
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	public Pick findPick (final String pickID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	public Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException;

	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	public ProductionTypeEx findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	public RaceEx findRace (final String raceID, final String caller) throws RecordNotFoundException;

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object; note buildings in the graphics XML are just a special case of city view elements
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	public CityViewElement findBuilding (final String buildingID, final String caller) throws RecordNotFoundException;

	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	public UnitTypeEx findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param unitAttributeID Unit attribute ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit attribute object
	 * @throws RecordNotFoundException If the unitAttributeID doesn't exist
	 */
	public UnitAttributeEx findUnitAttribute (final String unitAttributeID, final String caller) throws RecordNotFoundException;

	/**
	 * @return List of all unit skill graphics
	 */
	public List<UnitSkill> getUnitSkill ();
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	public UnitSkill findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public Unit findUnit (final String unitID, final String caller) throws RecordNotFoundException;

	/**
	 * @param rangedAttackTypeID Ranged attack type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Ranged attack type object
	 * @throws RecordNotFoundException If the rangedAttackTypeID doesn't exist
	 */
	public RangedAttackTypeEx findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weaponGradNumber doesn't exist
	 */
	public WeaponGrade findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param scale Combat tile unit relative scale
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Scale object
	 * @throws RecordNotFoundException If the scale doesn't exist
	 */
	public CombatTileUnitRelativeScaleEx findCombatTileUnitRelativeScale (final int scale, final String caller) throws RecordNotFoundException;
		
	/**
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	public TileSetEx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException;

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	public MapFeatureEx findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;
	
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
	 * @return List of all city view elemenets (backgrounds, buildings, spell effects and so on)
	 */
    public List<CityViewElement> getCityViewElement ();
	
	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	public AnimationEx findAnimation (final String animationID, final String caller) throws RecordNotFoundException;
	
	/**
	 * NB. This will find the largest width and the largest height separately, so its possible this may return a dimension
	 * which no building actually has, if e.g. the widest is 50x25 and the tallest is 20x40 then it would return 50x40.
	 * 
	 * @return Size of the largest building image that can be constructed
	 */
	public Dimension getLargestBuildingSize ();
}

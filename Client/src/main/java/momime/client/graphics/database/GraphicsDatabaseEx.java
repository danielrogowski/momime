package momime.client.graphics.database;

import java.awt.Dimension;
import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.FrontOrBack;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MemoryBuilding;

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
	public PickGfx findPick (final String pickID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	public WizardGfx findWizard (final String wizardID, final String caller) throws RecordNotFoundException;

	/**
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	public ProductionTypeGfx findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	public RaceGfx findRace (final String raceID, final String caller) throws RecordNotFoundException;

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object; note buildings in the graphics XML are just a special case of city view elements
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	public CityViewElementGfx findBuilding (final String buildingID, final String caller) throws RecordNotFoundException;

	/**
	 * @param citySpellEffectID City spell effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return City spell effect object, or null if not found (e.g. Pestilence has no image)
	 */
	public CityViewElementGfx findCitySpellEffect (final String citySpellEffectID, final String caller);
	
	/**
	 * @param spellID Spell ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Spell object
	 * @throws RecordNotFoundException If the spellID doesn't exist
	 */
	public SpellGfx findSpell (final String spellID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param combatActionID Combat action ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Combat action object
	 * @throws RecordNotFoundException If the combatActionID doesn't exist
	 */
	public CombatActionGfx findCombatAction (final String combatActionID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	public UnitTypeGfx findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException;

	/**
	 * @param UnitSkillComponentID Unit attribute component ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit attribute component object
	 * @throws RecordNotFoundException If the UnitSkillComponentID doesn't exist
	 */
	public UnitSkillComponentImageGfx findUnitSkillComponent (final UnitSkillComponent UnitSkillComponentID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return List of all unit skill graphics
	 */
	public List<UnitSkillGfx> getUnitSkills ();
	
	/**
	 * @param unitSkillID Unit skill ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit skill object
	 * @throws RecordNotFoundException If the unitSkillID doesn't exist
	 */
	public UnitSkillGfx findUnitSkill (final String unitSkillID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public UnitGfx findUnit (final String unitID, final String caller) throws RecordNotFoundException;

	/**
	 * @param unitSpecialOrderID Unit special order ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit special order object
	 * @throws RecordNotFoundException If the unitSpecialOrderID doesn't exist
	 */
	public UnitSpecialOrderImageGfx findUnitSpecialOrder (final UnitSpecialOrder unitSpecialOrderID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param rangedAttackTypeID Ranged attack type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Ranged attack type object
	 * @throws RecordNotFoundException If the rangedAttackTypeID doesn't exist
	 */
	public RangedAttackTypeGfx findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weaponGradNumber doesn't exist
	 */
	public WeaponGradeGfx findWeaponGrade (final int weaponGradeNumber, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param scale Combat tile unit relative scale
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Scale object
	 * @throws RecordNotFoundException If the scale doesn't exist
	 */
	public CombatTileUnitRelativeScaleGfx findCombatTileUnitRelativeScale (final int scale, final String caller) throws RecordNotFoundException;
		
	/**
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	public TileSetGfx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException;

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	public MapFeatureGfx findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param combatAreaEffectID Combat area effect ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Combat area effect object
	 * @throws RecordNotFoundException If the combatAreaEffectID doesn't exist
	 */
	public CombatAreaEffectGfx findCombatAreaEffect (final String combatAreaEffectID, final String caller) throws RecordNotFoundException;
	
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
	public CityImageGfx findBestCityImage (final String citySizeID, final MapCoordinates3DEx cityLocation,
		final List<MemoryBuilding> buildings, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return List of all city view elemenets (backgrounds, buildings, spell effects and so on)
	 */
    public List<CityViewElementGfx> getCityViewElements ();
    
    /**
     * @param combatTileBorderID Combat tile border ID to search for
     * @param directions Border directions to search for
     * @param frontOrBack Whether to look for the front or back image
     * @return Image details if found; null if not found
     */
    public CombatTileBorderImageGfx findCombatTileBorderImages (final String combatTileBorderID, final String directions, final FrontOrBack frontOrBack);
	
	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	public AnimationGfx findAnimation (final String animationID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param playListID Play list ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Play list object
	 * @throws RecordNotFoundException If the playListID doesn't exist
	 */
	public PlayListGfx findPlayList (final String playListID, final String caller) throws RecordNotFoundException;
	
	/**
	 * NB. This will find the largest width and the largest height separately, so its possible this may return a dimension
	 * which no building actually has, if e.g. the widest is 50x25 and the tallest is 20x40 then it would return 50x40.
	 * 
	 * @return Size of the largest building image that can be constructed
	 */
	public Dimension getLargestBuildingSize ();
}
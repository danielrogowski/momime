package momime.client.graphics.database;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.CityImage;
import momime.client.graphics.database.v0_9_5.CityImagePrerequisite;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.graphics.database.v0_9_5.CombatAction;
import momime.client.graphics.database.v0_9_5.CombatTileUnitRelativeScale;
import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.MapFeature;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.PlayList;
import momime.client.graphics.database.v0_9_5.ProductionType;
import momime.client.graphics.database.v0_9_5.Race;
import momime.client.graphics.database.v0_9_5.RangedAttackType;
import momime.client.graphics.database.v0_9_5.Spell;
import momime.client.graphics.database.v0_9_5.TileSet;
import momime.client.graphics.database.v0_9_5.Unit;
import momime.client.graphics.database.v0_9_5.UnitAttribute;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.client.graphics.database.v0_9_5.UnitType;
import momime.client.graphics.database.v0_9_5.WeaponGrade;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;

/**
 * Implementation of graphics XML database - extends stubs auto-generated from XSD to add additional functionality from the interface
 */
public final class GraphicsDatabaseExImpl extends GraphicsDatabase implements GraphicsDatabaseEx
{
	/** Class logger */
	private final Log log = LogFactory.getLog (GraphicsDatabaseExImpl.class);
	
	/** Map of pick IDs to pick objects */
	private Map<String, Pick> picksMap;

	/** Map of wizard IDs to wizard objects */
	private Map<String, Wizard> wizardsMap;

	/** Map of production type IDs to production type objects */
	private Map<String, ProductionTypeEx> productionTypesMap;
	
	/** Map of race IDs to race objects */
	private Map<String, RaceEx> racesMap;

	/** Map of building IDs to city view elements */
	private Map<String, CityViewElement> buildingsMap;
	
	/** Map of spell IDs to spell objects */
	private Map<String, Spell> spellsMap;
	
	/** Map of combat action IDs to combat action objects */
	private Map<String, CombatAction> combatActionsMap;

	/** Map of unit type IDs to unit type objects */
	private Map<String, UnitTypeEx> unitTypesMap;
	
	/** Map of unit attribute IDs to unit attribute objects */
	private Map<String, UnitAttributeEx> unitAttributesMap;
	
	/** Map of unit skill IDs to unit skill objects */
	private Map<String, UnitSkill> unitSkillsMap;
	
	/** Map of unit IDs to unit objects */
	private Map<String, UnitEx> unitsMap;

	/** Map of ranged attack type IDs to ranged attack type objects */
	private Map<String, RangedAttackTypeEx> rangedAttackTypesMap;
	
	/** Map of weapon grade numbers to weapon grade objects */
	private Map<Integer, WeaponGrade> weaponGradesMap;
	
	/** Map of scales to coordinates for each figure count */
	private Map<Integer, CombatTileUnitRelativeScaleEx> combatTileUnitRelativeScalesMap;
	
	/** Map of tileSet IDs to tileSet objects */
	private Map<String, TileSetEx> tileSetsMap;

	/** Map of map feature IDs to map feature XML objects */
	private Map<String, MapFeatureEx> mapFeaturesMap;
	
	/** Map of animation IDs to animation objects */
	private Map<String, AnimationEx> animationsMap;
	
	/** Map of play list IDs to play list objects */
	private Map<String, PlayList> playListsMap;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Size of the largest building image that can be constructed */
	private Dimension largestBuildingSize;
	
	/**
	 * Builds all the hash maps to enable finding records faster
	 */
	public final void buildMaps ()
	{
		log.trace ("Entering buildMaps");
		
		// Create picks map
		picksMap = new HashMap<String, Pick> ();
		for (final Pick thisPick : getPick ())
			picksMap.put (thisPick.getPickID (), thisPick);

		// Create wizards map
		wizardsMap = new HashMap<String, Wizard> ();
		for (final Wizard thisWizard : getWizard ())
			wizardsMap.put (thisWizard.getWizardID (), thisWizard);

		// Create production types map
		productionTypesMap = new HashMap<String, ProductionTypeEx> ();
		for (final ProductionType thisProductionType : getProductionType ())
		{
			final ProductionTypeEx ptex = (ProductionTypeEx) thisProductionType;
			ptex.buildMap ();
			productionTypesMap.put (thisProductionType.getProductionTypeID (), ptex);
		}
		
		// Create races map
		racesMap = new HashMap<String, RaceEx> ();
		for (final Race thisRace : getRace ())
		{
			final RaceEx rex = (RaceEx) thisRace;
			rex.buildMap ();
			racesMap.put (rex.getRaceID (), rex);
		}

		// Create buildings map
		buildingsMap = new HashMap<String, CityViewElement> ();
		for (final CityViewElement thisBuilding : getCityViewElement ())
			
			// Not all CityViewElements represent buildings
			if (thisBuilding.getBuildingID () != null)
				buildingsMap.put (thisBuilding.getBuildingID (), thisBuilding);

		// Create spells map
		spellsMap = new HashMap<String, Spell> ();
		for (final Spell thisSpell : getSpell ())
			spellsMap.put (thisSpell.getSpellID (), thisSpell);
		
		// Create combatActions map
		combatActionsMap = new HashMap<String, CombatAction> ();
		for (final CombatAction thisCombatAction : getCombatAction ())
			combatActionsMap.put (thisCombatAction.getCombatActionID (), thisCombatAction);
		
		// Create unit types map
		unitTypesMap = new HashMap<String, UnitTypeEx> ();
		for (final UnitType thisUnitType : getUnitType ())
		{
			final UnitTypeEx utex = (UnitTypeEx) thisUnitType;
			utex.buildMap ();
			unitTypesMap.put (utex.getUnitTypeID (), utex);
		}
		
		// Create unit attributes map
		unitAttributesMap = new HashMap<String, UnitAttributeEx> ();
		for (final UnitAttribute thisUnitAttribute : getUnitAttribute ())
		{
			final UnitAttributeEx attrEx = (UnitAttributeEx) thisUnitAttribute;
			attrEx.buildMap ();
			unitAttributesMap.put (attrEx.getUnitAttributeID (), attrEx);
		}

		// Create unit skills map
		unitSkillsMap = new HashMap<String, UnitSkill> ();
		for (final UnitSkill thisUnitSkill : getUnitSkill ())
			unitSkillsMap.put (thisUnitSkill.getUnitSkillID (), thisUnitSkill);
		
		// Create units map
		unitsMap = new HashMap<String, UnitEx> ();
		for (final Unit thisUnit : getUnit ())
		{
			final UnitEx unitEx = (UnitEx) thisUnit;
			unitEx.buildMap ();
			unitsMap.put (unitEx.getUnitID (), unitEx);
		}

		// Create ranged attack types map
		rangedAttackTypesMap = new HashMap<String, RangedAttackTypeEx> ();
		for (final RangedAttackType thisRangedAttackType : getRangedAttackType ())
		{
			final RangedAttackTypeEx ratEx = (RangedAttackTypeEx) thisRangedAttackType;
			ratEx.buildMap ();
			rangedAttackTypesMap.put (ratEx.getRangedAttackTypeID (), ratEx);
		}
		
		// Create weapon grades map
		weaponGradesMap = new HashMap<Integer, WeaponGrade> ();
		for (final WeaponGrade thisWeaponGrade : getWeaponGrade ())
			weaponGradesMap.put (thisWeaponGrade.getWeaponGradeNumber (), thisWeaponGrade);
		
		// Create combat tile unit relative scales map
		combatTileUnitRelativeScalesMap = new HashMap<Integer, CombatTileUnitRelativeScaleEx> ();
		for (final CombatTileUnitRelativeScale scale : getCombatTileUnitRelativeScale ())
		{
			final CombatTileUnitRelativeScaleEx scaleEx = (CombatTileUnitRelativeScaleEx) scale;
			scaleEx.buildMap ();
			combatTileUnitRelativeScalesMap.put (scaleEx.getScale (), scaleEx);
		}
		
		// Create animations map
		animationsMap = new HashMap<String, AnimationEx> ();
		for (final Animation anim : getAnimation ())
			animationsMap.put (anim.getAnimationID (), (AnimationEx) anim);
		
		// Create tile sets map
		tileSetsMap = new HashMap<String, TileSetEx> ();
		for (final TileSet ts : getTileSet ())
			tileSetsMap.put (ts.getTileSetID (), (TileSetEx) ts);

		// Create map features map
		mapFeaturesMap = new HashMap<String, MapFeatureEx> ();
		for (final MapFeature mf : getMapFeature ())
			mapFeaturesMap.put (mf.getMapFeatureID (), (MapFeatureEx) mf);

		// Create play lists map
		playListsMap = new HashMap<String, PlayList> ();
		for (final PlayList pl : getPlayList ())
			playListsMap.put (pl.getPlayListID (), pl);
		
		log.trace ("Exiting buildMaps");
	}

	/**
	 * Verifies that all animations, tiles and so on are consistent across the graphics DB
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public final void consistencyChecks () throws IOException
	{
		log.trace ("Entering consistencyChecks");
		log.info ("Processing graphics XML file");
		
		// Check all animations have frames with consistent sizes
		for (final Animation anim : getAnimation ())
		{
			final AnimationEx aex = (AnimationEx) anim;
			aex.deriveAnimationWidthAndHeight ();
		}
		log.info ("All " + getAnimation ().size () + " animations passed consistency checks");		
		
		// Build all the smoothing rule bitmask maps, and determine the size of tiles in each set
		for (final TileSet ts : getTileSet ())
		{
			final TileSetEx tsex = (TileSetEx) ts;
			tsex.buildMaps ();
			tsex.deriveAnimationFrameCountAndSpeed (this);
			tsex.deriveTileWidthAndHeight (this);
		}
		final TileSetEx overlandMapTileSet = findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "consistencyChecks");

		// Ensure all map features match the size of the overland map tiles
		for (final MapFeature mf : getMapFeature ())
		{
			final MapFeatureEx mfex = (MapFeatureEx) mf;
			mfex.checkWidthAndHeight (overlandMapTileSet);
		}
		log.info ("All " + getMapFeature ().size () + " map features passed consistency checks");
		
		// Find the largest building
		int largestWidth = 0;
		int largestHeight = 0;
		for (final CityViewElement thisBuilding : getCityViewElement ())
			
			// Not all CityViewElements represent buildings; also ignore Summoning circle and Wizard's fortress because we can't actually construct those
			// (Hard coding these is a bit of a hack, but we don't learn "properly" that we can't construct these until we receive the XML from the server when we join a game)
			if ((thisBuilding.getBuildingID () != null) && (!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_SUMMONING_CIRCLE)) &&
				(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.VALUE_BUILDING_FORTRESS)))
			{
				// It could be an image or animation
				final int thisWidth;
				final int thisHeight;
				if (thisBuilding.getCityViewAnimation () != null)
				{
					final AnimationEx anim = findAnimation (thisBuilding.getCityViewAnimation (), "consistencyChecks");
					thisWidth = anim.getAnimationWidth ();
					thisHeight = anim.getAnimationHeight ();
				}
				else
				{
					final BufferedImage image = getUtils ().loadImage
						((thisBuilding.getCityViewAlternativeImageFile () != null) ? thisBuilding.getCityViewAlternativeImageFile () : thisBuilding.getCityViewImageFile ());
					thisWidth = image.getWidth ();
					thisHeight = image.getHeight ();
				}
				
				largestWidth = Math.max (largestWidth, thisWidth);
				largestHeight = Math.max (largestHeight, thisHeight);
			}
		
		log.info ("Largest building image is " + largestWidth + "x" + largestHeight);
		largestBuildingSize = new Dimension (largestWidth, largestHeight);
		
		log.trace ("Exiting consistencyChecks");
	}

	/**
	 * Method triggered by Spring when the the DB is created
	 * @throws IOException If any images cannot be loaded, or any consistency checks fail
	 */
	public final void buildMapsAndRunConsistencyChecks () throws IOException
	{
		log.trace ("Entering buildMapsAndRunConsistencyChecks");

		buildMaps ();
		consistencyChecks ();

		log.trace ("Exiting buildMapsAndRunConsistencyChecks");
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
	 * @param productionTypeID Production type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Production type object
	 * @throws RecordNotFoundException If the productionTypeID doesn't exist
	 */
	@Override
	public final ProductionTypeEx findProductionType (final String productionTypeID, final String caller) throws RecordNotFoundException
	{
		final ProductionTypeEx found = productionTypesMap.get (productionTypeID);
		if (found == null)
			throw new RecordNotFoundException (ProductionType.class, productionTypeID, caller);

		return found;
	}
	
	/**
	 * @param raceID Race ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Race object
	 * @throws RecordNotFoundException If the raceID doesn't exist
	 */
	@Override
	public final RaceEx findRace (final String raceID, final String caller) throws RecordNotFoundException
	{
		final RaceEx found = racesMap.get (raceID);
		if (found == null)
			throw new RecordNotFoundException (Race.class, raceID, caller);

		return found;
	}

	/**
	 * @param buildingID Building ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Building object; note buildings in the graphics XML are just a special case of city view elements
	 * @throws RecordNotFoundException If the buildingID doesn't exist
	 */
	@Override
	public final CityViewElement findBuilding (final String buildingID, final String caller) throws RecordNotFoundException
	{
		final CityViewElement found = buildingsMap.get (buildingID);
		if (found == null)
			throw new RecordNotFoundException (CityViewElement.class, buildingID, caller);

		return found;
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
	 * @param combatActionID Combat action ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Combat action object
	 * @throws RecordNotFoundException If the combatActionID doesn't exist
	 */
	@Override
	public final CombatAction findCombatAction (final String combatActionID, final String caller) throws RecordNotFoundException
	{
		final CombatAction found = combatActionsMap.get (combatActionID);
		if (found == null)
			throw new RecordNotFoundException (CombatAction.class, combatActionID, caller);

		return found;
	}
	
	/**
	 * @param unitTypeID Unit type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit type object
	 * @throws RecordNotFoundException If the unitTypeID doesn't exist
	 */
	@Override
	public final UnitTypeEx findUnitType (final String unitTypeID, final String caller) throws RecordNotFoundException
	{
		final UnitTypeEx found = unitTypesMap.get (unitTypeID);
		if (found == null)
			throw new RecordNotFoundException (UnitType.class, unitTypeID, caller);

		return found;
	}
	
	/**
	 * @param unitAttributeID Unit attribute ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit attribute object
	 * @throws RecordNotFoundException If the unitAttributeID doesn't exist
	 */
	@Override
	public final UnitAttributeEx findUnitAttribute (final String unitAttributeID, final String caller) throws RecordNotFoundException
	{
		final UnitAttributeEx found = unitAttributesMap.get (unitAttributeID);
		if (found == null)
			throw new RecordNotFoundException (UnitAttribute.class, unitAttributeID, caller);

		return found;
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
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	@Override
	public final UnitEx findUnit (final String unitID, final String caller) throws RecordNotFoundException
	{
		final UnitEx found = unitsMap.get (unitID);
		if (found == null)
			throw new RecordNotFoundException (Unit.class, unitID, caller);

		return found;
	}
	
	/**
	 * @param rangedAttackTypeID Ranged attack type ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Ranged attack type object
	 * @throws RecordNotFoundException If the rangedAttackTypeID doesn't exist
	 */
	@Override
	public final RangedAttackTypeEx findRangedAttackType (final String rangedAttackTypeID, final String caller) throws RecordNotFoundException
	{
		final RangedAttackTypeEx found = rangedAttackTypesMap.get (rangedAttackTypeID);
		if (found == null)
			throw new RecordNotFoundException (RangedAttackType.class, rangedAttackTypeID, caller);

		return found;
	}
	
	/**
	 * @param weaponGradeNumber Weapon grade number to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Weapon grade object
	 * @throws RecordNotFoundException If the weaponGradNumber doesn't exist
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
	 * @param scale Combat tile unit relative scale
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Scale object
	 * @throws RecordNotFoundException If the scale doesn't exist
	 */
	@Override
	public final CombatTileUnitRelativeScaleEx findCombatTileUnitRelativeScale (final int scale, final String caller) throws RecordNotFoundException
	{
		final CombatTileUnitRelativeScaleEx found = combatTileUnitRelativeScalesMap.get (scale);
		if (found == null)
			throw new RecordNotFoundException (CombatTileUnitRelativeScale.class, scale, caller);

		return found;
	}
	
	/**
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	@Override
	public final TileSetEx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException
	{
		final TileSetEx found = tileSetsMap.get (tileSetID);
		if (found == null)
			throw new RecordNotFoundException (TileSet.class, tileSetID, caller);

		return found;
	}

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public final MapFeatureEx findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException
	{
		final MapFeatureEx found = mapFeaturesMap.get (mapFeatureID);
		if (found == null)
			throw new RecordNotFoundException (MapFeature.class, mapFeatureID, caller);

		return found;
	}
	
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
	@Override
	public final CityImage findBestCityImage (final String citySizeID, final MapCoordinates3DEx cityLocation,
		final List<MemoryBuilding> buildings, final String caller) throws RecordNotFoundException
	{
		CityImage bestMatch = null;
		int bestMatchBuildingCount = 0;
		
		for (final CityImage image : getCityImage ())
			if (image.getCitySizeID ().equals (citySizeID))
			{
				// So the size matches - but does this image require buildings to be present?  If so, count how many
				boolean buildingsMatch = true;
				int buildingCount = 0;
				final Iterator<CityImagePrerequisite> iter = image.getCityImagePrerequisite ().iterator ();
				while ((buildingsMatch) && (iter.hasNext ()))
				{
					final String buildingID = iter.next ().getPrerequisiteID ();
					if (getMemoryBuildingUtils ().findBuilding (buildings, cityLocation, buildingID))
						buildingCount++;
					else
						buildingsMatch = false;
				}
				
				// Is it a better match than we had already?
				if ((buildingsMatch) && ((bestMatch == null) || (buildingCount > bestMatchBuildingCount)))
				{
					bestMatch = image;
					bestMatchBuildingCount = buildingCount;
				}
			}
		
		if (bestMatch == null)
			throw new RecordNotFoundException (CityImage.class, citySizeID, caller);
			
		return bestMatch;
	}
	
	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	@Override
	public final AnimationEx findAnimation (final String animationID, final String caller) throws RecordNotFoundException
	{
		final AnimationEx found = animationsMap.get (animationID);
		if (found == null)
			throw new RecordNotFoundException (Animation.class, animationID, caller);

		return found;
	}

	/**
	 * @param playListID Play list ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Play list object
	 * @throws RecordNotFoundException If the playListID doesn't exist
	 */
	@Override
	public final PlayList findPlayList (final String playListID, final String caller) throws RecordNotFoundException
	{
		final PlayList found = playListsMap.get (playListID);
		if (found == null)
			throw new RecordNotFoundException (PlayList.class, playListID, caller);

		return found;
	}
	
	/**
	 * NB. This will find the largest width and the largest height separately, so its possible this may return a dimension
	 * which no building actually has, if e.g. the widest is 50x25 and the tallest is 20x40 then it would return 50x40.
	 * 
	 * @return Size of the largest building image that can be constructed
	 */
	@Override
	public final Dimension getLargestBuildingSize ()
	{
		return largestBuildingSize;
	}
	
	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param util Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils util)
	{
		memoryBuildingUtils = util;
	}

	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}
}
package momime.client.graphics.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.CityImage;
import momime.client.graphics.database.v0_9_5.CityImagePrerequisite;
import momime.client.graphics.database.v0_9_5.GraphicsDatabase;
import momime.client.graphics.database.v0_9_5.MapFeature;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Race;
import momime.client.graphics.database.v0_9_5.TileSet;
import momime.client.graphics.database.v0_9_5.Unit;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.utils.MemoryBuildingUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

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

	/** Map of race IDs to race objects */
	private Map<String, RaceEx> racesMap;

	/** Map of unit IDs to unit objects */
	private Map<String, Unit> unitsMap;
	
	/** Map of tileSet IDs to tileSet objects */
	private Map<String, TileSetEx> tileSetsMap;

	/** Map of map feature IDs to map feature XML objects */
	private Map<String, MapFeatureEx> mapFeaturesMap;
	
	/** Map of animation IDs to animation objects */
	private Map<String, AnimationEx> animationsMap;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
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

		// Create races map
		racesMap = new HashMap<String, RaceEx> ();
		for (final Race thisRace : getRace ())
		{
			final RaceEx rex = (RaceEx) thisRace;
			rex.buildMap ();
			racesMap.put (rex.getRaceID (), rex);
		}

		// Create units map
		unitsMap = new HashMap<String, Unit> ();
		for (final Unit thisUnit : getUnit ())
			unitsMap.put (thisUnit.getUnitID (), thisUnit);

		// Create animations map, and check for consistency
		animationsMap = new HashMap<String, AnimationEx> ();
		for (final Animation anim : getAnimation ())
			animationsMap.put (anim.getAnimationID (), (AnimationEx) anim);
		
		// Create tileSets map, and build all the smoothing rule bitmask maps
		tileSetsMap = new HashMap<String, TileSetEx> ();
		for (final TileSet ts : getTileSet ())
			tileSetsMap.put (ts.getTileSetID (), (TileSetEx) ts);

		// Create map features map, and check for consistency
		mapFeaturesMap = new HashMap<String, MapFeatureEx> ();
		for (final MapFeature mf : getMapFeature ())
			mapFeaturesMap.put (mf.getMapFeatureID (), (MapFeatureEx) mf);
		
		log.trace ("Exiting buildMaps");
	}

	/**
	 * Builds all the hash maps to enable finding records faster
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}
}
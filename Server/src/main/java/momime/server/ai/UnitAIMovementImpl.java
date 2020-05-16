package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.random.RandomUtils;

import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.utils.MemoryGridCellUtils;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.TileTypeSvr;
import momime.server.messages.ServerMemoryGridCellUtils;

/**
 * Provides a method for processing each movement code that the AI uses to decide where to send units overland.
 * 
 * Each of these methods must be able to operate in "test" mode, so we just test to see if the unit has something
 * to do using that code, without actually doing it.
 */
public final class UnitAIMovementImpl implements UnitAIMovement
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UnitAIMovementImpl.class);
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/**
	 * AI tries to move units to any location that lacks defence or can be captured without a fight.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param underdefendedLocations Locations which are either ours (cities/towers) but lack enough defence, or not ours but can be freely captured (empty lairs/cities/etc)
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_Reinforce (final int [] [] [] doubleMovementDistances,
		final List<AIDefenceLocation> underdefendedLocations)
	{
		log.trace ("Entering considerUnitMovement_Reinforce");
		
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;
		
		// Check all locations we want to head to and find the closest one (or multiple closest ones)
		for (final AIDefenceLocation location : underdefendedLocations)
		{
			final int doubleThisDistance = doubleMovementDistances [location.getMapLocation ().getZ ()] [location.getMapLocation ().getY ()] [location.getMapLocation ().getX ()];
			if (doubleThisDistance >= 0)
			{
				// We can get there, eventually
				if ((doubleDestinationDistance == null) || (doubleThisDistance < doubleDestinationDistance))
				{
					doubleDestinationDistance = doubleThisDistance;
					destinations.clear ();
					destinations.add (location.getMapLocation ());
				}
				else if (doubleThisDistance == doubleDestinationDistance)
					destinations.add (location.getMapLocation ());
			}
		}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable underdefended locations
		else
			decision = new AIMovementDecision (destinations.get (getRandomUtils ().nextInt (destinations.size ())));
		
		log.trace ("Exiting considerUnitMovement_Reinforce = " + decision);
		return decision;
	}

	/**
	 * AI tries to move units to attack defended stationary locations (nodes/lairs/towers/cities) where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_AttackStationary (final AIUnitsAndRatings units, final int [] [] [] doubleMovementDistances,
		final AIUnitsAndRatings [] [] [] enemyUnits, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering considerUnitMovement_AttackStationary");

		final int ourCurrentRating = units.totalCurrentRatings ();
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;

		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					// Only process towers on the first plane
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final OverlandMapTerrainData terrainData = mc.getTerrainData ();
					if ((z == 0) || (!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData)))
					{					
						final OverlandMapCityData cityData = mc.getCityData ();
						final AIUnitsAndRatings enemyUnitStack = enemyUnits [z] [y] [x];
						final int doubleThisDistance = doubleMovementDistances [z] [y] [x];
						if (((cityData != null) || (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db))) &&
							(enemyUnitStack != null) && (enemyUnitStack.totalCurrentRatings () < ourCurrentRating) && (doubleThisDistance >= 0))
						{
							// We can get there eventually, and stand a chance of beating them
							final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);
							if ((doubleDestinationDistance == null) || (doubleThisDistance < doubleDestinationDistance))
							{
								doubleDestinationDistance = doubleThisDistance;
								destinations.clear ();
								destinations.add (location);
							}
							else if (doubleThisDistance == doubleDestinationDistance)
								destinations.add (location);
						}
					}
				}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable enemy unit defence positions that we stand a chance of beating
		else
			decision = new AIMovementDecision (destinations.get (getRandomUtils ().nextInt (destinations.size ())));
		
		log.trace ("Exiting considerUnitMovement_AttackStationary = " + decision);
		return decision;
	}
	
	/**
	 * AI tries to move units to attack enemy unit stacks wandering around the map where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_AttackWandering (final AIUnitsAndRatings units, final int [] [] [] doubleMovementDistances,
		final AIUnitsAndRatings [] [] [] enemyUnits, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering considerUnitMovement_AttackWandering");

		final int ourCurrentRating = units.totalCurrentRatings ();
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;

		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final OverlandMapTerrainData terrainData = mc.getTerrainData ();
					final OverlandMapCityData cityData = mc.getCityData ();
					final AIUnitsAndRatings enemyUnitStack = enemyUnits [z] [y] [x];
					final int doubleThisDistance = doubleMovementDistances [z] [y] [x];
					if ((cityData == null) && (!ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)) &&
						(enemyUnitStack != null) && (enemyUnitStack.totalCurrentRatings () < ourCurrentRating) && (doubleThisDistance >= 0))
					{
						// We can get there eventually, and stand a chance of beating them
						final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);
						if ((doubleDestinationDistance == null) || (doubleThisDistance < doubleDestinationDistance))
						{
							doubleDestinationDistance = doubleThisDistance;
							destinations.clear ();
							destinations.add (location);
						}
						else if (doubleThisDistance == doubleDestinationDistance)
							destinations.add (location);
					}
				}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable wandering enemy units that we stand a chance of beating
		else
			decision = new AIMovementDecision (destinations.get (getRandomUtils ().nextInt (destinations.size ())));
		
		log.trace ("Exiting considerUnitMovement_AttackWandering = " + decision);
		return decision;
	}

	/**
	 * AI tries to move units to scout any unknown terrain that is adjacent to at least one tile that we know to be land.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_ScoutLand (final int [] [] [] doubleMovementDistances,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering considerUnitMovement_ScoutLand");

		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;
		
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					if ((mc.getTerrainData () == null) || (mc.getTerrainData ().getTileTypeID () == null))
					{
						final int doubleThisDistance = doubleMovementDistances [z] [y] [x];
						if (doubleThisDistance >= 0)
						{
							// We can get there, eventually
							// Now look for an adjacent land tile
							boolean found = false;
							for (int d = 1; d <= getCoordinateSystemUtils ().getMaxDirection (sys.getCoordinateSystemType ()); d++)
							{
								final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);
								if (getCoordinateSystemUtils ().move3DCoordinates (sys, coords, d))
								{
									final OverlandMapTerrainData terrainData = terrain.getPlane ().get (coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get
										(coords.getX ()).getTerrainData ();
									
									if ((terrainData != null) && (terrainData.getTileTypeID () != null))
									{
										final TileTypeSvr tileType = db.findTileType (terrainData.getTileTypeID (), "considerUnitMovement_ScoutLand");
										if ((tileType.isLand () != null) && (tileType.isLand ()))
											found = true;
									}
								}
							}

							if (found)
							{
								final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);
								if ((doubleDestinationDistance == null) || (doubleThisDistance < doubleDestinationDistance))
								{
									doubleDestinationDistance = doubleThisDistance;
									destinations.clear ();
									destinations.add (location);
								}
								else if (doubleThisDistance == doubleDestinationDistance)
									destinations.add (location);
							}
						}
					}
				}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable unscouted locations adjacent to known land tiles
		else
			decision = new AIMovementDecision (destinations.get (getRandomUtils ().nextInt (destinations.size ())));
		
		log.trace ("Exiting considerUnitMovement_ScoutLand = " + decision);
		return decision;
	}
	
	/**
	 * AI tries to move units to scout any unknown terrain.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_ScoutAll (final int [] [] [] doubleMovementDistances,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys)
	{
		log.trace ("Entering considerUnitMovement_ScoutAll");
		
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;
		
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					if ((mc.getTerrainData () == null) || (mc.getTerrainData ().getTileTypeID () == null))
					{
						final int doubleThisDistance = doubleMovementDistances [z] [y] [x];
						if (doubleThisDistance >= 0)
						{
							// We can get there, eventually
							final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);
							if ((doubleDestinationDistance == null) || (doubleThisDistance < doubleDestinationDistance))
							{
								doubleDestinationDistance = doubleThisDistance;
								destinations.clear ();
								destinations.add (location);
							}
							else if (doubleThisDistance == doubleDestinationDistance)
								destinations.add (location);
						}
					}
				}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable unscouted locations
		else
			decision = new AIMovementDecision (destinations.get (getRandomUtils ().nextInt (destinations.size ())));
		
		log.trace ("Exiting considerUnitMovement_ScoutAll = " + decision);
		return decision;
	}

	/**
	 * AI looks to see if any defended locations (nodes/lairs/towers/cities) are too well defended to attack at the moment,
	 * and if it can see any then will look to merge together our units into a bigger stack.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_JoinStack (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_JoinStack");

		log.warn ("AI movement code JOIN_STACK is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_JoinStack = " + decision);
		return decision;
	}

	/**
	 * AI looks for a tower garissoned by our units, and imagines that we are stood there and rechecks preceeding movement codes.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_PlaneShift (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_PlaneShift");

		log.warn ("AI movement code PLANE_SHIFT is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_PlaneShift = " + decision);
		return decision;
	}
	
	/**
	 * AI looks for a transport to get in (or stay where we are if we are already in one).
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_GetInTransport (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_GetInTransport");

		log.warn ("AI movement code GET_IN_TRANSPORT is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_GetInTransport = " + decision);
		return decision;
	}

	/**
	 * AI looks for any of our locations (nodes/cities/towers) that we can reach, regardless of if they already have plenty of defence.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_Overdefend (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_Overdefend");

		log.warn ("AI movement code OVERDEFEND is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_Overdefend = " + decision);
		return decision;
	}

	/**
	 * AI looks for a good place for settlers to build a city
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param currentLocation Current location of settler unit
	 * @param desiredCityLocation Location where we want to put a city
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_BuildCity (final int [] [] [] doubleMovementDistances, final MapCoordinates3DEx currentLocation, final MapCoordinates3DEx desiredCityLocation)
	{
		log.trace ("Entering considerUnitMovement_BuildCity");

		// If we have no idea where to put a city, then nothing to do
		final AIMovementDecision decision;
		if (desiredCityLocation == null)
			decision = null;
		
		// If we're already at the right spot, then go ahead and make a city
		else if (desiredCityLocation.equals (currentLocation))
			decision = new AIMovementDecision (UnitSpecialOrder.BUILD_CITY);
		
		// If we can head towards the location where we want to put a city then do so
		else if (doubleMovementDistances [desiredCityLocation.getZ ()] [desiredCityLocation.getY ()] [desiredCityLocation.getX ()] >= 0)
			decision = new AIMovementDecision (desiredCityLocation);
		
		else
			decision = null;
		
		log.trace ("Exiting considerUnitMovement_BuildCity = " + decision);
		return decision;
	}

	/**
	 * AI looks for a good place for engineers to build a road
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_BuildRoad (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_BuildRoad");

		log.warn ("AI movement code BUILD_ROAD is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_BuildRoad = " + decision);
		return decision;
	}

	/**
	 * AI looks for any corrupted land that priests need to purify
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_Purify (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_Purify");

		log.warn ("AI movement code PURIFY is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_Purify = " + decision);
		return decision;
	}

	/**
	 * AI looks for a node that a magic/guardian spirit can meld with
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_MeldWithNode (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_MeldWithNode");

		log.warn ("AI movement code MELD_WITH_NODE is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_MeldWithNode = " + decision);
		return decision;
	}
	
	/**
	 * AI transports look for a suitable island to carry units to, if we are holding any.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_CarryUnits (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_CarryUnits");

		log.warn ("AI movement code CARRY_UNITS is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_CarryUnits = " + decision);
		return decision;
	}
	
	/**
	 * AI transports that are empty head for any islands where any unit stacks went on OVERDEFEND.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_LoadUnits (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_LoadUnits");

		log.warn ("AI movement code LOAD_UNITS is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_LoadUnits = " + decision);
		return decision;
	}
	
	/**
	 * If we are on the same plane as our Wizards' Fortress, then head the island that it is on.
	 * (This is intended for transport ships that have nothing better to do, so we're assuming we can't actually get *onto* the island).
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_FortressIsland (final int [] [] [] doubleMovementDistances)
	{
		log.trace ("Entering considerUnitMovement_FortressIsland");

		log.warn ("AI movement code FORTRESS_ISLAND is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		log.trace ("Exiting considerUnitMovement_FortressIsland = " + decision);
		return decision;
	}
	
	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}
}
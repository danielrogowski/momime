package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
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
	
	/** AI decisions about cities */
	private CityAI cityAI;
	
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
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to go help reinforce underdefended location at " + chosenLocation + " which is " + doubleDestinationDistance + " double-moves away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		log.trace ("Exiting considerUnitMovement_Reinforce = " + decision);
		return decision;
	}

	/**
	 * AI tries to move units to attack defended stationary locations (nodes/lairs/towers/cities) where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_AttackStationary (final AIUnitsAndRatings units, final int [] [] [] doubleMovementDistances,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering considerUnitMovement_AttackStationary");

		final int ourCurrentRating = units.totalCurrentRatings ();
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;

		// The only way that any location on the plane other than where we are will be reachable is if we're stood right on a tower of wizardry,
		// in which case this will evaluate which plane to exit off from, which is exactly what we want.  Same is true for most of the other movement codes.
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
						if (((cityData != null) || ((!isRaiders) && (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)))) &&
							(enemyUnitStack != null) && (ourCurrentRating > enemyUnitStack.totalCurrentRatings ()) && (doubleThisDistance >= 0))
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
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to attack stationary target at " + chosenLocation + " which is " + doubleDestinationDistance + " double-moves away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
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
						(enemyUnitStack != null) && (ourCurrentRating > enemyUnitStack.totalCurrentRatings ()) && (doubleThisDistance >= 0))
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
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to attack wandering target at " + chosenLocation + " which is " + doubleDestinationDistance + " double-moves away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
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
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_ScoutLand (final int [] [] [] doubleMovementDistances,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db, final int playerID) throws RecordNotFoundException
	{
		log.trace ("Entering considerUnitMovement_ScoutLand");

		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;
		
		for (int z = 0; z < sys.getDepth (); z++)
		{
			final List<MapCoordinates2DEx> ourCitiesOnPlane = getCityAI ().listOurCitiesOnPlane (playerID, z, terrain, sys);
			
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					if ((mc.getTerrainData () == null) || (mc.getTerrainData ().getTileTypeID () == null))
					{
						int doubleThisDistance = doubleMovementDistances [z] [y] [x];
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
								doubleThisDistance = doubleThisDistance + getCityAI ().findDistanceToClosestCity (x, y, ourCitiesOnPlane, sys);

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
		}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable unscouted locations adjacent to known land tiles
		else
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to go scout unknown land at " + chosenLocation + " which is " + doubleDestinationDistance + " double-moves away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		log.trace ("Exiting considerUnitMovement_ScoutLand = " + decision);
		return decision;
	}
	
	/**
	 * AI tries to move units to scout any unknown terrain.
	 * 
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_ScoutAll (final int [] [] [] doubleMovementDistances,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final int playerID)
	{
		log.trace ("Entering considerUnitMovement_ScoutAll");
		
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;
		
		for (int z = 0; z < sys.getDepth (); z++)
		{
			final List<MapCoordinates2DEx> ourCitiesOnPlane = getCityAI ().listOurCitiesOnPlane (playerID, z, terrain, sys);
			
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					if ((mc.getTerrainData () == null) || (mc.getTerrainData ().getTileTypeID () == null))
					{
						int doubleThisDistance = doubleMovementDistances [z] [y] [x];
						if (doubleThisDistance >= 0)
						{
							doubleThisDistance = doubleThisDistance + getCityAI ().findDistanceToClosestCity (x, y, ourCitiesOnPlane, sys);
							
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
		}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable unscouted locations
		else
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to go scout unknown scenery (possibly not land) at " + chosenLocation + " which is " + doubleDestinationDistance + " double-moves away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		log.trace ("Exiting considerUnitMovement_ScoutAll = " + decision);
		return decision;
	}

	/**
	 * AI looks to see if any defended locations (nodes/lairs/towers/cities) are too well defended to attack at the moment,
	 * and if it can see any then will look to merge together our units into a bigger stack.
	 * 
	 * @param units The units to move
	 * @param doubleMovementDistances Movement required to reach every location on both planes; 0 = can move there for free, negative value = can't move there
	 * @param ourUnitsInSameCategory List of all our mobile unit stacks in the same category as the ones we are moving
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_JoinStack (final AIUnitsAndRatings units, final int [] [] [] doubleMovementDistances,
		final List<AIUnitsAndRatings> ourUnitsInSameCategory, final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering considerUnitMovement_JoinStack");

		// First of all we have to find the weakest enemy unit stack that we can reach but that is still too strong for us to fight alone
		final int ourCurrentRating = units.totalCurrentRatings ();
		Integer weakestEnemyUnitStackWeCannotBeat = null;

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
						final int enemyUnitStackRating = (enemyUnitStack == null) ? 0 : enemyUnitStack.totalCurrentRatings (); 
						if (((cityData != null) || ((!isRaiders) && (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)))) &&
							(enemyUnitStack != null) && (enemyUnitStackRating >= ourCurrentRating) && (doubleMovementDistances [z] [y] [x] >= 0))
						{
							// We don't care how far away it is - we care how strong they are, not how long it'll take us to get there
							if ((weakestEnemyUnitStackWeCannotBeat == null) || (enemyUnitStackRating < weakestEnemyUnitStackWeCannotBeat))
								weakestEnemyUnitStackWeCannotBeat = enemyUnitStackRating;
						}
					}
				}
		
		// Did we find one?
		final AIMovementDecision decision;
		if (weakestEnemyUnitStackWeCannotBeat == null)
			decision = null;
		else
		{
			// Now total up all friendly unit stacks that we can reach
			// The list only includes other mobile units, so we don't need to check whether they're in a city, tower, etc - but we do have to check that we can reach them
			int mergedStackRating = ourCurrentRating;
			final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
			Integer doubleDestinationDistance = null;
			
			for (final AIUnitsAndRatings ourUnitStack : ourUnitsInSameCategory)
				if (ourUnitStack != units)
				{
					final MapCoordinates3DEx unitStackLocation = (MapCoordinates3DEx) ourUnitStack.get (0).getUnit ().getUnitLocation ();
					final int doubleThisDistance = doubleMovementDistances [unitStackLocation.getZ ()] [unitStackLocation.getY ()] [unitStackLocation.getX ()];
					if (doubleThisDistance >= 0)
					{
						mergedStackRating = mergedStackRating + ourUnitStack.totalCurrentRatings ();
						
						final MapCoordinates3DEx location = new MapCoordinates3DEx (unitStackLocation);
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

			if ((mergedStackRating <= weakestEnemyUnitStackWeCannotBeat) || (destinations.isEmpty ()))
				decision = null;		// No reachable friendly unit stacks that would be useful for us to merge with
			else
			{
				final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
				log.debug ("Unit movement AI - Decided to go join up with our other unit stack at " + chosenLocation + " which is " + doubleDestinationDistance + " double-moves away");
				decision = new AIMovementDecision (chosenLocation);
			}
		}		
		
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
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_Overdefend (final int [] [] [] doubleMovementDistances,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final ServerDatabaseEx db)
		throws RecordNotFoundException
	{
		log.trace ("Entering considerUnitMovement_Overdefend");

		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		Integer doubleDestinationDistance = null;
		
		// This is like "reinforce", except cells that we check work like method evaluateCurrentDefence
		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					// Only process towers on the first plane
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final OverlandMapTerrainData terrainData = mc.getTerrainData ();
					if ((z == 0) || (!getMemoryGridCellUtils ().isTerrainTowerOfWizardry (terrainData)))
					{					
						final AIUnitsAndRatings theirs = enemyUnits [z] [y] [x];
						
						final OverlandMapCityData cityData = mc.getCityData ();
						if ((theirs == null) &&
							((cityData != null) ||
								((!isRaiders) && (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db)))))
						{
							final int doubleThisDistance = doubleMovementDistances [z] [y] [x];
							if (doubleThisDistance >= 0)
							{
								// We can get there, eventually
								if ((doubleDestinationDistance == null) || (doubleThisDistance < doubleDestinationDistance))
								{
									doubleDestinationDistance = doubleThisDistance;
									destinations.clear ();
									destinations.add (new MapCoordinates3DEx (x, y, z));
								}
								else if (doubleThisDistance == doubleDestinationDistance)
									destinations.add (new MapCoordinates3DEx (x, y, z));
							}
						}
					}
				}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable underdefended locations
		else
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to go overdefend " + chosenLocation + " which is " + doubleDestinationDistance + " double-moves away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
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

	/**
	 * @return AI decisions about cities
	 */
	public final CityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final CityAI ai)
	{
		cityAI = ai;
	}
}
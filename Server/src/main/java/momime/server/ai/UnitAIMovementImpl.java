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

import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.TileType;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.movement.OverlandMovementCell;
import momime.common.utils.MemoryGridCellUtils;

/**
 * Provides a method for processing each movement code that the AI uses to decide where to send units overland.
 * 
 * Each of these methods must be able to operate in "test" mode, so we just test to see if the unit has something
 * to do using that code, without actually doing it.
 */
public final class UnitAIMovementImpl implements UnitAIMovement
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (UnitAIMovementImpl.class);
	
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
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param underdefendedLocations Locations which are either ours (cities/towers) but lack enough defence, or not ours but can be freely captured (empty lairs/cities/etc)
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_Reinforce (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<AIDefenceLocation> underdefendedLocations, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		AIMovementDistance bestDistanceSoFar = null;
		
		// Check all locations we want to head to and find the closest one (or multiple closest ones)
		for (final AIDefenceLocation location : underdefendedLocations)
		{
			final OverlandMovementCell cell = moves [location.getMapLocation ().getZ ()] [location.getMapLocation ().getY ()] [location.getMapLocation ().getX ()];
			if (cell != null)
			{
				// We can get there, eventually
				final int doubleThisDistance = cell.getDoubleMovementDistance ();
				final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
					getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location.getMapLocation ()));
				
				if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
				{
					bestDistanceSoFar = thisDistance;
					destinations.clear ();
					destinations.add (location.getMapLocation ());
				}
				else if (thisDistance.equals (bestDistanceSoFar))
					destinations.add (location.getMapLocation ());
			}
		}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable underdefended locations
		else
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to go help reinforce underdefended location at " + chosenLocation + " which is " + bestDistanceSoFar + " away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		return decision;
	}

	/**
	 * AI tries to move units to attack defended stationary locations (nodes/lairs/towers/cities) where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_AttackStationary (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		final int ourCurrentRating = units.totalCombatUnitCurrentRatings ();
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		AIMovementDistance bestDistanceSoFar = null;

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
						final OverlandMovementCell cell = moves [z] [y] [x];
						if (((cityData != null) || ((!isRaiders) && (getMemoryGridCellUtils ().isNodeLairTower (terrainData, db)))) &&
							(enemyUnitStack != null) && (ourCurrentRating > enemyUnitStack.totalCombatUnitCurrentRatings ()) && (cell != null))
						{
							// We can get there eventually, and stand a chance of beating them
							final int doubleThisDistance = cell.getDoubleMovementDistance ();
							final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);
							
							final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
								getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
							
							if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
							{
								bestDistanceSoFar = thisDistance;
								destinations.clear ();
								destinations.add (location);
							}
							else if (thisDistance.equals (bestDistanceSoFar))
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
			log.debug ("Unit movement AI - Decided to attack stationary target at " + chosenLocation + " which is " + bestDistanceSoFar + " away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		return decision;
	}
	
	/**
	 * AI tries to move units to attack enemy unit stacks wandering around the map where the sum of our UARs > the sum of their UARs.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_AttackWandering (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final AIUnitsAndRatings [] [] [] enemyUnits, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		final int ourCurrentRating = units.totalCombatUnitCurrentRatings ();
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		AIMovementDistance bestDistanceSoFar = null;

		for (int z = 0; z < sys.getDepth (); z++)
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final OverlandMapTerrainData terrainData = mc.getTerrainData ();
					final OverlandMapCityData cityData = mc.getCityData ();
					final AIUnitsAndRatings enemyUnitStack = enemyUnits [z] [y] [x];
					final OverlandMovementCell cell = moves [z] [y] [x];
					if ((cityData == null) && (!getMemoryGridCellUtils ().isNodeLairTower (terrainData, db)) &&
						(enemyUnitStack != null) && (ourCurrentRating > enemyUnitStack.totalCombatUnitCurrentRatings ()) && (cell != null))
					{
						// We can get there eventually, and stand a chance of beating them
						final int doubleThisDistance = cell.getDoubleMovementDistance ();
						final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);
						
						final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
							getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
						
						if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
						{
							bestDistanceSoFar = thisDistance;
							destinations.clear ();
							destinations.add (location);
						}
						else if (thisDistance.equals (bestDistanceSoFar))
							destinations.add (location);
					}
				}
		
		final AIMovementDecision decision;
		if (destinations.isEmpty ())
			decision = null;		// No reachable wandering enemy units that we stand a chance of beating
		else
		{
			final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
			log.debug ("Unit movement AI - Decided to attack wandering target at " + chosenLocation + " which is " + bestDistanceSoFar + " away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		return decision;
	}

	/**
	 * AI tries to move units to scout any unknown terrain that is adjacent to at least one tile that we know to be land.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_ScoutLand (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db, final int playerID) throws RecordNotFoundException
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		AIMovementDistance bestDistanceSoFar = null;
		
		for (int z = 0; z < sys.getDepth (); z++)
		{
			final List<MapCoordinates2DEx> ourCitiesOnPlane = getCityAI ().listOurCitiesOnPlane (playerID, z, terrain, sys);
			
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					if ((mc.getTerrainData () == null) || (mc.getTerrainData ().getTileTypeID () == null))
					{
						final OverlandMovementCell cell = moves [z] [y] [x];
						if (cell != null)
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
										final TileType tileType = db.findTileType (terrainData.getTileTypeID (), "considerUnitMovement_ScoutLand");
										if ((tileType.isLand () != null) && (tileType.isLand ()))
											found = true;
									}
								}
							}

							if (found)
							{
								final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);

								// Add on fudge factor to make us want to scout areas close to cities first
								final int doubleThisDistance = cell.getDoubleMovementDistance ();
								
								final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance + getCityAI ().findDistanceToClosestCity (x, y, ourCitiesOnPlane, sys),
									getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
								
								if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
								{
									bestDistanceSoFar = thisDistance;
									destinations.clear ();
									destinations.add (location);
								}
								else if (thisDistance.equals (bestDistanceSoFar))
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
			log.debug ("Unit movement AI - Decided to go scout unknown land at " + chosenLocation + " which is " + bestDistanceSoFar + " away, including fudge factor");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		return decision;
	}
	
	/**
	 * AI tries to move units to scout any unknown terrain.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param playerID Player who is moving
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_ScoutAll (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final int playerID)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		AIMovementDistance bestDistanceSoFar = null;
		
		for (int z = 0; z < sys.getDepth (); z++)
		{
			final List<MapCoordinates2DEx> ourCitiesOnPlane = getCityAI ().listOurCitiesOnPlane (playerID, z, terrain, sys);
			
			for (int y = 0; y < sys.getHeight (); y++)
				for (int x = 0; x < sys.getWidth (); x++)
				{
					final MemoryGridCell mc = terrain.getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					if ((mc.getTerrainData () == null) || (mc.getTerrainData ().getTileTypeID () == null))
					{
						final OverlandMovementCell cell = moves [z] [y] [x];
						if (cell != null)
						{
							final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);

							// Add on fudge factor to make us want to scout areas close to cities first
							final int doubleThisDistance = cell.getDoubleMovementDistance ();
							final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance + getCityAI ().findDistanceToClosestCity (x, y, ourCitiesOnPlane, sys),
								getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
							
							if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
							{
								bestDistanceSoFar = thisDistance;
								destinations.clear ();
								destinations.add (location);
							}
							else if (thisDistance.equals (bestDistanceSoFar))
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
			log.debug ("Unit movement AI - Decided to go scout unknown scenery (possibly not land) at " + chosenLocation + " which is " + bestDistanceSoFar + " away, including fudge factor");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		return decision;
	}

	/**
	 * AI looks to see if any defended locations (nodes/lairs/towers/cities) are too well defended to attack at the moment,
	 * and if it can see any then will look to merge together our units into a bigger stack.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
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
	public final AIMovementDecision considerUnitMovement_JoinStack (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<AIUnitsAndRatings> ourUnitsInSameCategory, final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders,
		final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		// First of all we have to find the weakest enemy unit stack that we can reach but that is still too strong for us to fight alone
		final int ourCurrentRating = units.totalCombatUnitCurrentRatings ();
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
						final int enemyUnitStackRating = (enemyUnitStack == null) ? 0 : enemyUnitStack.totalCombatUnitCurrentRatings (); 
						if (((cityData != null) || ((!isRaiders) && (getMemoryGridCellUtils ().isNodeLairTower (terrainData, db)))) &&
							(enemyUnitStack != null) && (enemyUnitStackRating >= ourCurrentRating) && (moves [z] [y] [x] != null))
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
			AIMovementDistance bestDistanceSoFar = null;
			
			for (final AIUnitsAndRatings ourUnitStack : ourUnitsInSameCategory)
				if (ourUnitStack != units)
				{
					final MapCoordinates3DEx unitStackLocation = (MapCoordinates3DEx) ourUnitStack.get (0).getUnit ().getUnitLocation ();
					final OverlandMovementCell cell = moves [unitStackLocation.getZ ()] [unitStackLocation.getY ()] [unitStackLocation.getX ()];
					if (cell != null)
					{
						mergedStackRating = mergedStackRating + ourUnitStack.totalCombatUnitCurrentRatings ();
						
						final MapCoordinates3DEx location = new MapCoordinates3DEx (unitStackLocation);
						
						final int doubleThisDistance = cell.getDoubleMovementDistance ();
						final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
							getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
						
						if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
						{
							bestDistanceSoFar = thisDistance;
							destinations.clear ();
							destinations.add (location);
						}
						else if (thisDistance.equals (bestDistanceSoFar))
							destinations.add (location);
					}
				}

			if ((mergedStackRating <= weakestEnemyUnitStackWeCannotBeat) || (destinations.isEmpty ()))
				decision = null;		// No reachable friendly unit stacks that would be useful for us to merge with
			else
			{
				final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
				log.debug ("Unit movement AI - Decided to go join up with our other unit stack at " + chosenLocation + " which is " + bestDistanceSoFar + " away");
				decision = new AIMovementDecision (chosenLocation);
			}
		}		
		
		return decision;
	}

	/**
	 * AI looks for a transport to get in (or stay where we are if we are already in one).
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_GetInTransport (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		log.warn ("AI movement code GET_IN_TRANSPORT is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		return decision;
	}

	/**
	 * AI looks for any of our locations (nodes/cities/towers) that we can reach, regardless of if they already have plenty of defence.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param enemyUnits Array of enemy unit ratings populated by calculateUnitRatingsAtEveryMapCell
	 * @param isRaiders Whether it is the raiders player
	 * @param terrain Player knowledge of terrain
	 * @param sys Overland map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return See AIMovementDecision for explanation of return values
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the database
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_Overdefend (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final AIUnitsAndRatings [] [] [] enemyUnits, final boolean isRaiders, final MapVolumeOfMemoryGridCells terrain, final CoordinateSystem sys, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
		AIMovementDistance bestDistanceSoFar = null;
		
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
								((!isRaiders) && (getMemoryGridCellUtils ().isNodeLairTower (terrainData, db)))))
						{
							final OverlandMovementCell cell = moves [z] [y] [x];
							if (cell != null)
							{
								// We can get there, eventually
								final MapCoordinates3DEx location = new MapCoordinates3DEx (x, y, z);
								
								final int doubleThisDistance = cell.getDoubleMovementDistance ();
								final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
									getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
								
								if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
								{
									bestDistanceSoFar = thisDistance;
									destinations.clear ();
									destinations.add (location);
								}
								else if (thisDistance.equals (bestDistanceSoFar))
									destinations.add (location);
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
			log.debug ("Unit movement AI - Decided to go overdefend " + chosenLocation + " which is " + bestDistanceSoFar + " away");
			decision = new AIMovementDecision (chosenLocation);
		}
		
		return decision;
	}

	/**
	 * AI looks for a good place for settlers to build a city
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param desiredCityLocations Locations where we want to put cities
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_BuildCity (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<MapCoordinates3DEx> desiredCityLocations, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		// If we have no idea where to put a city, then nothing to do
		final AIMovementDecision decision;
		if ((desiredCityLocations == null) || (desiredCityLocations.size () == 0))
			decision = null;
		
		// If we're already at any of the right spots, then go ahead and make a city
		else if (desiredCityLocations.contains (currentLocation))
		{
			log.debug ("Unit movement AI - Decided to stay at " + currentLocation + " and build a city here");
			decision = new AIMovementDecision (UnitSpecialOrder.BUILD_CITY);
		}
		
		// Find the closest location where we want to put a city and head there
		else
		{
			final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
			AIMovementDistance bestDistanceSoFar = null;
			
			// Check all locations we want to head to and find the closest one (or multiple closest ones)
			for (final MapCoordinates3DEx location : desiredCityLocations)
			{
				final OverlandMovementCell cell = moves [location.getZ ()] [location.getY ()] [location.getX ()];
				if (cell != null)
				{
					// We can get there, eventually
					final int doubleThisDistance = cell.getDoubleMovementDistance ();
					final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
						getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
					
					if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
					{
						bestDistanceSoFar = thisDistance;
						destinations.clear ();
						destinations.add (location);
					}
					else if (thisDistance.equals (bestDistanceSoFar))
						destinations.add (location);
				}
			}
			
			if (destinations.isEmpty ())
				decision = null;		// No reachable build locations
			else
			{
				final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
				log.debug ("Unit movement AI - Decided to go head towards " + chosenLocation + " to build a city there which is " + bestDistanceSoFar + " away");
				decision = new AIMovementDecision (chosenLocation);
			}
		}
		
		return decision;
	}

	/**
	 * AI looks for a good place for engineers to build a road
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param desiredRoadLocations Locations where we want to put cities
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	 @Override
	public final AIMovementDecision considerUnitMovement_BuildRoad (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<MapCoordinates3DEx> desiredRoadLocations, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		// If we have nowhere we need to put road, then nothing to do
		final AIMovementDecision decision;
		if ((desiredRoadLocations == null) || (desiredRoadLocations.size () == 0))
			decision = null;
		
		// If we're already at any of the right spots, then go ahead and build a road
		else if (desiredRoadLocations.contains (currentLocation))
		{
			log.debug ("Unit movement AI - Decided to stay at " + currentLocation + " and build a road here");
			decision = new AIMovementDecision (UnitSpecialOrder.BUILD_ROAD);
		}
		
		// Find the closest location where we want to put a road and head there
		else
		{
			final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
			AIMovementDistance bestDistanceSoFar = null;
			
			// Check all locations we want to head to and find the closest one (or multiple closest ones)
			for (final MapCoordinates3DEx location : desiredRoadLocations)
			{
				final OverlandMovementCell cell = moves [location.getZ ()] [location.getY ()] [location.getX ()];
				if (cell != null)
				{
					// We can get there, eventually
					final int doubleThisDistance = cell.getDoubleMovementDistance ();
					final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
						getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
					
					if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
					{
						bestDistanceSoFar = thisDistance;
						destinations.clear ();
						destinations.add (location);
					}
					else if (thisDistance.equals (bestDistanceSoFar))
						destinations.add (location);
				}
			}
			
			if (destinations.isEmpty ())
				decision = null;		// No reachable build locations
			else
			{
				final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
				log.debug ("Unit movement AI - Decided to go head towards " + chosenLocation + " to build a road there which is " + bestDistanceSoFar + " away");
				decision = new AIMovementDecision (chosenLocation);
			}
		}
		
		return decision;
	}

	/**
	 * AI looks for any corrupted land that priests need to purify
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_Purify (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		log.warn ("AI movement code PURIFY is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		return decision;
	}

	/**
	 * AI looks for a node that a magic/guardian spirit can meld with
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param nodeCaptureLocations Locations where we have guarded nodes ready to capture
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_MeldWithNode (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves,
		final List<MapCoordinates3DEx> nodeCaptureLocations, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		// If we have no nodes to capture, then nothing to do
		final AIMovementDecision decision;
		if ((nodeCaptureLocations == null) || (nodeCaptureLocations.size () == 0))
			decision = null;
		
		// If we're already at any of the right spots, then go ahead and try to capture the node
		else if (nodeCaptureLocations.contains (currentLocation))
		{
			log.debug ("Unit movement AI - Decided to stay at " + currentLocation + " and try to capture the node here");
			decision = new AIMovementDecision (UnitSpecialOrder.MELD_WITH_NODE);
		}
		
		// Find the closest location where we want to capture a node and head there
		else
		{
			final List<MapCoordinates3DEx> destinations = new ArrayList<MapCoordinates3DEx> ();
			AIMovementDistance bestDistanceSoFar = null;
			
			// Check all locations we want to head to and find the closest one (or multiple closest ones)
			for (final MapCoordinates3DEx location : nodeCaptureLocations)
			{
				final OverlandMovementCell cell = moves [location.getZ ()] [location.getY ()] [location.getX ()];
				if (cell != null)
				{
					// We can get there, eventually
					final int doubleThisDistance = cell.getDoubleMovementDistance ();
					final AIMovementDistance thisDistance = new AIMovementDistance (doubleThisDistance,
						getCoordinateSystemUtils ().determineStep2DDistanceBetween (sys, currentLocation, location));
					
					if ((bestDistanceSoFar == null) || (thisDistance.isShorterThan (bestDistanceSoFar)))
					{
						bestDistanceSoFar = thisDistance;
						destinations.clear ();
						destinations.add (location);
					}
					else if (thisDistance.equals (bestDistanceSoFar))
						destinations.add (location);
				}
			}
			
			if (destinations.isEmpty ())
				decision = null;		// No reachable nodes
			else
			{
				final MapCoordinates3DEx chosenLocation = destinations.get (getRandomUtils ().nextInt (destinations.size ()));
				log.debug ("Unit movement AI - Decided to go head towards " + chosenLocation + " to capture a node there which is " + bestDistanceSoFar + " away");
				decision = new AIMovementDecision (chosenLocation);
			}
		}
		
		return decision;
	}
	
	/**
	 * AI transports look for a suitable island to carry units to, if we are holding any.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_CarryUnits (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		log.warn ("AI movement code CARRY_UNITS is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		return decision;
	}
	
	/**
	 * AI transports that are empty head for any islands where any unit stacks went on OVERDEFEND.
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_LoadUnits (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		log.warn ("AI movement code LOAD_UNITS is not yet implemented");
		
		final AIMovementDecision decision = null;
		
		return decision;
	}
	
	/**
	 * If we are on the same plane as our Wizards' Fortress, then head the island that it is on.
	 * (This is intended for transport ships that have nothing better to do, so we're assuming we can't actually get *onto* the island).
	 * 
	 * @param units The units to move
	 * @param moves Array listing all cells we can reach and the paths to get there
	 * @param sys Overland map coordinate system
	 * @return See AIMovementDecision for explanation of return values
	 */
	@Override
	public final AIMovementDecision considerUnitMovement_FortressIsland (final AIUnitsAndRatings units, final OverlandMovementCell [] [] [] moves, final CoordinateSystem sys)
	{
		final MapCoordinates3DEx currentLocation = (MapCoordinates3DEx) units.get (0).getUnit ().getUnitLocation ();
		
		log.warn ("AI movement code FORTRESS_ISLAND is not yet implemented");
		
		final AIMovementDecision decision = null;
		
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
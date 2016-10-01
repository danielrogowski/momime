package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.random.RandomUtils;

import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;

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
			decision = null;		// No reachable underdefended locations
		else
			decision = new AIMovementDecision (destinations.get (getRandomUtils ().nextInt (destinations.size ())));
		
		log.trace ("Exiting considerUnitMovement_ScoutAll = " + decision);
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
}
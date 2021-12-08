package momime.server.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Event;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.server.MomSessionVariables;

/**
 * Deals with random events which pick a city target:
 * Great Meteor, Earthquake, Plague, Rebellion, Depletion, New Minerals, Population Boom
 * Also Diplomatic Marriage, but here we're picking a raider city rather than one owned by a wizard
 */
public final class RandomCityEventsImpl implements RandomCityEvents
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RandomCityEventsImpl.class);
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Rolls random events */
	private RandomEvents randomEvents;
	
	/**
	 * Can only call this on events that are targeted at cities
	 * 
	 * @param event Event we want to find a target for
	 * @param cityLocation Location we are considering for the event
	 * @param cityOwnerID Will only consider cities with the specified owner
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the city is a valid target for the event or not
	 * @throws RecordNotFoundException If we can't find the definition for a unit stationed in the city
	 */
	@Override
	public final boolean isCityValidTargetForEvent (final Event event, final MapCoordinates3DEx cityLocation, final int cityOwnerID, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		boolean valid = false;

		final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		if ((cityData != null) && (cityData.getCityOwnerID () == cityOwnerID))
		{
			// Do we need to make sure there's a mineral in the area?  Depletion
			if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DEPLETION))
			{
				final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
				for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
					if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
					{
						final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
							(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
						if (event.getEventMapFeatureID ().contains (terrainData.getMapFeatureID ()))
							valid = true;
					}
			}
			
			// Do we need to make sure there's a hill or mountain in the area without a mineral?  New Minerals
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_NEW_MINERALS))
			{
				final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
				for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
					if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
					{
						final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
							(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
						if ((event.getEventTileTypeID ().contains (terrainData.getTileTypeID ())) && (terrainData.getMapFeatureID () == null))
							valid = true;
					}
			}
			
			// Do we need to make sure it isn't the wizard's fortress, there's no heroes, and not too many summoned units here?  Rebellion
			else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_REBELLION))
			{
				if (getMemoryBuildingUtils ().findBuilding (mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding (),
					cityLocation, CommonDatabaseConstants.BUILDING_FORTRESS) != null)
				{
					// How many of each type of unit type are here?
					boolean heroFound = false;
					int normalUnitCount = 0;
					int summonedUnitCount = 0;
					
					final Iterator<MemoryUnit> iter = mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().iterator ();
					while ((!heroFound) && (iter.hasNext ()))
					{
						final MemoryUnit mu = iter.next ();
						if ((cityLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE))
						{
							final UnitEx unitDef = mom.getServerDB ().findUnit (mu.getUnitID (), "isCityValidTargetForEvent");
							if (unitDef.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
								heroFound = true;
							else if (unitDef.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL))
								normalUnitCount++;
							else
								summonedUnitCount++;							
						}
					}
					
					valid = (!heroFound) && (summonedUnitCount <= normalUnitCount);
				}
			}
			
			// Population Boom and Plague can only be applied if the city doesn't already have some other population effect in place
			else if ((event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_POPULATION_BOOM)) ||
				(event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_PLAGUE)))
				
				valid = (cityData.getPopulationEventID () == null);
			
			// Other city events have no special requirements
			else
				valid = true;
		}
		
		return valid;
	}

	/**
	 * @param event Event to trigger
	 * @param targetWizard Wizard the event is being triggered for
	 * @param cityLocation City the event is being triggered for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void triggerCityEvent (final Event event, final PlayerServerDetails targetWizard, final MapCoordinates3DEx cityLocation, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
		
		// Do we need to pick an existing mineral in the area?  Depletion
		if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_DEPLETION))
		{
			final List<MapCoordinates3DEx> mineralLocations = new ArrayList<MapCoordinates3DEx> ();
			
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if (event.getEventMapFeatureID ().contains (terrainData.getMapFeatureID ()))
						mineralLocations.add (coords);
				}
					
			if (mineralLocations.isEmpty ())
				throw new MomException ("Event " + event.getEventID () + " was picked for wizard " + targetWizard.getPlayerDescription ().getPlayerName () +
					" and city " + cityLocation + ", but now can't find a suitable mineral");
			
			final MapCoordinates3DEx mineralLocation = mineralLocations.get (getRandomUtils ().nextInt (mineralLocations.size ()));
			log.debug ("Depletion event, wizard " + targetWizard.getPlayerDescription ().getPlayerName () + ", city " + cityLocation +
				" will lose mineral at " + mineralLocation);
			
			final OverlandMapTerrainData mineralTerrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(mineralLocation.getZ ()).getRow ().get (mineralLocation.getY ()).getCell ().get (mineralLocation.getX ()).getTerrainData ();
			getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
				cityData.getCitySizeID (), cityData.getCityName (), mineralTerrainData.getMapFeatureID (), null, null, false, mom.getPlayers ());
		}
		
		// Do we need to pick a hill or mountain in the area without a mineral, as well as a new mineral to add?  New Minerals
		else if (event.getEventID ().equals (CommonDatabaseConstants.EVENT_ID_NEW_MINERALS))
		{
			final List<MapCoordinates3DEx> mineralLocations = new ArrayList<MapCoordinates3DEx> ();
			
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
				{
					final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
					if ((event.getEventTileTypeID ().contains (terrainData.getTileTypeID ())) && (terrainData.getMapFeatureID () == null))
						mineralLocations.add (coords);
				}
					
			if (mineralLocations.isEmpty ())
				throw new MomException ("Event " + event.getEventID () + " was picked for wizard " + targetWizard.getPlayerDescription ().getPlayerName () +
					" and city " + cityLocation + ", but now can't find a suitable hill/mountain without minerals");
			
			final MapCoordinates3DEx mineralLocation = mineralLocations.get (getRandomUtils ().nextInt (mineralLocations.size ()));
			final String mapFeatureID = event.getEventMapFeatureID ().get (getRandomUtils ().nextInt (event.getEventMapFeatureID ().size ()));
			
			log.debug ("New Minerals event, wizard " + targetWizard.getPlayerDescription ().getPlayerName () + ", city " + cityLocation +
				" will gain mineral " + mapFeatureID + " at " + mineralLocation);
			getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
				cityData.getCitySizeID (), cityData.getCityName (), mapFeatureID, null, null, false, mom.getPlayers ());
		}
		
		// Other city events have no additional random elements to roll
		else
		{
			getRandomEvents ().sendRandomEventMessage (event.getEventID (), targetWizard.getPlayerDescription ().getPlayerID (),
				cityData.getCitySizeID (), cityData.getCityName (), null, null, null, false, mom.getPlayers ());
		}
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
	 * @return Rolls random events
	 */
	public final RandomEvents getRandomEvents ()
	{
		return randomEvents;
	}

	/**
	 * @param e Rolls random events
	 */
	public final void setRandomEvents (final RandomEvents e)
	{
		randomEvents = e;
	}
}
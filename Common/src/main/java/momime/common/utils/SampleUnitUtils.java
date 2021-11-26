package momime.common.utils;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;

/**
 * Lots of places in the code need to create a properly initialized test unit, so better to have a common method for this rather than
 * repeating it all over the place.  Also makes mocking it in unit tests easier.
 */
public interface SampleUnitUtils
{
	/**
	 * @param unitID Type of unit to create
	 * @param owningPlayerID Player who owns the unit
	 * @param startingExperience Initial experience; if -1 or null then experience won't be added into skill list
	 * @param db Lookup lists built over the XML database
	 * @return Sample unit, if we only need the AvailableUnit basic object
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 */
	public AvailableUnit createSampleAvailableUnit (final String unitID, final int owningPlayerID, final Integer startingExperience, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * @param unitID Type of unit to create
	 * @param owningPlayerID Player who owns the unit
	 * @param startingExperience Initial experience; if -1 or null then experience won't be added into skill list
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Fully initialized sample unit
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public ExpandedUnitDetails createSampleUnit (final String unitID, final int owningPlayerID, final Integer startingExperience,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * Creates a sample unit as it will be constructed from a city, with weapon grade (from e.g. adamantium ore if we have an Alchemists' guild)
	 * and experience (from e.g. War College or Altar of Battle spell) all set correctly.
	 * 
	 * @param unitID Type of unit to create
	 * @param owningPlayer Player who owns the unit
	 * @param cityLocation City where the unit is being constructed
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return Sample unit, if we only need the AvailableUnit basic object
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 */
	public AvailableUnit createSampleAvailableUnitFromCity (final String unitID, final PlayerPublicDetails owningPlayer, final MapCoordinates3DEx cityLocation,
		final FogOfWarMemory mem, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException;
	
	/**
	 * Creates a sample unit as it will be constructed from a city, with weapon grade (from e.g. adamantium ore if we have an Alchemists' guild)
	 * and experience (from e.g. War College or Altar of Battle spell) all set correctly.
	 * 
	 * @param unitID Type of unit to create
	 * @param owningPlayer Player who owns the unit
	 * @param cityLocation City where the unit is being constructed
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @param db Lookup lists built over the XML database
	 * @return Fully initialized sample unit
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public ExpandedUnitDetails createSampleUnitFromCity (final String unitID, final PlayerPublicDetails owningPlayer, final MapCoordinates3DEx cityLocation,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem,
		final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}
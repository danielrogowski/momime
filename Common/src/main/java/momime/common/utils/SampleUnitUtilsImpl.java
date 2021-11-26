package momime.common.utils;

import java.util.List;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;

/**
 * Lots of places in the code need to create a properly initialized test unit, so better to have a common method for this rather than
 * repeating it all over the place.  Also makes mocking it in unit tests easier.
 */
public final class SampleUnitUtilsImpl implements SampleUnitUtils
{
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;

	/**
	 * @param unitID Type of unit to create
	 * @param owningPlayerID Player who owns the unit
	 * @param startingExperience Initial experience; if -1 or null then experience won't be added into skill list
	 * @param db Lookup lists built over the XML database
	 * @return Sample unit, if we only need the AvailableUnit basic object
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 */
	@Override
	public final AvailableUnit createSampleAvailableUnit (final String unitID, final int owningPlayerID, final Integer startingExperience, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final AvailableUnit sample = new AvailableUnit ();
		sample.setUnitID (unitID);
		sample.setOwningPlayerID (owningPlayerID);

		getUnitUtils ().initializeUnitSkills (sample, startingExperience, db);
		
		return sample;
	}
	
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
	@Override
	public final ExpandedUnitDetails createSampleUnit (final String unitID, final int owningPlayerID, final Integer startingExperience,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final AvailableUnit sample = createSampleAvailableUnit (unitID, owningPlayerID, startingExperience, db);
		return getExpandUnitDetails ().expandUnitDetails (sample, null, null, null, players, mem, db);
	}

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
	@Override
	public final AvailableUnit createSampleAvailableUnitFromCity (final String unitID, final PlayerPublicDetails owningPlayer, final MapCoordinates3DEx cityLocation,
		final FogOfWarMemory mem, final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) owningPlayer.getPersistentPlayerPublicKnowledge ();
		
		final AvailableUnit sample = new AvailableUnit ();
		sample.setUnitID (unitID);
		sample.setOwningPlayerID (owningPlayer.getPlayerDescription ().getPlayerID ());
		sample.setUnitLocation (cityLocation);

		final int startingExperience = getMemoryBuildingUtils ().experienceFromBuildings
			(mem.getBuilding (), mem.getMaintainedSpell (), cityLocation, db);
		
		sample.setWeaponGrade (getUnitCalculations ().calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
			(mem.getBuilding (), mem.getMap (), cityLocation, pub.getPick (), overlandMapCoordinateSystem, db));

		getUnitUtils ().initializeUnitSkills (sample, startingExperience, db);
		
		return sample;
	}

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
	@Override
	public final ExpandedUnitDetails createSampleUnitFromCity (final String unitID, final PlayerPublicDetails owningPlayer, final MapCoordinates3DEx cityLocation,
		final List<? extends PlayerPublicDetails> players, final FogOfWarMemory mem,
		final CoordinateSystem overlandMapCoordinateSystem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final AvailableUnit sample = createSampleAvailableUnitFromCity (unitID, owningPlayer, cityLocation, mem, overlandMapCoordinateSystem, db);
		return getExpandUnitDetails ().expandUnitDetails (sample, null, null, null, players, mem, db);
	}
	
	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}
}
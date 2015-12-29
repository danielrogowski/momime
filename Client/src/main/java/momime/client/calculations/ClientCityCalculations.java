package momime.client.calculations;

import java.util.List;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.MomException;
import momime.common.database.Building;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;

/**
 * Client side only methods dealing with city calculations
 */
public interface ClientCityCalculations
{
	/**
	 * @param breakdown Results of unrest calculation
	 * @return Readable calculation details
	 */
	public String describeCityUnrestCalculation (final CityUnrestBreakdown breakdown);
	
	/**
	 * @param breakdown Results of growth calculation
	 * @return Readable calculation details
	 */
	public String describeCityGrowthRateCalculation (final CityGrowthRateBreakdown breakdown);
	
	/**
	 * @param calc Results of production calculation
	 * @return Readable calculation details
	 * @throws MomException If we find a breakdown entry that we don't know how to describe
	 */
	public String describeCityProductionCalculation (final CityProductionBreakdown calc) throws MomException;
	
	/**
	 * @param buildingID Building that we're constructing
	 * @param cityLocation City location
	 * @return Readable list of what constructing this building will then allow us to build, both buildings and units
	 */
	public String describeWhatBuildingAllows (final String buildingID, final MapCoordinates3DEx cityLocation);
	
	/**
	 * @param cityLocation City location
	 * @return List of all buildings that the player can choose between to construct at the city (including Housing and Trade Goods)
	 * @throws RecordNotFoundException If the race inhabiting the city cannot be found
	 */
	public List<Building> listBuildingsCityCanConstruct (final MapCoordinates3DEx cityLocation) throws RecordNotFoundException;
		
	/**
	 * @param cityLocation City location
	 * @return List of all units that the player can choose between to construct at the city
	 */
	public List<Unit> listUnitsCityCanConstruct (final MapCoordinates3DEx cityLocation);
}
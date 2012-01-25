package momime.editors.server.raceCannotBuild;

/**
 * Stores details of one building that a particular race cannot build and the reason(s) why not
 */
public class RaceImplicitCannotBuild
{
	/**
	 * The ID of the building that we cannot build
	 */
	private final String buildingId;
	
	/**
	 * The name of the building that we cannot build
	 */
	private final String buildingName;
	
	/**
	 * List of building names that are the reasons why we implicitly cannot build this building
	 * If this building is explicitly listed in the XML file as one the race cannot build, this will be null 
	 */
	private final String reasons;
	
	/**
	 * Creates and fixes details of one building that a particular race cannot build and the reason(s) why not
	 * @param aBuildingId
	 * @param aBuildingName
	 * @param aReasons
	 */
	public RaceImplicitCannotBuild (String aBuildingId, String aBuildingName, String aReasons)
	{
		super ();
		
		buildingId = aBuildingId;
		buildingName = aBuildingName;
		reasons = aReasons;
	}

	/**
	 * @return The ID of the building that we cannot build
	 */
	public String getBuildingId ()
	{
		return buildingId;
	}

	/**
	 * @return The name of the building that we cannot build
	 */
	public String getBuildingName ()
	{
		return buildingName;
	}

	/**
	 * @return List of building names that are the reasons why we implicitly cannot build this building
	 * If this building is explicitly listed in the XML file as one the race cannot build, this will be null
	 */
	public String getReasons ()
	{
		return reasons;
	}
	
}

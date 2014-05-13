package momime.client.graphics.database;

import java.util.List;

import momime.client.graphics.database.v0_9_5.CityImage;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.graphics.database.v0_9_5.Unit;
import momime.client.graphics.database.v0_9_5.Wizard;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MemoryBuilding;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Describes operations that we need to support over the graphics XML file
 */
public interface GraphicsDatabaseEx
{
	/**
	 * @param pickID Pick ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Pick object
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	public Pick findPick (final String pickID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	public Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException;

	/**
	 * @param unitID Unit ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Unit object
	 * @throws RecordNotFoundException If the unitID doesn't exist
	 */
	public Unit findUnit (final String unitID, final String caller) throws RecordNotFoundException;
	
	/**
	 * @param tileSetID Tile set ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Tile set object
	 * @throws RecordNotFoundException If the tileSetID doesn't exist
	 */
	public TileSetEx findTileSet (final String tileSetID, final String caller) throws RecordNotFoundException;

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	public MapFeatureEx findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException;
	
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
	public CityImage findBestCityImage (final String citySizeID, final MapCoordinates3DEx cityLocation,
		final List<MemoryBuilding> buildings, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return List of all city view elemenets (backgrounds, buildings, spell effects and so on)
	 */
    public List<CityViewElement> getCityViewElement ();
	
	/**
	 * @param animationID Animation ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Animation object
	 * @throws RecordNotFoundException If the animationID doesn't exist
	 */
	public AnimationEx findAnimation (final String animationID, final String caller) throws RecordNotFoundException;
}

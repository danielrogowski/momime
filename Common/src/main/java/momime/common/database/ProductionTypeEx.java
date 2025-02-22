package momime.common.database;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adds a map over the production values, so we can find their images faster
 */
public final class ProductionTypeEx extends ProductionType
{
	/** Map of production values to image filenames */
	private Map<String, String> productionValuesMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		productionValuesMap = getProductionTypeImage ().stream ().collect (Collectors.toMap (i -> i.getProductionValue (), i -> i.getProductionImageFile ()));
	}
	
	/**
	 * NB. This is a bit of a special case, normally we expect all elements to be present in the graphics DB and throw exceptions if they aren't.
	 * But here its acceptable for there to be no '10' or '-10' image, in which case the code will just draw 10 '1's or '-1's instead.
	 * 
	 * @param productionValue Production value to search for (1, 10, -1, -10)
	 * @return Filename for the image of this production value; or null if no image exists for it
	 */
	public final String findProductionValueImageFile (final String productionValue)
	{
		return productionValuesMap.get (productionValue);
	}
}
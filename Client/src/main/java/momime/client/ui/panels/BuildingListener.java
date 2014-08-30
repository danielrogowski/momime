package momime.client.ui.panels;

/**
 * Interface for listening to clicks on buildings in the CityViewPanel
 */
public interface BuildingListener
{
	/**
	 * @param buildingID Building that was clicked on
	 * @throws Exception If there is a problem
	 */
	public void buildingClicked (final String buildingID) throws Exception;
}
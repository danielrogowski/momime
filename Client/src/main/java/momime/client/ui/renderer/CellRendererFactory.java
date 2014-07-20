package momime.client.ui.renderer;

/**
 * Creates cell renderers from prototype definitions in Spring
 */
public interface CellRendererFactory
{
	/**
	 * @return Renderer for writing a building name and its image in a JList
	 */
	public BuildingListCellRenderer createBuildingListCellRenderer ();
}
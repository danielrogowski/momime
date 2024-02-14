package momime.client.messages.process;

/**
 * Various updates we get from the server can require triggering updates to various UI elements.
 * This lists all the UI updates that might be necessary. *
 */
public enum UpdateUIElement
{
	/** Some item visible on the overland map was changed */
	REGENERATE_OVERLAND_MAP_BITMAPS,
	
	/** Some item visible on the "1 pixel per map cell" map was changed */
	REGENERATE_MINI_MAP_BITMAPS,
	
	/** Some item visible on our list of cities was changed */
	REFRESH_CITIES_LIST;	
}
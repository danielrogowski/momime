package momime.client.ui.panels;

/**
 * Different ways we can draw a city view element (or not)
 */
public enum DrawCityViewElement
{
	/** No, because we don't have the building, spell, etc drawn by this element */
	NO,
	
	/** Yes, we do have the building, spell, etc */
	YES,
	
	/** Yes draw it, but use rubble image instead of what's defined in the city view element */
	RUBBLE;
}
package momime.client.ui.components;

/**
 * Creates cell renderers from prototype definitions in Spring
 */
public interface UIComponentFactory
{
	/**
	 * @return Button for the main map screen which selects and deselects units; showing the player's colour, an image of the unit, a health indicator, experience and weapon grade
	 */
	public SelectUnitButton createSelectUnitButton ();
	
	/**
	 * @return Mana/Research/Skill slider for the magic screen that allows sliding the value up and down and clicking the head of the staff to lock it
	 */
	public MagicSlider createMagicSlider ();
}
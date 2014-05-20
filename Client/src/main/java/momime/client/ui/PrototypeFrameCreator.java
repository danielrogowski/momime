package momime.client.ui;

/**
 * Allows spring to create instances of frames that we allow multiple copies of, like message boxes and the city screen, and perform all necessary injections
 */
public interface PrototypeFrameCreator
{
	/**
	 * @return New message box UI
	 */
	public MessageBoxUI createMessageBox ();

	/**
	 * @return New calculation box UI
	 */
	public CalculationBoxUI createCalculationBox ();
	
	/**
	 * @return New edit string UI
	 */
	public EditStringUI createEditString ();	
	
	/**
	 * @return New city view UI
	 */
	public CityViewUI createCityView ();
}

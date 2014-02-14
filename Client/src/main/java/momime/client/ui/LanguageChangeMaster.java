package momime.client.ui;

/**
 * All screens (unless maybe some trivial screen made only of images and no text) register themselves by calling the
 * method on this interface, to basically say "please notify me if the selected language changes"
 */
public interface LanguageChangeMaster
{
	/**
	 * Asks that the master implementing this interface call the listener's .languageChanged () method whenever the selected language changes
	 * @param listener Screen on which to call the .languageChanged () method
	 */
	public void addLanuageChangeListener (final MomClientUI listener);
}

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
	 * @return New edit string UI
	 */
	public EditStringUI createEditString ();	
}

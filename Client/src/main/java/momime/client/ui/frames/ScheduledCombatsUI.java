package momime.client.ui.frames;

import java.io.IOException;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UI displaying all scheduled combats that us and others need to play out in the combat phase of this simultaneous turn
 */
public final class ScheduledCombatsUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ScheduledCombatsUI.class);

	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		
		log.trace ("Exiting init");
	}
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		log.trace ("Exiting languageChanged");
	}
}
package momime.client.ui.frames;

import java.io.IOException;

import javax.swing.JFrame;

import momime.client.language.LanguageVariableUIImpl;

/**
 * Ancestor used by all of the UI frames.
 * 
 * The way this basically works is that, we aren't allowed to create/configure Swing components outside of the event dispatcher thread.
 * Spring will start up in the JVM's initial thread, so we can't create Swing components from Spring.  So instead Spring creates these
 * "container" objects and injects any resources like fonts that we might need, and injects the Language XML holder.
 * 
 * At some later point, the app then calls setVisible (true) for the first time, which must be done from the event dispatcher thread, at which
 * point init () gets called to create all the Swing components.  setVisible is done in such a way that the screen can be subsequently hidden
 * and then redisplayed, and the Swing components won't need to be recreated.
 */
public abstract class MomClientFrameUI extends LanguageVariableUIImpl
{
	/** The actual frame */
	private JFrame frame;

	/**
	 * This is package-private so it can be accessed for setLocationRelativeTo () methods
	 * @return The actual frame
	 */
	final JFrame getFrame ()
	{
		return frame;
	}

	/**
	 * @return Whether this screen is currently displayed or not
	 */
	public final boolean isVisible ()
	{
		// If haven't done init yet, then obviously it isn't visible
		return (frame == null) ? false : frame.isVisible ();
	}
	
	/**
	 * @param v Whether to display or hide this screen
	 * @throws IOException If a resource cannot be found
	 */
	public final void setVisible (final boolean v) throws IOException
	{
		if (frame == null)
		{
			frame = new JFrame ();
			init ();
			languageChanged ();
			getLanguageChangeMaster ().addLanuageChangeListener (this);
		}
		frame.setVisible (v);
	}
}
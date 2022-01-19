package momime.client.ui.frames;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	/** Class logger */
	private final static Log log = LogFactory.getLog (MomClientFrameUI.class);
	
	/** The actual frame */
	private JFrame frame;

	/** Whether clicks close the form (can't use this on prototype forms, since they'll only close with setVisible (false) and won't be tidied up properly) */
	private boolean closeOnClick = false;
	
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
			getLanguageChangeMaster ().addLanguageChangeListener (this);
			
			// If the window isn't resizeable, then hide the window border
			// but this means it has no title bar to enable it to be moved, so also set up dragging the background for it
			if (!frame.isResizable ())
			{
				frame.setUndecorated (true);
				
				final MouseAdapter undecoratedFrameMover = new MouseAdapter ()
				{
					/** The location the mouse was at when it was initially clicked */
					private Point mouseStart;

					/** The location the frame was at when the mouse was initially clicked */
					private Point frameStart;
					
					/**
					 * Record the current position of the mouse and frame, so we know where to drag from
					 */
					@Override
					public final void mousePressed (final MouseEvent e)
					{
						mouseStart = e.getLocationOnScreen ();
						frameStart = frame.getLocation ();
					}

					/**
					 * Update the position of the frame based on how far the mouse has moved
					 */
					@Override
					public final void mouseDragged (final MouseEvent e)
					{
						if (frameStart != null)
						{
							final Point mouseNow = e.getLocationOnScreen ();
							final Point frameNow = new Point (frameStart.x + mouseNow.x - mouseStart.x, frameStart.y + mouseNow.y - mouseStart.y);
							frame.setLocation (frameNow);
						}
					}
					
					/**
					 * Maybe close the form with a mouse click
					 */
					@Override
					public final void mouseClicked (@SuppressWarnings ("unused") final MouseEvent ev)
					{
						if (isCloseOnClick ())
							try
							{
								setVisible (false);
							}
							catch (final IOException e)
							{
								log.error (e, e);
							}
					}
				};
				
				frame.addMouseListener (undecoratedFrameMover);
				frame.addMouseMotionListener (undecoratedFrameMover);
			}

			// Build and position the frame
			getFrame ().setIconImage (getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/frameIcon.png"));
			getFrame ().pack ();
			getFrame ().setLocationRelativeTo (null);
		}
		frame.setVisible (v);
	}

	/**
	 * @return Whether clicks close the form
	 */
	final boolean isCloseOnClick ()
	{
		return closeOnClick;
	}

	/**
	 * @param close Whether clicks close the form
	 */
	final void setCloseOnClick (final boolean close)
	{
		closeOnClick = close;
	}
}
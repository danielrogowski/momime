package momime.client.ui.frames;

import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import jakarta.xml.bind.Marshaller;
import momime.client.config.MomImeClientConfig;
import momime.client.config.WindowID;
import momime.client.config.WindowPosition;
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
	private boolean closeOnClick;
	
	/** Enum value representing this window; null if its position isn't recorded in config file (for example scope=prototype windows) */
	private WindowID windowID;
	
	/** Whether the window being shown and hidden is recorded in the config file; only relevant if getWindowID does not return null */
	private boolean persistVisibility = true;

	/** Used when game is closing down and all windows are being hidden, so we don't record that hide into the config file */
	private boolean ignoreNextHide;
	
	/** Client config, containing various overland map settings */
	private MomImeClientConfig clientConfig;
	
	/** Marshaller for saving client config */
	private Marshaller clientConfigMarshaller;
	
	/** Location to save updated client config */
	private String clientConfigLocation;

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
			
			if ((getWindowID () != null) && (getClientConfig () != null))
			{
				// Restore window back to its saved position
				final WindowPosition windowPos = findWindowPosition ();
				if (windowPos != null)
				{
					getFrame ().setLocation (windowPos.getLeft (), windowPos.getTop ());
					
					if ((getFrame ().isResizable ()) && (windowPos.getWidth () != null) && (windowPos.getHeight () != null))
						getFrame ().setSize (windowPos.getWidth (), windowPos.getHeight ());
				}
				
				// Record changes to the frame in the config file
				getFrame ().addComponentListener (new ComponentListener ()
				{
					@Override
					public final void componentResized (@SuppressWarnings ("unused") final ComponentEvent e)
					{
						if (getFrame ().isResizable ())
						{
							final WindowPosition pos = findOrAddWindowPosition ();
							pos.setWidth (getFrame ().getWidth ());
							pos.setHeight (getFrame ().getHeight ());
							saveConfigFile ();
						}
					}

					@Override
					public final void componentMoved (@SuppressWarnings ("unused") final ComponentEvent e)
					{
						final WindowPosition pos = findOrAddWindowPosition ();
						pos.setLeft (getFrame ().getLocation ().x);
						pos.setTop (getFrame ().getLocation ().y);
						saveConfigFile ();
					}

					@Override
					public final void componentShown (@SuppressWarnings ("unused") final ComponentEvent e)
					{
						if (isPersistVisibility ())
						{
							final WindowPosition pos = findOrAddWindowPosition ();
							pos.setVisible (true);
							saveConfigFile ();
						}
					}

					@Override
					public final void componentHidden (@SuppressWarnings ("unused") final ComponentEvent e)
					{
						if (isPersistVisibility ())
						{
							if (ignoreNextHide)
								ignoreNextHide = false;
							else
							{
								final WindowPosition pos = findOrAddWindowPosition ();
								pos.setVisible (false);
								saveConfigFile ();
							}
						}
					}
				});
			}
		}
		frame.setVisible (v);
	}
	
	/**
	 * @return Window position entry from config file for this frame, or null if not found
	 */
	private final WindowPosition findWindowPosition ()
	{
		return getClientConfig ().getWindowPosition ().stream ().filter (p -> p.getWindowID () == getWindowID ()).findAny ().orElse (null);
	}
	
	/**
	 * @return Window position entry from config file for this frame
	 */
	private final WindowPosition findOrAddWindowPosition ()
	{
		WindowPosition pos = getClientConfig ().getWindowPosition ().stream ().filter (p -> p.getWindowID () == getWindowID ()).findAny ().orElse (null);
		if (pos == null)
		{
			pos = new WindowPosition ();
			pos.setWindowID (getWindowID ());
			getClientConfig ().getWindowPosition ().add (pos);
		}
		return pos;
	}

	/**
	 * After any change to the config options, we resave out the config XML immediately
	 */
	final void saveConfigFile ()
	{
		if (getClientConfigLocation () != null)
			try
			{
				getClientConfigMarshaller ().marshal (getClientConfig (), new File (getClientConfigLocation ()));
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
	}
	
	/**
	 * Called when game is closing down; hides the window, but doesn't record the fact that its hidden in the config file
	 */
	public final void closedown ()
	{
		if ((frame != null) && (frame.isVisible ()))
		{
			if (isPersistVisibility ())
				ignoreNextHide = true;
			
			frame.setVisible (false);
		}
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
	
	/**
	 * @return Enum value representing this window; null if its position isn't recorded in config file (for example scope=prototype windows)
	 */
	final WindowID getWindowID ()
	{
		return windowID;
	}

	/**
	 * @param w Enum value representing this window
	 */
	final void setWindowID (final WindowID w)
	{
		windowID = w;
	}
	
	/**
	 * @return Whether the window being shown and hidden is recorded in the config file; only relevant if getWindowID does not return null
	 */
	final boolean isPersistVisibility ()
	{
		return persistVisibility;
	}

	/**
	 * @param v Whether the window being shown and hidden is recorded in the config file; only relevant if getWindowID does not return null
	 */
	final void setPersistVisibility (final boolean v)
	{
		persistVisibility = v;
	}

	/**
	 * @return Client config, containing various overland map settings
	 */	
	public final MomImeClientConfig getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing various overland map settings
	 */
	public final void setClientConfig (final MomImeClientConfig config)
	{
		clientConfig = config;
	}

	/**
	 * @return Marshaller for saving client config
	 */
	public final Marshaller getClientConfigMarshaller ()
	{
		return clientConfigMarshaller;
	}

	/**
	 * @param marsh Marshaller for saving client config
	 */
	public final void setClientConfigMarshaller (final Marshaller marsh)
	{
		clientConfigMarshaller = marsh;
	}

	/**
	 * @return Location to save updated client config
	 */
	public final String getClientConfigLocation ()
	{
		return clientConfigLocation;
	}

	/**
	 * @param loc Location to save updated client config
	 */
	public final void setClientConfigLocation (final String loc)
	{
		clientConfigLocation = loc;
	}
}
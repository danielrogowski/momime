package momime.client.ui.dialogs;

import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JDialog;

import momime.client.language.LanguageVariableUIImpl;

/**
 * Ancestor used by all of the UI dialogs.
 */
public abstract class MomClientDialogUI extends LanguageVariableUIImpl
{
	/** The actual dialog */
	private JDialog dialog;
	
	/** Whether the dialog box should be modal - so that unit tests can set this to false */
	private boolean modal = true;
	
	/** Whether clicks close the dialog box */
	private boolean closeOnClick = false;

	/**
	 * This is package-private so it can be accessed for setLocationRelativeTo () methods
	 * @return The actual dialog
	 */
	final JDialog getDialog ()
	{
		return dialog;
	}

	/**
	 * @return Whether this screen is currently displayed or not
	 */
	public final boolean isVisible ()
	{
		// If haven't done init yet, then obviously it isn't visible
		return (dialog == null) ? false : dialog.isVisible ();
	}
	
	/**
	 * @param v Whether to display or hide this screen
	 * @throws IOException If a resource cannot be found
	 */
	public final void setVisible (final boolean v) throws IOException
	{
		if (dialog == null)
		{
			dialog = new JDialog ();
			init ();
			languageChanged ();
			getLanguageChangeMaster ().addLanguageChangeListener (this);
			
			// If the window isn't resizeable, then hide the window border
			// but this means it has no title bar to enable it to be moved, so also set up dragging the background for it
			if (!dialog.isResizable ())
			{
				dialog.setUndecorated (true);
				
				final MouseAdapter undecoratedDialogMover = new MouseAdapter ()
				{
					/** The location the mouse was at when it was initially clicked */
					private Point mouseStart;

					/** The location the dialog was at when the mouse was initially clicked */
					private Point dialogStart;
					
					/**
					 * Record the current position of the mouse and dialog, so we know where to drag from
					 */
					@Override
					public final void mousePressed (final MouseEvent ev)
					{
						mouseStart = ev.getLocationOnScreen ();
						dialogStart = dialog.getLocation ();
					}

					/**
					 * Update the position of the dialog based on how far the mouse has moved
					 */
					@Override
					public final void mouseDragged (final MouseEvent ev)
					{
						if (dialogStart != null)
						{
							final Point mouseNow = ev.getLocationOnScreen ();
							final Point dialogNow = new Point (dialogStart.x + mouseNow.x - mouseStart.x, dialogStart.y + mouseNow.y - mouseStart.y);
							dialog.setLocation (dialogNow);
						}
					}

					/**
					 * Maybe close the dialog with a mouse click
					 */
					@Override
					public final void mouseClicked (@SuppressWarnings ("unused") final MouseEvent ev)
					{
						if (isCloseOnClick ())
							dialog.dispose ();
					}
				};
				
				dialog.addMouseListener (undecoratedDialogMover);
				dialog.addMouseMotionListener (undecoratedDialogMover);
			}

			// Build and position the dialog
			getDialog ().setIconImage (getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/frameIcon.png"));
			getDialog ().pack ();
			getDialog ().setLocationRelativeTo (null);
			
			if (isModal ())
				getDialog ().setModalityType (ModalityType.APPLICATION_MODAL);
		}
		dialog.setVisible (v);
	}

	/**
	 * @return Whether the dialog box should be modal - so that unit tests can set this to false
	 */
	final boolean isModal ()
	{
		return modal;
	}
	
	/**
	 * @param value Whether the dialog box should be modal - so that unit tests can set this to false
	 */
	final void setModal (final boolean value)
	{
		modal = value;
	}

	/**
	 * @return Whether clicks close the dialog box
	 */
	final boolean isCloseOnClick ()
	{
		return closeOnClick;
	}

	/**
	 * @param close Whether clicks close the dialog box
	 */
	final void setCloseOnClick (final boolean close)
	{
		closeOnClick = close;
	}
}
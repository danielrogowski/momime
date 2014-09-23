package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import momime.client.newturnmessages.NewTurnMessageAnimated;
import momime.client.newturnmessages.NewTurnMessageClickable;
import momime.client.newturnmessages.NewTurnMessageRepaintOnCityDataChanged;
import momime.client.newturnmessages.NewTurnMessageUI;
import momime.client.ui.renderer.NewTurnMessageRenderer;
import momime.client.utils.AnimationController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Scroll that displays new turn messages like "City of blah has grown from population 4,900 to 5,050" or "City of blah has finished constructing a Granary" or
 * "You have finished casting Ironheart and must choose a friendly unit to target it on"
 */
public final class NewTurnMessagesUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewTurnMessagesUI.class);

	/** Animation controller */
	private AnimationController anim;
	
	/** Number of pixels that the roller at the top of the scroll overlaps the main piece of background */
	private final static int SCROLL_OVERLAP_TOP = 6;
	
	/** Number of pixels that the roller at the bottom of the scroll overlaps the main piece of background */
	private final static int SCROLL_OVERLAP_BOTTOM = 7;
	
	/** Number of pixels that the close button overlaps the roller at the bottom */
	private final static int CLOSE_BUTTON_OVERLAP = 6;
	
	/** Width of the drawable (list box) area of the NTM scroll */
	public final static int SCROLL_WIDTH = 452;
	
	/** Stores the list of messages to display on the scroll; initialize it out here, prior to the init method, so we can write to it before the UI gets displayed */
	private final DefaultListModel<NewTurnMessageUI> newTurnMessages = new DefaultListModel<NewTurnMessageUI> ();

	/** Messages list box */
	private JList<NewTurnMessageUI> newTurnMessagesList;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/background.png");
		final BufferedImage roller = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/position3-0.png");
		final BufferedImage closeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/closeButtonNormal.png");
		final BufferedImage closeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/scroll/closeButtonPressed.png");
		
		final Dimension backgroundSize = new Dimension (roller.getWidth (),
			background.getHeight () + (2 * roller.getHeight ()) - SCROLL_OVERLAP_TOP - SCROLL_OVERLAP_BOTTOM +
			closeButtonNormal.getHeight () - CLOSE_BUTTON_OVERLAP);
		
		final int backgroundTop = roller.getHeight () - SCROLL_OVERLAP_TOP;
		final int backgroundLeft = (roller.getWidth () - background.getWidth ()) / 2;
		final int bottomRollerTop = backgroundTop + background.getHeight () - SCROLL_OVERLAP_BOTTOM;
		final int bottomRollerBottom = bottomRollerTop + roller.getHeight ();

		// Actions
		final Action closeAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 333951123000045641L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().setVisible (false);
			}
		};
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = -6170657321368020350L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (background, backgroundLeft, backgroundTop, null);
				
				g.drawImage (roller, 0, 0, null);
				g.drawImage (roller, 0, bottomRollerTop, null);
			}
		};
		
		contentPane.setBackground (Color.BLACK);
		contentPane.setMinimumSize (backgroundSize);
		contentPane.setMaximumSize (backgroundSize);
		contentPane.setPreferredSize (backgroundSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());

		final Dimension listSize = new Dimension (SCROLL_WIDTH, 361);
		
		newTurnMessagesList = new JList<NewTurnMessageUI> ();
		newTurnMessagesList.setOpaque (false);
		newTurnMessagesList.setModel (newTurnMessages);
		newTurnMessagesList.setCellRenderer (new NewTurnMessageRenderer ());		// The renderer has no injections (yet) so doesn't have a spring prototype
		newTurnMessagesList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		newTurnMessagesList.setMinimumSize (listSize);
		newTurnMessagesList.setMaximumSize (listSize);
		newTurnMessagesList.setPreferredSize (listSize);
		
		contentPane.add (newTurnMessagesList, getUtils ().createConstraintsNoFill (0, 0, 1, 1,
			new Insets (backgroundTop + 2, 0, roller.getHeight () - SCROLL_OVERLAP_BOTTOM - CLOSE_BUTTON_OVERLAP + 2, 0), GridBagConstraintsNoFill.CENTRE));
		
		contentPane.add (getUtils ().createImageButton (closeAction, null, null, null, closeButtonNormal, closeButtonPressed, closeButtonNormal),
			getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, 300, 0, 0), GridBagConstraintsNoFill.CENTRE));
		
		// Pass clicks to the NTM objects
		newTurnMessagesList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				final NewTurnMessageUI msg = newTurnMessages.get (newTurnMessagesList.getSelectedIndex ());
				
				// Not all types of message are clickable
				if (msg instanceof NewTurnMessageClickable)
					try
					{
						((NewTurnMessageClickable) msg).clicked ();
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		});
		
		// If any messages were added prior to calling init (), then they may need anims started for them
		final Enumeration<NewTurnMessageUI> msgs = newTurnMessages.elements ();
		while (msgs.hasMoreElements ())
		{
			final NewTurnMessageUI msg = msgs.nextElement ();
			
			if (msg instanceof NewTurnMessageAnimated)
				((NewTurnMessageAnimated) msg).registerRepaintTriggers (newTurnMessagesList);
		}
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);

		// Trying to get the shape of this right is very long and complicated, and impossible to get perfect because the edges are all jagged,
		// so this is just as close as I can get it.  It starts from the top-right corner under the roller and proceeds clockwise.
		
		// Also note the rollers are lopsided, with 7 pixels above them but only 6 below
		getFrame ().setShape (new Polygon
			(new int [] {backgroundLeft + background.getWidth (), backgroundLeft + background.getWidth (),
					
				// Bottom-right roller
				backgroundLeft + background.getWidth () + 2, backgroundSize.width - 29, backgroundSize.width - 22, backgroundSize.width - 16, backgroundSize.width - 7, backgroundSize.width, backgroundSize.width, backgroundSize.width - 7, backgroundSize.width - 16, backgroundSize.width - 22, backgroundSize.width - 29, backgroundLeft + background.getWidth () + 2,
				
				// Bottom edge incl. close button
				backgroundLeft + background.getWidth (), 428, 431, 431, 428, 428, 432, 412, 410, 410, 413, 413, 410, backgroundLeft,
					
				// Bottom-left roller
				backgroundLeft - 2, 29, 22, 16, 7, 0, 0, 7, 16, 22, 29, backgroundLeft - 2,
					
				// Left edge
				backgroundLeft, backgroundLeft,
					
				// Top-left roller
				backgroundLeft - 2, 29, 22, 16, 7, 0, 0, 7, 16, 22, 29, backgroundLeft - 2,
				
				// Top edge
				backgroundLeft, backgroundLeft + background.getWidth (),
					
				// Top-right roller
				backgroundLeft + background.getWidth () + 2, backgroundSize.width - 29, backgroundSize.width - 22, backgroundSize.width - 16, backgroundSize.width - 7, backgroundSize.width, backgroundSize.width, backgroundSize.width - 7, backgroundSize.width - 16, backgroundSize.width - 22, backgroundSize.width - 29, backgroundLeft + background.getWidth () + 2},
					
			new int [] {backgroundTop, backgroundTop + background.getHeight (),
						
				// Bottom-right roller
				bottomRollerTop, bottomRollerTop, bottomRollerTop + 9, bottomRollerTop + 10, bottomRollerTop + 6, bottomRollerTop + 14, bottomRollerBottom - 14, bottomRollerBottom - 6, bottomRollerBottom - 10, bottomRollerBottom - 9, bottomRollerBottom, bottomRollerBottom,

				// Bottom edge incl. close button
				bottomRollerBottom - 6, bottomRollerBottom - 6, bottomRollerBottom - 3, bottomRollerBottom + 2, bottomRollerBottom + 5, backgroundSize.height - 7, backgroundSize.height, backgroundSize.height, backgroundSize.height - 7, bottomRollerBottom + 5, bottomRollerBottom + 2, bottomRollerBottom - 3, bottomRollerBottom - 6, bottomRollerBottom - 6,
				
				// Bottom-left roller
				bottomRollerBottom, bottomRollerBottom, bottomRollerBottom - 9, bottomRollerBottom - 10, bottomRollerBottom - 6, bottomRollerBottom - 14, bottomRollerTop + 14, bottomRollerTop + 6, bottomRollerTop + 10, bottomRollerTop + 9, bottomRollerTop, bottomRollerTop,
					
				// Left edge
				backgroundTop + background.getHeight (), backgroundTop,
					
				// Top-left roller
				roller.getHeight (), roller.getHeight (), roller.getHeight () - 9, roller.getHeight () - 10, roller.getHeight () - 6, roller.getHeight () - 14, 14, 6, 10, 9, 0, 0,

				// Top edge
				7, 7,
				
				// Top-right roller
				0, 0, 9, 10, 6, 14, roller.getHeight () - 14, roller.getHeight () - 6, roller.getHeight () - 10, roller.getHeight () - 9, roller.getHeight (), roller.getHeight ()},
					
			68));
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		// This can be called before the screen has ever been used
		if (newTurnMessagesList != null)
		{
			getFrame ().setTitle (getLanguage ().findCategoryEntry ("NewTurnMessages", "Title"));
			newTurnMessagesList.repaint ();
		}
		
		log.trace ("Exiting languageChanged");
	}	

	/**
	 * @param msgs The replacement list of messages to display on the scroll
	 * @throws IOException If there is a problem
	 */
	public final void setNewTurnMessages (final List<NewTurnMessageUI> msgs) throws IOException
	{
		log.trace ("Entering setNewTurnMessages: " + msgs.size ());

		// Clear out the old messages and repaint triggers
		if (newTurnMessagesList != null)
			getAnim ().unregisterRepaintTrigger (null, newTurnMessagesList);
		
		newTurnMessages.clear ();
		
		// Add in the new messages and repaint triggers
		// However note setNewTurnMessages gets called before the NTM scroll opens up for the first time, in which case the list may be null
		for (final NewTurnMessageUI msg : msgs)
		{
			newTurnMessages.addElement (msg);
			
			if ((newTurnMessagesList != null) && (msg instanceof NewTurnMessageAnimated))
				((NewTurnMessageAnimated) msg).registerRepaintTriggers (newTurnMessagesList);
		}
		
		log.trace ("Exiting setNewTurnMessages");
	}

	/**
	 * The data about a particular city has been updated; if any NTMs are displaying that info then we need to update them accordingly
	 * @param cityLocation Location of the city that was updated
	 * @throws IOException If there is a problem
	 */
	public final void cityDataChanged (final MapCoordinates3DEx cityLocation) throws IOException
	{
		log.trace ("Entering cityDataChanged: " + cityLocation);

		boolean repaint = false;
		
		final Enumeration<NewTurnMessageUI> msgs = newTurnMessages.elements ();
		while (msgs.hasMoreElements ())
		{
			final NewTurnMessageUI msg = msgs.nextElement ();
			if (msg instanceof NewTurnMessageRepaintOnCityDataChanged)
				if (((NewTurnMessageRepaintOnCityDataChanged) msg).equals (cityLocation))
					repaint = true;

			// This handles things like, if the current construction project is changed to a Sawmill then we need to kick off the Sawmill animation
			if (msg instanceof NewTurnMessageAnimated)
				((NewTurnMessageAnimated) msg).registerRepaintTriggers (newTurnMessagesList);
		}
		
		if (repaint)
			newTurnMessagesList.repaint ();

		log.trace ("Exiting cityDataChanged");
	}
	
	/**
	 * @return Animation controller
	 */
	public final AnimationController getAnim ()
	{
		return anim;
	}

	/**
	 * @param controller Animation controller
	 */
	public final void setAnim (final AnimationController controller)
	{
		anim = controller;
	}
}
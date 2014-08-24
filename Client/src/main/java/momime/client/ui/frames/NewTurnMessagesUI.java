package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;

import momime.client.newturnmessages.NewTurnMessageUI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Scroll that displays new turn messages like "City of blah has grown from population 4,900 to 5,050" or "City of blah has finished constructing a Granary" or
 * "You have finished casting Ironheart and must choose a friendly unit to target it on"
 */
public final class NewTurnMessagesUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewTurnMessagesUI.class);

	/** Number of pixels that the roller at the top of the scroll overlaps the main piece of background */
	private final static int SCROLL_OVERLAP_TOP = 6;
	
	/** Number of pixels that the roller at the bottom of the scroll overlaps the main piece of background */
	private final static int SCROLL_OVERLAP_BOTTOM = 7;
	
	/** Stores the list of messages to display on the scroll; initialize it out here, prior to the init method, so we can write to it before the UI gets displayed */
	private final DefaultListModel<NewTurnMessageUI> newTurnMessages = new DefaultListModel<NewTurnMessageUI> ();
	
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
		
		final Dimension backgroundSize = new Dimension (roller.getWidth (),
			background.getHeight () + (2 * roller.getHeight ()) - SCROLL_OVERLAP_TOP - SCROLL_OVERLAP_BOTTOM);
		
		final int backgroundTop = roller.getHeight () - SCROLL_OVERLAP_TOP;
		final int backgroundLeft = (roller.getWidth () - background.getWidth ()) / 2;
		
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
				g.drawImage (roller, 0, backgroundSize.height - roller.getHeight (), null);
			}
		};
		
		contentPane.setBackground (Color.BLACK);
		contentPane.setMinimumSize (backgroundSize);
		contentPane.setMaximumSize (backgroundSize);
		contentPane.setPreferredSize (backgroundSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
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
				
				// Bottom edge
				backgroundLeft + background.getWidth (), backgroundLeft,
					
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
				backgroundSize.height - roller.getHeight (), backgroundSize.height - roller.getHeight (), backgroundSize.height - roller.getHeight () + 9, backgroundSize.height - roller.getHeight () + 10, backgroundSize.height - roller.getHeight () + 6, backgroundSize.height - roller.getHeight () + 14, backgroundSize.height - 14, backgroundSize.height - 6, backgroundSize.height - 10, backgroundSize.height - 9, backgroundSize.height, backgroundSize.height,

				// Bottom edge
				backgroundSize.height - 6, backgroundSize.height - 6,
				
				// Bottom-left roller
				backgroundSize.height, backgroundSize.height, backgroundSize.height - 9, backgroundSize.height - 10, backgroundSize.height - 6, backgroundSize.height - 14, backgroundSize.height - roller.getHeight () + 14, backgroundSize.height - roller.getHeight () + 6, backgroundSize.height - roller.getHeight () + 10, backgroundSize.height - roller.getHeight () + 9, backgroundSize.height - roller.getHeight (), backgroundSize.height - roller.getHeight (),
					
				// Left edge
				backgroundTop + background.getHeight (), backgroundTop,
					
				// Top-left roller
				roller.getHeight (), roller.getHeight (), roller.getHeight () - 9, roller.getHeight () - 10, roller.getHeight () - 6, roller.getHeight () - 14, 14, 6, 10, 9, 0, 0,

				// Top edge
				7, 7,
				
				// Top-right roller
				0, 0, 9, 10, 6, 14, roller.getHeight () - 14, roller.getHeight () - 6, roller.getHeight () - 10, roller.getHeight () - 9, roller.getHeight (), roller.getHeight ()},
					
			56));
		
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

	/**
	 * @param msgs The replacement list of messages to display on the scroll
	 */
	public final void setNewTurnMessages (final List<NewTurnMessageUI> msgs)
	{
		log.trace ("Entering setNewTurnMessages: " + msgs.size ());
		
		newTurnMessages.clear ();
		for (final NewTurnMessageUI msg : msgs)
			newTurnMessages.addElement (msg);
		
		log.trace ("Exiting setNewTurnMessages");
	}
}
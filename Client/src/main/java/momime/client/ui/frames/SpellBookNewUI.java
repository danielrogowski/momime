package momime.client.ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.actions.LoggingAction;

import momime.client.config.WindowID;

/**
 * POC for new spell book where the pages are drawn so the book will look thinner/thicker on the left/right side depending how far through you are turned
 */
public final class SpellBookNewUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SpellBookNewUI.class);

	/** How much higher than the book itself the page turn anim is drawn */
	private final static int ANIM_VERTICAL_OFFSET = 9;

	/** How many pixels were added at the top of the old background.png compared to the new cover.png */
	private final static int OLD_BACKGROUND_PADDING_TOP = 7;
	
	/** How many pixels were added at the bottom of the old background.png compared to the new cover.png */
	private final static int OLD_BACKGROUND_PADDING_BOTTOM = 14;
	
	/** Y coordinate of first (lowest) pages */
	private final static int FIRST_PAGE_TOP = 40;
	
	/** X coordinate of first (lowest) left page */
	private final static int FIRST_LEFT_PAGE = 10;
	
	/** X coordinate of first (lowest) right page */
	private final static int FIRST_RIGHT_PAGE = 295;
	
	/** How much higher each page is than the previous */
	private final static int PAGE_SPACING_Y = 3;
	
	/** How much further across each page is than the previous */
	private final static int PAGE_SPACING_X = 2;
	
	/** There's always 1 page on the left and 1 page on the right.  Besides those, this many pages need to be stacked on top, on one side, the other, or split between the two */
	private final static int FLIPPABLE_PAGE_COUNT = 12;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Currently visible left page */
	private int leftPageNumber = 0;
	
	/** Currently visible right page (normally this is leftPageNumber+1, but they're updated during different frames of page turn animations) */
	private int rightPageNumber = 1;
	
	/** Turn page left action */
	private Action turnPageLeftAction;
	
	/** Turn page rightaction */
	private Action turnPageRightAction;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage cover = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/cover.png");
		final BufferedImage pageLeft = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-left-1.png");
		final BufferedImage pageRight = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-right-1.png");
		final BufferedImage pageLeftCorner = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-left-corner.png");
		final BufferedImage pageRightCorner = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-right-corner.png");
		
		if ((pageLeft.getWidth () != pageRight.getWidth ()) || (pageLeft.getHeight () != pageRight.getHeight ()))
			throw new IOException ("Left and right page images are different sizes");

		if ((pageLeft.getWidth () != pageLeftCorner.getWidth ()) || (pageLeft.getHeight () != pageLeftCorner.getHeight ()))
			throw new IOException ("Left page and with corner images are different sizes");
		
		if ((pageRight.getWidth () != pageRightCorner.getWidth ()) || (pageRight.getHeight () != pageRightCorner.getHeight ()))
			throw new IOException ("Right page and with corner images are different sizes");
		
		final Dimension fixedSize = new Dimension (cover.getWidth () * 2,
			(cover.getHeight () + ANIM_VERTICAL_OFFSET + OLD_BACKGROUND_PADDING_TOP + OLD_BACKGROUND_PADDING_BOTTOM) * 2);
		
		// Actions
		final Action closeAction = new LoggingAction ("X", (ev) -> getFrame ().setVisible (false));
		
		turnPageLeftAction = new LoggingAction ("<", (ev) ->
		{
			leftPageNumber = leftPageNumber - 2;
			rightPageNumber = rightPageNumber - 2;
			contentPane.repaint ();
		});
		
		turnPageRightAction = new LoggingAction (">", (ev) ->
		{
			leftPageNumber = leftPageNumber + 2;
			rightPageNumber = rightPageNumber + 2;
			contentPane.repaint ();
		});
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			/**
			 * Draw the background of the frame
			 */
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (cover, 0, (ANIM_VERTICAL_OFFSET + OLD_BACKGROUND_PADDING_TOP) * 2, cover.getWidth () * 2, cover.getHeight () * 2, null);
				
				// There's always 1 left page and 1 right page at the bottom
				g.drawImage (pageLeft, FIRST_LEFT_PAGE, FIRST_PAGE_TOP, null);
				g.drawImage (pageRight, FIRST_RIGHT_PAGE, FIRST_PAGE_TOP, null);
				
				// How many additional pages to draw on each side?
				final int additionalLeftPages = leftPageNumber / 2;
				final int additionalRightPages = FLIPPABLE_PAGE_COUNT - additionalLeftPages;
				
				for (int n = 1; n <= FLIPPABLE_PAGE_COUNT; n++)
				{
					if (n <= additionalLeftPages)
						g.drawImage ((n == additionalLeftPages) ? pageLeftCorner : pageLeft,
							FIRST_LEFT_PAGE + (n * PAGE_SPACING_X), FIRST_PAGE_TOP - (n * PAGE_SPACING_Y), null);
					
					if (n <= additionalRightPages)
						g.drawImage ((n == additionalRightPages) ? pageRightCorner : pageRight,
							FIRST_RIGHT_PAGE - (n * PAGE_SPACING_X), FIRST_PAGE_TOP - (n * PAGE_SPACING_Y), null);
				}
			}
		};

		contentPane.setBackground (Color.BLACK);
		contentPane.setMinimumSize (fixedSize);
		contentPane.setMaximumSize (fixedSize);
		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new BorderLayout ());
		
		// Temporary buttons
		final JPanel buttonsPanel = new JPanel (new GridBagLayout ());
		buttonsPanel.add (new JButton (turnPageLeftAction), getUtils ().createConstraintsNoFill (0, 0, 1, 1, 1, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (turnPageRightAction), getUtils ().createConstraintsNoFill (1, 0, 1, 1, 1, GridBagConstraintsNoFill.CENTRE));
		buttonsPanel.add (new JButton (closeAction), getUtils ().createConstraintsNoFill (2, 0, 1, 1, 1, GridBagConstraintsNoFill.CENTRE));
		
		contentPane.add (buttonsPanel, BorderLayout.SOUTH);
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		setWindowID (WindowID.SPELL_BOOK);
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
	}
}
package momime.client.ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
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
	
	/** Image of left page */
	private BufferedImage pageLeft;

	/** Image of right page */
	private BufferedImage pageRight;
	
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
		pageLeft = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-left-1.png");
		pageRight = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-right-1.png");
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
		
		// Generate animation frames
		generatePageTurnAnimationFrame (pageRight, 77, 47, 106, 243, false, null);
		generatePageTurnAnimationFrame (pageRight, 80, 60, 115, 220, false, null);
		generatePageTurnAnimationFrame (pageRight, 83, 73, 126, 180, false, null);
		generatePageTurnAnimationFrame (pageRight, 86, 86, 140, 0, false, null);
		generatePageTurnAnimationFrame (pageRight, 140, 150, 70, 0, false, null);

		generatePageTurnAnimationFrame (pageLeft, 77, 47, 106, 243, true, null);
		generatePageTurnAnimationFrame (pageLeft, 80, 60, 115, 220, true, null);
		generatePageTurnAnimationFrame (pageLeft, 83, 73, 126, 180, true, null);
		generatePageTurnAnimationFrame (pageLeft, 86, 86, 140, 0, true, null);
		generatePageTurnAnimationFrame (pageLeft, 140, 150, 70, 0, true, null);
		
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

				/* drawCurve (g, 77, 47, 106, 243);
				drawCurve (g, 80, 60, 115, 220);		// Original anim 1
				drawCurve (g, 83, 73, 126, 180);
				drawCurve (g, 86, 86, 140, 0);		// Original anim 2
				drawCurve (g, 140, 150, 70, 0); */
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
	 * Used to test params for curve generation
	 * 
	 * @param g Graphics context to draw onto
	 * @param radiusx Radius left-right of the initial curved part
	 * @param radiusy Radius top-bottom of the initial curved part
	 * @param angleend Angle where the curved part ends at
	 * @param endx X coordinate where the straight part ends
	private final void drawCurve (final Graphics g, final int radiusx, final int radiusy, final int angleend, final int endx)
	{		
		final int n = FLIPPABLE_PAGE_COUNT;
		final int basex = FIRST_RIGHT_PAGE - (n * PAGE_SPACING_X);
		final int basey = FIRST_PAGE_TOP + pageRight.getHeight () - (n * PAGE_SPACING_Y) - 5;		// -5 is fudge factor to make it line up to the unmodified page image
		
		// What point will that angle end at?
		final int curveendx = radiusx - ((int) (Math.cos (Math.toRadians (angleend)) * radiusx));
		final int curveendy = ((int) (Math.sin (Math.toRadians (angleend)) * radiusy));
		final double dxdy = Math.tan (Math.toRadians (270 - angleend));
		
		g.setColor (Color.WHITE);
		for (int x = 0; x < curveendx; x++)
		{
			final int cx = x - radiusx;
			
			// x² + y² = r², so y² = r² - x², so y = sqrt (r² - x²)
			double y = Math.sqrt ((radiusx * radiusx) - (cx * cx));
			
			// Compensate if it isn't an even circle
			if (radiusx != radiusy)
				y = y / radiusx * radiusy;
			
			g.fillRect (basex + x, basey - ((int) y), 1, 1);
		}
		
		for (int x = curveendx; x < endx; x++)
		{
			final int cx = x - curveendx;
			final double cy = cx * dxdy;
			
			g.fillRect (basex + x, basey - curveendy - ((int) cy), 1, 1);
		}
	} */

	/**
	 * Uses generated curve to warp one of the page images to generate the page turn animation frames
	 *  
	 * @param source Source image
	 * @param radiusx Radius left-right of the initial curved part
	 * @param radiusy Radius top-bottom of the initial curved part
	 * @param angleend Angle where the curved part ends at
	 * @param endx X coordinate where the straight part ends
	 * @param reverseX Whether to reverse X coords (need this for left page, since curve generation was tested and configured for the right pages)
	 * @param destFile File to save generated image out to for testing purposes, usually just pass null here
	 * @return Generated image
	 * @throws IOException If there is a problem
	 */
	private final BufferedImage generatePageTurnAnimationFrame
		(final BufferedImage source, final int radiusx, final int radiusy, final int angleend, final int endx, final boolean reverseX, final File destFile) throws IOException
	{
		// Anim frames are double height
		final BufferedImage dest = new BufferedImage (source.getWidth (), source.getHeight () * 2, source.getType ());
		
		// What point will that angle end at?
		final int curveendx = radiusx - ((int) (Math.cos (Math.toRadians (angleend)) * radiusx));
		final int curveendy = ((int) (Math.sin (Math.toRadians (angleend)) * radiusy));
		final double dxdy = Math.tan (Math.toRadians (270 - angleend));
		final int totalendx = Math.max (endx, curveendx);		// If there's only an angle portion, endx will be 0, but we still need to know where the actual end is
		
		for (int x = 0; x < curveendx; x++)
		{
			final int cx = x - radiusx;
			
			int sourcex = (x * (source.getWidth () - 1)) / (totalendx - 1);
			int destx = x;
			if (reverseX)
			{
				sourcex = source.getWidth () - 1 - sourcex;
				destx = dest.getWidth () - 1 - destx;
			}
			
			// x² + y² = r², so y² = r² - x², so y = sqrt (r² - x²)
			double y = Math.sqrt ((radiusx * radiusx) - (cx * cx));
			
			// Compensate if it isn't an even circle
			if (radiusx != radiusy)
				y = y / radiusx * radiusy;

			for (int copy = 0; copy < source.getHeight (); copy++)
				dest.setRGB (destx, dest.getHeight () - 1 - copy - ((int) y), source.getRGB (sourcex, source.getHeight () - 1 - copy));
		}
		
		for (int x = curveendx; x < endx; x++)
		{
			final int cx = x - curveendx;
			final double cy = cx * dxdy;

			int sourcex = (x * (source.getWidth () - 1)) / (totalendx - 1);
			int destx = x;
			if (reverseX)
			{
				sourcex = source.getWidth () - 1 - sourcex;
				destx = dest.getWidth () - 1 - destx;
			}
			
			for (int copy = 0; copy < source.getHeight (); copy++)
				dest.setRGB (destx, dest.getHeight () - 1 - copy - curveendy - ((int) cy), source.getRGB (sourcex, source.getHeight () - 1 - copy));
		}
		
		if (destFile != null)
			ImageIO.write (dest, "png", destFile);
		
		return dest;
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
	}
}
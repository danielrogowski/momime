package momime.client.ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

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

	/** How much space to leave above the cover background */
	private final static int BACKGROUND_PADDING_TOP = 83;
	
	/** How much space to leave below the cover background */
	private final static int BACKGROUND_PADDING_BOTTOM = 30;
	
	/** Y coordinate of first (lowest) pages */
	private final static int FIRST_PAGE_BOTTOM = (BACKGROUND_PADDING_TOP * 2) + 322;
	
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
	
	/** Old one with only 2 frames was ran at 5 FPS */
	private final static int PAGE_FLIP_ANIMATION_SPEED = 12;
	
	/** How many frames apart the animations for adjacent pages must be kept */
	private final static int ANIMATION_MINIMUM_SEPARATION = 3;

	/** Images of left pages at various stages of turning */
	private List<BufferedImage> pageLeftFrames = new ArrayList<BufferedImage> ();

	/** Images of right pages at various stages of turning*/
	private List<BufferedImage> pageRightFrames = new ArrayList<BufferedImage> ();
	
	/** Content pane */
	private JPanel contentPane;
	
	/** 0..5 = progress through 6 images in pageLeftFrames, 6..11 = progress through 6 images in pageRightFrames.  There are FLIPPABLE_PAGE_COUNT+2 items in this list. */
	private List<Integer> pageState = new ArrayList<Integer> ();
	
	/** Number of frames of animation that exist for each page, so valid page states are 0 .. pageStateCount - 1 */
	private int pageStateCount;
	
	/** Number of pages we want fully on the left, with all the others fully on the right.  Animation timer will attempt to update all the pages towards this state. */
	private int desiredPagesOnLeft;
	
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
		pageLeftFrames.add (getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-left-1.png"));
		pageRightFrames.add (getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-right-1.png"));
		final BufferedImage pageLeftCorner = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-left-corner.png");
		final BufferedImage pageRightCorner = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-right-corner.png");
		
		if ((pageLeftFrames.get (0).getWidth () != pageRightFrames.get (0).getWidth ()) || (pageLeftFrames.get (0).getHeight () != pageRightFrames.get (0).getHeight ()))
			throw new IOException ("Left and right page images are different sizes");

		if ((pageLeftFrames.get (0).getWidth () != pageLeftCorner.getWidth ()) || (pageLeftFrames.get (0).getHeight () != pageLeftCorner.getHeight ()))
			throw new IOException ("Left page and with corner images are different sizes");
		
		if ((pageRightFrames.get (0).getWidth () != pageRightCorner.getWidth ()) || (pageRightFrames.get (0).getHeight () != pageRightCorner.getHeight ()))
			throw new IOException ("Right page and with corner images are different sizes");
		
		final Dimension fixedSize = new Dimension (cover.getWidth () * 2,
			(cover.getHeight () + BACKGROUND_PADDING_TOP + BACKGROUND_PADDING_BOTTOM) * 2);
		
		// Generate animation frames
		pageRightFrames.add (generatePageTurnAnimationFrame (pageRightFrames.get (0), 77, 47, 106, 243, false, null));
		pageRightFrames.add (generatePageTurnAnimationFrame (pageRightFrames.get (0), 80, 60, 115, 220, false, null));
		pageRightFrames.add (generatePageTurnAnimationFrame (pageRightFrames.get (0), 83, 73, 126, 180, false, null));
		pageRightFrames.add (generatePageTurnAnimationFrame (pageRightFrames.get (0), 86, 86, 140, 0, false, null));
		pageRightFrames.add (generatePageTurnAnimationFrame (pageRightFrames.get (0), 140, 150, 70, 0, false, null));

		pageLeftFrames.add (generatePageTurnAnimationFrame (pageLeftFrames.get (0), 77, 47, 106, 243, true, null));
		pageLeftFrames.add (generatePageTurnAnimationFrame (pageLeftFrames.get (0), 80, 60, 115, 220, true, null));
		pageLeftFrames.add (generatePageTurnAnimationFrame (pageLeftFrames.get (0), 83, 73, 126, 180, true, null));
		pageLeftFrames.add (generatePageTurnAnimationFrame (pageLeftFrames.get (0), 86, 86, 140, 0, true, null));
		pageLeftFrames.add (generatePageTurnAnimationFrame (pageLeftFrames.get (0), 140, 150, 70, 0, true, null));
		
		// Populate initial page state
		// 1st page is fully left, all the rest are fully right
		pageStateCount = pageLeftFrames.size () + pageRightFrames.size ();
		pageState.add (0);
		for (int n = 0; n <= FLIPPABLE_PAGE_COUNT; n++)
			pageState.add (pageStateCount - 1);
		
		desiredPagesOnLeft = 1;
		
		// Actions
		final Action closeAction = new LoggingAction ("X", (ev) -> getFrame ().setVisible (false));
		
		turnPageLeftAction = new LoggingAction ("<", (ev) -> desiredPagesOnLeft++);
		turnPageRightAction = new LoggingAction (">", (ev) -> desiredPagesOnLeft--);
		
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
				g.drawImage (cover, 0, BACKGROUND_PADDING_TOP * 2, cover.getWidth () * 2, cover.getHeight () * 2, null);
				
				for (int leftIndex = 0; leftIndex < pageState.size (); leftIndex++)
				{
					// Draw left pages (those with pageState <= 5) in order, since page 0 is at the bottom of the left pile
					final int leftPageState = pageState.get (leftIndex);
					if (leftPageState < pageLeftFrames.size ())
					{
						final BufferedImage image = pageLeftFrames.get (leftPageState);
						g.drawImage (image, FIRST_LEFT_PAGE + (leftIndex * PAGE_SPACING_X), FIRST_PAGE_BOTTOM - image.getHeight () - (leftIndex * PAGE_SPACING_Y), null);
					}
				}
				
				for (int fromRight = 0; fromRight < pageState.size (); fromRight++)
				{
					// Draw right pages (those with pageState >= 6) in reverse order, since page (max) is at the bottom of the right pile
					final int rightIndex = pageState.size () - 1 - fromRight;
					final int rightPageState = pageState.get (rightIndex);
					if (rightPageState >= pageLeftFrames.size ())
					{
						final BufferedImage image = pageRightFrames.get (pageStateCount - 1 - rightPageState);
						g.drawImage (image, FIRST_RIGHT_PAGE - (fromRight * PAGE_SPACING_X), FIRST_PAGE_BOTTOM - image.getHeight () - (fromRight * PAGE_SPACING_Y), null);
					}
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
		
		// Update the animation each tick
		final Timer pageTurnTimer = new Timer (1000 / PAGE_FLIP_ANIMATION_SPEED, (ev) ->
		{
			boolean updated = false;
			
			// Go through page in turn
			// I don't think this is perfect - whether we need to process the pages left-first or right-first depends which way they are moving
			// so the animation frames will be 1 out in one direction compared to the other... but you probably can't notice this when its moving fast
			for (int leftIndex = 0; leftIndex < pageState.size (); leftIndex++)
			{
				final int leftPageState = pageState.get (leftIndex);
				final int leftPageDesiredState = (leftIndex < desiredPagesOnLeft) ? 0 : (pageStateCount - 1);
				if (leftPageState != leftPageDesiredState)
				{
					// Increase or decrease it?
					final int leftPageNewState = (leftPageDesiredState > leftPageState) ? (leftPageState + 1) : (leftPageState - 1);
					if (newStateWouldBeValid (leftIndex, leftPageNewState))
					{
						pageState.set (leftIndex, leftPageNewState);
						updated = true;
					}
				}
			}
			
			if (updated)
				contentPane.repaint ();
		});
		pageTurnTimer.start ();
	}
	
	/**
	 * @param index Index into pageState list that we want to update
	 * @param value Value we want to update it to
	 * @return Whether the new list would be valid, i.e. is the updated value too close to either adjacent value
	 */
	private final boolean newStateWouldBeValid (final int index, final int value)
	{
		boolean valid = true;
		
		if (index > 0)
		{
			final int prevValue = pageState.get (index - 1);
			if ((prevValue != 0) && (prevValue != pageStateCount - 1) &&
				(Math.abs (value - prevValue) < ANIMATION_MINIMUM_SEPARATION))
				
				valid = false;
		}

		if ((index + 1) < pageState.size ())
		{
			final int nextValue = pageState.get (index + 1);
			if ((nextValue != 0) && (nextValue != pageStateCount - 1) &&
				(Math.abs (value - nextValue) < ANIMATION_MINIMUM_SEPARATION))
				
				valid = false;
		}
		
		// Also the states have to be in ascending order - so if we block a page moving, the one behind it can't start moving
		int check = 0;
		while ((valid) && (check + 1 < pageState.size ()))
		{
			final int thisValue = (check == index) ? value : pageState.get (check);
			final int nextValue = (check + 1 == index) ? value : pageState.get (check + 1);
			
			if (thisValue > nextValue)
				valid = false;
			else			
				check++;
		}
		
		return valid;
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
			
			// x� + y� = r�, so y� = r� - x�, so y = sqrt (r� - x�)
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
			
			// x� + y� = r�, so y� = r� - x�, so y = sqrt (r� - x�)
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
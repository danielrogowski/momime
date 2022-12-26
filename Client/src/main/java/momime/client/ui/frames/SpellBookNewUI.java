package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.config.WindowID;
import momime.client.ui.MomUIConstants;
import momime.client.utils.SpellBookPage;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.SpellClientUtilsImpl;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.WizardState;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

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

	/** The flippable pages, plus the fixed first and last page which can never be turned */
	private final static int PHYSICAL_PAGE_COUNT = FLIPPABLE_PAGE_COUNT + 2;
	
	/** Old one with only 2 frames was ran at 5 FPS */
	private final static int PAGE_FLIP_ANIMATION_SPEED = 12;
	
	/** How many frames apart the animations for adjacent pages must be kept */
	private final static int ANIMATION_MINIMUM_SEPARATION = 3;
	
	/** Width of the fold down */
	private final static int PAGE_CORNER_WIDTH = 32;
	
	/** Height of the fold down */
	private final static int PAGE_CORNER_HEIGHT = 38;

	/** Colour to draw arcane spells */
	private final static Color ARCANE_SPELL_COLOUR = new Color (0xA0A0A0);

	/** Shadow colour to draw the spell that we're currently researching */
	private final static Color RESEARCHED_SPELL_COLOUR = new Color (0x6060FF);
	
	/** Images of left pages at various stages of turning */
	private List<BufferedImage> pageLeftFrames = new ArrayList<BufferedImage> ();

	/** Images of right pages at various stages of turning*/
	private List<BufferedImage> pageRightFrames = new ArrayList<BufferedImage> ();
	
	/** Large font */
	private Font largeFont;
	
	/** Medium font */
	private Font mediumFont;

	/** Small font */
	private Font smallFont;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** 0..5 = progress through 6 images in pageLeftFrames, 6..11 = progress through 6 images in pageRightFrames.  There are PHYSICAL_PAGE_COUNT items in this list, but the
	 * first entry must always be 0 and the last entry must always be 11 - those pages always lay fully flat and cannot be turned. */
	private List<Integer> pageState = new ArrayList<Integer> ();
	
	/** Number of frames of animation that exist for each page, so valid page states are 0 .. pageStateCount - 1 */
	private int pageStateCount;
	
	/** Number of pages we want fully on the left, with all the others fully on the right.  Animation timer will attempt to update all the pages towards this state. */
	private int desiredPagesOnLeft;

	/** We can see the front and back of every physical page except the first and last which can never be turned */
	private final static int LOGICAL_PAGE_COUNT = (FLIPPABLE_PAGE_COUNT + 1) * 2;
	
	/** Logical spell book pages (note these are numbered differently as in a real book, page 1 is on the left, page 2 is on the right (front+back are different pages) */
	private List<SpellBookPage> pages = new ArrayList<SpellBookPage> ();
	
	/** Section headings, numbered the same as the pages */
	private List<JLabel> sectionHeadings = new ArrayList<JLabel> ();
	
	/** Spell panels, numbered the same as the pages */
	private List<List<JPanel>> spellPanels = new ArrayList<List<JPanel>> ();
	
	/** Name of each spell */
	private List<List<JLabel>> spellNames = new ArrayList<List<JLabel>> ();

	/** Cost of casting spell overland */
	private List<List<JLabel>> spellOverlandCosts = new ArrayList<List<JLabel>> ();
	
	/** Cost of casting spell in combat */
	private List<List<JLabel>> spellCombatCosts = new ArrayList<List<JLabel>> ();

	/** Research cost of spell (this is separate just so it can be a double width box to fit the high research cost of Spell of Mastery) */
	private List<List<JLabel>> spellResearchCosts = new ArrayList<List<JLabel>> ();
	
	/** Long description of each spell */
	private List<List<JTextArea>> spellDescriptions = new ArrayList<List<JTextArea>> ();
	
	/** XML layout for the spell book as a whole */
	private XmlLayoutContainerEx spellBookLayout;
	
	/** XML layout for each individual spell panel */
	private XmlLayoutContainerEx spellLayout;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Overland or combat casting */
	private SpellCastType castType;
	
	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;

	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Text utils */
	private TextUtils textUtils;
	
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
		final BufferedImage pageLeftShading = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-left-shading.png");
		final BufferedImage pageRightShading = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/page-right-shading.png");
		
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
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			/**
			 * Draw the background of the frame, and pages that are sat fully flat
			 */
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (cover, 0, BACKGROUND_PADDING_TOP * 2, cover.getWidth () * 2, cover.getHeight () * 2, null);
				
				// Find how many pages are flat on each side, to know what page to draw with the corner folded down
				// Don't draw a corner on the very first or very last page
				Integer leftCorner = null;
				Integer rightCorner = null;
				for (int index = 1; (index + 1) < pageState.size (); index++)
				{
					final int value = pageState.get (index);
					if (value == 0)
						leftCorner = index;
					else if ((value == pageStateCount - 1) && (rightCorner == null))
						rightCorner = index;
				}
				
				for (int leftIndex = 0; leftIndex < pageState.size (); leftIndex++)
				{
					// Draw fully left pages (those with pageState = 0) in order, since page 0 is at the bottom of the left pile
					final int leftPageState = pageState.get (leftIndex);
					if (leftPageState == 0)
					{
						final BufferedImage image = ((leftCorner != null) && (leftCorner == leftIndex)) ? pageLeftCorner : pageLeftFrames.get (leftPageState);
						final int imageX = FIRST_LEFT_PAGE + (leftIndex * PAGE_SPACING_X);
						final int imageY = FIRST_PAGE_BOTTOM - image.getHeight () - (leftIndex * PAGE_SPACING_Y);
						g.drawImage (image, imageX, imageY, null);
						
						// Shading in between pages, so they blend into a brown blob near the spine
						if (leftIndex > 0)
							g.drawImage (pageLeftShading, imageX + 31, imageY + image.getHeight () - 15, null);
					}
				}
				
				for (int fromRight = 0; fromRight < pageState.size (); fromRight++)
				{
					// Draw right pages (those with pageState = max) in reverse order, since page (max) is at the bottom of the right pile
					final int rightIndex = pageState.size () - 1 - fromRight;
					final int rightPageState = pageState.get (rightIndex);
					if (rightPageState == pageStateCount - 1)
					{
						final BufferedImage image = ((rightCorner != null) && (rightCorner == rightIndex)) ? pageRightCorner : pageRightFrames.get (pageStateCount - 1 - rightPageState);
						final int imageX = FIRST_RIGHT_PAGE - (fromRight * PAGE_SPACING_X);
						final int imageY = FIRST_PAGE_BOTTOM - image.getHeight () - (fromRight * PAGE_SPACING_Y);
						g.drawImage (image, imageX, imageY, null);
						
						// Shading in between pages, so they blend into a brown blob near the spine
						if (fromRight > 0)
							g.drawImage (pageRightShading, imageX + 2, imageY + image.getHeight () - 15, null);
					}
				}

				/* drawCurve (g, 77, 47, 106, 243);
				drawCurve (g, 80, 60, 115, 220);		// Original anim 1
				drawCurve (g, 83, 73, 126, 180);
				drawCurve (g, 86, 86, 140, 0);		// Original anim 2
				drawCurve (g, 140, 150, 70, 0); */
			}
			
			/**
			 * Draw pages that are in mid movement here, so they appear in front of the text on the flat pages
			 */
			@Override
			protected final void paintChildren (final Graphics g)
			{
				super.paintChildren (g);
				
				for (int leftIndex = 0; leftIndex < pageState.size (); leftIndex++)
				{
					// Draw left pages (those with pageState 1..5) in order, since page 0 is at the bottom of the left pile
					final int leftPageState = pageState.get (leftIndex);
					if ((leftPageState > 0) && (leftPageState < pageLeftFrames.size ()))
					{
						final BufferedImage image = pageLeftFrames.get (leftPageState);
						final int imageX = FIRST_LEFT_PAGE + (leftIndex * PAGE_SPACING_X);
						final int imageY = FIRST_PAGE_BOTTOM - image.getHeight () - (leftIndex * PAGE_SPACING_Y);
						g.drawImage (image, imageX, imageY, null);
					}
				}
				
				for (int fromRight = 0; fromRight < pageState.size (); fromRight++)
				{
					// Draw right pages (those with pageState 6..max-1) in reverse order, since page (max) is at the bottom of the right pile
					final int rightIndex = pageState.size () - 1 - fromRight;
					final int rightPageState = pageState.get (rightIndex);
					if ((rightPageState >= pageLeftFrames.size ()) && (rightPageState < pageStateCount - 1))
					{
						final BufferedImage image = pageRightFrames.get (pageStateCount - 1 - rightPageState);
						final int imageX = FIRST_RIGHT_PAGE - (fromRight * PAGE_SPACING_X);
						final int imageY = FIRST_PAGE_BOTTOM - image.getHeight () - (fromRight * PAGE_SPACING_Y);
						g.drawImage (image, imageX, imageY, null);
					}
				}
			}
		};
		
		contentPane.setBackground (Color.BLACK);
		contentPane.setMinimumSize (fixedSize);
		contentPane.setMaximumSize (fixedSize);
		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getSpellBookLayout ()));
		
		// Logical pages
		for (int pageNumber = 1; pageNumber <= LOGICAL_PAGE_COUNT; pageNumber++)
		{
			final JLabel sectionHeading = getUtils ().createLabel (MomUIConstants.DARK_RED, getLargeFont ());
			sectionHeadings.add (sectionHeading);
			contentPane.add (sectionHeading, "frmSpellBookPage" + pageNumber + "SectionHeading");
			
			// Lists of things on this page
			final List<JPanel> spellPanelsOnThisPage = new ArrayList<JPanel> ();
			spellPanels.add (spellPanelsOnThisPage);
			
			final List<JLabel> spellNamesOnThisPage = new ArrayList<JLabel> ();
			spellNames.add (spellNamesOnThisPage);

			final List<JLabel> spellOverlandCostsOnThisPage = new ArrayList<JLabel> ();
			spellOverlandCosts.add (spellOverlandCostsOnThisPage);
			
			final List<JLabel> spellCombatCostsOnThisPage = new ArrayList<JLabel> ();
			spellCombatCosts.add (spellCombatCostsOnThisPage);

			final List<JLabel> spellResearchCostsOnThisPage = new ArrayList<JLabel> ();
			spellResearchCosts.add (spellResearchCostsOnThisPage);
			
			final List<JTextArea> spellDescriptionsOnThisPage = new ArrayList<JTextArea> ();
			spellDescriptions.add (spellDescriptionsOnThisPage);
			
			// Spells on this logical page
			for (int spellNumber = 1; spellNumber <= SpellClientUtilsImpl.SPELLS_PER_PAGE; spellNumber++)
			{
				final JPanel spellPanel = new JPanel (new XmlLayoutManager (getSpellLayout ()));
				spellPanel.setOpaque (false);
				spellPanelsOnThisPage.add (spellPanel);
				contentPane.add (spellPanel, "frmSpellBookPage" + pageNumber + "Spell" + spellNumber);

				final JLabel spellName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getMediumFont ());
				spellNamesOnThisPage.add (spellName);
				spellPanel.add ("frmSpellBookSpellName", spellName);

				final JLabel spellCombatCost = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getMediumFont ());
				spellCombatCostsOnThisPage.add (spellCombatCost);
				spellPanel.add ("frmSpellBookCombatCost", spellCombatCost);
				
				final JLabel spellOverlandCost = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getMediumFont ());
				spellOverlandCostsOnThisPage.add (spellOverlandCost);
				spellPanel.add ("frmSpellBookOverlandCost", spellOverlandCost);
				
				final JLabel spellResearchCost = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getMediumFont ());
				spellResearchCostsOnThisPage.add (spellResearchCost);
				spellPanel.add ("frmSpellBookResearchCost", spellResearchCost);
				
				final JTextArea spellDescription = getUtils ().createWrappingLabel (MomUIConstants.DARK_BROWN, getSmallFont ());
				spellDescriptionsOnThisPage.add (spellDescription);
				spellPanel.add ("frmSpellBookSpellDescription", spellDescription);
			}
		}
		
		// Handle mouse clicks
		final MouseAdapter spellBookMouseAdapter = new MouseAdapter ()
		{
			/**
			 * Figure out what was clicked on
			 */
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isLeftMouseButton (ev))
				{
					// We basically have to duplicate the logic from paintComponent, when all we're really trying to spot is find is the location of the
					// pages drawn with corners so we can tell if the corners were clicked on
					Integer leftCorner = null;
					Integer rightCorner = null;
					for (int index = 1; (index + 1) < pageState.size (); index++)
					{
						final int value = pageState.get (index);
						if (value == 0)
							leftCorner = index;
						else if ((value == pageStateCount - 1) && (rightCorner == null))
							rightCorner = index;
					}
					
					if (ev.getPoint ().x < cover.getWidth ())
					{
						// Possible click on left corner
						if ((leftCorner != null) && (desiredPagesOnLeft > 1))
						{
							final int imageLeft = FIRST_LEFT_PAGE + (leftCorner * PAGE_SPACING_X);
							final int imageTop = FIRST_PAGE_BOTTOM - pageLeftCorner.getHeight () - (leftCorner * PAGE_SPACING_Y);

							if ((ev.getPoint ().x >= imageLeft) && (ev.getPoint ().y >= imageTop) &&
								(ev.getPoint ().x < imageLeft + PAGE_CORNER_WIDTH) && (ev.getPoint ().y < imageTop + PAGE_CORNER_HEIGHT))
								
								desiredPagesOnLeft--;
						}
					}
					else
					{
						// Possible click on right corner
						if ((rightCorner != null) && (desiredPagesOnLeft < PHYSICAL_PAGE_COUNT - 1))
						{
							final int fromRight = pageState.size () - 1 - rightCorner;
							final int imageRight = FIRST_RIGHT_PAGE - (fromRight * PAGE_SPACING_X) + pageRightCorner.getWidth ();
							final int imageTop = FIRST_PAGE_BOTTOM - pageRightCorner.getHeight () - (fromRight * PAGE_SPACING_Y);
							
							if ((ev.getPoint ().x >= imageRight - PAGE_CORNER_WIDTH) && (ev.getPoint ().y >= imageTop) &&
								(ev.getPoint ().x < imageRight) && (ev.getPoint ().y < imageTop + PAGE_CORNER_HEIGHT))
									
								desiredPagesOnLeft++;
						}
					}
				}
			}
		};
		contentPane.addMouseListener (spellBookMouseAdapter);
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		setWindowID (WindowID.SPELL_BOOK);
		updateSpellBook ();
		
		// Update the animation each tick
		final Timer pageTurnTimer = new Timer (1000 / PAGE_FLIP_ANIMATION_SPEED, (ev) ->
		{
			boolean updated = false;
			boolean refreshPages = false;
			
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
						
						// If changing FROM or TO a fully flat page, this will alter which pages the text needs to appear on
						if ((leftPageState == 0) || (leftPageState == pageStateCount - 1) || (leftPageNewState == 0) || (leftPageNewState == pageStateCount - 1))
							refreshPages = true;
					}
				}
			}
			
			if (refreshPages)
				languageOrPageChanged ();
				
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
		getFrame ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getSpellBookScreen ().getTitle ()));
		languageOrPageChanged ();
	}
	
	/**
	 * When we learn a new spell, updates the spells in the spell book to include it.
	 * That may involve shuffling pages around if a page is now full, or adding new pages if the spell is a kind we didn't previously have.
	 * 
	 * Unlike the original MoM and earlier MoM IME versions, because the spell book can be left up permanently now, it will always
	 * draw all spells - so combat spells are shown when on the overland map, and overland spells are shown in combat, just greyed out.
	 * So here we don't need to pay any attention to the cast type.
	 * 
	 * @throws MomException If we encounter an unknown research unexpected status
	 * @throws RecordNotFoundException If we can't find a research status for a particular spell
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 */
	public final void updateSpellBook () throws MomException, RecordNotFoundException, PlayerNotFoundException
	{
		pages.clear ();
		pages.addAll (getSpellClientUtils ().generateSpellBookPages (getCastType ()));
		languageOrPageChanged ();
	}
	
	/**
	 * Called when either the language or the currently displayed page changes
	 */
	public final void languageOrPageChanged ()
	{
		// This can be called before we've ever opened the spell book, so none of the components exist
		if (contentPane != null)
			try
			{
				final String manaSuffix = getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "SpellBookUI").getProductionTypeSuffix ());
	
				final String researchSuffix = getLanguageHolder ().findDescription
					(getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "SpellBookUI").getProductionTypeSuffix ());
				
				final KnownWizardDetails ourWizard = getKnownWizardUtils ().findKnownWizardDetails
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getClient ().getOurPlayerID (), "languageOrPageChanged");
				
				final boolean unitCasting = (getCastType () == SpellCastType.COMBAT) && (getCombatUI ().getCastingSource () != null) &&
					(getCombatUI ().getCastingSource ().getCastingUnit () != null);
				
				// Only the topmost fully flat left page and the topmost fully flat right page have the text drawn on them
				Integer leftPage = null;
				Integer rightPage = null;
				for (int index = 0; index < pageState.size (); index++)
				{
					final int value = pageState.get (index);
					if (value == 0)
						leftPage = index;
					else if ((value == pageStateCount - 1) && (rightPage == null))
						rightPage = index;
				}
				
				// Above are the physical page numbers in the book - initially 0 on left, 1 on right and when we turn the page
				// we end up with the same page 1 on left (just now the reverse side of it) and page 2 on right.
				// But what we need below are the pages as actually numbered in a book, where the reverse of page 1 is page 2, and so on.
				leftPage = leftPage * 2;
				rightPage = (rightPage * 2) - 1;
				
				// Update section headings and spells
				for (int pageNumber = 0; pageNumber < sectionHeadings.size (); pageNumber++)
				{
					if ((pageNumber < pages.size ()) && ((pageNumber == leftPage) || (pageNumber == rightPage)))
					{
						final SpellBookPage page = pages.get (pageNumber);
						
						// Section heading
						try
						{
							sectionHeadings.get (pageNumber).setVisible (page.isFirstPageOfSection ());
							if (page.isFirstPageOfSection ())
								sectionHeadings.get (pageNumber).setText (getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpellBookSection
									(page.getSectionID (), "SpellBookUI").getSpellBookSectionName ()));
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
						
						// Spells
						for (int spellNumber = 0; spellNumber < SpellClientUtilsImpl.SPELLS_PER_PAGE; spellNumber++)
						{
							final JPanel spellPanel = spellPanels.get (pageNumber).get (spellNumber);
							if (spellNumber < page.getSpells ().size ())
							{
								spellPanel.setVisible (true);
								
								// How to render each spell is lifted straight out of the old spell book UI
								final Spell spell = page.getSpells ().get (spellNumber);
								
								// Draw the spell being researched with a different colour shadow
								final Color shadowColor = spell.getSpellID ().equals (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ()) ?
									RESEARCHED_SPELL_COLOUR : Color.BLACK;
								
								spellNames.get (pageNumber).get (spellNumber).setBackground (shadowColor);
								spellOverlandCosts.get (pageNumber).get (spellNumber).setBackground (shadowColor);
								
								// If we're banished, then grey out (light brown out) the entire spell book, as long as its the wizard casting spells
								if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN)) ||
									((ourWizard.getWizardState () != WizardState.ACTIVE) && (!unitCasting)))
								{
									spellNames.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
									spellCombatCosts.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
									spellOverlandCosts.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
									spellResearchCosts.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
									spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
								}
								else
								{
									// Let unknown spells be magic realm coloured too, as a hint
									spellNames.get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
									spellCombatCosts.get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
									spellOverlandCosts.get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
									spellResearchCosts.get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
									spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.DARK_BROWN);
									if (spell.getSpellRealm () != null)
										try
										{
											final Pick magicRealm = getClient ().getClientDB ().findPick (spell.getSpellRealm (), "languageOrPageChanged");
											if (magicRealm.getPickBookshelfTitleColour () != null)
											{
												final Color spellColour = new Color (Integer.parseInt (magicRealm.getPickBookshelfTitleColour (), 16));
												spellNames.get (pageNumber).get (spellNumber).setForeground (spellColour);
												spellCombatCosts.get (pageNumber).get (spellNumber).setForeground (spellColour);
												spellOverlandCosts.get (pageNumber).get (spellNumber).setForeground (spellColour);
												spellResearchCosts.get (pageNumber).get (spellNumber).setForeground (spellColour);
											}
										}
										catch (final Exception e)
										{
											log.error (e, e);
										}
								}
								
								// Set text for this spell
								spellCombatCosts.get (pageNumber).get (spellNumber).setText (null);
								spellOverlandCosts.get (pageNumber).get (spellNumber).setText (null);

								if (page.getSectionID () == SpellBookSectionID.RESEARCHABLE)
								{
									spellNames.get (pageNumber).get (spellNumber).setText ("??????????");
									spellDescriptions.get (pageNumber).get (spellNumber).setText ("?????????????????????????????????????????");
									spellResearchCosts.get (pageNumber).get (spellNumber).setText ("??? " + researchSuffix);
								}
								else
								{
									final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
									final String spellDescription = getLanguageHolder ().findDescription (spell.getSpellDescription ());
									
									spellNames.get (pageNumber).get (spellNumber).setText ((spellName == null) ? spell.getSpellID () : spellName);
									spellDescriptions.get (pageNumber).get (spellNumber).setText ((spellDescription == null) ? spell.getSpellID () : spellDescription);
	
									spellCombatCosts.get (pageNumber).get (spellNumber).setText (null);
									spellOverlandCosts.get (pageNumber).get (spellNumber).setText (null);
									spellResearchCosts.get (pageNumber).get (spellNumber).setText (null);
									
									// Show cost in MP for known spells, and RP for researchable spells
									try
									{
										if (page.getSectionID () == SpellBookSectionID.RESEARCHABLE_NOW)
										{
											if (spell.getResearchCost () != null)
											{
												final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (), spell.getSpellID ());
												spellResearchCosts.get (pageNumber).get (spellNumber).setText (getTextUtils ().intToStrCommas (researchStatus.getRemainingResearchCost ()) + " " + researchSuffix);
											}
										}
										else
										{
											// Show combat and overland casting cost separately
											Integer overlandCost;
											Integer combatCost;
											
											if (getCastType () == SpellCastType.SPELL_CHARGES)
											{
												overlandCost = null;
												combatCost = (spell.getCombatCastingCost () == null) ? null : spell.getCombatCastingCost () * 20;
											}
											else
											{
												overlandCost = (spell.getOverlandCastingCost () == null) ? null :
													getSpellUtils ().getReducedOverlandCastingCost (spell, null, null, ourWizard.getPick (),
														getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
														getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
			
												combatCost = getReducedCombatCastingCost (spell, ourWizard.getPick ());
											}
	
											if (overlandCost != null)
												spellOverlandCosts.get (pageNumber).get (spellNumber).setText (getTextUtils ().intToStrCommas (overlandCost) + " " + manaSuffix);
											
											if (combatCost != null)
												spellCombatCosts.get (pageNumber).get (spellNumber).setText (getTextUtils ().intToStrCommas (combatCost) + " " + manaSuffix);
											
											// Grey out (ok, light brown out...) casting cost that's inappropriate for our current cast type.
											// If we're in a combat, this also greys out spells that we don't have enough remaining skill/MP to cast in the combat.
											switch (getCastType ())
											{
												case COMBAT:
													spellOverlandCosts.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
													if ((!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)) || (combatCost > getCombatMaxCastable ()))
													{
														spellNames.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
													}
													break;
	
												case SPELL_CHARGES:
													spellOverlandCosts.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
													if (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT))
													{
														spellNames.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
													}
													break;													
													
												case OVERLAND:
													spellCombatCosts.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
													if (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND))
													{
														spellNames.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
													}
													break;
													
												default:
													throw new MomException ("SpellBookUI doesn't know how to prepare spell " + spell.getSpellID () +
														" in section " + page.getSectionID () + " when the cast type is " + getCastType ());
											}
										}
									}
									catch (final Exception e)
									{
										log.error (e, e);
									}
								}
							}
							else
								spellPanel.setVisible (false);
						}
					}
					else
					{
						// Not one of topmost pages
						sectionHeadings.get (pageNumber).setVisible (false);
						spellPanels.get (pageNumber).forEach (p -> p.setVisible (false));
					}
				}
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
	}

	/**
	 * This isn't straightforward anymore, so pulled it out into a separate method.  The casting cost reduction from having a lot of spell books in one magic
	 * realm, or retorts like Runemaster that might also reduce casting cost, only apply if it is the wizard casting the spell, and not a hero or a unit
	 * with the caster ability e.g. Archangel.
	 * 
	 * @param spell Spell to calculate the combat casting cost for
	 * @param picks Player's picks
	 * @return Combat casting cost for this spell, or null if it can't be cast in combat
	 * @throws MomException If MomSpellCastType.OVERLAND is unexpected by getCastingCostForCastingType (this should never happen)
	 * @throws RecordNotFoundException If there is a pick in the list that we can't find in the DB
	 */
	private final Integer getReducedCombatCastingCost (final Spell spell, final List<PlayerPick> picks) throws MomException, RecordNotFoundException
	{
		final Integer combatCost;
		
		if (spell.getCombatCastingCost () == null)
			combatCost = null;
		
		// If casting overland, then its the wizard casting, so show their cost reduction even though the combat MP costs are greyed out at the moment
		else if ((getCastType () != SpellCastType.COMBAT) || (getCombatUI ().getCastingSource () == null))
			combatCost = getSpellUtils ().getReducedCombatCastingCost (spell, null, picks,
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
		
		// If its the wizard casting in combat, then again show their cost reduction
		else if (getCombatUI ().getCastingSource ().getCastingUnit () == null)
			combatCost = getSpellUtils ().getReducedCombatCastingCost (spell, null, picks,
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
		
		// Its a unit or hero casting in combat, so show no reduction
		else
			combatCost = spell.getCombatCastingCost ();
		
		return combatCost;
	}
	
	/**
	 * Similar to above, this now varies between being derived from the wizard's MP pool, casting skill and how many spells they have cast so far this combat,
	 * or can be taken from the unit's MP pool.
	 * 
	 * @return Highest MP cost of combat spell that we can cast
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public final int getCombatMaxCastable () throws MomException
	{
		final int maxCastable;
		
		// Wizard casting limit is worked out by the combatUI, taking into account casting skill, spells cast already this combat, MP pool and range from fortress
		if ((getCombatUI ().getCastingSource () == null) || (getCombatUI ().getCastingSource ().getCastingUnit () == null))
			maxCastable = getCombatUI ().getMaxCastable ();
		
		// Unit or hero casting limit is simply the amount of MP they have left
		else if ((getCombatUI ().getCastingSource ().getHeroItemSlotNumber () == null) && (getCombatUI ().getCastingSource ().getFixedSpellNumber () == null))
			maxCastable = getCombatUI ().getCastingSource ().getCastingUnit ().getManaRemaining ();
		
		// Disable casting limits for casting spells from hero items or casting fixed spells - the spell is already imbued - we don't have to "pay" for it
		else
			maxCastable = Integer.MAX_VALUE;
		
		return maxCastable;
	}
	
	/**
	 * @return Large font
	 */
	public final Font getLargeFont ()
	{
		return largeFont;
	}

	/**
	 * @param font Large font
	 */
	public final void setLargeFont (final Font font)
	{
		largeFont = font;
	}
	
	/**
	 * @return Medium font
	 */
	public final Font getMediumFont ()
	{
		return mediumFont;
	}

	/**
	 * @param font Medium font
	 */
	public final void setMediumFont (final Font font)
	{
		mediumFont = font;
	}

	/**
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
	}
	
	/**
	 * @return XML layout for the spell book as a whole
	 */
	public final XmlLayoutContainerEx getSpellBookLayout ()
	{
		return spellBookLayout;
	}

	/**
	 * @param l XML layout for the spell book as a whole
	 */
	public final void setSpellBookLayout (final XmlLayoutContainerEx l)
	{
		spellBookLayout = l;
	}

	/**
	 * @return XML layout for each individual spell panel
	 */
	public final XmlLayoutContainerEx getSpellLayout ()
	{
		return spellLayout;
	}

	/**
	 * @param l XML layout for each individual spell panel
	 */
	public final void setSpellLayout (final XmlLayoutContainerEx l)
	{
		spellLayout = l;
	}
	
	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
	
	/**
	 * @return Overland or combat casting
	 */
	public final SpellCastType getCastType ()
	{
		return castType;
	}
	
	/**
	 * @param ct Overland or combat casting
	 */
	public final void setCastType (final SpellCastType ct)
	{
		castType = ct;
		languageOrPageChanged ();
	}

	/**
	 * @return Client-side spell utils
	 */
	public final SpellClientUtils getSpellClientUtils ()
	{
		return spellClientUtils;
	}

	/**
	 * @param utils Client-side spell utils
	 */
	public final void setSpellClientUtils (final SpellClientUtils utils)
	{
		spellClientUtils = utils;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}

	/**
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}
}
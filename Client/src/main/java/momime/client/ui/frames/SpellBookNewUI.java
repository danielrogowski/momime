package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.config.SpellBookViewMode;
import momime.client.config.WindowID;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.UnitRowDisplayUI;
import momime.client.ui.dialogs.VariableManaUI;
import momime.client.utils.MemoryMaintainedSpellClientUtils;
import momime.client.utils.SpellBookPage;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.database.AnimationEx;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.LanguageText;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SwitchResearch;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.WizardState;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.common.messages.clienttoserver.RequestResearchSpellMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;

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
	
	/** Animation for the blue swirl */
	private final static String ANIM_SWIRL = "SPELL_BOOK_SWIRL";
	
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
	private Map<SpellBookViewMode, List<List<JPanel>>> spellPanels = new HashMap<SpellBookViewMode, List<List<JPanel>>> ();
	
	/** Name of each spell */
	private Map<SpellBookViewMode, List<List<JLabel>>> spellNames = new HashMap<SpellBookViewMode, List<List<JLabel>>> ();

	/** Cost of casting spell overland */
	private Map<SpellBookViewMode, List<List<JLabel>>> spellOverlandCosts = new HashMap<SpellBookViewMode, List<List<JLabel>>> ();
	
	/** Cost of casting spell in combat */
	private Map<SpellBookViewMode, List<List<JLabel>>> spellCombatCosts = new HashMap<SpellBookViewMode, List<List<JLabel>>> ();

	/** Research cost of spell (this is separate just so it can be a double width box to fit the high research cost of Spell of Mastery) */
	private Map<SpellBookViewMode, List<List<JLabel>>> spellResearchCosts = new HashMap<SpellBookViewMode, List<List<JLabel>>> ();
	
	/** Long description of each spell */
	private List<List<JTextArea>> spellDescriptions = new ArrayList<List<JTextArea>> ();
	
	/** XML layout for the spell book as a whole */
	private XmlLayoutContainerEx spellBookLayout;
	
	/** XML layout for each individual spell panel */
	private XmlLayoutContainerEx spellLayout;

	/** XML layout for each individual spell panel when in compact mode */
	private XmlLayoutContainerEx compactLayout;
	
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
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Help text scroll */
	private HelpUI helpUI;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Methods for working with spells that are only needed on the client */
	private MemoryMaintainedSpellClientUtils memoryMaintainedSpellClientUtils;
	
	/** Variable MP popup */
	private VariableManaUI variableManaUI;
	
	/** Crafting popup */
	private CreateArtifactUI createArtifactUI;
	
	/** Blue swirl animation for when we click on a spell to cast */
	private AnimationEx castingAnim;
	
	/** This ticks up 0..13 and then goes back to null when we don't need to display the anim anymore */
	private Integer castingAnimFrame;
	
	/** Timer for ticking up castingAnimFrame */
	private Timer castingAnimTimer;
	
	/** Logical page number in spell book of the spell we're displaying the casting anim for */
	private int castingAnimPageNumber;

	/** Spell number on the page of the spell book we're displaying the casting anim for (0..3) */
	private int castingAnimSpellNumber;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Action for switching to standard view */
	private Action viewModeStandardAction;
	
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
		
		final BufferedImage viewModeStandardNormal = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/viewModeStandardNormal.png");
		final BufferedImage viewModeStandardPressed = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/viewModeStandardPressed.png");
		final BufferedImage viewModeStandardDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/viewModeStandardDisabled.png");
		final BufferedImage viewModeCompactNormal = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/viewModeCompactNormal.png");
		final BufferedImage viewModeCompactPressed = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/viewModeCompactPressed.png");
		
		if ((pageLeftFrames.get (0).getWidth () != pageRightFrames.get (0).getWidth ()) || (pageLeftFrames.get (0).getHeight () != pageRightFrames.get (0).getHeight ()))
			throw new IOException ("Left and right page images are different sizes");

		if ((pageLeftFrames.get (0).getWidth () != pageLeftCorner.getWidth ()) || (pageLeftFrames.get (0).getHeight () != pageLeftCorner.getHeight ()))
			throw new IOException ("Left page and with corner images are different sizes");
		
		if ((pageRightFrames.get (0).getWidth () != pageRightCorner.getWidth ()) || (pageRightFrames.get (0).getHeight () != pageRightCorner.getHeight ()))
			throw new IOException ("Right page and with corner images are different sizes");
		
		final Dimension fixedSize = new Dimension (cover.getWidth () * 2,
			(cover.getHeight () + BACKGROUND_PADDING_TOP + BACKGROUND_PADDING_BOTTOM) * 2);
		
		// Find animations we need
		castingAnim = getGraphicsDB ().findAnimation (ANIM_SWIRL, "SpellBookUI");
		
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
		viewModeStandardAction = new LoggingAction ((ev) ->
		{
			getClientConfig ().setSpellBookViewMode (SpellBookViewMode.STANDARD);
			updateSpellBook ();
			saveConfigFile ();
		});

		final Action viewModeCompactAction = new LoggingAction ((ev) ->
		{
			getClientConfig ().setSpellBookViewMode (SpellBookViewMode.COMPACT);
			updateSpellBook ();
			saveConfigFile ();
		});
		
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

		// Buttons
		contentPane.add ("frmSpellBookViewModeStandard", getUtils ().createImageButton
			(viewModeStandardAction, null, null, null, viewModeStandardNormal, viewModeStandardPressed, viewModeStandardDisabled));
		contentPane.add ("frmSpellBookViewModeCompact", getUtils ().createImageButton
			(viewModeCompactAction, null, null, null, viewModeCompactNormal, viewModeCompactPressed, viewModeCompactNormal));
		
		// Logical pages
		for (final SpellBookViewMode viewMode : SpellBookViewMode.values ())
		{
			// Lists of pages in this view mode
			final List<List<JPanel>> spellPanelsInThisMode = new ArrayList<List<JPanel>> ();
			spellPanels.put (viewMode, spellPanelsInThisMode);
			
			final List<List<JLabel>> spellNamesInThisMode = new ArrayList<List<JLabel>> ();
			spellNames.put (viewMode, spellNamesInThisMode);

			final List<List<JLabel>> spellOverlandCostsInThisMode = new ArrayList<List<JLabel>> ();
			spellOverlandCosts.put (viewMode, spellOverlandCostsInThisMode);
			
			final List<List<JLabel>> spellCombatCostsInThisMode = new ArrayList<List<JLabel>> ();
			spellCombatCosts.put (viewMode, spellCombatCostsInThisMode);
			
			final List<List<JLabel>> spellResearchCostsInThisMode = new ArrayList<List<JLabel>> ();
			spellResearchCosts.put (viewMode, spellResearchCostsInThisMode);
			
			for (int pageNumber = 1; pageNumber <= LOGICAL_PAGE_COUNT; pageNumber++)
			{
				final int clickedPageNumber = pageNumber - 1;
	
				if (viewMode == SpellBookViewMode.STANDARD)
				{
					final JLabel sectionHeading = getUtils ().createLabel (MomUIConstants.DARK_RED, getLargeFont ());
					sectionHeadings.add (sectionHeading);
					contentPane.add (sectionHeading, "frmSpellBookPage" + pageNumber + "SectionHeading");
				}
				
				// Lists of things on this page
				final List<JPanel> spellPanelsOnThisPage = new ArrayList<JPanel> ();
				spellPanelsInThisMode.add (spellPanelsOnThisPage);
				
				final List<JLabel> spellNamesOnThisPage = new ArrayList<JLabel> ();
				spellNamesInThisMode.add (spellNamesOnThisPage);
	
				final List<JLabel> spellOverlandCostsOnThisPage = new ArrayList<JLabel> ();
				spellOverlandCostsInThisMode.add (spellOverlandCostsOnThisPage);
				
				final List<JLabel> spellCombatCostsOnThisPage = new ArrayList<JLabel> ();
				spellCombatCostsInThisMode.add (spellCombatCostsOnThisPage);
	
				final List<JLabel> spellResearchCostsOnThisPage = new ArrayList<JLabel> ();
				spellResearchCostsInThisMode.add (spellResearchCostsOnThisPage);
				
				final List<JTextArea> spellDescriptionsOnThisPage = (viewMode == SpellBookViewMode.STANDARD) ? new ArrayList<JTextArea> () : null;
				if (spellDescriptionsOnThisPage != null)
					spellDescriptions.add (spellDescriptionsOnThisPage);
				
				// Spells on this logical page
				for (int spellNumber = 1; spellNumber <= getSpellClientUtils ().getSpellsPerPage (viewMode); spellNumber++)
				{
					final int clickedSpellNumber = spellNumber - 1;
					
					final JPanel spellPanel = new JPanel (new XmlLayoutManager
						((viewMode == SpellBookViewMode.STANDARD) ? getSpellLayout () : getCompactLayout ()))
					{
						/**
						 * The animation of the swirl has to be drawn in front of the controls, so have to do it here rather than in paintComponent
						 */
						@Override
						protected final void paintChildren (final Graphics g)
						{
							super.paintChildren (g);
							
							// Need to draw casting anim?
							if ((castingAnimFrame != null) && (castingAnimPageNumber == clickedPageNumber) && (castingAnimSpellNumber == clickedSpellNumber))
								try
								{
									final BufferedImage swirl = getUtils ().loadImage (castingAnim.getFrame ().get (castingAnimFrame).getImageFile ());
									g.drawImage (swirl, 0, 0, getWidth (), getHeight (), null);
								}
								catch (final Exception e)
								{
									log.error (e, e);
								}
						}
					};
					
					spellPanel.setOpaque (false);
					spellPanelsOnThisPage.add (spellPanel);
					contentPane.add (spellPanel, "frmSpellBookPage" + pageNumber + ((viewMode == SpellBookViewMode.STANDARD) ? "Spell" : "Compact") + spellNumber);
	
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
					
					JTextArea spellDescription = null;
					if (spellDescriptionsOnThisPage != null)
					{
						spellDescription = getUtils ().createWrappingLabel (MomUIConstants.DARK_BROWN, getSmallFont ());
						spellDescriptionsOnThisPage.add (spellDescription);
						spellPanel.add ("frmSpellBookSpellDescription", spellDescription);
					}
					
					// Handle clicking on spells
					final MouseListener spellClickListener = new MouseAdapter ()
					{
						@Override
						public final void mouseClicked (final MouseEvent ev)
						{
							// Find the spell that was clicked on
							if (clickedPageNumber < pages.size ())
							{
								final SpellBookPage page = pages.get (clickedPageNumber);
								if (clickedSpellNumber < page.getSpells ().size ())
								{
									final Spell spell = page.getSpells ().get (clickedSpellNumber);
									final SpellBookSectionID sectionID = page.getSectionID ();
									
									try
									{
										if (SwingUtilities.isRightMouseButton (ev))
										{
											// Right clicking on a spell to get help text for it
											// Don't allow right clicking on ????? spells to find out what they are!
											final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "clickSpell");
											if (sectionID != SpellBookSectionID.RESEARCHABLE)
												getHelpUI ().showSpellID (spell.getSpellID (), ourPlayer);
										}
										else if (sectionID == SpellBookSectionID.RESEARCHABLE_NOW)
										{
											final KnownWizardDetails ourWizard = getKnownWizardUtils ().findKnownWizardDetails
												(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getClient ().getOurPlayerID (), "clickSpell");
											
											if (ourWizard.getWizardState () == WizardState.ACTIVE)
											{
												// Clicking on a spell to research it
												// Whether we're allowed to depends on what spell settings are, and what's currently selected to research
												final boolean sendMessage;
												
												// Picking research when we've got no current research is always fine
												if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null)
													sendMessage = true;
												
												// Picking the same research that we're already researching is just ignored
												else if (spell.getSpellID ().equals (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ()))
													sendMessage = false;
												
												// If there's no penalty, then don't bother with a warning message
												else if (getClient ().getSessionDescription ().getSpellSetting ().getSwitchResearch () == SwitchResearch.FREE)
													sendMessage = true;
												
												else
												{
													// Now we need to know details about the spell that was previously being researched
													final Spell oldSpell = getClient ().getClientDB ().findSpell (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched (), "switchResearch");
													final SpellResearchStatus oldResearchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (),
														getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
													
													// If we've made no progress researching the old spell, then we can switch with no penalty
													if ((oldResearchStatus.getRemainingResearchCost () == oldSpell.getResearchCost ()) &&
														(getClient ().getSessionDescription ().getSpellSetting ().getSwitchResearch () != SwitchResearch.DISALLOWED))
														sendMessage = true;
													
													else
													{
														// We've either just not allowed to switch at all, or can switch but will lose research towards the old spell, so either way
														// we've got to display a message about it, and won't be sending any message now
														final String oldSpellName = getLanguageHolder ().findDescription (oldSpell.getSpellName ());
														final boolean lose = getClient ().getSessionDescription ().getSpellSetting ().getSwitchResearch () == SwitchResearch.LOSE_CURRENT_RESEARCH;
		
														final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
														msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getSwitchResearchTitle ());
														
														final List<LanguageText> languageText = lose ?
															getLanguages ().getSpellBookScreen ().getSwitchResearchLose () : getLanguages ().getSpellBookScreen ().getSwitchResearchDisallowed ();													
														
														msg.setText (getLanguageHolder ().findDescription (languageText).replaceAll
															("OLD_SPELL_NAME", oldSpellName).replaceAll
															("NEW_SPELL_NAME", getLanguageHolder ().findDescription (spell.getSpellName ())).replaceAll
															("RESEARCH_SO_FAR", getTextUtils ().intToStrCommas (oldSpell.getResearchCost () - oldResearchStatus.getRemainingResearchCost ())).replaceAll
															("RESEARCH_TOTAL", getTextUtils ().intToStrCommas (oldSpell.getResearchCost ())));
														
														if (lose)
															msg.setResearchSpellID (spell.getSpellID ());
														
														msg.setVisible (true);
														
														sendMessage = false;
													}
												}
												
												// Send message?
												if (sendMessage)
												{
													final RequestResearchSpellMessage msg = new RequestResearchSpellMessage ();
													msg.setSpellID (spell.getSpellID ());
													getClient ().getServerConnection ().sendMessageToServer (msg);
												}
											}
										}
										else if (sectionID != SpellBookSectionID.RESEARCHABLE)
											castSpell (spell, clickedPageNumber, clickedSpellNumber);
									}
									catch (final Exception e)
									{
										log.error (e, e);
									}
								}
							}
						}
					};
					
					spellPanel.addMouseListener (spellClickListener);
					if (spellDescription != null)
						spellDescription.addMouseListener (spellClickListener);
				}
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
		getFrame ().addMouseListener (spellBookMouseAdapter);
		
		// Default cast type to overland, if one wasn't already pre-set
		if (getCastType () == null)
			setCastType (SpellCastType.OVERLAND);
		
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
	 * Handles clicking on a spell to cast it.  This is in its own method because casting spells imbued into hero items
	 * comes here directly without showing the spell book UI at all.
	 * 
	 * @param spell Spell to calculate the combat casting cost for
	 * @param pageNumber Logical page number spell book of spell that was clicked on; null in cases where we don't want to display the animation
	 * @param spellNumber Which number spell on the page that was clicked on (0..3); null in cases where we don't want to display the animation
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there are any other problems
	 */
	public final void castSpell (final Spell spell, final Integer pageNumber, final Integer spellNumber) throws IOException, JAXBException, XMLStreamException
	{
		final SpellBookSectionID sectionID = spell.getSpellBookSectionID ();

		final KnownWizardDetails ourWizard = getKnownWizardUtils ().findKnownWizardDetails
			(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getClient ().getOurPlayerID (), "castSpell");

		final ExpandedUnitDetails castingUnit;
		final boolean castingFixedSpell;
		if ((getCastType () == SpellCastType.COMBAT) && (getCombatUI ().getCastingSource () != null))
		{
			castingUnit = getCombatUI ().getCastingSource ().getCastingUnit ();
			castingFixedSpell = (getCombatUI ().getCastingSource ().getFixedSpellNumber () != null);
		}
		else
		{
			castingUnit = null;
			castingFixedSpell = false;
		}
		
		// Look up name
		final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
		
		// Ignore trying to cast spells in combat when it isn't our turn
		final boolean proceed;
		final List<MemoryUnit> deadUnits = new ArrayList<MemoryUnit> ();
		if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN)) ||
			((ourWizard.getWizardState () != WizardState.ACTIVE) && (castingUnit == null)))
			
			proceed = false;
		else if ((getCastType () == SpellCastType.COMBAT) && (getCombatUI ().getCastingSource () == null))
			proceed = false;
		else
		{										
			// If spell is greyed due to incorrect cast type or not enough MP/skill in combat, then just ignore the click altogether
			final Integer combatCost = getReducedCombatCastingCost (spell, ourWizard.getPick ());
			
			if ((getCastType () == SpellCastType.OVERLAND) && (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND)))
				proceed = false;

			else if ((getCastType () == SpellCastType.SPELL_CHARGES) && (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)))
				proceed = false;
			
			// If we're casting a fixed spell on a unit, allow it even if it can't normally be cast that way from the spell book
			else if ((getCastType () == SpellCastType.COMBAT) && (!castingFixedSpell) &&
				((!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)) || (combatCost > getCombatMaxCastable ())))
				proceed = false;
			
			// Check if it is an overland enchantment that we already have
			else if (sectionID == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
			{
				proceed = (getMemoryMaintainedSpellUtils ().findMaintainedSpell
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPlayerID (), spell.getSpellID (), null, null, null, null) == null);
				if (!proceed)
				{
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
					msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellBookScreen ().getRequestCastExistingOverlandEnchantment ()).replaceAll
						("SPELL_NAME", spellName));

					msg.setCastSpellID (spell.getSpellID ());
					msg.setVisible (true);
				}
			}
			
			// Check for Spell Ward blocking combat spells
			else if ((getCastType () == SpellCastType.COMBAT) && (getMemoryMaintainedSpellUtils ().isBlockedCastingCombatSpellsOfRealm
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), getClient ().getOurPlayerID (),
					getCombatUI ().getCombatLocation (), spell.getSpellRealm (), getClient ().getClientDB ())))
			{
				proceed = false;
				final Pick pick = getClient ().getClientDB ().findPick (spell.getSpellRealm (), "castSpell");
				
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
				msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellBookScreen ().getBlockedCastingCombatSpellsOfRealm ()).replaceAll
					("SPELL_REALM", getLanguageHolder ().findDescription (pick.getBookshelfDescription ())));

				msg.setVisible (true);
			}
			
			// If its a combat spell then make sure there's at least something we can target it on
			// Only do this for spells without variable damage, because otherwise we might raise or lower the saving throw modifier
			// enough to make a difference as to whether there are any valid targets 
			else if ((getCastType () == SpellCastType.COMBAT) && ((sectionID == SpellBookSectionID.DISPEL_SPELLS) || ((spell.getCombatMaxDamage () == null) &&
				((sectionID == SpellBookSectionID.UNIT_ENCHANTMENTS) || (sectionID == SpellBookSectionID.UNIT_CURSES) ||
				(sectionID == SpellBookSectionID.ATTACK_SPELLS) || (sectionID == SpellBookSectionID.SPECIAL_UNIT_SPELLS)))))
			{
				boolean found = false;
				final Iterator<MemoryUnit> iter = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ().iterator ();
				while ((!found) && (iter.hasNext ()))
				{
					final MemoryUnit thisUnit = iter.next ();
					
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, spell.getSpellRealm (),
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					if (getSpellTargetingUtils ().isUnitValidTargetForSpell
						(spell, null, getCombatUI ().getCombatLocation (), getCombatUI ().getCombatTerrain (), getClient ().getOurPlayerID (), castingUnit, null, xu, true,
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (), getClient ().getPlayers (),
							getClient ().getClientDB ()) == TargetSpellResult.VALID_TARGET)
						
						found = true;
				}
				
				// Disenchant Area / True will also affect spells at the location that aren't cast on units, like Wall of Fire or Heavenly Light
				if ((!found) && (sectionID == SpellBookSectionID.DISPEL_SPELLS) && (spell.getAttackSpellCombatTarget () == AttackSpellTargetID.ALL_UNITS))
					found = (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().stream ().anyMatch
						(s -> (s.getCastingPlayerID () != getClient ().getOurPlayerID ()) && (getCombatUI ().getCombatLocation ().equals (s.getCityLocation ())))) ||
					
						// Or CAEs like Prayer
						(getMemoryCombatAreaEffectUtils ().listCombatAreaEffectsFromLocalisedSpells
							(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCombatUI ().getCombatLocation (), getClient ().getClientDB ()).stream ().anyMatch
								(cae -> !cae.getCastingPlayerID ().equals (getClient ().getOurPlayerID ())));
				
				// Or warped nodes
				if ((!found) && (sectionID == SpellBookSectionID.DISPEL_SPELLS) && (spell.getAttackSpellCombatTarget () == AttackSpellTargetID.ALL_UNITS))
				{
					final OverlandMapTerrainData combatTerrainData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(getCombatUI ().getCombatLocation ().getZ ()).getRow ().get (getCombatUI ().getCombatLocation ().getY ()).getCell ().get
						(getCombatUI ().getCombatLocation ().getX ()).getTerrainData ();
					
					found = (combatTerrainData.isWarped () != null) && (combatTerrainData.isWarped () &&
						(getClient ().getClientDB ().findTileType (combatTerrainData.getTileTypeID (), "castSpell").getMagicRealmID () != null));
				}
				
				// Cracks call can also be aimed at walls
				if ((!found) && (sectionID == SpellBookSectionID.ATTACK_SPELLS) && (spell.getSpellValidBorderTarget ().size () > 0))
					found = getMemoryMaintainedSpellClientUtils ().isAnyCombatLocationValidTargetForSpell (spell,
						getCombatUI ().getCombatTerrain (), getClient ().getSessionDescription ().getCombatMapSize ());
				
				// If no valid targets then tell player and don't allow casting it
				proceed = found;
				if (!proceed)
				{
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
					msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellBookScreen ().getNoValidTargets ()).replaceAll
						("SPELL_NAME", spellName));

					msg.setVisible (true);
				}
			}
			
			// Similar for spells targeted at a location
			else if ((getCastType () == SpellCastType.COMBAT) && (sectionID == SpellBookSectionID.SPECIAL_COMBAT_SPELLS))
			{
				proceed = getMemoryMaintainedSpellClientUtils ().isAnyCombatLocationValidTargetForSpell (spell,
					getCombatUI ().getCombatTerrain (), getClient ().getSessionDescription ().getCombatMapSize ());
				
				if (!proceed)
				{
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
					msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellBookScreen ().getNoValidTargets ()).replaceAll
						("SPELL_NAME", spellName));

					msg.setVisible (true);
				}
			}
			
			// Check combat enchantments have some available effects left
			else if ((getCastType () == SpellCastType.COMBAT) && (sectionID == SpellBookSectionID.COMBAT_ENCHANTMENTS))
			{
				final List<String> combatAreaEffectIDs = getMemoryCombatAreaEffectUtils ().listCombatEffectsNotYetCastAtLocation
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (),
					spell, getClient ().getOurPlayerID (), getCombatUI ().getCombatLocation ());
				
				proceed = ((combatAreaEffectIDs != null) && (combatAreaEffectIDs.size () > 0));
				if (!proceed)
				{
					final List<LanguageText> languageText = (combatAreaEffectIDs == null) ?
						getLanguages ().getSpellBookScreen ().getNoCombatSpellEffectIDsDefined () : getLanguages ().getSpellBookScreen ().getAlreadyHasAllPossibleCombatSpellEffects ();
					
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
					msg.setText (getLanguageHolder ().findDescription (languageText).replaceAll
						("SPELL_NAME", spellName));

					msg.setVisible (true);
				}
			}
			
			// For raise dead spells, check that at least one suitable unit has died
			else if ((getCastType () == SpellCastType.COMBAT) && (sectionID == SpellBookSectionID.SUMMONING) &&
				(spell.getResurrectedHealthPercentage () != null))
			{
				// This is basically the same loop as above, except now we need a list of the dead units, not simply to find one and exit the loop 
				for (final MemoryUnit thisUnit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, spell.getSpellRealm (),
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					if (getSpellTargetingUtils ().isUnitValidTargetForSpell
						(spell, null, getCombatUI ().getCombatLocation (), getCombatUI ().getCombatTerrain (), getClient ().getOurPlayerID (), castingUnit, null, xu, true,
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (), getClient ().getPlayers (),
							getClient ().getClientDB ()) == TargetSpellResult.VALID_TARGET)
						
						deadUnits.add (thisUnit);
				};						
						
				proceed = (deadUnits.size () > 0);
				if (!proceed)
				{
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
					msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellBookScreen ().getNoDeadUnitsToBeRaised ()).replaceAll
						("SPELL_NAME", spellName));

					msg.setVisible (true);
				}
			}			
			else
				proceed = true;
		}
		
		// Prevent casting more than one combat spell each turn
		if ((proceed) && (getCastType () == SpellCastType.COMBAT) && (getCombatUI ().getCastingSource () == null))
		{
			final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
			msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
			msg.setLanguageText (getLanguages ().getCombatScreen ().getOneSpellPerTurn ());
			msg.setVisible (true);
		}
		
		// Everything beyond here, we succeed in casting the spell (or requesting to), just various other pops may be necessary depending on which spell it is,
		// so show the swirly animation that shows that we cast something.
		if (proceed)
		{
			// Is it a spell with variable MP cost so we need to pop up a window with a slider to choose how much to put into it?
			if (((getCastType () == SpellCastType.COMBAT) && (spell.getCombatMaxDamage () != null) &&
				(getCombatUI ().getCastingSource ().getHeroItemSlotNumber () == null) &&		// Can't put additional power into spells imbued into items
				(getCombatUI ().getCastingSource ().getFixedSpellNumber () == null))	||			// or casting fixed spells like Magicians' Fireball spell
				((getCastType () == SpellCastType.OVERLAND) && (spell.getOverlandMaxDamage () != null)))				
			{
				getVariableManaUI ().setSpellBeingTargeted (spell);
				
				// If we've only got enough casting skill/MP to cast the spell at base cost, then don't even bother showing the variable damage form 
				if (getVariableManaUI ().anySelectableRange ())
					getVariableManaUI ().setVisible (true);
				else
					getVariableManaUI ().variableDamageChosen ();
			}
			
			// For raise dead spells, first select the unit to raise
			else if ((getCastType () == SpellCastType.COMBAT) && (sectionID == SpellBookSectionID.SUMMONING) &&
				(spell.getResurrectedHealthPercentage () != null))
			{
				final UnitRowDisplayUI unitRowDisplay = getPrototypeFrameCreator ().createUnitRowDisplay ();
				unitRowDisplay.setUnits (deadUnits);
				unitRowDisplay.setTargetSpellID (spell.getSpellID ());
				unitRowDisplay.setVisible (true);
			}
			
			// Is it a combat spell that we need to pick a target for?  If so then set up the combat UI to prompt for it
			else if ((getCastType () == SpellCastType.COMBAT) &&
				((sectionID == SpellBookSectionID.UNIT_ENCHANTMENTS) || (sectionID == SpellBookSectionID.UNIT_CURSES) ||
				(sectionID == SpellBookSectionID.SUMMONING) || (sectionID == SpellBookSectionID.SPECIAL_COMBAT_SPELLS) ||
				(((sectionID == SpellBookSectionID.ATTACK_SPELLS) || (sectionID == SpellBookSectionID.SPECIAL_UNIT_SPELLS)) &&
					(spell.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT))))
				
				getCombatUI ().setSpellBeingTargeted (spell);
			
			// Show item crafting window for Enchant Item / Create Artifact
			else if (spell.getHeroItemBonusMaximumCraftingCost () != null)
			{
				getCreateArtifactUI ().setSpell (spell);
				getCreateArtifactUI ().setVisible (true);
			}
			
			// Go back to the create artifact UI
			else if (getCastType () == SpellCastType.SPELL_CHARGES)
			{
				getCreateArtifactUI ().setSpellCharges (spell);
				
				// Go back to showing normal spell book
				setCastType (SpellCastType.OVERLAND);
			}
				
			// Tell server to cast it
			else
			{
				final RequestCastSpellMessage msg = new RequestCastSpellMessage ();
				msg.setSpellID (spell.getSpellID ());
				
				if (getCastType () == SpellCastType.COMBAT)
				{
					msg.setCombatURN (getCombatUI ().getCombatURN ());
					if ((getCombatUI ().getCastingSource () != null) && (getCombatUI ().getCastingSource ().getCastingUnit () != null))
					{
						msg.setCombatCastingUnitURN (getCombatUI ().getCastingSource ().getCastingUnit ().getUnitURN ());
						msg.setCombatCastingFixedSpellNumber (getCombatUI ().getCastingSource ().getFixedSpellNumber ());
						msg.setCombatCastingSlotNumber (getCombatUI ().getCastingSource ().getHeroItemSlotNumber ());
					}
				}
				
				getClient ().getServerConnection ().sendMessageToServer (msg);
			}
			
			// Show swirly animation on spell book
			if ((pageNumber != null) && (spellNumber != null))
			{
				castingAnimPageNumber = pageNumber;
				castingAnimSpellNumber = spellNumber;
				
				// Show casting animation
				if (castingAnimTimer != null)
					castingAnimTimer.stop ();				
				
				castingAnimFrame = 0;
				contentPane.repaint ();
				
				castingAnimTimer = new Timer ((int) (1000 / castingAnim.getAnimationSpeed ()), (ev2) ->
				{
					if ((castingAnimFrame == null) || (castingAnimFrame+1 >= castingAnim.getFrame ().size ()))
					{
						if (castingAnimTimer != null)
							castingAnimTimer.stop ();
						
						castingAnimTimer = null;
						castingAnimFrame = null;
					}
					else
						castingAnimFrame = castingAnimFrame + 1;
					
					contentPane.repaint ();
				});
				castingAnimTimer.start ();
			}
		}
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
		pages.addAll (getSpellClientUtils ().generateSpellBookPages (getClientConfig ().getSpellBookViewMode (), getCastType ()));
		
		// If we've got too many spells for standard mode, force into compact mode
		if ((pages.size () > LOGICAL_PAGE_COUNT) && (getClientConfig ().getSpellBookViewMode () == SpellBookViewMode.STANDARD))
		{
			getClientConfig ().setSpellBookViewMode (SpellBookViewMode.COMPACT);
			
			// Try again
			pages.clear ();
			pages.addAll (getSpellClientUtils ().generateSpellBookPages (getClientConfig ().getSpellBookViewMode (), getCastType ()));
			
			saveConfigFile ();
		}
		
		// If we're in compact mode, its not straightforward working out whether the spells would also fix in standard mode - just have to try it.
		// This isn't very optmized - it means generateSpellBookPages is doing 90% of the work twice.
		// Maybe it could return a map with pages organized in standard AND compact mode at the same time?
		viewModeStandardAction.setEnabled ((getClientConfig ().getSpellBookViewMode () == SpellBookViewMode.STANDARD) ||
			(getSpellClientUtils ().generateSpellBookPages (SpellBookViewMode.STANDARD, getCastType ()).size () <= LOGICAL_PAGE_COUNT));
		
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
				for (int pageNumber = 0; pageNumber < LOGICAL_PAGE_COUNT; pageNumber++)
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
						for (final SpellBookViewMode viewMode : SpellBookViewMode.values ())
							for (int spellNumber = 0; spellNumber < getSpellClientUtils ().getSpellsPerPage (viewMode); spellNumber++)
							{
								final JPanel spellPanel = spellPanels.get (viewMode).get (pageNumber).get (spellNumber);
								if ((viewMode == getClientConfig ().getSpellBookViewMode ()) && (spellNumber < page.getSpells ().size ()))
								{
									spellPanel.setVisible (true);
									
									// How to render each spell is lifted straight out of the old spell book UI
									final Spell spell = page.getSpells ().get (spellNumber);
									
									// Draw the spell being researched with a different colour shadow
									final Color shadowColor = spell.getSpellID ().equals (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ()) ?
										RESEARCHED_SPELL_COLOUR : Color.BLACK;
									
									spellNames.get (viewMode).get (pageNumber).get (spellNumber).setBackground (shadowColor);
									spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setBackground (shadowColor);
									
									// If we're banished, then grey out (light brown out) the entire spell book, as long as its the wizard casting spells
									if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN)) ||
										((ourWizard.getWizardState () != WizardState.ACTIVE) && (!unitCasting)))
									{
										spellNames.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
										spellCombatCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
										spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
										spellResearchCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
										
										if (viewMode == SpellBookViewMode.STANDARD)
											spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
									}
									else
									{
										// Let unknown spells be magic realm coloured too, as a hint
										spellNames.get (viewMode).get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
										spellCombatCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
										spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
										spellResearchCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (ARCANE_SPELL_COLOUR);
										
										if (viewMode == SpellBookViewMode.STANDARD)
											spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.DARK_BROWN);
										
										if (spell.getSpellRealm () != null)
											try
											{
												final Pick magicRealm = getClient ().getClientDB ().findPick (spell.getSpellRealm (), "languageOrPageChanged");
												if (magicRealm.getPickBookshelfTitleColour () != null)
												{
													final Color spellColour = new Color (Integer.parseInt (magicRealm.getPickBookshelfTitleColour (), 16));
													spellNames.get (viewMode).get (pageNumber).get (spellNumber).setForeground (spellColour);
													spellCombatCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (spellColour);
													spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (spellColour);
													spellResearchCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (spellColour);
												}
											}
											catch (final Exception e)
											{
												log.error (e, e);
											}
									}
									
									// Set text for this spell
									spellCombatCosts.get (viewMode).get (pageNumber).get (spellNumber).setText (null);
									spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setText (null);
	
									if (page.getSectionID () == SpellBookSectionID.RESEARCHABLE)
									{
										spellNames.get (viewMode).get (pageNumber).get (spellNumber).setText ("??????????");
										spellResearchCosts.get (viewMode).get (pageNumber).get (spellNumber).setText ("??? " + researchSuffix);
										
										if (viewMode == SpellBookViewMode.STANDARD)
											spellDescriptions.get (pageNumber).get (spellNumber).setText ("?????????????????????????????????????????");
									}
									else
									{
										final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
										final String spellDescription = getLanguageHolder ().findDescription (spell.getSpellDescription ());
										
										spellNames.get (viewMode).get (pageNumber).get (spellNumber).setText ((spellName == null) ? spell.getSpellID () : spellName);
										spellCombatCosts.get (viewMode).get (pageNumber).get (spellNumber).setText (null);
										spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setText (null);
										spellResearchCosts.get (viewMode).get (pageNumber).get (spellNumber).setText (null);

										if (viewMode == SpellBookViewMode.STANDARD)
											spellDescriptions.get (pageNumber).get (spellNumber).setText ((spellDescription == null) ? spell.getSpellID () : spellDescription);
										
										// Show cost in MP for known spells, and RP for researchable spells
										try
										{
											if (page.getSectionID () == SpellBookSectionID.RESEARCHABLE_NOW)
											{
												if (spell.getResearchCost () != null)
												{
													final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (), spell.getSpellID ());
													spellResearchCosts.get (viewMode).get (pageNumber).get (spellNumber).setText
														(getTextUtils ().intToStrCommas (researchStatus.getRemainingResearchCost ()) + " " + researchSuffix);
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
													spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setText (getTextUtils ().intToStrCommas (overlandCost) + " " + manaSuffix);
												
												if (combatCost != null)
													spellCombatCosts.get (viewMode).get (pageNumber).get (spellNumber).setText (getTextUtils ().intToStrCommas (combatCost) + " " + manaSuffix);
												
												// Grey out (ok, light brown out...) casting cost that's inappropriate for our current cast type.
												// If we're in a combat, this also greys out spells that we don't have enough remaining skill/MP to cast in the combat.
												switch (getCastType ())
												{
													case COMBAT:
														spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														if ((!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT)) || (combatCost > getCombatMaxCastable ()))
														{
															spellNames.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
															if (viewMode == SpellBookViewMode.STANDARD)
																spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														}
														break;
		
													case SPELL_CHARGES:
														spellOverlandCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														if (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.COMBAT))
														{
															spellNames.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
															if (viewMode == SpellBookViewMode.STANDARD)
																spellDescriptions.get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														}
														break;													
														
													case OVERLAND:
														spellCombatCosts.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
														if (!getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND))
														{
															spellNames.get (viewMode).get (pageNumber).get (spellNumber).setForeground (MomUIConstants.LIGHT_BROWN);
															if (viewMode == SpellBookViewMode.STANDARD)
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
						for (final SpellBookViewMode viewMode : SpellBookViewMode.values ())
							spellPanels.get (viewMode).get (pageNumber).forEach (p -> p.setVisible (false));
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
	 * @return XML layout for each individual spell panel when in compact mode
	 */
	public final XmlLayoutContainerEx getCompactLayout ()
	{
		return compactLayout;
	}

	/**
	 * @param l XML layout for each individual spell panel when in compact mode
	 */
	public final void setCompactLayout (final XmlLayoutContainerEx l)
	{
		compactLayout = l;
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

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Help text scroll
	 */
	public final HelpUI getHelpUI ()
	{
		return helpUI;
	}

	/**
	 * @param ui Help text scroll
	 */
	public final void setHelpUI (final HelpUI ui)
	{
		helpUI = ui;
	}

	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param su MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils su)
	{
		memoryMaintainedSpellUtils = su;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}

	/**
	 * @return Methods that determine whether something is a valid target for a spell
	 */
	public final SpellTargetingUtils getSpellTargetingUtils ()
	{
		return spellTargetingUtils;
	}

	/**
	 * @param s Methods that determine whether something is a valid target for a spell
	 */
	public final void setSpellTargetingUtils (final SpellTargetingUtils s)
	{
		spellTargetingUtils = s;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}

	/**
	 * @return Methods for working with spells that are only needed on the client
	 */
	public final MemoryMaintainedSpellClientUtils getMemoryMaintainedSpellClientUtils ()
	{
		return memoryMaintainedSpellClientUtils;
	}

	/**
	 * @param u Methods for working with spells that are only needed on the client
	 */
	public final void setMemoryMaintainedSpellClientUtils (final MemoryMaintainedSpellClientUtils u)
	{
		memoryMaintainedSpellClientUtils = u;
	}

	/**
	 * @return Variable MP popup
	 */
	public VariableManaUI getVariableManaUI ()
	{
		return variableManaUI;
	}

	/**
	 * @param ui Variable MP popup
	 */
	public final void setVariableManaUI (final VariableManaUI ui)
	{
		variableManaUI = ui;
	}
	
	/**
	 * @return Crafting popup
	 */
	public final CreateArtifactUI getCreateArtifactUI ()
	{
		return createArtifactUI;
	}

	/**
	 * @param ui Crafting popup
	 */
	public final void setCreateArtifactUI (final CreateArtifactUI ui)
	{
		createArtifactUI = ui;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}
}
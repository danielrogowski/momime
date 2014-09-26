package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

import momime.client.MomClient;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.Pick;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.SpellBookSection;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.HideableComponent;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.utils.SpellSorter;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_5.SwitchResearch;
import momime.common.database.v0_9_5.Spell;
import momime.common.messages.clienttoserver.v0_9_5.RequestCastSpellMessage;
import momime.common.messages.clienttoserver.v0_9_5.RequestResearchSpellMessage;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.SpellResearchStatus;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.MomSpellCastType;
import momime.common.utils.SpellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Spell book with fancy turning pages - the same book is used for casting spells overland, in combat, and research
 * but spells not appropriate for the current setting (overland/combat) will be greyed out so the spells and
 * pages don't keep jumping around
 */
public final class SpellBookUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SpellBookUI.class);
	
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;

	/** Small font */
	private Font smallFont;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Multiplayer client */
	private MomClient client;

	/** Spell utils */
	private SpellUtils spellUtils;

	/** Text utils */
	private TextUtils textUtils;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** How many spells we show on each page (this is sneakily set to half the number of spells we can choose from for research, so we get 2 research pages) */
	private final static int SPELLS_PER_PAGE = 4;
	
	/** Vertical gap between spells */
	private final static int GAP_BETWEEN_SPELLS = 2;
	
	/** The height of the portion of the book that we can use for drawing spells */
	private final static int SPELL_BOOK_HEIGHT = (145 * 2) - 26;

	/** The width of the portion of each page that we can use for drawing spells */
	private final static int PAGE_WIDTH = 124 * 2;
	
	/** How much higher than the book itself the page turn anim is drawn */
	private final static int ANIM_VERTICAL_OFFSET = 9 * 2;
	
	/** Animation for the page turning */
	final static String ANIM_PAGE_TURN = "SPELL_BOOK_PAGE_TURN";
	
	/** Typical inset used by layouts */
	private final static int INSET = 0;
	
	/** Colour to draw arcane spells */
	private final static Color ARCANE_SPELL_COLOUR = new Color (0xA0A0A0);

	/** Shadow colour to draw the spell that we're currently researching */
	private final static Color RESEARCHED_SPELL_COLOUR = new Color (0x6060FF);
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Page curl to turn the page left */
	private HideableComponent<JButton> turnPageLeftButton;

	/** Page curl to turn the page right */
	private HideableComponent<JButton> turnPageRightButton;

	/** This ticks up 0..1..2..3 and then goes back to null when we don't need to display the anim anymore */
	private Integer pageTurnFrame;
	
	/** Timer for ticking up pageTurnFrame */
	private Timer pageTurnTimer;
	
	/** Spell book pages */
	private List<SpellBookPage> pages = new ArrayList<SpellBookPage> ();
	
	/** Currently visible left page */
	private int leftPageNumber = 0;
	
	/** Currently visible right page (normally this is leftPageNumber+1, but they're updated during different frames of page turn animations) */
	private int rightPageNumber = 1;
	
	/** Section heading */
	private JLabel [] sectionHeadings = new JLabel [2];
	
	/** Spell names */
	private JLabel [] [] spellNames = new JLabel [2] [SPELLS_PER_PAGE];

	/** Spell descriptions */
	private JTextArea [] [] spellDescriptions = new JTextArea [2] [SPELLS_PER_PAGE];

	/** Spell overland costs */
	private JLabel [] [] spellOverlandCosts = new JLabel [2] [SPELLS_PER_PAGE];

	/** Spell combat costs */
	private JLabel [] [] spellCombatCosts = new JLabel [2] [SPELLS_PER_PAGE];
	
	/** Overland or combat casting (this will probably change to something like getClient ().getCurrentCombatLocation () == null or not null) */
	private MomSpellCastType castType = MomSpellCastType.OVERLAND;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/background.png");
		final BufferedImage closeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/closeButtonNormal.png");
		final BufferedImage closeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/closeButtonPressed.png");
		final BufferedImage turnPageLeftButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/turnPageLeft.png");
		final BufferedImage turnPageRightButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/turnPageRight.png");
		
		final Dimension fixedSize = new Dimension (background.getWidth () * 2, (background.getHeight () * 2) + ANIM_VERTICAL_OFFSET);

		// Set up animation of the page flipping left and right
		final AnimationEx pageTurnAnim = getGraphicsDB ().findAnimation (ANIM_PAGE_TURN, "SpellBookUI");
		
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

		final Action turnPageLeftAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 8767751980243329047L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				// Any more pages available to turn after this anim finishes?
				turnPageLeftButton.setHidden ((leftPageNumber - 2) <= 0);
				
				// Show page turn animation
				if (pageTurnTimer != null)
					pageTurnTimer.stop ();				
				
				pageTurnFrame = pageTurnAnim.getFrame ().size () - 1;
				contentPane.repaint ();
				
				pageTurnTimer = new Timer ((int) (1000 / pageTurnAnim.getAnimationSpeed ()), new ActionListener ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev2)
					{
						if ((pageTurnFrame == null) || (pageTurnFrame <= 0))
						{
							if (pageTurnTimer != null)
								pageTurnTimer.stop ();
							
							pageTurnTimer = null;
							pageTurnFrame = null;
							
							turnPageRightButton.setHidden (false);
						}
						else
						{
							pageTurnFrame = pageTurnFrame - 1;
							
							if (pageTurnFrame == 0)
							{
								// Update the right hand page while it is covered up
								rightPageNumber = rightPageNumber - 2;
								languageOrPageChanged ();
							}
							else if (pageTurnFrame == 2)
							{
								// Update the left hand page while it is covered up
								leftPageNumber = leftPageNumber - 2;
								languageOrPageChanged ();
							}
						}
						
						contentPane.repaint ();
					}
				});
				pageTurnTimer.start ();
			}
		};
		
		final Action turnPageRightAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 3223600649990240103L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				// Any more pages available to turn after this anim finishes?
				turnPageRightButton.setHidden ((rightPageNumber + 2) + 1 >= pages.size ());

				// Show page turn animation
				if (pageTurnTimer != null)
					pageTurnTimer.stop ();				
				
				pageTurnFrame = 0;
				contentPane.repaint ();
				
				pageTurnTimer = new Timer ((int) (1000 / pageTurnAnim.getAnimationSpeed ()), new ActionListener ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev2)
					{
						if ((pageTurnFrame == null) || (pageTurnFrame+1 >= pageTurnAnim.getFrame ().size ()))
						{
							if (pageTurnTimer != null)
								pageTurnTimer.stop ();
							
							pageTurnTimer = null;
							pageTurnFrame = null;
							
							turnPageLeftButton.setHidden (false);
						}
						else
						{
							pageTurnFrame = pageTurnFrame + 1;
							
							if (pageTurnFrame == 1)
							{
								// Update the right hand page while it is covered up
								rightPageNumber = rightPageNumber + 2;
								languageOrPageChanged ();
							}
							else if (pageTurnFrame == 3)
							{
								// Update the left hand page while it is covered up
								leftPageNumber = leftPageNumber + 2;
								languageOrPageChanged ();
							}
						}
						
						contentPane.repaint ();
					}
				});
				pageTurnTimer.start ();
			}
		};
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			private static final long serialVersionUID = 2219199398189071694L;

			/**
			 * Draw the background of the frame
			 */
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (background, 0, ANIM_VERTICAL_OFFSET, background.getWidth () * 2, background.getHeight () * 2, null);
			}
			
			/**
			 * The animation of the pages flipping has to be drawn in front of the controls, so it appears on top of the text/title of the spells in the flat pages
			 * so have to do it here rather than in paintComponent
			 */
			@Override
			protected final void paintChildren (final Graphics g)
			{
				super.paintChildren (g);
				
				// Need to draw anim of page turning?
				if (pageTurnFrame != null)
					try
					{
						final BufferedImage page = getUtils ().loadImage (pageTurnAnim.getFrame ().get (pageTurnFrame).getFrameImageFile ());
						g.drawImage (page, -17 * 2, 0, page.getWidth () * 2, page.getHeight () * 2, null);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		};

		contentPane.setBackground (Color.BLACK);
		contentPane.setMinimumSize (fixedSize);
		contentPane.setMaximumSize (fixedSize);
		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		final Dimension spellSize = new Dimension (PAGE_WIDTH, ((SPELL_BOOK_HEIGHT + GAP_BETWEEN_SPELLS) / SPELLS_PER_PAGE) - GAP_BETWEEN_SPELLS);
		
		turnPageLeftButton = new HideableComponent<JButton>
			(getUtils ().createImageButton (turnPageLeftAction, null, null, null, turnPageLeftButtonNormal, turnPageLeftButtonNormal, turnPageLeftButtonNormal));
		contentPane.add (turnPageLeftButton, getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (0, 26, 0, 0), GridBagConstraintsNoFill.SOUTHWEST));
		
		turnPageRightButton = new HideableComponent<JButton>
			(getUtils ().createImageButton (turnPageRightAction, null, null, null, turnPageRightButtonNormal, turnPageRightButtonNormal, turnPageRightButtonNormal));
		contentPane.add (turnPageRightButton, getUtils ().createConstraintsNoFill (3, 0, 1, 1, new Insets (0, 0, 0, 26), GridBagConstraintsNoFill.SOUTHEAST));

		for (int x = 0; x < 2; x++)
		{
			final GridBagConstraints headingConstraints = getUtils ().createConstraintsNoFill (x + 1, 0, 1, 1,
				new Insets (0, 12 * x, GAP_BETWEEN_SPELLS, 12 * (1-x)),
				(x == 0) ? GridBagConstraintsNoFill.SOUTH : GridBagConstraintsNoFill.SOUTH);
			headingConstraints.weighty = 1;		// Push all the controls down, so the close tag sits at the bottom of the window
			headingConstraints.weightx = 1;		// Don't let the page turns get more space than necessary
			
			final JLabel sectionHeading = getUtils ().createLabel (MomUIConstants.DARK_RED, getLargeFont ());
			contentPane.add (sectionHeading, headingConstraints);
			sectionHeadings [x] = sectionHeading;
			
			for (int y = 0; y < SPELLS_PER_PAGE; y++)
			{
				final JPanel spellPanel = new JPanel ();
				spellPanel.setLayout (new GridBagLayout ());
				
				spellPanel.setOpaque (false);
				spellPanel.setMinimumSize (spellSize);
				spellPanel.setMaximumSize (spellSize);
				spellPanel.setPreferredSize (spellSize);
				
				contentPane.add (spellPanel, getUtils ().createConstraintsNoFill (x * 2, y + 1, 2, 1, new Insets (0, 9, GAP_BETWEEN_SPELLS, 9),
					(x == 0) ? GridBagConstraintsNoFill.EAST : GridBagConstraintsNoFill.WEST));
				
				// Layout the individual spell panel
				final GridBagConstraints spellNameConstraints = getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.WEST);
				spellNameConstraints.weightx = 1;
				
				final JLabel spellName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getMediumFont ());
				spellPanel.add (spellName, spellNameConstraints);

				final JLabel spellCombatCost = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getMediumFont ());
				spellPanel.add (spellCombatCost, getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));				
				
				final JLabel spellOverlandCost = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getMediumFont ());
				spellPanel.add (spellOverlandCost, getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
				
				final GridBagConstraints descriptionConstraints = getUtils ().createConstraintsBothFill (0, 1, 3, 1, INSET);
				descriptionConstraints.weightx = 1;
				descriptionConstraints.weighty = 1;
				
				final JTextArea spellDescription = getUtils ().createWrappingLabel (MomUIConstants.DARK_BROWN, getSmallFont ());
				spellPanel.add (spellDescription, descriptionConstraints);
				
				spellNames [x] [y] = spellName;
				spellCombatCosts [x] [y] = spellCombatCost;
				spellOverlandCosts [x] [y] = spellOverlandCost;
				spellDescriptions [x] [y] = spellDescription;
				
				// Make the MP columns a consistent size
				spellPanel.add (Box.createRigidArea (new Dimension (50, 0)), getUtils ().createConstraintsNoFill (1, 2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
				spellPanel.add (Box.createRigidArea (new Dimension (50, 0)), getUtils ().createConstraintsNoFill (2, 2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
				
				// Handle clicking on spells
				final int spellX = x;
				final int spellY = y;
				final MouseListener spellClickListener = new MouseAdapter ()
				{
					@Override
					public final void mouseClicked (final MouseEvent ev)
					{
						// Find the spell that was clicked on
						final int pageNumber = (spellX == 0) ? leftPageNumber : rightPageNumber;
						if ((pageNumber >= 0) && (pageNumber < pages.size ()))
						{
							final SpellBookPage page = pages.get (pageNumber);
							if (spellY < page.getSpells ().size ())
							{
								final Spell spell = page.getSpells ().get (spellY);
								final String sectionID = page.getSectionID ();
								
								try
								{
									if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_RESEARCH_SPELLS))
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
												final momime.client.language.database.v0_9_5.Spell oldSpellLang = getLanguage ().findSpell (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
												final String oldSpellName = (oldSpellLang != null) ? oldSpellLang.getSpellName () : getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ();
												final boolean lose = getClient ().getSessionDescription ().getSpellSetting ().getSwitchResearch () == SwitchResearch.LOSE_CURRENT_RESEARCH;

												final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
												msg.setTitleLanguageCategoryID ("frmSpellBook");
												msg.setTitleLanguageEntryID ("SwitchResearchTitle");
												msg.setText (getLanguage ().findCategoryEntry ("frmSpellBook", lose ? "SwitchResearchLose" : "SwitchResearchDisallowed").replaceAll
													("OLD_SPELL_NAME", oldSpellName).replaceAll
													("NEW_SPELL_NAME", spellNames [spellX] [spellY].getText ()).replaceAll
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
									else if (!sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNKNOWN_SPELLS))
									{
										// Clicking on a spell to cast it
										final boolean proceed;
										
										// Check if it is an overland enchantment that we already have
										if (sectionID.equals (CommonDatabaseConstants.SPELL_BOOK_SECTION_OVERLAND_ENCHANTMENTS))
										{
											proceed = (getMemoryMaintainedSpellUtils ().findMaintainedSpell
												(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
												getClient ().getOurPlayerID (), spell.getSpellID (), null, null, null, null) == null);
											if (!proceed)
											{
												final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
												msg.setTitleLanguageCategoryID ("frmSpellBook");
												msg.setTitleLanguageEntryID ("CastSpellTitle");
												msg.setText (getLanguage ().findCategoryEntry ("frmSpellBook", "RequestCastExistingOverlandEnchantment").replaceAll
													("SPELL_NAME", spellNames [spellX] [spellY].getText ()));
	
												msg.setCastSpellID (spell.getSpellID ());
												msg.setVisible (true);
											}
										}
										else if (castType == MomSpellCastType.COMBAT)
										{
											throw new UnsupportedOperationException ("Casting combat spells hasn't been written yet");
										}
										else
											proceed = true;
										
										if (proceed)
										{
											// Tell server to cast it
											final RequestCastSpellMessage msg = new RequestCastSpellMessage ();
											msg.setSpellID (spell.getSpellID ());
											
											getClient ().getServerConnection ().sendMessageToServer (msg);
											
											// Close the spell book
											setVisible (false);
										}
									}
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
				spellDescription.addMouseListener (spellClickListener);
			}
		}
		
		contentPane.add (getUtils ().createImageButton (closeAction, null, null, null, closeButtonNormal, closeButtonPressed, closeButtonNormal),
			getUtils ().createConstraintsNoFill (2, SPELLS_PER_PAGE + 1, 1, 1, new Insets (38, 33, 0, 0), GridBagConstraintsNoFill.WEST));
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);

		// Set custom shape
		getFrame ().setShape (new Polygon
			(new int [] {1, 5,
					
				// Top
				19, 26, 250, 276, 282, fixedSize.width / 2, fixedSize.width - 282, fixedSize.width - 276, fixedSize.width - 250, fixedSize.width - 26, fixedSize.width - 19,
					
				// Right side
				fixedSize.width - 4, fixedSize.width - 1, fixedSize.width - 1, fixedSize.width - 3, fixedSize.width - 3, fixedSize.width - 1, fixedSize.width - 1, fixedSize.width - 4, fixedSize.width - 47, fixedSize.width - 47,
				
				// Pendant with close button
				355, 357, 357, 353, 353, 351, 335, 319, 317, 317, 321, 321, 319,
				
				// Book spine at the bottom
				fixedSize.width - 261, fixedSize.width - 268, 267, 260,
				
				// Left side
				47, 47, 5, 1, 1, 3, 3, 1},
				
			new int [] {37, 33,
					
				// Top
				33, 25, 19, 23, 27, 33, 27, 23, 19, 25, 33,
					
				// Right side
				33, 37, 83, 83, 313, 313, 355, 359, 359, 357,
					
				// Pendant with close button
				357, 364, 375, 384, fixedSize.height, fixedSize.height, fixedSize.height - 7, fixedSize.height, fixedSize.height, 384, 375, 364, 357,
				
				// Book spine at the bottom
				357, 361, 361, 357,
				
				// Left side
				357, 359, 359, 355, 313, 313, 83, 83},
				
			48));
		
		updateSpellBook ();
		turnPageLeftButton.setHidden (true);
		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmSpellBook", "Title"));
		languageOrPageChanged ();
		
		log.trace ("Exiting languageChanged");
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
	 */
	public final void updateSpellBook () throws MomException, RecordNotFoundException
	{
		log.trace ("Entering updateSpellBook");
		
		// Get a list of all spells we know, and all spells we can research now; grouped by section
		final Map<String, List<Spell>> sections = new HashMap<String, List<Spell>> ();
		for (final Spell spell : getClient ().getClientDB ().getSpell ())
		{
			final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (), spell.getSpellID ());
			final String sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus, true);
			if (sectionID != CommonDatabaseConstants.SPELL_BOOK_SECTION_NOT_IN_SPELL_BOOK)		// value of constant is null, so can't do .equals ()
			{
				// Do we have this section already?
				List<Spell> section = sections.get (sectionID);
				if (section == null)
				{
					section = new ArrayList<Spell> ();
					sections.put (sectionID, section);
				}
				
				section.add (spell);
			}
		}
		
		// Sort them into sections
		final List<String> sortedSections = new ArrayList<String> ();
		sortedSections.addAll (sections.keySet ());
		Collections.sort (sortedSections);
		
		// Go through each section
		pages.clear ();
		for (final String sectionID : sortedSections)
		{
			final List<Spell> spells = sections.get (sectionID);
			
			// Sort the spells within this section
			Collections.sort (spells, new SpellSorter (sectionID));
			
			// Divide them into pages with up to SPELLS_PER_PAGE on each page
			boolean first = true;
			while (spells.size () > 0)
			{
				final SpellBookPage page = new SpellBookPage ();
				page.setSectionID (sectionID);
				page.setFirstPageOfSection (first);
				pages.add (page);
				
				while ((spells.size () > 0) && (page.getSpells ().size () < SPELLS_PER_PAGE))
				{
					page.getSpells ().add (spells.get (0));
					spells.remove (0);
				}
				
				first = false;
			}
		}
		
		// This can be called before we've ever opened the spell book, so none of the components exist
		if (contentPane != null)
			languageOrPageChanged ();
		
		log.trace ("Exiting updateSpellBook");
	}
	
	/**
	 * Called when either the language or the currently displayed page changes
	 */
	public final void languageOrPageChanged ()
	{
		log.trace ("Entering languageOrPageChanged");

		final ProductionType manaProductionType = getLanguage ().findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		String manaSuffix = (manaProductionType == null) ? null : manaProductionType.getProductionTypeSuffix ();
		if (manaSuffix == null)
			manaSuffix = "";
		
		final ProductionType researchProductionType = getLanguage ().findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH);
		String researchSuffix = (researchProductionType == null) ? null : researchProductionType.getProductionTypeSuffix ();
		if (researchSuffix == null)
			researchSuffix = "";
		
		try
		{
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "languageOrPageChanged");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
			
			// Do the left+right pages in turn
			for (int x = 0; x < 2; x++)
			{
				final int pageNumber = (x == 0) ? leftPageNumber : rightPageNumber;
				if ((pageNumber >= 0) && (pageNumber < pages.size ()))
				{
					final SpellBookPage page = pages.get (pageNumber);
					
					// Write section heading?
					if (page.isFirstPageOfSection ())
					{
						final SpellBookSection spellBookSection = getLanguage ().findSpellBookSection (page.getSectionID ());
						final String sectionHeading = (spellBookSection == null) ? null : spellBookSection.getSpellBookSectionName ();
						sectionHeadings [x].setText ((sectionHeading == null) ? page.getSectionID () : sectionHeading);
					}
					else
						sectionHeadings [x].setText (null);
					
					// Spell names
					for (int y = 0; y < SPELLS_PER_PAGE; y++)
						if (y < page.getSpells ().size ())
						{
							final Spell spell = page.getSpells ().get (y);
							
							// Draw the spell being researched with a different colour shadow
							final Color shadowColor = spell.getSpellID ().equals (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ()) ?
								RESEARCHED_SPELL_COLOUR : Color.BLACK;
							
							spellNames [x] [y].setBackground (shadowColor);
							spellOverlandCosts [x] [y].setBackground (shadowColor);

							// Let unknown spells be magic realm coloured too, as a hint
							spellNames [x] [y].setForeground (ARCANE_SPELL_COLOUR);
							spellCombatCosts [x] [y].setForeground (ARCANE_SPELL_COLOUR);
							spellOverlandCosts [x] [y].setForeground (ARCANE_SPELL_COLOUR);
							spellDescriptions [x] [y].setForeground (MomUIConstants.DARK_BROWN);
							if (spell.getSpellRealm () != null)
								try
								{
									final Pick magicRealm = getGraphicsDB ().findPick (spell.getSpellRealm (), "languageOrPageChanged");
									if (magicRealm.getPickBookshelfTitleColour () != null)
									{
										final Color spellColour = new Color (Integer.parseInt (magicRealm.getPickBookshelfTitleColour (), 16));
										spellNames [x] [y].setForeground (spellColour);
										spellCombatCosts [x] [y].setForeground (spellColour);
										spellOverlandCosts [x] [y].setForeground (spellColour);
									}
								}
								catch (final Exception e)
								{
									log.error (e, e);
								}
							
							// Set text for this spell
							if (CommonDatabaseConstants.SPELL_BOOK_SECTION_UNKNOWN_SPELLS.equals (page.getSectionID ()))
							{
								spellNames [x] [y].setText ("??????????");
								spellDescriptions [x] [y].setText ("?????????????????????????????????????????");
								spellCombatCosts [x] [y].setText (null);
								spellOverlandCosts [x] [y].setText ("??? " + researchSuffix);
							}
							else
							{
								final momime.client.language.database.v0_9_5.Spell spellLang = getLanguage ().findSpell (spell.getSpellID ());
								final String spellName = (spellLang == null) ? null : spellLang.getSpellName ();
								final String spellDescription = (spellLang == null) ? null : spellLang.getSpellDescription ();
								
								spellNames [x] [y].setText ((spellName == null) ? spell.getSpellID () : spellName);
								spellDescriptions [x] [y].setText ((spellDescription == null) ? spell.getSpellID () : spellDescription);

								spellCombatCosts [x] [y].setText (null);
								spellOverlandCosts [x] [y].setText (null);
								
								// Show cost in MP for known spells, and RP for researchable spells
								try
								{
									if (CommonDatabaseConstants.SPELL_BOOK_SECTION_RESEARCH_SPELLS.equals (page.getSectionID ()))
									{
										if (spell.getResearchCost () != null)
										{
											final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (), spell.getSpellID ());
											spellOverlandCosts [x] [y].setText (getTextUtils ().intToStrCommas (researchStatus.getRemainingResearchCost ()) + " " + researchSuffix);
										}
									}
									else
									{
										// Show combat and overland casting cost separately
										final Integer overlandCost = (spell.getOverlandCastingCost () == null) ? null :
											getSpellUtils ().getReducedOverlandCastingCost (spell, pub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
	
										if (overlandCost != null)
											spellOverlandCosts [x] [y].setText (getTextUtils ().intToStrCommas (overlandCost) + " " + manaSuffix);
										
										final Integer combatCost = (spell.getCombatCastingCost () == null) ? null :
											getSpellUtils ().getReducedCombatCastingCost (spell, pub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
										
										if (combatCost != null)
											spellCombatCosts [x] [y].setText (getTextUtils ().intToStrCommas (combatCost) + " " + manaSuffix);
										
										// Grey out casting cost that's inappropriate for our current cast type
										// Once combat is done, this needs improving to also grey out spells that we don't have enough mana to cast in combat
										if (getCastType () == MomSpellCastType.COMBAT)
										{
											spellOverlandCosts [x] [y].setForeground (MomUIConstants.LIGHT_BROWN);
											if (spell.getCombatCastingCost () == null)
											{
												spellNames [x] [y].setForeground (MomUIConstants.LIGHT_BROWN);
												spellDescriptions [x] [y].setForeground (MomUIConstants.LIGHT_BROWN);
											}
										}
	
										if (getCastType () == MomSpellCastType.OVERLAND)
										{
											spellCombatCosts [x] [y].setForeground (MomUIConstants.LIGHT_BROWN);
											if (spell.getOverlandCastingCost () == null)
											{
												spellNames [x] [y].setForeground (MomUIConstants.LIGHT_BROWN);
												spellDescriptions [x] [y].setForeground (MomUIConstants.LIGHT_BROWN);
											}
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
						{
							// Blank spell
							spellNames [x] [y].setText (null);
							spellDescriptions [x] [y].setText (null);
							spellCombatCosts [x] [y].setText (null);
							spellOverlandCosts [x] [y].setText (null);
						}
				}
				else
				{
					// Blank page
					sectionHeadings [x].setText (null);
					for (int y = 0; y < SPELLS_PER_PAGE; y++)
					{
						spellNames [x] [y].setText (null);
						spellDescriptions [x] [y].setText (null);
						spellCombatCosts [x] [y].setText (null);
						spellOverlandCosts [x] [y].setText (null);
					}
				}
			}
		}
		catch (final PlayerNotFoundException e)
		{
			log.error (e, e);
		}
		
		log.trace ("Exiting languageOrPageChanged");
	}

	/**
	 * @return Overland or combat casting
	 */
	public final MomSpellCastType getCastType ()
	{
		return castType;
	}
	
	/**
	 * @param ct Overland or combat casting
	 */
	public final void setCastType (final MomSpellCastType ct)
	{
		if (castType != ct)
		{
			castType = ct;
			languageOrPageChanged ();
		}
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
	 * Represents one page of the spell book and the up-to-6 spells on it
	 */
	final class SpellBookPage
	{
		/** The spell book section */
		private String sectionID;
		
		/** Is this the first page of this section?  i.e. show the title or not */
		private boolean firstPageOfSection;
		
		/** The spells on this page */
		private List<Spell> spells = new ArrayList<Spell> ();

		/**
		 * @return The spell book section
		 */
		public final String getSectionID ()
		{
			return sectionID;
		}

		/**
		 * @param section The spell book section
		 */
		public final void setSectionID (final String section)
		{
			sectionID = section;
		}
		
		/**
		 * @return Is this the first page of this section?  i.e. show the title or not
		 */
		public final boolean isFirstPageOfSection ()
		{
			return firstPageOfSection;
		}

		/**
		 * @param first Is this the first page of this section?  i.e. show the title or not
		 */
		public final void setFirstPageOfSection (final boolean first)
		{
			firstPageOfSection = first;
		}
		
		/**
		 * @return The spells on this page
		 */
		public final List<Spell> getSpells ()
		{
			return spells;
		}
	}
}
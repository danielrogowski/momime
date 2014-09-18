package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.HideableComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	
	/** How many spells we show on each page */
	private final static int SPELLS_PER_PAGE = 6;
	
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
						}
						else
							pageTurnFrame = pageTurnFrame + 1;
						
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
						}
						else
							pageTurnFrame = pageTurnFrame - 1;
						
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
			
			final JLabel sectionHeading = getUtils ().createLabel (MomUIConstants.DARK_RED, getLargeFont (), "Spell book section Heading");
			contentPane.add (sectionHeading, headingConstraints);
			
			for (int y = 0; y < SPELLS_PER_PAGE; y++)
			{
				final JPanel spellPanel = new JPanel ();
				
				spellPanel.setOpaque (false);
				spellPanel.setMinimumSize (spellSize);
				spellPanel.setMaximumSize (spellSize);
				spellPanel.setPreferredSize (spellSize);
				
				contentPane.add (spellPanel, getUtils ().createConstraintsNoFill (x * 2, y + 1, 2, 1, new Insets (0, 9, GAP_BETWEEN_SPELLS, 9),
					(x == 0) ? GridBagConstraintsNoFill.EAST : GridBagConstraintsNoFill.WEST));
			}
		}
		
		contentPane.add (getUtils ().createImageButton (closeAction, null, null, null, closeButtonNormal, closeButtonPressed, closeButtonNormal),
			getUtils ().createConstraintsNoFill (2, SPELLS_PER_PAGE + 1, 1, 1, new Insets (38, 33, 0, 0), GridBagConstraintsNoFill.WEST));
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);

		// Set custom shape
		
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
}
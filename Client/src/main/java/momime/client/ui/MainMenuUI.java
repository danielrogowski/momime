package momime.client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Main menu with options to connect to a server and create or join games
 */
public final class MainMenuUI extends MomClientAbstractUI
{
	/** Maven version number, injected from spring */
	private String version;
	
	/** Large font */
	private Font largeFont;
	
	/** Medium font */
	private Font mediumFont;
	
	/** Frame number being displayed */
	private int titleFrame;
	
	/** Change language action */
	private Action changeLanguageAction;

	/** Connect to server action */
	private Action connectToServerAction;

	/** New game action */
	private Action newGameAction;

	/** Join game action */
	private Action joinGameAction;

	/** Options action */
	private Action optionsAction;

	/** Exit to windows action */
	private Action exitToWindowsAction;

	/** Language file author */
	private JLabel authorLabel;
	
	/** Gap above labels */
	private Filler labelsGap;
	
	/** Gap under buttons */
	private Filler buttonsGap;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = ImageIO.read (getClass ().getResource ("/momime.client.graphics/ui/mainMenu/background.png"));
		final List<BufferedImage> title = new ArrayList<BufferedImage> (20);
		for (int n = 1; n <= 20; n++)
			title.add (ImageIO.read (getClass ().getResource ("/momime.client.graphics/ui/mainMenu/title-frame" + ((n < 10) ? "0" : "") + n + ".png")));

		// Create actions
		changeLanguageAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		connectToServerAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		newGameAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		joinGameAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		optionsAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		exitToWindowsAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 5037769648975101972L;

			@Override
			public void actionPerformed (final ActionEvent e)
			{
				System.exit (0);
			}
		};
		
		// Initialize the frame
		getFrame ().setTitle ("Master of Magic - Implode's Multiplayer Edition - Client v" + getVersion ());
		getFrame ().setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);

		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			/** Unique value for serialization */
			private static final long serialVersionUID = 358769518041873860L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Scale the background image up smoothly
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
				// Stretch and centre background, preserving aspect ratio
				// Also note background image is split into two images, one static and one animated
				final int edge = Math.min (getWidth () / 4, getHeight () / 3);
				final int imgWidth = edge * 4;
				final int imgHeight = edge * 3;
				final int leftBorder = (getWidth () - imgWidth) / 2;
				final int topBorder = (getHeight () - imgHeight) / 2;
				
				final int titleHeight = (imgHeight * 41) / 200; 
				
				g.drawImage (title.get (titleFrame), leftBorder, topBorder, imgWidth, titleHeight, null); 
				g.drawImage (background, leftBorder, topBorder + titleHeight, imgWidth, imgHeight - titleHeight, null);
			}
		};
		contentPane.setBackground (Color.BLACK);
		contentPane.setPreferredSize (new Dimension (640, 480));
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		// Static text
		final Dimension labelsSpace = new Dimension (630, (480 * 41) / 200);
		labelsGap = new Box.Filler (labelsSpace, labelsSpace, labelsSpace);
		contentPane.add (labelsGap, createConstraints (0, GridBagConstraints.CENTER));
		
		final JLabel titleLabel = new JLabel ("Implode's Multiplayer Edition - Client");
		titleLabel.setForeground (new Color (0xD8DCEC));
		titleLabel.setFont (getLargeFont ());
		contentPane.add (titleLabel, createConstraints (1, GridBagConstraints.EAST));
		
		final JLabel versionLabel = new JLabel ("version " + getVersion ());
		versionLabel.setForeground (new Color (0xD8DCEC));
		versionLabel.setFont (getMediumFont ());
		contentPane.add (versionLabel, createConstraints (2, GridBagConstraints.EAST));
		
		final JLabel originalLabel = new JLabel ("Original Master of Magic is Copyright");
		originalLabel.setForeground (new Color (0xFCC864));
		originalLabel.setFont (getMediumFont ());
		contentPane.add (originalLabel, createConstraints (3, GridBagConstraints.EAST));
		
		final JLabel simtexLabel = new JLabel ("Simtex Software and Microprose");
		simtexLabel.setForeground (new Color (0xFCC864));
		simtexLabel.setFont (getMediumFont ());
		contentPane.add (simtexLabel, createConstraints (4, GridBagConstraints.EAST));
		
		authorLabel = new JLabel ();
		authorLabel.setForeground (new Color (0xD8DCEC));
		authorLabel.setFont (getMediumFont ());
		contentPane.add (authorLabel, createConstraints (5, GridBagConstraints.EAST));
		
		// Space in between
		final GridBagConstraints constraints = createConstraints (6, GridBagConstraints.CENTER);
		constraints.weighty = 1;
		contentPane.add (Box.createGlue (), constraints);
		
		// Main menu options
		final JButton changeLanguage = new JButton (changeLanguageAction);
		changeLanguage.setForeground (new Color (0xFCC864));
		changeLanguage.setFont (getLargeFont ());
		changeLanguage.setContentAreaFilled (false);
		changeLanguage.setMargin (new Insets (0, 0, 0, 0));
		changeLanguage.setBorder (null);
		contentPane.add (changeLanguage, createConstraints (7, GridBagConstraints.CENTER));

		final JButton connectToServer = new JButton (connectToServerAction);
		connectToServer.setForeground (new Color (0xFCC864));
		connectToServer.setFont (getLargeFont ());
		connectToServer.setContentAreaFilled (false);
		connectToServer.setMargin (new Insets (0, 0, 0, 0));
		connectToServer.setBorder (null);
		contentPane.add (connectToServer, createConstraints (8, GridBagConstraints.CENTER));

		final JButton newGame = new JButton (newGameAction);
		newGame.setForeground (new Color (0xFCC864));
		newGame.setFont (getLargeFont ());
		newGame.setContentAreaFilled (false);
		newGame.setMargin (new Insets (0, 0, 0, 0));
		newGame.setBorder (null);
		contentPane.add (newGame, createConstraints (9, GridBagConstraints.CENTER));

		final JButton joinGame = new JButton (joinGameAction);
		joinGame.setForeground (new Color (0xFCC864));
		joinGame.setFont (getLargeFont ());
		joinGame.setContentAreaFilled (false);
		joinGame.setMargin (new Insets (0, 0, 0, 0));
		joinGame.setBorder (null);
		contentPane.add (joinGame, createConstraints (10, GridBagConstraints.CENTER));

		final JButton options = new JButton (optionsAction);
		options.setForeground (new Color (0xFCC864));
		options.setFont (getLargeFont ());
		options.setContentAreaFilled (false);
		options.setMargin (new Insets (0, 0, 0, 0));
		options.setBorder (null);
		contentPane.add (options, createConstraints (11, GridBagConstraints.CENTER));

		final JButton exitToWindows = new JButton (exitToWindowsAction);
		exitToWindows.setForeground (new Color (0xFCC864));
		exitToWindows.setFont (getLargeFont ());
		exitToWindows.setContentAreaFilled (false);
		exitToWindows.setMargin (new Insets (0, 0, 0, 0));
		exitToWindows.setBorder (null);
		contentPane.add (exitToWindows, createConstraints (12, GridBagConstraints.CENTER));

		final Dimension buttonsSpace = new Dimension (630, 0);
		buttonsGap = new Box.Filler (buttonsSpace, buttonsSpace, buttonsSpace);
		contentPane.add (buttonsGap, createConstraints (13, GridBagConstraints.CENTER));
		
		// Animate the title
		new Timer (1000 / 8, new ActionListener ()
		{
			@Override
			public final void actionPerformed (final ActionEvent e)
			{
				final int newFrame = titleFrame + 1;
				titleFrame = (newFrame >= 20) ? 0 : newFrame;
				contentPane.repaint ();
			}
		}).start ();

		// Resize the areas above+below the image as the size of the window changes
		final ComponentListener onResize = new ComponentAdapter ()
		{
			@Override
			public final void componentResized (final ComponentEvent e)
			{
				final int edge = Math.min (contentPane.getWidth () / 4, contentPane.getHeight () / 3);
				final int imgWidth = edge * 4;
				final int imgHeight = edge * 3;
				final int topBorder = (contentPane.getHeight () - imgHeight) / 2;
				
				final int titleHeight = (imgHeight * 41) / 200; 

				// Adjust gap above labels
				final Dimension newLabelsSpace = new Dimension (imgWidth - 10, topBorder + titleHeight);
				labelsGap.setMinimumSize (newLabelsSpace);
				labelsGap.setPreferredSize (newLabelsSpace);
				labelsGap.setMaximumSize (newLabelsSpace);
				
				// Adjust gap below buttons
				final Dimension newButtonsSpace = new Dimension (imgWidth - 10, topBorder);
				buttonsGap.setMinimumSize (newButtonsSpace);
				buttonsGap.setPreferredSize (newButtonsSpace);
				buttonsGap.setMaximumSize (newButtonsSpace);
			}
		};
		
		contentPane.addComponentListener (onResize);
		getFrame ().addComponentListener (onResize);

		// Stop frame being shrunk smaller than this
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setMinimumSize (getFrame ().getSize ());
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		changeLanguageAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "ChangeLanguage"));
		connectToServerAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "ConnectToServer"));
		newGameAction.putValue			(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "NewGame"));
		joinGameAction.putValue				(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "JoinGame"));
		optionsAction.putValue				(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "Options"));
		exitToWindowsAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "Exit"));

		authorLabel.setText (getLanguage ().findCategoryEntry ("frmMainMenu", "LanguageFileAuthor"));
	}
	
	/**
	 * @param gridy Y cell we are putting a component into
	 * @param anchor Position of the component within the grid cell
	 * @return Constraints object
	 */
	private final GridBagConstraints createConstraints (final int gridy, final int anchor)
	{
		final GridBagConstraints c = new GridBagConstraints ();
		c.gridy = gridy;
		c.anchor = anchor;
		return c;
	}
	
	/**
	 * @return Maven version number, injected from spring
	 */
	public final String getVersion ()
	{
		return version;
	}

	/**
	 * @param ver Maven version number, injected from spring
	 */
	public final void setVersion (final String ver)
	{
		version = ver;
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
}

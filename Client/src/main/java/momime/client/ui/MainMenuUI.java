package momime.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import momime.client.MomClient;

import com.ndg.swing.GridBagConstraintsNoFill;

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
	
	/** Choose language UI */
	private ChooseLanguageUI chooseLanguageUI;
	
	/** Connect to server UI */
	private ConnectToServerUI connectToServerUI;

	/** New Game UI */
	private NewGameUI newGameUI;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Frame number being displayed */
	private int titleFrame;
	
	/** Change language action */
	private Action changeLanguageAction;

	/** Connect to server action */
	private Action connectToServerAction;

	/** New game action */
	private Action newGameAction;
	
	/** New game button */
	private JButton newGameButton;

	/** Join game action */
	private Action joinGameAction;
	
	/** Join game button */
	private JButton joinGameButton;

	/** Options action */
	private Action optionsAction;

	/** Exit to windows action */
	private Action exitToWindowsAction;

	/** Short title */
	private JLabel shortTitleLabel;
	
	/** Version */
	private JLabel versionLabel;

	/** Original copyright line 1 */
	private JLabel originalCopyrightLine1Label;
	
	/** Original copyright line 2 */
	private JLabel originalCopyrightLine2Label;
	
	/** Language file author */
	private JLabel authorLabel;
	
	/** Gap above labels */
	private Filler labelsGap;
	
	/** Gap under buttons */
	private Filler buttonsGap;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/mainMenu/background.png");
		final List<BufferedImage> title = new ArrayList<BufferedImage> (20);
		for (int n = 1; n <= 20; n++)
			title.add (getUtils ().loadImage ("/momime.client.graphics/ui/mainMenu/title-frame" + ((n < 10) ? "0" : "") + n + ".png"));

		// Create actions
		changeLanguageAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 7129552133649247249L;

			@Override
			public void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getChooseLanguageUI ().setVisible (true);
				}
				catch (final IOException e)
				{
					e.printStackTrace ();
				}
			}
		};
		
		connectToServerAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 438056656620043266L;

			@Override
			public void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getConnectToServerUI ().setVisible (true);
				}
				catch (final IOException e)
				{
					e.printStackTrace ();
				}
			}
		};
		
		newGameAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -1111824002334990953L;

			@Override
			public void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getNewGameUI ().setVisible (true);
					getNewGameUI ().showNewGamePanel ();
				}
				catch (final IOException e)
				{
					e.printStackTrace ();
				}
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
		getFrame ().setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);

		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
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
		contentPane.add (labelsGap, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		shortTitleLabel = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (shortTitleLabel, getUtils ().createConstraintsNoFill (0, 1, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		versionLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (versionLabel, getUtils ().createConstraintsNoFill (0, 2, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		originalCopyrightLine1Label = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (originalCopyrightLine1Label, getUtils ().createConstraintsNoFill (0, 3, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		originalCopyrightLine2Label = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (originalCopyrightLine2Label, getUtils ().createConstraintsNoFill (0, 4, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		authorLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (authorLabel, getUtils ().createConstraintsNoFill (0, 5, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		// Space in between
		final GridBagConstraints constraints = getUtils ().createConstraintsNoFill (0, 6, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE);
		constraints.weighty = 1;
		contentPane.add (Box.createGlue (), constraints);
		
		// Main menu options
		contentPane.add (getUtils ().createTextOnlyButton (changeLanguageAction,	MomUIConstants.GOLD, getLargeFont ()), getUtils ().createConstraintsNoFill (0, 7, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		contentPane.add (getUtils ().createTextOnlyButton (connectToServerAction,	MomUIConstants.GOLD, getLargeFont ()), getUtils ().createConstraintsNoFill (0, 8, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		newGameButton = getUtils ().createTextOnlyButton (newGameAction, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (newGameButton, getUtils ().createConstraintsNoFill (0, 9, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		joinGameButton = getUtils ().createTextOnlyButton (joinGameAction, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (joinGameButton, getUtils ().createConstraintsNoFill (0, 10, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		contentPane.add (getUtils ().createTextOnlyButton (optionsAction,				MomUIConstants.GOLD, getLargeFont ()), getUtils ().createConstraintsNoFill (0, 11, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		contentPane.add (getUtils ().createTextOnlyButton (exitToWindowsAction,	MomUIConstants.GOLD, getLargeFont ()), getUtils ().createConstraintsNoFill (0, 12, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final Dimension buttonsSpace = new Dimension (630, 0);
		buttonsGap = new Box.Filler (buttonsSpace, buttonsSpace, buttonsSpace);
		contentPane.add (buttonsGap, getUtils ().createConstraintsNoFill (0, 13, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
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
		getFrame ().setLocationRelativeTo (null);
		enableActions ();
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getFrame ().setTitle						(getLanguage ().findCategoryEntry ("frmMainMenu", "LongTitle").replaceAll ("VERSION", getVersion ()));
		shortTitleLabel.setText					(getLanguage ().findCategoryEntry ("frmMainMenu", "ShortTitle"));
		versionLabel.setText						(getLanguage ().findCategoryEntry ("frmMainMenu", "Version").replaceAll ("VERSION", getVersion ()));
		originalCopyrightLine1Label.setText	(getLanguage ().findCategoryEntry ("frmMainMenu", "OriginalCopyrightLine1"));
		originalCopyrightLine2Label.setText	(getLanguage ().findCategoryEntry ("frmMainMenu", "OriginalCopyrightLine2"));
		authorLabel.setText						(getLanguage ().findCategoryEntry ("frmMainMenu", "LanguageFileAuthor"));

		changeLanguageAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "ChangeLanguage"));
		connectToServerAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "ConnectToServer"));
		newGameAction.putValue			(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "NewGame"));
		joinGameAction.putValue				(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "JoinGame"));
		optionsAction.putValue				(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "Options"));
		exitToWindowsAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmMainMenu", "Exit"));
	}
	
	/**
	 * Updates enable/disable status of actions
	 */
	public final void enableActions ()
	{
		newGameAction.setEnabled (getClient ().getOurPlayerID () != null);
		newGameButton.setForeground (newGameAction.isEnabled () ? MomUIConstants.GOLD : MomUIConstants.GRAY);
		
		joinGameAction.setEnabled (getClient ().getOurPlayerID () != null);
		joinGameButton.setForeground (joinGameAction.isEnabled () ? MomUIConstants.GOLD : MomUIConstants.GRAY);
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

	/**
	 * @return Choose language UI
	 */
	public final ChooseLanguageUI getChooseLanguageUI ()
	{
		return chooseLanguageUI;
	}

	/**
	 * @param ui Choose language UI
	 */
	public final void setChooseLanguageUI (final ChooseLanguageUI ui)
	{
		chooseLanguageUI = ui;
	}

	/**
	 * @return Connect to server UI
	 */
	public final ConnectToServerUI getConnectToServerUI ()
	{
		return connectToServerUI;
	}

	/**
	 * @param ui Connect to server UI
	 */
	public final void setConnectToServerUI (final ConnectToServerUI ui)
	{
		connectToServerUI = ui;
	}

	/**
	 * @return New Game UI
	 */
	public final NewGameUI getNewGameUI ()
	{
		return newGameUI;
	}

	/**
	 * @param ui New Game UI
	 */
	public final void setNewGameUI (final NewGameUI ui)
	{
		newGameUI = ui;
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
}

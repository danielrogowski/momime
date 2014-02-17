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
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import momime.client.language.database.v0_9_4.KnownServer;

/**
 * Screen for choosing a server to connect to
 */
public final class ConnectToServerUI extends MomClientAbstractUI
{
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Main menu UI */
	private MainMenuUI mainMenuUI;
	
	/** Title */
	private JLabel title;
	
	/** Select server label */
	private JLabel selectServer;
	
	/** IP address label */
	private JLabel ipAddressLabel;

	/** Player name label */
	private JLabel playerNameLabel;

	/** Password label */
	private JLabel passwordLabel;

	/** New account checkbox */
	private JCheckBox newAccount;
	
	/** Kick existing connection checkbox */
	private JCheckBox kickAccount;
	
	/** Cancel action */
	private Action cancelAction;

	/** OK action */
	private Action okAction;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 3;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/background.png");
		final BufferedImage divider = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/divider.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Pressed.png");
		final BufferedImage wideButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button290x17Normal.png");
		final BufferedImage wideButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button290x17Pressed.png");
		final BufferedImage checkboxUnticked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Unticked.png");
		final BufferedImage checkboxTicked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Ticked.png");
		final BufferedImage editbox = getUtils ().loadImage ("/momime.client.graphics/ui/editBoxes/editBox125x23.png");

		// Actions
		cancelAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -6610185264156193758L;

			@Override
			public void actionPerformed (final ActionEvent e)
			{
				getFrame ().setVisible (false);
			}
		};

		final JTextField ipAddress;
		final JTextField playerName;
		final JTextField password;
		
		okAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
		
		// Do this "too early" on purpose, so that the window isn't centred over the main menu, but is a little down-right of it
		getFrame ().setLocationRelativeTo (getMainMenuUI ().getFrame ());

		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = 4787936461589746999L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Scale the background image up smoothly
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
				g.drawImage (background, 0, 0, getWidth (), getHeight (), null);
			}
		};
		contentPane.setBackground (Color.BLACK);
		
		final Dimension fixedSize = new Dimension (640, 480);
 		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
 		// The LHS of the screen is 1 column
 		// The RHS of the screen is 3 columns, the right column is the cancel button, middle is the OK button
 		// (the player name+password boxes being stretched across the 2) and the left column contains the labels
		contentPane.setLayout (new GridBagLayout ());
		
		// Cut off left half of the window
		contentPane.add (Box.createRigidArea (new Dimension (335, 0)), getUtils ().createConstraints (0, 0, 1, INSET, GridBagConstraints.CENTER));
		
		// Header
		title = getUtils ().createLabel (MomUIUtils.GOLD, getLargeFont ());
		contentPane.add (title, getUtils ().createConstraints (1, 0, 3, INSET, GridBagConstraints.CENTER));
		
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, 1, 3, INSET, GridBagConstraints.CENTER));

		selectServer = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (selectServer, getUtils ().createConstraints (1, 2, 3, INSET, GridBagConstraints.CENTER));
		
		// Server list
		int gridy = 3;
		for (final KnownServer server : getLanguage ().getKnownServer ())
		{
			final Action serverAction = new AbstractAction (server.getKnownServerDescription () + " (" + server.getKnownServerIP () + ")")
			{
				@Override
				public void actionPerformed (final ActionEvent ev)
				{
				}
			};
			
			contentPane.add (getUtils ().createImageButton (serverAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (), wideButtonNormal, wideButtonPressed),
				getUtils ().createConstraints (1, gridy, 3, INSET, GridBagConstraints.CENTER));
			gridy++;
		}
		
		// IP address
		ipAddressLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (ipAddressLabel, getUtils ().createConstraints (1, gridy, 3, INSET, GridBagConstraints.CENTER));
		gridy++;
		
		ipAddress = getUtils ().createTextFieldWithBackgroundImage (MomUIUtils.SILVER, getMediumFont (), editbox);
		contentPane.add (ipAddress, getUtils ().createConstraints (1, gridy, 3, INSET, GridBagConstraints.CENTER));
		gridy++;

		// Space in between
		final GridBagConstraints constraints = getUtils ().createConstraints (1, gridy, 3, INSET, GridBagConstraints.CENTER);
		constraints.weightx = 1;
		constraints.weighty = 1;
		contentPane.add (Box.createGlue (), constraints);
		gridy++;
		
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, gridy, 3, INSET, GridBagConstraints.CENTER));
		gridy++;

		// Player name and password
		playerNameLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (playerNameLabel, getUtils ().createConstraints (1, gridy, 1, INSET, GridBagConstraints.WEST));

		playerName = getUtils ().createTextFieldWithBackgroundImage (MomUIUtils.SILVER, getMediumFont (), editbox);
		contentPane.add (playerName, getUtils ().createConstraints (2, gridy, 2, INSET, GridBagConstraints.EAST));
		gridy++;
		
		passwordLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (passwordLabel, getUtils ().createConstraints (1, gridy, 1, INSET, GridBagConstraints.WEST));

		password = getUtils ().createPasswordFieldWithBackgroundImage (MomUIUtils.SILVER, getMediumFont (), editbox);
		contentPane.add (password, getUtils ().createConstraints (2, gridy, 2, INSET, GridBagConstraints.EAST));
		gridy++;
		
		// Checkboxes
		newAccount = getUtils ().createImageCheckBox (MomUIUtils.GOLD, getMediumFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (newAccount, getUtils ().createConstraints (1, gridy, 3, INSET, GridBagConstraints.WEST));
		gridy++;

		kickAccount = getUtils ().createImageCheckBox (MomUIUtils.GOLD, getMediumFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (kickAccount, getUtils ().createConstraints (1, gridy, 3, INSET, GridBagConstraints.WEST));
		gridy++;
		
		// Footer
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, gridy, 2, INSET, GridBagConstraints.CENTER));
		gridy++;

		final GridBagConstraints constraints2 = getUtils ().createConstraints (2, gridy, 1, INSET, GridBagConstraints.EAST);
		constraints2.weightx = 1;		// Move the OK button as far to the right as possible
		contentPane.add (getUtils ().createImageButton (okAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed),
			constraints2);
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed),
			getUtils ().createConstraints (3, gridy, 1, INSET, GridBagConstraints.EAST));
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setResizable (false);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		final String text = getLanguage ().findCategoryEntry ("frmConnectToServer", "Title");
		getFrame ().setTitle (text);
		title.setText (text);

		selectServer.setText (getLanguage ().findCategoryEntry ("frmConnectToServer", "PickFromList"));
		ipAddressLabel.setText (getLanguage ().findCategoryEntry ("frmConnectToServer", "EnterServer"));
		playerNameLabel.setText (getLanguage ().findCategoryEntry ("frmConnectToServer", "PlayerName"));
		passwordLabel.setText (getLanguage ().findCategoryEntry ("frmConnectToServer", "Password"));
		
		newAccount.setText (getLanguage ().findCategoryEntry ("frmConnectToServer", "CreateAccount"));
		kickAccount.setText (getLanguage ().findCategoryEntry ("frmConnectToServer", "KickExistingConnection"));
		
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmConnectToServer", "Cancel"));
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmConnectToServer", "OK"));
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
	 * @return Main menu UI
	 */
	public final MainMenuUI getMainMenuUI ()
	{
		return mainMenuUI;
	}

	/**
	 * @param ui Main menu UI
	 */
	public final void setMainMenuUI (final MainMenuUI ui)
	{
		mainMenuUI = ui;
	}
}
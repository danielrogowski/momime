package momime.client.ui.frames;

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
import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.language.database.v0_9_5.KnownServer;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.sessionbase.CreateAccount;
import com.ndg.multiplayer.sessionbase.Login;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Screen for choosing a server to connect to
 */
public final class ConnectToServerUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ConnectToServerUI.class);
	
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Title */
	private JLabel title;
	
	/** Select server label */
	private JLabel selectServer;
	
	/** IP address label */
	private JLabel ipAddressLabel;
	
	/** IP address edit box */
	private JTextField ipAddress;

	/** Player name label */
	private JLabel playerNameLabel;

	/** Player name edit box */
	private JTextField playerName;
	
	/** Password label */
	private JLabel passwordLabel;

	/** Password edit box */
	private JTextField password;
	
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
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Disabled.png");
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

		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -1830313744189041084L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getClient ().setServerAddress (ipAddress.getText ());
					getClient ().connect ();
					
					// The next thing that happens from there is we either get an exception if we can't
					// connect, or the server sends NewGameDatabaseMessage if we can
					// See the afterConnected () method below
				}
				catch (final Exception e)
				{
					// Get the key for the message in the langauge XML
					// This still uses the old Delphi numeric codes, but didn't see much point in changing them, they're just keys
					final String entryID;
					if (e instanceof UnknownHostException)
						entryID = "10049";
					else if (e instanceof ConnectException)
					{
						if (e.getMessage ().contains ("timed"))
							entryID = "10060";
						else
							entryID = "10061";
					}
					else
					{
						entryID = "Other";
						log.error (e, e);
					}

					// Display in window
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setTitleLanguageCategoryID ("ConnectionErrors");
					msg.setTitleLanguageEntryID ("Title");
					msg.setTextLanguageCategoryID ("ConnectionErrors");
					msg.setTextLanguageEntryID (entryID);
					try
					{
						msg.setVisible (true);
					}
					catch (final Exception e2)
					{
						log.error (e2, e2);
					}
				}
			}
		};
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
		
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
		contentPane.add (Box.createRigidArea (new Dimension (335, 0)), getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Header
		title = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (title, getUtils ().createConstraintsNoFill (1, 0, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraintsNoFill (1, 1, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		selectServer = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (selectServer, getUtils ().createConstraintsNoFill (1, 2, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Server list
		int gridy = 3;
		for (final KnownServer server : getLanguage ().getKnownServer ())
		{
			final Action serverAction = new AbstractAction (server.getKnownServerDescription () + " (" + server.getKnownServerIP () + ")")
			{
				private static final long serialVersionUID = 3576390732905287196L;

				@Override
				public void actionPerformed (final ActionEvent ev)
				{
					ipAddress.setText (server.getKnownServerIP ());
				}
			};
			
			// There's no "disabled" image for the wide button
			contentPane.add (getUtils ().createImageButton (serverAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
				wideButtonNormal, wideButtonPressed, wideButtonNormal), getUtils ().createConstraintsNoFill (1, gridy, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			gridy++;
		}
		
		// IP address
		ipAddressLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (ipAddressLabel, getUtils ().createConstraintsNoFill (1, gridy, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		gridy++;

		ipAddress = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getMediumFont (), editbox);
		contentPane.add (ipAddress, getUtils ().createConstraintsNoFill (1, gridy, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		gridy++;

		// Space in between
		final GridBagConstraints constraints = getUtils ().createConstraintsNoFill (1, gridy, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE);
		constraints.weightx = 1;
		constraints.weighty = 1;
		contentPane.add (Box.createGlue (), constraints);
		gridy++;
		
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraintsNoFill (1, gridy, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		gridy++;

		// Player name and password
		playerNameLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (playerNameLabel, getUtils ().createConstraintsNoFill (1, gridy, 1, 1, INSET, GridBagConstraintsNoFill.WEST));

		playerName = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getMediumFont (), editbox);
		contentPane.add (playerName, getUtils ().createConstraintsNoFill (2, gridy, 2, 1, INSET, GridBagConstraintsNoFill.EAST));
		gridy++;
		
		passwordLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (passwordLabel, getUtils ().createConstraintsNoFill (1, gridy, 1, 1, INSET, GridBagConstraintsNoFill.WEST));

		password = getUtils ().createPasswordFieldWithBackgroundImage (MomUIConstants.SILVER, getMediumFont (), editbox);
		contentPane.add (password, getUtils ().createConstraintsNoFill (2, gridy, 2, 1, INSET, GridBagConstraintsNoFill.EAST));
		gridy++;
		
		// Checkboxes
		newAccount = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getMediumFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (newAccount, getUtils ().createConstraintsNoFill (1, gridy, 3, 1, INSET, GridBagConstraintsNoFill.WEST));
		gridy++;

		kickAccount = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getMediumFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (kickAccount, getUtils ().createConstraintsNoFill (1, gridy, 3, 1, INSET, GridBagConstraintsNoFill.WEST));
		gridy++;
		
		// Footer
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraintsNoFill (1, gridy, 2, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		gridy++;

		final GridBagConstraints constraints2 = getUtils ().createConstraintsNoFill (2, gridy, 1, 1, INSET, GridBagConstraintsNoFill.EAST);
		constraints2.weightx = 1;		// Move the OK button as far to the right as possible
		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), constraints2);
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), getUtils ().createConstraintsNoFill (3, gridy, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		// Ok button should only be enabled once we have enough info
		final DocumentListener documentListener = new DocumentListener ()
		{
			@Override
			public final void insertUpdate (final DocumentEvent e)
			{
				enabledOrDisableOkButton ();
			}

			@Override
			public final void removeUpdate (final DocumentEvent e)
			{
				enabledOrDisableOkButton ();
			}

			@Override
			public final void changedUpdate (final DocumentEvent e)
			{
				enabledOrDisableOkButton ();
			}
		};
		
		ipAddress.getDocument ().addDocumentListener (documentListener);
		playerName.getDocument ().addDocumentListener (documentListener);
		password.getDocument ().addDocumentListener (documentListener);
		enabledOrDisableOkButton ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
	}
	
	/**
	 * Ok button should only be enabled once we have enough info
	 */
	private final void enabledOrDisableOkButton ()
	{
		okAction.setEnabled ((!ipAddress.getText ().trim ().equals ("")) &&
			(!playerName.getText ().trim ().equals ("")) &&
			(!password.getText ().trim ().equals ("")));
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
	 * Triggered after we connect + receive the new game database from the server
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public final void afterConnected () throws JAXBException, XMLStreamException
	{
		if (newAccount.isSelected ())
		{
			final CreateAccount msg = new CreateAccount ();
			msg.setPlayerName (playerName.getText ());
			msg.setPlayerPassword (password.getText ());
			getClient ().getServerConnection ().sendMessageToServer (msg);
			
			// We'll then proceed to login after the createAccount call returns
		}
		else
			afterAccountCreated ();
	}
	
	/**
	 * Triggered after we connect and create an account above
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public final void afterAccountCreated () throws JAXBException, XMLStreamException
	{
		final Login msg = new Login ();
		msg.setPlayerName (playerName.getText ());
		msg.setPlayerPassword (password.getText ());
		msg.setKickExistingConnection (kickAccount.isSelected ());
		getClient ().getServerConnection ().sendMessageToServer (msg);
	}
	
	/**
	 * Triggered after we successfully log in
	 */
	public final void afterLoggedIn ()
	{
		// Remember the name that we logged in as
		getClient ().setOurPlayerName (playerName.getText ());
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
}
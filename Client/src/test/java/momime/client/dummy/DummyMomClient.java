package momime.client.dummy;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.net.Socket;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import momime.client.database.v0_9_4.AvailableDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.DifficultyLevel;
import momime.common.database.v0_9_4.DifficultyLevelNodeStrength;
import momime.common.database.v0_9_4.FogOfWarSetting;
import momime.common.database.v0_9_4.LandProportion;
import momime.common.database.v0_9_4.MapSize;
import momime.common.database.v0_9_4.NodeStrength;
import momime.common.database.v0_9_4.SpellSetting;
import momime.common.database.v0_9_4.UnitSetting;
import momime.common.database.v0_9_4.WizardPick;
import momime.common.messages.clienttoserver.v0_9_4.ChooseCustomFlagColourMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseCustomPicksMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseInitialSpellsMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseRaceMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseStandardPhotoMessage;
import momime.common.messages.clienttoserver.v0_9_4.ChooseWizardMessage;
import momime.common.messages.clienttoserver.v0_9_4.UploadCustomPhotoMessage;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.TurnSystem;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ndg.multiplayer.sessionbase.CreateAccount;
import com.ndg.multiplayer.sessionbase.JoinSession;
import com.ndg.multiplayer.sessionbase.Login;
import com.ndg.multiplayer.sessionbase.Logout;
import com.ndg.multiplayer.sessionbase.NewSession;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.RequestSessionList;

/**
 * Demo showing how to connect to a session server
 */
public final class DummyMomClient
{
	/** Spring application context */
	private ApplicationContext applicationContext;
	
	/** Area on demo panel to add messages to */
	private final JTextArea textArea;

	/** Hostname/IP address of server */
	private final JTextField hostname;

	/** Server port */
	private final JTextField port;

	/** Player name */
	private final JTextField playerName;

	/** Player password */
	private final JTextField playerPassword;

	/** Kick existing connection checkbox */
	private final JCheckBox kickExistingConnection;

	/** Session name */
	private final JTextField sessionName;

	/** Human opponents */
	private final JTextField humanOpponents;

	/** AI opponents */
	private final JTextField aiOpponents;

	/** Connect action */
	private final Action connectAction;

	/** Disconnect action */
	private final Action disconnectAction;

	/** Create account action */
	private final Action createAccountAction;

	/** Login action */
	private final Action loginAction;

	/** Logout action */
	private final Action logoutAction;

	/** Player ID we are logged on with, 0 if not yet logged in */
	private final JTextField playerID;

	/** Request session list action */
	private final Action requestSessionListAction;

	/** New session action */
	private final Action newSessionAction;

	/** Join session action */
	private final Action joinSessionAction;

	/** Wizard ID */
	private final JTextField wizardID;

	/** Choose wizard action */
	private final Action chooseWizardAction;

	/** Photo ID */
	private final JTextField photoID;

	/** Choose standard photo action */
	private final Action chooseStandardPhotoAction;

	/** Choose custom photo action */
	private final Action chooseCustomPhotoAction;

	/** Custom picks */
	private final JTextField customPicks;

	/** Custom picks action */
	private final Action customPicksAction;

	/** Flag colour */
	private final JTextField flagColour;

	/** Choose flag colour action */
	private final Action flagColourAction;

	/** Pick ID */
	private final JTextField pickID;

	/** Spell IDs */
	private final JTextField spellIDs;

	/** Choose free spells action */
	private final Action chooseFreeSpellsAction;

	/** Session ID we want to join */
	private final JTextField sessionID;

	/** Race ID */
	private final JTextField raceID;

	/** Choose race action */
	private final Action chooseRaceAction;

	/** New game database sent by server */
	private AvailableDatabase newGameDatabase;

	/** Connection to server */
	DummyMomClientThread connection;

	/** Standard label height */
	public static final int LABEL_HEIGHT = 16;

	/** Standard button width */
	public static final int BUTTON_WIDTH = 120;

	/** Standard text field height */
	public static final int TEXT_FIELD_HEIGHT = 21;

	/** Space to leave between controls */
	public static final int SPACE_BETWEEN_CONTROLS = 8;

	/** Standard button height (just large enough for the Delphi button images with a 1 pixel border around the image) */
	public static final int BUTTON_HEIGHT = 22;

	/** Standard size for full width buttons */
	public static final Dimension BUTTON_SIZE = new Dimension (BUTTON_WIDTH, BUTTON_HEIGHT);

	/** Size to make labels */
	public static final Dimension LABEL_SIZE = new Dimension (BUTTON_WIDTH, LABEL_HEIGHT);

	/** Size to make text fields */
	public static final Dimension TEXT_FIELD_SIZE = new Dimension (BUTTON_WIDTH, TEXT_FIELD_HEIGHT);

	/**
	 * Initializes the frame
	 */
	public DummyMomClient ()
	{
		super ();
		final DummyMomClient client = this;

		// Connect action
		connectAction = new AbstractAction ("Connect")
		{
			private static final long serialVersionUID = 4328466973434785602L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Connecting to " + hostname.getText () + " port " + port.getText () + "...");
				try
				{
					connection = new DummyMomClientThread (new Socket (hostname.getText (), Integer.parseInt (port.getText ())), null, client);
					connection.start ();
					enableOrDisableButtons ();
				}
				catch (final Exception e)
				{
					addToTextArea ("Connection failed, error was: " + e.getMessage ());
				}
			}
		};

		// Disconnect action
		disconnectAction = new AbstractAction ("Disconnect")
		{
			private static final long serialVersionUID = -5872873358962789620L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Disconnecting...");

				try
				{
					connection.close (null);
					connection = null;
					enableOrDisableButtons ();
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Create account action
		createAccountAction = new AbstractAction ("Create Account")
		{
			private static final long serialVersionUID = -5872873358962789620L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting creating an account for " + playerName.getText () + "...");

				try
				{
					final CreateAccount msg = new CreateAccount ();
					msg.setPlayerName (playerName.getText ());
					msg.setPlayerPassword (playerPassword.getText ());
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Login action
		loginAction = new AbstractAction ("Login")
		{
			private static final long serialVersionUID = 6564124834507036623L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Logging in " + playerName.getText () + "...");

				try
				{
					final Login msg = new Login ();
					msg.setPlayerName (playerName.getText ());
					msg.setPlayerPassword (playerPassword.getText ());
					msg.setKickExistingConnection (kickExistingConnection.isSelected ());
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Logout action
		logoutAction = new AbstractAction ("Logout")
		{
			private static final long serialVersionUID = -194241534913818554L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Logging out...");

				try
				{
					connection.sendMessageToServer (new Logout ());
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Request session list action
		requestSessionListAction = new AbstractAction ("Req. session list")
		{
			private static final long serialVersionUID = 7322577254317003010L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting session list...");

				try
				{
					connection.sendMessageToServer (new RequestSessionList ());
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// New session action
		newSessionAction = new AbstractAction ("New Session")
		{
			private static final long serialVersionUID = 6564124834507036623L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting new session for " + playerName.getText () + "...");

				try
				{
					final MomSessionDescription sd = createMomSessionDescription (sessionName.getText (), TurnSystem.ONE_PLAYER_AT_A_TIME,
						Integer.parseInt (humanOpponents.getText ()), Integer.parseInt (aiOpponents.getText ()),
						"60x40", "LP03", "NS03", "DL05", "FOW02", "US02", "SS02");

					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (Integer.parseInt (playerID.getText ()));
					pd.setPlayerName (playerName.getText ());

					final NewSession msg = new NewSession ();
					msg.setSessionDescription (sd);
					msg.setPlayerDescription (pd);
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Join session action
		joinSessionAction = new AbstractAction ("Join Session")
		{
			private static final long serialVersionUID = 5471646458344352757L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting player " + playerName.getText () + " to join session " + sessionID.getText () + "...");

				try
				{
					final PlayerDescription pd = new PlayerDescription ();
					pd.setPlayerID (Integer.parseInt (playerID.getText ()));
					pd.setPlayerName (playerName.getText ());

					final JoinSession msg = new JoinSession ();
					msg.setSessionID (Integer.parseInt (sessionID.getText ()));
					msg.setPlayerDescription (pd);
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Choose wizard action
		chooseWizardAction = new AbstractAction ("Choose Wizard")
		{
			private static final long serialVersionUID = 6854763329571504656L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				final String useWizardID = ((wizardID.getText () != null) && (wizardID.getText ().equals (""))) ? null : wizardID.getText ();

				addToTextArea ("Requesting wizard ID " + useWizardID);

				final ChooseWizardMessage msg = new ChooseWizardMessage ();
				msg.setWizardID (useWizardID);
				try
				{
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Choose standard photo action
		chooseStandardPhotoAction = new AbstractAction ("Standard photo")
		{
			private static final long serialVersionUID = 8557338197224962330L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting standard photo ID " + photoID.getText ());

				final ChooseStandardPhotoMessage msg = new ChooseStandardPhotoMessage ();
				msg.setPhotoID (photoID.getText ());
				try
				{
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Choose custom photo action
		chooseCustomPhotoAction = new AbstractAction ("Custom photo")
		{
			private static final long serialVersionUID = 6753145627993394894L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting custom photo");

				final byte [] ndgBmp = new byte [256];
				for (int n = 0; n <= 255; n++)
					ndgBmp [n] = (byte) n;

				final UploadCustomPhotoMessage msg = new UploadCustomPhotoMessage ();
				msg.setNdgBmpImage (ndgBmp);
				try
				{
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Custom picks action
		customPicksAction = new AbstractAction ("Custom picks")
		{
			private static final long serialVersionUID = -5493611257340805193L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting custom pick(s) " + customPicks.getText ());

				final ChooseCustomPicksMessage msg = new ChooseCustomPicksMessage ();
				for (final String thisPick : customPicks.getText ().split (","))
				{
					final WizardPick pick = new WizardPick ();
					final int xpos = thisPick.indexOf ("x");
					if (xpos < 0)
					{
						pick.setQuantity (1);
						pick.setPick (thisPick);
					}
					else
					{
						pick.setQuantity (Integer.parseInt (thisPick.substring (0, xpos)));
						pick.setPick (thisPick.substring (xpos + 1));
					}
					msg.getPick ().add (pick);
				}

				try
				{
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Choose flag colour action
		flagColourAction = new AbstractAction ("Custom flag colour")
		{
			private static final long serialVersionUID = -5493611257340805193L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting flag colour " + flagColour.getText ());

				final ChooseCustomFlagColourMessage msg = new ChooseCustomFlagColourMessage ();
				msg.setFlagColour (flagColour.getText ());
				try
				{
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Choose free spells action
		chooseFreeSpellsAction = new AbstractAction ("Choose free spells")
		{
			private static final long serialVersionUID = 8557338197224962330L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				final ChooseInitialSpellsMessage msg = new ChooseInitialSpellsMessage ();
				msg.setPickID (pickID.getText ());
				for (final String thisSpellID : spellIDs.getText ().split (","))
				{
					msg.getSpell ().add (thisSpellID);
				}

				try
				{
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Choose race action
		chooseRaceAction = new AbstractAction ("Choose Race")
		{
			private static final long serialVersionUID = 6753145627993394894L;

			@Override
			public final void actionPerformed (final ActionEvent event)
			{
				addToTextArea ("Requesting race ID " + raceID.getText ());

				final ChooseRaceMessage msg = new ChooseRaceMessage ();
				msg.setRaceID (raceID.getText ());
				try
				{
					connection.sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};

		// Create frame
		final JFrame frame = new JFrame ();
		frame.setSize (1400, 800);
		frame.setTitle ("DummyMomClient");
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);

		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		SwingLayoutUtils.addGapBorder (contentPane);
		contentPane.setLayout (new BoxLayout (contentPane, BoxLayout.Y_AXIS));

		// Create top/bottom sections
		final JPanel topSection = new JPanel ();
		SwingLayoutUtils.addGapBorder (topSection);
		topSection.setLayout (new BoxLayout (topSection, BoxLayout.X_AXIS));
		contentPane.add (topSection);

		contentPane.add (Box.createRigidArea (new Dimension (0, SPACE_BETWEEN_CONTROLS)));

		final JScrollPane scrollPane = new JScrollPane ();

		textArea = new JTextArea ();
		scrollPane.setViewportView (textArea);
		contentPane.add (scrollPane);
		scrollPane.setPreferredSize (new Dimension (2000, 2000));		// By making the text area as large as possible, the buttons end up as small as possible

		// Connection box
		final JPanel connectSection = new JPanel ();
		SwingLayoutUtils.addTextBorder (connectSection, "Connect");
		connectSection.setLayout (new GridLayout (4, 2, SPACE_BETWEEN_CONTROLS, SPACE_BETWEEN_CONTROLS));
		topSection.add (connectSection);

		topSection.add (Box.createRigidArea (new Dimension (SPACE_BETWEEN_CONTROLS, 0)));

		hostname = createTextField ("localhost");
		port = createTextField ("18250");

		connectSection.add (createLabel ("Host name/IP:"));
		connectSection.add (hostname);
		connectSection.add (createLabel ("Port:"));
		connectSection.add (port);
		connectSection.add (createButton (requestSessionListAction));
		connectSection.add (createButton (connectAction));
		connectSection.add (createButton ("Leave session"));
		connectSection.add (createButton (disconnectAction));

		// Player info box
		final JPanel playerInfoSection = new JPanel ();
		SwingLayoutUtils.addTextBorder (playerInfoSection, "New Player");
		playerInfoSection.setLayout (new GridLayout (6, 2, SPACE_BETWEEN_CONTROLS, SPACE_BETWEEN_CONTROLS));
		topSection.add (playerInfoSection);

		topSection.add (Box.createRigidArea (new Dimension (SPACE_BETWEEN_CONTROLS, 0)));

		playerName = createTextField ("Nigel");
		playerPassword = createTextField ("m0m1m3");
		kickExistingConnection = new JCheckBox ();
		playerID = createTextField ("0");

		playerID.setEnabled (false);

		playerInfoSection.add (createLabel ("Player name:"));
		playerInfoSection.add (playerName);
		playerInfoSection.add (createLabel ("Password:"));
		playerInfoSection.add (playerPassword);
		playerInfoSection.add (createLabel ("Kick existing:"));
		playerInfoSection.add (kickExistingConnection);
		playerInfoSection.add (createLabel ("Player ID:"));
		playerInfoSection.add (playerID);
		playerInfoSection.add (createButton (createAccountAction));
		playerInfoSection.add (createButton (loginAction));
		playerInfoSection.add (Box.createRigidArea (new Dimension (0, 0)));	// Leave a space in grid
		playerInfoSection.add (createButton (logoutAction));

		// Panel for new and join session
		final JPanel newAndJoinSessionSection = new JPanel ();
		newAndJoinSessionSection.setLayout (new BoxLayout (newAndJoinSessionSection, BoxLayout.Y_AXIS));
		topSection.add (newAndJoinSessionSection);

		topSection.add (Box.createRigidArea (new Dimension (SPACE_BETWEEN_CONTROLS, 0)));

		// New session box
		final JPanel newSessionSection = new JPanel ();
		SwingLayoutUtils.addTextBorder (newSessionSection, "New Session");
		newSessionSection.setLayout (new GridLayout (4, 2, SPACE_BETWEEN_CONTROLS, SPACE_BETWEEN_CONTROLS));
		newAndJoinSessionSection.add (newSessionSection);

		newAndJoinSessionSection.add (Box.createRigidArea (new Dimension (0, SPACE_BETWEEN_CONTROLS)));

		sessionName = createTextField ("Java test client");
		humanOpponents = createTextField ("0");
		aiOpponents = createTextField ("4");

		newSessionSection.add (createLabel ("Session name:"));
		newSessionSection.add (sessionName);
		newSessionSection.add (createLabel ("Human opponents:"));
		newSessionSection.add (humanOpponents);
		newSessionSection.add (createLabel ("AI opponents:"));
		newSessionSection.add (aiOpponents);
		newSessionSection.add (Box.createRigidArea (new Dimension (0, 0)));	// Leave a space in grid
		newSessionSection.add (createButton (newSessionAction));

		// Join session box
		final JPanel joinSessionSection = new JPanel ();
		SwingLayoutUtils.addTextBorder (joinSessionSection, "Join Session");
		joinSessionSection.setLayout (new GridLayout (2, 2, SPACE_BETWEEN_CONTROLS, SPACE_BETWEEN_CONTROLS));
		newAndJoinSessionSection.add (joinSessionSection);

		sessionID = createTextField ("1");

		joinSessionSection.add (createLabel ("Session ID:"));
		joinSessionSection.add (sessionID);
		joinSessionSection.add (Box.createRigidArea (new Dimension (0, 0)));	// Leave a space in grid
		joinSessionSection.add (createButton (joinSessionAction));

		// MoM pre game config box
		final JPanel momPregameConfig = new JPanel ();
		SwingLayoutUtils.addTextBorder (momPregameConfig, "MoM pre-game config");
		momPregameConfig.setLayout (new GridLayout (7, 3, SPACE_BETWEEN_CONTROLS, SPACE_BETWEEN_CONTROLS));
		topSection.add (momPregameConfig);

		wizardID = createTextField ("WZ12");																						// Ariel
		photoID = createTextField ("WZ09");																						// Tauron
		customPicks = createTextField ("9xMB01,RT02");																		// 9 life books and Walord
		flagColour = createTextField ("A0A0A0");
		pickID = createTextField ("MB01");																							// Life spell selection for Ariel
		spellIDs = createTextField ("SP121,SP122,SP123,SP124,SP125,SP126,SP127,SP128,SP129");		// Free common life spells to choose
		raceID = createTextField ("RC04");																							// High elves

		momPregameConfig.add (createLabel ("Wizard ID:"));
		momPregameConfig.add (wizardID);
		momPregameConfig.add (createButton (chooseWizardAction));

		momPregameConfig.add (createButton (chooseCustomPhotoAction));
		momPregameConfig.add (photoID);
		momPregameConfig.add (createButton (chooseStandardPhotoAction));

		momPregameConfig.add (createLabel ("Custom picks:"));
		momPregameConfig.add (customPicks);
		momPregameConfig.add (createButton (customPicksAction));

		momPregameConfig.add (createLabel ("Flag colour:"));
		momPregameConfig.add (flagColour);
		momPregameConfig.add (createButton (flagColourAction));

		momPregameConfig.add (createLabel ("Pick ID:"));
		momPregameConfig.add (pickID);
		momPregameConfig.add (Box.createRigidArea (new Dimension (0, 0)));	// Leave a space in grid

		momPregameConfig.add (createLabel ("Spell ID(s):"));
		momPregameConfig.add (spellIDs);
		momPregameConfig.add (createButton (chooseFreeSpellsAction));

		momPregameConfig.add (createLabel ("Race ID:"));
		momPregameConfig.add (raceID);
		momPregameConfig.add (createButton (chooseRaceAction));

		// Show frame
		frame.setContentPane (contentPane);
		frame.setVisible (true);

		// Set initial state of form
		enableOrDisableButtons ();
	}

	/**
	 * @param action Action to assign to button
	 * @return Creates a button and sizes it
	 */
	private final JButton createButton (final Action action)
	{
		final JButton button = new JButton (action);
		button.setMinimumSize (BUTTON_SIZE);
		button.setMaximumSize (BUTTON_SIZE);
		button.setPreferredSize (BUTTON_SIZE);
		return button;
	}

	/**
	 * @param text Text to write on button
	 * @return Creates a button and sizes it
	 */
	private final JButton createButton (final String text)
	{
		final JButton button = new JButton (text);
		button.setMinimumSize (BUTTON_SIZE);
		button.setMaximumSize (BUTTON_SIZE);
		button.setPreferredSize (BUTTON_SIZE);
		return button;
	}

	/**
	 * @param text Text to write on label
	 * @return Creates a button and sizes it
	 */
	private final JLabel createLabel (final String text)
	{
		final JLabel label = new JLabel (text);
		label.setMinimumSize (LABEL_SIZE);
		label.setMaximumSize (LABEL_SIZE);
		label.setPreferredSize (LABEL_SIZE);
		return label;
	}

	/**
	 * @param text Text to write on button
	 * @return Creates a button and sizes it
	 */
	private final JTextField createTextField (final String text)
	{
		final JTextField textField = new JTextField (text);
		textField.setMinimumSize (TEXT_FIELD_SIZE);
		textField.setMaximumSize (TEXT_FIELD_SIZE);
		textField.setPreferredSize (TEXT_FIELD_SIZE);
		return textField;
	}

	/**
	 * Enables or disables buttons depending on whether we are connected and whether we are in a session
	 */
	final void enableOrDisableButtons ()
	{
		connectAction.setEnabled (connection == null);
		disconnectAction.setEnabled (connection != null);
		createAccountAction.setEnabled (connection != null);

		loginAction.setEnabled ((connection != null) && (playerID.getText ().equals ("0")));
		logoutAction.setEnabled ((connection != null) && (!playerID.getText ().equals ("0")));
		requestSessionListAction.setEnabled ((connection != null) && (!playerID.getText ().equals ("0")));
		newSessionAction.setEnabled ((connection != null) && (!playerID.getText ().equals ("0")));
	}

	/**
	 * @param text Text to add to the output area on the panel
	 */
	final void addToTextArea (final String text)
	{
		textArea.setText (textArea.getText () + "\r\n" + text);
	}

	/**
	 * @param id Value to update player ID to
	 */
	final void setPlayerID (final int id)
	{
		playerID.setText (new Integer (id).toString ());
	}

	/**
	 * @param aNewGameDatabase New game database sent by server
	 */
	final void setNewGameDatabase (final AvailableDatabase aNewGameDatabase)
	{
		newGameDatabase = aNewGameDatabase;
	}

	/**
	 * @param sessionNameToUse Session name
	 * @param turnSystem Turn system
	 * @param humanOpponentCount Number of human opponents, i.e. excluding this player
	 * @param aiOpponentCount Number of AI opponents, excluding raiders and monsters guarding nodes/lairs/towers
	 * @param mapSizeID Map size to use
	 * @param landProportionID Land proportion to use
	 * @param nodeStrengthID Node strength to use
	 * @param difficultyLevelID Difficulty level to use
	 * @param fogOfWarSettingID Fog of war settings to use
	 * @param unitSettingID Unit settings to use
 	 * @param spellSettingID Spell settings to use
	 * @return Session description, built from selecting the specified parts from the new game database
	 * @throws RecordNotFoundException
	 */
	private final MomSessionDescription createMomSessionDescription (final String sessionNameToUse, final TurnSystem turnSystem, final int humanOpponentCount, final int aiOpponentCount,
		final String mapSizeID, final String landProportionID, final String nodeStrengthID, final String difficultyLevelID, final String fogOfWarSettingID, final String unitSettingID, final String spellSettingID)
		throws RecordNotFoundException
	{
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSessionName (sessionNameToUse);
		sd.setXmlDatabaseName (newGameDatabase.getDbName ());
		sd.setTurnSystem (turnSystem);
		sd.setAiPlayerCount (aiOpponentCount);
		sd.setMaxPlayers (humanOpponentCount + aiOpponentCount + 3);
		// sd.setDisableFogOfWar (true);

		for (final MapSize mapSize : newGameDatabase.getMapSize ())
			if (mapSize.getMapSizeID ().equals (mapSizeID))
				sd.setMapSize (mapSize);
		if (sd.getMapSize () == null)
			throw new RecordNotFoundException (MapSize.class.getName (), mapSizeID, "createMomSessionDescription");

		for (final LandProportion landProportion : newGameDatabase.getLandProportion ())
			if (landProportion.getLandProportionID ().equals (landProportionID))
				sd.setLandProportion (landProportion);
		if (sd.getLandProportion () == null)
			throw new RecordNotFoundException (LandProportion.class.getName (), landProportionID, "createMomSessionDescription");

		for (final NodeStrength nodeStrength : newGameDatabase.getNodeStrength ())
			if (nodeStrength.getNodeStrengthID ().equals (nodeStrengthID))
				sd.setNodeStrength (nodeStrength);
		if (sd.getNodeStrength () == null)
			throw new RecordNotFoundException (NodeStrength.class.getName (), nodeStrengthID, "createMomSessionDescription");

		for (final DifficultyLevel difficultyLevel : newGameDatabase.getDifficultyLevel ())
			if (difficultyLevel.getDifficultyLevelID ().equals (difficultyLevelID))
			{
				sd.setDifficultyLevel (difficultyLevel);

				// Also find difficulty level - node strength settings
				for (final DifficultyLevelNodeStrength nodeStrength : difficultyLevel.getDifficultyLevelNodeStrength ())
					if (nodeStrength.getNodeStrengthID ().equals (nodeStrengthID))
						sd.getDifficultyLevelNodeStrength ().add (nodeStrength);
			}
		if (sd.getDifficultyLevel () == null)
			throw new RecordNotFoundException (DifficultyLevel.class.getName (), difficultyLevelID, "createMomSessionDescription");

		for (final FogOfWarSetting fogOfWarSetting : newGameDatabase.getFogOfWarSetting ())
			if (fogOfWarSetting.getFogOfWarSettingID ().equals (fogOfWarSettingID))
				sd.setFogOfWarSetting (fogOfWarSetting);
		if (sd.getFogOfWarSetting () == null)
			throw new RecordNotFoundException (FogOfWarSetting.class.getName (), fogOfWarSettingID, "createMomSessionDescription");

		for (final UnitSetting unitSetting : newGameDatabase.getUnitSetting ())
			if (unitSetting.getUnitSettingID ().equals (unitSettingID))
				sd.setUnitSetting (unitSetting);
		if (sd.getUnitSetting () == null)
			throw new RecordNotFoundException (UnitSetting.class.getName (), unitSettingID, "createMomSessionDescription");

		for (final SpellSetting spellSetting : newGameDatabase.getSpellSetting ())
			if (spellSetting.getSpellSettingID ().equals (spellSettingID))
				sd.setSpellSetting (spellSetting);
		if (sd.getSpellSetting () == null)
			throw new RecordNotFoundException (SpellSetting.class.getName (), spellSettingID, "createMomSessionDescription");

		return sd;
	}

	/**
	 * @return Spring application context
	 */
	public final ApplicationContext getApplicationContext ()
	{
		return applicationContext;
	}
	
	/**
	 * @param ctx Spring application context 
	 */
	public final void setApplicationContext (final ApplicationContext ctx)
	{
		applicationContext = ctx;
	}
	
	/**
	 * @param args Command line arguments, ignored
	 */
	public final static void main (final String [] args)
	{
		// Switch to Windows look and feel if available, otherwise the open/save dialogs look gross
		try
		{
			UIManager.setLookAndFeel ("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		}
		catch (final Exception e)
		{
			// Don't worry if can't switch look and feel
		}

		// Create frame
		final ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/momime.common.spring/momime-common-beans.xml");
		new DummyMomClient ().setApplicationContext (applicationContext);
	}
}

package momime.client.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import momime.client.MomClient;
import momime.client.database.v0_9_4.AvailableDatabase;
import momime.client.database.v0_9_4.Wizard;
import momime.client.ui.actions.CycleAction;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_4.DifficultyLevelData;
import momime.common.database.newgame.v0_9_4.FogOfWarSettingData;
import momime.common.database.newgame.v0_9_4.LandProportionData;
import momime.common.database.newgame.v0_9_4.MapSizeData;
import momime.common.database.newgame.v0_9_4.NodeStrengthData;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.newgame.v0_9_4.UnitSettingData;
import momime.common.database.v0_9_4.DifficultyLevel;
import momime.common.database.v0_9_4.DifficultyLevelNodeStrength;
import momime.common.database.v0_9_4.FogOfWarSetting;
import momime.common.database.v0_9_4.LandProportion;
import momime.common.database.v0_9_4.MapSize;
import momime.common.database.v0_9_4.NodeStrength;
import momime.common.database.v0_9_4.SpellSetting;
import momime.common.database.v0_9_4.UnitSetting;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.TurnSystem;

import com.ndg.multiplayer.sessionbase.NewSession;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Screens for setting up new and joining existing games
 * This is pretty complex because the right hand side is a CardLayout with all the different dialog pages, especially for custom options
 */
public final class NewGameUI extends MomClientAbstractUI
{
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Main menu UI */
	private MainMenuUI mainMenuUI;

	/** Multiplayer client */
	private MomClient client;
	
	/** Cancel action */
	private Action cancelAction;

	/** OK action */
	private Action okAction;
	
	/** Card layout for right hand side */
	private CardLayout cardLayout;
	
	/** Panel that the card layout is the layout manager for */
	private JPanel cards;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 3;
	
	/** 74x21 button */
	private BufferedImage buttonNormal;
	
	/** 74x21 button */
	private BufferedImage buttonPressed;
	
	/** 74x21 button */
	private BufferedImage buttonDisabled;
	
	// NEW GAME PANEL

	/** Panel key */
	private final static String NEW_GAME_PANEL = "New";
	
	/** Panel title */
	private JLabel newGameTitle;

	/** Action for changing selected database */
	private CycleAction<AvailableDatabase> changeDatabaseAction;
	
	/** Label for human opponents button */
	private JLabel humanOpponentsLabel;
	
	/** Number of human opponents */
	private CycleAction<Integer> changeHumanOpponentsAction;
	
	/** Number of AI opponents */
	private CycleAction<Integer> changeAIOpponentsAction;
	
	/** Label for AI opponents button */
	private JLabel aiOpponentsLabel;
	
	/** Label for map size button */
	private JLabel mapSizeLabel;

	/** Action for selecting pre-defined map size or custom */
	private CycleAction<MapSize> changeMapSizeAction;
	
	/** Label for land proportion button */
	private JLabel landProportionLabel;

	/** Action for selecting pre-defined land proportion or custom */
	private CycleAction<LandProportion> changeLandProportionAction;
	
	/** Label for nodes button */
	private JLabel nodesLabel;

	/** Action for selecting pre-defined node strength or custom */
	private CycleAction<NodeStrength> changeNodeStrengthAction;
	
	/** Label for difficulty button */
	private JLabel difficultyLabel;

	/** Action for selecting pre-defined difficulty level or custom */
	private CycleAction<DifficultyLevel> changeDifficultyLevelAction;
	
	/** Label for turn system button */
	private JLabel turnSystemLabel;

	/** Action for changing turn system */
	private CycleAction<TurnSystem> changeTurnSystemAction;
	
	/** Label for Fog of War button */
	private JLabel fogOfWarLabel;

	/** Action for selecting pre-defined fog of war settings or custom */
	private CycleAction<FogOfWarSetting> changeFogOfWarSettingsAction;
	
	/** Label for unit settings button */
	private JLabel unitSettingsLabel;

	/** Action for selecting pre-defined unit settings or custom */
	private CycleAction<UnitSetting> changeUnitSettingsAction;
	
	/** Label for spell settings button */
	private JLabel spellSettingsLabel;

	/** Action for selecting pre-defined spell settings or custom */
	private CycleAction<SpellSetting> changeSpellSettingsAction;
	
	/** Label for debug options button */
	private JLabel debugOptionsLabel;

	/** Action for toggling debug options */
	private CycleAction<Boolean> changeDebugOptionsAction;
	
	/** Label for game name */
	private JLabel gameNameLabel;
	
	/** Game name */
	private JTextField gameName;
	
	// CUSTOM MAP SIZE PANEL

	/** Panel key */
	private final static String MAP_SIZE_PANEL = "Map";
	
	/** Panel title */
	private JLabel mapSizeTitle;
	
	// CUSTOM LAND PROPORTION PANEL

	/** Panel key */
	private final static String LAND_PROPORTION_PANEL = "Land";
	
	/** Panel title */
	private JLabel landProportionTitle;
	
	// CUSTOM NODES PANEL

	/** Panel key */
	private final static String NODES_PANEL = "Nodes";
	
	/** Panel title */
	private JLabel nodesTitle;
	
	// CUSTOM DIFFICULTY PANEL (1 of 2)

	/** Panel key */
	private final static String DIFFICULTY_1_PANEL = "Diff1";
	
	/** Panel title */
	private JLabel difficulty1Title;
	
	// CUSTOM DIFFICULTY PANEL (2 of 2)
	
	/** Panel key */
	private final static String DIFFICULTY_2_PANEL = "Diff2";
	
	/** Panel title */
	private JLabel difficulty2Title;
	
	// CUSTOM NODES-DIFFICULTY PANEL
	
	/** Panel key */
	private final static String NODES_DIFFICULTY_PANEL = "NodesDiff";
	
	/** Panel title */
	private JLabel nodesDifficultyTitle;	
	
	// CUSTOM FOG OF WAR PANEL
	
	/** Panel key */
	private final static String FOG_OF_WAR_PANEL = "FOW";
	
	/** Panel title */
	private JLabel fogOfWarTitle;
	
	// CUSTOM UNIT SETTINGS PANEL
	
	/** Panel key */
	private final static String UNITS_PANEL = "Units";
	
	/** Panel title */
	private JLabel unitsTitle;

	// CUSTOM SPELL SETTINGS PANEL
	
	/** Panel key */
	private final static String SPELLS_PANEL = "Spell";
	
	/** Panel title */
	private JLabel spellsTitle;
	
	// DEBUG OPTIONS PANEL
	
	/** Panel key */
	private final static String DEBUG_PANEL = "Debug";
	
	/** Panel title */
	private JLabel debugTitle;
	
	// JOIN GAME PANEL
	
	/** Panel key */
	private final static String JOIN_GAME_PANEL = "Join";
	
	/** Panel title */
	private JLabel joinGameTitle;
	
	// WIZARD SELECTION PANEL

	/** Panel key */
	private final static String WIZARD_PANEL = "Wizard";

	/** Panel */
	private JPanel wizardPanel;
	
	/** Panel title */
	private JLabel wizardTitle;
	
	/** Dynamically created button actions */
	private Map<String, Action> wizardButtonActions = new HashMap<String, Action> ();

	/** Dynamically created buttons etc */
	private List<Component> wizardComponents = new ArrayList<Component> ();
	
	// PORTRAIT SELECTION PANEL (for custom wizards)

	/** Panel key */
	private final static String PORTRAIT_PANEL = "Portrait";
	
	/** Panel title */
	private JLabel portraitTitle;
	
	// FLAG COLOUR PANEL (for custom wizards with custom portraits)
	
	/** Panel key */
	private final static String FLAG_PANEL = "Flag";
	
	/** Panel title */
	private JLabel flagTitle;
	
	// CUSTOM PICKS PANEL (for custom wizards)
	
	/** Panel key */
	private final static String PICKS_PANEL = "Picks";
	
	/** Panel title */
	private JLabel picksTitle;
	
	// FREE SPELL SELECTION PANEL
	
	/** Panel key */
	private final static String FREE_SPELLS_PANEL = "Free";
	
	/** Panel title */
	private JLabel freeSpellsTitle;
	
	// RACE SELECTION PANEL
	
	/** Panel key */
	private final static String RACE_PANEL = "Race";
	
	/** Panel title */
	private JLabel raceTitle;
	
	// WAITING TO OTHER PLAYERS TO JOIN PANEL
	
	/** Panel key */
	private final static String WAIT_PANEL = "Wait";
	
	/** Panel title */
	private JLabel waitTitle;
	
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
		final BufferedImage midButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button135x17Normal.png");
		final BufferedImage midButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button135x17Pressed.png");
		final BufferedImage wideButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button290x17Normal.png");
		final BufferedImage wideButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button290x17Pressed.png");
		final BufferedImage checkboxUnticked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Unticked.png");
		final BufferedImage checkboxTicked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Ticked.png");
		final BufferedImage editbox = getUtils ().loadImage ("/momime.client.graphics/ui/editBoxes/editBox125x23.png");

		buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Normal.png");
		buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Pressed.png");
		buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Disabled.png");
		
		// Actions
		cancelAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 3392213175312761329L;

			@Override
			public void actionPerformed (final ActionEvent e)
			{
				getFrame ().setVisible (false);
			}
		};

		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -6935146629512835133L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				// What this does depends on which 'card' is currently displayed
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (getClient ().getOurPlayerID ());
				pd.setPlayerName (getClient ().getOurPlayerName ());
				
				final NewSession msg = new NewSession ();
				msg.setSessionDescription (buildSessionDescription ());
				msg.setPlayerDescription (pd);
				
				try
				{
					getClient ().getServerConnection ().sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
			}
		};
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
		
		// Do this "too early" on purpose, so that the window isn't centred over the main menu, but is a little down-right of it
		getFrame ().setLocationRelativeTo (getMainMenuUI ().getFrame ());

		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = 4885116900695807364L;

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
		
		// Set up main layout
 		// This includes the left half of the screen, and the slice under the right half containing the OK and Cancel buttons
 		// to save all the individual "cards" having their own OK and Cancel buttons, so is 3x3
		contentPane.setLayout (new GridBagLayout ());
		
		// Cut off left half of the window
		final GridBagConstraints lhsConstraints = getUtils ().createConstraints (0, 0, 1, INSET, GridBagConstraints.CENTER);
		lhsConstraints.gridheight = 3;
		
		contentPane.add (Box.createRigidArea (new Dimension (335, 0)), lhsConstraints);
		
		final GridBagConstraints rhsConstraints = getUtils ().createConstraints (1, 0, 2, INSET, GridBagConstraints.CENTER);
		rhsConstraints.weightx = 1;
		rhsConstraints.weighty = 1;
		rhsConstraints.fill = GridBagConstraints.BOTH;
		
		cardLayout = new CardLayout ();
		
		cards = new JPanel (cardLayout);
		cards.setOpaque (false);
		contentPane.add (cards, rhsConstraints);
		
		// Divider, OK and Cancel buttons
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, 1, 2, INSET, GridBagConstraints.CENTER));
		
		final GridBagConstraints constraints2 = getUtils ().createConstraints (1, 2, 1, INSET, GridBagConstraints.EAST);
		constraints2.weightx = 1;		// Move the OK button as far to the right as possible
		contentPane.add (getUtils ().createImageButton (okAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), constraints2);
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), getUtils ().createConstraints (2, 2, 1, INSET, GridBagConstraints.EAST));
		
		// NEW GAME PANEL
		// This is easy with 2 columns
		changeDatabaseAction = new CycleAction<AvailableDatabase> ()
		{
			private static final long serialVersionUID = 8242544558930278937L;

			/**
			 * Update available choices of all the buttons when a database is chosen
			 */
			@Override
			protected final void selectedItemChanged ()
			{
				selectedDatabaseOrLanguageChanged ();
			}
		};

		changeHumanOpponentsAction = new CycleAction<Integer> ()
		{
			private static final long serialVersionUID = 6976686450640981530L;

			/**
			 * Enable/disable OK button when number of oppponents changes
			 */
			@Override
			protected final void selectedItemChanged ()
			{
				enableOrDisableOkButton ();
			}
		};
		
		changeAIOpponentsAction = new CycleAction<Integer> ()
		{
			private static final long serialVersionUID = -800824274156080772L;

			/**
			 * Enable/disable OK button when number of oppponents changes
			 */
			@Override
			protected final void selectedItemChanged ()
			{
				enableOrDisableOkButton ();
			}
		};
		
		changeMapSizeAction = new CycleAction<MapSize> ();
		changeLandProportionAction = new CycleAction<LandProportion> ();
		changeNodeStrengthAction = new CycleAction<NodeStrength> ();
		changeDifficultyLevelAction = new CycleAction<DifficultyLevel> ();
		changeTurnSystemAction = new CycleAction<TurnSystem> ();
		changeFogOfWarSettingsAction = new CycleAction<FogOfWarSetting> ();
		changeUnitSettingsAction = new CycleAction<UnitSetting> ();
		changeSpellSettingsAction = new CycleAction<SpellSetting> ();
		changeDebugOptionsAction = new CycleAction<Boolean> ();
		
		final JPanel newGamePanel = new JPanel ();
		newGamePanel.setOpaque (false);
		newGamePanel.setLayout (new GridBagLayout ());
		
		newGameTitle = getUtils ().createLabel (MomUIUtils.GOLD, getLargeFont ());
		newGamePanel.add (newGameTitle, getUtils ().createConstraints (0, 0, 2, INSET, GridBagConstraints.CENTER));
		
		newGamePanel.add (getUtils ().createImage (divider), getUtils ().createConstraints (0, 1, 2, INSET, GridBagConstraints.CENTER));
		
		newGamePanel.add (getUtils ().createImageButton (changeDatabaseAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			wideButtonNormal, wideButtonPressed, wideButtonNormal), getUtils ().createConstraints (0, 2, 2, INSET, GridBagConstraints.CENTER));

		newGamePanel.add (getUtils ().createImage (divider), getUtils ().createConstraints (0, 3, 2, INSET, GridBagConstraints.CENTER));

		humanOpponentsLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (humanOpponentsLabel, getUtils ().createConstraints (0, 4, 1, INSET, GridBagConstraints.WEST));

		newGamePanel.add (getUtils ().createImageButton (changeHumanOpponentsAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 4, 1, INSET, GridBagConstraints.EAST));
		
		aiOpponentsLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (aiOpponentsLabel, getUtils ().createConstraints (0, 5, 1, INSET, GridBagConstraints.WEST));

		newGamePanel.add (getUtils ().createImageButton (changeAIOpponentsAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 5, 1, INSET, GridBagConstraints.EAST));
		
		mapSizeLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (mapSizeLabel, getUtils ().createConstraints (0, 6, 1, INSET, GridBagConstraints.WEST));

		newGamePanel.add (getUtils ().createImageButton (changeMapSizeAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 6, 1, INSET, GridBagConstraints.EAST));
		
		landProportionLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (landProportionLabel, getUtils ().createConstraints (0, 7, 1, INSET, GridBagConstraints.WEST));

		newGamePanel.add (getUtils ().createImageButton (changeLandProportionAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 7, 1, INSET, GridBagConstraints.EAST));
		
		nodesLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (nodesLabel, getUtils ().createConstraints (0, 8, 1, INSET, GridBagConstraints.WEST));
		
		newGamePanel.add (getUtils ().createImageButton (changeNodeStrengthAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 8, 1, INSET, GridBagConstraints.EAST));
		
		difficultyLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (difficultyLabel, getUtils ().createConstraints (0, 9, 1, INSET, GridBagConstraints.WEST));
		
		newGamePanel.add (getUtils ().createImageButton (changeDifficultyLevelAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 9, 1, INSET, GridBagConstraints.EAST));
		
		turnSystemLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (turnSystemLabel, getUtils ().createConstraints (0, 10, 1, INSET, GridBagConstraints.WEST));
		
		newGamePanel.add (getUtils ().createImageButton (changeTurnSystemAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 10, 1, INSET, GridBagConstraints.EAST));
		
		fogOfWarLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (fogOfWarLabel, getUtils ().createConstraints (0, 11, 1, INSET, GridBagConstraints.WEST));

		newGamePanel.add (getUtils ().createImageButton (changeFogOfWarSettingsAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 11, 1, INSET, GridBagConstraints.EAST));
		
		unitSettingsLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (unitSettingsLabel, getUtils ().createConstraints (0, 12, 1, INSET, GridBagConstraints.WEST));

		newGamePanel.add (getUtils ().createImageButton (changeUnitSettingsAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 12, 1, INSET, GridBagConstraints.EAST));
		
		spellSettingsLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (spellSettingsLabel, getUtils ().createConstraints (0, 13, 1, INSET, GridBagConstraints.WEST));
		
		newGamePanel.add (getUtils ().createImageButton (changeSpellSettingsAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 13, 1, INSET, GridBagConstraints.EAST));
		
		debugOptionsLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (debugOptionsLabel, getUtils ().createConstraints (0, 14, 1, INSET, GridBagConstraints.WEST));
		
		newGamePanel.add (getUtils ().createImageButton (changeDebugOptionsAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), getUtils ().createConstraints (1, 14, 1, INSET, GridBagConstraints.EAST));
		
		final GridBagConstraints newGameSpace = getUtils ().createConstraints (0, 15, 2, INSET, GridBagConstraints.CENTER);
		newGameSpace.weightx = 1;
		newGameSpace.weighty = 1;
		newGamePanel.add (Box.createGlue (), newGameSpace);
		
		newGamePanel.add (getUtils ().createImage (divider), getUtils ().createConstraints (0, 16, 2, INSET, GridBagConstraints.CENTER));

		gameNameLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		newGamePanel.add (gameNameLabel, getUtils ().createConstraints (0, 17, 1, INSET, GridBagConstraints.WEST));
		
		gameName = getUtils ().createTextFieldWithBackgroundImage (MomUIUtils.SILVER, getMediumFont (), editbox);
		newGamePanel.add (gameName, getUtils ().createConstraints (1, 17, 1, INSET, GridBagConstraints.EAST));
		
		cards.add (newGamePanel, NEW_GAME_PANEL);
		
		// CUSTOM MAP SIZE PANEL
		// CUSTOM LAND PROPORTION PANEL
		// CUSTOM NODES PANEL
		// CUSTOM DIFFICULTY PANEL (1 of 2)
		// CUSTOM DIFFICULTY PANEL (2 of 2)
		// CUSTOM NODES-DIFFICULTY PANEL
		// CUSTOM FOG OF WAR PANEL
		// CUSTOM UNIT SETTINGS PANEL
		// CUSTOM SPELL SETTINGS PANEL
		// DEBUG OPTIONS PANEL
		// JOIN GAME PANEL
		
		// WIZARD SELECTION PANEL
		wizardPanel = new JPanel ();
		wizardPanel.setOpaque (false);
		wizardPanel.setLayout (new GridBagLayout ());
		
		wizardTitle = getUtils ().createLabel (MomUIUtils.GOLD, getLargeFont ());
		wizardPanel.add (wizardTitle, getUtils ().createConstraints (0, 0, 2, INSET, GridBagConstraints.CENTER));
		
		wizardPanel.add (getUtils ().createImage (divider), getUtils ().createConstraints (0, 1, 2, INSET, GridBagConstraints.CENTER));
		
		cards.add (wizardPanel, WIZARD_PANEL);

		// PORTRAIT SELECTION PANEL (for custom wizards)
		// FLAG COLOUR PANEL (for custom wizards with custom portraits)
		// CUSTOM PICKS PANEL (for custom wizards)
		// FREE SPELL SELECTION PANEL
		// RACE SELECTION PANEL
		// WAITING TO OTHER PLAYERS TO JOIN PANEL

		// Load the database list last, because it causes the first database to be selected and hence loads all the other buttons with values
		for (final AvailableDatabase db : getClient ().getNewGameDatabase ().getMomimeXmlDatabase ())
			changeDatabaseAction.addItem (db, db.getDbName ());
		
		// Ok button should only be enabled once we have enough info
		final DocumentListener documentListener = new DocumentListener ()
		{
			@Override
			public final void insertUpdate (final DocumentEvent e)
			{
				enableOrDisableOkButton ();
			}

			@Override
			public final void removeUpdate (final DocumentEvent e)
			{
				enableOrDisableOkButton ();
			}

			@Override
			public final void changedUpdate (final DocumentEvent e)
			{
				enableOrDisableOkButton ();
			}
		};
		
		gameName.getDocument ().addDocumentListener (documentListener);
		
		// Add these last, because they trigger enableOrDisableOkButton
		for (int playerCount = 0; playerCount <= 13; playerCount++)
		{
			changeHumanOpponentsAction.addItem (playerCount, new Integer (playerCount).toString ());
			changeAIOpponentsAction.addItem (playerCount, new Integer (playerCount).toString ());
		}
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setResizable (false);
	}
	
	/**
	 * Show new game card, if they cancelled and then took option again
	 */
	public final void showNewGamePanel ()
	{
		cardLayout.show (cards, NEW_GAME_PANEL);
	}

	/**
	 * Ok button should only be enabled once we have enough info
	 */
	private final void enableOrDisableOkButton ()
	{
		// This gets triggered during startup before both actions have been created
		final int totalOpponents = ((changeHumanOpponentsAction.getSelectedItem () == null) || (changeAIOpponentsAction.getSelectedItem () == null)) ? 0 :
			changeHumanOpponentsAction.getSelectedItem () + changeAIOpponentsAction.getSelectedItem ();
		
		okAction.setEnabled ((totalOpponents >= 1) && (totalOpponents <= 13) && (!gameName.getText ().trim ().equals ("")));
	}
	
	/**
	 * After we join a session, server sends us the database so then we know all things like
	 * all the wizards and retorts available, so can set up the controls for those
	 */
	public final void afterJoinedSession ()
	{
		// Remove any old buttons leftover from a previous joining game
		for (final Component oldComponent : wizardComponents)
			wizardPanel.remove (oldComponent);

		wizardComponents.clear ();
		wizardButtonActions.clear ();
		
		// WIZARD SELECTION PANEL
		// First list all the wizards, we need to know up front how many there are so we can arrange the buttons properly
		final List<String> wizardIDs = new ArrayList<String> ();
		for (final Wizard wizard : getClient ().getClientDB ().getWizard ())
			if ((!wizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) &&
				(!wizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)))
				
				wizardIDs.add (wizard.getWizardID ());
		
		// Make space for custom button
		if (getClient ().getSessionDescription ().getDifficultyLevel ().isCustomWizards ())
			wizardIDs.add (null);
		
		// Work out button locations
		// We do entire left column first, then entire right column
		final int colCount = 2;
		final int rowCount = (wizardIDs.size () + colCount - 1) / colCount;
		int wizardNo = 0;
		
		for (int colNo = 0; colNo < colCount; colNo++)
			for (int rowNo = 0; rowNo < rowCount; rowNo++)
				
				// This forumla is to make sure any rows which need an extra button (i.e. because number of wizards did not divide equally into columns) on the right
				if ((rowNo < rowCount - 1) || (wizardNo + (rowCount * (colCount - 1 - colNo)) < wizardIDs.size ()))
				{
					final String wizardID = wizardIDs.get (wizardNo);
					
					final Action wizardButtonAction = new AbstractAction ()
					{
						@Override
						public void actionPerformed (final ActionEvent e)
						{
						}
					};
				
					wizardButtonActions.put (wizardID, wizardButtonAction);

					final JButton wizardButton = getUtils ().createImageButton (wizardButtonAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
						buttonNormal, buttonPressed, buttonDisabled);
					wizardPanel.add (wizardButton, getUtils ().createConstraints (colNo, rowNo+2, 1, INSET,
						(colNo == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST));
					wizardComponents.add (wizardButton);
					
					// Next wizard
					wizardNo++;
				}

		// Put all the space underneath the buttons
		// Have to create two space 'blobs' to make the columns stay even
		for (int colNo = 0; colNo < colCount; colNo++)
		{
			final GridBagConstraints wizardSpace = getUtils ().createConstraints (colNo, rowCount+2, 1, INSET, GridBagConstraints.CENTER);
			wizardSpace.weightx = 0.5;
			wizardSpace.weighty = 1;
			
			final Component glue = Box.createGlue ();
			wizardPanel.add (glue, wizardSpace);
			wizardComponents.add (glue);
		}
		
		// Set all the text
		languageChangedDynamic ();
		
		// Show the page
		cardLayout.show (cards, WIZARD_PANEL);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Overall panel
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmNewGame", "Cancel"));
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmNewGame", "OK"));
		
		// NEW GAME PANEL
		newGameTitle.setText				(getLanguage ().findCategoryEntry ("frmNewGame", "Title"));
		humanOpponentsLabel.setText	(getLanguage ().findCategoryEntry ("frmNewGame", "HumanOpponents"));
		aiOpponentsLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGame", "AIOpponents"));
		mapSizeLabel.setText					(getLanguage ().findCategoryEntry ("frmNewGame", "MapSize"));
		landProportionLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGame", "LandProportion"));
		nodesLabel.setText						(getLanguage ().findCategoryEntry ("frmNewGame", "Nodes"));
		difficultyLabel.setText					(getLanguage ().findCategoryEntry ("frmNewGame", "Difficulty"));
		turnSystemLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGame", "TurnSystem"));
		fogOfWarLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGame", "FogOfWar"));
		unitSettingsLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGame", "UnitSettings"));
		spellSettingsLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGame", "SpellSettings"));
		debugOptionsLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGame", "DebugOptions"));
		gameNameLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGame", "GameName"));
		
		changeTurnSystemAction.clearItems ();
		for (final TurnSystem turnSystem : TurnSystem.values ())
			changeTurnSystemAction.addItem (turnSystem, getLanguage ().findCategoryEntry ("NewGameFormTurnSystems", turnSystem.name ()));
		
		changeDebugOptionsAction.clearItems ();
		for (final boolean debugOptions : new boolean [] {false, true})
			changeDebugOptionsAction.addItem (debugOptions, getLanguage ().findCategoryEntry ("XsdBoolean", new Boolean (debugOptions).toString ().toLowerCase ()));
	
		// CUSTOM MAP SIZE PANEL
		/* mapSizeTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "Title"));
		
		// CUSTOM LAND PROPORTION PANEL
		landProportionTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "Title"));
		
		// CUSTOM NODES PANEL
		nodesTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "Title"));
		
		// CUSTOM DIFFICULTY PANEL (1 of 2)
		difficulty1Title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "Title"));
		
		// CUSTOM DIFFICULTY PANEL (2 of 2)
		difficulty2Title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "Title"));
		
		// CUSTOM NODES-DIFFICULTY PANEL
		nodesDifficultyTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty3", "Title"));	
		
		// CUSTOM FOG OF WAR PANEL
		fogOfWarTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "Title"));
		
		// CUSTOM UNIT SETTINGS PANEL
		unitsTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomUnits", "Title"));

		// CUSTOM SPELL SETTINGS PANEL
		spellsTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "Title"));
		
		// DEBUG OPTIONS PANEL
		debugTitle.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDebug", "Title"));
		
		// JOIN GAME PANEL
		joinGameTitle.setText (getLanguage ().findCategoryEntry ("frmJoinGame", "Title")); */
		
		// WIZARD SELECTION PANEL
		wizardTitle.setText (getLanguage ().findCategoryEntry ("frmChooseWizard", "Title"));
		
		// PORTRAIT SELECTION PANEL (for custom wizards)
		/* portraitTitle.setText (getLanguage ().findCategoryEntry ("frmChoosePortrait", "Title"));
		
		// FLAG COLOUR PANEL (for custom wizards with custom portraits)
		flagTitle.setText (getLanguage ().findCategoryEntry ("frmChooseFlagColour", "Title"));
		
		// CUSTOM PICKS PANEL (for custom wizards)
		picksTitle.setText (getLanguage ().findCategoryEntry ("frmCustomPicks", "Title"));
		
		// FREE SPELL SELECTION PANEL
		freeSpellsTitle.setText (getLanguage ().findCategoryEntry ("frmChooseInitialSpells", "Title"));
		
		// RACE SELECTION PANEL
		raceTitle.setText (getLanguage ().findCategoryEntry ("frmChooseRace", "Title"));
		
		// WAITING TO OTHER PLAYERS TO JOIN PANEL
		waitTitle.setText (getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Title")); */
		
		// Change labels for buttons on the new game form
		if (changeDatabaseAction.getSelectedItem () != null)
			selectedDatabaseOrLanguageChanged ();
		
		// Change all the forms dynamically built after we join a game
		languageChangedDynamic ();
	}
	
	/**
	 * Many of the buttons on the new game form are dependant both on the chosen database and chosen language,
	 * so update them whenever either change
	 */
	private final void selectedDatabaseOrLanguageChanged ()
	{
		changeMapSizeAction.clearItems ();
		for (final MapSize mapSize : changeDatabaseAction.getSelectedItem ().getMapSize ())
			changeMapSizeAction.addItem (mapSize, getLanguage ().findMapSizeDescription (mapSize.getMapSizeID ()));
		
		changeLandProportionAction.clearItems ();
		for (final LandProportion landProportion : changeDatabaseAction.getSelectedItem ().getLandProportion ())
			changeLandProportionAction.addItem (landProportion, getLanguage ().findLandProportionDescription (landProportion.getLandProportionID ()));
		
		changeNodeStrengthAction.clearItems ();
		for (final NodeStrength nodeStrength : changeDatabaseAction.getSelectedItem ().getNodeStrength ())
			changeNodeStrengthAction.addItem (nodeStrength, getLanguage ().findNodeStrengthDescription (nodeStrength.getNodeStrengthID ()));
		
		changeDifficultyLevelAction.clearItems ();
		for (final DifficultyLevel difficultyLevel : changeDatabaseAction.getSelectedItem ().getDifficultyLevel ())
			changeDifficultyLevelAction.addItem (difficultyLevel, getLanguage ().findDifficultyLevelDescription (difficultyLevel.getDifficultyLevelID ()));
		
		changeFogOfWarSettingsAction.clearItems ();
		for (final FogOfWarSetting fowSetting : changeDatabaseAction.getSelectedItem ().getFogOfWarSetting ())
			changeFogOfWarSettingsAction.addItem (fowSetting, getLanguage ().findFogOfWarSettingDescription (fowSetting.getFogOfWarSettingID ()));
		
		changeUnitSettingsAction.clearItems ();
		for (final UnitSetting unitSetting : changeDatabaseAction.getSelectedItem ().getUnitSetting ())
			changeUnitSettingsAction.addItem (unitSetting, getLanguage ().findUnitSettingDescription (unitSetting.getUnitSettingID ()));
		
		changeSpellSettingsAction.clearItems ();
		for (final SpellSetting spellSetting : changeDatabaseAction.getSelectedItem ().getSpellSetting ())
			changeSpellSettingsAction.addItem (spellSetting, getLanguage ().findSpellSettingDescription (spellSetting.getSpellSettingID ()));
		
		// Add "custom" options for all as well
		final String custom = getLanguage ().findCategoryEntry ("frmNewGame", "Custom");
		changeMapSizeAction.addItem					(null, custom);
		changeLandProportionAction.addItem		(null, custom);
		changeNodeStrengthAction.addItem			(null, custom);
		changeDifficultyLevelAction.addItem			(null, custom);
		changeFogOfWarSettingsAction.addItem	(null, custom);
		changeUnitSettingsAction.addItem			(null, custom);
		changeSpellSettingsAction.addItem			(null, custom);
	}
	
	/**
	 * Update all labels and buttons that are only dynamically created after we join a game
	 */
	private final void languageChangedDynamic ()
	{
		for (final Entry<String, Action> wizard : wizardButtonActions.entrySet ())
			if (wizard.getKey () == null)
				wizard.getValue ().putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmChooseWizard", "Custom"));
			else
				wizard.getValue ().putValue (Action.NAME, getLanguage ().findWizardName (wizard.getKey ()));
	}
	
	/**
	 * @return Session description built from all the selected options
	 */
	private final MomSessionDescription buildSessionDescription ()
	{
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Easy fields
		sd.setSessionName (gameName.getText ());
		sd.setXmlDatabaseName (changeDatabaseAction.getSelectedItem ().getDbName ());
		sd.setTurnSystem (changeTurnSystemAction.getSelectedItem ());
		
		// +3 for this player (since we only select nbr of opponents) + raiders + monsters
		sd.setMaxPlayers (changeHumanOpponentsAction.getSelectedItem () + changeAIOpponentsAction.getSelectedItem () + 3);
		sd.setAiPlayerCount (changeAIOpponentsAction.getSelectedItem ());
		
		// Map size
		if (changeMapSizeAction.getSelectedItem () != null)
			sd.setMapSize (changeMapSizeAction.getSelectedItem ());
		else
		{
			final MapSizeData customMapSize = new MapSizeData ();
			sd.setMapSize (customMapSize);
			throw new UnsupportedOperationException ("Custom map size not yet supported");
		}
		
		// Land proportion
		if (changeLandProportionAction.getSelectedItem () != null)
			sd.setLandProportion (changeLandProportionAction.getSelectedItem ());
		else
		{
			final LandProportionData customLandProportion = new LandProportionData ();
			sd.setLandProportion (customLandProportion);
			throw new UnsupportedOperationException ("Custom land proportion not yet supported");
		}
		
		// Node strength
		if (changeNodeStrengthAction.getSelectedItem () != null)
			sd.setNodeStrength (changeNodeStrengthAction.getSelectedItem ());
		else
		{
			final NodeStrengthData customNodeStrength = new NodeStrengthData ();
			sd.setNodeStrength (customNodeStrength);
			throw new UnsupportedOperationException ("Custom node strength not yet supported");
		}
		
		// Difficulty level
		if (changeDifficultyLevelAction.getSelectedItem () != null)
			sd.setDifficultyLevel (changeDifficultyLevelAction.getSelectedItem ());
		else
		{
			final DifficultyLevelData customDifficultyLevel = new DifficultyLevelData ();
			sd.setDifficultyLevel (customDifficultyLevel);
			throw new UnsupportedOperationException ("Custom difficulty level not yet supported");
		}

		// Difficulty level - node strength
		if ((changeDifficultyLevelAction.getSelectedItem () != null) && (changeNodeStrengthAction.getSelectedItem () != null))
		{
			// Note there's multiple entries, one for each plane, so isn't a simple search and exit as soon as we get a match
			final String nodeStrengthID = changeNodeStrengthAction.getSelectedItem ().getNodeStrengthID ();
			for (final DifficultyLevelNodeStrength dlns : changeDifficultyLevelAction.getSelectedItem ().getDifficultyLevelNodeStrength ())
				if (dlns.getNodeStrengthID ().equals (nodeStrengthID))
					sd.getDifficultyLevelNodeStrength ().add (dlns);
		}
		else
		{
			throw new UnsupportedOperationException ("Custom difficulty level-node strength not yet supported");
		}
		
		// FOW setting
		if (changeFogOfWarSettingsAction.getSelectedItem () != null)
			sd.setFogOfWarSetting (changeFogOfWarSettingsAction.getSelectedItem ());
		else
		{
			final FogOfWarSettingData customFogOfWarSetting = new FogOfWarSettingData ();
			sd.setFogOfWarSetting (customFogOfWarSetting);
			throw new UnsupportedOperationException ("Custom fog of war settings not yet supported");
		}
		
		// Unit setting
		if (changeUnitSettingsAction.getSelectedItem () != null)
			sd.setUnitSetting (changeUnitSettingsAction.getSelectedItem ());
		else
		{
			final UnitSettingData customUnitSetting = new UnitSettingData ();
			sd.setUnitSetting (customUnitSetting);
			throw new UnsupportedOperationException ("Custom unit settings not yet supported");
		}
		
		// Spell setting
		if (changeSpellSettingsAction.getSelectedItem () != null)
			sd.setSpellSetting (changeSpellSettingsAction.getSelectedItem ());
		else
		{
			final SpellSettingData customSpellSetting = new SpellSettingData ();
			sd.setSpellSetting (customSpellSetting);
			throw new UnsupportedOperationException ("Custom spell settings not yet supported");
		}
		
		// Debug options
		if (!changeDebugOptionsAction.getSelectedItem ())
		{
			// No debug options, set defaults
			sd.setDisableFogOfWar (false);
		}
		else
		{
			throw new UnsupportedOperationException ("Custom debug options not yet supported");
		}
		
		return sd;
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
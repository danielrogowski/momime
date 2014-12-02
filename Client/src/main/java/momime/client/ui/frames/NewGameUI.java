package momime.client.ui.frames;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import momime.client.MomClient;
import momime.client.database.AvailableDatabase;
import momime.client.database.Wizard;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.BookImage;
import momime.client.language.database.v0_9_5.Pick;
import momime.client.ui.MomUIConstants;
import momime.client.ui.actions.CycleAction;
import momime.client.ui.actions.ToggleAction;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.DifficultyLevelNodeStrength;
import momime.common.database.FogOfWarSetting;
import momime.common.database.LandProportion;
import momime.common.database.MapSize;
import momime.common.database.NodeStrength;
import momime.common.database.Plane;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.database.WizardPick;
import momime.common.database.newgame.DifficultyLevelData;
import momime.common.database.newgame.FogOfWarSettingData;
import momime.common.database.newgame.LandProportionData;
import momime.common.database.newgame.MapSizeData;
import momime.common.database.newgame.NodeStrengthData;
import momime.common.database.newgame.SpellSettingData;
import momime.common.database.newgame.UnitSettingData;
import momime.common.messages.CombatMapSizeData;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.TurnSystem;
import momime.common.messages.clienttoserver.ChooseInitialSpellsMessage;
import momime.common.messages.clienttoserver.ChooseRaceMessage;
import momime.common.messages.clienttoserver.ChooseStandardPhotoMessage;
import momime.common.messages.clienttoserver.ChooseWizardMessage;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowRank;
import momime.common.utils.PlayerPickUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemType;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.NewSession;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Screens for setting up new and joining existing games
 * This is pretty complex because the right hand side is a CardLayout with all the different dialog pages, especially for custom options
 */
public final class NewGameUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewGameUI.class);
	
	/** XML layout of the main form */
	private XmlLayoutContainerEx newGameLayoutMain;

	/** XML layout of the "new game" right hand side */
	private XmlLayoutContainerEx newGameLayoutNew;
	
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Cancel action */
	private Action cancelAction;

	/** OK action */
	private Action okAction;
	
	/** Player name under wizard portrait */
	private JLabel playerName;
	
	/** Card layout for right hand side */
	private CardLayout cardLayout;
	
	/** Panel that the card layout is the layout manager for */
	private JPanel cards;
	
	/** Shelf displaying chosen books */
	private JPanel bookshelf;
	
	/** Currently selected picks (whether from pre-defined wizard or custom) */
	private List<WizardPick> picks = new ArrayList<WizardPick> ();
	
	/** Images added to draw the books on the shelf */
	private List<JLabel> bookImages = new ArrayList<JLabel> ();
	
	/** Currently selected retorts */
	private JTextArea retorts;
	
	/** Wizard portrait */
	private JLabel wizardPortrait;
	
	/** Wizard's colour flag */
	private JLabel flag1;
	
	/** Wizard's colour flag */
	private JLabel flag2;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 3;

	/** Special inset for books */
	private final static int NO_INSET = 0;

	/** 74x21 button */
	private BufferedImage buttonNormal;
	
	/** 74x21 button */
	private BufferedImage buttonPressed;
	
	/** 74x21 button */
	private BufferedImage buttonDisabled;
	
	/** White flag */
	private BufferedImage flag;
	
	/** Title that changes as we change cards */
	private JLabel title;
	
	// NEW GAME PANEL

	/** Panel key */
	private final static String NEW_GAME_PANEL = "New";
	
	/** Panel */
	private JPanel newGamePanel;
	
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
	
	// CUSTOM LAND PROPORTION PANEL

	/** Panel key */
	private final static String LAND_PROPORTION_PANEL = "Land";
	
	// CUSTOM NODES PANEL

	/** Panel key */
	private final static String NODES_PANEL = "Nodes";
	
	// CUSTOM DIFFICULTY PANEL (1 of 2)

	/** Panel key */
	private final static String DIFFICULTY_1_PANEL = "Diff1";
	
	// CUSTOM DIFFICULTY PANEL (2 of 2)
	
	/** Panel key */
	private final static String DIFFICULTY_2_PANEL = "Diff2";
	
	// CUSTOM NODES-DIFFICULTY PANEL
	
	/** Panel key */
	private final static String NODES_DIFFICULTY_PANEL = "NodesDiff";
	
	// CUSTOM FOG OF WAR PANEL
	
	/** Panel key */
	private final static String FOG_OF_WAR_PANEL = "FOW";
	
	// CUSTOM UNIT SETTINGS PANEL
	
	/** Panel key */
	private final static String UNITS_PANEL = "Units";
	
	// CUSTOM SPELL SETTINGS PANEL
	
	/** Panel key */
	private final static String SPELLS_PANEL = "Spell";
	
	// DEBUG OPTIONS PANEL
	
	/** Panel key */
	private final static String DEBUG_PANEL = "Debug";
	
	// JOIN GAME PANEL
	
	/** Panel key */
	private final static String JOIN_GAME_PANEL = "Join";
	
	// WIZARD SELECTION PANEL

	/** Panel key */
	private final static String WIZARD_PANEL = "Wizard";

	/** Panel */
	private JPanel wizardPanel;
	
	/** Dynamically created button actions */
	private Map<String, Action> wizardButtonActions = new HashMap<String, Action> ();

	/** Dynamically created buttons etc */
	private List<Component> wizardComponents = new ArrayList<Component> ();
	
	/** Gets set to true after player clicks a button */
	private boolean isWizardChosen;
	
	/** Gets set to chosen wizard ID when player clicks a button, or null if they click custom */
	private String wizardChosen;
	
	// PORTRAIT SELECTION PANEL (for custom wizards)

	/** Panel key */
	private final static String PORTRAIT_PANEL = "Portrait";

	/** Panel */
	private JPanel portraitPanel;
	
	/** Dynamically created button actions */
	private Map<String, Action> portraitButtonActions = new HashMap<String, Action> ();

	/** Dynamically created buttons etc */
	private List<Component> portraitComponents = new ArrayList<Component> ();
	
	/** Gets set to true after player clicks a button */
	private boolean isPortraitChosen;
	
	/** Gets set to chosen portrait ID when player clicks a button, or null if they click custom */
	private String portraitChosen;
	
	// FLAG COLOUR PANEL (for custom wizards with custom portraits)
	
	/** Panel key */
	private final static String FLAG_PANEL = "Flag";
	
	// CUSTOM PICKS PANEL (for custom wizards)
	
	/** Panel key */
	private final static String PICKS_PANEL = "Picks";
	
	// FREE SPELL SELECTION PANEL
	
	/** Panel key */
	private final static String FREE_SPELLS_PANEL = "Free";
	
	/** Panel */
	private JPanel freeSpellsPanel;
	
	/** Magic realm we're current choosing free spells for */
	private String currentMagicRealmID;
	
	/** Dynamically created rank titles */
	private Map<ChooseInitialSpellsNowRank, JLabel> spellRankTitles = new HashMap<ChooseInitialSpellsNowRank, JLabel> ();
	
	/** Free spell actions */
	private Map<Spell, ToggleAction> freeSpellActions = new HashMap<Spell, ToggleAction> ();

	/** Dynamically created buttons etc */
	private List<Component> freeSpellsComponents = new ArrayList<Component> ();
	
	// RACE SELECTION PANEL
	
	/** Panel key */
	private final static String RACE_PANEL = "Race";

	/** Panel */
	private JPanel racePanel;
	
	/** Dynamically created titles */
	private Map<Integer, JLabel> racePlanes = new HashMap<Integer, JLabel> ();
	
	/** Dynamically created button actions */
	private Map<Race, Action> raceButtonActions = new HashMap<Race, Action> ();

	/** Dynamically created buttons etc */
	private List<Component> raceComponents = new ArrayList<Component> ();
	
	/** Gets set to chosen race ID when player clicks a button */
	private String raceChosen;
	
	// WAITING TO OTHER PLAYERS TO JOIN PANEL
	
	/** Panel key */
	private final static String WAIT_PANEL = "Wait";

	/** Panel */
	private JPanel waitPanel;
	
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
		flag = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/flag.png");

		buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Normal.png");
		buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Pressed.png");
		buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Disabled.png");
		
		// Actions
		cancelAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
				getFrame ().setVisible (false);
			}
		};

		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					// What this does depends on which 'card' is currently displayed
					if (newGamePanel.isVisible ())
					{
						final PlayerDescription pd = new PlayerDescription ();
						pd.setPlayerID (getClient ().getOurPlayerID ());
						pd.setPlayerName (getClient ().getOurPlayerName ());
				
						final NewSession msg = new NewSession ();
						msg.setSessionDescription (buildSessionDescription ());
						msg.setPlayerDescription (pd);
				
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					else if (wizardPanel.isVisible ())
					{
						final ChooseWizardMessage msg = new ChooseWizardMessage ();
						msg.setWizardID (wizardChosen);
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					else if (portraitPanel.isVisible ())
					{
						if (portraitChosen == null)
						{
						}
						else
						{
							final ChooseStandardPhotoMessage msg = new ChooseStandardPhotoMessage ();
							msg.setPhotoID (portraitChosen);
							getClient ().getServerConnection ().sendMessageToServer (msg);
						}						
					}
					else if (freeSpellsPanel.isVisible ())
					{
						final ChooseInitialSpellsMessage msg = new ChooseInitialSpellsMessage ();
						msg.setPickID (currentMagicRealmID);
						
						for (final Entry<Spell, ToggleAction> spell : freeSpellActions.entrySet ())
							if (spell.getValue ().isSelected ())
								msg.getSpell ().add (spell.getKey ().getSpellID ());

						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					else if (racePanel.isVisible ())
					{
						final ChooseRaceMessage msg = new ChooseRaceMessage ();
						msg.setRaceID (raceChosen);
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					
					okAction.setEnabled (false);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Intentionally let the image be blocky (omit KEY_INTERPOLATION), or the borders and such on the
				// background look blurry and we can't line the e.g. wizard portrait up inside them correctly
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
				g.drawImage (background, 0, 0, getWidth (), getHeight (), null);
			}
		};
		contentPane.setBackground (Color.BLACK);
		
		final Dimension fixedSize = new Dimension (640, 480);
 		contentPane.setPreferredSize (fixedSize);
		
		// Set up main layout
		contentPane.setLayout (new XmlLayoutManager (getNewGameLayoutMain ()));
		cardLayout = new CardLayout ()
		{
			/**
			 * Update the title whenever the displayed card changes
			 */
			@Override
			public final void show (final Container parent, final String name)
			{
				super.show (parent, name);
				languageOrCardChanged ();
			}
		};
		
		cards = new JPanel (cardLayout);
		cards.setOpaque (false);
		contentPane.add (cards, "frmNewGameLHSCard");
		
		// Dividers, Title, OK and Cancel buttons (above and below the card layout)
		for (int n = 1; n <= 2; n++)
			contentPane.add (getUtils ().createImage (divider), "frmNewGameLHSBar" + n);

		title = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (title, "frmNewGameLHSTitle");
		
		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmNewGameLHSOK");
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmNewGameLHSCancel");
		
		// Images in left hand side
		wizardPortrait = new JLabel ();
		contentPane.add (wizardPortrait, "frmNewGameLHSPhoto");
		
		flag1 = new JLabel ();
		contentPane.add (flag1, "frmNewGameLHSFlag1");

		flag2 = new JLabel ();
		contentPane.add (flag2, "frmNewGameLHSFlag2");

		playerName = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (playerName, "frmNewGameLHSWizardName");
		
		bookshelf = new JPanel (new GridBagLayout ());
		bookshelf.setOpaque (false);
		
		// Force the books to sit on the bottom of the shelf
		bookshelf.add (Box.createRigidArea (new Dimension (0, getNewGameLayoutMain ().findComponent ("frmNewGameLHSBookshelf").getHeight ())));

		contentPane.add (bookshelf, "frmNewGameLHSBookshelf");
		
		retorts = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (retorts, "frmNewGameLHSRetorts");
		
		// NEW GAME PANEL
		changeDatabaseAction = new CycleAction<AvailableDatabase> ()
		{
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
		
		newGamePanel = new JPanel (new XmlLayoutManager (getNewGameLayoutNew ()));
		newGamePanel.setOpaque (false);
		
		newGamePanel.add (getUtils ().createImageButton (changeDatabaseAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			wideButtonNormal, wideButtonPressed, wideButtonNormal), "frmNewGameDatabaseButton");

		humanOpponentsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (humanOpponentsLabel, "frmNewGameHumanOpponents");

		newGamePanel.add (getUtils ().createImageButton (changeHumanOpponentsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameHumanOpponentsButton");
		
		aiOpponentsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (aiOpponentsLabel, "frmNewGameAIOpponents");

		newGamePanel.add (getUtils ().createImageButton (changeAIOpponentsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameAIOpponentsButton");
		
		mapSizeLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (mapSizeLabel, "frmNewGameMapSize");

		newGamePanel.add (getUtils ().createImageButton (changeMapSizeAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameMapSizeButton");
		
		landProportionLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (landProportionLabel, "frmNewGameLandProportion");

		newGamePanel.add (getUtils ().createImageButton (changeLandProportionAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameLandProportionButton");
		
		nodesLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (nodesLabel, "frmNewGameNodes");
		
		newGamePanel.add (getUtils ().createImageButton (changeNodeStrengthAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameNodesButton");
		
		difficultyLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (difficultyLabel, "frmNewGameDifficulty");
		
		newGamePanel.add (getUtils ().createImageButton (changeDifficultyLevelAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameDifficultyButton");
		
		turnSystemLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (turnSystemLabel, "frmNewGameTurnSystem");
		
		newGamePanel.add (getUtils ().createImageButton (changeTurnSystemAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameTurnSystemButton");
		
		fogOfWarLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (fogOfWarLabel, "frmNewGameFogOfWar");

		newGamePanel.add (getUtils ().createImageButton (changeFogOfWarSettingsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameFogOfWarButton");
		
		unitSettingsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (unitSettingsLabel, "frmNewGameUnitSettings");

		newGamePanel.add (getUtils ().createImageButton (changeUnitSettingsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameUnitButton");
		
		spellSettingsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (spellSettingsLabel, "frmNewGameSpellSettings");
		
		newGamePanel.add (getUtils ().createImageButton (changeSpellSettingsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameSpellButton");
		
		debugOptionsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (debugOptionsLabel, "frmNewGameDebugOptions");
		
		newGamePanel.add (getUtils ().createImageButton (changeDebugOptionsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameDebugButton");
		
		gameNameLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (gameNameLabel, "frmNewGameGameName");
		
		gameName = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getMediumFont (), editbox);
		newGamePanel.add (gameName, "frmNewGameGameNameEdit");

		for (int n = 1; n <= 2; n++)
			newGamePanel.add (getUtils ().createImage (divider), "frmNewGameBar" + n);
		
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
		
		cards.add (wizardPanel, WIZARD_PANEL);

		// PORTRAIT SELECTION PANEL (for custom wizards)
		portraitPanel = new JPanel ();
		portraitPanel.setOpaque (false);
		portraitPanel.setLayout (new GridBagLayout ());
		
		cards.add (portraitPanel, PORTRAIT_PANEL);
		
		// FLAG COLOUR PANEL (for custom wizards with custom portraits)
		// CUSTOM PICKS PANEL (for custom wizards)
		
		// FREE SPELL SELECTION PANEL
		freeSpellsPanel = new JPanel ();
		freeSpellsPanel.setOpaque (false);
		freeSpellsPanel.setLayout (new GridBagLayout ());
		
		cards.add (freeSpellsPanel, FREE_SPELLS_PANEL);
		
		// RACE SELECTION PANEL
		racePanel = new JPanel ();
		racePanel.setOpaque (false);
		racePanel.setLayout (new GridBagLayout ());
		
		cards.add (racePanel, RACE_PANEL);
		
		// WAITING TO OTHER PLAYERS TO JOIN PANEL
		waitPanel = new JPanel ();
		waitPanel.setOpaque (false);
		waitPanel.setLayout (new GridBagLayout ());
		
		cards.add (waitPanel, WAIT_PANEL);

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
		getFrame ().setResizable (false);
	}
	
	/**
	 * Show new game card, if they cancelled and then took option again
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void showNewGamePanel () throws IOException
	{
		playerName.setText (null);
		wizardPortrait.setIcon (null);
		flag1.setIcon (null);
		flag2.setIcon (null);
		retorts.setText (null);
		
		picks.clear ();
		updateBookshelfFromPicks ();
		
		isWizardChosen = false;
		isPortraitChosen = false;
		currentMagicRealmID = null;
		cardLayout.show (cards, NEW_GAME_PANEL);
	}
	
	/**
	 * After we join a session, server sends us the database so then we know all things like
	 * all the wizards and retorts available, so can set up the controls for those
	 */
	public final void afterJoinedSession ()
	{
		playerName.setText (getClient ().getOurPlayerName ());
		
		// Remove any old buttons leftover from a previous joining game
		for (final Component oldComponent : wizardComponents)
			wizardPanel.remove (oldComponent);

		for (final Component oldComponent : portraitComponents)
			portraitPanel.remove (oldComponent);

		for (final Component oldComponent : raceComponents)
			racePanel.remove (oldComponent);
		
		wizardComponents.clear ();
		wizardButtonActions.clear ();
		portraitComponents.clear ();
		portraitButtonActions.clear ();
		raceComponents.clear ();
		racePlanes.clear ();
		raceButtonActions.clear ();
		
		// WIZARD SELECTION PANEL and PORTRAIT SELECTION PANEL (for custom wizards)
		// The two panels use the same button arrangement, so do both at once
		// First list all the wizards, we need to know up front how many there are so we can arrange the buttons properly
		final List<Wizard> wizards = new ArrayList<Wizard> ();
		for (final Wizard wizard : getClient ().getClientDB ().getWizard ())
			if ((!wizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_MONSTERS)) &&
				(!wizard.getWizardID ().equals (CommonDatabaseConstants.WIZARD_ID_RAIDERS)))
				
				wizards.add (wizard);
		
		// Make space for custom button
		if (getClient ().getSessionDescription ().getDifficultyLevel ().isCustomWizards ())
			wizards.add (null);
		
		// Work out button locations
		// We do entire left column first, then entire right column
		final int colCount = 2;
		final int rowCount = (wizards.size () + colCount - 1) / colCount;
		int wizardNo = 0;
		
		for (int colNo = 0; colNo < colCount; colNo++)
			for (int rowNo = 0; rowNo < rowCount; rowNo++)
				
				// This forumla is to make sure any rows which need an extra button (i.e. because number of wizards did not divide equally into columns) on the right
				if ((rowNo < rowCount - 1) || (wizardNo + (rowCount * (colCount - 1 - colNo)) < wizards.size ()))
				{
					final Wizard wizard = wizards.get (wizardNo);
					final String wizardID = (wizard == null) ? null : wizard.getWizardID ();
					
					// Choose wizard button
					final Action wizardButtonAction = new AbstractAction ()
					{
						@Override
						public final void actionPerformed (final ActionEvent ev)
						{
							isWizardChosen = true;
							wizardChosen = wizardID;
							enableOrDisableOkButton ();
							try
							{
								picks.clear ();
								if (wizard == null)
								{
									// Custom wizard - select portait and flag colour in next stages
									wizardPortrait.setIcon (null);
									flag1.setIcon (null);
									flag2.setIcon (null);
								}
								else
								{
									final momime.client.graphics.database.v0_9_5.Wizard portrait = getGraphicsDB ().findWizard (wizard.getWizardID (), "NewGameUI.wizardButtonAction"); 
									wizardPortrait.setIcon (new ImageIcon (getUtils ().loadImage (portrait.getPortraitFile ()).getScaledInstance
										(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH)));
									
									final BufferedImage wizardFlag = getUtils ().multiplyImageByColour (flag, Integer.parseInt (portrait.getFlagColour (), 16));
									flag1.setIcon (new ImageIcon (wizardFlag));
									flag2.setIcon (new ImageIcon (wizardFlag));
									
									picks.addAll (wizard.getWizardPick ());
								}
								updateBookshelfFromPicks ();
								updateRetortsFromPicks ();
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
						}
					};
				
					wizardButtonActions.put (wizardID, wizardButtonAction);

					final JButton wizardButton = getUtils ().createImageButton (wizardButtonAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
						buttonNormal, buttonPressed, buttonDisabled);
					wizardPanel.add (wizardButton, getUtils ().createConstraintsNoFill (colNo, rowNo+2, 1, 1, INSET,
						(colNo == 0) ? GridBagConstraintsNoFill.EAST : GridBagConstraintsNoFill.WEST));
					wizardComponents.add (wizardButton);
					
					// Choose portrait button
					final Action portraitButtonAction = new AbstractAction ()
					{
						@Override
						public final void actionPerformed (final ActionEvent ev)
						{
							isPortraitChosen = true;
							portraitChosen = wizardID;
							enableOrDisableOkButton ();
							try
							{
								if (wizard == null)
								{
									// Custom wizard - ask for filename
									wizardPortrait.setIcon (null);
									flag1.setIcon (null);
									flag2.setIcon (null);
								}
								else
								{
									final momime.client.graphics.database.v0_9_5.Wizard portrait = getGraphicsDB ().findWizard (wizard.getWizardID (), "NewGameUI.portraitButtonAction"); 
									wizardPortrait.setIcon (new ImageIcon (getUtils ().loadImage (portrait.getPortraitFile ()).getScaledInstance
										(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH)));

									final BufferedImage wizardFlag = getUtils ().multiplyImageByColour (flag, Integer.parseInt (portrait.getFlagColour (), 16));
									flag1.setIcon (new ImageIcon (wizardFlag));
									flag2.setIcon (new ImageIcon (wizardFlag));
								}
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
						}
					};

					portraitButtonActions.put (wizardID, portraitButtonAction);

					final JButton portraitButton = getUtils ().createImageButton (portraitButtonAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
						buttonNormal, buttonPressed, buttonDisabled);
					portraitPanel.add (portraitButton, getUtils ().createConstraintsNoFill (colNo, rowNo+2, 1, 1, INSET,
						(colNo == 0) ? GridBagConstraintsNoFill.EAST : GridBagConstraintsNoFill.WEST));
					portraitComponents.add (portraitButton);
					
					// Next wizard
					wizardNo++;
				}

		// Put all the space underneath the buttons
		// Have to create two space 'blobs' to make the columns stay even
		for (int colNo = 0; colNo < colCount; colNo++)
		{
			// Choose wizard panel
			final GridBagConstraints wizardSpace = getUtils ().createConstraintsNoFill (colNo, rowCount+2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE);
			wizardSpace.weightx = 0.5;
			wizardSpace.weighty = 1;
			
			final Component wizardGlue = Box.createGlue ();
			wizardPanel.add (wizardGlue, wizardSpace);
			wizardComponents.add (wizardGlue);
			
			// Choose portrait panel
			final GridBagConstraints portraitSpace = getUtils ().createConstraintsNoFill (colNo, rowCount+2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE);
			portraitSpace.weightx = 0.5;
			portraitSpace.weighty = 1;
			
			final Component portraitGlue = Box.createGlue ();
			portraitPanel.add (portraitGlue, portraitSpace);
			portraitComponents.add (portraitGlue);
		}
		
		// Race buttons - first go through each plane
		int gridy = 2;
		for (final Plane plane : getClient ().getClientDB ().getPlane ())
		{
			final JLabel planeLabel = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
			racePanel.add (planeLabel, getUtils ().createConstraintsNoFill (0, gridy, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
			racePlanes.put (plane.getPlaneNumber (), planeLabel);
			raceComponents.add (planeLabel);
			gridy++;
			
			// Then search for races native to this plane
			for (final Race race : getClient ().getClientDB ().getRace ())
				if (race.getNativePlane () == plane.getPlaneNumber ())
				{
					final Action raceButtonAction = new AbstractAction ()
					{
						@Override
						public final void actionPerformed (final ActionEvent ev)
						{
							raceChosen = race.getRaceID ();
							enableOrDisableOkButton ();
						}
					};
					
					raceButtonActions.put (race, raceButtonAction);
					
					final JButton raceButton =  getUtils ().createImageButton (raceButtonAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
						buttonNormal, buttonPressed, buttonDisabled);
					racePanel.add (raceButton, getUtils ().createConstraintsNoFill (0, gridy, 1, 1, NO_INSET, GridBagConstraintsNoFill.CENTRE));
					
					raceComponents.add (raceButton);
					gridy++;
				}
		}

		// Put all the space underneath the buttons
		final GridBagConstraints raceSpace = getUtils ().createConstraintsNoFill (0, gridy, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE);
		raceSpace.weightx = 1;
		raceSpace.weighty = 1;
			
		final Component raceGlue = Box.createGlue ();
		racePanel.add (raceGlue, raceSpace);
		raceComponents.add (raceGlue);
		
		// Set all the text
		languageChangedAfterInGame ();
		
		// Show the page
		cardLayout.show (cards, WIZARD_PANEL);
	}
	
	/**
	 * Show portrait panel, if custom wizard was chosen
	 */
	public final void showPortraitPanel ()
	{
		cardLayout.show (cards, PORTRAIT_PANEL);
	}
	
	/**
	 * Show panel to select spells to get for free at the start of the game
	 * If we have multiple types of spell books that grant free spells (e.g. 5 life books + 5 nature books) then this will get called twice
	 * 
	 * @param magicRealmID Magic realm ID to choose free spells for
	 * @param spellRanks How many free spells of each rank we get; only ranks that we get free spells for are listed
	 * @throws RecordNotFoundException If the pickID doesn't exist
	 */
	public final void showInitialSpellsPanel (final String magicRealmID, final List<ChooseInitialSpellsNowRank> spellRanks) throws RecordNotFoundException
	{
		log.trace ("Entering showInitialSpellsPanel: " + magicRealmID);
		
		currentMagicRealmID = magicRealmID;
		
		// Remove old ones
		for (final Component oldComponent : freeSpellsComponents)
			freeSpellsPanel.remove (oldComponent);
		
		freeSpellsComponents.clear ();
		freeSpellActions.clear ();
		spellRankTitles.clear ();
		
		// Set the colour of the labels to match the spell book colour
		final Color magicRealmColour = new Color (Integer.parseInt (getGraphicsDB ().findPick (magicRealmID, "showInitialSpellsPanel").getPickBookshelfTitleColour (), 16));
		
		// Start by copying the list of spells that we get for free
		final List<ChooseInitialSpellsNowRank> spellRanksList = new ArrayList<ChooseInitialSpellsNowRank> ();
		
		// Just to scope spellRankIDs
		{
			final List<String> spellRankIDs = new ArrayList<String> ();
			for (final ChooseInitialSpellsNowRank rank : spellRanks)
			{
				spellRanksList.add (rank);
				spellRankIDs.add (rank.getSpellRankID ());
			}
		
			// See if there's any other spell ranks for this magic realm that we don't get free spells for
			// i.e. for any of the 5 magic realms in the default MoM setup, this will give SR01, SR02, SR03, SR04 but not SR05
			for (final Spell spell : getClient ().getClientDB ().getSpell ())
				if ((magicRealmID.equals (spell.getSpellRealm ())) && (!spellRankIDs.contains (spell.getSpellRank ())))
				{
					spellRankIDs.add (spell.getSpellRank ());
					
					final ChooseInitialSpellsNowRank rank = new ChooseInitialSpellsNowRank ();
					rank.setSpellRankID (spell.getSpellRank ());
					spellRanksList.add (rank);
				}
		}
		
		Collections.sort (spellRanksList, new Comparator<ChooseInitialSpellsNowRank> ()
		{
			@Override
			public final int compare (final ChooseInitialSpellsNowRank o1, final ChooseInitialSpellsNowRank o2)
			{
				return o1.getSpellRankID ().compareTo (o2.getSpellRankID ());
			}
		});
		
		// Now create labels for each spell rank
		int gridy = 2;
		final int colCount = 2;
		for (final ChooseInitialSpellsNowRank rank : spellRanksList)
		{
			final JLabel spellRankTitle = getUtils ().createLabel (magicRealmColour, getLargeFont ());
			freeSpellsPanel.add (spellRankTitle, getUtils ().createConstraintsNoFill (0, gridy, 2, 1, INSET, GridBagConstraintsNoFill.CENTRE));
					
			spellRankTitles.put (rank, spellRankTitle);
			freeSpellsComponents.add (spellRankTitle);
			gridy++;
			
			// All the spells at this rank
			final List<Spell> spellsAtThisRank = new ArrayList<Spell> ();
			for (final Spell spell : getClient ().getClientDB ().getSpell ())
				if ((magicRealmID.equals (spell.getSpellRealm ())) && (rank.getSpellRankID ().equals (spell.getSpellRank ())))
					spellsAtThisRank.add (spell);
			
			Collections.sort (spellsAtThisRank, new Comparator<Spell> ()
			{
				@Override
				public final int compare (final Spell o1, final Spell o2)
				{
					return o1.getSpellID ().compareTo (o2.getSpellID ());
				}
			});
			
			final int rowCount = (spellsAtThisRank.size () + colCount - 1) / colCount;
			int spellNo = 0;

			for (int colNo = 0; colNo < colCount; colNo++)
				for (int rowNo = 0; rowNo < rowCount; rowNo++)
					if (spellNo < spellsAtThisRank.size ())
					{
						final Spell spell = spellsAtThisRank.get (spellNo);
						final ToggleAction spellAction = new ToggleAction ()
						{
							@Override
							protected final void selectedChanged ()
							{
								updateInitialSpellsCount ();
								enableOrDisableOkButton ();
							}
						};
						freeSpellActions.put (spell, spellAction);
						
						final JButton spellButton = getUtils ().createTextOnlyButton (spellAction, MomUIConstants.DULL_GOLD, getSmallFont ());
						freeSpellsPanel.add (spellButton, getUtils ().createConstraintsNoFill (colNo, gridy + rowNo, 1, 1, NO_INSET, GridBagConstraintsNoFill.CENTRE));
						freeSpellsComponents.add (spellButton);
						
						spellNo++;
					}
			
			gridy = gridy + rowCount;
		}
		
		// Put any free space at the bottom
		// Have to create two space 'blobs' to make the columns stay even
		for (int colNo = 0; colNo < colCount; colNo++)
		{
			final GridBagConstraints freeSpellsSpace = getUtils ().createConstraintsNoFill (colNo, gridy, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE);
			freeSpellsSpace.weightx = 0.5;
			freeSpellsSpace.weighty = 1;
		
			final Component freeSpellsGlue = Box.createGlue ();
			freeSpellsPanel.add (freeSpellsGlue, freeSpellsSpace);
			freeSpellsComponents.add (freeSpellsGlue);
		}
		
		setCurrentMagicRealmSpellNames ();
		updateInitialSpellsCount ();
		freeSpellsPanel.validate ();
		
		cardLayout.show (cards, FREE_SPELLS_PANEL);

		log.trace ("Exiting showInitialSpellsPanel");
	}

	/**
	 * Show race selection panel
	 * @throws PlayerNotFoundException If we can't find ourPlayerID in the players list
	 * @throws RecordNotFoundException If one of the races referrs to a native plane that can't be found
	 */
	public final void showRacePanel () throws PlayerNotFoundException, RecordNotFoundException
	{
		// Enable/disable Myrran buttons, now we know what picks we chose
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "NewGameUI.showRacePanel");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
		
		for (final Entry<Race, Action> race : raceButtonActions.entrySet ())
		{
			final Plane plane = getClient ().getClientDB ().findPlane (race.getKey ().getNativePlane (), "NewGameUI.showRacePanel");
			
			race.getValue ().setEnabled ((plane.getPrerequisitePickToChooseNativeRace () == null) ? true :
				(getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), plane.getPrerequisitePickToChooseNativeRace ()) > 0));
		}		
		
		// Now can show it
		cardLayout.show (cards, RACE_PANEL);
	}
	
	/**
	 * Shows "wait for other players to join" panel
	 */
	public final void showWaitPanel ()
	{
		cardLayout.show (cards, WAIT_PANEL);
	}

	/**
	 * Ok button should only be enabled once we have enough info
	 */
	private final void enableOrDisableOkButton ()
	{
		// This gets triggered during startup before both actions have been created
		final int totalOpponents = ((changeHumanOpponentsAction.getSelectedItem () == null) || (changeAIOpponentsAction.getSelectedItem () == null)) ? 0 :
			changeHumanOpponentsAction.getSelectedItem () + changeAIOpponentsAction.getSelectedItem ();
		
		okAction.setEnabled
			(((newGamePanel.isVisible ()) && (totalOpponents >= 1) && (totalOpponents <= 13) && (!gameName.getText ().trim ().equals (""))) ||
			((wizardPanel.isVisible ()) && (isWizardChosen)) ||
			((portraitPanel.isVisible ()) && (isPortraitChosen)) ||
			((freeSpellsPanel.isVisible ()) && (isCorrectNumberOfFreeSpellsChosen ())) ||
			((racePanel.isVisible ()) && (raceChosen != null)));
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		// Overall panel
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmNewGame", "Cancel"));
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmNewGame", "OK"));
		
		// NEW GAME PANEL
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
		
		// PORTRAIT SELECTION PANEL (for custom wizards)
		
		// FLAG COLOUR PANEL (for custom wizards with custom portraits)
		/* flagTitle.setText (getLanguage ().findCategoryEntry ("frmChooseFlagColour", "Title"));
		
		// CUSTOM PICKS PANEL (for custom wizards)
		picksTitle.setText (getLanguage ().findCategoryEntry ("frmCustomPicks", "Title")); */
		
		// RACE SELECTION PANEL
		
		// WAITING TO OTHER PLAYERS TO JOIN PANEL
		
		// Set title according to which card is displayed
		languageOrCardChanged ();
		
		// Change labels for buttons on the new game form
		if (changeDatabaseAction.getSelectedItem () != null)
			selectedDatabaseOrLanguageChanged ();
		
		// Change all the forms dynamically built after we join a game
		languageChangedAfterInGame ();
		try
		{
			updateRetortsFromPicks ();
		}
		catch (final RecordNotFoundException e)
		{
			log.error (e, e);
		}
		
		// Choose initial spells title depends on current magic realm
		if (currentMagicRealmID != null)
		{
			setCurrentMagicRealmSpellNames ();
			updateInitialSpellsCount ();
		}

		log.trace ("Exiting languageChanged");
	}

	/**
	 * Updates the title above the card, depending on which card is currently displayed
	 */
	private final void languageOrCardChanged ()
	{
		log.trace ("Entering languageOrCardChanged");
		
		title.setForeground (MomUIConstants.GOLD);
		if (newGamePanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGame", "Title"));
		else if (wizardPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmChooseWizard", "Title"));
		else if (portraitPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmChoosePortrait", "Title"));
		else if (racePanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmChooseRace", "Title"));
		else if (waitPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Title"));
		else if (freeSpellsPanel.isVisible ())
		{
			// "Choose Life Spells" title
			final Pick currentMagicRealm = getLanguage ().findPick (currentMagicRealmID);
			final String magicRealmDescription = (currentMagicRealm == null) ? currentMagicRealmID : currentMagicRealm.getBookshelfDescription ();
			title.setText (getLanguage ().findCategoryEntry ("frmChooseInitialSpells", "Title").replaceAll ("MAGIC_REALM", magicRealmDescription));

			try
			{
				final Color magicRealmColour = new Color (Integer.parseInt (getGraphicsDB ().findPick (currentMagicRealmID, "languageOrCardChanged").getPickBookshelfTitleColour (), 16));
				title.setForeground (magicRealmColour);
			}
			catch (final RecordNotFoundException e)
			{
				log.error (e, e);
			}
		}
		
		getFrame ().setTitle (title.getText ());

		log.trace ("Exiting languageOrCardChanged");
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
	 * When the selected picks change, update the books on the bookshelf
	 * @throws IOException If there is a problem loading any of the book images
	 */
	private final void updateBookshelfFromPicks () throws IOException
	{
		// Remove all the old books
		for (final JLabel oldBook : bookImages)
			bookshelf.remove (oldBook);
		
		bookImages.clear ();
		
		// Generate new images
		int gridx = 1;
		for (final WizardPick pick : picks)
		{
			// Pick must exist in the graphics XML file, but may not have any image(s)
			final List<BookImage> possibleImages = getGraphicsDB ().findPick (pick.getPick (), "NewGameUI.updateBookshelfFromPicks").getBookImage ();
			if (possibleImages.size () > 0)
			{
				// Add images onto bookshelf
				for (int n = 0; n < pick.getQuantity (); n++)
				{
					// Choose random image for the pick
					final JLabel img = getUtils ().createImage (getUtils ().loadImage (possibleImages.get (getRandomUtils ().nextInt (possibleImages.size ())).getBookImageFile ()));
					bookshelf.add (img, getUtils ().createConstraintsNoFill (gridx, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.SOUTH));
					bookImages.add (img);
					gridx++;
				}
			}
		}
		
		// Redrawing only the bookshelf isn't enough, because the new books might be smaller than before so only the smaller so
		// bookshelf.validate only redraws the new smaller area and leaves bits of the old books showing
		contentPane.validate ();
		contentPane.repaint ();
	}

	/**
	 * When the selected picks (or language) change, update the retort descriptions
	 * @throws RecordNotFoundException If one of the picks we have isn't in the graphics XML file
	 */
	private final void updateRetortsFromPicks () throws RecordNotFoundException
	{
		final StringBuffer desc = new StringBuffer ();
		for (final WizardPick pick : picks)
		{
			// Pick must exist in the graphics XML file, but may not have any image(s)
			final List<BookImage> possibleImages = getGraphicsDB ().findPick (pick.getPick (), "NewGameUI.updateRetortsFromPicks").getBookImage ();
			if (possibleImages.size () == 0)
			{
				if (desc.length () > 0)
					desc.append (", ");
				
				if (pick.getQuantity () > 1)
					desc.append (pick.getQuantity () + "x");
				
				final Pick pickDesc = getLanguage ().findPick (pick.getPick ());
				if (pickDesc == null)
					desc.append (pick.getPick ());
				else
					desc.append (pickDesc.getPickDescription ());
			}
		}
		retorts.setText (desc.toString ());
	}	
	
	/**
	 * Update all labels and buttons that are only dynamically created after we join a game
	 */
	private final void languageChangedAfterInGame ()
	{
		// Choose wizard buttons
		for (final Entry<String, Action> wizard : wizardButtonActions.entrySet ())
			if (wizard.getKey () == null)
				wizard.getValue ().putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmChooseWizard", "Custom"));
			else
				wizard.getValue ().putValue (Action.NAME, getLanguage ().findWizardName (wizard.getKey ()));
		
		// Choose portrait buttons
		for (final Entry<String, Action> portrait : portraitButtonActions.entrySet ())
			if (portrait.getKey () == null)
				portrait.getValue ().putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmChoosePortrait", "Custom"));
			else
				portrait.getValue ().putValue (Action.NAME, getLanguage ().findWizardName (portrait.getKey ()));
		
		// Race plane titles
		for (final Entry<Integer, JLabel> planeLabel : racePlanes.entrySet ())
		{
			final momime.client.language.database.v0_9_5.Plane plane = getLanguage ().findPlane (planeLabel.getKey ());
			planeLabel.getValue ().setText ((plane == null) ? planeLabel.getKey ().toString () : plane.getPlaneRacesTitle ());
		}
		
		// Choose race buttons
		for (final Entry<Race, Action> raceAction : raceButtonActions.entrySet ())
		{
			final momime.client.language.database.v0_9_5.Race race = getLanguage ().findRace (raceAction.getKey ().getRaceID ());
			raceAction.getValue ().putValue (Action.NAME, (race == null) ? raceAction.getKey () : race.getRaceName ());
		}
	}
	
	/**
	 * Choose initial spells title depends on current magic realm; sets the title for that and the names of all 40 spells
	 */
	private final void setCurrentMagicRealmSpellNames ()
	{
		// Names of every spell
		for (final Entry<Spell, ToggleAction> spellAction : freeSpellActions.entrySet ())
		{
			final momime.client.language.database.v0_9_5.Spell spell = getLanguage ().findSpell (spellAction.getKey ().getSpellID ());
			spellAction.getValue ().putValue (Action.NAME, (spell == null) ? spellAction.getKey ().getSpellID () : spell.getSpellName ());			
		}
	}
	
	/**
	 * Update spell rank titles that include how many free spells we have left to choose, like Uncommon: 8/10
	 */
	private final void updateInitialSpellsCount ()
	{
		for (final Entry<ChooseInitialSpellsNowRank, JLabel> rank : spellRankTitles.entrySet ())
		{
			// How many spells have been chosen at this rank
			int chosen = 0;
			for (final Entry<Spell, ToggleAction> spell : freeSpellActions.entrySet ())
				if ((spell.getValue ().isSelected ()) && (rank.getKey ().getSpellRankID ().equals (spell.getKey ().getSpellRank ())))
					chosen++;
			
			// Set rank title as 0/10
			rank.getValue ().setText (getLanguage ().findSpellRankDescription (rank.getKey ().getSpellRankID ()) + ": " +
				chosen + "/" + rank.getKey ().getFreeSpellCount ());
			
			// Enable or disable all the buttons
			for (final Entry<Spell, ToggleAction> spell : freeSpellActions.entrySet ())
				if (rank.getKey ().getSpellRankID ().equals (spell.getKey ().getSpellRank ()))
				{
					final boolean enabled;
					
					// If can't choose any free spells ever, then never enable it
					if (rank.getKey ().getFreeSpellCount () == 0)
						enabled = false;
					
					// If we've selected spells, we have to be allowed to unselect them, so we can change our mind
					else if (spell.getValue ().isSelected ())
						enabled = true;
					
					// Otherwise enable it only if we haven't already made all selections
					else
						enabled = (chosen < rank.getKey ().getFreeSpellCount ());
					
					spell.getValue ().setEnabled (enabled);
				}
		}
		
		// Now we've set the state of the actions correctly, colour the buttons to match
		for (final Component comp : freeSpellsComponents)
			if (comp instanceof JButton)
			{
				final JButton spellButton = (JButton) comp;
				final ToggleAction spellAction = (ToggleAction) spellButton.getAction ();
				
				if (spellAction.isEnabled ())
					spellButton.setForeground (spellAction.isSelected () ? MomUIConstants.GOLD : MomUIConstants.DULL_GOLD);
				else
					spellButton.setForeground (MomUIConstants.GRAY);
			}
	}
	
	/**
	 * @return True only if exactly the right number of spells in all ranks have been chosen
	 */
	private final boolean isCorrectNumberOfFreeSpellsChosen ()
	{
		boolean allOk = true;
		final Iterator<ChooseInitialSpellsNowRank> rankIter = spellRankTitles.keySet ().iterator ();
		
		while ((allOk) && (rankIter.hasNext ()))
		{
			final ChooseInitialSpellsNowRank rank = rankIter.next ();
			
			// How many spells have been chosen at this rank
			int chosen = 0;
			for (final Entry<Spell, ToggleAction> spell : freeSpellActions.entrySet ())
				if ((spell.getValue ().isSelected ()) && (rank.getSpellRankID ().equals (spell.getKey ().getSpellRank ())))
					chosen++;
			
			// Is it correct?
			if (chosen != rank.getFreeSpellCount ())
				allOk = false;
		}
		
		return allOk;
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
		
		// Combat map size is fixed, at least for now
		final CombatMapSizeData combatMapSize = new CombatMapSizeData ();
		combatMapSize.setWidth (CommonDatabaseConstants.COMBAT_MAP_WIDTH);
		combatMapSize.setHeight (CommonDatabaseConstants.COMBAT_MAP_HEIGHT);
		combatMapSize.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		combatMapSize.setWrapsLeftToRight (false);
		combatMapSize.setWrapsTopToBottom (false);
		combatMapSize.setZoneWidth (10);
		combatMapSize.setZoneHeight (8);
		
		sd.setCombatMapSize (combatMapSize);
		
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
		{
			// Recreate the difficulty level, otherwise it gets sent as the DifficultyLevel subtype (rather than DifficultyLevelData) including all the unncessary DifficultyLevelNodeStrengths
			final DifficultyLevelData src = changeDifficultyLevelAction.getSelectedItem ();
			final DifficultyLevelData dest = new DifficultyLevelData ();
			
		    dest.setHumanSpellPicks (src.getHumanSpellPicks ());
		    dest.setAiSpellPicks (src.getAiSpellPicks ());
		    dest.setHumanStartingGold (src.getHumanStartingGold ());
		    dest.setAiStartingGold (src.getAiStartingGold ());
		    dest.setCustomWizards (src.isCustomWizards ());
		    dest.setEachWizardOnlyOnce (src.isEachWizardOnlyOnce ());
		    
		    dest.setNormalLairCount (src.getNormalLairCount ());
		    dest.setWeakLairCount (src.getWeakLairCount ());
		    
		    dest.setTowerMonstersMinimum (src.getTowerMonstersMinimum ());
		    dest.setTowerMonstersMaximum (src.getTowerMonstersMaximum ());
		    dest.setTowerTreasureMinimum (src.getTowerTreasureMinimum ());
		    dest.setTowerTreasureMaximum (src.getTowerTreasureMaximum ());
		    
		    dest.setRaiderCityCount (src.getRaiderCityCount ());
		    dest.setRaiderCityStartSizeMin (src.getRaiderCityStartSizeMin ());
		    dest.setRaiderCityStartSizeMax (src.getRaiderCityStartSizeMax ());
		    dest.setRaiderCityGrowthCap (src.getRaiderCityGrowthCap ());
		    
		    dest.setWizardCityStartSize (src.getWizardCityStartSize ());
		    dest.setCityMaxSize (src.getCityMaxSize ());
		    dest.getDifficultyLevelPlane ().addAll (src.getDifficultyLevelPlane ());
			
			sd.setDifficultyLevel (dest);
		}
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
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
	
	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
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
	 * @return XML layout of the main form
	 */
	public final XmlLayoutContainerEx getNewGameLayoutMain ()
	{
		return newGameLayoutMain;
	}

	/**
	 * @param layout XML layout of the main form
	 */
	public final void setNewGameLayoutMain (final XmlLayoutContainerEx layout)
	{
		newGameLayoutMain = layout;
	}
	
	/**
	 * @return XML layout of the "new game" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutNew ()
	{
		return newGameLayoutNew;
	}

	/**
	 * @param layout XML layout of the "new game" right hand side
	 */
	public final void setNewGameLayoutNew (final XmlLayoutContainerEx layout)
	{
		newGameLayoutNew = layout;
	}
}
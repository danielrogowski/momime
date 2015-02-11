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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.client.MomClient;
import momime.client.database.AvailableDatabase;
import momime.client.database.Wizard;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.PickGfx;
import momime.client.graphics.database.WizardGfx;
import momime.client.language.database.PickLang;
import momime.client.language.database.PlaneLang;
import momime.client.language.database.RaceLang;
import momime.client.language.database.SpellLang;
import momime.client.ui.MomUIConstants;
import momime.client.ui.actions.CycleAction;
import momime.client.ui.actions.ToggleAction;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.utils.PlayerPickClientUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DifficultyLevel;
import momime.common.database.DifficultyLevelNodeStrength;
import momime.common.database.FogOfWarSetting;
import momime.common.database.LandProportion;
import momime.common.database.MapSize;
import momime.common.database.NewGameDefaults;
import momime.common.database.NodeStrength;
import momime.common.database.Pick;
import momime.common.database.Plane;
import momime.common.database.Race;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.database.WizardPick;
import momime.common.database.newgame.CastingReductionCombination;
import momime.common.database.newgame.DifficultyLevelData;
import momime.common.database.newgame.DifficultyLevelNodeStrengthData;
import momime.common.database.newgame.DifficultyLevelPlane;
import momime.common.database.newgame.FogOfWarSettingData;
import momime.common.database.newgame.FogOfWarValue;
import momime.common.database.newgame.LandProportionData;
import momime.common.database.newgame.LandProportionPlane;
import momime.common.database.newgame.LandProportionTileType;
import momime.common.database.newgame.MapSizeData;
import momime.common.database.newgame.MapSizePlane;
import momime.common.database.newgame.NodeStrengthData;
import momime.common.database.newgame.NodeStrengthPlane;
import momime.common.database.newgame.SpellSettingData;
import momime.common.database.newgame.SwitchResearch;
import momime.common.database.newgame.UnitSettingData;
import momime.common.messages.CombatMapSizeData;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.messages.TurnSystem;
import momime.common.messages.clienttoserver.ChooseCustomFlagColourMessage;
import momime.common.messages.clienttoserver.ChooseCustomPicksMessage;
import momime.common.messages.clienttoserver.ChooseInitialSpellsMessage;
import momime.common.messages.clienttoserver.ChooseRaceMessage;
import momime.common.messages.clienttoserver.ChooseStandardPhotoMessage;
import momime.common.messages.clienttoserver.ChooseWizardMessage;
import momime.common.messages.clienttoserver.UploadCustomPhotoMessage;
import momime.common.messages.servertoclient.ChooseInitialSpellsNowRank;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.PlayerPickUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemType;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.JoinSession;
import com.ndg.multiplayer.sessionbase.NewSession;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.RequestSessionList;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.random.RandomUtils;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.filefilters.ExtensionFileFilter;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;
import com.ndg.utils.StreamUtils;

/**
 * Screens for setting up new and joining existing games
 * This is pretty complex because the right hand side is a CardLayout with all the different dialog pages, especially for custom options
 */
public final class NewGameUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewGameUI.class);

	/** White with some alpha to make the bar brighten the background colour */
	private static final Color SLIDER_BAR_COLOUR = new Color (255, 255, 255, 80);
	
	/** XML layout of the main form */
	private XmlLayoutContainerEx newGameLayoutMain;

	/** XML layout of the "new game" right hand side */
	private XmlLayoutContainerEx newGameLayoutNew;
	
	/** XML layout of the "custom map size" right hand side */
	private XmlLayoutContainerEx newGameLayoutMapSize;
	
	/** XML layout of the "custom land proportion" right hand side */
	private XmlLayoutContainerEx newGameLayoutLandProportion;
	
	/** XML layout of the "custom nodes" right hand side */
	private XmlLayoutContainerEx newGameLayoutNodes;

	/** XML layout of the "custom difficulty 1" right hand side */
	private XmlLayoutContainerEx newGameLayoutDifficulty1;
	
	/** XML layout of the "custom difficulty 2" right hand side */
	private XmlLayoutContainerEx newGameLayoutDifficulty2;
	
	/** XML layout of the "custom difficulty 3" right hand side */
	private XmlLayoutContainerEx newGameLayoutDifficulty3;
	
	/** XML layout of the "custom fog of war" right hand side */
	private XmlLayoutContainerEx newGameLayoutFogOfWar;
	
	/** XML layout of the "custom unit settings" right hand side */
	private XmlLayoutContainerEx newGameLayoutUnits;
	
	/** XML layout of the "custom spell settings" right hand side */
	private XmlLayoutContainerEx newGameLayoutSpells;
	
	/** XML layout of the "custom debug options" right hand side */
	private XmlLayoutContainerEx newGameLayoutDebug;

	/** XML layout of the "join game" right hand side */
	private XmlLayoutContainerEx newGameLayoutJoin;
	
	/** XML layout of the "custom flag colour" right hand side */
	private XmlLayoutContainerEx newGameLayoutFlagColour;
	
	/** XML layout of the "custom picks" right hand side */
	private XmlLayoutContainerEx newGameLayoutPicks;

	/** XML layout of the "wait for players to join" right hand side */
	private XmlLayoutContainerEx newGameLayoutWait;
	
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

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Help text scroll */
	private HelpUI helpUI;
	
	/** Client-side pick utils */
	private PlayerPickClientUtils playerPickClientUtils;
	
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
	private final List<PlayerPick> picks = new ArrayList<PlayerPick> ();
	
	/** Images added to draw the books on the shelf, this includes all 6 bookshelves (merged, and for each magic realm) */
	private final List<JLabel> bookImages = new ArrayList<JLabel> ();
	
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
	
	/** Shelf where we add and take away books from the 5 magic realms */
	private BufferedImage bookshelfWithGargoyles;
	
	/** Bottom shelf has no space for gargoyles */
	private BufferedImage bookshelfWithoutGargoyles;
	
	/** Add custom pick book button */
	private BufferedImage addBookNormal;
	
	/** Add custom pick book button pressed */
	private BufferedImage addBookPressed;
	
	/** Remove custom pick book button */
	private BufferedImage removeBookNormal;
	
	/** Remove custom pick book button pressed */
	private BufferedImage removeBookPressed;
	
	/** Background for flag colour sliders */
	private BufferedImage flagColourSliderBackground;
	
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
	
	/** Checkbox for customizing map size */
	JCheckBox customizeMapSize;
	
	/** Label for land proportion button */
	private JLabel landProportionLabel;

	/** Action for selecting pre-defined land proportion or custom */
	private CycleAction<LandProportion> changeLandProportionAction;

	/** Checkbox for customizing land proportion */
	JCheckBox customizeLandProportion;
	
	/** Label for nodes button */
	private JLabel nodesLabel;

	/** Action for selecting pre-defined node strength or custom */
	private CycleAction<NodeStrength> changeNodeStrengthAction;
	
	/** Checkbox for customizing nodes */
	JCheckBox customizeNodes;
	
	/** Label for difficulty button */
	private JLabel difficultyLabel;

	/** Action for selecting pre-defined difficulty level or custom */
	private CycleAction<DifficultyLevel> changeDifficultyLevelAction;

	/** Checkbox for customizing difficulty */
	JCheckBox customizeDifficulty;
	
	/** Label for turn system button */
	private JLabel turnSystemLabel;

	/** Action for changing turn system */
	private CycleAction<TurnSystem> changeTurnSystemAction;
	
	/** Label for Fog of War button */
	private JLabel fogOfWarLabel;

	/** Action for selecting pre-defined fog of war settings or custom */
	private CycleAction<FogOfWarSetting> changeFogOfWarSettingsAction;

	/** Checkbox for customizing fog of war settings */
	JCheckBox customizeFogOfWar;
	
	/** Label for unit settings button */
	private JLabel unitSettingsLabel;

	/** Action for selecting pre-defined unit settings or custom */
	private CycleAction<UnitSetting> changeUnitSettingsAction;

	/** Checkbox for customizing unit settings */
	JCheckBox customizeUnits;
	
	/** Label for spell settings button */
	private JLabel spellSettingsLabel;

	/** Action for selecting pre-defined spell settings or custom */
	private CycleAction<SpellSetting> changeSpellSettingsAction;

	/** Checkbox for customizing spell settings */
	JCheckBox customizeSpells;
	
	/** Label for debug options button */
	private JLabel debugOptionsLabel;

	/** Action for toggling debug options */
	CycleAction<Boolean> changeDebugOptionsAction;
	
	/** Label for game name */
	private JLabel gameNameLabel;
	
	/** Game name */
	private JTextField gameName;
	
	/** Customize? heading over the tickboxes */
	private JLabel customizeLabel;
	
	// CUSTOM MAP SIZE PANEL

	/** Panel key */
	private final static String MAP_SIZE_PANEL = "Map";

	/** Panel */
	private JPanel mapSizePanel;
	
	/** Map size prompt, before width x height */
	private JLabel mapSizeEdit;
	
	/** Map width */
	private JTextField mapSizeWidth;
	
	/** Map height */
	private JTextField mapSizeHeight;
	
	/** Map wraps left to right */
	private JCheckBox mapWrapsLeftToRight;
	
	/** Map wraps top to bottom */
	private JCheckBox mapWrapsTopToBottom;
	
	/** Zone size label */
	private JTextArea mapZonesLabel;

	/** Map zone width */
	private JTextField mapZoneWidth;
	
	/** Map zone height */
	private JTextField mapZoneHeight;
	
	/** Towers of wizardry label */
	private JLabel towersOfWizardryLabel;
	
	/** Towers of wizardry */
	private JTextField towersOfWizardryCount;
	
	/** Towers of wizardry separation label */
	private JLabel towersOfWizardrySeparationLabel;
	
	/** Towers of wizardry separation */
	private JTextField towersOfWizardrySeparation;
	
	/** Continental race chance label */
	private JTextArea continentalRaceChanceLabel;
	
	/** Continental race chance */
	private JTextField continentalRaceChance;
	
	/** City separation label */
	private JLabel citySeparationLabel;
	
	/** City separation */
	private JTextField citySeparation;

	/** River count */
	private JTextField riverCount;
	
	/** River count label */
	private JLabel riverCountLabel;

	/** Raider city count label */
	private JLabel raiderCityCountLabel;
	
	/** Raider city count */
	private JTextField raiderCityCount;

	/** Arcanus node count */
	private JTextField arcanusNodeCount;

	/** Arcanus node count label */
	private JLabel arcanusNodeCountLabel;
	
	/** Myrror node count */
	private JTextField myrrorNodeCount;

	/** Myrror node count label */
	private JLabel myrrorNodeCountLabel;
	
	// CUSTOM LAND PROPORTION PANEL

	/** Panel key */
	private final static String LAND_PROPORTION_PANEL = "Land";

	/** Panel */
	private JPanel landProportionPanel;
	
	/** % of map which is land */
	private JTextField landPercentage;

	/** % of map which is land label */
	private JLabel landPercentageLabel;
	
	/** % of land which is hills */
	private JTextField hillsPercentage;

	/** % of land which is hills label */
	private JLabel hillsPercentageLabel;
	
	/** % of hills which are mountains */
	private JTextField mountainsPercentage;

	/** % of hills which are mountains label */
	private JLabel mountainsPercentageLabel;
	
	/** % of land which is trees */
	private JTextField treesPercentage;

	/** % of land which is trees label */
	private JLabel treesPercentageLabel;
	
	/** Size of tree areas */
	private JTextField treeAreaSize;
	
	/** Size of tree areas prefix */
	private JLabel treeAreaSizePrefix;

	/** Size of tree areas suffix */
	private JLabel treeAreaSizeSuffix;

	/** % of land which is deserts */
	private JTextField desertsPercentage;

	/** % of land which is deserts label */
	private JLabel desertsPercentageLabel;
	
	/** Size of desert areas */
	private JTextField desertAreaSize;
	
	/** Size of desert areas prefix */
	private JLabel desertAreaSizePrefix;

	/** Size of desert areas suffix */
	private JLabel desertAreaSizeSuffix;
	
	/** % of land which is swamps */
	private JTextField swampsPercentage;

	/** % of land which is swamps label */
	private JLabel swampsPercentageLabel;
	
	/** Size of swamp areas */
	private JTextField swampAreaSize;
	
	/** Size of swamp areas prefix */
	private JLabel swampAreaSizePrefix;

	/** Size of swamp areas suffix */
	private JLabel swampAreaSizeSuffix;
	
	/** Tundra edge distance */
	private JTextField tundraDistance;
	
	/** Tundra edge distance prefix */
	private JLabel tundraDistancePrefix;
	
	/** Tundra edge distance suffix */
	private JLabel tundraDistanceSuffix;
	
	/** Arcanus mineral chance */
	private JTextField arcanusMineralChance;
	
	/** Arcanus mineral chance prefix */
	private JLabel arcanusMineralChancePrefix;
	
	/** Arcanus mineral chance suffix */
	private JLabel arcanusMineralChanceSuffix;
	
	/** Myrror mineral chance */
	private JTextField myrrorMineralChance;

	/** Myrror mineral chance prefix */
	private JLabel myrrorMineralChancePrefix;

	/** Myrror mineral chance suffix */
	private JLabel myrrorMineralChanceSuffix;

	// CUSTOM NODES PANEL

	/** Panel key */
	private final static String NODES_PANEL = "Nodes";

	/** Panel */
	private JPanel nodesPanel;
	
	/** 2x magic power from each cell of node aura */
	private JTextField doubleNodeAuraMagicPower;

	/** 2x magic power from each cell of node aura prefix */
	private JLabel doubleNodeAuraMagicPowerPrefix;

	/** 2x magic power from each cell of node aura suffix */
	private JLabel doubleNodeAuraMagicPowerSuffix;
	
	/** Minimum nbr of cells of nodes on Arcanus */
	private JTextField arcanusNodeSizeMin;

	/** Maximum nbr of cells of nodes on Arcanus */
	private JTextField arcanusNodeSizeMax;

	/** Nbr of cells of nodes on Arcanus prefix */
	private JLabel arcanusNodeSizePrefix;
	
	/** Nbr of cells of nodes on Arcanus suffix */
	private JLabel arcanusNodeSizeSuffix;

	/** Minimum nbr of cells of nodes on Myrror */
	private JTextField myrrorNodeSizeMin;

	/** Maximum nbr of cells of nodes on Myrror */
	private JTextField myrrorNodeSizeMax;
	
	/** Nbr of cells of nodes on Myrror prefix */
	private JLabel myrrorNodeSizePrefix;

	/** Nbr of cells of nodes on Myrror suffix */
	private JLabel myrrorNodeSizeSuffix;
	
	// CUSTOM DIFFICULTY PANEL (1 of 3)

	/** Panel key */
	private final static String DIFFICULTY_1_PANEL = "Diff1";

	/** Panel */
	private JPanel difficulty1Panel;
	
	/** Spell picks label */
	private JLabel spellPicksLabel;
	
	/** Human spell picks label */
	private JLabel humanSpellPicksLabel;

	/** Human spell picks */
	private JTextField humanSpellPicks;

	/** AI spell picks label */
	private JLabel aiSpellPicksLabel;

	/** AI spell picks */
	private JTextField aiSpellPicks;

	/** Starting gold label */
	private JLabel startingGoldLabel;
	
	/** Human starting gold label */
	private JLabel humanStartingGoldLabel;

	/** Human starting gold */
	private JTextField humanStartingGold;

	/** AI starting gold label */
	private JLabel aiStartingGoldLabel;

	/** AI starting gold */
	private JTextField aiStartingGold;
	
	/** Allow custom wizards? */
	private JCheckBox allowCustomWizards;
	
	/** Each wizard can be chosen only once? */
	private JCheckBox eachWizardOnlyOnce;
	
	/** Wizard city start size label */
	private JLabel wizardCityStartSizeLabel;
	
	/** Wizard city start size */
	private JTextField wizardCityStartSize;
	
	/** Maximum city size label */
	private JLabel maxCitySizeLabel;
	
	/** Maximum city size */
	private JTextField maxCitySize;
	
	/** Raider city start size label */
	private JLabel raiderCityStartSizeLabel;

	/** Raider city start size minimum */
	private JTextField raiderCityStartSizeMin;
	
	/** Raider city start size "and" */
	private JLabel raiderCityStartSizeAnd;
	
	/** Raider city start size maximum */
	private JTextField raiderCityStartSizeMax;
	
	/** Raider city size cap prefix */
	private JLabel raiderCitySizeCapPrefix;

	/** Raider city size cap */
	private JTextField raiderCitySizeCap;

	/** Raider city size cap suffix */
	private JLabel raiderCitySizeCapSuffix;
	
	// CUSTOM DIFFICULTY PANEL (2 of 3)
	
	/** Panel key */
	private final static String DIFFICULTY_2_PANEL = "Diff2";

	/** Panel */
	private JPanel difficulty2Panel;
	
	/** Strength of monsters in towers */
	private JLabel towersMonsters;

	/** Minimum strength of monsters in towers */
	private JTextField towersMonstersMin;
	
	/** Maximum strength of monsters in towers */
	private JTextField towersMonstersMax;
	
	/** Value of treasure in towers */
	private JLabel towersTreasure;

	/** Minimum value of treasure in towers */
	private JTextField towersTreasureMin;
	
	/** Maximum value of treasure in towers */
	private JTextField towersTreasureMax;
	
	/** Number of normal lairs label */
	private JLabel normalLairCountLabel;

	/** Number of normal lairs */
	private JTextField normalLairCount;

	/** Strength of monsters in normal lairs on Arcanus */
	private JLabel arcanusNormalLairMonsters;

	/** Minimum strength of monsters in normal lairs on Arcanus */
	private JTextField arcanusNormalLairMonstersMin;
	
	/** Maximum strength of monsters in normal lairs on Arcanus */
	private JTextField arcanusNormalLairMonstersMax;
	
	/** Value of treasure in normal lairs on Arcanus */
	private JLabel arcanusNormalLairTreasure;

	/** Minimum value of treasure in normal lairs on Arcanus */
	private JTextField arcanusNormalLairTreasureMin;
	
	/** Maximum value of treasure in normal lairs on Arcanus */
	private JTextField arcanusNormalLairTreasureMax;
	
	/** Strength of monsters in normal lairs on Myrror */
	private JLabel myrrorNormalLairMonsters;

	/** Minimum strength of monsters in normal lairs on Myrror */
	private JTextField myrrorNormalLairMonstersMin;
	
	/** Maximum strength of monsters in normal lairs on Myrror */
	private JTextField myrrorNormalLairMonstersMax;
	
	/** Value of treasure in normal lairs on Myrror */
	private JLabel myrrorNormalLairTreasure;

	/** Minimum value of treasure in normal lairs on Myrror */
	private JTextField myrrorNormalLairTreasureMin;
	
	/** Maximum value of treasure in normal lairs on Myrror */
	private JTextField myrrorNormalLairTreasureMax;

	/** Number of weak lairs label */
	private JLabel weakLairCountLabel;

	/** Number of weak lairs */
	private JTextField weakLairCount;

	/** Strength of monsters in weak lairs on Arcanus */
	private JLabel arcanusWeakLairMonsters;

	/** Minimum strength of monsters in weak lairs on Arcanus */
	private JTextField arcanusWeakLairMonstersMin;
	
	/** Maximum strength of monsters in weak lairs on Arcanus */
	private JTextField arcanusWeakLairMonstersMax;
	
	/** Value of treasure in weak lairs on Arcanus */
	private JLabel arcanusWeakLairTreasure;

	/** Minimum value of treasure in weak lairs on Arcanus */
	private JTextField arcanusWeakLairTreasureMin;
	
	/** Maximum value of treasure in weak lairs on Arcanus */
	private JTextField arcanusWeakLairTreasureMax;
	
	/** Strength of monsters in weak lairs on Myrror */
	private JLabel myrrorWeakLairMonsters;

	/** Minimum strength of monsters in weak lairs on Myrror */
	private JTextField myrrorWeakLairMonstersMin;
	
	/** Maximum strength of monsters in weak lairs on Myrror */
	private JTextField myrrorWeakLairMonstersMax;
	
	/** Value of treasure in weak lairs on Myrror */
	private JLabel myrrorWeakLairTreasure;

	/** Minimum value of treasure in weak lairs on Myrror */
	private JTextField myrrorWeakLairTreasureMin;
	
	/** Maximum value of treasure in weak lairs on Myrror */
	private JTextField myrrorWeakLairTreasureMax;
	
	// CUSTOM DIFFICULTY PANEL (3 of 3 - nodes/difficulty)
	
	/** Panel key */
	private final static String DIFFICULTY_3_PANEL = "Diff3";
	
	/** Panel */
	private JPanel difficulty3Panel;
	
	/** Strength of monsters in nodes on Arcanus */
	private JLabel arcanusNodeMonsters;

	/** Minimum strength of monsters in nodes on Arcanus */
	private JTextField arcanusNodeMonstersMin;
	
	/** Maximum strength of monsters in nodes on Arcanus */
	private JTextField arcanusNodeMonstersMax;
	
	/** Value of treasure in nodes on Arcanus */
	private JLabel arcanusNodeTreasure;

	/** Minimum value of treasure in nodes on Arcanus */
	private JTextField arcanusNodeTreasureMin;
	
	/** Maximum value of treasure in nodes on Arcanus */
	private JTextField arcanusNodeTreasureMax;
	
	/** Strength of monsters in nodes on Myrror */
	private JLabel myrrorNodeMonsters;

	/** Minimum strength of monsters in nodes on Myrror */
	private JTextField myrrorNodeMonstersMin;
	
	/** Maximum strength of monsters in nodes on Myrror */
	private JTextField myrrorNodeMonstersMax;
	
	/** Value of treasure in nodes on Myrror */
	private JLabel myrrorNodeTreasure;

	/** Minimum value of treasure in nodes on Myrror */
	private JTextField myrrorNodeTreasureMin;
	
	/** Maximum value of treasure in nodes on Myrror */
	private JTextField myrrorNodeTreasureMax;
	
	// CUSTOM FOG OF WAR PANEL
	
	/** Panel key */
	private final static String FOG_OF_WAR_PANEL = "FOW";
	
	/** Panel */
	private JPanel fogOfWarPanel;

	/** FOW setting for terrain */
	private JLabel fowTerrain;
	
	/** Can always see updates to terrain after seeing it once */
	private JCheckBox fowTerrainAlways;

	/** Remember terrain as we last saw it */
	private JCheckBox fowTerrainRemember;
	
	/** Forget terrain once we can no longer see it */
	private JCheckBox fowTerrainForget;
	
	/** FOW setting for cities */
	private JLabel fowCities;
	
	/** Can always see updates to cities after seeing them once */
	private JCheckBox fowCitiesAlways;

	/** Remember cities as we last saw them */
	private JCheckBox fowCitiesRemember;
	
	/** Forget cities once we can no longer see them */
	private JCheckBox fowCitiesForget;
	
	/** Can see what enemy cities are constructing? */
	private JCheckBox canSeeEnemyCityConstruction;
	
	/** FOW setting for units */
	private JLabel fowUnits;
	
	/** Can always see updates to units after seeing them once */
	private JCheckBox fowUnitsAlways;

	/** Remember units as we last saw them */
	private JCheckBox fowUnitsRemember;
	
	/** Forget units once we can no longer see them */
	private JCheckBox fowUnitsForget;
	
	// CUSTOM UNIT SETTINGS PANEL
	
	/** Panel key */
	private final static String UNITS_PANEL = "Units";
	
	/** Panel */
	private JPanel unitsPanel;
	
	/** Maximum units per overland map grid cell label */
	private JLabel maxUnitsPerGridCellLabel;
	
	/** Maximum units per overland map grid cell */
	private JTextField maxUnitsPerGridCell;
	
	/** Can temporary exceed maximum units during combat */
	private JCheckBox exceedMaxUnitsDuringCombat;

	/** Can temporary exceed maximum units during combat label */
	private JTextArea exceedMaxUnitsDuringCombatLabel;
	
	/** Maximum heroes hired at once label */
	private JLabel maximumHeroesLabel;
	
	/** Maximum heroes hired at once */
	private JTextField maximumHeroes;
	
	/** Roll hero skills at start of game? */
	private JCheckBox rollHeroSkillsAtStart;
	
	/** Roll hero skills at start of game label */
	private JTextArea rollHeroSkillsAtStartLabel;
	
	// CUSTOM SPELL SETTINGS PANEL
	
	/** Panel key */
	private final static String SPELLS_PANEL = "Spell";
	
	/** Panel */
	private JPanel spellsPanel;
	
	/** Options for whether we're allowed to switch research */
	private JLabel switchResearch;
	
	/** No, must finish current research first */
	private JCheckBox switchResearchNo;
	
	/** Can only switch if haven't started researching current spell */ 
	private JCheckBox switchResearchNotStarted;
	
	/** Can switch but lose any research towards current spell */
	private JCheckBox switchResearchLose;
	
	/** Can switch freely */
	private JCheckBox switchResearchFreely;
	
	/** Spell books to obtain first reduction label */
	private JLabel spellBookCountForFirstReductionLabel;
	
	/** Spell books to obtain first reduction */
	private JTextField spellBookCountForFirstReduction;
	
	/** Each book gives */
	private JLabel eachBookGives;
	
	/** Casting cost reduction prefix */
	private JLabel castingCostReductionPrefix;

	/** Casting cost reduction */
	private JTextField castingCostReduction;

	/** Casting cost reduction suffix */
	private JLabel castingCostReductionSuffix;
	
	/** Research bonus prefix */
	private JLabel researchBonusPrefix;

	/** Research bonus */
	private JTextField researchBonus;

	/** Research bonus suffix */
	private JLabel researchBonusSuffix;

	/** Casting cost reductions are added together */
	private JCheckBox castingCostReductionAdditive;

	/** Casting cost reductions are multiplied together */
	private JCheckBox castingCostReductionMultiplicative;
	
	/** Research bonus are added together */
	private JCheckBox researchBonusAdditive;

	/** Research bonus are multiplied together */
	private JCheckBox researchBonusMultiplicative;
	
	/** Casting cost reduction cap label */
	private JLabel castingCostReductionCapLabel;

	/** Casting cost reduction cap */
	private JTextField castingCostReductionCap;
	
	/** Research bonus cap label */
	private JLabel researchBonusCapLabel;

	/** Research bonus cap */
	private JTextField researchBonusCap;
	
	// DEBUG OPTIONS PANEL
	
	/** Panel key */
	private final static String DEBUG_PANEL = "Debug";
	
	/** Panel */
	private JPanel debugPanel;
	
	/** Disable fog of war */
	private JCheckBox disableFogOfWar;

	/** Disable fog of war label */
	private JTextArea disableFogOfWarLabel;
	
	// JOIN GAME PANEL
	
	/** Panel key */
	private final static String JOIN_GAME_PANEL = "Join";
	
	/** Panel */
	private JPanel joinPanel;
	
	/** List of sessions we can join */
	private List<SessionAndPlayerDescriptions> sessions = new ArrayList<SessionAndPlayerDescriptions> ();
	
	/** Table of sessions we can join */
	private JTable sessionsTable;
	
	/** Table model of sessions we can join */
	private final SessionListTableModel sessionsTableModel = new SessionListTableModel ();
	
	/** Refresh action */
	private Action refreshAction;
	
	// WIZARD SELECTION PANEL

	/** Panel key */
	private final static String WIZARD_PANEL = "Wizard";

	/** Panel */
	private JPanel wizardPanel;
	
	/** Dynamically created button actions */
	private final Map<String, Action> wizardButtonActions = new HashMap<String, Action> ();

	/** Dynamically created buttons etc */
	private final List<Component> wizardComponents = new ArrayList<Component> ();
	
	/** Gets set to chosen wizard ID when player clicks a button, or null if they click custom */
	private String wizardChosen;
	
	// PORTRAIT SELECTION PANEL (for custom wizards)

	/** Panel key */
	private final static String PORTRAIT_PANEL = "Portrait";

	/** Panel */
	private JPanel portraitPanel;
	
	/** Dynamically created button actions */
	private final Map<String, Action> portraitButtonActions = new HashMap<String, Action> ();

	/** Dynamically created buttons etc */
	private final List<Component> portraitComponents = new ArrayList<Component> ();
	
	/** Gets set to chosen portrait ID when player clicks a button, or null if they click custom */
	private String portraitChosen;
	
	/** File chooser for selecting image file */
	private JFileChooser customPortraitChooser;
	
	// FLAG COLOUR PANEL (for custom wizards with custom portraits)
	
	/** Panel key */
	private final static String FLAG_PANEL = "Flag";
	
	/** Panel */
	private JPanel flagColourPanel;
	
	/** Red title */
	private JLabel flagColourRedTitle;
	
	/** Green title */
	private JLabel flagColourGreenTitle;
	
	/** Blue title */
	private JLabel flagColourBlueTitle;

	/** Red slider */
	private JSlider flagColourRed;
	
	/** Green slider */
	private JSlider flagColourGreen;
	
	/** Blue slider */
	private JSlider flagColourBlue;
	
	// CUSTOM PICKS PANEL (for custom wizards)
	
	/** Panel key */
	private final static String PICKS_PANEL = "Picks";
	
	/** Panel */
	private JPanel picksPanel;
	
	/** Dynamically created button actions */
	private final Map<String, ToggleAction> retortButtonActions = new HashMap<String, ToggleAction> ();

	/** Dynamically created bookshelf titles */
	private final Map<String, JLabel> bookshelfTitles = new HashMap<String, JLabel> ();

	/** Dynamically created buttons for adding books */
	private final Map<String, Action> addBookActions = new HashMap<String, Action> ();
	
	/** Dynamically created buttons for removing books */
	private final Map<String, Action> removeBookActions = new HashMap<String, Action> ();
	
	/** Dynamically created buttons etc */
	private final List<Component> customPicksComponents = new ArrayList<Component> ();

	/** Shelf displaying each kind of book */
	private final Map<String, JPanel> magicRealmBookshelves = new HashMap<String, JPanel> ();
	
	// FREE SPELL SELECTION PANEL
	
	/** Panel key */
	private final static String FREE_SPELLS_PANEL = "Free";
	
	/** Panel */
	private JPanel freeSpellsPanel;
	
	/** Magic realm we're current choosing free spells for */
	private String currentMagicRealmID;
	
	/** Dynamically created rank titles */
	private final Map<ChooseInitialSpellsNowRank, JLabel> spellRankTitles = new HashMap<ChooseInitialSpellsNowRank, JLabel> ();
	
	/** Free spell actions */
	private final Map<Spell, ToggleAction> freeSpellActions = new HashMap<Spell, ToggleAction> ();

	/** Dynamically created buttons etc */
	private final List<Component> freeSpellsComponents = new ArrayList<Component> ();
	
	// RACE SELECTION PANEL
	
	/** Panel key */
	private final static String RACE_PANEL = "Race";

	/** Panel */
	private JPanel racePanel;
	
	/** Dynamically created titles */
	private final Map<Integer, JLabel> racePlanes = new HashMap<Integer, JLabel> ();
	
	/** Dynamically created button actions */
	private final Map<Race, Action> raceButtonActions = new HashMap<Race, Action> ();

	/** Dynamically created buttons etc */
	private final List<Component> raceComponents = new ArrayList<Component> ();
	
	/** Gets set to chosen race ID when player clicks a button */
	private String raceChosen;
	
	// WAITING TO OTHER PLAYERS TO JOIN PANEL
	
	/** Panel key */
	private final static String WAIT_PANEL = "Wait";

	/** Panel */
	private JPanel waitPanel;
	
	/** Table players in session */
	private final PlayerListTableModel playersTableModel = new PlayerListTableModel ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");

		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/background.png");
		final BufferedImage divider = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/divider.png");
		flagColourSliderBackground = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/flagColourSlider.png");
		
		bookshelfWithGargoyles = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/bookshelfWithGargoyles.png");
		bookshelfWithoutGargoyles = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/bookshelfWithoutGargoyles.png");
		addBookNormal = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/addBookNormal.png");
		addBookPressed = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/addBookPressed.png");
		removeBookNormal = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/removeBookNormal.png");
		removeBookPressed = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/removeBookPressed.png");
		
		final BufferedImage midButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button135x17Normal.png");
		final BufferedImage midButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button135x17Pressed.png");
		final BufferedImage wideButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button290x17Normal.png");
		final BufferedImage wideButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button290x17Pressed.png");
		final BufferedImage checkboxUnticked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Unticked.png");
		final BufferedImage checkboxTicked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Ticked.png");
		final BufferedImage editboxSmall = getUtils ().loadImage ("/momime.client.graphics/ui/editBoxes/editBox65x23.png");
		final BufferedImage editboxWide = getUtils ().loadImage ("/momime.client.graphics/ui/editBoxes/editBox125x23.png");
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
					if ((newGamePanel.isVisible ()) || (mapSizePanel.isVisible ()) || (landProportionPanel.isVisible ()) || (nodesPanel.isVisible ()) ||
						(difficulty1Panel.isVisible ()) || (difficulty2Panel.isVisible ()) || (difficulty3Panel.isVisible ()) || (fogOfWarPanel.isVisible ()) ||
						(unitsPanel.isVisible ()) || (spellsPanel.isVisible ()) || (debugPanel.isVisible ()))
						
						showNextNewGamePanel ();
					
					else if (joinPanel.isVisible ())
					{
						final SessionAndPlayerDescriptions spd = getSessions ().get (sessionsTable.getSelectedRow ());
						
						final PlayerDescription pd = new PlayerDescription ();
						pd.setPlayerID (getClient ().getOurPlayerID ());
						pd.setPlayerName (getClient ().getOurPlayerName ());
				
						final JoinSession msg = new JoinSession ();
						msg.setSessionID (spd.getSessionDescription ().getSessionID ());
						msg.setPlayerDescription (pd);
				
						getClient ().getServerConnection ().sendMessageToServer (msg);

						okAction.setEnabled (false);
					}
					else if (wizardPanel.isVisible ())
					{
						final ChooseWizardMessage msg = new ChooseWizardMessage ();
						msg.setWizardID (wizardChosen);
						getClient ().getServerConnection ().sendMessageToServer (msg);

						okAction.setEnabled (false);
					}
					else if (portraitPanel.isVisible ())
					{
						if (PlayerKnowledgeUtils.isCustomWizard (portraitChosen))
						{
							try (final FileInputStream in = new FileInputStream (customPortraitChooser.getSelectedFile ()))
							{
								try (final ByteArrayOutputStream out = new ByteArrayOutputStream ())
								{
									StreamUtils.copyBetweenStreams (in, out, true, false, 0);
									
									final UploadCustomPhotoMessage msg = new UploadCustomPhotoMessage ();
									msg.setNdgBmpImage (out.toByteArray ());
									getClient ().getServerConnection ().sendMessageToServer (msg);
								}
							}
						}
						else
						{
							final ChooseStandardPhotoMessage msg = new ChooseStandardPhotoMessage ();
							msg.setPhotoID (portraitChosen);
							getClient ().getServerConnection ().sendMessageToServer (msg);
						}						

						okAction.setEnabled (false);
					}
					else if (flagColourPanel.isVisible ())
					{
						final ChooseCustomFlagColourMessage msg = new ChooseCustomFlagColourMessage ();
						msg.setFlagColour (sliderToHex (flagColourRed) + sliderToHex (flagColourGreen) + sliderToHex (flagColourBlue));
						getClient ().getServerConnection ().sendMessageToServer (msg);
						
						// We assume the server is going to accept this, and go straight to the custom picks screen
						showCustomPicksPanel ();
					}
					else if (picksPanel.isVisible ())
					{
						final ChooseCustomPicksMessage msg = new ChooseCustomPicksMessage ();
						for (final PlayerPick src : picks)
						{
							final WizardPick dest = new WizardPick ();
							dest.setPick (src.getPickID ());
							dest.setQuantity (src.getQuantity ());
							msg.getPick ().add (dest);
						}
						getClient ().getServerConnection ().sendMessageToServer (msg);
						okAction.setEnabled (false);
					}
					else if (freeSpellsPanel.isVisible ())
					{
						final ChooseInitialSpellsMessage msg = new ChooseInitialSpellsMessage ();
						msg.setPickID (currentMagicRealmID);
						
						for (final Entry<Spell, ToggleAction> spell : freeSpellActions.entrySet ())
							if (spell.getValue ().isSelected ())
								msg.getSpell ().add (spell.getKey ().getSpellID ());

						getClient ().getServerConnection ().sendMessageToServer (msg);

						okAction.setEnabled (false);
					}
					else if (racePanel.isVisible ())
					{
						final ChooseRaceMessage msg = new ChooseRaceMessage ();
						msg.setRaceID (raceChosen);
						getClient ().getServerConnection ().sendMessageToServer (msg);

						okAction.setEnabled (false);
					}
					else
						log.warn ("Don't know what action to take from clicking OK button");
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
		
		// Right clicking on specific retorts in the text area brings up help text about them
		retorts.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					try
					{
						final String pickID = updateRetortsFromPicks (retorts.viewToModel (ev.getPoint ()));
						if (pickID != null)
							getHelpUI ().showPickID (pickID);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		});				
		
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
				enableOrDisableNewGameOkButton ();
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
				enableOrDisableNewGameOkButton ();
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
		
		customizeMapSize = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		newGamePanel.add (customizeMapSize, "frmNewGameMapSizeCustomize");
		
		landProportionLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (landProportionLabel, "frmNewGameLandProportion");

		newGamePanel.add (getUtils ().createImageButton (changeLandProportionAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameLandProportionButton");

		customizeLandProportion = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		newGamePanel.add (customizeLandProportion, "frmNewGameLandProportionCustomize");
		
		nodesLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (nodesLabel, "frmNewGameNodes");
		
		newGamePanel.add (getUtils ().createImageButton (changeNodeStrengthAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameNodesButton");
		
		customizeNodes = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		newGamePanel.add (customizeNodes, "frmNewGameNodesCustomize");
		
		difficultyLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (difficultyLabel, "frmNewGameDifficulty");
		
		newGamePanel.add (getUtils ().createImageButton (changeDifficultyLevelAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameDifficultyButton");
		
		customizeDifficulty = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		newGamePanel.add (customizeDifficulty, "frmNewGameDifficultyCustomize");
		
		turnSystemLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (turnSystemLabel, "frmNewGameTurnSystem");
		
		newGamePanel.add (getUtils ().createImageButton (changeTurnSystemAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameTurnSystemButton");
		
		fogOfWarLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (fogOfWarLabel, "frmNewGameFogOfWar");

		newGamePanel.add (getUtils ().createImageButton (changeFogOfWarSettingsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameFogOfWarButton");
		
		customizeFogOfWar = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		newGamePanel.add (customizeFogOfWar, "frmNewGameFogOfWarCustomize");
		
		unitSettingsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (unitSettingsLabel, "frmNewGameUnitSettings");

		newGamePanel.add (getUtils ().createImageButton (changeUnitSettingsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameUnitButton");

		customizeUnits = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		newGamePanel.add (customizeUnits, "frmNewGameUnitCustomize");
		
		spellSettingsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (spellSettingsLabel, "frmNewGameSpellSettings");
		
		newGamePanel.add (getUtils ().createImageButton (changeSpellSettingsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameSpellButton");
		
		customizeSpells = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		newGamePanel.add (customizeSpells, "frmNewGameSpellCustomize");
		
		debugOptionsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (debugOptionsLabel, "frmNewGameDebugOptions");
		
		newGamePanel.add (getUtils ().createImageButton (changeDebugOptionsAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			midButtonNormal, midButtonPressed, midButtonNormal), "frmNewGameDebugButton");
		
		gameNameLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		newGamePanel.add (gameNameLabel, "frmNewGameGameName");
		
		gameName = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getMediumFont (), editboxWide);
		newGamePanel.add (gameName, "frmNewGameGameNameEdit");

		customizeLabel = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		newGamePanel.add (customizeLabel, "frmNewGameCustomize");
		
		for (int n = 1; n <= 2; n++)
			newGamePanel.add (getUtils ().createImage (divider), "frmNewGameBar" + n);
		
		cards.add (newGamePanel, NEW_GAME_PANEL);
		
		// CUSTOM MAP SIZE PANEL
		mapSizePanel = new JPanel (new XmlLayoutManager (getNewGameLayoutMapSize ()));
		mapSizePanel.setOpaque (false);
		
		mapSizeEdit = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (mapSizeEdit, "frmNewGameCustomMapSizeMapSize");
		
		mapSizeWidth = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (mapSizeWidth, "frmNewGameCustomMapSizeWidth");
		
		mapSizePanel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "x"), "frmNewGameCustomMapSizeX1");
		
		mapSizeHeight = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (mapSizeHeight, "frmNewGameCustomMapSizeHeight");
		
		mapWrapsLeftToRight = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		mapSizePanel.add (mapWrapsLeftToRight, "frmNewGameCustomMapSizeWrapsLeftRight");
		
		mapWrapsTopToBottom = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		mapSizePanel.add (mapWrapsTopToBottom, "frmNewGameCustomMapSizeWrapsTopBottom");
		
		mapZonesLabel = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (mapZonesLabel, "frmNewGameCustomMapSizeZones");

		mapZoneWidth = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (mapZoneWidth, "frmNewGameCustomMapSizeZoneWidth");

		mapSizePanel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "x"), "frmNewGameCustomMapSizeX2");
		
		mapZoneHeight = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (mapZoneHeight, "frmNewGameCustomMapSizeZoneHeight");
		
		towersOfWizardryLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (towersOfWizardryLabel, "frmNewGameCustomMapSizeTowers");
		
		towersOfWizardryCount = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (towersOfWizardryCount, "frmNewGameCustomMapSizeTowerCount");
		
		towersOfWizardrySeparationLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (towersOfWizardrySeparationLabel, "frmNewGameCustomMapSizeTowerSeparation");
		
		towersOfWizardrySeparation = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (towersOfWizardrySeparation, "frmNewGameCustomMapSizeTowerSeparationEdit");
		
		continentalRaceChanceLabel = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (continentalRaceChanceLabel, "frmNewGameCustomMapSizeContinental");
		
		continentalRaceChance = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (continentalRaceChance, "frmNewGameCustomMapSizeContinentalRaceChance");
		
		mapSizePanel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "%"), "frmNewGameCustomMapSizeP1");
		
		citySeparationLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (citySeparationLabel, "frmNewGameCustomMapSizeCitySeparation");
		
		citySeparation = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (citySeparation, "frmNewGameCustomMapSizeCitySeparationEdit");

		riverCount = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (riverCount, "frmNewGameCustomMapSizeRiversEdit");

		riverCountLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (riverCountLabel, "frmNewGameCustomMapSizeRivers");

		raiderCityCountLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (raiderCityCountLabel, "frmNewGameCustomMapSizeRaiderCityCount");
		
		raiderCityCount = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (raiderCityCount, "frmNewGameCustomMapSizeRaiderCityCountEdit");

		arcanusNodeCount = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (arcanusNodeCount, "frmNewGameCustomMapSizeArcanusNodeCountEdit");

		arcanusNodeCountLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (arcanusNodeCountLabel, "frmNewGameCustomMapSizeArcanusNodeCount");
		
		myrrorNodeCount = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		mapSizePanel.add (myrrorNodeCount, "frmNewGameCustomMapSizeMyrrorNodeCountEdit");

		myrrorNodeCountLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapSizePanel.add (myrrorNodeCountLabel, "frmNewGameCustomMapSizeMyrrorNodeCount");
		
		cards.add (mapSizePanel, MAP_SIZE_PANEL);
		
		// CUSTOM LAND PROPORTION PANEL
		landProportionPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutLandProportion ()));
		landProportionPanel.setOpaque (false);

		landPercentage = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (landPercentage, "frmNewGameCustomLandProportionPercentageMapIsLandEdit");

		landPercentageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (landPercentageLabel, "frmNewGameCustomLandProportionPercentageMapIsLand");

		hillsPercentage = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (hillsPercentage, "frmNewGameCustomLandProportionHillsProportionEdit");

		hillsPercentageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (hillsPercentageLabel, "frmNewGameCustomLandProportionHillsProportion");

		mountainsPercentage = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (mountainsPercentage, "frmNewGameCustomLandProportionMountainsProportionEdit");

		mountainsPercentageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (mountainsPercentageLabel, "frmNewGameCustomLandProportionMountainsProportion");

		treesPercentage = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (treesPercentage, "frmNewGameCustomLandProportionTreesProportionEdit");

		treesPercentageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (treesPercentageLabel, "frmNewGameCustomLandProportionTreesProportion");

		treeAreaSize = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (treeAreaSize, "frmNewGameCustomLandProportionTreeAreaTileCountEdit");

		treeAreaSizePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (treeAreaSizePrefix, "frmNewGameCustomLandProportionTreeAreaTileCountPrefix");

		treeAreaSizeSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (treeAreaSizeSuffix, "frmNewGameCustomLandProportionTreeAreaTileCountSuffix");

		desertsPercentage = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (desertsPercentage, "frmNewGameCustomLandProportionDesertProportionEdit");

		desertsPercentageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (desertsPercentageLabel, "frmNewGameCustomLandProportionDesertProportion");

		desertAreaSize = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (desertAreaSize, "frmNewGameCustomLandProportionDesertAreaTileCountEdit");

		desertAreaSizePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (desertAreaSizePrefix, "frmNewGameCustomLandProportionDesertAreaTileCountPrefix");

		desertAreaSizeSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (desertAreaSizeSuffix, "frmNewGameCustomLandProportionDesertAreaTileCountSuffix");

		swampsPercentage = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (swampsPercentage, "frmNewGameCustomLandProportionSwampProportionEdit");

		swampsPercentageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (swampsPercentageLabel, "frmNewGameCustomLandProportionSwampProportion");

		swampAreaSize = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (swampAreaSize, "frmNewGameCustomLandProportionSwampAreaTileCountEdit");

		swampAreaSizePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (swampAreaSizePrefix, "frmNewGameCustomLandProportionSwampAreaTileCountPrefix");

		swampAreaSizeSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (swampAreaSizeSuffix, "frmNewGameCustomLandProportionSwampAreaTileCountSuffix");

		tundraDistance = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (tundraDistance, "frmNewGameCustomLandProportionTundraEdit");

		tundraDistancePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (tundraDistancePrefix, "frmNewGameCustomLandProportionTundraPrefix");

		tundraDistanceSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (tundraDistanceSuffix, "frmNewGameCustomLandProportionTundraSuffix");

		arcanusMineralChance = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (arcanusMineralChance, "frmNewGameCustomLandProportionArcanusEdit");

		arcanusMineralChancePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (arcanusMineralChancePrefix, "frmNewGameCustomLandProportionArcanusPrefix");

		arcanusMineralChanceSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (arcanusMineralChanceSuffix, "frmNewGameCustomLandProportionArcanusSuffix");

		myrrorMineralChance = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		landProportionPanel.add (myrrorMineralChance, "frmNewGameCustomLandProportionMyrrorEdit");

		myrrorMineralChancePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (myrrorMineralChancePrefix, "frmNewGameCustomLandProportionMyrrorPrefix");

		myrrorMineralChanceSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		landProportionPanel.add (myrrorMineralChanceSuffix, "frmNewGameCustomLandProportionMyrrorSuffix");

		cards.add (landProportionPanel, LAND_PROPORTION_PANEL);
		
		// CUSTOM NODES PANEL
		nodesPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutNodes ()));
		nodesPanel.setOpaque (false);

		doubleNodeAuraMagicPower = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		nodesPanel.add (doubleNodeAuraMagicPower, "frmNewGameCustomNodesMagicPowerEdit");

		doubleNodeAuraMagicPowerPrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		nodesPanel.add (doubleNodeAuraMagicPowerPrefix, "frmNewGameCustomNodesMagicPowerPrefix");

		doubleNodeAuraMagicPowerSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		nodesPanel.add (doubleNodeAuraMagicPowerSuffix, "frmNewGameCustomNodesMagicPowerSuffix");

		arcanusNodeSizeMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		nodesPanel.add (arcanusNodeSizeMin, "frmNewGameCustomNodesArcanusNodeAuraMin");

		arcanusNodeSizeMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		nodesPanel.add (arcanusNodeSizeMax, "frmNewGameCustomNodesArcanusNodeAuraMax");

		arcanusNodeSizePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		nodesPanel.add (arcanusNodeSizePrefix, "frmNewGameCustomNodesArcanusNodeAuraPrefix");

		arcanusNodeSizeSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		nodesPanel.add (arcanusNodeSizeSuffix, "frmNewGameCustomNodesArcanusNodeAuraSuffix");
		
		nodesPanel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomNodesArcanusNodeAuraDash");

		myrrorNodeSizeMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		nodesPanel.add (myrrorNodeSizeMin, "frmNewGameCustomNodesMyrrorNodeAuraMin");

		myrrorNodeSizeMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		nodesPanel.add (myrrorNodeSizeMax, "frmNewGameCustomNodesMyrrorNodeAuraMax");

		myrrorNodeSizePrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		nodesPanel.add (myrrorNodeSizePrefix, "frmNewGameCustomNodesMyrrorNodeAuraPrefix");

		myrrorNodeSizeSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		nodesPanel.add (myrrorNodeSizeSuffix, "frmNewGameCustomNodesMyrrorNodeAuraSuffix");

		nodesPanel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomNodesMyrrorNodeAuraDash");
		
		cards.add (nodesPanel, NODES_PANEL);
		
		// CUSTOM DIFFICULTY PANEL (1 of 3)
		difficulty1Panel = new JPanel (new XmlLayoutManager (getNewGameLayoutDifficulty1 ()));
		difficulty1Panel.setOpaque (false);

		spellPicksLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (spellPicksLabel, "frmNewGameCustomDifficulty1SpellPicks");
		
		humanSpellPicksLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (humanSpellPicksLabel, "frmNewGameCustomDifficulty1HumanSpellPicks");

		humanSpellPicks = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (humanSpellPicks, "frmNewGameCustomDifficulty1HumanSpellPicksEdit");

		aiSpellPicksLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (aiSpellPicksLabel, "frmNewGameCustomDifficulty1AISpellPicks");

		aiSpellPicks = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (aiSpellPicks, "frmNewGameCustomDifficulty1AISpellPicksEdit");

		startingGoldLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (startingGoldLabel, "frmNewGameCustomDifficulty1Gold");
		
		humanStartingGoldLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (humanStartingGoldLabel, "frmNewGameCustomDifficulty1HumanGold");

		humanStartingGold = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (humanStartingGold, "frmNewGameCustomDifficulty1HumanGoldEdit");

		aiStartingGoldLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (aiStartingGoldLabel, "frmNewGameCustomDifficulty1AIGold");

		aiStartingGold = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (aiStartingGold, "frmNewGameCustomDifficulty1AIGoldEdit");
		
		allowCustomWizards = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		difficulty1Panel.add (allowCustomWizards, "frmNewGameCustomDifficulty1CustomWizards");
		
		eachWizardOnlyOnce = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		difficulty1Panel.add (eachWizardOnlyOnce, "frmNewGameCustomDifficulty1EachWizardOnlyOnce");
		
		wizardCityStartSizeLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (wizardCityStartSizeLabel, "frmNewGameCustomDifficulty1WizardCitySize");
		
		wizardCityStartSize = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (wizardCityStartSize, "frmNewGameCustomDifficulty1WizardCitySizeEdit");
		
		maxCitySizeLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (maxCitySizeLabel, "frmNewGameCustomDifficulty1MaxCitySize");
		
		maxCitySize = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (maxCitySize, "frmNewGameCustomDifficulty1MaxCitySizeEdit");
		
		raiderCityStartSizeLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (raiderCityStartSizeLabel, "frmNewGameCustomDifficulty1RaiderCitySizePrefix");

		raiderCityStartSizeMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (raiderCityStartSizeMin, "frmNewGameCustomDifficulty1RaiderCitySizeMin");
		
		raiderCityStartSizeAnd = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (raiderCityStartSizeAnd, "frmNewGameCustomDifficulty1RaiderCitySizeAnd");
		
		raiderCityStartSizeMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (raiderCityStartSizeMax, "frmNewGameCustomDifficulty1RaiderCitySizeMax");
		
		raiderCitySizeCapPrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (raiderCitySizeCapPrefix, "frmNewGameCustomDifficulty1RaiderCityGrowthPrefix");

		raiderCitySizeCap = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty1Panel.add (raiderCitySizeCap, "frmNewGameCustomDifficulty1RaiderCityGrowthEdit");

		raiderCitySizeCapSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty1Panel.add (raiderCitySizeCapSuffix, "frmNewGameCustomDifficulty1RaiderCityGrowthSuffix");
		
		cards.add (difficulty1Panel, DIFFICULTY_1_PANEL);
		
		// CUSTOM DIFFICULTY PANEL (2 of 3)
		difficulty2Panel = new JPanel (new XmlLayoutManager (getNewGameLayoutDifficulty2 ()));
		difficulty2Panel.setOpaque (false);

		towersMonsters = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (towersMonsters, "frmNewGameCustomDifficulty2TowerMonsters");

		towersMonstersMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (towersMonstersMin, "frmNewGameCustomDifficulty2TowerMonstersMin");
		
		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2TowerMonstersDash");
		
		towersMonstersMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (towersMonstersMax, "frmNewGameCustomDifficulty2TowerMonstersMax");
		
		towersTreasure = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (towersTreasure, "frmNewGameCustomDifficulty2TowerTreasure");

		towersTreasureMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (towersTreasureMin, "frmNewGameCustomDifficulty2TowerTreasureMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2TowerTreasureDash");
		
		towersTreasureMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (towersTreasureMax, "frmNewGameCustomDifficulty2TowerTreasureMax");
		
		normalLairCountLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (normalLairCountLabel, "frmNewGameCustomDifficulty2NormalLairCount");

		normalLairCount = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (normalLairCount, "frmNewGameCustomDifficulty2NormalLairCountEdit");

		arcanusNormalLairMonsters = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (arcanusNormalLairMonsters, "frmNewGameCustomDifficulty2NormalArcanusLairMonsters");

		arcanusNormalLairMonstersMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusNormalLairMonstersMin, "frmNewGameCustomDifficulty2NormalArcanusLairMonstersMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2NormalArcanusLairMonstersDash");
		
		arcanusNormalLairMonstersMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusNormalLairMonstersMax, "frmNewGameCustomDifficulty2NormalArcanusLairMonstersMax");
		
		arcanusNormalLairTreasure = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (arcanusNormalLairTreasure, "frmNewGameCustomDifficulty2NormalArcanusLairTreasure");

		arcanusNormalLairTreasureMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusNormalLairTreasureMin, "frmNewGameCustomDifficulty2NormalArcanusLairTreasureMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2NormalArcanusLairTreasureDash");
		
		arcanusNormalLairTreasureMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusNormalLairTreasureMax, "frmNewGameCustomDifficulty2NormalArcanusLairTreasureMax");
		
		myrrorNormalLairMonsters = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (myrrorNormalLairMonsters, "frmNewGameCustomDifficulty2NormalMyrrorLairMonsters");

		myrrorNormalLairMonstersMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorNormalLairMonstersMin, "frmNewGameCustomDifficulty2NormalMyrrorLairMonstersMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2NormalMyrrorLairMonstersDash");
		
		myrrorNormalLairMonstersMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorNormalLairMonstersMax, "frmNewGameCustomDifficulty2NormalMyrrorLairMonstersMax");
		
		myrrorNormalLairTreasure = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (myrrorNormalLairTreasure, "frmNewGameCustomDifficulty2NormalMyrrorLairTreasure");

		myrrorNormalLairTreasureMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorNormalLairTreasureMin, "frmNewGameCustomDifficulty2NormalMyrrorLairTreasureMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2NormalMyrrorLairTreasureDash");
		
		myrrorNormalLairTreasureMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorNormalLairTreasureMax, "frmNewGameCustomDifficulty2NormalMyrrorLairTreasureMax");

		weakLairCountLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (weakLairCountLabel, "frmNewGameCustomDifficulty2WeakLairCount");

		weakLairCount = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (weakLairCount, "frmNewGameCustomDifficulty2WeakLairCountEdit");

		arcanusWeakLairMonsters = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (arcanusWeakLairMonsters, "frmNewGameCustomDifficulty2WeakArcanusLairMonsters");

		arcanusWeakLairMonstersMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusWeakLairMonstersMin, "frmNewGameCustomDifficulty2WeakArcanusLairMonstersMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2WeakArcanusLairMonstersDash");
		
		arcanusWeakLairMonstersMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusWeakLairMonstersMax, "frmNewGameCustomDifficulty2WeakArcanusLairMonstersMax");
		
		arcanusWeakLairTreasure = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (arcanusWeakLairTreasure, "frmNewGameCustomDifficulty2WeakArcanusLairTreasure");

		arcanusWeakLairTreasureMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusWeakLairTreasureMin, "frmNewGameCustomDifficulty2WeakArcanusLairTreasureMin");
		
		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2WeakArcanusLairTreasureDash");

		arcanusWeakLairTreasureMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (arcanusWeakLairTreasureMax, "frmNewGameCustomDifficulty2WeakArcanusLairTreasureMax");
		
		myrrorWeakLairMonsters = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (myrrorWeakLairMonsters, "frmNewGameCustomDifficulty2WeakMyrrorLairMonsters");

		myrrorWeakLairMonstersMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorWeakLairMonstersMin, "frmNewGameCustomDifficulty2WeakMyrrorLairMonstersMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2WeakMyrrorLairMonstersDash");
		
		myrrorWeakLairMonstersMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorWeakLairMonstersMax, "frmNewGameCustomDifficulty2WeakMyrrorLairMonstersMax");
		
		myrrorWeakLairTreasure = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty2Panel.add (myrrorWeakLairTreasure, "frmNewGameCustomDifficulty2WeakMyrrorLairTreasure");

		myrrorWeakLairTreasureMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorWeakLairTreasureMin, "frmNewGameCustomDifficulty2WeakMyrrorLairTreasureMin");

		difficulty2Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty2WeakMyrrorLairTreasureDash");
		
		myrrorWeakLairTreasureMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty2Panel.add (myrrorWeakLairTreasureMax, "frmNewGameCustomDifficulty2WeakMyrrorLairTreasureMax");
		
		cards.add (difficulty2Panel, DIFFICULTY_2_PANEL);
		
		// CUSTOM DIFFICULTY PANEL (3 of 3)
		difficulty3Panel = new JPanel (new XmlLayoutManager (getNewGameLayoutDifficulty3 ()));
		difficulty3Panel.setOpaque (false);

		arcanusNodeMonsters = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty3Panel.add (arcanusNodeMonsters, "frmNewGameCustomDifficulty3ArcanusNodeMonsters");

		arcanusNodeMonstersMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (arcanusNodeMonstersMin, "frmNewGameCustomDifficulty3ArcanusNodeMonstersMin");

		difficulty3Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty3ArcanusNodeMonstersDash");
		
		arcanusNodeMonstersMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (arcanusNodeMonstersMax, "frmNewGameCustomDifficulty3ArcanusNodeMonstersMax");
		
		arcanusNodeTreasure = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty3Panel.add (arcanusNodeTreasure, "frmNewGameCustomDifficulty3ArcanusNodeTreasure");

		arcanusNodeTreasureMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (arcanusNodeTreasureMin, "frmNewGameCustomDifficulty3ArcanusNodeTreasureMin");

		difficulty3Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty3ArcanusNodeTreasureDash");
		
		arcanusNodeTreasureMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (arcanusNodeTreasureMax, "frmNewGameCustomDifficulty3ArcanusNodeTreasureMax");
		
		myrrorNodeMonsters = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty3Panel.add (myrrorNodeMonsters, "frmNewGameCustomDifficulty3MyrrorNodeMonsters");

		myrrorNodeMonstersMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (myrrorNodeMonstersMin, "frmNewGameCustomDifficulty3MyrrorNodeMonstersMin");

		difficulty3Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty3MyrrorNodeMonstersDash");
		
		myrrorNodeMonstersMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (myrrorNodeMonstersMax, "frmNewGameCustomDifficulty3MyrrorNodeMonstersMax");
		
		myrrorNodeTreasure = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		difficulty3Panel.add (myrrorNodeTreasure, "frmNewGameCustomDifficulty3MyrrorNodeTreasure");

		myrrorNodeTreasureMin = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (myrrorNodeTreasureMin, "frmNewGameCustomDifficulty3MyrrorNodeTreasureMin");

		difficulty3Panel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "-"), "frmNewGameCustomDifficulty3MyrrorNodeTreasureDash");
		
		myrrorNodeTreasureMax = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		difficulty3Panel.add (myrrorNodeTreasureMax, "frmNewGameCustomDifficulty3MyrrorNodeTreasureMax");
		
		cards.add (difficulty3Panel, DIFFICULTY_3_PANEL);
		
		// CUSTOM FOG OF WAR PANEL
		fogOfWarPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutFogOfWar ()));
		fogOfWarPanel.setOpaque (false);
		
		fowTerrain = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		fogOfWarPanel.add (fowTerrain, "frmNewGameCustomFogOfWarTerrainNodesAuras");
		
		fowTerrainAlways = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowTerrainAlways, "frmNewGameCustomFogOfWarTerrainAlways");

		fowTerrainRemember = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowTerrainRemember, "frmNewGameCustomFogOfWarTerrainRemember");
		
		fowTerrainForget = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowTerrainForget, "frmNewGameCustomFogOfWarTerrainForget");
		
		fowCities = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		fogOfWarPanel.add (fowCities, "frmNewGameCustomFogOfWarCitiesSpellsCAEs");
		
		fowCitiesAlways = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowCitiesAlways, "frmNewGameCustomFogOfWarCitiesAlways");

		fowCitiesRemember = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowCitiesRemember, "frmNewGameCustomFogOfWarCitiesRemember");
		
		fowCitiesForget = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowCitiesForget, "frmNewGameCustomFogOfWarCitiesForget");
		
		canSeeEnemyCityConstruction = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (canSeeEnemyCityConstruction, "frmNewGameCustomFogOfWarConstructing");
		
		fowUnits = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		fogOfWarPanel.add (fowUnits, "frmNewGameCustomFogOfWarUnits");
		
		fowUnitsAlways = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowUnitsAlways, "frmNewGameCustomFogOfWarUnitsAlways");

		fowUnitsRemember = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowUnitsRemember, "frmNewGameCustomFogOfWarUnitsRemember");
		
		fowUnitsForget = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		fogOfWarPanel.add (fowUnitsForget, "frmNewGameCustomFogOfWarUnitsForget");
		
		final ButtonGroup terrainFowChoices = new ButtonGroup ();
		terrainFowChoices.add (fowTerrainAlways);
		terrainFowChoices.add (fowTerrainRemember);
		terrainFowChoices.add (fowTerrainForget);

		final ButtonGroup citiesFowChoices = new ButtonGroup ();
		citiesFowChoices.add (fowCitiesAlways);
		citiesFowChoices.add (fowCitiesRemember);
		citiesFowChoices.add (fowCitiesForget);

		final ButtonGroup unitsFowChoices = new ButtonGroup ();
		unitsFowChoices.add (fowUnitsAlways);
		unitsFowChoices.add (fowUnitsRemember);
		unitsFowChoices.add (fowUnitsForget);
		
		cards.add (fogOfWarPanel, FOG_OF_WAR_PANEL);
		
		// CUSTOM UNIT SETTINGS PANEL
		unitsPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutUnits ()));
		unitsPanel.setOpaque (false);
		
		maxUnitsPerGridCellLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		unitsPanel.add (maxUnitsPerGridCellLabel, "frmNewGameCustomUnitsMaxPerGridCell");
		
		maxUnitsPerGridCell = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		unitsPanel.add (maxUnitsPerGridCell, "frmNewGameCustomUnitsMaxPerGridCellEdit");
		
		exceedMaxUnitsDuringCombat = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		unitsPanel.add (exceedMaxUnitsDuringCombat, "frmNewGameCustomUnitsCanExceedMaximumUnitsDuringCombatCheckbox");

		exceedMaxUnitsDuringCombatLabel = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		unitsPanel.add (exceedMaxUnitsDuringCombatLabel, "frmNewGameCustomUnitsCanExceedMaximumUnitsDuringCombat");
		
		maximumHeroesLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		unitsPanel.add (maximumHeroesLabel, "frmNewGameCustomUnitsMaxHeroes");
		
		maximumHeroes = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		unitsPanel.add (maximumHeroes, "frmNewGameCustomUnitsMaxHeroesEdit");
		
		rollHeroSkillsAtStart = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		unitsPanel.add (rollHeroSkillsAtStart, "frmNewGameCustomUnitsRollHeroSkillsAtStartOfGameCheckbox");
		
		rollHeroSkillsAtStartLabel = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		unitsPanel.add (rollHeroSkillsAtStartLabel, "frmNewGameCustomUnitsRollHeroSkillsAtStartOfGame");
		
		cards.add (unitsPanel, UNITS_PANEL);
		
		// CUSTOM SPELL SETTINGS PANEL
		spellsPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutSpells ()));
		spellsPanel.setOpaque (false);

		switchResearch = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (switchResearch, "frmNewGameCustomSpellsSwitchResearch");
		
		switchResearchNo = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (switchResearchNo, "frmNewGameCustomSpellsSwitchResearchNo");
		
		switchResearchNotStarted = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (switchResearchNotStarted, "frmNewGameCustomSpellsSwitchResearchNotStarted");
		
		switchResearchLose = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (switchResearchLose, "frmNewGameCustomSpellsSwitchResearchLose");
		
		switchResearchFreely = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (switchResearchFreely, "frmNewGameCustomSpellsSwitchResearchFreely");
		
		spellBookCountForFirstReductionLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (spellBookCountForFirstReductionLabel, "frmNewGameCustomSpellsBooksToObtainFirstReduction");
		
		spellBookCountForFirstReduction = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		spellsPanel.add (spellBookCountForFirstReduction, "frmNewGameCustomSpellsBooksToObtainFirstReductionEdit");
		
		eachBookGives = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (eachBookGives, "frmNewGameCustomSpellsEachBook");
		
		castingCostReductionPrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (castingCostReductionPrefix, "frmNewGameCustomSpellsCastingReductionPrefix");

		castingCostReduction = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		spellsPanel.add (castingCostReduction, "frmNewGameCustomSpellsCastingReduction");

		castingCostReductionSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (castingCostReductionSuffix, "frmNewGameCustomSpellsCastingReductionSuffix");
		
		researchBonusPrefix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (researchBonusPrefix, "frmNewGameCustomSpellsResearchBonusPrefix");

		researchBonus = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		spellsPanel.add (researchBonus, "frmNewGameCustomSpellsResearchBonus");

		researchBonusSuffix = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (researchBonusSuffix, "frmNewGameCustomSpellsResearchBonusSuffix");

		castingCostReductionAdditive = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (castingCostReductionAdditive, "frmNewGameCustomSpellsCastingReductionCombinationAdd");

		castingCostReductionMultiplicative = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (castingCostReductionMultiplicative, "frmNewGameCustomSpellsCastingReductionCombinationMultiply");
		
		researchBonusAdditive = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (researchBonusAdditive, "frmNewGameCustomSpellsResearchBonusCombinationAdd");

		researchBonusMultiplicative = getUtils ().createImageCheckBox (MomUIConstants.GOLD, getSmallFont (), checkboxUnticked, checkboxTicked);
		spellsPanel.add (researchBonusMultiplicative, "frmNewGameCustomSpellsResearchBonusCombinationMultiply");
		
		castingCostReductionCapLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (castingCostReductionCapLabel, "frmNewGameCustomSpellsCastingReductionCap");

		castingCostReductionCap = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		spellsPanel.add (castingCostReductionCap, "frmNewGameCustomSpellsCastingReductionCapEdit");
		
		spellsPanel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "%"), "frmNewGameCustomSpellsP1");
		
		researchBonusCapLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		spellsPanel.add (researchBonusCapLabel, "frmNewGameCustomSpellsResearchBonusCap");

		researchBonusCap = getUtils ().createTextFieldWithBackgroundImage (MomUIConstants.SILVER, getSmallFont (), editboxSmall);
		spellsPanel.add (researchBonusCap, "frmNewGameCustomSpellsResearchBonusCapEdit");

		spellsPanel.add (getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont (), "%"), "frmNewGameCustomSpellsP2");
		
		final ButtonGroup switchResearchChoices = new ButtonGroup ();
		switchResearchChoices.add (switchResearchNo);
		switchResearchChoices.add (switchResearchNotStarted);
		switchResearchChoices.add (switchResearchLose);
		switchResearchChoices.add (switchResearchFreely);

		final ButtonGroup castingCostReductionChoices = new ButtonGroup ();
		castingCostReductionChoices.add (castingCostReductionAdditive);
		castingCostReductionChoices.add (castingCostReductionMultiplicative);

		final ButtonGroup researchBonusChoices = new ButtonGroup ();
		researchBonusChoices.add (researchBonusAdditive);
		researchBonusChoices.add (researchBonusMultiplicative);
		
		cards.add (spellsPanel, SPELLS_PANEL);

		// DEBUG OPTIONS PANEL
		debugPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutDebug ()));
		debugPanel.setOpaque (false);

		disableFogOfWar = getUtils ().createImageCheckBox (null, null, checkboxUnticked, checkboxTicked);
		debugPanel.add (disableFogOfWar, "frmNewGameCustomDebugDisableFogOfWarCheckbox");

		disableFogOfWarLabel = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		debugPanel.add (disableFogOfWarLabel, "frmNewGameCustomDebugDisableFogOfWar");
		
		cards.add (debugPanel, DEBUG_PANEL);
		
		// JOIN GAME PANEL
		refreshAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getClient ().getServerConnection ().sendMessageToServer (new RequestSessionList ());
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		joinPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutJoin ()));
		joinPanel.setOpaque (false);
		
		joinPanel.add (getUtils ().createImageButton (refreshAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmJoinGameRefresh");
		
		sessionsTable = new JTable ();
		sessionsTable.setModel (sessionsTableModel);
		sessionsTable.setFont (getSmallFont ());
		sessionsTable.setForeground (MomUIConstants.SILVER);
		sessionsTable.setBackground (new Color (0, 0, 0, 0));
		sessionsTable.getTableHeader ().setFont (getSmallFont ());
		sessionsTable.setOpaque (false);
		sessionsTable.setRowSelectionAllowed (true);
		sessionsTable.setColumnSelectionAllowed (false);
		
		final JScrollPane sessionsTablePane = new JScrollPane (sessionsTable);
		sessionsTablePane.getViewport ().setOpaque (false);
		joinPanel.add (sessionsTablePane, "frmJoinGameSessions");
		
		sessionsTable.getSelectionModel ().addListSelectionListener (new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				// Enable button as soon as a row is clicked on
				okAction.setEnabled (true);
			}
		});
		
		cards.add (joinPanel, JOIN_GAME_PANEL);
		
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
		
		customPortraitChooser = new JFileChooser ();
		customPortraitChooser.setAcceptAllFileFilterUsed (false);
		customPortraitChooser.addChoosableFileFilter (new ExtensionFileFilter (new String [] {"jpg", "jpeg", "gif", "png"}, "Image files"));
		customPortraitChooser.setAcceptAllFileFilterUsed (true);		// Add it again after, so we make sure it isn't the default
		
		// FLAG COLOUR PANEL (for custom wizards with custom portraits)
		flagColourPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutFlagColour ()));
		flagColourPanel.setOpaque (false);
		
		flagColourRedTitle = getUtils ().createLabel (Color.RED, getLargeFont ());
		flagColourPanel.add (flagColourRedTitle, "frmChooseFlagColourRedLabel");

		flagColourGreenTitle = getUtils ().createLabel (Color.GREEN, getLargeFont ());
		flagColourPanel.add (flagColourGreenTitle, "frmChooseFlagColourGreenLabel");
		
		flagColourBlueTitle = getUtils ().createLabel (Color.BLUE, getLargeFont ());
		flagColourPanel.add (flagColourBlueTitle, "frmChooseFlagColourBlueLabel");
		
		flagColourRed = new FlagColourSlider ();
		flagColourRed.setMaximum (255);
		flagColourPanel.add (flagColourRed, "frmChooseFlagColourRed");
		
		flagColourGreen = new FlagColourSlider ();
		flagColourGreen.setMaximum (255);
		flagColourPanel.add (flagColourGreen, "frmChooseFlagColourGreen");
		
		flagColourBlue = new FlagColourSlider ();
		flagColourBlue.setMaximum (255);
		flagColourPanel.add (flagColourBlue, "frmChooseFlagColourBlue");
		
		final ChangeListener flagColourChangeListener = new ChangeListener ()
		{
			@Override
			public final void stateChanged (final ChangeEvent ev)
			{
				try
				{
					updateCustomFlagColour ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		flagColourRed.addChangeListener (flagColourChangeListener);
		flagColourGreen.addChangeListener (flagColourChangeListener);
		flagColourBlue.addChangeListener (flagColourChangeListener);
		
		cards.add (flagColourPanel, FLAG_PANEL);
		
		// CUSTOM PICKS PANEL (for custom wizards)
		picksPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutPicks ()));
		picksPanel.setOpaque (false);
		
		cards.add (picksPanel, PICKS_PANEL);
		
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
		waitPanel = new JPanel (new XmlLayoutManager (getNewGameLayoutWait ()));
		waitPanel.setOpaque (false);
		
		final JTable playersTable = new JTable ();
		playersTable.setModel (playersTableModel);
		playersTable.setFont (getSmallFont ());
		playersTable.setForeground (MomUIConstants.SILVER);
		playersTable.setBackground (new Color (0, 0, 0, 0));
		playersTable.getTableHeader ().setFont (getSmallFont ());
		playersTable.setOpaque (false);
		playersTable.setRowSelectionAllowed (false);
		playersTable.setColumnSelectionAllowed (false);
		
		final JScrollPane playersTablePane = new JScrollPane (playersTable);
		playersTablePane.getViewport ().setOpaque (false);
		waitPanel.add (playersTablePane, "frmWaitForPlayersToJoinSessions");
		
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
				enableOrDisableNewGameOkButton ();
			}

			@Override
			public final void removeUpdate (final DocumentEvent e)
			{
				enableOrDisableNewGameOkButton ();
			}

			@Override
			public final void changedUpdate (final DocumentEvent e)
			{
				enableOrDisableNewGameOkButton ();
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

		log.trace ("Exiting init");
	}
	
	/**
	 * Show new game card, if they cancelled and then took option again
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void showNewGamePanel () throws IOException
	{
		log.trace ("Entering showNewGamePanel");
		
		playerName.setText (null);
		wizardPortrait.setIcon (null);
		flag1.setIcon (null);
		flag2.setIcon (null);
		retorts.setText (null);
		
		picks.clear ();
		updateBookshelfFromPicks ();
		
		currentMagicRealmID = null;
		cardLayout.show (cards, NEW_GAME_PANEL);

		log.trace ("Exiting showNewGamePanel");
	}
	
	/**
	 * After clicking OK on the new game panel, or one of the subsequent "custom details" screens, finds the next
	 * custom details screen we need to show, or if all done then we go ahead and send the session description
	 * to the server to start the game
	 * 
	 * @throws JAXBException If we go ahead creating the game, and there is a problem converting the object into XML
	 * @throws XMLStreamException If we go ahead creating the game, and there is a problem writing to the XML stream
	 * @throws MomException If we couldn't figure which panel was visible when the player clicked OK
	 */
	final void showNextNewGamePanel () throws JAXBException, XMLStreamException, MomException
	{
		log.trace ("Entering showNextNewGamePanel");

		// Which panel did we click OK on; basically this is "how many custom screens have we already OK'd"
		final int currentPanel;
		if (newGamePanel.isVisible ())
		{
			currentPanel = 0;
			populateNewGameFieldsFromSelectedOptions ();
		}
		else if (mapSizePanel.isVisible ())
			currentPanel = 1;
		else if (landProportionPanel.isVisible ())
			currentPanel = 2;
		else if (nodesPanel.isVisible ())
			currentPanel = 3;
		else if (difficulty1Panel.isVisible ())
			currentPanel = 4;
		else if (difficulty2Panel.isVisible ())
			currentPanel = 5;
		else if (difficulty3Panel.isVisible ())
			currentPanel = 6;
		else if (fogOfWarPanel.isVisible ())
			currentPanel = 7;
		else if (unitsPanel.isVisible ())
			currentPanel = 8;
		else if (spellsPanel.isVisible ())
			currentPanel = 9;
		else if (debugPanel.isVisible ())
			currentPanel = 10;
		else
			throw new MomException ("showNextNewGamePanel could not determine currently visible panel");
		
		// Customize map size
		if ((customizeMapSize.isSelected ()) && (currentPanel < 1))
			cardLayout.show (cards, MAP_SIZE_PANEL);
		
		// Customize land proportion
		else if ((customizeLandProportion.isSelected ()) && (currentPanel < 2))
			cardLayout.show (cards, LAND_PROPORTION_PANEL);
		
		// Customize nodes
		else if ((customizeNodes.isSelected ()) && (currentPanel < 3))
			cardLayout.show (cards, NODES_PANEL);
		
		// Customize difficulty (1 of 3)
		else if ((customizeDifficulty.isSelected ()) && (currentPanel < 4))
			cardLayout.show (cards, DIFFICULTY_1_PANEL);

		// Customize difficulty (2 of 3)
		else if ((customizeDifficulty.isSelected ()) && (currentPanel < 5))
			cardLayout.show (cards, DIFFICULTY_2_PANEL);

		// Customize difficulty (3 of 3)
		else if (((customizeDifficulty.isSelected ()) || (customizeNodes.isSelected ())) && (currentPanel < 6))
			cardLayout.show (cards, DIFFICULTY_3_PANEL);

		// Customize fog of war
		else if ((customizeFogOfWar.isSelected ()) && (currentPanel < 7))
			cardLayout.show (cards, FOG_OF_WAR_PANEL);

		// Customize units
		else if ((customizeUnits.isSelected ()) && (currentPanel < 8))
			cardLayout.show (cards, UNITS_PANEL);
		
		// Customize spells
		else if ((customizeSpells.isSelected ()) && (currentPanel < 9))
			cardLayout.show (cards, SPELLS_PANEL);
		
		// Debug options
		else if ((changeDebugOptionsAction.getSelectedItem ()) && (currentPanel < 10))
			cardLayout.show (cards, DEBUG_PANEL);
		
		// Start up game
		else
		{
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (getClient ().getOurPlayerID ());
			pd.setPlayerName (getClient ().getOurPlayerName ());
	
			final NewSession msg = new NewSession ();
			msg.setSessionDescription (buildSessionDescription ());
			msg.setPlayerDescription (pd);
	
			getClient ().getServerConnection ().sendMessageToServer (msg);

			// Only disable button if actually starting the game; otherwise OK is enabled for all the
			// "custom" screens because they can just to confirm settings as they are if desired
			okAction.setEnabled (false);
		}
		
		log.trace ("Exiting showNextNewGamePanel");
	}
	
	/**
	 * After we join a session, server sends us the database so then we know all things like
	 * all the wizards and retorts available, so can set up the controls for those
	 * 
	 * @throws RecordNotFoundException If we find a pick that doesn't have an entry in the graphics XML
	 */
	public final void afterJoinedSession () throws RecordNotFoundException
	{
		log.trace ("Entering afterJoinedSession");
		
		playerName.setText (getClient ().getOurPlayerName ());
		
		// Remove any old buttons leftover from a previous joining game
		for (final Component oldComponent : wizardComponents)
			wizardPanel.remove (oldComponent);

		for (final Component oldComponent : portraitComponents)
			portraitPanel.remove (oldComponent);

		for (final Component oldComponent : customPicksComponents)
			picksPanel.remove (oldComponent);
		
		for (final Component oldComponent : raceComponents)
			racePanel.remove (oldComponent);
		
		wizardComponents.clear ();
		wizardButtonActions.clear ();
		portraitComponents.clear ();
		portraitButtonActions.clear ();
		customPicksComponents.clear ();
		retortButtonActions.clear ();
		addBookActions.clear ();
		removeBookActions.clear ();
		bookshelfTitles.clear ();
		magicRealmBookshelves.clear ();
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
					final String wizardID = (wizard == null) ? "" : wizard.getWizardID ();
					
					// Choose wizard button
					final Action wizardButtonAction = new AbstractAction ()
					{
						@Override
						public final void actionPerformed (final ActionEvent ev)
						{
							wizardChosen = wizardID;
							okAction.setEnabled (true);
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
									final WizardGfx portrait = getGraphicsDB ().findWizard (wizard.getWizardID (), "NewGameUI.wizardButtonAction"); 
									wizardPortrait.setIcon (new ImageIcon (getUtils ().loadImage (portrait.getPortraitFile ()).getScaledInstance
										(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH)));
									
									updateFlagColour (Integer.parseInt (portrait.getFlagColour (), 16));
									
									for (final WizardPick src : wizard.getWizardPick ())
									{
										final PlayerPick dest = new PlayerPick ();
										dest.setPickID (src.getPick ());
										dest.setQuantity (src.getQuantity ());
										picks.add (dest);
									}
								}
								updateBookshelfFromPicks ();
								updateRetortsFromPicks (-1);
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
							portraitChosen = wizardID;
							try
							{
								if (wizard == null)
								{
									// Custom wizard - ask for filename
									if (customPortraitChooser.showOpenDialog (null) == JFileChooser.APPROVE_OPTION)
									{
										final BufferedImage customPortrait = ImageIO.read (customPortraitChooser.getSelectedFile ());
										final XmlLayoutComponent portraitSize = getNewGameLayoutMain ().findComponent ("frmNewGameLHSPhoto");
										if ((customPortrait.getWidth () > portraitSize.getWidth ()) || (customPortrait.getHeight () > portraitSize.getHeight ()))
										{
											final MessageBoxUI msgBox = getPrototypeFrameCreator ().createMessageBox ();
											msgBox.setTitleLanguageCategoryID ("frmChoosePortrait");
											msgBox.setTitleLanguageEntryID ("Title");
											msgBox.setTextLanguageCategoryID ("frmChoosePortrait");
											msgBox.setTextLanguageEntryID ("CustomTooLarge");
											msgBox.setVisible (true);
										}
										else
										{										
											wizardPortrait.setIcon (new ImageIcon (customPortrait));
											flag1.setIcon (null);
											flag2.setIcon (null);
											okAction.setEnabled (true);
										}
									}
								}
								else
								{
									final WizardGfx portrait = getGraphicsDB ().findWizard (wizard.getWizardID (), "NewGameUI.portraitButtonAction"); 
									wizardPortrait.setIcon (new ImageIcon (getUtils ().loadImage (portrait.getPortraitFile ()).getScaledInstance
										(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH)));

									updateFlagColour (Integer.parseInt (portrait.getFlagColour (), 16));
									okAction.setEnabled (true);
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
		
		// Deal with the 'each wizard only once' option
		enableOrDisableWizardButtons ();
		
		// CUSTOM PICKS PANEL (for custom wizards)
		// First we need to count how many bookshelves we need
		int bookshelfCount = 0;
		for (final Pick pick : getClient ().getClientDB ().getPick ())
		{
			final PickGfx pickGfx = getGraphicsDB ().findPick (pick.getPickID (), "afterJoinedSession");
			if (pickGfx.getBookImage ().size () > 0)
				bookshelfCount++;
		}
		
		int retortNo = 0;
		for (final Pick pick : getClient ().getClientDB ().getPick ())
		{
			final PickGfx pickGfx = getGraphicsDB ().findPick (pick.getPickID (), "afterJoinedSession");
			if (pickGfx.getBookImage ().size () == 0)
			{
				// Show as a retort label in the top half of the screen
				retortNo++;
				final ToggleAction retortAction = new ToggleAction ()
				{
					@Override
					protected final void selectedChanged ()
					{
						try
						{
							getPlayerPickUtils ().updatePickQuantity (picks, pick.getPickID (), isSelected () ? 1 : -1);
							updateCustomPicksCount ();
							updateRetortsFromPicks (-1);
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				};
				
				retortButtonActions.put (pick.getPickID (), retortAction);
				
				final JButton retortButton = getUtils ().createTextOnlyButton (retortAction, MomUIConstants.DULL_GOLD, getSmallFont ());
				picksPanel.add (retortButton, "frmCustomPicksRetort" + retortNo);
				customPicksComponents.add (retortButton);
				
				// Right clicking on reports gets help text
				retortButton.addMouseListener (new MouseAdapter ()
				{
					@Override
					public final void mouseClicked (final MouseEvent ev)
					{
						try
						{
							// Right clicking gets help text describing the retort
							if (SwingUtilities.isRightMouseButton (ev))
								getHelpUI ().showPickID (pick.getPickID ());
							
							// Clicking on disabled retorts explains why they're disabled
							else if (!retortAction.isEnabled ())
							{
								final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
								msg.setTitleLanguageCategoryID ("frmCustomPicks");
								msg.setTitleLanguageEntryID ("Title");
								
								final PickLang pickLang = getLanguage ().findPick (pick.getPickID ());
								final String pickDescription = (pickLang == null) ? null : pickLang.getPickDescriptionSingular ();
								
								final String languageEntryID;
								final int count = getPlayerPickUtils ().getTotalPickCost (picks, getClient ().getClientDB ());
								if (count == getClient ().getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks ())
									languageEntryID = "NoPicks";
								else if (count + pick.getPickCost () > getClient ().getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks ())
									languageEntryID = "InsufficientPicks";
								else
									languageEntryID = "Prerequisites";
								
								msg.setText (getLanguage ().findCategoryEntry ("frmCustomPicks", languageEntryID).replaceAll
									("PICK_COUNT", new Integer (pick.getPickCost ()).toString ()).replaceAll
									("PICK", (pickDescription != null) ? pickDescription : pick.getPickID ()).replaceAll
									("PREREQUISITES", getPlayerPickClientUtils ().describePickPreRequisites (pick)));
								
								msg.setVisible (true);
							}
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				});
			}
		}

		int bookshelfNo = 0;
		for (final Pick pick : getClient ().getClientDB ().getPick ())
		{
			final PickGfx pickGfx = getGraphicsDB ().findPick (pick.getPickID (), "afterJoinedSession");
			if (pickGfx.getBookImage ().size () > 0)
			{
				// Show as bookshelf
				bookshelfNo++;
				final JLabel thisImage = getUtils ().createImage ((bookshelfNo == bookshelfCount) ? bookshelfWithoutGargoyles : bookshelfWithGargoyles);
				picksPanel.add (thisImage, "frmCustomPicksBookshelf" + bookshelfNo);
				customPicksComponents.add (thisImage);
				
				final JPanel thisBookshelf = new JPanel (new GridBagLayout ());
				thisBookshelf.setOpaque (false);
				thisBookshelf.add (Box.createRigidArea (new Dimension (0, getNewGameLayoutPicks ().findComponent ("frmCustomPicksBooks" + bookshelfNo).getHeight ())));
				picksPanel.add (thisBookshelf, "frmCustomPicksBooks" + bookshelfNo);
				magicRealmBookshelves.put (pick.getPickID (), thisBookshelf);

				// Right clicking on the bookshels gets help text
				thisBookshelf.addMouseListener (new MouseAdapter ()
				{
					@Override
					public final void mouseClicked (final MouseEvent ev)
					{
						try
						{
							// Right clicking gets help text describing the realm of magic
							if (SwingUtilities.isRightMouseButton (ev))
								getHelpUI ().showPickID (pick.getPickID ());
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				});
				
				// Have to add title after panel, so it appears behind the books
				final Color magicRealmColour = new Color (Integer.parseInt (pickGfx.getPickBookshelfTitleColour (), 16));
				
				final JLabel bookshelfTitle = getUtils ().createLabel (magicRealmColour, getLargeFont ());
				picksPanel.add (bookshelfTitle, "frmCustomPicksBookshelfTitle" + bookshelfNo);
				bookshelfTitles.put (pick.getPickID (), bookshelfTitle);
				
				// Add a book of this type
				final Action addBookAction = new AbstractAction ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev)
					{
						try
						{
							getPlayerPickUtils ().updatePickQuantity (picks, pick.getPickID (), 1);
							updateCustomPicksCount ();
							updateBookshelfFromPicks ();
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				};
				
				addBookActions.put (pick.getPickID (), addBookAction);
				
				final JButton addBookButton = getUtils ().createImageButton (addBookAction, null, null, null, addBookNormal, addBookPressed, addBookNormal);
				picksPanel.add (addBookButton, "frmCustomPicksBookshelfAdd" + bookshelfNo);
				customPicksComponents.add (addBookButton);				
				
				// Remove a book of this type
				final Action removeBookAction = new AbstractAction ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev)
					{
						try
						{
							getPlayerPickUtils ().updatePickQuantity (picks, pick.getPickID (), -1);
							updateCustomPicksCount ();
							updateBookshelfFromPicks ();
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				};
				
				removeBookActions.put (pick.getPickID (), removeBookAction);
				
				final JButton removeBookButton = getUtils ().createImageButton (removeBookAction, null, null, null, removeBookNormal, removeBookPressed, removeBookNormal);
				picksPanel.add (removeBookButton, "frmCustomPicksBookshelfRemove" + bookshelfNo);
				customPicksComponents.add (removeBookButton);				
			}
		}
		
		// RACE SELECTION PANEL
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
							okAction.setEnabled (true);
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

		log.trace ("Exiting afterJoinedSession");
	}

	/**
	 * Enables or disables all the wizard selection buttons, on the chance that the "allow each wizard only once" option is ticked
	 */
	public final void enableOrDisableWizardButtons ()
	{
		log.trace ("Entering enableOrDisableWizardButtons");

		// First enable them all
		for (final Entry<String, Action> wizard : wizardButtonActions.entrySet ())
			wizard.getValue ().setEnabled (true);
		
		// Then if the option is chosen, disable actions for any already chosen wizards
		if (getClient ().getSessionDescription ().getDifficultyLevel ().isEachWizardOnlyOnce ())
		{
			for (final PlayerPublicDetails player : getClient ().getPlayers ())
			{
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
				System.out.println (player.getPlayerDescription ().getPlayerName () + " = " + pub.getWizardID ());
				if ((PlayerKnowledgeUtils.hasWizardBeenChosen (pub.getWizardID ())) && (PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) &&
					(!PlayerKnowledgeUtils.isCustomWizard (pub.getWizardID ())))
					
					wizardButtonActions.get (pub.getWizardID ()).setEnabled (false);
			}
			
			// If we've now got a wizard selected who's become disabled, better disable the OK button
			if ((okAction.isEnabled ()) && (!PlayerKnowledgeUtils.isCustomWizard (wizardChosen)) &&
				(!wizardButtonActions.get (wizardChosen).isEnabled ()))
				
				okAction.setEnabled (false);
		}
		
		log.trace ("Exiting enableOrDisableWizardButtons");
	}

	/**
	 * Show join game panel
	 */
	public final void showJoinGamePanel ()
	{
		log.trace ("Entering showJoinGamePanel");

		refreshAction.actionPerformed (null);
		cardLayout.show (cards, JOIN_GAME_PANEL);

		log.trace ("Exiting showJoinGamePanel");
	}
	
	/**
	 * Show portrait panel, if custom wizard was chosen
	 */
	public final void showPortraitPanel ()
	{
		log.trace ("Entering showPortraitPanel");

		cardLayout.show (cards, PORTRAIT_PANEL);

		log.trace ("Exiting showPortraitPanel");
	}

	/**
	 * Show flag colour panel, if custom wizard with custom portrait was chosen
	 * @throws PlayerNotFoundException If we cannot find the local player
	 */
	public final void showCustomFlagColourPanel () throws PlayerNotFoundException
	{
		log.trace ("Entering showCustomFlagColourPanel");

		updateCustomFlagColour ();
		okAction.setEnabled (true);		// Since we can just OK the default colour immediately if we wish
		cardLayout.show (cards, FLAG_PANEL);

		log.trace ("Exiting showCustomFlagColourPanel");
	}

	/**
	 * Show picks panel, if custom wizard was chosen
	 */
	public final void showCustomPicksPanel ()
	{
		log.trace ("Entering showCustomPicksPanel");

		cardLayout.show (cards, PICKS_PANEL);

		log.trace ("Exiting showCustomPicksPanel");
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
								okAction.setEnabled (isCorrectNumberOfFreeSpellsChosen ());
							}
						};
						freeSpellActions.put (spell, spellAction);
						
						final JButton spellButton = getUtils ().createTextOnlyButton (spellAction, MomUIConstants.DULL_GOLD, getSmallFont ());
						freeSpellsPanel.add (spellButton, getUtils ().createConstraintsNoFill (colNo, gridy + rowNo, 1, 1, NO_INSET, GridBagConstraintsNoFill.CENTRE));
						freeSpellsComponents.add (spellButton);
						
						// Right clicking on a spell displays help text for it
						spellButton.addMouseListener (new MouseAdapter ()
						{
							@Override
							public final void mouseClicked (final MouseEvent ev)
							{
								if (SwingUtilities.isRightMouseButton (ev))
								{
									try
									{
										final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID
											(getClient ().getPlayers (), getClient ().getOurPlayerID (), "NewGameUI.showSpellHelpText");
										
										getHelpUI ().showSpellID (spell.getSpellID (), ourPlayer);
									}
									catch (final Exception e)
									{
										log.error (e, e);
									}
								}
							}
						});
						
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
		log.trace ("Entering showRacePanel");
		
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

		log.trace ("Exiting showRacePanel");
	}
	
	/**
	 * Shows "wait for other players to join" panel
	 */
	public final void showWaitPanel ()
	{
		log.trace ("Entering showWaitPanel");

		playersTableModel.fireTableDataChanged ();
		cardLayout.show (cards, WAIT_PANEL);

		log.trace ("Exiting showWaitPanel");
	}
	
	/**
	 * Hook so the multiplayer layer can call into it when players join or leave
	 */
	public final void updateWaitPanelPlayersList ()
	{
		log.trace ("Entering updateWaitPanelPlayersList");

		playersTableModel.fireTableDataChanged ();

		log.trace ("Exiting updateWaitPanelPlayersList");
	}

	/**
	 * Ok button should only be enabled once we have enough info on the newGamePanel
	 */
	private final void enableOrDisableNewGameOkButton ()
	{
		log.trace ("Entering enableOrDisableNewGameOkButton");

		// This gets triggered during startup before both actions have been created
		final int totalOpponents = ((changeHumanOpponentsAction.getSelectedItem () == null) || (changeAIOpponentsAction.getSelectedItem () == null)) ? 0 :
			changeHumanOpponentsAction.getSelectedItem () + changeAIOpponentsAction.getSelectedItem ();
		
		okAction.setEnabled ((totalOpponents >= 1) && (totalOpponents <= 13) && (!gameName.getText ().trim ().equals ("")));

		log.trace ("Exiting enableOrDisableNewGameOkButton");
	}

	/**
	 * Keeps the flag colour preview up to date as the sliders move
	 * @throws PlayerNotFoundException If we cannot find the local player
	 */
	private final void updateCustomFlagColour () throws PlayerNotFoundException
	{
		log.trace ("Entering updateCustomFlagColour");

		updateFlagColour ((flagColourRed.getValue () << 16) + (flagColourGreen.getValue () << 8) + flagColourBlue.getValue ());

		log.trace ("Exiting updateCustomFlagColour");
	}
	
	/**
	 * This is called to recolour the two sample flags on the left hand side.  Its called regardless how the colour is set,
	 * via choosing a standard wizard, standard portrait, or dragging the custom flag colour sliders.
	 * 
	 * @param rgb New colour to set flags to
	 * @throws PlayerNotFoundException If we cannot find the local player
	 */
	private final void updateFlagColour (final int rgb) throws PlayerNotFoundException
	{
		log.trace ("Entering updateFlagColour: " + rgb);

		final BufferedImage wizardFlag = getUtils ().multiplyImageByColour (flag, rgb);
		flag1.setIcon (new ImageIcon (wizardFlag));
		flag2.setIcon (new ImageIcon (wizardFlag));
		
		// Store the flag colour temporarily to our player data.  The server will send this properly when the game starts up,
		// but we need it before then for right clicking on the free spell selection screen on overland spells, to get the
		// mirror the right colour.
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "updateFlagColour");
		final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) ourPlayer.getTransientPlayerPublicKnowledge ();

		String value = Integer.toHexString (rgb);
		while (value.length () < 6)
			value = "0" + value;
		
		trans.setFlagColour (value);
		
		log.trace ("Exiting updateFlagColour");
	}
	
	/**
	 * @param slider Slider to read value from
	 * @return Value converted to hex string
	 */
	private final String sliderToHex (final JSlider slider)
	{
		String value = Integer.toHexString (slider.getValue ());
		
		// The main point of having this as a method is to ensure that the output is 2 chars long
		while (value.length () < 2)
			value = "0" + value;
		
		return value.toUpperCase ();
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
		customizeLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGame", "Customize"));
		
		changeTurnSystemAction.clearItems ();
		for (final TurnSystem turnSystem : TurnSystem.values ())
			changeTurnSystemAction.addItem (turnSystem, getLanguage ().findCategoryEntry ("NewGameFormTurnSystems", turnSystem.name ()));
		
		changeDebugOptionsAction.clearItems ();
		for (final boolean debugOptions : new boolean [] {false, true})
			changeDebugOptionsAction.addItem (debugOptions, getLanguage ().findCategoryEntry ("XsdBoolean", new Boolean (debugOptions).toString ().toLowerCase ()));

		// CUSTOM MAP SIZE PANEL
		mapSizeEdit.setText									(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "MapSize"));
		mapWrapsLeftToRight.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "WrapsLeftRight"));
		mapWrapsTopToBottom.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "WrapsTopBottom"));
		mapZonesLabel.setText								(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "Zones"));
		towersOfWizardryLabel.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "Towers"));
		towersOfWizardrySeparationLabel.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "TowersSeparation"));
		continentalRaceChanceLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "Continental"));
		citySeparationLabel.setText							(getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "CitySeparation"));
		riverCountLabel.setText								(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "Rivers"));
		raiderCityCountLabel.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCityCount"));
		arcanusNodeCountLabel.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "ArcanusCount"));
		myrrorNodeCountLabel.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "MyrrorCount"));
		
		// CUSTOM LAND PROPORTION PANEL
		landPercentageLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "PercentageMapIsLand"));
		hillsPercentageLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "HillsProportion"));
		mountainsPercentageLabel.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "MountainsProportion"));
		treesPercentageLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "TreesProportion"));
		treeAreaSizePrefix.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "TreeAreaTileCountPrefix"));
		treeAreaSizeSuffix.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "TreeAreaTileCountSuffix"));
		desertsPercentageLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "DesertProportion"));
		desertAreaSizePrefix.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "DesertAreaTileCountPrefix"));
		desertAreaSizeSuffix.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "DesertAreaTileCountSuffix"));
		swampsPercentageLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "SwampProportion"));
		swampAreaSizePrefix.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "SwampAreaTileCountPrefix"));
		swampAreaSizeSuffix.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "SwampAreaTileCountSuffix"));
		tundraDistancePrefix.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "TundraPrefix"));
		tundraDistanceSuffix.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "TundraSuffix"));
		arcanusMineralChancePrefix.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "ArcanusPrefix"));
		arcanusMineralChanceSuffix.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "ArcanusSuffix"));
		myrrorMineralChancePrefix.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "MyrrorPrefix"));
		myrrorMineralChanceSuffix.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "MyrrorSuffix"));
		
		// CUSTOM NODES PANEL
		doubleNodeAuraMagicPowerPrefix.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "MagicPowerPrefix"));
		doubleNodeAuraMagicPowerSuffix.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "MagicPowerSuffix"));
		arcanusNodeSizePrefix.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "ArcanusNodeAuraPrefix"));
		arcanusNodeSizeSuffix.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "ArcanusNodeAuraSuffix"));
		myrrorNodeSizePrefix.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "MyrrorNodeAuraPrefix"));
		myrrorNodeSizeSuffix.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "MyrrorNodeAuraSuffix"));
		
		// CUSTOM DIFFICULTY PANEL (1 of 3)
		spellPicksLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "SpellPicks"));
		humanSpellPicksLabel.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "HumanSpellPicks"));
		aiSpellPicksLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "AISpellPicks"));
		startingGoldLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "Gold"));
		humanStartingGoldLabel.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "HumanGold"));
		aiStartingGoldLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "AIGold"));
		allowCustomWizards.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "CustomWizards"));
		eachWizardOnlyOnce.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "EachWizardOnlyOnce"));
		wizardCityStartSizeLabel.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "WizardCitySize"));
		maxCitySizeLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "MaxCitySize"));
		raiderCityStartSizeLabel.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCitySizePrefix"));
		raiderCityStartSizeAnd.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCitySizeAnd"));
		raiderCitySizeCapPrefix.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCityGrowthPrefix"));
		raiderCitySizeCapSuffix.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "RaiderCityGrowthSuffix"));
		
		// CUSTOM DIFFICULTY PANEL (2 of 3)
		towersMonsters.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "TowerMonsters"));
		towersTreasure.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "TowerTreasure"));
		normalLairCountLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalLairCount"));
		arcanusNormalLairMonsters.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalArcanusLairMonsters"));
		arcanusNormalLairTreasure.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalArcanusLairTreasure"));
		myrrorNormalLairMonsters.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalMyrrorLairMonsters"));
		myrrorNormalLairTreasure.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "NormalMyrrorLairTreasure"));
		weakLairCountLabel.setText			(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakLairCount"));
		arcanusWeakLairMonsters.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakArcanusLairMonsters"));
		arcanusWeakLairTreasure.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakArcanusLairTreasure"));
		myrrorWeakLairMonsters.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakMyrrorLairMonsters"));
		myrrorWeakLairTreasure.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "WeakMyrrorLairTreasure"));
		
		// CUSTOM DIFFICULTY PANEL (3 of 3)
		arcanusNodeMonsters.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty3", "ArcanusNodeMonsters"));
		arcanusNodeTreasure.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty3", "ArcanusNodeTreasure"));
		myrrorNodeMonsters.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty3", "MyrrorNodeMonsters"));
		myrrorNodeTreasure.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty3", "MyrrorNodeTreasure"));
		
		// CUSTOM FOG OF WAR PANEL
		fowTerrain.setText								(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainNodesAuras"));
		fowTerrainAlways.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainAlways"));
		fowTerrainRemember.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainRemember"));
		fowTerrainForget.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "TerrainForget"));
		fowCities.setText								(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesSpellsCAEs"));
		fowCitiesAlways.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesAlways"));
		fowCitiesRemember.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesRemember"));
		fowCitiesForget.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "CitiesForget"));
		canSeeEnemyCityConstruction.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "Constructing"));
		fowUnits.setText									(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "Units"));
		fowUnitsAlways.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "UnitsAlways"));
		fowUnitsRemember.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "UnitsRemember"));
		fowUnitsForget.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "UnitsForget"));
		
		// CUSTOM UNIT SETTINGS PANEL
		maxUnitsPerGridCellLabel.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomUnits", "MaxPerGridCell"));
		exceedMaxUnitsDuringCombatLabel.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomUnits", "CanExceedMaximumUnitsDuringCombat"));
		maximumHeroesLabel.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomUnits", "MaxHeroes"));
		rollHeroSkillsAtStartLabel.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomUnits", "RollHeroSkillsAtStartOfGame"));

		// CUSTOM SPELL SETTINGS PANEL
		switchResearch.setText									(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearch"));
		switchResearchNo.setText								(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchNo"));
		switchResearchNotStarted.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchNotStarted"));
		switchResearchLose.setText							(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchLose"));
		switchResearchFreely.setText							(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "SwitchResearchFreely"));
		spellBookCountForFirstReductionLabel.setText	(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "BooksToObtainFirstReduction"));
		eachBookGives.setText									(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "EachBook"));
		castingCostReductionPrefix.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionPrefix"));
		castingCostReductionSuffix.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionSuffix"));
		researchBonusPrefix.setText							(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusPrefix"));
		researchBonusSuffix.setText							(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusSuffix"));
		castingCostReductionAdditive.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionCombinationAdd"));
		castingCostReductionMultiplicative.setText		(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionCombinationMultiply"));
		researchBonusAdditive.setText							(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusCombinationAdd"));
		researchBonusMultiplicative.setText					(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusCombinationMultiply"));
		castingCostReductionCapLabel.setText				(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "CastingReductionCap"));
		researchBonusCapLabel.setText						(getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "ResearchBonusCap"));
		
		// DEBUG OPTIONS PANEL
		disableFogOfWarLabel.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDebug", "DisableFogOfWar"));
		
		// JOIN GAME PANEL
		refreshAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmJoinGame", "Refresh"));
		sessionsTableModel.fireTableDataChanged ();
		
		// FLAG COLOUR PANEL (for custom wizards with custom portraits)
		flagColourRedTitle.setText		(getLanguage ().findCategoryEntry ("frmChooseFlagColour", "RedLabel"));
		flagColourGreenTitle.setText	(getLanguage ().findCategoryEntry ("frmChooseFlagColour", "GreenLabel"));
		flagColourBlueTitle.setText		(getLanguage ().findCategoryEntry ("frmChooseFlagColour", "BlueLabel"));
		
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
			updateRetortsFromPicks (-1);
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
		else if (mapSizePanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomMapSize", "Title"));
		else if (landProportionPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomLandProportion", "Title"));
		else if (nodesPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomNodes", "Title"));
		else if (difficulty1Panel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty1", "Title"));
		else if (difficulty2Panel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty2", "Title"));
		else if (difficulty3Panel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDifficulty3", "Title"));
		else if (fogOfWarPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomFogOfWar", "Title"));
		else if (unitsPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomUnits", "Title"));
		else if (spellsPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomSpells", "Title"));
		else if (debugPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmNewGameCustomDebug", "Title"));		
		else if (joinPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmJoinGame", "SelectGame"));		
		else if (wizardPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmChooseWizard", "Title"));
		else if (portraitPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmChoosePortrait", "Title"));
		else if (flagColourPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmChooseFlagColour", "Title"));
		else if (picksPanel.isVisible ())
		{
			try
			{
				updateCustomPicksCount ();
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		}
		else if (racePanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmChooseRace", "Title"));
		else if (waitPanel.isVisible ())
			title.setText (getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Title"));
		else if (freeSpellsPanel.isVisible ())
		{
			// "Choose Life Spells" title
			final PickLang currentMagicRealm = getLanguage ().findPick (currentMagicRealmID);
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
		log.trace ("Entering selectedDatabaseOrLanguageChanged");

		// Reload the available selections of all the buttons
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
		
		// Select the default values for each button
		final NewGameDefaults defaults = changeDatabaseAction.getSelectedItem ().getNewGameDefaults ();
		if (defaults != null)
		{
			while ((defaults.getDefaultMapSizeID () != null) && (!defaults.getDefaultMapSizeID ().equals (changeMapSizeAction.getSelectedItem ().getMapSizeID ())))
				changeMapSizeAction.actionPerformed (null);

			while ((defaults.getDefaultLandProportionID () != null) && (!defaults.getDefaultLandProportionID ().equals (changeLandProportionAction.getSelectedItem ().getLandProportionID ())))
				changeLandProportionAction.actionPerformed (null);

			while ((defaults.getDefaultNodeStrengthID () != null) && (!defaults.getDefaultNodeStrengthID ().equals (changeNodeStrengthAction.getSelectedItem ().getNodeStrengthID ())))
				changeNodeStrengthAction.actionPerformed (null);

			while ((defaults.getDefaultDifficultyLevelID () != null) && (!defaults.getDefaultDifficultyLevelID ().equals (changeDifficultyLevelAction.getSelectedItem ().getDifficultyLevelID ())))
				changeDifficultyLevelAction.actionPerformed (null);

			while ((defaults.getDefaultFogOfWarSettingID () != null) && (!defaults.getDefaultFogOfWarSettingID ().equals (changeFogOfWarSettingsAction.getSelectedItem ().getFogOfWarSettingID ())))
				changeFogOfWarSettingsAction.actionPerformed (null);

			while ((defaults.getDefaultUnitSettingID () != null) && (!defaults.getDefaultUnitSettingID ().equals (changeUnitSettingsAction.getSelectedItem ().getUnitSettingID ())))
				changeUnitSettingsAction.actionPerformed (null);

			while ((defaults.getDefaultSpellSettingID () != null) && (!defaults.getDefaultSpellSettingID ().equals (changeSpellSettingsAction.getSelectedItem ().getSpellSettingID ())))
				changeSpellSettingsAction.actionPerformed (null);
		}

		log.trace ("Exiting selectedDatabaseOrLanguageChanged");
	}
	
	/**
	 * When the selected picks change, update the books on the bookshelf
	 * @throws IOException If there is a problem loading any of the book images
	 */
	private final void updateBookshelfFromPicks () throws IOException
	{
		log.trace ("Entering updateBookshelfFromPicks");

		// Remove all the old books; try to remove from all bookshelves since we only keep one list so we don't know which bookshelf this image is on
		for (final JLabel oldBook : bookImages)
		{
			bookshelf.remove (oldBook);
			for (final JPanel thisBookshelf : magicRealmBookshelves.values ())
				thisBookshelf.remove (oldBook);
		}
		
		bookImages.clear ();
		
		// Generate new images
		int mergedBookshelfGridx = 0;
		final Map<String, Integer> magicRealmBookshelvesGridx = new HashMap<String, Integer> (); 
		
		for (final PlayerPick pick : picks)
		{
			// Pick must exist in the graphics XML file, but may not have any image(s)
			final PickGfx pickGfx = getGraphicsDB ().findPick (pick.getPickID (), "NewGameUI.updateBookshelfFromPicks");
			if (pickGfx.getBookImage ().size () > 0)
				for (int n = 0; n < pick.getQuantity (); n++)
				{
					// Choose random image for the pick
					final BufferedImage bookImage = getUtils ().loadImage (pickGfx.chooseRandomBookImageFilename ());
					
					// Add on merged bookshelf
					mergedBookshelfGridx++;
					final JLabel mergedBookshelfImg = getUtils ().createImage (bookImage);
					bookshelf.add (mergedBookshelfImg, getUtils ().createConstraintsNoFill (mergedBookshelfGridx, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.SOUTH));
					bookImages.add (mergedBookshelfImg);
					
					// Add on bookshelf for this pick type
					Integer magicRealmBookshelfGridx = magicRealmBookshelvesGridx.get (pick.getPickID ());
					if (magicRealmBookshelfGridx == null)
						magicRealmBookshelfGridx = 0;
					
					magicRealmBookshelfGridx++;
					magicRealmBookshelvesGridx.put (pick.getPickID (), magicRealmBookshelfGridx);

					final JLabel magicRealmBookshelfImg = getUtils ().createImage (bookImage);
					magicRealmBookshelves.get (pick.getPickID ()).add (magicRealmBookshelfImg,
						getUtils ().createConstraintsNoFill (magicRealmBookshelfGridx, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.SOUTH));
					bookImages.add (magicRealmBookshelfImg);
				}
		}
		
		// Redrawing only the bookshelf isn't enough, because the new books might be smaller than before so only the smaller so
		// bookshelf.validate only redraws the new smaller area and leaves bits of the old books showing
		contentPane.validate ();
		contentPane.repaint ();

		log.trace ("Exiting updateBookshelfFromPicks");
	}

	/**
	 * When the selected picks (or language) change, update the retort descriptions
	 * 
	 * @param charIndex Index into the generated text that we want to locate and get in the return param; -1 if we don't care about the return param
	 * @return PickID of the pick at the requested charIndex; -1 if the charIndex is outside of the text or doesn't represent a pick (i.e. is one of the commas)
	 * @throws RecordNotFoundException If one of the picks we have isn't in the graphics XML file
	 */
	private final String updateRetortsFromPicks (final int charIndex) throws RecordNotFoundException
	{
		log.trace ("Entering updateRetortsFromPicks");
		
		final StringBuffer desc = new StringBuffer ();
		String result = null;
		for (final PlayerPick pick : picks)
		{
			// Pick must exist in the graphics XML file, but may not have any image(s)
			if (getGraphicsDB ().findPick (pick.getPickID (), "NewGameUI.updateRetortsFromPicks").getBookImage ().size () == 0)
			{
				if (desc.length () > 0)
					desc.append (", ");
				
				if (pick.getQuantity () > 1)
					desc.append (pick.getQuantity () + "x");

				final PickLang pickDesc = getLanguage ().findPick (pick.getPickID ());
				final String thisPickText;
				if (pickDesc == null)
					thisPickText = pick.getPickID ();
				else
					thisPickText = pickDesc.getPickDescriptionSingular ();

				// Does the required index fall within the text for this pick?
				if ((charIndex >= desc.length ()) && (charIndex < desc.length () + thisPickText.length ()))
					result = pick.getPickID ();
				
				// Now add it
				desc.append (thisPickText);
			}
		}
		retorts.setText (desc.toString ());

		log.trace ("Exiting updateRetortsFromPicks = " + result);
		return result;
	}	
	
	/**
	 * Update all labels and buttons that are only dynamically created after we join a game
	 */
	private final void languageChangedAfterInGame ()
	{
		log.trace ("Entering languageChangedAfterInGame");
		
		// Choose wizard buttons
		for (final Entry<String, Action> wizard : wizardButtonActions.entrySet ())
			if (PlayerKnowledgeUtils.isCustomWizard (wizard.getKey ()))
				wizard.getValue ().putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmChooseWizard", "Custom"));
			else
				wizard.getValue ().putValue (Action.NAME, getLanguage ().findWizardName (wizard.getKey ()));
		
		// Choose portrait buttons
		for (final Entry<String, Action> portrait : portraitButtonActions.entrySet ())
			if (PlayerKnowledgeUtils.isCustomWizard (portrait.getKey ()))
				portrait.getValue ().putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmChoosePortrait", "Custom"));
			else
				portrait.getValue ().putValue (Action.NAME, getLanguage ().findWizardName (portrait.getKey ()));
		
		// Retort buttons
		for (final Entry<String, ToggleAction> retort : retortButtonActions.entrySet ())
		{
			final PickLang pick = getLanguage ().findPick (retort.getKey ());
			final String pickDescription = (pick == null) ? null : pick.getPickDescriptionSingular ();
			retort.getValue ().putValue (Action.NAME, (pickDescription != null) ? pickDescription : retort.getKey ());
		}
		
		// Bookshelf titles
		for (final Entry<String, JLabel> bookshelfTitle : bookshelfTitles.entrySet ())
		{
			final PickLang pick = getLanguage ().findPick (bookshelfTitle.getKey ());
			final String pickDescription = (pick == null) ? null : pick.getBookshelfDescription ();
			bookshelfTitle.getValue ().setText ((pickDescription != null) ? pickDescription : bookshelfTitle.getKey ());
		}
		
		// Race plane titles
		for (final Entry<Integer, JLabel> planeLabel : racePlanes.entrySet ())
		{
			final PlaneLang plane = getLanguage ().findPlane (planeLabel.getKey ());
			planeLabel.getValue ().setText ((plane == null) ? planeLabel.getKey ().toString () : plane.getPlaneRacesTitle ());
		}
		
		// Choose race buttons
		for (final Entry<Race, Action> raceAction : raceButtonActions.entrySet ())
		{
			final RaceLang race = getLanguage ().findRace (raceAction.getKey ().getRaceID ());
			raceAction.getValue ().putValue (Action.NAME, (race == null) ? raceAction.getKey ().getRaceID () : race.getRaceName ());
		}

		log.trace ("Exiting languageChangedAfterInGame");
	}
	
	/**
	 * Recount how many custom picks we've chosen so far, e.g. 5/11, and enable/disable all buttons appropriately.
	 * e.g. once we reach 9/11 we disable the Myrran button since it costs 3 picks.
	 * 
	 * @throws RecordNotFoundException If we counter a button for a pick that we can't find in the DB
	 */
	private final void updateCustomPicksCount () throws RecordNotFoundException
	{
		log.trace ("Entering updateCustomPicksCount");
		
		// Count retorts and books
		final int count = getPlayerPickUtils ().getTotalPickCost (picks, getClient ().getClientDB ());
		
		// Update counter in title
		final int totalPicks = getClient ().getSessionDescription ().getDifficultyLevel ().getHumanSpellPicks ();
		title.setText (getLanguage ().findCategoryEntry ("frmCustomPicks", "Title") + ": " + count + "/" + totalPicks);

		// Enable or disable retort buttons
		for (final Entry<String, ToggleAction> retort : retortButtonActions.entrySet ())
			retort.getValue ().setEnabled (retort.getValue ().isSelected () ||
				((count + getClient ().getClientDB ().findPick (retort.getKey (), "updateCustomPicksCount").getPickCost () <= totalPicks) &&
				(getPlayerPickUtils ().meetsPickRequirements (retort.getKey (), picks, getClient ().getClientDB ()))));

		// Enable or disable add book buttons
		for (final Entry<String, Action> pick : addBookActions.entrySet ())
			pick.getValue ().setEnabled ((count + getClient ().getClientDB ().findPick (pick.getKey (), "updateCustomPicksCount").getPickCost () <= totalPicks) &&
				(getPlayerPickUtils ().canSafelyAdd (pick.getKey (), picks, getClient ().getClientDB ())));
		
		// Enable or disable remove book buttons
		for (final Entry<String, Action> pick : removeBookActions.entrySet ())
			pick.getValue ().setEnabled (getPlayerPickUtils ().canSafelyRemove (pick.getKey (), picks, getClient ().getClientDB ()));

		// Now we've set the state of the actions correctly, colour the buttons to match
		for (final Component comp : customPicksComponents)
			if (comp instanceof JButton)
			{
				final JButton button = (JButton) comp;
				if (button.getAction () instanceof ToggleAction)
				{
					// Retort
					final ToggleAction retortAction = (ToggleAction) button.getAction ();
					
					if (retortAction.isEnabled ())
						button.setForeground (retortAction.isSelected () ? MomUIConstants.GOLD : MomUIConstants.DULL_GOLD);
					else
						button.setForeground (MomUIConstants.GRAY);
				}
				else
				{
					// Add or remove book buttons
					button.setVisible (button.getAction ().isEnabled ());
				}
			}

		okAction.setEnabled (count == totalPicks);
		log.trace ("Exiting updateCustomPicksCount");
	}
	
	/**
	 * Choose initial spells title depends on current magic realm; sets the title for that and the names of all 40 spells
	 */
	private final void setCurrentMagicRealmSpellNames ()
	{
		log.trace ("Entering setCurrentMagicRealmSpellNames");
		
		// Names of every spell
		for (final Entry<Spell, ToggleAction> spellAction : freeSpellActions.entrySet ())
		{
			final SpellLang spell = getLanguage ().findSpell (spellAction.getKey ().getSpellID ());
			spellAction.getValue ().putValue (Action.NAME, (spell == null) ? spellAction.getKey ().getSpellID () : spell.getSpellName ());			
		}

		log.trace ("Exiting setCurrentMagicRealmSpellNames");
	}
	
	/**
	 * Update spell rank titles that include how many free spells we have left to choose, like Uncommon: 8/10
	 */
	private final void updateInitialSpellsCount ()
	{
		log.trace ("Entering updateInitialSpellsCount");
		
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

		log.trace ("Exiting updateInitialSpellsCount");
	}
	
	/**
	 * @return True only if exactly the right number of spells in all ranks have been chosen
	 */
	private final boolean isCorrectNumberOfFreeSpellsChosen ()
	{
		log.trace ("Entering isCorrectNumberOfFreeSpellsChosen");
		
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
		
		log.trace ("Exiting isCorrectNumberOfFreeSpellsChosen = " + allOk);
		return allOk;
	}
	
	/**
	 * Populates all the custom new game forms according to what options were chosen on the new game screen 
	 */
	private final void populateNewGameFieldsFromSelectedOptions ()
	{
		log.trace ("Entering populateNewGameFieldsFromSelectedOptions");
		
		// Map size
		final MapSizeData mapSize = changeMapSizeAction.getSelectedItem ();
		mapSizeWidth.setText						(new Integer (mapSize.getWidth ()).toString ());
		mapSizeHeight.setText						(new Integer (mapSize.getHeight ()).toString ());
		mapWrapsLeftToRight.setSelected		(mapSize.isWrapsLeftToRight ());
		mapWrapsTopToBottom.setSelected	(mapSize.isWrapsTopToBottom ());
		mapZoneWidth.setText						(new Integer (mapSize.getZoneWidth ()).toString ());
		mapZoneHeight.setText						(new Integer (mapSize.getZoneHeight ()).toString ());
		towersOfWizardryCount.setText			(new Integer (mapSize.getTowersOfWizardryCount ()).toString ());
		towersOfWizardrySeparation.setText	(new Integer (mapSize.getTowersOfWizardrySeparation ()).toString ());
		continentalRaceChance.setText			(new Integer (mapSize.getContinentalRaceChance ()).toString ());
		citySeparation.setText							(new Integer (mapSize.getCitySeparation ()).toString ());
		riverCount.setText								(new Integer (mapSize.getRiverCount ()).toString ());
		raiderCityCount.setText						(new Integer (mapSize.getRaiderCityCount ()).toString ());
		
		for (final MapSizePlane plane : mapSize.getMapSizePlane ())
			if (plane.getPlaneNumber () == 0)
				arcanusNodeCount.setText (new Integer (plane.getNumberOfNodesOnPlane ()).toString ());
			else
				myrrorNodeCount.setText (new Integer (plane.getNumberOfNodesOnPlane ()).toString ());
	    
		// Land proportion
		final LandProportionData landProportion = changeLandProportionAction.getSelectedItem ();
		landPercentage.setText			(new Integer (landProportion.getPercentageOfMapIsLand ()).toString ());
		hillsPercentage.setText			(new Integer (landProportion.getPercentageOfLandIsHills ()).toString ());
		mountainsPercentage.setText	(new Integer (landProportion.getPercentageOfHillsAreMountains ()).toString ());
		tundraDistance.setText			(new Integer (landProportion.getTundraRowCount ()).toString ());
		
		for (final LandProportionTileType tileType : landProportion.getLandProportionTileType ())
			if (tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_FOREST))
			{
				treesPercentage.setText (new Integer (tileType.getPercentageOfLand ()).toString ());
				treeAreaSize.setText (new Integer (tileType.getEachAreaTileCount ()).toString ());
			}
			else if (tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_DESERT))
			{
				desertsPercentage.setText (new Integer (tileType.getPercentageOfLand ()).toString ());
				desertAreaSize.setText (new Integer (tileType.getEachAreaTileCount ()).toString ());
			}
			else if (tileType.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_SWAMP))
			{
				swampsPercentage.setText (new Integer (tileType.getPercentageOfLand ()).toString ());
				swampAreaSize.setText (new Integer (tileType.getEachAreaTileCount ()).toString ());
			}
		
		for (final LandProportionPlane plane : landProportion.getLandProportionPlane ())
			if (plane.getPlaneNumber () == 0)
				arcanusMineralChance.setText (new Integer (plane.getFeatureChance ()).toString ());
			else
				myrrorMineralChance.setText (new Integer (plane.getFeatureChance ()).toString ());

		// Node strength
		final NodeStrengthData nodeStrength = changeNodeStrengthAction.getSelectedItem ();
		doubleNodeAuraMagicPower.setText (new Integer (nodeStrength.getDoubleNodeAuraMagicPower ()).toString ());
		
		for (final NodeStrengthPlane plane : nodeStrength.getNodeStrengthPlane ())
			if (plane.getPlaneNumber () == 0)
			{
				arcanusNodeSizeMin.setText (new Integer (plane.getNodeAuraSquaresMinimum ()).toString ());
				arcanusNodeSizeMax.setText (new Integer (plane.getNodeAuraSquaresMaximum ()).toString ());
			}
			else
			{
				myrrorNodeSizeMin.setText (new Integer (plane.getNodeAuraSquaresMinimum ()).toString ());
				myrrorNodeSizeMax.setText (new Integer (plane.getNodeAuraSquaresMaximum ()).toString ());
			}
		
		// Difficulty level
		final DifficultyLevelData difficultyLevel = changeDifficultyLevelAction.getSelectedItem ();
		humanSpellPicks.setText					(new Integer (difficultyLevel.getHumanSpellPicks ()).toString ());
		aiSpellPicks.setText						(new Integer (difficultyLevel.getAiSpellPicks ()).toString ());
		humanStartingGold.setText				(new Integer (difficultyLevel.getHumanStartingGold ()).toString ());
		aiStartingGold.setText						(new Integer (difficultyLevel.getAiStartingGold ()).toString ());
		allowCustomWizards.setSelected		(difficultyLevel.isCustomWizards ());
		eachWizardOnlyOnce.setSelected		(difficultyLevel.isEachWizardOnlyOnce ());
		wizardCityStartSize.setText				(new Integer (difficultyLevel.getWizardCityStartSize ()).toString ());
		maxCitySize.setText						(new Integer (difficultyLevel.getCityMaxSize ()).toString ());
		raiderCityStartSizeMin.setText			(new Integer (difficultyLevel.getRaiderCityStartSizeMin ()).toString ());
		raiderCityStartSizeMax.setText		(new Integer (difficultyLevel.getRaiderCityStartSizeMax ()).toString ());
		raiderCitySizeCap.setText				(new Integer (difficultyLevel.getRaiderCityGrowthCap ()).toString ());
		towersMonstersMin.setText				(new Integer (difficultyLevel.getTowerMonstersMinimum ()).toString ());
		towersMonstersMax.setText			(new Integer (difficultyLevel.getTowerMonstersMaximum ()).toString ());
		towersTreasureMin.setText				(new Integer (difficultyLevel.getTowerTreasureMinimum ()).toString ());
		towersTreasureMax.setText			(new Integer (difficultyLevel.getTowerTreasureMaximum ()).toString ());
		normalLairCount.setText					(new Integer (difficultyLevel.getNormalLairCount ()).toString ());
		weakLairCount.setText					(new Integer (difficultyLevel.getWeakLairCount ()).toString ());
		
		for (final DifficultyLevelPlane plane : difficultyLevel.getDifficultyLevelPlane ())
			if (plane.getPlaneNumber () == 0)
			{
				arcanusNormalLairMonstersMin.setText	(new Integer (plane.getNormalLairMonstersMinimum ()).toString ());
				arcanusNormalLairMonstersMax.setText	(new Integer (plane.getNormalLairMonstersMaximum ()).toString ());
				arcanusNormalLairTreasureMin.setText	(new Integer (plane.getNormalLairTreasureMinimum ()).toString ());
				arcanusNormalLairTreasureMax.setText	(new Integer (plane.getNormalLairTreasureMaximum ()).toString ());
				arcanusWeakLairMonstersMin.setText		(new Integer (plane.getWeakLairMonstersMinimum ()).toString ());
				arcanusWeakLairMonstersMax.setText		(new Integer (plane.getWeakLairMonstersMaximum ()).toString ());
				arcanusWeakLairTreasureMin.setText		(new Integer (plane.getWeakLairTreasureMinimum ()).toString ());
				arcanusWeakLairTreasureMax.setText		(new Integer (plane.getWeakLairTreasureMaximum ()).toString ());
			}
			else
			{
				myrrorNormalLairMonstersMin.setText		(new Integer (plane.getNormalLairMonstersMinimum ()).toString ());
				myrrorNormalLairMonstersMax.setText		(new Integer (plane.getNormalLairMonstersMaximum ()).toString ());
				myrrorNormalLairTreasureMin.setText		(new Integer (plane.getNormalLairTreasureMinimum ()).toString ());
				myrrorNormalLairTreasureMax.setText		(new Integer (plane.getNormalLairTreasureMaximum ()).toString ());
				myrrorWeakLairMonstersMin.setText		(new Integer (plane.getWeakLairMonstersMinimum ()).toString ());
				myrrorWeakLairMonstersMax.setText		(new Integer (plane.getWeakLairMonstersMaximum ()).toString ());
				myrrorWeakLairTreasureMin.setText		(new Integer (plane.getWeakLairTreasureMinimum ()).toString ());
				myrrorWeakLairTreasureMax.setText		(new Integer (plane.getWeakLairTreasureMaximum ()).toString ());
			}
		
		// Node difficulty
		// Note there's multiple entries, one for each plane, so isn't a simple search and exit as soon as we get a match
		final String nodeStrengthID = changeNodeStrengthAction.getSelectedItem ().getNodeStrengthID ();
		for (final DifficultyLevelNodeStrength dlns : changeDifficultyLevelAction.getSelectedItem ().getDifficultyLevelNodeStrength ())
			if (dlns.getNodeStrengthID ().equals (nodeStrengthID))
			{
				if (dlns.getPlaneNumber () == 0)
				{
					arcanusNodeMonstersMin.setText	(new Integer (dlns.getMonstersMinimum ()).toString ());
					arcanusNodeMonstersMax.setText	(new Integer (dlns.getMonstersMaximum ()).toString ());
					arcanusNodeTreasureMin.setText	(new Integer (dlns.getTreasureMinimum ()).toString ());
					arcanusNodeTreasureMax.setText	(new Integer (dlns.getTreasureMaximum ()).toString ());
				}
				else
				{
					myrrorNodeMonstersMin.setText		(new Integer (dlns.getMonstersMinimum ()).toString ());
					myrrorNodeMonstersMax.setText		(new Integer (dlns.getMonstersMaximum ()).toString ());
					myrrorNodeTreasureMin.setText		(new Integer (dlns.getTreasureMinimum ()).toString ());
					myrrorNodeTreasureMax.setText		(new Integer (dlns.getTreasureMaximum ()).toString ());
				}
			}
		
		// Fog of war settings
		final FogOfWarSettingData fowSettings = changeFogOfWarSettingsAction.getSelectedItem ();
		fowTerrainAlways.setSelected						(fowSettings.getTerrainAndNodeAuras () == FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		fowTerrainRemember.setSelected				(fowSettings.getTerrainAndNodeAuras () == FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowTerrainForget.setSelected						(fowSettings.getTerrainAndNodeAuras () == FogOfWarValue.FORGET);
		fowCitiesAlways.setSelected						(fowSettings.getCitiesSpellsAndCombatAreaEffects () == FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		fowCitiesRemember.setSelected					(fowSettings.getCitiesSpellsAndCombatAreaEffects () == FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowCitiesForget.setSelected							(fowSettings.getCitiesSpellsAndCombatAreaEffects () == FogOfWarValue.FORGET);
		canSeeEnemyCityConstruction.setSelected	(fowSettings.isSeeEnemyCityConstruction ());
		fowUnitsAlways.setSelected							(fowSettings.getUnits () == FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		fowUnitsRemember.setSelected					(fowSettings.getUnits () == FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		fowUnitsForget.setSelected							(fowSettings.getUnits () == FogOfWarValue.FORGET);
		
		// Unit settings
		final UnitSettingData unitSettings = changeUnitSettingsAction.getSelectedItem ();
		maxUnitsPerGridCell.setText						(new Integer (unitSettings.getUnitsPerMapCell ()).toString ());
		exceedMaxUnitsDuringCombat.setSelected	(unitSettings.isCanExceedMaximumUnitsDuringCombat ());
		maximumHeroes.setText								(new Integer (unitSettings.getMaxHeroes ()).toString ());
		rollHeroSkillsAtStart.setSelected					(unitSettings.isRollHeroSkillsAtStartOfGame ());
		
		// Spell settings
		final SpellSettingData spellSettings = changeSpellSettingsAction.getSelectedItem ();
		switchResearchNo.setSelected							(spellSettings.getSwitchResearch () == SwitchResearch.DISALLOWED);
		switchResearchNotStarted.setSelected				(spellSettings.getSwitchResearch () == SwitchResearch.ONLY_IF_NOT_STARTED);
		switchResearchLose.setSelected						(spellSettings.getSwitchResearch () == SwitchResearch.LOSE_CURRENT_RESEARCH);
		switchResearchFreely.setSelected					(spellSettings.getSwitchResearch () == SwitchResearch.FREE);
		castingCostReductionAdditive.setSelected			(spellSettings.getSpellBooksCastingReductionCombination () == CastingReductionCombination.ADDITIVE);
		castingCostReductionMultiplicative.setSelected	(spellSettings.getSpellBooksCastingReductionCombination () == CastingReductionCombination.MULTIPLICATIVE);
		researchBonusAdditive.setSelected					(spellSettings.getSpellBooksResearchBonusCombination () == CastingReductionCombination.ADDITIVE);
		researchBonusMultiplicative.setSelected			(spellSettings.getSpellBooksResearchBonusCombination () == CastingReductionCombination.MULTIPLICATIVE);
		spellBookCountForFirstReduction.setText			(new Integer (spellSettings.getSpellBooksToObtainFirstReduction ()).toString ());
		castingCostReduction.setText							(new Integer (spellSettings.getSpellBooksCastingReduction ()).toString ());
		researchBonus.setText									(new Integer (spellSettings.getSpellBooksResearchBonus ()).toString ());
		castingCostReductionCap.setText						(new Integer (spellSettings.getSpellBooksCastingReductionCap ()).toString ());
		researchBonusCap.setText								(new Integer (spellSettings.getSpellBooksResearchBonusCap ()).toString ());
		
		// Debug options
		disableFogOfWar.setSelected (false);
		
		log.trace ("Exiting populateNewGameFieldsFromSelectedOptions");
	}
	
	/**
	 * @return Session description built from all the custom new game forms
	 */
	private final MomSessionDescription buildSessionDescription ()
	{
		log.trace ("Entering buildSessionDescription");
		
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Easy fields
		sd.setSessionName (gameName.getText ());
		sd.setXmlDatabaseName (changeDatabaseAction.getSelectedItem ().getDbName ());
		sd.setTurnSystem (changeTurnSystemAction.getSelectedItem ());
		
		// +3 for this player (since we only select nbr of opponents) + raiders + monsters
		sd.setMaxPlayers (changeHumanOpponentsAction.getSelectedItem () + changeAIOpponentsAction.getSelectedItem () + 3);
		sd.setAiPlayerCount (changeAIOpponentsAction.getSelectedItem ());
		
		// Map size
		final MapSizePlane arcanusMapSize = new MapSizePlane ();
		arcanusMapSize.setPlaneNumber (0);
		arcanusMapSize.setNumberOfNodesOnPlane (Integer.parseInt (arcanusNodeCount.getText ()));
		
		final MapSizePlane myrrorMapSize = new MapSizePlane ();
		myrrorMapSize.setPlaneNumber (1);
		myrrorMapSize.setNumberOfNodesOnPlane (Integer.parseInt (myrrorNodeCount.getText ()));
		
		final MapSizeData mapSize = new MapSizeData ();
	    mapSize.setCoordinateSystemType			(CoordinateSystemType.SQUARE);
	    mapSize.setWidth									(Integer.parseInt (mapSizeWidth.getText ()));
	    mapSize.setHeight									(Integer.parseInt (mapSizeHeight.getText ()));
	    mapSize.setDepth									(2);
	    mapSize.setWrapsLeftToRight					(mapWrapsLeftToRight.isSelected ());
	    mapSize.setWrapsTopToBottom				(mapWrapsTopToBottom.isSelected ());
	    mapSize.setZoneWidth							(Integer.parseInt (mapZoneWidth.getText ()));
	    mapSize.setZoneHeight							(Integer.parseInt (mapZoneHeight.getText ()));
	    mapSize.setTowersOfWizardryCount		(Integer.parseInt (towersOfWizardryCount.getText ()));
	    mapSize.setTowersOfWizardrySeparation	(Integer.parseInt (towersOfWizardrySeparation.getText ()));
	    mapSize.setContinentalRaceChance			(Integer.parseInt (continentalRaceChance.getText ()));
	    mapSize.setCitySeparation						(Integer.parseInt (citySeparation.getText ()));
	    mapSize.setRiverCount							(Integer.parseInt (riverCount.getText ()));
	    mapSize.setRaiderCityCount					(Integer.parseInt (raiderCityCount.getText ()));
	    mapSize.getMapSizePlane ().add (arcanusMapSize);
	    mapSize.getMapSizePlane ().add (myrrorMapSize);
		sd.setMapSize (mapSize);
		
		// Combat map size is fixed, at least for now
		final CombatMapSizeData combatMapSize = new CombatMapSizeData ();
		combatMapSize.setCoordinateSystemType (CoordinateSystemType.DIAMOND);
		combatMapSize.setWidth (CommonDatabaseConstants.COMBAT_MAP_WIDTH);
		combatMapSize.setHeight (CommonDatabaseConstants.COMBAT_MAP_HEIGHT);
		combatMapSize.setDepth (1);
		combatMapSize.setWrapsLeftToRight (false);
		combatMapSize.setWrapsTopToBottom (false);
		combatMapSize.setZoneWidth (10);
		combatMapSize.setZoneHeight (8);
		sd.setCombatMapSize (combatMapSize);
		
		// Land proportion
		final LandProportionTileType forestTileType = new LandProportionTileType ();
		forestTileType.setTileTypeID				(CommonDatabaseConstants.TILE_TYPE_FOREST);
		forestTileType.setPercentageOfLand		(Integer.parseInt (treesPercentage.getText ()));
		forestTileType.setEachAreaTileCount	(Integer.parseInt (treeAreaSize.getText ()));

		final LandProportionTileType desertTileType = new LandProportionTileType ();
		desertTileType.setTileTypeID				(CommonDatabaseConstants.TILE_TYPE_DESERT);
		desertTileType.setPercentageOfLand	(Integer.parseInt (desertsPercentage.getText ()));
		desertTileType.setEachAreaTileCount	(Integer.parseInt (desertAreaSize.getText ()));
		
		final LandProportionTileType swampTileType = new LandProportionTileType ();
		swampTileType.setTileTypeID				(CommonDatabaseConstants.TILE_TYPE_SWAMP);
		swampTileType.setPercentageOfLand	(Integer.parseInt (swampsPercentage.getText ()));
		swampTileType.setEachAreaTileCount	(Integer.parseInt (swampAreaSize.getText ()));
		
		final LandProportionPlane arcanusLandProportion = new LandProportionPlane ();
		arcanusLandProportion.setPlaneNumber (0);
		arcanusLandProportion.setFeatureChance (Integer.parseInt (arcanusMineralChance.getText ()));

		final LandProportionPlane myrrorLandProportion = new LandProportionPlane ();
		myrrorLandProportion.setPlaneNumber (1);
		myrrorLandProportion.setFeatureChance (Integer.parseInt (myrrorMineralChance.getText ()));
		
		final LandProportionData landProportion = new LandProportionData (); 
		landProportion.setPercentageOfMapIsLand			(Integer.parseInt (landPercentage.getText ()));
		landProportion.setPercentageOfLandIsHills			(Integer.parseInt (hillsPercentage.getText ()));
		landProportion.setPercentageOfHillsAreMountains	(Integer.parseInt (mountainsPercentage.getText ()));
		landProportion.setTundraRowCount						(Integer.parseInt (tundraDistance.getText ()));
		landProportion.getLandProportionTileType ().add (forestTileType);
		landProportion.getLandProportionTileType ().add (desertTileType);
		landProportion.getLandProportionTileType ().add (swampTileType);
		landProportion.getLandProportionPlane ().add (arcanusLandProportion);
		landProportion.getLandProportionPlane ().add (myrrorLandProportion);
		sd.setLandProportion (landProportion);		
		
		// Node strength
		final NodeStrengthPlane arcanusNodeStrength = new NodeStrengthPlane ();
		arcanusNodeStrength.setPlaneNumber (0);
		arcanusNodeStrength.setNodeAuraSquaresMinimum	(Integer.parseInt (arcanusNodeSizeMin.getText ()));
		arcanusNodeStrength.setNodeAuraSquaresMaximum	(Integer.parseInt (arcanusNodeSizeMax.getText ()));
		
		final NodeStrengthPlane myrrorNodeStrength = new NodeStrengthPlane ();
		myrrorNodeStrength.setPlaneNumber (1);
		myrrorNodeStrength.setNodeAuraSquaresMinimum	(Integer.parseInt (myrrorNodeSizeMin.getText ()));
		myrrorNodeStrength.setNodeAuraSquaresMaximum	(Integer.parseInt (myrrorNodeSizeMax.getText ()));
		
		final NodeStrengthData nodeStrength = new NodeStrengthData ();
		nodeStrength.setDoubleNodeAuraMagicPower (Integer.parseInt (doubleNodeAuraMagicPower.getText ()));
		nodeStrength.getNodeStrengthPlane ().add (arcanusNodeStrength);
		nodeStrength.getNodeStrengthPlane ().add (myrrorNodeStrength);
		sd.setNodeStrength (nodeStrength);
		
		// Difficulty level
		final DifficultyLevelPlane arcanusDifficultyLevel = new DifficultyLevelPlane ();
		arcanusDifficultyLevel.setPlaneNumber (0);
		arcanusDifficultyLevel.setNormalLairMonstersMinimum	(Integer.parseInt (arcanusNormalLairMonstersMin.getText ()));
		arcanusDifficultyLevel.setNormalLairMonstersMaximum	(Integer.parseInt (arcanusNormalLairMonstersMax.getText ()));
		arcanusDifficultyLevel.setNormalLairTreasureMinimum	(Integer.parseInt (arcanusNormalLairTreasureMin.getText ()));
		arcanusDifficultyLevel.setNormalLairTreasureMaximum	(Integer.parseInt (arcanusNormalLairTreasureMax.getText ()));
		arcanusDifficultyLevel.setWeakLairMonstersMinimum	(Integer.parseInt (arcanusWeakLairMonstersMin.getText ()));
		arcanusDifficultyLevel.setWeakLairMonstersMaximum	(Integer.parseInt (arcanusWeakLairMonstersMax.getText ()));
		arcanusDifficultyLevel.setWeakLairTreasureMinimum	(Integer.parseInt (arcanusWeakLairTreasureMin.getText ()));
		arcanusDifficultyLevel.setWeakLairTreasureMaximum	(Integer.parseInt (arcanusWeakLairTreasureMax.getText ()));
		
		final DifficultyLevelPlane myrrorDifficultyLevel = new DifficultyLevelPlane ();
		myrrorDifficultyLevel.setPlaneNumber (1);
		myrrorDifficultyLevel.setNormalLairMonstersMinimum	(Integer.parseInt (myrrorNormalLairMonstersMin.getText ()));
		myrrorDifficultyLevel.setNormalLairMonstersMaximum	(Integer.parseInt (myrrorNormalLairMonstersMax.getText ()));
		myrrorDifficultyLevel.setNormalLairTreasureMinimum	(Integer.parseInt (myrrorNormalLairTreasureMin.getText ()));
		myrrorDifficultyLevel.setNormalLairTreasureMaximum	(Integer.parseInt (myrrorNormalLairTreasureMax.getText ()));
		myrrorDifficultyLevel.setWeakLairMonstersMinimum	(Integer.parseInt (myrrorWeakLairMonstersMin.getText ()));
		myrrorDifficultyLevel.setWeakLairMonstersMaximum	(Integer.parseInt (myrrorWeakLairMonstersMax.getText ()));
		myrrorDifficultyLevel.setWeakLairTreasureMinimum	(Integer.parseInt (myrrorWeakLairTreasureMin.getText ()));
		myrrorDifficultyLevel.setWeakLairTreasureMaximum	(Integer.parseInt (myrrorWeakLairTreasureMax.getText ()));
		
		final DifficultyLevelData difficultyLevel = new DifficultyLevelData ();
	    difficultyLevel.setHumanSpellPicks				(Integer.parseInt (humanSpellPicks.getText ()));
	    difficultyLevel.setAiSpellPicks						(Integer.parseInt (aiSpellPicks.getText ()));
	    difficultyLevel.setHumanStartingGold				(Integer.parseInt (humanStartingGold.getText ()));
	    difficultyLevel.setAiStartingGold					(Integer.parseInt (aiStartingGold.getText ()));
	    difficultyLevel.setCustomWizards					(allowCustomWizards.isSelected ());
	    difficultyLevel.setEachWizardOnlyOnce			(eachWizardOnlyOnce.isSelected ());
	    difficultyLevel.setNormalLairCount				(Integer.parseInt (normalLairCount.getText ()));
	    difficultyLevel.setWeakLairCount					(Integer.parseInt (weakLairCount.getText ()));
	    difficultyLevel.setTowerMonstersMinimum		(Integer.parseInt (towersMonstersMin.getText ()));
	    difficultyLevel.setTowerMonstersMaximum	(Integer.parseInt (towersMonstersMax.getText ()));
	    difficultyLevel.setTowerTreasureMinimum		(Integer.parseInt (towersTreasureMin.getText ()));
	    difficultyLevel.setTowerTreasureMaximum	(Integer.parseInt (towersTreasureMax.getText ()));
	    difficultyLevel.setRaiderCityStartSizeMin		(Integer.parseInt (raiderCityStartSizeMin.getText ()));
	    difficultyLevel.setRaiderCityStartSizeMax		(Integer.parseInt (raiderCityStartSizeMax.getText ()));
	    difficultyLevel.setRaiderCityGrowthCap			(Integer.parseInt (doubleNodeAuraMagicPower.getText ()));
	    difficultyLevel.setWizardCityStartSize			(Integer.parseInt (wizardCityStartSize.getText ()));
	    difficultyLevel.setCityMaxSize						(Integer.parseInt (maxCitySize.getText ()));
	    difficultyLevel.getDifficultyLevelPlane ().add (arcanusDifficultyLevel);
	    difficultyLevel.getDifficultyLevelPlane ().add (myrrorDifficultyLevel);
		sd.setDifficultyLevel (difficultyLevel);
		
		// Difficulty level - node strength
		final DifficultyLevelNodeStrengthData arcanusNodeDifficultyLevel = new DifficultyLevelNodeStrengthData ();
		arcanusNodeDifficultyLevel.setPlaneNumber (0);
		arcanusNodeDifficultyLevel.setMonstersMinimum	(Integer.parseInt (arcanusNodeMonstersMin.getText ()));
		arcanusNodeDifficultyLevel.setMonstersMaximum	(Integer.parseInt (arcanusNodeMonstersMax.getText ()));
		arcanusNodeDifficultyLevel.setTreasureMinimum	(Integer.parseInt (arcanusNodeTreasureMin.getText ()));
		arcanusNodeDifficultyLevel.setTreasureMaximum	(Integer.parseInt (arcanusNodeTreasureMax.getText ()));
		
		final DifficultyLevelNodeStrengthData myrrorNodeDifficultyLevel = new DifficultyLevelNodeStrengthData ();
		myrrorNodeDifficultyLevel.setPlaneNumber (1);
		myrrorNodeDifficultyLevel.setMonstersMinimum		(Integer.parseInt (myrrorNodeMonstersMin.getText ()));
		myrrorNodeDifficultyLevel.setMonstersMaximum	(Integer.parseInt (myrrorNodeMonstersMax.getText ()));
		myrrorNodeDifficultyLevel.setTreasureMinimum		(Integer.parseInt (myrrorNodeTreasureMin.getText ()));
		myrrorNodeDifficultyLevel.setTreasureMaximum	(Integer.parseInt (myrrorNodeTreasureMax.getText ()));
		
		sd.getDifficultyLevelNodeStrength ().add (arcanusNodeDifficultyLevel);
		sd.getDifficultyLevelNodeStrength ().add (myrrorNodeDifficultyLevel);
		
		// FOW settings
		final FogOfWarSettingData fowSettings = new FogOfWarSettingData ();
		if (fowTerrainAlways.isSelected ())
			fowSettings.setTerrainAndNodeAuras (FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		else if (fowTerrainRemember.isSelected ())
			fowSettings.setTerrainAndNodeAuras (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		else
			fowSettings.setTerrainAndNodeAuras (FogOfWarValue.FORGET);

		if (fowCitiesAlways.isSelected ())
			fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		else if (fowCitiesRemember.isSelected ())
			fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		else
			fowSettings.setCitiesSpellsAndCombatAreaEffects (FogOfWarValue.FORGET);

		if (fowUnitsAlways.isSelected ())
			fowSettings.setUnits (FogOfWarValue.ALWAYS_SEE_ONCE_SEEN);
		else if (fowUnitsRemember.isSelected ())
			fowSettings.setUnits (FogOfWarValue.REMEMBER_AS_LAST_SEEN);
		else
			fowSettings.setUnits (FogOfWarValue.FORGET);
		
		fowSettings.setSeeEnemyCityConstruction (canSeeEnemyCityConstruction.isSelected ());
		sd.setFogOfWarSetting (fowSettings);
		
		// Unit settings
		final UnitSettingData unitSettings = new UnitSettingData ();
		unitSettings.setUnitsPerMapCell									(Integer.parseInt (maxUnitsPerGridCell.getText ()));
		unitSettings.setCanExceedMaximumUnitsDuringCombat	(exceedMaxUnitsDuringCombat.isSelected ());
		unitSettings.setMaxHeroes											(Integer.parseInt (maximumHeroes.getText ()));
		unitSettings.setRollHeroSkillsAtStartOfGame					(rollHeroSkillsAtStart.isSelected ());
		sd.setUnitSetting (unitSettings);
		
		// Spell settings
		final SpellSettingData spellSettings = new SpellSettingData ();
		if (switchResearchNo.isSelected ())
			spellSettings.setSwitchResearch (SwitchResearch.DISALLOWED);
		else if (switchResearchNotStarted.isSelected ())
			spellSettings.setSwitchResearch (SwitchResearch.ONLY_IF_NOT_STARTED);
		else if (switchResearchLose.isSelected ())
			spellSettings.setSwitchResearch (SwitchResearch.LOSE_CURRENT_RESEARCH);
		else
			spellSettings.setSwitchResearch (SwitchResearch.FREE);

		if (castingCostReductionAdditive.isSelected ())
			spellSettings.setSpellBooksCastingReductionCombination (CastingReductionCombination.ADDITIVE);
		else
			spellSettings.setSpellBooksCastingReductionCombination (CastingReductionCombination.MULTIPLICATIVE);
		
		if (researchBonusAdditive.isSelected ())
			spellSettings.setSpellBooksResearchBonusCombination (CastingReductionCombination.ADDITIVE);
		else
			spellSettings.setSpellBooksResearchBonusCombination (CastingReductionCombination.MULTIPLICATIVE);
		
		spellSettings.setSpellBooksToObtainFirstReduction	(Integer.parseInt (spellBookCountForFirstReduction.getText ()));
		spellSettings.setSpellBooksCastingReduction			(Integer.parseInt (castingCostReduction.getText ()));
		spellSettings.setSpellBooksCastingReductionCap	(Integer.parseInt (castingCostReductionCap.getText ()));
		spellSettings.setSpellBooksResearchBonus			(Integer.parseInt (researchBonus.getText ()));
		spellSettings.setSpellBooksResearchBonusCap		(Integer.parseInt (researchBonusCap.getText ()));
		sd.setSpellSetting (spellSettings);
		
		// Debug options
		sd.setDisableFogOfWar (disableFogOfWar.isSelected ());
		
		log.trace ("Exiting buildSessionDescription = " + sd);
		return sd;
	}

	/**
	 * @return Gets set to chosen portrait ID when player clicks a button, or null if they click custom
	 */
	public final String getPortraitChosen ()
	{
		return portraitChosen;
	}
	
	/**
	 * @return List of sessions we can join
	 */
	public final List<SessionAndPlayerDescriptions> getSessions ()
	{
		return sessions;
	}

	/**
	 * @param ses List of sessions we can join
	 */
	public final void setSessions (final List<SessionAndPlayerDescriptions> ses)
	{
		sessions = ses;
		sessionsTableModel.fireTableDataChanged ();
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
	 * @return Wizard client utils
	 */
	public final WizardClientUtils getWizardClientUtils ()
	{
		return wizardClientUtils;
	}

	/**
	 * @param util Wizard client utils
	 */
	public final void setWizardClientUtils (final WizardClientUtils util)
	{
		wizardClientUtils = util;
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
	 * @return Client-side pick utils
	 */
	public final PlayerPickClientUtils getPlayerPickClientUtils ()
	{
		return playerPickClientUtils;
	}

	/**
	 * @param util Client-side pick utils
	 */
	public final void setPlayerPickClientUtils (final PlayerPickClientUtils util)
	{
		playerPickClientUtils = util;
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

	/**
	 * @return XML layout of the "custom map size" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutMapSize ()
	{
		return newGameLayoutMapSize;
	}

	/**
	 * @param layout XML layout of the "custom map size" right hand side
	 */
	public final void setNewGameLayoutMapSize (final XmlLayoutContainerEx layout)
	{
		newGameLayoutMapSize = layout;
	}
	
	/**
	 * @return XML layout of the "custom land proportion" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutLandProportion ()
	{
		return newGameLayoutLandProportion;
	}
	
	/**
	 * @param layout XML layout of the "custom land proportion" right hand side
	 */
	public final void setNewGameLayoutLandProportion (final XmlLayoutContainerEx layout)
	{
		newGameLayoutLandProportion = layout;
	}
	
	/**
	 * @return XML layout of the "custom nodes" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutNodes ()
	{
		return newGameLayoutNodes;
	}

	/**
	 * @param layout XML layout of the "custom nodes" right hand side
	 */
	public final void setNewGameLayoutNodes (final XmlLayoutContainerEx layout)
	{
		newGameLayoutNodes = layout;
	}

	/**
	 * @return XML layout of the "custom difficulty 1" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutDifficulty1 ()
	{
		return newGameLayoutDifficulty1;
	}

	/**
	 * @param layout XML layout of the "custom difficulty 1" right hand side
	 */
	public final void setNewGameLayoutDifficulty1 (final XmlLayoutContainerEx layout)
	{
		newGameLayoutDifficulty1 = layout;
	}
	
	/**
	 * @return XML layout of the "custom difficulty 2" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutDifficulty2 ()
	{
		return newGameLayoutDifficulty2;
	}
	
	/**
	 * @param layout XML layout of the "custom difficulty 2" right hand side
	 */
	public final void setNewGameLayoutDifficulty2 (final XmlLayoutContainerEx layout)
	{
		newGameLayoutDifficulty2 = layout;
	}
	
	/**
	 * @return XML layout of the "custom difficulty 3" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutDifficulty3 ()
	{
		return newGameLayoutDifficulty3;
	}
	
	/**
	 * @param layout XML layout of the "custom difficulty 3" right hand side
	 */
	public final void setNewGameLayoutDifficulty3 (final XmlLayoutContainerEx layout)
	{
		newGameLayoutDifficulty3 = layout;
	}
	
	/**
	 * @return XML layout of the "custom fog of war" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutFogOfWar ()
	{
		return newGameLayoutFogOfWar;
	}

	/**
	 * @param layout XML layout of the "custom fog of war" right hand side
	 */
	public final void setNewGameLayoutFogOfWar (final XmlLayoutContainerEx layout)
	{
		newGameLayoutFogOfWar = layout;
	}
	
	/**
	 * @return XML layout of the "custom unit settings" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutUnits ()
	{
		return newGameLayoutUnits;
	}
	
	/**
	 * @param layout XML layout of the "custom unit settings" right hand side
	 */
	public final void setNewGameLayoutUnits (final XmlLayoutContainerEx layout)
	{
		newGameLayoutUnits = layout;
	}
	
	/**
	 * @return XML layout of the "custom spell settings" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutSpells ()
	{
		return newGameLayoutSpells;
	}

	/**
	 * @param layout XML layout of the "custom spell settings" right hand side
	 */
	public final void setNewGameLayoutSpells (final XmlLayoutContainerEx layout)
	{
		newGameLayoutSpells = layout;
	}
	
	/**
	 * @return XML layout of the "custom debug options" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutDebug ()
	{
		return newGameLayoutDebug;
	}
	
	/**
	 * @param layout XML layout of the "custom debug options" right hand side
	 */
	public final void setNewGameLayoutDebug (final XmlLayoutContainerEx layout)
	{
		newGameLayoutDebug = layout;
	}

	/**
	 * @return XML layout of the "join game" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutJoin ()
	{
		return newGameLayoutJoin;
	}

	/**
	 * @param layout XML layout of the "join game" right hand side
	 */
	public final void setNewGameLayoutJoin (final XmlLayoutContainerEx layout)
	{
		newGameLayoutJoin = layout;
	}
	
	/**
	 * @return XML layout of the "custom flag colour" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutFlagColour ()
	{
		return newGameLayoutFlagColour;
	}

	/**
	 * @param layout XML layout of the "custom flag colour" right hand side
	 */
	public final void setNewGameLayoutFlagColour (final XmlLayoutContainerEx layout)
	{
		newGameLayoutFlagColour = layout;
	}
	
	/**
	 * @return XML layout of the "custom picks" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutPicks ()
	{
		return newGameLayoutPicks;
	}

	/**
	 * @param layout XML layout of the "custom picks" right hand side
	 */
	public final void setNewGameLayoutPicks (final XmlLayoutContainerEx layout)
	{
		newGameLayoutPicks = layout;
	}

	/**
	 * @return XML layout of the "wait for players to join" right hand side
	 */
	public final XmlLayoutContainerEx getNewGameLayoutWait ()
	{
		return newGameLayoutWait;
	}

	/**
	 * @param layout XML layout of the "wait for players to join" right hand side
	 */
	public final void setNewGameLayoutWait (final XmlLayoutContainerEx layout)
	{
		newGameLayoutWait = layout;
	}
	
	/**
	 * Overrides slider appearance to more match the brown background colour
	 */
	private final class FlagColourSlider extends JSlider
	{
		/**
		 * Default slider to vertical orientation
		 */
		private FlagColourSlider ()
		{
			super (SwingConstants.VERTICAL);
		}
		
		/**
		 * Override slider appearance
		 */
		@Override
		protected final void paintComponent (final Graphics g)
		{
			g.drawImage (flagColourSliderBackground, 0, 0, null);
			
			// Whole area is 7x205
			g.setColor (SLIDER_BAR_COLOUR);
			final int height = (205 * getValue ()) / getMaximum ();
			g.fillRect (7, 212-height, 7, height);
		}
	}
	
	/**
	 * Table model for displaying game session we can join
	 */
	private final class SessionListTableModel extends AbstractTableModel
	{
		/**
		 * @return Number of columns in the grid
		 */
		@Override
		public final int getColumnCount ()
		{
			return 3;
		}
		
		/**
		 * @return Heading for each column
		 */
		@Override
		public final String getColumnName (final int column)
		{
			return getLanguage ().findCategoryEntry ("frmJoinGame", "SessionsColumn" + column);
		}
		
		/**
		 * @return Number of sessions we can join
		 */
		@Override
		public final int getRowCount ()
		{
			return getSessions ().size ();
		}

		/**
		 * @return Value to display at particular cell
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			final SessionAndPlayerDescriptions spd = getSessions ().get (rowIndex);
			final MomSessionDescription sd = (MomSessionDescription) spd.getSessionDescription ();
			
			final String value;
			switch (columnIndex)
			{
				case 0:
					value = spd.getSessionDescription ().getSessionName ();
					break;

				case 1:
					value = spd.getPlayer ().size () + " / " + (sd.getMaxPlayers () - sd.getAiPlayerCount () - 2) +
						(sd.getAiPlayerCount () == 0 ? "" : (", +" + sd.getAiPlayerCount () + " AI")); 
					break;

				case 2:
					value = sd.getMapSize ().getWidth () + " x " + sd.getMapSize ().getHeight ();
					break;
					
				default:
					value = null;
			}
			return value;
		}
	}

	/**
	 * Table model for displaying players in the session
	 */
	private final class PlayerListTableModel extends AbstractTableModel
	{
		/**
		 * @return Number of columns in the grid
		 */
		@Override
		public final int getColumnCount ()
		{
			return 3;
		}
		
		/**
		 * @return Heading for each column
		 */
		@Override
		public final String getColumnName (final int column)
		{
			return getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "ListColumn" + column);
		}
		
		/**
		 * @return Number of sessions we can join
		 */
		@Override
		public final int getRowCount ()
		{
			return getClient ().getPlayers ().size ();
		}

		/**
		 * @return Value to display at particular cell
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			final PlayerPublicDetails ppd = getClient ().getPlayers ().get (rowIndex);
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ppd.getPersistentPlayerPublicKnowledge ();
			
			final String value;
			switch (columnIndex)
			{
				case 0:
					value = getWizardClientUtils ().getPlayerName (ppd);
					break;

				case 1:
					if (!PlayerKnowledgeUtils.hasWizardBeenChosen (pub.getWizardID ()))
						value = "";
					
					else if (PlayerKnowledgeUtils.isCustomWizard (pub.getWizardID ()))
						value = getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Custom");
					
					else
						value = getLanguage ().findWizardName (pub.getWizardID ());
					break;

				case 2:
					value = getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin",
						ppd.getPlayerDescription ().isHuman () ? "Human" : "AI");
					break;
					
				default:
					value = null;
			}
			return value;
		}
	}
}
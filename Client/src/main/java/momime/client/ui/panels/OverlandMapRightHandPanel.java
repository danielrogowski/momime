package momime.client.ui.panels;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import momime.client.MomClient;
import momime.client.database.MapFeature;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.MapFeatureLang;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.SpellBookSectionLang;
import momime.client.language.database.SpellLang;
import momime.client.language.database.TileTypeLang;
import momime.client.newturnmessages.NewTurnMessageMustBeAnswered;
import momime.client.newturnmessages.NewTurnMessageSpellEx;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.HideableComponent;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.utils.TextUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.EnforceProductionID;
import momime.common.database.MapFeatureProduction;
import momime.common.database.ProductionType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.TileType;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.NewTurnMessageData;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PendingMovement;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitSpecialOrder;
import momime.common.messages.clienttoserver.CancelPendingMovementAndSpecialOrdersMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * The right hand panel has a switchable top section and switchable bottom section (see the two enums),
 * implemented with two CardLayouts.  Setting that up is fairly involved, and OverlandMapUI is complicated
 * enough already so I wanted to keep the mechanics of this panel separated out into its own class. 
 */
public final class OverlandMapRightHandPanel extends MomClientPanelUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OverlandMapRightHandPanel.class);
	
	/** Bullet point prefix for each line explaining why we cannot end turn right now */
	private final static String BULLET_POINT = "\u2022 ";

	/** Width of the colour patch for one player */
	private final static int COLOUR_PATCH_WIDTH = 8;
	
	/** XML layout for the surveyor subpanel */
	private XmlLayoutContainerEx surveyorLayout;
	
	/** Select unit buttons */
	private List<HideableComponent<SelectUnitButton>> selectUnitButtons = new ArrayList<HideableComponent<SelectUnitButton>> ();

	/** There's a lot of pixel precision positionining going on here so the panel typically uses no insets or custom insets per component */
	private final static int INSET = 0;

	/** Gap between unit selection buttons */
	private final static int UNIT_BUTTONS_INSET = 1;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Small font */
	private Font smallFont;

	/** Medium font */
	private Font mediumFont;
	
	/** Large font */
	private Font largeFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Pending movement utils */
	private PendingMovementUtils pendingMovementUtils;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** UI component factory */
	private UIComponentFactory uiComponentFactory;

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;

	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;

	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** What is displayed in the variable top section */
	private OverlandMapRightHandPanelTop top;
	
	/** What is displayed in the variable bottom section */
	private OverlandMapRightHandPanelBottom bottom;
	
	/** Overall background image */
	private BufferedImage background;
	
	/** Gold amount stored */
	private JLabel goldAmountStored;
	
	/** Mana amount stored */
	private JLabel manaAmountStored;
	
	/** Gold amount per turn */
	private JLabel goldAmountPerTurn;
	
	/** Mana amount per turn */
	private JLabel manaAmountPerTurn;

	/** Rations amount per turn */
	private JLabel rationsAmountPerTurn;

	/** Magic power amount per turn */
	private JLabel magicPowerAmountPerTurn;
	
	/** Player line 1, either says 'Current player:' or 'Waiting for' */
	private JLabel playerLine1;

	/** Player line 2, either says a name or 'other players' */
	private JLabel playerLine2;
	
	/** Card layout for top section */
	private CardLayout topCardLayout;
	
	/** Panel that the top card layout is the layout manager for */
	private JPanel topCards;

	/** Panel that the bottom card layout is the layout manager for */
	private JPanel bottomCards;

	/** Card layout for bottom section */
	private CardLayout bottomCardLayout;
	
	/** Cancel action */
	private Action cancelAction;
	
	/** Done action */
	private Action doneAction;
	
	/** Patrol action */
	private Action patrolAction;
	
	/** Wait action */
	private Action waitAction;

	/** Settlers creating a new outpost */
	private Action createOutpostAction;
	
	/** Engineers building a road */
	private Action buildRoadAction;
	
	/** Spirits melding with a node */
	private Action meldWithNodeAction;
	
	/** Priests purifying corrupted lands */
	private Action purifyAction;

	/** Title for targetting spells */
	private JLabel targetSpellTitle;
	
	/** Text saying what's being targetted */
	private JTextArea targetSpellText;
	
	/** NTM about the spell being targetted */
	private NewTurnMessageSpellEx targetSpell;
	
	/** Title for surveyor */
	private JLabel surveyorTitle;
	
	/** Title for city resources section */
	private JLabel surveyorCityTitle;
	
	/** Tile type here */
	private JLabel surveyorTileType;
	
	/** Food harvested from this tile type */
	private JLabel surveyorTileTypeFood;
	
	/** % production bonus from this tile type */
	private JLabel surveyorTileTypeProduction;
	
	/** % trade bonus from this tile type */
	private JLabel surveyorTileTypeGold;

	/** Map feature here */
	private JLabel surveyorMapFeature;

	/** First effect of this map feature */
	private JLabel surveyorMapFeatureFirst;
	
	/** Second effect of this map feature */
	private JLabel surveyorMapFeatureSecond;

	/** First line of surveyor info about a potential city */
	private JLabel surveyorCityFirst;

	/** Second line of surveyor info about a potential city */
	private JLabel surveyorCitySecond;

	/** Third line of surveyor info about a potential city */
	private JLabel surveyorCityThird;
	
	/** Map location currently being surveyed; can be null to blank out all the surveyor labels */
	private MapCoordinates3DEx surveyorLocation;
	
	/** Next turn action */
	private Action nextTurnAction;
	
	/** Next turn button */
	private JButton nextTurnButton;
	
	/** Image of disabled next turn button with no resource icons on it */
	private BufferedImage nextTurnButtonDisabled;
	
	/** Number of pixels that the colour patches are shifted at the moment */
	private int colourPatchCurrentPos;

	/** Number of pixels that the colour patches should be shifted at the moment (according to who the current player is) */
	private int colourPatchDesiredPos;
	
	/** Mini panel showing colour patch for each wizard to show progression of their turns */
	private JPanel colourPatches;
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	public final void init () throws IOException
	{
		log.trace ("Entering init");

		// Load in all necessary images
		background = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/background.png");
		
		final BufferedImage economyBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/globalEconomy.png");
		final BufferedImage surveyorBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/surveyor.png");
		final BufferedImage targetSpellBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/targetSpell.png");

		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldDisabled.png");

		final BufferedImage createOutpostButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/createOutpostNormal.png");
		final BufferedImage createOutpostButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/createOutpostPressed.png");
		final BufferedImage createOutpostButtonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/createOutpostDisabled.png");
		final BufferedImage buildRoadButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/buildRoadNormal.png");
		final BufferedImage buildRoadButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/buildRoadPressed.png");
		final BufferedImage buildRoadButtonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/buildRoadDisabled.png");
		final BufferedImage meldWithNodeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/meldWithNodeNormal.png");
		final BufferedImage meldWithNodeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/meldWithNodePressed.png");
		final BufferedImage meldWithNodeButtonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/meldWithNodeDisabled.png");
		final BufferedImage purifyButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/purifyNormal.png");
		final BufferedImage purifyButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/purifyPressed.png");
		final BufferedImage purifyButtonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/purifyDisabled.png");

		final BufferedImage nextTurnButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/nextTurnNormal.png");
		final BufferedImage nextTurnButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/nextTurnPressed.png");
		nextTurnButtonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/nextTurnDisabled.png");
		final BufferedImage cancelBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/oneButton.png");
		final BufferedImage specialOrdersBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/fourButtons.png");

		final BufferedImage playerHuman = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/playerHuman.png");
		final BufferedImage playerAI = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/playerAI.png");
		final BufferedImage playerAllocatedMovement = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/playerAllocatedMovement.png");
		
		// Fix the size of the panel to be the same as the background
		final Dimension backgroundSize = new Dimension (background.getWidth (), background.getHeight ());
		getPanel ().setMinimumSize (backgroundSize);
		getPanel ().setMaximumSize (backgroundSize);
		getPanel ().setPreferredSize (backgroundSize);
		
		// Actions
		nextTurnAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().nextTurnButton ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		doneAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().selectedUnitsDone ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		patrolAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().selectedUnitsPatrol ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		waitAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().selectedUnitsWait ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		createOutpostAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().specialOrderButton (UnitSpecialOrder.BUILD_CITY);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		buildRoadAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().specialOrderButton (UnitSpecialOrder.BUILD_ROAD);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		meldWithNodeAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().specialOrderButton (UnitSpecialOrder.MELD_WITH_NODE);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		purifyAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOverlandMapProcessing ().specialOrderButton (UnitSpecialOrder.PURIFY);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		cancelAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					// What are we cancelling - check the top part of the panel
					switch (getTop ())
					{
						case SURVEYOR:
							getOverlandMapProcessing ().updateMovementRemaining ();
							break;
							
						case TARGET_SPELL:
							final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
							msg.setTitleLanguageCategoryID ("SpellTargetting");
							msg.setTitleLanguageEntryID ("CancelTitle");
							msg.setTextLanguageCategoryID ("SpellTargetting");
							msg.setTextLanguageEntryID ("CancelText");
							msg.setCancelTargettingSpell (getTargetSpell ());
							msg.setVisible (true);
							break;
							
						default:
							log.warn ("Cancel button clicked in overland map RHP but don't know what we're cancelling, top = " + getTop ());
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		// Set up layout
		getPanel ().setLayout (new GridBagLayout ());
		
		// Minimap view
		final Dimension minimapSize = new Dimension (116, 73);
		
		final JPanel minimap = new JPanel ();
		minimap.setBackground (new Color (0x008000));
		minimap.setMinimumSize (minimapSize);
		minimap.setMaximumSize (minimapSize);
		minimap.setPreferredSize (minimapSize);
		
		getPanel ().add (minimap, getUtils ().createConstraintsNoFill (0, 0, 2, 1, new Insets (0, 0, 35, 0), GridBagConstraintsNoFill.CENTRE));
		
		// Amounts stored
		goldAmountStored = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getMediumFont ());
		
		final GridBagConstraints goldAmountStoredConstraints = getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, 4, 0, 0), GridBagConstraintsNoFill.CENTRE);
		goldAmountStoredConstraints.weightx = 0.5;
		getPanel ().add (goldAmountStored, goldAmountStoredConstraints);

		manaAmountStored = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getMediumFont ());
		
		final GridBagConstraints manaAmountStoredConstraints = getUtils ().createConstraintsNoFill (1, 1, 1, 1, new Insets (0, 0, 0, 2), GridBagConstraintsNoFill.CENTRE);
		manaAmountStoredConstraints.weightx = 0.5;
		getPanel ().add (manaAmountStored, manaAmountStoredConstraints);
		
		// Card layouts - the top and bottom are stuck directly together with 0 pixel gap in between
		topCardLayout = new CardLayout ();
		
		topCards = new JPanel (topCardLayout);
		topCards.setOpaque (false);
		getPanel ().add (topCards, getUtils ().createConstraintsNoFill (0, 2, 2, 1, new Insets (6, 0, 0, 0), GridBagConstraintsNoFill.CENTRE));

		bottomCardLayout = new CardLayout ();
		
		bottomCards = new JPanel (bottomCardLayout);
		bottomCards.setOpaque (false);
		getPanel ().add (bottomCards, getUtils ().createConstraintsNoFill (0, 3, 2, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Colour patches showing players' turn sequence
		final Dimension colourPatchesSize = new Dimension (134, 26);
		
		colourPatches = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Black out background
				super.paintComponent (g);
				
				int x;
				switch (getClient ().getSessionDescription ().getTurnSystem ())
				{
					case ONE_PLAYER_AT_A_TIME:
						x = -colourPatchCurrentPos;
						int playerIndex = 0;
						while (x < colourPatchesSize.getWidth ())
						{
							// Draw colour patch for next player
							final PlayerPublicDetails player = getClient ().getPlayers ().get (playerIndex);
							final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
							g.setColor (new Color (Integer.parseInt (trans.getFlagColour (), 16)));
							g.fillRect (x, 0, COLOUR_PATCH_WIDTH, colourPatchesSize.height);
							
							// Draw icon for AI or human player
							g.drawImage (player.getPlayerDescription ().isHuman () ? playerHuman : playerAI, x + 1, 1, null);
							
							// Move to next position
							x = x + COLOUR_PATCH_WIDTH;
							playerIndex++;
							
							// Draw white line if boundary between turns
							if (playerIndex >= getClient ().getPlayers ().size ())
							{
								playerIndex = 0;
								x++;				// leave black gap
								g.setColor (Color.WHITE);
								g.drawLine (x, 0, x, colourPatchesSize.height);
								x = x + 2;		// leave white line + black gap
							}
						}
						break;
						
					case SIMULTANEOUS:
						// Always draw players in order, with no animation, so much simpler than above
						x = 0;
						for (final PlayerPublicDetails player : getClient ().getPlayers ())
						{
							final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
							g.setColor (new Color (Integer.parseInt (trans.getFlagColour (), 16)));
							g.fillRect (x, 0, COLOUR_PATCH_WIDTH, colourPatchesSize.height);
							
							// Draw icon for AI or human player
							g.drawImage (player.getPlayerDescription ().isHuman () ? playerHuman : playerAI, x + 1, 1, null);
							
							// Show whether they finished allocating movement yet
							if (trans.getMovementAllocatedForTurnNumber () >= getClient ().getGeneralPublicKnowledge ().getTurnNumber ())
								g.drawImage (playerAllocatedMovement, x + 1, colourPatchesSize.height - playerAllocatedMovement.getHeight () - 1, null);

							// Move to next position
							x = x + COLOUR_PATCH_WIDTH;
						}
						break;
				};
			}
		};
		
		colourPatches.setBackground (Color.BLACK);
		colourPatches.setMinimumSize (colourPatchesSize);
		colourPatches.setMaximumSize (colourPatchesSize);
		colourPatches.setPreferredSize (colourPatchesSize);

		getPanel ().add (colourPatches, getUtils ().createConstraintsNoFill (0, 4, 2, 1, new Insets (4, 0, 8, 0), GridBagConstraintsNoFill.CENTRE));
		
		// Top card - global economy
		final JPanel economyPanel = getUtils ().createPanelWithBackgroundImage (economyBackground);
		topCards.add (economyPanel, OverlandMapRightHandPanelTop.ECONOMY.name ());
		
		economyPanel.setLayout (new GridBagLayout ());
		
		goldAmountPerTurn = getUtils ().createBorderedLabel (Color.BLACK, MomUIConstants.GOLD, getSmallFont ());
		economyPanel.add (goldAmountPerTurn, getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (58, 0, 63, 0), GridBagConstraintsNoFill.CENTRE));
		
		rationsAmountPerTurn = getUtils ().createBorderedLabel (Color.BLACK, MomUIConstants.GOLD, getSmallFont ());
		economyPanel.add (rationsAmountPerTurn, getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, 0, 7, 0), GridBagConstraintsNoFill.CENTRE));
		
		magicPowerAmountPerTurn = getUtils ().createBorderedLabel (Color.BLACK, MomUIConstants.SILVER, getSmallFont ());
		economyPanel.add (magicPowerAmountPerTurn, getUtils ().createConstraintsNoFill (0, 2, 1, 1, new Insets (0, 0, 42, 0), GridBagConstraintsNoFill.CENTRE));
		
		manaAmountPerTurn = getUtils ().createBorderedLabel (Color.BLACK, MomUIConstants.GOLD, getSmallFont ());
		economyPanel.add (manaAmountPerTurn, getUtils ().createConstraintsNoFill (0, 3, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Top card - surveyor
		final JPanel surveyorPanel = getUtils ().createPanelWithBackgroundImage (surveyorBackground);		
		topCards.add (surveyorPanel, OverlandMapRightHandPanelTop.SURVEYOR.name ());
		
		surveyorPanel.setLayout (new XmlLayoutManager (getSurveyorLayout ()));

		surveyorTitle = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getLargeFont ());
		surveyorPanel.add (surveyorTitle, "frmSurveyorTitle");
		
		surveyorTileType = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		surveyorPanel.add (surveyorTileType, "frmSurveyorTileType");
		
		surveyorTileTypeFood = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorTileTypeFood, "frmSurveyorFood");
		
		surveyorTileTypeProduction = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorTileTypeProduction, "frmSurveyorProduction");
		
		surveyorTileTypeGold = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorTileTypeGold, "frmSurveyorGold");

		surveyorMapFeature = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		surveyorPanel.add (surveyorMapFeature, "frmSurveyorFeature");

		surveyorMapFeatureFirst = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorMapFeatureFirst, "frmSurveyorFeatureEffect1");
		
		surveyorMapFeatureSecond = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorMapFeatureSecond, "frmSurveyorFeatureEffect2");

		surveyorCityTitle = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		surveyorPanel.add (surveyorCityTitle, "frmSurveyorCityResources");

		surveyorCityFirst = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorCityFirst, "frmSurveyorCityDetails1");

		surveyorCitySecond = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorCitySecond, "frmSurveyorCityDetails2");

		surveyorCityThird = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorPanel.add (surveyorCityThird, "frmSurveyorCityDetails3");

		// Top card - target spell
		final JPanel targetSpellPanel = getUtils ().createPanelWithBackgroundImage (targetSpellBackground);
		topCards.add (targetSpellPanel, OverlandMapRightHandPanelTop.TARGET_SPELL.name ());
		
		targetSpellPanel.setLayout (new GridBagLayout ());
		
		targetSpellTitle = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getLargeFont ());
		targetSpellTitle.setText ("Target Spell");
		targetSpellPanel.add (targetSpellTitle, getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (4, 0, 4, 0), GridBagConstraintsNoFill.CENTRE));
		
		targetSpellText = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		
		final GridBagConstraints targetSpellConstraints = getUtils ().createConstraintsNoFill (0, 1, 1, 1, INSET, GridBagConstraintsNoFill.NORTH);
		targetSpellConstraints.weightx = 1;
		targetSpellConstraints.weighty = 1;
		targetSpellPanel.add (targetSpellText, targetSpellConstraints);
		
		// Bottom card - next turn button
		nextTurnButton = getUtils ().createImageButton (nextTurnAction, null, null, null, nextTurnButtonNormal, nextTurnButtonPressed, nextTurnButtonDisabled);
		bottomCards.add (nextTurnButton, OverlandMapRightHandPanelBottom.NEXT_TURN_BUTTON.name ());
		
		// Bottom card - cancel
		final JPanel cancelPanel = getUtils ().createPanelWithBackgroundImage (cancelBackground);
		bottomCards.add (cancelPanel, OverlandMapRightHandPanelBottom.CANCEL.name ());
		
		cancelPanel.setLayout (new GridBagLayout ());
		
		cancelPanel.add (getUtils ().createImageButton (cancelAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (0, 0, 1, 1), GridBagConstraintsNoFill.CENTRE));
		
		// Bottom card - special orders
		final JPanel specialOrdersPanel = getUtils ().createPanelWithBackgroundImage (specialOrdersBackground);
		bottomCards.add (specialOrdersPanel, OverlandMapRightHandPanelBottom.SPECIAL_ORDERS.name ());
		
		specialOrdersPanel.setLayout (new GridBagLayout ());
		
		specialOrdersPanel.add (getUtils ().createImageButton (doneAction, MomUIConstants.TRANSPARENT, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (0, 0, 2, 2), GridBagConstraintsNoFill.CENTRE));

		specialOrdersPanel.add (getUtils ().createImageButton (waitAction, MomUIConstants.TRANSPARENT, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, 0, 1, 2), GridBagConstraintsNoFill.CENTRE));

		specialOrdersPanel.add (getUtils ().createImageButton (patrolAction, MomUIConstants.TRANSPARENT, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (1, 0, 4, 1, new Insets (0, 0, 2, 1), GridBagConstraintsNoFill.CENTRE));

		specialOrdersPanel.add (getUtils ().createImageButton (createOutpostAction, null, null, null, createOutpostButtonNormal, createOutpostButtonPressed, createOutpostButtonDisabled),
			getUtils ().createConstraintsNoFill (1, 1, 1, 1, new Insets (0, 0, 2, 0), GridBagConstraintsNoFill.CENTRE));

		specialOrdersPanel.add (getUtils ().createImageButton (buildRoadAction, null, null, null, buildRoadButtonNormal, buildRoadButtonPressed, buildRoadButtonDisabled),
			getUtils ().createConstraintsNoFill (2, 1, 1, 1, new Insets (0, 1, 2, 0), GridBagConstraintsNoFill.CENTRE));
		
		specialOrdersPanel.add (getUtils ().createImageButton (meldWithNodeAction, null, null, null, meldWithNodeButtonNormal, meldWithNodeButtonPressed, meldWithNodeButtonDisabled),
			getUtils ().createConstraintsNoFill (3, 1, 1, 1, new Insets (0, 1, 2, 0), GridBagConstraintsNoFill.CENTRE));

		specialOrdersPanel.add (getUtils ().createImageButton (purifyAction, null, null, null, purifyButtonNormal, purifyButtonPressed, purifyButtonDisabled),
			getUtils ().createConstraintsNoFill (4, 1, 1, 1, new Insets (0, 1, 2, 0), GridBagConstraintsNoFill.CENTRE));		

		// Top card - units (this is the one with no background image so it just sees through to the main background)
		final JPanel unitsPanel = new JPanel ();
		unitsPanel.setOpaque (false);

		// Assume this is the same size as one of the other top panels
		unitsPanel.setMinimumSize (surveyorPanel.getMinimumSize ());
		unitsPanel.setMaximumSize (surveyorPanel.getMaximumSize ());
		unitsPanel.setPreferredSize (surveyorPanel.getPreferredSize ());
		
		topCards.add (unitsPanel, OverlandMapRightHandPanelTop.UNITS.name ());
		
		unitsPanel.setLayout (new GridBagLayout ());
		
		for (int y = 0; y < 5; y++)
			for (int x = 0; x < 4; x++)
			{
				final SelectUnitButton selectUnitButton = getUiComponentFactory ().createSelectUnitButton ();
				selectUnitButton.init ();
				
				selectUnitButton.addMouseListener (new MouseAdapter ()
				{
					@Override
					public final void mouseClicked (final MouseEvent ev)
					{
						try
						{
							// Right mouse clicks to open up the unit info screen are always enabled
							if (SwingUtilities.isRightMouseButton (ev))
							{
								// Is there a unit info screen already open for this unit?
								UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (selectUnitButton.getUnit ().getUnitURN ());
								if (unitInfo == null)
								{
									unitInfo = getPrototypeFrameCreator ().createUnitInfo ();
									unitInfo.setUnit (selectUnitButton.getUnit ());
									getClient ().getUnitInfos ().put (selectUnitButton.getUnit ().getUnitURN (), unitInfo);
								}
							
								unitInfo.setVisible (true);
							}
							else
							{
								// Left clicks are more complicated - by default Swing will toggle the selected state
								// of the button, but we might need to block that from happening.  We also need to
								// clear pending moves/special orders from units even if we block them from being selected.
								boolean allowClick = false;
							
								// Don't allow deselecting units that we don't own or that have no movement left.
								// We aren't allowed to move then anyway so deselecting them just hides their flag colour which looks confusing.
								// Also if it isn't our turn, then ignore clicks just as if we were clicking on someone else's unit
								if ((selectUnitButton.getUnit ().getOwningPlayerID () == getClient ().getOurPlayerID ()) &&
									((getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) ||
										(getClient ().getOurPlayerID ().equals (getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ()))))
								{
									// Remove any pending movements for this unit and blank out any special orders
									final PendingMovement pendingMovement = getPendingMovementUtils ().findPendingMoveForUnit
										(getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement (), selectUnitButton.getUnit ().getUnitURN ());
								
									// We don't need to tell the server about cancelling 'Patrol' orders since this is basically 'do nothing' -
									// but cancelling any other kind of special order we do need to notify the server.
									if ((pendingMovement != null) ||
										((selectUnitButton.getUnit ().getSpecialOrder () != null) && (selectUnitButton.getUnit ().getSpecialOrder () != UnitSpecialOrder.PATROL)))
									{
										// Remove on server
										final CancelPendingMovementAndSpecialOrdersMessage msg = new CancelPendingMovementAndSpecialOrdersMessage ();
										msg.setUnitURN (selectUnitButton.getUnit ().getUnitURN ());
										getClient ().getServerConnection ().sendMessageToServer (msg);
									}

									// Remove on client
									getPendingMovementUtils ().removeUnitFromAnyPendingMoves
										(getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement (), selectUnitButton.getUnit ().getUnitURN ());
									selectUnitButton.getUnit ().setSpecialOrder (null);
									
									// Select/deselect unit
									if (selectUnitButton.getUnit ().getDoubleOverlandMovesLeft () > 0)
									{
										allowClick = true;
										getOverlandMapProcessing ().enableOrDisableSpecialOrderButtons ();
										getOverlandMapProcessing ().updateMovementRemaining ();
									}
								}
							
								// If we disallowed the click, force the button back to its previous state
								if (!allowClick)
									selectUnitButton.setSelected (!selectUnitButton.isSelected ());
							}
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				});
				
				final HideableComponent<SelectUnitButton> hideable = new HideableComponent<SelectUnitButton> (selectUnitButton);
				selectUnitButtons.add (hideable);
				
				unitsPanel.add (hideable, getUtils ().createConstraintsNoFill (x, y, 1, 1, UNIT_BUTTONS_INSET, GridBagConstraintsNoFill.CENTRE));
			}
		
		unitsPanel.add (Box.createRigidArea (new Dimension (0, 10)), getUtils ().createConstraintsNoFill (0, 5, 4, 1, UNIT_BUTTONS_INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Bottom card - player (this is the one with no background image so it just sees through to the main background)
		final JPanel playerPanel = new JPanel ();
		playerPanel.setOpaque (false);

		// Assume this is the same size as one of the other bottom panels
		playerPanel.setMinimumSize (specialOrdersPanel.getMinimumSize ());
		playerPanel.setMaximumSize (specialOrdersPanel.getMaximumSize ());
		playerPanel.setPreferredSize (specialOrdersPanel.getPreferredSize ());
		
		bottomCards.add (playerPanel, OverlandMapRightHandPanelBottom.PLAYER.name ());
		
		playerPanel.setLayout (new GridBagLayout ());
		
		playerLine1 = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		playerPanel.add (playerLine1, getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (0, 0, 4, 0), GridBagConstraintsNoFill.CENTRE));
		
		playerLine2 = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		playerPanel.add (playerLine2, getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, 0, 4, 0), GridBagConstraintsNoFill.CENTRE));

		// Clicking on next turn button when its disabled explains why we can't end turn right now
		nextTurnButton.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				try
				{
					final String text = updateProductionTypesStoppingUsFromEndingTurn ();
					if (text != null)
					{
						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setTitleLanguageCategoryID ("frmMapRightHandBar");
						msg.setTitleLanguageEntryID ("CannotEndTurnTitle");
						msg.setText (getLanguage ().findCategoryEntry ("frmMapRightHandBar", "CannotEndTurnPrefix") + System.lineSeparator () + text);
						msg.setVisible (true);
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		// Fill in variable labels
		updateGlobalEconomyValues ();
		
		// Set up animation to make the colour patches slide along
		new Timer (30, new ActionListener ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				if (colourPatchCurrentPos != colourPatchDesiredPos)
				{
					colourPatchCurrentPos++;
					if (colourPatchCurrentPos >= getClient ().getPlayers ().size () * COLOUR_PATCH_WIDTH)
						colourPatchCurrentPos = 0;
					
					colourPatches.repaint ();
				}
			}
		}).start ();
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Draws the overall background behind all the other components
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		g.drawImage (background, 0, 0, null);
	}
	
	/**
	 * Forces the colour patches to repaint when a player's status changes
	 */
	public final void repaintColourPatches ()
	{
		colourPatches.repaint ();
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapRightHandBar", "Cancel"));
		doneAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapRightHandBar", "Done"));
		patrolAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapRightHandBar", "Patrol"));
		waitAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapRightHandBar", "Wait"));

		surveyorTitle.setText (getLanguage ().findCategoryEntry ("frmSurveyor", "Title"));
		surveyorCityTitle.setText (getLanguage ().findCategoryEntry ("frmSurveyor", "CityResources"));
		
		targetSpellTitle.setText (getLanguage ().findCategoryEntry ("frmMapRightHandBar", "TargetSpell"));
		
		// A bit more involved to set the spell targetting text correctly
		if (getTargetSpell () != null)
			try
			{
				final SpellLang spellLang = getLanguage ().findSpell (getTargetSpell ().getSpellID ());
				final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
				
				final Spell spell = getClient ().getClientDB ().findSpell (getTargetSpell ().getSpellID (), "OverlandMapRightHandPanel");
				final SpellBookSectionLang section = getLanguage ().findSpellBookSection (spell.getSpellBookSectionID ());
				final String target = (section != null) ? section.getSpellTargetPrompt () : null;
				
				targetSpellText.setText ((target == null) ? ("Select target of type " + spell.getSpellBookSectionID ()) :
					(target.replaceAll ("SPELL_NAME", (spellName != null) ? spellName : getTargetSpell ().getSpellID ())));
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}		
		
		try
		{
			updateGlobalEconomyValues ();
			surveyorLocationOrLanguageChanged ();
			turnSystemOrCurrentPlayerChanged ();
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * Updates one 'stored' global economy value
	 * 
	 * @param label Label to update
	 * @param productionTypeID Production type to display in this label
	 */
	private final void updateAmountStored (final JLabel label, final String productionTypeID)
	{
		// Resource values get sent to us during game startup before the screen has been set up, so its possible to get here before the labels even exist
		if (label != null)
		{
			String amountStored = getTextUtils ().intToStrCommas (getResourceValueUtils ().findAmountStoredForProductionType
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), productionTypeID));
		
			final ProductionTypeLang productionType = getLanguage ().findProductionType (productionTypeID);
			if ((productionType != null) && (productionType.getProductionTypeSuffix () != null))
				amountStored = amountStored + " " + productionType.getProductionTypeSuffix ();
			
			label.setText (amountStored);
		}
	}
	
	/**
	 * Updates one 'per turn' global economy value
	 * 
	 * @param label Label to update
	 * @param productionTypeID Production type to display in this label
	 * @param languageEntryID Language entry to use to format the text
	 * @param positiveColour The colour to display this value if it is positive
	 * @throws PlayerNotFoundException If our player isn't in the list
	 * @throws RecordNotFoundException If we look for a particular record that we expect to be present in the XML file and we can't find it
	 * @throws MomException If we find an invalid casting reduction type
	 */
	private final void updateAmountPerTurn (final JLabel label, final String productionTypeID, final String languageEntryID, final Color positiveColour)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		// Resource values get sent to us during game startup before the screen has been set up, so its possible to get here before the labels even exist
		if (label != null)
		{
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "updateAmountPerTurn");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
			
			final ProductionTypeLang productionType = getLanguage ().findProductionType (productionTypeID);
			final int amountPerTurn = getResourceValueUtils ().calculateAmountPerTurnForProductionType (getClient ().getOurPersistentPlayerPrivateKnowledge (),
				pub.getPick (), productionTypeID, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
			
			label.setText (getLanguage ().findCategoryEntry ("frmMapRightHandBar", languageEntryID).replaceAll
				("AMOUNT_PER_TURN", getTextUtils ().intToStrCommas (amountPerTurn)).replaceAll
				("PRODUCTION_TYPE", (productionType != null) ? productionType.getProductionTypeDescription () : productionTypeID));
		
			label.setForeground ((amountPerTurn >= 0) ? positiveColour : Color.RED);
		}
	}
	
	/**
	 * Updates the form when our resource values change 
	 * @throws PlayerNotFoundException If our player isn't in the list
	 * @throws RecordNotFoundException If we look for a particular record that we expect to be present in the XML file and we can't find it
	 * @throws MomException If we find an invalid casting reduction type
	 */
	public final void updateGlobalEconomyValues () throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.trace ("Entering updateGlobalEconomyValues");
		
		// Amounts stored
		updateAmountStored (goldAmountStored, CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		updateAmountStored (manaAmountStored, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		
		// Amounts per turn
		updateAmountPerTurn (goldAmountPerTurn, CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, "ProductionPerTurn", MomUIConstants.GOLD);
		updateAmountPerTurn (manaAmountPerTurn, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "ProductionPerTurn", MomUIConstants.GOLD);
		updateAmountPerTurn (rationsAmountPerTurn, CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS, "ProductionPerTurn", MomUIConstants.GOLD);
		updateAmountPerTurn (magicPowerAmountPerTurn, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, "ProductionPerTurnMagicPower", MomUIConstants.SILVER);
		
		log.trace ("Exiting updateGlobalEconomyValues");
	}

	/**
	 * Update labels that depend both on the language, and on the turn system and/or current player
	 * @throws PlayerNotFoundException If we can't find the player whose turn it now is
	 */
	public final void turnSystemOrCurrentPlayerChanged () throws PlayerNotFoundException
	{
		log.trace ("Entering turnSystemOrCurrentPlayerChanged");
		
		switch (getClient ().getSessionDescription ().getTurnSystem ())
		{
			case ONE_PLAYER_AT_A_TIME:
				playerLine1.setText (getLanguage ().findCategoryEntry ("frmMapRightHandBar", "OnePlayerAtATimeCurrentPlayer"));
				playerLine2.setForeground (MomUIConstants.SILVER);
				
				// This gets called, via languageChanged (), as the UI is setting up prior to the first turn starting,
				// which means we get here when currentPlayerID is still null
				if (getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID () == null)
					playerLine2.setText (null);
				else
				{
					final PlayerPublicDetails currentPlayer = getMultiplayerSessionUtils ().findPlayerWithID
						(getClient ().getPlayers (), getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID (), "turnSystemOrCurrentPlayerChanged");
					
					playerLine2.setText (getWizardClientUtils ().getPlayerName (currentPlayer));
				}
				break;
				
			case SIMULTANEOUS:
				playerLine1.setText (getLanguage ().findCategoryEntry ("frmMapRightHandBar", "SimultaneousTurnsLine1"));
				playerLine2.setText (getLanguage ().findCategoryEntry ("frmMapRightHandBar", "SimultaneousTurnsLine2"));
				playerLine2.setForeground (MomUIConstants.GOLD);
				break;
		}
		
		log.trace ("Exiting turnSystemOrCurrentPlayerChanged");
	}
	
	/**
	 * Update labels that depend both on the language and on the location being surveyed
	 * @throws RecordNotFoundException If we encounter a map element that can't be found in the DB
	 * @throws MomException If we encounter data that there isn't appropriate fields to display 
	 */
	private final void surveyorLocationOrLanguageChanged () throws RecordNotFoundException, MomException
	{
		log.trace ("Entering surveyorLocationOrLanguageChanged: " + getSurveyorLocation ());

		final MemoryGridCell mc = (getSurveyorLocation () == null) ? null :
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getSurveyorLocation ().getZ ()).getRow ().get (getSurveyorLocation ().getY ()).getCell ().get (getSurveyorLocation ().getX ());

		// Note terrainData can be null here if surveying an area that we've never seen or that is totally off the map
		final OverlandMapTerrainData terrainData = (mc == null) ? null : mc.getTerrainData ();
		final String tileTypeID = getMemoryGridCellUtils ().convertNullTileTypeToFOW (terrainData);
		final TileTypeLang tileTypeLang = getLanguage ().findTileType (tileTypeID);

		// Text about building a city can be set in a bunch of places
		String cityInfo = null;
		
		// Tile type - when looking at an area completely off the map, don't even display "unknown"
		surveyorMapFeature.setText (null);
		
		if (mc == null)
		{
			surveyorTileType.setText (null);
			cityInfo = "";
		}
		else
		{
			// Tile type, note this also outputs a description of Unknown for terrain that we haven't scouted
			final String tileTypeDescription = (tileTypeLang == null) ? null : tileTypeLang.getTileTypeDescription ();

			surveyorTileType.setText ((tileTypeDescription != null) ? tileTypeDescription : tileTypeID);
			
			// Show nodes in the map feature slot, since that's really what they look like
			if ((tileTypeLang != null) && (tileTypeLang.getTileTypeShowAsFeature () != null))
				surveyorMapFeature.setText (tileTypeLang.getTileTypeShowAsFeature ());
		}
			
		// When looking at areas of the map we haven't seen, that "unknown" is all that is displayed, so blank everything else
		if (terrainData == null)
		{			
			surveyorTileTypeFood.setText			(null);
			surveyorTileTypeProduction.setText	(null);
			surveyorTileTypeGold.setText			(null);
			surveyorMapFeatureFirst.setText		(null);
			surveyorMapFeatureSecond.setText	(null);

			if (cityInfo == null)
				cityInfo = getLanguage ().findCategoryEntry ("frmSurveyor", "CantBuildCityBecauseUnscouted");
		}
		else
		{
			// Details about the tile type
			final TileType tileType = getClient ().getClientDB ().findTileType (tileTypeID, "surveyorLocationOrLanguageChanged");
			
			if ((tileType.getDoubleFood () == null) || (tileType.getDoubleFood () <= 0))
				surveyorTileTypeFood.setText (null);
			else
			{
				final ProductionTypeLang productionType = getLanguage ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
				final String productionTypeDescription = (productionType != null) ? productionType.getProductionTypeDescription () : null;
				
				surveyorTileTypeFood.setText (getTextUtils ().halfIntToStr (tileType.getDoubleFood ()) + " " +
					((productionTypeDescription != null) ? productionTypeDescription : CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD));
			}

			if ((tileType.getProductionBonus () == null) || (tileType.getProductionBonus () <= 0))
				surveyorTileTypeProduction.setText (null);
			else
			{
				final ProductionTypeLang productionType = getLanguage ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION);
				final String productionTypeDescription = (productionType != null) ? productionType.getProductionTypeDescription () : null;
				
				surveyorTileTypeProduction.setText ("+" + tileType.getProductionBonus ().toString () + "% " +
					((productionTypeDescription != null) ? productionTypeDescription : CommonDatabaseConstants.PRODUCTION_TYPE_ID_PRODUCTION));
			}

			if ((tileType.getGoldBonus () == null) || (tileType.getGoldBonus () <= 0))
				surveyorTileTypeGold.setText (null);
			else
			{
				final ProductionTypeLang productionType = getLanguage ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				final String productionTypeDescription = (productionType != null) ? productionType.getProductionTypeDescription () : null;
				
				surveyorTileTypeGold.setText ("+" + tileType.getGoldBonus ().toString () + "% " +
					((productionTypeDescription != null) ? productionTypeDescription : CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD));
			}
			
			// Does the tile type stop us from building a city?
			if ((tileType.isCanBuildCity () == null) || (!tileType.isCanBuildCity ()))
			{
				final String tileTypeDescription = (tileTypeLang == null) ? null : tileTypeLang.getTileTypeCannotBuildCityDescription ();
				cityInfo = getLanguage ().findCategoryEntry ("frmSurveyor", "CantBuildCityBecauseOfTerrain").replaceAll
					("TILE_TYPE", (tileTypeDescription != null) ? tileTypeDescription : tileTypeID);
			}
			
			// Effects of the map feature
			final List<String> effects = new ArrayList<String> ();
			
			if (terrainData.getMapFeatureID () != null)
			{
				final MapFeatureLang mapFeatureLang = getLanguage ().findMapFeature (terrainData.getMapFeatureID ());
				final String mapFeatureDescription = (mapFeatureLang != null) ? mapFeatureLang.getMapFeatureDescription () : null;
				surveyorMapFeature.setText ((mapFeatureDescription != null) ? mapFeatureDescription : terrainData.getMapFeatureID ());
				
				// Production bonuses from feature (e.g. +5 gold, +2 mana)
				final MapFeature mapFeature = getClient ().getClientDB ().findMapFeature (terrainData.getMapFeatureID (), "surveyorLocationOrLanguageChanged");
				for (final MapFeatureProduction mapFeatureProduction : mapFeature.getMapFeatureProduction ())
					if (mapFeatureProduction.getDoubleAmount () != 0)
					{
						final ProductionType productionType = getClient ().getClientDB ().findProductionType (mapFeatureProduction.getProductionTypeID (), "surveyorLocationOrLanguageChanged");
						
						final ProductionTypeLang productionTypeLang = getLanguage ().findProductionType (mapFeatureProduction.getProductionTypeID ());
						final String productionTypeDescription = (productionTypeLang != null) ? productionTypeLang.getProductionTypeDescription () : null;
						
						effects.add (getTextUtils ().halfIntToStrPlusMinus (mapFeatureProduction.getDoubleAmount ()) +
							(productionType.isIsPercentage () ? "% " : " ") +
							((productionTypeDescription != null) ? productionTypeDescription : mapFeatureProduction.getProductionTypeID ()));
					}
				
				// Fixed effects of feature
				if ((mapFeature.isFeatureSpellProtection () != null) && (mapFeature.isFeatureSpellProtection ()))
					effects.add (getLanguage ().findCategoryEntry ("frmSurveyor", "FeatureProvidesSpellProtection"));
				
				if ((mapFeatureLang != null) && (mapFeatureLang.getMapFeatureMagicWeaponsDescription () != null))
					effects.add (mapFeatureLang.getMapFeatureMagicWeaponsDescription ());
				
				// Does the tile type stop us from building a city?
				if ((cityInfo == null) && (!mapFeature.isCanBuildCity ()))
				{
					final String mapFeatureCityDescription = (mapFeatureLang != null) ? mapFeatureLang.getMapFeatureCannotBuildCityDescription () : null;
					cityInfo = getLanguage ().findCategoryEntry ("frmSurveyor", "CantBuildCityBecauseOfFeature").replaceAll
						("MAP_FEATURE", (mapFeatureCityDescription != null) ? mapFeatureCityDescription : terrainData.getMapFeatureID ());
				}
			}

			surveyorMapFeatureFirst.setText		((effects.size () < 1) ? null : effects.get (0));
			surveyorMapFeatureSecond.setText	((effects.size () < 2) ? null : effects.get (1));

			if (effects.size () > 2)
				log.warn ("Map feature ID \"" + terrainData.getMapFeatureID () + "\" has more than 2 effects, so can't display them on the surveyor");
		}
		
		// Check distance to other cities
		if (cityInfo == null)
		{
			if (getCityCalculations ().markWithinExistingCityRadius (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
				getSurveyorLocation ().getZ (), getClient ().getSessionDescription ().getMapSize ()).get (getSurveyorLocation ().getX (), getSurveyorLocation ().getY ()))
			{
				cityInfo = getLanguage ().findCategoryEntry ("frmSurveyor", "CantBuildCityTooCloseToAnotherCity").replaceAll
					("CITY_SEPARATION", new Integer (getClient ().getSessionDescription ().getMapSize ().getCitySeparation ()).toString ());
			}
			else
			{
				final CityProductionBreakdown gold = new CityProductionBreakdown ();
				getCityCalculations ().calculateGoldTradeBonus (gold,
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), getSurveyorLocation (), null,
					getClient ().getSessionDescription ().getMapSize (), getClient ().getClientDB ());
				
				cityInfo = getLanguage ().findCategoryEntry ("frmSurveyor", "CanBuildCity").replaceAll
					("MAXIMUM_POPULATION", new Integer (getCityCalculations ().listCityFoodProductionFromTerrainTiles
						(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), getSurveyorLocation (),
						getClient ().getSessionDescription ().getMapSize (), getClient ().getClientDB ()).getDoubleProductionAmount ()).toString () + ",000").replaceAll
					("PRODUCTION_BONUS", new Integer (getCityCalculations ().listCityProductionPercentageBonusesFromTerrainTiles
						(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), getSurveyorLocation (),
						getClient ().getSessionDescription ().getMapSize (), getClient ().getClientDB ()).getPercentageBonus ()).toString ()).replaceAll
					("GOLD_BONUS", new Integer (gold.getTradePercentageBonusFromTileType ()).toString ());
			}
		}
		
		// Output the 3 lines of city info
		// These seem to be read from the XML with only newline chars, and no carriage returns
		final String [] lines = cityInfo.replaceAll ("\r", "").split ("\n");
		
		surveyorCityFirst.setText		((lines.length < 1) ? null : lines [0]);
		surveyorCitySecond.setText	((lines.length < 2) ? null : lines [1]);
		surveyorCityThird.setText	((lines.length < 3) ? null : lines [2]);

		if (lines.length > 3)
			log.warn ("Surveyor city info generated more than 3 lines");

		log.trace ("Exiting surveyorLocationOrLanguageChanged");
	}

	/**
	 * We can't End Turn if we have insufficient rations, gold or mana - this routine checks each of those resources
	 * or other conditions that prevent us from ending turn, such as needing to target a spell.
	 * This regenerates the "disbaled" image state of the button with a new image containing the relevant resource icons, and enables or disables the button.
	 * We don't need to replace the images for the "normal" or "pressed" states, since if the button is enabled then obviously nothing is stopping us from ending turn.
	 * 
	 * @return Text description of why we can't end turn right now; null if we can end turn
	 * @throws IOException If we can't find any of the resource images
	 */
	public final String updateProductionTypesStoppingUsFromEndingTurn () throws IOException
	{
		log.trace ("Entering updateProductionTypesStoppingUsFromEndingTurn");

		final StringBuffer text = new StringBuffer (); 
		
		// This can be ran prior to init even being called
		if (nextTurnButton != null)
		{		
			// Before we bother creating an image that we might not need, first just get a list of all the images we need to put onto the button
			final List<String> resourceIconFilenames = new ArrayList<String> ();
			
			// Now check every defined production type
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "updateProductionTypesStoppingUsFromEndingTurn");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();

			for (final ProductionType productionType : getClient ().getClientDB ().getProductionTypes ())
				if (productionType.getEnforceProduction () != null)
				{
					int valueToCheck = getResourceValueUtils ().calculateAmountPerTurnForProductionType (getClient ().getOurPersistentPlayerPrivateKnowledge (),
						pub.getPick (), productionType.getProductionTypeID (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
					
					if (productionType.getEnforceProduction () == EnforceProductionID.STORED_AMOUNT_CANNOT_GO_BELOW_ZERO)
						valueToCheck = valueToCheck + getResourceValueUtils ().findAmountStoredForProductionType
							(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), productionType.getProductionTypeID ());
					
					// If gold in a simultaneous turns game, we will get gold at the end of the turn from selling buildings - we need to allow this
					// e.g. 0 gold stored, -3 gold production per turnHowever we're selling a builders' hall - this alters our per turn production to -2 but this
					// still isn't good enough - However, we'll also get 20 gold from the sale so that's fine, we'll have 18 gold for next turn, so have to allow this.
					if ((productionType.getProductionTypeID ().equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD)) &&
						(getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS))
						
						// Search whole map for all cities with pending building sales
						for (int plane = 0; plane < getClient ().getSessionDescription ().getMapSize ().getDepth (); plane++)
							for (int y = 0; y < getClient ().getSessionDescription ().getMapSize ().getHeight (); y++)
								for (int x = 0; x < getClient ().getSessionDescription ().getMapSize ().getWidth (); x++)
								{
									final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
										(plane).getRow ().get (y).getCell ().get (x);
									final OverlandMapCityData cityData = mc.getCityData ();
										
									if ((cityData != null) && (getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ())) && (mc.getBuildingIdSoldThisTurn () != null))
										valueToCheck = valueToCheck + getMemoryBuildingUtils ().goldFromSellingBuilding
											(getClient ().getClientDB ().findBuilding (mc.getBuildingIdSoldThisTurn (), "updateProductionTypesStoppingUsFromEndingTurn"));
								}						
	                
	                // Do we have a problem?
					if (valueToCheck < 0)
					{
						resourceIconFilenames.add (getGraphicsDB ().findProductionType
							(productionType.getProductionTypeID (), "updateProductionTypesStoppingUsFromEndingTurn").findProductionValueImageFile ("1"));
						
						final ProductionTypeLang productionTypeLang = getLanguage ().findProductionType (productionType.getProductionTypeID ());
						final String msg = (productionTypeLang == null) ? null : productionTypeLang.getCannotEndTurnDueToLackOfProduction ();
						text.append (BULLET_POINT + ((msg != null) ? msg : productionType.getProductionTypeID ()) + System.lineSeparator ()); 
					}
				}
			
			// Must choose a spell to research?
			if ((getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null) &&
				(getSpellUtils ().getSpellsForStatus (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellResearchStatus (),
					SpellResearchStatusID.RESEARCHABLE_NOW, getClient ().getClientDB ()).size () > 0) &&
				(getResourceValueUtils ().calculateAmountPerTurnForProductionType (getClient ().getOurPersistentPlayerPrivateKnowledge (),
					pub.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ()) > 0))
			{
				resourceIconFilenames.add (getGraphicsDB ().findProductionType
					(CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "updateProductionTypesStoppingUsFromEndingTurn").findProductionValueImageFile ("1"));
				
				text.append (BULLET_POINT + getLanguage ().findCategoryEntry ("frmMapRightHandBar", "CannotEndTurnResearch") + System.lineSeparator ());
			}
			
			// Do we have unanswered new turn messages?
			boolean found = false;
			final Iterator<NewTurnMessageData> iter = getClient ().getOurTransientPlayerPrivateKnowledge ().getNewTurnMessage ().iterator ();
			while ((!found) && (iter.hasNext ()))
			{
				final NewTurnMessageData ntm = iter.next ();
				if (ntm instanceof NewTurnMessageMustBeAnswered)
				{
					if (!((NewTurnMessageMustBeAnswered) ntm).isAnswered ())
						found = true;
				}
			}
			
			if (found)
			{
				resourceIconFilenames.add ("/momime.client.graphics/ui/overland/rightHandPanel/cannotEndTurnDueToNTMs.png");
				text.append (BULLET_POINT + getLanguage ().findCategoryEntry ("frmMapRightHandBar", "CannotEndTurnNTMs") + System.lineSeparator ());
			}
	
			// Regenerate disabled button?
			if (resourceIconFilenames.size () > 0)
			{
				final BufferedImage disabledImage = new BufferedImage (nextTurnButtonDisabled.getWidth (), nextTurnButtonDisabled.getHeight (), BufferedImage.TYPE_INT_ARGB);
				final Graphics g = disabledImage.getGraphics ();
				try
				{
					g.drawImage (nextTurnButtonDisabled, 0, 0, null);
					int nextX = 12;
					for (final String resourceIconFilename : resourceIconFilenames)
					{
						final BufferedImage iconImage = getUtils ().loadImage (resourceIconFilename);
						g.drawImage (iconImage, nextX, 14, null);
						nextX = nextX + iconImage.getWidth () + 2;
					}
				}
				finally
				{
					g.dispose ();
				}
				
				nextTurnButton.setDisabledIcon (new ImageIcon (disabledImage));
			}
			
			// Can we end turn?
			nextTurnAction.setEnabled (resourceIconFilenames.size () == 0);
		}
		
		final String result = text.toString ();
		log.trace ("Exiting updateProductionTypesStoppingUsFromEndingTurn: " + result);
		return result;
	}
	
	/**
	 * @param index The index into the player list of the current player, in a one-at-a-time turns game, so 0 = 1st player, 1 = 2nd and so on, including monsters+raiders players
	 */
	public final void setIndexOfCurrentPlayer (final int index)
	{
		colourPatchDesiredPos = index * COLOUR_PATCH_WIDTH;
	}
	
	/**
	 * @return What is displayed in the variable top section
	 */
	public final OverlandMapRightHandPanelTop getTop ()
	{
		return top;
	}

	/**
	 * @param section What is displayed in the variable top section
	 */
	public final void setTop (final OverlandMapRightHandPanelTop section)
	{
		top = section;
		topCardLayout.show (topCards, section.name ());
	}
	
	/**
	 * @return What is displayed in the variable bottom section
	 */
	public final OverlandMapRightHandPanelBottom getBottom ()
	{
		return bottom;
	}
	
	/**
	 * @param section What is displayed in the variable bottom section
	 */
	public final void setBottom (final OverlandMapRightHandPanelBottom section)
	{
		bottom = section;
		bottomCardLayout.show (bottomCards, section.name ());
	}
	
	/**
	 * @return Select unit buttons
	 */
	public final List<HideableComponent<SelectUnitButton>> getSelectUnitButtons ()
	{
		return selectUnitButtons;
	}

	/**
	 * @param enabled Whether settlers can create a new outpost
	 */
	public final void setCreateOutpostEnabled (final boolean enabled)
	{
		createOutpostAction.setEnabled (enabled);
	}
	
	/**
	 * @param enabled Whether engineers can build a road
	 */
	public final void setBuildRoadEnabled (final boolean enabled)
	{
		buildRoadAction.setEnabled (enabled);
	}
	
	/**
	 * @param enabled Whether spirits can meld with a node
	 */
	public final void setMeldWithNodeEnabled (final boolean enabled)
	{
		meldWithNodeAction.setEnabled (enabled);
	}

	/**
	 * @param enabled Whether priests can purify corrupted lands
	 */
	public final void setPurifyEnabled (final boolean enabled)
	{
		purifyAction.setEnabled (enabled);
	}
	
	/**
	 * @param enabled Whether units can go on patrol so as not to be asked for orders in future turns
	 */
	public final void setPatrolEnabled (final boolean enabled)
	{
		patrolAction.setEnabled (enabled);
	}

	/**
	 * @return Whether units can be done to not take an action for this turn
	 */
	public final boolean isDoneEnabled ()
	{
		return doneAction.isEnabled ();
	}
	
	/**
	 * @param enabled Whether units can be done to not take an action for this turn
	 */
	public final void setDoneEnabled (final boolean enabled)
	{
		doneAction.setEnabled (enabled);
	}

	/**
	 * @return NTM about the spell being targetted
	 */
	public final NewTurnMessageSpellEx getTargetSpell ()
	{
		return targetSpell;
	}
	
	/**
	 * @param msg NTM about the spell being targetted
	 */
	public final void setTargetSpell (final NewTurnMessageSpellEx msg)
	{
		targetSpell = msg;
		languageChanged ();
	}

	/**
	 * @return Map location currently being surveyed; can be null to blank out all the surveyor labels
	 */
	public final MapCoordinates3DEx getSurveyorLocation ()
	{
		return surveyorLocation;
	}

	/**
	 * @param loc Map location currently being surveyed; can be null to blank out all the surveyor labels
	 * @throws RecordNotFoundException If we encounter a map element that can't be found in the DB 
	 * @throws MomException If we encounter data that there isn't appropriate fields to display 
	 */
	public final void setSurveyorLocation (final MapCoordinates3DEx loc) throws RecordNotFoundException, MomException
	{
		surveyorLocation = loc;
		surveyorLocationOrLanguageChanged ();
	}

	/**
	 * @return Next turn action
	 */
	public final Action getNextTurnAction ()
	{
		return nextTurnAction;
	}
	
	/**
	 * @return Done action
	 */
	public final Action getDoneAction ()
	{
		return doneAction;
	}
	
	/**
	 * @return Wait action
	 */
	public final Action getWaitAction ()
	{
		return waitAction;
	}
	
	/**
	 * @return Settlers creating a new outpost
	 */
	public final Action getCreateOutpostAction ()
	{
		return createOutpostAction;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}

	/**
	 * @return Pending movement utils
	 */
	public final PendingMovementUtils getPendingMovementUtils ()
	{
		return pendingMovementUtils;
	}

	/**
	 * @param utils Pending movement utils
	 */
	public final void setPendingMovementUtils (final PendingMovementUtils utils)
	{
		pendingMovementUtils = utils;
	}

	/**
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
	
	/**
	 * @return UI component factory
	 */
	public final UIComponentFactory getUiComponentFactory ()
	{
		return uiComponentFactory;
	}

	/**
	 * @param factory UI component factory
	 */
	public final void setUiComponentFactory (final UIComponentFactory factory)
	{
		uiComponentFactory = factory;
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
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
	}

	/**
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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
	 * @return XML layout for the surveyor subpanel
	 */
	public final XmlLayoutContainerEx getSurveyorLayout ()
	{
		return surveyorLayout;
	}

	/**
	 * @param layout XML layout for the surveyor subpanel
	 */
	public final void setSurveyorLayout (final XmlLayoutContainerEx layout)
	{
		surveyorLayout = layout;
	}
}
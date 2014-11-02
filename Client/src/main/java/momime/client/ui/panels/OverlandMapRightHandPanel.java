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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import momime.client.MomClient;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.SpellBookSection;
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
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.clienttoserver.CancelPendingMovementAndSpecialOrdersMessage;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PendingMovement;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitSpecialOrder;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.ResourceValueUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * The right hand panel has a switchable top section and switchable bottom section (see the two enums),
 * implemented with two CardLayouts.  Setting that up is fairly involved, and OverlandMapUI is complicated
 * enough already so I wanted to keep the mechanics of this panel separated out into its own class. 
 */
public final class OverlandMapRightHandPanel extends MomClientPanelUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OverlandMapRightHandPanel.class);
	
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
	
	/** Title for targetting spells */
	private JLabel targetSpellTitle;
	
	/** Text saying what's being targetted */
	private JTextArea targetSpellText;
	
	/** NTM about the spell being targetted */
	private NewTurnMessageSpellEx targetSpell;
	
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
		final BufferedImage cancelBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/oneButton.png");
		final BufferedImage specialOrdersBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/fourButtons.png");
		
		// Fix the size of the panel to be the same as the background
		final Dimension backgroundSize = new Dimension (background.getWidth (), background.getHeight ());
		getPanel ().setMinimumSize (backgroundSize);
		getPanel ().setMaximumSize (backgroundSize);
		getPanel ().setPreferredSize (backgroundSize);
		
		// Actions
		final Action nextTurnAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent ev)
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
			public void actionPerformed (final ActionEvent ev)
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
			public void actionPerformed (final ActionEvent ev)
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
			public void actionPerformed (final ActionEvent ev)
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
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		buildRoadAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		meldWithNodeAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		purifyAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		cancelAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
				msg.setTitleLanguageCategoryID ("SpellTargetting");
				msg.setTitleLanguageEntryID ("CancelTitle");
				msg.setTextLanguageCategoryID ("SpellTargetting");
				msg.setTextLanguageEntryID ("CancelText");
				msg.setCancelTargettingSpell (getTargetSpell ());
				try
				{
					msg.setVisible (true);
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
		
		final JPanel colourPatches = new JPanel ();
		colourPatches.setBackground (new Color (0x800000));
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
		
		surveyorPanel.setLayout (new GridBagLayout ());

		final JLabel surveyorTitle = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.SILVER, getLargeFont ());
		surveyorTitle.setText ("Surveyor");
		surveyorPanel.add (surveyorTitle, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		final JLabel surveyorTileType = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		surveyorTileType.setText ("Forest");
		surveyorPanel.add (surveyorTileType, getUtils ().createConstraintsNoFill (0, 1, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		final JLabel surveyorTileTypeFood = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorTileTypeFood.setText ("Unknown food");
		surveyorPanel.add (surveyorTileTypeFood, getUtils ().createConstraintsNoFill (0, 2, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		final JLabel surveyorTileTypeProduction = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorTileTypeProduction.setText ("Unknown production");
		surveyorPanel.add (surveyorTileTypeProduction, getUtils ().createConstraintsNoFill (0, 3, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		final JLabel surveyorTileTypeGold = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorTileTypeGold.setText ("Unknown gold");
		surveyorPanel.add (surveyorTileTypeGold, getUtils ().createConstraintsNoFill (0, 4, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final JLabel surveyorMapFeature = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		surveyorMapFeature.setText ("Adamantium");
		surveyorPanel.add (surveyorMapFeature, getUtils ().createConstraintsNoFill (0, 5, 1, 1, new Insets (22, 0, 0, 0), GridBagConstraintsNoFill.CENTRE));

		final JLabel surveyorMapFeatureFirst = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorMapFeatureFirst.setText ("First effect");
		surveyorPanel.add (surveyorMapFeatureFirst, getUtils ().createConstraintsNoFill (0, 6, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		final JLabel surveyorMapFeatureSecond = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorMapFeatureSecond.setText ("Second effect");
		surveyorPanel.add (surveyorMapFeatureSecond, getUtils ().createConstraintsNoFill (0, 7, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final JLabel surveyorCityTitle = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		surveyorCityTitle.setText ("City Resources");
		surveyorPanel.add (surveyorCityTitle, getUtils ().createConstraintsNoFill (0, 8, 1, 1, new Insets (22, 0, 0, 0), GridBagConstraintsNoFill.CENTRE));

		final JLabel surveyorCityFood = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorCityFood.setText ("Max population 25,000");
		surveyorPanel.add (surveyorCityFood, getUtils ().createConstraintsNoFill (0, 9, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final JLabel surveyorCityProduction = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorCityProduction.setText ("Production bonus +50%");
		surveyorPanel.add (surveyorCityProduction, getUtils ().createConstraintsNoFill (0, 10, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final JLabel surveyorCityGold = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		surveyorCityGold.setText ("Gold trade bonus +30%");
		surveyorPanel.add (surveyorCityGold, getUtils ().createConstraintsNoFill (0, 11, 1, 1, new Insets (0, 0, 5, 0), GridBagConstraintsNoFill.CENTRE));

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
		bottomCards.add (getUtils ().createImageButton (nextTurnAction, null, null, null, nextTurnButtonNormal, nextTurnButtonPressed, nextTurnButtonNormal),
			OverlandMapRightHandPanelBottom.NEXT_TURN_BUTTON.name ());
		
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
							if (ev.getButton () != MouseEvent.BUTTON1)
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
		
		// Fill in variable labels
		updateGlobalEconomyValues ();
		
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

		targetSpellTitle.setText (getLanguage ().findCategoryEntry ("frmMapRightHandBar", "TargetSpell"));
		
		// A bit more involved to set the spell targetting text correctly
		if (getTargetSpell () != null)
			try
			{
				final momime.client.language.database.v0_9_5.Spell spellLang = getLanguage ().findSpell (getTargetSpell ().getSpellID ());
				final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
				
				final Spell spell = getClient ().getClientDB ().findSpell (getTargetSpell ().getSpellID (), "OverlandMapRightHandPanel");
				final SpellBookSection section = getLanguage ().findSpellBookSection (spell.getSpellBookSectionID ());
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
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		turnSystemOrCurrentPlayerChanged ();
		
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
		
			final ProductionType productionType = getLanguage ().findProductionType (productionTypeID);
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
			
			final ProductionType productionType = getLanguage ().findProductionType (productionTypeID);
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
		updateAmountStored (goldAmountStored, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD);
		updateAmountStored (manaAmountStored, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		
		// Amounts per turn
		updateAmountPerTurn (goldAmountPerTurn, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD, "ProductionPerTurn", MomUIConstants.GOLD);
		updateAmountPerTurn (manaAmountPerTurn, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, "ProductionPerTurn", MomUIConstants.GOLD);
		updateAmountPerTurn (rationsAmountPerTurn, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RATIONS, "ProductionPerTurn", MomUIConstants.GOLD);
		updateAmountPerTurn (magicPowerAmountPerTurn, CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER, "ProductionPerTurnMagicPower", MomUIConstants.SILVER);
		
		log.trace ("Exiting updateGlobalEconomyValues");
	}

	/**
	 * Update labels that depend both on the language, and on the turn system and/or current player
	 */
	public final void turnSystemOrCurrentPlayerChanged ()
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
					final PlayerPublicDetails currentPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ());
					playerLine2.setText ((currentPlayer != null) ? currentPlayer.getPlayerDescription ().getPlayerName () : "Player ID " + getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ());
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
}
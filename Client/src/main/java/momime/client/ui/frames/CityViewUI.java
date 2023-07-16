package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.ModifiedImageCache;
import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;
import com.ndg.utils.swing.zorder.ZOrderGraphicsImmediateImpl;

import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.graphics.AnimationContainer;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.panels.CityViewPanel;
import momime.client.ui.renderer.MemoryMaintainedSpellListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.CityProductionCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.Building;
import momime.common.database.CityViewElement;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.LanguageText;
import momime.common.database.RaceEx;
import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.internal.OutpostDeathChanceBreakdown;
import momime.common.internal.OutpostGrowthChanceBreakdown;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.ChangeOptionalFarmersMessage;
import momime.common.messages.clienttoserver.SellBuildingMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SampleUnitUtils;
import momime.common.utils.UnitVisibilityUtils;

/**
 * City screen, so you can view current buildings, production and civilians, examine
 * calculation breakdowns and change production and civilian task assignments
 */
public final class CityViewUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CityViewUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx cityViewLayout;
	
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;

	/** Small font */
	private Font smallFont;
	
	/** Panel where all the buildings are drawn */
	private CityViewPanel cityViewPanel;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** City calculations */
	private CityCalculations cityCalculations;

	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;
	
	/** Client city calculations */
	private ClientCityCalculations clientCityCalculations;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Resource value client utils */
	private ResourceValueClientUtils resourceValueClientUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;

	/** Methods dealing with checking whether we can see units or not */
	private UnitVisibilityUtils unitVisibilityUtils;

	/** Utils for drawing units */
	private UnitClientUtils unitClientUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;

	/** Animation controller */
	private AnimationController anim;
	
	/** UI component factory */
	private UIComponentFactory uiComponentFactory;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Renderer for the enchantments list */
	private MemoryMaintainedSpellListCellRenderer memoryMaintainedSpellListCellRenderer;

	/** Bitmap generator */
	private OverlandMapBitmapGenerator overlandMapBitmapGenerator;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;

	/** Help text scroll */
	private HelpUI helpUI;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Combat UI */
	private CombatUI combatUI;

	/** Sample unit method */
	private SampleUnitUtils sampleUnitUtils;
	
	/** For creating resized images */
	private ModifiedImageCache modifiedImageCache;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/** Tiny 1 pixel inset */
	private final static int TINY_INSET = 1;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** City size+name label */
	private JLabel cityNameLabel;
	
	/** Race label */
	private JLabel raceLabel;
	
	/** Current population label */
	private Action currentPopulationAction;
	
	/** Maximum population label */
	private Action maximumPopulationAction;
	
	/** Resources label */
	private JLabel resourcesLabel;
	
	/** Enchantments/Curses label */
	private JLabel enchantmentsLabel;
	
	/** Terrain label */
	private JLabel terrainLabel;
	
	/** Buildings label */
	private JLabel buildings;
	
	/** Units label */
	private JLabel units;
	
	/** Production label */
	private JLabel production;

	/** Production number of turns label*/
	private JLabel productionTurns;
	
	/** Rush buy action */
	private Action rushBuyAction;
	
	/** Change construction action */
	private Action changeConstructionAction;
	
	/** OK (close) action */
	private Action okAction;

	/** Rename city action */
	private Action renameAction;
	
	/** Rename city button */
	private JButton renameButton;
	
	/** Panel where all the civilians are drawn */
	private JPanel civilianPanel;
	
	/** Panel where all the production icons are drawn */
	private JPanel productionPanel;
	
	/** Panel where we show the image of what we're currently constructing */
	private JPanel constructionPanel;
	
	/** Sample unit to display in constructionpanel */
	private ExpandedUnitDetails sampleUnit;
	
	/** Dynamically created select unit buttons */
	private List<SelectUnitButton> selectUnitButtons = new ArrayList<SelectUnitButton> ();
	
	/** Items in the Enchantments box */
	private DefaultListModel<Object> spellsItems;
	
	/** Enchantments list box */
	private JList<Object> spellsList;
	
	/** Bitmaps for each animation frame of the mini map */
	private BufferedImage [] miniMapBitmaps;

	/** Bitmap for the shading at the edges of the area we can see in the mini map */
	private BufferedImage fogOfWarBitmap;
	
	/** Panel showing the terrain around the city */
	private JPanel miniMapPanel;
	
	/** Panel that covers up an area of the screen if it isn't our city */
	private JPanel notOursPanel;

	/** Panel that covers up an area of the screen if we can't see what this city is constructing */
	private JPanel cannotSeeConstructionPanel;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/background.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonDisabled.png");
		
		final BufferedImage progressCoinDone = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/productionProgressDone.png");
		final BufferedImage progressCoinNotDone = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/productionProgressNotDone.png");
		
		final BufferedImage notOurs = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/notOurs.png");
		final BufferedImage cannotSeeConstruction = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cannotSeeConstruction.png");
		final BufferedImage resourceArea = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/resourceArea.png");

		final XmlLayoutComponent constructionProgressPanelSize = getCityViewLayout ().findComponent ("frmCityConstructionProgress");
		
		// What's the maximum number of progress coins we can fit in the box
		// Assume a gap of 1 between each coin
		final int coinsTotal = ((constructionProgressPanelSize.getWidth () + 1) / (progressCoinNotDone.getWidth () + 1)) *
			((constructionProgressPanelSize.getHeight () + 1) / (progressCoinNotDone.getHeight () + 1));
		
		// So how many production points must each coin represent in order for the most expensive building to still fit in the box?
		// Need to round up
		final int productionProgressDivisor = (getClient ().getClientDB ().getMostExpensiveConstructionCost () + coinsTotal - 1) / coinsTotal;
		
		// Actions
		rushBuyAction = new LoggingAction ((ev) -> getClientCityCalculations ().showRushBuyPrompt (getCityLocation ()));

		final CityViewUI ui = this;
		changeConstructionAction = new LoggingAction ((ev) ->
		{
			// Is there a change construction window already open for this city?
			ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (getCityLocation ().toString ());
			if (changeConstruction == null)
			{
				changeConstruction = getPrototypeFrameCreator ().createChangeConstruction ();
				changeConstruction.setCityLocation (new MapCoordinates3DEx (getCityLocation ()));
				getClient ().getChangeConstructions ().put (getCityLocation ().toString (), changeConstruction);
			}
				
			changeConstruction.setVisible (true);
		});
		
		okAction = new LoggingAction ((ev) -> getFrame ().dispose ());

		renameAction = new LoggingAction ((ev) ->
		{
			final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
			
			final EditStringUI askForCityName = getPrototypeFrameCreator ().createEditString ();
			askForCityName.setLanguageTitle (getLanguages ().getNameCityScreen ().getTitle ());
			askForCityName.setLanguagePrompt (getLanguages ().getNameCityScreen ().getPrompt ());
			askForCityName.setCityBeingNamed (getCityLocation ());
			askForCityName.setText (cityData.getCityName ());
			askForCityName.setVisible (true);
		});
		
		// Explain the max size calculation
		maximumPopulationAction = new LoggingAction ((ev) ->
		{
			final CityProductionBreakdown breakdown = getCityProductionCalculations ().calculateAllCityProductions (getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
				getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), true, false, getClient ().getClientDB ()).findProductionType
					(CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
			
			final String productionTypeDescription = getLanguageHolder ().findDescription
				(getClient ().getClientDB ().findProductionType (breakdown.getProductionTypeID (), "CityViewUI").getProductionTypeDescription ());
			
			final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
			calc.setTitle (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getTitle ()).replaceAll
				("CITY_SIZE_AND_NAME", getFrame ().getTitle ()).replaceAll
				("PRODUCTION_TYPE", productionTypeDescription));
			calc.setText (getClientCityCalculations ().describeCityProductionCalculation (breakdown));
			calc.setVisible (true);
		}); 

		// Explain the city growth calculation
		currentPopulationAction = new LoggingAction ((ev) ->
		{
			final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
			
			final int maxCitySize = getCityCalculations ().calculateSingleCityProduction (getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
				getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), true, getClient ().getClientDB (),
				CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
		
			final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
			if (cityData.getCityPopulation () >= 1000)
			{
				// Normal city growth/death rate calculation
				final CityGrowthRateBreakdown breakdown = getCityCalculations ().calculateCityGrowthRate (getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (), maxCitySize,
					getClient ().getSessionDescription ().getDifficultyLevel (), getClient ().getClientDB ());

				calc.setTitle (getLanguageHolder ().findDescription (getLanguages ().getCityGrowthRate ().getTitle ()).replaceAll ("CITY_SIZE_AND_NAME", getFrame ().getTitle ()));
				calc.setText (getClientCityCalculations ().describeCityGrowthRateCalculation (breakdown));
			}
			else
			{
				// Outpost growth/death chance calculation
				final OutpostGrowthChanceBreakdown growthBreakdown = getCityCalculations ().calculateOutpostGrowthChance
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), getCityLocation (), maxCitySize,
						getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getClientDB ());

				final OutpostDeathChanceBreakdown deathBreakdown = getCityCalculations ().calculateOutpostDeathChance
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), getCityLocation (), getClient ().getClientDB ());
				
				calc.setTitle (getLanguageHolder ().findDescription (getLanguages ().getOutpostGrowthChance ().getTitle ()).replaceAll ("CITY_SIZE_AND_NAME", getFrame ().getTitle ()));
				calc.setText (getClientCityCalculations ().describeOutpostGrowthAndDeathChanceCalculation (growthBreakdown, deathBreakdown));
			}

			calc.setVisible (true);
		});
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				try
				{
					getAnim ().unregisterRepaintTrigger (null, constructionPanel);
					getCityViewPanel ().cityViewClosing ();
				}
				catch (final MomException e)
				{
					log.error (e, e);
				}
				
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				getClient ().getCityViews ().remove (getCityLocation ().toString ());
			}
		});
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getCityViewLayout ()));

		// Set up the city view panel first so its in front of the "not ours" panel
		getCityViewPanel ().setCityLocation (getCityLocation ());
		contentPane.add (getCityViewPanel (), "frmCityView");
		
		// OK button is also in front of the "not ours" panel
		contentPane.add (getUtils ().createImageButton (okAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCityOK");
		
		// Set up panels to cover up production images and buttons on cities that aren't ours.
		// The ordering of when we add this is significant - it must be behind the area where we draw the city (all the buildings) but in front of all the production buttons.
		notOursPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (notOurs, 0, 0, notOurs.getWidth () * 2, notOurs.getHeight () * 2, null);
			}
		};

		cannotSeeConstructionPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (cannotSeeConstruction, 0, 0, cannotSeeConstruction.getWidth () * 2, cannotSeeConstruction.getHeight () * 2, null);
			}
		};
		
		contentPane.add (cannotSeeConstructionPanel, "frmCityCannotSeeConstruction");
		contentPane.add (notOursPanel, "frmCityNotOurs");
		
		// Labels
		cityNameLabel = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (cityNameLabel, "frmCityName");
		
		resourcesLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (resourcesLabel, "frmCityResources");
		
		enchantmentsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (enchantmentsLabel, "frmCityEnchantmentsLabel");
		
		terrainLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (terrainLabel, "frmCityTerrainLabel");
		
		buildings = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (buildings, "frmCityBuildings");
		
		production = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (production, "frmCityProductionLabel");

		productionTurns = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (productionTurns, "frmCityProductionTurns");

		units = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (units, "frmCityUnits");
		
		raceLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (raceLabel, "frmCityRace");

		contentPane.add (getUtils ().createTextOnlyButton (currentPopulationAction, MomUIConstants.GOLD, getMediumFont ()), "frmCityGrowth");
		contentPane.add (getUtils ().createTextOnlyButton (maximumPopulationAction, MomUIConstants.GOLD, getMediumFont ()), "frmCityMaxCitySize");
		
		// Buttons
		contentPane.add (getUtils ().createImageButton (rushBuyAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCityBuy");
		contentPane.add (getUtils ().createImageButton (changeConstructionAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCityChange");
		
		renameButton = getUtils ().createImageButton (renameAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled);
		contentPane.add (renameButton, "frmCityRename");
		
		// Set up the mini terrain panel
		miniMapPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Draw the terrain
				g.drawImage (miniMapBitmaps [getOverlandMapUI ().getTerrainAnimFrame ()], 0, 0, null);
						
				// Shade the fog of war edges
				if (fogOfWarBitmap != null)
					g.drawImage (fogOfWarBitmap, 0, 0, null);
				
				// Draw a ring around the area the city gathers resources from
				g.drawImage (resourceArea, 19, 17, null);
			}
		};
		miniMapPanel.setBackground (Color.BLACK);
		contentPane.add (miniMapPanel, "frmCityMiniMap");

		miniMapPanel.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isLeftMouseButton (ev))
					getOverlandMapUI ().scrollTo (getCityLocation ().getX (), getCityLocation ().getY (), getCityLocation ().getZ (), true);
			}
		});
		
		// Set up the mini panel to hold all the civilians - this also has a 2 pixel gap to correct the labels above it
		civilianPanel = new JPanel ();
		civilianPanel.setOpaque (false);
		civilianPanel.setLayout (new GridBagLayout ());
		
		contentPane.add (civilianPanel, "frmCityCivilians");
		
		// Set up the mini panel to hold all the productions
		productionPanel = new JPanel ();
		productionPanel.setOpaque (false);
		productionPanel.setLayout (new GridBagLayout ());
		
		contentPane.add (productionPanel, "frmCityProduction");
		
		// Set up the mini panel to hold all the enchantments
		getMemoryMaintainedSpellListCellRenderer ().setFont (getSmallFont ());
		
		spellsItems = new DefaultListModel<Object> ();
		spellsList = new JList<Object> ();
		spellsList.setOpaque (false);
		spellsList.setModel (spellsItems);
		spellsList.setCellRenderer (getMemoryMaintainedSpellListCellRenderer ());
		spellsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		final JScrollPane spellsScrollPane = getUtils ().createTransparentScrollPane (spellsList);
		spellsScrollPane.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellsScrollPane, "frmCityEnchantments");
		
		// Clicking a spell asks about cancelling it
		spellsList.addListSelectionListener ((ev) ->
		{
			if (spellsList.getSelectedIndex () >= 0)
			{
				final Object obj = spellsItems.get (spellsList.getSelectedIndex ());
				if (obj instanceof MemoryMaintainedSpell)
				{
					final MemoryMaintainedSpell spell = (MemoryMaintainedSpell) obj;
					try
					{
						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setLanguageTitle (getLanguages ().getSpellCasting ().getSwitchOffSpellTitle ());
	
						if (!getClient ().isPlayerTurn ())
							msg.setLanguageText (getLanguages ().getSpellCasting ().getSwitchOffSpellNotYourTurn ());
						else if (getCombatUI ().isVisible ())
							msg.setLanguageText (getLanguages ().getSpellCasting ().getSwitchOffSpellInCombat ());
						else
						{
							final String effectName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findCitySpellEffect (spell.getCitySpellEffectID (), "CityViewUI").getCitySpellEffectName ());
							
							if (spell.getCastingPlayerID () != getClient ().getOurPlayerID ())
								msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getSwitchOffSpellNotOurs ()).replaceAll ("SPELL_NAME", effectName));
							else
							{
								msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getSwitchOffSpell ()).replaceAll ("SPELL_NAME", effectName));
								msg.setSwitchOffSpell (spell);
							}
						}
	
						msg.setVisible (true);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		});
		
		// Right clicking on city spell effects shows help text about them
		spellsList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					final int row = spellsList.locationToIndex (ev.getPoint ());
					if ((row >= 0) && (row < spellsItems.size ()))
					{
						final Object obj = spellsItems.get (row);
						if (obj instanceof MemoryMaintainedSpell)
						{
							final MemoryMaintainedSpell spell = (MemoryMaintainedSpell) obj;
							try
							{
								getHelpUI ().showCitySpellEffectID (spell.getCitySpellEffectID (), spell.getSpellID (),
									getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), spell.getCastingPlayerID (), "citySpellEffectHelp"));
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
						}
					}
				}
			}
		});
		
		// Set up the mini panel to show progress towards current construction
		final JPanel constructionProgressPanel = new JPanel ()
		{
			/**
			 * Draws coins appropriate for how far through construction we are
			 */
			@Override
			protected final void paintComponent (final Graphics g)
			{
				try
				{
					// Find the cost of what's being built
					final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
					final OverlandMapCityData cityData = mc.getCityData ();

					if (getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ()))
					{
						// How many coins does it take to draw this (round up)
						final Integer productionCost = getCityProductionCalculations ().calculateProductionCost (getClient ().getPlayers (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
							getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), getClient ().getClientDB (), null);
						
						if (productionCost != null)
						{
							// How many coins does it take to draw this? (round up)
							final int totalCoins = (productionCost + productionProgressDivisor - 1) / productionProgressDivisor;
							
							// How many of those coins should be coloured in for what we've built so far? (round down, so things don't have every coin filled in but not completed)
							final int goldCoins = (cityData.getProductionSoFar () == null) ? 0 : (cityData.getProductionSoFar () / productionProgressDivisor);
							
							// Draw the coins
							int x = 0;
							int y = 0;
							
							for (int n = 0; n < totalCoins; n++)
							{
								// Draw this one
								g.drawImage ((n < goldCoins) ? progressCoinDone : progressCoinNotDone, x, y, null);
								
								// Move to next spot
								x = x + progressCoinDone.getWidth () + 1;
								if (x + progressCoinDone.getWidth () > constructionProgressPanelSize.getWidth ())
								{
									x = 0;
									y = y + progressCoinDone.getHeight () + 1;
								}
							}
						}
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		contentPane.add (constructionProgressPanel, "frmCityConstructionProgress");
		
		// Set up the mini panel to what's being currently constructed
		final ZOrderGraphicsImmediateImpl zOrderGraphics = new ZOrderGraphicsImmediateImpl ();
		constructionPanel = new JPanel ()
		{
			/**
			 * Draws whatever is currently selected to construct
			 */
			@Override
			protected final void paintComponent (final Graphics g)
			{
				final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
				try
				{
					// Draw building
					if (cityData.getCurrentlyConstructingBuildingID () != null)
					{
						final CityViewElement buildingImage = getClient ().getClientDB ().findCityViewElementBuilding (cityData.getCurrentlyConstructingBuildingID (), "constructionPanel");
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame
							((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
							buildingImage.getCityViewAnimation (), true, AnimationContainer.COMMON_XML);
					
						g.drawImage (image, (getSize ().width - image.getWidth ()) / 2, (getSize ().height - image.getHeight ()) / 2, null);
					}

					// Draw unit
					if (sampleUnit != null)
					{
						zOrderGraphics.setGraphics (g);
						final String movingActionID = getUnitCalculations ().determineCombatActionID (sampleUnit, true, getClient ().getClientDB ());
						getUnitClientUtils ().drawUnitFigures (sampleUnit, movingActionID, 4, zOrderGraphics, (constructionPanel.getWidth () - 60) / 2, 28, true, true, 0, null, null, false);
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		constructionPanel.setOpaque (false);
		contentPane.add (constructionPanel, "frmCityConstruction");

		// Deal with clicking on buildings to sell them
		getCityViewPanel ().addBuildingListener ((buildingID) ->
		{
			// If the city isn't ours then don't even show a message
			final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
			final OverlandMapCityData cityData = mc.getCityData ();
			if ((cityData != null) && (getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ())))
			{
				// If cancelling a pending sale, there's no lookups or confirmations or anything to do, just send the message
				if (buildingID == null)
				{
					// But ignore clicking the gold coin in one-player-at-a-time turn games - then the building is already sold, we can't change our mind
					if (getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
					{
						final SellBuildingMessage msg = new SellBuildingMessage ();
						msg.setCityLocation (getCityLocation ());
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
				}
				else
				{					
					// Language entry ID of error or confirmation message
					final List<LanguageText> languageText;
					String prerequisiteBuildingName = null;
					boolean ok = false;
					
					// How much money do we get for selling it?
					// If this is zero, then they're trying to do something daft like sell their Wizard's Fortress or Summoning Circle
					final Building buildingDef = getClient ().getClientDB ().findBuilding (buildingID, "buildingClicked");
					final int goldValue = getMemoryBuildingUtils ().goldFromSellingBuilding (buildingDef);
					if (goldValue <= 0)
						languageText = getLanguages ().getBuyingAndSellingBuildings ().getCannotSellSpecialBuilding ();
					
					// We can only sell one building a turn
					else if (mc.getBuildingIdSoldThisTurn () != null)
						languageText = getLanguages ().getBuyingAndSellingBuildings ().getOnlySellOneEachTurn ();
					
					else
					{
						// We can't sell a building if another building depends on it, e.g. trying to sell a Granary when we already have a Farmers' Market
						final String prerequisiteBuildingID = getMemoryBuildingUtils ().doAnyBuildingsDependOn (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
							getCityLocation (), buildingID, getClient ().getClientDB ());
						if (prerequisiteBuildingID != null)
						{
							languageText = getLanguages ().getBuyingAndSellingBuildings ().getCannotSellRequiredByAnother ();
							prerequisiteBuildingName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (prerequisiteBuildingID, "buildingClicked").getBuildingName ());
						}
						else
						{
							// OK - but first check if current construction project depends on the one we're selling
							// If so, then we can still sell it, but it will cancel our current construction project
							ok = true;
							if (((cityData.getCurrentlyConstructingBuildingID () != null) &&
									(getMemoryBuildingUtils ().isBuildingAPrerequisiteForBuilding (buildingID, cityData.getCurrentlyConstructingBuildingID (), getClient ().getClientDB ()))) ||
								((cityData.getCurrentlyConstructingUnitID () != null) &&
									(getMemoryBuildingUtils ().isBuildingAPrerequisiteForUnit (buildingID, cityData.getCurrentlyConstructingUnitID (), getClient ().getClientDB ()))))
							{
								languageText = getLanguages ().getBuyingAndSellingBuildings ().getSellPromptPrerequisite ();
								if (cityData.getCurrentlyConstructingBuildingID () != null)
									prerequisiteBuildingName = getLanguageHolder ().findDescription
										(getClient ().getClientDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "buildingClicked").getBuildingName ());
								else if (cityData.getCurrentlyConstructingUnitID () != null)
									prerequisiteBuildingName = getLanguageHolder ().findDescription
										(getClient ().getClientDB ().findUnit (cityData.getCurrentlyConstructingUnitID (), "buildingClicked").getUnitName ());
							}
							else
								languageText = getLanguages ().getBuyingAndSellingBuildings ().getSellPromptNormal ();
						}
					}
					
					// Work out the text for the message box
					String text = getLanguageHolder ().findDescription (languageText).replaceAll
						("BUILDING_NAME", getLanguageHolder ().findDescription (buildingDef.getBuildingName ())).replaceAll
						("PRODUCTION_VALUE", getTextUtils ().intToStrCommas (goldValue));
					
					if (prerequisiteBuildingName != null)
						text = text.replaceAll ("PREREQUISITE_NAME", prerequisiteBuildingName);
					
					// Show message box
					final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
					msg.setLanguageTitle (getLanguages ().getBuyingAndSellingBuildings ().getSellTitle ());
					msg.setText (text);
					
					if (ok)
					{
						final MemoryBuilding sellBuilding = getMemoryBuildingUtils ().findBuilding
							(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (), buildingID);
						if (sellBuilding == null)
							log.error ("Can't find building with ID " + buildingID + " in city " + getCityLocation () + " to sell even though it was clicked on");
						else
						{						
							msg.setCityLocation (getCityLocation ());
							msg.setBuildingURN (sellBuilding.getBuildingURN ());
						}
					}
					
					msg.setVisible (true);
				}
			}				
		});

		cityDataChanged ();
		unitsChanged ();
		spellsChanged ();
		regenerateCityViewMiniMapBitmaps ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
	}
	
	/**
	 * Regenerates the buttons along the bottom showing the units that are in the city
	 * @throws IOException If a resource cannot be found
	 */
	public final void unitsChanged () throws IOException
	{
		for (final SelectUnitButton button : selectUnitButtons)
			contentPane.remove (button);
		
		selectUnitButtons.clear ();
		
		int x = 0;
		for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
			if ((cityLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE) && (getUnitVisibilityUtils ().canSeeUnitOverland
				(mu, getClient ().getOurPlayerID (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), getClient ().getClientDB ())))
			{
				if (x < CommonDatabaseConstants.MAX_UNITS_PER_MAP_CELL)
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					final SelectUnitButton selectUnitButton = getUiComponentFactory ().createSelectUnitButton ();
					selectUnitButton.init ();
					selectUnitButton.setUnit (xu);
					selectUnitButton.setSelected (true);		// Just so the owner's background colour appears
	
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
										unitInfo.setUnit (selectUnitButton.getUnit ().getMemoryUnit ());
										getClient ().getUnitInfos ().put (selectUnitButton.getUnit ().getUnitURN (), unitInfo);
									}
								
									unitInfo.setVisible (true);
								}
								else
								{
									// Left click shows the units in the city in the right hand panel of the overland map
									getOverlandMapProcessing ().showSelectUnitBoxes (getCityLocation ());
									
									// Don't actually deselect the button
									selectUnitButton.setSelected (!selectUnitButton.isSelected ());
								}
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
						}
					});
	
					selectUnitButtons.add (selectUnitButton);
					contentPane.add (selectUnitButton, "frmCitySelectUnitButton." + x);
					x++;
				}
				else
				{
					x++;
					log.warn ("Ran out of select unit boxes when trying to show units in city at " + cityLocation + " (found " + x + " units)");
				}
			}

		contentPane.revalidate ();
		contentPane.repaint ();
	}
	
	/**
	 * Update the list of enchantments and curses cast on this city whenever they change
	 * @throws RecordNotFoundException If there is an event in progress that we can't find
	 */
	public final void spellsChanged () throws RecordNotFoundException
	{
		spellsItems.clear ();
		
		for (final MemoryMaintainedSpell spell : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ())
			if (getCityLocation ().equals (spell.getCityLocation ()))
				spellsItems.addElement (spell);

		final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
		final OverlandMapCityData cityData = mc.getCityData ();
		if (cityData.getPopulationEventID () != null)
			spellsItems.addElement (getClient ().getClientDB ().findEvent (cityData.getPopulationEventID (), "spellsChanged"));
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Fixed labels
		resourcesLabel.setText		(getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getResources ()));
		enchantmentsLabel.setText	(getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getEnchantments ()));
		terrainLabel.setText			(getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getTerrain ()));
		buildings.setText				(getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getBuildings ()));
		units.setText						(getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getUnits ()));
		production.setText				(getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getProduction ()));
		
		// Actions
		rushBuyAction.putValue					(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getRushBuy ()));
		changeConstructionAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getChangeConstruction ()));
		okAction.putValue							(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
		renameAction.putValue					(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getRename ()));
		
		languageOrCityDataChanged ();
		
		// Spell names are dynamically looked up by the ListCellRenderer, so just force a repaint
		spellsList.repaint ();
	}

	/**
	 * Performs any updates that need to be redone when the cityData changes - principally this means the population may have changed, so we
	 * need to redraw all the civilians, but also production may have changed from the number of farmers/workers/etc changing.
	 * 
	 * @throws IOException If there is a problem
	 */
	public final void cityDataChanged () throws IOException
	{
		civilianPanel.removeAll ();
		productionPanel.removeAll ();

		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		final RaceEx race = getClient ().getClientDB ().findRace (cityData.getCityRaceID (), "cityDataChanged");
		
		// Start with farmers
		Image civilianImage = getModifiedImageCache ().doubleSize (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, "cityDataChanged"));
		final int civvyCount = cityData.getCityPopulation () / 1000;
		int x = 0;
		for (int civvyNo = 1; civvyNo <= civvyCount; civvyNo++)
		{
			// Is this the first rebel?
			if (civvyNo == civvyCount - cityData.getNumberOfRebels () + 1)
				civilianImage = getModifiedImageCache ().doubleSize (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_REBEL, "cityDataChanged"));
			
			// Is this the first worker?
			else if (civvyNo == cityData.getMinimumFarmers () + cityData.getOptionalFarmers () + 1)
				civilianImage = getModifiedImageCache ().doubleSize (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER, "cityDataChanged"));
			
			// Is this civilian changeable (between farmer and worker) - if so, create a button for them instead of a plain image
			final Action action;
			if ((civvyNo > civvyCount - cityData.getNumberOfRebels ()) ||	// Rebels
				(civvyNo <= cityData.getMinimumFarmers ()))						// Enforced farmers
			{
				// Create as a 'show unrest calculation' button
				action = new LoggingAction ((ev) ->
				{
					final CityUnrestBreakdown breakdown = getCityCalculations ().calculateCityRebels (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
						getCityLocation (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getClientDB ());
					
					final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
					calc.setTitle (getLanguageHolder ().findDescription (getLanguages ().getUnrestCalculation ().getTitle ()).replaceAll ("CITY_SIZE_AND_NAME", getFrame ().getTitle ()));
					calc.setText (getClientCityCalculations ().describeCityUnrestCalculation (breakdown));
					calc.setVisible (true);							
				}); 
			}
			else
			{
				// Create as an 'optional farmers' button
				final int civvyNoCopy = civvyNo;
				action = new LoggingAction ((ev) ->
				{
					// Clicking on the same number toggles it, so we can turn the last optional farmer into a worker
					int optionalFarmers = civvyNoCopy - cityData.getMinimumFarmers ();
					if (optionalFarmers == cityData.getOptionalFarmers ())
						optionalFarmers--;
					
					log.debug ("Requesting optional farmers in city " + getCityLocation () + " to be set to " + optionalFarmers);
					
					final ChangeOptionalFarmersMessage msg = new ChangeOptionalFarmersMessage ();
					msg.setCityLocation (getCityLocation ());
					msg.setOptionalFarmers (optionalFarmers);
					getClient ().getServerConnection ().sendMessageToServer (msg);
				}); 
			}
			
			// Left justify all the civilians
			final GridBagConstraints imageConstraints = getUtils ().createConstraintsNoFill (x, 0, 1, 1, TINY_INSET, GridBagConstraintsNoFill.SOUTHWEST);
			if (civvyNo == civvyCount)
				imageConstraints.weightx = 1;
			
			civilianPanel.add (getUtils ().createImageButton (action, null, null, null, civilianImage, civilianImage, civilianImage), imageConstraints);
			x++;
			
			// If this is the last farmer who's necessary to feed the population (& so we cannot convert them to a worker) then leave a gap to show this
			if (civvyNo == cityData.getMinimumFarmers ())
			{
				civilianPanel.add (Box.createRigidArea (new Dimension (10, 0)), getUtils ().createConstraintsNoFill (x, 0, 1, 1, INSET, GridBagConstraintsNoFill.SOUTHWEST));
				x++;
			}
		}
		
		// Display all productions which have graphics, i.e. Rations / Production / Gold / Power / Research
		int ypos = 0;
		for (final CityProductionBreakdown thisProduction : getCityProductionCalculations ().calculateAllCityProductions (getClient ().getPlayers (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
			getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), true, false, getClient ().getClientDB ()).getProductionType ())
		{
			final BufferedImage buttonImage = getResourceValueClientUtils ().generateProductionImage (thisProduction.getProductionTypeID (),
				thisProduction.getCappedProductionAmount () + thisProduction.getConvertToProductionAmount (), thisProduction.getConsumptionAmount ());
			
			if (buttonImage != null)
			{
				// Explain this production calculation
				final Action productionAction = new LoggingAction ((ev) ->
				{
					final CityProductionBreakdown breakdown = getCityProductionCalculations ().calculateAllCityProductions (getClient ().getPlayers (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
						getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), true, false, getClient ().getClientDB ()).findProductionType
							(thisProduction.getProductionTypeID ());
						
					final String productionTypeDescription = getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findProductionType (breakdown.getProductionTypeID (), "CityViewUI").getProductionTypeDescription ());
						
					final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
					calc.setTitle (getLanguageHolder ().findDescription (getLanguages ().getCityProduction ().getTitle ()).replaceAll
						("CITY_SIZE_AND_NAME", getFrame ().getTitle ()).replaceAll
						("PRODUCTION_TYPE", productionTypeDescription));
					calc.setText (getClientCityCalculations ().describeCityProductionCalculation (breakdown));
					calc.setVisible (true);
				}); 
					
				// Create the button - leave 5 gap underneath before the next button
				productionPanel.add (getUtils ().createImageButton (productionAction, null, null, null, buttonImage, buttonImage, buttonImage),
					getUtils ().createConstraintsNoFill (0, ypos, 1, 1, new Insets (0, 0, 5, 0), GridBagConstraintsNoFill.WEST));
				ypos++;
			}
		}
		
		// Push all the production images to the top-level
		final GridBagConstraints fillerConstraints = getUtils ().createConstraintsBothFill (0, ypos, 1, 1, INSET);
		fillerConstraints.weightx = 1;
		fillerConstraints.weighty = 1;
		productionPanel.add (Box.createRigidArea (new Dimension (0, 0)), fillerConstraints);
		
		// Find what we're currently constructing
		getAnim ().unregisterRepaintTrigger (null, constructionPanel);
		
		if (cityData.getCurrentlyConstructingBuildingID () != null)
			getAnim ().registerRepaintTrigger (getClient ().getClientDB ().findCityViewElementBuilding
				(cityData.getCurrentlyConstructingBuildingID (), "cityDataChanged").getCityViewAnimation (), constructionPanel, AnimationContainer.COMMON_XML);
		
		if (cityData.getCurrentlyConstructingUnitID () == null)
			sampleUnit = null;
		else
		{
			// Create a dummy unit here, rather than on every paintComponent call
			sampleUnit = getSampleUnitUtils ().createSampleUnit (cityData.getCurrentlyConstructingUnitID (), cityData.getCityOwnerID (), null,
				getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
			
			final String movingActionID = getUnitCalculations ().determineCombatActionID (sampleUnit, true, getClient ().getClientDB ());
			getUnitClientUtils ().registerUnitFiguresAnimation (cityData.getCurrentlyConstructingUnitID (), movingActionID, 4, constructionPanel);
		}
		
		constructionPanel.repaint ();

		civilianPanel.revalidate ();
		civilianPanel.repaint ();
		productionPanel.revalidate ();
		productionPanel.repaint ();
		
		languageOrCityDataChanged ();
		
		// Is it ours or not (note this can change - the city view might already be open when a city is captured).
		// Also we have to take care to disable the buttons - just showing the panel doesn't stop you from clicking the buttons that its covering up.
		notOursPanel.setVisible ((!getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ())) || (cityData.getCityPopulation () < 1000));
		production.setVisible (!notOursPanel.isVisible ());
		changeConstructionAction.setEnabled (!notOursPanel.isVisible ());
		renameButton.setVisible (getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ()));
		
		// Can we see what this city is constructing?
		cannotSeeConstructionPanel.setVisible ((!getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ())) &&
			(!getClient ().getSessionDescription ().getFogOfWarSetting ().isSeeEnemyCityConstruction ()));
		
		// Must do this after setting the "not ours" panel visibility, since it uses it
		recheckRushBuyEnabled ();
		
		// Rebuild render data from scratch, plus if one is an animation then calling init () again will ensure it gets registered properly
		getCityViewPanel ().setRenderCityData (getCityCalculations ().buildRenderCityData (getCityLocation (),
			getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ()));
		getCityViewPanel ().init ();
	}
	
	/**
	 * Performs updates that depend both on the city data and the language file
	 */
	private final void languageOrCityDataChanged ()
	{
		// Get details about the city
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		if (cityData != null)
			try
			{
				String cityName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findCitySize
					(cityData.getCitySizeID (), "CityViewUI").getCitySizeNameIncludingOwner ()).replaceAll ("CITY_NAME", cityData.getCityName ());
	
				final PlayerPublicDetails cityOwner = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), cityData.getCityOwnerID ());
				if (cityOwner != null)
					cityName = cityName.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (cityOwner));
				
				cityNameLabel.setText (cityName);
				getFrame ().setTitle (cityName);
				
				raceLabel.setText (getLanguageHolder ().findDescription (getClient ().getClientDB ().findRace (cityData.getCityRaceID (), "CityViewUI").getRaceNameSingular ()));
			
				// Max city size
				final CityProductionBreakdownsEx productions = getCityProductionCalculations ().calculateAllCityProductions (getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
					getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), true, false, getClient ().getClientDB ());
			
				final CityProductionBreakdown maxCitySizeProd = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
				final int maxCitySize = (maxCitySizeProd == null) ? 0 : maxCitySizeProd.getCappedProductionAmount ();
			
				maximumPopulationAction.putValue (Action.NAME,
					getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getMaxCitySize ()).replaceAll ("MAX_CITY_SIZE",
					getTextUtils ().intToStrCommas (maxCitySize * 1000)));
			
				// Growth rate
				final String cityPopulation = getTextUtils ().intToStrCommas (cityData.getCityPopulation ());
				if (cityData.getCityPopulation () < 1000)
					currentPopulationAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getPopulationOutpost ()).replaceAll
						("POPULATION", cityPopulation));
				else if (cityData.getCityPopulation () == maxCitySize * 1000)
					currentPopulationAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getPopulationMaxed ()).replaceAll
						("POPULATION", cityPopulation));
				else
				{
					final CityGrowthRateBreakdown cityGrowthBreakdown = getCityCalculations ().calculateCityGrowthRate (getClient ().getPlayers (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (), maxCitySize,
						getClient ().getSessionDescription ().getDifficultyLevel (), getClient ().getClientDB ());
				
					final int cityGrowth = cityGrowthBreakdown.getCappedTotal ();
				
					currentPopulationAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getCityScreen ().getPopulationAndGrowth ()).replaceAll
						("POPULATION", cityPopulation).replaceAll ("GROWTH_RATE", getTextUtils ().intToStrPlusMinus (cityGrowth)));
				}
				
				// Turns until construction completed
				Integer turns = null;
				if ((getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ())) && (cityData.getCityPopulation () >= 1000))
					turns = getClientCityCalculations ().calculateProductionTurnsRemaining (getCityLocation ());
				
				if (turns != null)
					productionTurns.setText (getLanguageHolder ().findDescription
						((turns == 1) ? getLanguages ().getCityScreen ().getProductionTurn () : getLanguages ().getCityScreen ().getProductionTurns ()).replaceAll
							("NUMBER_OF_TURNS", turns.toString ()));
				
				productionTurns.setVisible (turns != null);
			}
			catch (final IOException e)
			{
				log.error (e, e);
			}
	}
	
	/**
	 * Forces the grey/gold coins to update to show how much has now been constructed
	 */
	public final void productionSoFarChanged ()
	{
		// Since the panel is transparent and doesn't completely draw itself, can end up with garbage
		// showing if we literally just redraw the panel, so need to redraw the whole screen
		getFrame ().repaint ();
	}
	
	/**
	 * @throws PlayerNotFoundException If we can't find the player who owns the city
	 * @throws RecordNotFoundException If we can't find the building or unit being constructed
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	public final void recheckRushBuyEnabled () throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		boolean rushBuyEnabled = false;
		if (!notOursPanel.isVisible ())
		{
			final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
			final OverlandMapCityData cityData = mc.getCityData ();
	
			final Integer productionCost = getCityProductionCalculations ().calculateProductionCost (getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCityLocation (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (),
				getClient ().getGeneralPublicKnowledge ().getConjunctionEventID (), getClient ().getClientDB (), null);
			
			if (productionCost != null)
			{
				final int goldToRushBuy = getCityCalculations ().goldToRushBuy (productionCost, (cityData.getProductionSoFar () == null) ? 0 : cityData.getProductionSoFar ());
				rushBuyEnabled = (goldToRushBuy > 0) && (goldToRushBuy <= getResourceValueUtils ().findAmountStoredForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD));
			}
		}		
		rushBuyAction.setEnabled (rushBuyEnabled);
	}

	/**
	 * Generates bitmaps of little area of the overland map immediately surrounding this city, in each frame of animation.
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateCityViewMiniMapBitmaps () throws IOException
	{
		// This might move us off the top or left of the map and get -ve coordinates if they aren't wrapping edges, but that's fine, the bitmap generator copes with that
		final MapCoordinates3DEx mapTopLeft = new MapCoordinates3DEx (getCityLocation ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		
		miniMapBitmaps = getOverlandMapBitmapGenerator ().generateOverlandMapBitmaps (mapTopLeft.getZ (), mapTopLeft.getX (), mapTopLeft.getY (), 7, 7);
	}
	
	/**
	 * Repaints the mini map view when the animation frame ticks
	 */
	public final void repaintCityViewMiniMap ()
	{
		miniMapPanel.repaint ();
	}
	
	/**
	 * Generates a bitmap of the fog of war in the little area of the overland map immediately surrounding this city.
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateCityViewMiniMapFogOfWar () throws IOException
	{
		final MapCoordinates3DEx mapTopLeft = new MapCoordinates3DEx (getCityLocation ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		
		fogOfWarBitmap = getOverlandMapBitmapGenerator ().generateFogOfWarBitmap (mapTopLeft.getZ (), mapTopLeft.getX (), mapTopLeft.getY (), 7, 7);
	}
	
	/**
	 * Close the city screen when a city is destroyed 
	 */
	public final void close ()
	{
		getFrame ().dispose ();
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCityViewLayout ()
	{
		return cityViewLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCityViewLayout (final XmlLayoutContainerEx layout)
	{
		cityViewLayout = layout;
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
	 * @return Panel where all the buildings are drawn
	 */
	public final CityViewPanel getCityViewPanel ()
	{
		return cityViewPanel;
	}

	/**
	 * @param pnl Panel where all the buildings are drawn
	 */
	public final void setCityViewPanel (final CityViewPanel pnl)
	{
		cityViewPanel = pnl;
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
	 * @return City production calculations
	 */
	public final CityProductionCalculations getCityProductionCalculations ()
	{
		return cityProductionCalculations;
	}

	/**
	 * @param c City production calculations
	 */
	public final void setCityProductionCalculations (final CityProductionCalculations c)
	{
		cityProductionCalculations = c;
	}
	
	/**
	 * @return Client city calculations
	 */
	public final ClientCityCalculations getClientCityCalculations ()
	{
		return clientCityCalculations;
	}

	/**
	 * @param calc Client city calculations
	 */
	public final void setClientCityCalculations (final ClientCityCalculations calc)
	{
		clientCityCalculations = calc;
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
	 * @return Resource value client utils
	 */
	public final ResourceValueClientUtils getResourceValueClientUtils ()
	{
		return resourceValueClientUtils;
	}

	/**
	 * @param utils Resource value client utils
	 */
	public final void setResourceValueClientUtils (final ResourceValueClientUtils utils)
	{
		resourceValueClientUtils = utils;
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param mbu Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils mbu)
	{
		memoryBuildingUtils = mbu;
	}
	
	/**
	 * @return Variable replacer for outputting skill descriptions
	 */
	public final UnitStatsLanguageVariableReplacer getUnitStatsReplacer ()
	{
		return unitStatsReplacer;
	}

	/**
	 * @param replacer Variable replacer for outputting skill descriptions
	 */
	public final void setUnitStatsReplacer (final UnitStatsLanguageVariableReplacer replacer)
	{
		unitStatsReplacer = replacer;
	}

	/**
	 * @return Methods dealing with checking whether we can see units or not
	 */
	public final UnitVisibilityUtils getUnitVisibilityUtils ()
	{
		return unitVisibilityUtils;
	}

	/**
	 * @param u Methods dealing with checking whether we can see units or not
	 */
	public final void setUnitVisibilityUtils (final UnitVisibilityUtils u)
	{
		unitVisibilityUtils = u;
	}
	
	/**
	 * @return Utils for drawing units
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Utils for drawing units
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
	 * @return The city being viewed
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param loc The city being viewed
	 */
	public final void setCityLocation (final MapCoordinates3DEx loc)
	{
		cityLocation = loc;
	}

	/**
	 * @return Animation controller
	 */
	public final AnimationController getAnim ()
	{
		return anim;
	}

	/**
	 * @param controller Animation controller
	 */
	public final void setAnim (final AnimationController controller)
	{
		anim = controller;
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
	 * @return Renderer for the enchantments list
	 */
	public final MemoryMaintainedSpellListCellRenderer getMemoryMaintainedSpellListCellRenderer ()
	{
		return memoryMaintainedSpellListCellRenderer;
	}

	/**
	 * @param renderer Renderer for the enchantments list
	 */
	public final void setMemoryMaintainedSpellListCellRenderer (final MemoryMaintainedSpellListCellRenderer renderer)
	{
		memoryMaintainedSpellListCellRenderer = renderer;
	}

	/**
	 * @return Bitmap generator
	 */
	public final OverlandMapBitmapGenerator getOverlandMapBitmapGenerator ()
	{
		return overlandMapBitmapGenerator;
	}

	/**
	 * @param gen Bitmap generator
	 */
	public final void setOverlandMapBitmapGenerator (final OverlandMapBitmapGenerator gen)
	{
		overlandMapBitmapGenerator = gen;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param csu Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils csu)
	{
		coordinateSystemUtils = csu;
	}

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}

	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return Dynamically created select unit buttons
	 */
	public final List<SelectUnitButton> getSelectUnitButtons ()
	{
		return selectUnitButtons;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
	}

	/**
	 * @return Sample unit method
	 */
	public final SampleUnitUtils getSampleUnitUtils ()
	{
		return sampleUnitUtils;
	}

	/**
	 * @param s Sample unit method
	 */
	public final void setSampleUnitUtils (final SampleUnitUtils s)
	{
		sampleUnitUtils = s;
	}

	/**
	 * @return For creating resized images
	 */
	public final ModifiedImageCache getModifiedImageCache ()
	{
		return modifiedImageCache;
	}

	/**
	 * @param m For creating resized images
	 */
	public final void setModifiedImageCache (final ModifiedImageCache m)
	{
		modifiedImageCache = m;
	}
}
package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import momime.client.MomClient;
import momime.client.calculations.ClientCityCalculations;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RaceGfx;
import momime.client.language.database.BuildingLang;
import momime.client.language.database.CitySpellEffectLang;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.RaceLang;
import momime.client.language.database.UnitLang;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.panels.BuildingListener;
import momime.client.ui.panels.CityViewPanel;
import momime.client.ui.renderer.MemoryMaintainedSpellListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.ChangeOptionalFarmersMessage;
import momime.common.messages.clienttoserver.SellBuildingMessage;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * City screen, so you can view current buildings, production and civilians, examine
 * calculation breakdowns and change production and civilian task assignments
 */
public final class CityViewUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (CityViewUI.class);
	
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
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** City calculations */
	private CityCalculations cityCalculations;

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

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Utils for drawing units */
	private UnitClientUtils unitClientUtils;
	
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
	
	/** Client unit calculations */
	private ClientUnitCalculations clientUnitCalculations;

	/** Help text scroll */
	private HelpUI helpUI;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
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

	/** Rush buy action */
	private Action rushBuyAction;
	
	/** Change construction action */
	private Action changeConstructionAction;
	
	/** OK (close) action */
	private Action okAction;
	
	/** Panel where all the civilians are drawn */
	private JPanel civilianPanel;
	
	/** Panel where all the production icons are drawn */
	private JPanel productionPanel;
	
	/** Panel where we show the image of what we're currently constructing */
	private JPanel constructionPanel;
	
	/** Sample unit to display in constructionpanel */
	private AvailableUnit sampleUnit;
	
	/** Dynamically created select unit buttons */
	private List<SelectUnitButton> selectUnitButtons = new ArrayList<SelectUnitButton> ();
	
	/** Items in the Enchantments box */
	private DefaultListModel<MemoryMaintainedSpell> spellsItems;
	
	/** Enchantments list box */
	private JList<MemoryMaintainedSpell> spellsList;
	
	/** Bitmaps for each animation frame of the mini map */
	private BufferedImage [] miniMapBitmaps;

	/** Bitmap for the shading at the edges of the area we can see in the mini map */
	private BufferedImage fogOfWarBitmap;
	
	/** Panel showing the terrain around the city */
	private JPanel miniMapPanel;
	
	/** Panel that covers up an area of the screen if it isn't our city */
	private JPanel notOursPanel;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getCityLocation ());
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/background.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonDisabled.png");
		
		final BufferedImage progressCoinDone = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/productionProgressDone.png");
		final BufferedImage progressCoinNotDone = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/productionProgressNotDone.png");
		
		final BufferedImage notOurs = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/notOurs.png");
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
		rushBuyAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					// Get the text to display
					String text = getLanguage ().findCategoryEntry ("BuyingAndSellingBuildings", "RushBuyPrompt");
					
					// How much will it cost us to rush buy it?
					final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
					final OverlandMapCityData cityData = mc.getCityData ();

					Integer productionCost = null;
					if (cityData.getCurrentlyConstructingBuildingID () != null)
					{
						productionCost = getClient ().getClientDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "rushBuyAction").getProductionCost ();
						final BuildingLang building = getLanguage ().findBuilding (cityData.getCurrentlyConstructingBuildingID ());
						text = text.replaceAll ("A_UNIT_NAME", (building != null) ? building.getBuildingName () : cityData.getCurrentlyConstructingBuildingID ());
					}

					else if (cityData.getCurrentlyConstructingUnitID () != null)
					{
						productionCost = getClient ().getClientDB ().findUnit (cityData.getCurrentlyConstructingUnitID (), "rushBuyAction").getProductionCost ();

						final AvailableUnit unit = new AvailableUnit ();
						unit.setUnitID (cityData.getCurrentlyConstructingUnitID ());
						getUnitStatsReplacer ().setUnit (unit);

						text = getUnitStatsReplacer ().replaceVariables (text);
					}
					
					if (productionCost != null)
					{
						final int goldToRushBuy = getCityCalculations ().goldToRushBuy (productionCost, (mc.getProductionSoFar () == null) ? 0 : mc.getProductionSoFar ());
						text = text.replaceAll ("PRODUCTION_VALUE", getTextUtils ().intToStrCommas (goldToRushBuy));
					
						// Now show the message
						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setTitleLanguageCategoryID ("BuyingAndSellingBuildings");
						msg.setTitleLanguageEntryID ("RushBuyTitle");
						msg.setText (text);
						msg.setCityLocation (getCityLocation ());
						msg.setVisible (true);
					}
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		};

		final CityViewUI ui = this;
		changeConstructionAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				// Is there a change construction window already open for this city?
				ChangeConstructionUI changeConstruction = getClient ().getChangeConstructions ().get (getCityLocation ().toString ());
				if (changeConstruction == null)
				{
					changeConstruction = getPrototypeFrameCreator ().createChangeConstruction ();
					changeConstruction.setCityLocation (new MapCoordinates3DEx (getCityLocation ()));
					getClient ().getChangeConstructions ().put (getCityLocation ().toString (), changeConstruction);
				}
				
				try
				{
					changeConstruction.setVisible (true);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		};
		
		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		
		// Explain the max size calculation
		maximumPopulationAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					final CityProductionBreakdown breakdown = getCityCalculations ().calculateAllCityProductions
						(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, false, getClient ().getClientDB ()).findProductionType
							(CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
					
					final ProductionTypeLang productionType = getLanguage ().findProductionType (breakdown.getProductionTypeID ());
					final String productionTypeDescription = (productionType == null) ? breakdown.getProductionTypeID () : productionType.getProductionTypeDescription ();
					
					final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
					calc.setTitle (getLanguage ().findCategoryEntry ("CityProduction", "Title").replaceAll
						("CITY_SIZE_AND_NAME", getFrame ().getTitle ()).replaceAll
						("PRODUCTION_TYPE", productionTypeDescription));
					calc.setText (getClientCityCalculations ().describeCityProductionCalculation (breakdown));
					calc.setVisible (true);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		}; 

		// Explain the city growth calculation
		currentPopulationAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					final int maxCitySize = getCityCalculations ().calculateSingleCityProduction
						(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, getClient ().getClientDB (),
						CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
				
					final CityGrowthRateBreakdown breakdown = getCityCalculations ().calculateCityGrowthRate
						(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (), maxCitySize, getClient ().getClientDB ());

					final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
					calc.setTitle (getLanguage ().findCategoryEntry ("CityGrowthRate", "Title").replaceAll ("CITY_SIZE_AND_NAME", getFrame ().getTitle ()));
					calc.setText (getClientCityCalculations ().describeCityGrowthRateCalculation (breakdown));
					calc.setVisible (true);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		};
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
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
		
		// Set up panel to cover up production images and buttons on cities that aren't ours.
		// The ordering of when we add this is significant - it must be behind the area where we draw the city (all the buildings) but in front of all the production buttons.
		notOursPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (notOurs, 0, 0, notOurs.getWidth () * 2, notOurs.getHeight () * 2, null);
			}
		};

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

		units = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (units, "frmCityUnits");
		
		raceLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (raceLabel, "frmCityRace");

		contentPane.add (getUtils ().createTextOnlyButton (currentPopulationAction, MomUIConstants.GOLD, getMediumFont ()), "frmCityGrowth");
		contentPane.add (getUtils ().createTextOnlyButton (maximumPopulationAction, MomUIConstants.GOLD, getMediumFont ()), "frmCityMaxCitySize");
		
		// Buttons
		contentPane.add (getUtils ().createImageButton (rushBuyAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCityBuy");
		contentPane.add (getUtils ().createImageButton (changeConstructionAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCityChange");
		
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
		
		spellsItems = new DefaultListModel<MemoryMaintainedSpell> ();
		spellsList = new JList<MemoryMaintainedSpell> ();
		spellsList.setOpaque (false);
		spellsList.setModel (spellsItems);
		spellsList.setCellRenderer (getMemoryMaintainedSpellListCellRenderer ());
		spellsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		final JScrollPane spellsScrollPane = getUtils ().createTransparentScrollPane (spellsList);
		spellsScrollPane.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellsScrollPane, "frmCityEnchantments");
		
		// Clicking a spell asks about cancelling it
		spellsList.addListSelectionListener (new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				if (spellsList.getSelectedIndex () >= 0)
				{
					final MemoryMaintainedSpell spell = spellsItems.get (spellsList.getSelectedIndex ());
					try
					{
						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setTitleLanguageCategoryID ("SpellCasting");
						msg.setTitleLanguageEntryID ("SwitchOffSpellTitle");

						final CitySpellEffectLang effect = getLanguage ().findCitySpellEffect (spell.getCitySpellEffectID ());
						final String effectName = (effect != null) ? effect.getCitySpellEffectName () : null;
						
						if (spell.getCastingPlayerID () != getClient ().getOurPlayerID ())
							msg.setText (getLanguage ().findCategoryEntry ("SpellCasting", "SwitchOffSpellNotOurs").replaceAll
								("SPELL_NAME", (effectName != null) ? effectName : spell.getCitySpellEffectID ()));
						else
						{
							msg.setText (getLanguage ().findCategoryEntry ("SpellCasting", "SwitchOffSpell").replaceAll
								("SPELL_NAME", (effectName != null) ? effectName : spell.getCitySpellEffectID ()));
							msg.setSwitchOffSpell (spell);
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
						final MemoryMaintainedSpell spell = spellsItems.get (row);
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

					// How many coins does it take to draw this (round up)
					Integer productionCost = null;
					if (cityData.getCurrentlyConstructingBuildingID () != null)
						productionCost = getClient ().getClientDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "constructionProgressPanel").getProductionCost ();

					if (cityData.getCurrentlyConstructingUnitID () != null)
						productionCost = getClient ().getClientDB ().findUnit (cityData.getCurrentlyConstructingUnitID (), "constructionProgressPanel").getProductionCost ();
					
					if (productionCost != null)
					{
						// How many coins does it take to draw this? (round up)
						final int totalCoins = (productionCost + productionProgressDivisor - 1) / productionProgressDivisor;
						
						// How many of those coins should be coloured in for what we've built so far? (round down, so things don't have every coin filled in but not completed)
						final int goldCoins = (mc.getProductionSoFar () == null) ? 0 : (mc.getProductionSoFar () / productionProgressDivisor);
						
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
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		contentPane.add (constructionProgressPanel, "frmCityConstructionProgress");
		
		// Set up the mini panel to what's being currently constructed
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
						final CityViewElementGfx buildingImage = getGraphicsDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "constructionPanel");
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame
							((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
							buildingImage.getCityViewAnimation (), true);
					
						g.drawImage (image, (getSize ().width - image.getWidth ()) / 2, (getSize ().height - image.getHeight ()) / 2, null);
					}

					// Draw unit
					if (sampleUnit != null)
					{
						final String movingActionID = getClientUnitCalculations ().determineCombatActionID (sampleUnit, true);
						getUnitClientUtils ().drawUnitFigures (sampleUnit, movingActionID, 4, g, (constructionPanel.getWidth () - 60) / 2, 28, true, true);
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
		getCityViewPanel ().addBuildingListener (new BuildingListener ()
		{
			@Override
			public final void buildingClicked (final String buildingID) throws Exception
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
						final SellBuildingMessage msg = new SellBuildingMessage ();
						msg.setCityLocation (getCityLocation ());
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					else
					{					
						// Language entry ID of error or confirmation message
						final String languageEntryID;
						String prerequisiteBuildingName = null;
						boolean ok = false;
						
						// How much money do we get for selling it?
						// If this is zero, then they're trying to do something daft like sell their Wizard's Fortress or Summoning Circle
						final int goldValue = getMemoryBuildingUtils ().goldFromSellingBuilding (getClient ().getClientDB ().findBuilding (buildingID, "buildingClicked"));
						if (goldValue <= 0)
							languageEntryID = "CannotSellSpecialBuilding";
						
						// We can only sell one building a turn
						else if (mc.getBuildingIdSoldThisTurn () != null)
							languageEntryID = "OnlySellOneEachTurn";
						
						else
						{
							// We can't sell a building if another building depends on it, e.g. trying to sell a Granary when we already have a Farmers' Market
							final String prerequisiteBuildingID = getMemoryBuildingUtils ().doAnyBuildingsDependOn (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
								getCityLocation (), buildingID, getClient ().getClientDB ());
							if (prerequisiteBuildingID != null)
							{
								languageEntryID = "CannotSellRequiredByAnother";
								final BuildingLang prerequisiteBuilding = getLanguage ().findBuilding (prerequisiteBuildingID);
								prerequisiteBuildingName = (prerequisiteBuilding != null) ? prerequisiteBuilding.getBuildingName () : prerequisiteBuildingID;
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
									languageEntryID = "SellPromptPrerequisite";
									if (cityData.getCurrentlyConstructingBuildingID () != null)
									{
										final BuildingLang currentConstruction = getLanguage ().findBuilding (cityData.getCurrentlyConstructingBuildingID ());
										prerequisiteBuildingName = (currentConstruction != null) ? currentConstruction.getBuildingName () : cityData.getCurrentlyConstructingBuildingID ();
									}
									else if (cityData.getCurrentlyConstructingUnitID () != null)
									{
										final UnitLang currentConstruction = getLanguage ().findUnit (cityData.getCurrentlyConstructingUnitID ());
										prerequisiteBuildingName = (currentConstruction != null) ? currentConstruction.getUnitName () : cityData.getCurrentlyConstructingUnitID ();
									}
								}
								else
									languageEntryID = "SellPromptNormal";
							}
						}
						
						// Work out the text for the message box
						final BuildingLang building = getLanguage ().findBuilding (buildingID);
						String text = getLanguage ().findCategoryEntry ("BuyingAndSellingBuildings", languageEntryID).replaceAll
							("BUILDING_NAME", (building != null) ? building.getBuildingName () : buildingID).replaceAll
							("PRODUCTION_VALUE", getTextUtils ().intToStrCommas (goldValue));
						
						if (prerequisiteBuildingName != null)
							text = text.replaceAll ("PREREQUISITE_NAME", prerequisiteBuildingName);
						
						// Show message box
						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setTitleLanguageCategoryID ("BuyingAndSellingBuildings");
						msg.setTitleLanguageEntryID ("SellTitle");
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
			}
		});

		cityDataChanged ();
		unitsChanged ();
		spellsChanged ();
		regenerateCityViewMiniMapBitmaps ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);

		log.trace ("Exiting init");
	}
	
	/**
	 * Regenerates the buttons along the bottom showing the units that are in the city
	 * @throws IOException If a resource cannot be found
	 */
	public final void unitsChanged () throws IOException
	{
		log.trace ("Entering unitsChanged: " + getCityLocation ());
		
		for (final SelectUnitButton button : selectUnitButtons)
			contentPane.remove (button);
		
		selectUnitButtons.clear ();
		
		int x = 0;
		for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
			if ((cityLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE))
			{
				final SelectUnitButton selectUnitButton = getUiComponentFactory ().createSelectUnitButton ();
				selectUnitButton.init ();
				selectUnitButton.setUnit (mu);
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
									unitInfo.setUnit (selectUnitButton.getUnit ());
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

		contentPane.revalidate ();
		contentPane.repaint ();
		
		log.trace ("Exiting unitsChanged");
	}
	
	/**
	 * Update the list of enchantments and curses cast on this city whenever they change
	 */
	public final void spellsChanged ()
	{
		log.trace ("Entering spellsChanged");
		
		spellsItems.clear ();
		for (final MemoryMaintainedSpell spell : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ())
			if (getCityLocation ().equals (spell.getCityLocation ()))
				spellsItems.addElement (spell);
		
		log.trace ("Exiting spellsChanged");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getCityLocation ());
		
		// Fixed labels
		resourcesLabel.setText		(getLanguage ().findCategoryEntry ("frmCity", "Resources"));
		enchantmentsLabel.setText	(getLanguage ().findCategoryEntry ("frmCity", "Enchantments"));
		terrainLabel.setText			(getLanguage ().findCategoryEntry ("frmCity", "Terrain"));
		buildings.setText				(getLanguage ().findCategoryEntry ("frmCity", "Buildings"));
		units.setText						(getLanguage ().findCategoryEntry ("frmCity", "Units"));
		production.setText				(getLanguage ().findCategoryEntry ("frmCity", "Production"));
		
		// Actions
		rushBuyAction.putValue					(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "RushBuy"));
		changeConstructionAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "ChangeConstruction"));
		okAction.putValue							(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "OK"));
		
		languageOrCityDataChanged ();
		
		// Spell names are dynamically looked up by the ListCellRenderer, so just force a repaint
		spellsList.repaint ();

		log.trace ("Exiting languageChanged");
	}

	/**
	 * Performs any updates that need to be redone when the cityData changes - principally this means the population may have changed, so we
	 * need to redraw all the civilians, but also production may have changed from the number of farmers/workers/etc changing.
	 * 
	 * @throws IOException If there is a problem
	 */
	public final void cityDataChanged () throws IOException
	{
		log.trace ("Entering cityDataChanged: " + getCityLocation ());
		civilianPanel.removeAll ();
		productionPanel.removeAll ();

		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		final RaceGfx race = getGraphicsDB ().findRace (cityData.getCityRaceID (), "cityDataChanged");
		
		// Start with farmers
		Image civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_FARMER, "cityDataChanged")));
		final int civvyCount = cityData.getCityPopulation () / 1000;
		int x = 0;
		for (int civvyNo = 1; civvyNo <= civvyCount; civvyNo++)
		{
			// Is this the first rebel?
			if (civvyNo == civvyCount - cityData.getNumberOfRebels () + 1)
				civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_REBEL, "cityDataChanged")));
			
			// Is this the first worker?
			else if (civvyNo == cityData.getMinimumFarmers () + cityData.getOptionalFarmers () + 1)
				civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.POPULATION_TASK_ID_WORKER, "cityDataChanged")));
			
			// Is this civilian changeable (between farmer and worker) - if so, create a button for them instead of a plain image
			final Action action;
			if ((civvyNo > civvyCount - cityData.getNumberOfRebels ()) ||	// Rebels
				(civvyNo <= cityData.getMinimumFarmers ()))						// Enforced farmers
			{
				// Create as a 'show unrest calculation' button
				action = new AbstractAction ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev)
					{
						try
						{
							final CityUnrestBreakdown breakdown = getCityCalculations ().calculateCityRebels (getClient ().getPlayers (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getClientDB ());
							
							final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
							calc.setTitle (getLanguage ().findCategoryEntry ("UnrestCalculation", "Title").replaceAll ("CITY_SIZE_AND_NAME", getFrame ().getTitle ()));
							calc.setText (getClientCityCalculations ().describeCityUnrestCalculation (breakdown));
							calc.setVisible (true);							
						}
						catch (final IOException e)
						{
							log.error (e, e);
						}
					}
				}; 
			}
			else
			{
				// Create as an 'optional farmers' button
				final int civvyNoCopy = civvyNo;
				action = new AbstractAction ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev)
					{
						// Clicking on the same number toggles it, so we can turn the last optional farmer into a worker
						int optionalFarmers = civvyNoCopy - cityData.getMinimumFarmers ();
						if (optionalFarmers == cityData.getOptionalFarmers ())
							optionalFarmers--;
						
						log.debug ("Requesting optional farmers in city " + getCityLocation () + " to be set to " + optionalFarmers);
						
						try
						{
							final ChangeOptionalFarmersMessage msg = new ChangeOptionalFarmersMessage ();
							msg.setCityLocation (getCityLocation ());
							msg.setOptionalFarmers (optionalFarmers);
							getClient ().getServerConnection ().sendMessageToServer (msg);
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
					}
				}; 
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
		for (final CityProductionBreakdown thisProduction : getCityCalculations ().calculateAllCityProductions
			(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, false, getClient ().getClientDB ()).getProductionType ())
		{
			final BufferedImage buttonImage = getResourceValueClientUtils ().generateProductionImage (thisProduction.getProductionTypeID (),
				thisProduction.getCappedProductionAmount (), thisProduction.getConsumptionAmount ());
			
			if (buttonImage != null)
			{
				// Explain this production calculation
				final Action productionAction = new AbstractAction ()
				{
					@Override
					public final void actionPerformed (final ActionEvent ev)
					{
						try
						{
							final CityProductionBreakdown breakdown = getCityCalculations ().calculateAllCityProductions
								(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
								getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, false, getClient ().getClientDB ()).findProductionType
									(thisProduction.getProductionTypeID ());
								
							final ProductionTypeLang productionType = getLanguage ().findProductionType (breakdown.getProductionTypeID ());
							final String productionTypeDescription = (productionType == null) ? breakdown.getProductionTypeID () : productionType.getProductionTypeDescription ();
								
							final CalculationBoxUI calc = getPrototypeFrameCreator ().createCalculationBox ();
							calc.setTitle (getLanguage ().findCategoryEntry ("CityProduction", "Title").replaceAll
								("CITY_SIZE_AND_NAME", getFrame ().getTitle ()).replaceAll
								("PRODUCTION_TYPE", productionTypeDescription));
							calc.setText (getClientCityCalculations ().describeCityProductionCalculation (breakdown));
							calc.setVisible (true);
						}
						catch (final IOException e)
						{
							log.error (e, e);
						}
					}
				}; 
					
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
			getAnim ().registerRepaintTrigger (getGraphicsDB ().findBuilding
				(cityData.getCurrentlyConstructingBuildingID (), "cityDataChanged").getCityViewAnimation (), constructionPanel);
		
		if (cityData.getCurrentlyConstructingUnitID () == null)
			sampleUnit = null;
		else
		{
			// Create a dummy unit here, rather than on every paintComponent call
			sampleUnit = new AvailableUnit ();
			sampleUnit.setUnitID (cityData.getCurrentlyConstructingUnitID ());

			// We don't have to get the weapon grade or experience right just to draw the figures
			getUnitUtils ().initializeUnitSkills (sampleUnit, null, getClient ().getClientDB ());
			
			final String movingActionID = getClientUnitCalculations ().determineCombatActionID (sampleUnit, true);
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
		notOursPanel.setVisible (!getClient ().getOurPlayerID ().equals (cityData.getCityOwnerID ()));
		production.setVisible (!notOursPanel.isVisible ());
		changeConstructionAction.setEnabled (!notOursPanel.isVisible ());
		
		// Must do this after setting the "not ours" panel visibility, since it uses it
		recheckRushBuyEnabled ();
		
		// The buildings are drawn dynamically, but if one is an animation then calling init () again will ensure it gets registered properly
		getCityViewPanel ().init ();
		
		log.trace ("Exiting cityDataChanged");
	}
	
	/**
	 * Performs updates that depend both on the city data and the language file
	 */
	private final void languageOrCityDataChanged ()
	{
		log.trace ("Entering languageOrCityDataChanged");

		// Get details about the city
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		if (cityData != null)
		{
			final String cityName = getLanguage ().findCitySizeName (cityData.getCitySizeID ()).replaceAll ("CITY_NAME", cityData.getCityName ()); 
			cityNameLabel.setText (cityName);
			getFrame ().setTitle (cityName);
			
			final RaceLang race = getLanguage ().findRace (cityData.getCityRaceID ());
			raceLabel.setText ((race == null) ? cityData.getCityRaceID () : race.getRaceName ());
			
			try
			{
				// Max city size
				final CityProductionBreakdownsEx productions = getCityCalculations ().calculateAllCityProductions
					(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, false, getClient ().getClientDB ());
			
				final CityProductionBreakdown maxCitySizeProd = productions.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_FOOD);
				final int maxCitySize = (maxCitySizeProd == null) ? 0 : maxCitySizeProd.getCappedProductionAmount ();
			
				maximumPopulationAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "MaxCitySize").replaceAll ("MAX_CITY_SIZE",
					getTextUtils ().intToStrCommas (maxCitySize * 1000)));
			
				// Growth rate
				final CityGrowthRateBreakdown cityGrowthBreakdown = getCityCalculations ().calculateCityGrowthRate
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (), maxCitySize, getClient ().getClientDB ());
			
				final int cityGrowth = cityGrowthBreakdown.getFinalTotal ();
				final String cityPopulation = getTextUtils ().intToStrCommas (cityData.getCityPopulation ());
			
				if (cityGrowth == 0)
					currentPopulationAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "PopulationMaxed").replaceAll ("POPULATION", cityPopulation));
				else
					currentPopulationAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "PopulationAndGrowth").replaceAll ("POPULATION", cityPopulation).replaceAll
						("GROWTH_RATE", getTextUtils ().intToStrPlusMinus (cityGrowth)));
			}
			catch (final IOException e)
			{
				log.error (e, e);
			}
		}
		
		log.trace ("Exiting languageOrCityDataChanged");
	}
	
	/**
	 * Forces the grey/gold coins to update to show how much has now been constructed
	 */
	public final void productionSoFarChanged ()
	{
		log.trace ("Entering productionSoFarChanged");
		
		// Since the panel is transparent and doesn't completely draw itself, can end up with garbage
		// showing if we literally just redraw the panel, so need to redraw the whole screen
		getFrame ().repaint ();
		
		log.trace ("Exiting productionSoFarChanged");
	}
	
	/**
	 *  
	 * @throws RecordNotFoundException If we can't find the building or unit being constructed
	 */
	public final void recheckRushBuyEnabled () throws RecordNotFoundException
	{
		log.trace ("Entering recheckRushBuyEnabled");

		boolean rushBuyEnabled = false;
		if (!notOursPanel.isVisible ())
		{
			final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ());
			final OverlandMapCityData cityData = mc.getCityData ();
	
			Integer productionCost = null;
			if (cityData.getCurrentlyConstructingBuildingID () != null)
				productionCost = getClient ().getClientDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "recheckRushBuyEnabled").getProductionCost ();
	
			if (cityData.getCurrentlyConstructingUnitID () != null)
				productionCost = getClient ().getClientDB ().findUnit (cityData.getCurrentlyConstructingUnitID (), "recheckRushBuyEnabled").getProductionCost ();
			
			if (productionCost != null)
			{
				final int goldToRushBuy = getCityCalculations ().goldToRushBuy (productionCost, (mc.getProductionSoFar () == null) ? 0 : mc.getProductionSoFar ());
				rushBuyEnabled = (goldToRushBuy > 0) && (goldToRushBuy <= getResourceValueUtils ().findAmountStoredForProductionType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD));
			}
		}		
		rushBuyAction.setEnabled (rushBuyEnabled);
		
		log.trace ("Exiting recheckRushBuyEnabled = " + rushBuyEnabled);
	}

	/**
	 * Generates bitmaps of little area of the overland map immediately surrounding this city, in each frame of animation.
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateCityViewMiniMapBitmaps () throws IOException
	{
		log.trace ("Entering regenerateCityViewMiniMapBitmaps: " + getCityLocation ());

		// This might move us off the top or left of the map and get -ve coordinates if they aren't wrapping edges, but that's fine, the bitmap generator copes with that
		final MapCoordinates3DEx mapTopLeft = new MapCoordinates3DEx (getCityLocation ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		
		miniMapBitmaps = getOverlandMapBitmapGenerator ().generateOverlandMapBitmaps (mapTopLeft.getZ (), mapTopLeft.getX (), mapTopLeft.getY (), 7, 7);
		
		log.trace ("Exiting regenerateCityViewMiniMapBitmaps");
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
		log.trace ("Entering regenerateCityViewMiniMapFogOfWar: " + getCityLocation ());

		final MapCoordinates3DEx mapTopLeft = new MapCoordinates3DEx (getCityLocation ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getMapSize (), mapTopLeft, SquareMapDirection.NORTHWEST.getDirectionID ());
		
		fogOfWarBitmap = getOverlandMapBitmapGenerator ().generateFogOfWarBitmap (mapTopLeft.getZ (), mapTopLeft.getX (), mapTopLeft.getY (), 7, 7);
		
		log.trace ("Exiting regenerateCityViewMiniMapFogOfWar");
	}
	
	/**
	 * @param source Source image
	 * @return Double sized image
	 */
	private final Image doubleSize (final BufferedImage source)
	{
		return source.getScaledInstance (source.getWidth () * 2, source.getHeight () * 2, Image.SCALE_FAST);
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
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
	 * @return Client unit calculations
	 */
	public final ClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final ClientUnitCalculations calc)
	{
		clientUnitCalculations = calc;
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
}
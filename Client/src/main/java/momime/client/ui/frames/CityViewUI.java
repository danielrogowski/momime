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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.calculations.MomClientCityCalculations;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RaceEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.language.database.v0_9_5.Building;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.Race;
import momime.client.language.database.v0_9_5.Unit;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.components.SelectUnitButton;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.panels.BuildingListener;
import momime.client.ui.panels.CityViewPanel;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.common.MomException;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.clienttoserver.v0_9_5.ChangeOptionalFarmersMessage;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsHorizontalFill;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * City screen, so you can view current buildings, production and civilians, examine
 * calculation breakdowns and change production and civilian task assignments
 */
public final class CityViewUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (CityViewUI.class);
	
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
	private MomCityCalculations cityCalculations;

	/** Client city calculations */
	private MomClientCityCalculations clientCityCalculations;
	
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
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/** Tiny 1 pixel inset */
	private final static int TINY_INSET = 1;
	
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
	
	/** Panel containing all the select unit buttons */
	private JPanel unitPanel;
	
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

		final Dimension constructionProgressPanelSize = new Dimension (132, 62);
		
		// What's the maximum number of progress coins we can fit in the box
		// Assume a gap of 1 between each coin
		final int coinsTotal = ((constructionProgressPanelSize.width + 1) / (progressCoinNotDone.getWidth () + 1)) *
			((constructionProgressPanelSize.height + 1) / (progressCoinNotDone.getHeight () + 1));
		
		// So how many production points must each coin represent in order for the most expensive building to still fit in the box?
		// Need to round up
		final int productionProgressDivisor = (getClient ().getClientDB ().getMostExpensiveConstructionCost () + coinsTotal - 1) / coinsTotal;
		
		// Actions
		rushBuyAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -1438162385777956688L;

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
						final Building building = getLanguage ().findBuilding (cityData.getCurrentlyConstructingBuildingID ());
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
			private static final long serialVersionUID = -3428462848507413383L;

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
			private static final long serialVersionUID = 1562419693690602353L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		
		// Explain the max size calculation
		maximumPopulationAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -6963167374686168788L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					final CityProductionBreakdown breakdown = getCityCalculations ().calculateAllCityProductions
						(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, false, getClient ().getClientDB ()).findProductionType
							(CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
					
					final ProductionType productionType = getLanguage ().findProductionType (breakdown.getProductionTypeID ());
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
			private static final long serialVersionUID = -6963167374686168788L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					final int maxCitySize = getCityCalculations ().calculateSingleCityProduction
						(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, getClient ().getClientDB (),
						CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
				
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
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = -63741181816458999L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		final Dimension fixedSize = new Dimension (background.getWidth () * 2, background.getHeight () * 2);
 		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		// Labels - put any spare space in the title bar, so everything else is pushed down
		final GridBagConstraints cityNameConstraints = getUtils ().createConstraintsNoFill (0, 0, 4, 1, INSET, GridBagConstraintsNoFill.CENTRE);
		cityNameConstraints.weighty = 1;
		
		cityNameLabel = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (cityNameLabel, cityNameConstraints);
		
		resourcesLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (resourcesLabel, getUtils ().createConstraintsNoFill (0, 3, 1, 1, new Insets (0, 8, 0, 0), GridBagConstraintsNoFill.WEST));
		
		enchantmentsLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (enchantmentsLabel, getUtils ().createConstraintsNoFill (1, 3, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		terrainLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (terrainLabel, getUtils ().createConstraintsNoFill (2, 3, 2, 1, new Insets (0, 4, 0, 0), GridBagConstraintsNoFill.WEST));
		
		buildings = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (buildings, getUtils ().createConstraintsNoFill (0, 5, 1, 1, new Insets (0, 8, 0, 0), GridBagConstraintsNoFill.WEST));
		
		production = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (production, getUtils ().createConstraintsNoFill (2, 7, 2, 1, new Insets (0, 4, 0, 0), GridBagConstraintsNoFill.WEST));

		// Make column 1 as wide as possible, so the 3 buttons don't get extra space
		final GridBagConstraints unitsLabelConstraints = getUtils ().createConstraintsNoFill (0, 10, 1, 1, new Insets (0, 8, 0, 0), GridBagConstraintsNoFill.SOUTHWEST);
		unitsLabelConstraints.weightx = 1;
		
		units = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (units, unitsLabelConstraints);
		
		// For some reason I couldn't figure out, trying to put the maximum population label into the main grid kept making
		// the right hand column (including the OK button) too wide and I couldn't make it work right, so instead put
		// the 3 labels into a subpanel so their columns are independant of the rest of the panel
		final GridBagConstraints labelPanelConstraints = getUtils ().createConstraintsHorizontalFill (0, 1, 4, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE);
		
		final JPanel labelsPanel = new JPanel ();
		labelsPanel.setOpaque (false);
		labelsPanel.setLayout (new GridBagLayout ());
		contentPane.add (labelsPanel, labelPanelConstraints);
		
		final GridBagConstraints raceLabelConstraints = getUtils ().createConstraintsNoFill (0, 0, 1, 1, new Insets (0, 8, 0, 0), GridBagConstraintsNoFill.WEST);
		raceLabelConstraints.weightx = 1;
		
		raceLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		labelsPanel.add (raceLabel, raceLabelConstraints);

		labelsPanel.add (getUtils ().createTextOnlyButton (currentPopulationAction, MomUIConstants.GOLD, getMediumFont ()),
			getUtils ().createConstraintsNoFill (1, 0, 1, 1, new Insets (0, 0, 0, 8), GridBagConstraintsNoFill.EAST));
		
		labelsPanel.add (getUtils ().createTextOnlyButton (maximumPopulationAction, MomUIConstants.GOLD, getMediumFont ()),
			getUtils ().createConstraintsNoFill (2, 0, 1, 1, new Insets (0, 0, 0, 8), GridBagConstraintsNoFill.EAST));
		
		// Buttons - put a gap of 10 underneath them to push the buttons slightly above the Units label, and 6 above to push the view panel up
		contentPane.add (getUtils ().createImageButton (rushBuyAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (1, 10, 1, 1, new Insets (6, 0, 10, 6), GridBagConstraintsNoFill.EAST));

		contentPane.add (getUtils ().createImageButton (changeConstructionAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (2, 10, 1, 1, new Insets (6, 0, 10, 6), GridBagConstraintsNoFill.EAST));

		contentPane.add (getUtils ().createImageButton (okAction, Color.BLACK, MomUIConstants.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraintsNoFill (3, 10, 1, 1, new Insets (6, 0, 10, 8), GridBagConstraintsNoFill.EAST));
		
		// Set up the city view panel
		getCityViewPanel ().setCityLocation (getCityLocation ());
		
		final GridBagConstraints cityViewPanelConstraints = getUtils ().createConstraintsNoFill (0, 6, 2, 1, new Insets (0, 8, 0, 0), GridBagConstraintsNoFill.CENTRE);
		cityViewPanelConstraints.gridheight = 4;
		
		contentPane.add (getCityViewPanel (), cityViewPanelConstraints);
		
		// Set up the mini terrain panel
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "OverlandMapUI.init");
		final Dimension miniTerrainPanelSize = new Dimension (overlandMapTileSet.getTileWidth () * 7, overlandMapTileSet.getTileHeight () * 7);
		
		final JPanel miniTerrainPanel = new JPanel ();
		miniTerrainPanel.setMinimumSize (miniTerrainPanelSize);
		miniTerrainPanel.setMaximumSize (miniTerrainPanelSize);
		miniTerrainPanel.setPreferredSize (miniTerrainPanelSize);

		final GridBagConstraints miniTerrainPanelConstraints = getUtils ().createConstraintsNoFill (2, 4, 2, 1, new Insets (2, 0, 0, 10), GridBagConstraintsNoFill.EAST);
		miniTerrainPanelConstraints.gridheight = 3;
		
		contentPane.add (miniTerrainPanel, miniTerrainPanelConstraints);
		
		// Set up the mini panel to hold all the civilians - this also has a 2 pixel gap to correct the labels above it
		final Dimension civilianPanelSize = new Dimension (560, 36);
		
		civilianPanel = new JPanel ();
		civilianPanel.setOpaque (false);
		civilianPanel.setLayout (new GridBagLayout ());
		civilianPanel.setMinimumSize (civilianPanelSize);
		civilianPanel.setMaximumSize (civilianPanelSize);
		civilianPanel.setPreferredSize (civilianPanelSize);
		
		contentPane.add (civilianPanel, getUtils ().createConstraintsNoFill (0, 2, 4, 1, new Insets (2, 0, 0, 0), GridBagConstraintsNoFill.CENTRE));

		// Set up the mini panel to hold all the productions
		// Note on this and other panels in the same row (enchantments+terrain) we put a 2 pixel filler above the panel, to push the labels above up a fraction higher
		final Dimension productionPanelSize = new Dimension (256, 84);
		
		productionPanel = new JPanel ();
		productionPanel.setOpaque (false);
		productionPanel.setLayout (new GridBagLayout ());
		productionPanel.setMinimumSize (productionPanelSize);
		productionPanel.setMaximumSize (productionPanelSize);
		productionPanel.setPreferredSize (productionPanelSize);
		
		contentPane.add (productionPanel, getUtils ().createConstraintsNoFill (0, 4, 1, 1, new Insets (2, 0, 0, 12), GridBagConstraintsNoFill.SOUTHEAST));
		
		// Set up the mini panel to hold all the enchantments
		final Dimension enchantmentPanelSize = new Dimension (138, 84);
		
		final JPanel enchantmentPanel = new JPanel ();
		enchantmentPanel.setMinimumSize (enchantmentPanelSize);
		enchantmentPanel.setMaximumSize (enchantmentPanelSize);
		enchantmentPanel.setPreferredSize (enchantmentPanelSize);
		
		contentPane.add (enchantmentPanel, getUtils ().createConstraintsNoFill (1, 4, 1, 1, new Insets (2, 0, 0, 6), GridBagConstraintsNoFill.CENTRE));
		
		// Set up the mini panel to hold all the units
		final Dimension unitPanelSize = new Dimension (background.getWidth () * 2, 40);
		
		unitPanel = new JPanel ();
		unitPanel.setLayout (new GridBagLayout ());
		unitPanel.setOpaque (false);
		unitPanel.setMinimumSize (unitPanelSize);
		unitPanel.setMaximumSize (unitPanelSize);
		unitPanel.setPreferredSize (unitPanelSize);
		
		contentPane.add (unitPanel, getUtils ().createConstraintsNoFill (0, 11, 4, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Set up the mini panel to show progress towards current construction
		final JPanel constructionProgressPanel = new JPanel ()
		{
			private static final long serialVersionUID = -2428422973639205496L;

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
							if (x + progressCoinDone.getWidth () > constructionProgressPanelSize.width)
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
		
		constructionProgressPanel.setMinimumSize (constructionProgressPanelSize);
		constructionProgressPanel.setMaximumSize (constructionProgressPanelSize);
		constructionProgressPanel.setPreferredSize (constructionProgressPanelSize);
		
		contentPane.add (constructionProgressPanel, getUtils ().createConstraintsNoFill (2, 8, 2, 1, new Insets (8, 0, 0, 14), GridBagConstraintsNoFill.SOUTHEAST));
		
		// Set up the mini panel to what's being currently constructed
		final Dimension constructionPanelSize = new Dimension (140, 74);
		
		constructionPanel = new JPanel ()
		{
			private static final long serialVersionUID = -7350768464461438141L;

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
						final CityViewElement buildingImage = getGraphicsDB ().findBuilding (cityData.getCurrentlyConstructingBuildingID (), "constructionPanel");
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame
							((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
							buildingImage.getCityViewAnimation ());
					
						g.drawImage (image, (getSize ().width - image.getWidth ()) / 2, (getSize ().height - image.getHeight ()) / 2, null);
					}

					// Draw unit
					if (sampleUnit != null)
						getUnitClientUtils ().drawUnitFigures (sampleUnit, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, (constructionPanelSize.width - 60) / 2, 28, true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		constructionPanel.setOpaque (false);
		constructionPanel.setMinimumSize (constructionPanelSize);
		constructionPanel.setMaximumSize (constructionPanelSize);
		constructionPanel.setPreferredSize (constructionPanelSize);
		
		contentPane.add (constructionPanel, getUtils ().createConstraintsNoFill (2, 9, 2, 1, new Insets (0, 0, 2, 10), GridBagConstraintsNoFill.SOUTHEAST));
		
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
							final Building prerequisiteBuilding = getLanguage ().findBuilding (prerequisiteBuildingID);
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
									final Building currentConstruction = getLanguage ().findBuilding (cityData.getCurrentlyConstructingBuildingID ());
									prerequisiteBuildingName = (currentConstruction != null) ? currentConstruction.getBuildingName () : cityData.getCurrentlyConstructingBuildingID ();
								}
								else if (cityData.getCurrentlyConstructingUnitID () != null)
								{
									final Unit currentConstruction = getLanguage ().findUnit (cityData.getCurrentlyConstructingUnitID ());
									prerequisiteBuildingName = (currentConstruction != null) ? currentConstruction.getUnitName () : cityData.getCurrentlyConstructingUnitID ();
								}
							}
							else
								languageEntryID = "SellPromptNormal";
						}
					}
					
					// Work out the text for the message box
					final Building building = getLanguage ().findBuilding (buildingID);
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
						msg.setCityLocation (getCityLocation ());
						msg.setBuildingID (buildingID);
					}
					
					msg.setVisible (true);
				}				
			}
		});

		cityDataChanged ();
		unitsChanged ();
		
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
		
		unitPanel.removeAll ();
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

				unitPanel.add (selectUnitButton, getUtils ().createConstraintsNoFill (x, 0, 1, 1, new Insets (0, 2, 0, 0), GridBagConstraintsNoFill.CENTRE));
				x++;
			}

		// Left justify the buttons
		final GridBagConstraints fillConstraints = getUtils ().createConstraintsHorizontalFill (x, 0, 1, 1, INSET, GridBagConstraintsHorizontalFill.CENTRE);
		fillConstraints.weightx = 1;
		
		unitPanel.add (Box.createRigidArea (new Dimension (0, 0)), fillConstraints);

		unitPanel.revalidate ();
		unitPanel.repaint ();
		
		log.trace ("Exiting unitsChanged");
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

		final RaceEx race = getGraphicsDB ().findRace (cityData.getCityRaceID (), "cityDataChanged");
		
		// Start with farmers
		Image civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER, "cityDataChanged")));
		final int civvyCount = cityData.getCityPopulation () / 1000;
		int x = 0;
		for (int civvyNo = 1; civvyNo <= civvyCount; civvyNo++)
		{
			// Is this the first rebel?
			if (civvyNo == civvyCount - cityData.getNumberOfRebels () + 1)
				civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_REBEL, "cityDataChanged")));
			
			// Is this the first worker?
			else if (civvyNo == cityData.getMinimumFarmers () + cityData.getOptionalFarmers () + 1)
				civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_WORKER, "cityDataChanged")));
			
			// Is this civilian changeable (between farmer and worker) - if so, create a button for them instead of a plain image
			final Action action;
			if ((civvyNo > civvyCount - cityData.getNumberOfRebels ()) ||	// Rebels
				(civvyNo <= cityData.getMinimumFarmers ()))						// Enforced farmers
			{
				// Create as a 'show unrest calculation' button
				action = new AbstractAction ()
				{
					private static final long serialVersionUID = -5279215265703452922L;

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
					private static final long serialVersionUID = 7655922473370295899L;

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
					private static final long serialVersionUID = 1785342094563388840L;

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
								
							final ProductionType productionType = getLanguage ().findProductionType (breakdown.getProductionTypeID ());
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
			getUnitClientUtils ().registerUnitFiguresAnimation (cityData.getCurrentlyConstructingUnitID (), GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, constructionPanel);
			
			// Create a dummy unit here, rather than on every paintComponent call
			sampleUnit = new AvailableUnit ();
			sampleUnit.setUnitID (cityData.getCurrentlyConstructingUnitID ());

			// We don't have to get the weapon grade or experience right just to draw the figures
			getUnitUtils ().initializeUnitSkills (sampleUnit, -1, true, getClient ().getClientDB ());			
		}
		
		constructionPanel.repaint ();

		civilianPanel.revalidate ();
		civilianPanel.repaint ();
		productionPanel.revalidate ();
		productionPanel.repaint ();
		
		languageOrCityDataChanged ();
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
			
			final Race race = getLanguage ().findRace (cityData.getCityRaceID ());
			raceLabel.setText ((race == null) ? cityData.getCityRaceID () : race.getRaceName ());
			
			try
			{
				// Max city size
				final CityProductionBreakdownsEx productions = getCityCalculations ().calculateAllCityProductions
					(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, false, getClient ().getClientDB ());
			
				final CityProductionBreakdown maxCitySizeProd = productions.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
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
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_GOLD));
		}
		
		rushBuyAction.setEnabled (rushBuyEnabled);
		
		log.trace ("Exiting recheckRushBuyEnabled = " + rushBuyEnabled);
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
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
	{
		cityCalculations = calc;
	}

	/**
	 * @return Client city calculations
	 */
	public final MomClientCityCalculations getClientCityCalculations ()
	{
		return clientCityCalculations;
	}

	/**
	 * @param calc Client city calculations
	 */
	public final void setClientCityCalculations (final MomClientCityCalculations calc)
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
}
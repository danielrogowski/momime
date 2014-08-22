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
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.database.v0_9_5.Race;
import momime.client.ui.MomUIConstants;
import momime.client.ui.panels.CityViewPanel;
import momime.client.utils.AnimationController;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.common.calculations.CityProductionBreakdownsEx;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.internal.CityGrowthRateBreakdown;
import momime.common.internal.CityProductionBreakdown;
import momime.common.internal.CityUnrestBreakdown;
import momime.common.messages.clienttoserver.v0_9_5.ChangeOptionalFarmersMessage;
import momime.common.messages.v0_9_5.OverlandMapCityData;

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
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;

	/** Animation controller */
	private AnimationController anim;
	
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

		// Actions
		rushBuyAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
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
					changeConstruction.setCityViewUI (ui);
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
				getAnim ().unregisterRepaintTrigger (null, constructionPanel);
				getCityViewPanel ().cityViewClosing ();
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
				super.paintComponent (g);
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
		getCityViewPanel ().init ();
		
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
		
		final JPanel unitPanel = new JPanel ();
		unitPanel.setMinimumSize (unitPanelSize);
		unitPanel.setMaximumSize (unitPanelSize);
		unitPanel.setPreferredSize (unitPanelSize);
		
		contentPane.add (unitPanel, getUtils ().createConstraintsNoFill (0, 11, 4, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Set up the mini panel to show progress towards current construction
		final Dimension constructionProgressPanelSize = new Dimension (132, 62);
		
		final JPanel constructionProgressPanel = new JPanel ();
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
				super.paintComponent (g);
				
				final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
				try
				{
					final CityViewElement buildingImage = getGraphicsDB ().findBuilding (cityData.getCurrentlyConstructingBuildingOrUnitID (), "constructionPanel");
					final BufferedImage image = getAnim ().loadImageOrAnimationFrame
						((buildingImage.getCityViewAlternativeImageFile () != null) ? buildingImage.getCityViewAlternativeImageFile () : buildingImage.getCityViewImageFile (),
						buildingImage.getCityViewAnimation ());
					
					g.drawImage (image, (getSize ().width - image.getWidth ()) / 2, (getSize ().height - image.getHeight ()) / 2, null);
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
		
		// Set up info that depends on cityData
		cityDataUpdated ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);

		log.trace ("Exiting init");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getCityLocation ());
		
		// Get details about the city
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
		
		// Fixed labels
		resourcesLabel.setText		(getLanguage ().findCategoryEntry ("frmCity", "Resources"));
		enchantmentsLabel.setText	(getLanguage ().findCategoryEntry ("frmCity", "Enchantments"));
		terrainLabel.setText			(getLanguage ().findCategoryEntry ("frmCity", "Terrain"));
		buildings.setText				(getLanguage ().findCategoryEntry ("frmCity", "Buildings"));
		units.setText						(getLanguage ().findCategoryEntry ("frmCity", "Units"));
		production.setText				(getLanguage ().findCategoryEntry ("frmCity", "Production"));
		
		// Dynamic labels
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
		
		// Actions
		rushBuyAction.putValue					(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "RushBuy"));
		changeConstructionAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "ChangeConstruction"));
		okAction.putValue							(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "OK"));

		log.trace ("Exiting languageChanged");
	}

	/**
	 * Performs any updates that need to be redone when the cityData changes - principally this means the population may have changed, so we
	 * need to redraw all the civilians, but also production may have changed from the number of farmers/workers/etc changing.
	 * 
	 * @throws IOException If there is a problem
	 */
	public final void cityDataUpdated () throws IOException
	{
		log.trace ("Entering cityDataUpdated: " + getCityLocation ());
		civilianPanel.removeAll ();
		productionPanel.removeAll ();

		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

		final RaceEx race = getGraphicsDB ().findRace (cityData.getCityRaceID (), "cityDataUpdated");
		
		// Start with farmers
		Image civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_FARMER)));
		final int civvyCount = cityData.getCityPopulation () / 1000;
		int x = 0;
		for (int civvyNo = 1; civvyNo <= civvyCount; civvyNo++)
		{
			// Is this the first rebel?
			if (civvyNo == civvyCount - cityData.getNumberOfRebels () + 1)
				civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_REBEL)));
			
			// Is this the first worker?
			else if (civvyNo == cityData.getMinimumFarmers () + cityData.getOptionalFarmers () + 1)
				civilianImage = doubleSize (getUtils ().loadImage (race.findCivilianImageFile (CommonDatabaseConstants.VALUE_POPULATION_TASK_ID_WORKER)));
			
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
		getAnim ().registerRepaintTrigger (getGraphicsDB ().findBuilding
			(cityData.getCurrentlyConstructingBuildingOrUnitID (), "cityDataUpdated").getCityViewAnimation (), constructionPanel);
		constructionPanel.repaint ();

		civilianPanel.revalidate ();
		civilianPanel.repaint ();
		productionPanel.revalidate ();
		productionPanel.repaint ();
		
		log.trace ("Exiting cityDataUpdated");
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
}
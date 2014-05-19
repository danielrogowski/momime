package momime.client.ui;

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
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RaceEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.language.database.v0_9_5.CitySize;
import momime.client.language.database.v0_9_5.Race;
import momime.client.ui.panels.CityViewPanel;
import momime.client.utils.TextUtils;
import momime.common.calculations.CalculateCityGrowthRateBreakdown;
import momime.common.calculations.CalculateCityProductionResult;
import momime.common.calculations.CalculateCityProductionResults;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.clienttoserver.v0_9_5.ChangeOptionalFarmersMessage;
import momime.common.messages.v0_9_5.OverlandMapCityData;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * City screen, so you can view current buildings, production and civilians, examine
 * calculation breakdowns and change production and civilian task assignments
 */
public final class CityViewUI extends MomClientAbstractUI
{
	/** Class logger */
	private final Logger log = Logger.getLogger (CityViewUI.class.getName ());
	
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;

	/** Small font */
	private Font smallFont;
	
	/** Panel where all the buildings are drawn */
	private CityViewPanel cityViewPanel;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/** City size+name label */
	private JLabel cityNameLabel;
	
	/** Race label */
	private JLabel raceLabel;
	
	/** Current population label */
	private JLabel currentPopulationLabel;
	
	/** Maximum population label */
	private JLabel maximumPopulationLabel;
	
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
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.entering (CityViewUI.class.getName (), "init", getCityLocation ());
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/background.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/cityView/cityViewButtonDisabled.png");

		// Actions
		rushBuyAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent ev)
			{
			}
		};

		changeConstructionAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 1562419693690602353L;

			@Override
			public void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		
		// Initialize the frame
		final CityViewUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanuageChangeListener (ui);
				getClient ().getCityViews ().remove (getCityLocation ().toString ());
			}
		});
		
		// Do this "too early" on purpose, so that the window isn't centred over the map, but is a little down-right of it
		getFrame ().setLocationRelativeTo (getOverlandMapUI ().getFrame ());
		
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
		final GridBagConstraints cityNameConstraints = getUtils ().createConstraints (0, 0, 4, INSET, GridBagConstraints.CENTER);
		cityNameConstraints.weighty = 1;
		
		cityNameLabel = getUtils ().createLabel (MomUIUtils.GOLD, getLargeFont ());
		contentPane.add (cityNameLabel, cityNameConstraints);
		
		resourcesLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (resourcesLabel, getUtils ().createConstraints (0, 3, 1, new Insets (0, 8, 0, 0), GridBagConstraints.WEST));
		
		enchantmentsLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (enchantmentsLabel, getUtils ().createConstraints (1, 3, 1, INSET, GridBagConstraints.WEST));
		
		terrainLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (terrainLabel, getUtils ().createConstraints (2, 3, 2, new Insets (0, 4, 0, 0), GridBagConstraints.WEST));
		
		buildings = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (buildings, getUtils ().createConstraints (0, 5, 1, new Insets (0, 8, 0, 0), GridBagConstraints.WEST));
		
		production = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (production, getUtils ().createConstraints (2, 7, 2, new Insets (0, 4, 0, 0), GridBagConstraints.WEST));

		// Make column 1 as wide as possible, so the 3 buttons don't get extra space
		final GridBagConstraints unitsLabelConstraints = getUtils ().createConstraints (0, 10, 1, new Insets (0, 8, 0, 0), GridBagConstraints.SOUTHWEST);
		unitsLabelConstraints.weightx = 1;
		
		units = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		contentPane.add (units, unitsLabelConstraints);
		
		// For some reason I couldn't figure out, trying to put the maximum population label into the main grid kept making
		// the right hand column (including the OK button) too wide and I couldn't make it work right, so instead put
		// the 3 labels into a subpanel so their columns are independant of the rest of the panel
		final GridBagConstraints labelPanelConstraints = getUtils ().createConstraints (0, 1, 4, INSET, GridBagConstraints.WEST);
		labelPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		
		final JPanel labelsPanel = new JPanel ();
		labelsPanel.setOpaque (false);
		labelsPanel.setLayout (new GridBagLayout ());
		contentPane.add (labelsPanel, labelPanelConstraints);
		
		final GridBagConstraints raceLabelConstraints = getUtils ().createConstraints (0, 0, 1, new Insets (0, 8, 0, 0), GridBagConstraints.WEST);
		raceLabelConstraints.weightx = 1;
		
		raceLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		labelsPanel.add (raceLabel, raceLabelConstraints);

		currentPopulationLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		labelsPanel.add (currentPopulationLabel, getUtils ().createConstraints (1, 0, 1, new Insets (0, 0, 0, 8), GridBagConstraints.EAST));
		
		maximumPopulationLabel = getUtils ().createLabel (MomUIUtils.GOLD, getMediumFont ());
		labelsPanel.add (maximumPopulationLabel, getUtils ().createConstraints (2, 0, 1, new Insets (0, 0, 0, 8), GridBagConstraints.EAST));
		
		// Buttons - put a gap of 10 underneath them to push the buttons slightly above the Units label, and 6 above to push the view panel up
		contentPane.add (getUtils ().createImageButton (rushBuyAction, Color.BLACK, MomUIUtils.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraints (1, 10, 1, new Insets (6, 0, 10, 6), GridBagConstraints.EAST));

		contentPane.add (getUtils ().createImageButton (changeConstructionAction, Color.BLACK, MomUIUtils.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraints (2, 10, 1, new Insets (6, 0, 10, 6), GridBagConstraints.EAST));

		contentPane.add (getUtils ().createImageButton (okAction, Color.BLACK, MomUIUtils.SILVER, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled),
			getUtils ().createConstraints (3, 10, 1, new Insets (6, 0, 10, 8), GridBagConstraints.EAST));
		
		// Set up the city view panel
		getCityViewPanel ().setCityLocation (getCityLocation ());
		getCityViewPanel ().init ();
		
		final GridBagConstraints cityViewPanelConstraints = getUtils ().createConstraints (0, 6, 2, new Insets (0, 8, 0, 0), GridBagConstraints.CENTER);
		cityViewPanelConstraints.gridheight = 4;
		
		contentPane.add (getCityViewPanel (), cityViewPanelConstraints);
		
		// Set up the mini terrain panel
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "OverlandMapUI.init");
		final Dimension miniTerrainPanelSize = new Dimension (overlandMapTileSet.getTileWidth () * 7, overlandMapTileSet.getTileHeight () * 7);
		
		final JPanel miniTerrainPanel = new JPanel ();
		miniTerrainPanel.setMinimumSize (miniTerrainPanelSize);
		miniTerrainPanel.setMaximumSize (miniTerrainPanelSize);
		miniTerrainPanel.setPreferredSize (miniTerrainPanelSize);

		final GridBagConstraints miniTerrainPanelConstraints = getUtils ().createConstraints (2, 4, 2, new Insets (2, 0, 0, 10), GridBagConstraints.EAST);
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
		
		contentPane.add (civilianPanel, getUtils ().createConstraints (0, 2, 4, new Insets (2, 0, 0, 0), GridBagConstraints.CENTER));

		// Set up the mini panel to hold all the productions
		// Note on this and other panels in the same row (enchantments+terrain) we put a 2 pixel filler above the panel, to push the labels above up a fraction higher
		final Dimension productionPanelSize = new Dimension (256, 84);
		
		final JPanel productionPanel = new JPanel ();
		productionPanel.setMinimumSize (productionPanelSize);
		productionPanel.setMaximumSize (productionPanelSize);
		productionPanel.setPreferredSize (productionPanelSize);
		
		contentPane.add (productionPanel, getUtils ().createConstraints (0, 4, 1, new Insets (2, 0, 0, 12), GridBagConstraints.SOUTHEAST));
		
		// Set up the mini panel to hold all the enchantments
		final Dimension enchantmentPanelSize = new Dimension (138, 84);
		
		final JPanel enchantmentPanel = new JPanel ();
		enchantmentPanel.setMinimumSize (enchantmentPanelSize);
		enchantmentPanel.setMaximumSize (enchantmentPanelSize);
		enchantmentPanel.setPreferredSize (enchantmentPanelSize);
		
		contentPane.add (enchantmentPanel, getUtils ().createConstraints (1, 4, 1, new Insets (2, 0, 0, 6), GridBagConstraints.CENTER));
		
		// Set up the mini panel to hold all the units
		final Dimension unitPanelSize = new Dimension (background.getWidth () * 2, 40);
		
		final JPanel unitPanel = new JPanel ();
		unitPanel.setMinimumSize (unitPanelSize);
		unitPanel.setMaximumSize (unitPanelSize);
		unitPanel.setPreferredSize (unitPanelSize);
		
		contentPane.add (unitPanel, getUtils ().createConstraints (0, 11, 4, INSET, GridBagConstraints.CENTER));
		
		// Set up the mini panel to show progress towards current construction
		final Dimension constructionProgressPanelSize = new Dimension (132, 62);
		
		final JPanel constructionProgressPanel = new JPanel ();
		constructionProgressPanel.setMinimumSize (constructionProgressPanelSize);
		constructionProgressPanel.setMaximumSize (constructionProgressPanelSize);
		constructionProgressPanel.setPreferredSize (constructionProgressPanelSize);
		
		contentPane.add (constructionProgressPanel, getUtils ().createConstraints (2, 8, 2, new Insets (8, 0, 0, 14), GridBagConstraints.SOUTHEAST));
		
		// Set up the mini panel to what's being currently constructed
		final Dimension constructionPanelSize = new Dimension (140, 74);
		
		final JPanel constructionPanel = new JPanel ();
		constructionPanel.setMinimumSize (constructionPanelSize);
		constructionPanel.setMaximumSize (constructionPanelSize);
		constructionPanel.setPreferredSize (constructionPanelSize);
		
		contentPane.add (constructionPanel, getUtils ().createConstraints (2, 9, 2, new Insets (0, 0, 2, 10), GridBagConstraints.SOUTHEAST));
		
		// Lock frame size
		cityDataUpdated ();
		
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);	// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		getFrame ().pack ();

		log.exiting (CityViewUI.class.getName (), "init");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.entering (CityViewUI.class.getName (), "languageChanged", getCityLocation ());
		
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
			final CitySize citySize = getLanguage ().findCitySize (cityData.getCitySizeID ());
			final String cityName = (citySize == null) ? cityData.getCityName () : citySize.getCitySizeName ().replaceAll ("CITY_NAME", cityData.getCityName ()); 
			cityNameLabel.setText (cityName);
			getFrame ().setTitle (cityName);
			
			final Race race = getLanguage ().findRace (cityData.getCityRaceID ());
			raceLabel.setText ((race == null) ? cityData.getCityRaceID () : race.getRaceName ());
			
			try
			{
				// Max city size
				final CalculateCityProductionResults productions = getCityCalculations ().calculateAllCityProductions
					(getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID (), getClient ().getSessionDescription (), true, getClient ().getClientDB ());
			
				final CalculateCityProductionResult maxCitySizeProd = productions.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_FOOD);
				final int maxCitySize = (maxCitySizeProd == null) ? 0 : maxCitySizeProd.getModifiedProductionAmount ();
			
				maximumPopulationLabel.setText (getLanguage ().findCategoryEntry ("frmCity", "MaxCitySize").replaceAll ("MAX_CITY_SIZE",
					getTextUtils ().intToStrCommas (maxCitySize * 1000)));
			
				// Growth rate
				final CalculateCityGrowthRateBreakdown cityGrowthBreakdown = getCityCalculations ().calculateCityGrowthRate
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (), maxCitySize, getClient ().getClientDB ());
			
				final int cityGrowth = cityGrowthBreakdown.getFinalTotal ();
				final String cityPopulation = getTextUtils ().intToStrCommas (cityData.getCityPopulation ());
			
				if (cityGrowth == 0)
					currentPopulationLabel.setText (getLanguage ().findCategoryEntry ("frmCity", "PopulationMaxed").replaceAll ("POPULATION", cityPopulation));
				else
					currentPopulationLabel.setText (getLanguage ().findCategoryEntry ("frmCity", "PopulationAndGrowth").replaceAll ("POPULATION", cityPopulation).replaceAll
						("GROWTH_RATE", getTextUtils ().intToStrPlusMinus (cityGrowth)));
			}
			catch (final IOException e)
			{
				e.printStackTrace ();
			}
		}
		
		// Actions
		rushBuyAction.putValue					(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "RushBuy"));
		changeConstructionAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "ChangeConstruction"));
		okAction.putValue							(Action.NAME, getLanguage ().findCategoryEntry ("frmCity", "OK"));

		log.exiting (CityViewUI.class.getName (), "languageChanged");
	}

	/**
	 * Performs any updates that need to be redone when the cityData changes
	 * @throws IOException If there is a problem
	 */
	public final void cityDataUpdated () throws IOException
	{
		log.entering (CityViewUI.class.getName (), "cityDataUpdated", getCityLocation ());
		civilianPanel.removeAll ();

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
				action = null;
			}
			else
			{
				// Create as an 'optional farmers' button
				final int civvyNoCopy = civvyNo;
				action = new AbstractAction ()
				{
					private static final long serialVersionUID = 7655922473370295899L;

					@Override
					public void actionPerformed (final ActionEvent ev)
					{
						// Clicking on the same number toggles it, so we can turn the last optional farmer into a worker
						int optionalFarmers = civvyNoCopy - cityData.getMinimumFarmers ();
						if (optionalFarmers == cityData.getOptionalFarmers ())
							optionalFarmers--;
						
						log.finest ("Requesting optional farmers in city " + getCityLocation () + " to be set to " + optionalFarmers);
						
						try
						{
							final ChangeOptionalFarmersMessage msg = new ChangeOptionalFarmersMessage ();
							msg.setCityLocation (getCityLocation ());
							msg.setOptionalFarmers (optionalFarmers);
							getClient ().getServerConnection ().sendMessageToServer (msg);
						}
						catch (final Exception e)
						{
							e.printStackTrace ();
						}
					}
				}; 
			}
			
			// Left justify all the civilians
			final GridBagConstraints imageConstraints = getUtils ().createConstraints (x, 0, 1, 1, GridBagConstraints.SOUTHWEST);
			if (civvyNo == civvyCount)
				imageConstraints.weightx = 1;
			
			civilianPanel.add (getUtils ().createImageButton (action, null, null, null, civilianImage, civilianImage, civilianImage), imageConstraints);
			x++;
			
			// If this is the last farmer who's necessary to feed the population (& so we cannot convert them to a worker) then leave a gap to show this
			if (civvyNo == cityData.getMinimumFarmers ())
			{
				civilianPanel.add (Box.createRigidArea (new Dimension (10, 0)), getUtils ().createConstraints (x, 0, 1, 0, GridBagConstraints.SOUTHWEST));
				x++;
			}
		}

		civilianPanel.revalidate ();
		civilianPanel.repaint ();
		log.exiting (CityViewUI.class.getName (), "cityDataUpdated");
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
}
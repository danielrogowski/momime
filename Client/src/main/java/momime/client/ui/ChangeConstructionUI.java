package momime.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import momime.client.MomClient;
import momime.client.calculations.MomClientCityCalculations;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.renderer.BuildingListCellRenderer;
import momime.client.ui.renderer.CellRendererFactory;
import momime.client.utils.ResourceValueClientUtils;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.BuildingPopulationProductionModifier;
import momime.common.database.v0_9_5.Race;
import momime.common.database.v0_9_5.RaceCannotBuild;
import momime.common.database.v0_9_5.Unit;
import momime.common.database.v0_9_5.UnitUpkeep;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Screen to change the current construction project at a city
 */
public final class ChangeConstructionUI extends MomClientAbstractUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChangeConstructionUI.class);

	/** Typical inset used on this screen layout */
	private final static int INSET = 1;

	/** Larger inset for spanning the borders of the background bitmaps */
	private final static int BIG_INSET = 3;
	
	/** No inset, used for positioning some components */
	private final static int NO_INSET = 0;
	
	/** Medium font */
	private Font mediumFont;

	/** Small font */
	private Font smallFont;
	
	/** The city view UI that opened this change construction window */
	private CityViewUI cityViewUI;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** Client city calculations */
	private MomClientCityCalculations clientCityCalculations;
	
	/** Factory for creating cell renderers */
	private CellRendererFactory cellRendererFactory;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Resource value client utils */
	private ResourceValueClientUtils resourceValueClientUtils;
	
	/** Cancel action */
	private Action cancelAction;
	
	/** OK action */
	private Action okAction;
	
	/** Upkeep label */
	private JLabel upkeepLabel;
	
	/** Moves label */
	private JLabel movesLabel;
	
	/** Cost label */
	private JLabel costLabel;

	/** Name of what's currently being constructed */
	private JLabel currentlyConstructingName;
	
	/** Long description of what's currently being constructed */
	private JTextArea currentlyConstructingDescription;
	
	/** List of what's currently being constructed will allow us to build */
	private JTextArea currentlyConstructingAllows;	

	/** The actual cost figure */
	private JLabel currentlyConstructingProductionCost;

	/** Image of upkeep in coins */
	private JLabel currentlyConstructingUpkeep;
	
	/** Currently selected building - doesn't get set as the current construction project until we click OK */
	private Building selectedBuilding;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getCityLocation ());

		// Load images
		final BufferedImage unitDetailsBackground = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/unitDetails.png");
		final BufferedImage changeConstructionBackground = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/changeConstruction.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button49x12redNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button49x12redPressed.png");
		
		// Actions
		cancelAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -5862161820735023601L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};

		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		// Initialize the frame
		final ChangeConstructionUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanuageChangeListener (ui);
				getClient ().getChangeConstructions ().remove (getCityLocation ().toString ());
			}
		});
		
		// Do this "too early" on purpose, so that the window isn't centred over the city view, but is a little down-right of it
		getFrame ().setLocationRelativeTo (getCityViewUI ().getFrame ());
		
		// Set up cell renderers
		final BuildingListCellRenderer buildingListCellRenderer = getCellRendererFactory ().createBuildingListCellRenderer ();
		buildingListCellRenderer.setFont (getMediumFont ());
		buildingListCellRenderer.setForeground (MomUIConstants.SILVER);
		buildingListCellRenderer.init ();

		// Set list boxes
		final DefaultListModel<Building> buildingsItems = new DefaultListModel<Building> ();
		final JList<Building> buildingsList = new JList<Building> ();
		buildingsList.setOpaque (false);
		buildingsList.setModel (buildingsItems);
		buildingsList.setCellRenderer (buildingListCellRenderer);
		buildingsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		buildingListCellRenderer.setListBox (buildingsList);
		
		final DefaultListModel<Unit> unitsItems = new DefaultListModel<Unit> ();
		final JList<Unit> unitsList = new JList<Unit>  ();		
		unitsList.setOpaque (false);
		unitsList.setModel (unitsItems);
		unitsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		contentPane.setBackground (Color.BLACK);
		
		// Set up the scroll panes containing the list boxes
		final JScrollPane buildingsScroll = getUtils ().createScrollPaneWithBackgroundImage (buildingsList, changeConstructionBackground);
		buildingsScroll.setBorder (BorderFactory.createEmptyBorder (6, 6, 6, 6));

		final JScrollPane unitsScroll = getUtils ().createScrollPaneWithBackgroundImage (unitsList, changeConstructionBackground);
		unitsScroll.setBorder (BorderFactory.createEmptyBorder (6, 6, 6, 6));
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		contentPane.add (buildingsScroll, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final JPanel currentlyConstructingPanel = getUtils ().createPanelWithBackgroundImage (unitDetailsBackground);
		contentPane.add (currentlyConstructingPanel, getUtils ().createConstraintsNoFill (1, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.CENTRE));

		contentPane.add (unitsScroll, getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Set up the layout of the Currently Constructing panel in the middle
		currentlyConstructingPanel.setLayout (new GridBagLayout ());
		
		final Dimension currentlyConstructingImageSize = new Dimension (62, 60);
		
		final JLabel currentlyConstructingImage = new JLabel ();
		currentlyConstructingImage.setMinimumSize (currentlyConstructingImageSize);
		currentlyConstructingImage.setMaximumSize (currentlyConstructingImageSize);
		currentlyConstructingImage.setPreferredSize (currentlyConstructingImageSize);
		
		currentlyConstructingPanel.add (currentlyConstructingImage, getUtils ().createConstraintsNoFill (0, 0, 1, 5, new Insets (6, 3, 3, 5), GridBagConstraintsNoFill.CENTRE));
		
		currentlyConstructingName = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		currentlyConstructingPanel.add (currentlyConstructingName, getUtils ().createConstraintsNoFill (1, 0, 2, 1, new Insets (5, 1, 1, 1), GridBagConstraintsNoFill.WEST));

		costLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		currentlyConstructingPanel.add (costLabel, getUtils ().createConstraintsNoFill (1, 1, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		upkeepLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		currentlyConstructingPanel.add (upkeepLabel, getUtils ().createConstraintsNoFill (1, 2, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		movesLabel = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		currentlyConstructingPanel.add (movesLabel, getUtils ().createConstraintsNoFill (1, 3, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		currentlyConstructingProductionCost = getUtils ().createLabel (MomUIConstants.AQUA, getSmallFont ());
		currentlyConstructingPanel.add (currentlyConstructingProductionCost, getUtils ().createConstraintsNoFill (2, 1, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		currentlyConstructingUpkeep = new JLabel ();
		currentlyConstructingPanel.add (currentlyConstructingUpkeep, getUtils ().createConstraintsNoFill (2, 2, 1, 1, INSET, GridBagConstraintsNoFill.WEST));
		
		final Dimension allowsSize = new Dimension (360, 119);

		currentlyConstructingAllows = getUtils ().createWrappingLabel (MomUIConstants.AQUA, getMediumFont ());
		currentlyConstructingAllows.setMinimumSize (allowsSize);
		currentlyConstructingAllows.setMaximumSize (allowsSize);
		currentlyConstructingAllows.setPreferredSize (allowsSize);

		currentlyConstructingPanel.add (currentlyConstructingAllows, getUtils ().createConstraintsNoFill (0, 5, 3, 1, BIG_INSET, GridBagConstraintsNoFill.CENTRE));
		
		final Dimension currentConstructingDescriptionSize = new Dimension (360, 57);

		currentlyConstructingDescription = getUtils ().createWrappingLabel (MomUIConstants.AQUA, getMediumFont ());
		currentlyConstructingDescription.setMinimumSize (currentConstructingDescriptionSize);
		currentlyConstructingDescription.setMaximumSize (currentConstructingDescriptionSize);
		currentlyConstructingDescription.setPreferredSize (currentConstructingDescriptionSize);
		
		currentlyConstructingPanel.add (currentlyConstructingDescription, getUtils ().createConstraintsNoFill (0, 6, 3, 1, new Insets (3, 3, 1, 3), GridBagConstraintsNoFill.CENTRE));
		
		// Mini panel at the bottom containing the 2 red buttons
		final GridBagConstraints redButtonsConstraints = getUtils ().createConstraintsNoFill (0, 7, 3, 1, new Insets (1, 1, 1, 1), GridBagConstraintsNoFill.NORTH);
		redButtonsConstraints.weighty = 1;
		
		final JPanel redButtonsPanel = new JPanel ();
		redButtonsPanel.setOpaque (false);
		currentlyConstructingPanel.add (redButtonsPanel, redButtonsConstraints);
		
		redButtonsPanel.setLayout (new GridBagLayout ());
		redButtonsPanel.add (getUtils ().createImageButton (cancelAction, MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (0, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.NORTH));

		redButtonsPanel.add (Box.createRigidArea (new Dimension (20, 0)), getUtils ().createConstraintsNoFill (1, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.NORTH));

		redButtonsPanel.add (getUtils ().createImageButton (okAction, MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (2, 0, 1, 1, NO_INSET, GridBagConstraintsNoFill.NORTH));
		
		// What's currently being constructed?
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
		
		final Race race = getClient ().getClientDB ().findRace (cityData.getCityRaceID (), "ChangeConstructionUI.init");
		
		// Which buildings can we construct?
		for (final Building thisBuilding : getClient ().getClientDB ().getBuilding ())
			
			// If we don't have this building already
			if ((!getMemoryBuildingUtils ().findBuilding (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
				getCityLocation (), thisBuilding.getBuildingID ())) &&
				
				// and we have necessary prerequisite buildings (e.g. Farmers' Market requires a Granary)
				(getMemoryBuildingUtils ().meetsBuildingRequirements (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
					getCityLocation (), thisBuilding)) &&
				
				// and we have any necessary prerequisite tile types (e.g. Ship Wrights' Guild requires an adjacent Ocean tile)
				(getCityCalculations ().buildingPassesTileTypeRequirements (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
					getCityLocation (), thisBuilding, getClient ().getSessionDescription ().getMapSize ())))
			{
				// Check the race inhabiting this city can construct this kind of building
				boolean canBuild = true;
				final Iterator<RaceCannotBuild> iter = race.getRaceCannotBuild ().iterator ();
				while ((canBuild) && (iter.hasNext ()))
				{
					final RaceCannotBuild cannotBuild = iter.next ();
					if (cannotBuild.getCannotBuildBuildingID ().equals (thisBuilding.getBuildingID ()))
						canBuild = false;
				}
				
				if (canBuild)
				{
					buildingsItems.addElement (thisBuilding);
					
					// Pre-select whatever was previously being built when the form first opens up
					if (thisBuilding.getBuildingID ().equals (cityData.getCurrentlyConstructingBuildingOrUnitID ()))
						buildingsList.setSelectedIndex (buildingsItems.size () - 1);
				}
			}
		
		// Which units can we construct?
		for (final Unit thisUnit : getClient ().getClientDB ().getUnit ())
			
			// If its a regular unit
			if ((CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL.equals (thisUnit.getUnitMagicRealm ())) &&
				
				// and unit either specifies no race (e.g. Trireme) or matches the race inhabiting this city
				((thisUnit.getUnitRaceID () == null) || (thisUnit.getUnitRaceID ().equals (cityData.getCityRaceID ()))) &&
				
				// and we have the necessary buildings to construct this unit
				(getMemoryBuildingUtils ().meetsUnitRequirements (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
					getCityLocation (), thisUnit)))
				
				unitsItems.addElement (thisUnit);
		
		// Clicking a building previews its details
		final ListSelectionListener buildingSelectionListener = new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				try
				{
					final Building building = buildingsItems.get (buildingsList.getSelectedIndex ());
					selectedBuilding = building;
				
					// Update language independant labels
					currentlyConstructingProductionCost.setText ((building.getProductionCost () == null) ? null : getTextUtils ().intToStrCommas (building.getProductionCost ()));
					costLabel.setVisible (building.getProductionCost () != null);
					movesLabel.setVisible (false);
				
					// Search for upkeep values (i.e. no population task specified, and the value is negative)
					final List<UnitUpkeep> upkeeps = new ArrayList<UnitUpkeep> ();
					for (final BuildingPopulationProductionModifier upkeepValue : building.getBuildingPopulationProductionModifier ())
						if ((upkeepValue.getPopulationTaskID () == null) && (upkeepValue.getDoubleAmount () != null) && (upkeepValue.getDoubleAmount () < 0))
						{
							if (upkeepValue.getDoubleAmount () % 2 != 0)
								throw new MomException ("Building " + building.getBuildingID () + " has an upkeep of type " + upkeepValue.getProductionTypeID () + " which is not a multiple of 2");
							
							final UnitUpkeep upkeep = new UnitUpkeep ();
							upkeep.setProductionTypeID (upkeepValue.getProductionTypeID ());
							upkeep.setUpkeepValue (-upkeepValue.getDoubleAmount () / 2);
							upkeeps.add (upkeep);
						}
					
					// Generate an image from the upkeeps
					final BufferedImage upkeepImage = getResourceValueClientUtils ().generateUpkeepImage (upkeeps, false);
					currentlyConstructingUpkeep.setIcon ((upkeepImage == null) ? null : new ImageIcon (upkeepImage));
					upkeepLabel.setVisible (upkeepImage != null);
				
					// Update language dependant labels
					currentConstructionChanged ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		buildingsList.addListSelectionListener (buildingSelectionListener);
		buildingSelectionListener.valueChanged (null);
		
		// Show the frame
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);	// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		getFrame ().pack ();

		log.trace ("Exiting init");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getCityLocation ());

		// Fixed labels
		upkeepLabel.setText	(getLanguage ().findCategoryEntry ("frmChangeConstruction", "Upkeep"));
		movesLabel.setText	(getLanguage ().findCategoryEntry ("frmChangeConstruction", "Moves"));
		costLabel.setText		(getLanguage ().findCategoryEntry ("frmChangeConstruction", "Cost"));
		
		// Get details about the city
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
		
		// Dynamic labels
		if (cityData != null)
		{
			final String cityName = getLanguage ().findCitySizeName (cityData.getCitySizeID ()).replaceAll ("CITY_NAME", cityData.getCityName ());
			getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmChangeConstruction", "Title").replaceAll ("CITY_NAME", cityName));
		}

		// Actions
		okAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmChangeConstruction", "OK"));
		cancelAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmChangeConstruction", "Cancel"));
		
		currentConstructionChanged ();
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * This is called when either the language or building/unit being previewed for construction changes,
	 * so we can update the name and description of it
	 */
	private final void currentConstructionChanged ()
	{
		log.trace ("Entering languageChanged: " + getCityLocation ());
		
		final momime.client.language.database.v0_9_5.Building building = getLanguage ().findBuilding (selectedBuilding.getBuildingID ());
		currentlyConstructingName.setText ((building != null) ? building.getBuildingName () : selectedBuilding.getBuildingID ());
		currentlyConstructingDescription.setText ((building != null) ? building.getBuildingHelpText () : null);
		currentlyConstructingAllows.setText (getClientCityCalculations ().describeWhatBuildingAllows (selectedBuilding.getBuildingID (), getCityLocation ()));
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return The city view UI that opened this change construction window
	 */
	public final CityViewUI getCityViewUI ()
	{
		return cityViewUI;
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
	 * @param ui The city view UI that opened this change construction window
	 */
	public final void setCityViewUI (final CityViewUI ui)
	{
		cityViewUI = ui;
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
	 * @return Factory for creating cell renderers
	 */
	public final CellRendererFactory getCellRendererFactory ()
	{
		return cellRendererFactory;
	}

	/**
	 * @param fac Factory for creating cell renderers
	 */
	public final void setCellRendererFactory (final CellRendererFactory fac)
	{
		cellRendererFactory = fac;
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
}
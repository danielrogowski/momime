package momime.client.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.renderer.BuildingListCellRenderer;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.v0_9_5.Building;
import momime.common.database.v0_9_5.Race;
import momime.common.database.v0_9_5.RaceCannotBuild;
import momime.common.database.v0_9_5.Unit;
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
	
	/** Medium font */
	private Font mediumFont;
	
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
		final BuildingListCellRenderer buildingListCellRenderer = new BuildingListCellRenderer ();
		buildingListCellRenderer.setFont (getMediumFont ());
		buildingListCellRenderer.setForeground (MomUIConstants.SILVER);
		buildingListCellRenderer.setOpaque (false);
		buildingListCellRenderer.setLanguageHolder (getLanguageHolder ());
		buildingListCellRenderer.setGraphicsDB (getGraphicsDB ());
		buildingListCellRenderer.setUtils (getUtils ());
		buildingListCellRenderer.init ();

		// Set list boxes
		final DefaultListModel<Building> buildingsItems = new DefaultListModel<Building> ();
		final JList<Building> buildingsList = new JList<Building> ();
		buildingsList.setOpaque (false);
		buildingsList.setModel (buildingsItems);
		buildingsList.setCellRenderer (buildingListCellRenderer);
		
		final DefaultListModel<Unit> unitsItems = new DefaultListModel<Unit> ();
		final JList<Unit> unitsList = new JList<Unit>  ();		
		unitsList.setOpaque (false);
		unitsList.setModel (unitsItems);
		unitsList.setFont (getMediumFont ());
		unitsList.setForeground (MomUIConstants.SILVER);
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		contentPane.setBackground (Color.BLACK);
		
		// Set up the scroll panes containing the list boxes
		final JScrollPane buildingsScroll = getUtils ().createScrollPaneWithbackgroundImage (buildingsList, changeConstructionBackground);
		buildingsScroll.setBorder (BorderFactory.createEmptyBorder (6, 6, 6, 6));

		final JScrollPane unitsScroll = getUtils ().createScrollPaneWithbackgroundImage (unitsList, changeConstructionBackground);
		unitsScroll.setBorder (BorderFactory.createEmptyBorder (6, 6, 6, 6));
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		contentPane.add (buildingsScroll, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		contentPane.add (getUtils ().createImage (unitDetailsBackground),
			getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		contentPane.add (unitsScroll, getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		// Get details about this city
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
					buildingsItems.addElement (thisBuilding);
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

		// Get details about the city
		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
		
		// Dynamic labels
		if (cityData != null)
		{
			final String cityName = getLanguage ().findCitySizeName (cityData.getCitySizeID ()).replaceAll ("CITY_NAME", cityData.getCityName ());
			getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmChangeConstruction", "Title").replaceAll ("CITY_NAME", cityName));
		}
		
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
}
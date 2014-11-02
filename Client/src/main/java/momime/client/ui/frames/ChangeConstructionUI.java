package momime.client.ui.frames;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import momime.client.MomClient;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.ui.CompositeShape;
import momime.client.ui.MomUIConstants;
import momime.client.ui.panels.UnitInfoPanel;
import momime.client.ui.renderer.BuildingListCellRenderer;
import momime.client.ui.renderer.UnitListCellRenderer;
import momime.client.utils.AnimationController;
import momime.client.utils.UnitClientUtils;
import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Building;
import momime.common.database.Race;
import momime.common.database.RaceCannotBuild;
import momime.common.database.Unit;
import momime.common.messages.clienttoserver.ChangeCityConstructionMessage;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Screen to change the current construction project at a city
 */
public final class ChangeConstructionUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ChangeConstructionUI.class);

	/** Typical inset used on this screen layout */
	private final static int INSET = 0;

	/** Medium font */
	private Font mediumFont;

	/** Small font */
	private Font smallFont;
	
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
	
	/** Renderer for the buildings list */
	private BuildingListCellRenderer buildingListCellRenderer;
	
	/** Renderer for the units list */
	private UnitListCellRenderer unitListCellRenderer;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private MomUnitCalculations unitCalculations;

	/** Utils for drawing units */
	private UnitClientUtils unitClientUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Client unit calculations */
	private MomClientUnitCalculations clientUnitCalculations;
	
	/** Unit/building info panel */
	private UnitInfoPanel unitInfoPanel;
	
	/** Cancel action */
	private Action cancelAction;
	
	/** OK action */
	private Action okAction;

	/** Items in the buildings list box */
	private DefaultListModel<Building> buildingsItems; 
	
	/** Buildings list box */
	private JList<Building> buildingsList;
	
	/** Handles clicks on the buildings list */
	private ListSelectionListener buildingSelectionListener;
	
	/** Items in the units list box */
	private DefaultListModel<AvailableUnit> unitsItems;
	
	/** Units list box */
	private JList<AvailableUnit> unitsList;

	/** Handles clicks on the units list */
	private ListSelectionListener unitSelectionListener;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getCityLocation ());

		// Load images
		final BufferedImage changeConstructionBackground = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/changeConstruction.png");
		
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
			private static final long serialVersionUID = -7988136722401366834L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();

				if (((getUnitInfoPanel ().getBuilding () != null) && (!getUnitInfoPanel ().getBuilding ().getBuildingID ().equals (cityData.getCurrentlyConstructingBuildingID ()))) ||
					((getUnitInfoPanel ().getUnit () != null) && (!getUnitInfoPanel ().getUnit ().getUnitID ().equals ( cityData.getCurrentlyConstructingUnitID ()))))
				{
					// Tell server that we want to change our construction
					// Note we don't update our own copy of it on the client - the server will confirm back to us that the choice was OK
					final ChangeCityConstructionMessage msg = new ChangeCityConstructionMessage ();
					
					if (getUnitInfoPanel ().getBuilding () != null)
						msg.setBuildingID (getUnitInfoPanel ().getBuilding ().getBuildingID ());

					if (getUnitInfoPanel ().getUnit () != null)
						msg.setUnitID (getUnitInfoPanel ().getUnit ().getUnitID ());
					
					msg.setCityLocation (getCityLocation ());
					try
					{
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
				
				// Close form even if we didn't change what's being constructed
				getFrame ().dispose ();
			}
		};
		
		// Must do this prior to calling .getPanel () on it
		getUnitInfoPanel ().setActions (new Action [] {cancelAction, okAction});
		
		// Initialize the frame
		final ChangeConstructionUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				try
				{
					getAnim ().unregisterRepaintTrigger (null, buildingsList);
					getAnim ().unregisterRepaintTrigger (null, unitsList);
					getUnitInfoPanel ().unitInfoPanelClosing ();
				}
				catch (final MomException e)
				{
					log.error (e, e);
				}
				
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				getClient ().getChangeConstructions ().remove (getCityLocation ().toString ());
			}
		});
		
		// Set up cell renderers
		getBuildingListCellRenderer ().setFont (getMediumFont ());
		getBuildingListCellRenderer ().setForeground (MomUIConstants.SILVER);
		getBuildingListCellRenderer ().init ();

		getUnitListCellRenderer ().setFont (getMediumFont ());
		getUnitListCellRenderer ().setForeground (MomUIConstants.SILVER);
		
		// Set list boxes
		buildingsItems = new DefaultListModel<Building> ();
		buildingsList = new JList<Building> ();
		buildingsList.setOpaque (false);
		buildingsList.setModel (buildingsItems);
		buildingsList.setCellRenderer (getBuildingListCellRenderer ());
		buildingsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		unitsItems = new DefaultListModel<AvailableUnit> ();
		unitsList = new JList<AvailableUnit>  ();		
		unitsList.setOpaque (false);
		unitsList.setModel (unitsItems);
		unitsList.setCellRenderer (getUnitListCellRenderer ());
		unitsList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		contentPane.setOpaque (false);
		
		// Set up the scroll panes containing the list boxes
		final JScrollPane buildingsScroll = getUtils ().createScrollPaneWithBackgroundImage (buildingsList, changeConstructionBackground);
		buildingsScroll.setBorder (BorderFactory.createEmptyBorder (6, 6, 6, 6));
		buildingsScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		final JScrollPane unitsScroll = getUtils ().createScrollPaneWithBackgroundImage (unitsList, changeConstructionBackground);
		unitsScroll.setBorder (BorderFactory.createEmptyBorder (6, 6, 6, 6));
		unitsScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		contentPane.add (buildingsScroll, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		contentPane.add (getUnitInfoPanel ().getPanel (), getUtils ().createConstraintsNoFill (1, 0, 1, 1, new Insets (0, 2, 0, 2), GridBagConstraintsNoFill.CENTRE));
		contentPane.add (unitsScroll, getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Clicking a building previews its details
		buildingSelectionListener = new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				if (buildingsList.getSelectedIndex () >= 0)
				{
					final MemoryBuilding building = new MemoryBuilding ();
					building.setBuildingID (buildingsItems.get (buildingsList.getSelectedIndex ()).getBuildingID ());
					building.setCityLocation (getCityLocation ());
					try
					{
						getUnitInfoPanel ().showBuilding (building);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		};
		buildingsList.addListSelectionListener (buildingSelectionListener);
		
		// Clicking a unit previews its details
		unitSelectionListener = new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				if (unitsList.getSelectedIndex () >= 0)
				{
					final AvailableUnit sampleUnit = new AvailableUnit ();
					sampleUnit.setUnitID (unitsItems.get (unitsList.getSelectedIndex ()).getUnitID ());
					sampleUnit.setOwningPlayerID (getClient ().getOurPlayerID ());
					try
					{
						final int startingExperience = getMemoryBuildingUtils ().experienceFromBuildings
							(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (), getClient ().getClientDB ());

						final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) getMultiplayerSessionUtils ().findPlayerWithID
							(getClient ().getPlayers (), getClient ().getOurPlayerID (), "unitSelectionListener").getPersistentPlayerPublicKnowledge ();
					
						sampleUnit.setWeaponGrade (getUnitCalculations ().calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
							(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
							getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), getCityLocation (),
							pub.getPick (), getClient ().getSessionDescription ().getMapSize (), getClient ().getClientDB ()));
					
						getUnitUtils ().initializeUnitSkills (sampleUnit, startingExperience, getClient ().getClientDB ());
						getUnitInfoPanel ().showUnit (sampleUnit);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		};
		unitsList.addListSelectionListener (unitSelectionListener);

		// Set up initial contents of the lists
		updateWhatCanBeConstructed ();
		
		// Show the frame
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		
		// This gets a bit complicated, because there are 3 independant unjoined areas of the frame
		final Dimension panelSize = getUnitInfoPanel ().getPanel ().getPreferredSize ();
		final int panelLeft = changeConstructionBackground.getWidth () + 2;
		final int panelTop = (changeConstructionBackground.getHeight () - panelSize.height) / 2;
		final int panelSides = (panelSize.width - getUnitInfoPanel ().getBackgroundButtonsWidth ()) / 2;
		final int panelButtonsTop = panelTop + panelSize.height - getUnitInfoPanel ().getBackgroundButtonsHeight ();
		
		getFrame ().setShape (new CompositeShape (new Shape []
				
			// Shape of buildings list box
			{new Rectangle (0, 0, changeConstructionBackground.getWidth (), changeConstructionBackground.getHeight ()), new Polygon

				// Shape of centre unit panel including the poking out buttons at the bottom
				(new int [] {panelLeft, panelLeft + panelSize.width, panelLeft + panelSize.width, panelLeft + panelSize.width - panelSides, panelLeft + panelSize.width - panelSides, panelLeft + panelSides, panelLeft + panelSides, panelLeft},
				new int [] {panelTop, panelTop, panelButtonsTop, panelButtonsTop, panelTop + panelSize.height, panelTop + panelSize.height, panelButtonsTop, panelButtonsTop},
				8),
				
			// Shape of units list box
			new Rectangle (panelLeft + panelSize.width + 2, 0, changeConstructionBackground.getWidth (), changeConstructionBackground.getHeight ())}));

		log.trace ("Exiting init");
	}
	
	/**
	 * Updates the buildings and units list boxes with what can be constructed in this city
	 * @throws IOException If there is a problem
	 */
	public final void updateWhatCanBeConstructed () throws IOException
	{
		log.trace ("Entering updateWhatCanBeConstructed");

		final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getCityData ();
		
		final Race race = getClient ().getClientDB ().findRace (cityData.getCityRaceID (), "updateWhatCanBeConstructed");
		
		// Which buildings can we construct?
		buildingsItems.clear ();
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
					getAnim ().registerRepaintTrigger (getGraphicsDB ().findBuilding (thisBuilding.getBuildingID (), "ChangeConstructionUI.init").getCityViewAnimation (), buildingsList);
					
					// Pre-select whatever was previously being built when the form first opens up
					if (thisBuilding.getBuildingID ().equals (cityData.getCurrentlyConstructingBuildingID ()))
						buildingsList.setSelectedIndex (buildingsItems.size () - 1);
				}
			}
		
		// Which units can we construct?
		unitsItems.clear ();
		for (final Unit thisUnit : getClient ().getClientDB ().getUnit ())
			
			// If its a regular unit
			if ((CommonDatabaseConstants.VALUE_UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL.equals (thisUnit.getUnitMagicRealm ())) &&
				
				// and unit either specifies no race (e.g. Trireme) or matches the race inhabiting this city
				((thisUnit.getUnitRaceID () == null) || (thisUnit.getUnitRaceID ().equals (cityData.getCityRaceID ()))) &&
				
				// and we have the necessary buildings to construct this unit
				(getMemoryBuildingUtils ().meetsUnitRequirements (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
					getCityLocation (), thisUnit)))
			{
				// Create a sample unit for it now, so the list box can keep it to redraw the unit every frame
				final AvailableUnit sampleUnit = new AvailableUnit ();
				sampleUnit.setUnitID (thisUnit.getUnitID ());

				// We don't have to get the weapon grade or experience right just to draw the figures
				getUnitUtils ().initializeUnitSkills (sampleUnit, null, getClient ().getClientDB ());

				final String movingActionID = getClientUnitCalculations ().determineCombatActionID (sampleUnit, true);
				getUnitClientUtils ().registerUnitFiguresAnimation (thisUnit.getUnitID (), movingActionID, 4, unitsList);

				unitsItems.addElement (sampleUnit);
				
				// Pre-select whatever was previously being built when the form first opens up
				if (thisUnit.getUnitID ().equals (cityData.getCurrentlyConstructingUnitID ()))
					unitsList.setSelectedIndex (unitsItems.size () - 1);
			}

		// Select the current construction project
		if (buildingsList.getSelectedIndex () >= 0)
			buildingSelectionListener.valueChanged (null);
		
		if (unitsList.getSelectedIndex () >= 0)
			unitSelectionListener.valueChanged (null);
		
		log.trace ("Exiting updateWhatCanBeConstructed");
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

		// Actions
		okAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmChangeConstruction", "OK"));
		cancelAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmChangeConstruction", "Cancel"));
		
		log.trace ("Exiting languageChanged");
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
	 * @return Renderer for the buildings list
	 */
	public final BuildingListCellRenderer getBuildingListCellRenderer ()
	{
		return buildingListCellRenderer;
	}

	/**
	 * @param rend Factory for creating cell renderers
	 */
	public final void setBuildingListCellRenderer (final BuildingListCellRenderer rend)
	{
		buildingListCellRenderer = rend;
	}

	/**
	 * @return Renderer for the units list
	 */
	public final UnitListCellRenderer getUnitListCellRenderer ()
	{
		return unitListCellRenderer;
	}

	/**
	 * @param rend Factory for creating cell renderers
	 */
	public final void setUnitListCellRenderer (final UnitListCellRenderer rend)
	{
		unitListCellRenderer = rend;
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
	 * @return Unit calculations
	 */
	public final MomUnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final MomUnitCalculations calc)
	{
		unitCalculations = calc;
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
	 * @return Unit/building info panel
	 */
	public final UnitInfoPanel getUnitInfoPanel ()
	{
		return unitInfoPanel;
	}

	/**
	 * @param pnl Unit/building info panel
	 */
	public final void setUnitInfoPanel (final UnitInfoPanel pnl)
	{
		unitInfoPanel = pnl;
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
	 * @return Client unit calculations
	 */
	public final MomClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final MomClientUnitCalculations calc)
	{
		clientUnitCalculations = calc;
	}
}
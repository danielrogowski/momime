package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.calculations.MiniMapBitmapGenerator;
import momime.client.ui.MomUIConstants;
import momime.client.ui.renderer.CitiesListCellRenderer;
import momime.client.ui.renderer.CitiesListEntry;
import momime.client.utils.CitiesListSorter;
import momime.client.utils.WizardClientUtils;
import momime.common.calculations.CityProductionCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitSkillEx;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.MemoryBuildingUtils;

/**
 * Screen showing a summary list of all the player's cities
 */
public final class CitiesListUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CitiesListUI.class);

	/** XML layout */
	private XmlLayoutContainerEx citiesListLayout;

	/** Large font */
	private Font largeFont;

	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Minimap generator */
	private MiniMapBitmapGenerator miniMapBitmapGenerator;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** OK action */
	private Action okAction;
	
	/** Title */
	private JLabel title;

	/** City name column heading */
	private JLabel cityNameHeading;
	
	/** City population column heading */
	private JLabel cityPopulationHeading;
	
	/** Units garrison in city column heading */
	private JLabel cityUnitsHeading;
	
	/** Weapon grade of units constructed in city column heading */
	private JLabel cityWeaponGradeHeading;
	
	/** Enchantments and curses column heading */
	private JLabel cityEnchantmentsHeading;
	
	/** Sell building column heading */
	private JLabel citySellHeading;
	
	/** City construction project column heading */
	private JLabel cityConstructingHeading;
	
	/** Items in the cities list box */
	private DefaultListModel<CitiesListEntry> citiesItems; 
	
	/** Cities list box */
	private JList<CitiesListEntry> citiesList;
	
	/** Cell renderer */
	private CitiesListCellRenderer citiesListCellRenderer;
	
	/** Minimap panel */
	private JPanel miniMapPanel;
	
	/** Bitmaps for the mini map; one has a white dot and the other doesn't */
	private final BufferedImage [] miniMapBitmaps = new BufferedImage [2];
	
	/** Which frame of the minimap "animation" is currently being drawn */
	private int miniMapFrame = 0;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/citiesList.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button82x30goldBorderNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button82x30goldBorderPressed.png");
		
		// Actions
		okAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getCitiesListLayout ()));
		
		title = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (title, "frmCitiesListTitle");

		cityNameHeading = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (cityNameHeading, "frmCitiesListHeadingName");
		
		cityPopulationHeading = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (cityPopulationHeading, "frmCitiesListHeadingPopulation");
		
		cityUnitsHeading = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (cityUnitsHeading, "frmCitiesListHeadingUnits");
		
		cityWeaponGradeHeading = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (cityWeaponGradeHeading, "frmCitiesListHeadingWeaponGrade");
		
		cityEnchantmentsHeading = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (cityEnchantmentsHeading, "frmCitiesListHeadingEnchantments");
		
		citySellHeading = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (citySellHeading, "frmCitiesListHeadingSell");
		
		cityConstructingHeading = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (cityConstructingHeading, "frmCitiesListHeadingCurrentlyConstructing");
		
		final JButton okButton = getUtils ().createImageButton (okAction, MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getLargeFont (),
			buttonNormal, buttonPressed, buttonNormal);
		contentPane.add (okButton, "frmCitiesListOK");
		
		// Set up cell renderer
		getCitiesListCellRenderer ().init ();
		
		// Set up the list
		citiesItems = new DefaultListModel<CitiesListEntry> ();
		citiesList = new JList<CitiesListEntry> ();
		citiesList.setOpaque (false);
		citiesList.setModel (citiesItems);
		citiesList.setCellRenderer (getCitiesListCellRenderer ());
		citiesList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);

		final JScrollPane spellsScroll = getUtils ().createTransparentScrollPane (citiesList);
		spellsScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellsScroll, "frmCitiesListCities");
			
		// Minimap view
		final XmlLayoutComponent miniMapSize = getCitiesListLayout ().findComponent ("frmCitiesListMiniMap");
		miniMapPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Black out background
				super.paintComponent (g);
				
				g.drawImage (miniMapBitmaps [miniMapFrame], 0, 0, miniMapSize.getWidth (), miniMapSize.getHeight (), null);
			}			
		};
		
		miniMapPanel.setBackground (Color.BLACK);
		contentPane.add (miniMapPanel, "frmCitiesListMiniMap");
		
		// Flash the white dot on the minimap
		new Timer (250, (ev) ->
		{
			miniMapFrame = 1 - miniMapFrame;
			miniMapPanel.repaint ();
		}).start ();
		
		citiesList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (!SwingUtilities.isRightMouseButton (ev))
				{
					final int row = citiesList.locationToIndex (ev.getPoint ());
					if ((row >= 0) && (row < citiesItems.size ()))
					{
						final Rectangle rect = citiesList.getCellBounds (row, row);
						if (rect.contains (ev.getPoint ()))
							try
							{
								getCitiesListCellRenderer ().handleClick (ev, citiesItems.getElementAt (row), ev.getPoint ().x - rect.x, ev.getPoint ().y - rect.y);
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
					}
				}
			}
		});
								
		// Initialize the list
		refreshCitiesList ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		
		final int w = getCitiesListLayout ().getFormWidth ();
		getFrame ().setShape (new Polygon
			(new int [] {10, 6, 6, 10, 0, 0,		w, w, w-10, w-6, w-6, w-10,		w-10, w-6, w-6, w-10, w, w,		0, 0, 10, 6, 6, 10},
			new int [] {38, 38, 30, 30, 10, 0,	0, 10, 30, 30, 38, 38,					378, 378, 386, 386, 406, 416,		416, 406, 386, 386, 378, 378},
			24));		
	}
	
	/**
	 * Refreshes the list of cities
	 * @throws IOException If there is a problem finding any of the necessary data
	 */
	public final void refreshCitiesList () throws IOException
	{
		if (citiesItems != null)
		{
			// Where is the wizard's fortress? (this could give a null if we're banished)
			final MemoryBuilding fortress = getMemoryBuildingUtils ().findCityWithBuilding
				(getClient ().getOurPlayerID (), CommonDatabaseConstants.BUILDING_FORTRESS,
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding ());
			
			final MapCoordinates3DEx fortressLocation = (fortress == null) ? null : (MapCoordinates3DEx) fortress.getCityLocation ();
			
			// Player picks
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "refreshCitiesList");
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();

			// Container for weapon grade images
			final UnitSkillEx melee = getClient ().getClientDB ().findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "refreshCitiesList");
			
			// Make a new list of every city owned by the player
			final List<CitiesListEntry> cities = new ArrayList<CitiesListEntry> ();
			for (int plane = 0; plane < getClient ().getSessionDescription ().getOverlandMapSize ().getDepth (); plane++)
				for (int y = 0; y < getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
							(plane).getRow ().get (y).getCell ().get (x);
						final OverlandMapCityData cityData = mc.getCityData ();
						
						if ((cityData != null) && (cityData.getCityPopulation () > 0) && (cityData.getCityOwnerID () == getClient ().getOurPlayerID ()))
						{
							final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane);
							
							final int weaponGrade = getUnitCalculations ().calculateWeaponGradeFromBuildingsAndSurroundingTilesAndAlchemyRetort
								(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
									getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), cityLocation,
									pub.getPick (), getClient ().getSessionDescription ().getOverlandMapSize (), getClient ().getClientDB ());
							final String weaponGradeImageFile = melee.findWeaponGradeImageFile (weaponGrade, "refreshCitiesList");
							
							final int enchantmentCount = (int) getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().stream ().filter
								(s -> (cityLocation.equals (s.getCityLocation ())) && (s.getCastingPlayerID () == getClient ().getOurPlayerID ())).count ();
							final int curseCount = (int) getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ().stream ().filter
								(s -> (cityLocation.equals (s.getCityLocation ())) && (s.getCastingPlayerID () != getClient ().getOurPlayerID ())).count ();
							
							cities.add (new CitiesListEntry (cityData, cityLocation, cityLocation.equals (fortressLocation),
								weaponGradeImageFile, enchantmentCount, curseCount));
						}
					}
			
			// Sort it and update the list
			citiesItems.clear ();
			cities.stream ().sorted (new CitiesListSorter ()).forEach (c -> citiesItems.addElement (c));
		}
	}

	/**
	 * Generates bitmap of the entire overland map with only 1 pixel per terrain file, to display in the mini map area in the top right corner.
	 * @throws IOException If there is a problem finding any of the necessary data
	 */
	public final void regenerateMiniMapBitmaps () throws IOException
	{
		if ((citiesList != null) && (citiesList.getSelectedIndex () >= 0))
		{
			final MapCoordinates3DEx coords = citiesItems.get (citiesList.getSelectedIndex ()).getCityLocation ();
			
			// Generate the map without a dot
			miniMapBitmaps [0] = getMiniMapBitmapGenerator ().generateMiniMapBitmap (coords.getZ ());
			
			// Now copy it and add the dot
			final BufferedImage b = new BufferedImage (getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (),
				getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (), BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = b.createGraphics ();
			try
			{
				g.drawImage (miniMapBitmaps [0], 0, 0, null);
			}
			finally
			{
				g.dispose ();
			}
			
			b.setRGB (coords.getX (), coords.getY (), Color.WHITE.getRGB ());
			
			miniMapBitmaps [1] = b;			
			miniMapPanel.repaint ();
		}
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Title containing player name
		String titleText = getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getTitle ());
		
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID ());
		if (ourPlayer != null)
			titleText = titleText.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourPlayer));
		
		title.setText (titleText);
		getFrame ().setTitle (titleText);

		// Column headings
		cityNameHeading.setText				(getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getCityName ()));
		cityPopulationHeading.setText			(getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getCityPopulation ()));
		cityUnitsHeading.setText					(getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getCityUnits ()));
		cityWeaponGradeHeading.setText	(getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getCityWeaponGrade ()));
		cityEnchantmentsHeading.setText		(getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getCityEnchantments ()));
		citySellHeading.setText					(getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getCitySell ()));
		cityConstructingHeading.setText		(getLanguageHolder ().findDescription (getLanguages ().getCitiesListScreen ().getCityConstructing ()));
		
		// Buttons
		okAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCitiesListLayout ()
	{
		return citiesListLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCitiesListLayout (final XmlLayoutContainerEx layout)
	{
		citiesListLayout = layout;
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
	 * @return Minimap generator
	 */
	public final MiniMapBitmapGenerator getMiniMapBitmapGenerator ()
	{
		return miniMapBitmapGenerator;
	}

	/**
	 * @param gen Minimap generator
	 */
	public final void setMiniMapBitmapGenerator (final MiniMapBitmapGenerator gen)
	{
		miniMapBitmapGenerator = gen;
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
	 * @return Cell renderer
	 */
	public final CitiesListCellRenderer getCitiesListCellRenderer ()
	{
		return citiesListCellRenderer;
	}

	/**
	 * @param rend Cell renderer
	 */
	public final void setCitiesListCellRenderer (final CitiesListCellRenderer rend)
	{
		citiesListCellRenderer = rend;
	}
}
package momime.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.MapFeatureEx;
import momime.client.graphics.database.SmoothedTileTypeEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.v0_9_5.CityImage;
import momime.client.graphics.database.v0_9_5.SmoothedTile;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.MapSizeData;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Screen for displaying the overland map, including the buttons and side panels and so on that appear in the same frame
 */
public final class OverlandMapUI extends MomClientAbstractUI
{
	/** Class logger */
	private final Logger log = Logger.getLogger (OverlandMapUI.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Small font */
	private Font smallFont;
	
	/** Smoothed tiles to display at every map cell */
	private SmoothedTile [] [] [] smoothedTiles;
	
	/** Bitmaps for each animation frame of the overland map */
	private BufferedImage [] overlandMapBitmaps;
	
	/** Colour multiplied flags for each player's cities */
	final Map<Integer, BufferedImage> cityFlagImages = new HashMap<Integer, BufferedImage> ();
	
	/** The plane that the UI is currently displaying */
	private int mapViewPlane = 0;
	
	// UI Components

	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/** Frame number being displayed */
	private int terrainAnimFrame;
	
	/** Game action */
	private Action gameAction;
	
	/** Spells action */
	private Action spellsAction;
	
	/** Armies action */
	private Action armiesAction;
	
	/** Cities action */
	private Action citiesAction;
	
	/** Magic action */
	private Action magicAction;
	
	/** Plane action */
	private Action planeAction;
	
	/** Messages action */
	private Action messagesAction;
	
	/** Chat action */
	private Action chatAction;
	
	/** Info action */
	private Action infoAction;
	
	/** Zoom in action */
	private Action zoomInAction;
	
	/** Zoom out action */
	private Action zoomOutAction;
	
	/** Options action */
	private Action optionsAction;
	
	/** Turn label */
	private JLabel turnLabel;
	
	/**
	 * Creates the smoothedTiles array as the correct size
	 * Can't just do this at the start of init (), because the server sends the first FogOfWarVisibleAreaChanged prior to the overland map being displayed,
	 * so we can prepare the map image before displaying it - so we have to create the area for it to prepare it into
	 */
	public final void afterJoinedSession ()
	{
		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		smoothedTiles = new SmoothedTile [mapSize.getDepth ()] [mapSize.getHeight ()] [mapSize.getWidth ()];
	}
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage topBarBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/background.png");
		final BufferedImage topBarGoldButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/goldNormal.png");
		final BufferedImage topBarGoldButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/goldPressed.png");
		final BufferedImage topBarInfoNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/infoNormal.png");
		final BufferedImage topBarInfoPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/infoPressed.png");
		final BufferedImage topBarZoomInNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/zoomInNormal.png");
		final BufferedImage topBarZoomInPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/zoomInPressed.png");
		final BufferedImage topBarZoomOutNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/zoomOutNormal.png");
		final BufferedImage topBarZoomOutPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/zoomOutPressed.png");
		final BufferedImage topBarOptionsNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/optionsNormal.png");
		final BufferedImage topBarOptionsPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/optionsPressed.png");

		final BufferedImage rightHandPanelBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/rightHandPanel/background.png");

		// Actions
		gameAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		spellsAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		armiesAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		citiesAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		magicAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		planeAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		messagesAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		chatAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		infoAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		zoomInAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		zoomOutAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		optionsAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};
		
		// Initialize the frame
		getFrame ().setTitle ("Overland Map");
		getFrame ().setDefaultCloseOperation (WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		contentPane.setBackground (Color.BLACK);
		contentPane.setPreferredSize (new Dimension (640, 480));
		
		// Set up main layout
 		// This is a 2x2 grid, with the top two cells being joined into one long bar
		contentPane.setLayout (new GridBagLayout ());
		
		final Dimension mapButtonBarSize = new Dimension (topBarBackground.getWidth (), topBarBackground.getHeight ());
		final JPanel mapButtonBar = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (topBarBackground, 0, 0, null);
			}
		};
		
		mapButtonBar.setMinimumSize (mapButtonBarSize);
		mapButtonBar.setMaximumSize (mapButtonBarSize);
		mapButtonBar.setPreferredSize (mapButtonBarSize);
		contentPane.add (mapButtonBar, getUtils ().createConstraints (0, 0, 2, INSET, GridBagConstraints.WEST));

		final Dimension rightHandPanelSize = new Dimension (rightHandPanelBackground.getWidth (), rightHandPanelBackground.getHeight ());
		final JPanel rightHandPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (rightHandPanelBackground, 0, 0, null);
			}
		};
		rightHandPanel.setBackground (Color.BLACK);
		
		rightHandPanel.setMinimumSize (rightHandPanelSize);
		rightHandPanel.setMaximumSize (rightHandPanelSize);
		rightHandPanel.setPreferredSize (rightHandPanelSize);
		contentPane.add (rightHandPanel, getUtils ().createConstraints (1, 1, 1, INSET, GridBagConstraints.NORTH));
		
		final JPanel sceneryPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (overlandMapBitmaps [terrainAnimFrame], 0, 0, null);
			}
		};
		sceneryPanel.setBackground (Color.BLACK);
		
		// Let the scenery panel take up as much space as possible
		final GridBagConstraints sceneryConstraints = getUtils ().createConstraints (0, 1, 1, INSET, GridBagConstraints.CENTER);
		sceneryConstraints.fill = GridBagConstraints.BOTH;
		sceneryConstraints.weightx = 1;
		sceneryConstraints.weighty = 1;
		
		contentPane.add (sceneryPanel, sceneryConstraints);
	
		// Animate the terrain tiles
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "OverlandMapUI.init");
		new Timer ((int) (1000 / overlandMapTileSet.getAnimationSpeed ()), new ActionListener ()
		{
			@Override
			public final void actionPerformed (final ActionEvent e)
			{
				final int newFrame = terrainAnimFrame + 1;
				terrainAnimFrame = (newFrame >= overlandMapTileSet.getAnimationFrameCount ()) ? 0 : newFrame;
				sceneryPanel.repaint ();
			}
		}).start ();
		
		// Set up the row of gold buttons along the top
		mapButtonBar.setLayout (new GridBagLayout ());
		mapButtonBar.add (Box.createRigidArea (new Dimension (7, 0)), getUtils ().createConstraints (0, 0, 1, INSET, GridBagConstraints.CENTER));

		mapButtonBar.add (getUtils ().createImageButton (gameAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (1, 0, 1, INSET, GridBagConstraints.CENTER));
		
		mapButtonBar.add (getUtils ().createImageButton (spellsAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (2, 0, 1, INSET, GridBagConstraints.CENTER));
			
		mapButtonBar.add (getUtils ().createImageButton (armiesAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (3, 0, 1, INSET, GridBagConstraints.CENTER));
			
		mapButtonBar.add (getUtils ().createImageButton (citiesAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (4, 0, 1, INSET, GridBagConstraints.CENTER));
			
		mapButtonBar.add (getUtils ().createImageButton (magicAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (5, 0, 1, INSET, GridBagConstraints.CENTER));

		mapButtonBar.add (getUtils ().createImageButton (planeAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (6, 0, 1, INSET, GridBagConstraints.CENTER));

		mapButtonBar.add (getUtils ().createImageButton (messagesAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (7, 0, 1, INSET, GridBagConstraints.CENTER));

		mapButtonBar.add (getUtils ().createImageButton (chatAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraints (8, 0, 1, INSET, GridBagConstraints.CENTER));

		mapButtonBar.add (getUtils ().createImageButton (infoAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarInfoNormal, topBarInfoPressed, topBarInfoNormal), getUtils ().createConstraints (9, 0, 1, INSET, GridBagConstraints.CENTER));
		
		mapButtonBar.add (getUtils ().createImageButton (zoomInAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarZoomInNormal, topBarZoomInPressed, topBarZoomInNormal), getUtils ().createConstraints (10, 0, 1, INSET, GridBagConstraints.CENTER));
			
		mapButtonBar.add (getUtils ().createImageButton (zoomOutAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarZoomOutNormal, topBarZoomOutPressed, topBarZoomOutNormal), getUtils ().createConstraints (11, 0, 1, INSET, GridBagConstraints.CENTER));
			
		mapButtonBar.add (getUtils ().createImageButton (optionsAction, MomUIUtils.GOLD, Color.BLACK, getSmallFont (),
			topBarOptionsNormal, topBarOptionsPressed, topBarOptionsNormal), getUtils ().createConstraints (12, 0, 1, INSET, GridBagConstraints.CENTER));

		final GridBagConstraints turnLabelConstraints = getUtils ().createConstraints (13, 0, 1, INSET, GridBagConstraints.EAST);
		turnLabelConstraints.weightx = 1;		// Right justify the label
		turnLabel = getUtils ().createLabel (MomUIUtils.GOLD, getSmallFont (), "January 1400 (Turn X)");
		mapButtonBar.add (turnLabel, turnLabelConstraints);

		mapButtonBar.add (Box.createRigidArea (new Dimension (7, 0)), getUtils ().createConstraints (14, 0, 1, INSET, GridBagConstraints.CENTER));
		
		// Stop frame being shrunk smaller than this
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setMinimumSize (getFrame ().getSize ());
		getFrame ().setLocationRelativeTo (null);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		gameAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Game"));
		spellsAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Spells"));
		armiesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Armies"));
		citiesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Cities"));
		magicAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Magic"));
		planeAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Plane"));
		messagesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "NewTurnMessages"));
		chatAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Chat"));
	}
	
	/**
	 * Converts the tile types sent by the server into actual tile numbers, smoothing the edges of various terrain types in the process
	 * @param areaToSmooth Keeps track of which tiles have been updated, so we know which need graphics updating for; or can supply null to resmooth everything
	 * @throws RecordNotFoundException If required entries in the graphics XML cannot be found
	 */
	public final void smoothMapTerrain (final MapArea3D<Boolean> areaToSmooth) throws RecordNotFoundException
	{
		log.entering (OverlandMapUI.class.getName (), "smoothMapTerrain", areaToSmooth);

		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		final int maxDirection = getCoordinateSystemUtils ().getMaxDirection (mapSize.getCoordinateSystemType ());
		
		// Choose the appropriate tile set
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "smoothMapTerrain");
		
		// Now check each map cell
		for (int planeNo = 0; planeNo < mapSize.getDepth (); planeNo++) 
			for (int y = 0; y < mapSize.getHeight (); y++) 
				for (int x = 0; x < mapSize.getWidth (); x++)
					if ((areaToSmooth == null) || ((areaToSmooth.get (x, y, planeNo) != null) && (areaToSmooth.get (x, y, planeNo))))
					{
						final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
							(planeNo).getRow ().get (y).getCell ().get (x);
						
						// Have we ever seen this tile?
						final String tileTypeID = (gc.getTerrainData () == null) ? null : gc.getTerrainData ().getTileTypeID ();
						if (tileTypeID == null)
							smoothedTiles [planeNo] [y] [x] = null;
						else
						{
							final SmoothedTileTypeEx smoothedTileType = overlandMapTileSet.findSmoothedTileType (tileTypeID, mapViewPlane, null);
							
							// If this is ticked then fix the bitmask
							// If a land based tile, want to assume grass in every direction (e.g. for mountains, draw a single mountain), so want 11111111
							
							// But for a sea tile, this looks really daft - you get a 'sea' of lakes surrounded by grass!  So we have to force these to 00000000 instead
							// to make it look remotely sensible
							
							// Rather than hard coding which tile types need 00000000 and which need 11111111, the graphics XML file has a special
							// entry under every tile for the image to use for 'NoSmooth' = No Smoothing
							final StringBuffer bitmask = new StringBuffer ();
							
							// --- Leaving this 'if' out for now since there's no options screen yet via which to turn smoothing off, but I did prove that it works ---
							// bitmask.append (GraphicsDatabaseConstants.VALUE_TILE_BITMASK_NO_SMOOTHING);
							
							{
								// 3 possibilities for how we create the bitmask
								// 0 = force 00000000
								// 1 = use 0 for this type of tile, 1 for anything else (assume grass)
								// 2 = use 0 for this type of tile, 1 for anything else (assume grass), 2 for rivers (in a joining direction)
								final int maxValueInEachDirection = overlandMapTileSet.findSmoothingSystem
									(smoothedTileType.getSmoothingSystemID (), "smoothMapTerrain").getMaxValueEachDirection ();
								
								if (maxValueInEachDirection == 0)
								{
									for (int d = 1; d <= maxDirection; d++)
										bitmask.append ("0");
								}
								
								// If a river tile, decide whether to treat this direction as a river based on the RiverDirections FROM this tile, not by looking at adjoining tiles
								// NB. This is only inland rivers - oceanside river mouths are just special shore/ocean tiles
								else if ((maxValueInEachDirection == 1) && (gc.getTerrainData ().getRiverDirections () != null))
								{
									for (int d = 1; d <= maxDirection; d++)
										if (gc.getTerrainData ().getRiverDirections ().contains (new Integer (d).toString ()))
											bitmask.append ("0");
										else
											bitmask.append ("1");
								}
								
								// Normal type of smoothing
								else
								{
									for (int d = 1; d <= maxDirection; d++)
										
										// Want rivers? i.e. is this an ocean tile
										if ((maxValueInEachDirection == 2) && (gc.getTerrainData ().getRiverDirections () != null) &&
											(gc.getTerrainData ().getRiverDirections ().contains (new Integer (d).toString ())))
											
											bitmask.append ("2");
										else
										{
											final MapCoordinates3DEx coords = new MapCoordinates3DEx ();
											coords.setX (x);
											coords.setY (y);
											coords.setZ (planeNo);
											if (getCoordinateSystemUtils ().move3DCoordinates (mapSize, coords, d))
											{
												final MemoryGridCell otherGc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
													(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ());
												final String otherTileTypeID = (otherGc.getTerrainData () == null) ? null : otherGc.getTerrainData ().getTileTypeID ();
												
												if ((otherTileTypeID == null) || (otherTileTypeID.equals (tileTypeID)) ||
													(otherTileTypeID.equals (smoothedTileType.getSecondaryTileTypeID ())) ||
													(otherTileTypeID.equals (smoothedTileType.getTertiaryTileTypeID ())))
													
													bitmask.append ("0");
												else
													bitmask.append ("1");
											}
											else
												bitmask.append ("0");
										}
								}
							}

							// The cache works directly on unsmoothed bitmasks so no reduction to do
							smoothedTiles [planeNo] [y] [x] = smoothedTileType.getRandomImage (bitmask.toString ());
						}
					}
		
		log.exiting (OverlandMapUI.class.getName (), "smoothMapTerrain");
	}
	
	/**
	 * Generates big bitmaps of the entire overland map in each frame of animation
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateOverlandMapBitmaps () throws IOException
	{
		log.entering (OverlandMapUI.class.getName (), "regenerateOverlandMapBitmaps", mapViewPlane);

		final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
		
		// We need the tile set so we know how many animation frames there are
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "regenerateOverlandMapBitmaps");
		
		// Create the set of empty bitmaps
		overlandMapBitmaps = new BufferedImage [overlandMapTileSet.getAnimationFrameCount ()];
		final Graphics2D [] g = new Graphics2D [overlandMapTileSet.getAnimationFrameCount ()];
		for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
		{
			overlandMapBitmaps [frameNo] = new BufferedImage
				(mapSize.getWidth () * overlandMapTileSet.getTileWidth (), mapSize.getHeight () * overlandMapTileSet.getTileHeight (), BufferedImage.TYPE_INT_ARGB);
			
			g [frameNo] = overlandMapBitmaps [frameNo].createGraphics ();
		}
		
		// Run through each tile
		for (int y = 0; y < mapSize.getHeight (); y++) 
			for (int x = 0; x < mapSize.getWidth (); x++)
			{
				// Terrain
				final SmoothedTile tile = smoothedTiles [mapViewPlane] [y] [x];
				if (tile != null)
				{
					if (tile.getTileFile () != null)
					{
						// Use same image for all frames
						final BufferedImage image = getUtils ().loadImage (tile.getTileFile ());
						for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
							g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
					}
					else if (tile.getTileAnimation () != null)
					{
						// Copy each animation frame over to each bitmap
						final AnimationEx anim = getGraphicsDB ().findAnimation (tile.getTileAnimation (), "regenerateOverlandMapBitmaps");
						for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
						{
							final BufferedImage image = getUtils ().loadImage (anim.getFrame ().get (frameNo).getFrameImageFile ());
							g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
						}
					}
				}
				
				// Map feature
				final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(mapViewPlane).getRow ().get (y).getCell ().get (x);
				final String mapFeatureID = (gc.getTerrainData () == null) ? null : gc.getTerrainData ().getMapFeatureID ();
				if (mapFeatureID != null)
				{
					final MapFeatureEx mapFeature = getGraphicsDB ().findMapFeature (mapFeatureID, "regenerateOverlandMapBitmaps");
					final BufferedImage image = getUtils ().loadImage (mapFeature.getOverlandMapImageFile ());

					// Use same image for all frames
					for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
						g [frameNo].drawImage (image, x * overlandMapTileSet.getTileWidth (), y * overlandMapTileSet.getTileHeight (), null);
				}
			}
		
		// Cities have to be done in a 2nd pass, since they're larger than the terrain tiles
		for (int y = 0; y < mapSize.getHeight (); y++) 
			for (int x = 0; x < mapSize.getWidth (); x++)
			{
				final MemoryGridCell gc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(mapViewPlane).getRow ().get (y).getCell ().get (x);
				final String citySizeID = (gc.getCityData () == null) ? null : gc.getCityData ().getCitySizeID ();
				if (citySizeID != null)
				{
					final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx ();
					cityLocation.setX (x);
					cityLocation.setY (y);
					cityLocation.setZ (mapViewPlane);
					
					final CityImage cityImage = getGraphicsDB ().findBestCityImage (citySizeID, cityLocation,
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), "regenerateOverlandMapBitmaps");
					final BufferedImage image = getUtils ().loadImage (cityImage.getCityImageFile ());
					
					final int xpos = (x * overlandMapTileSet.getTileWidth ()) - ((image.getWidth () - overlandMapTileSet.getTileWidth ()) / 2);
					final int ypos = (y * overlandMapTileSet.getTileHeight ()) - ((image.getHeight () - overlandMapTileSet.getTileHeight ()) / 2);

					// Use same image for all frames
					final BufferedImage cityFlagImage = getCityFlagImage (gc.getCityData ().getCityOwnerID ());
					for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
					{
						g [frameNo].drawImage (image, xpos, ypos, null);
						g [frameNo].drawImage (cityFlagImage, xpos + cityImage.getFlagOffsetX (), ypos + cityImage.getFlagOffsetY (), null);
					}
				}
			}
		
		// Clean up the drawing contexts 
		for (int frameNo = 0; frameNo < overlandMapTileSet.getAnimationFrameCount (); frameNo++)
			g [frameNo].dispose ();
		
		log.exiting (OverlandMapUI.class.getName (), "regenerateOverlandMapBitmaps"); 
	}
	
	/**
	 * @param playerID City owner player ID
	 * @return City flag image in their correct colour 
	 * @throws IOException If there is a problem loading the flag image
	 */
	private final BufferedImage getCityFlagImage (final int playerID) throws IOException
	{
		BufferedImage image = cityFlagImages.get (playerID);
		if (image == null)
		{
			// Generate a new one
			final BufferedImage whiteImage = getUtils ().loadImage (GraphicsDatabaseConstants.IMAGE_CITY_FLAG);
			
			final PlayerPublicDetails player = MultiplayerSessionUtils.findPlayerWithID (getClient ().getPlayers (), playerID);
			final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
			
			image = getUtils ().multiplyImageByColour (whiteImage, Integer.parseInt (trans.getFlagColour (), 16));
			cityFlagImages.put (playerID, image);
		}
		return image;
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
}

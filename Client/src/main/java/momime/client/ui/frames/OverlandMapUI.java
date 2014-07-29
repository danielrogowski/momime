package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.database.newgame.v0_9_5.MapSizeData;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.UnitSpecialOrder;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.MemoryGridCellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Screen for displaying the overland map, including the buttons and side panels and so on that appear in the same frame
 */
public final class OverlandMapUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OverlandMapUI.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;
	
	/** Bitmap generator */
	private OverlandMapBitmapGenerator overlandMapBitmapGenerator;
	
	/** Small font */
	private Font smallFont;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Bitmaps for each animation frame of the overland map */
	private BufferedImage [] overlandMapBitmaps;
	
	/** Bitmap for the shading at the edges of the area we can see */
	private BufferedImage fogOfWarBitmap;
	
	/** Colour backgrounds for each player's units */
	final Map<Integer, BufferedImage> unitBackgroundImages = new HashMap<Integer, BufferedImage> ();
	
	/** The plane that the UI is currently displaying */
	private int mapViewPlane = 0;
	
	/** Zoom factor for the overland map; between 10 (1x) and 20 (2x) */
	private int mapViewZoom = 10;
	
	/** Pixel offset into the map where the top left of the window is currently positioned, for scrolling around */
	private int mapViewX = 0;

	/** Pixel offset into the map where the top left of the window is currently positioned, for scrolling around */
	private int mapViewY = 0;
	
	/** Border around the top row of gold buttons */ 
	private BufferedImage topBarBackground;

	/** Number of pixels at the edge of the window where the map scrolls */
	private final static int MOUSE_SCROLL_WIDTH = 8;
	
	/** Number of pixels the map scrolls with each tick of the timer */
	private final static int MOUSE_SCROLL_SPEED = 4;
	
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
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");

		// Load images
		topBarBackground = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/background.png");
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

		optionsAction = new AbstractAction ()
		{
			@Override
			public void actionPerformed (final ActionEvent e)
			{
			}
		};

		// Need the tile set in a few places
		final TileSetEx overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_OVERLAND_MAP, "OverlandMapUI.init");
		
		// Initialize the frame
		getFrame ().setTitle ("Overland Map");
		getFrame ().setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ();
		contentPane.setBackground (Color.BLACK);
		contentPane.setPreferredSize (new Dimension (640, 480));
		
		// Set up main layout
 		// This is a 2x2 grid, with the top two cells being joined into one long bar
		contentPane.setLayout (new GridBagLayout ());
		
		final JPanel mapButtonBar = getUtils ().createPanelWithBackgroundImage (topBarBackground);
		contentPane.add (mapButtonBar, getUtils ().createConstraintsNoFill (0, 0, 2, 1, INSET, GridBagConstraintsNoFill.WEST));

		contentPane.add (getOverlandMapRightHandPanel ().getPanel (), getUtils ().createConstraintsNoFill (1, 1, 1, 1, INSET, GridBagConstraintsNoFill.NORTH));
		
		final JPanel sceneryPanel = new JPanel ()
		{
			private static final long serialVersionUID = -266294091485642841L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Scale the map image up smoothly
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

				// If the size of the map is smaller than the size of the space we're drawing it in, clip the right and/or bottom off, so that
				// the same bit doesn't get drawn twice
				final int mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
				final int mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
				
				g.setClip (0, 0, mapZoomedWidth, mapZoomedHeight);
				
				// Need to draw it 1-2 times in each direction, depending on wrapping params
				final int xRepeatCount = getClient ().getSessionDescription ().getMapSize ().isWrapsLeftToRight () ? 2 : 1;
				final int yRepeatCount = getClient ().getSessionDescription ().getMapSize ().isWrapsTopToBottom () ? 2 : 1;
				
				for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
					for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
					{
						// Draw the terrain
						g.drawImage (overlandMapBitmaps [terrainAnimFrame],
							(mapZoomedWidth * xRepeat) - mapViewX, (mapZoomedHeight * yRepeat) - mapViewY,
							mapZoomedWidth, mapZoomedHeight, null);
						
						// Shade the fog of war edges
						g.drawImage (fogOfWarBitmap,
							(mapZoomedWidth * xRepeat) - mapViewX, (mapZoomedHeight * yRepeat) - mapViewY,
							mapZoomedWidth, mapZoomedHeight, null);
					}
				
				// Draw units dynamically, over the bitmap.
				
				// For some reason, from the JUnit test for the overland map UI, this code makes the length of time that
				// paintComponent takes to execute behave very erratically, despite the fact that it never has anything to do
				// since the unit test includes no units to draw so unitToDrawAtEachLocation will always be a big area of nulls.
				
				// However this only seems to happen within the unit test and not when when this is running for real, so
				// my only conclusion can be that its Mockito itself that behaves erratically.
				final MemoryUnit [] [] unitToDrawAtEachLocation = chooseUnitToDrawAtEachLocation ();
				if (unitToDrawAtEachLocation != null)
					for (int x = 0; x < getClient ().getSessionDescription ().getMapSize ().getWidth (); x++)
						for (int y = 0; y < getClient ().getSessionDescription ().getMapSize ().getHeight (); y++)
						{
							final MemoryUnit unit = unitToDrawAtEachLocation [y] [x];
							if (unit != null)
							{
								try
								{
									final BufferedImage unitBackground = getUnitBackgroundImage (unit.getOwningPlayerID ());
									final BufferedImage unitImage = getUtils ().loadImage (getGraphicsDB ().findUnit (unit.getUnitID (), "sceneryPanel.paintComponent").getUnitOverlandImageFile ());

									final int unitZoomedWidth = (unitImage.getWidth () * mapViewZoom) / 10;
									final int unitZoomedHeight = (unitImage.getHeight () * mapViewZoom) / 10;
								
									final int xpos = (((x * overlandMapTileSet.getTileWidth ()) - ((unitImage.getWidth () - overlandMapTileSet.getTileWidth ()) / 2)) * mapViewZoom) / 10;
									final int ypos = (((y * overlandMapTileSet.getTileHeight ()) - ((unitImage.getHeight () - overlandMapTileSet.getTileHeight ()) / 2)) * mapViewZoom) / 10;

									for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
										for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
										{
											// Draw the unit
											g.drawImage (unitBackground,
												(mapZoomedWidth * xRepeat) - mapViewX + xpos, (mapZoomedHeight * yRepeat) - mapViewY + ypos,
												unitZoomedWidth, unitZoomedHeight, null);

											g.drawImage (unitImage,
												(mapZoomedWidth * xRepeat) - mapViewX + xpos, (mapZoomedHeight * yRepeat) - mapViewY + ypos,
												unitZoomedWidth, unitZoomedHeight, null);
										}
								}
								catch (final IOException e)
								{
									log.error ("Error trying to load graphics to draw Unit URN " + unit.getUnitURN () + " with ID " + unit.getUnitID (), e);
									
								}
							}
						}
			}
		};
		sceneryPanel.setBackground (Color.BLACK);
		
		// Let the scenery panel take up as much space as possible
		final GridBagConstraints sceneryConstraints = getUtils ().createConstraintsBothFill (0, 1, 1, 1, INSET);
		sceneryConstraints.weightx = 1;
		sceneryConstraints.weighty = 1;
		
		contentPane.add (sceneryPanel, sceneryConstraints);
	
		// Animate the terrain tiles
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

		// Zoom actions (need the sceneryPanel, hence why defined down here)
		zoomInAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -1891817301226938020L;

			@Override
			public void actionPerformed (final ActionEvent e)
			{
				if (mapViewZoom < 20)
				{
					// Make the zoom take effect from the centrepoint of the map, not the top-left corner
					double mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
					final double scaledX = (mapViewX + (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2)) / mapZoomedWidth;

					double mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
					final double scaledY = (mapViewY + (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2)) / mapZoomedHeight;
					
					mapViewZoom++;
					
					mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
					final int newMapViewX = (int) ((scaledX * mapZoomedWidth) - (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2));

					mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
					final int newMapViewY = (int) ((scaledY * mapZoomedHeight) - (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2));
					
					mapViewX = fixMapViewLimits (newMapViewX, (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10,
						sceneryPanel.getWidth (), getClient ().getSessionDescription ().getMapSize ().isWrapsLeftToRight ());

					mapViewY = fixMapViewLimits (newMapViewY, (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10,
						sceneryPanel.getHeight (), getClient ().getSessionDescription ().getMapSize ().isWrapsTopToBottom ());
					
					sceneryPanel.repaint ();
				}
			}
		};

		zoomOutAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 6611489435130331341L;

			@Override
			public void actionPerformed (final ActionEvent e)
			{
				if (mapViewZoom > 10)
				{
					// Make the zoom take effect from the centrepoint of the map, not the top-left corner
					double mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
					final double scaledX = (mapViewX + (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2)) / mapZoomedWidth;

					double mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
					final double scaledY = (mapViewY + (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2)) / mapZoomedHeight;
					
					mapViewZoom--;
					
					mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
					final int newMapViewX = (int) ((scaledX * mapZoomedWidth) - (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2));

					mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
					final int newMapViewY = (int) ((scaledY * mapZoomedHeight) - (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2));
					
					mapViewX = fixMapViewLimits (newMapViewX, (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10,
						sceneryPanel.getWidth (), getClient ().getSessionDescription ().getMapSize ().isWrapsLeftToRight ());

					mapViewY = fixMapViewLimits (newMapViewY, (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10,
						sceneryPanel.getHeight (), getClient ().getSessionDescription ().getMapSize ().isWrapsTopToBottom ());
					
					sceneryPanel.repaint ();
				}
			}
		};
		
		// Set up the row of gold buttons along the top
		mapButtonBar.setLayout (new GridBagLayout ());
		mapButtonBar.add (Box.createRigidArea (new Dimension (7, 0)), getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (gameAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		mapButtonBar.add (getUtils ().createImageButton (spellsAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (armiesAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (3, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (citiesAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (4, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (magicAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (5, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (planeAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (6, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (messagesAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (7, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (chatAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (8, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (infoAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarInfoNormal, topBarInfoPressed, topBarInfoNormal), getUtils ().createConstraintsNoFill (9, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		mapButtonBar.add (getUtils ().createImageButton (zoomInAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarZoomInNormal, topBarZoomInPressed, topBarZoomInNormal), getUtils ().createConstraintsNoFill (10, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (zoomOutAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarZoomOutNormal, topBarZoomOutPressed, topBarZoomOutNormal), getUtils ().createConstraintsNoFill (11, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (optionsAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
			topBarOptionsNormal, topBarOptionsPressed, topBarOptionsNormal), getUtils ().createConstraintsNoFill (12, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final GridBagConstraints turnLabelConstraints = getUtils ().createConstraintsNoFill (13, 0, 1, 1, INSET, GridBagConstraintsNoFill.EAST);
		turnLabelConstraints.weightx = 1;		// Right justify the label
		turnLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapButtonBar.add (turnLabel, turnLabelConstraints);

		mapButtonBar.add (Box.createRigidArea (new Dimension (7, 0)), getUtils ().createConstraintsNoFill (14, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Stop frame being shrunk smaller than this
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setLocationRelativeTo (null);
		getFrame ().setMinimumSize (getFrame ().getSize ());
		
		// Capture mouse clicks on the scenery panel
		sceneryPanel.addMouseListener (new MouseAdapter ()
		{
			/**
			 * @param ev Click event
			 */
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				// Ignore clicks in the black area if the window is too large for the map
				final int mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
				final int mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
				if ((ev.getX () < mapZoomedWidth) && (ev.getY () < mapZoomedHeight))
				{
					// Convert pixel coordinates back into a map cell
					int mapCellX = (((ev.getX () + mapViewX) * 10) / mapViewZoom) / overlandMapTileSet.getTileWidth ();
					int mapCellY = (((ev.getY () + mapViewY) * 10) / mapViewZoom) / overlandMapTileSet.getTileHeight ();
					
					final MapSizeData mapSize = getClient ().getSessionDescription ().getMapSize ();
					
					while (mapCellX < 0) mapCellX = mapCellX + mapSize.getWidth ();
					while (mapCellX >= mapSize.getWidth ()) mapCellX = mapCellX - mapSize.getWidth (); 
					while (mapCellY < 0) mapCellY = mapCellY + mapSize.getHeight ();
					while (mapCellY >= mapSize.getHeight ()) mapCellY = mapCellY - mapSize.getHeight ();
					
					// What's at that location
					final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(mapViewPlane).getRow ().get (mapCellY).getCell ().get (mapCellX).getCityData ();
					if ((cityData != null) && (cityData.getCityPopulation () != null) && (cityData.getCityPopulation () > 0))
					{
						// Is there a city view already open for this city?
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (mapCellX, mapCellY, mapViewPlane);
						CityViewUI cityView = getClient ().getCityViews ().get (cityLocation.toString ());
						if (cityView == null)
						{
							cityView = getPrototypeFrameCreator ().createCityView ();
							cityView.setCityLocation (new MapCoordinates3DEx (mapCellX, mapCellY, mapViewPlane));
							getClient ().getCityViews ().put (cityLocation.toString (), cityView);
						}
						
						try
						{
							cityView.setVisible (true);
						}
						catch (final IOException e)
						{
							log.error (e, e);
						}
					}
					else
					{
						// Show selection boxes for any units at this location
					}
				}
			}
		});
		
		// We should also set a MaximumSize both here and whenever the zoom is changed, but JFrame.setMaximumSize doesn't work and
		// any workaround listening to componentResized events and forcibly setting the size after just doesn't work cleanly or look at all nice.
		// So I'd rather just allow the window to be made too big and leave a black area.
		
		// Scroll the map around if the mouse pointer is near the edges
		new Timer (20, new ActionListener ()
		{
			@Override
			public final void actionPerformed (final ActionEvent e)
			{
				final Point pos = contentPane.getMousePosition ();
				if (pos != null)
				{
					int newMapViewX = mapViewX;
					int newMapViewY = mapViewY;
					
					boolean mapViewUpdated = false;
					if (pos.x < MOUSE_SCROLL_WIDTH)
					{
						newMapViewX = newMapViewX - MOUSE_SCROLL_SPEED;
						mapViewUpdated = true;
					}
					
					if (pos.y < MOUSE_SCROLL_WIDTH)
					{
						newMapViewY = newMapViewY - MOUSE_SCROLL_SPEED;
						mapViewUpdated = true;
					}

					if (pos.x >= contentPane.getWidth () - MOUSE_SCROLL_WIDTH)
					{
						newMapViewX = newMapViewX + MOUSE_SCROLL_SPEED;
						mapViewUpdated = true;
					}

					if (pos.y >= contentPane.getHeight () - MOUSE_SCROLL_WIDTH)
					{
						newMapViewY = newMapViewY + MOUSE_SCROLL_SPEED;
						mapViewUpdated = true;
					}
					
					if (mapViewUpdated)
					{
						newMapViewX = fixMapViewLimits (newMapViewX, (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10,
							sceneryPanel.getWidth (), getClient ().getSessionDescription ().getMapSize ().isWrapsLeftToRight ());

						newMapViewY = fixMapViewLimits (newMapViewY, (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10,
							sceneryPanel.getHeight (), getClient ().getSessionDescription ().getMapSize ().isWrapsTopToBottom ());
					
						if ((newMapViewX != mapViewX) || (newMapViewY != mapViewY))
						{
							mapViewX = newMapViewX;
							mapViewY = newMapViewY;
							sceneryPanel.repaint ();
						}
					}
				}
			}
		}).start ();

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		gameAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Game"));
		spellsAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Spells"));
		armiesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Armies"));
		citiesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Cities"));
		magicAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Magic"));
		planeAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Plane"));
		messagesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "NewTurnMessages"));
		chatAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMapButtonBar", "Chat"));

		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * Make sure map scroll value doesn't go outside acceptable range; if it does then constrain it back or wrap, as appropriate
	 * 
	 * @param value Desired value
	 * @param mapSize Size of the map in this direction (width or height of the map taking current zoom level into account)
	 * @param windowSize Size of the window that the map is displayed in
	 * @param wrapping Whether the map wraps in this direction
	 * @return Corrected value
	 */
	final int fixMapViewLimits (final int value, final int mapSize, final int windowSize, final boolean wrapping)
	{
		int newValue = value;
		
		if (wrapping)
		{
			while (newValue < 0)
				newValue = newValue + mapSize;

			while (newValue >= mapSize)
				newValue = newValue - mapSize;
		}
		else
		{
			newValue = Math.min (newValue, mapSize - windowSize);
			newValue = Math.max (0, newValue);
		}
		
		return newValue;
	}
	
	/**
	 * Generates big bitmaps of the entire overland map in each frame of animation
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateOverlandMapBitmaps () throws IOException
	{
		log.trace ("Entering regenerateOverlandMapBitmaps: " + mapViewPlane);
		
		overlandMapBitmaps = getOverlandMapBitmapGenerator ().generateOverlandMapBitmaps (mapViewPlane);
		
		log.trace ("Exiting regenerateOverlandMapBitmaps"); 
	}
	
	/**
	 * Generates big bitmap of the smoothed edges of blackness that obscure the edges
	 * of the outermost tiles we can see, so that the edges aren't just a solid black line
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateFogOfWarBitmap () throws IOException
	{
		log.trace ("Entering regenerateFogOfWarBitmap");
		
		fogOfWarBitmap = getOverlandMapBitmapGenerator ().generateFogOfWarBitmap (mapViewPlane);
		
		log.trace ("Exiting regenerateFogOfWarBitmap"); 
	}

	/**	 * @param playerID Unit owner player ID
	 * @return Unit background image in their correct colour 
	 * @throws IOException If there is a problem loading the background image
	 */
	private final BufferedImage getUnitBackgroundImage (final int playerID) throws IOException
	{
		BufferedImage image = unitBackgroundImages.get (playerID);
		if (image == null)
		{
			// Generate a new one
			final BufferedImage whiteImage = getUtils ().loadImage (GraphicsDatabaseConstants.UNIT_BACKGROUND_FLAG);
			
			final PlayerPublicDetails player = MultiplayerSessionUtils.findPlayerWithID (getClient ().getPlayers (), playerID, "getUnitBackgroundImage");
			final MomTransientPlayerPublicKnowledge trans = (MomTransientPlayerPublicKnowledge) player.getTransientPlayerPublicKnowledge ();
			
			image = getUtils ().multiplyImageByColour (whiteImage, Integer.parseInt (trans.getFlagColour (), 16));
			unitBackgroundImages.put (playerID, image);
		}
		return image;
	}
	
	/**
	 * This prefers to units with the highest special order - this will make settlers always show on top if
	 * they're building cities, and make non-patrolling units appear in preference to patrolling units
	 * 
	 * @return Array of units to draw at each map cell; or null if there are absolutely no units to draw
	 */
	final MemoryUnit [] [] chooseUnitToDrawAtEachLocation ()
	{
		final MemoryUnit [] [] bestUnits = new MemoryUnit
			[getClient ().getSessionDescription ().getMapSize ().getHeight ()] [getClient ().getSessionDescription ().getMapSize ().getWidth ()];
		
		boolean found = false;
		for (final MemoryUnit unit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())

			// Is it alive, and are we looking at the right plane to see it?
			if ((unit.getStatus () == UnitStatusID.ALIVE) && ((unit.getUnitLocation ().getZ () == mapViewPlane) ||
				(getMemoryGridCellUtils ().isTerrainTowerOfWizardry (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(unit.getUnitLocation ().getZ ()).getRow ().get (unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ()).getTerrainData ()))))
			{
				found = true;
				final MemoryUnit existingUnit = bestUnits [unit.getUnitLocation ().getY ()] [unit.getUnitLocation ().getX ()];
				
				// Show real special orders in preference to patrol
				if ((existingUnit == null) ||
					((existingUnit != null) && (existingUnit.getSpecialOrder () == null) && (unit.getSpecialOrder () != null)) ||
					((existingUnit != null) && (existingUnit.getSpecialOrder () == UnitSpecialOrder.PATROL) && (unit.getSpecialOrder () != null) && (unit.getSpecialOrder () != UnitSpecialOrder.PATROL)))
					
					bestUnits [unit.getUnitLocation ().getY ()] [unit.getUnitLocation ().getX ()] = unit;
			}
		
		// This is really here for the benefit of the unit test, which has no units, but the performance of
		// Mockito when executing 1000s of mocks (like in the unit loop that processes the output from this method)
		// is very erratic and makes the scrolling very irregular.  So outputting null can cut that entire loop, and
		// several 1000 mock invocations, out.
		return found ? bestUnits : null;
	}
	
	/**
	 * Updates the turn label
	 */
	public final void updateTurnLabelText ()
	{
		log.trace ("Entering updateTurnLabelText");
		
		// Turn 1 is January 1400 - so last turn in 1400 is turn 12
		// Months are numbered 1-12
		final int year = 1400 + ((getClient ().getGeneralPublicKnowledge ().getTurnNumber () - 1) / 12);
		int month = getClient ().getGeneralPublicKnowledge ().getTurnNumber () % 12;
		if (month == 0)
			month = 12;
		
		// Build up description
		final String monthText = getLanguage ().findCategoryEntry ("Months", "MNTH" + ((month < 10) ? "0" : "") + month);
		turnLabel.setText (getLanguage ().findCategoryEntry ("frmMapButtonBar", "Turn").replaceAll
			("MONTH", monthText).replaceAll ("YEAR", new Integer (year).toString ()).replaceAll ("TURN",
			new Integer (getClient ().getGeneralPublicKnowledge ().getTurnNumber ()).toString ()));
		
		log.trace ("Exiting updateTurnLabelText");
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
	 * @return MemoryGridCell utils
	 */
	public final MemoryGridCellUtils getMemoryGridCellUtils ()
	{
		return memoryGridCellUtils;
	}

	/**
	 * @param utils MemoryGridCell utils
	 */
	public final void setMemoryGridCellUtils (final MemoryGridCellUtils utils)
	{
		memoryGridCellUtils = utils;
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
	 * @return Bitmap generator */
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
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}
}
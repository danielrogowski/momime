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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.language.database.v0_9_5.Building;
import momime.client.messages.process.MoveUnitStackOverlandMessageImpl;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.UnitRowDisplayUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.OverlandMapRightHandPanelTop;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.newgame.MapSizeData;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PendingMovement;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitSpecialOrder;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.messages.servertoclient.MapVolumeOfOverlandMoveType;
import momime.common.messages.servertoclient.OverlandMoveTypeID;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.TargetSpellResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Screen for displaying the overland map, including the buttons and side panels and so on that appear in the same frame
 */
public final class OverlandMapUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OverlandMapUI.class);

	/** Number of pixels at the edge of the window where the map scrolls */
	private final static int MOUSE_SCROLL_WIDTH = 8;

	/** Number of pixels the map scrolls with each tick of the timer */
	private final static int MOUSE_SCROLL_SPEED = 4;

	/** Brighten areas we can move to in 1 turn */
	private final static int MOVE_IN_ONE_TURN_COLOUR = 0x30FFFFFF;

	/** Darken areas we cannot move to at all */
	private final static int CANNOT_MOVE_HERE_COLOUR = 0x70000000;

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

	/** Player colour image generator */
	private PlayerColourImageGenerator playerColourImageGenerator;

	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** Spell book */
	private SpellBookUI spellBookUI;

	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Advisors UI */
	private SelectAdvisorUI selectAdvisorUI;

	/** Options UI */
	private OptionsUI optionsUI;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;

	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;

	/** Client config, containing various overland map settings */
	private MomImeClientConfig clientConfig;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Unit stack that's in the middle of moving from one cell to another */
	private MoveUnitStackOverlandMessageImpl unitStackMoving;

	/** Bitmaps for each animation frame of the overland map */
	private BufferedImage [] overlandMapBitmaps;

	/** Bitmap for the shading at the edges of the area we can see */
	private BufferedImage fogOfWarBitmap;

	/** Area detailing which map cells we can/can't move to */
	private MapVolumeOfOverlandMoveType movementTypes;

	/** Shading area generated from movementTypes */
	private BufferedImage movementTypesBitmap;

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
	
	/** Overland map tileset */
	private TileSetEx overlandMapTileSet;

	/** Animation to display for a spell being cast */
	private AnimationEx overlandCastAnimation;
	
	/** X coord to display overland cast animation at, in pixels */
	private int overlandCastAnimationX;

	/** Y coord to display overland cast animation at, in pixels */
	private int overlandCastAnimationY;
	
	/** Frame number to display of overland cast animation */
	private int overlandCastAnimationFrame;
	
	// UI Components

	/** Typical inset used on this screen layout */
	private final static int INSET = 0;

	/** Panel showing the map terrain */
	private JPanel sceneryPanel;

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
	 * 
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
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};

		spellsAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getSpellBookUI ().setVisible (true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		armiesAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};

		citiesAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};

		magicAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getMagicSlidersUI ().setVisible (true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		planeAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				mapViewPlane = 1 - mapViewPlane;
				
				try
				{
					regenerateOverlandMapBitmaps ();
					regenerateFogOfWarBitmap ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}

				// Keep the same movement types array, but regenerate the bitmap from it to show movement available on the new plane
				setMovementTypes (movementTypes);
			}
		};

		messagesAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getNewTurnMessagesUI ().setVisible (true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		chatAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};

		infoAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getSelectAdvisorUI ().setVisible (true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		optionsAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getOptionsUI ().setVisible (true);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		};

		// Need the tile set in a few places
		overlandMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_OVERLAND_MAP, "OverlandMapUI.init");
		
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
		
		sceneryPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Scale the map image up smoothly
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION,
					getClientConfig ().isOverlandSmoothTextures () ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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
						if (fogOfWarBitmap != null)
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
								final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
									(mapViewPlane).getRow ().get (y).getCell ().get (x);

								// Make the unit that's selected to move blink
								final boolean drawUnit;
								if ((getOverlandMapProcessing ().isAnyUnitSelectedToMove ()) && (getOverlandMapProcessing ().getUnitMoveFrom () != null) &&
									(getOverlandMapProcessing ().getUnitMoveFrom ().getX () == x) && (getOverlandMapProcessing ().getUnitMoveFrom ().getY () == y))
								{
									// The moving stack might be on the other plane
									if ((mapViewPlane == getOverlandMapProcessing ().getUnitMoveFrom ().getZ ()) ||
										(getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ())))
										
										// We are looking at the unit stack that's moving - so blink it on and off, even if its inside a city
										drawUnit = (terrainAnimFrame % 2 == 0);
									else
										// We are looking at a unit in the same location as the unit stack that's moving, but on the other plane - so draw it, unless its a city
										drawUnit = (mc.getCityData () == null);
								}
								else
									// Regular location away from the moving unit stack - don't draw units in cities since they cover the city up
									drawUnit = (mc.getCityData () == null);
								
								if (drawUnit)
									try
									{
										final BufferedImage unitBackground = getPlayerColourImageGenerator ().getUnitBackgroundImage (unit.getOwningPlayerID ());
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

				// Draw the unit stack that's halfway between two cells during movement
				if (getUnitStackMoving () != null)
				{
					final MemoryUnit unit = getUnitStackMoving ().getUnitToDraw ();
					try
					{
						final BufferedImage unitBackground = getPlayerColourImageGenerator ().getUnitBackgroundImage (unit.getOwningPlayerID ());
						final BufferedImage unitImage = getUtils ().loadImage (getGraphicsDB ().findUnit (unit.getUnitID (), "sceneryPanel.paintComponent").getUnitOverlandImageFile ());

						final int unitZoomedWidth = (unitImage.getWidth () * mapViewZoom) / 10;
						final int unitZoomedHeight = (unitImage.getHeight () * mapViewZoom) / 10;
					
						final int xpos = ((getUnitStackMoving ().getCurrentX () - ((unitImage.getWidth () - overlandMapTileSet.getTileWidth ()) / 2)) * mapViewZoom) / 10;
						final int ypos = ((getUnitStackMoving ().getCurrentY () - ((unitImage.getHeight () - overlandMapTileSet.getTileHeight ()) / 2)) * mapViewZoom) / 10;

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
						log.error ("Error trying to load graphics to draw moving Unit URN " + unit.getUnitURN () + " with ID " + unit.getUnitID (), e);
					}
				}
				
				// Darken areas of the map that the selected units cannot move to
				if (movementTypesBitmap != null)
				{
					// Turn anti-aliasing back off for this, so the shaded areas line up properly over the tiles
					if (getClientConfig ().isOverlandSmoothTextures ())
						g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					
					for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
						for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
							g.drawImage (movementTypesBitmap,
								(mapZoomedWidth * xRepeat) - mapViewX, (mapZoomedHeight * yRepeat) - mapViewY,
								mapZoomedWidth, mapZoomedHeight, null);
					
					if (getClientConfig ().isOverlandSmoothTextures ())
						g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				}
				
				// Show pending movements
				if (getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement ().size () > 0)
				{
					try
					{
						// All pending movements are now drawn with a boot - its too difficult to figure out what icon to draw
						// if we have e.g. a Spearmen, Magic Spirit and Great Drake in the same stack.
						// The Delphi code actually tried to derive the location of this from the Graphics XML but it isn't really necessary.
						final BufferedImage bootImage = getUtils ().loadImage ("/momime.client.graphics/unitSkills/USX01-move.png");

						final int bootZoomedWidth = (bootImage.getWidth () * mapViewZoom) / 10;
						final int bootZoomedHeight = (bootImage.getHeight () * mapViewZoom) / 10;

						final int arrowZoomedWidth = (overlandMapTileSet.getTileWidth () * mapViewZoom) / 10;
						final int arrowZoomedHeight = (overlandMapTileSet.getTileHeight () * mapViewZoom) / 10;
						
						for (final PendingMovement pendingMovement : getClient ().getOurTransientPlayerPrivateKnowledge ().getPendingMovement ())
							if (pendingMovement.getMoveTo ().getZ () == mapViewPlane)
							{
								// Note we actually start from the destination and walk backwards to the current unit location
								final MapCoordinates2DEx coords = new MapCoordinates2DEx (pendingMovement.getMoveTo ().getX (), pendingMovement.getMoveTo ().getY ());
								int moveOutFromDirectionImageNo = 0;
								
								for (final Integer d : pendingMovement.getPath ())
								{
									final int dReversed = getCoordinateSystemUtils ().normalizeDirection (getClient ().getSessionDescription ().getMapSize ().getCoordinateSystemType (), d+4);

									final int bootX = (((coords.getX () * overlandMapTileSet.getTileWidth ()) - ((bootImage.getWidth () - overlandMapTileSet.getTileWidth ()) / 2)) * mapViewZoom) / 10;
									final int bootY = (((coords.getY () * overlandMapTileSet.getTileHeight ()) - ((bootImage.getHeight () - overlandMapTileSet.getTileHeight ()) / 2)) * mapViewZoom) / 10;

									final int arrowX = (coords.getX () * overlandMapTileSet.getTileWidth () * mapViewZoom) / 10;
									final int arrowY = (coords.getY () * overlandMapTileSet.getTileHeight () * mapViewZoom) / 10;
									
									for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
										for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
										{
											// Draw boot in centre
											g.drawImage (bootImage,
												(mapZoomedWidth * xRepeat) - mapViewX + bootX, (mapZoomedHeight * yRepeat) - mapViewY + bootY,
												bootZoomedWidth, bootZoomedHeight, null);
											
											// Draw arrow moving out from this square
											if (moveOutFromDirectionImageNo > 0)
											{
												final BufferedImage arrowImage = getUtils ().loadImage ("/momime.client.graphics/overland/pendingMovement/moveOutOfMapCell-d" + moveOutFromDirectionImageNo + ".png");
												g.drawImage (arrowImage,
													(mapZoomedWidth * xRepeat) - mapViewX + arrowX, (mapZoomedHeight * yRepeat) - mapViewY + arrowY,
													arrowZoomedWidth, arrowZoomedHeight, null);
											}
											
											// Draw arrow moving into this square
											final BufferedImage arrowImage = getUtils ().loadImage ("/momime.client.graphics/overland/pendingMovement/moveInToMapCell-d" + d + ".png");
											g.drawImage (arrowImage,
												(mapZoomedWidth * xRepeat) - mapViewX + arrowX, (mapZoomedHeight * yRepeat) - mapViewY + arrowY,
												arrowZoomedWidth, arrowZoomedHeight, null);
										}
									
									// Set arrow number to draw moving out from the next square
									moveOutFromDirectionImageNo = d;
									
									// Move to next square
									getCoordinateSystemUtils ().move2DCoordinates (getClient ().getSessionDescription ().getMapSize (), coords, dReversed);
								}
							}
					}
					catch (final IOException e)
					{
						log.error (e, e);
					}
				}

				// Draw casting animation?
				if (getOverlandCastAnimation () != null)
					try
					{
						final BufferedImage castImage = getUtils ().loadImage (getOverlandCastAnimation ().getFrame ().get (getOverlandCastAnimationFrame ()));

						final int castZoomedWidth = (castImage.getWidth () * mapViewZoom) / 10;
						final int castZoomedHeight = (castImage.getHeight () * mapViewZoom) / 10;
					
						final int xpos = (getOverlandCastAnimationX () * mapViewZoom) / 10;
						final int ypos = (getOverlandCastAnimationY () * mapViewZoom) / 10;

						for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
							for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
								g.drawImage (castImage,
									(mapZoomedWidth * xRepeat) - mapViewX + xpos, (mapZoomedHeight * yRepeat) - mapViewY + ypos,
									castZoomedWidth, castZoomedHeight, null);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				// Draw white border around map edge?
				if (getClientConfig ().isDebugShowEdgesOfMap ())
				{
					g.setColor (Color.GRAY);
					
					if (getClient ().getSessionDescription ().getMapSize ().isWrapsLeftToRight ())
						g.drawLine (mapZoomedWidth - mapViewX, 0, mapZoomedWidth - mapViewX, mapZoomedHeight);

					if (getClient ().getSessionDescription ().getMapSize ().isWrapsTopToBottom ())
						g.drawLine (0, mapZoomedHeight - mapViewY, mapZoomedWidth, mapZoomedHeight - mapViewY);
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
				
				// The mini maps on all the city views run from the same timer
				for (final CityViewUI cityView : getClient ().getCityViews ().values ())
					cityView.repaintCityViewMiniMap ();
			}
		}).start ();

		// Zoom actions (need the sceneryPanel, hence why defined down here)
		zoomInAction = new AbstractAction ()
		{
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
		
		// Capture mouse clicks on the scenery panel
		sceneryPanel.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				final MapCoordinates2DEx mapCoords = convertMouseCoordsToMapGridCell (ev);
				if (mapCoords != null)
				{
					final int mapCellX = mapCoords.getX ();
					final int mapCellY = mapCoords.getY ();
					
					final MapCoordinates3DEx mapLocation = new MapCoordinates3DEx (mapCellX, mapCellY, mapViewPlane);
					final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
						(mapViewPlane).getRow ().get (mapCellY).getCell ().get (mapCellX);
					
					try
					{
						// Right clicking to get unit info screen
						if (ev.getButton () != MouseEvent.BUTTON1)
						{
							final OverlandMapCityData cityData = mc.getCityData ();
							if ((cityData != null) && (cityData.getCityPopulation () != null) && (cityData.getCityPopulation () > 0))
							{
								// Right clicking on a city to get the city screen up - is there a city view already open for this city?
								CityViewUI cityView = getClient ().getCityViews ().get (mapLocation.toString ());
								if (cityView == null)
								{
									cityView = getPrototypeFrameCreator ().createCityView ();
									cityView.setCityLocation (new MapCoordinates3DEx (mapCellX, mapCellY, mapViewPlane));
									getClient ().getCityViews ().put (mapLocation.toString (), cityView);
								}
							
								cityView.setVisible (true);
							}
							else
							{
								// Right clicking to select or view a stack of units
								if (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ()))
									getOverlandMapProcessing ().showSelectUnitBoxes (new MapCoordinates3DEx (mapCellX, mapCellY, 0));
								else
									getOverlandMapProcessing ().showSelectUnitBoxes (new MapCoordinates3DEx (mapCellX, mapCellY, mapViewPlane));
							}
						}
						
						// Left clicking to target an overland spell
						else if (getOverlandMapRightHandPanel ().getTop () == OverlandMapRightHandPanelTop.TARGET_SPELL)
						{
							final Spell spell = getClient ().getClientDB ().findSpell (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID (), "OverlandMapUI");
							if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) ||
								(spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES))
							{
								// If there isn't even a city here then don't even display a message
								if ((mc.getCityData () != null) && (mc.getCityData ().getCityPopulation () != null) && (mc.getCityData ().getCityPopulation () > 0))
								{
									// Use common routine to do all the validation
									final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell
										(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), spell,
										getClient ().getOurPlayerID (), mapLocation, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
										getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getClient ().getClientDB ());
									
									if (validTarget == TargetSpellResult.VALID_TARGET)
									{
										final TargetSpellMessage msg = new TargetSpellMessage ();
										msg.setSpellID (spell.getSpellID ());
										msg.setCityLocation (mapLocation);
										getClient ().getServerConnection ().sendMessageToServer (msg);
										
										// Close out the "Target Spell" right hand panel
										getOverlandMapProcessing ().updateMovementRemaining ();
									}
									else if (validTarget.getCityLanguageEntryID () != null)
									{
										final momime.client.language.database.v0_9_5.Spell spellLang = getLanguage ().findSpell (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ());
										final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
										
										String buildingName;
										if (spell.getBuildingID () == null)
											buildingName = "";
										else
										{
											final Building buildingLang = getLanguage ().findBuilding (spell.getBuildingID ());
											buildingName = (buildingLang != null) ? buildingLang.getBuildingName () : null;
											if (buildingName == null)
												buildingName = "";
										}
										
										final String text = getLanguage ().findCategoryEntry ("SpellTargetting", validTarget.getCityLanguageEntryID ()).replaceAll
											("SPELL_NAME", (spellName != null) ? spellName : getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ()).replaceAll
											("BUILDING_NAME", buildingName);
										
										final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
										msg.setTitleLanguageCategoryID ("SpellTargetting");
										msg.setTitleLanguageEntryID ("Title");
										msg.setText (text);
										msg.setVisible (true);												
									}
								}
							}
							
							// NB. There are no overland unit curses
							else if (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS)
							{
								// Find our units at this map location
								final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
								for (final MemoryUnit unit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
									if ((mapLocation.equals (unit.getUnitLocation ())) && (unit.getStatus () == UnitStatusID.ALIVE) &&
										(unit.getOwningPlayerID () == getClient ().getOurPlayerID ()))
										
										units.add (unit);
								
								if (units.size () > 0)
								{
									final UnitRowDisplayUI unitRowDisplay = getPrototypeFrameCreator ().createUnitRowDisplay ();
									unitRowDisplay.setUnits (units);
									unitRowDisplay.setTargetSpell (getOverlandMapRightHandPanel ().getTargetSpell ());
									unitRowDisplay.setVisible (true);
								}
							}
						}
						
						// Left clicking on a space to move a stack of units to - can only do this if its our turn
						else if ((getOverlandMapRightHandPanel ().getTop () != OverlandMapRightHandPanelTop.SURVEYOR) &&
							((getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) ||
							(getClient ().getOurPlayerID ().equals (getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ()))) &&
							(getMovementTypes () != null) &&
							(getMovementTypes ().getPlane ().get (mapViewPlane).getRow ().get (mapCellY).getCell ().get (mapCellX) != OverlandMoveTypeID.CANNOT_MOVE_HERE))
							
							// NB. We don't check here that we actually have a unit selected - MoveUnitStackTo does this for us
							getOverlandMapProcessing ().moveUnitStackTo (new MapCoordinates3DEx (mapCellX, mapCellY, mapViewPlane));
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		});

		sceneryPanel.addMouseMotionListener (new MouseAdapter ()
		{
			@Override
			public final void mouseMoved (final MouseEvent ev)
			{
				// We only care about mouse movement when displaying the surveyor
				if (getOverlandMapRightHandPanel ().getTop () == OverlandMapRightHandPanelTop.SURVEYOR)
					try
					{				
						final MapCoordinates2DEx mapCoords = convertMouseCoordsToMapGridCell (ev);
						if (mapCoords != null)
						{
							final int mapCellX = mapCoords.getX ();
							final int mapCellY = mapCoords.getY ();
		
							getOverlandMapRightHandPanel ().setSurveyorLocation (new MapCoordinates3DEx (mapCellX, mapCellY, mapViewPlane));
						}
						else
							getOverlandMapRightHandPanel ().setSurveyorLocation (null);
					}
					catch (final Exception e)
					{
						log.error (e, e);
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
	 * Converts from pixel coordinates back to overland map coordinates
	 * 
	 * @param ev Mouse click event
	 * @return Overland map coordinates, or null if the mouse coordinates are off the map
	 */
	private MapCoordinates2DEx convertMouseCoordsToMapGridCell (final MouseEvent ev)
	{
		final MapCoordinates2DEx result;
		
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
			
			result = new MapCoordinates2DEx (mapCellX, mapCellY);
		}
		else
			result = null;
		
		return result;
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
	 * Generates big bitmaps of the entire overland map in each frame of animation.
	 * Delphi client did this rather differently, by building Direct3D vertex buffers to display all the map tiles; equivalent method there was RegenerateCompleteSceneryView.
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateOverlandMapBitmaps () throws IOException
	{
		log.trace ("Entering regenerateOverlandMapBitmaps: " + mapViewPlane);

		overlandMapBitmaps = getOverlandMapBitmapGenerator ().generateOverlandMapBitmaps (mapViewPlane,
			0, 0, getClient ().getSessionDescription ().getMapSize ().getWidth (), getClient ().getSessionDescription ().getMapSize ().getHeight ());
		
		// Tell all the city screens to do the same.
		// A bit weird putting this in the map UI, it should go on the messages, but at least this way the messages then only have to call 1 method so I don't
		// have to duplicate this city loop in 5+ places and forget to do it somewhere.
		for (final CityViewUI cityView : getClient ().getCityViews ().values ())
			cityView.regenerateCityViewMiniMapBitmaps ();

		log.trace ("Exiting regenerateOverlandMapBitmaps");
	}

	/**
	 * Generates big bitmap of the smoothed edges of blackness that obscure the edges of the outermost tiles we can see, so that the edges aren't just a solid black line
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateFogOfWarBitmap () throws IOException
	{
		log.trace ("Entering regenerateFogOfWarBitmap");

		fogOfWarBitmap = getOverlandMapBitmapGenerator ().generateFogOfWarBitmap (mapViewPlane,
			0, 0, getClient ().getSessionDescription ().getMapSize ().getWidth (), getClient ().getSessionDescription ().getMapSize ().getHeight ());

		// Tell all the city screens to do the same
		for (final CityViewUI cityView : getClient ().getCityViews ().values ())
			cityView.regenerateCityViewMiniMapFogOfWar ();
		
		log.trace ("Exiting regenerateFogOfWarBitmap");
	}

	/**
	 * This prefers to units with the highest special order - this will make settlers always show on top if they're building cities, and make non-patrolling units appear in preference to patrolling units
	 * 
	 * @return Array of units to draw at each map cell; or null if there are absolutely no units to draw
	 */
	final MemoryUnit [][] chooseUnitToDrawAtEachLocation ()
	{
		final MemoryUnit [][] bestUnits = new MemoryUnit [getClient ().getSessionDescription ().getMapSize ().getHeight ()] [getClient ().getSessionDescription ().getMapSize ().getWidth ()];

		boolean found = false;
		for (final MemoryUnit unit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())

			// Is it alive, and are we looking at the right plane to see it?
			if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getUnitLocation () != null) && ((unit.getUnitLocation ().getZ () == mapViewPlane) || (getMemoryGridCellUtils ().isTerrainTowerOfWizardry (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get (unit.getUnitLocation ().getZ ()).getRow ().get (unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ()).getTerrainData ()))))
			{
				// If this is a map cell where we've got units selected to move, make sure we show one of the units that's moving
				final boolean drawUnit;
				if ((getOverlandMapProcessing ().getUnitMoveFrom () != null) && (getOverlandMapProcessing ().isAnyUnitSelectedToMove ()) &&
					(getOverlandMapProcessing ().getUnitMoveFrom ().equals (unit.getUnitLocation ())))
					
					drawUnit = getOverlandMapProcessing ().isUnitSelected (unit);
				else
					drawUnit = true;
				
				if (drawUnit)
				{
					found = true;
					final MemoryUnit existingUnit = bestUnits [unit.getUnitLocation ().getY ()] [unit.getUnitLocation ().getX ()];
	
					// Show real special orders in preference to patrol
					if ((existingUnit == null) || ((existingUnit != null) && (existingUnit.getSpecialOrder () == null) && (unit.getSpecialOrder () != null)) || ((existingUnit != null) && (existingUnit.getSpecialOrder () == UnitSpecialOrder.PATROL) && (unit.getSpecialOrder () != null) && (unit.getSpecialOrder () != UnitSpecialOrder.PATROL)))
	
						bestUnits [unit.getUnitLocation ().getY ()] [unit.getUnitLocation ().getX ()] = unit;
				}
			}

		// This is really here for the benefit of the unit test, which has no units, but the performance of
		// Mockito when executing 1000s of mocks (like in the unit loop that processes the output from this method)
		// is very erratic and makes the scrolling very irregular. So outputting null can cut that entire loop, and
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
		turnLabel.setText (getLanguage ().findCategoryEntry ("frmMapButtonBar", "Turn").replaceAll ("MONTH", monthText).replaceAll ("YEAR", new Integer (year).toString ()).replaceAll ("TURN", new Integer (getClient ().getGeneralPublicKnowledge ().getTurnNumber ()).toString ()));

		log.trace ("Exiting updateTurnLabelText");
	}

	/**
	 * @return Area detailing which map cells we can/can't move to
	 */
	public final MapVolumeOfOverlandMoveType getMovementTypes ()
	{
		return movementTypes;
	}

	/**
	 * @param moves Area detailing which map cells we can/can't move to
	 */
	public final void setMovementTypes (final MapVolumeOfOverlandMoveType moves)
	{
		log.trace ("Entering setMovementTypes");

		movementTypes = moves;

		// Regenerate shading bitmap
		if (getMovementTypes () == null)
			movementTypesBitmap = null;
		else
		{
			movementTypesBitmap = new BufferedImage (getClient ().getSessionDescription ().getMapSize ().getWidth (), getClient ().getSessionDescription ().getMapSize ().getHeight (), BufferedImage.TYPE_INT_ARGB);

			for (int x = 0; x < getClient ().getSessionDescription ().getMapSize ().getWidth (); x++)
				for (int y = 0; y < getClient ().getSessionDescription ().getMapSize ().getHeight (); y++)
					switch (getMovementTypes ().getPlane ().get (mapViewPlane).getRow ().get (y).getCell ().get (x))
					{
						// Brighten areas we can move to in 1 turn
						case MOVE_IN_ONE_TURN:
							movementTypesBitmap.setRGB (x, y, MOVE_IN_ONE_TURN_COLOUR);
							break;

						// Darken areas we cannot move to at all
						case CANNOT_MOVE_HERE:
							movementTypesBitmap.setRGB (x, y, CANNOT_MOVE_HERE_COLOUR);
							break;

						// Leave areas we can move to in multiple turns looking like normal
						case MOVE_IN_MULTIPLE_TURNS:
							break;
					}
		}

		sceneryPanel.repaint ();

		log.trace ("Exiting setMovementTypes");
	}

	/**
	 * Forces the scenery panel to be redrawn as soon as possible
	 */
	public final void repaintSceneryPanel ()
	{
		sceneryPanel.repaint ();
	}

	/**
	 * @return Frame number being displayed
	 */
	public final int getTerrainAnimFrame ()
	{
		return terrainAnimFrame;
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

	/**
	 * @return Player colour image generator
	 */
	public final PlayerColourImageGenerator getPlayerColourImageGenerator ()
	{
		return playerColourImageGenerator;
	}

	/**
	 * @param gen Player colour image generator
	 */
	public final void setPlayerColourImageGenerator (final PlayerColourImageGenerator gen)
	{
		playerColourImageGenerator = gen;
	}

	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
	}

	/**
	 * @return Spell book
	 */
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
	}

	/**
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
	}

	/**
	 * @return Advisors UI
	 */
	public final SelectAdvisorUI getSelectAdvisorUI ()
	{
		return selectAdvisorUI;
	}

	/**
	 * @param ui Advisors UI
	 */
	public final void setSelectAdvisorUI (final SelectAdvisorUI ui)
	{
		selectAdvisorUI = ui;
	}

	/**
	 * @return Options UI
	 */
	public final OptionsUI getOptionsUI ()
	{
		return optionsUI;
	}

	/**
	 * @param ui Options UI
	 */
	public final void setOptionsUI (final OptionsUI ui)
	{
		optionsUI = ui;
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
	 * @return Client config, containing various overland map settings
	 */	
	public final MomImeClientConfig getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing various overland map settings
	 */
	public final void setClientConfig (final MomImeClientConfig config)
	{
		clientConfig = config;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}
	
	/**
	 * @return Unit stack that's in the middle of moving from one cell to another
	 */
	public final MoveUnitStackOverlandMessageImpl getUnitStackMoving ()
	{
		return unitStackMoving;
	}

	/**
	 * @param stack Unit stack that's in the middle of moving from one cell to another
	 */
	public final void setUnitStackMoving (final MoveUnitStackOverlandMessageImpl stack)
	{
		unitStackMoving = stack;
	}

	/**
	 * @return Animation to display for a spell being cast
	 */
	public final AnimationEx getOverlandCastAnimation ()
	{
		return overlandCastAnimation;
	}

	/**
	 * @param an Animation to display for a spell being cast
	 */
	public final void setOverlandCastAnimation (final AnimationEx an)
	{
		overlandCastAnimation = an;
	}
	
	/**
	 * @return X coord to display overland cast animation at, in pixels
	 */
	public final int getOverlandCastAnimationX ()
	{
		return overlandCastAnimationX;
	}

	/**
	 * @param x X coord to display overland cast animation at, in pixels
	 */
	public final void setOverlandCastAnimationX (final int x)
	{
		overlandCastAnimationX = x;
	}

	/**
	 * @return Y coord to display overland cast animation at, in pixels
	 */
	public final int getOverlandCastAnimationY ()
	{
		return overlandCastAnimationY;
	}
	
	/**
	 * @param y Y coord to display overland cast animation at, in pixels
	 */
	public final void setOverlandCastAnimationY (final int y)
	{
		overlandCastAnimationY = y;
	}

	/**
	 * @return Frame number to display of overland cast animation
	 */
	public final int getOverlandCastAnimationFrame ()
	{
		return overlandCastAnimationFrame;
	}
	
	/**
	 * @param frame Frame number to display of overland cast animation
	 */
	public final void setOverlandCastAnimationFrame (final int frame)
	{
		overlandCastAnimationFrame = frame;
	}
}
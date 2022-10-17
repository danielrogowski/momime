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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.operations.BooleanMapAreaOperations3D;
import com.ndg.map.areas.storage.MapArea;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.actions.LoggingAction;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.config.WindowID;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.languages.database.Month;
import momime.client.languages.database.Shortcut;
import momime.client.messages.process.MoveUnitStackOverlandMessageImpl;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.PlayerColourImageGenerator;
import momime.client.ui.dialogs.ArmyListUI;
import momime.client.ui.dialogs.ChooseCitySpellEffectUI;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.UnitRowDisplayUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.OverlandMapRightHandPanelTop;
import momime.common.MomException;
import momime.common.ai.ZoneAI;
import momime.common.database.AnimationEx;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.OverlandMapSize;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.TileSetEx;
import momime.common.database.UnitEx;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.PendingMovement;
import momime.common.messages.PendingMovementStep;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.movement.OverlandMovementCell;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitVisibilityUtils;

/**
 * Screen for displaying the overland map, including the buttons and side panels and so on that appear in the same frame
 */
public final class OverlandMapUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (OverlandMapUI.class);

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
	
	/** Cities list */
	private CitiesListUI citiesListUI;

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

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods dealing with checking whether we can see units or not */
	private UnitVisibilityUtils unitVisibilityUtils;
	
	/** Zone AI */
	private ZoneAI zoneAI;

	/** UI for displaying damage calculations */
	private DamageCalculationsUI damageCalculationsUI;
	
	/** Operations for 3D boolean map areas */
	private BooleanMapAreaOperations3D booleanMapAreaOperations3D;

	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Spell Ward popup */
	private ChooseCitySpellEffectUI chooseCitySpellEffectUI;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Unit stack that's in the middle of moving from one cell to another */
	private MoveUnitStackOverlandMessageImpl unitStackMoving;

	/** Bitmaps for each animation frame of the overland map */
	private BufferedImage [] overlandMapBitmaps;
	
	/** Bitmap for the shading at the edges of the area we can see */
	private BufferedImage fogOfWarBitmap;

	/** Area detailing which map cells we can/can't move to */
	private OverlandMovementCell [] [] [] moves;

	/** The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack */
	private MapCoordinates3DEx unitMoveFrom;
	
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
	
	/** Plane to display overland cast animation at; null means both (its cast at a tower) */
	private Integer overlandCastAnimationPlane;
	
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

	/** Turn label */
	private JLabel turnLabel;
	
	/** Content pane */
	private JPanel contentPane;
	
	/**
	 * Sets up the frame once all values have been injected
	 * 
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
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

		final BufferedImage calculatorButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/calculatorNormal.png");
		final BufferedImage calculatorButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/overland/topBar/calculatorPressed.png");
		
		// Actions
		spellsAction = new LoggingAction ((ev) -> getSpellBookUI ().setVisible (true));
		citiesAction = new LoggingAction ((ev) -> getCitiesListUI ().setVisible (true));
		magicAction = new LoggingAction ((ev) -> getMagicSlidersUI ().setVisible (true));
		planeAction = new LoggingAction ((ev) -> switchMapViewPlane ());
		messagesAction = new LoggingAction ((ev) -> getNewTurnMessagesUI ().setVisible (true));
		chatAction = new LoggingAction ((ev) -> {});

		gameAction = new LoggingAction ((ev) ->
		{
			final long otherHumanPlayersCount = getClient ().getPlayers ().stream ().filter
				(p -> (!p.getPlayerDescription ().getPlayerID ().equals (getClient ().getOurPlayerID ())) && (p.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)).count ();
			
			final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
			msg.setLanguageTitle (getLanguages ().getOverlandMapScreen ().getLeaveSessionTitle ());
			msg.setLeaveSession (true);
			
			switch ((int) otherHumanPlayersCount)
			{
				case 0:
					msg.setLanguageText (getLanguages ().getOverlandMapScreen ().getLeaveSessionNoHumanPlayers ());
					break;
					
				case 1:
					msg.setLanguageText (getLanguages ().getOverlandMapScreen ().getLeaveSessionOneHumanPlayer ());
					break;
					
				default:
					msg.setText (getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getLeaveSessionManyHumanPlayers ()).replaceAll
						("PLAYER_COUNT", Long.valueOf (otherHumanPlayersCount).toString ()));
			}
			
			msg.setVisible (true);
		});
		
		armiesAction = new LoggingAction ((ev) ->
		{
			final ArmyListUI armyListUI = getPrototypeFrameCreator ().createArmyList ();
			armyListUI.setVisible (true);
		});
		
		final Action infoAction = new LoggingAction ((ev) -> getSelectAdvisorUI ().setVisible (true));
		final Action optionsAction = new LoggingAction ((ev) -> getOptionsUI ().setVisible (true));
		
		final Action centreOnSelectedUnitAction = new LoggingAction ((ev) ->
		{
			final MapCoordinates3DEx coords = getUnitMoveFrom ();
			if (coords != null)
				scrollTo (coords.getX (), coords.getY (), coords.getZ (), true);
		});
		
		// Compacted these up a lot because they're so repetetive
		final Map<SquareMapDirection, Action> moveUnitStackInDirectionActions = new HashMap<SquareMapDirection, Action> ();
		for (final SquareMapDirection d : SquareMapDirection.values ())
			moveUnitStackInDirectionActions.put (d, new LoggingAction ((ev) -> moveUnitStackInDirection (d)));
		
		// Need the tile set in a few places
		overlandMapTileSet = getClient ().getClientDB ().findTileSet (CommonDatabaseConstants.TILE_SET_OVERLAND_MAP, "OverlandMapUI.init");
		
		// Initialize the frame
		getFrame ().setTitle ("Overland Map");
		getFrame ().setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);
		
		// Initialize the content pane
		contentPane = new JPanel ();
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
				
				g.setClip (0, 0, Math.min (sceneryPanel.getWidth (), mapZoomedWidth), Math.min (sceneryPanel.getHeight (), mapZoomedHeight));
				
				// Need to draw it 1-2 times in each direction, depending on wrapping params
				final int xRepeatCount = getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsLeftToRight () ? 2 : 1;
				final int yRepeatCount = getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsTopToBottom () ? 2 : 1;
				
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
				
				// Draw any borders?
				if (getClientConfig ().isOverlandShowOurBorder () || getClientConfig ().isOverlandShowEnemyBorders ())
					try
					{
						final MapArea3D<Integer> zones = getZoneAI ().calculateZones (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
							getClient ().getSessionDescription ().getOverlandMapSize ());

						final MapArea3D<Boolean> friendlyZone = new MapArea3DArrayListImpl<Boolean> ();
						friendlyZone.setCoordinateSystem (getClient ().getSessionDescription ().getOverlandMapSize ());
						
						for (final KnownWizardDetails wizardDetails : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails ()) 
							if ((getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ())) &&
								(((wizardDetails.getPlayerID () == getClient ().getOurPlayerID ()) && (getClientConfig ().isOverlandShowOurBorder ())) ||
									((wizardDetails.getPlayerID () != getClient ().getOurPlayerID ()) && (getClientConfig ().isOverlandShowEnemyBorders ()))))
							{
								final Integer zonePlayerID = wizardDetails.getPlayerID ();
								
								final int borderZoomedWidth = (overlandMapTileSet.getTileWidth () * mapViewZoom) / 10;
								final int borderZoomedHeight = (overlandMapTileSet.getTileHeight () * mapViewZoom) / 10;
		
								// Generate border
								for (int plane = 0; plane < getClient ().getSessionDescription ().getOverlandMapSize ().getDepth (); plane++)
									for (int y = 0; y < getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
										for (int x = 0; x < getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
											friendlyZone.set (x, y, plane, zonePlayerID.equals (zones.get (x, y, plane)));
								
								final MapArea<List<SquareMapDirection>, MapCoordinates3DEx> friendlyZoneBorders = getBooleanMapAreaOperations3D ().traceBorders (friendlyZone);
								
								// Draw border
								for (int x = 0; x < getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
									for (int y = 0; y < getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
									{
										final List<SquareMapDirection> directions = friendlyZoneBorders.get (new MapCoordinates3DEx (x, y, mapViewPlane));
										if (directions != null)
										{
											final int borderX = (x * overlandMapTileSet.getTileWidth () * mapViewZoom) / 10;
											final int borderY = (y * overlandMapTileSet.getTileHeight () * mapViewZoom) / 10;
		
											for (final SquareMapDirection d : directions)
											{
												final BufferedImage borderImage = getPlayerColourImageGenerator ().getFriendlyZoneBorderImage
													(d.getDirectionID (), wizardDetails.getPlayerID ());
		
												for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
													for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
														
														g.drawImage (borderImage,
															(mapZoomedWidth * xRepeat) - mapViewX + borderX, (mapZoomedHeight * yRepeat) - mapViewY + borderY,
															borderZoomedWidth, borderZoomedHeight, null);
											}
										}
									}
							}
					}
					catch (final IOException e)
					{
						log.error ("Error trying to calculate and draw friendly zones");
					}
				
				// Draw units dynamically, over the bitmap.
				
				// For some reason, from the JUnit test for the overland map UI, this code makes the length of time that
				// paintComponent takes to execute behave very erratically, despite the fact that it never has anything to do
				// since the unit test includes no units to draw so unitToDrawAtEachLocation will always be a big area of nulls.
				
				// However this only seems to happen within the unit test and not when when this is running for real, so
				// my only conclusion can be that its Mockito itself that behaves erratically.
				final MemoryUnit [] [] unitToDrawAtEachLocation = chooseUnitToDrawAtEachLocation ();
				if (unitToDrawAtEachLocation != null)
					for (int x = 0; x < getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						for (int y = 0; y < getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						{
							final MemoryUnit unit = unitToDrawAtEachLocation [y] [x];
							if (unit != null)
							{
								final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
									(mapViewPlane).getRow ().get (y).getCell ().get (x);

								// Make the unit that's selected to move blink
								final boolean drawUnit;
								if ((getOverlandMapProcessing ().isAnyUnitSelectedToMove ()) && (getUnitMoveFrom () != null) &&
									(getUnitMoveFrom ().getX () == x) && (getUnitMoveFrom ().getY () == y))
								{
									// The moving stack might be on the other plane
									if ((mapViewPlane == getUnitMoveFrom ().getZ ()) ||
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
										final BufferedImage unitBackground = getPlayerColourImageGenerator ().getModifiedImage
											(GraphicsDatabaseConstants.UNIT_BACKGROUND, true, null, null, null, unit.getOwningPlayerID (), null);
										final UnitEx unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "sceneryPanel.paintComponent");
										final BufferedImage unitImage = getPlayerColourImageGenerator ().getOverlandUnitImage (unitDef, unit.getOwningPlayerID ());
	
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
												
												// Any special order to draw on the unit?
												if (unit.getSpecialOrder () != null)
												{
													final BufferedImage unitSpecialOrderImage = getUtils ().loadImage
														(getGraphicsDB ().findUnitSpecialOrder (unit.getSpecialOrder (), "sceneryPanel.paintComponent").getUnitSpecialOrderImageFile ());

													g.drawImage (unitSpecialOrderImage,
														(mapZoomedWidth * xRepeat) - mapViewX + xpos, (mapZoomedHeight * yRepeat) - mapViewY + ypos,
														unitZoomedWidth, unitZoomedHeight, null);
												}
											}
									}
									catch (final IOException e)
									{
										log.error ("Error trying to load graphics to draw Unit URN " + unit.getUnitURN () + " with ID " + unit.getUnitID (), e);
									}
							}
						}

				// Draw the unit stack that's halfway between two cells during movement
				if ((getUnitStackMoving () != null) && (getUnitStackMoving ().getAnimationPlane () == mapViewPlane))
				{
					final MemoryUnit unit = getUnitStackMoving ().getUnitToDraw ();
					if (unit != null)
						try
						{
							final BufferedImage unitBackground = getPlayerColourImageGenerator ().getModifiedImage
								(GraphicsDatabaseConstants.UNIT_BACKGROUND, true, null, null, null, unit.getOwningPlayerID (), null);
							final UnitEx unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "sceneryPanel.paintComponent");
							final BufferedImage unitImage = getPlayerColourImageGenerator ().getOverlandUnitImage (unitDef, unit.getOwningPlayerID ());
	
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
				if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getPendingMovement ().size () > 0)
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
						
						for (final PendingMovement pendingMovement : getClient ().getOurPersistentPlayerPrivateKnowledge ().getPendingMovement ())

							// Draw each step of the movement path where direction is filled in; note the steps may cross both planes so we have to check all of it
							for (final PendingMovementStep step : pendingMovement.getPath ())
								if ((step.getDirection () != null) && (Math.max (step.getMoveFrom ().getZ (), step.getMoveTo ().getZ ()) == mapViewPlane))
								{
									final int fromArrowX = (step.getMoveFrom ().getX () * overlandMapTileSet.getTileWidth () * mapViewZoom) / 10;
									final int fromArrowY = (step.getMoveFrom ().getY () * overlandMapTileSet.getTileHeight () * mapViewZoom) / 10;

									final int toArrowX = (step.getMoveTo ().getX () * overlandMapTileSet.getTileWidth () * mapViewZoom) / 10;
									final int toArrowY = (step.getMoveTo ().getY () * overlandMapTileSet.getTileHeight () * mapViewZoom) / 10;

									final int bootX = (((step.getMoveTo ().getX () * overlandMapTileSet.getTileWidth ()) - ((bootImage.getWidth () - overlandMapTileSet.getTileWidth ()) / 2)) * mapViewZoom) / 10;
									final int bootY = (((step.getMoveTo ().getY () * overlandMapTileSet.getTileHeight ()) - ((bootImage.getHeight () - overlandMapTileSet.getTileHeight ()) / 2)) * mapViewZoom) / 10;
	
									for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
										for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
										{
											// Draw arrow moving out from the "from" tile
											final BufferedImage fromArrowImage = getUtils ().loadImage ("/momime.client.graphics/overland/pendingMovement/moveOutOfMapCell-d" + step.getDirection () + ".png");
											g.drawImage (fromArrowImage,
												(mapZoomedWidth * xRepeat) - mapViewX + fromArrowX, (mapZoomedHeight * yRepeat) - mapViewY + fromArrowY,
												arrowZoomedWidth, arrowZoomedHeight, null);

											// Draw arrow moving into the "to" tile
											final BufferedImage toArrowImage = getUtils ().loadImage ("/momime.client.graphics/overland/pendingMovement/moveInToMapCell-d" + step.getDirection () + ".png");
											g.drawImage (toArrowImage,
												(mapZoomedWidth * xRepeat) - mapViewX + toArrowX, (mapZoomedHeight * yRepeat) - mapViewY + toArrowY,
												arrowZoomedWidth, arrowZoomedHeight, null);
											
											// Draw boot in centre of the "to" tile
											g.drawImage (bootImage,
												(mapZoomedWidth * xRepeat) - mapViewX + bootX, (mapZoomedHeight * yRepeat) - mapViewY + bootY,
												bootZoomedWidth, bootZoomedHeight, null);
										}
								}
					}
					catch (final IOException e)
					{
						log.error (e, e);
					}

				// Draw casting animation?
				if ((getOverlandCastAnimation () != null) &&
					((getOverlandCastAnimationPlane () == null) || (getOverlandCastAnimationPlane () == getMapViewPlane ())))
					
					try
					{
						final BufferedImage castImage = getUtils ().loadImage (getOverlandCastAnimation ().getFrame ().get (getOverlandCastAnimationFrame ()).getImageFile ());

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
					
					if (getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsLeftToRight ())
						g.drawLine (mapZoomedWidth - mapViewX, 0, mapZoomedWidth - mapViewX, mapZoomedHeight);

					if (getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsTopToBottom ())
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
		new Timer ((int) (1000 / overlandMapTileSet.getAnimationSpeed ()), (ev) ->
		{
			final int newFrame = terrainAnimFrame + 1;
			terrainAnimFrame = (newFrame >= overlandMapTileSet.getAnimationFrameCount ()) ? 0 : newFrame;
			sceneryPanel.repaint ();
			
			// The mini maps on all the city views run from the same timer
			for (final CityViewUI cityView : getClient ().getCityViews ().values ())
				cityView.repaintCityViewMiniMap ();
		}).start ();

		// Zoom actions (need the sceneryPanel, hence why defined down here)
		final Action zoomInAction = new LoggingAction ((ev) ->
		{
			if (mapViewZoom < 20)
			{
				// Make the zoom take effect from the centrepoint of the map, not the top-left corner
				double mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
				final double scaledX = (mapViewX + (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2d)) / mapZoomedWidth;

				double mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
				final double scaledY = (mapViewY + (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2d)) / mapZoomedHeight;
				
				mapViewZoom++;
				
				mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
				final int newMapViewX = (int) ((scaledX * mapZoomedWidth) - (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2));

				mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
				final int newMapViewY = (int) ((scaledY * mapZoomedHeight) - (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2));
				
				mapViewX = fixMapViewLimits (newMapViewX, (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10,
					sceneryPanel.getWidth (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsLeftToRight ());

				mapViewY = fixMapViewLimits (newMapViewY, (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10,
					sceneryPanel.getHeight (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsTopToBottom ());
				
				sceneryPanel.repaint ();
			}
		});

		final Action zoomOutAction = new LoggingAction ((ev) ->
		{
			if (mapViewZoom > 10)
			{
				// Make the zoom take effect from the centrepoint of the map, not the top-left corner
				double mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
				final double scaledX = (mapViewX + (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2d)) / mapZoomedWidth;

				double mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
				final double scaledY = (mapViewY + (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2d)) / mapZoomedHeight;
				
				mapViewZoom--;
				
				mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
				final int newMapViewX = (int) ((scaledX * mapZoomedWidth) - (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2));

				mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
				final int newMapViewY = (int) ((scaledY * mapZoomedHeight) - (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2));
				
				mapViewX = fixMapViewLimits (newMapViewX, (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10,
					sceneryPanel.getWidth (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsLeftToRight ());

				mapViewY = fixMapViewLimits (newMapViewY, (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10,
					sceneryPanel.getHeight (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsTopToBottom ());
				
				sceneryPanel.repaint ();
			}
		});

		final Action toggleDamageCalculationsAction = new LoggingAction
			((ev) -> getDamageCalculationsUI ().setVisible (!getDamageCalculationsUI ().isVisible ()));
		
		// Set up the row of gold buttons along the top
		mapButtonBar.setLayout (new GridBagLayout ());
		mapButtonBar.add (Box.createRigidArea (new Dimension (7, 0)), getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (gameAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		mapButtonBar.add (getUtils ().createImageButton (spellsAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (2, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (armiesAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (3, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (citiesAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (4, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (magicAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (5, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (planeAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (6, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (messagesAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (7, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (chatAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarGoldButtonNormal, topBarGoldButtonPressed, topBarGoldButtonNormal), getUtils ().createConstraintsNoFill (8, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		mapButtonBar.add (getUtils ().createImageButton (infoAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarInfoNormal, topBarInfoPressed, topBarInfoNormal), getUtils ().createConstraintsNoFill (9, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		mapButtonBar.add (getUtils ().createImageButton (toggleDamageCalculationsAction, null, null, null,
			calculatorButtonNormal, calculatorButtonPressed, calculatorButtonNormal), getUtils ().createConstraintsNoFill (10, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		mapButtonBar.add (getUtils ().createImageButton (zoomInAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarZoomInNormal, topBarZoomInPressed, topBarZoomInNormal), getUtils ().createConstraintsNoFill (11, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (zoomOutAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarZoomOutNormal, topBarZoomOutPressed, topBarZoomOutNormal), getUtils ().createConstraintsNoFill (12, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
			
		mapButtonBar.add (getUtils ().createImageButton (optionsAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			topBarOptionsNormal, topBarOptionsPressed, topBarOptionsNormal), getUtils ().createConstraintsNoFill (13, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final GridBagConstraints turnLabelConstraints = getUtils ().createConstraintsNoFill (14, 0, 1, 1, INSET, GridBagConstraintsNoFill.EAST);
		turnLabelConstraints.weightx = 1;		// Right justify the label
		turnLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		mapButtonBar.add (turnLabel, turnLabelConstraints);

		mapButtonBar.add (Box.createRigidArea (new Dimension (7, 0)), getUtils ().createConstraintsNoFill (15, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Stop frame being shrunk smaller than this
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setMinimumSize (getFrame ().getSize ());
		setWindowID (WindowID.OVERLAND_MAP);
		setPersistVisibility (false);
		
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
						if (SwingUtilities.isRightMouseButton (ev))
						{
							final OverlandMapCityData cityData = mc.getCityData ();
							if (cityData != null)
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
								final int towerPlane = getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ()) ? 0 : mapViewPlane;
								if (!getOverlandMapProcessing ().showSelectUnitBoxes (new MapCoordinates3DEx (mapCellX, mapCellY, towerPlane)))
									scrollTo (mapCellX, mapCellY, towerPlane, true);
							}
						}
						
						// Left clicking to target an overland spell
						else if (getOverlandMapRightHandPanel ().getTop () == OverlandMapRightHandPanelTop.TARGET_SPELL)
						{
							final Spell spell = getClient ().getClientDB ().findSpell (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID (), "OverlandMapUI");
							final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
							
							// Spells aimed at cities
							if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) ||
								(spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES) || (kind == KindOfSpell.ATTACK_UNITS_AND_BUILDINGS))
							{
								// If there isn't even a city here then don't even display a message
								if (mc.getCityData () != null)
								{
									// Use common routine to do all the validation
									final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell
										(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), spell,
											getClient ().getOurPlayerID (), mapLocation, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
											getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (),
											getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (),
											getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getClient ().getClientDB ());
									
									if (validTarget == TargetSpellResult.VALID_TARGET)
									{
										// Need to ask the player for which city spell effect they want?  (Spell Ward)
										boolean choiceRequired = false;
										if (spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS)
										{
											final List<String> citySpellEffectIDs = getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
												(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), spell,
													getClient ().getOurPlayerID (), mapLocation);
											if ((citySpellEffectIDs != null) && (citySpellEffectIDs.size () > 1))
											{
												choiceRequired = true;
												getChooseCitySpellEffectUI ().setSpellID (spell.getSpellID ());
												getChooseCitySpellEffectUI ().setCityLocation (mapLocation);
												getChooseCitySpellEffectUI ().setCitySpellEffectChoices (citySpellEffectIDs);
												getChooseCitySpellEffectUI ().setVisible (true);
											}
										}
										
										if (!choiceRequired)
										{
											final TargetSpellMessage msg = new TargetSpellMessage ();
											msg.setSpellID (spell.getSpellID ());
											msg.setOverlandTargetLocation (mapLocation);
											getClient ().getServerConnection ().sendMessageToServer (msg);
											
											// Close out the "Target Spell" right hand panel
											getOverlandMapProcessing ().updateMovementRemaining ();
										}
									}
									else
									{
										final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
										
										final String spellRealm;
										if (spell.getSpellRealm () == null)
											spellRealm = "";
										else
											spellRealm = getLanguageHolder ().findDescription (getClient ().getClientDB ().findPick (spell.getSpellRealm (), "OverlandMapUI").getBookshelfDescription ());
										
										final String buildingName;
										if (spell.getBuildingID () == null)
											buildingName = "";
										else
											buildingName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findBuilding (spell.getBuildingID (), "OverlandMapUI").getBuildingName ());
										
										final String text = getLanguageHolder ().findDescription (getLanguages ().getSpellTargeting ().getCityLanguageText (validTarget)).replaceAll
											("SPELL_NAME", spellName).replaceAll ("BUILDING_NAME", buildingName).replaceAll ("SPELL_REALM", spellRealm);
										
										final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
										msg.setLanguageTitle (getLanguages ().getSpellTargeting ().getTitle ());
										msg.setText (text);
										msg.setVisible (true);												
									}
								}
							}
							
							// Spells aimed at units
							else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
								(kind == KindOfSpell.ATTACK_UNITS) || (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES))
							{
								// Find units at this map location, and also check if they're valid targets
								final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
								final List<ExpandedUnitDetails> validUnits = new ArrayList<ExpandedUnitDetails> ();
								final Set<TargetSpellResult> invalidReasons = new HashSet<TargetSpellResult> ();
								
								for (final MemoryUnit mu : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
									if ((mapLocation.equals (mu.getUnitLocation ())) && (mu.getStatus () == UnitStatusID.ALIVE))
									{
										final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (mu, null, null, null,
											getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
										
										final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, null, null, null,
											getClient ().getOurPlayerID (), null, null, xu, true, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
											getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (), getClient ().getPlayers (), getClient ().getClientDB ());
										
										if (validTarget == TargetSpellResult.VALID_TARGET)
										{
											units.add (mu);
											validUnits.add (xu);
										}
										
										// If its invisible, don't even add it to the units list
										else if (validTarget != TargetSpellResult.INVISIBLE)
										{
											units.add (mu);
											invalidReasons.add (validTarget);
										}
									}
								
								if (units.size () > 0)
								{
									if ((spell.getAttackSpellOverlandTarget () != null) && (spell.getAttackSpellOverlandTarget () == AttackSpellTargetID.ALL_UNITS))
									{
										if (validUnits.size () > 0)
										{
											// Aim spell at all units in this location
											final TargetSpellMessage msg = new TargetSpellMessage ();
											msg.setSpellID (spell.getSpellID ());
											msg.setOverlandTargetLocation (mapLocation);
											getClient ().getServerConnection ().sendMessageToServer (msg);
											
											// Close out the "Target Spell" right hand panel
											getOverlandMapProcessing ().updateMovementRemaining ();
										}
										else
										{
											// There's units here, but none are suitable targets so reject it
											final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
											final StringBuilder text = new StringBuilder ();
											for (final TargetSpellResult invalidReason : invalidReasons)
											{
												if (text.length () > 0)
													text.append (System.lineSeparator ());
												
												text.append (getLanguageHolder ().findDescription (getLanguages ().getSpellTargeting ().getUnitLanguageText (invalidReason)).replaceAll
													("SPELL_NAME", spellName));
											}
											
											final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
											msg.setLanguageTitle (getLanguages ().getSpellTargeting ().getTitle ());
											msg.setText (text.toString ());
											msg.setVisible (true);												
										}
									}
									else
									{
										// Pick a specific unit
										final UnitRowDisplayUI unitRowDisplay = getPrototypeFrameCreator ().createUnitRowDisplay ();
										unitRowDisplay.setUnits (units);
										unitRowDisplay.setTargetSpellID (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID ());
										unitRowDisplay.setVisible (true);
									}
								}
							}
							
							// Spells aimed at a location
							else if ((spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS) ||
								(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING))
							{
								// Use common routine to do all the validation
								final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isOverlandLocationValidTargetForSpell (spell, getClient ().getOurPlayerID (),
									mapLocation, getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
									getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (), getClient ().getPlayers (), getClient ().getClientDB ());
								
								if (validTarget == TargetSpellResult.VALID_TARGET)
								{
									final TargetSpellMessage msg = new TargetSpellMessage ();
									msg.setSpellID (spell.getSpellID ());
									msg.setOverlandTargetLocation (mapLocation);
									getClient ().getServerConnection ().sendMessageToServer (msg);
									
									// Close out the "Target Spell" right hand panel
									getOverlandMapProcessing ().updateMovementRemaining ();
								}
								else
								{
									final String spellName = getLanguageHolder ().findDescription (spell.getSpellName ());
									
									final String spellRealm;
									if (spell.getSpellRealm () == null)
										spellRealm = "";
									else
										spellRealm = getLanguageHolder ().findDescription (getClient ().getClientDB ().findPick (spell.getSpellRealm (), "OverlandMapUI").getBookshelfDescription ());
									
									final String text = getLanguageHolder ().findDescription (getLanguages ().getSpellTargeting ().getLocationLanguageText (validTarget)).replaceAll
										("SPELL_NAME", spellName).replaceAll ("SPELL_REALM", spellRealm);
									
									final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
									msg.setLanguageTitle (getLanguages ().getSpellTargeting ().getTitle ());
									msg.setText (text);
									msg.setVisible (true);												
								}
							}
							else
								throw new MomException ("Clicking on the overland map to target a spell, but don't know what to do with spells from section " + spell.getSpellBookSectionID ());
						}
						
						// Left clicking on a space to move a stack of units to - can only do this if its our turn
						// Conditions here are duplicated in moveUnitStackInDirection
						else if ((getOverlandMapRightHandPanel ().getTop () != OverlandMapRightHandPanelTop.SURVEYOR) &&
							((getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) ||
							(getClient ().getOurPlayerID ().equals (getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ()))) &&
							(getMoves () != null) && (getMoves () [mapViewPlane] [mapCellY] [mapCellX] != null))
							
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
		new Timer (20, (ev) ->
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
						sceneryPanel.getWidth (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsLeftToRight ());

					newMapViewY = fixMapViewLimits (newMapViewY, (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10,
						sceneryPanel.getHeight (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsTopToBottom ());
				
					if ((newMapViewX != mapViewX) || (newMapViewY != mapViewY))
					{
						mapViewX = newMapViewX;
						mapViewY = newMapViewY;
						sceneryPanel.repaint ();
					}
				}
			}
		}).start ();
		
		// Shortcut keys
		// 'Build city or road' isn't really right, maybe once you can actually build roads I'll make B=built city, R=build road
		// since you could have both a settler and engineer in the same stack and hit 'B' and it can't be ambigious which happens.
		contentPane.getActionMap ().put (Shortcut.ARMIES_SCREEN,										armiesAction);
		contentPane.getActionMap ().put (Shortcut.GAME_SCREEN,											planeAction);
		contentPane.getActionMap ().put (Shortcut.MAGIC_SCREEN,											magicAction);
		contentPane.getActionMap ().put (Shortcut.SWITCH_PLANE,											planeAction);
		contentPane.getActionMap ().put (Shortcut.SPELLBOOK,												spellsAction);
		contentPane.getActionMap ().put (Shortcut.ZOOM_IN,													zoomInAction);
		contentPane.getActionMap ().put (Shortcut.ZOOM_OUT,												zoomOutAction);
		contentPane.getActionMap ().put (Shortcut.CENTRE_OVERLAND_ON_SELECTED_UNIT,	centreOnSelectedUnitAction);
		contentPane.getActionMap ().put (Shortcut.NEXT_TURN,												getOverlandMapRightHandPanel ().getNextTurnAction ());
		contentPane.getActionMap ().put (Shortcut.OVERLAND_MOVE_DONE,							getOverlandMapRightHandPanel ().getDoneAction ());
		contentPane.getActionMap ().put (Shortcut.OVERLAND_MOVE_WAIT,							getOverlandMapRightHandPanel ().getWaitAction ());
		contentPane.getActionMap ().put (Shortcut.BUILD_ROAD_OR_CITY,								getOverlandMapRightHandPanel ().getCreateOutpostAction ());
		contentPane.getActionMap ().put (Shortcut.ADVISOR_SCREEN,										infoAction);

		for (final Entry<SquareMapDirection, Action> d : moveUnitStackInDirectionActions.entrySet ())
			contentPane.getActionMap ().put (Shortcut.fromValue ("MOVE_OVERLAND_" + d.getKey ().name ()), d.getValue ());
		
		// Plus we need to hook all the F-keys from the select advisor screen.
		// To do so, we have to force the advisors screen to initialize itself to ensure these are all created.
		// The way I've done this is a bit sneaky - isVisible is null-safe, and setVisible inits the form even if you pass in "false".
		if (getSelectAdvisorUI () != null)		// Just so TestOverlandMapUI doesn't require the SelectAdvisorUI
		{
			if (!getSelectAdvisorUI ().isVisible ())
				getSelectAdvisorUI ().setVisible (false);
		
			for (final Object shortcut : getSelectAdvisorUI ().getActionMap ().keys ())
				if (shortcut instanceof Shortcut)
					contentPane.getActionMap ().put (shortcut, getSelectAdvisorUI ().getActionMap ().get (shortcut));
		}
	}

	/**
	 * Update all labels and such from the chosen language
	 */
	@Override
	public final void languageChanged ()
	{
		gameAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getGame ()));
		spellsAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getSpells ()));
		armiesAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getArmies ()));
		citiesAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getCities ()));
		magicAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getMagic ()));
		planeAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getPlane ()));
		messagesAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getNewTurnMessages ()));
		chatAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getChat ()));

		// Shortcut keys
		getLanguageHolder ().configureShortcutKeys (contentPane);
	}
	
	/**
	 * @param d Direction for which arrow key was pressed
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	private final void moveUnitStackInDirection (final SquareMapDirection d) throws JAXBException, XMLStreamException, MomException
	{
		if ((getOverlandMapRightHandPanel ().getTop () != OverlandMapRightHandPanelTop.SURVEYOR) &&
			((getClient ().getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) ||
			(getClient ().getOurPlayerID ().equals (getClient ().getGeneralPublicKnowledge ().getCurrentPlayerID ()))) &&
			(getMoves () != null) && (getUnitMoveFrom () != null))
		{
			// So its our turn, and have a current position, now have to check the direction pressed is a valid move
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (getUnitMoveFrom ());
			if (getCoordinateSystemUtils ().move3DCoordinates (getClient ().getSessionDescription ().getOverlandMapSize (), coords, d.getDirectionID ()))
				if (getMoves () [coords.getZ ()] [coords.getY ()] [coords.getX ()] != null)
					getOverlandMapProcessing ().moveUnitStackTo (coords);
		}
	}
	
	/**
	 * Converts from pixel coordinates back to overland map coordinates
	 * 
	 * @param ev Mouse click event
	 * @return Overland map coordinates, or null if the mouse coordinates are off the map
	 */
	private final MapCoordinates2DEx convertMouseCoordsToMapGridCell (final MouseEvent ev)
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
			
			final OverlandMapSize overlandMapSize = getClient ().getSessionDescription ().getOverlandMapSize ();
			
			while (mapCellX < 0) mapCellX = mapCellX + overlandMapSize.getWidth ();
			while (mapCellX >= overlandMapSize.getWidth ()) mapCellX = mapCellX - overlandMapSize.getWidth (); 
			while (mapCellY < 0) mapCellY = mapCellY + overlandMapSize.getHeight ();
			while (mapCellY >= overlandMapSize.getHeight ()) mapCellY = mapCellY - overlandMapSize.getHeight ();
			
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
		overlandMapBitmaps = getOverlandMapBitmapGenerator ().generateOverlandMapBitmaps (mapViewPlane,
			0, 0, getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (), getClient ().getSessionDescription ().getOverlandMapSize ().getHeight ());
		
		// Tell all the city screens to do the same.
		// A bit weird putting this in the map UI, it should go on the messages, but at least this way the messages then only have to call 1 method so I don't
		// have to duplicate this city loop in 5+ places and forget to do it somewhere.
		for (final CityViewUI cityView : getClient ().getCityViews ().values ())
			cityView.regenerateCityViewMiniMapBitmaps ();
	}
	
	/**
	 * Generates big bitmap of the smoothed edges of blackness that obscure the edges of the outermost tiles we can see, so that the edges aren't just a solid black line
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 */
	public final void regenerateFogOfWarBitmap () throws IOException
	{
		fogOfWarBitmap = getOverlandMapBitmapGenerator ().generateFogOfWarBitmap (mapViewPlane,
			0, 0, getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (), getClient ().getSessionDescription ().getOverlandMapSize ().getHeight ());

		// Tell all the city screens to do the same
		for (final CityViewUI cityView : getClient ().getCityViews ().values ())
			cityView.regenerateCityViewMiniMapFogOfWar ();
	}

	/**
	 * This prefers to units with the highest special order - this will make settlers always show on top if they're building cities, and make non-patrolling units appear in preference to patrolling units
	 * 
	 * @return Array of units to draw at each map cell; or null if there are absolutely no units to draw
	 */
	final MemoryUnit [][] chooseUnitToDrawAtEachLocation ()
	{
		final MemoryUnit [][] bestUnits = new MemoryUnit [getClient ().getSessionDescription ().getOverlandMapSize ().getHeight ()] [getClient ().getSessionDescription ().getOverlandMapSize ().getWidth ()];

		boolean found = false;
		for (final MemoryUnit unit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())

			// Is it alive, and are we looking at the right plane to see it?
			if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getUnitLocation () != null) && ((unit.getUnitLocation ().getZ () == mapViewPlane) ||
				(getMemoryGridCellUtils ().isTerrainTowerOfWizardry (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(unit.getUnitLocation ().getZ ()).getRow ().get (unit.getUnitLocation ().getY ()).getCell ().get (unit.getUnitLocation ().getX ()).getTerrainData ()))) &&
				(getUnitVisibilityUtils ().canSeeUnitOverland (unit, getClient ().getOurPlayerID (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), getClient ().getClientDB ())))
			{
				// If this is a map cell where we've got units selected to move, make sure we show one of the units that's moving
				final boolean drawUnit;
				if ((getUnitMoveFrom () != null) && (getOverlandMapProcessing ().isAnyUnitSelectedToMove ()) &&
					(getUnitMoveFrom ().equals (unit.getUnitLocation ())))
					
					drawUnit = getOverlandMapProcessing ().isUnitSelected (unit);
				else
					drawUnit = true;
				
				if (drawUnit)
				{
					found = true;
					final MemoryUnit existingUnit = bestUnits [unit.getUnitLocation ().getY ()] [unit.getUnitLocation ().getX ()];
	
					// Show real special orders in preference to patrol
					if ((existingUnit == null) ||
						((existingUnit != null) && (existingUnit.getSpecialOrder () == null) && (unit.getSpecialOrder () != null)) ||
						((existingUnit != null) && (existingUnit.getSpecialOrder () == UnitSpecialOrder.PATROL) && (unit.getSpecialOrder () != null) && (unit.getSpecialOrder () != UnitSpecialOrder.PATROL)))
	
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
		// Turn 1 is January 1400 - so last turn in 1400 is turn 12
		// Months are numbered 1-12
		final int year = 1400 + ((getClient ().getGeneralPublicKnowledge ().getTurnNumber () - 1) / 12);
		int month = getClient ().getGeneralPublicKnowledge ().getTurnNumber () % 12;
		if (month == 0)
			month = 12;

		// Build up description
		final int monthNumber = month;
		final Optional<Month> monthLang = getLanguages ().getMonth ().stream ().filter (m -> m.getMonthNumber () == monthNumber).findAny ();
		final String monthText = monthLang.isEmpty () ? Integer.valueOf (month).toString () :
			getLanguageHolder ().findDescription (monthLang.get ().getName ());
		
		turnLabel.setText (getLanguageHolder ().findDescription (getLanguages ().getOverlandMapScreen ().getMapButtonBar ().getTurn ()).replaceAll
			("MONTH", monthText).replaceAll ("YEAR", Integer.valueOf (year).toString ()).replaceAll
			("TURN", Integer.valueOf (getClient ().getGeneralPublicKnowledge ().getTurnNumber ()).toString ()));
	}

	/**
	 * Creates highlighting bitmap showing map cells we can/can't move to
	 */
	public final void regenerateMovementTypesBitmap ()
	{
		// Regenerate shading bitmap
		if (getMoves () == null)
			movementTypesBitmap = null;
		else
		{
			movementTypesBitmap = new BufferedImage (getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (), getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (), BufferedImage.TYPE_INT_ARGB);

			for (int x = 0; x < getClient ().getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				for (int y = 0; y < getClient ().getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				{
					final OverlandMovementCell move = moves [mapViewPlane] [y] [x];
					
					// Darken areas we cannot move to at all
					if (move == null)
						movementTypesBitmap.setRGB (x, y, CANNOT_MOVE_HERE_COLOUR);
			
					// Brighten areas we can move to in 1 turn
					else if (move.isMoveToInOneTurn ())
						movementTypesBitmap.setRGB (x, y, MOVE_IN_ONE_TURN_COLOUR);

					// Leave areas we can move to in multiple turns looking like normal
				}
		}

		sceneryPanel.repaint ();
	}
	
	/**
	 * @return The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack
	 */
	public final MapCoordinates3DEx getUnitMoveFrom ()
	{
		return unitMoveFrom;
	}

	/**
	 * @param u The map location we're currently selecting/deselecting units at ready to choose an order for them or tell them where to move/attack
	 */
	public final void setUnitMoveFrom (final MapCoordinates3DEx u)
	{
		unitMoveFrom = u;
	}
	
	/**
	 * Switches the plane we're viewing when the player clicks the button, or scrollTo below changes it
	 * @throws IOException If there is a problem loading any of the images
	 */
	private final void switchMapViewPlane () throws IOException
	{
		mapViewPlane = 1 - mapViewPlane;
		
		regenerateOverlandMapBitmaps ();
		regenerateFogOfWarBitmap ();
		getOverlandMapRightHandPanel ().regenerateMiniMapBitmap ();

		// Keep the same movement types array, but regenerate the bitmap from it to show movement available on the new plane
		regenerateMovementTypesBitmap ();
	}
	
	/**
	 * Ensures that the specified location is visible
	 * 
	 * @param x X coordinate to show, in map coords
	 * @param y Y coordinate to show, in map coords
	 * @param plane Plane to show, in map coords
	 * @param force If true, will forcibly recentre the map on x, y regardless of whether x, y is already visible
	 */
	public final void scrollTo (final int x, final int y, final int plane, final boolean force)
	{
		try
		{
			// Switch plane if necessary
			if (plane != mapViewPlane)
				switchMapViewPlane ();

			// Work out centre pixel of the tile
			final double mapZoomedWidth = (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10;
			final double mapZoomedHeight = (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10;
			
			final double scaledX = (x + 0.5d) / getClient ().getSessionDescription ().getOverlandMapSize ().getWidth ();
			final double scaledY = (y + 0.5d) / getClient ().getSessionDescription ().getOverlandMapSize ().getHeight ();
			
			final double centreX = scaledX * mapZoomedWidth;
			final double centreY = scaledY * mapZoomedHeight;
			
			// Are the required coords already visible?
			boolean visible = false;
			if (!force)
			{
				// Check the map in all 4 positions to see if it covers that pixel
				final int xRepeatCount = getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsLeftToRight () ? 2 : 1;
				final int yRepeatCount = getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsTopToBottom () ? 2 : 1;
				
				for (int xRepeat = 0; xRepeat < xRepeatCount; xRepeat++)
					for (int yRepeat = 0; yRepeat < yRepeatCount; yRepeat++)
					{
						final double drawX = mapViewX - (mapZoomedWidth * xRepeat);
						final double drawY = mapViewY - (mapZoomedHeight * yRepeat);
						
						if ((centreX >= drawX) && (centreY >= drawY) &&
							(centreX < drawX + Math.min (sceneryPanel.getWidth (), mapZoomedWidth)) &&
							(centreY < drawY + Math.min (sceneryPanel.getHeight (), mapZoomedHeight)))
							visible = true;
					}
			}
			
			if (!visible)
			{
				final int newMapViewX = (int) (centreX - (Math.min (sceneryPanel.getWidth (), mapZoomedWidth) / 2));
				final int newMapViewY = (int) (centreY - (Math.min (sceneryPanel.getHeight (), mapZoomedHeight) / 2));
				
				mapViewX = fixMapViewLimits (newMapViewX, (overlandMapBitmaps [terrainAnimFrame].getWidth () * mapViewZoom) / 10,
					sceneryPanel.getWidth (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsLeftToRight ());
	
				mapViewY = fixMapViewLimits (newMapViewY, (overlandMapBitmaps [terrainAnimFrame].getHeight () * mapViewZoom) / 10,
					sceneryPanel.getHeight (), getClient ().getSessionDescription ().getOverlandMapSize ().isWrapsTopToBottom ());
				
				sceneryPanel.repaint ();
			}
		}
		catch (final IOException e)
		{
			log.error (e, e);
		}
	}
	
	/**
	 * Called when player clicks an Overland Enchantment on the magic sliders screen to target a Disjunction-type spell at
	 * 
	 * @param spellURN Overland Enchantment to target
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public final void targetOverlandSpellURN (final int spellURN)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final Spell spell = getClient ().getClientDB ().findSpell (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID (), "targetOverlandSpellURN");

		final TargetSpellMessage msg = new TargetSpellMessage ();
		msg.setSpellID (spell.getSpellID ());
		msg.setOverlandTargetSpellURN (spellURN);
		getClient ().getServerConnection ().sendMessageToServer (msg);
		
		// Close out the "Target Spell" right hand panel
		getOverlandMapProcessing ().updateMovementRemaining ();
	}

	/**
	 * Called when player clicks a Wizard to target a spell like Spell Blast
	 * 
	 * @param targetPlayerID Wizard to target
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If a unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If the unit whose details we are storing is not a MemoryUnit 
	 */
	public final void targetOverlandPlayerID (final int targetPlayerID)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final Spell spell = getClient ().getClientDB ().findSpell (getOverlandMapRightHandPanel ().getTargetSpell ().getSpellID (), "targetOverlandPlayerID");

		final TargetSpellMessage msg = new TargetSpellMessage ();
		msg.setSpellID (spell.getSpellID ());
		msg.setOverlandTargetPlayerID (targetPlayerID);
		getClient ().getServerConnection ().sendMessageToServer (msg);
		
		// Close out the "Target Spell" right hand panel
		getOverlandMapProcessing ().updateMovementRemaining ();
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
	 * @return The plane that the UI is currently displaying
	 */
	public final int getMapViewPlane ()
	{
		return mapViewPlane;
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
	 * @return Cities list
	 */
	public final CitiesListUI getCitiesListUI ()
	{
		return citiesListUI;
	}

	/**
	 * @param ui Cities list
	 */
	public final void setCitiesListUI (final CitiesListUI ui)
	{
		citiesListUI = ui;
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
	 * @return Methods dealing with checking whether we can see units or not
	 */
	public final UnitVisibilityUtils getUnitVisibilityUtils ()
	{
		return unitVisibilityUtils;
	}

	/**
	 * @param u Methods dealing with checking whether we can see units or not
	 */
	public final void setUnitVisibilityUtils (final UnitVisibilityUtils u)
	{
		unitVisibilityUtils = u;
	}
	
	/**
	 * @return Zone AI
	 */
	public final ZoneAI getZoneAI ()
	{
		return zoneAI;
	}

	/**
	 * @param ai Zone AI
	 */
	public final void setZoneAI (final ZoneAI ai)
	{
		zoneAI = ai;
	}
	
	/**
	 * @return UI for displaying damage calculations
	 */
	public final DamageCalculationsUI getDamageCalculationsUI ()
	{
		return damageCalculationsUI;
	}

	/**
	 * @param ui UI for displaying damage calculations
	 */
	public final void setDamageCalculationsUI (final DamageCalculationsUI ui)
	{
		damageCalculationsUI = ui;
	}
	
	/**
	 * @return Operations for 3D boolean map areas
	 */
	public final BooleanMapAreaOperations3D getBooleanMapAreaOperations3D ()
	{
		return booleanMapAreaOperations3D;
	}

	/**
	 * @param op Operations for 3D boolean map areas
	 */
	public final void setBooleanMapAreaOperations3D (final BooleanMapAreaOperations3D op)
	{
		booleanMapAreaOperations3D = op;
	}

	/**
	 * @return Kind of spell utils
	 */
	public final KindOfSpellUtils getKindOfSpellUtils ()
	{
		return kindOfSpellUtils;
	}

	/**
	 * @param k Kind of spell utils
	 */
	public final void setKindOfSpellUtils (final KindOfSpellUtils k)
	{
		kindOfSpellUtils = k;
	}

	/**
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
	
	/**
	 * @return Spell Ward popup
	 */
	public final ChooseCitySpellEffectUI getChooseCitySpellEffectUI ()
	{
		return chooseCitySpellEffectUI;
	}

	/**
	 * @param w Spell Ward popup
	 */
	public final void setChooseCitySpellEffectUI (final ChooseCitySpellEffectUI w)
	{
		chooseCitySpellEffectUI = w;
	}
	
	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
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
	 * @return Plane to display overland cast animation at; null means both (its cast at a tower)
	 */
	public final Integer getOverlandCastAnimationPlane ()
	{
		return overlandCastAnimationPlane;
	}

	/**
	 * @param p Plane to display overland cast animation at; null means both (its cast at a tower)
	 */
	public final void setOverlandCastAnimationPlane (final Integer p)
	{
		overlandCastAnimationPlane = p;
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

	/**
	 * @return Area detailing which map cells we can/can't move to
	 */
	public final OverlandMovementCell [] [] [] getMoves ()
	{
		return moves;
	}

	/**
	 * @param m Area detailing which map cells we can/can't move to
	 */
	public final void setMoves (final OverlandMovementCell [] [] [] m)
	{
		moves = m;
	}
}
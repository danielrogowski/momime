package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.calculations.MomClientUnitCalculations;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetEx;
import momime.client.graphics.database.v0_9_5.WizardCombatPlayList;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.TileType;
import momime.client.messages.process.ApplyDamageMessageImpl;
import momime.client.messages.process.MoveUnitInCombatMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.utils.AnimationController;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.CombatAutoControlMessage;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.random.RandomUtils;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Combat UI.  Note there's only one of these - I played with the idea of allowing multiple combats going on at once (for simultaneous
 * turns games) but it makes things too complicated - e.g. if you have the spellbook open and click a spell, which combat are you casting it into?
 * So the same Combat UI window is kept and reused, so it retains its position.
 * 
 * The Combat UI isn't modal, you have to be able to use the spell book and other windows.
 */
public final class CombatUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (CombatUI.class);

	/** XML layout */
	private XmlLayoutContainerEx combatLayout;
	
	/** Small font */
	private Font smallFont;
	
	/** Large font */
	private Font largeFont;
	
	/** Combat location */
	private MapCoordinates3DEx combatLocation;
	
	/** Combat terrain */
	private MapAreaOfCombatTiles combatTerrain;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Bitmap generator for the static terrain */
	private CombatMapBitmapGenerator combatMapBitmapGenerator;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Music player */
	private AudioPlayer musicPlayer;
	
	/** Random utils */
	private RandomUtils randomUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Utils for drawing units */
	private UnitClientUtils unitClientUtils;
	
	/** Client unit calculations */
	private MomClientUnitCalculations clientUnitCalculations;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Spell book action */
	private Action spellAction;
	
	/** Wait action */
	private Action waitAction;
	
	/** Done action */
	private Action doneAction;
	
	/** Flee action */
	private Action fleeAction;
	
	/** Auto action */
	private Action autoAction;

	/** Content pane */
	private JPanel contentPane;
	
	/** Attacking and defending players */
	private CombatPlayers players;
	
	/** Name of defending player */
	private JLabel defendingPlayerName;
	
	/** Name of attacking player */
	private JLabel attackingPlayerName;
	
	/** Bitmaps for each animation frame of the combat map */
	private BufferedImage [] combatMapBitmaps;
	
	/** Frame number being displayed */
	private int terrainAnimFrame;
	
	/** Units occupying each cell of the combat map */
	private MemoryUnit [] [] unitToDrawAtEachLocation;
	
	/** Let AI auto control our units? */
	private boolean autoControl;
	
	/** Whose turn it currently is in this combat */
	private Integer currentPlayerID;

	/** Unit that's in the middle of moving from one cell to another */
	private MoveUnitInCombatMessageImpl unitMoving;
	
	/** Attack that's in the middle of taking place */
	private ApplyDamageMessageImpl attackAnim;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/combat/background.png");
		
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button50x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button50x18goldPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button50x18goldDisabled.png");
		
		final BufferedImage calculatorButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/combat/calculatorButtonNormal.png");
		final BufferedImage calculatorButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/combat/calculatorButtonPressed.png");
		
		// We need the tile set to know how big combat tiles are
		final TileSetEx combatMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.VALUE_TILE_SET_COMBAT_MAP, "CombatUI");
		
		// Actions
		spellAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		waitAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		doneAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		fleeAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		autoAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				// If it is currently our turn, then we immediately need to tell the server to have the AI take the rest of our turn
				autoControl = !autoControl;
				
				if ((autoControl) && (getClient ().getOurPlayerID ().equals (currentPlayerID)))
				{
					final CombatAutoControlMessage msg = new CombatAutoControlMessage ();
					msg.setCombatLocation (getCombatLocation ());
					try
					{
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				}
			}
		};		

		final Action toggleDamageCalculationsAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
			}
		};
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Draw the static portion of the terrain
				g.drawImage (combatMapBitmaps [terrainAnimFrame], 0, 0, null);
				
				// Draw units at the top first and work downwards
				for (int y = 0; y < getClient ().getSessionDescription ().getCombatMapSize ().getHeight (); y++)
					for (int x = 0; x < getClient ().getSessionDescription ().getCombatMapSize ().getWidth (); x++)
					{
						final MemoryUnit unit = unitToDrawAtEachLocation [y] [x];
						if (unitToDrawAtEachLocation [y] [x] != null)
							try
							{
								// Is the unit currently animating in an attack?
								String combatActionID = null;
								if (getAttackAnim () != null)
								{
									// Ranged attack animation
									if (getAttackAnim ().isRangedAttack ())
									{
										// Show firing unit going 'pew'
										if ((unit == getAttackAnim ().getAttackerUnit ()) && (getAttackAnim ().getCurrent () == null))
											combatActionID = GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_RANGED_ATTACK;
									}
									
									// Melee attack animation
									else if ((unit == getAttackAnim ().getAttackerUnit ()) || (unit == getAttackAnim ().getDefenderUnit ()))
										combatActionID = GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK;
								}
								
								// If animation didn't provide a specific combatActionID then just default to standing still
								if (combatActionID == null)
									combatActionID = getClientUnitCalculations ().determineCombatActionID (unit, false);
								
								// Draw unit
								getUnitClientUtils ().drawUnitFigures (unit, combatActionID, unit.getCombatHeading (), g,
									getCombatMapBitmapGenerator ().combatCoordinatesX (x, y, combatMapTileSet),
									getCombatMapBitmapGenerator ().combatCoordinatesY (x, y, combatMapTileSet), false);
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
					}
				
				// Draw unit that's part way through moving.
				// Really we need to sort this to draw it at the same time as the other units its on the way 'y' row as, but this will do for now.
				if (getUnitMoving () != null)
					try
					{
						final String movingActionID = getClientUnitCalculations ().determineCombatActionID (getUnitMoving ().getUnit (), true);
						getUnitClientUtils ().drawUnitFigures (getUnitMoving ().getUnit (), movingActionID, getUnitMoving ().getUnit ().getCombatHeading (), g,
							getUnitMoving ().getCurrentX (), getUnitMoving ().getCurrentY (), false);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				// Draw ranged attack missiles?
				if ((getAttackAnim () != null) && (getAttackAnim ().isRangedAttack ()) && (getAttackAnim ().getCurrent () != null))
					try
					{
						// Draw which missile image?
						final BufferedImage ratImage = getAnim ().loadImageOrAnimationFrame (getAttackAnim ().getRatCurrentImage ().getRangedAttackTypeCombatImageFile (),
							getAttackAnim ().getRatCurrentImage ().getRangedAttackTypeCombatAnimation ());
						
						// Draw each missile
						for (final int [] position : getAttackAnim ().getCurrent ())
						{
							final int imageWidth = ratImage.getWidth () * position [2];
							final int imageHeight = ratImage.getHeight () * position [2];
							
							final int currentX = position [0] - (imageWidth / 2);
							final int currentY = position [1] - imageHeight;
							
							g.drawImage (ratImage, currentX, currentY, imageWidth, imageHeight, null);
						}
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				// Draw the buttons panel background at the bottom of the window
				g.drawImage (background, 0, getHeight () - (background.getHeight () * 2), background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		// Animate the terrain tiles
		new Timer ((int) (1000 / combatMapTileSet.getAnimationSpeed ()), new ActionListener ()
		{
			@Override
			public final void actionPerformed (final ActionEvent e)
			{
				final int newFrame = terrainAnimFrame + 1;
				terrainAnimFrame = (newFrame >= combatMapTileSet.getAnimationFrameCount ()) ? 0 : newFrame;
				contentPane.repaint ();
			}
		}).start ();
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getCombatLayout ()));
		
		// Buttons
		contentPane.add (getUtils ().createImageButton (spellAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatSpell");
		contentPane.add (getUtils ().createImageButton (waitAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatWait");
		contentPane.add (getUtils ().createImageButton (doneAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatDone");
		contentPane.add (getUtils ().createImageButton (fleeAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatFlee");
		contentPane.add (getUtils ().createImageButton (autoAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatAuto");
		
		contentPane.add (getUtils ().createImageButton (toggleDamageCalculationsAction, null, null, null, calculatorButtonNormal, calculatorButtonPressed, calculatorButtonNormal), "frmCombatToggleDamageCalculations");
		
		// Player names (colour gets set in initNewCombat once we know who the players actually are)
		defendingPlayerName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (defendingPlayerName, "frmCombatDefendingPlayer");
		
		attackingPlayerName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (attackingPlayerName, "frmCombatAttackingPlayer");
		
		// The first time the CombatUI opens, we'll have skipped the call to initNewCombat () because it takes place before init (), so do it now
		initNewCombat ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		
		log.trace ("Exiting init");
	}

	/**
	 * The CombatUI is reused for every combat, since there can only be one at a time.  The init method must only set up things that are permanent no matter
	 * how many combats are played.  So this method does the configuration that needs to be done each time a new combat is started.
	 * 
	 * @throws IOException If there is a problem
	 */
	public final void initNewCombat () throws IOException
	{
		log.trace ("Entering initNewCombat");

		// Skip if the controls don't exist yet - there's a duplicate call to initNewCombat () at the end of init () for the case of the 1st combat that takes place
		if (contentPane != null)
		{
			// Always turn auto back off again for new combats
			autoControl = false;
			currentPlayerID = null;
			
			// Generates the bitmap for the static portion of the terrain
			getCombatMapBitmapGenerator ().smoothMapTerrain (getCombatLocation (), getCombatTerrain ());
			combatMapBitmaps = getCombatMapBitmapGenerator ().generateCombatMapBitmaps ();
			
			// Work out who the two players involved are.
			// There must always be at least one unit on each side.  The only situation where we can get a combat with zero defenders is attacking an empty city,
			// but in that case the server doesn't even send the startCombat message, so we don't even bother firing up the combatUI, it just goes straight to the
			// Raze/Capture screen (see how the startCombat method on the server is written).
			players = getCombatMapUtils ().determinePlayersInCombatFromLocation
				(getCombatLocation (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), getClient ().getPlayers ());
			if (!players.bothFound ())
				throw new MomException ("CombatUI tried to start up with zero units on one or other side");
			
			// Which one of the players isn't us?
			final PlayerPublicDetails otherPlayer = (players.getAttackingPlayer ().getPlayerDescription ().getPlayerID ().equals (getClient ().getOurPlayerID ())) ?
				players.getDefendingPlayer () : players.getAttackingPlayer ();
				
			// Now we can start the right music; if they've got a custom photo then default to the standard (raiders) music
			final MomPersistentPlayerPublicKnowledge otherPub = (MomPersistentPlayerPublicKnowledge) otherPlayer.getPersistentPlayerPublicKnowledge ();
			final String otherPhotoID = (otherPub.getStandardPhotoID () != null) ? otherPub.getStandardPhotoID () : CommonDatabaseConstants.WIZARD_ID_RAIDERS;
			final List<WizardCombatPlayList> possiblePlayLists = getGraphicsDB ().findWizard (otherPhotoID, "initNewCombat").getCombatPlayList ();
			
			// Pick a music track at random
			try
			{
				if (possiblePlayLists.size () < 1)
					throw new MomException ("Wizard " + otherPhotoID + " has no combat music defined");
				
				getMusicPlayer ().setShuffle (false);
				getMusicPlayer ().playPlayList (possiblePlayLists.get (getRandomUtils ().nextInt (possiblePlayLists.size ())).getPlayListID ());
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
			
			// If using the CombatUI for the first combat, languageChanged () will be called automatically after this method.  If calling it for a subsequent
			// combat then it won't be, so have to force a call here to get the player names to be displayed properly.
			if (defendingPlayerName != null)
				languageChanged ();
			
			// Find all the units involved in this combat, kick off their animations (if they're flying), and make a
			// grid showing which is in which cell, so when we draw them its easier to draw the back ones first.
			
			// unitToDrawAtEachLocation is a lot simpler here than in OverlandMapUI since there can only ever be 1 unit at each location.
			unitToDrawAtEachLocation = new MemoryUnit [getClient ().getSessionDescription ().getCombatMapSize ().getHeight ()] [getClient ().getSessionDescription ().getCombatMapSize ().getWidth ()];
			                                                                  
			for (final MemoryUnit unit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getCombatPosition () != null) && (getCombatLocation ().equals (unit.getCombatLocation ())))
				{
					// False, because other than during a move anim, units are stood still
					final String standingActionID = getClientUnitCalculations ().determineCombatActionID (unit, false);
					getUnitClientUtils ().registerUnitFiguresAnimation (unit.getUnitID (), standingActionID, unit.getCombatHeading (), contentPane);
					
					unitToDrawAtEachLocation [unit.getCombatPosition ().getY ()] [unit.getCombatPosition ().getX ()] = unit;
				}
		}

		log.trace ("Exiting initNewCombat");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmCombat", "Title"));
		
		spellAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCombat", "Spell"));
		waitAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCombat", "Wait"));
		doneAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCombat", "Done"));
		fleeAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCombat", "Flee"));
		autoAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCombat", "Auto"));

		// Set the player name labels to the correct colours.
		// Prefer this was done in initNewCombat, but there's no guarantee that the labels exist yet at that point.
		final MomTransientPlayerPublicKnowledge atkTrans = (MomTransientPlayerPublicKnowledge) players.getAttackingPlayer ().getTransientPlayerPublicKnowledge ();
		attackingPlayerName.setForeground (new Color (Integer.parseInt (atkTrans.getFlagColour (), 16)));

		final MomTransientPlayerPublicKnowledge defTrans = (MomTransientPlayerPublicKnowledge) players.getDefendingPlayer ().getTransientPlayerPublicKnowledge ();
		defendingPlayerName.setForeground (new Color (Integer.parseInt (defTrans.getFlagColour (), 16)));
		
		// Set the player name labels to the correct text.
		// If its the monsters player, use the name of the map cell (Ancient Temple, Nature Node, etc) - they're only actually called "Rampaging Monsters"
		// if they're openly walking around the map or attack us; this is how the original MoM works.
		attackingPlayerName.setText (getWizardClientUtils ().getPlayerName (players.getAttackingPlayer ()));
		
		String defPlayerName = getWizardClientUtils ().getPlayerName (players.getDefendingPlayer ());
		if (!players.getDefendingPlayer ().getPlayerDescription ().isHuman ())
		{
			final MomPersistentPlayerPublicKnowledge defPub = (MomPersistentPlayerPublicKnowledge) players.getDefendingPlayer ().getPersistentPlayerPublicKnowledge ();
			if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (defPub.getWizardID ()))
			{
				final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
					(getCombatLocation ().getZ ()).getRow ().get (getCombatLocation ().getY ()).getCell ().get (getCombatLocation ().getX ());
				if ((mc != null) && (mc.getTerrainData () != null))
					try
					{
						// Tile types (nodes)
						if ((mc.getTerrainData ().getTileTypeID () != null) && (getClient ().getClientDB ().findTileType (mc.getTerrainData ().getTileTypeID (), "CombatUI").getMagicRealmID () != null))
						{
							final TileType tileType = getLanguage ().findTileType (mc.getTerrainData ().getTileTypeID ());
							if ((tileType != null) && (tileType.getTileTypeShowAsFeature () != null))
								defPlayerName = tileType.getTileTypeShowAsFeature ();
						}
						
						// Map features (lairs and towers)
						else if ((mc.getTerrainData ().getMapFeatureID () != null) && (getClient ().getClientDB ().findMapFeature (mc.getTerrainData ().getMapFeatureID (), "CombatUI").isAnyMagicRealmsDefined ()))
						{
							final MapFeature mapFeature = getLanguage ().findMapFeature (mc.getTerrainData ().getMapFeatureID ());
							if ((mapFeature != null) && (mapFeature.getMapFeatureDescription () != null))
								defPlayerName = mapFeature.getMapFeatureDescription (); 
						}
					}
					catch (final RecordNotFoundException e)
					{
						log.error (e, e);
					}
			}
		}
		
		defendingPlayerName.setText (defPlayerName);
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * Careful with making updates to this since all the drawing is based on it.  Updates must be consistent with the current location of units, i.e. unit.setCombatPosition () 
	 * @return Units occupying each cell of the combat map
	 */
	public final MemoryUnit [] [] getUnitToDrawAtEachLocation ()
	{
		return unitToDrawAtEachLocation;
	}
	
	/**
	 * Anim messages need access to this to trigger repaints
	 * @return Content pane
	 */
	public final JPanel getContentPane ()
	{
		return contentPane;
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCombatLayout ()
	{
		return combatLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCombatLayout (final XmlLayoutContainerEx layout)
	{
		combatLayout = layout;
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
	 * @return Combat location
	 */
	public final MapCoordinates3DEx getCombatLocation ()
	{
		return combatLocation;
	}

	/**
	 * @param loc Combat location
	 */
	public final void setCombatLocation (final MapCoordinates3DEx loc)
	{
		combatLocation = loc;
	}
	
	/**
	 * @return Combat terrain
	 */
	public final MapAreaOfCombatTiles getCombatTerrain ()
	{
		return combatTerrain;
	}
	
	/**
	 * @param map Combat terrain
	 */
	public final void setCombatTerrain (final MapAreaOfCombatTiles map)
	{
		combatTerrain = map;
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
	 * @return Bitmap generator for the static terrain
	 */
	public final CombatMapBitmapGenerator getCombatMapBitmapGenerator ()
	{
		return combatMapBitmapGenerator;
	}

	/**
	 * @param gen Bitmap generator for the static terrain
	 */
	public final void setCombatMapBitmapGenerator (final CombatMapBitmapGenerator gen)
	{
		combatMapBitmapGenerator = gen;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param util Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils util)
	{
		combatMapUtils = util;
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
	 * @return Music player
	 */
	public final AudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final AudioPlayer player)
	{
		musicPlayer = player;
	}

	/**
	 * @return Random utils
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random utils
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
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
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
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
	 * @return Let AI auto control our units?
	 */
	public final boolean isAutoControl ()
	{
		return autoControl;
	}

	/**
	 * @param auto Let AI auto control our units?
	 */
	public final void setAutoControl (final boolean auto)
	{
		autoControl = auto;
	}

	/**
	 * @return Whose turn it currently is in this combat
	 */
	public final Integer getCurrentPlayerID ()
	{
		return currentPlayerID;
	}

	/**
	 * @param playerID Whose turn it currently is in this combat
	 */
	public final void setCurrentPlayerID (final Integer playerID)
	{
		currentPlayerID = playerID;
	}

	/**
	 * @return Unit that's in the middle of moving from one cell to another
	 */
	public final MoveUnitInCombatMessageImpl getUnitMoving ()
	{
		return unitMoving;
	}

	/**
	 * @param u Unit that's in the middle of moving from one cell to another
	 */
	public final void setUnitMoving (final MoveUnitInCombatMessageImpl u)
	{
		unitMoving = u;
	}

	/**
	 * @return Attack that's in the middle of taking place
	 */
	public final ApplyDamageMessageImpl getAttackAnim ()
	{
		return attackAnim;
	}

	/**
	 * @param aa Attack that's in the middle of taking place
	 */
	public final void setAttackAnim (final ApplyDamageMessageImpl aa)
	{
		attackAnim = aa;
	}
}
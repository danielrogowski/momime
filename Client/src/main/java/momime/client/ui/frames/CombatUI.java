package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.ClientUnitCalculations;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.AnimationGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.TileSetGfx;
import momime.client.graphics.database.WizardGfx;
import momime.client.language.database.MapFeatureLang;
import momime.client.language.database.ShortcutKeyLang;
import momime.client.language.database.SpellBookSectionLang;
import momime.client.language.database.SpellLang;
import momime.client.language.database.TileTypeLang;
import momime.client.messages.process.ApplyDamageMessageImpl;
import momime.client.messages.process.MoveUnitInCombatMessageImpl;
import momime.client.process.CombatMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.VariableManaUI;
import momime.client.utils.AnimationController;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.TextUtils;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.calculations.CombatMoveType;
import momime.common.calculations.SpellCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Shortcut;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitAttributeComponent;
import momime.common.database.UnitAttributePositiveNegative;
import momime.common.messages.CombatMapSize;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.CombatAutoControlMessage;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.common.messages.clienttoserver.RequestMoveCombatUnitMessage;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.JPanelWithConstantRepaints;
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
	/** Invisible colour for when there's no CAE being cast */
	public final static Color NO_FLASH_COLOUR = new Color (0, 0, 0, 0);
	
	/** Class logger */
	private final Log log = LogFactory.getLog (CombatUI.class);

	/** XML layout */
	private XmlLayoutContainerEx combatLayoutMain;

	/** XML layout */
	private XmlLayoutContainerEx combatLayoutBottom;
	
	/** Small font */
	private Font smallFont;

	/** Medium font */
	private Font mediumFont;
	
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
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Utils for drawing units */
	private UnitClientUtils unitClientUtils;
	
	/** Client unit calculations */
	private ClientUnitCalculations clientUnitCalculations;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Combat map processing */
	private CombatMapProcessing combatMapProcessing;

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Spell calculations */
	private SpellCalculations spellCalculations;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** MemoryGridCell utils */
	private MemoryGridCellUtils memoryGridCellUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;

	/** Spell book */
	private SpellBookUI spellBookUI;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;

	/** Help text scroll */
	private HelpUI helpUI;
	
	/** UI for displaying damage calculations */
	private DamageCalculationsUI damageCalculationsUI;

	/** Variable MP popup */
	private VariableManaUI variableManaUI;	

	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;
	
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

	/** Attacking and defending players */
	private CombatPlayers players;
	
	/** Name of defending player */
	private JLabel defendingPlayerName;
	
	/** Name of attacking player */
	private JLabel attackingPlayerName;
	
	/** Casting skill label */
	private JLabel skillLabel;
	
	/** Mana label */
	private JLabel manaLabel;
	
	/** Range label */
	private JLabel rangeLabel;
	
	/** Max castable label */
	private JLabel castableLabel;
	
	/** Casting skill value */
	private JLabel skillValue;
	
	/** Mana value */
	private JLabel manaValue;
	
	/** Range value */
	private JLabel rangeValue;
	
	/** Max castable value */
	private JLabel castableValue;
	
	/** Max castable spell cost */
	private int maxCastable;
	
	/** Unit name label */
	private JLabel selectedUnitName;
	
	/** Unit's melee attack value */
	private JLabel selectedUnitMeleeValue;

	/** Unit's ranged attack value */
	private JLabel selectedUnitRangedValue;

	/** Average number of hits this unit's melee attack will score */
	private String selectedUnitMeleeAverage;
	
	/** Average number of hits this unit's melee attack will score */
	private JLabel selectedUnitMeleeAverageLabel;

	/** Average number of hits this unit's ranged attack will score */
	private String selectedUnitRangedAverage;
	
	/** Average number of hits this unit's ranged attack will score */
	private JLabel selectedUnitRangedAverageLabel;
	
	/** Movement remaining */
	private JLabel selectedUnitMovement;
	
	/** Image of unit's melee attack */
	private JLabel selectedUnitMeleeImage;

	/** Image of unit's ranged attack */
	private JLabel selectedUnitRangedImage;

	/** Image of unit's movement */
	private JLabel selectedUnitMovementImage;
	
	/** Image of selected unit */
	private JLabel selectedUnitImage;
	
	/** Subpanel showing icons for CAEs cast by the defender */
	private JPanel defenderCAEs;
	
	/** Subpanel showing icons for CAEs cast by the attacker */
	private JPanel attackerCAEs;
	
	/** Subpanel showing icons for CAEs cast by neither player (e.g. node auras) */
	private JPanel commonCAEs;
	
	/** Images added to display CAEs */
	private List<JLabel> defenderCAEImages = new ArrayList<JLabel> ();
	
	/** Images added to display CAEs */
	private List<JLabel> attackerCAEImages = new ArrayList<JLabel> ();
	
	/** Images added to display CAEs */
	private List<JLabel> commonCAEImages = new ArrayList<JLabel> ();

	/** MP penalty multiplier for how far this combat is from our wizard's fortress; null if we're banished and can't cast anything at all */
	private Integer doubleRangePenalty;
	
	/** Cancel spell targetting */
	private Action cancelTargetSpellAction;
	
	/** Spell targetting prompt if we're the defender */
	private JTextArea targetSpellPromptDefender;

	/** Cancel spell targetting if we're the defender */
	private JButton cancelTargetSpellDefender;
	
	/** Spell targetting prompt if we're the attacker */
	private JTextArea targetSpellPromptAttacker;

	/** Cancel spell targetting if we're the defender */
	private JButton cancelTargetSpellAttacker;
	
	/** Bitmaps for each animation frame of the combat map */
	private BufferedImage [] combatMapBitmaps;
	
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
	
	/** Combat tile set */
	private TileSetGfx combatMapTileSet;
	
	/** Combat tile that the mouse is currently over */
	private MapCoordinates2DEx moveToLocation;
	
	/** Details of what action (if any) will take place if we click on each tile; this can be null when it isn't our turn */
	private CombatMoveType [] [] movementTypes;
	
	/** Currently selected unit */
	private MemoryUnit selectedUnitInCombat;
	
	/** Spell chosen from spell book that we want to cast into this combat, and need to select a target for */
	private Spell spellBeingTargetted;
	
	/** Colour to flash the combat screen when a CAE is being cast */
	private Color flashColour = NO_FLASH_COLOUR;
	
	/** Animation to display for a spell being cast */
	private AnimationGfx combatCastAnimation;
	
	/** Whether the combat case animation appears behind or in front of the units */
	private boolean combatCastAnimationInFront;
	
	/** Coords to display combat cast animation(s) at, in pixels */
	private List<Point> combatCastAnimationPositions = new ArrayList<Point> ();

	/** Frame number to display of combat cast animation */
	private int combatCastAnimationFrame;
	
	/** Content pane */
	private JPanel contentPane;
	
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
		
		// We need the tile set to know the frame rate and number of frames
		combatMapTileSet = getGraphicsDB ().findTileSet (GraphicsDatabaseConstants.TILE_SET_COMBAT_MAP, "CombatUI");
		
		// Actions
		spellAction = new AbstractAction ()
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

		waitAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getCombatMapProcessing ().selectedUnitWait ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		doneAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					getCombatMapProcessing ().selectedUnitDone ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
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
				try
				{
					getDamageCalculationsUI ().setVisible (!getDamageCalculationsUI ().isVisible ());
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		cancelTargetSpellAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				spellBeingTargetted = null;
				
				targetSpellPromptDefender.setText (null);
				cancelTargetSpellDefender.setVisible (false);
				targetSpellPromptAttacker.setText (null);
				cancelTargetSpellAttacker.setVisible (false);
				
				defendingPlayerName.setVisible (true);
				attackingPlayerName.setVisible (true);
			}
		};
		
		// Initialize the content pane
		
		// This is split into a top and bottom half.  The top half shows the terrain and units and is all custom drawn.  This repaints
		// itself as fast as it can, so its not necessary to fire repaint events at it every time an animation updates.
		
		// The bottom half contains all the standard Swing controls such as all the buttons.  So there's nothing special here.
		contentPane = new JPanel (new XmlLayoutManager (getCombatLayoutMain ()));
		
		final JPanelWithConstantRepaints topPanel = new JPanelWithConstantRepaints ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Work out the frame number to display for the terrain
				final double frameNumber = System.nanoTime () / (1000000000d / combatMapTileSet.getAnimationSpeed ());
				final int terrainAnimFrame = ((int) frameNumber) % combatMapTileSet.getAnimationFrameCount ();
				
				// Draw the static portion of the terrain
				g.drawImage (combatMapBitmaps [terrainAnimFrame], 0, 0, null);
				
				// Draw the blue outline marking the tile the mouse is currently over
				if (moveToLocation != null)
					try
					{
						final int x = getCombatMapBitmapGenerator ().combatCoordinatesX (moveToLocation.getX (), moveToLocation.getY (), combatMapTileSet);
						final int y = getCombatMapBitmapGenerator ().combatCoordinatesY (moveToLocation.getX (), moveToLocation.getY (), combatMapTileSet);
						
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame (null, "COMBAT_MOVE_TO", false);
						g.drawImage (image, x, y, image.getWidth () * 2, image.getHeight () * 2, null);
						
						// Show icon for whether we can move here, shoot here, target a spell here or so on
						final String moveTypeFilename;
						if (getSpellBeingTargetted () == null)
						{
							// Trying to move here
							final CombatMoveType moveType = (movementTypes == null) ? CombatMoveType.CANNOT_MOVE :
								movementTypes [moveToLocation.getY ()] [moveToLocation.getX ()];
							moveTypeFilename = moveType.getImageFilename ();
						}
						else
						{
							// Trying to target a spell here
							final MemoryUnit unit = getUnitUtils ().findAliveUnitInCombatAt (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (),
								getCombatLocation (), moveToLocation);
							
							final boolean validTarget;
							if (getSpellBeingTargetted ().getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
							{
								// Summoning spell - valid as long as there isn't a unit here
								validTarget = (unit == null);
							}
							else
							{
								// Unit enchantment / curse - separate method to perform all validation that this unit is a valid target
								validTarget = (unit != null) && (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
									(getSpellBeingTargetted (), getCombatLocation (), getClient ().getOurPlayerID (),
									(getSpellBeingTargetted ().getCombatMaxDamage () == null) ? null : getVariableManaUI ().getVariableDamage (),
									unit, getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
									getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (),
									getClient ().getClientDB ()) == TargetSpellResult.VALID_TARGET);
							}
							
							moveTypeFilename = "/momime.client.graphics/ui/combat/moveType-" + (validTarget ? "spell" : "cannot") + ".png";
						}
						
						if (moveTypeFilename != null)
						{
							final BufferedImage moveTypeImage = getUtils ().loadImage (moveTypeFilename);
							g.drawImage (moveTypeImage, x, y, moveTypeImage.getWidth () * 2, moveTypeImage.getHeight () * 2, null);
						}
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				// Draw the red outline marking the unit who we're currently moving
				// NB. combatPosition gets nulled out when units are halfway walking between cells
				if ((getSelectedUnitInCombat () != null) && (getSelectedUnitInCombat ().getCombatPosition () != null))
					try
					{
						final int x = getCombatMapBitmapGenerator ().combatCoordinatesX (getSelectedUnitInCombat ().getCombatPosition ().getX (),
							getSelectedUnitInCombat ().getCombatPosition ().getY (), combatMapTileSet);
						final int y = getCombatMapBitmapGenerator ().combatCoordinatesY (getSelectedUnitInCombat ().getCombatPosition ().getX (),
							getSelectedUnitInCombat ().getCombatPosition ().getY (), combatMapTileSet);
						
						final BufferedImage image = getAnim ().loadImageOrAnimationFrame (null, "COMBAT_SELECTED_UNIT", false);
						g.drawImage (image, x, y, image.getWidth () * 2, image.getHeight () * 2, null);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				// Draw casting animation behind units?
				if ((getCombatCastAnimation () != null) && (!isCombatCastAnimationInFront ()))
					try
					{
						final BufferedImage image = getUtils ().loadImage (getCombatCastAnimation ().getFrame ().get (getCombatCastAnimationFrame ()));
						for (final Point p : getCombatCastAnimationPositions ())
							g.drawImage (image, p.x, p.y, image.getWidth () * 2, image.getHeight () * 2, null);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
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
									if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (getAttackAnim ().getAttackAttributeID ()))
									{
										// Show firing unit going 'pew'
										if ((unit == getAttackAnim ().getAttackerUnit ()) && (getAttackAnim ().getCurrent () == null))
											combatActionID = GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_RANGED_ATTACK;
									}
									
									// Melee attack animation
									else if (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK.equals (getAttackAnim ().getAttackAttributeID ()))
									{
										if ((unit == getAttackAnim ().getAttackerUnit ()) || (unit == getAttackAnim ().getDefenderUnit ()))
											combatActionID = GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_MELEE_ATTACK;
									}
								}
								
								// If animation didn't provide a specific combatActionID then just default to standing still
								if (combatActionID == null)
									combatActionID = getClientUnitCalculations ().determineCombatActionID (unit, false);
								
								// Draw unit
								getUnitClientUtils ().drawUnitFigures (unit, combatActionID, unit.getCombatHeading (), g,
									getCombatMapBitmapGenerator ().combatCoordinatesX (x, y, combatMapTileSet),
									getCombatMapBitmapGenerator ().combatCoordinatesY (x, y, combatMapTileSet), false, false);
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
							getUnitMoving ().getCurrentX (), getUnitMoving ().getCurrentY (), false, false);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				// Draw ranged attack missiles?
				if ((getAttackAnim () != null) && (getAttackAnim ().getCurrent () != null) &&
					(CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK.equals (getAttackAnim ().getAttackAttributeID ())))
					
					try
					{
						// Draw which missile image?
						final BufferedImage ratImage = getAnim ().loadImageOrAnimationFrame (getAttackAnim ().getRatCurrentImage ().getRangedAttackTypeCombatImageFile (),
							getAttackAnim ().getRatCurrentImage ().getRangedAttackTypeCombatAnimation (), false);
						
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
				
				// Draw casting animation in front of units?
				if ((getCombatCastAnimation () != null) && (isCombatCastAnimationInFront ()))
					try
					{
						final BufferedImage image = getUtils ().loadImage (getCombatCastAnimation ().getFrame ().get (getCombatCastAnimationFrame ()));
						for (final Point p : getCombatCastAnimationPositions ())
							g.drawImage (image, p.x, p.y, image.getWidth () * 2, image.getHeight () * 2, null);
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
			
			@Override
			protected final void paintChildren (final Graphics g)
			{
				super.paintChildren (g);
				
				// Flash the colour of the screen for CAEs
				if (getFlashColour () != NO_FLASH_COLOUR)
				{
					g.setColor (getFlashColour ());
					g.fillRect (0, 0, getWidth (), getHeight ());
				}
			}
		};
		
		contentPane.add (topPanel, "frmCombatScenery");		
		
		// Bottom portion containing all the buttons
		final JPanel bottomPanel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};

		bottomPanel.setLayout (new XmlLayoutManager (getCombatLayoutBottom ()));
		contentPane.add (bottomPanel, "frmCombatBottomPanel");		
		
		bottomPanel.add (getUtils ().createImageButton (spellAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatSpell");
		bottomPanel.add (getUtils ().createImageButton (waitAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatWait");
		bottomPanel.add (getUtils ().createImageButton (doneAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatDone");
		bottomPanel.add (getUtils ().createImageButton (fleeAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatFlee");
		bottomPanel.add (getUtils ().createImageButton (autoAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled), "frmCombatAuto");
		
		bottomPanel.add (getUtils ().createImageButton (toggleDamageCalculationsAction, null, null, null, calculatorButtonNormal, calculatorButtonPressed, calculatorButtonNormal), "frmCombatToggleDamageCalculations");
		
		// Player names (colour gets set in initNewCombat once we know who the players actually are)
		defendingPlayerName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		bottomPanel.add (defendingPlayerName, "frmCombatDefendingPlayer");
		
		attackingPlayerName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		bottomPanel.add (attackingPlayerName, "frmCombatAttackingPlayer");

		// Casting status labels
		skillLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (skillLabel, "frmCombatSkillLabel");
		
		manaLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (manaLabel, "frmCombatManaLabel");
		
		rangeLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (rangeLabel, "frmCombatRangeLabel");
		
		castableLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (castableLabel, "frmCombatCastableLabel");
		
		skillValue = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (skillValue, "frmCombatSkill");
		
		manaValue = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (manaValue, "frmCombatMana");
		
		rangeValue = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (rangeValue, "frmCombatRange");
		
		castableValue = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (castableValue, "frmCombatCastable");
		
		// Selected unit labels
		selectedUnitName = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		bottomPanel.add (selectedUnitName, "frmCombatCurrentUnitName");

		selectedUnitMeleeValue = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (selectedUnitMeleeValue, "frmCombatCurrentUnitMelee");
		
		selectedUnitRangedValue = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (selectedUnitRangedValue, "frmCombatCurrentUnitRanged");
		
		selectedUnitMeleeAverageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (selectedUnitMeleeAverageLabel, "frmCombatCurrentUnitMeleeAverageHits");
		
		selectedUnitRangedAverageLabel = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (selectedUnitRangedAverageLabel, "frmCombatCurrentUnitRangedAverageHits");
		
		selectedUnitMovement = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		bottomPanel.add (selectedUnitMovement, "frmCombatCurrentUnitMovement");

		selectedUnitMeleeImage = new JLabel ();
		bottomPanel.add (selectedUnitMeleeImage, "frmCombatCurrentUnitMeleeImage");

		selectedUnitRangedImage = new JLabel ();
		bottomPanel.add (selectedUnitRangedImage, "frmCombatCurrentUnitRangedImage");

		selectedUnitMovementImage = new JLabel ();
		bottomPanel.add (selectedUnitMovementImage, "frmCombatCurrentUnitMovementImage");
		
		selectedUnitImage = new JLabel ();
		bottomPanel.add (selectedUnitImage, "frmCombatCurrentUnitImage");
		
		// Spell targetting
		targetSpellPromptDefender = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
		bottomPanel.add (targetSpellPromptDefender, "frmCombatSelectSpellTargetDefender");
		
		cancelTargetSpellDefender = getUtils ().createImageButton (cancelTargetSpellAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled);
		bottomPanel.add (cancelTargetSpellDefender, "frmCombatCancelSpellDefender");
		
		targetSpellPromptAttacker = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
		bottomPanel.add (targetSpellPromptAttacker, "frmCombatSelectSpellTargetAttacker");
		
		cancelTargetSpellAttacker = getUtils ().createImageButton (cancelTargetSpellAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonDisabled);
		bottomPanel.add (cancelTargetSpellAttacker, "frmCombatCancelSpellAttacker");
		
		// CAEs
		defenderCAEs = new JPanel ();
		defenderCAEs.setOpaque (false);
		defenderCAEs.setLayout (new GridBagLayout ());
		bottomPanel.add (defenderCAEs, "frmCombatDefenderCombatAreaEffects");

		attackerCAEs = new JPanel ();
		attackerCAEs.setOpaque (false);
		attackerCAEs.setLayout (new GridBagLayout ());
		bottomPanel.add (attackerCAEs, "frmCombatAttackerCombatAreaEffects");
		
		commonCAEs = new JPanel ();
		commonCAEs.setOpaque (false);
		commonCAEs.setLayout (new GridBagLayout ());
		bottomPanel.add (commonCAEs, "frmCombatCommonCombatAreaEffects");
		
		// The first time the CombatUI opens, we'll have skipped the call to initNewCombat () because it takes place before init (), so do it now
		initNewCombat ();
		
		// Capture mouse clicks on the scenery panel
		topPanel.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				final MapCoordinates2DEx combatCoords = convertMouseCoordsToCombatCoords (ev);

				// Right clicking, if we're right clicking on the selected unit or on a unit which has no movement left or is not ours, shows the unit info.
				// If it is our unit, not the currently selected unit, and it has movement left, then right clicking selects it.
				try
				{
					if (SwingUtilities.isRightMouseButton (ev))
					{
						final MemoryUnit unit = getUnitUtils ().findAliveUnitInCombatAt (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (),
							getCombatLocation (), combatCoords);
						if (unit != null)
						{
							if ((unit == getSelectedUnitInCombat ()) || (unit.getOwningPlayerID () != getClient ().getOurPlayerID ()) ||
								(unit.getDoubleCombatMovesLeft () == null) || (unit.getDoubleCombatMovesLeft () <= 0))
							{
								// Is there a unit info screen already open for this unit?
								UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (unit.getUnitURN ());
								if (unitInfo == null)
								{
									unitInfo = getPrototypeFrameCreator ().createUnitInfo ();
									unitInfo.setUnit (unit);
									getClient ().getUnitInfos ().put (unit.getUnitURN (), unitInfo);
								}
							
								unitInfo.setVisible (true);
							}
							else
							{
								// Move it to the first unit in the list.
								// We could just select it - but this mucks up if the unit then has two moves - half way through we'd end up jumping to a different unit.
								getCombatMapProcessing ().moveToFrontOfList (unit);
							}
						}
					}
					else if (getSpellBeingTargetted () != null)
					{
						// Left clicking to target a spell
						final MemoryUnit unit = getUnitUtils ().findAliveUnitInCombatAt (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (),
							getCombatLocation (), moveToLocation);
						
						// Build message
						final RequestCastSpellMessage msg = new RequestCastSpellMessage ();
						msg.setSpellID (getSpellBeingTargetted ().getSpellID ());
						msg.setCombatLocation (getCombatLocation ());
						
						// Does the spell have varying cost?
						if (getSpellBeingTargetted ().getCombatMaxDamage () != null)
							msg.setVariableDamage (getVariableManaUI ().getVariableDamage ());
									
						boolean isValidTarget = false;
						if (getSpellBeingTargetted ().getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
						{
							// Summoning spell - valid as long as there isn't a unit here
							if (unit == null)
							{
								isValidTarget = true;
								msg.setCombatTargetLocation (combatCoords);
							}
						}
						else
						{
							// Unit enchantment / curse - separate method to perform all validation that this unit is a valid target
							if (unit != null)
							{
								final TargetSpellResult validTarget = getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
									(getSpellBeingTargetted (), getCombatLocation (), getClient ().getOurPlayerID (),
									(getSpellBeingTargetted ().getCombatMaxDamage () == null) ? null : getVariableManaUI ().getVariableDamage (),
									unit, getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
									getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (),
									getClient ().getClientDB ());
								
								if (validTarget == TargetSpellResult.VALID_TARGET)
								{
									isValidTarget = true;
									msg.setCombatTargetUnitURN (unit.getUnitURN ());
								}

								// If we can't target on this unit, tell the player why not
								else if (validTarget.getUnitLanguageEntryID () != null)
								{
									final SpellLang spellLang = getLanguage ().findSpell (getSpellBeingTargetted ().getSpellID ());
									final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
									
									String text = getLanguage ().findCategoryEntry ("SpellTargetting", validTarget.getUnitLanguageEntryID ()).replaceAll
										("SPELL_NAME", (spellName != null) ? spellName : getSpellBeingTargetted ().getSpellID ());
									
									// If spell can only be targetted on specific magic realm/lifeform types, the list them
									if (validTarget == TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE)
										text = text + getSpellClientUtils ().listValidMagicRealmLifeformTypeTargetsOfSpell (getSpellBeingTargetted ());
									
									final MessageBoxUI msgBox = getPrototypeFrameCreator ().createMessageBox ();
									msgBox.setTitleLanguageCategoryID ("SpellTargetting");
									msgBox.setTitleLanguageEntryID ("Title");
									msgBox.setText (text);
									msgBox.setVisible (true);
								}
							}
						}

						if (isValidTarget)
						{
							getClient ().getServerConnection ().sendMessageToServer (msg);
							
							// Just use the common routine to close out the spell targetting prompt
							cancelTargetSpellAction.actionPerformed (null);
						}
					}
					else
					{
						// Left clicking to move to, or shoot at, this location (the server figures out which)
						if ((movementTypes != null) && (getSelectedUnitInCombat () != null))
						{
							final CombatMoveType moveType = movementTypes [combatCoords.getY ()] [combatCoords.getX ()];
							if ((moveType != null) && (moveType != CombatMoveType.CANNOT_MOVE))
							{
								final RequestMoveCombatUnitMessage msg = new RequestMoveCombatUnitMessage ();
								msg.setUnitURN (getSelectedUnitInCombat ().getUnitURN ());
								msg.setMoveTo (combatCoords);
								
								getClient ().getServerConnection ().sendMessageToServer (msg);
								
								// Blank out the movement types array, so its impossible for further clicks to send spurious additional moves to the server
								movementTypes = null;
							}
						}
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		topPanel.addMouseMotionListener (new MouseAdapter ()
		{
			@Override
			public final void mouseMoved (final MouseEvent ev)
			{
				moveToLocation = convertMouseCoordsToCombatCoords (ev);
			}
		});
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		
		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.COMBAT_TOGGLE_AUTO,								autoAction);
		contentPane.getActionMap ().put (Shortcut.COMBAT_TOGGLE_DAMAGE_CALCULATIONS,	toggleDamageCalculationsAction);
		contentPane.getActionMap ().put (Shortcut.COMBAT_MOVE_DONE,									doneAction);
		contentPane.getActionMap ().put (Shortcut.COMBAT_FLEE,												fleeAction);
		contentPane.getActionMap ().put (Shortcut.COMBAT_CAST_SPELL,									spellAction);
		contentPane.getActionMap ().put (Shortcut.COMBAT_MOVE_WAIT,									waitAction);
		
		topPanel.init ("CombatUI-repaintTimer");
		
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
		if (defendingPlayerName != null)
		{
			// Always turn auto back off again for new combats
			autoControl = false;
			currentPlayerID = null;
			flashColour = NO_FLASH_COLOUR;

			targetSpellPromptDefender.setText (null);
			cancelTargetSpellDefender.setVisible (false);
			targetSpellPromptAttacker.setText (null);
			cancelTargetSpellAttacker.setVisible (false);
			
			// If we're banished, then hide all casting-related info
			final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "initNewCombat");

			final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getCombatLocation ().getZ ()).getRow ().get (getCombatLocation ().getY ()).getCell ().get (getCombatLocation ().getX ());
			
			doubleRangePenalty = getSpellCalculations ().calculateDoubleCombatCastingRangePenalty
				(ourPlayer, getCombatLocation (), getMemoryGridCellUtils ().isTerrainTowerOfWizardry (mc.getTerrainData ()),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getClient ().getSessionDescription ().getOverlandMapSize ());

			if (doubleRangePenalty == null)
			{
				skillLabel.setVisible (false);
				manaLabel.setVisible (false);
				rangeLabel.setVisible (false);
				castableLabel.setVisible (false);

				skillValue.setText (null);
				manaValue.setText (null);
				rangeValue.setText (null);
				castableValue.setText (null);
				
				maxCastable = 0;
			}
			else
			{
				skillLabel.setVisible (true);
				manaLabel.setVisible (true);
				rangeLabel.setVisible (true);
				castableLabel.setVisible (true);

				// Set up initial casting values
				updateRemainingCastingSkill (getResourceValueUtils ().calculateCastingSkillOfPlayer (getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue ()), true);
			}
			
			// Generates the bitmap for the static portion of the terrain
			smoothCombatMapAndGenerateBitmaps ();
			
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
			final WizardGfx wizardGfx = getGraphicsDB ().findWizard (otherPhotoID, "initNewCombat");
			
			// Pick a music track at random
			try
			{
				if (wizardGfx.getCombatPlayList ().size () < 1)
					throw new MomException ("Wizard " + otherPhotoID + " has no combat music defined");
				
				getMusicPlayer ().setShuffle (false);
				getMusicPlayer ().playPlayList (wizardGfx.chooseRandomCombatPlayListID ());
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
			
			// If using the CombatUI for the first combat, languageChanged () will be called automatically after this method.  If calling it for a subsequent
			// combat then it won't be, so have to force a call here to get the player names to be displayed properly.
			languageChanged ();
			
			// Find all the units involved in this combat and make a grid showing which is in
			// which cell, so when we draw them its easier to draw the back ones first.
			
			// unitToDrawAtEachLocation is a lot simpler here than in OverlandMapUI since there can only ever be 1 unit at each location.
			unitToDrawAtEachLocation = new MemoryUnit [getClient ().getSessionDescription ().getCombatMapSize ().getHeight ()] [getClient ().getSessionDescription ().getCombatMapSize ().getWidth ()];
			                                                                  
			for (final MemoryUnit unit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				if ((unit.getStatus () == UnitStatusID.ALIVE) && (unit.getCombatPosition () != null) && (getCombatLocation ().equals (unit.getCombatLocation ())))
					unitToDrawAtEachLocation [unit.getCombatPosition ().getY ()] [unit.getCombatPosition ().getX ()] = unit;
			
			generateCombatAreaEffectIcons ();
		}

		log.trace ("Exiting initNewCombat");
	}
	
	/**
	 * This is called during map startup, but also from the options screen if we change smoothing options
	 * 
	 * @throws IOException If there is a problem loading any of the images
	 * @throws RecordNotFoundException If required entries in the graphics XML cannot be found
	 */
	public final void smoothCombatMapAndGenerateBitmaps () throws IOException, RecordNotFoundException
	{
		log.trace ("Entering smoothCombatMapAndGenerateBitmaps");

		getCombatMapBitmapGenerator ().smoothMapTerrain (getCombatLocation (), getCombatTerrain ());
		combatMapBitmaps = getCombatMapBitmapGenerator ().generateCombatMapBitmaps ();

		log.trace ("Exiting smoothCombatMapAndGenerateBitmaps");
	}

	/**
	 * @param currentSkill How much skill and MP we have remaining to use in this combat
	 * @param enableCasting Whether to enable the spell button; i.e. set to false if we've already cast a spell this combat turn
	 */
	public final void updateRemainingCastingSkill (final int currentSkill, final boolean enableCasting)
	{
		log.trace ("Entering updateRemainingCastingSkill: " + currentSkill);

		skillValue.setText (getTextUtils ().intToStrCommas (currentSkill));
		
		final int manaStored = getResourceValueUtils ().findAmountStoredForProductionType
			(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		manaValue.setText (getTextUtils ().intToStrCommas (manaStored));
	
		rangeValue.setText ("x " + getTextUtils ().halfIntToStr (doubleRangePenalty));
		
		// How much mana can we put into a spell, given the range?
		final int manaAvailable = (manaStored * 2) / doubleRangePenalty;
		maxCastable = Math.min (manaAvailable, currentSkill);
		castableValue.setText (getTextUtils ().intToStrCommas (maxCastable));
		
		// Additional spells may need to be greyed out in the spell book now we have less casting skill/MP
		getSpellBookUI ().languageOrPageChanged ();
		spellAction.setEnabled (enableCasting);

		log.trace ("Exiting updateRemainingCastingSkill");
	}

	/**
	 * @return Whether to enable the spells button
	 */
	public final boolean isSpellActionEnabled ()
	{
		return spellAction.isEnabled ();
	}
	
	/**
	 * @param b Whether to enable the spells button
	 */
	public final void setSpellActionEnabled (final boolean b)
	{
		spellAction.setEnabled (b);
	}
	
	/**
	 * Adds icons to the CombatUI showing all the CAEs applicable to this combat
	 * @throws IOException If there icon for any CAEs can't be found
	 */
	public final void generateCombatAreaEffectIcons () throws IOException
	{
		log.trace ("Entering generateCombatAreaEffectIcons");

		// Don't do anything if the combatUI has never been displayed (possible we're casting a spell like Heavenly Light that adds a CAE prior to any combat being played)
		if (defendingPlayerName != null)
		{
			// First remove all the old icons
			for (final JLabel image : defenderCAEImages)
				defenderCAEs.remove (image);

			for (final JLabel image : attackerCAEImages)
				attackerCAEs.remove (image);

			for (final JLabel image : commonCAEImages)
				commonCAEs.remove (image);
			
			defenderCAEImages.clear ();
			attackerCAEImages.clear ();
			commonCAEImages.clear ();
			
			// Then check all CAEs to see which ones are applicable to this combat
			for (final MemoryCombatAreaEffect cae : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect ())
				if ((cae.getMapLocation () == null) || (cae.getMapLocation ().equals (getCombatLocation ())))
				{
					// Which list should we add it to
					final JPanel caePanel;
					final List<JLabel> caeList;
					if (players.getAttackingPlayer ().getPlayerDescription ().getPlayerID ().equals (cae.getCastingPlayerID ()))
					{
						caePanel = attackerCAEs;
						caeList = attackerCAEImages;
					}
					else if (players.getDefendingPlayer ().getPlayerDescription ().getPlayerID ().equals (cae.getCastingPlayerID ()))
					{
						caePanel = defenderCAEs;
						caeList = defenderCAEImages;
					}
					else
					{
						caePanel = commonCAEs;
						caeList = commonCAEImages;
					}
					
					// Add the image - caeList.size () is a sneaky way of generating the 'x' values for the GridBagLayout
					final BufferedImage image = getUtils ().loadImage (getGraphicsDB ().findCombatAreaEffect
						(cae.getCombatAreaEffectID (), "generateCombatAreaEffectIcons").getCombatAreaEffectImageFile ());
					final JLabel label = getUtils ().createImage (doubleSize (image));
					
					caePanel.add (label, getUtils ().createConstraintsNoFill (caeList.size (), 0, 1, 1, new Insets (0, 1, 0, 1), GridBagConstraintsNoFill.CENTRE));
					caeList.add (label);
					
					// Right clicking on CAEs displays help text about them
					label.addMouseListener (new MouseAdapter ()
					{
						@Override
						public final void mouseClicked (final MouseEvent ev)
						{
							if (SwingUtilities.isRightMouseButton (ev))
							{
								try
								{
									getHelpUI ().showCombatAreaEffectID (cae.getCombatAreaEffectID ());
								}
								catch (final Exception e)
								{
									log.error (e, e);
								}
							}
						}
					});
				}
			
			defenderCAEs.revalidate ();
			attackerCAEs.revalidate ();
			commonCAEs.revalidate ();
		}		
		
		log.trace ("Exiting generateCombatAreaEffectIcons");
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
		cancelTargetSpellAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmCombat", "CancelTargetSpell"));
		
		skillLabel.setText (getLanguage ().findCategoryEntry ("frmCombat", "Skill") + ":");
		manaLabel.setText (getLanguage ().findCategoryEntry ("frmCombat", "Mana") + ":");
		rangeLabel.setText (getLanguage ().findCategoryEntry ("frmCombat", "Range") + ":");
		castableLabel.setText (getLanguage ().findCategoryEntry ("frmCombat", "Castable") + ":");

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
							final TileTypeLang tileType = getLanguage ().findTileType (mc.getTerrainData ().getTileTypeID ());
							if ((tileType != null) && (tileType.getTileTypeShowAsFeature () != null))
								defPlayerName = tileType.getTileTypeShowAsFeature ();
						}
						
						// Map features (lairs and towers)
						else if ((mc.getTerrainData ().getMapFeatureID () != null) && (getClient ().getClientDB ().findMapFeature (mc.getTerrainData ().getMapFeatureID (), "CombatUI").isAnyMagicRealmsDefined ()))
						{
							final MapFeatureLang mapFeature = getLanguage ().findMapFeature (mc.getTerrainData ().getMapFeatureID ());
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
		languageOrSelectedUnitChanged ();
		
		// Shortcut keys
		contentPane.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).clear ();
		if (contentPane.getActionMap ().keys () != null)
			for (final Object shortcut : contentPane.getActionMap ().keys ())
				if (shortcut instanceof Shortcut)
				{
					final ShortcutKeyLang shortcutKey = getLanguage ().findShortcutKey ((Shortcut) shortcut);
					if (shortcutKey != null)
					{
						final String keyCode = (shortcutKey.getNormalKey () != null) ? shortcutKey.getNormalKey () : shortcutKey.getVirtualKey ().value ().substring (3);
						log.debug ("Binding \"" + keyCode + "\" to action " + shortcut);
						contentPane.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (keyCode), shortcut);
					}
				}
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * Converts from pixel coordinates back to combat map coordinates
	 * 
	 * @param ev Mouse click event
	 * @return Combat map coordinates
	 */
	private final MapCoordinates2DEx convertMouseCoordsToCombatCoords (final MouseEvent ev)
	{
		// Work out y first
		final int separationY = combatMapTileSet.getTileHeight () / 2;
		int y = ev.getY () / 2;
		y = (y / separationY) + 1;
		
		// Now work out x
		final int separationX = combatMapTileSet.getTileWidth () + 2;		// Because of the way the tiles slot together
		int x = ev.getX () / 2;

		if (y % 2 == 1)
			x = x - (separationX/2);

		x = (x / separationX) + 1;
		
		return new MapCoordinates2DEx (x, y);
	}
	
	/**
	 * @return Currently selected unit
	 */
	public final MemoryUnit getSelectedUnitInCombat ()
	{
		return selectedUnitInCombat;
	}	
	
	/**
	 * Updates various controls, e.g. melee/ranged attack strength displayed in the panel at the bottom, when a different unit is selected in combat
	 * 
	 * @param unit Unit to select; this may be null if we've got no further units to move and our turn is ending
	 * @throws IOException If there is a problem
	 */
	public final void setSelectedUnitInCombat (final MemoryUnit unit) throws IOException
	{
		log.trace ("Entering setSelectedUnitInCombat: " + ((unit == null) ? "null" : "Unit URN " + unit.getUnitURN ()));
		
		// Record the selected unit
		selectedUnitInCombat = unit;
		
		// Work out where this unit can and cannot move
		if (unit == null)
		{
			movementTypes = null;
			selectedUnitMeleeValue.setText (null);
			selectedUnitRangedValue.setText (null);
			selectedUnitMovement.setText (null);
			selectedUnitMeleeImage.setIcon (null);
			selectedUnitRangedImage.setIcon (null);
			selectedUnitMovementImage.setIcon (null);
			selectedUnitImage.setIcon (null);
		}
		else
		{
			final CombatMapSize combatMapSize = getClient ().getSessionDescription ().getCombatMapSize ();
			
			final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
			final int [] [] doubleMovementDistances = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];

			// The only array we actually need to keep is the movementTypes, to show the correct icons as the mouse moves over different tiles
			movementTypes = new CombatMoveType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
			
			getUnitCalculations ().calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, unit,
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getCombatTerrain (),
				combatMapSize, getClient ().getPlayers (), getClient ().getClientDB ());
			
			// Calculate unit stats
			final int chanceToHit = Math.min (10, 3 +
				getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
					UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()));
			
			final int chanceToHitTimesFigures = chanceToHit * getUnitCalculations ().calculateAliveFigureCount (unit, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
						
				getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
					UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, getClient ().getPlayers (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
			
			// Melee attack / average hits / image
			final int meleeAttack = getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
				UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
			
			if (meleeAttack <= 0)
			{
				selectedUnitMeleeValue.setText (null);
				selectedUnitMeleeImage.setIcon (null);
				selectedUnitMeleeAverage = null;
			}
			else
			{
				selectedUnitMeleeValue.setText (new Integer (meleeAttack).toString ());
				selectedUnitMeleeAverage = getTextUtils ().insertDecimalPoint (meleeAttack * chanceToHitTimesFigures, 1);
				selectedUnitMeleeImage.setIcon (new ImageIcon (getUnitClientUtils ().getUnitAttributeIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)));
			}
			
			// Ranged attack / average hits / image
			final int rangedAttack = getUnitUtils ().getModifiedAttributeValue (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
				UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, getClient ().getPlayers (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
				getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ());
			
			if (rangedAttack <= 0)
			{
				selectedUnitRangedValue.setText (null);
				selectedUnitRangedImage.setIcon (null);
				selectedUnitRangedAverage = null;
			}
			else
			{
				selectedUnitRangedValue.setText (new Integer (rangedAttack).toString ());
				selectedUnitRangedAverage = getTextUtils ().insertDecimalPoint (rangedAttack * chanceToHitTimesFigures, 1);
				selectedUnitRangedImage.setIcon (new ImageIcon (getUnitClientUtils ().getUnitAttributeIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)));
			}
			
			// Movement
			selectedUnitMovement.setText (getTextUtils ().halfIntToStr (unit.getDoubleCombatMovesLeft ()));
			selectedUnitMovementImage.setIcon (new ImageIcon (getUtils ().loadImage
				(getClientUnitCalculations ().findPreferredMovementSkillGraphics (unit).getMovementIconImageFile ())));
			
			// Unit image
			selectedUnitImage.setIcon (new ImageIcon (getUtils ().loadImage
				(getGraphicsDB ().findUnit (unit.getUnitID (), "setSelectedUnitInCombat").getUnitOverlandImageFile ())));
		}
		
		languageOrSelectedUnitChanged ();
		
		log.trace ("Exiting setSelectedUnitInCombat");
	}
	
	/**
	 * Called when either the language or the currently selected unit changes, so we can show various stats about the current unit
	 */
	private final void languageOrSelectedUnitChanged ()
	{
		log.trace ("Entering languageOrSelectedUnitChanged");
		
		selectedUnitName.setText (null);
		selectedUnitMeleeAverageLabel.setText (null);
		selectedUnitRangedAverageLabel.setText (null);

		if (getSelectedUnitInCombat () != null)
			try
			{
				final String avg = getLanguage ().findCategoryEntry ("frmCombat", "AveragePrefix");
				
				if (selectedUnitMeleeAverage != null)
					selectedUnitMeleeAverageLabel.setText (avg + " " + selectedUnitMeleeAverage);
				
				if (selectedUnitRangedAverage != null)
					selectedUnitRangedAverageLabel.setText (avg + " " + selectedUnitRangedAverage);
				
				selectedUnitName.setText (getUnitClientUtils ().getUnitName (getSelectedUnitInCombat (), UnitNameType.RACE_UNIT_NAME));
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
				
		log.trace ("Exiting languageOrSelectedUnitChanged");
	}

	/**
	 * @return spell Spell chosen from spell book that we want to cast into this combat, and need to select a target for
	 */
	public final Spell getSpellBeingTargetted ()
	{
		return spellBeingTargetted;
	}
	
	/**
	 * Sets up prompt and cancel button to target a spell
	 * @param spell Spell chosen from spell book that we want to cast into this combat, and need to select a target for
	 */
	public final void setSpellBeingTargetted (final Spell spell)
	{
		log.trace ("Entering setSpellBeingTargetted: " + spell.getSpellID ());
		
		spellBeingTargetted = spell;
		
		// Find all the controls to use, depending on whether we're the attacker or defender
		final JLabel playerName;
		final JTextArea spellPrompt;
		final JButton spellCancel;
		
		if (getClient ().getOurPlayerID ().equals (players.getAttackingPlayer ().getPlayerDescription ().getPlayerID ()))
		{
			playerName = attackingPlayerName;
			spellPrompt = targetSpellPromptAttacker;
			spellCancel = cancelTargetSpellAttacker;
		}
		else
		{
			playerName = defendingPlayerName;
			spellPrompt = targetSpellPromptDefender;
			spellCancel = cancelTargetSpellDefender;
		}
		
		// Set up prompt
		final SpellLang spellLang = getLanguage ().findSpell (getSpellBeingTargetted ().getSpellID ());
		final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
		
		final SpellBookSectionLang section = getLanguage ().findSpellBookSection (spell.getSpellBookSectionID ());
		final String target = (section != null) ? section.getSpellTargetPrompt () : null;
		
		spellPrompt.setText ((target == null) ? ("Select target of type " + spell.getSpellBookSectionID ()) :
			(target.replaceAll ("SPELL_NAME", (spellName != null) ? spellName : getSpellBeingTargetted ().getSpellID ())));
		
		playerName.setVisible (false);
		spellCancel.setVisible (true);
		
		log.trace ("Exiting setSpellBeingTargetted");
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
	 * Careful with making updates to this since all the drawing is based on it.  Updates must be consistent with the current location of units, i.e. unit.setCombatPosition () 
	 * @return Units occupying each cell of the combat map
	 */
	public final MemoryUnit [] [] getUnitToDrawAtEachLocation ()
	{
		return unitToDrawAtEachLocation;
	}

	/**
	 * @return Max castable spell cost
	 */
	public final int getMaxCastable ()
	{
		return maxCastable;
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCombatLayoutMain ()
	{
		return combatLayoutMain;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCombatLayoutMain (final XmlLayoutContainerEx layout)
	{
		combatLayoutMain = layout;
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCombatLayoutBottom ()
	{
		return combatLayoutBottom;
	}
	
	/**
	 * @param layout XML layout
	 */
	public final void setCombatLayoutBottom (final XmlLayoutContainerEx layout)
	{
		combatLayoutBottom = layout;
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
	public final ClientUnitCalculations getClientUnitCalculations ()
	{
		return clientUnitCalculations;
	}

	/**
	 * @param calc Client unit calculations
	 */
	public final void setClientUnitCalculations (final ClientUnitCalculations calc)
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

	/**
	 * @return Combat map processing
	 */
	public final CombatMapProcessing getCombatMapProcessing ()
	{
		return combatMapProcessing;
	}

	/**
	 * @param proc Combat map processing
	 */
	public final void setCombatMapProcessing (final CombatMapProcessing proc)
	{
		combatMapProcessing = proc;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}

	/**
	 * @return Spell calculations
	 */
	public final SpellCalculations getSpellCalculations ()
	{
		return spellCalculations;
	}

	/**
	 * @param calc Spell calculations
	 */
	public final void setSpellCalculations (final SpellCalculations calc)
	{
		spellCalculations = calc;
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
	 * @return Help text scroll
	 */
	public final HelpUI getHelpUI ()
	{
		return helpUI;
	}

	/**
	 * @param ui Help text scroll
	 */
	public final void setHelpUI (final HelpUI ui)
	{
		helpUI = ui;
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
	 * @return Variable MP popup
	 */
	public VariableManaUI getVariableManaUI ()
	{
		return variableManaUI;
	}

	/**
	 * @param ui Variable MP popup
	 */
	public final void setVariableManaUI (final VariableManaUI ui)
	{
		variableManaUI = ui;
	}
	
	/**
	 * @return Colour to flash the combat screen when a CAE is being cast
	 */
	public final Color getFlashColour ()
	{
		return flashColour;
	}

	/**
	 * @param colour Colour to flash the combat screen when a CAE is being cast
	 */
	public final void setFlashColour (final Color colour)
	{
		flashColour = colour;
	}

	/**
	 * @return Animation to display for a spell being cast
	 */
	public final AnimationGfx getCombatCastAnimation ()
	{
		return combatCastAnimation;
	}

	/**
	 * @param an Animation to display for a spell being cast
	 */
	public final void setCombatCastAnimation (final AnimationGfx an)
	{
		combatCastAnimation = an;
	}

	/**
	 * @return Whether the combat case animation appears behind or in front of the units
	 */
	public boolean isCombatCastAnimationInFront ()
	{
		return combatCastAnimationInFront;
	}

	/**
	 * @param inFront Whether the combat case animation appears behind or in front of the units
	 */
	public final void setCombatCastAnimationInFront (final boolean inFront)
	{
		combatCastAnimationInFront = inFront;
	}

	/**
	 * @return Coords to display combat cast animation(s) at, in pixels
	 */	
	public final List<Point> getCombatCastAnimationPositions ()
	{
		return combatCastAnimationPositions;
	}
	
	/**
	 * @return Frame number to display of combat cast animation
	 */
	public final int getCombatCastAnimationFrame ()
	{
		return combatCastAnimationFrame;
	}
	
	/**
	 * @param frame Frame number to display of combat cast animation
	 */
	public final void setCombatCastAnimationFrame (final int frame)
	{
		combatCastAnimationFrame = frame;
	}

	/**
	 * @return Client-side spell utils
	 */
	public final SpellClientUtils getSpellClientUtils ()
	{
		return spellClientUtils;
	}

	/**
	 * @param utils Client-side spell utils
	 */
	public final void setSpellClientUtils (final SpellClientUtils utils)
	{
		spellClientUtils = utils;
	}
}
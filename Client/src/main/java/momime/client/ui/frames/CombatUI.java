package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;

import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.calculations.CombatMapBitmapGenerator;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.WizardCombatPlayList;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.TileType;
import momime.client.ui.MomUIConstants;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_5.MemoryGridCell;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomTransientPlayerPublicKnowledge;
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
	
	/** Image of static portion of the combat terrain */
	private BufferedImage staticTerrainImage;

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
		final BufferedImage calculatorButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/combat/calculatorButtonNormal.png");
		final BufferedImage calculatorButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/combat/calculatorButtonPressed.png");
		
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
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = 7609379654619244164L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Draw the static portion of the terrain
				g.drawImage (staticTerrainImage, 0, 0, null);
				
				// Draw the buttons panel background at the bottom of the window
				g.drawImage (background, 0, getHeight () - (background.getHeight () * 2), background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getCombatLayout ()));
		
		// Buttons
		contentPane.add (getUtils ().createImageButton (spellAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmCombatSpell");
		contentPane.add (getUtils ().createImageButton (waitAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmCombatWait");
		contentPane.add (getUtils ().createImageButton (doneAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmCombatDone");
		contentPane.add (getUtils ().createImageButton (fleeAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmCombatFlee");
		contentPane.add (getUtils ().createImageButton (autoAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal), "frmCombatAuto");
		
		contentPane.add (getUtils ().createImageButton (toggleDamageCalculationsAction, null, null, null, calculatorButtonNormal, calculatorButtonPressed, calculatorButtonNormal), "frmCombatToggleDamageCalculations");
		
		// Player names (colour gets set in initNewCombat once we know who the players actually are)
		defendingPlayerName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (defendingPlayerName, "frmCombatDefendingPlayer");
		
		attackingPlayerName = getUtils ().createShadowedLabel (Color.BLACK, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (attackingPlayerName, "frmCombatAttackingPlayer");
		
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

		// Generates the bitmap for the static portion of the terrain
		getCombatMapBitmapGenerator ().smoothMapTerrain (getCombatLocation (), getCombatTerrain ());
		staticTerrainImage = getCombatMapBitmapGenerator ().generateCombatMapBitmap ();
		
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
}
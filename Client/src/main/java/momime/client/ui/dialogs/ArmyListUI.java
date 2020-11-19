package momime.client.ui.dialogs;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.ndg.utils.Holder;

import momime.client.MomClient;
import momime.client.calculations.MiniMapBitmapGenerator;
import momime.client.languages.database.Shortcut;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.UnitInfoUI;
import momime.client.ui.renderer.ArmyListCellRenderer;
import momime.client.utils.TextUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.UnitUtils;

/**
 * UI for the army list, which shows one line per unit stack on the overland map
 */
public final class ArmyListUI extends MomClientDialogUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (ArmyListUI.class);

	/** XML layout */
	private XmlLayoutContainerEx armyListLayout;

	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Minimap generator */
	private MiniMapBitmapGenerator miniMapBitmapGenerator;
	
	/** Text utils */
	private TextUtils textUtils;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** Title */
	private JLabel title;
	
	/** Hero items action */
	private Action heroItemsAction;
	
	/** OK action */
	private Action okAction;
	
	/** Items in the unit stacks list box */
	private DefaultListModel<Entry<MapCoordinates3DEx, List<MemoryUnit>>> unitStacksItems; 
	
	/** Unit stacks list box */
	private JList<Entry<MapCoordinates3DEx, List<MemoryUnit>>> unitStacksList;
	
	/** Cell renderer */
	private ArmyListCellRenderer armyListCellRenderer;
	
	/** Minimap panel */
	private JPanel miniMapPanel;
	
	/** Bitmaps for the mini map; one has a white dot and the other doesn't */
	private final BufferedImage [] miniMapBitmaps = new BufferedImage [2];
	
	/** Which frame of the minimap "animation" is currently being drawn */
	private int miniMapFrame = 0;
	
	/** Gold upkeep for entire army */
	private JLabel goldUpkeepLabel;

	/** Mana upkeep for entire army */
	private JLabel manaUpkeepLabel;

	/** Rations upkeep for entire army */
	private JLabel rationsUpkeepLabel;
	
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
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/armyList.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button82x30goldBorderNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button82x30goldBorderPressed.png");
		
		// We need any overland unit image to know the size of it
		final String firstUnitID = getClient ().getClientDB ().getUnits ().get (0).getUnitID ();
		final BufferedImage firstUnitImage = getUtils ().loadImage (getClient ().getClientDB ().findUnit (firstUnitID, "ArmyListUI").getUnitOverlandImageFile ());
		
		// Actions
		heroItemsAction = new LoggingAction ((ev) -> getHeroItemsUI ().setVisible (true));
		okAction = new LoggingAction ((ev) -> getDialog ().setVisible (false));
		
		// Initialize the content pane
		contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getArmyListLayout ()));
		
		title = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (title, "frmArmyListTitle");
		
		final JButton heroItemsButton = getUtils ().createImageButton (heroItemsAction, MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getLargeFont (),
			buttonNormal, buttonPressed, buttonNormal);
		contentPane.add (heroItemsButton, "frmArmyListHeroItems");
		
		final JButton okButton = getUtils ().createImageButton (okAction, MomUIConstants.DULL_GOLD, MomUIConstants.GOLD, getLargeFont (),
			buttonNormal, buttonPressed, buttonNormal);
		contentPane.add (okButton, "frmArmyListOK");

		goldUpkeepLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (goldUpkeepLabel, "frmArmyListUpkeepGold");

		manaUpkeepLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (manaUpkeepLabel, "frmArmyListUpkeepMana");

		rationsUpkeepLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (rationsUpkeepLabel, "frmArmyListUpkeepRations");
		
		// Set up cell renderer
		getArmyListCellRenderer ().init ();
		
		// Set up the list
		unitStacksItems = new DefaultListModel<Entry<MapCoordinates3DEx, List<MemoryUnit>>> ();
		unitStacksList = new JList<Entry<MapCoordinates3DEx, List<MemoryUnit>>> ();
		unitStacksList.setOpaque (false);
		unitStacksList.setModel (unitStacksItems);
		unitStacksList.setCellRenderer (getArmyListCellRenderer ());
		unitStacksList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);

		final JScrollPane spellsScroll = getUtils ().createTransparentScrollPane (unitStacksList);
		spellsScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellsScroll, "frmArmyListUnitStacks");
			
		// Minimap view
		final XmlLayoutComponent miniMapSize = getArmyListLayout ().findComponent ("frmArmyListMiniMap");
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
		contentPane.add (miniMapPanel, "frmArmyListMiniMap");
		
		// Flash the white dot on the minimap
		new Timer (250, (ev) ->
		{
			miniMapFrame = 1 - miniMapFrame;
			miniMapPanel.repaint ();
		}).start ();
		
		// Update the map as we click on different unit stacks
		unitStacksList.addListSelectionListener ((ev) ->
		{
			try
			{
				regenerateMiniMapBitmaps ();
			}
			catch (final IOException e)
			{
				log.error (e, e);
			}
		});
		
		// Right clicks open up the unit info panel
		unitStacksList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					final int row = unitStacksList.locationToIndex (ev.getPoint ());
					if ((row >= 0) && (row < unitStacksItems.size ()))
					{
						final Rectangle rect = unitStacksList.getCellBounds (row, row);
						if (rect.contains (ev.getPoint ()))
						{
							// Work out the coordinates within the list cell, and adjust for the position of the units
							final int x = ev.getX () - (int) rect.getMinX () - ArmyListCellRenderer.LEFT_BORDER;
							final int y = ev.getY () - (int) rect.getMinY () - ArmyListCellRenderer.TOP_BORDER;
							
							final List<MemoryUnit> unitStack = unitStacksItems.get (row).getValue ();
							if ((x >= 0) && (y >= 0) && (y < firstUnitImage.getHeight () * 2) &&
								(x < firstUnitImage.getWidth () * 2 * unitStack.size ()))
							{
								final int unitNo = x / (firstUnitImage.getWidth () * 2);
								final MemoryUnit unit = unitStack.get (unitNo);
			
								// Is there a unit info screen already open for this unit?
								UnitInfoUI unitInfo = getClient ().getUnitInfos ().get (unit.getUnitURN ());
								if (unitInfo == null)
								{
									unitInfo = getPrototypeFrameCreator ().createUnitInfo ();
									unitInfo.setUnit (unit);
									getClient ().getUnitInfos ().put (unit.getUnitURN (), unitInfo);
								}
							
								try
								{
									unitInfo.setVisible (true);
								}
								catch (final IOException e)
								{
									log.error (e, e);
								}
							}
						}
					}
				}
			}
		});

		// Initialize the list
		refreshArmyList (null);
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		getDialog ().setUndecorated (true);
		
		getDialog ().setShape (new Polygon
			(new int [] {10, 6, 6, 10, 0, 0,		830, 830, 820, 824, 824, 820,		820, 824, 824, 820, 830, 830,		0, 0, 10, 6, 6, 10},
			new int [] {38, 38, 30, 30, 10, 0,	0, 10, 30, 30, 38, 38,					378, 378, 386, 386, 406, 416,		416, 406, 386, 386, 378, 378},
			24));		
		
		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.HERO_ITEMS, heroItemsAction);
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Refreshes the list of units
	 * @param selectLocation Location to show units of after we refresh the list; can pass as null if we've no preference which to select
	 * @throws IOException If there is a problem finding any of the necessary data
	 */
	public final void refreshArmyList (final MapCoordinates3DEx selectLocation) throws IOException
	{
		log.trace ("Entering refreshArmyList");
		
		if (unitStacksItems != null)
		{
			// Generate a map containing all of our units at every location on both planes,
			// and also total up all the upkeep our units are taking up.
			final Map<MapCoordinates3DEx, List<MemoryUnit>> unitStacksMap = new HashMap<MapCoordinates3DEx, List<MemoryUnit>> ();
			final Map<String, Integer> upkeepsMap = new HashMap<String, Integer> ();
			
			for (final MemoryUnit thisUnit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				if ((thisUnit.getStatus () == UnitStatusID.ALIVE) && (thisUnit.getOwningPlayerID () == getClient ().getOurPlayerID ()))
				{
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null,
						getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
					
					// Add to stack, or create a new one
					List<MemoryUnit> unitStack = unitStacksMap.get (thisUnit.getUnitLocation ());
					if (unitStack == null)
					{
						unitStack = new ArrayList<MemoryUnit> ();
						unitStacksMap.put ((MapCoordinates3DEx) thisUnit.getUnitLocation (), unitStack);
					}
					unitStack.add (thisUnit);
					
					// Total up upkeep
					for (final String productionTypeID : xu.listModifiedUpkeepProductionTypeIDs ())
					{
						Integer value = upkeepsMap.get (productionTypeID);
						if (value == null)
							value = 0;
						
						value = value + xu.getModifiedUpkeepValue (productionTypeID);						
						upkeepsMap.put (productionTypeID, value);
					}
				}
			
			// Now take all the entries in the map and sort them, with the biggest stacks first.
			// While we're at it, see if we can spot the index of the row we want to select.
			unitStacksItems.clear ();
			final Holder<Integer> index = new Holder<Integer> ();
			
			unitStacksMap.entrySet ().stream ().sorted ((e1, e2) -> e2.getValue ().size () - e1.getValue ().size ()).forEach ((e) ->
			{
				if (e.getKey ().equals (selectLocation))
					index.setValue (unitStacksItems.size ());

				unitStacksItems.addElement (e);
			});
			
			// Select a unit stack from the list
			if (index.getValue () != null)
				unitStacksList.setSelectedIndex (index.getValue ());
			else if (unitStacksItems.size () > 0)
				unitStacksList.setSelectedIndex (0);
			
			regenerateMiniMapBitmaps ();
			
			// Add on upkeep of spells - but only spells cast on our own units
			for (final MemoryMaintainedSpell spell : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell ())
				if ((spell.getUnitURN () != null) && (spell.getCastingPlayerID () == getClient ().getOurPlayerID ()) &&
					(getUnitUtils ().findUnitURN (spell.getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (),
						"refreshArmyList").getOwningPlayerID () == getClient ().getOurPlayerID ()))
				{
					final Spell spellDetails = getClient ().getClientDB ().findSpell (spell.getSpellID (), "refreshArmyList");

					for (final ProductionTypeAndUndoubledValue upkeep : spellDetails.getSpellUpkeep ())
					{
						Integer value = upkeepsMap.get (upkeep.getProductionTypeID ());
						if (value == null)
							value = 0;
						
						value = value + upkeep.getUndoubledProductionValue ();
						
						upkeepsMap.put (upkeep.getProductionTypeID (), value);
					}
				}
			
			// Show upkeep numbers
			final Integer goldUpkeep = upkeepsMap.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
			goldUpkeepLabel.setText ((goldUpkeep == null) ? "" : getTextUtils ().intToStrCommas (goldUpkeep));

			Integer manaUpkeep = upkeepsMap.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
			if ((manaUpkeep != null) && (manaUpkeep > 1))
			{
				// Do we have channeler, to halve all upkeep?
				final PlayerPublicDetails unitOwner = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "refreshArmyList");
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) unitOwner.getPersistentPlayerPublicKnowledge ();

				if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER) >= 1)
					manaUpkeep = manaUpkeep - (manaUpkeep / 2);
			}			
			manaUpkeepLabel.setText ((manaUpkeep == null) ? "" : getTextUtils ().intToStrCommas (manaUpkeep));

			final Integer rationsUpkeep = upkeepsMap.get (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RATIONS);
			rationsUpkeepLabel.setText ((rationsUpkeep == null) ? "" : getTextUtils ().intToStrCommas (rationsUpkeep));
		}
		
		log.trace ("Ending refreshArmyList");
	}

	/**
	 * Generates bitmap of the entire overland map with only 1 pixel per terrain file, to display in the mini map area in the top right corner.
	 * @throws IOException If there is a problem finding any of the necessary data
	 */
	public final void regenerateMiniMapBitmaps () throws IOException
	{
		log.trace ("Entering regenerateMiniMapBitmaps");
		
		if ((unitStacksList != null) && (unitStacksList.getSelectedIndex () >= 0))
		{
			final MapCoordinates3DEx coords = unitStacksItems.get (unitStacksList.getSelectedIndex ()).getKey ();
			
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
		
		log.trace ("Exiting regenerateMiniMapBitmaps");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		// Title containing player name
		String titleText = getLanguageHolder ().findDescription (getLanguages ().getArmyListScreen ().getTitle ());
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID ());
		if (ourPlayer != null)
			titleText = titleText.replaceAll ("PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourPlayer));
		
		title.setText (titleText);
		getDialog ().setTitle (titleText);
		
		// Buttons
		heroItemsAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getArmyListScreen ().getHeroItems ()));
		okAction.putValue				(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
		
		// Shortcut keys
		getLanguageHolder ().configureShortcutKeys (contentPane);
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getArmyListLayout ()
	{
		return armyListLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setArmyListLayout (final XmlLayoutContainerEx layout)
	{
		armyListLayout = layout;
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
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Cell renderer
	 */
	public final ArmyListCellRenderer getArmyListCellRenderer ()
	{
		return armyListCellRenderer;
	}

	/**
	 * @param rend Cell renderer
	 */
	public final void setArmyListCellRenderer (final ArmyListCellRenderer rend)
	{
		armyListCellRenderer = rend;
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
	 * @return Hero items UI
	 */
	public final HeroItemsUI getHeroItemsUI ()
	{
		return heroItemsUI;
	}

	/**
	 * @param ui Hero items UI
	 */
	public final void setHeroItemsUI (final HeroItemsUI ui)
	{
		heroItemsUI = ui;
	}
}
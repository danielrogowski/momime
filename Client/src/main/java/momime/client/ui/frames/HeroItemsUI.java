package momime.client.ui.frames;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.ShortcutKeyLang;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.draganddrop.TransferableFactory;
import momime.client.ui.draganddrop.TransferableHeroItem;
import momime.client.ui.renderer.HeroTableCellRenderer;
import momime.client.ui.renderer.UnassignedHeroItemCellRenderer;
import momime.client.utils.TextUtils;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlotType;
import momime.common.database.HeroSlotAllowedItemType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Shortcut;
import momime.common.database.Unit;
import momime.common.messages.MemoryUnit;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.HeroItemLocationID;
import momime.common.messages.clienttoserver.RequestMoveHeroItemMessage;
import momime.common.utils.ResourceValueUtils;

/**
 * Screen for redistributing items between heroes, the bank vault, or destroying them on the anvil for MP
 */
public final class HeroItemsUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (HeroItemsUI.class);

	/** XML layout */
	private XmlLayoutContainerEx heroItemsLayout;

	/** Small font */
	private Font smallFont;
	
	/** Medium font */
	private Font mediumFont;

	/** Large font */
	private Font largeFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Drag and drop factory */
	private TransferableFactory transferableFactory;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Alchemy UI */
	private AlchemyUI alchemyUI;
	
	/** The data flavour for hero items */
	private DataFlavor heroItemFlavour;
	
	/** Bank title */
	private JLabel bankTitle;

	/** Items not assigned to any hero */
	private DefaultListModel<NumberedHeroItem> bankItems; 
	
	/** Items not assigned to any hero */
	private JList<NumberedHeroItem> bankList;
	
	/** Bank list cell renderer */
	private UnassignedHeroItemCellRenderer unassignedHeroItemCellRenderer;

	/** Hero table cell renderer */
	private HeroTableCellRenderer heroTableCellRenderer;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;

	/** Help text scroll */
	private HelpUI helpUI;
	
	/** Heroes in the grid */
	private HeroesTableModel heroesTableModel;

	/** Alchemy action */
	private Action alchemyAction;
	
	/** OK action */
	private Action okAction;

	/** Partially filled out movement message - created and the "from" parts filled out when we start a drag - completed and sent when we end a drop */
	private RequestMoveHeroItemMessage requestMoveHeroItemMessage;

	/** Content pane */
	private JPanel contentPane;
	
	/** Gold amount stored */
	private JLabel goldAmountStored;
	
	/** Mana amount stored */
	private JLabel manaAmountStored;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");

		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/heroItems/background.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button118x30Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button118x30Pressed.png");
		
		// Actions
		alchemyAction = new LoggingAction ((ev) -> getAlchemyUI ().setVisible (true));
		okAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
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
		contentPane.setLayout (new XmlLayoutManager (getHeroItemsLayout ()));
		
		bankTitle = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (bankTitle, "frmHeroItemsBankLabel");

		final JButton alchemyButton = getUtils ().createImageButton (alchemyAction, MomUIConstants.GRAY, MomUIConstants.SILVER, getMediumFont (),
			buttonNormal, buttonPressed, buttonNormal);
		contentPane.add (alchemyButton, "frmHeroItemsAlchemy");
		
		final JButton okButton = getUtils ().createImageButton (okAction, MomUIConstants.GRAY, MomUIConstants.SILVER, getMediumFont (),
			buttonNormal, buttonPressed, buttonNormal);
		contentPane.add (okButton, "frmHeroItemsOK");

		goldAmountStored = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (goldAmountStored, "frmHeroItemsGoldStored");
		
		manaAmountStored = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (manaAmountStored, "frmHeroItemsManaStored");
		
		// Dragging items onto the anvil destroys them and gets MP back
		final JPanel anvil = new JPanel ();
		anvil.setOpaque (false);
		contentPane.add (anvil, "frmHeroItemsAnvil");
		
		anvil.setTransferHandler (new TransferHandler ()
		{
			@Override
			public final boolean canImport (final TransferSupport support)
			{
				return support.getTransferable ().isDataFlavorSupported (getHeroItemFlavour ());
			}
			
			@Override
			public final boolean importData (final TransferSupport support)
			{
				boolean imported = false;
				if ((requestMoveHeroItemMessage != null) && (canImport (support)))
					try
					{
						final NumberedHeroItem item = (NumberedHeroItem) support.getTransferable ().getTransferData (getHeroItemFlavour ());
						final int manaGained = getHeroItemCalculations ().calculateCraftingCost (item, getClient ().getClientDB ()) / 2;

						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setTitleLanguageCategoryID ("frmHeroItems");
						msg.setTitleLanguageEntryID ("DestroyTitle");
						msg.setText (getLanguage ().findCategoryEntry ("frmHeroItems", "Destroy").replaceAll
							("ITEM_NAME", item.getHeroItemName ()).replaceAll
							("MANA_GAINED", getTextUtils ().intToStrCommas (manaGained)));
						msg.setDestroyHeroItemMessage (requestMoveHeroItemMessage);
						msg.setVisible (true);
						
						requestMoveHeroItemMessage = null;
						imported = true;
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				return imported;
			}
		});
		
		// Set up renderers
		getUnassignedHeroItemCellRenderer ().init ();
		getHeroTableCellRenderer ().init ();
		
		// Set up the list
		bankItems = new DefaultListModel<NumberedHeroItem> ();
		bankList = new JList<NumberedHeroItem> ();
		bankList.setOpaque (false);
		bankList.setModel (bankItems);
		bankList.setCellRenderer (getUnassignedHeroItemCellRenderer ());
		bankList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);

		final JScrollPane spellsScroll = getUtils ().createTransparentScrollPane (bankList);
		spellsScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellsScroll, "frmHeroItemsBank");

		// Dragging items from the unassigned list
		DragSource.getDefaultDragSource ().createDefaultDragGestureRecognizer (bankList, DnDConstants.ACTION_MOVE, (ev) ->
		{
			final int index = bankList.locationToIndex (ev.getDragOrigin ());
			if ((index >= 0) && (index < bankItems.size ()) && (bankList.getCellBounds (index, index).contains (ev.getDragOrigin ())))
				try
				{
					final NumberedHeroItem item = bankItems.get (index);
					
					// Create a mouse cursor that looks like the chosen item
					final HeroItemTypeGfx itemGfx = getGraphicsDB ().findHeroItemType (item.getHeroItemTypeID (), "HeroItemsUI");
					final BufferedImage itemImage = getUtils ().loadImage (itemGfx.getHeroItemTypeImageFile ().get (item.getHeroItemImageNumber ()));
					final Cursor cursor = Toolkit.getDefaultToolkit ().createCustomCursor
						(getUtils ().doubleSize (itemImage), new Point (itemImage.getWidth (), itemImage.getHeight ()), item.getHeroItemName ());
					
					// Keep a note of where we dragged it from, and which item it is
					requestMoveHeroItemMessage = new RequestMoveHeroItemMessage ();
					requestMoveHeroItemMessage.setFromLocation (HeroItemLocationID.UNASSIGNED);
					requestMoveHeroItemMessage.setHeroItemURN (item.getHeroItemURN ());
					
					// Initiate drag and drop
					final TransferableHeroItem trans = getTransferableFactory ().createTransferableHeroItem ();
					trans.setHeroItem (item);
					ev.startDrag (cursor, trans);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
		});

		// Dragging items to the unassigned list
		bankList.setTransferHandler (new TransferHandler ()
		{
			@Override
			public final boolean canImport (final TransferSupport support)
			{
				return support.getTransferable ().isDataFlavorSupported (getHeroItemFlavour ());
			}
			
			@Override
			public final boolean importData (final TransferSupport support)
			{
				boolean imported = false;
				if ((requestMoveHeroItemMessage != null) && (canImport (support)))
					try
					{
						// Moving to bank is always OK
						requestMoveHeroItemMessage.setToLocation (HeroItemLocationID.UNASSIGNED);
						getClient ().getServerConnection ().sendMessageToServer (requestMoveHeroItemMessage);
						imported = true;

						requestMoveHeroItemMessage = null;
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				return imported;
			}
		});
		
		// Right clicks open up item info panel
		bankList.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					final int index = bankList.locationToIndex (ev.getPoint ());
					if ((index >= 0) && (index < bankItems.size ()) && (bankList.getCellBounds (index, index).contains (ev.getPoint ())))
						try
						{
							final NumberedHeroItem item = bankItems.get (index);
							if (item != null)
							{
								// Is there an item info screen already open for this item?
								HeroItemInfoUI itemInfo = getClient ().getHeroItemInfos ().get (item.getHeroItemURN ());
								if (itemInfo == null)
								{
									itemInfo = getPrototypeFrameCreator ().createHeroItemInfo ();
									itemInfo.setItem (item);
									getClient ().getHeroItemInfos ().put (item.getHeroItemURN (), itemInfo);
								}
								itemInfo.setVisible (true);
							}
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
				}
			}
		});
		
		// Heroes grid
		heroesTableModel = new HeroesTableModel ();
		final JTable heroesTable = new JTable ();
		heroesTable.setOpaque (false);
		heroesTable.setModel (heroesTableModel);
		heroesTable.setDefaultRenderer (MemoryUnit.class, getHeroTableCellRenderer ());
		heroesTable.setRowHeight (getUtils ().loadImage ("/momime.client.graphics/ui/heroItems/heroItemsGridCell.png").getHeight ());
		heroesTable.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		heroesTable.setTableHeader (null);
		heroesTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
		heroesTable.getColumnModel ().getColumn (0).setPreferredWidth (270);
		heroesTable.getColumnModel ().getColumn (1).setPreferredWidth (262);

		final JScrollPane heroesScrollPane = getUtils ().createTransparentScrollPane (heroesTable);
		heroesScrollPane.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (heroesScrollPane, "frmHeroItemsHeroes");

		// Dragging items from heroes
		DragSource.getDefaultDragSource ().createDefaultDragGestureRecognizer (heroesTable, DnDConstants.ACTION_MOVE, (ev) ->
		{
			final int rowIndex = heroesTable.rowAtPoint (ev.getDragOrigin ());
			final int columnIndex = heroesTable.columnAtPoint (ev.getDragOrigin ());
			final int index = (rowIndex * 2) + columnIndex;
			if ((index >= 0) && (index < heroesTableModel.getUnits ().size ()))
				try
				{
					final MemoryUnit dragUnit = heroesTableModel.getUnits ().get (index);

					// Now check the exact point within this unit that the mouse it at
					final Rectangle cellRect = heroesTable.getCellRect (rowIndex, columnIndex, false);
					final int x = ev.getDragOrigin ().x - cellRect.x;
					final int y = ev.getDragOrigin ().y - cellRect.y;
					final int slotNumber = getHeroTableCellRenderer ().getSlotNumberAt (x, y);
					if ((slotNumber >= 0) && (slotNumber < dragUnit.getHeroItemSlot ().size ()))
					{
						// Is there an item in the slot to drag?
						final NumberedHeroItem item = dragUnit.getHeroItemSlot ().get (slotNumber).getHeroItem ();
						if (item != null)
						{
							// Create a mouse cursor that looks like the chosen item
							final HeroItemTypeGfx itemGfx = getGraphicsDB ().findHeroItemType (item.getHeroItemTypeID (), "HeroItemsUI");
							final BufferedImage itemImage = getUtils ().loadImage (itemGfx.getHeroItemTypeImageFile ().get (item.getHeroItemImageNumber ()));
							final Cursor cursor = Toolkit.getDefaultToolkit ().createCustomCursor
								(getUtils ().doubleSize (itemImage), new Point (itemImage.getWidth (), itemImage.getHeight ()), item.getHeroItemName ());
							
							// Keep a note of where we dragged it from, and which item it is
							requestMoveHeroItemMessage = new RequestMoveHeroItemMessage ();
							requestMoveHeroItemMessage.setFromLocation (HeroItemLocationID.HERO);
							requestMoveHeroItemMessage.setFromUnitURN (dragUnit.getUnitURN ());
							requestMoveHeroItemMessage.setFromSlotNumber (slotNumber);
							requestMoveHeroItemMessage.setHeroItemURN (item.getHeroItemURN ());
							
							// Initiate drag and drop
							final TransferableHeroItem trans = getTransferableFactory ().createTransferableHeroItem ();
							trans.setHeroItem (item);
							ev.startDrag (cursor, trans);
						}
					}
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
		});
		
		// Handle dragging items onto heroes
		heroesTable.setTransferHandler (new TransferHandler ()
		{
			/**
			 * @return Whether the location the mouse was released on within the heroes grid can accept a dragged item or not
			 */
			@Override
			public final boolean canImport (final TransferSupport support)
			{
				boolean imported = false;
				if (support.getTransferable ().isDataFlavorSupported (getHeroItemFlavour ()))
					try
					{
						final JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation ();
						final int index = (dropLocation.getRow () * 2) + dropLocation.getColumn ();
						if ((index >= 0) && (index < heroesTableModel.getUnits ().size ()))
						{
							final MemoryUnit dropUnit = heroesTableModel.getUnits ().get (index);
							
							// Now check the exact point within this unit that the mouse it at
							final Rectangle cellRect = heroesTable.getCellRect (dropLocation.getRow (), dropLocation.getColumn (), false);
							final int x = dropLocation.getDropPoint ().x - cellRect.x;
							final int y = dropLocation.getDropPoint ().y - cellRect.y;
							final int slotNumber = getHeroTableCellRenderer ().getSlotNumberAt (x, y);
							if (slotNumber >= 0)
								
								// Is there an item already in the slot?
								if ((slotNumber < dropUnit.getHeroItemSlot ().size ()) && (dropUnit.getHeroItemSlot ().get (slotNumber).getHeroItem () == null))
								{
									// Is the item being dragged appropriate for the slot type?
									final Unit unitDef = getClient ().getClientDB ().findUnit (dropUnit.getUnitID (), "HeroItemsUI-canImport");
									if (slotNumber < unitDef.getHeroItemSlot ().size ())
									{
										final String slotTypeID = unitDef.getHeroItemSlot ().get (slotNumber).getHeroItemSlotTypeID ();
										final HeroItemSlotType slotType = getClient ().getClientDB ().findHeroItemSlotType (slotTypeID, "HeroItemsUI-canImport");
										final NumberedHeroItem item = (NumberedHeroItem) support.getTransferable ().getTransferData (getHeroItemFlavour ());
										
										for (final HeroSlotAllowedItemType allowed : slotType.getHeroSlotAllowedItemType ())
											if (allowed.getHeroItemTypeID ().equals (item.getHeroItemTypeID ()))
												imported = true;
									}
								}
						}
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				
				return imported;
			}
			
			/**
			 * Handles actually dragging an item onto a hero (either from another hero, or the bank of unassigned items)
			 */
			@Override
			public final boolean importData (final TransferSupport support)
			{
				boolean imported = false;
				if ((requestMoveHeroItemMessage != null) && (canImport (support)))
					try
					{
						final JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation ();
						final int index = (dropLocation.getRow () * 2) + dropLocation.getColumn ();
						if ((index >= 0) && (index < heroesTableModel.getUnits ().size ()))
						{
							final MemoryUnit dropUnit = heroesTableModel.getUnits ().get (index);

							// Now check the exact point within this unit that the mouse was released
							final Rectangle cellRect = heroesTable.getCellRect (dropLocation.getRow (), dropLocation.getColumn (), false);
							final int x = dropLocation.getDropPoint ().x - cellRect.x;
							final int y = dropLocation.getDropPoint ().y - cellRect.y;
							final int slotNumber = getHeroTableCellRenderer ().getSlotNumberAt (x, y);
							if (slotNumber >= 0)
							{
								// We've already checked that its allowed and filled out the "from" parts, so just send the message
								requestMoveHeroItemMessage.setToLocation (HeroItemLocationID.HERO);
								requestMoveHeroItemMessage.setToUnitURN (dropUnit.getUnitURN ());
								requestMoveHeroItemMessage.setToSlotNumber (slotNumber);
								getClient ().getServerConnection ().sendMessageToServer (requestMoveHeroItemMessage);

								requestMoveHeroItemMessage = null;
								imported = true;
							}
						}
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				return imported;
			}
		});
		
		// Right clicks open up the unit info panel, or the item info panel
		heroesTable.addMouseListener (new MouseAdapter ()
		{
			@Override
			public final void mouseClicked (final MouseEvent ev)
			{
				if (SwingUtilities.isRightMouseButton (ev))
				{
					final int rowIndex = heroesTable.rowAtPoint (ev.getPoint ());
					final int columnIndex = heroesTable.columnAtPoint (ev.getPoint ());
					final int index = (rowIndex * 2) + columnIndex;
					if ((index >= 0) && (index < heroesTableModel.getUnits ().size ()))
						try
						{
							final MemoryUnit unit = heroesTableModel.getUnits ().get (index);

							// Now check the exact point within this unit that the mouse it at
							final Rectangle cellRect = heroesTable.getCellRect (rowIndex, columnIndex, false);
							final int x = ev.getPoint ().x - cellRect.x;
							final int y = ev.getPoint ().y - cellRect.y;
							final int slotNumber = getHeroTableCellRenderer ().getSlotNumberAt (x, y);
							if ((slotNumber >= 0) && (slotNumber < unit.getHeroItemSlot ().size ()))
							{
								// Is there an item in the slot to view?
								final NumberedHeroItem item = unit.getHeroItemSlot ().get (slotNumber).getHeroItem ();
								if (item == null)
								{
									// Show info about the slot type
									final Unit unitDef = getClient ().getClientDB ().findUnit (unit.getUnitID (), "HeroItemsUI-slotHelp");
									if (slotNumber < unitDef.getHeroItemSlot ().size ())
										getHelpUI ().showHeroItemSlotTypeID (unitDef.getHeroItemSlot ().get (slotNumber).getHeroItemSlotTypeID ());
								}
								else
								{
									// Is there an item info screen already open for this item?
									HeroItemInfoUI itemInfo = getClient ().getHeroItemInfos ().get (item.getHeroItemURN ());
									if (itemInfo == null)
									{
										itemInfo = getPrototypeFrameCreator ().createHeroItemInfo ();
										itemInfo.setItem (item);
										getClient ().getHeroItemInfos ().put (item.getHeroItemURN (), itemInfo);
									}
									itemInfo.setVisible (true);
								}
							}
							else if (getHeroTableCellRenderer ().isWithinHeroPortrait (x, y))
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
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
				}
			}
		});
		
		// Load the initial lists
		refreshHeroes ();
		refreshItemsBank ();
		updateGlobalEconomyValues ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		
		getFrame ().setShape (new Polygon
			(new int [] {0, 798, 798, 570, 570, 182, 182, 0},
			new int [] {0, 0, 304, 304, 394, 394, 304, 304},
			8));
		
		// Shortcut keys
		contentPane.getActionMap ().put (Shortcut.ALCHEMY, alchemyAction);
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Refresh list of heroes
	 * @throws RecordNotFoundException If we can't find the unit definition of one of our units
	 */
	public final void refreshHeroes () throws RecordNotFoundException
	{
		log.trace ("Entering refreshHeroes");

		if (heroesTableModel != null)
		{
			heroesTableModel.getUnits ().clear ();
			for (final MemoryUnit thisUnit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
				if ((thisUnit.getOwningPlayerID () == getClient ().getOurPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
					(getClient ().getClientDB ().findUnit (thisUnit.getUnitID (), "refreshHeroes").getUnitMagicRealm ().equals
						(CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO)))
					
					heroesTableModel.getUnits ().add (thisUnit);
			
			heroesTableModel.fireTableDataChanged ();
		}
		
		log.trace ("Exiting refreshHeroes");
	}
	
	/**
	 * Refresh list of items in bank
	 */
	public final void refreshItemsBank ()
	{
		log.trace ("Entering refreshItemsBank");

		if (bankItems != null)
		{
			bankItems.clear ();
			for (final NumberedHeroItem item : getClient ().getOurPersistentPlayerPrivateKnowledge ().getUnassignedHeroItem ())
				bankItems.addElement (item);
		}
		
		log.trace ("Exiting refreshItemsBank");
	}
	
	/**
	 * Updates one 'stored' global economy value
	 * 
	 * @param label Label to update
	 * @param productionTypeID Production type to display in this label
	 */
	private final void updateAmountStored (final JLabel label, final String productionTypeID)
	{
		// Resource values get sent to us during game startup before the screen has been set up, so its possible to get here before the labels even exist
		if (label != null)
		{
			String amountStored = getTextUtils ().intToStrCommas (getResourceValueUtils ().findAmountStoredForProductionType
				(getClient ().getOurPersistentPlayerPrivateKnowledge ().getResourceValue (), productionTypeID));
		
			final ProductionTypeLang productionType = getLanguage ().findProductionType (productionTypeID);
			if ((productionType != null) && (productionType.getProductionTypeSuffix () != null))
				amountStored = amountStored + " " + productionType.getProductionTypeSuffix ();
			
			label.setText (amountStored);
		}
	}
	
	/**
	 * Updates the form when our resource values change 
	 */
	public final void updateGlobalEconomyValues ()
	{
		log.trace ("Entering updateGlobalEconomyValues");
		
		updateAmountStored (goldAmountStored, CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		updateAmountStored (manaAmountStored, CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		
		log.trace ("Exiting updateGlobalEconomyValues");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmHeroItems", "Title"));
		bankTitle.setText (getLanguage ().findCategoryEntry ("frmHeroItems", "Bank"));

		// Buttons
		alchemyAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmHeroItems", "Alchemy"));
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmHeroItems", "OK"));
	
		// Shortcut keys
		contentPane.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).clear ();
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
		
		// GP or MP suffix may have changed
		updateGlobalEconomyValues ();
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getHeroItemsLayout ()
	{
		return heroItemsLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setHeroItemsLayout (final XmlLayoutContainerEx layout)
	{
		heroItemsLayout = layout;
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
	 * @return Drag and drop factory
	 */
	public final TransferableFactory getTransferableFactory ()
	{
		return transferableFactory;
	}

	/**
	 * @param fac Drag and drop factory
	 */
	public final void setTransferableFactory (final TransferableFactory fac)
	{
		transferableFactory = fac;
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
	 * @return Alchemy UI
	 */
	public final AlchemyUI getAlchemyUI ()
	{
		return alchemyUI;
	}

	/**
	 * @param ui Alchemy UI
	 */
	public final void setAlchemyUI (final AlchemyUI ui)
	{
		alchemyUI = ui;
	}
	
	/**
	 * @return The data flavour for hero items
	 */
	public final DataFlavor getHeroItemFlavour ()
	{
		return heroItemFlavour;
	}

	/**
	 * @param flavour The data flavour for hero items
	 */
	public final void setHeroItemFlavour (final DataFlavor flavour)
	{
		heroItemFlavour = flavour;
	}
	
	/**
	 * @return Bank cell renderer
	 */
	public final UnassignedHeroItemCellRenderer getUnassignedHeroItemCellRenderer ()
	{
		return unassignedHeroItemCellRenderer;
	}

	/**
	 * @param rend Bank cell renderer
	 */
	public final void setUnassignedHeroItemCellRenderer (final UnassignedHeroItemCellRenderer rend)
	{
		unassignedHeroItemCellRenderer = rend;
	}

	/**
	 * @return Hero table cell renderer
	 */
	public final HeroTableCellRenderer getHeroTableCellRenderer ()
	{
		return heroTableCellRenderer;
	}

	/**
	 * @param rend Hero table cell renderer
	 */
	public final void setHeroTableCellRenderer (final HeroTableCellRenderer rend)
	{
		heroTableCellRenderer = rend;
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
	 * @return Hero item calculations
	 */
	public final HeroItemCalculations getHeroItemCalculations ()
	{
		return heroItemCalculations;
	}

	/**
	 * @param calc Hero item calculations
	 */
	public final void setHeroItemCalculations (final HeroItemCalculations calc)
	{
		heroItemCalculations = calc;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
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
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}
	
	/**
	 * Table model for displaying the heroesgrid
	 */
	private class HeroesTableModel extends AbstractTableModel
	{
		/** Underlying storage */
		private List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		/**
		 * @return Underlying storage
		 */
		public final List<MemoryUnit> getUnits ()
		{
			return units;
		}
		
		/**
		 * @return Enough rows to fit however many heroes we have to draw, rounding up
		 */
		@Override
		public final int getRowCount ()
		{
			return (getUnits ().size () + 1) / 2;
		}

		/**
		 * @return Fixed at 2 columns
		 */
		@Override
		public final int getColumnCount ()
		{
			return 2;
		}

		/**
		 * @return Hero to display at the specified grid location
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			final int index = (rowIndex * 2) + columnIndex;
			return ((index < 0) || (index >= getUnits ().size ())) ? null : getUnits ().get (index);
		}

		/**
		 * @return Columns are all units
		 */
		@Override
		public final Class<?> getColumnClass (@SuppressWarnings ("unused") final int columnIndex)
		{
			return MemoryUnit.class;
		}
	}
}
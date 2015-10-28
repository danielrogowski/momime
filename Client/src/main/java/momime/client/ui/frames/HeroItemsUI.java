package momime.client.ui.frames;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.TransferHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.draganddrop.TransferableFactory;
import momime.client.ui.draganddrop.TransferableHeroItem;
import momime.client.ui.renderer.UnassignedHeroItemCellRenderer;
import momime.client.utils.TextUtils;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.HeroItem;

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
	
	/** The data flavour for hero items */
	private DataFlavor heroItemFlavour;
	
	/** Bank title */
	private JLabel bankTitle;

	/** Items not assigned to any hero */
	private DefaultListModel<HeroItem> bankItems; 
	
	/** Items not assigned to any hero */
	private JList<HeroItem> bankList;
	
	/** Bank cell renderer */
	private UnassignedHeroItemCellRenderer unassignedHeroItemCellRenderer;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;
	
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
		
		// Actions
		
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
		contentPane.setLayout (new XmlLayoutManager (getHeroItemsLayout ()));
		
		bankTitle = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (bankTitle, "frmHeroItemsBankLabel");
		
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
				if (canImport (support))
					try
					{
						final HeroItem item = (HeroItem) support.getTransferable ().getTransferData (getHeroItemFlavour ());
						final int manaGained = getHeroItemCalculations ().calculateCraftingCost (item, getClient ().getClientDB ()) / 2;

						final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
						msg.setTitleLanguageCategoryID ("frmHeroItems");
						msg.setTitleLanguageEntryID ("DestroyTitle");
						msg.setText (getLanguage ().findCategoryEntry ("frmHeroItems", "Destroy").replaceAll
							("ITEM_NAME", item.getHeroItemName ()).replaceAll
							("MANA_GAINED", getTextUtils ().intToStrCommas (manaGained)));
						msg.setDestroyHeroItem (item);
						msg.setVisible (true);
						
						imported = true;
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
				return imported;
			}
		});
		
		// Set up cell renderer
		getUnassignedHeroItemCellRenderer ().init ();
		
		// Set up the list
		bankItems = new DefaultListModel<HeroItem> ();
		bankList = new JList<HeroItem> ();
		bankList.setOpaque (false);
		bankList.setModel (bankItems);
		bankList.setCellRenderer (getUnassignedHeroItemCellRenderer ());
		bankList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);

		final JScrollPane spellsScroll = getUtils ().createTransparentScrollPane (bankList);
		spellsScroll.setHorizontalScrollBarPolicy (ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentPane.add (spellsScroll, "frmHeroItemsBank");

		// Set up dragging and dropping
		DragSource.getDefaultDragSource ().createDefaultDragGestureRecognizer (bankList, DnDConstants.ACTION_MOVE, (ev) ->
		{
			final int index = bankList.locationToIndex (ev.getDragOrigin ());
			if ((index >= 0) && (index < bankItems.size ()) && (bankList.getCellBounds (index, index).contains (ev.getDragOrigin ())))
				try
				{
					final HeroItem item = bankItems.get (index);
					
					// Create a mouse cursor that looks like the chosen item
					final HeroItemTypeGfx itemGfx = getGraphicsDB ().findHeroItemType (item.getHeroItemTypeID (), "HeroItemsUI");
					final BufferedImage itemImage = getUtils ().loadImage (itemGfx.getHeroItemTypeImageFile ().get (item.getHeroItemImageNumber ()));
					final Image doubleItemImage = getUtils ().doubleSize (itemImage);
					final Cursor cursor = Toolkit.getDefaultToolkit ().createCustomCursor
						(doubleItemImage, new Point (itemImage.getWidth (), itemImage.getHeight ()), item.getHeroItemName ());
					
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
		
		// Load the initial list
		refreshItemsBank ();
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		
		getFrame ().setShape (new Polygon
			(new int [] {0, 798, 798, 570, 570, 182, 182, 0},
			new int [] {0, 0, 304, 304, 394, 394, 304, 304},
			8));
		
		log.trace ("Exiting init");
	}
	
	/**
	 * Refresh list of items in bank
	 */
	public final void refreshItemsBank ()
	{
		log.trace ("Entering refreshItemsBank");

		bankItems.clear ();
		for (final HeroItem item : getClient ().getOurPersistentPlayerPrivateKnowledge ().getUnassignedHeroItem ())
			bankItems.addElement (item);
		
		log.trace ("Exiting refreshItemsBank");
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
}
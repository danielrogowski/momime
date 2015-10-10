package momime.client.ui.frames;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.database.SpellLang;
import momime.client.ui.MomUIConstants;
import momime.common.database.HeroItemType;
import momime.common.database.Spell;

/**
 * UI for designing hero items
 */
public final class CreateArtifactUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (QueuedSpellsUI.class);

	/** XML layout */
	private XmlLayoutContainerEx createArtifactLayout;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Dynamically created item type actions */
	private final Map<String, Action> itemTypeActions = new HashMap<String, Action> ();

	/** Dynamically created item type buttons */
	private final Map<String, JButton> itemTypeButtons = new HashMap<String, JButton> ();
	
	/** Image of the item being made */
	private JLabel itemImage;

	/** The item creation spell being cast */
	private Spell spell;
	
	/** The currently selected item type */
	private HeroItemType heroItemType;

	/** The graphics for the currently selected item type */
	private HeroItemTypeGfx heroItemTypeGfx;
	
	/** Index into the available images list for the selected item type */
	private int imageNumber;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/createArtifact.png");
		final BufferedImage itemTypeButtonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button62x26Normal.png");
		final BufferedImage itemTypeButtonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button62x26Pressed.png");
		final BufferedImage leftArrowNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowLeftNormal.png");
		final BufferedImage leftArrowPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowLeftPressed.png");
		final BufferedImage rightArrowNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowRightNormal.png");
		final BufferedImage rightArrowPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/goldArrowRightPressed.png");

		// Actions
		final Action previousImageAction = new LoggingAction ((ev) -> updateItemImage (-1));
		final Action nextImageAction = new LoggingAction ((ev) -> updateItemImage (1));
		
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
		contentPane.setLayout (new XmlLayoutManager (getCreateArtifactLayout ()));
		
		itemImage = new JLabel ();
		contentPane.add (itemImage, "frmCreateArtifactImage");
		
		contentPane.add (getUtils ().createImageButton (previousImageAction, null, null, null, leftArrowNormal, leftArrowPressed, leftArrowNormal), "frmCreateArtifactImagePrevious");
		contentPane.add (getUtils ().createImageButton (nextImageAction, null, null, null, rightArrowNormal, rightArrowPressed, rightArrowNormal), "frmCreateArtifactImageNext");
		
		// Item type buttons
		int itemTypeNumber = 0;
		for (final HeroItemType itemType : getClient ().getClientDB ().getHeroItemType ())
		{
			itemTypeNumber++;
			
			final Action itemTypeAction = new LoggingAction ((ev) -> selectItemType (itemType));
			
			final JButton itemTypeButton = getUtils ().createImageButton (itemTypeAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
				itemTypeButtonNormal, itemTypeButtonPressed, itemTypeButtonNormal);
			contentPane.add (itemTypeButton, "frmCreateArtifactItemType" + itemTypeNumber);
			
			itemTypeActions.put (itemType.getHeroItemTypeID (), itemTypeAction);
			itemTypeButtons.put (itemType.getHeroItemTypeID (), itemTypeButton);
		}
		
		// Lock frame size
		selectItemType (getClient ().getClientDB ().getHeroItemType ().get (0));		// Pick Sword by default
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);

		log.trace ("Exiting init");
	}
	
	/**
	 * @param newItemType Type of item we now wish to make
	 * @throws IOException If we can't find the new item type in the graphics XML, we find it but it has no image(s) defined, or there's a problem loading the first image
	 */
	private final void selectItemType (final HeroItemType newItemType) throws IOException
	{
		heroItemType = newItemType;
		
		// Light up the relevant item type button gold
		for (final Entry<String, JButton> itemTypeButton : itemTypeButtons.entrySet ())
			itemTypeButton.getValue ().setForeground
				(itemTypeButton.getKey ().equals (newItemType.getHeroItemTypeID ()) ? MomUIConstants.GOLD : MomUIConstants.DARK_BROWN);
		
		// Update the image
		heroItemTypeGfx = getGraphicsDB ().findHeroItemType (newItemType.getHeroItemTypeID (), "selectItemType");
		if (heroItemTypeGfx.getHeroItemTypeImageFile ().size () == 0)
			throw new IOException ("Hero item type " + newItemType.getHeroItemTypeID () + " exists in graphics XML but has no image(s) defined"); 
				
		imageNumber = 0;
		updateItemImage (0);
	}
	
	/**
	 * @param changeBy Amount to change the image by; set to +1/-1 for the image selection buttons
	 * @throws IOException If there is a problem loading the new image
	 */
	private final void updateItemImage (final int changeBy) throws IOException
	{
		// Update image number
		imageNumber = imageNumber + changeBy;
		while (imageNumber < 0)
			imageNumber = imageNumber + heroItemTypeGfx.getHeroItemTypeImageFile ().size ();

		while (imageNumber >= heroItemTypeGfx.getHeroItemTypeImageFile ().size ())
			imageNumber = imageNumber - heroItemTypeGfx.getHeroItemTypeImageFile ().size ();
		
		// Update icon
		itemImage.setIcon (new ImageIcon (doubleSize (getUtils ().loadImage (heroItemTypeGfx.getHeroItemTypeImageFile ().get (imageNumber)))));
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
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		final SpellLang spellLang = getLanguage ().findSpell (spell.getSpellID ());
		final String spellName = (spellLang == null) ? null : spellLang.getSpellName ();
		getFrame ().setTitle (spellName != null ? spellName : spell.getSpellID ());
		
		// Item type buttons
		for (final Entry<String, Action> itemTypeAction : itemTypeActions.entrySet ())
			itemTypeAction.getValue ().putValue (Action.NAME, getLanguage ().findHeroItemTypeDescription (itemTypeAction.getKey ()));
		
		log.trace ("Exiting languageChanged");
	}

	/**
	 * @return The item creation spell being cast
	 */
	public final Spell getSpell ()
	{
		return spell;
	}

	/**
	 * @param s The item creation spell being cast
	 */
	public final void setSpell (final Spell s)
	{
		spell = s;
		
		// If the form is already displayed and we're simply switching which spell is being cast, then update the form
		if (itemImage != null)
			languageChanged ();
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCreateArtifactLayout ()
	{
		return createArtifactLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCreateArtifactLayout (final XmlLayoutContainerEx layout)
	{
		createArtifactLayout = layout;
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
}
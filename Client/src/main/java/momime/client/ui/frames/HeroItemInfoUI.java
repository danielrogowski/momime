package momime.client.ui.frames;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

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
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.messages.NumberedHeroItem;

/**
 * Small popup box displaying the picture, name and bonuses of a particular hero item
 */
public final class HeroItemInfoUI extends MomClientFrameUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (HeroItemInfoUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx heroItemInfoLayout;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Small font */
	private Font smallFont;
	
	/** Medium font */
	private Font mediumFont;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Close action */
	private Action closeAction;
	
	/** Text area showing all the item bonuses */
	private JTextArea bonuses;
	
	/** The item being displayed */
	private NumberedHeroItem item;
	
	/**
	 * Sets up the dialog once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/heroItems/itemInfo.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button50x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button50x18goldPressed.png");

		// Actions
		closeAction = new LoggingAction ((ev) -> getFrame ().dispose ());
		
		// Initialize the frame
		final HeroItemInfoUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				getClient ().getHeroItemInfos ().remove (getItem ().getHeroItemURN ());
			}
		});
		
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
		contentPane.setLayout (new XmlLayoutManager (getHeroItemInfoLayout ()));
		
		final JLabel itemImage = new JLabel ();
		contentPane.add (itemImage, "frmHeroItemInfoImage");
		
		final JLabel itemName = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (itemName, "frmHeroItemInfoName");
		
		bonuses = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (getUtils ().createTransparentScrollPane (bonuses), "frmHeroItemInfoBonuses");

		contentPane.add (getUtils ().createImageButton (closeAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmHeroItemInfoClose");
		
		// Fill in details from the item
		itemName.setText (getItem ().getHeroItemName ());
		getFrame ().setTitle (getItem ().getHeroItemName ());
		
		final HeroItemTypeGfx itemType = getGraphicsDB ().findHeroItemType (getItem ().getHeroItemTypeID (), "HeroItemInfoUI");
		final BufferedImage image = getUtils ().loadImage (itemType.getHeroItemTypeImageFile ().get (getItem ().getHeroItemImageNumber ()));
		itemImage.setIcon (new ImageIcon (getUtils ().doubleSize (image)));
		
		// Lock dialog size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		// Button
		closeAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmHeroItemInfo", "Close"));
		
		// Bonus descriptions
		final StringBuilder text = new StringBuilder ();
		for (final HeroItemTypeAllowedBonus bonus : item.getHeroItemChosenBonus ())
		{
			if (text.length () > 0)
				text.append (System.lineSeparator ());

			// Bullet point
			text.append ("\u2022 ");
			
			if (bonus.getHeroItemBonusID ().equals (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES))
			{
				final SpellLang spell = getLanguage ().findSpell (item.getSpellID ());
				final String spellName = (spell == null) ? null : spell.getSpellName ();
				
				text.append (item.getSpellChargeCount () + "x " + ((spellName != null) ? spellName : item.getSpellID ()));
			}
			else
				text.append (getLanguage ().findHeroItemBonusDescription (bonus.getHeroItemBonusID ()));
		}
		
		bonuses.setText (text.toString ());

		log.trace ("Exiting languageChanged");
	}

	/**
	 * Close the hero item screen 
	 */
	public final void close ()
	{
		log.trace ("Entering close: " + getItem ().getHeroItemURN ());
		
		getFrame ().dispose ();

		log.trace ("Exiting close: " + getItem ().getHeroItemURN ());
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getHeroItemInfoLayout ()
	{
		return heroItemInfoLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setHeroItemInfoLayout (final XmlLayoutContainerEx layout)
	{
		heroItemInfoLayout = layout;
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
	 * @return The item being displayed
	 */
	public final NumberedHeroItem getItem ()
	{
		return item;
	}

	/**
	 * @param i The item being displayed
	 */
	public final void setItem (final NumberedHeroItem i)
	{
		item = i;
	}
}
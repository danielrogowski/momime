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

import com.ndg.utils.swing.ModifiedImageCache;
import com.ndg.utils.swing.actions.LoggingAction;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.newturnmessages.NewTurnMessageOfferItemEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemType;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.clienttoserver.RequestAcceptOfferMessage;

/**
 * Dialog asking user to confirm or reject offer to buy a hero item
 */
public final class HeroItemOfferUI extends MomClientFrameUI implements OfferUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (HeroItemOfferUI.class);
	
	/** XML layout */
	private XmlLayoutContainerEx heroItemInfoLayout;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Small font */
	private Font smallFont;
	
	/** Medium font */
	private Font mediumFont;

	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** For creating resized images */
	private ModifiedImageCache modifiedImageCache;
	
	/** Buy action */
	private Action buyAction;

	/** Reject action */
	private Action rejectAction;
	
	/** Text area showing all the item bonuses */
	private JTextArea bonuses;

	/** The offer we're showing this UI for */
	private NewTurnMessageOfferItemEx newTurnMessageOffer;

	/**
	 * Sets up the dialog once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/heroItems/itemInfo.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button50x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button50x18goldPressed.png");

		// Actions
		buyAction = new LoggingAction ((ev) ->
		{
			final RequestAcceptOfferMessage msg = new RequestAcceptOfferMessage ();
			msg.setOfferURN (getNewTurnMessageOffer ().getOfferURN ());
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		});

		rejectAction = new LoggingAction ((ev) ->
		{
			getNewTurnMessageOffer ().setOfferAccepted (false);
			getNewTurnMessagesUI ().languageChanged ();
			getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
			getFrame ().dispose ();
		});
		
		// Initialize the frame
		final HeroItemOfferUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
				getClient ().getOffers ().remove (getNewTurnMessageOffer ().getOfferURN ());
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

		contentPane.add (getUtils ().createImageButton (buyAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmHeroItemInfoExtraButton");
		
		contentPane.add (getUtils ().createImageButton (rejectAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmHeroItemInfoClose");
		
		// Fill in details from the item
		itemName.setText (getNewTurnMessageOffer ().getHeroItem ().getHeroItemName ());
		getFrame ().setTitle (getNewTurnMessageOffer ().getHeroItem ().getHeroItemName ());
		
		final HeroItemType itemType = getClient ().getClientDB ().findHeroItemType (getNewTurnMessageOffer ().getHeroItem ().getHeroItemTypeID (), "HeroItemOfferUI");
		final String image = itemType.getHeroItemTypeImageFile ().get (getNewTurnMessageOffer ().getHeroItem ().getHeroItemImageNumber ());
		itemImage.setIcon (new ImageIcon (getModifiedImageCache ().doubleSize (image)));
		
		// Lock dialog size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Buttons
		buyAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getBuy ()));
		rejectAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getUnitInfoScreen ().getReject ()));
		
		// Bonus descriptions
		final StringBuilder text = new StringBuilder ();
		for (final String bonusID : getNewTurnMessageOffer ().getHeroItem ().getHeroItemChosenBonus ())
		{
			if (text.length () > 0)
				text.append (System.lineSeparator ());

			// Bullet point
			text.append ("\u2022 ");
			
			try
			{
				if (bonusID.equals (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES))
					text.append (getNewTurnMessageOffer ().getHeroItem ().getSpellChargeCount () + "x " + getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findSpell (getNewTurnMessageOffer ().getHeroItem ().getSpellID (), "HeroItemOfferUI").getSpellName ()));
				else
					text.append (getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findHeroItemBonus (bonusID, "HeroItemOfferUI").getHeroItemBonusDescription ()));
			}
			catch (final RecordNotFoundException e)
			{
				log.error (e, e);
			}
		}
		
		bonuses.setText (text.toString ());
	}

	/**
	 * Close out the offer UI
	 */
	@Override
	public final void close ()
	{
		getFrame ().dispose ();
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
	 * @return The offer we're showing this UI for
	 */
	public final NewTurnMessageOfferItemEx getNewTurnMessageOffer ()
	{
		return newTurnMessageOffer;
	}

	/**
	 * @param o The offer we're showing this UI for
	 */
	public final void setNewTurnMessageOffer (final NewTurnMessageOfferItemEx o)
	{
		newTurnMessageOffer = o;
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
	 * @return For creating resized images
	 */
	public final ModifiedImageCache getModifiedImageCache ()
	{
		return modifiedImageCache;
	}

	/**
	 * @param m For creating resized images
	 */
	public final void setModifiedImageCache (final ModifiedImageCache m)
	{
		modifiedImageCache = m;
	}
}
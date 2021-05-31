package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.HeroItemOfferUI;
import momime.client.ui.frames.OfferUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.TextUtils;
import momime.common.database.HeroItemType;
import momime.common.database.LanguageText;
import momime.common.messages.NewTurnMessageOfferItem;

/**
 * Offer to buy a hero item.  Similar to heroes - we don't actually have the item yet so cannot look it up in our list,
 * so must include the full details of the item here.
 */
public final class NewTurnMessageOfferItemEx extends NewTurnMessageOfferItem implements NewTurnMessageOfferEx,
	NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageClickable, NewTurnMessageMustBeAnswered
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (NewTurnMessageOfferItemEx.class);

	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Multiplayer client */
	private MomClient client;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** null = not yet decided; true = accepted; false = rejected */
	private Boolean offerAccepted;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_OFFERS;
	}
	
	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		Image image = null;
		try
		{
			final HeroItemType itemType = getClient ().getClientDB ().findHeroItemType (getHeroItem ().getHeroItemTypeID (), "NewTurnMessageOfferItemEx");
			final String imageName = itemType.getHeroItemTypeImageFile ().get (getHeroItem ().getHeroItemImageNumber ());
			image = getUtils ().loadImage (imageName);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return image;
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		final List<LanguageText> languageText;
		if (isOfferAccepted () == null)
			languageText = getLanguages ().getNewTurnMessages ().getOfferItem ();
		else if (isOfferAccepted ())
			languageText = getLanguages ().getNewTurnMessages ().getOfferItemAccepted ();
		else
			languageText = getLanguages ().getNewTurnMessages ().getOfferItemRejected ();
		
		final String text = getLanguageHolder ().findDescription (languageText).replaceAll
			("COST", getTextUtils ().intToStrCommas (getCost ())).replaceAll
			("ITEM_NAME", getHeroItem ().getHeroItemName ());

		return text;
	}
	
	/**
	 * Take appropriate action when a new turn message is clicked on
	 * @throws Exception If there was a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		if (!isAnswered ())
		{
			OfferUI ui = getClient ().getOffers ().get (getOfferURN ());
			if (ui == null)
			{
				final HeroItemOfferUI offer = getPrototypeFrameCreator ().createHeroItemOffer ();
				offer.setNewTurnMessageOffer (this);
				
				getClient ().getOffers ().put (getOfferURN (), offer);
				ui = offer;
			}
			ui.setVisible (true);
		}
	}

	/**
	 * @return null = not yet decided; true = accepted; false = rejected
	 */
	@Override
	public final Boolean isOfferAccepted ()
	{
		return offerAccepted;
	}
	
	/**
	 * @param a null = not yet decided; true = accepted; false = rejected
	 */
	@Override
	public final void setOfferAccepted (final Boolean a)
	{
		offerAccepted = a;
	}
	
	/**
	 * @return Whether the user has acted on this message yet
	 */
	@Override
	public final boolean isAnswered ()
	{
		return (isOfferAccepted () != null);
	}
	
	/**
	 * @return Text to display when we can't end turn because this NTM hasn't been answered yet 
	 */
	@Override
	public final List<LanguageText> getCannotEndTurnText ()
	{
		return getLanguages ().getOverlandMapScreen ().getMapRightHandBar ().getCannotEndTurnNewTurnMessagesOffer ();
	}
	
	/**
	 * @return Image to display when we can't end turn because this NTM hasn't been answered yet
	 */
	@Override
	public final String getCannotEndTurnImageFilename ()
	{
		return "/momime.client.graphics/ui/overland/rightHandPanel/cannotEndTurnDueToNTMs-offer.png";
	}
	
	/**
	 * @return Font to display the text in
	 */
	@Override
	public final Font getFont ()
	{
		return getSmallFont ();
	}
	
	/**
	 * @return Colour to display the text in
	 */
	@Override
	public final Color getColour ()
	{
		return MomUIConstants.SILVER;
	}
	
	/**
	 * @return Current status of this NTM
	 */
	@Override
	public final NewTurnMessageStatus getStatus ()
	{
		return status;
	}
	
	/**
	 * @param newStatus New status for this NTM
	 */
	@Override
	public final void setStatus (final NewTurnMessageStatus newStatus)
	{
		status = newStatus;
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
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
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
}
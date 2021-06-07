package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.HeroOrUnitsOfferUI;
import momime.client.ui.frames.OfferUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.TextUtils;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.LanguageText;
import momime.common.database.Pick;
import momime.common.database.UnitType;
import momime.common.messages.AvailableUnit;
import momime.common.messages.NewTurnMessageOfferUnits;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitTypeUtils;
import momime.common.utils.UnitUtils;

/**
 * Offer to hire mercenary unit(s).
 */
public final class NewTurnMessageOfferUnitsEx extends NewTurnMessageOfferUnits implements NewTurnMessageOfferEx,
	NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessagePreProcess, NewTurnMessageClickable, NewTurnMessageMustBeAnswered
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (NewTurnMessageOfferUnitsEx.class);

	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;

	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Sample unit representing the unit on offer */
	private AvailableUnit sampleUnit;
	
	/** The unit on offer */
	private ExpandedUnitDetails xu;
	
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
		if (xu != null)
			try
			{
				final String imageName = getClient ().getClientDB ().findUnit (xu.getUnitID (), "NewTurnMessageOfferUnitsEx").getUnitOverlandImageFile ();
				if (imageName != null)
					image = getUtils ().loadImage (imageName);
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		
		return image;
	}
	
	/**
	 * Just so that we do the unit lookup and detail expanding only once
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		// Work out the experience value from the level
		final Pick normalUnitRealm = getClient ().getClientDB ().findPick (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, "NewTurnMessageOfferUnitsEx");
		final UnitType normalUnit = getClient ().getClientDB ().findUnitType (normalUnitRealm.getUnitTypeID (), "NewTurnMessageOfferUnitsEx");
		final ExperienceLevel expLevel = UnitTypeUtils.findExperienceLevel (normalUnit, getLevelNumber ());
		
		// Now can create a sample unit
		sampleUnit = new AvailableUnit ();
		sampleUnit.setUnitID (getUnitID ());
		sampleUnit.setOwningPlayerID (getClient ().getOurPlayerID ());

		// We don't have to get the weapon grade or experience right just to draw the figures
		getUnitUtils ().initializeUnitSkills (sampleUnit, expLevel.getExperienceRequired (), getClient ().getClientDB ());
		
		xu = getUnitUtils ().expandUnitDetails (sampleUnit, null, null, null, getClient ().getPlayers (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		final List<LanguageText> languageText;
		if (isOfferAccepted () == null)
			languageText = (getUnitCount () == 1) ? getLanguages ().getNewTurnMessages ().getOfferUnit () : getLanguages ().getNewTurnMessages ().getOfferUnits ();
		else if (isOfferAccepted ())
			languageText = (getUnitCount () == 1) ? getLanguages ().getNewTurnMessages ().getOfferUnitAccepted () : getLanguages ().getNewTurnMessages ().getOfferUnitsAccepted ();
		else
			languageText = (getUnitCount () == 1) ? getLanguages ().getNewTurnMessages ().getOfferUnitRejected () : getLanguages ().getNewTurnMessages ().getOfferUnitsRejected ();
		
		String text = getLanguageHolder ().findDescription (languageText).replaceAll
			("COST", getTextUtils ().intToStrCommas (getCost ())).replaceAll
			("COUNT", getTextUtils ().intToStrCommas (getUnitCount ()));
		
		getUnitStatsReplacer ().setUnit (xu);
		text = getUnitStatsReplacer ().replaceVariables (text);

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
				final HeroOrUnitsOfferUI offer = getPrototypeFrameCreator ().createHeroOrUnitsOffer ();
				offer.setUnit (sampleUnit);
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
	 * @return Variable replacer for outputting skill descriptions
	 */
	public final UnitStatsLanguageVariableReplacer getUnitStatsReplacer ()
	{
		return unitStatsReplacer;
	}

	/**
	 * @param replacer Variable replacer for outputting skill descriptions
	 */
	public final void setUnitStatsReplacer (final UnitStatsLanguageVariableReplacer replacer)
	{
		unitStatsReplacer = replacer;
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
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param util Unit utils
	 */
	public final void setUnitUtils (final UnitUtils util)
	{
		unitUtils = util;
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
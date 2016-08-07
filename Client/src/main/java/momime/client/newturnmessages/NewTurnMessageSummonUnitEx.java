package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.MomUIConstants;
import momime.common.messages.AvailableUnit;
import momime.common.messages.NewTurnMessageSummonUnit;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;

/**
 * NTM describing a unit that was added from a summoning spell
 */
public final class NewTurnMessageSummonUnitEx extends NewTurnMessageSummonUnit
	implements NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageMusic, NewTurnMessagePreProcess
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewTurnMessageSummonUnitEx.class);

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
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** The unit that was summoned */
	private ExpandedUnitDetails xu;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_SPELLS;
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
			final String imageName = getGraphicsDB ().findUnit (getUnitID (), "NewTurnMessageSummonUnitEx").getUnitSummonImageFile ();
			if (imageName != null)
			{
				final BufferedImage fullSizeImage = getUtils ().loadImage (imageName);
				image = fullSizeImage.getScaledInstance (fullSizeImage.getWidth () / 2, fullSizeImage.getHeight () / 2, Image.SCALE_SMOOTH);
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return image;
	}
	
	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	@Override
	public final String getMusicResourceName ()
	{
		String music = null;
		try
		{
			music = getGraphicsDB ().findSpell (getSpellID (), "NewTurnMessageSummonUnitEx").getSpellMusicFile ();
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return music;
	}
	
	/**
	 * Just so that we do the unit lookup and detail expanding only once
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		log.trace ("Entering preProcess: Unit URN " + getUnitURN ());
		
		final AvailableUnit unit;
		if (getUnitURN () != null)
			unit = getUnitUtils ().findUnitURN (getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());

		// UnitURN will be null if bump type = no room and so no unit was actually created; in that case fall back to creating a dummy unit from the unitID
		else
		{
			unit = new AvailableUnit ();
			unit.setUnitID (getUnitID ());
		}
		
		xu = getUnitUtils ().expandUnitDetails (unit, null, null, null, getClient ().getPlayers (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());

		log.trace ("Exiting preProcess");
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		// Get prefix
		String text = getLanguage ().findCategoryEntry ("NewTurnMessages",
			(getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ? "SummonedUnitLastTurn" : "SummonedUnit");
		
		getUnitStatsReplacer ().setUnit (xu);
		text = getUnitStatsReplacer ().replaceVariables (text);
		
		// Get suffix (if applicable)
		switch (getUnitAddBumpType ())
		{
			case BUMPED:
				text = text + getLanguage ().findCategoryEntry ("NewTurnMessages", "SummonedUnitBumpedSuffix");
				break;
			
			case NO_ROOM:
				text = text + getLanguage ().findCategoryEntry ("NewTurnMessages", "SummonedUnitNoRoomSuffix");
				break;
				
			default:
				break;
		}

		return text;
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
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
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
}
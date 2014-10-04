package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.MomUIConstants;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.NewTurnMessageUnitKilledFromLackOfProduction;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.UnitUtils;

/**
 * A unit was killed off because we couldn't afford the rations, gold and/or mana to pay for it
 */
public final class NewTurnMessageUnitKilledFromLackOfProductionEx extends NewTurnMessageUnitKilledFromLackOfProduction
	implements NewTurnMessageSimpleUI, NewTurnMessageExpiration, NewTurnMessagePreProcess
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewTurnMessageUnitKilledFromLackOfProductionEx.class);
	
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** The unit that was killed */
	private MemoryUnit unit;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_LACK_OF_PRODUCTION;
	}

	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		return null;
	}
	
	/**
	 * The kill unit message arrives before the NTM explaining that it was killed due to lack of production.  So the unit is killed off via a special
	 * status that doesn't permanently remove it at that stage.  So as the NTM starts up, we grab a reference to the unit object and
	 * then permanently remove it.
	 * 
	 * @throws IOException If there is a problem
	 */
	@Override
	public final void preProcess () throws IOException
	{
		log.trace ("Entering preProcess: Unit URN " + getUnitURN ());
		
		unit = getUnitUtils ().findUnitURN (getUnitURN (),
			getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (), "NewTurnMessageUnitKilledFromLackOfProductionEx");
		
		if (unit.getStatus () != UnitStatusID.KILLED_BY_LACK_OF_PRODUCTION)
			log.warn ("Unit URN " + getUnitURN () + " was found by lack of production NTM but isn't at the expected status: " + unit.getStatus ());
		
		// Now we've got a hold of the unit, we can really kill it
		getUnitUtils ().removeUnitURN (getUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());

		log.trace ("Exiting preProcess");
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		final ProductionType productionType = getLanguage ().findProductionType (getProductionTypeID ());
		String text = (productionType != null) ? productionType.getUnitKilledFromLackOfProduction () : null;
		if (text == null)
			text = "Unit lost from lack of " + getProductionTypeID ();
		
		getUnitStatsReplacer ().setUnit (unit);
		text = getUnitStatsReplacer ().replaceVariables (text);
		
		// Make the first letter upper case, because on single units it'll start like "a Magic Spirit..."
		return text.substring (0, 1).toUpperCase () + text.substring (1);
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
}
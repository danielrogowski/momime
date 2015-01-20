package momime.client.scheduledcombatmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.MomUIConstants;

/**
 * Message about how many scheduled combats there are still left to play that we aren't involved in.
 */
public final class ScheduledCombatMessageOther implements ScheduledCombatMessageSimpleUI
{
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** The number of scheduled combats that still need to be played, but we aren't involved in */
	private int scheduledCombatsNotInvolvedIn;

	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final ScheduledCombatMessageSortOrder getSortOrder ()
	{
		return ScheduledCombatMessageSortOrder.SORT_ORDER_NOT_INVOLVED;
	}

	/**
	 * @return Image to draw for this combat, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		return null;
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
	 * @return Text to display for this combat
	 */
	@Override
	public final String getText ()
	{
		return getLanguage ().findCategoryEntry ("ScheduledCombats", "NotInvolved").replaceAll
			("COMBAT_COUNT", new Integer (getScheduledCombatsNotInvolvedIn ()).toString ());
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
	 * @return The number of scheduled combats that still need to be played, but we aren't involved in
	 */
	public final int getScheduledCombatsNotInvolvedIn ()
	{
		return scheduledCombatsNotInvolvedIn;
	}

	/**
	 * @param nbr The number of scheduled combats that still need to be played, but we aren't involved in
	 */
	public final void setScheduledCombatsNotInvolvedIn (final int nbr)
	{
		scheduledCombatsNotInvolvedIn = nbr;
	}
}
package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;

import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;

/**
 * Category header, which displays a title in the new turn messages scroll, e.g. "City Growth" above all the details about city populations that grew this turn.
 */
public final class NewTurnMessageCategory implements NewTurnMessageSimpleUI
{
	/** Large font */
	private Font largeFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** One of the SORT_ORDER_ constants, indicating the sort order group that this category is the title for */
	private NewTurnMessageSortOrder sortOrder;

	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order group that this category is the title for
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return sortOrder;
	}

	/**
	 * @param order One of the SORT_ORDER_ constants, indicating the sort order group that this category is the title for
	 */
	public final void setSortOrder (final NewTurnMessageSortOrder order)
	{
		sortOrder = order;
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
	 * @return Text to display for this NTM
	 * @throws IOException If there is a problem
	 */
	@Override
	public final String getText () throws IOException
	{
		return getLanguageHolder ().findDescription (getSortOrder ().getLanguageText (getLanguages ().getNewTurnMessages ()));
	}
	
	/**
	 * @return Font to display the text in
	 */
	@Override
	public final Font getFont ()
	{
		return getLargeFont ();
	}
	
	/**
	 * @return Colour to display the text in
	 */
	@Override
	public final Color getColour ()
	{
		return MomUIConstants.DARK_RED;
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
}
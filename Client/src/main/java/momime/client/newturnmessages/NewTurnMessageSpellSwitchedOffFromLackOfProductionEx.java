package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.NewTurnMessageSpellSwitchedOffFromLackOfProduction;

/**
 * A spell was switched off because we couldn't afford the mana to maintain it
 */
public final class NewTurnMessageSpellSwitchedOffFromLackOfProductionEx extends NewTurnMessageSpellSwitchedOffFromLackOfProduction
	implements NewTurnMessageSimpleUI, NewTurnMessageExpiration
{
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
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
	 * @return Text to display for this NTM
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final String getText () throws RecordNotFoundException
	{
		String text = getLanguageHolder ().findDescription
			(getClient ().getClientDB ().findProductionType (getProductionTypeID (), "NewTurnMessageSpellSwitchedOffFromLackOfProductionEx").getSpellSwitchedOffFromLackOfProduction ());
		
		final String spellName = getLanguageHolder ().findDescription
			(getClient ().getClientDB ().findSpell (getSpellID (), "NewTurnMessageSpellSwitchedOffFromLackOfProductionEx").getSpellName ());

		text = text.replaceAll ("SPELL_NAME", (spellName != null) ? spellName : getSpellID ());
		
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
}
package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Unit;
import momime.client.ui.MomUIConstants;
import momime.common.messages.NewTurnMessageNode;
import momime.common.messages.NewTurnMessageTypeID;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * NTM about a node that we captured or lost
 */
public final class NewTurnMessageNodeEx extends NewTurnMessageNode
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
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_NODES;
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
	 */
	@Override
	public final String getText ()
	{
		// Work out the right languageEntryID
		String languageEntryID;
		String otherPlayerName = "";
		if (getMsgType () == NewTurnMessageTypeID.NODE_CAPTURED)
		{
			languageEntryID = "NodeCaptured";
			if ((getOtherUnitID () != null) && (getOtherPlayerID () != null))
			{
				languageEntryID = "Owned" + languageEntryID;
				
				final PlayerPublicDetails otherPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getOtherPlayerID ());
				otherPlayerName = (otherPlayer != null) ? otherPlayer.getPlayerDescription ().getPlayerName () : getOtherPlayerID ().toString ();
			}
			else
				languageEntryID = "Empty" + languageEntryID;
		}
		else
			languageEntryID = "NostLost";
		
		if (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN)
			languageEntryID = languageEntryID + "LastTurn";
		
		// Find the unit(s) and other player involved
		final Unit ourUnit = getLanguage ().findUnit (getUnitID ());
		final String ourUnitName = (ourUnit != null) ? ourUnit.getUnitName () : null;

		final Unit otherUnit = (getOtherUnitID () != null) ? getLanguage ().findUnit (getOtherUnitID ()) : null;
		final String otherUnitName = (otherUnit != null) ? otherUnit.getUnitName () : null;
		
		// Look up text and do replacements
		return getLanguage ().findCategoryEntry ("NewTurnMessages", languageEntryID).replaceAll
			("OTHER_PLAYER_NAME", otherPlayerName).replaceAll
			("OTHER_UNIT_NAME", (otherUnitName != null) ? otherUnitName : getOtherUnitID ()).replaceAll
			("UNIT_NAME", (ourUnitName != null) ? ourUnitName : getUnitID ());
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
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}
}
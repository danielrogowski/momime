package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.util.List;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.common.database.LanguageText;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.NewTurnMessageNode;
import momime.common.messages.NewTurnMessageTypeID;

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
	 * @throws RecordNotFoundException If an expected data item can't be found
	 */
	@Override
	public final String getText () throws RecordNotFoundException
	{
		// Work out the right languageEntryID
		final List<LanguageText> languageText;
		String otherPlayerName = "";
		if (getMsgType () == NewTurnMessageTypeID.NODE_CAPTURED)
		{
			if ((getOtherUnitID () != null) && (getOtherPlayerID () != null))
			{
				languageText = (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ? getLanguages ().getNewTurnMessages ().getOwnedNodeCapturedLastTurn () :
					getLanguages ().getNewTurnMessages ().getOwnedNodeCaptured ();
				
				final PlayerPublicDetails otherPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getOtherPlayerID ());
				otherPlayerName = (otherPlayer != null) ? otherPlayer.getPlayerDescription ().getPlayerName () : getOtherPlayerID ().toString ();
			}
			else
				languageText = (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ? getLanguages ().getNewTurnMessages ().getEmptyNodeCapturedLastTurn () :
					getLanguages ().getNewTurnMessages ().getEmptyNodeCaptured ();
		}
		else
			languageText = (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ? getLanguages ().getNewTurnMessages ().getNodeLostLastTurn () :
				getLanguages ().getNewTurnMessages ().getNodeLost ();
		
		// Find the unit(s) and other player involved
		final String ourUnitName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findUnit (getUnitID (), "NewTurnMessageNodeEx").getUnitName ());

		final String otherUnitName = (getOtherUnitID () == null) ? "" : getLanguageHolder ().findDescription
			(getClient ().getClientDB ().findUnit (getOtherUnitID (), "NewTurnMessageNodeEx").getUnitName ());
		
		// Look up text and do replacements
		return getLanguageHolder ().findDescription (languageText).replaceAll
			("OTHER_PLAYER_NAME", otherPlayerName).replaceAll
			("OTHER_UNIT_NAME", otherUnitName).replaceAll
			("UNIT_NAME", ourUnitName);
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
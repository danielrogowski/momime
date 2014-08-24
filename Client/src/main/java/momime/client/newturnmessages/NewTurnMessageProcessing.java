package momime.client.newturnmessages;

import java.util.List;

import momime.common.MomException;
import momime.common.messages.v0_9_5.NewTurnMessageData;

/**
 * Methods dealing with new turn messages
 */
public interface NewTurnMessageProcessing
{
	/**
	 * At the start of a new turn, we get a new block of new turn messages, so need to get rid of the old ones.
	 * However we also may get messages show up during a turn, and we may or may not have had a chance to look at those, so we leave them for an additional turn.
	 * See much longer explanation of this in the comments of NewTurnMessageStatus.
	 */
	public void expireMessages ();
	
	/**
	 * Reads a block of new turn messages that the server has sent us.
	 * Kept separate since this is used for 3 types of message.
	 * 
	 * @param msgs New messages from server
	 * @param statusForNewMessages Status to give to the new messages
	 * @throws MomException If one of the messages doesn't support the NewTurnMessageExpiration interface
	 */
	public void readNewTurnMessagesFromServer (final List<NewTurnMessageData> msgs, final NewTurnMessageStatus statusForNewMessages)
		throws MomException;
	
	/**
	 * @return List of NTMs sorted and with title categories added, ready to display in the UI
	 * @throws MomException If one of the messages doesn't support the NewTurnMessageUI interface
	 */
	public List<NewTurnMessageUI> sortAndAddCategories () throws MomException;
}
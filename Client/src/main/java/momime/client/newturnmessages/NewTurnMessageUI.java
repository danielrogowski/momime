package momime.client.newturnmessages;

/**
 * Defines methods that NTMs must provide in order to be able to be sorted into categories.  NTMs won't implement
 * this interface directly however, they'll implement either NewTurnMessageSimpleUI or NewTurnMessageComplexUI
 * which define how the message is drawn onto the NTMs scroll.
 * 
 * This is kept separate from the NewTurnMessageExpiration interface so that NewTurnMessageCategory doesn't have to
 * implement expiration or a status, since doing so would make no sense.
 */
public interface NewTurnMessageUI
{
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	public NewTurnMessageSortOrder getSortOrder ();
}
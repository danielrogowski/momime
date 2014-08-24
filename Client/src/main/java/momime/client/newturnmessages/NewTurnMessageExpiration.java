package momime.client.newturnmessages;

/**
 * Defines methods that NTMs must provide in order to be able to track when they arrived, and be expired and automatically removed at the start of our next turn.
 */
public interface NewTurnMessageExpiration
{
	/**
	 * @return Current status of this NTM
	 */
	public NewTurnMessageStatus getStatus ();
	
	/**
	 * @param newStatus New status for this NTM
	 */
	public void setStatus (final NewTurnMessageStatus newStatus);
}
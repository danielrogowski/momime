package momime.client.newturnmessages;

/**
 * Most NTMs are just informational and can be ignored.  NTMs that require the player to take some action to
 * answer the message should implement this interface.  The next turn button will be disabled until
 * the NTM is acted upon.
 */
public interface NewTurnMessageMustBeAnswered
{
	/**
	 * @return Whether the user has acted on this message yet
	 */
	public boolean isAnswered ();
}
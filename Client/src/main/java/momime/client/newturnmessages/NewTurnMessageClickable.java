package momime.client.newturnmessages;

/**
 * NTMs can implement this method to support taking some action when clicked on, though this is optional
 */
public interface NewTurnMessageClickable
{
	/**
	 * Take appropriate action when a new turn message is clicked on
	 * @throws Exception If there is a problem
	 */
	public void clicked () throws Exception;
}
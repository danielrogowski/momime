package momime.client.newturnmessages;

/**
 * Allows NTMs to specify what music they should play when displayed
 */
public interface NewTurnMessageMusic
{
	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	public String getMusicResourceName ();
}
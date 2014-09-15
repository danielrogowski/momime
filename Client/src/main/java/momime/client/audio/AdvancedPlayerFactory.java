package momime.client.audio;

/**
 * Creates new player implementations as needed
 */
public interface AdvancedPlayerFactory
{
	/**
	 * @return New player implementation
	 */
	public AdvancedPlayer createAdvancedPlayer ();
}
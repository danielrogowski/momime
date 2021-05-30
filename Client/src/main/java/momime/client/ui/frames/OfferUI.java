package momime.client.ui.frames;

import java.io.IOException;

/**
 * Methods that UIs that present offers to the player must implement.
 */
public interface OfferUI
{
	/**
	 * @param v Whether to display or hide this screen
	 * @throws IOException If a resource cannot be found
	 */
	public void setVisible (final boolean v) throws IOException;
	
	/**
	 * Close out the offer UI
	 */
	public void close ();
}
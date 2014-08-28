package momime.client.newturnmessages;

import java.io.IOException;

import javax.swing.JList;

/**
 * Animated NTMs should implement this interface, to register themselves to be redrawn when the animation timer ticks
 */
public interface NewTurnMessageAnimated
{
	/**
	 * Register repaint triggers for any animations displayed by this NTM
	 * @param newTurnMessagesList The JList that is displaying the NTMs
	 * @throws IOException If there is a problem
	 */
	public void registerRepaintTriggers (final JList<NewTurnMessageUI> newTurnMessagesList) throws IOException;
}
package momime.client.newturnmessages;

import java.awt.Component;

/**
 * For drawing of complex NTMs, can take over and customize the type and complete makeup of the display component.
 */
public interface NewTurnMessageComplexUI extends NewTurnMessageUI
{
	/**
	 * @return Custom component to draw this NTM with
	 */
	public Component getComponent ();
}
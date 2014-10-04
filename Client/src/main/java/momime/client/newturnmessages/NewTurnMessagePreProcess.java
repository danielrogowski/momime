package momime.client.newturnmessages;

import java.io.IOException;

/**
 * NTMs can implement this if they need to perform some task as the start turn/add NTM message is processed, prior to being displayed
 */
public interface NewTurnMessagePreProcess
{
	/**
	 * Perform some pre-processing action
	 * @throws IOException If there is a problem
	 */
	public void preProcess () throws IOException;
}
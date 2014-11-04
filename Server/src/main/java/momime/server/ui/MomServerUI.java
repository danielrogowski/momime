package momime.server.ui;

import java.util.List;

import momime.common.messages.MomSessionDescription;
import momime.server.MomServer;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;

/**
 * The MoM IME server supports switchable user interfaces, so can run in a window, to the console, etc.
 * Other user interface classes could also be added - all they need to do is implement this interface and pass the classname as an input param to MomServer
 */
public interface MomServerUI
{
	/**
	 * Server calls this to tell the UI what version number to display
	 * @param version Maven version of MoM IME server build
	 */
	public void setVersion (final String version);
	
	/**
	 * @param session Newly created session
	 */
	public void createWindowForNewSession (final MomSessionDescription session);

	/**
	 * Allows the UI to update its list of sessions when a session is either added or closed
	 * This method is already synchronized by the fact that whatever is calling it must obtain a write lock on the session list
	 * @param server The server that the sessions are running on
	 * @param sessions The updated list of sessions
	 */
	public void doSessionListUpdatedProcessing (final MomServer server, final List<MultiplayerSessionThread> sessions);

	/**
	 * Allows the UI to perform some action when a session has been added to the server's session list
	 * @param session The new session
	 */
	public void sessionAdded (final MultiplayerSessionThread session);
	
	/**
	 * Allows the UI to perform some action when a session has been removed from the server's session list
	 * @param session The removed session
	 */
	public void sessionRemoved (final MultiplayerSessionThread session);
}
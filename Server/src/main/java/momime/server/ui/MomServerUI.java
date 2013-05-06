package momime.server.ui;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;

import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.MomServer;

/**
 * The MoM IME server supports switchable user interfaces, so can run in a window, to the console, etc.
 * Other user interface classes could also be added - all they need to do is implement this interface and pass the classname as an input param to MomServer
 */
public interface MomServerUI
{
	// NB. These interfaces are declared in the same order that MomServer calls them during startup

	/**
	 * If writing the debug log to a text file is switched off, this is used to set the level of the whole debugLogger, so all the finer log level calls can be thrown out more quickly
	 * @return The log level above which the UI will directly display messages written to the debugLogger
	 */
	public Level getMinimumDebugLoggerLogLevel ();

	/**
	 * Placeholder where the UI can perform any work startup work necessary, typically creating the main window
	 * By this stage the debug logger has been created, so if the UI wants to hook into this and add its own handler, it can do that here too
	 */
	public void createMainWindow ();

	/**
	 * @param session Newly created session
	 * @return Window created to display log messages for this session if using the OneWindowPerGameUI; if using a different UI then just returns null
	 */
	public SessionWindow createWindowForNewSession (final MomSessionDescription session);

	/**
	 * @param session Newly created session
	 * @param sessionWindow The session window created by createWindowForNewSession
	 * @param fileLogger Logger which writes to a disk file, if enabled
	 * @return Logger created and configured for this session
	 */
	public Logger createLoggerForNewSession (final MomSessionDescription session, final SessionWindow sessionWindow);

	/**
	 * Allows the UI to update its list of sessions when a session is either added or closed
	 * This method is already synchronized by the fact that whatever is calling it must obtain a write lock on the session list
	 * @param server The server that the sessions are running on
	 * @param sessions The updated list of sessions
	 */
	public void doSessionListUpdatedProcessing (final MomServer server, final List<MultiplayerSessionThread> sessions);

	/**
	 * Allows the UI to perform some action when a session has no players left in it and is just about to end
	 * @param session The ending session
	 */
	public void doSessionEnded (final MultiplayerSessionThread session);
}

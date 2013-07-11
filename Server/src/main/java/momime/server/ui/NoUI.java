package momime.server.ui;

import java.util.List;
import java.util.logging.Logger;

import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.MomServer;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;

/**
 * No GUI at all; session messages don't appear at all
 */
public final class NoUI implements MomServerUI
{
	/**
	 * Placeholder where the UI can perform any work startup work necessary, typically creating the main window
	 * By this stage the debug logger has been created, so if the UI wants to hook into this and add its own handler, it can do that here too
	 */
	@Override
	public final void createMainWindow ()
	{
	}

	/**
	 * @param session Newly created session
	 * @return Window created to display log messages for this session if using the OneWindowPerGameUI; if using a different UI then just returns null
	 */
	@Override
	public final SessionWindow createWindowForNewSession (final MomSessionDescription session)
	{
		return null;
	}

	/**
	 * @param session Newly created session
	 * @param sessionWindow The session window created by createWindowForNewSession
	 * @return Logger created and configured for this session
	 */
	@Override
	public final Logger createLoggerForNewSession (final MomSessionDescription session, final SessionWindow sessionWindow)
	{
		// Since there's no parent logger, and we force these not to log to the generic logger defined in the log XML, they just go nowhere
		final Logger sessionLogger = Logger.getLogger (ConsoleUI.MOM_SESSION_LOGGER_PREFIX + "." + session.getSessionID ());
		sessionLogger.setUseParentHandlers (false);
		return sessionLogger;
	}

	/**
	 * Allows the UI to update its list of sessions when a session is either added or closed
	 * This method is already synchronized by the fact that whatever is calling it must obtain a write lock on the session list
	 * @param server The server that the sessions are running on
	 * @param sessions The updated list of sessions
	 */
	@Override
	public final void doSessionListUpdatedProcessing (final MomServer server, final List<MultiplayerSessionThread> sessions)
	{
	}

	/**
	 * Allows the UI to perform some action when a session has been added to the server's session list
	 * @param session The new session
	 */
	@Override
	public final void sessionAdded (final MultiplayerSessionThread session)
	{
	}
	
	/**
	 * Allows the UI to perform some action when a session has been removed from the server's session list
	 * @param session The removed session
	 */
	@Override
	public final void sessionRemoved (final MultiplayerSessionThread session)
	{
	}
}

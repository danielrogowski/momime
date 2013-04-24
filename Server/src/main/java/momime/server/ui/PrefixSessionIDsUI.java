package momime.server.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import momime.common.messages.v0_9_4.MomSessionDescription;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;

/**
 * Implements the createLoggerForNewSession method by routing all session logs to the debug logger, but prefixing the messages with the session ID
 * This allows UIs which display all the messages bunched together in a single list to distinguish which messages are for each session
 */
abstract class PrefixSessionIDsUI implements MomServerUI
{
	/**
	 * @param session Newly created session
	 * @return Window created to display log messages for this session if using the OneWindowPerGameUI; if using a different UI then just returns null
	 */
	@Override
	public SessionWindow createWindowForNewSession (final MomSessionDescription session)
	{
		return null;
	}

	/**
	 * @param session Newly created session
	 * @param sessionWindow The session window created by createWindowForNewSession
	 * @param fileLogger Logger which writes to a disk file, if enabled
	 * @return Logger created and configured for this session
	 */
	@Override
	public Logger createLoggerForNewSession (final MomSessionDescription session, final SessionWindow sessionWindow, final Logger fileLogger)
	{
		final Logger sessionLogger = Logger.getLogger ("Session" + session.getSessionID ());
		sessionLogger.setLevel (Level.INFO);
		sessionLogger.setUseParentHandlers (false);

		// Careful here, when running test scripts, more than one test script will end up creating the same sesson logger
		// and so we have to make sure we don't add a new handler to it each time
/*		if (sessionLogger.getHandlers ().length == 0)
		{
			final Handler copyToDebugHandler = new WriteToOtherLogHandler (debugLogger, "[Session " + session.getSessionID () + "] ");
			copyToDebugHandler.setLevel (Level.INFO);

			// Would normally set the formatter here, but the formatter is irrelevant because the formatter of debugLogger is used instead
			sessionLogger.addHandler (copyToDebugHandler);

			// The debug logger then copies the message again to the file logger, so we don't need to worry about that here
		} */

		return sessionLogger;
	}

	/**
	 * Most of the UI's don't need to take an action when a session has no players left in it and is just about to end
	 * @param session The ending session
	 */
	@Override
	public void doSessionEnded (final MultiplayerSessionThread session)
	{
		// Do nothing
	}
}

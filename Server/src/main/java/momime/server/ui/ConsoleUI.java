package momime.server.ui;

import java.util.List;
import java.util.logging.Level;

import momime.server.MomServer;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;

/**
 * No GUI at all; msgs from all games are just written to the console
 */
public class ConsoleUI extends PrefixSessionIDsUI
{
	/**
	 * If writing the debug log to a text file is switched off, this is used to set the level of the whole debugLogger, so all the finer log level calls can be thrown out more quickly
	 * @return The log level above which the UI will directly display messages written to the debugLogger
	 */
	@Override
	public Level getMinimumDebugLoggerLogLevel ()
	{
		return Level.INFO;
	}

	/**
	 * Placeholder where the UI can perform any work startup work necessary, typically creating the main window
	 * By this stage the debug logger has been created, so if the UI wants to hook into this and add its own handler, it can do that here too
	 */
	@Override
	public void createMainWindow ()
	{
		// Debug to the console
		/* final Handler debugHandler = new ConsoleHandler ();
		debugHandler.setLevel (getMinimumDebugLoggerLogLevel ());
		debugHandler.setFormatter (new DateTimeAndMessageOnlyFormatter ());
		aDebugLogger.addHandler (debugHandler); */
	}

	/**
	 * Console UI doesn't display a session list
	 * @param server The server that the sessions are running on
	 * @param sessions The updated list of sessions
	 */
	@Override
	public void doSessionListUpdatedProcessing (final MomServer server, final List<MultiplayerSessionThread> sessions)
	{
		// Do nothing
	}
}

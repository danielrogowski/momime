package momime.server.ui;

import java.util.logging.Level;

/**
 * The same as "console" except that only SEVERE log messages are output so unless something goes wrong, the server is totally silent
 */
public final class NoUI extends ConsoleUI
{
	/**
	 * NoUI is identical to ConsoleUI except that only maximum severity messages are output
	 * @return The log level above which the UI will directly display messages written to the debugLogger
	 */
	@Override
	public Level getMinimumDebugLoggerLogLevel ()
	{
		return Level.SEVERE;
	}
}

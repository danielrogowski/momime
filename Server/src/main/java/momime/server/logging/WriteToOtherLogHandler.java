package momime.server.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Handler which outputs its log messages to another logger, with an optional prefix
 */
public final class WriteToOtherLogHandler extends Handler
{
	/**
	 * The logger to output messages to
	 */
	private final Logger destinationLogger;

	/**
	 * The prefix to add on the start of all messages before routing them to the other logger, or null if this is not required
	 */
	private final String prefix;

	/**
	 * Creates a handler which outputs its log messages to another logger
	 * @param aDestinationLogger The logger to output messages to
	 * @param aPrefix The prefix to add on the start of all messages before routing them to the other logger, or null if this is not required
	 */
	public WriteToOtherLogHandler (final Logger aDestinationLogger, final String aPrefix)
	{
		super ();
		destinationLogger = aDestinationLogger;
		prefix = aPrefix;
	}

	/**
	 * Outputs a log record to the other logger
	 * @param record The log record to output
	 */
	@Override
	public final void publish (final LogRecord record)
	{
		if (isLoggable (record))
		{
			if (prefix != null)
				record.setMessage (prefix + record.getMessage ());

			destinationLogger.log (record);
		}
	}

	/**
	 * Can put code here to close off the stream, but its not appopriate for logging to another logger
	 */
	@Override
	public final void close ()
	{
	}

	/**
	 * Can put code here to flush the stream, but its not appopriate for logging to another logger
	 */
	@Override
	public final void flush ()
	{
	}
}

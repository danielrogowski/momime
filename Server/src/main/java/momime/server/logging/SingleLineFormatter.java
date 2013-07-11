package momime.server.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import momime.server.ui.ConsoleUI;

/**
 * This is designed to match the SimpleFormatter format in the default MoMIMEServerLogging.properties file
 */
public final class SingleLineFormatter extends Formatter
{
	/** Include the class and method name in the output? */
	private final boolean includeClassAndMethodName;
	
	/** Convert logger names like MoMIMESession.1 into a prefix like [Session 1] */
	private final boolean includeSessionIdFromLoggerName;
	
	/**
	 * Sets flags at creation time
	 * @param anIncludeClassAndMethodName Include the class and method name in the output?
	 * @param anIncludeSessionIdFromLoggerName Convert logger names like MoMIMESession.1 into a prefix like [Session 1]
	 */
	public SingleLineFormatter (final boolean anIncludeClassAndMethodName, final boolean anIncludeSessionIdFromLoggerName)
	{
		includeClassAndMethodName = anIncludeClassAndMethodName;
		includeSessionIdFromLoggerName = anIncludeSessionIdFromLoggerName;
	}
	
	/**
	 * @param record Log record to format
	 * @return Formatted log record
	 */
	@Override
	public final String format (final LogRecord record)
	{
		// Start off with the date & time
		final StringBuffer sb = new StringBuffer ("[");
		sb.append (LoggingConstants.FULL_DATE_TIME_FORMAT.format (new Date (record.getMillis ())));
		sb.append (" ");
		
		// Add the logging level
		sb.append (record.getLevel ().getLocalizedName ());

		// Add the thread ID
		// sb.append (" T");
		// sb.append (record.getThreadID ());

		sb.append ("] ");
		
		// Add the class name or logger name
		if (includeClassAndMethodName)
		{
			if (record.getSourceClassName () != null)
				sb.append (record.getSourceClassName ());
			else
				sb.append (record.getLoggerName ());

			// Add the method name
			if (record.getSourceMethodName () != null)
			{
				sb.append (" ");
				sb.append (record.getSourceMethodName ());
			}
		}
		
		// Include the MoM session ID?
		if ((includeSessionIdFromLoggerName) && (record.getLoggerName () != null))
			if (record.getLoggerName ().startsWith (ConsoleUI.MOM_SESSION_LOGGER_PREFIX))
			{
				sb.append ("Session ");
				sb.append (record.getLoggerName ().substring (ConsoleUI.MOM_SESSION_LOGGER_PREFIX.length () + 1));
			}

		// Add the actual message text
		sb.append (": ");
		sb.append (formatMessage (record));

		// Add a stack trace
		if (record.getThrown () != null)
		{
			try
			{
				final StringWriter sw = new StringWriter ();
				try (final PrintWriter pw = new PrintWriter (sw))
				{
					record.getThrown ().printStackTrace (pw);
					pw.close ();
				}
				sb.append (sw.toString ());
			}
			catch (final Exception ex)
			{
			}
		}

		sb.append ("\r\n");

		// All done
		return sb.toString ();
	}
}

package momime.server.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Similiar output style to SimpleFormatter, except that I don't like the way the date/time/method is shown on a separate line from the actual message
 */
public final class SingleLineFormatter extends Formatter
{
	/**
	 * @param record Log record to format
	 * @return Formatted log record
	 */
	@Override
	public final String format (final LogRecord record)
	{
		// Start off with the date & time
		final StringBuffer sb = new StringBuffer (LoggingConstants.FULL_DATE_TIME_FORMAT.format (new Date (record.getMillis ())));

		// Add the thread ID
		sb.append (" T");
		sb.append (record.getThreadID ());

		// Add the class name or logger name
		sb.append (" ");
		if (record.getSourceClassName () != null)
			sb.append (record.getSourceClassName ());
		else
			sb.append (record.getLoggerName ());

		// Add the method name
		if (record.getSourceMethodName () != null)
		{
			sb.append (", ");
			sb.append (record.getSourceMethodName ());
		}

		// Add the log level
		sb.append (", ");
		sb.append (record.getLevel ().getLocalizedName ());

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

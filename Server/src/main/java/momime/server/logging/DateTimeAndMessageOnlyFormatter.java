package momime.server.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Really simple formatter, which only displays the date, time and message
 */
public final class DateTimeAndMessageOnlyFormatter extends Formatter
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

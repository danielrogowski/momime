package momime.server.logging;

import java.text.SimpleDateFormat;

/**
 * Formatting constants for logging
 */
public final class LoggingConstants
{
	/** Full date and time format showing every value */
	public static final String FULL_DATE_TIME_FORMAT_STRING = "d MMM yyyy H:mm:ss";

	/** Full date and time format showing every value */
	public static final SimpleDateFormat FULL_DATE_TIME_FORMAT = new SimpleDateFormat (FULL_DATE_TIME_FORMAT_STRING);
}

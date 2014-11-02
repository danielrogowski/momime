package momime.common;

import java.io.IOException;

/**
 * Type of exception thrown by MoM IME code
 */
public class MomException extends IOException
{
	/**
	 * @param message Exception message
	 */
	public MomException (final String message)
	{
		super (message);
	}
}
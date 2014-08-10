package momime.common;

import java.io.IOException;

/**
 * Type of exception thrown by MoM IME code
 */
public class MomException extends IOException
{
	/** Unique value for serialization */
	private static final long serialVersionUID = 5462827758709511076L;

	/**
	 * @param message Exception message
	 */
	public MomException (final String message)
	{
		super (message);
	}

}

package momime.server.ui;

/**
 * MomServerUI instances are created by log4j rather than spring, so need a singleton to allow spring managed classes to access the UI.
 */
public final class MomServerUIHolder
{
	/** The UI, may be null if running in console mode */
	private static MomServerUI ui;
	
	/**
	 * Prevent instantiation
	 */
	private MomServerUIHolder ()
	{
	}

	/**
	 * @return The UI, may be null if running in console mode
	 */
	public final static MomServerUI getUI ()
	{
		return ui;
	}

	/**
	 * @param obj The UI, may be null if running in console mode
	 */
	public final static void setUI (final MomServerUI obj)
	{
		ui = obj;
	}
}
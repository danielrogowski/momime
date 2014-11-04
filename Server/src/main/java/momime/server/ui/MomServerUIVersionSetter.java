package momime.server.ui;

/**
 * The maven build version number is read in by Spring and needs to be passed to the UI for display.  We could do so from MomServer, but that only starts
 * up after all the user registry, server XMLs and network connections are initialized, and it looks weird to leave the UI with no version number in the title for so long.
 */
public final class MomServerUIVersionSetter
{
	/**
	 * Server calls this to tell the UI what version number to display
	 * @param version Maven version of MoM IME server build
	 */
	public final void setVersion (final String version)
	{
		if (getUI () != null)
			getUI ().setVersion (version);
	}

	/**
	 * @return UI to display server status
	 */
	public final MomServerUI getUI ()
	{
		return MomServerUIHolder.getUI ();
	}
}
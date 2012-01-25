package momime.server;

import java.util.logging.Logger;

import momime.server.config.v0_9_4.MomImeServerConfig;
import momime.server.ui.MomServerUI;

/**
 * Because of the way sessions start up you can only pass in a single parameter, and we need multiple, so wrap them into an object
 */
final class MomSessionConstructorParam
{
	/** Server config loaded from XML config file */
	private final MomImeServerConfig config;

	/** UI being used by server */
	private final MomServerUI ui;

	/** Logger which writes to a disk file, if enabled */
	private final Logger fileLogger;

	/**
	 * @param aConfig Server config loaded from XML config file
	 * @param aUI UI being used by server
	 * @param aFileLogger Logger which writes to a disk file, if enabled
	 */
	MomSessionConstructorParam (final MomImeServerConfig aConfig, final MomServerUI aUI, final Logger aFileLogger)
	{
		super ();

		config = aConfig;
		ui = aUI;
		fileLogger = aFileLogger;
	}

	/**
	 * @return Server config loaded from XML config file
	 */
	final MomImeServerConfig getConfig ()
	{
		return config;
	}

	/**
	 * @return UI being used by server
	 */
	final MomServerUI getUI ()
	{
		return ui;
	}

	/**
	 * @return Logger which writes to a disk file, if enabled
	 */
	final Logger getFileLogger ()
	{
		return fileLogger;
	}
}

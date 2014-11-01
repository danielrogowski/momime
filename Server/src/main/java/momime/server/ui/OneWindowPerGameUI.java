package momime.server.ui;

import momime.common.messages.MomSessionDescription;

import org.apache.commons.logging.Log;

/**
 * Similar to the old Delphi MoM IME server, opens up a new window for each game in progress, and msgs relating to that game appear in that window
 */
public final class OneWindowPerGameUI extends SingleWindowUI
{
	// Inherited code sorts out creating the main window, outputting the debugLogger to the main window, and showing the session list
	// So we just have to deal with the individual session windows here

	/**
	 * @param session Newly created session
	 * @return Window created to display log messages for this session if using the OneWindowPerGameUI; if using a different UI then just returns null
	 */
	@Override
	public final SessionWindow createWindowForNewSession (final MomSessionDescription session)
	{
		return new SessionWindow (session);
	}

	/**
	 * @param session Newly created session
	 * @param sessionWindow The session window created by createWindowForNewSession
	 * @return Logger created and configured for this session
	 */
	@Override
	public final Log createLoggerForNewSession (final MomSessionDescription session, final SessionWindow sessionWindow)
	{
		// Every window gets its own logger
		// Disconnect it from the parent logger, since the parent logger logs messages to the main window
		final Log sessionLogger = super.createLoggerForNewSession (session, sessionWindow);
/*		sessionLogger.setLevel (Level.INFO);
		sessionLogger.setUseParentHandlers (false);

		final Handler sessionHandler = new SessionWindowHandler (sessionWindow);
		sessionHandler.setLevel (Level.INFO);
		sessionHandler.setFormatter (new DateTimeAndMessageOnlyFormatter ());
		sessionLogger.addHandler (sessionHandler); */

		return sessionLogger;
	}

	/**
	 * Log handler which outputs to one session window
	 */
//	private final class SessionWindowHandler extends Handler
//	{
		/** The session window this handler is outtputting to */
//		private final SessionWindow sessionWindow;

		/**
		 * Creates a log handler which outputs to one session window
		 * @param aSessionWindow The session window this handler is outtputting to
		 */
/*		private SessionWindowHandler (final SessionWindow aSessionWindow)
		{
			super ();
			sessionWindow = aSessionWindow;
		} */

		/**
		 * Outputs a log record to the window
		 * Has to be synchronized so two methods can't be trying to update the window at the same time
		 * @param record The log record to write to the text area
		 */
/*		@Override
		public synchronized void publish (final LogRecord record)
		{
			// This is pretty much copied from StreamHandler
			if (isLoggable (record))
			{
				String msg;
				try
				{
					msg = getFormatter ().format (record);
				}
				catch (final Exception ex)
				{
					// We don't want to throw an exception here, but we
					// report the exception to any registered ErrorManager.
					reportError (null, ex, ErrorManager.FORMAT_FAILURE);
					return;
				}

				try
				{
					sessionWindow.addLine (msg);
				}
				catch (final Exception ex)
				{
					// We don't want to throw an exception here, but we
					// report the exception to any registered ErrorManager.
					reportError (null, ex, ErrorManager.WRITE_FAILURE);
				}
			}
		} */

		/**
		 * Can put code here to close off the stream, but its not appopriate for logging to the text area
		 */
/*		@Override
		public void close ()
		{
		} */

		/**
		 * Can put code here to flush the stream, but its not appopriate for logging to the text area
		 */
/*		@Override
		public void flush ()
		{
		}
	} */
}
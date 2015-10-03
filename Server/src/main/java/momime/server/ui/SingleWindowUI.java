package momime.server.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import momime.common.messages.MomSessionDescription;
import momime.common.messages.TurnSystem;
import momime.server.MomServer;
import momime.server.MomSessionThread;
import momime.server.logging.LoggingConstants;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Custom Log4J appender that popups up a UI window and displays all messages in it
 */
public final class SingleWindowUI extends AppenderSkeleton implements MomServerUI
{
	/** Messages from session-specific loggers have a different layout, so that they include the session number */
	private Layout sessionLayout;
	
	/** Whether to create separate windows to log session info */
	private boolean separateWindowForEachSession;
	
	/** The text area on the form to write log messages to */
	private JTextArea textArea;

	/** Number of lines of text currently in the window */
	private int linesOfText;

	/** Number of lines of text we put in the window before we start wrapping lines off the top */
	private static final int MAX_LINE_COUNT = 400;

	/** The table model for displaying the list of sessions */
	private SingleWindowTableModel tableModel;
	
	/** The UI frame */
	private JFrame frame;
	
	/** Map of session IDs to session windows, only used if separateWindowForEachSession = true */
	private Map<Integer, SessionWindow> sessionWindows = new HashMap<Integer, SessionWindow> ();

	/**
	 * Sets up and displays the UI window
	 */
	public SingleWindowUI ()
	{
		MomServerUIHolder.setUI (this);
		
		SwingUtilities.invokeLater (() ->
		{
			new NdgUIUtilsImpl ().useNimbusLookAndFeel ();
	
			// Initialize the frame
			frame = new JFrame ();
			frame.setSize (1200, 500);
			frame.setTitle ("Master of Magic - Implode's Multiplayer Edition - Server");
			frame.setDefaultCloseOperation (WindowConstants.DO_NOTHING_ON_CLOSE);
	
			// Closing the main window kills the server, so ask for confirmation before doing so
			frame.addWindowListener (new WindowAdapter ()
			{
				/**
				 * User has clicked the X to close the window - make sure they really want to
				 */
				@Override
				public final void windowClosing (final WindowEvent e)
				{
					if (JOptionPane.showConfirmDialog (null, "Closing the main window will end the MoM IME server and any sessions currently in progress.  Are you sure?",
						frame.getTitle (), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
	
						System.exit (0);
				}
			});
	
			// Initialize the content pane
			final JPanel contentPane = new JPanel ();
			contentPane.setBorder (BorderFactory.createCompoundBorder (BorderFactory.createEmptyBorder (SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE, SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE,
				SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE, SwingLayoutConstants.CONTENT_PANE_BORDER_SIZE), contentPane.getBorder ()));
			contentPane.setLayout (new BoxLayout (contentPane, BoxLayout.Y_AXIS));
	
			// Create the list of log messages
			textArea = new JTextArea ();
			linesOfText = 0;
	
			// Put the list of log messages in a scroll pane
			final JScrollPane scrollPane = new JScrollPane (textArea);
			scrollPane.setAlignmentX (Component.LEFT_ALIGNMENT);
			scrollPane.setAlignmentY (Component.TOP_ALIGNMENT);
			scrollPane.setMinimumSize (new Dimension (100, 50));
			scrollPane.setPreferredSize (new Dimension (100, 50));
			contentPane.add (scrollPane);
	
			contentPane.add (Box.createRigidArea (new Dimension (0, SwingLayoutConstants.SPACE_BETWEEN_CONTROLS)));
	
			// Create the table showing the games in progress
			tableModel = new SingleWindowTableModel ();
			final JTable table = new JTable (tableModel, new SingleWindowColumnModel ());
			table.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);		// So it actually pays attention to the preferred widths
			table.setAutoCreateColumnsFromModel (true);	// Without this, no TableColumns are added to the column model
			table.setRowSelectionAllowed (true);
			table.setColumnSelectionAllowed (false);
	
			// Put the grid into a scrolling area
			final JScrollPane tableScroll = new JScrollPane (table);
			tableScroll.setAlignmentX (Component.LEFT_ALIGNMENT);
			tableScroll.setAlignmentY (Component.TOP_ALIGNMENT);
			tableScroll.setMinimumSize (new Dimension (100, 50));
			tableScroll.setPreferredSize (new Dimension (100, 50));
			contentPane.add (tableScroll);
	
			// Show frame
			frame.setContentPane (contentPane);
			frame.setVisible (true);
		});
	}

	/**
	 * Server calls this to tell the UI what version number to display
	 * @param version Maven version of MoM IME server build
	 */
	@Override
	public final void setVersion (final String version)
	{
		frame.setTitle ("Master of Magic - Implode's Multiplayer Edition - Server v" + version);
	}
	
	/**
	 * Close down the UI window when the appender closes down
	 */
	@Override
	public final void close ()
	{
		frame.dispose ();
	}

	/**
	 * @return True, since we do require a layout to format log messages for us
	 */
	@Override
	public final boolean requiresLayout ()
	{
		return true;
	}

	/**
	 * @param event Displays a log message in the UI
	 */
	@Override
	protected final void append (final LoggingEvent event)
	{
		SwingUtilities.invokeLater (() ->
		{
			// Log here, or in separate session window?
			final boolean isSessionLog = event.getLoggerName ().startsWith (MomSessionThread.MOM_SESSION_LOGGER_PREFIX);
			boolean done = false;
			if ((isSeparateWindowForEachSession ()) && (isSessionLog))
			{
				final int sessionID = Integer.parseInt (event.getLoggerName ().substring (MomSessionThread.MOM_SESSION_LOGGER_PREFIX.length ()));
				final SessionWindow sessionWindow = sessionWindows.get (sessionID);
				if (sessionWindow != null)
				{
					sessionWindow.addLine (layout.format (event));
					done = true;
				}
			}

			if (!done)
			{
				// Strip off old lines to make space
				while (linesOfText >= MAX_LINE_COUNT)
				{
					textArea.setText (textArea.getText ().substring (textArea.getText ().indexOf (System.lineSeparator ()) + System.lineSeparator ().length ()));
					linesOfText--;
				}
		
				// Use sessionLayout for messages from session loggers, and regular layout for global messages
				final Layout useLayout = isSessionLog ? getSessionLayout () : layout;
				textArea.setText (textArea.getText () + useLayout.format (event));
				linesOfText++;
				
				// Scroll to the bottom
				textArea.setCaretPosition (textArea.getDocument ().getLength ());
			}
		});
	}
	
	/**
	 * @param session Newly created session
	 */
	@Override
	public final void createWindowForNewSession (final MomSessionDescription session)
	{
		// Create new window?
		if (isSeparateWindowForEachSession ())
		{
			final SessionWindow newWindow = new SessionWindow (session);
			sessionWindows.put (session.getSessionID (), newWindow);
		}
	}

	/**
	 * Update the list of sessions when a session is either added or closed
	 * This method is already synchronized by the fact that whatever is calling it must obtain a write lock on the session list
	 * @param server The server that the sessions are running on
	 * @param sessions The updated list of sessions
	 */
	@Override
	public final void doSessionListUpdatedProcessing (final MomServer server, final List<MultiplayerSessionThread> sessions)
	{
		// Copy out all the session and player descriptions
		/*
		final SessionAndPlayerDescriptions [] sessionDescriptions = new SessionAndPlayerDescriptions [sessions.size ()];
		for (int sessionNo = 0; sessionNo < sessions.size (); sessionNo++)
		{
			final MultiplayerSessionThread thisSession = sessions.get (sessionNo);
			final PlayerDescription [] playerDescriptions = new PlayerDescription [thisSession.get

				playerDescriptions = server.createPlayerDescriptionsArray (players.size ());
				for (int playerNo = 0; playerNo < players.size (); playerNo++)
					playerDescriptions [playerNo] = players.get (playerNo).getPlayerDescription ();
			}
			finally
			{
				thisSession.unlockPlayersListForReading ();
			}

			sessionDescriptions [sessionNo] = server.createSessionAndPlayerDescriptions (thisSession.getSessionDescription (), playerDescriptions);
		}

		tableModel.setSessions (sessionDescriptions); */
	}

	/**
	 * Allows the UI to perform some action when a session has been added to the server's session list
	 * @param session The new session
	 */
	@Override
	public final void sessionAdded (final MultiplayerSessionThread session)
	{
		final SessionAndPlayerDescriptions spd = new SessionAndPlayerDescriptions ();
		spd.setSessionDescription (session.getSessionDescription ());
		spd.getPlayer ().add (session.getPlayers ().get (0).getPlayerDescription ());
		tableModel.sessions.add (spd);
		tableModel.fireTableDataChanged ();
	}
	
	/**
	 * Allows the UI to perform some action when a session has been removed from the server's session list
	 * @param session The removed session
	 */
	@Override
	public final void sessionRemoved (final MultiplayerSessionThread session)
	{
		final Iterator<SessionAndPlayerDescriptions> iter = tableModel.sessions.iterator ();
		while (iter.hasNext ())
			if (iter.next ().getSessionDescription ().getSessionID ().equals (session.getSessionDescription ().getSessionID ()))
				iter.remove ();
		
		tableModel.fireTableDataChanged ();
		
		// Hide window?
		if (isSeparateWindowForEachSession ())
		{
			final SessionWindow removeWindow = sessionWindows.get (session.getSessionDescription ().getSessionID ());
			if (removeWindow != null)
			{
				removeWindow.close ();
				sessionWindows.remove (session.getSessionDescription ().getSessionID ());
			}
		}
	}

	/**
	 * @return Messages from session-specific loggers have a different layout, so that they include the session number
	 */
	public final Layout getSessionLayout ()
	{
		return sessionLayout;
	}

	/**
	 * @param sessLayout Messages from session-specific loggers have a different layout, so that they include the session number
	 */
	public final void setSessionLayout (final Layout sessLayout)
	{
		sessionLayout = sessLayout;
	}

	/**
	 * @return Whether to create separate windows to log session info
	 */
	public final boolean isSeparateWindowForEachSession ()
	{
		return separateWindowForEachSession;
	}

	/**
	 * @param sep Whether to create separate windows to log session info
	 */
	public final void setSeparateWindowForEachSession (final boolean sep)
	{
		separateWindowForEachSession = sep;
	}
	
	/**
	 * Table model which displays all the sessions currently running on the server
	 * Keeps its own local copy of the session descriptions, otherwise we'd have to obtain read locks in every one of the methods here
	 */
	private final class SingleWindowTableModel extends AbstractTableModel
	{
		/** Local copy of the list of sessions */
		private final List<SessionAndPlayerDescriptions> sessions;

		/**
		 * Creates a table model which displays all the sessions currently running on the server
		 */
		private SingleWindowTableModel ()
		{
			super ();

			sessions = new ArrayList<SessionAndPlayerDescriptions> ();
		}

		/**
		 * @return Number of columns to display in the table
		 */
		@Override
		public final int getColumnCount ()
		{
			return 6;
		}

		/**
		 * @return Number of rows to display in the table
		 */
		@Override
		public final int getRowCount ()
		{
			return sessions.size ();
		}

		/**
		 * @param columnIndex Column number to get the heading for
		 * @return Title to display for each column
		 */
		@Override
		public final String getColumnName (final int columnIndex)
		{
			String result = null;
			switch (columnIndex)
			{
				case 0:
					result = "Session name";
					break;
				case 1:
					result = "Started at";
					break;
				case 2:
					result = "Player who created session";
					break;
				case 3:
					result = "Map size";
					break;
				case 4:
					result = "Turn system";
					break;
				case 5:
					result = "Database used";
					break;
			}
			return result;
		}

		/**
		 * @param rowIndex Row number to get the value for
		 * @param columnIndex Column number to get the value for
		 * @return Value to display in this cell
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			Object result = null;

			if ((rowIndex >= 0) && (rowIndex < sessions.size ()))
			{
				final SessionAndPlayerDescriptions thisSession = sessions.get (rowIndex);
				final MomSessionDescription sd = (MomSessionDescription) thisSession.getSessionDescription ();

				switch (columnIndex)
				{
					case 0:
						result = sd.getSessionName ();
						break;
					case 1:
						result = LoggingConstants.FULL_DATE_TIME_FORMAT.format (sd.getStartedAt ().toGregorianCalendar ().getTime ());
						break;
					case 2:
						for (final PlayerDescription thisPlayer : thisSession.getPlayer ())
						{
							if (result == null)
								result = thisPlayer.getPlayerName ();
							else
								result = result + ", " + thisPlayer.getPlayerName ();
						}
						break;
					case 3:
						result = sd.getOverlandMapSize ().getWidth () + " x " + sd.getOverlandMapSize ().getHeight ();
						break;
					case 4:
						if (sd.getTurnSystem () == TurnSystem.ONE_PLAYER_AT_A_TIME)
							result = "One at a time";
						else
							result = "Simultaneous";
						break;
					case 5:
						result = sd.getXmlDatabaseName ();
						break;
				}
			}

			return result;
		}
	}

	/**
	 * Column model which displays all the sessions currently running on the server
	 */
	private final class SingleWindowColumnModel extends DefaultTableColumnModel
	{
		/**
		 * Set column widths as columns are added
		 * @param aColumn The new column to add
		 */
		@Override
		public final void addColumn (final TableColumn aColumn)
		{
			final int columnIndex = getColumnCount ();
			int width = 0;

			switch (columnIndex)
			{
				case 0:
					width = 135; // Session name
					break;
				case 1:
					width = 125; // Started at
					break;
				case 2:
					width = 150; // Players
					break;
				case 3:
					width = 80; // Map size
					break;
				case 4:
					width = 100; // Turn system
					break;
				case 5:
					width = 200; // Database used
					break;
			}

			aColumn.setPreferredWidth (width);
			super.addColumn (aColumn);
		}
	}
}
package momime.server.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.server.MomServer;
import momime.server.logging.DateTimeAndMessageOnlyFormatter;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;

/**
 * Single window so all msgs from all games appear in the same window
 */
public class SingleWindowUI extends PrefixSessionIDsUI
{
	/** The text area on the form to write log messages to */
	private JTextArea textArea;

	/** Number of lines of text currently in the window */
	private int linesOfText;

	/** Number of lines of text we put in the window before we start wrapping lines off the top */
	private static final int MAX_LINE_COUNT = 400;

	/** The table model for displaying the list of sessions */
	private SingleWindowTableModel tableModel;

	/**
	 * If writing the debug log to a text file is switched off, this is used to set the level of the whole debugLogger, so all the finer log level calls can be thrown out more quickly
	 * @return The log level above which the UI will directly display messages written to the debugLogger
	 */
	@Override
	public final Level getMinimumDebugLoggerLogLevel ()
	{
		return Level.INFO;
	}

	/**
	 * Placeholder where the UI can perform any work startup work necessary, typically creating the main window
	 * By this stage the debug logger has been created, so if the UI wants to hook into this and add its own handler, it can do that here too
	 * @param aDebugLogger Logger to write to debug text file when the debug log is enabled
	 */
	@Override
	public final void createMainWindow (final Logger aDebugLogger)
	{
		// Switch to Windows look and feel if available
		try
		{
			UIManager.setLookAndFeel ("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		}
		catch (final Exception e)
		{
			// Don't worry if can't switch look and feel
		}

		// Initialize the frame
		final JFrame frame = new JFrame ();
		frame.setSize (900, 500);
		frame.setTitle ("Master of Magic - Implode's Multiplayer Edition - Server " + CommonDatabaseConstants.MOM_IME_VERSION);
		frame.setDefaultCloseOperation (JFrame.DO_NOTHING_ON_CLOSE);

		// Closing the main window kills the server, so ask for confirmation before doing so
		frame.addWindowListener (new WindowAdapter ()
		{
			/**
			 * User has clicked the X to close the window - make sure they really want to
			 * @param e Window closing event
			 */
			@Override
			public void windowClosing (final WindowEvent e)
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
		textArea.setFont (new JTextField ().getFont ());	// Otherwise defaults to a courier type font?
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

		// Debug to the text area
		final Handler debugHandler = new SingleWindowHandler ();
		debugHandler.setLevel (getMinimumDebugLoggerLogLevel ());
		debugHandler.setFormatter (new DateTimeAndMessageOnlyFormatter ());
		aDebugLogger.addHandler (debugHandler);

		// Show frame
		frame.setContentPane (contentPane);
		frame.setVisible (true);
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
	 * Log handler which outputs to the text area on the form
	 */
	private final class SingleWindowHandler extends Handler
	{
		/**
		 * Outputs a log record to the window
		 * Has to be synchronized so two methods can't be trying to update the window at the same time
		 * @param record The log record to write to the text area
		 */
		@Override
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
					// Strip off old lines to make space
					while (linesOfText >= MAX_LINE_COUNT)
					{
						textArea.setText (textArea.getText ().substring (textArea.getText ().indexOf ("\r\n") + 2));
						linesOfText--;
					}

					// Add new text
					textArea.setText (textArea.getText () + msg);
					linesOfText++;
				}
				catch (final Exception ex)
				{
					// We don't want to throw an exception here, but we
					// report the exception to any registered ErrorManager.
					reportError (null, ex, ErrorManager.WRITE_FAILURE);
				}
			}
		}

		/**
		 * Can put code here to close off the stream, but its not appopriate for logging to the text area
		 */
		@Override
		public final void close ()
		{
		}

		/**
		 * Can put code here to flush the stream, but its not appopriate for logging to the text area
		 */
		@Override
		public final void flush ()
		{
		}
	}

	/**
	 * Table model which displays all the sessions currently running on the server
	 * Keeps its own local copy of the session descriptions, otherwise we'd have to obtain read locks in every one of the methods here
	 */
	private final class SingleWindowTableModel extends AbstractTableModel
	{
		/**
		 * Unique value for serialization
		 */
		private static final long serialVersionUID = -3021595934938811021L;

		/**
		 * Local copy of the list of sessions
		 */
		private SessionAndPlayerDescriptions [] sessions;

		/**
		 * Creates a table model which displays all the sessions currently running on the server
		 */
		private SingleWindowTableModel ()
		{
			super ();

			// Initially there won't be any sessions running, so create an empty array
			sessions = new SessionAndPlayerDescriptions [0];
		}

		/**
		 * Allows replacing the list of sessions being displayed
		 * @param aSessions Local copy of the list of sessions
		 */
		private final void setSessions (final SessionAndPlayerDescriptions [] aSessions)
		{
			sessions = aSessions;
			fireTableDataChanged ();
		}

		/**
		 * @return Number of columns to display in the table
		 */
		@Override
		public int getColumnCount ()
		{
			return 6;
		}

		/**
		 * @return Number of rows to display in the table
		 */
		@Override
		public int getRowCount ()
		{
			return sessions.length;
		}

		/**
		 * @param columnIndex Column number to get the heading for
		 * @return Title to display for each column
		 */
		@Override
		public String getColumnName (final int columnIndex)
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
		public Object getValueAt (final int rowIndex, final int columnIndex)
		{
			Object result = null;

			if ((rowIndex >= 0) && (rowIndex < sessions.length))
			{
				final SessionAndPlayerDescriptions thisSession = sessions [rowIndex];
				final MomSessionDescription sd = (MomSessionDescription) thisSession.getSessionDescription ();

				switch (columnIndex)
				{
					case 0:
						result = sd.getSessionName ();
						break;
					case 1:
						result = sd.getStartedAt ();
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
						result = sd.getMapSize ().getWidth () + " x " + sd.getMapSize ().getHeight ();
						break;
					case 4:
						result = sd.getTurnSystem ();
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
		 * Unique value for serialization
		 */
		private static final long serialVersionUID = -1789692374701601514L;

		/**
		 * Set column widths as columns are added
		 * @param aColumn The new column to add
		 */
		@Override
		public void addColumn (final TableColumn aColumn)
		{
			final int columnIndex = getColumnCount ();
			int width = 0;

			switch (columnIndex)
			{
				case 0:
					width = 135; // Session name
					break;
				case 1:
					width = 165; // Started at
					break;
				case 2:
					width = 150; // Players
					break;
				case 3:
					width = 60; // Map size
					break;
				case 4:
					width = 80; // Turn system
					break;
				case 5:
					width = 180; // Database used
					break;
			}

			aColumn.setPreferredWidth (width);
			super.addColumn (aColumn);
		}
	}
}

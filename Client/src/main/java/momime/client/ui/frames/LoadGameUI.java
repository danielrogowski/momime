package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.common.messages.MomSessionDescription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.sessionbase.BrowseSavePoints;
import com.ndg.multiplayer.sessionbase.LoadGame;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.SavedGamePoint;
import com.ndg.multiplayer.sessionbase.SavedGameSession;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;
import com.ndg.utils.DateFormats;

/**
 * Screen for showing a list of saved games and save points that are running on the server so we can select one to load
 */
public final class LoadGameUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (LoadGameUI.class);

	/** XML layout */
	private XmlLayoutContainerEx joinGameLayout;

	/** Large font */
	private Font largeFont;
	
	/** Tiny font */
	private Font tinyFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Select saved game action */
	private Action selectSavedGameAction;

	/** Select saved game button */
	private JButton selectSavedGameButton;
	
	/** Select save point action */
	private Action selectSavePointAction;

	/** Select save point button */
	private JButton selectSavePointButton;
	
	/** Cancel action */
	private Action cancelAction;

	/** Back action */
	private Action backAction;
	
	/** Back button */
	private JButton backButton;
	
	/** List of saved games we can reload */
	private List<SavedGameSession> savedGames = new ArrayList<SavedGameSession> ();
	
	/** Table of saved games we can reload */
	private JTable savedGamesTable;
	
	/** Scroll pane containing table of saved games */
	private JScrollPane savedGamesTablePane;
	
	/** Table model of saved games we can reload */
	private final SavedGamesTableModel savedGamesTableModel = new SavedGamesTableModel ();

	/** List of save points we can reload */
	private List<SavedGamePoint> savePoints = new ArrayList<SavedGamePoint> ();
	
	/** Table of save points we can reload */
	private JTable savePointsTable;

	/** Scroll pane containing table of save points */
	private JScrollPane savePointsTablePane;
	
	/** Table model of save points we can reload */
	private final SavePointsTableModel savePointsTableModel = new SavePointsTableModel ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/sessionList.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldDisabled.png");

		// Actions
		cancelAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().setVisible (false);
			}
		};
		
		selectSavedGameAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				selectSavePointAction.setEnabled (false);
				
				final SavedGameSession savedGame = getSavedGames ().get (savedGamesTable.getSelectedRow ());

				final BrowseSavePoints msg = new BrowseSavePoints ();
				msg.setSavedGameID (savedGame.getSessionDescription ().getSavedGameID ());
				
				try
				{
					getClient ().getServerConnection ().sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		selectSavePointAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				final SavedGameSession savedGame = getSavedGames ().get (savedGamesTable.getSelectedRow ());
				final SavedGamePoint savePoint = getSavePoints ().get (savePointsTable.getSelectedRow ());

				final LoadGame msg = new LoadGame ();
				msg.setSavedGameID (savedGame.getSessionDescription ().getSavedGameID ());
				msg.setSavedGameFilename (savePoint.getSavedGameFilename ());
				
				try
				{
					getClient ().getServerConnection ().sendMessageToServer (msg);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};

		// Back goes back from the save points to the list of all saved games
		backAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				savedGamesTablePane.setVisible (true);
				savePointsTablePane.setVisible (false);
				selectSavedGameButton.setVisible (true);
				selectSavePointButton.setVisible (false);
				backButton.setVisible (false);
			}
		};
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};

		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getJoinGameLayout ()));
		
		backButton = getUtils ().createImageButton (backAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled);
		contentPane.add (backButton, "frmJoinGameRefresh");
		
		selectSavedGameButton = getUtils ().createImageButton (selectSavedGameAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled);
		contentPane.add (selectSavedGameButton, "frmJoinGameConfirm");

		selectSavePointButton = getUtils ().createImageButton (selectSavePointAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled);
		contentPane.add (selectSavePointButton, "frmJoinGameConfirm");
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmJoinGameCancel");
		
		// Saved games table
		savedGamesTable = new JTable ();
		savedGamesTable.setModel (savedGamesTableModel);
		savedGamesTable.setFont (getTinyFont ());
		savedGamesTable.setForeground (MomUIConstants.SILVER);
		savedGamesTable.setBackground (new Color (0, 0, 0, 0));
		savedGamesTable.getTableHeader ().setFont (getTinyFont ());
		savedGamesTable.setOpaque (false);
		savedGamesTable.setRowSelectionAllowed (true);
		savedGamesTable.setColumnSelectionAllowed (false);
		
		savedGamesTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);		// So it actually pays attention to the preferred widths
		savedGamesTable.getColumnModel ().getColumn (0).setPreferredWidth (80);
		savedGamesTable.getColumnModel ().getColumn (1).setPreferredWidth (70);
		savedGamesTable.getColumnModel ().getColumn (2).setPreferredWidth (140);
		savedGamesTable.getColumnModel ().getColumn (3).setPreferredWidth (180);
		
		savedGamesTablePane = new JScrollPane (savedGamesTable);
		savedGamesTablePane.getViewport ().setOpaque (false);
		contentPane.add (savedGamesTablePane, "frmJoinGameSessions");
		
		savedGamesTable.getSelectionModel ().addListSelectionListener (new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				// Enable button as soon as a row is clicked on
				selectSavedGameAction.setEnabled (true);
			}
		});
		
		// Save points table
		savePointsTable = new JTable ();
		savePointsTable.setModel (savePointsTableModel);
		savePointsTable.setFont (getTinyFont ());
		savePointsTable.setForeground (MomUIConstants.SILVER);
		savePointsTable.setBackground (new Color (0, 0, 0, 0));
		savePointsTable.getTableHeader ().setFont (getTinyFont ());
		savePointsTable.setOpaque (false);
		savePointsTable.setRowSelectionAllowed (true);
		savePointsTable.setColumnSelectionAllowed (false);
		
		savePointsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);		// So it actually pays attention to the preferred widths
		savePointsTable.getColumnModel ().getColumn (0).setPreferredWidth (150);
		savePointsTable.getColumnModel ().getColumn (1).setPreferredWidth (70);
		
		savePointsTablePane = new JScrollPane (savePointsTable);
		savePointsTablePane.getViewport ().setOpaque (false);
		contentPane.add (savePointsTablePane, "frmJoinGameSessions");
		
		savePointsTable.getSelectionModel ().addListSelectionListener (new ListSelectionListener ()
		{
			@Override
			public final void valueChanged (final ListSelectionEvent ev)
			{
				// Enable button as soon as a row is clicked on
				selectSavePointAction.setEnabled (true);
			}
		});
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmLoadGame", "Title"));
		
		selectSavedGameAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmLoadGame", "SelectSavedGame"));
		selectSavePointAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmLoadGame", "SelectSavePoint"));
		backAction.putValue					(Action.NAME, getLanguage ().findCategoryEntry ("frmLoadGame", "Back"));
		cancelAction.putValue					(Action.NAME, getLanguage ().findCategoryEntry ("frmLoadGame", "Cancel"));

		savedGamesTableModel.fireTableDataChanged ();
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getJoinGameLayout ()
	{
		return joinGameLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setJoinGameLayout (final XmlLayoutContainerEx layout)
	{
		joinGameLayout = layout;
	}

	/**
	 * @return Large font
	 */
	public final Font getLargeFont ()
	{
		return largeFont;
	}

	/**
	 * @param font Large font
	 */
	public final void setLargeFont (final Font font)
	{
		largeFont = font;
	}

	/**
	 * @return Tiny font
	 */
	public final Font getTinyFont ()
	{
		return tinyFont;
	}

	/**
	 * @param font Tiny font
	 */
	public final void setTinyFont (final Font font)
	{
		tinyFont = font;
	}

	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
	
	/**
	 * @return List of saved games we can reload
	 */
	public final List<SavedGameSession> getSavedGames ()
	{
		return savedGames;
	}

	/**
	 * @param games List of saved games we can reload
	 */
	public final void setSavedGames (final List<SavedGameSession> games)
	{
		selectSavedGameAction.setEnabled (false);
		
		savedGames = games;
		savedGamesTableModel.fireTableDataChanged ();
		
		savedGamesTablePane.setVisible (true);
		savePointsTablePane.setVisible (false);
		selectSavedGameButton.setVisible (true);
		selectSavePointButton.setVisible (false);
		backButton.setVisible (false);
	}
	
	/**
	 * @return List of save points we can reload
	 */
	public final List<SavedGamePoint> getSavePoints ()
	{
		return savePoints;
	}

	/**
	 * @param points List of save points we can reload
	 */
	public final void setSavePoints (final List<SavedGamePoint> points)
	{
		savePoints = points;
		savePointsTableModel.fireTableDataChanged ();
		
		savedGamesTablePane.setVisible (false);
		savePointsTablePane.setVisible (true);
		selectSavedGameButton.setVisible (false);
		selectSavePointButton.setVisible (true);
		backButton.setVisible (true);
	}
	
	/**
	 * Table model for displaying saved games we can reload
	 */
	private final class SavedGamesTableModel extends AbstractTableModel
	{
		/**
		 * @return Number of columns in the grid
		 */
		@Override
		public final int getColumnCount ()
		{
			return 4;
		}
		
		/**
		 * @return Heading for each column
		 */
		@Override
		public final String getColumnName (final int column)
		{
			return getLanguage ().findCategoryEntry ("frmLoadGame", "SavedGamesColumn" + column);
		}
		
		/**
		 * @return Number of sessions we can join
		 */
		@Override
		public final int getRowCount ()
		{
			return getSavedGames ().size ();
		}

		/**
		 * @return Value to display at particular cell
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			final SavedGameSession savedGame = getSavedGames ().get (rowIndex);
			final MomSessionDescription sd = (MomSessionDescription) savedGame.getSessionDescription ();
			
			final String value;
			switch (columnIndex)
			{
				case 0:
					value = savedGame.getSessionDescription ().getSessionName ();
					break;

				case 1:
					final StringBuffer players = new StringBuffer ();
					int aiPlayerCount = -2;		// For raiders and rampaging monsters
					for (final PlayerDescription pd : savedGame.getPlayer ())
						if (!pd.isHuman ())
							aiPlayerCount++;
						else
						{
							if (players.length () > 0)
								players.append (", ");
							
							players.append (pd.getPlayerName ());
						}
					
					if (aiPlayerCount > 0)
						players.append (" + " + aiPlayerCount + " AI");
					
					value = players.toString ();
					break;

				case 2:
					value = DateFormats.FULL_DATE_TIME_FORMAT.format (sd.getStartedAt ().toGregorianCalendar ().getTime ());
					break;
					
				case 3:
					value = DateFormats.FULL_DATE_TIME_FORMAT.format (savedGame.getLatestSavedAt ().toGregorianCalendar ().getTime ()) +
						", Turn " + savedGame.getLatestSavedGameIdentifier ();
					break;
					
				default:
					value = null;
			}
			return value;
		}
	}

	/**
	 * Table model for displaying save points we can reload
	 */
	private final class SavePointsTableModel extends AbstractTableModel
	{
		/**
		 * @return Number of columns in the grid
		 */
		@Override
		public final int getColumnCount ()
		{
			return 2;
		}
		
		/**
		 * @return Heading for each column
		 */
		@Override
		public final String getColumnName (final int column)
		{
			return getLanguage ().findCategoryEntry ("frmLoadGame", "SavePointsColumn" + column);
		}
		
		/**
		 * @return Number of sessions we can join
		 */
		@Override
		public final int getRowCount ()
		{
			return getSavePoints ().size ();
		}

		/**
		 * @return Value to display at particular cell
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			final SavedGamePoint savePoint = getSavePoints ().get (rowIndex);
			
			final String value;
			switch (columnIndex)
			{
				case 0:
					value = DateFormats.FULL_DATE_TIME_FORMAT.format (savePoint.getSavedAt ().toGregorianCalendar ().getTime ());
					break;

				case 1:
					value = savePoint.getSavedGameIdentifier ();
					break;
					
				default:
					value = null;
			}
			return value;
		}
	}
}
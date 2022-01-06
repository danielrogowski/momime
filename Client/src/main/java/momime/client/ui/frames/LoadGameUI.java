package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.ndg.multiplayer.sessionbase.BrowseSavePoints;
import com.ndg.multiplayer.sessionbase.LoadGame;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.multiplayer.sessionbase.SavedGamePoint;
import com.ndg.multiplayer.sessionbase.SavedGameSession;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;
import com.ndg.utils.DateFormats;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.common.database.LanguageText;
import momime.common.messages.MomSessionDescription;

/**
 * Screen for showing a list of saved games and save points that are running on the server so we can select one to load
 */
public final class LoadGameUI extends MomClientFrameUI
{
	/** XML layout */
	private XmlLayoutContainerEx joinGameLayout;

	/** Large font */
	private Font largeFont;
	
	/** Tiny font */
	private Font tinyFont;
	
	/** Multiplayer client */
	private MomClient client;

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Select saved game action */
	private Action selectSavedGameAction;

	/** Select saved game button */
	private JButton selectSavedGameButton;
	
	/** Delete saved game action */
	private Action deleteSavedGameAction;

	/** Delete saved game button */
	private JButton deleteSavedGameButton;
	
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
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/sessionList.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldDisabled.png");

		// Actions
		cancelAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
		deleteSavedGameAction = new LoggingAction ((ev) ->
		{
			final SavedGameSession savedGame = getSavedGames ().get (savedGamesTable.getSelectedRow ());
			
			final StringBuilder playersList = new StringBuilder ();
			for (final PlayerDescription player : savedGame.getPlayer ())
				if (player.getPlayerType () == PlayerType.HUMAN)
				{
					if (playersList.length () > 0)
						playersList.append (", ");
					
					playersList.append (player.getPlayerName ());
				}
			
			// Show message box to confirm
			final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
			msg.setLanguageTitle (getLanguages ().getMultiplayer ().getDeleteSavedGameTitle ());
			msg.setSavedGameID (savedGame.getSessionDescription ().getSavedGameID ());

			msg.setText (getLanguageHolder ().findDescription (getLanguages ().getMultiplayer ().getDeleteSavedGameText ()).replaceAll
				("SAVED_GAME_NAME", savedGame.getSessionDescription ().getSessionName ()).replaceAll
				("PLAYERS_LIST", playersList.toString ()));
			
			msg.setVisible (true);												
		});
		
		selectSavedGameAction = new LoggingAction ((ev) ->
		{
			selectSavePointAction.setEnabled (false);
			
			final SavedGameSession savedGame = getSavedGames ().get (savedGamesTable.getSelectedRow ());

			final BrowseSavePoints msg = new BrowseSavePoints ();
			msg.setSavedGameID (savedGame.getSessionDescription ().getSavedGameID ());
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		});
		
		selectSavePointAction = new LoggingAction ((ev) ->
		{
			final SavedGameSession savedGame = getSavedGames ().get (savedGamesTable.getSelectedRow ());
			final SavedGamePoint savePoint = getSavePoints ().get (savePointsTable.getSelectedRow ());

			final LoadGame msg = new LoadGame ();
			msg.setSavedGameID (savedGame.getSessionDescription ().getSavedGameID ());
			msg.setSavedGameFilename (savePoint.getSavedGameFilename ());
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		});

		// Back goes back from the save points to the list of all saved games
		backAction = new LoggingAction ((ev) ->
		{
			savedGamesTablePane.setVisible (true);
			savePointsTablePane.setVisible (false);
			deleteSavedGameButton.setVisible (true);
			selectSavedGameButton.setVisible (true);
			selectSavePointButton.setVisible (false);
			backButton.setVisible (false);
		});
		
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

		deleteSavedGameButton = getUtils ().createImageButton (deleteSavedGameAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled);
		contentPane.add (deleteSavedGameButton, "frmJoinGameRefresh");
		
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
		
		// Enable buttons as soon as a row is clicked on
		savedGamesTable.getSelectionModel ().addListSelectionListener ((ev) ->
		{
			deleteSavedGameAction.setEnabled (true);
			selectSavedGameAction.setEnabled (true);
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
		
		// Enable button as soon as a row is clicked on
		savePointsTable.getSelectionModel ().addListSelectionListener ((ev) -> selectSavePointAction.setEnabled (true));
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getFrame ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getLoadGameScreen ().getTitle ()));
		
		deleteSavedGameAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getLoadGameScreen ().getDeleteSavedGame ()));
		selectSavedGameAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getLoadGameScreen ().getSelectSavedGame ()));
		selectSavePointAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getLoadGameScreen ().getSelectSavePoint ()));
		backAction.putValue						(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getLoadGameScreen ().getBack ()));
		cancelAction.putValue						(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getCancel ()));

		savedGamesTableModel.fireTableDataChanged ();
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
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
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
		savedGames = games;
		savedGamesTableModel.fireTableDataChanged ();
		
		savedGamesTablePane.setVisible (true);
		savePointsTablePane.setVisible (false);
		deleteSavedGameButton.setVisible (true);
		selectSavedGameButton.setVisible (true);
		selectSavePointButton.setVisible (false);
		backButton.setVisible (false);
		
		// Must do this last, since calling fireTableDataChanged can re-enable them
		deleteSavedGameAction.setEnabled (false);
		selectSavedGameAction.setEnabled (false);
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
		deleteSavedGameButton.setVisible (false);
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
			final List<LanguageText> languageText;
			switch (column)
			{
				case 0:
					languageText = getLanguages ().getLoadGameScreen ().getSavedGamesColumnGameName ();
					break;
					
				case 1:
					languageText = getLanguages ().getLoadGameScreen ().getSavedGamesColumnPlayers ();
					break;
					
				case 2:
					languageText = getLanguages ().getLoadGameScreen ().getSavedGamesColumnGameStarted ();
					break;
					
				case 3:
					languageText = getLanguages ().getLoadGameScreen ().getSavedGamesColumnLatestSave ();
					break;
				
				default:
					languageText = null;
			}
			
			return (languageText == null) ? null : getLanguageHolder ().findDescription (languageText);
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
						if (pd.getPlayerType () == PlayerType.HUMAN)
						{
							if (players.length () > 0)
								players.append (", ");
							
							players.append (pd.getPlayerName ());
						}
						else
							aiPlayerCount++;
					
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
			final List<LanguageText> languageText;
			switch (column)
			{
				case 0:
					languageText = getLanguages ().getLoadGameScreen ().getSavePointsColumnSavedAt ();
					break;
					
				case 1:
					languageText = getLanguages ().getLoadGameScreen ().getSavePointsColumnTurn ();
					break;
				
				default:
					languageText = null;
			}
			
			return (languageText == null) ? null : getLanguageHolder ().findDescription (languageText);
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
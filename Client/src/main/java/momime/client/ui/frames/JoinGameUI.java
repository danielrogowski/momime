package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.ndg.multiplayer.sessionbase.JoinSession;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.multiplayer.sessionbase.RequestSessionList;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.common.database.LanguageText;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.TurnSystem;

/**
 * Screen for showing a list of sessions that are running on the server so we can select one to join
 */
public final class JoinGameUI extends MomClientFrameUI
{
	/** XML layout */
	private XmlLayoutContainerEx joinGameLayout;

	/** Large font */
	private Font largeFont;
	
	/** Tiny font */
	private Font tinyFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Refresh action */
	private Action refreshAction;

	/** Join action */
	private Action joinAction;

	/** Cancel action */
	private Action cancelAction;
	
	/** List of sessions we can join */
	private List<SessionAndPlayerDescriptions> sessions = new ArrayList<SessionAndPlayerDescriptions> ();
	
	/** Table of sessions we can join */
	private JTable sessionsTable;
	
	/** Table model of sessions we can join */
	private final SessionListTableModel sessionsTableModel = new SessionListTableModel ();
	
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
		refreshAction = new LoggingAction ((ev) ->
		{
			joinAction.setEnabled (false);
			getClient ().getServerConnection ().sendMessageToServer (new RequestSessionList ());
		});
		
		cancelAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
		joinAction = new LoggingAction ((ev) ->
		{
			final SessionAndPlayerDescriptions spd = getSessions ().get (sessionsTable.getSelectedRow ());
			
			final PlayerDescription pd = new PlayerDescription ();
			pd.setPlayerID (getClient ().getOurPlayerID ());
			pd.setPlayerName (getClient ().getOurPlayerName ());
			pd.setPlayerType (PlayerType.HUMAN);
	
			final JoinSession msg = new JoinSession ();
			msg.setSessionID (spd.getSessionDescription ().getSessionID ());
			msg.setPlayerDescription (pd);
	
			getClient ().getServerConnection ().sendMessageToServer (msg);
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
		
		contentPane.add (getUtils ().createImageButton (refreshAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmJoinGameRefresh");

		contentPane.add (getUtils ().createImageButton (joinAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmJoinGameConfirm");

		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmJoinGameCancel");
		
		sessionsTable = new JTable ();
		sessionsTable.setModel (sessionsTableModel);
		sessionsTable.setFont (getTinyFont ());
		sessionsTable.setForeground (MomUIConstants.SILVER);
		sessionsTable.setBackground (new Color (0, 0, 0, 0));
		sessionsTable.getTableHeader ().setFont (getTinyFont ());
		sessionsTable.setOpaque (false);
		sessionsTable.setRowSelectionAllowed (true);
		sessionsTable.setColumnSelectionAllowed (false);
		
		sessionsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);		// So it actually pays attention to the preferred widths
		sessionsTable.getColumnModel ().getColumn (0).setPreferredWidth (80);
		sessionsTable.getColumnModel ().getColumn (1).setPreferredWidth (70);
		sessionsTable.getColumnModel ().getColumn (2).setPreferredWidth (200);
		sessionsTable.getColumnModel ().getColumn (3).setPreferredWidth (110);
		
		final JScrollPane sessionsTablePane = new JScrollPane (sessionsTable);
		sessionsTablePane.getViewport ().setOpaque (false);
		contentPane.add (sessionsTablePane, "frmJoinGameSessions");

		// Enable button as soon as a row is clicked on
		sessionsTable.getSelectionModel ().addListSelectionListener ((ev) -> joinAction.setEnabled (true));
		
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
		getFrame ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getJoinGameScreen ().getTitle ()));
		
		refreshAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getJoinGameScreen ().getRefresh ()));
		joinAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getJoinGameScreen ().getJoin ()));
		cancelAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getCancel ()));

		sessionsTableModel.fireTableDataChanged ();
	}
	
	/**
	 * Updates the list immediately upon opening the screen
	 */
	public final void refreshSessionList ()
	{
		refreshAction.actionPerformed (null);
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
	 * @return List of sessions we can join
	 */
	public final List<SessionAndPlayerDescriptions> getSessions ()
	{
		return sessions;
	}

	/**
	 * @param ses List of sessions we can join
	 */
	public final void setSessions (final List<SessionAndPlayerDescriptions> ses)
	{
		sessions = ses;
		sessionsTableModel.fireTableDataChanged ();
	}
	
	/**
	 * Table model for displaying game session we can join
	 */
	private final class SessionListTableModel extends AbstractTableModel
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
					languageText = getLanguages ().getJoinGameScreen ().getSessionsColumnGameName ();
					break;
					
				case 1:
					languageText = getLanguages ().getJoinGameScreen ().getSessionsColumnPlayers ();
					break;
					
				case 2:
					languageText = getLanguages ().getJoinGameScreen ().getSessionsColumnSettings ();
					break;
					
				case 3:
					languageText = getLanguages ().getJoinGameScreen ().getSessionsColumnTurnSystem ();
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
			return getSessions ().size ();
		}

		/**
		 * @return Value to display at particular cell
		 */
		@Override
		public final Object getValueAt (final int rowIndex, final int columnIndex)
		{
			final SessionAndPlayerDescriptions spd = getSessions ().get (rowIndex);
			final MomSessionDescription sd = (MomSessionDescription) spd.getSessionDescription ();
			
			final String value;
			switch (columnIndex)
			{
				case 0:
					value = spd.getSessionDescription ().getSessionName ();
					break;

				case 1:
					value = spd.getPlayer ().size () + " / " + (sd.getMaxPlayers () - sd.getAiPlayerCount () - 2) +
						(sd.getAiPlayerCount () == 0 ? "" : (", +" + sd.getAiPlayerCount () + " AI")); 
					break;

				case 2:
					// Display the name of each settings preset or "Custom"
					value = (sd.getOverlandMapSize ().getOverlandMapSizeID () == null ?
							getLanguageHolder ().findDescription (getLanguages ().getWaitForPlayersToJoinScreen ().getCustom ()) :
							getLanguageHolder ().findDescription (sd.getOverlandMapSize ().getOverlandMapSizeDescription ())) + ", " +
							
						(sd.getLandProportion ().getLandProportionID () == null ?
							getLanguageHolder ().findDescription (getLanguages ().getWaitForPlayersToJoinScreen ().getCustom ()) :
							getLanguageHolder ().findDescription (sd.getLandProportion ().getLandProportionDescription ())) + ", " +
						
						(sd.getNodeStrength ().getNodeStrengthID () == null ?
							getLanguageHolder ().findDescription (getLanguages ().getWaitForPlayersToJoinScreen ().getCustom ()) :
							getLanguageHolder ().findDescription (sd.getNodeStrength ().getNodeStrengthDescription ())) + ", " +
								
						(sd.getDifficultyLevel ().getDifficultyLevelID () == null ?
							getLanguageHolder ().findDescription (getLanguages ().getWaitForPlayersToJoinScreen ().getCustom ()) :
							getLanguageHolder ().findDescription (sd.getDifficultyLevel ().getDifficultyLevelDescription ()));
					break;
					
				case 3:
					final List<LanguageText> languageText = (sd.getTurnSystem () == TurnSystem.SIMULTANEOUS) ?
						getLanguages ().getTurnSystems ().getSimultaneous () : getLanguages ().getTurnSystems ().getOnePlayerAtATime ();
						
					value = getLanguageHolder ().findDescription (languageText);
					break;
					
				default:
					value = null;
			}
			return value;
		}
	}
}
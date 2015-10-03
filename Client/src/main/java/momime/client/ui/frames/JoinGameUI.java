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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.sessionbase.JoinSession;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.RequestSessionList;
import com.ndg.multiplayer.sessionbase.SessionAndPlayerDescriptions;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.common.messages.MomSessionDescription;

/**
 * Screen for showing a list of sessions that are running on the server so we can select one to join
 */
public final class JoinGameUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (JoinGameUI.class);

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
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/sessionList.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldDisabled.png");

		// Actions
		refreshAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				joinAction.setEnabled (false);
				try
				{
					getClient ().getServerConnection ().sendMessageToServer (new RequestSessionList ());
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		
		cancelAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().setVisible (false);
			}
		};
		
		joinAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				final SessionAndPlayerDescriptions spd = getSessions ().get (sessionsTable.getSelectedRow ());
				
				final PlayerDescription pd = new PlayerDescription ();
				pd.setPlayerID (getClient ().getOurPlayerID ());
				pd.setPlayerName (getClient ().getOurPlayerName ());
				pd.setHuman (true);
		
				final JoinSession msg = new JoinSession ();
				msg.setSessionID (spd.getSessionDescription ().getSessionID ());
				msg.setPlayerDescription (pd);
		
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

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");
		
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmJoinGame", "Title"));
		
		refreshAction.putValue	(Action.NAME, getLanguage ().findCategoryEntry ("frmJoinGame", "Refresh"));
		joinAction.putValue			(Action.NAME, getLanguage ().findCategoryEntry ("frmJoinGame", "Join"));
		cancelAction.putValue		(Action.NAME, getLanguage ().findCategoryEntry ("frmJoinGame", "Cancel"));

		sessionsTableModel.fireTableDataChanged ();
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * Updates the list immediately upon opening the screen
	 */
	public final void refreshSessionList ()
	{
		log.trace ("Entering refreshSessionList");

		refreshAction.actionPerformed (null);

		log.trace ("Exiting refreshSessionList");
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
			return getLanguage ().findCategoryEntry ("frmJoinGame", "SessionsColumn" + column);
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
					value = (sd.getOverlandMapSize ().getOverlandMapSizeID () == null ? getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Custom") :
							getLanguage ().findOverlandMapSizeDescription (sd.getOverlandMapSize ().getOverlandMapSizeID ())) + ", " +
							
						(sd.getLandProportion ().getLandProportionID () == null ? getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Custom") :
							getLanguage ().findLandProportionDescription (sd.getLandProportion ().getLandProportionID ())) + ", " +
						
						(sd.getNodeStrength ().getNodeStrengthID () == null ? getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Custom") :
							getLanguage ().findNodeStrengthDescription (sd.getNodeStrength ().getNodeStrengthID ())) + ", " +
								
						(sd.getDifficultyLevel ().getDifficultyLevelID () == null ? getLanguage ().findCategoryEntry ("frmWaitForPlayersToJoin", "Custom") :
							getLanguage ().findDifficultyLevelDescription (sd.getDifficultyLevel ().getDifficultyLevelID ()));
					break;
					
				case 3:
					value = getLanguage ().findCategoryEntry ("NewGameFormTurnSystems", sd.getTurnSystem ().name ());
					break;
					
				default:
					value = null;
			}
			return value;
		}
	}
}
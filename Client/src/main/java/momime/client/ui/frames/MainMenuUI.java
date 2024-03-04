package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.sessionbase.BrowseSavedGames;
import com.ndg.utils.swing.GridBagConstraintsNoFill;
import com.ndg.utils.swing.actions.LoggingAction;

import momime.client.MomClient;
import momime.client.audio.MomAudioPlayer;
import momime.client.graphics.AnimationContainer;
import momime.client.ui.MomUIConstants;
import momime.client.utils.AnimationController;

/**
 * Main menu with options to connect to a server and create or join games
 */
public final class MainMenuUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MainMenuUI.class);
	
	/** Animation for the big red 'Master of Magic' animated title */
	final static String ANIM_MAIN_MENU_TITLE = "MAIN_MENU_TITLE";
	
	/** Maven version number, injected from spring */
	private String version;
	
	/** Large font */
	private Font largeFont;
	
	/** Medium font */
	private Font mediumFont;
	
	/** Options UI */
	private OptionsUI optionsUI;
	
	/** Connect to server UI */
	private ConnectToServerUI connectToServerUI;

	/** New Game UI */
	private NewGameUI newGameUI;

	/** Join Game UI */
	private JoinGameUI joinGameUI;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Music player */
	private MomAudioPlayer musicPlayer;
	
	/** Connect to server action */
	private Action connectToServerAction;

	/** New game action */
	private Action newGameAction;
	
	/** New game button */
	private JButton newGameButton;

	/** Join game action */
	private Action joinGameAction;
	
	/** Join game button */
	private JButton joinGameButton;

	/** Load game action */
	private Action loadGameAction;
	
	/** Load game button */
	private JButton loadGameButton;
	
	/** Options action */
	private Action optionsAction;

	/** Exit to windows action */
	private Action exitToWindowsAction;

	/** Short title */
	private JLabel shortTitleLabel;
	
	/** Version */
	private JLabel versionLabel;

	/** Original copyright line 1 */
	private JLabel originalCopyrightLine1Label;
	
	/** Original copyright line 2 */
	private JLabel originalCopyrightLine2Label;
	
	/** Gap above labels */
	private Filler labelsGap;
	
	/** Gap under buttons */
	private Filler buttonsGap;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 0;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/mainMenu/background.png");

		// Create actions
		connectToServerAction = new LoggingAction ((ev) -> getConnectToServerUI ().setVisible (true));
		
		newGameAction = new LoggingAction ((ev) ->
		{
			getNewGameUI ().setVisible (true);
			getNewGameUI ().showNewGamePanel ();
		});
		
		joinGameAction = new LoggingAction ((ev) ->
		{
			getJoinGameUI ().setVisible (true);
			getJoinGameUI ().refreshSessionList ();
		});
		
		// Receiving the reply to this causes LoadGameUI to be displayed, see receivedSavedGamesList ()
		loadGameAction = new LoggingAction ((ev) -> getClient ().getServerConnection ().sendMessageToServer (new BrowseSavedGames ()));
		
		optionsAction = new LoggingAction ((ev) -> getOptionsUI ().setVisible (true));
		
		exitToWindowsAction = new LoggingAction ((ev) -> System.exit (0));
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.EXIT_ON_CLOSE);

		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Scale the background image up smoothly
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
				// Stretch and centre background, preserving aspect ratio
				// Also note background image is split into two images, one static and one animated
				final int edge = Math.min (getWidth () / 4, getHeight () / 3);
				final int imgWidth = edge * 4;
				final int imgHeight = edge * 3;
				final int leftBorder = (getWidth () - imgWidth) / 2;
				final int topBorder = (getHeight () - imgHeight) / 2;
				
				final int titleHeight = (imgHeight * 41) / 200; 
				
				try
				{
					g.drawImage (getAnim ().loadImageOrAnimationFrame (null, ANIM_MAIN_MENU_TITLE, true, AnimationContainer.GRAPHICS_XML),
						leftBorder, topBorder, imgWidth, titleHeight, null);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
				
				g.drawImage (background, leftBorder, topBorder + titleHeight, imgWidth, imgHeight - titleHeight, null);
			}
		};
		contentPane.setBackground (Color.BLACK);
		contentPane.setPreferredSize (new Dimension (640, 480));
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		// Static text
		final Dimension labelsSpace = new Dimension (630, (480 * 41) / 200);
		labelsGap = new Box.Filler (labelsSpace, labelsSpace, labelsSpace);
		contentPane.add (labelsGap, getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		shortTitleLabel = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (shortTitleLabel, getUtils ().createConstraintsNoFill (0, 1, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		versionLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (versionLabel, getUtils ().createConstraintsNoFill (0, 2, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		originalCopyrightLine1Label = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (originalCopyrightLine1Label, getUtils ().createConstraintsNoFill (0, 3, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		originalCopyrightLine2Label = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (originalCopyrightLine2Label, getUtils ().createConstraintsNoFill (0, 4, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		// Space in between
		final GridBagConstraints constraints = getUtils ().createConstraintsNoFill (0, 6, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE);
		constraints.weighty = 1;
		contentPane.add (Box.createGlue (), constraints);
		
		// Main menu options
		contentPane.add (getUtils ().createTextOnlyButton (connectToServerAction,	MomUIConstants.GOLD, getLargeFont ()), getUtils ().createConstraintsNoFill (0, 8, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		newGameButton = getUtils ().createTextOnlyButton (newGameAction, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (newGameButton, getUtils ().createConstraintsNoFill (0, 9, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		joinGameButton = getUtils ().createTextOnlyButton (joinGameAction, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (joinGameButton, getUtils ().createConstraintsNoFill (0, 10, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		loadGameButton = getUtils ().createTextOnlyButton (loadGameAction, MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (loadGameButton, getUtils ().createConstraintsNoFill (0, 11, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		contentPane.add (getUtils ().createTextOnlyButton (optionsAction,				MomUIConstants.GOLD, getLargeFont ()), getUtils ().createConstraintsNoFill (0, 12, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		contentPane.add (getUtils ().createTextOnlyButton (exitToWindowsAction,	MomUIConstants.GOLD, getLargeFont ()), getUtils ().createConstraintsNoFill (0, 13, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));

		final Dimension buttonsSpace = new Dimension (630, 0);
		buttonsGap = new Box.Filler (buttonsSpace, buttonsSpace, buttonsSpace);
		contentPane.add (buttonsGap, getUtils ().createConstraintsNoFill (0, 14, 1, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
		// Animate the title
		// This anim never finishes - if we close the window or click exit, it immediately shuts down the JVM and so it doesn't matter
		// If we proceed to start a game, this main menu frame is merely hidden and not disposed so it can be reused later, so the anim just also becomes hidden
		getAnim ().registerRepaintTrigger (ANIM_MAIN_MENU_TITLE, contentPane, AnimationContainer.GRAPHICS_XML);

		// Start title screen music
		playMusic ();
		
		// Resizing the window is a bit pointless since there's no more info to display
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		enableActions ();
	}

	/**
	 * Plays the title screen music
	 */
	public final void playMusic ()
	{
		try
		{
			getMusicPlayer ().playAudioFile ("/momime.client.music/MUSIC_104 - Title screen.mp3");
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getFrame ().setTitle						(getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getLongTitle ()).replaceAll ("VERSION", getVersion ()));
		shortTitleLabel.setText					(getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getShortTitle ()));
		versionLabel.setText						(getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getVersion ()).replaceAll ("VERSION", getVersion ()));
		originalCopyrightLine1Label.setText	(getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getOriginalCopyrightLine1 ()));
		originalCopyrightLine2Label.setText	(getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getOriginalCopyrightLine2 ()));

		connectToServerAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getConnectToServer ()));
		newGameAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getNewGame ()));
		joinGameAction.putValue				(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getJoinGame ()));
		loadGameAction.putValue			(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getLoadGame ()));
		optionsAction.putValue				(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getOptions ()));
		exitToWindowsAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getMainMenuScreen ().getExit ()));
	}
	
	/**
	 * Updates enable/disable status of actions
	 */
	public final void enableActions ()
	{
		newGameAction.setEnabled (getClient ().getOurPlayerID () != null);
		newGameButton.setForeground (newGameAction.isEnabled () ? MomUIConstants.GOLD : MomUIConstants.GRAY);
		
		joinGameAction.setEnabled (getClient ().getOurPlayerID () != null);
		joinGameButton.setForeground (joinGameAction.isEnabled () ? MomUIConstants.GOLD : MomUIConstants.GRAY);

		loadGameAction.setEnabled (getClient ().getOurPlayerID () != null);
		loadGameButton.setForeground (loadGameAction.isEnabled () ? MomUIConstants.GOLD : MomUIConstants.GRAY);
	}
	
	/**
	 * @return Maven version number, injected from spring
	 */
	public final String getVersion ()
	{
		return version;
	}

	/**
	 * @param ver Maven version number, injected from spring
	 */
	public final void setVersion (final String ver)
	{
		version = ver;
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
	 * @return Medium font
	 */
	public final Font getMediumFont ()
	{
		return mediumFont;
	}

	/**
	 * @param font Medium font
	 */
	public final void setMediumFont (final Font font)
	{
		mediumFont = font;
	}

	/**
	 * @return Options UI
	 */
	public final OptionsUI getOptionsUI ()
	{
		return optionsUI;
	}

	/**
	 * @param ui Options UI
	 */
	public final void setOptionsUI (final OptionsUI ui)
	{
		optionsUI = ui;
	}
	
	/**
	 * @return Connect to server UI
	 */
	public final ConnectToServerUI getConnectToServerUI ()
	{
		return connectToServerUI;
	}

	/**
	 * @param ui Connect to server UI
	 */
	public final void setConnectToServerUI (final ConnectToServerUI ui)
	{
		connectToServerUI = ui;
	}

	/**
	 * @return New Game UI
	 */
	public final NewGameUI getNewGameUI ()
	{
		return newGameUI;
	}

	/**
	 * @param ui New Game UI
	 */
	public final void setNewGameUI (final NewGameUI ui)
	{
		newGameUI = ui;
	}
	
	/**
	 * @return Join Game UI
	 */
	public final JoinGameUI getJoinGameUI ()
	{
		return joinGameUI;
	}

	/**
	 * @param ui Join Game UI
	 */
	public final void setJoinGameUI (final JoinGameUI ui)
	{
		joinGameUI = ui;
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
	 * @return Animation controller
	 */
	public final AnimationController getAnim ()
	{
		return anim;
	}

	/**
	 * @param controller Animation controller
	 */
	public final void setAnim (final AnimationController controller)
	{
		anim = controller;
	}

	/**
	 * @return Music player
	 */
	public final MomAudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final MomAudioPlayer player)
	{
		musicPlayer = player;
	}
}
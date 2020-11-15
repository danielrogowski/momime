package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.clienttoserver.CaptureCityDecisionMessage;

/**
 * Dialog box that pops up after successfully attacking an enemy city, to ask if you want to raze or keep it.
 */
public final class RazeCityUI extends MomClientDialogUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (RazeCityUI.class);

	/** XML layout */
	private XmlLayoutContainerEx razeCityLayout;

	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** The location of the city that we successfully attacked */
	private MapCoordinates3DEx cityLocation;
	
	/** The player who we took the city from */
	private int defendingPlayerID;
	
	/** Raze action */
	private Action razeAction;
	
	/** Capture action */
	private Action captureAction;
	
	/** Main text */
	private JTextArea messageText;

	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init: " + getCityLocation ());
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/twoButtonBox186x60.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x17Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x17Pressed.png");
		
		// Actions
		razeAction = new LoggingAction ((ev) ->
		{
			final CaptureCityDecisionMessage msg = new CaptureCityDecisionMessage ();
			msg.setCityLocation (getCityLocation ());
			msg.setDefendingPlayerID (getDefendingPlayerID ());
			msg.setCaptureCityDecision (CaptureCityDecisionID.RAZE);

			getClient ().getServerConnection ().sendMessageToServer (msg);
			setVisible (false);
		});
 
		captureAction = new LoggingAction ((ev) ->
		{
			final CaptureCityDecisionMessage msg = new CaptureCityDecisionMessage ();
			msg.setCityLocation (getCityLocation ());
			msg.setDefendingPlayerID (getDefendingPlayerID ());
			msg.setCaptureCityDecision (CaptureCityDecisionID.CAPTURE);
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
			setVisible (false);
		});
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getRazeCityLayout ()));
		
		contentPane.add (getUtils ().createImageButton (razeAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmRazeCityRaze");
		
		contentPane.add (getUtils ().createImageButton (captureAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmRazeCityNo");
		
		messageText = getUtils ().createWrappingLabel (MomUIConstants.GOLD, getSmallFont ());
		contentPane.add (messageText, "frmRazeCityText");

		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		
		log.trace ("Exiting init");
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged: " + getCityLocation ());
		
		messageText.setText (getLanguageHolder ().findDescription (getLanguages ().getRazeCityScreen ().getText ()));
		
		razeAction.putValue		(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getRazeCityScreen ().getRaze ()));
		captureAction.putValue	(Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getNo ()));
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getRazeCityLayout ()
	{
		return razeCityLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setRazeCityLayout (final XmlLayoutContainerEx layout)
	{
		razeCityLayout = layout;
	}
	
	/**
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
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
	 * @return The location of the city that we successfully attacked
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param loc The location of the city that we successfully attacked
	 */
	public final void setCityLocation (final MapCoordinates3DEx loc)
	{
		cityLocation = loc;
	}
	
	/**
	 * @return playerID The player who we took the city from
	 */
	public final int getDefendingPlayerID ()
	{
		return defendingPlayerID;
	}
	
	/**
	 * @param playerID The player who we took the city from
	 */
	public final void setDefendingPlayerID (final int playerID)
	{
		defendingPlayerID = playerID;
	}
}
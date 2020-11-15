package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.client.utils.TextUtils;
import momime.common.database.TaxRate;
import momime.common.messages.clienttoserver.ChangeTaxRateMessage;

/**
 * Window that shows a button for each possible tax rate so we can click one
 */
public final class TaxRateUI extends MomClientFrameUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (TaxRateUI.class);

	/** XML layout */
	private XmlLayoutContainerEx selectAdvisorLayout;
	
	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Text utils */
	private TextUtils textUtils;
	
	/** Title */
	private JLabel title;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Image of how buttons look normally */
	private BufferedImage buttonNormal;
	
	/** Image of how buttons look when pressed */
	private BufferedImage buttonPressed;
	
	/** Map of tax rates to actions */
	private Map<TaxRate, Action> taxRateActions = new HashMap<TaxRate, Action> ();
	
	/** List of buttons for each taxRateID */
	private List<JButton> taxRateButtons = new ArrayList<JButton> ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/selectAdvisor.png");
		buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Normal.png");
		buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Pressed.png");
		
		// Initialize the content pane
		contentPane = getUtils ().createPanelWithBackgroundImage (background);
		contentPane.setBackground (Color.BLACK);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getSelectAdvisorLayout ()));
		
		title = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		contentPane.add (title, "frmSelectAdvisorTitle");
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().setUndecorated (true);
		setCloseOnClick (true);
	
		getFrame ().setShape (new Polygon
			(new int [] {0, SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth (), background.getWidth (), background.getWidth () - 2, background.getWidth () - 2, background.getWidth (), background.getWidth (), background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, 0, 0, 2, 2, 0}, 
			new int [] {0, 0, 2, 2, 0, 0, SelectAdvisorUI.TOP_HEIGHT, SelectAdvisorUI.TOP_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight (), background.getHeight (), background.getHeight () - 2, background.getHeight () - 2, background.getHeight (), background.getHeight (), background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, SelectAdvisorUI.TOP_HEIGHT, SelectAdvisorUI.TOP_HEIGHT},
			20));		

		updateTaxRateButtons ();
		log.trace ("Exiting init");
	}
	
	/**
	 * Creates a button for each tax rate defined in the DB
	 */
	public final void updateTaxRateButtons ()
	{
		log.trace ("Entering updateTaxRateButtons");
		
		// This gets ran when we receive the DB from the server; for the first game the form won't exist yet, so skip the call at that stage
		if (contentPane != null)
		{
			// Clear out any old buttons
			for (final JButton button : taxRateButtons)
				contentPane.remove (button);
			
			taxRateButtons.clear ();
			taxRateActions.clear ();
			
			// Create new actions and buttons
			int index = 0;
			for (final TaxRate taxRate : getClient ().getClientDB ().getTaxRate ())
			{
				// Create new action
				final Action taxRateAction = new LoggingAction ((ev) ->
				{
					// Don't update it locally yet - tell the server what tax rate we want and it will send a message back to confirm the new tax rate is OK
					final ChangeTaxRateMessage msg = new ChangeTaxRateMessage ();
					msg.setTaxRateID (taxRate.getTaxRateID ());

					getClient ().getServerConnection ().sendMessageToServer (msg);
					setVisible (false);
				});
				
				taxRateActions.put (taxRate, taxRateAction);
				
				// Create new button
				index++;
				final JButton taxRateButton = getUtils ().createImageButton (taxRateAction, MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal);
				contentPane.add (taxRateButton, "frmSelectAdvisor" + index);
				taxRateButtons.add (taxRateButton);
			}
			
			// Set the action labels
			languageChanged ();
		}
		
		log.trace ("Exiting updateTaxRateButtons");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		title.setText (getLanguageHolder ().findDescription (getLanguages ().getTaxRateScreen ().getTitle ()));
		
		final String text = getLanguageHolder ().findDescription (getLanguages ().getTaxRateScreen ().getEntry ());
		if (text != null)
			for (final Entry<TaxRate, Action> taxRateAction : taxRateActions.entrySet ())
			{
				String thisText = text.replaceAll
					("GOLD_PER_POPULATION", getTextUtils ().halfIntToStr (taxRateAction.getKey ().getDoubleTaxGold ())).replaceAll
					("UNREST_PERCENTAGE", Integer.valueOf (taxRateAction.getKey ().getTaxUnrestPercentage ()).toString ());
				
				// Show current tax rate with a *
				if (taxRateAction.getKey ().getTaxRateID ().equals (getClient ().getOurPersistentPlayerPrivateKnowledge ().getTaxRateID ()))
					thisText = thisText + " *";
				
				taxRateAction.getValue ().putValue (Action.NAME, thisText);
			}
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getSelectAdvisorLayout ()
	{
		return selectAdvisorLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setSelectAdvisorLayout (final XmlLayoutContainerEx layout)
	{
		selectAdvisorLayout = layout;
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
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
	}
}
package momime.client.ui.dialogs;

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

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.process.OverlandMapProcessing;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.SelectAdvisorUI;
import momime.common.messages.clienttoserver.TargetSpellMessage;

/**
 * Small popup that asks which of several choices of city spell effect we want to cast.  For Spell Ward.
 */
public final class ChooseCitySpellEffectUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ChooseCitySpellEffectUI.class);

	/** XML layout */
	private XmlLayoutContainerEx selectAdvisorLayout;
	
	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Turn sequence and movement helper methods */
	private OverlandMapProcessing overlandMapProcessing;
	
	/** Title */
	private JLabel title;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Image of how buttons look normally */
	private BufferedImage buttonNormal;
	
	/** Image of how buttons look when pressed */
	private BufferedImage buttonPressed;
	
	/** List of city spell effect sources to create buttons for   */
	private List<String> citySpellEffectChoices;
	
	/** Map of city spell effect IDs to actions */
	private final Map<String, Action> citySpellEffectActions = new HashMap<String, Action> ();
	
	/** List of buttons for each city spell effect effect */
	private final List<JButton> citySpellEffectButtons = new ArrayList<JButton> ();
	
	/** The spell we are choosing the effect for */
	private String spellID;
	
	/** The city we are casting the spell on */
	private MapCoordinates3DEx cityLocation;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
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
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		getDialog ().setUndecorated (true);
		setCloseOnClick (true);
	
		getDialog ().setShape (new Polygon
			(new int [] {0, SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth (), background.getWidth (), background.getWidth () - 2, background.getWidth () - 2, background.getWidth (), background.getWidth (), background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, 0, 0, 2, 2, 0}, 
			new int [] {0, 0, 2, 2, 0, 0, SelectAdvisorUI.TOP_HEIGHT, SelectAdvisorUI.TOP_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight (), background.getHeight (), background.getHeight () - 2, background.getHeight () - 2, background.getHeight (), background.getHeight (), background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, SelectAdvisorUI.TOP_HEIGHT, SelectAdvisorUI.TOP_HEIGHT},
			20));		

		setCitySpellEffectChoices (citySpellEffectChoices);
	}
	
	/**
	 * @param aCitySpellEffectChoices List of city spell effect sources to create buttons for  
	 */
	public final void setCitySpellEffectChoices (final List<String> aCitySpellEffectChoices)
	{
		// Typically this gets called prior to showing the form
		citySpellEffectChoices = aCitySpellEffectChoices;
		if (contentPane != null)
		{
			// Clear out any old buttons
			for (final JButton button : citySpellEffectButtons)
				contentPane.remove (button);
			
			citySpellEffectButtons.clear ();
			citySpellEffectActions.clear ();
			
			// Create new actions and buttons
			int index = 0;
			if (citySpellEffectChoices != null)
				for (final String citySpellEffectID : citySpellEffectChoices)
				{
					// Create new action
					final Action citySpellEffectAction = new LoggingAction ((ev) ->
					{
						getDialog ().setVisible (false);
						
						final TargetSpellMessage msg = new TargetSpellMessage ();
						msg.setSpellID (getSpellID ());
						msg.setOverlandTargetLocation (getCityLocation ());
						msg.setChosenCitySpellEffectID (citySpellEffectID);
						getClient ().getServerConnection ().sendMessageToServer (msg);
						
						// Close out the "Target Spell" right hand panel
						getOverlandMapProcessing ().updateMovementRemaining ();
					});				
					citySpellEffectActions.put (citySpellEffectID, citySpellEffectAction);
					
					// Create new button
					index++;
					final JButton castingSourceButton = getUtils ().createImageButton (citySpellEffectAction, MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal);
					contentPane.add (castingSourceButton, "frmSelectAdvisor" + index);
					citySpellEffectButtons.add (castingSourceButton);
				}
				
			// Set the action labels
			languageChanged ();
		}
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		title.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getChooseCitySpellEffectTitle ()));
		
		try
		{
			for (final Entry<String, Action> citySpellEffectAction : citySpellEffectActions.entrySet ())
			{
				// Work out text for this line
				final String text = getLanguageHolder ().findDescription (getClient ().getClientDB ().findCitySpellEffect
					(citySpellEffectAction.getKey (), "ChooseCitySpellEffectUI").getCitySpellEffectName ());
				
				citySpellEffectAction.getValue ().putValue (Action.NAME, text);
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
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
	 * @return Turn sequence and movement helper methods
	 */
	public final OverlandMapProcessing getOverlandMapProcessing ()
	{
		return overlandMapProcessing;
	}

	/**
	 * @param proc Turn sequence and movement helper methods
	 */
	public final void setOverlandMapProcessing (final OverlandMapProcessing proc)
	{
		overlandMapProcessing = proc;
	}
	
	/**
	 * @return The spell we are choosing the effect for
	 */
	public final String getSpellID ()
	{
		return spellID;
	}

	/**
	 * @param s The spell we are choosing the effect for
	 */
	public final void setSpellID (final String s)
	{
		spellID = s;
	}
	
	/**
	 * @return The city we are casting the spell on
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}
	
	/**
	 * @param c The city we are casting the spell on
	 */
	public final void setCityLocation (final MapCoordinates3DEx c)
	{
		cityLocation = c;
	}
}
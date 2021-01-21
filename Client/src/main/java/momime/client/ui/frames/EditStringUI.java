package momime.client.ui.frames;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.common.database.LanguageText;
import momime.common.messages.clienttoserver.ChooseCityNameMessage;
import momime.common.messages.clienttoserver.RequestUpdateUnitNameMessage;

/**
 * Frame which a prompt, text box, and takes some action when OK is clicked
 */
public final class EditStringUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (EditStringUI.class);

	/** XML layout */
	private XmlLayoutContainerEx editStringLayout;
	
	/** Large font */
	private Font largeFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** OK action */
	private Action okAction;

	/** Prompt text saying what needs to be entered */
	private JLabel promptLabel;
	
	/** Language text to use for the title; null if title is not language-variable */
	private List<LanguageText> languageTitle;
	
	/** Language text to use for the prompt; null if prompt is not language-variable */
	private List<LanguageText> languagePrompt;
	
	/** Fixed title that isn't lanuage variant; null if title is language variable */
	private String title;	
	
	/** Fixed prompt that isn't lanuage variant; null if prompt is language variable */
	private String prompt;
	
	/** The coordinates of the city being named; null if the string being edited isn't a city name */
	private MapCoordinates3DEx cityBeingNamed;

	/** The unit being named; null if the string being edited isn't a unit name */
	private Integer unitBeingNamed;
	
	/** The text being entered; so we can set it prior to the UI components being created */
	private String text;
	
	/** The text field */
	private JTextField textField;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/editString298x76.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Pressed.png");

		// Actions
		okAction = new LoggingAction ((ev) ->
		{
			// Take whatever action is appropriate for the string being edited
			if (getCityBeingNamed () != null)
			{
				final ChooseCityNameMessage msg = new ChooseCityNameMessage ();
				msg.setCityLocation (getCityBeingNamed ());
				msg.setCityName (getText ());
			
				getClient ().getServerConnection ().sendMessageToServer (msg);					
			}
			else if (getUnitBeingNamed () != null)
			{
				final RequestUpdateUnitNameMessage msg = new RequestUpdateUnitNameMessage ();
				msg.setUnitURN (getUnitBeingNamed ());
				msg.setUnitName (getText ());
				
				getClient ().getServerConnection ().sendMessageToServer (msg);					
			}
			else
				log.warn ("EditStringUI had string entered for prompt \"" + promptLabel.getText () + " but took no action from clicking the OK button");
				
			// Close out the window
			getFrame ().dispose ();
		});
		
		// Initialize the frame
		final EditStringUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		contentPane.setBorder (BorderFactory.createEmptyBorder (2, 19, 8, 19));
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getEditStringLayout ()));
		
		promptLabel = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		contentPane.add (promptLabel, "frmEditStringPrompt");

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmEditStringOK");
		
		textField = getUtils ().createTransparentTextField (MomUIConstants.SILVER, getLargeFont (), new Dimension (244, 20));
		textField.setText (text);
		contentPane.add (textField, "frmEditStringText");
		
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
		// Title
		final String useTitle;
		if (getTitle () != null)
			useTitle = getTitle ();
		else
			useTitle = getLanguageHolder ().findDescription (getLanguageTitle ());
		
		// Prompt
		final String usePrompt;
		if (getPrompt () != null)
			usePrompt = getPrompt ();
		else
			usePrompt = getLanguageHolder ().findDescription (getLanguagePrompt ());
		
		getFrame ().setTitle (useTitle);
		promptLabel.setText (usePrompt);
		
		// No action text to set, because the button has OK on it as part of the image
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getEditStringLayout ()
	{
		return editStringLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setEditStringLayout (final XmlLayoutContainerEx layout)
	{
		editStringLayout = layout;
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
	 * @return Language text to use for the title; null if title is not language-variable
	 */
	public final List<LanguageText> getLanguageTitle ()
	{
		return languageTitle;
	}

	/**
	 * @param t Language text to use for the title; null if title is not language-variable
	 */
	public final void setLanguageTitle (final List<LanguageText> t)
	{
		languageTitle = t;
	}
	
	/**
	 * @return Language text to use for the prompt; null if prompt is not language-variable
	 */
	public final List<LanguageText> getLanguagePrompt ()
	{
		return languagePrompt;	
	}
	
	/**
	 * @param p Language text to use for the prompt; null if prompt is not language-variable
	 */
	public final void setLanguagePrompt (final List<LanguageText> p)
	{
		languagePrompt = p;
	}

	/**
	 * @return Fixed title that isn't lanuage variant; null if title is language variable
	 */
	public final String getTitle ()
	{
		return title;
	}

	/**
	 * @param txt Fixed title that isn't lanuage variant; null if title is language variable
	 */
	public final void setTitle (final String txt)
	{
		title = txt;
	}
	
	/**
	 * @return Fixed text that isn't lanuage variant; null if message is language variable
	 */
	public final String getPrompt ()
	{
		return prompt;
	}

	/**
	 * @param txt Fixed text that isn't lanuage variant; null if message is language variable
	 */
	public final void setPrompt (final String txt)
	{
		prompt = txt;
	}
	
	/**
	 * @return The coordinates of the city being named; null if the string being edited isn't a city name
	 */
	public final MapCoordinates3DEx getCityBeingNamed ()
	{
		return cityBeingNamed;
	}

	/**
	 * @param loc The coordinates of the city being named; null if the string being edited isn't a city name
	 */
	public final void setCityBeingNamed (final MapCoordinates3DEx loc)
	{
		cityBeingNamed = loc;
	}

	/**
	 * @return The unit being named; null if the string being edited isn't a unit name
	 */
	public final Integer getUnitBeingNamed ()
	{
		return unitBeingNamed;
	}

	/**
	 * @param unitURN The unit being named; null if the string being edited isn't a unit name
	 */
	public final void setUnitBeingNamed (final Integer unitURN)
	{
		unitBeingNamed = unitURN;
	}
	
	/**
	 * @return The text being entered
	 */
	public final String getText ()
	{
		return (textField == null) ? text : textField.getText ();
	}

	/**
	 * @param txt The text being entered
	 */
	public final void setText (final String txt)
	{
		text = txt;
		if (textField != null)
			textField.setText (txt);
	}
}
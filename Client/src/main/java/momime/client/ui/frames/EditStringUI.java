package momime.client.ui.frames;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.common.messages.clienttoserver.ChooseCityNameMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Frame which a prompt, text box, and takes some action when OK is clicked
 */
public final class EditStringUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (EditStringUI.class);
	
	/** Large font */
	private Font largeFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** OK action */
	private Action okAction;

	/** Prompt text saying what needs to be entered */
	private JLabel promptLabel;
	
	/** Language category ID to use for the title; null if title is not language-variable */
	private String titleLanguageCategoryID;
	
	/** Language entry ID to use for the title; null if title is not language-variable */
	private String titleLanguageEntryID;
	
	/** Language category ID to use for the prompt; null if prompt is not language-variable */
	private String promptLanguageCategoryID;
	
	/** Language entry ID to use for the prompt; null if prompt is not language-variable */
	private String promptLanguageEntryID;
	
	/** Fixed title that isn't lanuage variant; null if title is language variable */
	private String title;	
	
	/** Fixed prompt that isn't lanuage variant; null if prompt is language variable */
	private String prompt;
	
	/** The coordinates of the city being named; null if the string being edited isn't a city name */
	private MapCoordinates3DEx cityBeingNamed;
	
	/** The text being entered; so we can set it prior to the UI components being created */
	private String text;
	
	/** The text field */
	private JTextField textField;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 3;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/editString298x76.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Pressed.png");

		// Actions
		okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					// Take whatever action is appropriate for the string being edited
					if (getCityBeingNamed () != null)
					{
						final ChooseCityNameMessage msg = new ChooseCityNameMessage ();
						msg.setCityLocation (getCityBeingNamed ());
						msg.setCityName (getText ());
					
						getClient ().getServerConnection ().sendMessageToServer (msg);					
					}
					else
						log.warn ("EditStringUI had string entered for prompt \"" + promptLabel.getText () + " but took no action from clicking the OK button");
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
				
				// Close out the window
				getFrame ().dispose ();
			}
		};
		
		// Initialize the frame
		final EditStringUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		contentPane.setBorder (BorderFactory.createEmptyBorder (2, 19, 8, 19));
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		promptLabel = getUtils ().createLabel (MomUIConstants.GOLD, getLargeFont ());
		final GridBagConstraints promptConstraints = getUtils ().createConstraintsNoFill (0, 0, 1, 1, INSET, GridBagConstraintsNoFill.WEST);
		promptConstraints.weightx = 1;		// Push the OK button over to the right
		contentPane.add (promptLabel, promptConstraints);

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (1, 0, 1, 1, INSET, GridBagConstraintsNoFill.EAST));
		
		textField = getUtils ().createTransparentTextField (MomUIConstants.SILVER, getLargeFont (), new Dimension (244, 20));
		textField.setText (text);
		contentPane.add (textField, getUtils ().createConstraintsNoFill (0, 1, 3, 1, INSET, GridBagConstraintsNoFill.CENTRE));
		
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

		// Title
		if (getTitle () != null)
			getFrame ().setTitle (getTitle ());
		else
			getFrame ().setTitle (getLanguage ().findCategoryEntry (getTitleLanguageCategoryID (), getTitleLanguageEntryID ()));
		
		// Prompt
		if (getPrompt () != null)
			promptLabel.setText (getPrompt ());
		else
			promptLabel.setText (getLanguage ().findCategoryEntry (getPromptLanguageCategoryID (), getPromptLanguageEntryID ()));
		
		// No action text to set, because the button has OK on it as part of the image

		log.trace ("Exiting languageChanged");
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
	 * @return Language category ID to use for the title; null if title is not language-variable
	 */
	public final String getTitleLanguageCategoryID ()
	{
		return titleLanguageCategoryID;
	}

	/**
	 * @param cat Language category ID to use for the title; null if title is not language-variable
	 */
	public final void setTitleLanguageCategoryID (final String cat)
	{
		titleLanguageCategoryID = cat;
	}
	
	/**
	 * @return Language entry ID to use for the title; null if title is not language-variable
	 */
	public final String getTitleLanguageEntryID ()
	{
		return titleLanguageEntryID;
	}

	/**
	 * @param entry Language entry ID to use for the title; null if title is not language-variable
	 */
	public final void setTitleLanguageEntryID (final String entry)
	{
		titleLanguageEntryID = entry;
	}
	
	/**
	 * @return Language category ID to use for the message; null if message is not language-variable
	 */
	public final String getPromptLanguageCategoryID ()
	{
		return promptLanguageCategoryID;
	}
	
	/**
	 * @param cat Language category ID to use for the message; null if message is not language-variable
	 */
	public final void setPromptLanguageCategoryID (final String cat)
	{
		promptLanguageCategoryID = cat;
	}

	/**
	 * @return Language entry ID to use for the message; null if message is not language-variable
	 */
	public final String getPromptLanguageEntryID ()
	{
		return promptLanguageEntryID;
	}
	
	/**
	 * @param ent Language entry ID to use for the message; null if message is not language-variable
	 */
	public final void setPromptLanguageEntryID (final String ent)
	{
		promptLanguageEntryID = ent;
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
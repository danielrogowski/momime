package momime.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.common.messages.clienttoserver.v0_9_5.ChooseCityNameMessage;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Frame which a prompt, text box, and takes some action when OK is clicked
 */
public final class EditStringUI extends MomClientAbstractUI
{
	/** Class logger */
	private final Logger log = Logger.getLogger (EditStringUI.class.getName ());
	
	/** Large font */
	private Font largeFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** OK action */
	private Action okAction;

	/** Prompt text saying what needs to be entered */
	private JLabel promptLabel;
	
	/** Language category ID to use for the prompt; null if prompt is not language-variable */
	private String languageCategoryID;
	
	/** Language entry ID to use for the prompt; null if prompt is not language-variable */
	private String languageEntryID;
	
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
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/editString298x76.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Pressed.png");

		// Actions
		final EditStringUI ui = this;
		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -7614787019328142967L;

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
						log.warning ("EditStringUI had string entered for prompt \"" + promptLabel.getText () + " but took no action from clicking the OK button");
				}
				catch (final Exception e)
				{
					e.printStackTrace ();
				}
				
				// Close out the window
				getLanguageChangeMaster ().removeLanuageChangeListener (ui);
				getFrame ().dispose ();
			}
		};
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = 2146795759115714418L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				g.drawImage (background, 0, 0, null);
			}
		};
		contentPane.setBackground (Color.BLACK);
		contentPane.setBorder (BorderFactory.createEmptyBorder (2, 19, 8, 19));
		
		final Dimension fixedSize = new Dimension (background.getWidth (), background.getHeight ());
 		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		promptLabel = getUtils ().createLabel (MomUIUtils.GOLD, getLargeFont ());
		final GridBagConstraints promptConstraints = getUtils ().createConstraints (0, 0, 1, INSET, GridBagConstraints.WEST);
		promptConstraints.weightx = 1;		// Push the OK button over to the right
		contentPane.add (promptLabel, promptConstraints);

		contentPane.add (getUtils ().createImageButton (okAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraints (1, 0, 1, INSET, GridBagConstraints.EAST));
		
		textField = getUtils ().createTransparentTextField (MomUIUtils.SILVER, getLargeFont (), new Dimension (244, 20));
		textField.setText (text);
		contentPane.add (textField, getUtils ().createConstraints (0, 2, 3, INSET, GridBagConstraints.CENTER));
		
		// Lock frame size
		getFrame ().setUndecorated (true);
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setLocationRelativeTo (null);
		getFrame ().setResizable (false);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		if (getPrompt () != null)
			promptLabel.setText (getPrompt ());
		else
			promptLabel.setText (getLanguage ().findCategoryEntry (getLanguageCategoryID (), getLanguageEntryID ()));
		
		getFrame ().setTitle (promptLabel.getText ());
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
	 * @return Language category ID to use for the message; null if message is not language-variable
	 */
	public final String getLanguageCategoryID ()
	{
		return languageCategoryID;
	}
	
	/**
	 * @param cat Language category ID to use for the message; null if message is not language-variable
	 */
	public final void setLanguageCategoryID (final String cat)
	{
		languageCategoryID = cat;
	}

	/**
	 * @return Language entry ID to use for the message; null if message is not language-variable
	 */
	public final String getLanguageEntryID ()
	{
		return languageEntryID;
	}
	
	/**
	 * @param ent Language entry ID to use for the message; null if message is not language-variable
	 */
	public final void setLanguageEntryID (final String ent)
	{
		languageEntryID = ent;
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

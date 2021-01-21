package momime.client.ui.frames;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.ui.MomUIConstants;
import momime.common.database.LanguageText;

/**
 * Frame which displays a long calculation with an OK button
 */
public final class CalculationBoxUI extends MomClientFrameUI
{
	/** XML layout */
	private XmlLayoutContainerEx calculationBoxLayout;
	
	/** Small font */
	private Font smallFont;
	
	/** OK action */
	private Action okAction;

	/** Message text */
	private JTextArea messageText;

	/** Language options to use for the title; null if title is not language-variable */
	private List<LanguageText> languageTitle;
	
	/** Language category ID to use for the message; null if message is not language-variable */
	private List<LanguageText> languageText;

	/** Fixed title that isn't lanuage variant; null if title is language variable */
	private String title;	
	
	/** Fixed text that isn't lanuage variant; null if message is language variable */
	private String text;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/calculationBox498x200.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");

		// Actions
		okAction = new LoggingAction ((ev) -> getFrame ().dispose ());
		
		// Initialize the frame
		final CalculationBoxUI ui = this;
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
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getCalculationBoxLayout ()));
		
		messageText = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (getUtils ().createTransparentScrollPane (messageText), "frmCalculationBoxText");

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, MomUIConstants.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmCalculationBoxOK");
		
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
		String useTitle;
		if (getTitle () != null)
			useTitle = getTitle ();
		else
			useTitle = getLanguageHolder ().findDescription (getLanguageTitle ());
		
		// Text
		String useText;
		if (getText () != null)
			useText = getText ();
		else
			useText = getLanguageHolder ().findDescription (getLanguageText ());
		
		getFrame ().setTitle (useTitle);
		messageText.setText (useText);
		
		// Button
		okAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getSimple ().getOk ()));
	}

	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getCalculationBoxLayout ()
	{
		return calculationBoxLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setCalculationBoxLayout (final XmlLayoutContainerEx layout)
	{
		calculationBoxLayout = layout;
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
	 * @return Language options to use for the title; null if title is not language-variable
	 */
	public final List<LanguageText> getLanguageTitle ()
	{
		return languageTitle;
	}

	/**
	 * @param t Language options to use for the title; null if title is not language-variable
	 */
	public final void setLanguageTitle (final List<LanguageText> t)
	{
		languageTitle = t;
	}
	
	/**
	 * @return Language category ID to use for the message; null if message is not language-variable
	 */
	public final List<LanguageText> getLanguageText ()
	{
		return languageText;
	}
	
	/**
	 * @param t Language category ID to use for the message; null if message is not language-variable
	 */
	public final void setLanguageText (final List<LanguageText> t)
	{
		languageText = t;
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
	public final String getText ()
	{
		return text;
	}

	/**
	 * @param txt Fixed text that isn't lanuage variant; null if message is language variable
	 */
	public final void setText (final String txt)
	{
		text = txt;
	}
}
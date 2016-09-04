package momime.client.ui.frames;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.ui.MomUIConstants;

/**
 * Frame which displays a long calculation with an OK button
 */
public final class CalculationBoxUI extends MomClientFrameUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (CalculationBoxUI.class);

	/** XML layout */
	private XmlLayoutContainerEx calculationBoxLayout;
	
	/** Small font */
	private Font smallFont;
	
	/** OK action */
	private Action okAction;

	/** Message text */
	private JTextArea messageText;

	/** Language category ID to use for the title; null if title is not language-variable */
	private String titleLanguageCategoryID;
	
	/** Language entry ID to use for the title; null if title is not language-variable */
	private String titleLanguageEntryID;
	
	/** Language category ID to use for the message; null if message is not language-variable */
	private String textLanguageCategoryID;
	
	/** Language entry ID to use for the message; null if message is not language-variable */
	private String textLanguageEntryID;

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
		log.trace ("Entering init");
		
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

		log.trace ("Exiting init");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		// Title
		if (getTitle () != null)
			getFrame ().setTitle (getTitle ());
		else
			getFrame ().setTitle (getLanguage ().findCategoryEntry (getTitleLanguageCategoryID (), getTitleLanguageEntryID ()));
		
		// Text
		if (getText () != null)
			messageText.setText (getText ());
		else
			messageText.setText (getLanguage ().findCategoryEntry (getTextLanguageCategoryID (), getTextLanguageEntryID ()));
		
		// Button
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "OK"));
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
	public final String getTextLanguageCategoryID ()
	{
		return textLanguageCategoryID;
	}

	/**
	 * @param cat Language category ID to use for the message; null if message is not language-variable
	 */
	public final void setTextLanguageCategoryID (final String cat)
	{
		textLanguageCategoryID = cat;
	}
	
	/**
	 * @return Language entry ID to use for the message; null if message is not language-variable
	 */
	public final String getTextLanguageEntryID ()
	{
		return textLanguageEntryID;
	}

	/**
	 * @param entry Language entry ID to use for the message; null if message is not language-variable
	 */
	public final void setTextLanguageEntryID (final String entry)
	{
		textLanguageEntryID = entry;
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
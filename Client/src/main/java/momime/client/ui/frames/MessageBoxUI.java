package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import momime.client.MomClient;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.MomUIConstants;
import momime.common.messages.clienttoserver.v0_9_5.DismissUnitMessage;
import momime.common.messages.v0_9_5.MemoryUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.GridBagConstraintsNoFill;

/**
 * Frame which displays a message with an OK button
 */
public final class MessageBoxUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MessageBoxUI.class);
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Variable replacer for outputting skill descriptions */
	private UnitStatsLanguageVariableReplacer unitStatsReplacer;
	
	/** Small font */
	private Font smallFont;
	
	/** OK action */
	private Action okAction;

	/** No action */
	private Action noAction;

	/** Yes action */
	private Action yesAction;

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
	
	/** The unit being dismissed; null if the message box isn't asking about dismissing a unit */
	private MemoryUnit unitToDismiss;
	
	/** Typical inset used on this screen layout */
	private final static int INSET = 8;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/messageBox498x100.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button66x18goldPressed.png");

		// Actions
		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -7614787019328142967L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		
		noAction = new AbstractAction ()
		{
			private static final long serialVersionUID = 2288860186594112684L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().dispose ();
			}
		};
		
		yesAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -2607638551174255278L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				try
				{
					// Dismiss a unit on overland map - just tell the server, it deals with the request
					if (getUnitToDismiss () != null)
					{
						final DismissUnitMessage msg = new DismissUnitMessage ();
						msg.setUnitURN (getUnitToDismiss ().getUnitURN ());
						getClient ().getServerConnection ().sendMessageToServer (msg);
					}
					else
						log.warn ("MessageBoxUI had yes button clicked for text \"" + messageText.getText () + " but took no action");
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
				
				// Close the form
				getFrame ().dispose ();
			}
		};
		
		// Initialize the frame
		final MessageBoxUI ui = this;
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getFrame ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanuageChangeListener (ui);
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		final int buttonCount = (getUnitToDismiss () == null) ? 1 : 2;
		
		final GridBagConstraints constraints = getUtils ().createConstraintsBothFill (0, 0, buttonCount, 1, new Insets (INSET, INSET, 3, INSET));
		constraints.weightx = 1;
		constraints.weighty = 1;
		
		messageText = getUtils ().createWrappingLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (getUtils ().createTransparentScrollPane (messageText), constraints);

		// Is it just a regular message box with an OK button, or do we need separate no/yes buttons?
		if (buttonCount == 1)
			contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
				buttonNormal, buttonPressed, buttonNormal), getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, INSET, 9, INSET), GridBagConstraintsNoFill.CENTRE));
		else
		{
			final GridBagConstraints constraintsNo = getUtils ().createConstraintsNoFill (0, 1, 1, 1, new Insets (0, INSET, 9, INSET), GridBagConstraintsNoFill.EAST);
			constraintsNo.weightx = 0.5;
			
			contentPane.add (getUtils ().createImageButton (noAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
				buttonNormal, buttonPressed, buttonNormal), constraintsNo);

			final GridBagConstraints constraintsYes = getUtils ().createConstraintsNoFill (1, 1, 1, 1, new Insets (0, INSET, 9, INSET), GridBagConstraintsNoFill.WEST);
			constraintsYes.weightx = 0.5;
			
			contentPane.add (getUtils ().createImageButton (yesAction, MomUIConstants.GOLD, Color.BLACK, getSmallFont (),
				buttonNormal, buttonPressed, buttonNormal), constraintsYes);
		}
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);		// Must turn resizeable off before calling pack, so pack uses the size for the correct type of window decorations
		getFrame ().pack ();
		getFrame ().setLocationRelativeTo (null);
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
			useTitle = getLanguage ().findCategoryEntry (getTitleLanguageCategoryID (), getTitleLanguageEntryID ());
		
		// Text
		String useText;
		if (getText () != null)
			useText = getText ();
		else
			useText = getLanguage ().findCategoryEntry (getTextLanguageCategoryID (), getTextLanguageEntryID ());
		
		// If we've got a unit, use it to replace variables in the text
		if (getUnitToDismiss () != null)
		{
			getUnitStatsReplacer ().setUnit (getUnitToDismiss ());
			useTitle = getUnitStatsReplacer ().replaceVariables (useTitle);
			useText = getUnitStatsReplacer ().replaceVariables (useText);
		}
		
		getFrame ().setTitle (useTitle);
		messageText.setText (useText);
		
		// Button
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "OK"));
		noAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "No"));
		yesAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "Yes"));
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
	 * @return Variable replacer for outputting skill descriptions
	 */
	public final UnitStatsLanguageVariableReplacer getUnitStatsReplacer ()
	{
		return unitStatsReplacer;
	}

	/**
	 * @param replacer Variable replacer for outputting skill descriptions
	 */
	public final void setUnitStatsReplacer (final UnitStatsLanguageVariableReplacer replacer)
	{
		unitStatsReplacer = replacer;
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

	/**
	 * @return The unit being dismissed; null if the message box isn't asking about dismissing a unit
	 */
	public final MemoryUnit getUnitToDismiss ()
	{
		return unitToDismiss;
	}

	/**
	 * @param unit The unit being dismissed; null if the message box isn't asking about dismissing a unit
	 */
	public final void setUnitToDismiss (final MemoryUnit unit)
	{
		unitToDismiss = unit;
	}
}
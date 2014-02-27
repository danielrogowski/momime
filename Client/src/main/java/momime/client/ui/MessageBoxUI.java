package momime.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * Frame which displays a message with an OK button
 */
public final class MessageBoxUI extends MomClientAbstractUI
{
	/** Small font */
	private Font smallFont;
	
	/** OK action */
	private Action okAction;

	/** Message text */
	private JTextArea messageText;
	
	/** Language category ID to use for the message; null if message is not language-variable */
	private String languageCategoryID;
	
	/** Language entry ID to use for the message; null if message is not language-variable */
	private String languageEntryID;
	
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
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Pressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Disabled.png");

		// Actions
		final MessageBoxUI ui = this;
		okAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -7614787019328142967L;

			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getLanguageChangeMaster ().removeLanuageChangeListener (ui);
				getFrame ().dispose ();
			}
		};
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = 4787936461589746999L;

			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				
				// Scale the background image up smoothly
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
				g.drawImage (background, 0, 0, getWidth (), getHeight (), null);
			}
		};
		contentPane.setBackground (Color.BLACK);
		
		final Dimension fixedSize = new Dimension (background.getWidth (), background.getHeight ());
 		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		final GridBagConstraints constraints = getUtils ().createConstraints (0, 0, 1, INSET, GridBagConstraints.CENTER);
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		
		messageText = getUtils ().createTextArea (MomUIUtils.SILVER, getSmallFont ());
		messageText.setEditable (false);
		messageText.setOpaque (false);
		messageText.setWrapStyleWord (true);		// This is why we have to use a JTextArea, since JLabels don't support wrapping
		messageText.setLineWrap (true);
		messageText.setBorder (new EmptyBorder (0, 0, 0, 0));
		
		// Setting background to null just paints it black - to make it invisible we have to explicitly create a colour with 0 alpha component (4th param)
		messageText.setBackground (new Color (0, 0, 0, 0));
		
		contentPane.add (messageText, constraints);

		contentPane.add (getUtils ().createImageButton (okAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), getUtils ().createConstraints (0, 1, 1, INSET, GridBagConstraints.CENTER));
		
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
		messageText.setText (getLanguage ().findCategoryEntry (getLanguageCategoryID (), getLanguageEntryID ()));
		
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmMessageBox", "OK"));
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
}

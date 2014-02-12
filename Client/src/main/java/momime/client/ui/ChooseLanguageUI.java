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
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Screen for changing the selected language
 */
public final class ChooseLanguageUI extends MomClientAbstractUI
{
	/** Suffix we expect language files to have */
	private static final String FILE_SUFFIX = ".master of magic language.xml";
	
	/** Large font */
	private Font largeFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Where to look for language XML files */
	private String pathToLanguageXmlFiles;
	
	/** Cancel action */
	private Action cancelAction;

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
		final BufferedImage background = ImageIO.read (getClass ().getResource ("/momime.client.graphics/ui/newGame/background.png"));
		final BufferedImage divider = ImageIO.read (getClass ().getResource ("/momime.client.graphics/ui/newGame/divider.png"));
		final BufferedImage buttonNormal = ImageIO.read (getClass ().getResource ("/momime.client.graphics/ui/buttons/button74x21Normal.png"));
		final BufferedImage buttonPressed = ImageIO.read (getClass ().getResource ("/momime.client.graphics/ui/buttons/button74x21Pressed.png"));

		// Get list of files
		final FilenameFilter filter = new FilenameFilter ()
		{
			@Override
			public final boolean accept (final File dir, final String name)
			{
				return name.toLowerCase ().endsWith (FILE_SUFFIX);
			}
		};
		
		final String [] files = new File (getPathToLanguageXmlFiles ()).list (filter);

		// Actions
		cancelAction = new AbstractAction ()
		{
			private static final long serialVersionUID = -1748411812469406390L;

			@Override
			public void actionPerformed (final ActionEvent e)
			{
				getFrame ().setVisible (false);
			}
		};

		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);

		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			private static final long serialVersionUID = -1066389361645625294L;

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
		
		final Dimension fixedSize = new Dimension (640, 480);
 		contentPane.setPreferredSize (fixedSize);
		
		// Set up layout
		contentPane.setLayout (new GridBagLayout ());
		
		// Cut off left half of the window
		contentPane.add (Box.createRigidArea (new Dimension (335, 0)), getUtils ().createConstraints (0, 0, INSET, GridBagConstraints.CENTER));
		
		// Header
		contentPane.add (getUtils ().createLabel (MomUIUtils.GOLD, getLargeFont (), getLanguage ().findCategoryEntry ("frmChooseLanguage", "Title")),
			getUtils ().createConstraints (1, 0, INSET, GridBagConstraints.CENTER));
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, 1, INSET, GridBagConstraints.CENTER));
		
		// Actual language choice buttons
		int gridy = 2;
		for (final String file : files)
		{
			final String language = file.substring (0, file.length () - FILE_SUFFIX.length ());
			
			final Action languageAction = new AbstractAction (language)
			{
				@Override
				public void actionPerformed (final ActionEvent e)
				{
				}
			};
			
			contentPane.add (getUtils ().createImageButton (languageAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed),
				getUtils ().createConstraints (1, gridy, INSET, GridBagConstraints.CENTER));
			gridy++;
		}
		
		// Space in between
		final GridBagConstraints constraints = getUtils ().createConstraints (1, gridy, INSET, GridBagConstraints.CENTER);
		constraints.weightx = 1;
		constraints.weighty = 1;
		contentPane.add (Box.createGlue (), constraints);
		gridy++;
		
		// Footer
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, gridy, INSET, GridBagConstraints.CENTER));
		gridy++;
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (), buttonNormal, buttonPressed),
			getUtils ().createConstraints (1, gridy, INSET, GridBagConstraints.EAST));
		
		// Lock frame size
		getFrame ().setContentPane (contentPane);
		getFrame ().pack ();
		getFrame ().setResizable (false);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getFrame ().setTitle (getLanguage ().findCategoryEntry ("frmChooseLanguage", "Title"));
		
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmChooseLanguage", "Cancel"));
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
	 * @return Where to look for language XML files
	 */
	public final String getPathToLanguageXmlFiles ()
	{
		return pathToLanguageXmlFiles;
	}

	/**
	 * @param path Where to look for language XML files
	 */
	public final void setPathToLanguageXmlFiles (final String path)
	{
		pathToLanguageXmlFiles = path;
	}
}
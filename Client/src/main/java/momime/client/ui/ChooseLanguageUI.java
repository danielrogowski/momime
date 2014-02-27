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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.client.language.database.LanguageDatabaseExImpl;

/**
 * Screen for changing the selected language
 */
public final class ChooseLanguageUI extends MomClientAbstractUI implements LanguageChangeMaster
{
	/** Suffix we expect language files to have */
	private static final String FILE_SUFFIX = ".master of magic language.xml";
	
	/** Large font */
	private Font largeFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Main menu UI */
	private MainMenuUI mainMenuUI;
	
	/** Where to look for language XML files */
	private String pathToLanguageXmlFiles;
	
	/** For reading in different language XML files when selection is changed */
	private Unmarshaller languageDatabaseUnmarshaller;
	
	/** Complete client config, so we can replace the selected language */
	private MomImeClientConfig clientConfig;

	/** Marshaller for saving client config */
	private Marshaller clientConfigMarshaller;
	
	/** Title */
	private JLabel title;
	
	/** Cancel action */
	private Action cancelAction;

	/** Typical inset used on this screen layout */
	private final static int INSET = 3;
	
	/** List of screens that need to be notified when the selected language changes */
	private final List<MomClientUI> languageChangeListeners = new ArrayList<MomClientUI> ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/background.png");
		final BufferedImage divider = getUtils ().loadImage ("/momime.client.graphics/ui/newGame/divider.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Pressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button74x21Disabled.png");

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
		
		// Do this "too early" on purpose, so that the window isn't centred over the main menu, but is a little down-right of it
		getFrame ().setLocationRelativeTo (getMainMenuUI ().getFrame ());

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
		contentPane.add (Box.createRigidArea (new Dimension (335, 0)), getUtils ().createConstraints (0, 0, 1, INSET, GridBagConstraints.CENTER));
		
		// Header
		title = getUtils ().createLabel (MomUIUtils.GOLD, getLargeFont (), null);
		contentPane.add (title, getUtils ().createConstraints (1, 0, 1, INSET, GridBagConstraints.CENTER));
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, 1, 1, INSET, GridBagConstraints.CENTER));
		
		// Actual language choice buttons
		int gridy = 2;
		for (final String file : files)
		{
			final String language = file.substring (0, file.length () - FILE_SUFFIX.length ());
			
			final Action languageAction = new AbstractAction (language)
			{
				private static final long serialVersionUID = 7617503785697399045L;

				@Override
				public void actionPerformed (final ActionEvent ev)
				{
					try
					{
						// Load the new langauge XML
						final LanguageDatabaseExImpl lang = (LanguageDatabaseExImpl) getLanguageDatabaseUnmarshaller ().unmarshal (new File (getPathToLanguageXmlFiles () + file));
						lang.buildMaps ();
						getLanguageHolder ().setLanguage (lang);
						
						// Notify all the forms
						for (final MomClientUI ui : languageChangeListeners)
							ui.languageChanged ();
						
						// Update selected language in the config XML
						getClientConfig ().setChosenLanguage (language);
						getClientConfigMarshaller ().marshal (getClientConfig (), new File ("MoMIMEClientConfig.xml"));
						
						// Close out the screen
						setVisible (false);
					}
					catch (final Exception e)
					{
						e.printStackTrace ();
					}
				}
			};
			
			contentPane.add (getUtils ().createImageButton (languageAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
				buttonNormal, buttonPressed, buttonDisabled), getUtils ().createConstraints (1, gridy, 1, INSET, GridBagConstraints.CENTER));
			gridy++;
		}
		
		// Space in between
		final GridBagConstraints constraints = getUtils ().createConstraints (1, gridy, 1, INSET, GridBagConstraints.CENTER);
		constraints.weightx = 1;
		constraints.weighty = 1;
		contentPane.add (Box.createGlue (), constraints);
		gridy++;
		
		// Footer
		contentPane.add (getUtils ().createImage (divider), getUtils ().createConstraints (1, gridy, 1, INSET, GridBagConstraints.CENTER));
		gridy++;
		
		contentPane.add (getUtils ().createImageButton (cancelAction, MomUIUtils.LIGHT_BROWN, MomUIUtils.DARK_BROWN, getSmallFont (),
			buttonNormal, buttonPressed, buttonDisabled), getUtils ().createConstraints (1, gridy, 1, INSET, GridBagConstraints.EAST));
		
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
		final String text = getLanguage ().findCategoryEntry ("frmChooseLanguage", "Title");
		getFrame ().setTitle (text);
		title.setText (text);
		
		cancelAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmChooseLanguage", "Cancel"));
	}
	
	/**
	 * Remember that we need to tell the listener when the user changes the selected language
	 * @param listener Screen on which to call the .languageChanged () method
	 */
	@Override
	public final void addLanuageChangeListener (final MomClientUI listener)
	{
		languageChangeListeners.add (listener);
	}
	
	/**
	 * Since singleton screens have their containers kept around, this is typically only used by prototype screens disposing themselves
	 * @param listener Screen on which to cancel calling the .languageChanged () method
	 */
	@Override
	public final void removeLanuageChangeListener (final MomClientUI listener)
	{
		languageChangeListeners.remove (listener);
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
	 * @return Main menu UI
	 */
	public final MainMenuUI getMainMenuUI ()
	{
		return mainMenuUI;
	}

	/**
	 * @param ui Main menu UI
	 */
	public final void setMainMenuUI (final MainMenuUI ui)
	{
		mainMenuUI = ui;
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

	/**
	 * @return For reading in different language XML files when selection is changed
	 */
	public final Unmarshaller getLanguageDatabaseUnmarshaller ()
	{
		return languageDatabaseUnmarshaller;
	}

	/**
	 * @param unmarshaller For reading in different language XML files when selection is changed
	 */
	public final void setLanguageDatabaseUnmarshaller (final Unmarshaller unmarshaller)
	{
		languageDatabaseUnmarshaller = unmarshaller;
	}

	/**
	 * @return Complete client config, so we can replace the selected language
	 */
	public final MomImeClientConfig getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param cfg Complete client config, so we can replace the selected language
	 */
	public final void setClientConfig (final MomImeClientConfig cfg)
	{
		clientConfig = cfg;
	}

	/**
	 * @return Marshaller for saving client config
	 */
	public final Marshaller getClientConfigMarshaller ()
	{
		return clientConfigMarshaller;
	}

	/**
	 * @param marsh Marshaller for saving client config
	 */
	public final void setClientConfigMarshaller (final Marshaller marsh)
	{
		clientConfigMarshaller = marsh;
	}
}
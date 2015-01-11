package momime.client.ui.frames;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.bind.Marshaller;

import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.client.config.v0_9_5.UnitCombatScale;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.ui.MomUIConstants;
import momime.client.utils.UnitClientUtils;
import momime.common.database.CommonDatabaseConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

/**
 * Options screen, for changing the values held in the config file.
 */
public final class OptionsUI extends MomClientFrameUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (OptionsUI.class);
	
	/** Suffix we expect language files to have */
	private static final String FILE_SUFFIX = ".master of magic language.xml";
	
	/** First sample unit to demonstrate combat scale setting */
	private final static String SAMPLE_UNIT_1_ID = "UN106";
	
	/** Second sample unit to demonstrate combat scale setting */
	private final static String SAMPLE_UNIT_2_ID = "UN194";

	/** Number of figures to draw for first sample unit */
	private final static int SAMPLE_UNIT_1_FIGURE_COUNT = 6;
	
	/** Number of figures to draw for second sample unit  */
	private final static int SAMPLE_UNIT_2_FIGURE_COUNT = 1;
	
	/** Unit type ID of first sample unit (just anything other than 'summoned') */
	private final static String SAMPLE_UNIT_1_UNIT_TYPE_ID = null;
	
	/** Unit type ID of second sample unit */
	private final static String SAMPLE_UNIT_2_UNIT_TYPE_ID = CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED;
	
	/** Direction to show sample units facing */
	private final static int SAMPLE_UNIT_DIRECTION = 4;
	
	/** Sample grass tile to display underneath units */
	private final static String SAMPLE_TILE_FILENAME = "/momime.client.graphics/combat/terrain/arcanus/default/standard/00000000a.png";

	/** XML layout */
	private XmlLayoutContainerEx optionsLayout;
	
	/** Maven version number, injected from spring */
	private String version;
	
	/** Large font */
	private Font largeFont;

	/** Medium font */
	private Font mediumFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Complete client config, so we can edit various settings */
	private MomImeClientConfig clientConfig;
	
	/** Marshaller for saving client config */
	private Marshaller clientConfigMarshaller;
	
	/** Location to save updated client config */
	private String clientConfigLocation;
	
	/** Short title */
	private JLabel shortTitleLabel;
	
	/** Version */
	private JLabel versionLabel;
	
	/** Debug section heading */
	private JLabel debugSection;
	
	/** Overland map section heading */
	private JLabel overlandMapSection;
	
	/** Combat map section heading */
	private JLabel combatMapSection;

	/** Language section heading */
	private JLabel languageSection;
	
	/** True to use the full terrain tileset so e.g. hills flow into each other and coastline is drawn between ocean and land; false to use only "blocky" tiles */
	private JCheckBox overlandSmoothTerrain;
	
	/** When zooming in the overland map, this controls how the textures are scaled up */
	private JCheckBox overlandSmoothTextures;
	
	/** Whether to slightly darken terrain which we have seen, but cannot see now, so we're just remembering what we saw there before */
	private JCheckBox overlandShowPartialFogOfWar;
	
	/** True to use the full FOW tileset so the FOW border slightly enchroaches into tiles we can see and so looks smooth; false to use hard square edges */
	private JCheckBox overlandSmoothFogOfWar;	
	
	/** True to use the full terrain tileset so e.g. ridges and dark areas run together; false to use only "blocky" combat tiles */
	private JCheckBox combatSmoothTerrain;

	/** True to view all Units, Buildings and Spell URNs */
	private JCheckBox debugShowURNs;

	/** True to show a white line around the map edges, so you have a point of reference along axes that wrap */
	private JCheckBox debugShowEdgesOfMap;

	/** Whether to scale units to bigger resolution by drawing them bigger, or drawing them with more figures */
	private JLabel combatScaleLabel;

	/** Choose language label */
	private JLabel chooseLanguageLabel;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/options/background.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/options/okButtonNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/options/okButtonPressed.png");

		final BufferedImage checkboxUnticked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Unticked.png");
		final BufferedImage checkboxTicked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Ticked.png");
		
		// Actions
		final Action okAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				getFrame ().setVisible (false);
			}
		};
		
		final Action changeUnitCombatScaleAction = new AbstractAction ()
		{
			@Override
			public final void actionPerformed (final ActionEvent ev)
			{
				switch (getClientConfig ().getUnitCombatScale ())
				{
					case DOUBLE_SIZE_UNITS:
						getClientConfig ().setUnitCombatScale (UnitCombatScale.FOUR_TIMES_FIGURES);
						break;
						
					case FOUR_TIMES_FIGURES:
						getClientConfig ().setUnitCombatScale (UnitCombatScale.FOUR_TIMES_FIGURES_EXCEPT_SINGLE_SUMMONED);
						break;
						
					case FOUR_TIMES_FIGURES_EXCEPT_SINGLE_SUMMONED:
						getClientConfig ().setUnitCombatScale (UnitCombatScale.DOUBLE_SIZE_UNITS);
						break;
				}
				
				saveConfigFile ();
			}
		};			
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);

		final Dimension size = new Dimension (background.getWidth (), background.getHeight ());
		contentPane.setMinimumSize (size);
		contentPane.setMaximumSize (size);
		contentPane.setPreferredSize (size);
		
		contentPane.setBackground (Color.BLACK);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getOptionsLayout ()));
		
		contentPane.add (getUtils ().createImageButton (okAction, null, null, null, buttonNormal, buttonPressed, buttonNormal), "frmOptionsOK");
		
		shortTitleLabel = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (shortTitleLabel, "frmOptionsIMELabel");
		
		versionLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (versionLabel, "frmOptionsVersion");

		debugSection = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (debugSection, "frmOptionsDebugSection");

		overlandMapSection = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (overlandMapSection, "frmOptionsOverlandMapSection");

		combatMapSection = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (combatMapSection, "frmOptionsCombatMapSection");

		languageSection = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (languageSection, "frmOptionsLanguageSection");
		
		overlandSmoothTerrain = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (overlandSmoothTerrain, "frmOptionsOverlandSmoothTerrain");
		
		overlandSmoothTextures = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (overlandSmoothTextures, "frmOptionsLinearTextureFilter");
		
		overlandShowPartialFogOfWar = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (overlandShowPartialFogOfWar, "frmOptionsOverlandShowPartialFogOfWar");
		
		overlandSmoothFogOfWar = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (overlandSmoothFogOfWar, "frmOptionsOverlandSmoothFogOfWar");

		combatSmoothTerrain = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (combatSmoothTerrain, "frmOptionsSmoothCombatTerrain");

		debugShowURNs = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (debugShowURNs, "frmOptionsShowURNs");
		
		debugShowEdgesOfMap = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (debugShowEdgesOfMap, "frmOptionsShowEdgesOfMap");
		
		combatScaleLabel = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (combatScaleLabel, "frmOptionsCombatUnitScale");

		chooseLanguageLabel = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (chooseLanguageLabel, "frmOptionsChooseLanguage");
		
		final JButton changeUnitCombatScaleButton = new JButton (changeUnitCombatScaleAction)
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Draw the sample units
				try
				{
					getUnitClientUtils ().drawUnitFigures (SAMPLE_UNIT_1_ID, SAMPLE_UNIT_1_UNIT_TYPE_ID, SAMPLE_UNIT_1_FIGURE_COUNT, SAMPLE_UNIT_1_FIGURE_COUNT,
						GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, SAMPLE_UNIT_DIRECTION, g, 0, 26, SAMPLE_TILE_FILENAME, true);

					getUnitClientUtils ().drawUnitFigures (SAMPLE_UNIT_2_ID, SAMPLE_UNIT_2_UNIT_TYPE_ID, SAMPLE_UNIT_2_FIGURE_COUNT, SAMPLE_UNIT_2_FIGURE_COUNT,
						GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, SAMPLE_UNIT_DIRECTION, g, 65, 26, SAMPLE_TILE_FILENAME, true);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		};
		contentPane.add (changeUnitCombatScaleButton, "frmOptionsCombatSampleUnits");
		
		// Set up languages drop down list
		final FilenameFilter filter = new FilenameFilter ()
		{
			@Override
			public final boolean accept (final File dir, final String name)
			{
				return name.toLowerCase ().endsWith (FILE_SUFFIX);
			}
		};

		Integer currentLanguageIndex = null;
		final String [] files = new File (getClientConfig ().getPathToLanguageXmlFiles ()).list (filter);
		for (int n = 0; n < files.length; n++)
		{
			files [n] = files [n].substring (0, files [n].length () - FILE_SUFFIX.length ());
			if (files [n].equals (getClientConfig ().getChosenLanguage ()))
				currentLanguageIndex = n;
		}
		
		final JComboBox<String> chooseLanguage = new JComboBox<String> (files);
		chooseLanguage.setFont (getSmallFont ());
		
		contentPane.add (chooseLanguage, "frmOptionsChooseLanguageList");
		
		// Set all options according to current config
		if (currentLanguageIndex != null)
			chooseLanguage.setSelectedIndex (currentLanguageIndex);
		
		overlandSmoothTerrain.setSelected				(getClientConfig ().isOverlandSmoothTerrain ());
		overlandSmoothTextures.setSelected			(getClientConfig ().isOverlandSmoothTextures ());
		overlandShowPartialFogOfWar.setSelected	(getClientConfig ().isOverlandShowPartialFogOfWar ());
		overlandSmoothFogOfWar.setSelected			(getClientConfig ().isOverlandSmoothFogOfWar ());
		combatSmoothTerrain.setSelected				(getClientConfig ().isCombatSmoothTerrain ());
		debugShowURNs.setSelected						(getClientConfig ().isDebugShowURNs ());
		debugShowEdgesOfMap.setSelected			(getClientConfig ().isDebugShowEdgesOfMap ());
		
		// Changing the language saves out the config file
		chooseLanguage.addItemListener (new ItemListener ()
		{
			@Override
		    public final void itemStateChanged (final ItemEvent ev)
		    {
				getClientConfig ().setChosenLanguage (chooseLanguage.getSelectedItem ().toString ());
				saveConfigFile ();
		    }
		});
		
		// Clicking any options saves out the config file
		overlandSmoothTerrain.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (final ItemEvent ev)
			{
				getClientConfig ().setOverlandSmoothTerrain (overlandSmoothTerrain.isSelected ());
				saveConfigFile ();
			}
		});
		
		overlandSmoothTextures.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (final ItemEvent ev)
			{
				getClientConfig ().setOverlandSmoothTextures (overlandSmoothTextures.isSelected ());
				saveConfigFile ();
			}
		});

		overlandShowPartialFogOfWar.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (final ItemEvent ev)
			{
				getClientConfig ().setOverlandShowPartialFogOfWar (overlandShowPartialFogOfWar.isSelected ());
				saveConfigFile ();
			}
		});

		overlandSmoothFogOfWar.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (final ItemEvent ev)
			{
				getClientConfig ().setOverlandSmoothFogOfWar (overlandSmoothFogOfWar.isSelected ());
				saveConfigFile ();
			}
		});
	
		combatSmoothTerrain.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (final ItemEvent ev)
			{
				getClientConfig ().setCombatSmoothTerrain (combatSmoothTerrain.isSelected ());
				saveConfigFile ();
			}
		});

		debugShowURNs.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (final ItemEvent ev)
			{
				getClientConfig ().setDebugShowURNs (debugShowURNs.isSelected ());
				saveConfigFile ();
			}
		});

		debugShowEdgesOfMap.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (final ItemEvent ev)
			{
				getClientConfig ().setDebugShowEdgesOfMap (debugShowEdgesOfMap.isSelected ());
				saveConfigFile ();
			}
		});

		// Set up unit animations
		getUnitClientUtils ().registerUnitFiguresAnimation (SAMPLE_UNIT_1_ID, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, SAMPLE_UNIT_DIRECTION, contentPane);
		getUnitClientUtils ().registerUnitFiguresAnimation (SAMPLE_UNIT_2_ID, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, SAMPLE_UNIT_DIRECTION, contentPane);
		
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
		
		getFrame ().setTitle							(getLanguage ().findCategoryEntry ("frmOptions", "Title"));
		shortTitleLabel.setText						(getLanguage ().findCategoryEntry ("frmMainMenu", "ShortTitle"));
		versionLabel.setText							(getLanguage ().findCategoryEntry ("frmMainMenu", "Version").replaceAll ("VERSION", getVersion ()));
		
		debugSection.setText							(getLanguage ().findCategoryEntry ("frmOptions", "DebugSection"));
		overlandMapSection.setText				(getLanguage ().findCategoryEntry ("frmOptions", "OverlandMapSection"));
		combatMapSection.setText					(getLanguage ().findCategoryEntry ("frmOptions", "CombatMapSection"));
		languageSection.setText						(getLanguage ().findCategoryEntry ("frmOptions", "LanguageSection"));
		
		overlandSmoothTerrain.setText			(getLanguage ().findCategoryEntry ("frmOptions", "SmoothTerrain"));
		overlandSmoothTextures.setText			(getLanguage ().findCategoryEntry ("frmOptions", "LinearTextureFilter"));
		overlandShowPartialFogOfWar.setText	(getLanguage ().findCategoryEntry ("frmOptions", "ShowFogOfWar"));
		overlandSmoothFogOfWar.setText		(getLanguage ().findCategoryEntry ("frmOptions", "SmoothFogOfWar"));
		combatSmoothTerrain.setText				(getLanguage ().findCategoryEntry ("frmOptions", "SmoothTerrain"));
		debugShowURNs.setText						(getLanguage ().findCategoryEntry ("frmOptions", "ShowUnitURNs"));
		debugShowEdgesOfMap.setText			(getLanguage ().findCategoryEntry ("frmOptions", "ShowEdgesOfMap"));
		combatScaleLabel.setText					(getLanguage ().findCategoryEntry ("frmOptions", "CombatUnitScale"));
		chooseLanguageLabel.setText				(getLanguage ().findCategoryEntry ("frmOptions", "ChooseLanguage"));
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * After any change to the config options, we resave out the config XML immediately
	 */
	private final void saveConfigFile ()
	{
		log.trace ("Entering saveConfigFile");

		try
		{
			getClientConfigMarshaller ().marshal (getClientConfig (), new File (getClientConfigLocation ()));
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}

		log.trace ("Exiting saveConfigFile");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getOptionsLayout ()
	{
		return optionsLayout;
	}
	
	/**
	 * @param layout XML layout
	 */
	public final void setOptionsLayout (final XmlLayoutContainerEx layout)
	{
		optionsLayout = layout;
	}

	/**
	 * @return Maven version number, injected from spring
	 */
	public final String getVersion ()
	{
		return version;
	}

	/**
	 * @param ver Maven version number, injected from spring
	 */
	public final void setVersion (final String ver)
	{
		version = ver;
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
	 * @return Medium font
	 */
	public final Font getMediumFont ()
	{
		return mediumFont;
	}

	/**
	 * @param font Medium font
	 */
	public final void setMediumFont (final Font font)
	{
		mediumFont = font;
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
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
	}

	/**
	 * @return Complete client config, so we can edit various settings
	 */
	public final MomImeClientConfig getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param cfg Complete client config, so we can edit various settings
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

	/**
	 * @return Location to save updated client config
	 */
	public final String getClientConfigLocation ()
	{
		return clientConfigLocation;
	}

	/**
	 * @param loc Location to save updated client config
	 */
	public final void setClientConfigLocation (final String loc)
	{
		clientConfigLocation = loc;
	}
}
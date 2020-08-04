package momime.client.ui.frames;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;
import com.ndg.zorder.ZOrderGraphicsImmediateImpl;

import momime.client.MomClient;
import momime.client.calculations.OverlandMapBitmapGenerator;
import momime.client.config.MomImeClientConfigEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.LanguageVariableUI;
import momime.client.language.database.LanguageDatabaseExImpl;
import momime.client.ui.MomUIConstants;
import momime.client.utils.UnitClientUtils;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitCombatScale;
import momime.common.database.UnitSkillTypeID;

/**
 * Options screen, for changing the values held in the config file.
 */
public final class OptionsUI extends MomClientFrameUI implements LanguageChangeMaster
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (OptionsUI.class);
	
	/** Suffix we expect language files to have */
	private static final String FILE_SUFFIX = ".Master of Magic Language.xml";
	
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
	private MomImeClientConfigEx clientConfig;
	
	/** Marshaller for saving client config */
	private Marshaller clientConfigMarshaller;
	
	/** Location to save updated client config */
	private String clientConfigLocation;

	/** Where to look for language XML files */
	private String pathToLanguageXmlFiles;
	
	/** For reading in different language XML files when selection is changed */
	private Unmarshaller languageDatabaseUnmarshaller;

	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Overland map bitmap generator */
	private OverlandMapBitmapGenerator overlandMapBitmapGenerator;
	
	/** Combat UI */
	private CombatUI combatUI;

	/** Multiplayer client */
	private MomClient client;
	
	/** Short title */
	private JLabel shortTitleLabel;
	
	/** Version */
	private JLabel versionLabel;

	/** Unit info section heading */
	private JLabel unitInfoSection;
	
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

	/** Whether to draw a border around the area we control */
	private JCheckBox overlandShowOurBorder;
	
	/** Whether to draw borders around the area other wizards control */
	private JCheckBox overlandShowEnemyBorders;
	
	/** Whether to animate units moving on the overland map (turning it off speeds up the game a lot, especially once you have Awareness cast) */
	private JCheckBox overlandAnimateUnitsMoving;
	
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
	
	/** True to show still hero portraits; false to show animated combat pic like other untis */
	private JCheckBox showHeroPortraits;

	/** Unit attributes label */
	private JLabel unitAttributesLabel;
	
	/** Unit attributes choice combo box */
	private JComboBox<String> unitAttributesChoice;
	
	/** Ok action */
	private Action okAction;
	
	/** List of screens that need to be notified when the selected language changes */
	private final List<LanguageVariableUI> languageChangeListeners = new ArrayList<LanguageVariableUI> ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/options.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldNormal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldPressed.png");
		final BufferedImage buttonDisabled = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button80x26goldDisabled.png");

		final BufferedImage checkboxUnticked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Unticked.png");
		final BufferedImage checkboxTicked = getUtils ().loadImage ("/momime.client.graphics/ui/checkBoxes/checkbox11x11Ticked.png");
		
		// Actions
		okAction = new LoggingAction ((ev) -> getFrame ().setVisible (false));
		
		final Action changeUnitCombatScaleAction = new LoggingAction ((ev) ->
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
		});			
		
		// Initialize the content pane
		final JPanel contentPane = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
			}
		};

		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getOptionsLayout ()));
		
		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.DULL_GOLD, MomUIConstants.DARK_BROWN, getLargeFont (),
			buttonNormal, buttonPressed, buttonDisabled), "frmOptionsOK");
		
		shortTitleLabel = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (shortTitleLabel, "frmOptionsIMELabel");
		
		versionLabel = getUtils ().createLabel (MomUIConstants.SILVER, getMediumFont ());
		contentPane.add (versionLabel, "frmOptionsVersion");

		unitInfoSection = getUtils ().createLabel (MomUIConstants.SILVER, getLargeFont ());
		contentPane.add (unitInfoSection, "frmOptionsUnitInfoSection");
		
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
		
		overlandShowOurBorder = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (overlandShowOurBorder, "frmOptionsOverlandShowOurBorder");

		overlandShowEnemyBorders = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (overlandShowEnemyBorders, "frmOptionsOverlandShowEnemyBorders");
		
		overlandAnimateUnitsMoving = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (overlandAnimateUnitsMoving, "frmOptionsOverlandAnimateUnitsMoving");
		
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

		showHeroPortraits = getUtils ().createImageCheckBox (MomUIConstants.SILVER, getSmallFont (), checkboxUnticked, checkboxTicked);
		contentPane.add (showHeroPortraits, "frmOptionsUnitInfoHeroPortraits");
		
		unitAttributesLabel = getUtils ().createLabel (MomUIConstants.SILVER, getSmallFont ());
		contentPane.add (unitAttributesLabel, "frmOptionsUnitInfoAttributes");
		
		unitAttributesChoice = new JComboBox<String> ();
		unitAttributesChoice.setFont (getSmallFont ());
		contentPane.add (unitAttributesChoice, "frmOptionsUnitInfoAttributesList");
		
		final ZOrderGraphicsImmediateImpl zOrderGraphics = new ZOrderGraphicsImmediateImpl ();
		final JButton changeUnitCombatScaleButton = new JButton (changeUnitCombatScaleAction)
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Draw the sample units
				try
				{
					zOrderGraphics.setGraphics (g);
					
					getUnitClientUtils ().drawUnitFigures (SAMPLE_UNIT_1_ID, SAMPLE_UNIT_1_UNIT_TYPE_ID, SAMPLE_UNIT_1_FIGURE_COUNT, SAMPLE_UNIT_1_FIGURE_COUNT,
						GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, SAMPLE_UNIT_DIRECTION, zOrderGraphics, 0, 26, SAMPLE_TILE_FILENAME, true, 0, null);

					getUnitClientUtils ().drawUnitFigures (SAMPLE_UNIT_2_ID, SAMPLE_UNIT_2_UNIT_TYPE_ID, SAMPLE_UNIT_2_FIGURE_COUNT, SAMPLE_UNIT_2_FIGURE_COUNT,
						GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, SAMPLE_UNIT_DIRECTION, zOrderGraphics, 65, 26, SAMPLE_TILE_FILENAME, true, 0, null);
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
			public final boolean accept (@SuppressWarnings ("unused") final File dir, final String name)
			{
				return name.toLowerCase ().endsWith (FILE_SUFFIX.toLowerCase ());
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
		overlandShowOurBorder.setSelected			(getClientConfig ().isOverlandShowOurBorder ());
		overlandShowEnemyBorders.setSelected		(getClientConfig ().isOverlandShowEnemyBorders ());
		overlandAnimateUnitsMoving.setSelected		(getClientConfig ().isOverlandAnimateUnitsMoving ());
		combatSmoothTerrain.setSelected				(getClientConfig ().isCombatSmoothTerrain ());
		showHeroPortraits.setSelected						(getClientConfig ().isShowHeroPortraits ());
		debugShowURNs.setSelected						(getClientConfig ().isDebugShowURNs ());
		debugShowEdgesOfMap.setSelected			(getClientConfig ().isDebugShowEdgesOfMap ());
		
		// Changing the language saves out the config file
		chooseLanguage.addItemListener (new ItemListener ()
		{
			@Override
		    public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
		    {
				try
				{
					final String langName = chooseLanguage.getSelectedItem ().toString ();
					
					// Load the new langauge XML
					final LanguageDatabaseExImpl lang = (LanguageDatabaseExImpl) getLanguageDatabaseUnmarshaller ().unmarshal (new File
						(getPathToLanguageXmlFiles () + "/" + langName + FILE_SUFFIX));
					
					lang.buildMaps ();
					getLanguageHolder ().setLanguage (lang);
					
					// Notify all the forms
					for (final LanguageVariableUI ui : languageChangeListeners)
						ui.languageChanged ();
					
					// Update selected language in the config XML
					getClientConfig ().setChosenLanguage (langName);
					saveConfigFile ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
		    }
		});
		
		// Clicking any options saves out the config file
		overlandSmoothTerrain.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setOverlandSmoothTerrain (overlandSmoothTerrain.isSelected ());
				saveConfigFile ();
				
				try
				{
					getOverlandMapBitmapGenerator ().smoothMapTerrain (null);
					getOverlandMapUI ().regenerateOverlandMapBitmaps ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
		
		overlandSmoothTextures.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setOverlandSmoothTextures (overlandSmoothTextures.isSelected ());
				saveConfigFile ();
			}
		});
		
		overlandShowPartialFogOfWar.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setOverlandShowPartialFogOfWar (overlandShowPartialFogOfWar.isSelected ());
				saveConfigFile ();

				try
				{
					getOverlandMapUI ().regenerateFogOfWarBitmap ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});

		overlandSmoothFogOfWar.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setOverlandSmoothFogOfWar (overlandSmoothFogOfWar.isSelected ());
				saveConfigFile ();

				try
				{
					getOverlandMapUI ().regenerateFogOfWarBitmap ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});
	
		overlandShowOurBorder.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setOverlandShowOurBorder (overlandShowOurBorder.isSelected ());
				saveConfigFile ();
			}
		});

		overlandShowEnemyBorders.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setOverlandShowEnemyBorders (overlandShowEnemyBorders.isSelected ());
				saveConfigFile ();
			}
		});
		
		overlandAnimateUnitsMoving.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setOverlandAnimateUnitsMoving (overlandAnimateUnitsMoving.isSelected ());
				saveConfigFile ();
			}
		});
		
		combatSmoothTerrain.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setCombatSmoothTerrain (combatSmoothTerrain.isSelected ());
				saveConfigFile ();

				try
				{
					getCombatUI ().smoothCombatMapAndGenerateBitmaps ();
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
			}
		});

		debugShowURNs.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setDebugShowURNs (debugShowURNs.isSelected ());
				saveConfigFile ();
				
				for (final UnitInfoUI unitInfo : getClient ().getUnitInfos ().values ())
					try
					{
						unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		});

		debugShowEdgesOfMap.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setDebugShowEdgesOfMap (debugShowEdgesOfMap.isSelected ());
				saveConfigFile ();
			}
		});

		showHeroPortraits.addItemListener (new ItemListener ()
		{
			@Override
			public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
			{
				getClientConfig ().setShowHeroPortraits (showHeroPortraits.isSelected ());
				saveConfigFile ();

				for (final UnitInfoUI unitInfo : getClient ().getUnitInfos ().values ())
					try
					{
						unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		});
		
		unitAttributesChoice.addItemListener (new ItemListener ()
		{
			@Override
		    public final void itemStateChanged (@SuppressWarnings ("unused") final ItemEvent ev)
		    {
				getClientConfig ().setDisplayUnitSkillsAsAttributes (UnitSkillTypeID.values () [unitAttributesChoice.getSelectedIndex ()]);
				saveConfigFile ();

				for (final UnitInfoUI unitInfo : getClient ().getUnitInfos ().values ())
					try
					{
						unitInfo.getUnitInfoPanel ().refreshUnitDetails ();
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
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
		unitInfoSection.setText						(getLanguage ().findCategoryEntry ("frmOptions", "UnitInfoSection"));
		
		overlandSmoothTerrain.setText			(getLanguage ().findCategoryEntry ("frmOptions", "SmoothTerrain"));
		overlandSmoothTextures.setText			(getLanguage ().findCategoryEntry ("frmOptions", "LinearTextureFilter"));
		overlandShowPartialFogOfWar.setText	(getLanguage ().findCategoryEntry ("frmOptions", "ShowFogOfWar"));
		overlandSmoothFogOfWar.setText		(getLanguage ().findCategoryEntry ("frmOptions", "SmoothFogOfWar"));
		overlandShowOurBorder.setText			(getLanguage ().findCategoryEntry ("frmOptions", "ShowOurBorder"));
		overlandShowEnemyBorders.setText	(getLanguage ().findCategoryEntry ("frmOptions", "ShowEnemyBorders"));
		overlandAnimateUnitsMoving.setText	(getLanguage ().findCategoryEntry ("frmOptions", "AnimateUnitsMoving"));
		combatSmoothTerrain.setText				(getLanguage ().findCategoryEntry ("frmOptions", "SmoothTerrain"));
		debugShowURNs.setText						(getLanguage ().findCategoryEntry ("frmOptions", "ShowUnitURNs"));
		debugShowEdgesOfMap.setText			(getLanguage ().findCategoryEntry ("frmOptions", "ShowEdgesOfMap"));
		combatScaleLabel.setText					(getLanguage ().findCategoryEntry ("frmOptions", "CombatUnitScale"));
		chooseLanguageLabel.setText				(getLanguage ().findCategoryEntry ("frmOptions", "ChooseLanguage"));
		showHeroPortraits.setText					(getLanguage ().findCategoryEntry ("frmOptions", "ShowHeroPortraits"));
		unitAttributesLabel.setText					(getLanguage ().findCategoryEntry ("frmOptions", "UnitAttributes"));
		
		okAction.putValue (Action.NAME, getLanguage ().findCategoryEntry ("frmOptions", "OK"));
		
		// Load the enum list for unit attribute choices
		final UnitSkillTypeID selectedSkillType = getClientConfig ().getDisplayUnitSkillsAsAttributes ();
		unitAttributesChoice.removeAllItems ();
		Integer selectedIndex = null;
		int n = 0;
		for (final UnitSkillTypeID unitSkillType : UnitSkillTypeID.values ())
			if (unitSkillType != UnitSkillTypeID.NO_VALUE)
			{
				unitAttributesChoice.addItem (getLanguage ().findCategoryEntry ("frmOptions", "UnitAttributes" + unitSkillType.value ()));
				if (selectedSkillType == unitSkillType)
					selectedIndex = n;
				
				n++;
			}
		
		if (selectedIndex != null)
			unitAttributesChoice.setSelectedIndex (selectedIndex);

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
	 * Remember that we need to tell the listener when the user changes the selected language
	 * @param listener Screen on which to call the .languageChanged () method
	 */
	@Override
	public final void addLanguageChangeListener (final LanguageVariableUI listener)
	{
		languageChangeListeners.add (listener);
	}
	
	/**
	 * Since singleton screens have their containers kept around, this is typically only used by prototype screens disposing themselves
	 * @param listener Screen on which to cancel calling the .languageChanged () method
	 */
	@Override
	public final void removeLanguageChangeListener (final LanguageVariableUI listener)
	{
		languageChangeListeners.remove (listener);
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
	public final MomImeClientConfigEx getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param cfg Complete client config, so we can edit various settings
	 */
	public final void setClientConfig (final MomImeClientConfigEx cfg)
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
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}

	/**
	 * @return Overland map bitmap generator
	 */
	public final OverlandMapBitmapGenerator getOverlandMapBitmapGenerator ()
	{
		return overlandMapBitmapGenerator;
	}
	
	/**
	 * @param gen Overland map bitmap generator
	 */
	public final void setOverlandMapBitmapGenerator (final OverlandMapBitmapGenerator gen)
	{
		overlandMapBitmapGenerator = gen;
	}

	/**
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
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
}
package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.config.MomImeClientConfig;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.LanguageOptionEx;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.MainMenuScreen;
import momime.client.languages.database.OptionsScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.UnitClientUtilsImpl;
import momime.common.database.Language;
import momime.common.database.UnitSkillTypeID;

/**
 * Tests the OptionsUI class
 */
public final class TestOptionsUI extends ClientTestData
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (TestOptionsUI.class);
	
	/**
	 * Tests the OptionsUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOptionsUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final MainMenuScreen mainMenuScreenLang = new MainMenuScreen ();
		mainMenuScreenLang.getShortTitle ().add (createLanguageText (Language.ENGLISH, "Implode's Multiplayer Edition - Client"));
		mainMenuScreenLang.getVersion ().add (createLanguageText (Language.ENGLISH, "version VERSION"));
		
		final OptionsScreen optionsScreenLang = new OptionsScreen ();
		optionsScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Options"));
		optionsScreenLang.getDebug ().add (createLanguageText (Language.ENGLISH, "Debug Options"));
		optionsScreenLang.getOverlandMap ().add (createLanguageText (Language.ENGLISH, "Overland Map"));
		optionsScreenLang.getCombatMap ().add (createLanguageText (Language.ENGLISH, "Combat Map"));
		optionsScreenLang.getLanguage ().add (createLanguageText (Language.ENGLISH, "Language"));
		optionsScreenLang.getUnitInfo ().add (createLanguageText (Language.ENGLISH, "Unit Info Display"));
		
		optionsScreenLang.getSmoothTerrain ().add (createLanguageText (Language.ENGLISH, "Smooth edges of terrain"));
		optionsScreenLang.getLinearTextureFilter ().add (createLanguageText (Language.ENGLISH, "Smooth textures when zooming in on overland map"));
		optionsScreenLang.getShowFogOfWar ().add (createLanguageText (Language.ENGLISH, "Darken areas of the map not currently visible"));
		optionsScreenLang.getSmoothFogOfWar ().add (createLanguageText (Language.ENGLISH, "Smooth edges of visible area"));
		optionsScreenLang.getShowOurBorder ().add (createLanguageText (Language.ENGLISH, "Show border of territory that we control"));
		optionsScreenLang.getShowEnemyBorders ().add (createLanguageText (Language.ENGLISH, "Show borders of territory that others wizards control"));
		optionsScreenLang.getAnimateUnitsMoving ().add (createLanguageText (Language.ENGLISH, "Animate units moving"));
		
		optionsScreenLang.getShowUnitURNs ().add (createLanguageText (Language.ENGLISH, "Show Unit, Building and Spell URNs"));
		optionsScreenLang.getShowEdgesOfMap ().add (createLanguageText (Language.ENGLISH, "Show edges of map"));
		optionsScreenLang.getChooseLanguage ().add (createLanguageText (Language.ENGLISH, "Choose Language:"));
		optionsScreenLang.getShowHeroPortraits ().add (createLanguageText (Language.ENGLISH, "Show hero portraits"));
		
		optionsScreenLang.getUnitAttributes ().add (createLanguageText (Language.ENGLISH, "Show full breakdown (in top list) for:"));
		optionsScreenLang.getUnitAttributesStandard ().add (createLanguageText (Language.ENGLISH, "Only standard attributes Melee, HP, etc"));
		optionsScreenLang.getUnitAttributesModifyable ().add (createLanguageText (Language.ENGLISH, "Skills with values modifyable by exp, auras, etc"));
		optionsScreenLang.getUnitAttributesAll ().add (createLanguageText (Language.ENGLISH, "All skills with values"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getMainMenuScreen ()).thenReturn (mainMenuScreenLang);
		when (lang.getOptionsScreen ()).thenReturn (optionsScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// This is dependant on way too many values to mock them all - so use the real graphics DB
		final GraphicsDatabaseEx gfx = loadGraphicsDatabase (utils, null);
		
		// Animation controller
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		anim.setPlayerColourImageGenerator (gen);
		
		// Language choices
		final MomLanguagesEx languages = mock (MomLanguagesEx.class);
		
		final List<LanguageOptionEx> languageOptions = new ArrayList<LanguageOptionEx> ();
		for (final Language language : Language.values ())
		{
			final LanguageOptionEx option = new LanguageOptionEx ();
			option.setLanguage (language);
			option.setLanguageDescription (language.toString ());
			languageOptions.add (option);
		}
		when (languages.getLanguageOptions ()).thenReturn (languageOptions);

		// Config
		final MomImeClientConfig config = new MomImeClientConfig ();
		config.setCombatSmoothTerrain (true);
		config.setOverlandSmoothTextures (true);
		config.setDebugShowEdgesOfMap (true);
		config.setDisplayUnitSkillsAsAttributes (UnitSkillTypeID.MODIFYABLE);
		
		// Decide a temp location to save updates to the config file
		final String clientConfigLocation = File.createTempFile ("MomImeClientConfig", ".xml").getAbsolutePath ();
		log.info ("Saving test updates to config file to \"" + clientConfigLocation + "\"");
		
		// Marshaller - have to specify the exact class here (not MomImeClientConfig) or saving the config does not work
		final Marshaller marshaller = JAXBContext.newInstance (MomImeClientConfig.class).createMarshaller ();
		marshaller.setProperty (Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		// Unit animations
		final UnitClientUtilsImpl unitClientUtils = new UnitClientUtilsImpl ();
		unitClientUtils.setGraphicsDB (gfx);
		unitClientUtils.setAnim (anim);
		unitClientUtils.setUtils (utils);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/OptionsUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final OptionsUI options = new OptionsUI ();
		options.setOptionsLayout (layout);
		options.setUtils (utils);
		options.setLanguageHolder (langHolder);
		options.setLanguageChangeMaster (langMaster);
		options.setVersion ("9.9.9");
		options.setUnitClientUtils (unitClientUtils);
		options.setLargeFont (CreateFontsForTests.getLargeFont ());
		options.setMediumFont (CreateFontsForTests.getMediumFont ());
		options.setSmallFont (CreateFontsForTests.getSmallFont ());
		options.setClientConfig (config);
		options.setClientConfigLocation (clientConfigLocation);
		options.setClientConfigMarshaller (marshaller);
		options.setClient (mock (MomClient.class));

		// Display form		
		options.setVisible (true);
		Thread.sleep (5000);
		options.setVisible (false);
	}
}
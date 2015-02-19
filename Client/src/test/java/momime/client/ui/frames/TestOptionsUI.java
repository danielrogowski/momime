package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URLDecoder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import momime.client.ClientTestData;
import momime.client.config.MomImeClientConfigEx;
import momime.client.config.v0_9_6.UnitCombatScale;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.AnimationControllerImpl;
import momime.client.utils.UnitClientUtilsImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the OptionsUI class
 */
public final class TestOptionsUI
{
	/** Class logger */
	private final Log log = LogFactory.getLog (TestOptionsUI.class);
	
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmOptions", "Title")).thenReturn ("Options");
		when (lang.findCategoryEntry ("frmMainMenu", "ShortTitle")).thenReturn ("Implode's Multiplayer Edition - Client");
		when (lang.findCategoryEntry ("frmMainMenu", "Version")).thenReturn ("version VERSION");

		when (lang.findCategoryEntry ("frmOptions", "DebugSection")).thenReturn ("Debug Options");
		when (lang.findCategoryEntry ("frmOptions", "OverlandMapSection")).thenReturn ("Overland Map");
		when (lang.findCategoryEntry ("frmOptions", "CombatMapSection")).thenReturn ("Combat Map");
		when (lang.findCategoryEntry ("frmOptions", "LanguageSection")).thenReturn ("Language");
		
		when (lang.findCategoryEntry ("frmOptions", "SmoothTerrain")).thenReturn ("Smooth edges of terrain");
		when (lang.findCategoryEntry ("frmOptions", "LinearTextureFilter")).thenReturn ("Smooth textures when zooming in on overland map");
		when (lang.findCategoryEntry ("frmOptions", "ShowFogOfWar")).thenReturn ("Darken areas of the map not currently visible");
		when (lang.findCategoryEntry ("frmOptions", "SmoothFogOfWar")).thenReturn ("Smooth edges of visible area");
		when (lang.findCategoryEntry ("frmOptions", "ShowUnitURNs")).thenReturn ("Show Unit, Building and Spell URNs");
		when (lang.findCategoryEntry ("frmOptions", "ShowEdgesOfMap")).thenReturn ("Show edges of map");
		when (lang.findCategoryEntry ("frmOptions", "CombatUnitScale")).thenReturn ("Unit scale (visual only; showing 4x figures does not mean 4x attacks):");
		when (lang.findCategoryEntry ("frmOptions", "ChooseLanguage")).thenReturn ("Choose Language:");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// This is dependant on way too many values to mock them all - so use the real graphics DB
		final GraphicsDatabaseEx gfx = ClientTestData.loadGraphicsDatabase (utils, null);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);

		// Config
		final MomImeClientConfigEx config = new MomImeClientConfigEx ();
		config.setCombatSmoothTerrain (true);
		config.setOverlandSmoothTextures (true);
		config.setDebugShowEdgesOfMap (true);
		config.setUnitCombatScale (UnitCombatScale.DOUBLE_SIZE_UNITS);
		
		// Find the test folder containing empty language files
		String path = getClass ().getResource ("/momime.client.language/Lang 1.Master of Magic Language.xml").toString ();
		path = URLDecoder.decode (path, "UTF-8").substring (6);
		path = path.substring (0, path.length () - 35);
		config.setPathToLanguageXmlFiles (path);
		
		// Decide a temp location to save updates to the config file
		final String clientConfigLocation = File.createTempFile ("MomImeClientConfigEx", ".xml").getAbsolutePath ();
		log.info ("Saving test updates to config file to \"" + clientConfigLocation + "\"");
		
		// Marshaller
		final Marshaller marshaller = JAXBContext.newInstance (MomImeClientConfigEx.class).createMarshaller ();
		marshaller.setProperty (Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		// Unit animations
		final UnitClientUtilsImpl unitClientUtils = new UnitClientUtilsImpl ();
		unitClientUtils.setGraphicsDB (gfx);
		unitClientUtils.setAnim (anim);
		unitClientUtils.setClientConfig (config);
		unitClientUtils.setUtils (utils);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/OptionsUI.xml"));
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

		// Display form		
		options.setVisible (true);
		Thread.sleep (5000);
		options.setVisible (false);
	}
}
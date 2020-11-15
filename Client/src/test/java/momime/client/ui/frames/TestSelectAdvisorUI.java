package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SelectAdvisorScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Language;

/**
 * Tests the SelectAdvisorUI class
 */
public final class TestSelectAdvisorUI extends ClientTestData
{
	/**
	 * Tests the SelectAdvisorUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSelectAdvisorUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final SelectAdvisorScreen selectAdvisorScreenLang = new SelectAdvisorScreen ();
		selectAdvisorScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Select an Advisor"));
		selectAdvisorScreenLang.getSurveyor ().add (createLanguageText (Language.ENGLISH, "F1 - Surveyor"));
		selectAdvisorScreenLang.getCartographer ().add (createLanguageText (Language.ENGLISH, "F2 - Cartographer"));
		selectAdvisorScreenLang.getApprentice ().add (createLanguageText (Language.ENGLISH, "F3 - Apprentice"));
		selectAdvisorScreenLang.getHistorian ().add (createLanguageText (Language.ENGLISH, "F4 - Historian"));
		selectAdvisorScreenLang.getAstrologer ().add (createLanguageText (Language.ENGLISH, "F5 - Astrologer"));
		selectAdvisorScreenLang.getChancellor ().add (createLanguageText (Language.ENGLISH, "F6 - Chancellor"));
		selectAdvisorScreenLang.getTaxCollector ().add (createLanguageText (Language.ENGLISH, "F7 - Tax Collector"));
		selectAdvisorScreenLang.getGrandVizier ().add (createLanguageText (Language.ENGLISH, "F8 - Grand Vizier"));
		selectAdvisorScreenLang.getWizards ().add (createLanguageText (Language.ENGLISH, "F9 - Wizards"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSelectAdvisorScreen ()).thenReturn (selectAdvisorScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/SelectAdvisorUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final SelectAdvisorUI advisors = new SelectAdvisorUI ();
		advisors.setSelectAdvisorLayout (layout);
		advisors.setUtils (utils);
		advisors.setLanguageHolder (langHolder);
		advisors.setLanguageChangeMaster (langMaster);
		advisors.setSmallFont (CreateFontsForTests.getSmallFont ());

		// Display form		
		advisors.setVisible (true);
		Thread.sleep (5000);
		advisors.setVisible (false);
	}
}
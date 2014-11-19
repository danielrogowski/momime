package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import momime.client.ClientTestData;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the SelectAdvisorUI class
 */
public final class TestSelectAdvisorUI
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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Title")).thenReturn ("Select an Advisor");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Surveyor")).thenReturn ("F1 - Surveyor");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Cartographer")).thenReturn ("F2 - Cartographer");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Apprentice")).thenReturn ("F3 - Apprentice");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Historian")).thenReturn ("F4 - Historian");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Astrologer")).thenReturn ("F5 - Astrologer");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Chancellor")).thenReturn ("F6 - Chancellor");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "TaxCollector")).thenReturn ("F7 - Tax Collector");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "GrandVizier")).thenReturn ("F8 - Grand Vizier");
		when (lang.findCategoryEntry ("frmSelectAdvisor", "Wizards")).thenReturn ("F9 - Wizards");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/SelectAdvisorUI.xml"));
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
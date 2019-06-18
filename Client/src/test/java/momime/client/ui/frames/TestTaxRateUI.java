package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.TaxRate;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the TaxRateUI class
 */
public final class TestTaxRateUI extends ClientTestData
{
	/**
	 * Tests the TaxRateUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testTaxRateUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmSetTaxRate", "Title")).thenReturn ("Set Tax Rate per Population");
		when (lang.findCategoryEntry ("frmSetTaxRate", "Entry")).thenReturn ("GOLD_PER_POPULATION gold, UNREST_PERCENTAGE% unrest");

		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock some tax rates
		final List<TaxRate> taxRates = new ArrayList<TaxRate> ();
		for (int n = 0; n <= 6; n++)
		{
			final TaxRate taxRate = new TaxRate ();
			taxRate.setTaxRateID ("TR0" + n);
			taxRate.setDoubleTaxGold (n);
			taxRate.setTaxUnrestPercentage (n * 10);
			
			taxRates.add (taxRate);
		}
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		when (db.getTaxRate ()).thenReturn (taxRates);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Current tax rate
		final MomPersistentPlayerPrivateKnowledge ppk = new MomPersistentPlayerPrivateKnowledge ();
		ppk.setTaxRateID ("TR02");
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (ppk);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/SelectAdvisorUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final TaxRateUI tax = new TaxRateUI ();
		tax.setSelectAdvisorLayout (layout);
		tax.setUtils (utils);
		tax.setLanguageHolder (langHolder);
		tax.setLanguageChangeMaster (langMaster);
		tax.setClient (client);
		tax.setSmallFont (CreateFontsForTests.getSmallFont ());
		tax.setTextUtils (new TextUtilsImpl ());

		// Display form		
		tax.setVisible (true);
		Thread.sleep (5000);
		tax.setVisible (false);
	}
}
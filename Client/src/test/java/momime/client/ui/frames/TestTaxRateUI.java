package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.TaxRateScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.TaxRate;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;

/**
 * Tests the TaxRateUI class
 */
@ExtendWith(MockitoExtension.class)
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
		final TaxRateScreen taxRateScreenLang = new TaxRateScreen ();
		taxRateScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Set Tax Rate per Population"));
		taxRateScreenLang.getEntry ().add (createLanguageText (Language.ENGLISH, "GOLD_PER_POPULATION gold, UNREST_PERCENTAGE% unrest"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getTaxRateScreen ()).thenReturn (taxRateScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

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
		
		final CommonDatabase db = mock (CommonDatabase.class);
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
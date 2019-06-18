package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import momime.client.ClientTestData;
import momime.client.calculations.damage.DamageCalculationText;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.fonts.CreateFontsForTests;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

/**
 * Tests the DamageCalculationsUI class
 */
public final class TestDamageCalculationsUI extends ClientTestData
{
	/**
	 * Tests the DamageCalculationsUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDamageCalculationsUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("CombatDamage", "Title")).thenReturn ("Damage calculations");
		when (lang.findCategoryEntry ("frmMessageBox", "OK")).thenReturn ("OK");
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/DamageCalculationsUI.xml"));
		layout.buildMaps ();

		// Set up form
		final DamageCalculationsUI box = new DamageCalculationsUI ();
		box.setDamageCalculationsLayout (layout);
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		box.setTinyFont (CreateFontsForTests.getTinyFont ());
		
		// Set up some dummy messages
		for (int n = 1; n <= 230; n++)
		{
			final int n2 = n;
			final DamageCalculationText breakdown = new DamageCalculationText ()
			{
				@Override
				public final void preProcess () throws IOException
				{
				}

				@Override
				public final String getText ()
				{
					return "Damage calculation breakdown line " + n2;
				}
			};
			
			box.addBreakdown (breakdown);
		}
		
		// Display form		
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}
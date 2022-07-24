package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.calculations.damage.DamageCalculationText;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.CombatDamage;
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.Language;

/**
 * Tests the DamageCalculationsUI class
 */
@ExtendWith(MockitoExtension.class)
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
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final CombatDamage combatDamageLang = new CombatDamage ();
		combatDamageLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Damage calculations"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getCombatDamage ()).thenReturn (combatDamageLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
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
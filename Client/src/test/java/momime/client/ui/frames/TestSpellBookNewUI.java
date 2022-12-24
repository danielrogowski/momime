package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.SpellBookScreen;
import momime.client.utils.SpellClientUtilsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

/**
 * Tests the SpellBookNewUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellBookNewUI extends ClientTestData
{
	/**
	 * Tests the SpellBookNewUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSpellBookNewUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Mock entries from the language XML
		final SpellBookScreen spellBookScreenLang = new SpellBookScreen ();
		spellBookScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Spells"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSpellBookScreen ()).thenReturn (spellBookScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock spells
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Mock client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Set up form
		final CombatUI combatUI = new CombatUI ();
		
		final SpellClientUtilsImpl spellClientUtils = new SpellClientUtilsImpl ();
		spellClientUtils.setCombatUI (combatUI);
		spellClientUtils.setClient (client);
		spellClientUtils.setSpellUtils (spellUtils);
		
		final SpellBookNewUI book = new SpellBookNewUI ();
		book.setUtils (utils);
		book.setLanguageHolder (langHolder);
		book.setLanguageChangeMaster (langMaster);
		book.setSpellClientUtils (spellClientUtils);
		book.setCastType (SpellCastType.OVERLAND);

		// Display form		
		book.setVisible (true);
		Thread.sleep (50000);
		book.setVisible (false);
	}	
}
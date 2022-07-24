package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import momime.client.languages.database.Simple;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemType;
import momime.common.database.Language;
import momime.common.database.Spell;
import momime.common.messages.NumberedHeroItem;

/**
 * Tests the HeroItemInfoUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestHeroItemInfoUI extends ClientTestData
{
	/**
	 * Tests the HeroItemInfoUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHeroItemInfoUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from DB
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell spell = new Spell ();
		spell.getSpellName ().add (createLanguageText (Language.ENGLISH, "Lightning Bolt"));
		when (db.findSpell ("SP001", "HeroItemInfoUI")).thenReturn (spell);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		final HeroItemType itemTypeGfx = new HeroItemType ();
		for (int n = 1; n <= 9; n++)
			itemTypeGfx.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/shield-0" + n + ".png");
		
		when (db.findHeroItemType ("IT01", "HeroItemInfoUI")).thenReturn (itemTypeGfx);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getClose ().add (createLanguageText (Language.ENGLISH, "Close"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		
		for (int n = 1; n <= 3; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.getHeroItemBonusDescription ().add (createLanguageText (Language.ENGLISH, "Bonus #" + n));
			
			when (db.findHeroItemBonus ("IB0" + n, "HeroItemInfoUI")).thenReturn (bonus);
		}
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// The item to display
		final NumberedHeroItem item = new NumberedHeroItem ();
		item.setHeroItemTypeID ("IT01");
		item.setHeroItemImageNumber (2);
		item.setHeroItemName ("Shield of Testing");
		item.setSpellChargeCount (4);
		item.setSpellID ("SP001");
		
		for (final String bonusID : new String [] {"IB01", "IB02", "IB03", CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES})
			item.getHeroItemChosenBonus ().add (bonusID);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemInfoUI.xml"));
		layout.buildMaps ();
	
		// Set up form
		final HeroItemInfoUI itemInfo = new HeroItemInfoUI ();
		itemInfo.setHeroItemInfoLayout (layout);
		itemInfo.setUtils (utils);
		itemInfo.setLanguageHolder (langHolder);
		itemInfo.setLanguageChangeMaster (langMaster);
		itemInfo.setClient (client);
		itemInfo.setSmallFont (CreateFontsForTests.getSmallFont ());
		itemInfo.setMediumFont (CreateFontsForTests.getMediumFont ());
		itemInfo.setItem (item);
		
		// Display form		
		itemInfo.setVisible (true);
		Thread.sleep (5000);
		itemInfo.setVisible (false);
	}	
}
package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.messages.NumberedHeroItem;

/**
 * Tests the HeroItemInfoUI class
 */
public final class TestHeroItemInfoUI
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
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHeroItemInfo", "Close")).thenReturn ("Close");
		
		for (int n = 1; n <= 3; n++)
			when (lang.findHeroItemBonusDescription ("IB0" + n)).thenReturn ("Bonus #" + n);
		
		final SpellLang spell = new SpellLang ();
		spell.setSpellName ("Lightning Bolt");
		when (lang.findSpell ("SP001")).thenReturn (spell);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock graphics
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final HeroItemTypeGfx itemTypeGfx = new HeroItemTypeGfx ();
		for (int n = 1; n <= 9; n++)
			itemTypeGfx.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/shield-0" + n + ".png");
		
		when (gfx.findHeroItemType ("IT01", "HeroItemInfoUI")).thenReturn (itemTypeGfx);
		
		// The item to display
		final NumberedHeroItem item = new NumberedHeroItem ();
		item.setHeroItemTypeID ("IT01");
		item.setHeroItemImageNumber (2);
		item.setHeroItemName ("Shield of Testing");
		item.setSpellChargeCount (4);
		item.setSpellID ("SP001");
		
		for (final String bonusID : new String [] {"IB01", "IB02", "IB03", CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES})
		{
			final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
			bonus.setHeroItemBonusID (bonusID);
			item.getHeroItemChosenBonus ().add (bonus);
		}

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/HeroItemInfoUI.xml"));
		layout.buildMaps ();
	
		// Set up form
		final HeroItemInfoUI itemInfo = new HeroItemInfoUI ();
		itemInfo.setHeroItemInfoLayout (layout);
		itemInfo.setUtils (utils);
		itemInfo.setLanguageHolder (langHolder);
		itemInfo.setLanguageChangeMaster (langMaster);
		itemInfo.setGraphicsDB (gfx);
		itemInfo.setSmallFont (CreateFontsForTests.getSmallFont ());
		itemInfo.setMediumFont (CreateFontsForTests.getMediumFont ());
		itemInfo.setItem (item);
		
		// Display form		
		itemInfo.setVisible (true);
		Thread.sleep (5000);
		itemInfo.setVisible (false);
	}	
}
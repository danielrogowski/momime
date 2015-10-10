package momime.client.ui.frames;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemTypeGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.common.database.HeroItemType;
import momime.common.database.Spell;

/**
 * Tests the CreateArtifactUI class
 */
public final class TestCreateArtifactUI
{
	/**
	 * Tests the CreateArtifactUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCreateArtifactUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock entries from the language XML
		final SpellLang spellLang = new SpellLang ();
		spellLang.setSpellName ("Create Artifact");
		
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		int itemTypeNumber = 0;
		for (final String itemTypeName : new String [] {"Sword", "Mace", "Axe", "Bow", "Staff", "Wand", "Misc", "Shield", "Chain", "Plate"})
		{
			itemTypeNumber++;
			when (lang.findHeroItemTypeDescription ("IT" + ((itemTypeNumber < 10) ? "0" : "") + itemTypeNumber)).thenReturn (itemTypeName);
		}
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock graphics
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);

		itemTypeNumber = 0;
		for (final String itemImagePrefix : new String [] {"sword", "mace", "axe", "bow", "staff", "wand", "misc", "shield", "chain", "plate"})
		{
			itemTypeNumber++;
			
			final HeroItemTypeGfx itemTypeGfx = new HeroItemTypeGfx ();
			for (int i = 1; i <= 7; i++)		// There's at least 7 images for every item type.. some have more.. but that doesn't matter for a test
				itemTypeGfx.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/" + itemImagePrefix + "-0" + i + ".png");
			
			when (gfx.findHeroItemType ("IT" + ((itemTypeNumber < 10) ? "0" : "") + itemTypeNumber, "selectItemType")).thenReturn (itemTypeGfx);
		}
		
		// Mock database
		final List<HeroItemType> itemTypes = new ArrayList<HeroItemType> ();
		for (int n = 1; n <= 10; n++)
		{
			final HeroItemType itemType = new HeroItemType ();
			itemType.setHeroItemTypeID ("IT" + ((n < 10) ? "0" : "") + n);
			itemType.setBaseCraftingCost (n * 50);
			
			itemTypes.add (itemType);
		}
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		doReturn (itemTypes).when (db).getHeroItemType ();
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);

		// The spell being cast
		final Spell spellDef = new Spell ();
		spellDef.setSpellID ("SP001");
		spellDef.setHeroItemBonusMaximumCraftingCost (0);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CreateArtifactUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final CreateArtifactUI createArtifact = new CreateArtifactUI ();
		createArtifact.setCreateArtifactLayout (layout);
		createArtifact.setUtils (utils);
		createArtifact.setLanguageHolder (langHolder);
		createArtifact.setLanguageChangeMaster (langMaster);
		createArtifact.setClient (client);
		createArtifact.setGraphicsDB (gfx);
		createArtifact.setSmallFont (CreateFontsForTests.getSmallFont ());
		createArtifact.setSpell (spellDef);

		// Display form		
		createArtifact.setVisible (true);
		Thread.sleep (50000);
		createArtifact.setVisible (false);
	}
}
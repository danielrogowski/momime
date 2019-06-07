package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
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
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.SpellLang;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.HeroItemClientUtils;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemType;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.utils.SpellUtils;

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
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmCreateArtifact", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("frmCreateArtifact", "Cancel")).thenReturn ("Cancel");
		
		final SpellLang spellLang = new SpellLang ();
		spellLang.setSpellName ("Create Artifact");
		
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		final ProductionTypeLang manaLang = new ProductionTypeLang ();
		manaLang.setProductionTypeSuffix ("MP");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (manaLang);
		
		int itemTypeNumber = 0;
		for (final String itemTypeName : new String [] {"Sword", "Mace", "Axe", "Bow", "Staff", "Wand", "Misc", "Shield", "Chain", "Plate"})
		{
			itemTypeNumber++;
			when (lang.findHeroItemTypeDescription ("IT" + ((itemTypeNumber < 10) ? "0" : "") + itemTypeNumber)).thenReturn (itemTypeName);
		}

		for (int n = 1; n <= 37; n++)
		{
			final String bonusID = "IB" + ((n < 10) ? "0" : "") + n;
			when (lang.findHeroItemBonusDescription (bonusID)).thenReturn ("Name of " + bonusID);
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
			
			// Which bonuses does this item type allow
			for (int m = 1; m <= 10; m++)
			{
				final HeroItemTypeAllowedBonus bonus = new HeroItemTypeAllowedBonus ();
				int bonusID = ((n-1) * 3) + m;		// So they are staggered a bit... item type 1 has bonuses 1..10; item type 2 has bonuses 4..14 and so on
				bonus.setHeroItemBonusID ("IB" + ((bonusID < 10) ? "0" : "") + bonusID);
				itemType.getHeroItemTypeAllowedBonus ().add (bonus);
			}
		}
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		doReturn (itemTypes).when (db).getHeroItemType ();
		
		for (int n = 1; n <= 37; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setHeroItemBonusID ("IB" + ((n < 10) ? "0" : "") + n);
			bonus.setCraftingCostMultiplierApplies ((n % 2) == 0);
			when (db.findHeroItemBonus (eq (bonus.getHeroItemBonusID ()), anyString ())).thenReturn (bonus);
		}

		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails player = new PlayerPublicDetails (null, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (eq (players), eq (pd.getPlayerID ()), anyString ())).thenReturn (player);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID ());
		
		// Session description
		final UnitSetting unitSetting = new UnitSetting ();
		unitSetting.setMaxHeroItemBonuses (4);
		
		final SpellSetting spellSetting = new SpellSetting ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setUnitSetting (unitSetting);
		sd.setSpellSetting (spellSetting);
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// We can get all bonuses
		final HeroItemCalculations heroItemCalculations = mock (HeroItemCalculations.class);
		when (heroItemCalculations.haveRequiredBooksForBonus (anyString (), eq (pub.getPick ()), eq (db))).thenReturn (true);
		when (heroItemCalculations.calculateCraftingCost (any (HeroItem.class), eq (db))).thenReturn (9999);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getReducedOverlandCastingCost (any (Spell.class), any (HeroItem.class), eq (pub.getPick ()), eq (spellSetting), eq (db))).thenReturn (9999);

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
		createArtifact.setMultiplayerSessionUtils (multiplayerSessionUtils);
		createArtifact.setHeroItemCalculations (heroItemCalculations);
		createArtifact.setHeroItemClientUtils (mock (HeroItemClientUtils.class));
		createArtifact.setSpellUtils (spellUtils);
		createArtifact.setGraphicsDB (gfx);
		createArtifact.setSmallFont (CreateFontsForTests.getSmallFont ());
		createArtifact.setLargeFont (CreateFontsForTests.getLargeFont ());
		createArtifact.setSpell (spellDef);
		createArtifact.setTextUtils (new TextUtilsImpl ());

		// Display form		
		createArtifact.setVisible (true);
		Thread.sleep (5000);
		createArtifact.setVisible (false);
	}
}
package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.MapFeatureGfx;
import momime.client.graphics.database.PickGfx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MapFeatureLang;
import momime.client.language.database.PickLang;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.SpellLang;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.PickAndQuantity;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitAddBumpTypeID;
import momime.common.messages.servertoclient.TreasureRewardMessage;
import momime.common.messages.servertoclient.TreasureRewardPrisoner;
import momime.common.utils.UnitUtils;

/**
 * Tests the TreasureUI class
 */
public final class TestTreasureUI extends ClientTestData
{
	/**
	 * Tests the TreasureUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testTreasureUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmTreasure", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("frmTreasure", "Title")).thenReturn ("Treasure");
		when (lang.findCategoryEntry ("frmTreasure", "Text")).thenReturn ("Inside the LOCATION_DESCRIPTION you find:");
		when (lang.findCategoryEntry ("frmTreasure", "Nothing")).thenReturn ("Absolutely nothing!");
		when (lang.findCategoryEntry ("frmTreasure", "Spell")).thenReturn ("SPELL_NAME spell");
		when (lang.findCategoryEntry ("frmTreasure", "Retort")).thenReturn ("Retort of PICK_NAME_SINGULAR");
		when (lang.findCategoryEntry ("frmTreasure", "PrisonerC")).thenReturn ("A_UNIT_NAME");
		when (lang.findCategoryEntry ("frmTreasure", "PrisonerB")).thenReturn ("A_UNIT_NAME, who would not fit here so was moved to an adjacent tile");
		when (lang.findCategoryEntry ("frmTreasure", "PrisonerN")).thenReturn ("A_UNIT_NAME, who would not fit anywhere so escaped");
		
		final MapFeatureLang mapFeatureLang = new MapFeatureLang ();
		mapFeatureLang.setMapFeatureDescription ("Mysterious Cave");
		when (lang.findMapFeature ("MF01")).thenReturn (mapFeatureLang);
		
		final ProductionTypeLang productionTypeLang = new ProductionTypeLang ();
		productionTypeLang.setProductionTypeDescription ("Gold");
		when (lang.findProductionType ("PT01")).thenReturn (productionTypeLang);
		
		final SpellLang spellLang = new SpellLang ();
		spellLang.setSpellName ("Great Wasting");
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		final PickLang retortLang = new PickLang ();
		retortLang.setPickDescriptionSingular ("Runemaster");
		when (lang.findPick ("RT01")).thenReturn (retortLang);
		
		final PickLang singleBookLang = new PickLang ();
		singleBookLang.setPickDescriptionSingular ("Chaos book");
		when (lang.findPick ("MB01")).thenReturn (singleBookLang);
		
		final PickLang multiBooksLang = new PickLang ();
		multiBooksLang.setPickDescriptionPlural ("Sorcery books");
		when (lang.findPick ("MB02")).thenReturn (multiBooksLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final MapFeatureGfx mapFeatureGfx = new MapFeatureGfx ();
		mapFeatureGfx.setMonsterFoundImageFile ("/momime.client.graphics/overland/mapFeatures/cave-scouting.png");
		when (gfx.findMapFeature ("MF01", "TreasureUI")).thenReturn (mapFeatureGfx);
		
		// Units
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		for (int n = 1; n <= 2; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			when (unitUtils.findUnitURN (n, fow.getUnit ())).thenReturn (unit);
		}
		
		// Unit names
		final UnitStatsLanguageVariableReplacer unitStatsReplacer = mock (UnitStatsLanguageVariableReplacer.class);
		when (unitStatsReplacer.replaceVariables ("A_UNIT_NAME;")).thenReturn ("Brax the Dwarf");
		when (unitStatsReplacer.replaceVariables ("A_UNIT_NAME, who would not fit here so was moved to an adjacent tile;")).thenReturn ("Zaldron the Sage, who would not fit here so was moved to an adjacent tile");
		
		// Set up some sample treasure
		final TreasureRewardMessage reward = new TreasureRewardMessage ();
		reward.setMapFeatureID ("MF01");
		reward.getSpellID ().add ("SP001");
		
		final NumberedHeroItem item = new NumberedHeroItem ();
		item.setHeroItemName ("Sword of blah");
		reward.getHeroItem ().add (item);
		
		final ProductionTypeAndUndoubledValue resource = new ProductionTypeAndUndoubledValue ();
		resource.setProductionTypeID ("PT01");
		resource.setUndoubledProductionValue (1234);
		reward.getResource ().add (resource);
		
		int n = 0;
		for (final UnitAddBumpTypeID bump : UnitAddBumpTypeID.values ())
		{
			n++;
			final TreasureRewardPrisoner prisoner = new TreasureRewardPrisoner ();
			prisoner.setPrisonerUnitURN (n);
			prisoner.setUnitAddBumpType (bump);
			reward.getPrisoner ().add (prisoner);
		}
		
		final PickAndQuantity retort = new PickAndQuantity ();
		retort.setPickID ("RT01");
		retort.setQuantity (1);
		reward.getPick ().add (retort);

		final PickAndQuantity singleBook = new PickAndQuantity ();
		singleBook.setPickID ("MB01");
		singleBook.setQuantity (1);
		reward.getPick ().add (singleBook);
		
		final PickAndQuantity multiBooks = new PickAndQuantity ();
		multiBooks.setPickID ("MB02");
		multiBooks.setQuantity (2);
		reward.getPick ().add (multiBooks);
		
		// Differentiate books and retorts
		for (final PickAndQuantity pick : reward.getPick ())
		{
			final PickGfx pickGfx = new PickGfx ();
			if (pick.getPickID ().startsWith ("MB"))
				pickGfx.getBookImageFile ().add (null);
			
			when (gfx.findPick (pick.getPickID (), "TreasureUI")).thenReturn (pickGfx);
		}
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/TreasureUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final TreasureUI box = new TreasureUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
		box.setGraphicsDB (gfx);
		box.setClient (client);
		box.setUnitUtils (unitUtils);
		box.setUnitStatsReplacer (unitStatsReplacer);
		box.setSmallFont (CreateFontsForTests.getSmallFont ());
		box.setTreasureLayout (layout);
		box.setTreasureReward (reward);
		box.setTextUtils (new TextUtilsImpl ());
		
		// Display form		
		box.setModal (false);
		box.setVisible (true);
		Thread.sleep (5000);
		box.setVisible (false);
	}
}
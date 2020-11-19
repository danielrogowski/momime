package momime.client.ui.dialogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.languages.database.Simple;
import momime.client.languages.database.TreasureScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.Language;
import momime.common.database.MapFeatureEx;
import momime.common.database.Pick;
import momime.common.database.PickAndQuantity;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
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
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final Spell spellDef = new Spell ();
		spellDef.getSpellName ().add (createLanguageText (Language.ENGLISH, "Great Wasting"));
		when (db.findSpell ("SP001", "TreasureUI")).thenReturn (spellDef);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));

		final TreasureScreen treasureScreenLang = new TreasureScreen ();
		treasureScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Treasure"));
		treasureScreenLang.getText ().add (createLanguageText (Language.ENGLISH, "Inside the LOCATION_DESCRIPTION you find:"));
		treasureScreenLang.getNothing ().add (createLanguageText (Language.ENGLISH, "Absolutely nothing!"));
		treasureScreenLang.getSpell ().add (createLanguageText (Language.ENGLISH, "SPELL_NAME spell"));
		treasureScreenLang.getRetort ().add (createLanguageText (Language.ENGLISH, "Retort of PICK_NAME_SINGULAR"));
		treasureScreenLang.getPrisoner ().add (createLanguageText (Language.ENGLISH, "A_UNIT_NAME"));
		treasureScreenLang.getPrisonerBumped ().add (createLanguageText (Language.ENGLISH, "A_UNIT_NAME, who would not fit here so was moved to an adjacent tile"));
		treasureScreenLang.getPrisonerEscaped ().add (createLanguageText (Language.ENGLISH, "A_UNIT_NAME, who would not fit anywhere so escaped"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getTreasureScreen ()).thenReturn (treasureScreenLang);
		
		final MapFeatureEx mapFeatureLang = new MapFeatureEx ();
		mapFeatureLang.getMapFeatureDescription ().add (createLanguageText (Language.ENGLISH, "Mysterious Cave"));
		mapFeatureLang.setMonsterFoundImageFile ("/momime.client.graphics/overland/mapFeatures/cave-scouting.png");
		when (db.findMapFeature ("MF01", "TreasureUI")).thenReturn (mapFeatureLang);		
		
		final ProductionTypeEx productionTypeLang = new ProductionTypeEx ();
		productionTypeLang.getProductionTypeDescription ().add (createLanguageText (Language.ENGLISH, "Gold"));
		when (db.findProductionType ("PT01", "TreasureUI")).thenReturn (productionTypeLang);
		
		final Pick retortLang = new Pick ();
		retortLang.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Runemaster"));
		when (db.findPick ("RT01", "TreasureUI")).thenReturn (retortLang);
		
		final Pick singleBookLang = new Pick ();
		singleBookLang.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Chaos book"));
		singleBookLang.getBookImageFile ().add (null);
		when (db.findPick ("MB01", "TreasureUI")).thenReturn (singleBookLang);
		
		final Pick multiBooksLang = new Pick ();
		multiBooksLang.getPickDescriptionPlural ().add (createLanguageText (Language.ENGLISH, "Sorcery books"));
		multiBooksLang.getBookImageFile ().add (null);
		when (db.findPick ("MB02", "TreasureUI")).thenReturn (multiBooksLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);

		// Units
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		final MomClient client = mock (MomClient.class);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		when (client.getClientDB ()).thenReturn (db);
		
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
		
		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.dialogs/TreasureUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final TreasureUI box = new TreasureUI ();
		box.setUtils (utils);
		box.setLanguageHolder (langHolder);
		box.setLanguageChangeMaster (langMaster);
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
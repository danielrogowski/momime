package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
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
import momime.client.utils.HeroItemClientUtils;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemType;
import momime.common.database.Language;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.database.UnitSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.SpellUtils;

/**
 * Tests the CreateArtifactUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestCreateArtifactUI extends ClientTestData
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

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "updateCraftingCost")).thenReturn (mana);
		
		final List<HeroItemType> itemTypes = new ArrayList<HeroItemType> ();
		int itemTypeNumber = 0;
		for (final String itemTypeName : new String [] {"Sword", "Mace", "Axe", "Bow", "Staff", "Wand", "Misc", "Shield", "Chain", "Plate"})
		{
			itemTypeNumber++;
			
			final HeroItemType itemType = new HeroItemType ();
			itemType.setHeroItemTypeID ("IT" + ((itemTypeNumber < 10) ? "0" : "") + itemTypeNumber);
			itemType.setBaseCraftingCost (itemTypeNumber * 50);
			itemType.getHeroItemTypeDescription ().add (createLanguageText (Language.ENGLISH, itemTypeName));
			itemTypes.add (itemType);
			
			// There's at least 7 images for every item type.. some have more.. but that doesn't matter for a test
			for (int i = 1; i <= 7; i++)
				itemType.getHeroItemTypeImageFile ().add ("/momime.client.graphics/heroItems/items/" + itemTypeName.toLowerCase () + "-0" + i + ".png");
			
			when (db.findHeroItemType (eq (itemType.getHeroItemTypeID ()), anyString ())).thenReturn (itemType);
			
			// Which bonuses does this item type allow
			for (int m = 1; m <= 10; m++)
			{
				int bonusID = ((itemTypeNumber-1) * 3) + m;		// So they are staggered a bit... item type 1 has bonuses 1..10; item type 2 has bonuses 4..14 and so on
				itemType.getHeroItemTypeAllowedBonus ().add ("IB" + ((bonusID < 10) ? "0" : "") + bonusID);
			}
		}
		doReturn (itemTypes).when (db).getHeroItemType ();
		
		for (int n = 1; n <= 10; n++)
		{
			final HeroItemBonus bonus = new HeroItemBonus ();
			bonus.setHeroItemBonusID ("IB" + ((n < 10) ? "0" : "") + n);
			bonus.setCraftingCostMultiplierApplies ((n % 2) == 0);
			bonus.getHeroItemBonusDescription ().add (createLanguageText (Language.ENGLISH, "Name of " + bonus.getHeroItemBonusID ()));
			
			when (db.findHeroItemBonus (eq (bonus.getHeroItemBonusID ()), anyString ())).thenReturn (bonus);
		}
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		simpleLang.getCancel ().add (createLanguageText (Language.ENGLISH, "Cancel"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final PlayerPublicDetails player = new PlayerPublicDetails (null, null, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		
		// Player's memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);
				
		// Wizard
		final KnownWizardDetails ourWizard = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (eq (mem.getWizardDetails ()), eq (pd.getPlayerID ()), anyString ())).thenReturn (ourWizard);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPlayerID ()).thenReturn (pd.getPlayerID ());
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
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
		when (heroItemCalculations.haveRequiredBooksForBonus (anyString (), eq (ourWizard.getPick ()), eq (db))).thenReturn (true);
		when (heroItemCalculations.calculateCraftingCost (any (HeroItem.class), eq (db))).thenReturn (9999);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.getReducedOverlandCastingCost (any (Spell.class), any (HeroItem.class), isNull (), eq (ourWizard.getPick ()), eq (mem.getMaintainedSpell ()),
			eq (spellSetting), eq (db))).thenReturn (9999);

		// The spell being cast
		final Spell spellDef = new Spell ();
		spellDef.getSpellName ().add (createLanguageText (Language.ENGLISH, "Create Artifact"));
		spellDef.setSpellID ("SP001");
		spellDef.setHeroItemBonusMaximumCraftingCost (0);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/CreateArtifactUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final CreateArtifactUI createArtifact = new CreateArtifactUI ();
		createArtifact.setCreateArtifactLayout (layout);
		createArtifact.setUtils (utils);
		createArtifact.setLanguageHolder (langHolder);
		createArtifact.setLanguageChangeMaster (langMaster);
		createArtifact.setClient (client);
		createArtifact.setHeroItemCalculations (heroItemCalculations);
		createArtifact.setHeroItemClientUtils (mock (HeroItemClientUtils.class));
		createArtifact.setSpellUtils (spellUtils);
		createArtifact.setKnownWizardUtils (knownWizardUtils);
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
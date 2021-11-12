package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.MagicSlidersScreen;
import momime.client.languages.database.Simple;
import momime.client.ui.components.MagicSlider;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.SkillCalculationsImpl;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Language;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MagicPowerDistribution;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.QueuedSpell;
import momime.common.messages.SpellResearchStatus;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtilsImpl;
import momime.common.utils.SpellUtils;

/**
 * Tests the MagicSlidersUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestMagicSlidersUI extends ClientTestData
{
	/**
	 * Tests the MagicSlidersUI form
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testMagicSlidersUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final ProductionTypeEx manaProduction = new ProductionTypeEx ();
		manaProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "updateProductionLabels")).thenReturn (manaProduction);
		
		final ProductionTypeEx researchProduction = new ProductionTypeEx ();
		researchProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "RP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "updateProductionLabels")).thenReturn (researchProduction);
		
		final ProductionTypeEx skillProduction = new ProductionTypeEx ();
		skillProduction.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "SP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, "updateProductionLabels")).thenReturn (skillProduction);
		
		// Mock entries from the language XML
		final Simple simpleLang = new Simple ();
		simpleLang.getOk ().add (createLanguageText (Language.ENGLISH, "OK"));
		
		final MagicSlidersScreen magicSlidersScreenLang = new MagicSlidersScreen ();
		magicSlidersScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Magic"));
		magicSlidersScreenLang.getPowerBase ().add (createLanguageText (Language.ENGLISH, "Power Base: AMOUNT_PER_TURN"));
		magicSlidersScreenLang.getManaTitle ().add (createLanguageText (Language.ENGLISH, "Mana"));
		magicSlidersScreenLang.getResearchTitle ().add (createLanguageText (Language.ENGLISH, "Research"));
		magicSlidersScreenLang.getSkillTitle ().add (createLanguageText (Language.ENGLISH, "Skill"));
		magicSlidersScreenLang.getManaLabel ().add (createLanguageText (Language.ENGLISH, "Casting"));
		magicSlidersScreenLang.getResearchLabel ().add (createLanguageText (Language.ENGLISH, "Researching"));
		magicSlidersScreenLang.getSkillLabel ().add (createLanguageText (Language.ENGLISH, "Casting Skill"));
		magicSlidersScreenLang.getResearchingNothing ().add (createLanguageText (Language.ENGLISH, "None"));
		magicSlidersScreenLang.getOverlandEnchantments ().add (createLanguageText (Language.ENGLISH, "Overland Enchantments"));
		magicSlidersScreenLang.getAlchemy ().add (createLanguageText (Language.ENGLISH, "Alchemy"));
		magicSlidersScreenLang.getApply ().add (createLanguageText (Language.ENGLISH, "Apply"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getSimple ()).thenReturn (simpleLang);
		when (lang.getMagicSlidersScreen ()).thenReturn (magicSlidersScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setHuman (true);
		pd.setPlayerID (3);
		
		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		
		final PlayerPublicDetails player = new PlayerPublicDetails (pd, pub, null);
		
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		players.add (player);
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPlayerID ()).thenReturn (3);
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "updatePerTurnLabels")).thenReturn (player);
		
		// Spell
		final Spell spell1 = new Spell ();
		spell1.getSpellName ().add (createLanguageText (Language.ENGLISH, "Great Unsummoning"));		// This was the longest spell name I could find!
		spell1.setResearchCost (50);
		
		final Spell spell2 = new Spell ();
		spell2.getSpellName ().add (createLanguageText (Language.ENGLISH, "Spell Binding"));
		
		when (db.findSpell ("SP001", "updateProductionLabels (r)")).thenReturn (spell1);
		when (db.findSpell ("SP002", "updateProductionLabels (c)")).thenReturn (spell2);
		when (client.getClientDB ()).thenReturn (db);
		
		// Initial slider values
		final MagicPowerDistribution dist = new MagicPowerDistribution ();
		dist.setManaRatio		(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setResearchRatio	(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setSkillRatio			(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		priv.setMagicPowerDistribution (dist);
		priv.setSpellIDBeingResearched ("SP001");
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);

		// Give us 100 power base, and some mana and skill stored
		final MomResourceValue powerBase = new MomResourceValue ();
		powerBase.setAmountPerTurn (100);
		powerBase.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
		priv.getResourceValue ().add (powerBase);

		final MomResourceValue manaStored = new MomResourceValue ();
		manaStored.setAmountStored (125);
		manaStored.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		priv.getResourceValue ().add (manaStored);

		final MomResourceValue skill = new MomResourceValue ();
		skill.setAmountStored (203);
		skill.setProductionTypeID (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		priv.getResourceValue ().add (skill);
		
		// Spell settings
		final SpellSetting spellSettings = new SpellSetting (); 
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Lets say we have Archmage, giving +50% bonus to magic power spent on skill improvement
		final PlayerPickUtils pickUtils = mock (PlayerPickUtils.class);
		when (pickUtils.totalProductionBonus (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, null, pub.getPick (), db)).thenReturn (50);
		
		// Spell research
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setRemainingResearchCost (40);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// Spell we're casting
		final QueuedSpell queued = new QueuedSpell ();
		queued.setQueuedSpellID ("SP002");
		
		priv.getQueuedSpell ().add (queued);
		priv.setManaSpentOnCastingCurrentSpell (70);
		
		when (spellUtils.getReducedOverlandCastingCost (spell2, null, null, pub.getPick (), spellSettings, db)).thenReturn (80);
		
		// Component factory
		final UIComponentFactory uiComponentFactory = mock (UIComponentFactory.class);
		when (uiComponentFactory.createMagicSlider ()).thenAnswer ((i) ->
		{
			final MagicSlider slider = new MagicSlider ();
			slider.setUtils (utils);
			return slider;
		});
		
		// Wizard's Fortress
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class); 
		
		// With the values moving, we need the real calc production values routine in order to demonstrate the UI working properly
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		
		final SkillCalculationsImpl skillCalc = new SkillCalculationsImpl (); 
		
		final ResourceValueUtilsImpl resourceValueUtils = new ResourceValueUtilsImpl ();
		resourceValueUtils.setPlayerPickUtils (pickUtils);
		resourceValueUtils.setSpellCalculations (spellCalc);
		resourceValueUtils.setSkillCalculations (skillCalc);
		resourceValueUtils.setMemoryBuildingUtils (memoryBuildingUtils);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/MagicSlidersUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final MagicSlidersUI sliders = new MagicSlidersUI ();
		sliders.setUtils (utils);
		sliders.setLanguageHolder (langHolder);
		sliders.setLanguageChangeMaster (langMaster);
		sliders.setUiComponentFactory (uiComponentFactory);
		sliders.setClient (client);
		sliders.setResourceValueUtils (resourceValueUtils);
		sliders.setSkillCalculations (skillCalc);
		sliders.setSpellUtils (spellUtils);
		sliders.setMultiplayerSessionUtils (multiplayerSessionUtils);
		sliders.setTextUtils (new TextUtilsImpl ());
		sliders.setLargeFont (CreateFontsForTests.getLargeFont ());
		sliders.setSmallFont (CreateFontsForTests.getSmallFont ());
		sliders.setMagicSlidersLayout (layout);

		// Display form		
		sliders.setVisible (true);
		Thread.sleep (5000);
		sliders.setVisible (false);
	}
}
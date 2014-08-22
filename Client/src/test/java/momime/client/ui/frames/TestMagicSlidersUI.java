package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.ProductionType;
import momime.client.ui.components.MagicSlider;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.MomSkillCalculationsImpl;
import momime.common.calculations.MomSpellCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.newgame.v0_9_5.SpellSettingData;
import momime.common.database.v0_9_5.Spell;
import momime.common.messages.v0_9_5.MagicPowerDistribution;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_5.MomResourceValue;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.SpellResearchStatus;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtilsImpl;
import momime.common.utils.SpellUtils;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the MagicSlidersUI class
 */
public final class TestMagicSlidersUI
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
		
		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);

		when (lang.findCategoryEntry ("frmMagicSliders", "Title")).thenReturn ("Magic");
		when (lang.findCategoryEntry ("frmMagicSliders", "PowerBase")).thenReturn ("Power Base: AMOUNT_PER_TURN");
		when (lang.findCategoryEntry ("frmMagicSliders", "ManaTitle")).thenReturn ("Mana");
		when (lang.findCategoryEntry ("frmMagicSliders", "ResearchTitle")).thenReturn ("Research");
		when (lang.findCategoryEntry ("frmMagicSliders", "SkillTitle")).thenReturn ("Skill");
		when (lang.findCategoryEntry ("frmMagicSliders", "ManaLabel")).thenReturn ("Mana Stored");
		when (lang.findCategoryEntry ("frmMagicSliders", "ResearchLabel")).thenReturn ("Researching");
		when (lang.findCategoryEntry ("frmMagicSliders", "SkillLabel")).thenReturn ("Casting Skill");
		when (lang.findCategoryEntry ("frmMagicSliders", "ResearchingNothing")).thenReturn ("None");
		when (lang.findCategoryEntry ("frmMagicSliders", "OverlandEnchantments")).thenReturn ("Overland Enchantments");
		when (lang.findCategoryEntry ("frmMagicSliders", "Alchemy")).thenReturn ("Alchemy");
		when (lang.findCategoryEntry ("frmMagicSliders", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("frmMagicSliders", "Apply")).thenReturn ("Apply");
		
		final ProductionType manaProduction = new ProductionType ();
		manaProduction.setProductionTypeSuffix ("MP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA)).thenReturn (manaProduction);
		
		final ProductionType researchProduction = new ProductionType ();
		researchProduction.setProductionTypeSuffix ("RP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH)).thenReturn (researchProduction);
		
		final ProductionType skillProduction = new ProductionType ();
		skillProduction.setProductionTypeSuffix ("SP");
		when (lang.findProductionType (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT)).thenReturn (skillProduction);
		
		final momime.client.language.database.v0_9_5.Spell spellLang = new momime.client.language.database.v0_9_5.Spell ();
		spellLang.setSpellName ("Great Unsummoning");		// This was the longest spell name I could find!
		when (lang.findSpell ("SP001")).thenReturn (spellLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);

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
		
		// Mock client database
		final Spell spell = new Spell ();
		spell.setResearchCost (50);
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		when (db.findSpell ("SP001", "updateProductionLabels")).thenReturn (spell);
		when (client.getClientDB ()).thenReturn (db);
		
		// Initial slider values
		final MagicPowerDistribution dist = new MagicPowerDistribution ();
		dist.setManaRatio		(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setResearchRatio	(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setSkillRatio			(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setMagicPowerDistribution (dist);
		priv.setSpellIDBeingResearched ("SP001");
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);

		// Give us 100 power base, and some mana and skill stored
		final MomResourceValue powerBase = new MomResourceValue ();
		powerBase.setAmountPerTurn (100);
		powerBase.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER);
		priv.getResourceValue ().add (powerBase);

		final MomResourceValue manaStored = new MomResourceValue ();
		manaStored.setAmountStored (125);
		manaStored.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		priv.getResourceValue ().add (manaStored);

		final MomResourceValue skill = new MomResourceValue ();
		skill.setAmountStored (203);
		skill.setProductionTypeID (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT);
		priv.getResourceValue ().add (skill);
		
		// Spell settings
		final SpellSettingData spellSettings = new SpellSettingData (); 
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setSpellSetting (spellSettings);
		
		when (client.getSessionDescription ()).thenReturn (sd);
		
		// Lets say we have Archmage, giving +50% bonus to magic power spent on skill improvement
		final PlayerPickUtils pickUtils = mock (PlayerPickUtils.class);
		when (pickUtils.totalProductionBonus (CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, null, pub.getPick (), db)).thenReturn (50);
		
		// Spell research
		final SpellResearchStatus researchStatus = new SpellResearchStatus ();
		researchStatus.setRemainingResearchCost (40);
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.findSpellResearchStatus (priv.getSpellResearchStatus (), "SP001")).thenReturn (researchStatus);
		
		// Component factory
		final UIComponentFactory uiComponentFactory = mock (UIComponentFactory.class);
		when (uiComponentFactory.createMagicSlider ()).thenAnswer (new Answer<MagicSlider> ()
		{
			@Override
			public final MagicSlider answer (final InvocationOnMock invocation) throws Throwable
			{
				final MagicSlider slider = new MagicSlider ();
				slider.setUtils (utils);
				return slider;
			}
		});
		
		// With the values moving, we need the real calc production values routine in order to demonstrate the UI working properly
		final MomSpellCalculations spellCalc = mock (MomSpellCalculations.class);
		
		final MomSkillCalculationsImpl skillCalc = new MomSkillCalculationsImpl (); 
		
		final ResourceValueUtilsImpl resourceValueUtils = new ResourceValueUtilsImpl ();
		resourceValueUtils.setPlayerPickUtils (pickUtils);
		resourceValueUtils.setSpellCalculations (spellCalc);
		resourceValueUtils.setSkillCalculations (skillCalc);
		
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
		sliders.setTextUtils (new TextUtilsImpl ());
		sliders.setLargeFont (CreateFontsForTests.getLargeFont ());
		sliders.setSmallFont (CreateFontsForTests.getSmallFont ());

		// Display form		
		sliders.setVisible (true);
		Thread.sleep (5000);
	}
}
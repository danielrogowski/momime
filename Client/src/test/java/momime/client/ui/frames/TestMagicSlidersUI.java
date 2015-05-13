package momime.client.ui.frames;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.SpellLang;
import momime.client.ui.components.MagicSlider;
import momime.client.ui.components.UIComponentFactory;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.TextUtilsImpl;
import momime.common.calculations.SkillCalculationsImpl;
import momime.common.calculations.SpellCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MagicPowerDistribution;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.SpellResearchStatus;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtilsImpl;
import momime.common.utils.SpellUtils;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

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
		when (lang.findCategoryEntry ("frmMagicSliders", "ManaLabel")).thenReturn ("Casting");
		when (lang.findCategoryEntry ("frmMagicSliders", "ResearchLabel")).thenReturn ("Researching");
		when (lang.findCategoryEntry ("frmMagicSliders", "SkillLabel")).thenReturn ("Casting Skill");
		when (lang.findCategoryEntry ("frmMagicSliders", "ResearchingNothing")).thenReturn ("None");
		when (lang.findCategoryEntry ("frmMagicSliders", "OverlandEnchantments")).thenReturn ("Overland Enchantments");
		when (lang.findCategoryEntry ("frmMagicSliders", "Alchemy")).thenReturn ("Alchemy");
		when (lang.findCategoryEntry ("frmMagicSliders", "OK")).thenReturn ("OK");
		when (lang.findCategoryEntry ("frmMagicSliders", "Apply")).thenReturn ("Apply");
		
		final ProductionTypeLang manaProduction = new ProductionTypeLang ();
		manaProduction.setProductionTypeSuffix ("MP");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (manaProduction);
		
		final ProductionTypeLang researchProduction = new ProductionTypeLang ();
		researchProduction.setProductionTypeSuffix ("RP");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH)).thenReturn (researchProduction);
		
		final ProductionTypeLang skillProduction = new ProductionTypeLang ();
		skillProduction.setProductionTypeSuffix ("SP");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT)).thenReturn (skillProduction);
		
		final SpellLang spellLang1 = new SpellLang ();
		spellLang1.setSpellName ("Great Unsummoning");		// This was the longest spell name I could find!
		when (lang.findSpell ("SP001")).thenReturn (spellLang1);
		
		final SpellLang spellLang2 = new SpellLang ();
		spellLang2.setSpellName ("Spell Binding");
		when (lang.findSpell ("SP002")).thenReturn (spellLang2);
		
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
		
		// Session utils
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, pd.getPlayerID (), "updatePerTurnLabels")).thenReturn (player);
		
		// Mock client database
		final Spell spell1 = new Spell ();
		spell1.setResearchCost (50);
		
		final Spell spell2 = new Spell ();
		
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
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
		priv.getQueuedSpellID ().add ("SP002");
		priv.setManaSpentOnCastingCurrentSpell (70);
		
		when (spellUtils.getReducedOverlandCastingCost (spell2, pub.getPick (), spellSettings, db)).thenReturn (80);
		
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
		final SpellCalculations spellCalc = mock (SpellCalculations.class);
		
		final SkillCalculationsImpl skillCalc = new SkillCalculationsImpl (); 
		
		final ResourceValueUtilsImpl resourceValueUtils = new ResourceValueUtilsImpl ();
		resourceValueUtils.setPlayerPickUtils (pickUtils);
		resourceValueUtils.setSpellCalculations (spellCalc);
		resourceValueUtils.setSkillCalculations (skillCalc);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/MagicSlidersUI.xml"));
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
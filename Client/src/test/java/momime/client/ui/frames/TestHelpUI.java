package momime.client.ui.frames;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.junit.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.CityViewElementGfx;
import momime.client.graphics.database.CombatAreaEffectGfx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.HeroItemSlotTypeGfx;
import momime.client.graphics.database.PickGfx;
import momime.client.graphics.database.UnitSkillComponentImageGfx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.client.graphics.database.v0_9_7.UnitSkillWeaponGrade;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.CitySpellEffectLang;
import momime.client.language.database.CombatAreaEffectLang;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.PickLang;
import momime.client.language.database.ProductionTypeLang;
import momime.client.language.database.SpellBookSectionLang;
import momime.client.language.database.SpellLang;
import momime.client.language.database.UnitSkillLang;
import momime.client.language.replacer.SpringExpressionReplacerImpl;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlotType;
import momime.common.database.HeroSlotAllowedItemType;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkillComponent;
import momime.common.messages.AvailableUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellUtils;

/**
 * Tests the HelpUI class
 */
public final class TestHelpUI
{
	/**
	 * @return HelpUI form ready to test with
	 * @throws Exception If there is a problem
	 */
	private final HelpUI createHelpUI () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		when (lang.findCategoryEntry ("frmHelp", "Title")).thenReturn ("Help");
		when (lang.findCategoryEntry ("frmHelp", "SpellBookSection")).thenReturn ("Spell book section: SPELL_BOOK_SECTION");
		when (lang.findCategoryEntry ("frmHelp", "SpellBookResearchCostNotOurs")).thenReturn ("Research cost: RESEARCH_TOTAL PRODUCTION_TYPE");
		when (lang.findCategoryEntry ("frmHeroItemInfo", "ItemSlotHelpTextPrefix")).thenReturn ("The following types of hero items can be used in this slot:");
		
		final PickLang retort = new PickLang ();
		retort.setPickDescriptionSingular ("Alchemy");
		retort.setPickHelpText ("Allows the wizard to change gold into mana and mana into gold in a 1 to 1 ratio, instead of loosing � in the exchange process." +
			System.lineSeparator () + System.lineSeparator () +
			"It also gives all units built in cities magic weapons with +1 to hit bonus and the ability to hurt units with Weapon Immunity, the same as if they were built in a city with an Alchemists' Guild." +
			System.lineSeparator () + System.lineSeparator () +
			"The Alchemy retort is the only way that Gnolls, Klackons, Lizardmen or Trolls can build units with magic weapons, since these races cannot build Alchemists' Guilds.");
		when (lang.findPick ("RT01")).thenReturn (retort);
		
		final PickLang book = new PickLang ();
		book.setPickDescriptionSingular ("Life Book");
		book.setPickHelpText ("Life spells focus on healing, protections and inspirational enchantments, and planar travel.  Life magic forcefully opposes the forces of death and mildly resists the forces of chaos.");
		when (lang.findPick ("MB01")).thenReturn (book);
		
		final UnitSkillLang unitSkill = new UnitSkillLang ();
		unitSkill.setUnitSkillDescription ("PFTitle");
		unitSkill.setUnitSkillHelpText ("PFText");
		when (lang.findUnitSkill ("US020")).thenReturn (unitSkill);
		
		final UnitSkillLang unitAttribute = new UnitSkillLang ();
		unitAttribute.setUnitSkillDescription ("MeleeTitle");
		unitAttribute.setUnitSkillHelpText ("Each icon represents one chance that each figure has to strike a blow in a hand to hand attack.  The base chance that each sword icon will score a hit is 30%, although this can be modified by the attacker's + to Hit attribute." +
			System.lineSeparator () + System.lineSeparator () +
			"The unit being attacked can then use its Defence attribute to try to block some of the hits.  The base chance that each shield icon will block a hit is 30%, although this can be modified by the defender's + to Block attribute." +
			System.lineSeparator () + System.lineSeparator () +
			"Melee weapons are available in a number of weapon grades which confer improved stats:" + System.lineSeparator () +
			"#{findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (0, 'HelpText')} Standard weapon" + System.lineSeparator () +
			"#{findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (1, 'HelpText')} Magic weapon" + System.lineSeparator () +
			"#{findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (2, 'HelpText')} Mithril weapon" + System.lineSeparator () +
			"#{findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (3, 'HelpText')} Adamantium weapon" +
			System.lineSeparator () + System.lineSeparator () +
			"The background colour of each icon depicts the source of the attribute:" + System.lineSeparator () +
			"#{findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).BASIC, 'HelpText').UnitSkillComponentImageFile} Basic statistic of the unit" + System.lineSeparator () +
			"#{findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).WEAPON_GRADE, 'HelpText').UnitSkillComponentImageFile} Bonus from an improved weapon grade" + System.lineSeparator () +
			"#{findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).EXPERIENCE, 'HelpText').UnitSkillComponentImageFile} Bonus from experience" + System.lineSeparator () +
			"#{findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).HERO_SKILLS, 'HelpText').UnitSkillComponentImageFile} Bonus from Might hero skill" + System.lineSeparator () +
			"#{findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).COMBAT_AREA_EFFECTS, 'HelpText').UnitSkillComponentImageFile} Bonus from a combat area effect (e.g. node aura or Prayer spell)" +
			System.lineSeparator () + System.lineSeparator () +
			"A darkened icon represents a minus, for example from a curse type spell." +
			System.lineSeparator () + System.lineSeparator () +
			"And some extra text on the end" + System.lineSeparator () +
			"to make the help text long enough" + System.lineSeparator () +
			"to require a scroll bar to appear.");
		when (lang.findUnitSkill ("UA01")).thenReturn (unitAttribute);
		
		final CombatAreaEffectLang cae = new CombatAreaEffectLang ();
		cae.setCombatAreaEffectDescription ("Counter Magic");
		cae.setCombatAreaEffectHelpText ("Creates a standing dispel magic spell over the entire battlefield." +
			System.lineSeparator () + System.lineSeparator () +
			"A spell cast by the enemy wizard or an enemy hero must first overcome the effects of this dispel magic spell (of strength equal to the magic power poured into the counter magic spell) before it can exert its effects." +
			System.lineSeparator () + System.lineSeparator () +
			"Every spell cast by the enemy drains the magic power (strength) of the counter magic spell by 5 mana points and, therefore, lessens its effectiveness against subsequent spells.");
		when (lang.findCombatAreaEffect ("CSE048")).thenReturn (cae);
		
		final CitySpellEffectLang citySpellEffect = new CitySpellEffectLang ();
		citySpellEffect.setCitySpellEffectName ("Chaos Rift");
		citySpellEffect.setCitySpellEffectHelpText ("Opens a great magical vortex over an enemy city." +
			System.lineSeparator () + System.lineSeparator () +
			"Each turn, units inside the city sustain five strength 8 Lightning Bolt attacks, and there is a 5% chance of a building being destroyed.");
		when (lang.findCitySpellEffect ("SE110")).thenReturn (citySpellEffect);
		
		final SpellLang spell = new SpellLang ();
		spell.setSpellName ("Counter Magic");
		spell.setSpellDescription ("All enemy spell cast in combat must resist being dispelled while this spell is in effect.");
		spell.setSpellHelpText ("Creates a reserve of counter magic power which resists all spells cast by an opponent wizard as if you had cast an equally-strong dispel magic." +
			System.lineSeparator () + System.lineSeparator () +
			"Each spell casting attempt by the opposing wizard reduces the strength of the counter magic reserve by five mana.");
		when (lang.findSpell ("SP048")).thenReturn (spell);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Spell book section
		final SpellBookSectionLang section = new SpellBookSectionLang ();
		section.setSpellBookSectionName ("Combat Enchantments");
		when (lang.findSpellBookSection (SpellBookSectionID.COMBAT_ENCHANTMENTS)).thenReturn (section);
		
		// Production type
		final ProductionTypeLang research = new ProductionTypeLang ();
		research.setProductionTypeSuffix ("RP");
		when (lang.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH)).thenReturn (research);

		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from client XML
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Spell spellDef1 = new Spell ();
		spellDef1.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		spellDef1.setResearchCost (180);
		when (db.findSpell ("SP048", "HelpUI")).thenReturn (spellDef1);

		final Spell spellDef2 = new Spell ();		
		when (db.findSpell ("SP110", "HelpUI")).thenReturn (spellDef2);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final PickGfx bookGfx = new PickGfx ();
		for (int n = 1; n <= 3; n++)
			bookGfx.getBookImageFile ().add ("/momime.client.graphics/picks/life-" + n + ".png");
		
		when (gfx.findPick ("MB01", "showPickID")).thenReturn (bookGfx);
		when (gfx.findPick ("RT01", "showPickID")).thenReturn (new PickGfx ());
		
		final CombatAreaEffectGfx caeGfx = new CombatAreaEffectGfx ();
		caeGfx.setCombatAreaEffectImageFile ("/momime.client.graphics/combat/effects/CSE048.png");
		when (gfx.findCombatAreaEffect ("CSE048", "showCombatAreaEffectID")).thenReturn (caeGfx);
		
		final CityViewElementGfx citySpellEffectGfx = new CityViewElementGfx ();
		citySpellEffectGfx.setCityViewImageFile ("/momime.client.graphics/cityView/sky/arcanus-SE110-mini.png");
		when (gfx.findCitySpellEffect ("SE110", "showCitySpellEffectID")).thenReturn (citySpellEffectGfx);
		
		// Images for inline text in EL expressions
		final UnitSkillGfx meleeGfx = new UnitSkillGfx ();
		int weaponGradeNumber = 0;
		for (final String imageFilename : new String [] {"Normal", "Alchemy", "Mithril", "Adamantium"})
		{
			final UnitSkillWeaponGrade weaponGrade = new UnitSkillWeaponGrade ();
			weaponGrade.setWeaponGradeNumber (weaponGradeNumber);
			weaponGrade.setSkillImageFile ("/momime.client.graphics/unitSkills/melee" + imageFilename + ".png");
			meleeGfx.getUnitSkillWeaponGrade ().add (weaponGrade);
			
			weaponGradeNumber++;
		}
		
		meleeGfx.buildMap ();
		when (gfx.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "HelpText")).thenReturn (meleeGfx);
		
		// Unit attribute component backgrounds
		final UnitSkillComponentImageGfx basicBackground = new UnitSkillComponentImageGfx ();
		basicBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/basic.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.BASIC, "HelpText")).thenReturn (basicBackground);
		
		final UnitSkillComponentImageGfx weaponGradeBackground = new UnitSkillComponentImageGfx ();
		weaponGradeBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/weaponGrade.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.WEAPON_GRADE, "HelpText")).thenReturn (weaponGradeBackground);
		
		final UnitSkillComponentImageGfx experienceBackground = new UnitSkillComponentImageGfx ();
		experienceBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/experience.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.EXPERIENCE, "HelpText")).thenReturn (experienceBackground);
		
		final UnitSkillComponentImageGfx heroSkillsBackground = new UnitSkillComponentImageGfx ();
		heroSkillsBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/heroSkills.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.HERO_SKILLS, "HelpText")).thenReturn (heroSkillsBackground);
		
		final UnitSkillComponentImageGfx caeBackground = new UnitSkillComponentImageGfx ();
		caeBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/combatAreaEffect.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.COMBAT_AREA_EFFECTS, "HelpText")).thenReturn (caeBackground);
		
		// Unit skills
		final BufferedImage skillIcon = utils.loadImage ("/momime.client.graphics/unitSkills/US020-icon.png");
		
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitSkillSingleIcon (any (AvailableUnit.class), eq ("US020"))).thenReturn (skillIcon);
		
		// Unit variable stats in help text
		final UnitStatsLanguageVariableReplacer unitStatsReplacer = mock (UnitStatsLanguageVariableReplacer.class);
		when (unitStatsReplacer.replaceVariables ("PFTitle")).thenReturn ("Path Finding");
		when (unitStatsReplacer.replaceVariables ("PFText")).thenReturn ("Pathfinding allows the unit and other land units stacked with it to move across any land square at the cost of � movement point, as if they were on a road.");
		
		when (unitStatsReplacer.replaceVariables ("MeleeTitle")).thenReturn ("Melee");
		
		// Spells
		final SpellClientUtils spellClientUtils = mock (SpellClientUtils.class);
		when (spellClientUtils.findImageForSpell ("SP048", 3)).thenReturn (utils.loadImage (caeGfx.getCombatAreaEffectImageFile ()));
		
		when (spellClientUtils.listUpkeepsOfSpell (spellDef2, new ArrayList<PlayerPick> ())).thenReturn ("Upkeep: 5 Mana per turn");
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Hero item slots
		final HeroItemSlotTypeGfx slotType = new HeroItemSlotTypeGfx ();
		slotType.setHeroItemSlotTypeImageFileWithBackground ("/momime.client.graphics/heroItems/slots/armour-bkg.png");
		when (gfx.findHeroItemSlotType ("IST01", "showHeroItemSlotTypeID")).thenReturn (slotType);
		
		when (lang.findHeroItemSlotTypeDescription ("IST01")).thenReturn ("Armour slot");
		
		final HeroItemSlotType slot = new HeroItemSlotType ();
		for (int n = 1; n <= 3; n++)
		{
			final HeroSlotAllowedItemType allowed = new HeroSlotAllowedItemType ();
			allowed.setHeroItemTypeID ("IT0"+ n);
			slot.getHeroSlotAllowedItemType ().add (allowed);
		}
		
		when (db.findHeroItemSlotType ("IST01", "HelpUI")).thenReturn (slot);
		
		int n = 0;
		for (final String itemTypeDescription : new String [] {"Shield", "Chain", "Plate"})
		{
			n++;
			when (lang.findHeroItemTypeDescription ("IT0" + n)).thenReturn (itemTypeDescription);
		}
		
		// EL replacer
		final StandardEvaluationContext context = new StandardEvaluationContext ();
		context.setRootObject (gfx);
		
		final SpringExpressionReplacerImpl replacer = new SpringExpressionReplacerImpl ();
		replacer.setEvaluationContext (context);
		replacer.setClasspathResource (true);
		replacer.setHtmlImage (true);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) ClientTestData.createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewTurnMessagesUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HelpUI help = new HelpUI ();
		help.setNewTurnMessagesLayout (layout);
		help.setUtils (utils);
		help.setLanguageHolder (langHolder);
		help.setLanguageChangeMaster (langMaster);
		help.setGraphicsDB (gfx);
		help.setUnitClientUtils (unitClientUtils);
		help.setUnitStatsReplacer (unitStatsReplacer);
		help.setSpellClientUtils (spellClientUtils);
		help.setSpellUtils (spellUtils);
		help.setClient (client);
		help.setLargeFont (CreateFontsForTests.getLargeFont ());
		help.setSmallFont (CreateFontsForTests.getSmallFont ());
		help.setTextUtils (new TextUtilsImpl ());
		help.setSpringExpressionReplacer (replacer);
		
		return help;
	}
	
	/**
	 * Tests displaying help text about a retort
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_Retort () throws Exception
	{
		final HelpUI help = createHelpUI ();
		help.showPickID ("RT01");
		Thread.sleep (5000);
		help.setVisible (false);
	}

	/**
	 * Tests displaying help text about a spell book / magic realm
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_Book () throws Exception
	{
		final HelpUI help = createHelpUI ();
		help.showPickID ("MB01");
		Thread.sleep (5000);
		help.setVisible (false);
	}

	/**
	 * Tests displaying help text about a unit skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_UnitSkill () throws Exception
	{
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		final HelpUI help = createHelpUI ();
		help.showUnitSkillID ("US020", xu);
		Thread.sleep (5000);
		help.setVisible (false);
	}

	/**
	 * Tests displaying help text about a unit attribute
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_UnitAttribute () throws Exception
	{
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		final HelpUI help = createHelpUI ();
		help.showUnitSkillID ("UA01", xu);
		Thread.sleep (5000);
		help.setVisible (false);
	}

	/**
	 * Tests displaying help text about a combat area effect
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_CombatAreaEffect () throws Exception
	{
		final HelpUI help = createHelpUI ();
		help.showCombatAreaEffectID ("CSE048");
		Thread.sleep (5000);
		help.setVisible (false);
	}

	/**
	 * Tests displaying help text about a city spell effect
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_CitySpellEffect () throws Exception
	{
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);

		final MomPersistentPlayerPublicKnowledge pub = new MomPersistentPlayerPublicKnowledge ();
		final PlayerPublicDetails castingPlayer = new PlayerPublicDetails (pd, pub, null);

		final HelpUI help = createHelpUI ();
		help.showCitySpellEffectID ("SE110", "SP110", castingPlayer);
		Thread.sleep (5000);
		help.setVisible (false);
	}

	/**
	 * Tests displaying help text about a spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_Spell () throws Exception
	{
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (3);
		
		final PlayerPublicDetails castingPlayer = new PlayerPublicDetails (pd, null, null);
		
		final HelpUI help = createHelpUI ();
		help.showSpellID ("SP048", castingPlayer);
		Thread.sleep (5000);
		help.setVisible (false);
	}

	/**
	 * Tests displaying help text about a hero item slot type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testHelpUI_HeroItemSlotType () throws Exception
	{
		final HelpUI help = createHelpUI ();
		help.showHeroItemSlotTypeID ("IST01");
		Thread.sleep (5000);
		help.setVisible (false);
	}
}
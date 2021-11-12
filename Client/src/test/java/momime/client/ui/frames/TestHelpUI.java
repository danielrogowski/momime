package momime.client.ui.frames;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitSkillComponentImage;
import momime.client.language.LanguageChangeMaster;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.language.replacer.SpringEvaluationContextRoot;
import momime.client.language.replacer.SpringExpressionReplacerImpl;
import momime.client.language.replacer.UnitStatsLanguageVariableReplacer;
import momime.client.languages.database.HelpScreen;
import momime.client.languages.database.HeroItemInfoScreen;
import momime.client.ui.fonts.CreateFontsForTests;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.TextUtilsImpl;
import momime.client.utils.UnitClientUtils;
import momime.common.database.CitySpellEffect;
import momime.common.database.CityViewElement;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlotType;
import momime.common.database.HeroItemType;
import momime.common.database.Language;
import momime.common.database.Pick;
import momime.common.database.ProductionTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellBookSection;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillWeaponGrade;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.PlayerPick;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellUtils;

/**
 * Tests the HelpUI class
 */
@ExtendWith(MockitoExtension.class)
public final class TestHelpUI extends ClientTestData
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

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Pick retort = new Pick ();
		retort.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Alchemy"));
		retort.getPickHelpText ().add (createLanguageText (Language.ENGLISH, "Allows the wizard to change gold into mana and mana into gold in a 1 to 1 ratio, instead of loosing ½ in the exchange process." +
			System.lineSeparator () + System.lineSeparator () +
			"It also gives all units built in cities magic weapons with +1 to hit bonus and the ability to hurt units with Weapon Immunity, the same as if they were built in a city with an Alchemists' Guild." +
			System.lineSeparator () + System.lineSeparator () +
			"The Alchemy retort is the only way that Gnolls, Klackons, Lizardmen or Trolls can build units with magic weapons, since these races cannot build Alchemists' Guilds."));
		when (db.findPick (eq ("RT01"), anyString ())).thenReturn (retort);
		
		final Pick book = new Pick ();
		book.getPickDescriptionSingular ().add (createLanguageText (Language.ENGLISH, "Life Book"));
		book.getPickHelpText ().add (createLanguageText (Language.ENGLISH,
			"Life spells focus on healing, protections and inspirational enchantments, and planar travel.  Life magic forcefully opposes the forces of death and mildly resists the forces of chaos."));
		
		for (int n = 1; n <= 3; n++)
			book.getBookImageFile ().add ("/momime.client.graphics/picks/life-" + n + ".png");

		when (db.findPick (eq ("MB01"), anyString ())).thenReturn (book);
		
		final UnitSkillEx unitSkill = new UnitSkillEx ();
		unitSkill.getUnitSkillDescription ().add (createLanguageText (Language.ENGLISH, "PFTitle"));
		unitSkill.getUnitSkillHelpText ().add (createLanguageText (Language.ENGLISH, "PFText"));
		when (db.findUnitSkill (eq ("US020"), anyString ())).thenReturn (unitSkill);
		
		final UnitSkillEx unitAttribute = new UnitSkillEx ();
		unitAttribute.getUnitSkillDescription ().add (createLanguageText (Language.ENGLISH, "MeleeTitle"));
		unitAttribute.getUnitSkillHelpText ().add (createLanguageText (Language.ENGLISH, "Each icon represents one chance that each figure has to strike a blow in a hand to hand attack.  The base chance that each sword icon will score a hit is 30%, although this can be modified by the attacker's + to Hit attribute." +
			System.lineSeparator () + System.lineSeparator () +
			"The unit being attacked can then use its Defence attribute to try to block some of the hits.  The base chance that each shield icon will block a hit is 30%, although this can be modified by the defender's + to Block attribute." +
			System.lineSeparator () + System.lineSeparator () +
			"Melee weapons are available in a number of weapon grades which confer improved stats:" + System.lineSeparator () +
			"#{clientDB.findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (0, 'HelpText')} Standard weapon" + System.lineSeparator () +
			"#{clientDB.findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (1, 'HelpText')} Magic weapon" + System.lineSeparator () +
			"#{clientDB.findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (2, 'HelpText')} Mithril weapon" + System.lineSeparator () +
			"#{clientDB.findUnitSkill ('UA01', 'HelpText').findWeaponGradeImageFile (3, 'HelpText')} Adamantium weapon" +
			System.lineSeparator () + System.lineSeparator () +
			"The background colour of each icon depicts the source of the attribute:" + System.lineSeparator () +
			"#{graphicsDB.findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).BASIC, 'HelpText').UnitSkillComponentImageFile} Basic statistic of the unit" + System.lineSeparator () +
			"#{graphicsDB.findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).WEAPON_GRADE, 'HelpText').UnitSkillComponentImageFile} Bonus from an improved weapon grade" + System.lineSeparator () +
			"#{graphicsDB.findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).EXPERIENCE, 'HelpText').UnitSkillComponentImageFile} Bonus from experience" + System.lineSeparator () +
			"#{graphicsDB.findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).HERO_SKILLS, 'HelpText').UnitSkillComponentImageFile} Bonus from Might hero skill" + System.lineSeparator () +
			"#{graphicsDB.findUnitSkillComponent (T(momime.common.database.UnitSkillComponent).COMBAT_AREA_EFFECTS, 'HelpText').UnitSkillComponentImageFile} Bonus from a combat area effect (e.g. node aura or Prayer spell)" +
			System.lineSeparator () + System.lineSeparator () +
			"A darkened icon represents a minus, for example from a curse type spell." +
			System.lineSeparator () + System.lineSeparator () +
			"And some extra text on the end" + System.lineSeparator () +
			"to make the help text long enough" + System.lineSeparator () +
			"to require a scroll bar to appear."));
		when (db.findUnitSkill (eq ("UA01"), anyString ())).thenReturn (unitAttribute);
		
		final CombatAreaEffect cae = new CombatAreaEffect ();
		cae.setCombatAreaEffectImageFile ("/momime.client.graphics/combat/effects/CSE048.png");
		cae.getCombatAreaEffectDescription ().add (createLanguageText (Language.ENGLISH, "Counter Magic"));
		cae.getCombatAreaEffectHelpText ().add (createLanguageText (Language.ENGLISH, "Creates a standing dispel magic spell over the entire battlefield." +
			System.lineSeparator () + System.lineSeparator () +
			"A spell cast by the enemy wizard or an enemy hero must first overcome the effects of this dispel magic spell (of strength equal to the magic power poured into the counter magic spell) before it can exert its effects." +
			System.lineSeparator () + System.lineSeparator () +
			"Every spell cast by the enemy drains the magic power (strength) of the counter magic spell by 5 mana points and, therefore, lessens its effectiveness against subsequent spells."));
		when (db.findCombatAreaEffect (eq ("CSE048"), anyString ())).thenReturn (cae);
		
		final CitySpellEffect citySpellEffect = new CitySpellEffect ();
		citySpellEffect.getCitySpellEffectName ().add (createLanguageText (Language.ENGLISH, "Chaos Rift"));
		citySpellEffect.getCitySpellEffectHelpText ().add (createLanguageText (Language.ENGLISH, "Opens a great magical vortex over an enemy city." +
			System.lineSeparator () + System.lineSeparator () +
			"Each turn, units inside the city sustain five strength 8 Lightning Bolt attacks, and there is a 5% chance of a building being destroyed."));
		when (db.findCitySpellEffect (eq ("SE110"), anyString ())).thenReturn (citySpellEffect);

		// Mock entries from the language XML
		final HelpScreen helpScreenLang = new HelpScreen ();
		helpScreenLang.getTitle ().add (createLanguageText (Language.ENGLISH, "Help"));
		helpScreenLang.getSpellBookSection ().add (createLanguageText (Language.ENGLISH, "Spell book section: SPELL_BOOK_SECTION"));
		helpScreenLang.getSpellBookResearchCostNotOurs ().add (createLanguageText (Language.ENGLISH, "Research cost: RESEARCH_TOTAL PRODUCTION_TYPE"));

		final HeroItemInfoScreen heroItemInfoScreenLang = new HeroItemInfoScreen ();
		heroItemInfoScreenLang.getItemSlotHelpTextPrefix ().add (createLanguageText (Language.ENGLISH, "The following types of hero items can be used in this slot:"));

		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getHelpScreen ()).thenReturn (helpScreenLang);
		when (lang.getHeroItemInfoScreen ()).thenReturn (heroItemInfoScreenLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Spell book section
		final SpellBookSection section = new SpellBookSection ();
		section.getSpellBookSectionName ().add (createLanguageText (Language.ENGLISH, "Combat Enchantments"));
		when (db.findSpellBookSection (eq (SpellBookSectionID.COMBAT_ENCHANTMENTS), anyString ())).thenReturn (section);
		
		// Production types
		final ProductionTypeEx research = new ProductionTypeEx();
		research.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "RP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, "HelpUI")).thenReturn (research);

		final ProductionTypeEx mana = new ProductionTypeEx ();
		mana.getProductionTypeSuffix ().add (createLanguageText (Language.ENGLISH, "MP"));
		when (db.findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "HelpUI")).thenReturn (mana);
		
		final CityViewElement citySpellEffectGfx = new CityViewElement ();
		citySpellEffectGfx.setCityViewImageFile ("/momime.client.graphics/cityView/sky/arcanus-SE110-mini.png");
		when (db.findCityViewElementSpellEffect ("SE110")).thenReturn (citySpellEffectGfx);
		
		// Mock dummy language change master, since the language won't be changing
		final LanguageChangeMaster langMaster = mock (LanguageChangeMaster.class);
		
		// Mock entries from client XML
		final Spell spellDef1 = new Spell ();
		spellDef1.setSpellBookSectionID (SpellBookSectionID.COMBAT_ENCHANTMENTS);
		spellDef1.setResearchCost (180);
		spellDef1.getSpellName ().add (createLanguageText (Language.ENGLISH, "Counter Magic"));
		spellDef1.getSpellDescription ().add (createLanguageText (Language.ENGLISH, "All enemy spell cast in combat must resist being dispelled while this spell is in effect."));
		spellDef1.getSpellHelpText ().add (createLanguageText (Language.ENGLISH,
			"Creates a reserve of counter magic power which resists all spells cast by an opponent wizard as if you had cast an equally-strong dispel magic." +
			System.lineSeparator () + System.lineSeparator () +
			"Each spell casting attempt by the opposing wizard reduces the strength of the counter magic reserve by five mana."));
		when (db.findSpell ("SP048", "HelpUI")).thenReturn (spellDef1);

		final Spell spellDef2 = new Spell ();		
		when (db.findSpell ("SP110", "HelpUI")).thenReturn (spellDef2);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock entries from the graphics XML
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		// Images for inline text in EL expressions
		final UnitSkillEx meleeGfx = new UnitSkillEx ();
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
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "HelpText")).thenReturn (meleeGfx);
		
		// Unit attribute component backgrounds
		final UnitSkillComponentImage basicBackground = new UnitSkillComponentImage ();
		basicBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/basic.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.BASIC, "HelpText")).thenReturn (basicBackground);
		
		final UnitSkillComponentImage weaponGradeBackground = new UnitSkillComponentImage ();
		weaponGradeBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/weaponGrade.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.WEAPON_GRADE, "HelpText")).thenReturn (weaponGradeBackground);
		
		final UnitSkillComponentImage experienceBackground = new UnitSkillComponentImage ();
		experienceBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/experience.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.EXPERIENCE, "HelpText")).thenReturn (experienceBackground);
		
		final UnitSkillComponentImage heroSkillsBackground = new UnitSkillComponentImage ();
		heroSkillsBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/heroSkills.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.HERO_SKILLS, "HelpText")).thenReturn (heroSkillsBackground);
		
		final UnitSkillComponentImage caeBackground = new UnitSkillComponentImage ();
		caeBackground.setUnitSkillComponentImageFile ("/momime.client.graphics/unitSkills/componentBackgrounds/combatAreaEffect.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.COMBAT_AREA_EFFECTS, "HelpText")).thenReturn (caeBackground);
		
		// Unit skills
		final BufferedImage skillIcon = utils.loadImage ("/momime.client.graphics/unitSkills/US020-icon.png");
		
		final UnitClientUtils unitClientUtils = mock (UnitClientUtils.class);
		when (unitClientUtils.getUnitSkillSingleIcon (any (ExpandedUnitDetails.class), eq ("US020"))).thenReturn (skillIcon);
		
		// Unit variable stats in help text
		final UnitStatsLanguageVariableReplacer unitStatsReplacer = mock (UnitStatsLanguageVariableReplacer.class);
		when (unitStatsReplacer.replaceVariables ("PFTitle")).thenReturn ("Path Finding");
		when (unitStatsReplacer.replaceVariables ("PFText")).thenReturn ("Pathfinding allows the unit and other land units stacked with it to move across any land square at the cost of ½ movement point, as if they were on a road.");
		
		when (unitStatsReplacer.replaceVariables ("MeleeTitle")).thenReturn ("Melee");
		
		// Spells
		final SpellClientUtils spellClientUtils = mock (SpellClientUtils.class);
		when (spellClientUtils.findImageForSpell ("SP048", 3)).thenReturn (utils.loadImage (cae.getCombatAreaEffectImageFile ()));
		
		when (spellClientUtils.listUpkeepsOfSpell (spellDef2, new ArrayList<PlayerPick> ())).thenReturn ("Upkeep: 5 Mana per turn");
		
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		// Hero item slots
		final HeroItemSlotType slotType = new HeroItemSlotType ();
		slotType.setHeroItemSlotTypeImageFileWithBackground ("/momime.client.graphics/heroItems/slots/armour-bkg.png");
		when (db.findHeroItemSlotType ("IST01", "showHeroItemSlotTypeID")).thenReturn (slotType);
		
		final HeroItemSlotType slot = new HeroItemSlotType ();
		slot.getSlotTypeDescription ().add (createLanguageText (Language.ENGLISH, "Armour slot"));
		
		for (int n = 1; n <= 3; n++)
			slot.getHeroSlotAllowedItemType ().add ("IT0"+ n);
		
		when (db.findHeroItemSlotType ("IST01", "HelpUI")).thenReturn (slot);
		
		int n = 0;
		for (final String itemTypeDescription : new String [] {"Shield", "Chain", "Plate"})
		{
			n++;
			final HeroItemType heroItemType = new HeroItemType ();
			heroItemType.getHeroItemTypeDescription ().add (createLanguageText (Language.ENGLISH, itemTypeDescription));
			when (db.findHeroItemType ("IT0" + n, "HelpUI")).thenReturn (heroItemType);
		}
		
		// EL replacer
		final StandardEvaluationContext context = new StandardEvaluationContext ();
		context.setRootObject (new SpringEvaluationContextRoot (gfx, client));
		
		final SpringExpressionReplacerImpl replacer = new SpringExpressionReplacerImpl ();
		replacer.setEvaluationContext (context);
		replacer.setClasspathResource (true);
		replacer.setHtmlImage (true);

		// Layout
		final XmlLayoutContainerEx layout = (XmlLayoutContainerEx) createXmlLayoutUnmarshaller ().unmarshal (getClass ().getResource ("/momime.client.ui.frames/NewTurnMessagesUI.xml"));
		layout.buildMaps ();
		
		// Set up form
		final HelpUI help = new HelpUI ();
		help.setNewTurnMessagesLayout (layout);
		help.setUtils (utils);
		help.setLanguageHolder (langHolder);
		help.setLanguageChangeMaster (langMaster);
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
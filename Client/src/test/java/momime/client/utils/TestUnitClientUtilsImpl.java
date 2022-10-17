package momime.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.utils.swing.NdgUIUtils;
import com.ndg.utils.swing.NdgUIUtilsImpl;
import com.ndg.utils.swing.zorder.ZOrderGraphicsImmediateImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitSkillComponentImage;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.languages.database.UnitName;
import momime.client.ui.PlayerColourImageGeneratorImpl;
import momime.common.database.AnimationEx;
import momime.common.database.AnimationFrame;
import momime.common.database.CombatAction;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.HeroName;
import momime.common.database.Language;
import momime.common.database.RaceEx;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.RangedAttackTypeWeaponGrade;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatActionEx;
import momime.common.database.UnitCombatImage;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.database.UnitSkillWeaponGrade;
import momime.common.database.UnitType;
import momime.common.database.WeaponGrade;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.UnitUtils;

/**
 * Tests the UnitClientUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestUnitClientUtilsImpl extends ClientTestData
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (TestUnitClientUtilsImpl.class);
	
	/** Colour for a transparent pixel */
	private final static int TRANSPARENT = 0;
	
	/** Background colour for basic stats */
	private final static int BACKGROUND_BASIC = 0xFF800000;
	
	/** Background colour for weapon grade bonuses */
	private final static int BACKGROUND_WEAPON_GRADE = 0xFF00FF00;

	/** Background colour for experience bonuses */
	private final static int BACKGROUND_EXPERIENCE = 0xFF008000;

	/** Background colour for hero skill bonuses */
	private final static int BACKGROUND_HERO_SKILLS = 0xFF0000FF;
	
	/** Background colour for CAE bonuses */
	private final static int BACKGROUND_CAE = 0xFFEE0000;
	
	/** Colour for a skill icon pixel */
	private final static int SKILL_ICON = 0xFF00EE00;
	
	/**
	 * Tests the getUnitName method
	 * @throws RecordNotFoundException If we can't find the unit definition in the server XML
	 */
	@Test
	public final void testGetUnitName () throws RecordNotFoundException
	{
		// Mock entries from client DB
		final CommonDatabase db = mock (CommonDatabase.class);

		int n = 0;
		for (final String unitName : new String [] {"Bard", "Trireme", "Swordsmen", "Longbowmen", "Magic Spirit", "Hell Hounds"})
		{
			n++;
			final UnitEx unitInfo = new UnitEx ();
			unitInfo.getUnitName ().add (createLanguageText (Language.ENGLISH, unitName));

			if ((n == 2) || (n == 5))
				unitInfo.getUnitNamePrefix ().add (createLanguageText (Language.ENGLISH, "a")); 
			
			if (n == 3)
				unitInfo.setIncludeRaceInUnitName (true);
			
			if ((n == 3) || (n == 4))
				unitInfo.setUnitRaceID ("RC01");

			if (n == 1)
			{
				final HeroName heroName = new HeroName ();
				heroName.setHeroNameID ("UN001_HN01");
				heroName.getHeroNameLang ().add (createLanguageText (Language.ENGLISH, "Valana the Bard"));
				unitInfo.getHeroName ().add (heroName);
			}
			
			unitInfo.buildMaps ();
			when (db.findUnit ("UN00" + n, "getUnitName")).thenReturn (unitInfo);
		}
		
		final RaceEx race = new RaceEx ();
		race.getRaceNameSingular ().add (createLanguageText (Language.ENGLISH, "Orc"));
		when (db.findRace ("RC01", "getUnitName")).thenReturn (race);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);

		// Mock entries from the language XML
		final UnitName unitNameLang = new UnitName ();
		unitNameLang.getTheUnitOfNameSingular ().add (createLanguageText (Language.ENGLISH, "the RACE_UNIT_NAME"));
		unitNameLang.getTheUnitOfNamePlural ().add (createLanguageText (Language.ENGLISH, "the unit of RACE_UNIT_NAME"));
		unitNameLang.getAUnitOfNameSingular ().add (createLanguageText (Language.ENGLISH, "a RACE_UNIT_NAME"));
		unitNameLang.getAUnitOfNamePlural ().add (createLanguageText (Language.ENGLISH, "a unit of RACE_UNIT_NAME"));
		unitNameLang.getUnitsOfNameSingular ().add (createLanguageText (Language.ENGLISH, "RACE_UNIT_NAMEs"));
		unitNameLang.getUnitsOfNamePlural ().add (createLanguageText (Language.ENGLISH, "units of RACE_UNIT_NAME"));
		
		final MomLanguagesEx lang = mock (MomLanguagesEx.class);
		when (lang.getUnitName ()).thenReturn (unitNameLang);
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguages (lang);
		
		// Create one of each kind of unit
		final MemoryUnit heroOverridden = new MemoryUnit ();
		heroOverridden.setUnitID ("UN001");
		heroOverridden.setHeroNameID ("UN001_HN01");
		heroOverridden.setUnitName ("Renamed Hero");
		
		final MemoryUnit heroGenerated = new MemoryUnit ();
		heroGenerated.setUnitID ("UN001");
		heroGenerated.setHeroNameID ("UN001_HN01");
		
		final AvailableUnit nonRaceSpecific = new AvailableUnit ();
		nonRaceSpecific.setUnitID ("UN002");

		final AvailableUnit raceSpecific = new AvailableUnit ();
		raceSpecific.setUnitID ("UN003");

		final AvailableUnit raceUnique = new AvailableUnit ();
		raceUnique.setUnitID ("UN004");
		
		final AvailableUnit summonedSingular = new AvailableUnit ();
		summonedSingular.setUnitID ("UN005");

		final AvailableUnit summonedPlural = new AvailableUnit ();
		summonedPlural.setUnitID ("UN006");

		final AvailableUnit unknown = new AvailableUnit ();
		unknown.setUnitID ("UN007");
		
		// Set up object to test
		final UnitClientUtilsImpl utils = new UnitClientUtilsImpl ();
		utils.setClient (client);
		utils.setLanguageHolder (langHolder);
		
		// Test SIMPLE_UNIT_NAME
		assertEquals ("Renamed Hero",	utils.getUnitName (heroOverridden,			UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Valana the Bard",	utils.getUnitName (heroGenerated,			UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Trireme",				utils.getUnitName (nonRaceSpecific,			UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Swordsmen",		utils.getUnitName (raceSpecific,				UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.SIMPLE_UNIT_NAME));

		// Test RACE_UNIT_NAME
		assertEquals ("Renamed Hero",	utils.getUnitName (heroOverridden,			UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Valana the Bard",	utils.getUnitName (heroGenerated,			UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Trireme",				utils.getUnitName (nonRaceSpecific,			UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.RACE_UNIT_NAME));

		// Test A_UNIT_NAME
		assertEquals ("Renamed Hero",	utils.getUnitName (heroOverridden,			UnitNameType.A_UNIT_NAME));
		assertEquals ("Valana the Bard",	utils.getUnitName (heroGenerated,			UnitNameType.A_UNIT_NAME));
		assertEquals ("a Trireme",			utils.getUnitName (nonRaceSpecific,			UnitNameType.A_UNIT_NAME));
		assertEquals ("Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.A_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.A_UNIT_NAME));
		assertEquals ("a Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.A_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.A_UNIT_NAME));

		// Test THE_UNIT_OF_NAME
		assertEquals ("Renamed Hero",					utils.getUnitName (heroOverridden,			UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("Valana the Bard",					utils.getUnitName (heroGenerated,			UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the Trireme",						utils.getUnitName (nonRaceSpecific,			UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the Magic Spirit",					utils.getUnitName (summonedSingular,		UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.THE_UNIT_OF_NAME));

		// Test A_UNIT_OF_NAME
		assertEquals ("Renamed Hero",					utils.getUnitName (heroOverridden,			UnitNameType.A_UNIT_OF_NAME));
		assertEquals ("Valana the Bard",					utils.getUnitName (heroGenerated,			UnitNameType.A_UNIT_OF_NAME));
		assertEquals ("a Trireme",							utils.getUnitName (nonRaceSpecific,			UnitNameType.A_UNIT_OF_NAME));
		assertEquals ("a unit of Orc Swordsmen",		utils.getUnitName (raceSpecific,				UnitNameType.A_UNIT_OF_NAME));
		assertEquals ("a unit of Longbowmen",			utils.getUnitName (raceUnique,				UnitNameType.A_UNIT_OF_NAME));
		assertEquals ("a Magic Spirit",						utils.getUnitName (summonedSingular,		UnitNameType.A_UNIT_OF_NAME));
		assertEquals ("a unit of Hell Hounds",			utils.getUnitName (summonedPlural,		UnitNameType.A_UNIT_OF_NAME));

		// Test UNITS_OF_NAME
		assertEquals ("Renamed Hero",					utils.getUnitName (heroOverridden,			UnitNameType.UNITS_OF_NAME));
		assertEquals ("Valana the Bard",					utils.getUnitName (heroGenerated,			UnitNameType.UNITS_OF_NAME));
		assertEquals ("Triremes",							utils.getUnitName (nonRaceSpecific,			UnitNameType.UNITS_OF_NAME));
		assertEquals ("units of Orc Swordsmen",		utils.getUnitName (raceSpecific,				UnitNameType.UNITS_OF_NAME));
		assertEquals ("units of Longbowmen",			utils.getUnitName (raceUnique,				UnitNameType.UNITS_OF_NAME));
		assertEquals ("Magic Spirits",						utils.getUnitName (summonedSingular,		UnitNameType.UNITS_OF_NAME));
		assertEquals ("units of Hell Hounds",			utils.getUnitName (summonedPlural,		UnitNameType.UNITS_OF_NAME));
	}
	
	/**
	 * Tests the getUnitSkillComponentBreakdownIcon method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnitSkillComponentBreakdownIcon () throws Exception
	{
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage plusToHitImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("plusToHit.png")).thenReturn (plusToHitImage);

		final BufferedImage meleeWepGrade2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("melee2.png")).thenReturn (meleeWepGrade2Image);
		
		final BufferedImage rat1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("rat1.png")).thenReturn (rat1Image);

		final BufferedImage rat2wepGrade2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("rat2-2.png")).thenReturn (rat2wepGrade2Image);
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// + to hit doesn't vary by weapon grade
		final UnitSkillEx plusToHit = new UnitSkillEx ();

		final UnitSkillWeaponGrade plusToHitIcon = new UnitSkillWeaponGrade ();
		plusToHitIcon.setSkillImageFile ("plusToHit.png");
		plusToHit.getUnitSkillWeaponGrade ().add (plusToHitIcon);
		
		plusToHit.buildMap ();;
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT, "getUnitSkillComponentBreakdownIcon")).thenReturn (plusToHit);
		
		// melee varies by weapon grade
		final UnitSkillEx melee = new UnitSkillEx ();

		for (int wepGrade = 1; wepGrade <= 3; wepGrade++)
		{
			final UnitSkillWeaponGrade meleeIcon = new UnitSkillWeaponGrade ();
			meleeIcon.setSkillImageFile ("melee" + wepGrade + ".png");
			meleeIcon.setWeaponGradeNumber (wepGrade);
			melee.getUnitSkillWeaponGrade ().add (meleeIcon);
		}
		
		melee.buildMap ();
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "getUnitSkillComponentBreakdownIcon")).thenReturn (melee);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// RAT that doesn't vary by weapon grade
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		rat.setRangedAttackTypeID ("RAT01");

		final RangedAttackTypeWeaponGrade rat1Icon = new RangedAttackTypeWeaponGrade ();
		rat1Icon.setUnitDisplayRangedImageFile ("rat1.png");
		rat.getRangedAttackTypeWeaponGrade ().add (rat1Icon);
		
		rat.buildMaps ();
		
		// Dummy unit
		final WeaponGrade weaponGrade = new WeaponGrade ();
		weaponGrade.setWeaponGradeNumber (2);
		
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);
		when (unit.getWeaponGrade ()).thenReturn (weaponGrade);
		when (unit.getRangedAttackType ()).thenReturn (rat);
		
		// Set up object to test
		final UnitClientUtilsImpl unitUtils = new UnitClientUtilsImpl ();
		unitUtils.setUtils (utils);
		unitUtils.setClient (client);
		
		// Run tests
		assertSame (plusToHitImage, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
		assertSame (meleeWepGrade2Image, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK));
		assertSame (rat1Image, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));

		// RAT that does vary by weapon grade
		rat.setRangedAttackTypeID ("RAT02");

		for (int wepGrade = 1; wepGrade <= 3; wepGrade++)
		{
			final RangedAttackTypeWeaponGrade rat2Icon = new RangedAttackTypeWeaponGrade ();
			rat2Icon.setUnitDisplayRangedImageFile ("rat2-" + wepGrade + ".png");
			rat2Icon.setWeaponGradeNumber (wepGrade);
			rat.getRangedAttackTypeWeaponGrade ().add (rat2Icon);
		}
		
		rat.buildMaps ();

		assertSame (rat2wepGrade2Image, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));
	}
	
	/**
	 * Tests the getUnitSkillSingleIcon method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnitSkillSingleIcon () throws Exception
	{
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage skillImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("skill.png")).thenReturn (skillImage);

		final BufferedImage exp2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("exp2.png")).thenReturn (exp2Image);

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitSkillEx skillGfx = new UnitSkillEx ();
		skillGfx.setUnitSkillImageFile ("skill.png");
		when (db.findUnitSkill ("US001", "getUnitSkillSingleIcon")).thenReturn (skillGfx);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);

		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		// Experience
		final ExperienceLevel expLvl = new ExperienceLevel ();
		expLvl.setLevelNumber (2);
		expLvl.setExperienceLevelImageFile ("exp2.png");

		// Dummy unit
		final UnitType unitTypeDef = new UnitType ();
		unitTypeDef.setUnitTypeID ("N");
		
		final ExpandedUnitDetails unit = mock (ExpandedUnitDetails.class);
		when (unit.getModifiedExperienceLevel ()).thenReturn (expLvl);
		
		// Set up object to test
		final UnitClientUtilsImpl unitClientUtils = new UnitClientUtilsImpl ();
		unitClientUtils.setGraphicsDB (gfx);
		unitClientUtils.setUtils (utils);
		unitClientUtils.setClient (client);

		// Run tests
		assertSame (skillImage, unitClientUtils.getUnitSkillSingleIcon (unit, "US001"));
		assertSame (exp2Image, unitClientUtils.getUnitSkillSingleIcon (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
	}
	
	/**
	 * @param unitID ID of unit to set up graphics for
	 * @return Unit with necessary details for testDrawUnit
	 */
	private final UnitEx createUnitGraphics (final String unitID)
	{
		final UnitCombatImage direction = new UnitCombatImage ();
		direction.setDirection (4);
		direction.setUnitCombatAnimation (unitID + "_D4_WALKFLY");

		final UnitCombatActionEx action = new UnitCombatActionEx ();
		action.setCombatActionID (GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK);
		action.getUnitCombatImage ().add (direction);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.getUnitCombatActions ().add (action);
		unitDef.buildMaps ();
		
		return unitDef;
	}
	
	/**
	 * @param unitID ID of unit to set up animation for
	 * @param flagImage1 Flag image corresponding to move-frame1
	 * @param flagOffsetX1 X offset of flag image corresponding to move-frame1
	 * @param flagOffsetY1 X offset of flag image corresponding to move-frame1
	 * @param flagImage2 Flag image corresponding to stand
	 * @param flagOffsetX2 X offset of flag image corresponding to stand
	 * @param flagOffsetY2 X offset of flag image corresponding to stand
	 * @param flagImage3 Flag image corresponding to move-frame3
	 * @param flagOffsetX3 X offset of flag image corresponding to move-frame3
	 * @param flagOffsetY3 X offset of flag image corresponding to move-frame3
	 * @return Animation with necessary details for testDrawUnit
	 */
	private final AnimationEx createUnitAnimation (final String unitID,
		final String flagImage1, final Integer flagOffsetX1, final Integer flagOffsetY1,
		final String flagImage2, final Integer flagOffsetX2, final Integer flagOffsetY2,
		final String flagImage3, final Integer flagOffsetX3, final Integer flagOffsetY3)
	{
		final AnimationEx anim = new AnimationEx ();
		anim.setAnimationSpeed (6);
		
		for (String action : new String [] {"move-frame1", "stand", "move-frame3", "stand"})
		{
			final AnimationFrame frame = new AnimationFrame ();
			frame.setImageFile ("/momime.client.graphics/units/" + unitID + "/d4-" + action + ".png");
			
			switch (anim.getFrame ().size ())
			{
				case 0:
					frame.setImageFlag (flagImage1);
					frame.setFlagOffsetX (flagOffsetX1);
					frame.setFlagOffsetY (flagOffsetY1);
					break;

				case 1:
				case 3:
					frame.setImageFlag (flagImage2);
					frame.setFlagOffsetX (flagOffsetX2);
					frame.setFlagOffsetY (flagOffsetY2);
					break;
					
				case 2:
					frame.setImageFlag (flagImage3);
					frame.setFlagOffsetX (flagOffsetX3);
					frame.setFlagOffsetY (flagOffsetY3);
					break;
			}
			
			anim.getFrame ().add (frame);
		}
		
		return anim;
	}
	
	/**
	 * Tests the drawUnit method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDrawUnit () throws Exception
	{
		// Set look and feel
		final NdgUIUtils utils = new NdgUIUtilsImpl ();
		utils.useNimbusLookAndFeel ();
		
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit (eq ("UN106"), anyString ())).thenReturn (createUnitGraphics ("UN106")); 
		when (db.findUnit (eq ("UN075"), anyString ())).thenReturn (createUnitGraphics ("UN075")); 
		when (db.findUnit (eq ("UN035"), anyString ())).thenReturn (createUnitGraphics ("UN035")); 
		when (db.findUnit (eq ("UN197"), anyString ())).thenReturn (createUnitGraphics ("UN197")); 
		when (db.findUnit (eq ("UN037"), anyString ())).thenReturn (createUnitGraphics ("UN037"));
		
		when (db.findAnimation (eq ("UN106_D4_WALKFLY"), anyString ())).thenReturn (createUnitAnimation ("UN106",
			"/momime.client.graphics/flags/combatHighMenFlag-d4-02.png", 12, 16,
			"/momime.client.graphics/flags/combatHighMenFlag-d4-05.png", 12, 16,
			"/momime.client.graphics/flags/combatHighMenFlag-d4-06.png", 11, 16));
		
		when (db.findAnimation (eq ("UN075_D4_WALKFLY"), anyString ())).thenReturn (createUnitAnimation ("UN075",
			null, null, null, null, null, null,null, null, null));
		
		when (db.findAnimation (eq ("UN035_D4_WALKFLY"), anyString ())).thenReturn (createUnitAnimation ("UN035",
			"/momime.client.graphics/flags/combatHeroFlag-d4-2.png", 10, 2,
			"/momime.client.graphics/flags/combatHeroFlag-d4-1.png", 10, 2,
			"/momime.client.graphics/flags/combatHeroFlag-d4-3.png", 10, 1));
		
		when (db.findAnimation (eq ("UN197_D4_WALKFLY"), anyString ())).thenReturn (createUnitAnimation ("UN197",
			null, null, null, null, null, null,null, null, null));
		
		when (db.findAnimation (eq ("UN037_D4_WALKFLY"), anyString ())).thenReturn (createUnitAnimation ("UN037",
			"/momime.client.graphics/flags/combatBoatFlag-d4-2.png", 14, 11,
			"/momime.client.graphics/flags/combatBoatFlag-d4-1.png", 14, 11,
			"/momime.client.graphics/flags/combatBoatFlag-d4-3.png", 14, 11));
		
		// Client
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Owner of the units
		final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
		trans.setFlagColour ("FF4040");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (null, null, trans);
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();

		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (client.getPlayers ()).thenReturn (players);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1, "getModifiedImage")).thenReturn (player);
		
		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");

		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 1, "getModifiedImage")).thenReturn (wizardDetails);
		
		// This is dependant on way too many values to mock them all - so use the real graphics DB
		final GraphicsDatabaseEx gfx = loadGraphicsDatabase (utils, null);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setClient (client);
		anim.setUtils (utils);
		
		// Image generator
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Set up object to test
		final UnitClientUtilsImpl unitUtils = new UnitClientUtilsImpl ();
		unitUtils.setAnim (anim);
		unitUtils.setGraphicsDB (gfx);
		unitUtils.setClient (client);
		unitUtils.setUtils (utils);
		unitUtils.setPlayerColourImageGenerator (gen);
		
		// Set up a dummy panel
		final Dimension panelSize = new Dimension (600, 400);
		final ZOrderGraphicsImmediateImpl zOrderGraphics = new ZOrderGraphicsImmediateImpl ();
		
		final JPanel panel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				try
				{
					zOrderGraphics.setGraphics (g);
					
					final int y = 40;
					unitUtils.drawUnitFigures ("UN106", 1, 6, 6, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 10, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null, null, false, 0);
					unitUtils.drawUnitFigures ("UN075", 1, 2, 2, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 80, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null, null, false, 0);
					unitUtils.drawUnitFigures ("UN035", 1, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 150, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null, null, false, 0);
					unitUtils.drawUnitFigures ("UN197", 1, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 220, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null, null, false, 0);
					unitUtils.drawUnitFigures ("UN037", 1, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 290, y, GraphicsDatabaseConstants.SAMPLE_OCEAN_TILE, true, 0, null, null, false, 0);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		};
		panel.setMinimumSize (panelSize);
		panel.setMaximumSize (panelSize);
		panel.setPreferredSize (panelSize);
		
		// Set up animations with the same params as all the draw calls above
		unitUtils.registerUnitFiguresAnimation ("UN106", GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, panel);
		unitUtils.registerUnitFiguresAnimation ("UN075", GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, panel);
		unitUtils.registerUnitFiguresAnimation ("UN035", GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, panel);
		unitUtils.registerUnitFiguresAnimation ("UN197", GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, panel);
		unitUtils.registerUnitFiguresAnimation ("UN037", GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, panel);

		// Set up a dummy frame
		final JFrame frame = new JFrame ();
		frame.setContentPane (panel);
		frame.setResizable (false);
		frame.pack ();
		frame.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo (null);
		
		frame.setVisible (true);
		Thread.sleep (5000);
		frame.setVisible (false);
	}
	
	/**
	 * Tests the playCombatActionSound method where a unit has no specific sound defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayCombatActionSound_Default () throws Exception
	{
		// Mock database
		final UnitCombatActionEx unitCombatAction = new UnitCombatActionEx ();
		unitCombatAction.setCombatActionID ("X");
		
		final UnitEx unitGfx = new UnitEx ();
		unitGfx.getUnitCombatAction ().add (unitCombatAction);
		unitGfx.buildMaps ();
		
		final CombatAction defaultAction = new CombatAction ();
		defaultAction.setDefaultActionSoundFile ("DefaultActionSound.mp3");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "playCombatActionSound")).thenReturn (unitGfx);
		when (db.findCombatAction ("X", "playCombatActionSound")).thenReturn (defaultAction);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final AudioPlayer soundPlayer = mock (AudioPlayer.class);
		
		final UnitClientUtilsImpl obj = new UnitClientUtilsImpl ();
		obj.setSoundPlayer (soundPlayer);
		obj.setClient (client);
		
		// Run method
		obj.playCombatActionSound (unit, "X");
		
		// Check results
		verify (soundPlayer).playAudioFile ("DefaultActionSound.mp3");
		
		verifyNoMoreInteractions (soundPlayer);
	}

	/**
	 * Tests the playCombatActionSound method where a unit has a specific sound defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayCombatActionSound_Override () throws Exception
	{
		// Mock database
		final UnitCombatActionEx unitCombatAction = new UnitCombatActionEx ();
		unitCombatAction.setCombatActionID ("X");
		unitCombatAction.setOverrideActionSoundFile ("OverrideActionSound.mp3");
		
		final UnitEx unitGfx = new UnitEx ();
		unitGfx.getUnitCombatAction ().add (unitCombatAction);
		unitGfx.buildMaps ();
		
		final CombatAction defaultAction = new CombatAction ();
		defaultAction.setDefaultActionSoundFile ("DefaultActionSound.mp3");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "playCombatActionSound")).thenReturn (unitGfx);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final AudioPlayer soundPlayer = mock (AudioPlayer.class);
		
		final UnitClientUtilsImpl obj = new UnitClientUtilsImpl ();
		obj.setSoundPlayer (soundPlayer);
		obj.setClient (client);
		
		// Run method
		obj.playCombatActionSound (unit, "X");
		
		// Check results
		verify (soundPlayer).playAudioFile ("OverrideActionSound.mp3");
		
		verifyNoMoreInteractions (soundPlayer);
	}
	
	/**
	 * Tests the generateAttributeImage method
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Test
	public final void testGenerateAttributeImage () throws IOException
	{
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("s.png")).thenReturn (createSolidImage (1, 1, SKILL_ICON));

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final UnitSkillWeaponGrade skillWeaponGradeGfx = new UnitSkillWeaponGrade ();
		skillWeaponGradeGfx.setSkillImageFile ("s.png");
				
		final UnitSkillEx skillGfx = new UnitSkillEx ();
		skillGfx.getUnitSkillWeaponGrade ().add (skillWeaponGradeGfx);
		when (db.findUnitSkill ("UA01", "getUnitSkillComponentBreakdownIcon")).thenReturn (skillGfx);
				
		// Mock graphics
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		// Attribute component backgrounds
		final UnitSkillComponentImage basicComponentBackground = new UnitSkillComponentImage ();
		basicComponentBackground.setUnitSkillComponentImageFile ("b.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.BASIC, "generateAttributeImage")).thenReturn (basicComponentBackground);
		when (utils.loadImage ("b.png")).thenReturn (createSolidImage (2, 1, BACKGROUND_BASIC));

		final UnitSkillComponentImage weaponGradeComponentBackground = new UnitSkillComponentImage ();
		weaponGradeComponentBackground.setUnitSkillComponentImageFile ("w.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.WEAPON_GRADE, "generateAttributeImage")).thenReturn (weaponGradeComponentBackground);
		when (utils.loadImage ("w.png")).thenReturn (createSolidImage (2, 1, BACKGROUND_WEAPON_GRADE));

		final UnitSkillComponentImage experienceComponentBackground = new UnitSkillComponentImage ();
		experienceComponentBackground.setUnitSkillComponentImageFile ("e.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.EXPERIENCE, "generateAttributeImage")).thenReturn (experienceComponentBackground);
		when (utils.loadImage ("e.png")).thenReturn (createSolidImage (2, 1, BACKGROUND_EXPERIENCE));

		final UnitSkillComponentImage heroSkillsComponentBackground = new UnitSkillComponentImage ();
		heroSkillsComponentBackground.setUnitSkillComponentImageFile ("h.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.HERO_SKILLS, "generateAttributeImage")).thenReturn (heroSkillsComponentBackground);
		when (utils.loadImage ("h.png")).thenReturn (createSolidImage (2, 1, BACKGROUND_HERO_SKILLS));

		final UnitSkillComponentImage caeComponentBackground = new UnitSkillComponentImage ();
		caeComponentBackground.setUnitSkillComponentImageFile ("c.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.COMBAT_AREA_EFFECTS, "generateAttributeImage")).thenReturn (caeComponentBackground);
		when (utils.loadImage ("c.png")).thenReturn (createSolidImage (2, 1, BACKGROUND_CAE));
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		// Unit stats
		final UnitUtils unitUtils = mock (UnitUtils.class);

		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Set up object to test
		final UnitClientUtilsImpl obj = new UnitClientUtilsImpl ();
		obj.setClient (client);
		obj.setUtils (utils);
		obj.setGraphicsDB (gfx);
		obj.setUnitUtils (unitUtils);
		
		// Generate image for when the unit doesn't have the attribute at all
		assertNull (obj.generateAttributeImage (xu, "UA01"));

		// Generate image for when the unit has a zero value of the attribute
		when (xu.hasModifiedSkill ("UA01")).thenReturn (true);
		when (xu.getModifiedSkillValue ("UA01")).thenReturn (null);
		assertNull (obj.generateAttributeImage (xu, "UA01"));
		
		// Generate image for when the unit has a single basic stat value of 1
		when (xu.getModifiedSkillValue ("UA01")).thenReturn (1);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.BASIC, UnitSkillPositiveNegative.POSITIVE)).thenReturn (1);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.WEAPON_GRADE, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.HERO_SKILLS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.HERO_ITEMS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.SPELL_EFFECTS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.STACK, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.PENALTIES, UnitSkillPositiveNegative.POSITIVE)).thenReturn (0);
		
		checkImage (obj.generateAttributeImage (xu, "UA01"),
			"SB                                                                   " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     ");
		
		// Generate image for when the unit has a makeup of 3 basic + 4 wep grade + 5 experience + 6 hero skills + 2 CAE = 20
		when (xu.getModifiedSkillValue ("UA01")).thenReturn (20);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.BASIC, UnitSkillPositiveNegative.POSITIVE)).thenReturn (3);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.WEAPON_GRADE, UnitSkillPositiveNegative.POSITIVE)).thenReturn (4);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.POSITIVE)).thenReturn (5);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.HERO_SKILLS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (6);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (2);		

		checkImage (obj.generateAttributeImage (xu, "UA01"),
			"SB SB SB SW SW   SW SW SE SE SE   SE SE SH SH SH   SH SH SH SC SC    " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     ");
		
		// Generate image for when the unit has a makeup of 4 basic + 5 wep grade + 7 experience + 8 hero skills + 3 CAE = 27 - 6 penalty = 21
		when (xu.getModifiedSkillValue ("UA01")).thenReturn (21);
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.BASIC, UnitSkillPositiveNegative.POSITIVE)).thenReturn (4);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.WEAPON_GRADE, UnitSkillPositiveNegative.POSITIVE)).thenReturn (5);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.POSITIVE)).thenReturn (7);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.HERO_SKILLS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (8);		
		when (xu.filterModifiedSkillValue ("UA01", UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.POSITIVE)).thenReturn (3);		

		checkImage (obj.generateAttributeImage (xu, "UA01"),
			"SB SB SB SB SW   SW SW SW SW SE   SE SE SE SE SE   SE SH SH SH SH    " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"    SH sh sh sh sc   sc sc                                           ");
	}

	/**
	 * Checks that a generated image was as expected, using a pattern where
	 * S = Skill icon
	 * B = Basic component
	 * W = Weapon grade component
	 * E = Experience component
	 * H = Hero skills component
	 * C = CAE component
	 * space = transparent
	 * 
	 * For generateUpkeepImage, mana upkeep uses the same letters in lower case
	 * 
	 * @param image Image generated by unit test
	 * @param pattern Pattern that the generated image should look like
	 */
	private final void checkImage (final BufferedImage image, final String pattern)
	{
		final String [] lines = pattern.split (System.lineSeparator ());
		
		assertEquals (lines [0].length (), image.getWidth ());
		assertEquals (lines.length, image.getHeight ());
		
		for (int y = 0; y < image.getHeight (); y++)
			for (int x = 0; x < image.getWidth (); x++)
			{
				final int expectedColour;
				switch (lines [y].charAt (x))
				{
					case 'S':
						expectedColour = SKILL_ICON;
						break;
						
					case 'B':
						expectedColour = BACKGROUND_BASIC;
						break;
	
					case 'W':
						expectedColour = BACKGROUND_WEAPON_GRADE;
						break;
						
					case 'E':
						expectedColour = BACKGROUND_EXPERIENCE;
						break;
	
					case 'H':
						expectedColour = BACKGROUND_HERO_SKILLS;
						break;
						
					case 'C':
						expectedColour = BACKGROUND_CAE;
						break;
						
					case ' ':
						expectedColour = TRANSPARENT;
						break;

					case 's':
						expectedColour = 0xFF005900;
						break;
						
					case 'h':
						expectedColour = 0xFF00005F;
						break;
						
					case 'c':
						expectedColour = 0xFF590000;
						break;
						
					default:
						throw new RuntimeException ("pattern contained an unhandled char \"" + pattern.charAt (x) + "\"");
				}
				
				assertEquals (expectedColour, image.getRGB (x, y), "Mismatching colour at " + x + ", " + y);
			}
	}
}
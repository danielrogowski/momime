package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;
import com.ndg.zorder.ZOrderGraphicsImmediateImpl;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.config.MomImeClientConfigEx;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.CombatActionGfx;
import momime.client.graphics.database.ExperienceLevelGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RangedAttackTypeGfx;
import momime.client.graphics.database.RangedAttackTypeWeaponGradeGfx;
import momime.client.graphics.database.UnitCombatActionGfx;
import momime.client.graphics.database.UnitGfx;
import momime.client.graphics.database.UnitSkillComponentImageGfx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.client.graphics.database.UnitSkillWeaponGradeGfx;
import momime.client.graphics.database.UnitTypeGfx;
import momime.client.graphics.database.v0_9_7.UnitSkillWeaponGrade;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.RaceLang;
import momime.client.language.database.UnitLang;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitCombatScale;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtils;

/**
 * Tests the UnitClientUtilsImpl class
 */
public final class TestUnitClientUtilsImpl
{
	/** Class logger */
	private final Log log = LogFactory.getLog (TestUnitClientUtilsImpl.class);
	
	/** Colour for a transparent pixel */
	private static final int TRANSPARENT = 0;
	
	/** Background colour for basic stats */
	private static final int BACKGROUND_BASIC = 0xFF800000;
	
	/** Background colour for weapon grade bonuses */
	private static final int BACKGROUND_WEAPON_GRADE = 0xFF00FF00;

	/** Background colour for experience bonuses */
	private static final int BACKGROUND_EXPERIENCE = 0xFF008000;

	/** Background colour for hero skill bonuses */
	private static final int BACKGROUND_HERO_SKILLS = 0xFF0000FF;
	
	/** Background colour for CAE bonuses */
	private static final int BACKGROUND_CAE = 0xFFEE0000;
	
	/** Colour for a skill icon pixel */
	private static final int SKILL_ICON = 0xFF00EE00;
	
	/**
	 * Tests the getUnitName method
	 * @throws RecordNotFoundException If we can't find the unit definition in the server XML
	 */
	@Test
	public final void testGetUnitName () throws RecordNotFoundException
	{
		// Mock entries from client DB
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		for (int n = 1; n <= 7; n++)
		{
			final Unit unitInfo = new Unit ();
			
			if ((n == 3) || (n == 7))
				unitInfo.setIncludeRaceInUnitName (true);
			
			if ((n == 3) || (n == 4))
				unitInfo.setUnitRaceID ("RC01");
			else if (n == 7)
				unitInfo.setUnitRaceID ("RC02");
				
			when (db.findUnit ("UN00" + n, "getUnitName")).thenReturn (unitInfo);
		}
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);

		// Mock entries from the language XML
		final LanguageDatabaseEx lang = mock (LanguageDatabaseEx.class);
		
		int n = 0;
		for (final String unitName : new String [] {"Bard", "Trireme", "Swordsmen", "Longbowmen", "Magic Spirit", "Hell Hounds"})
		{
			n++;
			final UnitLang unitLang = new UnitLang ();
			unitLang.setUnitName (unitName);
			
			if ((n == 2) || (n == 5))
				unitLang.setUnitNamePrefix ("a"); 
			
			when (lang.findUnit ("UN00" + n)).thenReturn (unitLang);
		}
		
		final RaceLang race = new RaceLang ();
		race.setRaceName ("Orc");
		when (lang.findRace ("RC01")).thenReturn (race);
		
		when (lang.findCategoryEntry ("UnitName", "TheUnitOfNameSingular")).thenReturn ("the RACE_UNIT_NAME");
		when (lang.findCategoryEntry ("UnitName", "TheUnitOfNamePlural")).thenReturn ("the unit of RACE_UNIT_NAME");
		
		when (lang.findHeroName ("UN001_HN01")).thenReturn ("Valana the Bard");				
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
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
		assertEquals ("UN007",				utils.getUnitName (unknown,					UnitNameType.SIMPLE_UNIT_NAME));

		// Test RACE_UNIT_NAME
		assertEquals ("Renamed Hero",	utils.getUnitName (heroOverridden,			UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Valana the Bard",	utils.getUnitName (heroGenerated,			UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Trireme",				utils.getUnitName (nonRaceSpecific,			UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.RACE_UNIT_NAME));
		assertEquals ("RC02 UN007",		utils.getUnitName (unknown,					UnitNameType.RACE_UNIT_NAME));

		// Test A_UNIT_NAME
		assertEquals ("Renamed Hero",	utils.getUnitName (heroOverridden,			UnitNameType.A_UNIT_NAME));
		assertEquals ("Valana the Bard",	utils.getUnitName (heroGenerated,			UnitNameType.A_UNIT_NAME));
		assertEquals ("a Trireme",			utils.getUnitName (nonRaceSpecific,			UnitNameType.A_UNIT_NAME));
		assertEquals ("Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.A_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.A_UNIT_NAME));
		assertEquals ("a Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.A_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.A_UNIT_NAME));
		assertEquals ("RC02 UN007",		utils.getUnitName (unknown,					UnitNameType.A_UNIT_NAME));

		// Test THE_UNIT_OF_NAME
		assertEquals ("Renamed Hero",					utils.getUnitName (heroOverridden,			UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("Valana the Bard",					utils.getUnitName (heroGenerated,			UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the Trireme",						utils.getUnitName (nonRaceSpecific,			UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the Magic Spirit",					utils.getUnitName (summonedSingular,		UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of RC02 UN007",		utils.getUnitName (unknown,					UnitNameType.THE_UNIT_OF_NAME));
	}
	
	/**
	 * Tests the getUnitSkillComponentBreakdownIcon method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnitSkillComponentBreakdownIcon () throws Exception
	{
		// Unit def
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit unitDef = new Unit ();
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit ("UN001", "getUnitSkillComponentBreakdownIcon")).thenReturn (unitDef);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage plusToHitImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("plusToHit.png")).thenReturn (plusToHitImage);

		final BufferedImage meleeWepGrade1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("melee1.png")).thenReturn (meleeWepGrade1Image);
		
		final BufferedImage meleeWepGrade2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("melee2.png")).thenReturn (meleeWepGrade2Image);
		
		final BufferedImage meleeWepGrade3Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("melee3.png")).thenReturn (meleeWepGrade3Image);

		final BufferedImage rat1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("rat1.png")).thenReturn (rat1Image);

		final BufferedImage rat2wepGrade1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("rat2-1.png")).thenReturn (rat2wepGrade1Image);
		
		final BufferedImage rat2wepGrade2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("rat2-2.png")).thenReturn (rat2wepGrade2Image);
		
		final BufferedImage rat2wepGrade3Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("rat2-3.png")).thenReturn (rat2wepGrade3Image);
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		// + to hit doesn't vary by weapon grade
		final UnitSkillGfx plusToHit = new UnitSkillGfx ();

		final UnitSkillWeaponGradeGfx plusToHitIcon = new UnitSkillWeaponGradeGfx ();
		plusToHitIcon.setSkillImageFile ("plusToHit.png");
		plusToHit.getUnitSkillWeaponGrade ().add (plusToHitIcon);
		
		plusToHit.buildMap ();;
		when (gfx.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT, "getUnitSkillComponentBreakdownIcon")).thenReturn (plusToHit);
		
		// melee varies by weapon grade
		final UnitSkillGfx melee = new UnitSkillGfx ();

		for (int wepGrade = 1; wepGrade <= 3; wepGrade++)
		{
			final UnitSkillWeaponGradeGfx meleeIcon = new UnitSkillWeaponGradeGfx ();
			meleeIcon.setSkillImageFile ("melee" + wepGrade + ".png");
			meleeIcon.setWeaponGradeNumber (wepGrade);
			melee.getUnitSkillWeaponGrade ().add (meleeIcon);
		}
		
		melee.buildMap ();
		when (gfx.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "getUnitSkillComponentBreakdownIcon")).thenReturn (melee);
		
		// RAT that doesn't vary by weapon grade
		final RangedAttackTypeGfx rat1 = new RangedAttackTypeGfx ();
		when (gfx.findRangedAttackType ("RAT01", "getUnitSkillComponentBreakdownIcon")).thenReturn (rat1);

		final RangedAttackTypeWeaponGradeGfx rat1Icon = new RangedAttackTypeWeaponGradeGfx ();
		rat1Icon.setUnitDisplayRangedImageFile ("rat1.png");
		rat1.getRangedAttackTypeWeaponGrade ().add (rat1Icon);
		
		rat1.buildMap ();
		
		// Dummy unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		unit.setWeaponGrade (2);
		
		// Set up object to test
		final UnitClientUtilsImpl unitUtils = new UnitClientUtilsImpl ();
		unitUtils.setUtils (utils);
		unitUtils.setGraphicsDB (gfx);
		unitUtils.setClient (client);
		
		// Run tests
		assertSame (plusToHitImage, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
		assertSame (meleeWepGrade2Image, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK));
		assertSame (rat1Image, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));

		// RAT that does vary by weapon grade
		unitDef.setRangedAttackType ("RAT02");

		final RangedAttackTypeGfx rat2 = new RangedAttackTypeGfx ();
		when (gfx.findRangedAttackType ("RAT02", "getUnitSkillComponentBreakdownIcon")).thenReturn (rat2);

		for (int wepGrade = 1; wepGrade <= 3; wepGrade++)
		{
			final RangedAttackTypeWeaponGradeGfx rat2Icon = new RangedAttackTypeWeaponGradeGfx ();
			rat2Icon.setUnitDisplayRangedImageFile ("rat2-" + wepGrade + ".png");
			rat2Icon.setWeaponGradeNumber (wepGrade);
			rat2.getRangedAttackTypeWeaponGrade ().add (rat2Icon);
		}
		
		rat2.buildMap ();

		assertSame (rat2wepGrade2Image, unitUtils.getUnitSkillComponentBreakdownIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));
	}
	
	/**
	 * Tests the getUnitSkillSingleIcon method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnitSkillSingleIcon () throws Exception
	{
		// Unit def
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "getUnitSkillSingleIcon")).thenReturn (unitDef);
		
		final Pick unitMagicRealm = new Pick ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findPick (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, "getUnitSkillSingleIcon")).thenReturn (unitMagicRealm);

		// Mock some images
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final BufferedImage skillImage = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("skill.png")).thenReturn (skillImage);

		final BufferedImage exp0Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("exp0.png")).thenReturn (exp0Image);
		
		final BufferedImage exp1Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("exp1.png")).thenReturn (exp1Image);
		
		final BufferedImage exp2Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("exp2.png")).thenReturn (exp2Image);
		
		final BufferedImage exp3Image = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("exp3.png")).thenReturn (exp3Image);
		
		// Mock entries from graphics DB
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		
		final UnitSkillGfx skillGfx = new UnitSkillGfx ();
		skillGfx.setUnitSkillImageFile ("skill.png");
		when (gfx.findUnitSkill ("US001", "getUnitSkillSingleIcon")).thenReturn (skillGfx);

		final UnitTypeGfx unitType = new UnitTypeGfx ();
		
		for (int n = 0; n < 4; n++)
		{
			final ExperienceLevelGfx expLvl = new ExperienceLevelGfx ();
			expLvl.setLevelNumber (n);
			expLvl.setExperienceLevelImageFile ("exp" + n + ".png");
			unitType.getExperienceLevel ().add (expLvl);
		}
		
		unitType.buildMap ();
		
		when (gfx.findUnitType ("N", "getUnitSkillSingleIcon")).thenReturn (unitType);
		
		// Player list
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getClientDB ()).thenReturn (db);
		
		// FOW memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Dummy unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Experience
		final ExperienceLevel expLvl = new ExperienceLevel ();
		expLvl.setLevelNumber (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getExperienceLevel (unit, true, players, fow.getCombatAreaEffect (), db)).thenReturn (expLvl);
		
		// Set up object to test
		final UnitClientUtilsImpl unitClientUtils = new UnitClientUtilsImpl ();
		unitClientUtils.setGraphicsDB (gfx);
		unitClientUtils.setUnitUtils (unitUtils);
		unitClientUtils.setUtils (utils);
		unitClientUtils.setClient (client);

		// Run tests
		assertSame (skillImage, unitClientUtils.getUnitSkillSingleIcon (unit, "US001"));
		assertSame (exp2Image, unitClientUtils.getUnitSkillSingleIcon (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
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
		
		// This is dependant on way too many values to mock them all - so use the real graphics DB
		final GraphicsDatabaseEx gfx = ClientTestData.loadGraphicsDatabase (utils, null);
		
		// Animation controller
		final AnimationControllerImpl anim = new AnimationControllerImpl ();
		anim.setGraphicsDB (gfx);
		anim.setUtils (utils);
		
		// Config
		final MomImeClientConfigEx config = new MomImeClientConfigEx (); 
		
		// Set up object to test
		final UnitClientUtilsImpl unitUtils = new UnitClientUtilsImpl ();
		unitUtils.setAnim (anim);
		unitUtils.setGraphicsDB (gfx);
		unitUtils.setUtils (utils);
		unitUtils.setClientConfig (config);
		
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
					
					int y = 40;
					for (final UnitCombatScale scale : UnitCombatScale.values ())
					{
						config.setUnitCombatScale (scale);
						unitUtils.drawUnitFigures ("UN106", null, 6, 6, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 10, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null);
						unitUtils.drawUnitFigures ("UN075", null, 2, 2, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 80, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null);
						unitUtils.drawUnitFigures ("UN035", null, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 150, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null);
						unitUtils.drawUnitFigures ("UN197", CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 220, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true, 0, null);
						unitUtils.drawUnitFigures ("UN037", null, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, zOrderGraphics, 290, y, GraphicsDatabaseConstants.SAMPLE_OCEAN_TILE, true, 0, null);
						
						y = y + 80;
					}
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
	 * Tests the calculateWalkTiming method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateWalkTiming () throws Exception
	{
		// Mock entries from client database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setUnitTypeID ("X");
		when (db.findPick ("N", "calculateWalkTiming")).thenReturn (normalUnits);				

		final Pick summonedUnits = new Pick ();
		summonedUnits.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		when (db.findPick ("S", "calculateWalkTiming")).thenReturn (summonedUnits);				
		
		// Unit definitions
		final Unit regularUnitDef = new Unit ();
		regularUnitDef.setUnitMagicRealm ("N");
		when (db.findUnit ("UN001", "calculateWalkTiming")).thenReturn (regularUnitDef);
		
		final Unit summonedMultipleDef = new Unit ();
		summonedMultipleDef.setUnitMagicRealm ("S");
		when (db.findUnit ("UN002", "calculateWalkTiming")).thenReturn (summonedMultipleDef);
		
		final Unit summonedSingleDef = new Unit ();
		summonedSingleDef.setUnitMagicRealm ("S");
		when (db.findUnit ("UN003", "calculateWalkTiming")).thenReturn (summonedSingleDef);
		
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		
		// Mock figure counts
		final UnitUtils unitUtils = mock (UnitUtils.class);

		when (unitUtils.getFullFigureCount (regularUnitDef)).thenReturn (1);
		when (unitUtils.getFullFigureCount (summonedMultipleDef)).thenReturn (2);
		when (unitUtils.getFullFigureCount (summonedSingleDef)).thenReturn (1);
		
		// Config
		final MomImeClientConfigEx config = new MomImeClientConfigEx ();

		// Regular unit (which just happens to only have 1 figure, e.g. a steam cannon)
		final AvailableUnit regularUnit = new AvailableUnit ();
		regularUnit.setUnitID ("UN001");
		
		// Summoned unit with multiple figures, e.g. hell hounds
		final AvailableUnit summonedMultiple = new AvailableUnit ();
		summonedMultiple.setUnitID ("UN002");
		
		// Summoned unit with single figure, e.g. storm giant
		final AvailableUnit summonedSingle = new AvailableUnit ();
		summonedSingle.setUnitID ("UN003");
		
		// Set up object to test
		final UnitClientUtilsImpl obj = new UnitClientUtilsImpl ();
		obj.setClient (client);
		obj.setClientConfig (config);
		obj.setUnitUtils (unitUtils);
		
		// Test double size units scale
		config.setUnitCombatScale (UnitCombatScale.DOUBLE_SIZE_UNITS);
		assertEquals (1, obj.calculateWalkTiming (regularUnit), 0.0001);
		assertEquals (1, obj.calculateWalkTiming (summonedMultiple), 0.0001);
		assertEquals (1, obj.calculateWalkTiming (summonedSingle), 0.0001);

		// Test 4x figures scale
		config.setUnitCombatScale (UnitCombatScale.FOUR_TIMES_FIGURES);
		assertEquals (2, obj.calculateWalkTiming (regularUnit), 0.0001);
		assertEquals (2, obj.calculateWalkTiming (summonedMultiple), 0.0001);
		assertEquals (2, obj.calculateWalkTiming (summonedSingle), 0.0001);

		// Test 4x figures scale except single summoned units scale
		config.setUnitCombatScale (UnitCombatScale.FOUR_TIMES_FIGURES_EXCEPT_SINGLE_SUMMONED);
		assertEquals (2, obj.calculateWalkTiming (regularUnit), 0.0001);
		assertEquals (2, obj.calculateWalkTiming (summonedMultiple), 0.0001);
		assertEquals (1, obj.calculateWalkTiming (summonedSingle), 0.0001);
	}
	
	/**
	 * Tests the playCombatActionSound method where a unit has no specific sound defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayCombatActionSound_Default () throws Exception
	{
		// Mock entries from graphics DB
		final UnitCombatActionGfx unitCombatAction = new UnitCombatActionGfx ();
		unitCombatAction.setCombatActionID ("X");
		
		final UnitGfx unitGfx = new UnitGfx ();
		unitGfx.getUnitCombatAction ().add (unitCombatAction);
		unitGfx.buildMap ();
		
		final CombatActionGfx defaultAction = new CombatActionGfx ();
		defaultAction.setDefaultActionSoundFile ("DefaultActionSound.mp3");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findUnit ("UN001", "playCombatActionSound")).thenReturn (unitGfx);
		when (gfx.findCombatAction ("X", "playCombatActionSound")).thenReturn (defaultAction);
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final AudioPlayer soundPlayer = mock (AudioPlayer.class);
		
		final UnitClientUtilsImpl obj = new UnitClientUtilsImpl ();
		obj.setSoundPlayer (soundPlayer);
		obj.setGraphicsDB (gfx);
		
		// Run method
		obj.playCombatActionSound (unit, "X");
		
		// Check results
		verify (soundPlayer).playAudioFile ("DefaultActionSound.mp3");
	}

	/**
	 * Tests the playCombatActionSound method where a unit has a specific sound defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlayCombatActionSound_Override () throws Exception
	{
		// Mock entries from graphics DB
		final UnitCombatActionGfx unitCombatAction = new UnitCombatActionGfx ();
		unitCombatAction.setCombatActionID ("X");
		unitCombatAction.setOverrideActionSoundFile ("OverrideActionSound.mp3");
		
		final UnitGfx unitGfx = new UnitGfx ();
		unitGfx.getUnitCombatAction ().add (unitCombatAction);
		unitGfx.buildMap ();
		
		final CombatActionGfx defaultAction = new CombatActionGfx ();
		defaultAction.setDefaultActionSoundFile ("DefaultActionSound.mp3");
		
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		when (gfx.findUnit ("UN001", "playCombatActionSound")).thenReturn (unitGfx);
		when (gfx.findCombatAction ("X", "playCombatActionSound")).thenReturn (defaultAction);
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final AudioPlayer soundPlayer = mock (AudioPlayer.class);
		
		final UnitClientUtilsImpl obj = new UnitClientUtilsImpl ();
		obj.setSoundPlayer (soundPlayer);
		obj.setGraphicsDB (gfx);
		
		// Run method
		obj.playCombatActionSound (unit, "X");
		
		// Check results
		verify (soundPlayer).playAudioFile ("OverrideActionSound.mp3");
	}
	
	/**
	 * Tests the generateAttributeImage method
	 * @throws IOException If there is a problem loading any of the images
	 */
	@Test
	public final void testGenerateAttributeImage () throws IOException
	{
		// Mock database
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);

		// Mock graphics
		final GraphicsDatabaseEx gfx = mock (GraphicsDatabaseEx.class);
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		
		final UnitSkillWeaponGrade skillWeaponGradeGfx = new UnitSkillWeaponGrade ();
		skillWeaponGradeGfx.setSkillImageFile ("s.png");
		
		final UnitSkillGfx skillGfx = new UnitSkillGfx ();
		skillGfx.getUnitSkillWeaponGrade ().add (skillWeaponGradeGfx);
		when (gfx.findUnitSkill ("UA01", "getUnitSkillComponentBreakdownIcon")).thenReturn (skillGfx);
		when (utils.loadImage ("s.png")).thenReturn (ClientTestData.createSolidImage (1, 1, SKILL_ICON));
		
		// Attribute component backgrounds
		final UnitSkillComponentImageGfx basicComponentBackground = new UnitSkillComponentImageGfx ();
		basicComponentBackground.setUnitSkillComponentImageFile ("b.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.BASIC, "generateAttributeImage")).thenReturn (basicComponentBackground);
		when (utils.loadImage ("b.png")).thenReturn (ClientTestData.createSolidImage (2, 1, BACKGROUND_BASIC));

		final UnitSkillComponentImageGfx weaponGradeComponentBackground = new UnitSkillComponentImageGfx ();
		weaponGradeComponentBackground.setUnitSkillComponentImageFile ("w.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.WEAPON_GRADE, "generateAttributeImage")).thenReturn (weaponGradeComponentBackground);
		when (utils.loadImage ("w.png")).thenReturn (ClientTestData.createSolidImage (2, 1, BACKGROUND_WEAPON_GRADE));

		final UnitSkillComponentImageGfx experienceComponentBackground = new UnitSkillComponentImageGfx ();
		experienceComponentBackground.setUnitSkillComponentImageFile ("e.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.EXPERIENCE, "generateAttributeImage")).thenReturn (experienceComponentBackground);
		when (utils.loadImage ("e.png")).thenReturn (ClientTestData.createSolidImage (2, 1, BACKGROUND_EXPERIENCE));

		final UnitSkillComponentImageGfx heroSkillsComponentBackground = new UnitSkillComponentImageGfx ();
		heroSkillsComponentBackground.setUnitSkillComponentImageFile ("h.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.HERO_SKILLS, "generateAttributeImage")).thenReturn (heroSkillsComponentBackground);
		when (utils.loadImage ("h.png")).thenReturn (ClientTestData.createSolidImage (2, 1, BACKGROUND_HERO_SKILLS));

		final UnitSkillComponentImageGfx caeComponentBackground = new UnitSkillComponentImageGfx ();
		caeComponentBackground.setUnitSkillComponentImageFile ("c.png");
		when (gfx.findUnitSkillComponent (UnitSkillComponent.COMBAT_AREA_EFFECTS, "generateAttributeImage")).thenReturn (caeComponentBackground);
		when (utils.loadImage ("c.png")).thenReturn (ClientTestData.createSolidImage (2, 1, BACKGROUND_CAE));
		
		// Player's memory
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (fow);
		
		// Player's list
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		// Unit
		final AvailableUnit unit = new AvailableUnit ();
		
		// Unit stats
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);

		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getClientDB ()).thenReturn (db);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Set up object to test
		final UnitClientUtilsImpl obj = new UnitClientUtilsImpl ();
		obj.setClient (client);
		obj.setUtils (utils);
		obj.setGraphicsDB (gfx);
		obj.setUnitUtils (unitUtils);
		obj.setUnitSkillUtils (unitSkillUtils);
		
		// Generate image for when the unit doesn't have the attribute at all
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "UA01")).thenReturn (-1);
		assertNull (obj.generateAttributeImage (unit, "UA01"));

		// Generate image for when the unit has a zero value of the attribute
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "UA01")).thenReturn (0);
		assertNull (obj.generateAttributeImage (unit, "UA01"));
		
		// Generate image for when the unit has a single basic stat value of 1
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "UA01")).thenReturn (1);
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (1);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			null, null, players, fow, db)).thenReturn (1);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.BASIC, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (1);		
		
		checkImage (obj.generateAttributeImage (unit, "UA01"),
			"SB                                                                   " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     ");

		// Generate image for when the unit has a makeup of 3 basic + 4 wep grade + 5 experience + 6 hero skills + 2 CAE = 20
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (20);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			null, null, players, fow, db)).thenReturn (20);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.BASIC, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (3);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.WEAPON_GRADE, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (4);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (5);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.HERO_SKILLS, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (6);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (2);		

		checkImage (obj.generateAttributeImage (unit, "UA01"),
			"SB SB SB SW SW   SW SW SE SE SE   SE SE SH SH SH   SH SH SH SC SC    " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     " + System.lineSeparator () +
			"                                                                     ");

		// Generate image for when the unit has a makeup of 4 basic + 5 wep grade + 7 experience + 8 hero skills + 3 CAE = 27 - 6 penalty = 21
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (27);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
			null, null, players, fow, db)).thenReturn (21);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.BASIC, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (4);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.WEAPON_GRADE, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (5);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.EXPERIENCE, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (7);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.HERO_SKILLS, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (8);		
		when (unitSkillUtils.getModifiedSkillValue (unit, unit.getUnitHasSkill (), "UA01", null, UnitSkillComponent.COMBAT_AREA_EFFECTS, UnitSkillPositiveNegative.POSITIVE,
			null, null, players, fow, db)).thenReturn (3);		

		checkImage (obj.generateAttributeImage (unit, "UA01"),
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
				
				assertEquals ("Mismatching colour at " + x + ", " + y, expectedColour, image.getRGB (x, y));
			}
	}
}
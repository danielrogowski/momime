package momime.client.utils;

import static org.junit.Assert.assertEquals;
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

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.config.MomImeClientConfigEx;
import momime.client.config.v0_9_6.UnitCombatScale;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.CombatActionGfx;
import momime.client.graphics.database.ExperienceLevelGfx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.RangedAttackTypeGfx;
import momime.client.graphics.database.RangedAttackTypeWeaponGradeGfx;
import momime.client.graphics.database.UnitAttributeGfx;
import momime.client.graphics.database.UnitAttributeWeaponGradeGfx;
import momime.client.graphics.database.UnitCombatActionGfx;
import momime.client.graphics.database.UnitGfx;
import momime.client.graphics.database.UnitSkillGfx;
import momime.client.graphics.database.UnitTypeGfx;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.RaceLang;
import momime.client.language.database.UnitLang;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitMagicRealm;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;
import com.ndg.swing.NdgUIUtilsImpl;

/**
 * Tests the UnitClientUtilsImpl class
 */
public final class TestUnitClientUtilsImpl
{
	/** Class logger */
	private final Log log = LogFactory.getLog (TestUnitClientUtilsImpl.class);
	
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
		
		final LanguageDatabaseHolder langHolder = new LanguageDatabaseHolder ();
		langHolder.setLanguage (lang);
		
		// Create one of each kind of unit
		final MemoryUnit hero = new MemoryUnit ();
		hero.setUnitID ("UN001");
		hero.setUnitName ("Valana the Bard");
		
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
		assertEquals ("Valana the Bard",	utils.getUnitName (hero,							UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Trireme",				utils.getUnitName (nonRaceSpecific,			UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Swordsmen",		utils.getUnitName (raceSpecific,				UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.SIMPLE_UNIT_NAME));
		assertEquals ("UN007",				utils.getUnitName (unknown,					UnitNameType.SIMPLE_UNIT_NAME));

		// Test RACE_UNIT_NAME
		assertEquals ("Valana the Bard",	utils.getUnitName (hero,							UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Trireme",				utils.getUnitName (nonRaceSpecific,			UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.RACE_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.RACE_UNIT_NAME));
		assertEquals ("RC02 UN007",		utils.getUnitName (unknown,					UnitNameType.RACE_UNIT_NAME));

		// Test A_UNIT_NAME
		assertEquals ("Valana the Bard",	utils.getUnitName (hero,							UnitNameType.A_UNIT_NAME));
		assertEquals ("a Trireme",			utils.getUnitName (nonRaceSpecific,			UnitNameType.A_UNIT_NAME));
		assertEquals ("Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.A_UNIT_NAME));
		assertEquals ("Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.A_UNIT_NAME));
		assertEquals ("a Magic Spirit",		utils.getUnitName (summonedSingular,		UnitNameType.A_UNIT_NAME));
		assertEquals ("Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.A_UNIT_NAME));
		assertEquals ("RC02 UN007",		utils.getUnitName (unknown,					UnitNameType.A_UNIT_NAME));

		// Test THE_UNIT_OF_NAME
		assertEquals ("Valana the Bard",					utils.getUnitName (hero,							UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the Trireme",						utils.getUnitName (nonRaceSpecific,			UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Orc Swordsmen",	utils.getUnitName (raceSpecific,				UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Longbowmen",		utils.getUnitName (raceUnique,				UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the Magic Spirit",					utils.getUnitName (summonedSingular,		UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of Hell Hounds",		utils.getUnitName (summonedPlural,		UnitNameType.THE_UNIT_OF_NAME));
		assertEquals ("the unit of RC02 UN007",		utils.getUnitName (unknown,					UnitNameType.THE_UNIT_OF_NAME));
	}
	
	/**
	 * Tests the getUnitAttributeIcon method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnitAttributeIcon () throws Exception
	{
		// Unit def
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit unitDef = new Unit ();
		unitDef.setRangedAttackType ("RAT01");
		when (db.findUnit ("UN001", "getUnitAttributeIcon")).thenReturn (unitDef);
		
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
		final UnitAttributeGfx plusToHit = new UnitAttributeGfx ();

		final UnitAttributeWeaponGradeGfx plusToHitIcon = new UnitAttributeWeaponGradeGfx ();
		plusToHitIcon.setAttributeImageFile ("plusToHit.png");
		plusToHit.getUnitAttributeWeaponGrade ().add (plusToHitIcon);
		
		plusToHit.buildMap ();;
		when (gfx.findUnitAttribute (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT, "getUnitAttributeIcon")).thenReturn (plusToHit);
		
		// melee varies by weapon grade
		final UnitAttributeGfx melee = new UnitAttributeGfx ();

		for (int wepGrade = 1; wepGrade <= 3; wepGrade++)
		{
			final UnitAttributeWeaponGradeGfx meleeIcon = new UnitAttributeWeaponGradeGfx ();
			meleeIcon.setAttributeImageFile ("melee" + wepGrade + ".png");
			meleeIcon.setWeaponGradeNumber (wepGrade);
			melee.getUnitAttributeWeaponGrade ().add (meleeIcon);
		}
		
		melee.buildMap ();
		when (gfx.findUnitAttribute (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "getUnitAttributeIcon")).thenReturn (melee);
		
		// RAT that doesn't vary by weapon grade
		final RangedAttackTypeGfx rat1 = new RangedAttackTypeGfx ();
		when (gfx.findRangedAttackType ("RAT01", "getUnitAttributeIcon")).thenReturn (rat1);

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
		assertSame (plusToHitImage, unitUtils.getUnitAttributeIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT));
		assertSame (meleeWepGrade2Image, unitUtils.getUnitAttributeIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK));
		assertSame (rat1Image, unitUtils.getUnitAttributeIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));

		// RAT that does vary by weapon grade
		unitDef.setRangedAttackType ("RAT02");

		final RangedAttackTypeGfx rat2 = new RangedAttackTypeGfx ();
		when (gfx.findRangedAttackType ("RAT02", "getUnitAttributeIcon")).thenReturn (rat2);

		for (int wepGrade = 1; wepGrade <= 3; wepGrade++)
		{
			final RangedAttackTypeWeaponGradeGfx rat2Icon = new RangedAttackTypeWeaponGradeGfx ();
			rat2Icon.setUnitDisplayRangedImageFile ("rat2-" + wepGrade + ".png");
			rat2Icon.setWeaponGradeNumber (wepGrade);
			rat2.getRangedAttackTypeWeaponGrade ().add (rat2Icon);
		}
		
		rat2.buildMap ();

		assertSame (rat2wepGrade2Image, unitUtils.getUnitAttributeIcon (unit, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK));
	}
	
	/**
	 * Tests the getUnitSkillIcon method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetUnitSkillIcon () throws Exception
	{
		// Unit def
		final ClientDatabaseEx db = mock (ClientDatabaseEx.class);
		
		final Unit unitDef = new Unit ();
		unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		when (db.findUnit ("UN001", "getUnitSkillIcon")).thenReturn (unitDef);
		
		final UnitMagicRealm unitMagicRealm = new UnitMagicRealm ();
		unitMagicRealm.setUnitTypeID ("N");
		when (db.findUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, "getUnitSkillIcon")).thenReturn (unitMagicRealm);

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
		when (gfx.findUnitSkill ("US001", "getUnitSkillIcon")).thenReturn (skillGfx);

		final UnitTypeGfx unitType = new UnitTypeGfx ();
		
		for (int n = 0; n < 4; n++)
		{
			final ExperienceLevelGfx expLvl = new ExperienceLevelGfx ();
			expLvl.setLevelNumber (n);
			expLvl.setExperienceLevelImageFile ("exp" + n + ".png");
			unitType.getExperienceLevel ().add (expLvl);
		}
		
		unitType.buildMap ();
		
		when (gfx.findUnitType ("N", "getUnitSkillIcon")).thenReturn (unitType);
		
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
		assertSame (skillImage, unitClientUtils.getUnitSkillIcon (unit, "US001"));
		assertSame (exp2Image, unitClientUtils.getUnitSkillIcon (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE));
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
		
		final JPanel panel = new JPanel ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				super.paintComponent (g);
				try
				{
					int y = 40;
					for (final UnitCombatScale scale : UnitCombatScale.values ())
					{
						config.setUnitCombatScale (scale);
						unitUtils.drawUnitFigures ("UN106", null, 6, 6, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 10, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true);
						unitUtils.drawUnitFigures ("UN075", null, 2, 2, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 80, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true);
						unitUtils.drawUnitFigures ("UN035", null, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 150, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true);
						unitUtils.drawUnitFigures ("UN197", CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 220, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE, true);
						unitUtils.drawUnitFigures ("UN037", null, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 290, y, GraphicsDatabaseConstants.SAMPLE_OCEAN_TILE, true);
						
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
		
		final UnitMagicRealm normalUnits = new UnitMagicRealm ();
		normalUnits.setUnitTypeID ("X");
		when (db.findUnitMagicRealm ("N", "calculateWalkTiming")).thenReturn (normalUnits);				

		final UnitMagicRealm summonedUnits = new UnitMagicRealm ();
		summonedUnits.setUnitTypeID (CommonDatabaseConstants.UNIT_TYPE_ID_SUMMONED);
		when (db.findUnitMagicRealm ("S", "calculateWalkTiming")).thenReturn (summonedUnits);				
		
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
}
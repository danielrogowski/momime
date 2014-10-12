package momime.client.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import momime.client.ClientTestData;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.config.v0_9_5.MomImeClientConfig;
import momime.client.config.v0_9_5.UnitCombatScale;
import momime.client.database.ClientDatabaseEx;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.UnitCombatActionEx;
import momime.client.graphics.database.UnitEx;
import momime.client.graphics.database.v0_9_5.CombatAction;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.Race;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.Unit;
import momime.common.database.v0_9_5.UnitMagicRealm;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

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
			final momime.client.language.database.v0_9_5.Unit unitLang = new momime.client.language.database.v0_9_5.Unit ();
			unitLang.setUnitName (unitName);
			
			if ((n == 2) || (n == 5))
				unitLang.setUnitNamePrefix ("a"); 
			
			when (lang.findUnit ("UN00" + n)).thenReturn (unitLang);
		}
		
		final Race race = new Race ();
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
		final MomImeClientConfig config = new MomImeClientConfig (); 
		
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
			private static final long serialVersionUID = 2679278561186067446L;

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
						unitUtils.drawUnitFigures ("UN106", null, 6, 6, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 10, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE);
						unitUtils.drawUnitFigures ("UN075", null, 2, 2, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 80, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE);
						unitUtils.drawUnitFigures ("UN035", null, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 150, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE);
						unitUtils.drawUnitFigures ("UN197", CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 220, y, GraphicsDatabaseConstants.SAMPLE_GRASS_TILE);
						unitUtils.drawUnitFigures ("UN037", null, 1, 1, GraphicsDatabaseConstants.UNIT_COMBAT_ACTION_WALK, 4, g, 290, y, GraphicsDatabaseConstants.SAMPLE_OCEAN_TILE);
						
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
		summonedUnits.setUnitTypeID (CommonDatabaseConstants.VALUE_UNIT_TYPE_ID_SUMMONED);
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
		final MomImeClientConfig config = new MomImeClientConfig ();

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
		final UnitCombatActionEx unitCombatAction = new UnitCombatActionEx ();
		unitCombatAction.setCombatActionID ("X");
		
		final UnitEx unitGfx = new UnitEx ();
		unitGfx.getUnitCombatAction ().add (unitCombatAction);
		unitGfx.buildMap ();
		
		final CombatAction defaultAction = new CombatAction ();
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
		final UnitCombatActionEx unitCombatAction = new UnitCombatActionEx ();
		unitCombatAction.setCombatActionID ("X");
		unitCombatAction.setOverrideActionSoundFile ("OverrideActionSound.mp3");
		
		final UnitEx unitGfx = new UnitEx ();
		unitGfx.getUnitCombatAction ().add (unitCombatAction);
		unitGfx.buildMap ();
		
		final CombatAction defaultAction = new CombatAction ();
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
package momime.server.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_5.UnitSettingData;
import momime.common.messages.servertoclient.v0_9_5.SetSpecialOrderMessage;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_5.OverlandMapCityData;
import momime.common.messages.v0_9_5.OverlandMapTerrainData;
import momime.common.messages.v0_9_5.UnitAddBumpTypeID;
import momime.common.messages.v0_9_5.UnitSpecialOrder;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.calculations.MomServerUnitCalculations;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_5.HeroName;
import momime.server.database.v0_9_5.TileType;
import momime.server.database.v0_9_5.Unit;
import momime.server.database.v0_9_5.UnitSkill;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Tests the UnitServerUtils class
 */
public final class TestUnitServerUtilsImpl
{
	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who gets no random rolled skills, so it generates a name only
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_NoExtras () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who gets one fighter pick, and there's 3 possible skills to choose
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_OnePick () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkill> possibleSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkill ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (3)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS01", 1);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS02", 1);
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS03", 1);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who gets one fighter pick, and there's 3 possible skills to choose
	 * One of them is "only if have already", and we do have the skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_OnePick_OnlyIfHaveAlready () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkill> possibleSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			skill.setOnlyIfHaveAlready (n==3);
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkill ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (3)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");

		// Set up existing skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "HS03")).thenReturn (1);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS01", 1);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS02", 1);
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS03", 1);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who only has no viable skills to pick (they are all "only if have already" and we have none of them)
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testGenerateHeroNameAndRandomSkills_NoChoices () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkill> possibleSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			skill.setOnlyIfHaveAlready (n <= 3);
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkill ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (3)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");

		// Set up existing skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method, providing that heroes with a defined hero random pick type can get skills with a blank hero random pick type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_SkillNotTypeSpecific () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkill> possibleSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 6; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : null);
			skill.setMaxOccurrences (10);
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkill ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (6)).thenReturn (1);		// Fix skill choice - note selects from all 6

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS01", 1);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS02", 1);
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS03", 1);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method, providing that heroes without a defined hero random pick type can get skills even if they do specify a hero random pick type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_HeroNotTypeSpecific () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType (null);	// <---
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkill> possibleSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 6; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkill ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (6)).thenReturn (1);		// Fix skill choice - note selects from all 6

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS01", 1);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS02", 1);
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS03", 1);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method where we add lots of adds into a single skill to prove it isn't capped
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Uncapped () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		unitDef.setHeroRandomPickCount (5);
		unitDef.setHeroRandomPickType ("M");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkill> possibleSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkill ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (2)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "HS05")).thenReturn (0, 0, 1, 1, 2, 2, 3, 3, 4, 4);	// Gets read twice each iteration
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (unitUtils, times (0)).setBasicSkillValue (unit, "HS04", 1);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 1);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 2);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 3);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 4);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 5);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method where we run out of skills to add to because they're all capped with a maxOccurrences value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Capped () throws Exception
	{
		// Mock unit detalis
		final Unit unitDef = new Unit ();
		unitDef.setHeroRandomPickCount (5);
		unitDef.setHeroRandomPickType ("M");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkill> possibleSkills = new ArrayList<UnitSkill> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkill skill = new UnitSkill ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			
			if (n == 5)
				skill.setMaxOccurrences (4);
			
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkill ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (2)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getBasicSkillValue (unit.getUnitHasSkill (), "HS05")).thenReturn (0, 0, 1, 1, 2, 2, 3, 3, 4, 4);	// Gets read twice each iteration
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 1);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 2);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 3);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS05", 4);
		verify (unitUtils, times (1)).setBasicSkillValue (unit, "HS04", 1);		// Last point goes into HS04 because HS05 is maxed out
	}

	/**
	 * Tests the doesUnitSpecialOrderResultInDeath method
	 */
	@Test
	public final void testDoesUnitSpecialOrderResultInDeath ()
	{
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();

		// Run test
		assertFalse (utils.doesUnitSpecialOrderResultInDeath (null));
		assertFalse (utils.doesUnitSpecialOrderResultInDeath (UnitSpecialOrder.BUILD_ROAD));
		assertTrue (utils.doesUnitSpecialOrderResultInDeath (UnitSpecialOrder.BUILD_CITY));
	}
	
	/**
	 * Tests the setAndSendSpecialOrder method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetAndSendSpecialOrder () throws Exception
	{
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge (); 
		final MomTransientPlayerPrivateKnowledge trans = new MomTransientPlayerPrivateKnowledge ();

		priv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, trans);
		
		// Connection
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		player.setConnection (msgs);
		
		// Unit
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (5);

		final MemoryUnit memoryUnit = new MemoryUnit ();
		memoryUnit.setUnitURN (5);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (5, priv.getFogOfWarMemory ().getUnit (), "setAndSendSpecialOrder")).thenReturn (memoryUnit);
		
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setPendingMovementUtils (pendingMovementUtils);
		
		// Run test
		utils.setAndSendSpecialOrder (trueUnit, UnitSpecialOrder.BUILD_ROAD, player);
		
		// Check results
		assertEquals (UnitSpecialOrder.BUILD_ROAD, trueUnit.getSpecialOrder ());
		assertEquals (UnitSpecialOrder.BUILD_ROAD, memoryUnit.getSpecialOrder ());
		
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (trans.getPendingMovement (), 5);
		
		assertEquals (1, msgs.getMessages ().size ());
		final SetSpecialOrderMessage msg = (SetSpecialOrderMessage) msgs.getMessages ().get (0);
		assertEquals (1, msg.getUnitURN ().size ());
		assertEquals (5, msg.getUnitURN ().get (0).intValue ());
		assertEquals (UnitSpecialOrder.BUILD_ROAD, msg.getSpecialOrder ());
	}

	/**
	 * Tests the findUnitWithPlayerAndID method on a unit that does exist
	 * @throws RecordNotFoundException If unit with requested URN is not found
	 */
	@Test
	public final void testFindUnitWithPlayerAndID () throws RecordNotFoundException
	{
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit unit = new MemoryUnit ();
			unit.setOwningPlayerID (n);
			unit.setUnitID ("UN00" + new Integer (n+1).toString ());
			units.add (unit);
		}

		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();

		// Run test
		assertEquals (2, utils.findUnitWithPlayerAndID (units, 2, "UN003").getOwningPlayerID ());
		assertNull (utils.findUnitWithPlayerAndID (units, 2, "UN002"));
	}

	/**
	 * Tests the canUnitBeAddedHere method in the simplest situation where there's no other units already in the map cell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertTrue (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}

	/**
	 * Tests the canUnitBeAddedHere method when there's an enemy unit in the way
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_EnemyUnit () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (1);
		
		// Other units
		final MemoryUnit enemyUnit = new MemoryUnit ();
		enemyUnit.setOwningPlayerID (3);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (enemyUnit);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}

	/**
	 * Tests the canUnitBeAddedHere method when there's a stack of 8 of our units already there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_StackOf8 () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (1);
		
		// Other units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (ourUnit);
		when (unitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (8);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertTrue (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}

	/**
	 * Tests the canUnitBeAddedHere method when there's a stack of 9 of our units already there, so we can't fit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_StackOf9 () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (1);
		
		// Other units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (ourUnit);
		when (unitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (9);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}
	
	/**
	 * Tests the canUnitBeAddedHere method trying to add a unit onto a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_Node () throws Exception
	{
		// We ARE trying to add on top of a node
		final TileType tt = new TileType ();
		tt.setMagicRealmID ("A");		// <---
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}
	
	/**
	 * Tests the canUnitBeAddedHere method trying to add a unit onto terrain that's impassable to them
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_Impassable () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (null);		// <---
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}
	
	/**
	 * Tests the canUnitBeAddedHere method adding units into our own city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_OurCity () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityPopulation (1);
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertTrue (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}
	
	/**
	 * Tests the canUnitBeAddedHere method adding units into am enemy city (which must have no units in it, to have skipped previous checks)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_EnemyCity () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = ServerTestData.createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);		// <---
		cityData.setCityPopulation (1);
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Unit settings
		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		
		// Unit to try to add
		final AvailableUnit testUnit = new AvailableUnit ();
		testUnit.setOwningPlayerID (2);
		
		final List<String> testUnitSkills = new ArrayList<String> ();

		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", trueMap.getMaintainedSpell (), db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, testUnitSkills, trueMap, settings, db));
	}

	/**
	 * Tests the findNearestLocationWhereUnitCanBeAdded method in the simple situation where we do fit in the city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindNearestLocationWhereUnitCanBeAdded_City () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (ServerTestData.createMapSizeData ());

		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		sd.setUnitSetting (settings);
		
		// Map
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sd.getMapSize ()));
		
		// Map cell and surrounding terrain that we're trying to add to
		for (int x = -1; x <= 1; x++)
			for (int y = -1; y <= 1; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
				terrainData.setTileTypeID ("TT01");
				trueMap.getMap ().getPlane ().get (1).getRow ().get (10+y).getCell ().get (20+x).setTerrainData (terrainData);
			}
		
		// Unit can enter this type of tile
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (any (AvailableUnit.class), anyListOf (String.class), eq ("TT01"),
			anyListOf (MemoryMaintainedSpell.class), any (ServerDatabaseEx.class))).thenReturn (1);
		
		// Put 8 units in the city so we just fit
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (ourUnit);
		when (unitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (8);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// So we eventually end up being positioned down-right of our preferred location
		final UnitAddLocation result = utils.findNearestLocationWhereUnitCanBeAdded (addLocation, "UN001", 2, trueMap, sd, db);
		assertEquals (UnitAddBumpTypeID.CITY, result.getBumpType ());
		assertEquals (20, result.getUnitLocation ().getX ());
		assertEquals (10, result.getUnitLocation ().getY ());
		assertEquals (1, result.getUnitLocation ().getZ ());
	}
	
	/**
	 * Tests the findNearestLocationWhereUnitCanBeAdded method where we can't fit inside the city or the first few surrounding tiles
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindNearestLocationWhereUnitCanBeAdded_Bumped () throws Exception
	{
		// Two tile types are clear, the middle one is a node
		final TileType tt = new TileType ();
		
		final TileType tt2 = new TileType ();
		tt2.setMagicRealmID ("A");

		final TileType tt3 = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		when (db.findTileType ("TT02", "isNodeLairTower")).thenReturn (tt2);
		when (db.findTileType ("TT03", "isNodeLairTower")).thenReturn (tt3);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (ServerTestData.createMapSizeData ());

		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		sd.setUnitSetting (settings);
		
		// Map
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sd.getMapSize ()));
		
		// Map cell and surrounding terrain that we're trying to add to
		for (int x = -1; x <= 1; x++)
			for (int y = -1; y <= 1; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
				terrainData.setTileTypeID ("TT01");
				trueMap.getMap ().getPlane ().get (1).getRow ().get (10+y).getCell ().get (20+x).setTerrainData (terrainData);
			}
		
		// Unit can enter tiles TT01 and TT02, but TT03 is impassable
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (any (AvailableUnit.class), anyListOf (String.class), eq ("TT01"),
			anyListOf (MemoryMaintainedSpell.class), any (ServerDatabaseEx.class))).thenReturn (1);
		when (calc.calculateDoubleMovementToEnterTileType (any (AvailableUnit.class), anyListOf (String.class), eq ("TT02"),
			anyListOf (MemoryMaintainedSpell.class), any (ServerDatabaseEx.class))).thenReturn (1);
		when (calc.calculateDoubleMovementToEnterTileType (any (AvailableUnit.class), anyListOf (String.class), eq ("TT03"),
			anyListOf (MemoryMaintainedSpell.class), any (ServerDatabaseEx.class))).thenReturn (null);
		
		// Put 9 units in the city so we can't fit
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (ourUnit);
		when (unitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (9);
		
		// Put an enemy unit above us so we can't fit there either
		final MemoryUnit enemyUnit = new MemoryUnit ();
		enemyUnit.setOwningPlayerID (3);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 9, 1, 0)).thenReturn (enemyUnit);
		
		// Make the tile up-right of us a node so we can't fit there either
		trueMap.getMap ().getPlane ().get (1).getRow ().get (9).getCell ().get (21).getTerrainData ().setTileTypeID ("TT02");
		
		// Make the tile right of us something impassable (like water), so we can't fit there either
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (21).getTerrainData ().setTileTypeID ("TT03");

		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// So we eventually end up being positioned down-right of our preferred location
		final UnitAddLocation result = utils.findNearestLocationWhereUnitCanBeAdded (addLocation, "UN001", 2, trueMap, sd, db);
		assertEquals (UnitAddBumpTypeID.BUMPED, result.getBumpType ());
		assertEquals (21, result.getUnitLocation ().getX ());
		assertEquals (11, result.getUnitLocation ().getY ());
		assertEquals (1, result.getUnitLocation ().getZ ());
	}
	
	/**
	 * Tests the findNearestLocationWhereUnitCanBeAdded method where we can't fit either in the city or any of the 8 surrounding tiles
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindNearestLocationWhereUnitCanBeAdded_NoRoom () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileType tt = new TileType ();
		
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setMapSize (ServerTestData.createMapSizeData ());

		final UnitSettingData settings = new UnitSettingData ();
		settings.setUnitsPerMapCell (9);
		sd.setUnitSetting (settings);
		
		// Map
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (ServerTestData.createOverlandMap (sd.getMapSize ()));
		
		// Map cell and surrounding terrain that we're trying to add to
		for (int x = -1; x <= 1; x++)
			for (int y = -1; y <= 1; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
				terrainData.setTileTypeID ("TT01");
				trueMap.getMap ().getPlane ().get (1).getRow ().get (10+y).getCell ().get (20+x).setTerrainData (terrainData);
			}
		
		// Easiest thing to do is make the tile type impassable, then we can't fit anywhere
		final MomServerUnitCalculations calc = mock (MomServerUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (any (AvailableUnit.class), anyListOf (String.class), eq ("TT01"),
			anyListOf (MemoryMaintainedSpell.class), any (ServerDatabaseEx.class))).thenReturn (null);

		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setServerUnitCalculations (calc);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		addLocation.setX (20);
		addLocation.setY (10);
		addLocation.setZ (1);
		
		// So we eventually end up being positioned down-right of our preferred location
		final UnitAddLocation result = utils.findNearestLocationWhereUnitCanBeAdded (addLocation, "UN001", 2, trueMap, sd, db);
		assertEquals (UnitAddBumpTypeID.NO_ROOM, result.getBumpType ());
		assertNull (result.getUnitLocation ());
	}
	
	/**
	 * Tests the listUnitsWithSpecialOrder method
	 */
	@Test
	public final void testListUnitsWithSpecialOrder ()
	{
		// Set up some test units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		// Matching unit
		final MemoryUnit u1 = new MemoryUnit ();
		u1.setStatus (UnitStatusID.ALIVE);
		u1.setSpecialOrder (UnitSpecialOrder.BUILD_ROAD);
		units.add (u1);
		
		// Dead unit
		final MemoryUnit u2 = new MemoryUnit ();
		u2.setStatus (UnitStatusID.DEAD);
		u2.setSpecialOrder (UnitSpecialOrder.BUILD_ROAD);
		units.add (u2);
		
		// Wrong order
		final MemoryUnit u3 = new MemoryUnit ();
		u3.setStatus (UnitStatusID.ALIVE);
		u3.setSpecialOrder (UnitSpecialOrder.BUILD_CITY);
		units.add (u3);
		
		// Prove nulls don't crash it
		final MemoryUnit u4 = new MemoryUnit ();
		units.add (u4);
		
		// Another matching unit
		final MemoryUnit u5 = new MemoryUnit ();
		u5.setStatus (UnitStatusID.ALIVE);
		u5.setSpecialOrder (UnitSpecialOrder.BUILD_ROAD);
		units.add (u5);
		
		// Run method
		final List<MemoryUnit> out = new UnitServerUtilsImpl ().listUnitsWithSpecialOrder (units, UnitSpecialOrder.BUILD_ROAD);
		
		// Check results
		assertEquals (2, out.size ());
		assertSame (u1, out.get (0));
		assertSame (u5, out.get (1));
	}
}

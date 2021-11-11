package momime.server.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;
import com.ndg.utils.Holder;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.HeroName;
import momime.common.database.RecordNotFoundException;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSkillEx;
import momime.common.database.UnitSpecialOrder;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitAddBumpTypeID;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PendingMovementUtils;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

/**
 * Tests the UnitServerUtils class
 */
public final class TestUnitServerUtilsImpl extends ServerTestData
{
	/**
	 * Tests the describeBasicSkillValuesInDebugString method
	 */
	@Test
	public final void testDescribeBasicSkillValuesInDebugString ()
	{
		// Create test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (3);

		// Create skills list
		final UnitSkillAndValue skillWithValue = new UnitSkillAndValue ();
		skillWithValue.setUnitSkillID ("US001");
		skillWithValue.setUnitSkillValue (5);
		unit.getUnitHasSkill ().add (skillWithValue);

		final UnitSkillAndValue skillWithoutValue = new UnitSkillAndValue ();
		skillWithoutValue.setUnitSkillID ("US002");
		unit.getUnitHasSkill ().add (skillWithoutValue);

		// Run test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		assertEquals ("5xUS001, US002", utils.describeBasicSkillValuesInDebugString (unit));
	}
	
	/**
	 * Tests the createMemoryUnit method
	 * We don't really need to test all combinations of params, since that just affects the call to initializeUnitSkills, which we've already tested above
	 *
	 * @throws RecordNotFoundException If we can't find the unit, unit type or magic realm
	 */
	@Test
	public final void testCreateMemoryUnit () throws RecordNotFoundException
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx unitDef = new UnitEx ();
		unitDef.getHeroItemSlot ().add ("IST01");
		
		// Initialize skills method returns the unit definition
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.initializeUnitSkills (any (MemoryUnit.class), eq (100), eq (db))).thenReturn (unitDef);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils); 
		
		// Run test
		final MemoryUnit unit = utils.createMemoryUnit ("UN001", 1, 3, 100, db);

		// Check results
		assertEquals (1, unit.getUnitURN ());
		assertEquals ("UN001", unit.getUnitID ());
		assertEquals (3, unit.getWeaponGrade ().intValue ());
		assertEquals (UnitStatusID.ALIVE, unit.getStatus ());
		assertEquals (1, unit.getHeroItemSlot ().size ());
	}
	
	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who gets no random rolled skills, so it generates a name only
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_NoExtras () throws Exception
	{
		// Mock unit detalis
		final UnitEx unitDef = new UnitEx ();
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
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
		final UnitEx unitDef = new UnitEx ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkillEx> possibleSkills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkills ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (3)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitSkillDirectAccess (direct);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (direct, times (0)).setDirectSkillValue (unit, "HS01", 1);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS02", 1);
		verify (direct, times (0)).setDirectSkillValue (unit, "HS03", 1);
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
		final UnitEx unitDef = new UnitEx ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkillEx> possibleSkills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			skill.setOnlyIfHaveAlready (n==3);
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkills ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (3)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");

		// Set up existing skills
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		when (direct.getDirectSkillValue (unit.getUnitHasSkill (), "HS03")).thenReturn (1);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitSkillDirectAccess (direct);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (direct, times (0)).setDirectSkillValue (unit, "HS01", 1);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS02", 1);
		verify (direct, times (0)).setDirectSkillValue (unit, "HS03", 1);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method on a hero who only has no viable skills to pick (they are all "only if have already" and we have none of them)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_NoChoices () throws Exception
	{
		// Mock unit detalis
		final UnitEx unitDef = new UnitEx ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkillEx> possibleSkills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			skill.setOnlyIfHaveAlready (n <= 3);
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkills ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (3)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");

		// Set up existing skills
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitSkillDirectAccess (direct);
		utils.setRandomUtils (random);
		
		// Run test
		assertThrows (MomException.class, () ->
		{
			utils.generateHeroNameAndRandomSkills (unit, db);
		});
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method, providing that heroes with a defined hero random pick type can get skills with a blank hero random pick type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_SkillNotTypeSpecific () throws Exception
	{
		// Mock unit detalis
		final UnitEx unitDef = new UnitEx ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType ("F");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkillEx> possibleSkills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 6; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : null);
			skill.setMaxOccurrences (10);
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkills ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (6)).thenReturn (1);		// Fix skill choice - note selects from all 6

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitSkillDirectAccess (direct);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (direct, times (0)).setDirectSkillValue (unit, "HS01", 1);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS02", 1);
		verify (direct, times (0)).setDirectSkillValue (unit, "HS03", 1);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method, providing that heroes without a defined hero random pick type can get skills even if they do specify a hero random pick type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_SingleChoice_HeroNotTypeSpecific () throws Exception
	{
		// Mock unit detalis
		final UnitEx unitDef = new UnitEx ();
		unitDef.setHeroRandomPickCount (1);
		unitDef.setHeroRandomPickType (null);	// <---
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkillEx> possibleSkills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 6; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkills ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (6)).thenReturn (1);		// Fix skill choice - note selects from all 6

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitSkillDirectAccess (direct);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (direct, times (0)).setDirectSkillValue (unit, "HS01", 1);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS02", 1);
		verify (direct, times (0)).setDirectSkillValue (unit, "HS03", 1);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method where we add lots of adds into a single skill to prove it isn't capped
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Uncapped () throws Exception
	{
		// Mock unit detalis
		final UnitEx unitDef = new UnitEx ();
		unitDef.setHeroRandomPickCount (5);
		unitDef.setHeroRandomPickType ("M");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkillEx> possibleSkills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkills ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (2)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		when (direct.getDirectSkillValue (unit.getUnitHasSkill (), "HS05")).thenReturn (0, 0, 1, 1, 2, 2, 3, 3, 4, 4);	// Gets read twice each iteration
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitSkillDirectAccess (direct);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (direct, times (0)).setDirectSkillValue (unit, "HS04", 1);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 1);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 2);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 3);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 4);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 5);
	}

	/**
	 * Tests the generateHeroNameAndRandomSkills method where we run out of skills to add to because they're all capped with a maxOccurrences value
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGenerateHeroNameAndRandomSkills_Capped () throws Exception
	{
		// Mock unit detalis
		final UnitEx unitDef = new UnitEx ();
		unitDef.setHeroRandomPickCount (5);
		unitDef.setHeroRandomPickType ("M");
		for (int n = 1; n <= 5; n++)
		{
			final HeroName name = new HeroName ();
			name.setHeroNameID ("UN001_HN0" + n);
			unitDef.getHeroName ().add (name);
		}
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findUnit ("UN001", "generateHeroNameAndRandomSkills")).thenReturn (unitDef);
		
		// Mock possible skill choices, 3 fighter picks and 2 mage picks
		final List<UnitSkillEx> possibleSkills = new ArrayList<UnitSkillEx> ();
		for (int n = 1; n <= 5; n++)
		{
			final UnitSkillEx skill = new UnitSkillEx ();
			skill.setUnitSkillID ("HS0" + n);
			skill.setHeroSkillTypeID ((n <= 3) ? "F" : "M");
			
			if (n == 5)
				skill.setMaxOccurrences (4);
			
			possibleSkills.add (skill);
		}		
		when (db.getUnitSkills ()).thenReturn (possibleSkills);
		
		// Fix random results
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (5)).thenReturn (2);		// Fix name ID
		when (random.nextInt (2)).thenReturn (1);		// Fix skill choice

		// Set up test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitID ("UN001");
		
		// Set up existing skills
		final UnitSkillDirectAccess direct = mock (UnitSkillDirectAccess.class);
		when (direct.getDirectSkillValue (unit.getUnitHasSkill (), "HS05")).thenReturn (0, 0, 1, 1, 2, 2, 3, 3, 4, 4);	// Gets read twice each iteration
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitSkillDirectAccess (direct);
		utils.setRandomUtils (random);
		
		// Run test
		utils.generateHeroNameAndRandomSkills (unit, db);
		assertEquals ("UN001_HN03", unit.getHeroNameID ());
		assertEquals (UnitStatusID.GENERATED, unit.getStatus ());
		
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 1);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 2);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 3);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS05", 4);
		verify (direct, times (1)).setDirectSkillValue (unit, "HS04", 1);		// Last point goes into HS04 because HS05 is maxed out
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Player
		final PlayerDescription pd = new PlayerDescription ();
		pd.setPlayerID (2);
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		final PlayerServerDetails player = new PlayerServerDetails (pd, null, priv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player);
		
		// Unit
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (5);
		
		// Terrain
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells ();
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		
		// Set up object to test
		final PendingMovementUtils pendingMovementUtils = mock (PendingMovementUtils.class);
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setPendingMovementUtils (pendingMovementUtils);
		utils.setFogOfWarMidTurnChanges (midTurn);
		
		// Run test
		utils.setAndSendSpecialOrder (trueUnit, UnitSpecialOrder.BUILD_ROAD, player, trueTerrain, players, db, fogOfWarSettings);
		
		// Check results
		assertEquals (UnitSpecialOrder.BUILD_ROAD, trueUnit.getSpecialOrder ());
		
		verify (pendingMovementUtils).removeUnitFromAnyPendingMoves (priv.getPendingMovement (), 5);
		verify (midTurn).updatePlayerMemoryOfUnit (trueUnit, trueTerrain, players, db, fogOfWarSettings, null);
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
			unit.setUnitID ("UN00" + Integer.valueOf (n+1).toString ());
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
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertTrue (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));
	}

	/**
	 * Tests the canUnitBeAddedHere method when there's an enemy unit in the way
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_EnemyUnit () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (1);
		
		// Other units
		final MemoryUnit enemyUnit = new MemoryUnit ();
		enemyUnit.setOwningPlayerID (3);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (enemyUnit);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));
	}

	/**
	 * Tests the canUnitBeAddedHere method when there's a stack of 8 of our units already there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_StackOf8 () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (1);
		
		// Other units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (ourUnit);
		when (unitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (8);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertTrue (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));
	}

	/**
	 * Tests the canUnitBeAddedHere method when there's a stack of 9 of our units already there, so we can't fit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_StackOf9 () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (1);
		
		// Other units
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (ourUnit);
		when (unitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (9);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));
	}
	
	/**
	 * Tests the canUnitBeAddedHere method trying to add a unit onto a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_Node () throws Exception
	{
		// We ARE trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		tt.setMagicRealmID ("A");		// <---
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertTrue (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));		// Adding onto empty nodes is now allowed
	}
	
	/**
	 * Tests the canUnitBeAddedHere method trying to add a unit onto terrain that's impassable to them
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_Impassable () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (null);		// <---
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));
	}
	
	/**
	 * Tests the canUnitBeAddedHere method adding units into our own city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_OurCity () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		cityData.setCityPopulation (1);
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertTrue (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));
	}
	
	/**
	 * Tests the canUnitBeAddedHere method adding units into am enemy city (which must have no units in it, to have skipped previous checks)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCanUnitBeAddedHere_EnemyCity () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sys));
		
		// Map cell we're trying to add to
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
		terrainData.setTileTypeID ("TT01");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (3);		// <---
		cityData.setCityPopulation (1);
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Unit to try to add
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		final Set<String> testUnitSkills = new HashSet<String> ();

		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (testUnit, testUnitSkills, "TT01", db)).thenReturn (1);
		
		// Other units
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		assertFalse (utils.canUnitBeAddedHere (addLocation, testUnit, trueMap, db));
	}

	/**
	 * Tests the findNearestLocationWhereUnitCanBeAdded method in the simple situation where we do fit in the city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFindNearestLocationWhereUnitCanBeAdded_City () throws Exception
	{
		// We aren't trying to add on top of a node
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (createOverlandMapSize ());

		// Map
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sd.getOverlandMapSize ()));
		
		// Unit we're trying to add
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (any (AvailableUnit.class), eq (null), eq (null), eq (null),
			eq (players), eq (trueMap), eq (db))).thenReturn (testUnit);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		// Map cell and surrounding terrain that we're trying to add to
		for (int x = -1; x <= 1; x++)
			for (int y = -1; y <= 1; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
				terrainData.setTileTypeID ("TT01");
				trueMap.getMap ().getPlane ().get (1).getRow ().get (10+y).getCell ().get (20+x).setTerrainData (terrainData);
			}
		
		// Unit can enter this type of tile
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (any (ExpandedUnitDetails.class), anySet (), eq ("TT01"),
			any (CommonDatabase.class))).thenReturn (1);
		
		// Put 8 units in the city so we just fit
		final MemoryUnit ourUnit = new MemoryUnit ();
		ourUnit.setOwningPlayerID (2);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (ourUnit);
		when (unitUtils.countAliveEnemiesAtLocation (trueMap.getUnit (), 20, 10, 1, 0)).thenReturn (8);
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);
		utils.setExpandUnitDetails (expand);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// So we eventually end up being positioned down-right of our preferred location
		final UnitAddLocation result = utils.findNearestLocationWhereUnitCanBeAdded (addLocation, "UN001", 2, trueMap, players, sd, db);
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
		final TileTypeEx tt = new TileTypeEx ();
		
		final TileTypeEx tt2 = new TileTypeEx ();
		tt2.setMagicRealmID ("A");

		final TileTypeEx tt3 = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		when (db.findTileType ("TT02", "isNodeLairTower")).thenReturn (tt2);
		when (db.findTileType ("TT03", "isNodeLairTower")).thenReturn (tt3);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (createOverlandMapSize ());

		// Map
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sd.getOverlandMapSize ()));

		// Unit we're trying to add
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (any (AvailableUnit.class), eq (null), eq (null), eq (null),
			eq (players), eq (trueMap), eq (db))).thenReturn (testUnit);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		// Map cell and surrounding terrain that we're trying to add to
		for (int x = -1; x <= 1; x++)
			for (int y = -1; y <= 1; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
				terrainData.setTileTypeID ("TT01");
				trueMap.getMap ().getPlane ().get (1).getRow ().get (10+y).getCell ().get (20+x).setTerrainData (terrainData);
			}
		
		// Unit can enter tiles TT01 and TT02, but TT03 is impassable
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (any (ExpandedUnitDetails.class), anySet (), eq ("TT01"),
			any (CommonDatabase.class))).thenReturn (1);
		when (calc.calculateDoubleMovementToEnterTileType (any (ExpandedUnitDetails.class), anySet (), eq ("TT02"),
			any (CommonDatabase.class))).thenReturn (1);
		when (calc.calculateDoubleMovementToEnterTileType (any (ExpandedUnitDetails.class), anySet (), eq ("TT03"),
			any (CommonDatabase.class))).thenReturn (null);
		
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
		
		// Make the tiles up-right and right of us something impassable (like water), so we can't fit there either
		trueMap.getMap ().getPlane ().get (1).getRow ().get (9).getCell ().get (21).getTerrainData ().setTileTypeID ("TT03");
		trueMap.getMap ().getPlane ().get (1).getRow ().get (10).getCell ().get (21).getTerrainData ().setTileTypeID ("TT03");

		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);
		utils.setExpandUnitDetails (expand);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// So we eventually end up being positioned down-right of our preferred location
		final UnitAddLocation result = utils.findNearestLocationWhereUnitCanBeAdded (addLocation, "UN001", 2, trueMap, players, sd, db);
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
		final TileTypeEx tt = new TileTypeEx ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setOverlandMapSize (createOverlandMapSize ());

		// Map
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (createOverlandMap (sd.getOverlandMapSize ()));

		// Unit we're trying to add
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		
		final ExpandedUnitDetails testUnit = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (any (AvailableUnit.class), eq (null), eq (null), eq (null),
			eq (players), eq (trueMap), eq (db))).thenReturn (testUnit);
		when (testUnit.getOwningPlayerID ()).thenReturn (2);
		
		// Map cell and surrounding terrain that we're trying to add to
		for (int x = -1; x <= 1; x++)
			for (int y = -1; y <= 1; y++)
			{
				final OverlandMapTerrainData terrainData = new OverlandMapTerrainData (); 
				terrainData.setTileTypeID ("TT01");
				trueMap.getMap ().getPlane ().get (1).getRow ().get (10+y).getCell ().get (20+x).setTerrainData (terrainData);
			}
		
		// Easiest thing to do is make the tile type impassable, then we can't fit anywhere
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterTileType (any (ExpandedUnitDetails.class), anySet (), eq ("TT01"),
			any (CommonDatabase.class))).thenReturn (null);

		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setUnitCalculations (calc);
		utils.setExpandUnitDetails (expand);
		utils.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());

		// Run method
		final MapCoordinates3DEx addLocation = new MapCoordinates3DEx (20, 10, 1);
		addLocation.setX (20);
		addLocation.setY (10);
		addLocation.setZ (1);
		
		// So we eventually end up being positioned down-right of our preferred location
		final UnitAddLocation result = utils.findNearestLocationWhereUnitCanBeAdded (addLocation, "UN001", 2, trueMap, players, sd, db);
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
	
	/**
	 * Tests the applySingleFigureDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testApplySingleFigureDamage () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		when (xu.calculateAliveFigureCount ()).thenReturn (3);		// Defender is 4 figure unit but 1's dead already...
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xu.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		// Note these are in sets of 4 - the defence rolls each figure makes trying to block, before taking damage to HP
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn
			(5, 8, 3, 9,		// First figure is unlucky and only blocks 1 hit, then loses its 2 HP and dies
			1, 5, 8, 2);		// Second figure blocks 2 of the hits, then loses 1 HP
								// So in total, 3 of the dmg went against HP (which is the overall result of the method call)
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setRandomUtils (random);
		
		// Run method
		assertEquals (3, utils.applySingleFigureDamage (xu, 6, 4, 5, true, db));	// Take 6 hits, each figure has defence 4, with 50% block chance
	}
	
	/**
	 * Tests the applyMultiFigureDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testApplyMultiFigureDamage () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Unit
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		
		when (xu.calculateAliveFigureCount ()).thenReturn (3);		// Defender is 4 figure unit but 1's dead already...
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xu.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		// Note these are in sets of 4 - the defence rolls each figure makes trying to block, before taking damage to HP
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn
			(0, 1, 2, 3, 4, 1,   6, 9, 1, 5,		// First figure takes 4 hits, blocks 1 so takes 3 dmg, but it only had 2 HP to begin with; the extra 1 does not carry to next figure
			2, 3, 1, 6, 0, 7,   6, 9, 1, 5,		// Second figure takes 3 hits, also blocks 1, so takes 3 dmg so it loses 2 HP and has 1 HP left
			0, 1, 2, 3, 2, 1,  6, 7, 8, 9);		// Third figure takes 5 hits and fails to block any of them so loses 3 HP and the extra 2 do not carry over anywhere
		
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setRandomUtils (random);
		
		// Run method
		// Possible 6 hits per figure with 30% chance of each striking, then each figure has defence 4, with 50% block chance
		final Holder<Integer> actualDamage = new Holder<Integer> ();
		assertEquals (7, utils.applyMultiFigureDamage (xu, 6, 3, 4, 5, actualDamage, true, db));
		assertEquals (12, actualDamage.getValue ().intValue ());
	}
	
	/**
	 * Tests the addDamage method
	 */
	@Test
	public final void testAddDamage ()
	{
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();

		// Add nothing to nothing
		final List<UnitDamage> damages = new ArrayList<UnitDamage> ();
		utils.addDamage (damages, StoredDamageTypeID.HEALABLE, 0);
		assertEquals (0, damages.size ());
		
		// Add new value
		utils.addDamage (damages, StoredDamageTypeID.HEALABLE, 3);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.HEALABLE, damages.get (0).getDamageType ());
		assertEquals (3, damages.get (0).getDamageTaken ());
		
		// Add to existing value
		utils.addDamage (damages, StoredDamageTypeID.HEALABLE, 2);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.HEALABLE, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
		
		// Add secondary value
		utils.addDamage (damages, StoredDamageTypeID.PERMANENT, 1);
		assertEquals (2, damages.size ());
		assertEquals (StoredDamageTypeID.HEALABLE, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (1).getDamageType ());
		assertEquals (1, damages.get (1).getDamageTaken ());
		
		// Add to both existing values
		utils.addDamage (damages, StoredDamageTypeID.HEALABLE, 2);
		utils.addDamage (damages, StoredDamageTypeID.PERMANENT, 5);
		assertEquals (2, damages.size ());
		assertEquals (StoredDamageTypeID.HEALABLE, damages.get (0).getDamageType ());
		assertEquals (7, damages.get (0).getDamageTaken ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (1).getDamageType ());
		assertEquals (6, damages.get (1).getDamageTaken ());
		
		// Eliminate a value
		utils.addDamage (damages, StoredDamageTypeID.HEALABLE, -7);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (6, damages.get (0).getDamageTaken ());
	}
	
	/**
	 * Tests the findDamageTakenOfType method
	 */
	@Test
	public final void testFindDamageTakenOfType ()
	{
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();

		// Search for nothing
		final List<UnitDamage> damages = new ArrayList<UnitDamage> ();
		assertEquals (0, utils.findDamageTakenOfType (damages, StoredDamageTypeID.PERMANENT));
		
		// Add some values
		final UnitDamage dmg1 = new UnitDamage ();
		dmg1.setDamageType (StoredDamageTypeID.HEALABLE);
		dmg1.setDamageTaken (3);
		damages.add (dmg1);

		final UnitDamage dmg2 = new UnitDamage ();
		dmg2.setDamageType (StoredDamageTypeID.PERMANENT);
		dmg2.setDamageTaken (4);
		damages.add (dmg2);

		assertEquals (0, utils.findDamageTakenOfType (damages, StoredDamageTypeID.LIFE_STEALING));
		assertEquals (4, utils.findDamageTakenOfType (damages, StoredDamageTypeID.PERMANENT));
		assertEquals (3, utils.findDamageTakenOfType (damages, StoredDamageTypeID.HEALABLE));
	}
	
	/**
	 * Tests the healDamage method, including healing permanent damage
	 */
	@Test
	public final void testHealDamage_IncludingPermanent ()
	{
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();

		// Heal nothing
		final List<UnitDamage> damages = new ArrayList<UnitDamage> ();

		final UnitDamage dmg1 = new UnitDamage ();
		dmg1.setDamageType (StoredDamageTypeID.PERMANENT);
		dmg1.setDamageTaken (5);
		damages.add (dmg1);
		
		utils.healDamage (damages, 0, true);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
		
		// Healing negative damage is dumb and will be ignored
		utils.healDamage (damages, -2, true);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
				
		// Heal when only one damage type in the list
		utils.healDamage (damages, 2, true);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (3, damages.get (0).getDamageTaken ());
		
		// 3 types of damage in list - heal enough to completely cure one of them and move on to the next
		final UnitDamage dmg2 = new UnitDamage ();
		dmg2.setDamageType (StoredDamageTypeID.HEALABLE);
		dmg2.setDamageTaken (5);
		damages.add (dmg2);

		final UnitDamage dmg3 = new UnitDamage ();
		dmg3.setDamageType (StoredDamageTypeID.LIFE_STEALING);
		dmg3.setDamageTaken (5);
		damages.add (dmg3);

		utils.healDamage (damages, 8, true);
		assertEquals (2, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (3, damages.get (0).getDamageTaken ());
		assertEquals (StoredDamageTypeID.LIFE_STEALING, damages.get (1).getDamageType ());
		assertEquals (2, damages.get (1).getDamageTaken ());
		
		// Try to heal permanent damage
		utils.healDamage (damages, 3, true);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (2, damages.get (0).getDamageTaken ());
	}
	
	/**
	 * Tests the healDamage method, blocking healing permanent damage
	 */
	@Test
	public final void testHealDamage_ExcludingPermanent ()
	{
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();

		// Heal nothing
		final List<UnitDamage> damages = new ArrayList<UnitDamage> ();

		final UnitDamage dmg1 = new UnitDamage ();
		dmg1.setDamageType (StoredDamageTypeID.HEALABLE);
		dmg1.setDamageTaken (5);
		damages.add (dmg1);
		
		utils.healDamage (damages, 0, false);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.HEALABLE, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
		
		// Healing negative damage is dumb and will be ignored
		utils.healDamage (damages, -2, false);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.HEALABLE, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
				
		// Heal when only one damage type in the list
		utils.healDamage (damages, 2, false);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.HEALABLE, damages.get (0).getDamageType ());
		assertEquals (3, damages.get (0).getDamageTaken ());
		
		// 3 types of damage in list - heal enough to completely cure one of them and move on to the next
		final UnitDamage dmg2 = new UnitDamage ();
		dmg2.setDamageType (StoredDamageTypeID.PERMANENT);
		dmg2.setDamageTaken (5);
		damages.add (dmg2);

		final UnitDamage dmg3 = new UnitDamage ();
		dmg3.setDamageType (StoredDamageTypeID.LIFE_STEALING);
		dmg3.setDamageTaken (5);
		damages.add (dmg3);

		utils.healDamage (damages, 6, false);
		assertEquals (2, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
		assertEquals (StoredDamageTypeID.LIFE_STEALING, damages.get (1).getDamageType ());
		assertEquals (2, damages.get (1).getDamageTaken ());
		
		// Try to heal permanent damage
		utils.healDamage (damages, 3, false);
		assertEquals (1, damages.size ());
		assertEquals (StoredDamageTypeID.PERMANENT, damages.get (0).getDamageType ());
		assertEquals (5, damages.get (0).getDamageTaken ());
	}
	
	/**
	 * Tests the whatKilledUnit method
	 */
	@Test
	public final void testWhatKilledUnit ()
	{
		// Set up object to test
		final UnitServerUtilsImpl utils = new UnitServerUtilsImpl ();
		utils.setUnitUtils (new UnitUtilsImpl ());

		// Taken only regular damage
		final List<UnitDamage> damages = new ArrayList<UnitDamage> ();

		final UnitDamage dmg1 = new UnitDamage ();
		dmg1.setDamageType (StoredDamageTypeID.HEALABLE);
		dmg1.setDamageTaken (2);
		damages.add (dmg1);
		
		assertEquals (StoredDamageTypeID.HEALABLE, utils.whatKilledUnit (damages));
		
		// Taken only special damage
		dmg1.setDamageType (StoredDamageTypeID.PERMANENT);
		assertEquals (StoredDamageTypeID.PERMANENT, utils.whatKilledUnit (damages));
		
		// Taken half of each
		final UnitDamage dmg2 = new UnitDamage ();
		dmg2.setDamageType (StoredDamageTypeID.HEALABLE);
		dmg2.setDamageTaken (2);
		damages.add (dmg2);

		assertEquals (StoredDamageTypeID.PERMANENT, utils.whatKilledUnit (damages));
		
		// Now special damage isn't quite half
		dmg2.setDamageTaken (3);
		assertEquals (StoredDamageTypeID.HEALABLE, utils.whatKilledUnit (damages));
		
		// 3 kinds of damage, but none are quite half
		final UnitDamage dmg3 = new UnitDamage ();
		dmg3.setDamageType (StoredDamageTypeID.LIFE_STEALING);
		dmg3.setDamageTaken (4);
		damages.add (dmg3);

		assertEquals (StoredDamageTypeID.HEALABLE, utils.whatKilledUnit (damages));
		
		// Now life stealing is half
		dmg3.setDamageTaken (5);
		assertEquals (StoredDamageTypeID.LIFE_STEALING, utils.whatKilledUnit (damages));
		
		// If half damaged with permanent damage, and half by life stealing, then life stealing wins
		damages.remove (dmg2);
		dmg1.setDamageTaken (5);
		assertEquals (StoredDamageTypeID.LIFE_STEALING, utils.whatKilledUnit (damages));
		
		// Now permanent is a little bit higher
		dmg1.setDamageTaken (6);
		assertEquals (StoredDamageTypeID.PERMANENT, utils.whatKilledUnit (damages));
	}
}
package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.MomException;
import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.CombatMapCoordinatesEx;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.servertoclient.v0_9_4.DamageCalculationMessage;
import momime.common.messages.servertoclient.v0_9_4.DamageCalculationMessageTypeID;
import momime.common.messages.servertoclient.v0_9_4.KillUnitActionID;
import momime.common.messages.servertoclient.v0_9_4.KillUnitMessage;
import momime.common.messages.servertoclient.v0_9_4.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.StartCombatMessage;
import momime.common.messages.servertoclient.v0_9_4.StartCombatMessageUnit;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MapAreaOfCombatTiles;
import momime.common.messages.v0_9_4.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitCombatSideID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.MomUnitAttributeComponent;
import momime.common.utils.MomUnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.CombatMapElement;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Tests the CombatProcessingImpl class
 */
public final class TestCombatProcessingImpl
{
	/**
	 * Just to save repeating this a dozen times in the test cases
	 * @param x X coord
	 * @return Coordinates object
	 */
	private final OverlandMapCoordinatesEx createCoordinates (final int x)
	{
		final OverlandMapCoordinatesEx combatLocation = new OverlandMapCoordinatesEx ();
		combatLocation.setX (x);
		combatLocation.setY (10);
		combatLocation.setPlane (1);
		return combatLocation;
	}

	/**
	 * Tests the determineMaxUnitsInRow method
	 * This does a mock setup for a defender in a city with city walls (see layout pattern in the comments of the main method)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineMaxUnitsInRow () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		// Combat map
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = ServerTestData.createCombatMap ();
		
		// Fake impassable tile
		final MomCombatTile impassable = new MomCombatTile ();
		
		// Rather than hard coding the coords here, use the server XML to find the location of city wall corners + wizard's fortress
		for (final CombatMapElement element : db.getCombatMapElement ())
			if ((CommonDatabaseConstants.VALUE_BUILDING_FORTRESS.equals (element.getBuildingID ())) ||
				("CTB02".equals (element.getCombatTileBorderID ())))
				
				combatMap.getRow ().get (element.getLocationY ()).getCell ().set (element.getLocationX (), impassable);
		
		// Set up test object
		final MomUnitCalculations calc = mock (MomUnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterCombatTile (impassable, db)).thenReturn (-1);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitCalculations (calc);
		
		// Run method
		final List<Integer> maxUnitsInRow = proc.determineMaxUnitsInRow (CombatProcessingImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X,
			CombatProcessingImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y, CombatProcessingImpl.COMBAT_SETUP_DEFENDER_FACING,
			CombatProcessingImpl.COMBAT_SETUP_DEFENDER_ROWS, combatMapCoordinateSystem, combatMap, db);
		
		// Check results
		assertEquals (5, maxUnitsInRow.size ());
		assertEquals (3, maxUnitsInRow.get (0).intValue ());
		assertEquals (5, maxUnitsInRow.get (1).intValue ());
		assertEquals (4, maxUnitsInRow.get (2).intValue ());
		assertEquals (5, maxUnitsInRow.get (3).intValue ());
		assertEquals (3, maxUnitsInRow.get (4).intValue ());
	}
	
	/**
	 * Tests the calculateUnitCombatClass method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitCombatClass () throws Exception
	{
		// Use real DB since it knows which units are heroes and which are normal
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Don't need anything real here since we're mocking the attribute calculation
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// One of each kind of test unit
		final MemoryUnit dwarfHero = new MemoryUnit ();
		dwarfHero.setUnitID ("UN001");
		
		final MemoryUnit spearmen = new MemoryUnit ();
		spearmen.setUnitID ("UN040");
		
		final MemoryUnit archerHero = new MemoryUnit ();
		archerHero.setUnitID ("UN031");
		
		final MemoryUnit bowmen = new MemoryUnit ();
		bowmen.setUnitID ("UN042");
		
		final MemoryUnit settlers = new MemoryUnit ();
		settlers.setUnitID ("UN045");

		// Set up test object
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitUtils.getModifiedAttributeValue (dwarfHero, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		when (unitUtils.getModifiedAttributeValue (dwarfHero, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (0);
		
		when (unitUtils.getModifiedAttributeValue (spearmen, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		when (unitUtils.getModifiedAttributeValue (spearmen, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (0);
			
		when (unitUtils.getModifiedAttributeValue (archerHero, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		when (unitUtils.getModifiedAttributeValue (archerHero, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
				
		when (unitUtils.getModifiedAttributeValue (bowmen, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
		when (unitUtils.getModifiedAttributeValue (bowmen, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);
					
		when (unitUtils.getModifiedAttributeValue (settlers, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (0);
		when (unitUtils.getModifiedAttributeValue (settlers, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (0);
						
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (unitUtils);
		
		// Check results
		assertEquals (1, proc.calculateUnitCombatClass (dwarfHero, players, spells, combatAreaEffects, db));
		assertEquals (2, proc.calculateUnitCombatClass (spearmen, players, spells, combatAreaEffects, db));
		assertEquals (3, proc.calculateUnitCombatClass (archerHero, players, spells, combatAreaEffects, db));
		assertEquals (4, proc.calculateUnitCombatClass (bowmen, players, spells, combatAreaEffects, db));
		assertEquals (5, proc.calculateUnitCombatClass (settlers, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the listNumberOfEachCombatClass method
	 */
	@Test
	public final void testListNumberOfEachCombatClass ()
	{
		// Create test list
		final List<MemoryUnitAndCombatClass> in = new ArrayList<MemoryUnitAndCombatClass> ();
		in.add (new MemoryUnitAndCombatClass (null, 1)); 
		in.add (new MemoryUnitAndCombatClass (null, 1)); 
		in.add (new MemoryUnitAndCombatClass (null, 1)); 
		in.add (new MemoryUnitAndCombatClass (null, 1)); 
		in.add (new MemoryUnitAndCombatClass (null, 2)); 
		in.add (new MemoryUnitAndCombatClass (null, 2)); 
		in.add (new MemoryUnitAndCombatClass (null, 2)); 
		in.add (new MemoryUnitAndCombatClass (null, 4)); 
		in.add (new MemoryUnitAndCombatClass (null, 4)); 
		in.add (new MemoryUnitAndCombatClass (null, 5)); 
		in.add (new MemoryUnitAndCombatClass (null, 5)); 
		in.add (new MemoryUnitAndCombatClass (null, 5)); 
		
		// Run method
		final List<Integer> out = new CombatProcessingImpl ().listNumberOfEachCombatClass (in);
		
		// Check results
		assertEquals (4, out.size ());
		assertEquals (4, out.get (0).intValue ());
		assertEquals (3, out.get (1).intValue ());
		assertEquals (2, out.get (2).intValue ());
		assertEquals (3, out.get (3).intValue ());
	}
	
	/**
	 * Tests the mergeRowsIfTooMany method - reduction should operate as follows:
	 * 4, 2, 2, 6, 3, 2, 4
	 * 4, 4, 6, 3, 2, 4
	 * 4, 4, 6, 5, 4
	 * 8, 6, 5, 4
	 */
	@Test
	public final void testMergeRowsIfTooMany ()
	{
		// Create test list
		final List<Integer> rows = new ArrayList<Integer> ();
		rows.add (4);
		rows.add (2);
		rows.add (2);
		rows.add (6);
		rows.add (3);
		rows.add (2);
		rows.add (4);
		
		// Run method
		new CombatProcessingImpl ().mergeRowsIfTooMany (rows, 4);

		// Check results
		assertEquals (4, rows.size ());
		assertEquals (8, rows.get (0).intValue ());
		assertEquals (6, rows.get (1).intValue ());
		assertEquals (5, rows.get (2).intValue ());
		assertEquals (4, rows.get (3).intValue ());
	}
	
	/**
	 * Tests the moveUnitsInOverfullRowsBackwards method, when there is enough space to move the units back into
	 * Say the maximum numbers per row are 5, 4, 3, 2, 1
	 * and we come in with 6, 6, 1, should then go to 5, 4, 3, 1
	 */
	@Test
	public final void testMoveUnitsInOverfullRowsBackwards ()
	{
		// Create test lists
		final List<Integer> rows = new ArrayList<Integer> ();
		rows.add (6);
		rows.add (6);
		rows.add (1);
		
		final List<Integer> maxInRow = new ArrayList<Integer> ();
		maxInRow.add (5);
		maxInRow.add (4);
		maxInRow.add (3);
		maxInRow.add (2);
		maxInRow.add (1);
		
		// Run method
		new CombatProcessingImpl ().moveUnitsInOverfullRowsBackwards (rows, maxInRow);

		// Check results
		assertEquals (4, rows.size ());
		assertEquals (5, rows.get (0).intValue ());
		assertEquals (4, rows.get (1).intValue ());
		assertEquals (3, rows.get (2).intValue ());
		assertEquals (1, rows.get (3).intValue ());
	}

	/**
	 * Tests the moveUnitsInOverfullRowsBackwards method, when we hit and fill the back row and still have an overflow of units
	 * Say the maximum numbers per row are 5, 4, 3, 2, 1
	 * and we come in with 2, 3, 4, 5, should then go to 2, 3, 3, 2, 4 - so 4 get shoved in the last row even though they don't fit
	 */
	@Test
	public final void testMoveUnitsInOverfullRowsBackwards_StillOverfull ()
	{
		// Create test lists
		final List<Integer> rows = new ArrayList<Integer> ();
		rows.add (2);
		rows.add (3);
		rows.add (4);
		rows.add (5);
		
		final List<Integer> maxInRow = new ArrayList<Integer> ();
		maxInRow.add (5);
		maxInRow.add (4);
		maxInRow.add (3);
		maxInRow.add (2);
		maxInRow.add (1);
		
		// Run method
		new CombatProcessingImpl ().moveUnitsInOverfullRowsBackwards (rows, maxInRow);

		// Check results
		assertEquals (5, rows.size ());
		assertEquals (2, rows.get (0).intValue ());
		assertEquals (3, rows.get (1).intValue ());
		assertEquals (3, rows.get (2).intValue ());
		assertEquals (2, rows.get (3).intValue ());
		assertEquals (4, rows.get (4).intValue ());
	}
	
	/**
	 * Tests the moveUnitsInOverfullRowsForwards method, when there is enough space to move the units back into
	 * Say the maximum numbers per row are 5, 4, 3, 2, 1
	 * and we come in with 1, 2, 3, 4, 3 should then go to 3, 4, 3, 2, 1
	 * @throws MomException If there's not enough space to fit all the units
	 */
	@Test
	public final void testMoveUnitsInOverfullRowsFowards () throws MomException
	{
		// Create test lists
		final List<Integer> rows = new ArrayList<Integer> ();
		rows.add (1);
		rows.add (2);
		rows.add (3);
		rows.add (4);
		rows.add (3);
		
		final List<Integer> maxInRow = new ArrayList<Integer> ();
		maxInRow.add (5);
		maxInRow.add (4);
		maxInRow.add (3);
		maxInRow.add (2);
		maxInRow.add (1);
		
		// Run method
		new CombatProcessingImpl ().moveUnitsInOverfullRowsForwards (rows, maxInRow);

		// Check results
		assertEquals (5, rows.size ());
		assertEquals (3, rows.get (0).intValue ());
		assertEquals (4, rows.get (1).intValue ());
		assertEquals (3, rows.get (2).intValue ());
		assertEquals (2, rows.get (3).intValue ());
		assertEquals (1, rows.get (4).intValue ());
	}

	/**
	 * Tests the moveUnitsInOverfullRowsForwards method, when we hit and fill the front row and still have an overflow of units
	 * Say the maximum numbers per row are 5, 4, 3, 2, 1
	 * and we come in with 4, 3, 6, 1, 1 should then should throw an exception (it gets to 5, 4, 3, 1, 1 and still 1 unit leftover)
	 * @throws MomException If there's not enough space to fit all the units
	 */
	@Test(expected=MomException.class)
	public final void testMoveUnitsInOverfullRowsFowards_StillOverfull () throws MomException
	{
		// Create test lists
		final List<Integer> rows = new ArrayList<Integer> ();
		rows.add (4);
		rows.add (3);
		rows.add (6);
		rows.add (1);
		rows.add (1);
		
		final List<Integer> maxInRow = new ArrayList<Integer> ();
		maxInRow.add (5);
		maxInRow.add (4);
		maxInRow.add (3);
		maxInRow.add (2);
		maxInRow.add (1);
		
		// Run method
		new CombatProcessingImpl ().moveUnitsInOverfullRowsForwards (rows, maxInRow);
	}
	
	/**
	 * Tests the placeCombatUnits method for setting up human units attacking a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPlaceCombatUnits_Attackers () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final StartCombatMessage msg = new StartCombatMessage ();
		
		// This isn't used directly, but easier to do the checks at the end if we have a true FOW memory object
		final List<FogOfWarMemory> FOWs = new ArrayList<FogOfWarMemory> ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		FOWs.add (trueMap);

		// Combat map
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = ServerTestData.createCombatMap ();
		
		// Combat location
		final OverlandMapCoordinatesEx combatLocation = new OverlandMapCoordinatesEx ();
		combatLocation.setX (20);
		combatLocation.setY (10);
		combatLocation.setPlane (1);

		// 1 in front row and 2 behind
		final List<Integer> unitsInRow = new ArrayList<Integer> ();
		unitsInRow.add (1);
		unitsInRow.add (2);

		// Players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		FOWs.add (attackingPriv.getFogOfWarMemory ());
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, attackingPriv, null, null);

		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		FOWs.add (defendingPriv.getFogOfWarMemory ());

		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, defendingPriv, null, null);
		
		// The actual units to place
		final List<MemoryUnitAndCombatClass> unitsToPosition = new ArrayList<MemoryUnitAndCombatClass> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setOwningPlayerID (attackingPD.getPlayerID ());
			
			unitsToPosition.add (new MemoryUnitAndCombatClass (tu, 0));
			trueMap.getUnit ().add (tu);
			
			// Routine expects attacker and defender to both have unit in their memory
			final MemoryUnit atkUnit = new MemoryUnit ();
			atkUnit.setUnitURN (n);
			atkUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			attackingPriv.getFogOfWarMemory ().getUnit ().add (atkUnit);

			final MemoryUnit defUnit = new MemoryUnit ();
			defUnit.setUnitURN (n);
			defUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			defendingPriv.getFogOfWarMemory ().getUnit ().add (defUnit);
		}
		
		// Set up object to test
		final MomUnitCalculations calc = mock (MomUnitCalculations.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitCalculations (calc);
		proc.setUnitUtils (new UnitUtilsImpl ());		// used for searching unit lists, so easier to use real one
		
		// Run method
		proc.placeCombatUnits (combatLocation, CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X,
			CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y, CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, unitsToPosition, unitsInRow, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, combatMap, db);
		
		// Check server's true memory, attacker's memory, defender's memory
		for (final FogOfWarMemory fow : FOWs)
		{
			assertEquals (3, fow.getUnit ().size ());
			
			final MemoryUnit unit1 = fow.getUnit ().get (0);
			assertEquals (1, unit1.getUnitURN ());
			assertSame (combatLocation, unit1.getCombatLocation ());
			assertEquals (7, unit1.getCombatPosition ().getX ());
			assertEquals (17, unit1.getCombatPosition ().getY ());
			assertEquals (CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FACING, unit1.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.ATTACKER, unit1.getCombatSide ());
			
			final MemoryUnit unit2 = fow.getUnit ().get (1);
			assertEquals (2, unit2.getUnitURN ());
			assertSame (combatLocation, unit2.getCombatLocation ());
			assertEquals (7, unit2.getCombatPosition ().getX ());
			assertEquals (19, unit2.getCombatPosition ().getY ());
			assertEquals (CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FACING, unit2.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.ATTACKER, unit2.getCombatSide ());

			final MemoryUnit unit3 = fow.getUnit ().get (2);
			assertEquals (3, unit3.getUnitURN ());
			assertSame (combatLocation, unit3.getCombatLocation ());
			assertEquals (8, unit3.getCombatPosition ().getX ());
			assertEquals (18, unit3.getCombatPosition ().getY ());
			assertEquals (CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FACING, unit3.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.ATTACKER, unit3.getCombatSide ());
		}
		
		// Check message
		assertEquals (3, msg.getUnitPlacement ().size ());

		final StartCombatMessageUnit unit1 = msg.getUnitPlacement ().get (0);
		assertEquals (1, unit1.getUnitURN ());
		assertEquals (7, unit1.getCombatPosition ().getX ());
		assertEquals (17, unit1.getCombatPosition ().getY ());
		assertEquals (CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FACING, unit1.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, unit1.getCombatSide ());
		assertNull (unit1.getUnitDetails ());

		final StartCombatMessageUnit unit2 = msg.getUnitPlacement ().get (1);
		assertEquals (2, unit2.getUnitURN ());
		assertEquals (7, unit2.getCombatPosition ().getX ());
		assertEquals (19, unit2.getCombatPosition ().getY ());
		assertEquals (CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FACING, unit2.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, unit2.getCombatSide ());
		assertNull (unit2.getUnitDetails ());

		final StartCombatMessageUnit unit3 = msg.getUnitPlacement ().get (2);
		assertEquals (3, unit3.getUnitURN ());
		assertEquals (8, unit3.getCombatPosition ().getX ());
		assertEquals (18, unit3.getCombatPosition ().getY ());
		assertEquals (CombatProcessingImpl.COMBAT_SETUP_ATTACKER_FACING, unit3.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, unit3.getCombatSide ());
		assertNull (unit3.getUnitDetails ());
	}
	
	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked another player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingOtherPlayer () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID ("WZ02");

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		defendingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection defendingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		defendingPlayer.setConnection (defendingPlayerConnection);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		assertEquals (2, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getData ().getUnitURN ());

		assertEquals (2, defendingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellDefenderToRemoveAttackersDeadUnit = (KillUnitMessage) defendingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellDefenderToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellDefenderToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellDefenderToRemoveDefendersDeadUnit = (KillUnitMessage) defendingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellDefenderToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellDefenderToRemoveDefendersDeadUnit.getData ().getUnitURN ());
		
		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (3, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, defendingPriv.getFogOfWarMemory ().getUnit ());
	}

	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked monsters walking around map
	 * Behaves exactly the same way, except that we don't send messages to the computer defender
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingRampagingMonsters () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		defendingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		assertEquals (2, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getData ().getUnitURN ());

		// Defender is now a computer player so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (3, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, defendingPriv.getFogOfWarMemory ().getUnit ());
	}

	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked monsters in a node
	 * Now ALL the defending units get "killed off" on the client, even any left alive, since client doesn't remember monsters guarding nodes/lairs/towers outside of combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		defendingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (3, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersAliveMonster = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersAliveMonster.getData ().getKillUnitActionID ());
		assertEquals (7, tellAttackerToRemoveDefendersAliveMonster.getData ().getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (2);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveDefendersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getData ().getUnitURN ());

		// Defender is now a computer player so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (7, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (8, attackingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (1)).removeUnitURN (3, defendingPriv.getFogOfWarMemory ().getUnit ());
		verify (unitUtils, times (0)).removeUnitURN (7, defendingPriv.getFogOfWarMemory ().getUnit ());		// Its the monster player's own unit, so don't remove it from their server memory
		verify (unitUtils, times (1)).removeUnitURN (8, defendingPriv.getFogOfWarMemory ().getUnit ());
	}
	
	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked an empty node, so defendingPlayer is null
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingEmptyNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, null, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());

		// Defender doesn't even exist, so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
	}
	
	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked a location that we'd already
	 * cleared with a previous unit stack in a simultaneous turns game, so defendingPlayer is null.
	 * The only difference from the EmptyNode test is the tileTypeID. 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_WalkInWithoutAFight () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		attackingPriv.getFogOfWarMemory ().getUnit ().add (new MemoryUnit ());		// Put some dummy unit in the list just to make the lists unique
		
		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, null, null, null, null);
		players.add (otherPlayer);
		
		// Attacker had 4 units, 1 regular unit still alive, 1 hero still alive, 1 regular unit killed, and 1 hero killed
		// Defender had 2 units, 1 regular unit still alive, 1 regular unit killed
		final MemoryUnit attackerAliveLongbowmen = new MemoryUnit ();
		attackerAliveLongbowmen.setUnitURN (1);
		attackerAliveLongbowmen.setUnitID ("UN102");
		attackerAliveLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		attackerAliveLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (createCoordinates (20));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (createCoordinates (20));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (createCoordinates (21));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, null, trueMap, players, sd, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.FREE, null, trueMap, players, sd, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (KillUnitActionID.FREE, tellAttackerToRemoveAttackersDeadUnit.getData ().getKillUnitActionID ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getData ().getUnitURN ());

		// Defender doesn't even exist, so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for summoning phantom warriors into combat against another human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_TwoHumanPlayers () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID ("WZ02");
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection defendingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		defendingPlayer.setConnection (defendingPlayerConnection);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.ATTACKER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.ATTACKER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		assertEquals (1, defendingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage defendingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) defendingPlayerConnection.getMessages ().get (0);
		assertEquals (1, defendingMsg.getUnitURN ());
		assertEquals (combatLocation, defendingMsg.getCombatLocation ());
		assertEquals (combatPosition, defendingMsg.getCombatPosition ());
		assertEquals (7, defendingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, defendingMsg.getCombatSide ());
		assertEquals ("SP045", defendingMsg.getSummonedBySpellID ());

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for summoning phantom warriors into combat against rampaging monsters walking around the map
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_AgainstRampagingMonsters () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.ATTACKER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.ATTACKER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for rampaging monsters walking around the map summoning phantom warriors against us
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_RampagingMonstersAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.DEFENDER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.DEFENDER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for summoning phantom warriors into combat against monsters in a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_AgainstNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit - outside observer can see it because was the attacker who summoned it
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.ATTACKER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 3; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			if (n == 2)
			{
				assertNull (playerUnit.getCombatLocation ());
				assertNull (playerUnit.getCombatPosition ());
				assertNull (playerUnit.getCombatHeading ());
				assertNull (playerUnit.getCombatSide ());
			}
			else
			{
				assertEquals (combatLocation, playerUnit.getCombatLocation ());
				assertEquals (combatPosition, playerUnit.getCombatPosition ());
				assertEquals (7, playerUnit.getCombatHeading ().intValue ());
				assertEquals (UnitCombatSideID.ATTACKER, playerUnit.getCombatSide ());
			}
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.ATTACKER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for monsters in a node summoning phantom warriors against us
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Summoning_NodeAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players already know about the unit, but the outside observer can't see it because its a monster in a node
		for (int index = 0; index < 2; index ++)
		{
			final PlayerServerDetails thisPlayer = players.get (index);
			
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit,
			combatLocation, combatLocation, combatPosition, 7, UnitCombatSideID.DEFENDER, "SP045", db);
		
		// Check true memory on server
		assertEquals (combatLocation, trueUnit.getCombatLocation ());
		assertEquals (combatPosition, trueUnit.getCombatPosition ());
		assertEquals (7, trueUnit.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (int n = 0; n < 2; n++)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) players.get (n).getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertEquals (combatLocation, playerUnit.getCombatLocation ());
			assertEquals (combatPosition, playerUnit.getCombatPosition ());
			assertEquals (7, playerUnit.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.DEFENDER, playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertEquals (combatLocation, attackingMsg.getCombatLocation ());
		assertEquals (combatPosition, attackingMsg.getCombatPosition ());
		assertEquals (7, attackingMsg.getCombatHeading ().intValue ());
		assertEquals (UnitCombatSideID.DEFENDER, attackingMsg.getCombatSide ());
		assertEquals ("SP045", attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection
		
		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a unit at the end of a combat against another human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_TwoHumanPlayers () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID ("WZ02");
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection defendingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		defendingPlayer.setConnection (defendingPlayerConnection);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.ATTACKER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		assertEquals (1, defendingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage defendingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) defendingPlayerConnection.getMessages ().get (0);
		assertEquals (1, defendingMsg.getUnitURN ());
		assertNull (defendingMsg.getCombatLocation ());
		assertNull (defendingMsg.getCombatPosition ());
		assertNull (defendingMsg.getCombatHeading ());
		assertNull (defendingMsg.getCombatSide ());
		assertNull (defendingMsg.getSummonedBySpellID ());

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a unit at the end of a combat against rampaging monsters
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_AgainstRampagingMonsters () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.ATTACKER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a rampaging monsters unit at the end of combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_RampagingMonstersAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");		// regular map tile
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.DEFENDER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}

	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a unit at the end of a combat against a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_AgainstNode () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		
		// Both players and the outside observer all already know about the unit, but the outside observer doesn't know that its in combat
		// Outside observer can see it because its the attacker's unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (19));		// Attacking from adjacent square

			if (thisPlayer != otherPlayer)
			{
				playerUnit.setCombatLocation (combatLocation);
				playerUnit.setCombatHeading (7);
				playerUnit.setCombatPosition (combatPosition);
				playerUnit.setCombatSide (UnitCombatSideID.ATTACKER);
			}
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final SetUnitIntoOrTakeUnitOutOfCombatMessage attackingMsg = (SetUnitIntoOrTakeUnitOutOfCombatMessage) attackingPlayerConnection.getMessages ().get (0);
		assertEquals (1, attackingMsg.getUnitURN ());
		assertNull (attackingMsg.getCombatLocation ());
		assertNull (attackingMsg.getCombatPosition ());
		assertNull (attackingMsg.getCombatHeading ());
		assertNull (attackingMsg.getCombatSide ());
		assertNull (attackingMsg.getSummonedBySpellID ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the setUnitIntoOrTakeUnitOutOfCombat method for removing a rampaging monsters unit at the end of combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSetUnitIntoOrTakeUnitOutOfCombat_Removing_NodeAgainstUs () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		final MomSessionDescription sd = ServerTestData.createMomSessionDescription (db, "60x40", "LP03", "NS03", "DL05", "FOW01", "US01", "SS01");

		final MapVolumeOfMemoryGridCells trueTerrain = ServerTestData.createOverlandMap (sd.getMapSize ());

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");		// node
		trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Attacking player
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge attackingPub = new MomPersistentPlayerPublicKnowledge ();
		attackingPub.setWizardID ("WZ01");
		
		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());

		final DummyServerToClientConnection attackingPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, attackingPub, attackingPriv, null, null);
		attackingPlayer.setConnection (attackingPlayerConnection);
		players.add (attackingPlayer);

		// Defending player
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);

		final MomPersistentPlayerPublicKnowledge defendingPub = new MomPersistentPlayerPublicKnowledge ();
		defendingPub.setWizardID (CommonDatabaseConstants.WIZARD_ID_MONSTERS);
		
		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, defendingPub, defendingPriv, null, null);
		players.add (defendingPlayer);

		// Some other player not involved in the combat
		final PlayerDescription otherPD = new PlayerDescription ();
		otherPD.setPlayerID (5);
		otherPD.setHuman (true);

		final MomPersistentPlayerPublicKnowledge otherPub = new MomPersistentPlayerPublicKnowledge ();
		otherPub.setWizardID ("WZ03");
		
		final MomPersistentPlayerPrivateKnowledge otherPriv = new MomPersistentPlayerPrivateKnowledge ();
		otherPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final DummyServerToClientConnection otherPlayerConnection = new DummyServerToClientConnection ();
		
		final PlayerServerDetails otherPlayer = new PlayerServerDetails (otherPD, otherPub, otherPriv, null, null);
		otherPlayer.setConnection (otherPlayerConnection);
		players.add (otherPlayer);

		// Location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Combat position
		final CombatMapCoordinatesEx combatPosition = new CombatMapCoordinatesEx ();
		combatPosition.setX (7);
		combatPosition.setY (12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (createCoordinates (20));
		
		trueUnit.setCombatLocation (combatLocation);
		trueUnit.setCombatHeading (7);
		trueUnit.setCombatPosition (combatPosition);
		trueUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		
		// By the time this method runs, all dead units, summoned units, and monsters guarding nodes/lairs/towers have already
		// been removed on the client - so the attacker now doesn't know about the units they were fighting against
		{
			final PlayerServerDetails thisPlayer = defendingPlayer;
			
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (createCoordinates (20));

			playerUnit.setCombatLocation (combatLocation);
			playerUnit.setCombatHeading (7);
			playerUnit.setCombatPosition (combatPosition);
			playerUnit.setCombatSide (UnitCombatSideID.DEFENDER);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitUtils (new UnitUtilsImpl ());	// only using it for searching, easier to just use real one
		
		// Run test
		proc.setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, trueTerrain, trueUnit, combatLocation, null, null, null, null, null, db);
		
		// Check true memory on server
		assertNull (trueUnit.getCombatLocation ());
		assertNull (trueUnit.getCombatPosition ());
		assertNull (trueUnit.getCombatHeading ());
		assertNull (trueUnit.getCombatSide ());
		
		// Check players' memory on server
		{
			final PlayerServerDetails thisPlayer = defendingPlayer;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final MemoryUnit playerUnit = priv.getFogOfWarMemory ().getUnit ().get (0);
			
			assertNull (playerUnit.getCombatLocation ());
			assertNull (playerUnit.getCombatPosition ());
			assertNull (playerUnit.getCombatHeading ());
			assertNull (playerUnit.getCombatSide ());
		}
		
		// Check players' memory on clients
		assertEquals (0, attackingPlayerConnection.getMessages ().size ());

		// Defending player is now AI so has no connection

		assertEquals (0, otherPlayerConnection.getMessages ().size ());
	}
	
	/**
	 * Tests the countUnitsInCombat method
	 */
	@Test
	public final void testCountUnitsInCombat ()
	{
		// Set up sample units
		final List<MemoryUnit> units = new ArrayList<MemoryUnit> ();
		
		// Right
		final MemoryUnit unit1 = new MemoryUnit ();
		unit1.setStatus (UnitStatusID.ALIVE);
		unit1.setCombatLocation (createCoordinates (20));
		unit1.setCombatPosition (new CombatMapCoordinatesEx ());
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit1);
		
		// Wrong location
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setCombatLocation (createCoordinates (21));
		unit2.setCombatPosition (new CombatMapCoordinatesEx ());
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit2);
		
		// Defender
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setCombatLocation (createCoordinates (20));
		unit3.setCombatPosition (new CombatMapCoordinatesEx ());
		unit3.setCombatSide (UnitCombatSideID.DEFENDER);
		units.add (unit3);
		
		// Dead
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setStatus (UnitStatusID.DEAD);
		unit4.setCombatLocation (createCoordinates (20));
		unit4.setCombatPosition (new CombatMapCoordinatesEx ());
		unit4.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit4);
		
		// Not in combat
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setCombatLocation (createCoordinates (20));
		unit5.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit5);
		
		// Another right one
		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setCombatLocation (createCoordinates (20));
		unit6.setCombatPosition (new CombatMapCoordinatesEx ());
		unit6.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit6);
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		
		// Run test
		assertEquals (2, proc.countUnitsInCombat (createCoordinates (20), UnitCombatSideID.ATTACKER, units));
	}
	
	/**
	 * Tests the calculateDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDamage () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingMsgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingMsgs);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (22);
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);
		
		// Set up attacker stats
		final MomUnitCalculations unitCalculations = mock (MomUnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (attacker, players, spells, combatAreaEffects, db)).thenReturn (6);		// Attacker has 6 figures...

		when (unitUtils.getModifiedAttributeValue (attacker, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// ..and 3 swords, so 18 hits...

		when (unitUtils.getModifiedAttributeValue (attacker, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);	// ..with 40% chance to hit on each
		
		// Set up defender stats
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender is 4 figure unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_DEFENCE,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 shields...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (2);	// ..with 50% chance to block on each

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_HIT_POINTS,
			MomUnitAttributeComponent.ALL, MomUnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		
		when (unitCalculations.calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db)).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8,		// Attack rolls, 6 of them are <4
			5, 8, 3, 9,		// First figure is unlucky and only blocks 1 hit, then loses its 2 HP and dies
			1, 5, 8, 2);		// Second figure blocks 2 of the hits, then loses 1 HP
								// So in total, 3 of the dmg went against HP (which is the overall result of the method call)
		
		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitCalculations (unitCalculations);
		proc.setUnitUtils (unitUtils);
		proc.setRandomUtils (random);
		
		// Run test
		final DamageCalculationMessage msg = new DamageCalculationMessage ();
		assertEquals (3, proc.calculateDamage (attacker, defender, attackingPlayer, defendingPlayer, CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			msg, players, spells, combatAreaEffects, db));
		
		// Check the message that got sent to the attacker
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertSame (msg, attackingMsgs.getMessages ().get (0));
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_AND_DEFENCE_STATISTICS, msg.getMessageType ());
		assertEquals (22, msg.getAttackerUnitURN ().intValue ());
		assertEquals (33, msg.getDefenderUnitURN ().intValue ());
		assertEquals (CommonDatabaseConstants.VALUE_UNIT_ATTRIBUTE_ID_MELEE_ATTACK, msg.getAttackAttributeID ());
		
		assertEquals (6, msg.getAttackerFigures ().intValue ());
		assertEquals (3, msg.getAttackStrength ().intValue ());
		assertEquals (18, msg.getPotentialDamage ().intValue ());
		assertEquals (4, msg.getChanceToHit ().intValue ());
		assertEquals (72, msg.getTenTimesAverageDamage ().intValue ());		// 18 hits * 0.4 chance = 7.2 average hits
		assertEquals (6, msg.getActualDamage ().intValue ());
		
		assertEquals (3, msg.getDefenderFigures ().intValue ());
		assertEquals (4, msg.getDefenceStrength ().intValue ());
		assertEquals (5, msg.getChanceToDefend ().intValue ());
		assertEquals (20, msg.getTenTimesAverageBlock ().intValue ());		// 4 shields * 0.5 chance = 2.0 average blocked
		assertEquals ("1,2", msg.getActualBlockedHits ());		// 1st figure blocked 1 hit, 2nd figure blocked 2 hits
	}
}

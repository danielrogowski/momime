package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtilsImpl;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.calculations.CombatMoveType;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.KillUnitMessage;
import momime.common.messages.servertoclient.MoveUnitInCombatMessage;
import momime.common.messages.servertoclient.MoveUnitInCombatReason;
import momime.common.messages.servertoclient.SetCombatPlayerMessage;
import momime.common.messages.servertoclient.SetUnitIntoOrTakeUnitOutOfCombatMessage;
import momime.common.messages.servertoclient.StartCombatMessage;
import momime.common.messages.servertoclient.StartCombatMessageUnit;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.CombatPlayers;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryGridCellUtils;
import momime.common.utils.UnitUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.DummyServerToClientConnection;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.ai.CombatAI;
import momime.server.ai.CombatAIMovementResult;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;

/**
 * Tests the CombatProcessingImpl class
 */
public final class TestCombatProcessingImpl extends ServerTestData
{
	/**
	 * Tests the determineMaxUnitsInRow method
	 * This does a mock setup for a defender in a city with city walls (see layout pattern in the comments of the main method)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineMaxUnitsInRow () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat map
		final CoordinateSystem combatMapCoordinateSystem = createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = createCombatMap ();
		
		// Fake impassable tile
		final MomCombatTile impassable = new MomCombatTile ();
		
		// This is a copy of the pattern layout of city wall corners + wizard's fortress, i.e. BL99 and CTB02 combatMapElements from the server XML
		final int [] [] wallsAndFortress = new int [] [] {{3, 8}, {4, 9}, {3, 6}, {1, 9}, {3, 12}};
		
		for (final int [] coords : wallsAndFortress)
			combatMap.getRow ().get (coords [1]).getCell ().set (coords [0], impassable);
		
		// Set up test object
		final UnitCalculations calc = mock (UnitCalculations.class);
		when (calc.calculateDoubleMovementToEnterCombatTile (impassable, db)).thenReturn (-1);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitCalculations (calc);
		proc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		final List<Integer> maxUnitsInRow = proc.determineMaxUnitsInRow (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y, CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FACING,
			CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_ROWS, combatMapCoordinateSystem, combatMap, db);
		
		// Check results
		assertEquals (4, maxUnitsInRow.size ());
		assertEquals (2, maxUnitsInRow.get (0).intValue ());
		assertEquals (4, maxUnitsInRow.get (1).intValue ());
		assertEquals (3, maxUnitsInRow.get (2).intValue ());
		assertEquals (2, maxUnitsInRow.get (3).intValue ());
	}
	
	/**
	 * Tests the calculateUnitCombatClass method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitCombatClass () throws Exception
	{
		// One of each kind of test unit
		final ExpandedUnitDetails dwarfHero = mock (ExpandedUnitDetails.class);
		when (dwarfHero.isHero ()).thenReturn (true);
		when (dwarfHero.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		when (dwarfHero.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (1);
		
		final ExpandedUnitDetails spearmen = mock (ExpandedUnitDetails.class);
		when (spearmen.isHero ()).thenReturn (false);
		when (spearmen.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		when (spearmen.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (1);
		
		final ExpandedUnitDetails archerHero = mock (ExpandedUnitDetails.class);
		when (archerHero.isHero ()).thenReturn (true);
		when (archerHero.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (true);
		when (archerHero.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (1);
		
		final ExpandedUnitDetails bowmen = mock (ExpandedUnitDetails.class);
		when (bowmen.isHero ()).thenReturn (false);
		when (bowmen.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (true);
		when (bowmen.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (1);
		
		final ExpandedUnitDetails settlers = mock (ExpandedUnitDetails.class);

		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		
		// Check results
		assertEquals (1, proc.calculateUnitCombatClass (dwarfHero));
		assertEquals (2, proc.calculateUnitCombatClass (spearmen));
		assertEquals (3, proc.calculateUnitCombatClass (archerHero));
		assertEquals (4, proc.calculateUnitCombatClass (bowmen));
		assertEquals (5, proc.calculateUnitCombatClass (settlers));
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Message to populate
		final StartCombatMessage msg = new StartCombatMessage ();
		
		// This isn't used directly, but easier to do the checks at the end if we have a true FOW memory object
		final List<FogOfWarMemory> FOWs = new ArrayList<FogOfWarMemory> ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();

		// Combat map
		final CoordinateSystem combatMapCoordinateSystem = createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatMap = createCombatMap ();
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);

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
		final List<ExpandedUnitDetails> trueExpandedUnits = new ArrayList<ExpandedUnitDetails> ();
		final UnitUtils unitUtils = mock (UnitUtils.class);
		for (int n = 1; n <= 3; n++)
		{
			final MemoryUnit tu = new MemoryUnit ();
			tu.setUnitURN (n);
			tu.setOwningPlayerID (attackingPD.getPlayerID ());
			
			final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
			when (xu.getUnitURN ()).thenReturn (n);
			trueExpandedUnits.add (xu);
			
			unitsToPosition.add (new MemoryUnitAndCombatClass (xu, 0));
			trueMap.getUnit ().add (tu);
			
			// Routine expects attacker and defender to both have unit in their memory
			final MemoryUnit atkUnit = new MemoryUnit ();
			atkUnit.setUnitURN (n);
			atkUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			attackingPriv.getFogOfWarMemory ().getUnit ().add (atkUnit);
			when (unitUtils.findUnitURN (n, attackingPriv.getFogOfWarMemory ().getUnit (), "placeCombatUnits-A")).thenReturn (atkUnit);

			final MemoryUnit defUnit = new MemoryUnit ();
			defUnit.setUnitURN (n);
			defUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			defendingPriv.getFogOfWarMemory ().getUnit ().add (defUnit);
			when (unitUtils.findUnitURN (n, defendingPriv.getFogOfWarMemory ().getUnit (), "placeCombatUnits-D")).thenReturn (defUnit);
		}
		
		// Set up object to test
		final UnitCalculations calc = mock (UnitCalculations.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setUnitCalculations (calc);
		proc.setUnitUtils (unitUtils);
		proc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		
		// Run method
		proc.placeCombatUnits (combatLocation, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_X,
			CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FRONT_ROW_CENTRE_Y, CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING,
			UnitCombatSideID.ATTACKER, unitsToPosition, unitsInRow, msg, attackingPlayer, defendingPlayer, combatMapCoordinateSystem, combatMap, db);
		
		// Check updates to server's true memory, via mocks
		final ExpandedUnitDetails xu1 = trueExpandedUnits.get (0);
		verify (xu1).setCombatLocation (combatLocation);
		verify (xu1).setCombatPosition (new MapCoordinates2DEx (7, 20));
		verify (xu1).setCombatHeading (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING);
		verify (xu1).setCombatSide (UnitCombatSideID.ATTACKER);
		
		final ExpandedUnitDetails xu2 = trueExpandedUnits.get (1);
		verify (xu2).setCombatLocation (combatLocation);
		verify (xu2).setCombatPosition (new MapCoordinates2DEx (7, 21));
		verify (xu2).setCombatHeading (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING);
		verify (xu2).setCombatSide (UnitCombatSideID.ATTACKER);

		final ExpandedUnitDetails xu3 = trueExpandedUnits.get (2);
		verify (xu3).setCombatLocation (combatLocation);
		verify (xu3).setCombatPosition (new MapCoordinates2DEx (8, 20));
		verify (xu3).setCombatHeading (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING);
		verify (xu3).setCombatSide (UnitCombatSideID.ATTACKER);
		
		// Check attacker's and defender's memory
		for (final FogOfWarMemory fow : FOWs)
		{
			assertEquals (3, fow.getUnit ().size ());
			
			final MemoryUnit unit1 = fow.getUnit ().get (0);
			assertEquals (1, unit1.getUnitURN ());
			assertSame (combatLocation, unit1.getCombatLocation ());
			assertEquals (7, unit1.getCombatPosition ().getX ());
			assertEquals (20, unit1.getCombatPosition ().getY ());
			assertEquals (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING, unit1.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.ATTACKER, unit1.getCombatSide ());
			
			final MemoryUnit unit2 = fow.getUnit ().get (1);
			assertEquals (2, unit2.getUnitURN ());
			assertSame (combatLocation, unit2.getCombatLocation ());
			assertEquals (7, unit2.getCombatPosition ().getX ());
			assertEquals (21, unit2.getCombatPosition ().getY ());
			assertEquals (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING, unit2.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.ATTACKER, unit2.getCombatSide ());

			final MemoryUnit unit3 = fow.getUnit ().get (2);
			assertEquals (3, unit3.getUnitURN ());
			assertSame (combatLocation, unit3.getCombatLocation ());
			assertEquals (8, unit3.getCombatPosition ().getX ());
			assertEquals (20, unit3.getCombatPosition ().getY ());
			assertEquals (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING, unit3.getCombatHeading ().intValue ());
			assertEquals (UnitCombatSideID.ATTACKER, unit3.getCombatSide ());
		}

		// Check message
		assertEquals (3, msg.getUnitPlacement ().size ());

		final StartCombatMessageUnit unit1 = msg.getUnitPlacement ().get (0);
		assertEquals (1, unit1.getUnitURN ());
		assertEquals (7, unit1.getCombatPosition ().getX ());
		assertEquals (20, unit1.getCombatPosition ().getY ());
		assertEquals (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING, unit1.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, unit1.getCombatSide ());

		final StartCombatMessageUnit unit2 = msg.getUnitPlacement ().get (1);
		assertEquals (2, unit2.getUnitURN ());
		assertEquals (7, unit2.getCombatPosition ().getX ());
		assertEquals (21, unit2.getCombatPosition ().getY ());
		assertEquals (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING, unit2.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, unit2.getCombatSide ());

		final StartCombatMessageUnit unit3 = msg.getUnitPlacement ().get (2);
		assertEquals (3, unit3.getUnitURN ());
		assertEquals (8, unit3.getCombatPosition ().getX ());
		assertEquals (20, unit3.getCombatPosition ().getY ());
		assertEquals (CombatStartAndEndImpl.COMBAT_SETUP_ATTACKER_FACING, unit3.getCombatHeading ());
		assertEquals (UnitCombatSideID.ATTACKER, unit3.getCombatSide ());
	}
	
	/**
	 * Tests the progressCombat method on a combat just starting with an AI player attacking a human player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressCombat_AIAttackingHuman_Starting () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (false);
		attackingPd.setPlayerID (-1);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (true);
		defendingPd.setPlayerID (3);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);

		final DummyServerToClientConnection defendingMsgs = new DummyServerToClientConnection ();
		defendingPlayer.setConnection (defendingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "progressCombat")).thenReturn (defendingPlayer);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setSpellCastThisCombatTurn (true);

		// Players in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players)).thenReturn (combatPlayers);
				
		// Set up object to test
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		final CombatEndTurn combatEndTurn = mock (CombatEndTurn.class);
		final FogOfWarMidTurnMultiChanges multi = mock (FogOfWarMidTurnMultiChanges.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setCombatMapUtils (combatMapUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setCombatEndTurn (combatEndTurn);
		proc.setFogOfWarMidTurnMultiChanges (multi);
		
		// Run method
		proc.progressCombat (combatLocation, true, false, mom);
		
		// Check player set correctly on server
		assertEquals (defendingPd.getPlayerID (), gc.getCombatCurrentPlayerID ());
		
		// Check player set correctly on client
		assertEquals (1, defendingMsgs.getMessages ().size ());
		assertEquals (SetCombatPlayerMessage.class.getName (), defendingMsgs.getMessages ().get (0).getClass ().getName ());
		final SetCombatPlayerMessage msg = (SetCombatPlayerMessage) defendingMsgs.getMessages ().get (0);
		assertEquals (combatLocation, msg.getCombatLocation ());
		assertEquals (defendingPd.getPlayerID ().intValue (), msg.getPlayerID ());
		
		// Check other setup
		assertNull (gc.isSpellCastThisCombatTurn ());
		verify (unitCalc, times (1)).resetUnitCombatMovement (defendingPd.getPlayerID (), combatLocation, players, trueMap, db);
	}

	/**
	 * Tests the progressCombat method on a combat with an AI player attacking a human player where we've just finished taking a turn
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressCombat_AIAttackingHuman_Continuing () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (false);
		attackingPd.setPlayerID (-1);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (true);
		defendingPd.setPlayerID (3);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);

		final DummyServerToClientConnection defendingMsgs = new DummyServerToClientConnection ();
		defendingPlayer.setConnection (defendingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "progressCombat")).thenReturn (defendingPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "progressCombat")).thenReturn (attackingPlayer);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setSpellCastThisCombatTurn (true);

		// Players in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players)).thenReturn (combatPlayers);
		
		// Defender/human player just finished turn
		gc.setCombatCurrentPlayerID (defendingPd.getPlayerID ());
		
		// AI player takes their turn
		final CombatAI ai = mock (CombatAI.class);
		when (ai.aiCombatTurn (combatLocation, attackingPlayer, mom)).thenReturn (CombatAIMovementResult.MOVED_OR_ATTACKED);
				
		// Set up object to test
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		final CombatEndTurn combatEndTurn = mock (CombatEndTurn.class);
		final FogOfWarMidTurnMultiChanges multi = mock (FogOfWarMidTurnMultiChanges.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setCombatMapUtils (combatMapUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setCombatAI (ai);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setCombatEndTurn (combatEndTurn);
		proc.setFogOfWarMidTurnMultiChanges (multi);
		
		// Run method
		proc.progressCombat (combatLocation, false, false, mom);
		
		// Check the AI player took their turn
		verify (ai, times (1)).aiCombatTurn (combatLocation, attackingPlayer, mom);
		
		// Check player set correctly on server
		assertEquals (defendingPd.getPlayerID (), gc.getCombatCurrentPlayerID ());
		
		// Check player set correctly on client (attacker, then back to defender)
		assertEquals (2, defendingMsgs.getMessages ().size ());
		
		assertEquals (SetCombatPlayerMessage.class.getName (), defendingMsgs.getMessages ().get (0).getClass ().getName ());
		final SetCombatPlayerMessage msg1 = (SetCombatPlayerMessage) defendingMsgs.getMessages ().get (0);
		assertEquals (combatLocation, msg1.getCombatLocation ());
		assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getPlayerID ());

		assertEquals (SetCombatPlayerMessage.class.getName (), defendingMsgs.getMessages ().get (1).getClass ().getName ());
		final SetCombatPlayerMessage msg2 = (SetCombatPlayerMessage) defendingMsgs.getMessages ().get (1);
		assertEquals (combatLocation, msg2.getCombatLocation ());
		assertEquals (defendingPd.getPlayerID ().intValue (), msg2.getPlayerID ());
		
		// Check other setup
		assertNull (gc.isSpellCastThisCombatTurn ());
		verify (unitCalc, times (1)).resetUnitCombatMovement (defendingPd.getPlayerID (), combatLocation, players, trueMap, db);
	}

	/**
	 * Tests the progressCombat where a human player hits Auto after being attacked by an AI player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressCombat_AIAttackingHuman_Auto () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (false);
		attackingPd.setPlayerID (-1);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (true);
		defendingPd.setPlayerID (3);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);

		final DummyServerToClientConnection defendingMsgs = new DummyServerToClientConnection ();
		defendingPlayer.setConnection (defendingMsgs);
		
		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "progressCombat")).thenReturn (defendingPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "progressCombat")).thenReturn (attackingPlayer);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setSpellCastThisCombatTurn (true);

		// Players in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players)).thenReturn (combatPlayers);

		// Its the Defender/human player's turn
		gc.setCombatCurrentPlayerID (defendingPd.getPlayerID ());
		
		// Both players have something useful to do
		final CombatAI ai = mock (CombatAI.class);
		when (ai.aiCombatTurn (combatLocation, defendingPlayer, mom)).thenReturn (CombatAIMovementResult.MOVED_OR_ATTACKED);
		when (ai.aiCombatTurn (combatLocation, attackingPlayer, mom)).thenReturn (CombatAIMovementResult.MOVED_OR_ATTACKED);
		
		// Set up object to test
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		final CombatEndTurn combatEndTurn = mock (CombatEndTurn.class);
		final FogOfWarMidTurnMultiChanges multi = mock (FogOfWarMidTurnMultiChanges.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setCombatMapUtils (combatMapUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setCombatAI (ai);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setCombatEndTurn (combatEndTurn);
		proc.setFogOfWarMidTurnMultiChanges (multi);
		
		// Run method
		proc.progressCombat (combatLocation, false, true, mom);

		// Check the AI played our turn for us, then took their turn
		verify (ai, times (1)).aiCombatTurn (combatLocation, defendingPlayer, mom);
		verify (ai, times (1)).aiCombatTurn (combatLocation, attackingPlayer, mom);
		
		// Check its now the defenders turn again (the method doesn't loop despite the human player
		// being on auto, to give the client a chance to turn auto off again)
		assertEquals (defendingPd.getPlayerID (), gc.getCombatCurrentPlayerID ());
		
		// Check player set correctly on client (attacker after AI takes our defender's turn, then back to defender)
		assertEquals (2, defendingMsgs.getMessages ().size ());
		
		assertEquals (SetCombatPlayerMessage.class.getName (), defendingMsgs.getMessages ().get (0).getClass ().getName ());
		final SetCombatPlayerMessage msg1 = (SetCombatPlayerMessage) defendingMsgs.getMessages ().get (0);
		assertEquals (combatLocation, msg1.getCombatLocation ());
		assertEquals (attackingPd.getPlayerID ().intValue (), msg1.getPlayerID ());

		assertEquals (SetCombatPlayerMessage.class.getName (), defendingMsgs.getMessages ().get (1).getClass ().getName ());
		final SetCombatPlayerMessage msg2 = (SetCombatPlayerMessage) defendingMsgs.getMessages ().get (1);
		assertEquals (combatLocation, msg2.getCombatLocation ());
		assertEquals (defendingPd.getPlayerID ().intValue (), msg2.getPlayerID ());
		
		// Check other setup
		assertNull (gc.isSpellCastThisCombatTurn ());
		verify (unitCalc, times (1)).resetUnitCombatMovement (attackingPd.getPlayerID (), combatLocation, players, trueMap, db);
		verify (unitCalc, times (1)).resetUnitCombatMovement (defendingPd.getPlayerID (), combatLocation, players, trueMap, db);
	}
	
	/**
	 * Tests the progressCombat method on a combat with two AI players fighting each other
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProgressCombat_AIOnly () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Attacking player
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (false);
		attackingPd.setPlayerID (-1);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		// Defending player
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-2);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);

		// Players list
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session utils
		final MultiplayerSessionServerUtils multiplayerSessionServerUtils = mock (MultiplayerSessionServerUtils.class);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, defendingPd.getPlayerID (), "progressCombat")).thenReturn (defendingPlayer);
		when (multiplayerSessionServerUtils.findPlayerWithID (players, attackingPd.getPlayerID (), "progressCombat")).thenReturn (attackingPlayer);
		
		// Session description
		final MomSessionDescription sd = new MomSessionDescription ();
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getSessionDescription ()).thenReturn (sd);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		final ServerGridCellEx gc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		gc.setSpellCastThisCombatTurn (true);

		// Players in combat, after a few turns the attacker wins
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		final CombatPlayers attackerWins = new CombatPlayers (attackingPlayer, null);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players)).thenReturn
			(combatPlayers, combatPlayers, combatPlayers, combatPlayers, attackerWins);
		
		// Defender/human player just finished turn
		gc.setCombatCurrentPlayerID (defendingPd.getPlayerID ());
		
		// Both players have something useful to do
		final CombatAI ai = mock (CombatAI.class);
		when (ai.aiCombatTurn (combatLocation, defendingPlayer, mom)).thenReturn (CombatAIMovementResult.MOVED_OR_ATTACKED);
		when (ai.aiCombatTurn (combatLocation, attackingPlayer, mom)).thenReturn (CombatAIMovementResult.MOVED_OR_ATTACKED);
				
		// Set up object to test
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		final CombatEndTurn combatEndTurn = mock (CombatEndTurn.class);
		final FogOfWarMidTurnMultiChanges multi = mock (FogOfWarMidTurnMultiChanges.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setCombatMapUtils (combatMapUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setCombatAI (ai);
		proc.setMultiplayerSessionServerUtils (multiplayerSessionServerUtils);
		proc.setCombatEndTurn (combatEndTurn);
		proc.setFogOfWarMidTurnMultiChanges (multi);
		
		// Run method
		proc.progressCombat (combatLocation, true, false, mom);
		
		// Check each AI player had 2 turns
		verify (ai, times (2)).aiCombatTurn (combatLocation, defendingPlayer, mom);
		verify (ai, times (2)).aiCombatTurn (combatLocation, attackingPlayer, mom);
		
		// Attacker had their turn last
		assertEquals (attackingPd.getPlayerID (), gc.getCombatCurrentPlayerID ());
		
		// Check other setup
		assertNull (gc.isSpellCastThisCombatTurn ());
		verify (unitCalc, times (2)).resetUnitCombatMovement (defendingPd.getPlayerID (), combatLocation, players, trueMap, db);
		verify (unitCalc, times (2)).resetUnitCombatMovement (attackingPd.getPlayerID (), combatLocation, players, trueMap, db);
	}
	
	/**
	 * Tests the purgeDeadUnitsAndCombatSummonsFromCombat method when we attacked another player
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPurgeDeadUnitsAndCombatSummonsFromCombat_AttackingOtherPlayer () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		final UnitEx hero = new UnitEx ();
		hero.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		final UnitEx phantomWarriors = new UnitEx ();
		phantomWarriors.setUnitMagicRealm ("MB01");
		
		when (db.findUnit ("UN102", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (longbowmen);
		when (db.findUnit ("UN002", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (hero);
		when (db.findUnit ("UN193", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (phantomWarriors);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
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
		attackerAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, settings, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		assertEquals (2, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertNull (tellAttackerToRemoveAttackersDeadUnit.getNewStatus ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertNull (tellAttackerToRemoveDefendersDeadUnit.getNewStatus ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getUnitURN ());

		assertEquals (2, defendingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellDefenderToRemoveAttackersDeadUnit = (KillUnitMessage) defendingPlayerConnection.getMessages ().get (0);
		assertNull (tellDefenderToRemoveAttackersDeadUnit.getNewStatus ());
		assertEquals (3, tellDefenderToRemoveAttackersDeadUnit.getUnitURN ());
		final KillUnitMessage tellDefenderToRemoveDefendersDeadUnit = (KillUnitMessage) defendingPlayerConnection.getMessages ().get (1);
		assertNull (tellDefenderToRemoveDefendersDeadUnit.getNewStatus ());
		assertEquals (8, tellDefenderToRemoveDefendersDeadUnit.getUnitURN ());
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx tt = new TileTypeEx ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		final UnitEx hero = new UnitEx ();
		hero.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		final UnitEx phantomWarriors = new UnitEx ();
		phantomWarriors.setUnitMagicRealm ("MB01");
		
		when (db.findUnit ("UN102", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (longbowmen);
		when (db.findUnit ("UN002", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (hero);
		when (db.findUnit ("UN193", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (phantomWarriors);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
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
		attackerAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, settings, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		assertEquals (2, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertNull (tellAttackerToRemoveAttackersDeadUnit.getNewStatus ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertNull (tellAttackerToRemoveDefendersDeadUnit.getNewStatus ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getUnitURN ());

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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx tt = new TileTypeEx ();
		tt.setMagicRealmID ("X");
		when (db.findTileType ("TT12", "isNodeLairTower")).thenReturn (tt);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		final UnitEx hero = new UnitEx ();
		hero.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		final UnitEx phantomWarriors = new UnitEx ();
		phantomWarriors.setUnitMagicRealm ("MB01");
		
		when (db.findUnit ("UN102", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (longbowmen);
		when (db.findUnit ("UN002", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (hero);
		when (db.findUnit ("UN193", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (phantomWarriors);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
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
		attackerAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		final MemoryUnit defenderAliveLongbowmen = new MemoryUnit ();
		defenderAliveLongbowmen.setUnitURN (7);
		defenderAliveLongbowmen.setUnitID ("UN102");
		defenderAliveLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderAliveLongbowmen.setStatus (UnitStatusID.ALIVE);
		defenderAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (defenderAliveLongbowmen);
		
		final MemoryUnit defenderDeadLongbowmen = new MemoryUnit ();
		defenderDeadLongbowmen.setUnitURN (8);
		defenderDeadLongbowmen.setUnitID ("UN102");
		defenderDeadLongbowmen.setOwningPlayerID (defendingPD.getPlayerID ());
		defenderDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		defenderDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (defenderDeadLongbowmen);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, defendingPlayer, trueMap, players, settings, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (defenderAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (defenderDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (2, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertNull (tellAttackerToRemoveAttackersDeadUnit.getNewStatus ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getUnitURN ());
		final KillUnitMessage tellAttackerToRemoveDefendersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (1);
		assertNull (tellAttackerToRemoveDefendersDeadUnit.getNewStatus ());
		assertEquals (8, tellAttackerToRemoveDefendersDeadUnit.getUnitURN ());

		// Defender is now a computer player so gets no messages

		// Same units must also get removed from players' memory on the server
		verify (unitUtils, times (1)).removeUnitURN (3, attackingPriv.getFogOfWarMemory ().getUnit ());
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		final UnitEx hero = new UnitEx ();
		hero.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		final UnitEx phantomWarriors = new UnitEx ();
		phantomWarriors.setUnitMagicRealm ("MB01");
		
		when (db.findUnit ("UN102", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (longbowmen);
		when (db.findUnit ("UN002", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (hero);
		when (db.findUnit ("UN193", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (phantomWarriors);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
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
		attackerAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, null, trueMap, players, settings, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertNull (tellAttackerToRemoveAttackersDeadUnit.getNewStatus ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getUnitURN ());

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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final UnitEx longbowmen = new UnitEx ();
		longbowmen.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL);
		
		final UnitEx hero = new UnitEx ();
		hero.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
		
		final UnitEx phantomWarriors = new UnitEx ();
		phantomWarriors.setUnitMagicRealm ("MB01");
		
		when (db.findUnit ("UN102", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (longbowmen);
		when (db.findUnit ("UN002", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (hero);
		when (db.findUnit ("UN193", "purgeDeadUnitsAndCombatSummonsFromCombat")).thenReturn (phantomWarriors);
		
		// Session description
		final FogOfWarSetting settings = new FogOfWarSetting ();
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);
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
		attackerAliveLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveLongbowmen);
		
		final MemoryUnit attackerAliveHero = new MemoryUnit ();
		attackerAliveHero.setUnitURN (2);
		attackerAliveHero.setUnitID ("UN002");
		attackerAliveHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAliveHero.setStatus (UnitStatusID.ALIVE);
		attackerAliveHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerAliveHero);

		final MemoryUnit attackerDeadLongbowmen = new MemoryUnit ();
		attackerDeadLongbowmen.setUnitURN (3);
		attackerDeadLongbowmen.setUnitID ("UN102");
		attackerDeadLongbowmen.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmen.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmen.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmen);
		
		final MemoryUnit attackerDeadHero = new MemoryUnit ();
		attackerDeadHero.setUnitURN (4);
		attackerDeadHero.setUnitID ("UN002");
		attackerDeadHero.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadHero.setStatus (UnitStatusID.DEAD);
		attackerDeadHero.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		trueMap.getUnit ().add (attackerDeadHero);

		final MemoryUnit attackerAlivePhantomWarriors = new MemoryUnit ();
		attackerAlivePhantomWarriors.setUnitURN (5);
		attackerAlivePhantomWarriors.setUnitID ("UN193");
		attackerAlivePhantomWarriors.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerAlivePhantomWarriors.setStatus (UnitStatusID.ALIVE);
		attackerAlivePhantomWarriors.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		attackerAlivePhantomWarriors.setWasSummonedInCombat (true);
		trueMap.getUnit ().add (attackerAlivePhantomWarriors);

		final MemoryUnit attackerDeadLongbowmenInADifferentCombat = new MemoryUnit ();
		attackerDeadLongbowmenInADifferentCombat.setUnitURN (6);
		attackerDeadLongbowmenInADifferentCombat.setUnitID ("UN102");
		attackerDeadLongbowmenInADifferentCombat.setOwningPlayerID (attackingPD.getPlayerID ());
		attackerDeadLongbowmenInADifferentCombat.setStatus (UnitStatusID.DEAD);
		attackerDeadLongbowmenInADifferentCombat.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		trueMap.getUnit ().add (attackerDeadLongbowmenInADifferentCombat);
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Set up object to test
		final FogOfWarMidTurnChanges fow = mock (FogOfWarMidTurnChanges.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setFogOfWarMidTurnChanges (fow);
		proc.setUnitUtils (unitUtils);
		
		// Run test
		proc.purgeDeadUnitsAndCombatSummonsFromCombat (combatLocation, attackingPlayer, null, trueMap, players, settings, db);

		// Verify regular kill routine called on the right units
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerAliveHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerDeadLongbowmen, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadHero, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (1)).killUnitOnServerAndClients (attackerAlivePhantomWarriors, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		verify (fow, times (0)).killUnitOnServerAndClients (attackerDeadLongbowmenInADifferentCombat, KillUnitActionID.PERMANENT_DAMAGE, trueMap, players, settings, db);
		
		// Alive units are still alive, dead hero stays a dead hero, but server should tell clients to remove the dead unit via custom message
		// Phantom warriors are removed by the regular routine which is mocked out, so doesn't get recorded here
		// Alive defender gets removed too since its a monster in a node
		assertEquals (1, attackingPlayerConnection.getMessages ().size ());
		final KillUnitMessage tellAttackerToRemoveAttackersDeadUnit = (KillUnitMessage) attackingPlayerConnection.getMessages ().get (0);
		assertNull (tellAttackerToRemoveAttackersDeadUnit.getNewStatus ());
		assertEquals (3, tellAttackerToRemoveAttackersDeadUnit.getUnitURN ());

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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		trueUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		trueUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		trueUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (defendingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		trueUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
		trueUnit.setWasSummonedInCombat (true);
		
		// Both players and the outside observer all already know about the unit - outside observer can see it because was the attacker who summoned it
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit playerUnit = new MemoryUnit ();
			playerUnit.setUnitURN (1);
			playerUnit.setUnitID ("UN193");
			playerUnit.setOwningPlayerID (attackingPD.getPlayerID ());
			playerUnit.setStatus (UnitStatusID.ALIVE);
			playerUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		trueUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
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
			playerUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
			playerUnit.setWasSummonedInCombat (true);
			
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			priv.getFogOfWarMemory ().getUnit ().add (playerUnit);
		}
		
		// Location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
		
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
			playerUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square

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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx tt = new TileTypeEx ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
		
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
			playerUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square

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
		proc.setMemoryGridCellUtils (mock (MemoryGridCellUtils.class));
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx tt = new TileTypeEx ();
		when (db.findTileType ("TT01", "isNodeLairTower")).thenReturn (tt);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		
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
			playerUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));

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
		proc.setMemoryGridCellUtils (mock (MemoryGridCellUtils.class));
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx tt = new TileTypeEx ();
		tt.setMagicRealmID ("X");
		when (db.findTileType ("TT12", "isNodeLairTower")).thenReturn (tt);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (attackingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square
		
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
			playerUnit.setUnitLocation (new MapCoordinates3DEx (19, 10, 1));		// Attacking from adjacent square

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
		proc.setMemoryGridCellUtils (mock (MemoryGridCellUtils.class));
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx tt = new TileTypeEx ();
		tt.setMagicRealmID ("X");
		when (db.findTileType ("TT12", "isNodeLairTower")).thenReturn (tt);
		
		// Overland map
		final CoordinateSystem sys = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (sys);

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
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Combat position
		final MapCoordinates2DEx combatPosition = new MapCoordinates2DEx (7, 12);
		
		// True unit being summoned
		final MemoryUnit trueUnit = new MemoryUnit ();
		trueUnit.setUnitURN (1);
		trueUnit.setUnitID ("UN193");
		trueUnit.setOwningPlayerID (defendingPD.getPlayerID ());
		trueUnit.setStatus (UnitStatusID.ALIVE);
		trueUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		
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
			playerUnit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));

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
		proc.setMemoryGridCellUtils (mock (MemoryGridCellUtils.class));
		
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
	 * Tests the reduceMovementRemaining method
	 */
	@Test
	public final void testReduceMovementRemaining ()
	{
		// Test unit
		final MemoryUnit unit = new MemoryUnit ();
		unit.setDoubleCombatMovesLeft (8);

		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		
		// Normal reduction
		proc.reduceMovementRemaining (unit, 5);
		assertEquals (3, unit.getDoubleCombatMovesLeft ().intValue ());		
		
		// Try to put below 0
		proc.reduceMovementRemaining (unit, 5);
		assertEquals (0, unit.getDoubleCombatMovesLeft ().intValue ());		
	}
	
	/**
	 * Tests the okToMoveUnitInCombat method to make a move (rather than an attack)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOkToMoveUnitInCombat_Move () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem overlandMapCoordinateSystem = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapCoordinateSystem);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Human attacker
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (5);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (msgs);

		// AI defender
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session description
		final CombatMapSize combatMapSize = createCombatMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Where the combat is taking place
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		
		// Combat map
		final MapAreaOfCombatTiles combatMap = createCombatMap ();
		final ServerGridCellEx combatCell = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		combatCell.setCombatMap (combatMap);
		
		// True unit
		final MemoryUnit tu = new MemoryUnit ();
		tu.setDoubleCombatMovesLeft (6);

		final MapCoordinates2DEx tuMoveFrom = new MapCoordinates2DEx (1, 7);
		tu.setCombatPosition (tuMoveFrom);
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getMemoryUnit ()).thenReturn (tu);
		when (xu.getUnitURN ()).thenReturn (101);
		when (xu.getCombatLocation ()).thenReturn (combatLocation);
		
		// We need combatPosition to be read/written a few times and the updates from the getter to reflect in the setter.
		// So this is a bit awkward to set up with a mock.  Similarly with doubleCombatMovesLeft.
		when (xu.getCombatPosition ()).thenAnswer ((i) -> (MapCoordinates2DEx) tu.getCombatPosition ());
		when (xu.getDoubleCombatMovesLeft ()).thenAnswer ((i) -> tu.getDoubleCombatMovesLeft ());
		
		final ArgumentCaptor<MapCoordinates2DEx> capturePosition = ArgumentCaptor.forClass (MapCoordinates2DEx.class);
		doAnswer ((i) ->
		{
			tu.setCombatPosition (capturePosition.getValue ());
			return null;
		}).when (xu).setCombatPosition (capturePosition.capture ());

		final ArgumentCaptor<Integer> captureMovesLeft = ArgumentCaptor.forClass (Integer.class);
		doAnswer ((i) ->
		{
			tu.setDoubleCombatMovesLeft (captureMovesLeft.getValue ());
			return null;
		}).when (xu).setDoubleCombatMovesLeft (captureMovesLeft.capture ());
		
		// Players' memories of unit
		final MemoryUnit attackingPlayerMemoryOfUnit = new MemoryUnit ();
		attackingPlayerMemoryOfUnit.setCombatLocation (combatLocation);
		attackingPlayerMemoryOfUnit.setDoubleCombatMovesLeft (6);
		attackingPlayerMemoryOfUnit.setUnitURN (101);

		attackingPlayerMemoryOfUnit.setCombatPosition (new MapCoordinates2DEx (1, 7));

		final MemoryUnit defendingPlayerMemoryOfUnit = new MemoryUnit ();
		defendingPlayerMemoryOfUnit.setCombatLocation (combatLocation);
		defendingPlayerMemoryOfUnit.setDoubleCombatMovesLeft (6);
		defendingPlayerMemoryOfUnit.setUnitURN (101);

		defendingPlayerMemoryOfUnit.setCombatPosition (new MapCoordinates2DEx (1, 7));
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (101, attackingPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-A")).thenReturn (attackingPlayerMemoryOfUnit);
		when (unitUtils.findUnitURN (101, defendingPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-D")).thenReturn (defendingPlayerMemoryOfUnit);
		
		// Players in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players)).thenReturn (combatPlayers);
		
		// Where we want to move to
		final MapCoordinates2DEx moveTo = new MapCoordinates2DEx (3, 8);
		
		// Movement areas
		final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		final CombatMoveType [] [] movementTypes = new CombatMoveType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];

		// From 1,7 we move down-right to 2,8 and then right to 3,8 
		movementDirections [8] [2] = 4;
		movementDirections [8] [3] = 3;
		
		movementTypes [8] [3] = CombatMoveType.MOVE;
		
		// Movement points to enter each tile
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (8).getCell ().get (2), db)).thenReturn (2);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (8).getCell ().get (3), db)).thenReturn (1);
		
		// Non-flying unit
		when (xu.unitIgnoresCombatTerrain (db)).thenReturn (false);

		// Set up object to test
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		proc.setCombatMapUtils (combatMapUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setUnitUtils (unitUtils);
		
		// Run method
		assertFalse (proc.okToMoveUnitInCombat (xu, moveTo, MoveUnitInCombatReason.MANUAL, movementDirections, movementTypes, mom));
		
		// Check movement path messages
		assertEquals (2, msgs.getMessages ().size ());
		
		assertEquals (MoveUnitInCombatMessage.class.getName (), msgs.getMessages ().get (0).getClass ().getName ());
		final MoveUnitInCombatMessage msg1 = (MoveUnitInCombatMessage) msgs.getMessages ().get (0);
		assertEquals (101, msg1.getUnitURN ());
		assertEquals (4, msg1.getDirection ().intValue ());
		assertEquals (1, msg1.getMoveFrom ().getX ());
		assertEquals (7, msg1.getMoveFrom ().getY ());
		assertEquals (4, msg1.getDoubleCombatMovesLeft ());
		
		assertEquals (MoveUnitInCombatMessage.class.getName (), msgs.getMessages ().get (1).getClass ().getName ());
		final MoveUnitInCombatMessage msg2 = (MoveUnitInCombatMessage) msgs.getMessages ().get (1);
		assertEquals (101, msg2.getUnitURN ());
		assertEquals (3, msg2.getDirection ().intValue ());
		assertEquals (2, msg2.getMoveFrom ().getX ());
		assertEquals (8, msg2.getMoveFrom ().getY ());
		assertEquals (3, msg2.getDoubleCombatMovesLeft ());
		
		// Check the unit ended up where it was supposed to
		assertEquals (moveTo, tu.getCombatPosition ());
		assertEquals (moveTo, attackingPlayerMemoryOfUnit.getCombatPosition ());
		assertEquals (moveTo, defendingPlayerMemoryOfUnit.getCombatPosition ());
		
		// Check its movement got reduced
		assertEquals (3, tu.getDoubleCombatMovesLeft ().intValue ());
	}

	/**
	 * Tests the okToMoveUnitInCombat method to make a ranged attack
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOkToMoveUnitInCombat_Ranged () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem overlandMapCoordinateSystem = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapCoordinateSystem);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Human attacker
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (5);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (msgs);

		// AI defender
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session description
		final CombatMapSize combatMapSize = createCombatMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Where the combat is taking place
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		
		// Combat map
		final MapAreaOfCombatTiles combatMap = createCombatMap ();
		final ServerGridCellEx combatCell = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		combatCell.setCombatMap (combatMap);
		
		// True unit
		final MemoryUnit tu = new MemoryUnit ();
		tu.setDoubleCombatMovesLeft (6);

		final MapCoordinates2DEx tuMoveFrom = new MapCoordinates2DEx (1, 7);
		tu.setCombatPosition (tuMoveFrom);
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getMemoryUnit ()).thenReturn (tu);
		when (xu.getUnitURN ()).thenReturn (101);
		when (xu.getCombatLocation ()).thenReturn (combatLocation);
		
		// We need combatPosition to be read/written a few times and the updates from the getter to reflect in the setter.
		// So this is a bit awkward to set up with a mock.  Similarly with doubleCombatMovesLeft.
		when (xu.getCombatPosition ()).thenAnswer ((i) -> (MapCoordinates2DEx) tu.getCombatPosition ());
		when (xu.getDoubleCombatMovesLeft ()).thenAnswer ((i) -> tu.getDoubleCombatMovesLeft ());
		
		final ArgumentCaptor<MapCoordinates2DEx> capturePosition = ArgumentCaptor.forClass (MapCoordinates2DEx.class);
		doAnswer ((i) ->
		{
			tu.setCombatPosition (capturePosition.getValue ());
			return null;
		}).when (xu).setCombatPosition (capturePosition.capture ());

		final ArgumentCaptor<Integer> captureMovesLeft = ArgumentCaptor.forClass (Integer.class);
		doAnswer ((i) ->
		{
			tu.setDoubleCombatMovesLeft (captureMovesLeft.getValue ());
			return null;
		}).when (xu).setDoubleCombatMovesLeft (captureMovesLeft.capture ());
		
		// Players' memories of unit
		final MemoryUnit attackingPlayerMemoryOfUnit = new MemoryUnit ();
		attackingPlayerMemoryOfUnit.setCombatLocation (combatLocation);
		attackingPlayerMemoryOfUnit.setDoubleCombatMovesLeft (6);
		attackingPlayerMemoryOfUnit.setUnitURN (101);

		attackingPlayerMemoryOfUnit.setCombatPosition (new MapCoordinates2DEx (1, 7));

		final MemoryUnit defendingPlayerMemoryOfUnit = new MemoryUnit ();
		defendingPlayerMemoryOfUnit.setCombatLocation (combatLocation);
		defendingPlayerMemoryOfUnit.setDoubleCombatMovesLeft (6);
		defendingPlayerMemoryOfUnit.setUnitURN (101);

		defendingPlayerMemoryOfUnit.setCombatPosition (new MapCoordinates2DEx (1, 7));
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (101, attackingPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-A")).thenReturn (attackingPlayerMemoryOfUnit);
		when (unitUtils.findUnitURN (101, defendingPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-D")).thenReturn (defendingPlayerMemoryOfUnit);
		
		// Players in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players)).thenReturn (combatPlayers);
		
		// Where we want to move to
		final MapCoordinates2DEx moveTo = new MapCoordinates2DEx (3, 8);
		
		// Movement areas
		final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		final CombatMoveType [] [] movementTypes = new CombatMoveType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];

		// From 1,7 we move down-right to 2,8 and then right to 3,8 
		movementDirections [8] [2] = 4;
		movementDirections [8] [3] = 3;
		
		movementTypes [8] [3] = CombatMoveType.RANGED_UNIT;		// <---
		
		// Movement points to enter each tile
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (8).getCell ().get (2), db)).thenReturn (2);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (8).getCell ().get (3), db)).thenReturn (1);
		
		// The unit we're attacking
		final MemoryUnit defender = new MemoryUnit ();
		when (unitUtils.findAliveUnitInCombatAt (trueMap.getUnit (), combatLocation, moveTo)).thenReturn (defender);

		// Set up object to test
		final DamageProcessor damageProcessor = mock (DamageProcessor.class); 
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		proc.setCombatMapUtils (combatMapUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setUnitUtils (unitUtils);
		proc.setDamageProcessor (damageProcessor);
		
		// Run method
		assertFalse (proc.okToMoveUnitInCombat (xu, moveTo, MoveUnitInCombatReason.MANUAL, movementDirections, movementTypes, mom));
		
		// Check there were no movement path messages
		assertEquals (0, msgs.getMessages ().size ());
		
		// Check the unit stayed where it was
		assertEquals (tuMoveFrom, tu.getCombatPosition ());
		assertEquals (tuMoveFrom, attackingPlayerMemoryOfUnit.getCombatPosition ());
		assertEquals (tuMoveFrom, defendingPlayerMemoryOfUnit.getCombatPosition ());
		
		// Check its movement got zeroed
		assertEquals (0, tu.getDoubleCombatMovesLeft ().intValue ());
		
		// Check the attack happened
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);
		
		verify (damageProcessor, times (1)).resolveAttack (tu, defenders, attackingPlayer, defendingPlayer, null, null, 4,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, null, null, combatLocation, mom);
	}

	/**
	 * Tests the okToMoveUnitInCombat method to move 1 tile then make a melee attack
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testOkToMoveUnitInCombat_Melee () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final CoordinateSystem overlandMapCoordinateSystem = createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapCoordinateSystem);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Human attacker
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (5);

		final MomPersistentPlayerPrivateKnowledge attackingPriv = new MomPersistentPlayerPrivateKnowledge ();
		attackingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, attackingPriv, null, null);
		
		final DummyServerToClientConnection msgs = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (msgs);

		// AI defender
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (false);
		defendingPd.setPlayerID (-1);

		final MomPersistentPlayerPrivateKnowledge defendingPriv = new MomPersistentPlayerPrivateKnowledge ();
		defendingPriv.setFogOfWarMemory (new FogOfWarMemory ());
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, defendingPriv, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Session description
		final CombatMapSize combatMapSize = createCombatMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Where the combat is taking place
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (25, 15, 1);
		
		// Combat map
		final MapAreaOfCombatTiles combatMap = createCombatMap ();
		final ServerGridCellEx combatCell = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (15).getCell ().get (25);
		combatCell.setCombatMap (combatMap);
		
		// True unit
		final MemoryUnit tu = new MemoryUnit ();
		tu.setDoubleCombatMovesLeft (6);

		final MapCoordinates2DEx tuMoveFrom = new MapCoordinates2DEx (1, 7);
		tu.setCombatPosition (tuMoveFrom);
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getMemoryUnit ()).thenReturn (tu);
		when (xu.getUnitURN ()).thenReturn (101);
		when (xu.getCombatLocation ()).thenReturn (combatLocation);
		
		// We need combatPosition to be read/written a few times and the updates from the getter to reflect in the setter.
		// So this is a bit awkward to set up with a mock.  Similarly with doubleCombatMovesLeft.
		when (xu.getCombatPosition ()).thenAnswer ((i) -> (MapCoordinates2DEx) tu.getCombatPosition ());
		when (xu.getDoubleCombatMovesLeft ()).thenAnswer ((i) -> tu.getDoubleCombatMovesLeft ());
		
		final ArgumentCaptor<MapCoordinates2DEx> capturePosition = ArgumentCaptor.forClass (MapCoordinates2DEx.class);
		doAnswer ((i) ->
		{
			tu.setCombatPosition (capturePosition.getValue ());
			return null;
		}).when (xu).setCombatPosition (capturePosition.capture ());

		final ArgumentCaptor<Integer> captureMovesLeft = ArgumentCaptor.forClass (Integer.class);
		doAnswer ((i) ->
		{
			tu.setDoubleCombatMovesLeft (captureMovesLeft.getValue ());
			return null;
		}).when (xu).setDoubleCombatMovesLeft (captureMovesLeft.capture ());
		
		// Unit speed
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED)).thenReturn (3);
		
		// Players' memories of unit
		final MemoryUnit attackingPlayerMemoryOfUnit = new MemoryUnit ();
		attackingPlayerMemoryOfUnit.setCombatLocation (combatLocation);
		attackingPlayerMemoryOfUnit.setDoubleCombatMovesLeft (6);
		attackingPlayerMemoryOfUnit.setUnitURN (101);

		attackingPlayerMemoryOfUnit.setCombatPosition (new MapCoordinates2DEx (1, 7));

		final MemoryUnit defendingPlayerMemoryOfUnit = new MemoryUnit ();
		defendingPlayerMemoryOfUnit.setCombatLocation (combatLocation);
		defendingPlayerMemoryOfUnit.setDoubleCombatMovesLeft (6);
		defendingPlayerMemoryOfUnit.setUnitURN (101);

		defendingPlayerMemoryOfUnit.setCombatPosition (new MapCoordinates2DEx (1, 7));
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findUnitURN (101, attackingPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-A")).thenReturn (attackingPlayerMemoryOfUnit);
		when (unitUtils.findUnitURN (101, defendingPriv.getFogOfWarMemory ().getUnit (), "okToMoveUnitInCombat-D")).thenReturn (defendingPlayerMemoryOfUnit);
		
		// Players in combat
		final CombatPlayers combatPlayers = new CombatPlayers (attackingPlayer, defendingPlayer);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.determinePlayersInCombatFromLocation (combatLocation, trueMap.getUnit (), players)).thenReturn (combatPlayers);
		
		// Where we want to move to
		final MapCoordinates2DEx moveTo = new MapCoordinates2DEx (3, 8);
		
		// Movement areas
		final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		final CombatMoveType [] [] movementTypes = new CombatMoveType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];

		// From 1,7 we move down-right to 2,8 and then right to 3,8 
		movementDirections [8] [2] = 4;
		movementDirections [8] [3] = 3;
		
		movementTypes [8] [3] = CombatMoveType.MELEE_UNIT;
		
		// Movement points to enter each tile
		final UnitCalculations unitCalc = mock (UnitCalculations.class);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (8).getCell ().get (2), db)).thenReturn (2);
		when (unitCalc.calculateDoubleMovementToEnterCombatTile (combatMap.getRow ().get (8).getCell ().get (3), db)).thenReturn (1);

		// The unit we're attacking
		final MemoryUnit defender = new MemoryUnit ();
		when (unitUtils.findAliveUnitInCombatAt (trueMap.getUnit (), combatLocation, moveTo)).thenReturn (defender);

		// Set up object to test
		final DamageProcessor damageProcessor = mock (DamageProcessor.class);
		
		final CombatProcessingImpl proc = new CombatProcessingImpl ();
		proc.setCoordinateSystemUtils (new CoordinateSystemUtilsImpl ());
		proc.setCombatMapUtils (combatMapUtils);
		proc.setUnitCalculations (unitCalc);
		proc.setUnitUtils (unitUtils);
		proc.setDamageProcessor (damageProcessor);
		
		// Run method
		assertFalse (proc.okToMoveUnitInCombat (xu, moveTo, MoveUnitInCombatReason.MANUAL, movementDirections, movementTypes, mom));
		
		// Check movement path messages
		assertEquals (1, msgs.getMessages ().size ());
		
		assertEquals (MoveUnitInCombatMessage.class.getName (), msgs.getMessages ().get (0).getClass ().getName ());
		final MoveUnitInCombatMessage msg1 = (MoveUnitInCombatMessage) msgs.getMessages ().get (0);
		assertEquals (101, msg1.getUnitURN ());
		assertEquals (4, msg1.getDirection ().intValue ());
		assertEquals (1, msg1.getMoveFrom ().getX ());
		assertEquals (7, msg1.getMoveFrom ().getY ());
		assertEquals (4, msg1.getDoubleCombatMovesLeft ());
		
		// Check the unit ended up where it was supposed to
		final MapCoordinates2DEx middle = new MapCoordinates2DEx (2, 8);
		
		assertEquals (middle, tu.getCombatPosition ());
		assertEquals (middle, attackingPlayerMemoryOfUnit.getCombatPosition ());
		assertEquals (middle, defendingPlayerMemoryOfUnit.getCombatPosition ());
		
		// Check its movement got reduced
		assertEquals (1, tu.getDoubleCombatMovesLeft ().intValue ());

		// Check the attack happened
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);

		verify (damageProcessor, times (1)).resolveAttack (tu, defenders, attackingPlayer, defendingPlayer, null, null, 3,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, null, null, combatLocation, mom);
	}
}
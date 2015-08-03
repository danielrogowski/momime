package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.UntransmittedKillUnitActionID;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeID;
import momime.common.database.FogOfWarSetting;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.CaptureCityDecisionID;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.messages.servertoclient.DamageCalculationHeaderData;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.DamageCalculator;
import momime.server.database.AttackResolutionStepSvr;
import momime.server.database.AttackResolutionSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.ndg.map.CoordinateSystemType;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

/**
 * Tests the DamageProcessorImpl class
 */
public final class TestDamageProcessorImpl
{
	/**
	 * Tests the resolveAttack method on a melee attack which kills the defender (owned by the attackingPlayer)
	 * but the attacker (owned by the defendingPlayer) still has some figures left, and both sides still have units left so the combat continues
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResolveAttack_Melee_ContinuesCombat () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (5);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (true);
		defendingPd.setPlayerID (7);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		attacker.setDamageTaken (2);
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		defender.setDamageTaken (3);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Coordinate system
		final CombatMapSize combatMapCoordinateSystem = ServerTestData.createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.normalizeDirection (CoordinateSystemType.DIAMOND, 7+4)).thenReturn (7+4-8);

		// Attack resolution
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);

		final AttackResolutionSvr attackResolution = new AttackResolutionSvr ();
		when (attackResolutionProc.chooseAttackResolution (attacker, defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, players,
			gsk.getTrueMap ().getMaintainedSpell (), gsk.getTrueMap ().getCombatAreaEffect (), db)).thenReturn (attackResolution);
		
		final List<AttackResolutionStepSvr> steps = new ArrayList<AttackResolutionStepSvr> ();
		
		final List<List<AttackResolutionStepSvr>> stepNumbers = new ArrayList<List<AttackResolutionStepSvr>> ();
		stepNumbers.add (steps);
		when (attackResolutionProc.splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionSteps ())).thenReturn (stepNumbers);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Damage taken
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateAliveFigureCount (attacker, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (3);
		when (unitCalculations.calculateAliveFigureCount (defender, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (1, 0);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		final DamageCalculator calc = mock (DamageCalculator.class);
		
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setCoordinateSystemUtils (coordinateSystemUtils);
		proc.setDamageCalculator (calc);
		proc.setFogOfWarMidTurnChanges (midTurnSingle);
		proc.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		proc.setUnitCalculations (unitCalculations);
		proc.setCombatStartAndEnd (combatStartAndEnd);
		proc.setAttackResolutionProcessing (attackResolutionProc);
		
		// Need another surviving unit on each side, so the combat doesn't end
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit survivingUnit = new MemoryUnit ();
			survivingUnit.setOwningPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
			survivingUnit.setStatus (UnitStatusID.ALIVE);
			survivingUnit.setCombatPosition (new MapCoordinates2DEx (7, 9));
			survivingUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
			survivingUnit.setCombatSide ((thisPlayer == attackingPlayer) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			survivingUnit.setCombatHeading (1);
			trueMap.getUnit ().add (survivingUnit);
		}
		
		// Run method
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);
		
		proc.resolveAttack (attacker, defenders, attackingPlayer, defendingPlayer, 7,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, null, null, combatLocation, mom);

		// Ensure steps were processed
		verify (attackResolutionProc, times (1)).processAttackResolutionStep (attacker, defender, attackingPlayer, defendingPlayer, steps, null, players,
			gsk.getTrueMap ().getMaintainedSpell (), gsk.getTrueMap ().getCombatAreaEffect (), db);

		final List<DamageTypeID> specialDamageTypesApplied = new ArrayList<DamageTypeID> ();
		verify (midTurnSingle, times (1)).sendCombatDamageToClients (attacker, attacker.getOwningPlayerID (), defenders,
			null, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null,
			specialDamageTypesApplied, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		final ArgumentCaptor<DamageCalculationData> msg = ArgumentCaptor.forClass (DamageCalculationData.class); 
		
		verify (calc, times (1)).sendDamageCalculationMessage (eq (attackingPlayer), eq (defendingPlayer), msg.capture ());
		assertEquals (DamageCalculationHeaderData.class.getName (), msg.getValue ().getClass ().getName ());
		
		final DamageCalculationHeaderData data = (DamageCalculationHeaderData) msg.getValue ();
		assertEquals (DamageCalculationMessageTypeID.HEADER, data.getMessageType ());
		assertEquals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, data.getAttackAttributeID ());
		assertEquals (attacker.getOwningPlayerID (), data.getAttackerPlayerID ());
		assertEquals (attacker.getUnitURN (), data.getAttackerUnitURN ().intValue ());
		assertEquals (defender.getUnitURN (), data.getDefenderUnitURN ().intValue ());
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());
		
		// Check the dead unit was killed off, and exp given to the other side
		verify (midTurnSingle, times (1)).killUnitOnServerAndClients (defender, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		verify (midTurnSingle, times (0)).killUnitOnServerAndClients (attacker, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		
		verify (midTurnMulti, times (1)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		verify (midTurnMulti, times (0)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		
		verify (combatStartAndEnd, times (0)).combatEnded (eq (combatLocation), eq (attackingPlayer), eq (defendingPlayer), any (PlayerServerDetails.class), any (CaptureCityDecisionID.class), eq (mom));
	}

	/**
	 * Tests the resolveAttack method on a ranged attack which kills the defender (owned by the attackingPlayer)
	 * and it was their last unit so the combat ends
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResolveAttack_Ranged_EndsCombat () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setHuman (true);
		attackingPd.setPlayerID (5);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setHuman (true);
		defendingPd.setPlayerID (7);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);
		
		// Units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		attacker.setDamageTaken (2);
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		defender.setDamageTaken (3);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledgeEx gsk = new MomGeneralServerKnowledgeEx ();
		gsk.setTrueMap (trueMap);

		// Coordinate system
		final CombatMapSize combatMapCoordinateSystem = ServerTestData.createCombatMapSize ();
		sd.setCombatMapSize (combatMapCoordinateSystem);
		
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.normalizeDirection (CoordinateSystemType.DIAMOND, 7+4)).thenReturn (7+4-8);

		// Attack resolution
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);

		final AttackResolutionSvr attackResolution = new AttackResolutionSvr ();
		when (attackResolutionProc.chooseAttackResolution (attacker, defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, players,
			gsk.getTrueMap ().getMaintainedSpell (), gsk.getTrueMap ().getCombatAreaEffect (), db)).thenReturn (attackResolution);
		
		final List<AttackResolutionStepSvr> steps = new ArrayList<AttackResolutionStepSvr> ();
		
		final List<List<AttackResolutionStepSvr>> stepNumbers = new ArrayList<List<AttackResolutionStepSvr>> ();
		stepNumbers.add (steps);
		when (attackResolutionProc.splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionSteps ())).thenReturn (stepNumbers);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Damage taken
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateAliveFigureCount (attacker, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (3);
		when (unitCalculations.calculateAliveFigureCount (defender, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (1, 0);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		final DamageCalculator calc = mock (DamageCalculator.class);
		
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setCoordinateSystemUtils (coordinateSystemUtils);
		proc.setDamageCalculator (calc);
		proc.setFogOfWarMidTurnChanges (midTurnSingle);
		proc.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		proc.setUnitCalculations (unitCalculations);
		proc.setCombatStartAndEnd (combatStartAndEnd);
		proc.setAttackResolutionProcessing (attackResolutionProc);
		
		// The 'attacker' unit is still left alive because it still took no dmg, so put a unit in the list so the combat doesn't end for them (attacker is owned by defendingPlayer)
		final MemoryUnit survivingUnit = new MemoryUnit ();
		survivingUnit.setOwningPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
		survivingUnit.setStatus (UnitStatusID.ALIVE);
		survivingUnit.setCombatPosition (new MapCoordinates2DEx (7, 9));
		survivingUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		survivingUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		trueMap.getUnit ().add (survivingUnit);
		
		// Run method
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);
		
		proc.resolveAttack (attacker, defenders, attackingPlayer, defendingPlayer, 7,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, null, null, combatLocation, mom);
		
		// Ensure steps were processed
		verify (attackResolutionProc, times (1)).processAttackResolutionStep (attacker, defender, attackingPlayer, defendingPlayer, steps, null, players,
			gsk.getTrueMap ().getMaintainedSpell (), gsk.getTrueMap ().getCombatAreaEffect (), db);

		final List<DamageTypeID> specialDamageTypesApplied = new ArrayList<DamageTypeID> ();
		verify (midTurnSingle, times (1)).sendCombatDamageToClients (attacker, attacker.getOwningPlayerID (), defenders,
			null, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null,
			specialDamageTypesApplied, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		final ArgumentCaptor<DamageCalculationData> msg = ArgumentCaptor.forClass (DamageCalculationData.class); 
		
		verify (calc, times (1)).sendDamageCalculationMessage (eq (attackingPlayer), eq (defendingPlayer), msg.capture ());
		assertEquals (DamageCalculationHeaderData.class.getName (), msg.getValue ().getClass ().getName ());
		
		final DamageCalculationHeaderData data = (DamageCalculationHeaderData) msg.getValue ();
		assertEquals (DamageCalculationMessageTypeID.HEADER, data.getMessageType ());
		assertEquals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, data.getAttackAttributeID ());
		assertEquals (attacker.getOwningPlayerID (), data.getAttackerPlayerID ());
		assertEquals (attacker.getUnitURN (), data.getAttackerUnitURN ().intValue ());
		assertEquals (defender.getUnitURN (), data.getDefenderUnitURN ().intValue ());
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());

		// Check the dead unit was killed off, and exp given to the other side
		verify (midTurnSingle, times (1)).killUnitOnServerAndClients (defender, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		verify (midTurnSingle, times (0)).killUnitOnServerAndClients (attacker, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		
		verify (midTurnMulti, times (1)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		verify (midTurnMulti, times (0)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		
		// Defending player won
		verify (combatStartAndEnd, times (1)).combatEnded (eq (combatLocation), eq (attackingPlayer), eq (defendingPlayer), eq (defendingPlayer), any (CaptureCityDecisionID.class), eq (mom));
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
		unit1.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit1.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit1.setCombatSide (UnitCombatSideID.ATTACKER);
		unit1.setCombatHeading (1);
		units.add (unit1);
		
		// Wrong location
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		unit2.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		unit2.setCombatHeading (1);
		units.add (unit2);
		
		// Defender
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit3.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit3.setCombatSide (UnitCombatSideID.DEFENDER);
		unit3.setCombatHeading (1);
		units.add (unit3);
		
		// Dead
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setStatus (UnitStatusID.DEAD);
		unit4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit4.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit4.setCombatSide (UnitCombatSideID.ATTACKER);
		unit4.setCombatHeading (1);
		units.add (unit4);
		
		// Not in combat
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit5.setCombatSide (UnitCombatSideID.ATTACKER);
		unit5.setCombatHeading (1);
		units.add (unit5);
		
		// Land unit waiting in a transport in a naval combat
		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit6.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit6);

		// Another right one
		final MemoryUnit unit7 = new MemoryUnit ();
		unit7.setStatus (UnitStatusID.ALIVE);
		unit7.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit7.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit7.setCombatSide (UnitCombatSideID.ATTACKER);
		unit7.setCombatHeading (1);
		units.add (unit7);
		
		// Set up object to test
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		
		// Run test
		assertEquals (2, proc.countUnitsInCombat (new MapCoordinates3DEx (20, 10, 1), UnitCombatSideID.ATTACKER, units));
	}
}
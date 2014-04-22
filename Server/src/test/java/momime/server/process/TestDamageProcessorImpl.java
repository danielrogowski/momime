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

import momime.common.calculations.MomUnitCalculations;
import momime.common.database.newgame.v0_9_4.FogOfWarSettingData;
import momime.common.messages.servertoclient.v0_9_5.DamageCalculationMessage;
import momime.common.messages.servertoclient.v0_9_5.DamageCalculationMessageTypeID;
import momime.common.messages.v0_9_5.CaptureCityDecisionID;
import momime.common.messages.v0_9_5.FogOfWarMemory;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.MomSessionDescription;
import momime.common.messages.v0_9_5.UnitCombatSideID;
import momime.common.messages.v0_9_5.UnitStatusID;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.DamageCalculator;
import momime.server.database.ServerDatabaseEx;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.UntransmittedKillUnitActionID;
import momime.server.messages.v0_9_5.MomGeneralServerKnowledge;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.ndg.map.CoordinateSystem;
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
		final FogOfWarSettingData fogOfWarSettings = new FogOfWarSettingData ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Coordinate system
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
		
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.normalizeDirection (CoordinateSystemType.DIAMOND, 7+4)).thenReturn (7+4-8);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getCombatMapCoordinateSystem ()).thenReturn (combatMapCoordinateSystem);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Damage amounts
		final DamageCalculator calc = mock (DamageCalculator.class);
		when (calc.calculateDamage (eq (attacker), eq (defender), eq (attackingPlayer), eq (defendingPlayer),
			any (String.class), any (DamageCalculationMessage.class), eq (players), eq (trueMap.getMaintainedSpell ()),
			eq (trueMap.getCombatAreaEffect ()), eq (db))).thenReturn (5);		// Dmg to defender
		
		when (calc.calculateDamage (eq (defender), eq (attacker), eq (attackingPlayer), eq (defendingPlayer),
			any (String.class), any (DamageCalculationMessage.class), eq (players), eq (trueMap.getMaintainedSpell ()),
			eq (trueMap.getCombatAreaEffect ()), eq (db))).thenReturn (2);		// Dmg to attacker

		// Damage taken
		final MomUnitCalculations unitCalculations = mock (MomUnitCalculations.class);
		when (unitCalculations.calculateAliveFigureCount (attacker, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (3);
		when (unitCalculations.calculateAliveFigureCount (defender, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (0);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setCoordinateSystemUtils (coordinateSystemUtils);
		proc.setDamageCalculator (calc);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setUnitCalculations (unitCalculations);
		proc.setCombatStartAndEnd (combatStartAndEnd);
		
		// Need another surviving unit on each side, so the combat doesn't end
		for (final PlayerServerDetails thisPlayer : players)
		{
			final MemoryUnit survivingUnit = new MemoryUnit ();
			survivingUnit.setOwningPlayerID (thisPlayer.getPlayerDescription ().getPlayerID ());
			survivingUnit.setStatus (UnitStatusID.ALIVE);
			survivingUnit.setCombatPosition (new MapCoordinates2DEx (7, 9));
			survivingUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
			survivingUnit.setCombatSide ((thisPlayer == attackingPlayer) ? UnitCombatSideID.ATTACKER : UnitCombatSideID.DEFENDER);
			trueMap.getUnit ().add (survivingUnit);
		}
		
		// Run method
		proc.resolveAttack (attacker, defender, attackingPlayer, defendingPlayer, 7, false, combatLocation, mom);
		
		// Check initial message was sent
		final ArgumentCaptor<DamageCalculationMessage> msg = ArgumentCaptor.forClass (DamageCalculationMessage.class); 
		
		verify (calc, times (1)).sendDamageCalculationMessage (eq (attackingPlayer), eq (defendingPlayer), msg.capture ());
		assertEquals (attacker.getUnitURN (), msg.getValue ().getAttackerUnitURN ().intValue ());
		assertEquals (defender.getUnitURN (), msg.getValue ().getDefenderUnitURN ().intValue ());
		assertEquals (DamageCalculationMessageTypeID.MELEE_ATTACK, msg.getValue ().getMessageType ());
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());

		// Check dmg was applied
		assertEquals (2+2, attacker.getDamageTaken ());
		assertEquals (3+5, defender.getDamageTaken ());
		
		verify (midTurn, times (1)).sendCombatDamageToClients (attacker, defender, attackingPlayer, defendingPlayer, false, players,
			trueTerrain, combatLocation, db, fogOfWarSettings);
		
		// Check the dead unit was killed off, and exp given to the other side
		verify (midTurn, times (1)).killUnitOnServerAndClients (defender, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		verify (midTurn, times (0)).killUnitOnServerAndClients (attacker, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		
		verify (midTurn, times (1)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		verify (midTurn, times (0)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		
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
		final FogOfWarSettingData fogOfWarSettings = new FogOfWarSettingData ();
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = new MapVolumeOfMemoryGridCells ();
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Coordinate system
		final CoordinateSystem combatMapCoordinateSystem = ServerTestData.createCombatMapCoordinateSystem ();
		
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.normalizeDirection (CoordinateSystemType.DIAMOND, 7+4)).thenReturn (7+4-8);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getCombatMapCoordinateSystem ()).thenReturn (combatMapCoordinateSystem);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Damage amounts
		final DamageCalculator calc = mock (DamageCalculator.class);
		when (calc.calculateDamage (eq (attacker), eq (defender), eq (attackingPlayer), eq (defendingPlayer),
			any (String.class), any (DamageCalculationMessage.class), eq (players), eq (trueMap.getMaintainedSpell ()),
			eq (trueMap.getCombatAreaEffect ()), eq (db))).thenReturn (5);		// Dmg to defender
			
		when (calc.calculateDamage (eq (defender), eq (attacker), eq (attackingPlayer), eq (defendingPlayer),
			any (String.class), any (DamageCalculationMessage.class), eq (players), eq (trueMap.getMaintainedSpell ()),
			eq (trueMap.getCombatAreaEffect ()), eq (db))).thenReturn (2);		// Dmg to attacker

		// Damage taken
		final MomUnitCalculations unitCalculations = mock (MomUnitCalculations.class);
		when (unitCalculations.calculateAliveFigureCount (attacker, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (3);
		when (unitCalculations.calculateAliveFigureCount (defender, players, trueMap.getMaintainedSpell (), trueMap.getCombatAreaEffect (), db)).thenReturn (0);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setCoordinateSystemUtils (coordinateSystemUtils);
		proc.setDamageCalculator (calc);
		proc.setFogOfWarMidTurnChanges (midTurn);
		proc.setUnitCalculations (unitCalculations);
		proc.setCombatStartAndEnd (combatStartAndEnd);
		
		// The 'attacker' unit is still left alive because it still took no dmg, so put a unit in the list so the combat doesn't end for them (attacker is owned by defendingPlayer)
		final MemoryUnit survivingUnit = new MemoryUnit ();
		survivingUnit.setOwningPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
		survivingUnit.setStatus (UnitStatusID.ALIVE);
		survivingUnit.setCombatPosition (new MapCoordinates2DEx (7, 9));
		survivingUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		survivingUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		trueMap.getUnit ().add (survivingUnit);
		
		// Run method
		proc.resolveAttack (attacker, defender, attackingPlayer, defendingPlayer, 7, true, combatLocation, mom);
		
		// Check initial message was sent
		final ArgumentCaptor<DamageCalculationMessage> msg = ArgumentCaptor.forClass (DamageCalculationMessage.class); 
		
		verify (calc, times (1)).sendDamageCalculationMessage (eq (attackingPlayer), eq (defendingPlayer), msg.capture ());
		assertEquals (attacker.getUnitURN (), msg.getValue ().getAttackerUnitURN ().intValue ());
		assertEquals (defender.getUnitURN (), msg.getValue ().getDefenderUnitURN ().intValue ());
		assertEquals (DamageCalculationMessageTypeID.RANGED_ATTACK, msg.getValue ().getMessageType ());
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());

		// Check dmg was applied
		assertEquals (2, attacker.getDamageTaken ());
		assertEquals (3+5, defender.getDamageTaken ());
		
		verify (midTurn, times (1)).sendCombatDamageToClients (attacker, defender, attackingPlayer, defendingPlayer, true, players,
			trueTerrain, combatLocation, db, fogOfWarSettings);
		
		// Check the dead unit was killed off, and exp given to the other side
		verify (midTurn, times (1)).killUnitOnServerAndClients (defender, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		verify (midTurn, times (0)).killUnitOnServerAndClients (attacker, null, UntransmittedKillUnitActionID.COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		
		verify (midTurn, times (1)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		verify (midTurn, times (0)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, trueTerrain, trueMap.getUnit (), players, db, fogOfWarSettings);
		
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
		units.add (unit1);
		
		// Wrong location
		final MemoryUnit unit2 = new MemoryUnit ();
		unit2.setStatus (UnitStatusID.ALIVE);
		unit2.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		unit2.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit2.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit2);
		
		// Defender
		final MemoryUnit unit3 = new MemoryUnit ();
		unit3.setStatus (UnitStatusID.ALIVE);
		unit3.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit3.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit3.setCombatSide (UnitCombatSideID.DEFENDER);
		units.add (unit3);
		
		// Dead
		final MemoryUnit unit4 = new MemoryUnit ();
		unit4.setStatus (UnitStatusID.DEAD);
		unit4.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit4.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit4.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit4);
		
		// Not in combat
		final MemoryUnit unit5 = new MemoryUnit ();
		unit5.setStatus (UnitStatusID.ALIVE);
		unit5.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit5.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit5);
		
		// Another right one
		final MemoryUnit unit6 = new MemoryUnit ();
		unit6.setStatus (UnitStatusID.ALIVE);
		unit6.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		unit6.setCombatPosition (new MapCoordinates2DEx (7, 9));
		unit6.setCombatSide (UnitCombatSideID.ATTACKER);
		units.add (unit6);
		
		// Set up object to test
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		
		// Run test
		assertEquals (2, proc.countUnitsInCombat (new MapCoordinates3DEx (20, 10, 1), UnitCombatSideID.ATTACKER, units));
	}
}

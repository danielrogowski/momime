package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystemType;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.AttackResolution;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.FogOfWarSetting;
import momime.common.database.OverlandMapSize;
import momime.common.database.Spell;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.SpellCastType;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.UnitServerUtils;
import momime.server.worldupdates.WorldUpdates;

/**
 * Tests the DamageProcessorImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestDamageProcessorImpl extends ServerTestData
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
		final CommonDatabase db = mock (CommonDatabase.class);
		
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
		final UnitDamage attackerDamageTaken = new UnitDamage ();
		attackerDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		attackerDamageTaken.setDamageTaken (2);
		
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		attacker.getUnitDamage ().add (attackerDamageTaken);

		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		defender.getUnitDamage ().add (defenderDamageTaken);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		final CombatMapSize combatMapSize = createCombatMapSize ();
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();

		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		sd.setCombatMapSize (combatMapSize);
		sd.setOverlandMapSize (overlandMapSize);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		final ServerGridCellEx tc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setAttackerSpecialFameLost (0);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Coordinate system
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.normalizeDirection (CoordinateSystemType.DIAMOND, 7+4)).thenReturn (7+4-8);

		// Expanded unit detalis
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (attacker), anyList (), isNull (), isNull (),
			eq (players), eq (trueMap), eq (db))).thenReturn (xuAttacker);
		when (expand.expandUnitDetails (attacker, null, null, null, players, trueMap, db)).thenReturn (xuAttacker);

		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (defender, null, null, null, players, trueMap, db)).thenReturn (xuDefender);
		
		// Attack resolution
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);

		final AttackResolution attackResolution = new AttackResolution ();
		when (attackResolutionProc.chooseAttackResolution (xuAttacker, xuDefender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, db)).thenReturn (attackResolution);
		
		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		
		final List<List<AttackResolutionStepContainer>> stepNumbers = new ArrayList<List<AttackResolutionStepContainer>> ();
		stepNumbers.add (steps);
		when (attackResolutionProc.splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionStep ())).thenReturn (stepNumbers);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Damage taken
		when (xuAttacker.calculateAliveFigureCount ()).thenReturn (3);
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (1, 0);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		final DamageCalculator calc = mock (DamageCalculator.class);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setCoordinateSystemUtils (coordinateSystemUtils);
		proc.setDamageCalculator (calc);
		proc.setFogOfWarMidTurnChanges (midTurnSingle);
		proc.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		proc.setCombatStartAndEnd (combatStartAndEnd);
		proc.setAttackResolutionProcessing (attackResolutionProc);
		proc.setUnitServerUtils (unitServerUtils);
		proc.setExpandUnitDetails (expand);
		
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
		final List<ResolveAttackTarget> defenders = new ArrayList<ResolveAttackTarget> ();
		defenders.add (new ResolveAttackTarget (defender));
		
		final List<MemoryUnit> defenderUnits = new ArrayList<MemoryUnit> ();
		defenderUnits.add (defender);
		
		final ResolveAttackResult result = proc.resolveAttack (attacker, defenders, attackingPlayer, defendingPlayer, null, null, null, 7,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, null, null, combatLocation, false, mom);
		
		// Check results
		assertFalse (result.isCombatEnded ());

		// Ensure steps were processed
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);

		verify (attackResolutionProc).processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, mom);

		verify (midTurnSingle).sendDamageToClients (attacker, attackingPlayer, defendingPlayer, defenders,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null,
			null, null, false, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc).sendDamageHeader (attacker, defenderUnits, false, attackingPlayer, defendingPlayer, null,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, null);
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());
		
		// Check the dead unit was killed off, and exp given to the other side
		verify (wu).killUnit (defender.getUnitURN (), KillUnitActionID.HEALABLE_COMBAT_DAMAGE);
		verify (wu).process (mom);
		
		verify (midTurnMulti).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueMap, players, db, fogOfWarSettings);
		
		verifyNoMoreInteractions (attackResolutionProc);
		verifyNoMoreInteractions (wu);
		verifyNoMoreInteractions (calc);
		verifyNoMoreInteractions (midTurnSingle);
		verifyNoMoreInteractions (midTurnMulti);
		verifyNoMoreInteractions (combatStartAndEnd);
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
		final CommonDatabase db = mock (CommonDatabase.class);
		
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
		final UnitDamage attackerDamageTaken = new UnitDamage ();
		attackerDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		attackerDamageTaken.setDamageTaken (2);
		
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		attacker.getUnitDamage ().add (attackerDamageTaken);

		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		defender.getUnitDamage ().add (defenderDamageTaken);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		final CombatMapSize combatMapSize = createCombatMapSize (); 
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();
		
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		sd.setCombatMapSize (combatMapSize);
		sd.setOverlandMapSize (overlandMapSize);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		final ServerGridCellEx tc = (ServerGridCellEx) trueTerrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20);
		tc.setAttackerSpecialFameLost (0);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Coordinate system
		final CoordinateSystemUtils coordinateSystemUtils = mock (CoordinateSystemUtils.class);
		when (coordinateSystemUtils.normalizeDirection (CoordinateSystemType.DIAMOND, 7+4)).thenReturn (7+4-8);

		// Expanded unit detalis
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);

		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (attacker), anyList (), isNull (), isNull (),
			eq (players), eq (trueMap), eq (db))).thenReturn (xuAttacker);
		when (expand.expandUnitDetails (attacker, null, null, null,
			players, trueMap, db)).thenReturn (xuAttacker);

		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (defender, null, null, null, players, trueMap, db)).thenReturn (xuDefender);
		
		// Attack resolution
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);

		final AttackResolution attackResolution = new AttackResolution ();
		when (attackResolutionProc.chooseAttackResolution (xuAttacker, xuDefender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, db)).thenReturn (attackResolution);
		
		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		
		final List<List<AttackResolutionStepContainer>> stepNumbers = new ArrayList<List<AttackResolutionStepContainer>> ();
		stepNumbers.add (steps);
		when (attackResolutionProc.splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionStep ())).thenReturn (stepNumbers);
		
		// Session variables
		final WorldUpdates wu = mock (WorldUpdates.class);

		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getWorldUpdates ()).thenReturn (wu);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Damage taken
		when (xuAttacker.calculateAliveFigureCount ()).thenReturn (3);
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (1, 0);
		
		// Set up object to test
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);
		final FogOfWarMidTurnMultiChanges midTurnMulti = mock (FogOfWarMidTurnMultiChanges.class);
		final CombatStartAndEnd combatStartAndEnd = mock (CombatStartAndEnd.class);
		final DamageCalculator calc = mock (DamageCalculator.class);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		
		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setCoordinateSystemUtils (coordinateSystemUtils);
		proc.setDamageCalculator (calc);
		proc.setFogOfWarMidTurnChanges (midTurnSingle);
		proc.setFogOfWarMidTurnMultiChanges (midTurnMulti);
		proc.setCombatStartAndEnd (combatStartAndEnd);
		proc.setAttackResolutionProcessing (attackResolutionProc);
		proc.setUnitServerUtils (unitServerUtils);
		proc.setExpandUnitDetails (expand);
		
		// The 'attacker' unit is still left alive because it still took no dmg, so put a unit in the list so the combat doesn't end for them (attacker is owned by defendingPlayer)
		final MemoryUnit survivingUnit = new MemoryUnit ();
		survivingUnit.setOwningPlayerID (defendingPlayer.getPlayerDescription ().getPlayerID ());
		survivingUnit.setStatus (UnitStatusID.ALIVE);
		survivingUnit.setCombatPosition (new MapCoordinates2DEx (7, 9));
		survivingUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		survivingUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		trueMap.getUnit ().add (survivingUnit);
		
		// Run method
		final List<ResolveAttackTarget> defenders = new ArrayList<ResolveAttackTarget> ();
		defenders.add (new ResolveAttackTarget (defender));
		
		final List<MemoryUnit> defenderUnits = new ArrayList<MemoryUnit> ();
		defenderUnits.add (defender);
		
		final ResolveAttackResult result = proc.resolveAttack (attacker, defenders, attackingPlayer, defendingPlayer, null, null, null, 7,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, null, null, combatLocation, false, mom);
		
		// Check results
		assertTrue (result.isCombatEnded ());
		
		// Ensure steps were processed
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);

		verify (attackResolutionProc).processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, mom);

		verify (midTurnSingle).sendDamageToClients (attacker, attackingPlayer, defendingPlayer, defenders,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null,
			null, null, false, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc).sendDamageHeader (attacker, defenderUnits, false, attackingPlayer, defendingPlayer, null,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, null);
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());

		// Check the dead unit was killed off, and exp given to the other side
		verify (wu).killUnit (defender.getUnitURN (), KillUnitActionID.HEALABLE_COMBAT_DAMAGE);
		verify (wu).process (mom);
		
		verify (midTurnMulti).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueMap, players, db, fogOfWarSettings);
		
		// Defending player won
		verify (combatStartAndEnd).combatEnded (eq (combatLocation), eq (attackingPlayer), eq (defendingPlayer), eq (defendingPlayer), isNull (), eq (mom));
		
		verifyNoMoreInteractions (attackResolutionProc);
		verifyNoMoreInteractions (wu);
		verifyNoMoreInteractions (calc);
		verifyNoMoreInteractions (midTurnSingle);
		verifyNoMoreInteractions (midTurnMulti);
		verifyNoMoreInteractions (combatStartAndEnd);
	}

	/**
	 * Tests the resolveAttack method on a spell that attacks a single unit, which survives
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResolveAttack_Spell_Survives () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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

		final PlayerServerDetails castingPlayer = defendingPlayer;
		
		// Units
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		defender.getUnitDamage ().add (defenderDamageTaken);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		final CombatMapSize combatMapSize = createCombatMapSize (); 
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();

		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		sd.setCombatMapSize (combatMapSize);
		sd.setOverlandMapSize (overlandMapSize);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);

		// Expanded unit detalis
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (defender, null, null, null, players, trueMap, db)).thenReturn (xuDefender);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Spell we're attacking with
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		spell.setAttackSpellCombatTarget (AttackSpellTargetID.SINGLE_UNIT);
		
		// Damage from spell
		final DamageCalculator calc = mock (DamageCalculator.class);
		final DamageType damageType = new DamageType (); 
		final AttackDamage spellDamage = new AttackDamage (6, 0, damageType, null, spell, null, null, 1);
		when (calc.attackFromSpell (spell, null, castingPlayer, null, attackingPlayer, defendingPlayer, null, db, SpellCastType.COMBAT, false)).thenReturn (spellDamage);

		// Damage taken
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (1);
		
		// Set up object to test
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);

		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setDamageCalculator (calc);
		proc.setAttackResolutionProcessing (attackResolutionProc);
		proc.setFogOfWarMidTurnChanges (midTurnSingle);
		proc.setExpandUnitDetails (expand);
		
		// Run method
		final List<ResolveAttackTarget> defenders = new ArrayList<ResolveAttackTarget> ();
		defenders.add (new ResolveAttackTarget (defender));
		
		final List<MemoryUnit> defenderUnits = new ArrayList<MemoryUnit> ();
		defenderUnits.add (defender);
		
		final ResolveAttackResult result = proc.resolveAttack (null, defenders, attackingPlayer, defendingPlayer, null, null, null, null,
			null, spell, null, castingPlayer, combatLocation, false, mom);
		
		// Check results
		assertFalse (result.isCombatEnded ());

		// Ensure steps were processed
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);

		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		steps.add (new AttackResolutionStepContainer (spellDamage));
		
		verify (attackResolutionProc).processAttackResolutionStep (null, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation, steps, mom);

		verify (midTurnSingle).sendDamageToClients (null, attackingPlayer, defendingPlayer, defenders,
			null, "SP001", null, null, false, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc).sendDamageHeader (null, defenderUnits, false, attackingPlayer, defendingPlayer, null, null, spell, castingPlayer);
		
		verifyNoMoreInteractions (attackResolutionProc);
		verifyNoMoreInteractions (calc);
		verifyNoMoreInteractions (midTurnSingle);
	}
	
	/**
	 * Tests the resolveAttack method on a spell that attacks all units; its an illusionary attack and 1 of the 3 units is immune to illusions
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testResolveAttack_MultiUnitSpell_Survives () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

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

		final PlayerServerDetails castingPlayer = defendingPlayer;
		
		// Units
		final UnitDamage defender1DamageTaken = new UnitDamage ();
		defender1DamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defender1DamageTaken.setDamageTaken (3);
		
		final MemoryUnit defender1 = new MemoryUnit ();
		defender1.setUnitURN (102);
		defender1.setOwningPlayerID (attackingPd.getPlayerID ());
		defender1.getUnitDamage ().add (defender1DamageTaken);
		
		final UnitDamage defender2DamageTaken = new UnitDamage ();
		defender2DamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defender2DamageTaken.setDamageTaken (3);
		
		final MemoryUnit defender2 = new MemoryUnit ();
		defender2.setUnitURN (103);
		defender2.setOwningPlayerID (attackingPd.getPlayerID ());
		defender2.getUnitDamage ().add (defender2DamageTaken);
		
		final UnitDamage defender3DamageTaken = new UnitDamage ();
		defender3DamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defender3DamageTaken.setDamageTaken (3);
		
		final MemoryUnit defender3 = new MemoryUnit ();
		defender3.setUnitURN (104);
		defender3.setOwningPlayerID (attackingPd.getPlayerID ());
		defender3.getUnitDamage ().add (defender3DamageTaken);
		
		// Session description
		final FogOfWarSetting fogOfWarSettings = new FogOfWarSetting ();
		final CombatMapSize combatMapSize = createCombatMapSize (); 
		final OverlandMapSize overlandMapSize = createOverlandMapSize ();

		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setFogOfWarSetting (fogOfWarSettings);
		sd.setCombatMapSize (combatMapSize);
		sd.setOverlandMapSize (overlandMapSize);
		
		// General server knowledge
		final MapVolumeOfMemoryGridCells trueTerrain = createOverlandMap (overlandMapSize);
		
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		trueMap.setMap (trueTerrain);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);

		// Expanded unit detalis
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final ExpandedUnitDetails xuDefender1 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (defender1, null, null, null, players, trueMap, db)).thenReturn (xuDefender1);
		
		final ExpandedUnitDetails xuDefender2 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (defender2, null, null, null, players, trueMap, db)).thenReturn (xuDefender2);
		
		final ExpandedUnitDetails xuDefender3 = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (defender3, null, null, null, players, trueMap, db)).thenReturn (xuDefender3);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Spell we're attacking with
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.ILLUSIONARY);
		spell.setAttackSpellCombatTarget (AttackSpellTargetID.ALL_UNITS);
		
		// Damage from spell
		final DamageCalculator calc = mock (DamageCalculator.class);
		final DamageType damageType = new DamageType ();
		final AttackDamage spellDamage = new AttackDamage (6, 0, damageType, null, spell, null, null, 1);
		when (calc.attackFromSpell (spell, null, castingPlayer, null, attackingPlayer, defendingPlayer, null, db, SpellCastType.COMBAT, false)).thenReturn (spellDamage);

		// Damage taken
		when (xuDefender1.calculateAliveFigureCount ()).thenReturn (1);
		when (xuDefender2.calculateAliveFigureCount ()).thenReturn (2);
		when (xuDefender3.calculateAliveFigureCount ()).thenReturn (3);
		
		// Set up object to test
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);

		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setDamageCalculator (calc);
		proc.setAttackResolutionProcessing (attackResolutionProc);
		proc.setFogOfWarMidTurnChanges (midTurnSingle);
		proc.setExpandUnitDetails (expand);
		
		// Run method
		final List<ResolveAttackTarget> defenders = new ArrayList<ResolveAttackTarget> ();
		defenders.add (new ResolveAttackTarget (defender1));
		defenders.add (new ResolveAttackTarget (defender2));
		defenders.add (new ResolveAttackTarget (defender3));
		
		final List<MemoryUnit> defenderUnits = new ArrayList<MemoryUnit> ();
		defenderUnits.add (defender1);
		defenderUnits.add (defender2);
		defenderUnits.add (defender3);
		
		final ResolveAttackResult result = proc.resolveAttack (null, defenders, attackingPlayer, defendingPlayer, null, null, null, null,
			null, spell, null, castingPlayer, combatLocation, false, mom);
		
		// Check results
		assertFalse (result.isCombatEnded ());

		// Ensure steps were processed
		final AttackResolutionUnit defender1Wrapper = new AttackResolutionUnit (defender1);
		final AttackResolutionUnit defender2Wrapper = new AttackResolutionUnit (defender2);
		final AttackResolutionUnit defender3Wrapper = new AttackResolutionUnit (defender3);

		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		steps.add (new AttackResolutionStepContainer (spellDamage));

		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		verify (attackResolutionProc).processAttackResolutionStep (null, defender1Wrapper, attackingPlayer, defendingPlayer, combatLocation, steps, mom);
		verify (attackResolutionProc).processAttackResolutionStep (null, defender2Wrapper, attackingPlayer, defendingPlayer, combatLocation, steps, mom);
		verify (attackResolutionProc).processAttackResolutionStep (null, defender3Wrapper, attackingPlayer, defendingPlayer, combatLocation, steps, mom);

		verify (midTurnSingle).sendDamageToClients (null, attackingPlayer, defendingPlayer, defenders,
			null, "SP001", null, null, false, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc).sendDamageHeader (null, defenderUnits, false, attackingPlayer, defendingPlayer, null, null, spell, castingPlayer);
		
		verifyNoMoreInteractions (attackResolutionProc);
		verifyNoMoreInteractions (calc);
		verifyNoMoreInteractions (midTurnSingle);
	}
	
	/**
	 * Tests the countUnitsInCombat method
	 */
	@Test
	public final void testCountUnitsInCombat ()
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
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
		assertEquals (2, proc.countUnitsInCombat (new MapCoordinates3DEx (20, 10, 1), UnitCombatSideID.ATTACKER, units, db));
	}
}
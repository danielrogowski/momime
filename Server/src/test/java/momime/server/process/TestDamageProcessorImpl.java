package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.CoordinateSystemType;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.database.AttackResolution;
import momime.common.database.AttackResolutionStep;
import momime.common.database.AttackSpellCombatTargetID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.FogOfWarSetting;
import momime.common.database.NegatedBySkill;
import momime.common.database.NegatedByUnitID;
import momime.common.database.OverlandMapSize;
import momime.common.database.Spell;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.UnitServerUtils;

/**
 * Tests the DamageProcessorImpl class
 */
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
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (attacker), anyList (), eq (null), eq (null),
			eq (players), eq (trueMap), eq (db))).thenReturn (xuAttacker);
		when (unitUtils.expandUnitDetails (attacker, null, null, null, players, trueMap, db)).thenReturn (xuAttacker);

		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (defender, null, null, null, players, trueMap, db)).thenReturn (xuDefender);
		
		// Attack resolution
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);

		final AttackResolution attackResolution = new AttackResolution ();
		when (attackResolutionProc.chooseAttackResolution (xuAttacker, xuDefender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, db)).thenReturn (attackResolution);
		
		final List<AttackResolutionStep> steps = new ArrayList<AttackResolutionStep> ();
		
		final List<List<AttackResolutionStep>> stepNumbers = new ArrayList<List<AttackResolutionStep>> ();
		stepNumbers.add (steps);
		when (attackResolutionProc.splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionStep ())).thenReturn (stepNumbers);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
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
		proc.setUnitUtils (unitUtils);
		
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
		
		assertFalse (proc.resolveAttack (attacker, defenders, attackingPlayer, defendingPlayer, null, null, 7,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, null, null, combatLocation, mom));

		// Ensure steps were processed
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);

		verify (attackResolutionProc, times (1)).processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, null, players, trueMap, combatMapSize, db);

		final List<DamageResolutionTypeID> specialDamageResolutionsApplied = new ArrayList<DamageResolutionTypeID> ();
		verify (midTurnSingle, times (1)).sendCombatDamageToClients (attacker, attackingPlayer, defendingPlayer, defenders,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null,
			specialDamageResolutionsApplied, null, null, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc, times (1)).sendDamageHeader (attacker, defenders, attackingPlayer, defendingPlayer,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, null);
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());
		
		// Check the dead unit was killed off, and exp given to the other side
		verify (midTurnSingle, times (1)).killUnitOnServerAndClients (defender, KillUnitActionID.HEALABLE_COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		verify (midTurnSingle, times (0)).killUnitOnServerAndClients (attacker, KillUnitActionID.HEALABLE_COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		
		verify (midTurnMulti, times (1)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueMap, players, db, fogOfWarSettings);
		verify (midTurnMulti, times (0)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, trueMap, players, db, fogOfWarSettings);
		
		verify (combatStartAndEnd, times (0)).combatEnded (eq (combatLocation), eq (attackingPlayer), eq (defendingPlayer), any (PlayerServerDetails.class), eq (null), eq (mom));
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
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (attacker), anyList (), eq (null), eq (null),
			eq (players), eq (trueMap), eq (db))).thenReturn (xuAttacker);
		when (unitUtils.expandUnitDetails (attacker, null, null, null,
			players, trueMap, db)).thenReturn (xuAttacker);

		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (defender, null, null, null, players, trueMap, db)).thenReturn (xuDefender);
		
		// Attack resolution
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);

		final AttackResolution attackResolution = new AttackResolution ();
		when (attackResolutionProc.chooseAttackResolution (xuAttacker, xuDefender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, db)).thenReturn (attackResolution);
		
		final List<AttackResolutionStep> steps = new ArrayList<AttackResolutionStep> ();
		
		final List<List<AttackResolutionStep>> stepNumbers = new ArrayList<List<AttackResolutionStep>> ();
		stepNumbers.add (steps);
		when (attackResolutionProc.splitAttackResolutionStepsByStepNumber (attackResolution.getAttackResolutionStep ())).thenReturn (stepNumbers);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getPlayers ()).thenReturn (players);
		
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
		proc.setUnitUtils (unitUtils);
		
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
		
		assertTrue (proc.resolveAttack (attacker, defenders, attackingPlayer, defendingPlayer, null, null, 7,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, null, null, combatLocation, mom));
		
		// Ensure steps were processed
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);

		verify (attackResolutionProc, times (1)).processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, null, players, trueMap, combatMapSize, db);

		final List<DamageResolutionTypeID> specialDamageResolutionsApplied = new ArrayList<DamageResolutionTypeID> ();
		verify (midTurnSingle, times (1)).sendCombatDamageToClients (attacker, attackingPlayer, defendingPlayer, defenders,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null,
			specialDamageResolutionsApplied, null, null, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc, times (1)).sendDamageHeader (attacker, defenders, attackingPlayer, defendingPlayer,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, null);
		
		// Check units facing each other
		assertEquals (7, attacker.getCombatHeading ().intValue ());
		assertEquals (3, defender.getCombatHeading ().intValue ());

		// Check the dead unit was killed off, and exp given to the other side
		verify (midTurnSingle, times (1)).killUnitOnServerAndClients (defender, KillUnitActionID.HEALABLE_COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		verify (midTurnSingle, times (0)).killUnitOnServerAndClients (attacker, KillUnitActionID.HEALABLE_COMBAT_DAMAGE, trueMap, players, fogOfWarSettings, db);
		
		verify (midTurnMulti, times (1)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.DEFENDER, trueMap, players, db, fogOfWarSettings);
		verify (midTurnMulti, times (0)).grantExperienceToUnitsInCombat (combatLocation, UnitCombatSideID.ATTACKER, trueMap, players, db, fogOfWarSettings);
		
		// Defending player won
		verify (combatStartAndEnd, times (1)).combatEnded (eq (combatLocation), eq (attackingPlayer), eq (defendingPlayer), eq (defendingPlayer), eq (null), eq (mom));
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
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (defender, null, null, null, players, trueMap, db)).thenReturn (xuDefender);
		
		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Spell we're attacking with
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		spell.setAttackSpellCombatTarget (AttackSpellCombatTargetID.SINGLE_UNIT);
		
		// Damage from spell
		final DamageCalculator calc = mock (DamageCalculator.class);
		final DamageType damageType = new DamageType (); 
		final AttackDamage spellDamage = new AttackDamage (6, 0, damageType, null, spell, null, null, 1);
		when (calc.attackFromSpell (spell, null, castingPlayer, null, attackingPlayer, defendingPlayer, db)).thenReturn (spellDamage);

		// Damage taken
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (1);
		
		// Set up object to test
		final AttackResolutionProcessing attackResolutionProc = mock (AttackResolutionProcessing.class);
		final FogOfWarMidTurnChanges midTurnSingle = mock (FogOfWarMidTurnChanges.class);

		final DamageProcessorImpl proc = new DamageProcessorImpl ();
		proc.setDamageCalculator (calc);
		proc.setAttackResolutionProcessing (attackResolutionProc);
		proc.setFogOfWarMidTurnChanges (midTurnSingle);
		proc.setUnitUtils (unitUtils);
		
		// Run method
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);
		
		assertFalse (proc.resolveAttack (null, defenders, attackingPlayer, defendingPlayer, null, null, null,
			null, spell, null, castingPlayer, combatLocation, mom));

		// Ensure steps were processed
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);

		final List<AttackResolutionStep> steps = new ArrayList<AttackResolutionStep> ();
		steps.add (null);
		
		verify (attackResolutionProc, times (1)).processAttackResolutionStep (null, defenderWrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, spellDamage, players, trueMap, combatMapSize, db);

		final List<DamageResolutionTypeID> specialDamageResolutionsApplied = new ArrayList<DamageResolutionTypeID> ();
		verify (midTurnSingle, times (1)).sendCombatDamageToClients (null, attackingPlayer, defendingPlayer, defenders,
			null, "SP001", specialDamageResolutionsApplied, null, null, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc, times (1)).sendDamageHeader (null, defenders, attackingPlayer, defendingPlayer, null, spell, castingPlayer);
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
		
		final NegatedBySkill immunityToIllusions = new NegatedBySkill ();
		immunityToIllusions.setNegatedByUnitID (NegatedByUnitID.ENEMY_UNIT);
		immunityToIllusions.setNegatedBySkillID ("US001");
		
		final UnitSkillEx illusionaryAttackSkill = new UnitSkillEx ();
		illusionaryAttackSkill.getNegatedBySkill ().add (immunityToIllusions);
		when (db.findUnitSkill (ServerDatabaseValues.UNIT_SKILL_ID_ILLUSIONARY_ATTACK, "resolveAttack")).thenReturn (illusionaryAttackSkill);

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
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuDefender1 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (defender1, null, null, null, players, trueMap, db)).thenReturn (xuDefender1);
		
		final ExpandedUnitDetails xuDefender2 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (defender2, null, null, null, players, trueMap, db)).thenReturn (xuDefender2);
		
		final ExpandedUnitDetails xuDefender3 = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (defender3, null, null, null, players, trueMap, db)).thenReturn (xuDefender3);
		
		// Middle unit is immune to illusions
		when (xuDefender1.hasModifiedSkill ("US001")).thenReturn (false);
		when (xuDefender2.hasModifiedSkill ("US001")).thenReturn (true);
		when (xuDefender3.hasModifiedSkill ("US001")).thenReturn (false);
		
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
		spell.setAttackSpellCombatTarget (AttackSpellCombatTargetID.ALL_UNITS);
		
		// Damage from spell
		final DamageCalculator calc = mock (DamageCalculator.class);
		final DamageType damageType = new DamageType ();
		final AttackDamage spellDamage = new AttackDamage (6, 0, damageType, null, spell, null, null, 1);
		when (calc.attackFromSpell (spell, null, castingPlayer, null, attackingPlayer, defendingPlayer, db)).thenReturn (spellDamage);

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
		proc.setUnitUtils (unitUtils);
		
		// Run method
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender1);
		defenders.add (defender2);
		defenders.add (defender3);
		
		assertFalse (proc.resolveAttack (null, defenders, attackingPlayer, defendingPlayer, null, null, null,
			null, spell, null, castingPlayer, combatLocation, mom));

		// Ensure steps were processed
		final AttackResolutionUnit defender1Wrapper = new AttackResolutionUnit (defender1);
		final AttackResolutionUnit defender2Wrapper = new AttackResolutionUnit (defender2);
		final AttackResolutionUnit defender3Wrapper = new AttackResolutionUnit (defender3);

		final List<AttackResolutionStep> steps = new ArrayList<AttackResolutionStep> ();
		steps.add (null);

		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		final AttackDamage reducedDamage = new AttackDamage (6, 0, damageType, null, spell, null, null, 1);
		
		verify (attackResolutionProc, times (1)).processAttackResolutionStep (null, defender1Wrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, spellDamage, players, trueMap, combatMapSize, db);
		
		verify (attackResolutionProc, times (1)).processAttackResolutionStep (null, defender2Wrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, reducedDamage, players, trueMap, combatMapSize, db);
		
		verify (attackResolutionProc, times (1)).processAttackResolutionStep (null, defender3Wrapper, attackingPlayer, defendingPlayer, combatLocation,
			steps, spellDamage, players, trueMap, combatMapSize, db);

		final List<DamageResolutionTypeID> specialDamageResolutionsApplied = new ArrayList<DamageResolutionTypeID> ();
		verify (midTurnSingle, times (1)).sendCombatDamageToClients (null, attackingPlayer, defendingPlayer, defenders,
			null, "SP001", specialDamageResolutionsApplied, null, null, players, trueTerrain, db, fogOfWarSettings);
		
		// Check initial message was sent
		verify (calc, times (1)).sendDamageHeader (null, defenders, attackingPlayer, defendingPlayer, null, spell, castingPlayer);
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
package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AttackResolution;
import momime.common.database.AttackResolutionCondition;
import momime.common.database.AttackResolutionStep;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillEx;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.UnitDamage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ServerTestData;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.knowledge.CombatDetails;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.CombatMapServerUtils;
import momime.server.utils.UnitServerUtilsImpl;

/**
 * Tests the AttackResolutionProcessingImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestAttackResolutionProcessingImpl extends ServerTestData
{
	/**
	 * Tests the chooseAttackResolution method when an appropraite attack resolution does exist 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseAttackResolution_Exists () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.hasModifiedSkill ("US001")).thenReturn (false);

		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.hasModifiedSkill ("US002")).thenReturn (true);
		
		// Attack resolutions to choose between - first one that doesn't match (see mocked skill values above, attacker returns -1 for this)
		final AttackResolutionCondition condition1 = new AttackResolutionCondition ();
		condition1.setCombatSide (UnitCombatSideID.ATTACKER);
		condition1.setUnitSkillID ("US001");
		
		final AttackResolution res1 = new AttackResolution ();
		res1.getAttackResolutionCondition ().add (condition1);

		// Now one that does match
		final AttackResolutionCondition condition2 = new AttackResolutionCondition ();
		condition2.setCombatSide (UnitCombatSideID.DEFENDER);
		condition2.setUnitSkillID ("US002");
		
		final AttackResolution res2 = new AttackResolution ();
		res2.getAttackResolutionCondition ().add (condition2);
		
		final UnitSkillEx unitAttr = new UnitSkillEx ();
		unitAttr.getAttackResolution ().add (res1);
		unitAttr.getAttackResolution ().add (res2);
		when (db.findUnitSkill ("UA01", "chooseAttackResolution")).thenReturn (unitAttr);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();

		// Run method
		final AttackResolution chosen = proc.chooseAttackResolution (attacker, defender, "UA01", db);
		
		// Check results
		assertSame (res2, chosen);
	}

	/**
	 * Tests the chooseAttackResolution method when no appropraite attack resolution exists 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseAttackResolution_NotExists () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Units
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		when (attacker.hasModifiedSkill ("US001")).thenReturn (false);

		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		when (defender.hasModifiedSkill ("US002")).thenReturn (false);	// <---
		
		// Attack resolutions to choose between - first one that doesn't match (see mocked skill values above, attacker returns -1 for this)
		final AttackResolutionCondition condition1 = new AttackResolutionCondition ();
		condition1.setCombatSide (UnitCombatSideID.ATTACKER);
		condition1.setUnitSkillID ("US001");
		
		final AttackResolution res1 = new AttackResolution ();
		res1.getAttackResolutionCondition ().add (condition1);

		// Now another one that doesn't match
		final AttackResolutionCondition condition2 = new AttackResolutionCondition ();
		condition2.setCombatSide (UnitCombatSideID.DEFENDER);
		condition2.setUnitSkillID ("US002");
		
		final AttackResolution res2 = new AttackResolution ();
		res2.getAttackResolutionCondition ().add (condition2);
		
		final UnitSkillEx unitAttr = new UnitSkillEx ();
		unitAttr.getAttackResolution ().add (res1);
		unitAttr.getAttackResolution ().add (res2);
		when (db.findUnitSkill ("UA01", "chooseAttackResolution")).thenReturn (unitAttr);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();

		// Run method
		assertThrows (MomException.class, () ->
		{
			proc.chooseAttackResolution (attacker, defender, "UA01", db);
		});
	}
	
	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when sorting a valid list
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test
	public final void testSplitAttackResolutionStepsByStepNumber_Valid () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStep> src = new ArrayList<AttackResolutionStep> ();
		for (final int stepNumber : new int [] {1, 1, 2, 3, 3, 3, 4})
		{
			final AttackResolutionStep step = new AttackResolutionStep ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		final List<List<AttackResolutionStepContainer>> dest = proc.splitAttackResolutionStepsByStepNumber (src);
		
		// Check results
		assertEquals (4, dest.size ());
		
		assertEquals (2, dest.get (0).size ());
		assertEquals (1, dest.get (0).get (0).getUnitSkillStep ().getStepNumber ());
		assertEquals (1, dest.get (0).get (1).getUnitSkillStep ().getStepNumber ());
		
		assertEquals (1, dest.get (1).size ());
		assertEquals (2, dest.get (1).get (0).getUnitSkillStep ().getStepNumber ());
		
		assertEquals (3, dest.get (2).size ());
		assertEquals (3, dest.get (2).get (0).getUnitSkillStep ().getStepNumber ());
		assertEquals (3, dest.get (2).get (1).getUnitSkillStep ().getStepNumber ());
		assertEquals (3, dest.get (2).get (2).getUnitSkillStep ().getStepNumber ());
		
		assertEquals (1, dest.get (3).size ());
		assertEquals (4, dest.get (3).get (0).getUnitSkillStep ().getStepNumber ());
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when the source list isn't correctly sorted
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test
	public final void testSplitAttackResolutionStepsByStepNumber_OutOfSequence () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStep> src = new ArrayList<AttackResolutionStep> ();
		for (final int stepNumber : new int [] {1, 1, 2, 3, 4, 3})
		{
			final AttackResolutionStep step = new AttackResolutionStep ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		
		assertThrows (MomException.class, () ->
		{
			proc.splitAttackResolutionStepsByStepNumber (src);
		});
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when the source list skips a number
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test
	public final void testSplitAttackResolutionStepsByStepNumber_SkippedSequence () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStep> src = new ArrayList<AttackResolutionStep> ();
		for (final int stepNumber : new int [] {1, 1, 2, 4, 4})
		{
			final AttackResolutionStep step = new AttackResolutionStep ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		
		assertThrows (MomException.class, () ->
		{
			proc.splitAttackResolutionStepsByStepNumber (src);
		});
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when there's a value below 0
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test
	public final void testSplitAttackResolutionStepsByStepNumber_Zero () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStep> src = new ArrayList<AttackResolutionStep> ();
		for (final int stepNumber : new int [] {0, 1, 1, 2, 3, 3, 3, 4})
		{
			final AttackResolutionStep step = new AttackResolutionStep ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		
		assertThrows (MomException.class, () ->
		{
			proc.splitAttackResolutionStepsByStepNumber (src);
		});
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber the list doesn't start with 1
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test
	public final void testSplitAttackResolutionStepsByStepNumber_SkippedStart () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStep> src = new ArrayList<AttackResolutionStep> ();
		for (final int stepNumber : new int [] {2, 2, 3, 3, 3, 4})
		{
			final AttackResolutionStep step = new AttackResolutionStep ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		
		assertThrows (MomException.class, () ->
		{
			proc.splitAttackResolutionStepsByStepNumber (src);
		});
	}
	
	/**
	 * Tests the processAttackResolutionStep method making a ranged attack, i.e. no counter-attack
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Ranged () throws Exception
	{
		// Attack resolution steps
		final AttackResolutionStep attackStep = new AttackResolutionStep ();
		attackStep.setCombatSide (UnitCombatSideID.ATTACKER);
		attackStep.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		
		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		steps.add (new AttackResolutionStepContainer (attackStep));

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (createOverlandMap (createOverlandMapCoordinateSystem ()));
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (5);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.HUMAN);
		defendingPd.setPlayerID (7);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		// Units
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (defender), isNull (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		when (expand.expandUnitDetails (eq (defender), anyList (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (attacker), isNull (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);
		when (unitCalc.canMakeRangedAttack (xuAttacker)).thenReturn (true);
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);
		
		defender.getUnitDamage ().add (defenderDamageTaken);
		when (xuDefender.calculateHitPointsRemaining ()).thenReturn (5);
		
		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// We make 5 hit rolls with 40% chance of each one striking
		final DamageCalculator damageCalc = mock (DamageCalculator.class);
		
		final DamageType damageTypeToDefender = new DamageType (); 
		
		final AttackDamage potentialDamageToDefender = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK, null, players, fow, db)).thenReturn (potentialDamageToDefender);
		
		// 3 of them actually hit
		when (damageCalc.calculateSingleFigureDamage (xuDefender, xuAttacker, attackingPlayer, defendingPlayer, potentialDamageToDefender,
			new MapCoordinates3DEx (20, 10, 1), null, fow.getBuilding (), db)).thenReturn (3);
		
		// Range penalty
		final ServerUnitCalculations serverUnitCalculations = mock (ServerUnitCalculations.class);
		
		// Combat details
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), null, 1, 2, null, null, 0, 0, 0, 0);
		when (combatMapServerUtils.findCombatByLocation (combatList, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), "processAttackResolutionStep")).thenReturn (combatDetails);
		
		// Session variables
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getSessionDescription ()).thenReturn (sd);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setServerUnitCalculations (serverUnitCalculations);
		proc.setExpandUnitDetails (expand);
		proc.setUnitUtils (unitUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, new MapCoordinates3DEx (20, 10, 1), steps, mom);
		
		// Check results
		assertEquals (0, attacker.getUnitDamage ().size ());
		
		assertEquals (2, defender.getUnitDamage ().size ());
		assertEquals (3, defender.getUnitDamage ().get (0).getDamageTaken ());
		assertEquals (3, defender.getUnitDamage ().get (1).getDamageTaken ());
	}

	/**
	 * Tests the processAttackResolutionStep method making a melee attack, i.e. defender simultaneously counter-attacks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Melee () throws Exception
	{
		// Attack resolution steps
		final AttackResolutionStep attackStep = new AttackResolutionStep ();
		attackStep.setCombatSide (UnitCombatSideID.ATTACKER);
		attackStep.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);

		final AttackResolutionStep counterattackStep = new AttackResolutionStep ();
		counterattackStep.setCombatSide (UnitCombatSideID.DEFENDER);
		counterattackStep.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);
		
		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		steps.add (new AttackResolutionStepContainer (attackStep));
		steps.add (new AttackResolutionStepContainer (counterattackStep));

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (createOverlandMap (createOverlandMapCoordinateSystem ()));
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (5);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.HUMAN);
		defendingPd.setPlayerID (7);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		// Units
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);

		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (defender), isNull (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		when (expand.expandUnitDetails (eq (defender), anyList (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (attacker), isNull (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);
		when (expand.expandUnitDetails (eq (attacker), anyList (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);
		
		// Attacker has already taken 2 hits, and can take 6 more
		final UnitDamage attackerDamageTaken = new UnitDamage ();
		attackerDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		attackerDamageTaken.setDamageTaken (2);

		attacker.getUnitDamage ().add (attackerDamageTaken);
		when (xuAttacker.calculateHitPointsRemaining ()).thenReturn (6);
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);
		
		defender.getUnitDamage ().add (defenderDamageTaken);
		when (xuDefender.calculateHitPointsRemaining ()).thenReturn (5);

		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Attacker make 5 hit rolls with 40% chance of each one striking; defender makes 6 hit rolls with 30% chance of each one striking
		final DamageCalculator damageCalc = mock (DamageCalculator.class);
		
		final DamageType damageTypeToDefender = new DamageType ();
		final DamageType damageTypeToAttacker = new DamageType ();
		
		final AttackDamage potentialDamageToDefender = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, players, fow, db)).thenReturn (potentialDamageToDefender);

		final AttackDamage potentialDamageToAttacker = new AttackDamage (6, 0, damageTypeToAttacker, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (defenderWrapper, attackerWrapper, attackingPlayer, defendingPlayer,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, null, players, fow, db)).thenReturn (potentialDamageToAttacker);
		
		// 3 of the attacker's hits do damage; 4 of the defender's hits do damage
		when (damageCalc.calculateSingleFigureDamage (xuDefender, xuAttacker, attackingPlayer, defendingPlayer, potentialDamageToDefender,
			new MapCoordinates3DEx (20, 10, 1), null, fow.getBuilding (), db)).thenReturn (3);
		when (damageCalc.calculateSingleFigureDamage (xuAttacker, xuDefender, attackingPlayer, defendingPlayer, potentialDamageToAttacker,
			new MapCoordinates3DEx (20, 10, 1), null, fow.getBuilding (), db)).thenReturn (4);
		
		// Combat details
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), null, 1, 2, null, null, 0, 0, 0, 0);
		when (combatMapServerUtils.findCombatByLocation (combatList, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), "processAttackResolutionStep")).thenReturn (combatDetails);
		
		// Session variables
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setExpandUnitDetails (expand);
		proc.setUnitUtils (unitUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, new MapCoordinates3DEx (20, 10, 1), steps, mom);
		
		// Check results
		assertEquals (2, attacker.getUnitDamage ().size ());
		assertEquals (2, attacker.getUnitDamage ().get (0).getDamageTaken ());
		assertEquals (4, attacker.getUnitDamage ().get (1).getDamageTaken ());

		assertEquals (2, defender.getUnitDamage ().size ());
		assertEquals (3, defender.getUnitDamage ().get (0).getDamageTaken ());
		assertEquals (3, defender.getUnitDamage ().get (1).getDamageTaken ());
	}

	/**
	 * Tests the processAttackResolutionStep method making some skill attacks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Skills () throws Exception
	{
		// Attack resolution steps
		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		for (int n = 1; n <= 4; n++)
		{
			final AttackResolutionStep attackStep = new AttackResolutionStep ();
			attackStep.setCombatSide (UnitCombatSideID.ATTACKER);
			attackStep.setUnitSkillID ("US00" + n);
			steps.add (new AttackResolutionStepContainer (attackStep));
		}

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (createOverlandMap (createOverlandMapCoordinateSystem ()));
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (5);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.HUMAN);
		defendingPd.setPlayerID (7);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		// Units
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);

		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (defender), isNull (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		when (expand.expandUnitDetails (eq (defender), anyList (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (eq (attacker), isNull (), isNull (), isNull (), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);

		defender.getUnitDamage ().add (defenderDamageTaken);
		when (xuDefender.calculateHitPointsRemaining ()).thenReturn (5);

		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Two of the skills we have and so generate some damage, the other two we don't
		final DamageCalculator damageCalc = mock (DamageCalculator.class);
		
		final DamageType damageTypeToDefender = new DamageType ();
		final DamageType damageTypeToAttacker = new DamageType ();
		
		final AttackDamage potentialDamageToDefender1 = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US001",
			null, players, fow, db)).thenReturn (null);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US002",
			null, players, fow, db)).thenReturn (potentialDamageToDefender1);
		
		final AttackDamage potentialDamageToDefender2 = new AttackDamage (4, 0, damageTypeToAttacker, DamageResolutionTypeID.DOOM, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US003",
			null, players, fow, db)).thenReturn (null);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US004",
			null, players, fow, db)).thenReturn (potentialDamageToDefender2);

		// 3+4 of them actually hit
		when (damageCalc.calculateResistOrTakeDamage (xuDefender, attackingPlayer, defendingPlayer, potentialDamageToDefender1, db)).thenReturn (3);
		when (damageCalc.calculateDoomDamage (xuDefender, attackingPlayer, defendingPlayer, potentialDamageToDefender2, db)).thenReturn (4);
		
		// Combat details
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), null, 1, 2, null, null, 0, 0, 0, 0);
		when (combatMapServerUtils.findCombatByLocation (combatList, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), "processAttackResolutionStep")).thenReturn (combatDetails);
		
		// Session variables
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setExpandUnitDetails (expand);
		proc.setUnitUtils (unitUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, new MapCoordinates3DEx (20, 10, 1), steps, mom);
		
		// Check results
		assertEquals (0, attacker.getUnitDamage ().size ());

		assertEquals (2, defender.getUnitDamage ().size ());
		assertEquals (3, defender.getUnitDamage ().get (0).getDamageTaken ());
		assertEquals (7, defender.getUnitDamage ().get (1).getDamageTaken ());
	}

	/**
	 * Tests the processAttackResolutionStep method dealing with damage from a spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Spell () throws Exception
	{
		// Attack resolution steps
		final DamageCalculator damageCalc = mock (DamageCalculator.class);

		final DamageType damageTypeToDefender = new DamageType ();

		final AttackDamage spellDamage = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);

		final List<AttackResolutionStepContainer> steps = new ArrayList<AttackResolutionStepContainer> ();
		steps.add (new AttackResolutionStepContainer (spellDamage));

		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		fow.setMap (createOverlandMap (createOverlandMapCoordinateSystem ()));
		
		// Players
		final PlayerDescription attackingPd = new PlayerDescription ();
		attackingPd.setPlayerType (PlayerType.HUMAN);
		attackingPd.setPlayerID (5);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPd, null, null, null, null);
		
		final PlayerDescription defendingPd = new PlayerDescription ();
		defendingPd.setPlayerType (PlayerType.HUMAN);
		defendingPd.setPlayerID (7);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPd, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (attackingPlayer);
		players.add (defendingPlayer);

		// Units
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);

		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (defender, null, null, null, players, fow, db)).thenReturn (xuDefender);
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);

		defender.getUnitDamage ().add (defenderDamageTaken);
		when (xuDefender.calculateHitPointsRemaining ()).thenReturn (5);

		// Wrapper
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// 3 of them actually hit
		when (damageCalc.calculateSingleFigureDamage (xuDefender, null, attackingPlayer, defendingPlayer, steps.get (0).getSpellStep (),
			new MapCoordinates3DEx (20, 10, 1), null, fow.getBuilding (), db)).thenReturn (3);

		// Combat details
		final CombatMapServerUtils combatMapServerUtils = mock (CombatMapServerUtils.class);
		final List<CombatDetails> combatList = new ArrayList<CombatDetails> ();
		
		final CombatDetails combatDetails = new CombatDetails (1, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), null, 1, 2, null, null, 0, 0, 0, 0);
		when (combatMapServerUtils.findCombatByLocation (combatList, new MapCoordinates3DEx (new MapCoordinates3DEx (20, 10, 1)), "processAttackResolutionStep")).thenReturn (combatDetails);
		
		// Session variables
		final MomSessionDescription sd = new MomSessionDescription ();
		sd.setCombatMapSize (combatMapSize);
		
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (fow);
		
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getServerDB ()).thenReturn (db);
		when (mom.getCombatDetails ()).thenReturn (combatList);
		
		// Set up object to test
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setExpandUnitDetails (expand);
		proc.setUnitUtils (unitUtils);
		proc.setCombatMapServerUtils (combatMapServerUtils);
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (null, defenderWrapper, attackingPlayer, defendingPlayer, new MapCoordinates3DEx (20, 10, 1), steps, mom);
		
		// Check results
		assertEquals (2, defender.getUnitDamage ().size ());
		assertEquals (3, defender.getUnitDamage ().get (0).getDamageTaken ());
		assertEquals (3, defender.getUnitDamage ().get (1).getDamageTaken ());
	}
}
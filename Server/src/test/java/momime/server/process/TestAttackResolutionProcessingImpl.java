package momime.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitDamage;
import momime.common.utils.UnitSkillUtils;
import momime.common.utils.UnitUtilsImpl;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.AttackResolutionConditionSvr;
import momime.server.database.AttackResolutionStepSvr;
import momime.server.database.AttackResolutionSvr;
import momime.server.database.DamageTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSkillSvr;
import momime.server.utils.UnitServerUtilsImpl;

/**
 * Tests the AttackResolutionProcessingImpl class
 */
public final class TestAttackResolutionProcessingImpl
{
	/**
	 * Tests the chooseAttackResolution method when an appropraite attack resolution does exist 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testChooseAttackResolution_Exists () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Set up players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		

		// Units
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (1);

		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (2);
		
		final List<MemoryUnit> attackers = new ArrayList<MemoryUnit> ();
		attackers.add (attacker);
		
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);

		when (unitSkillUtils.getModifiedSkillValue (attacker, attacker.getUnitHasSkill (), "US001", defenders,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);

		when (unitSkillUtils.getModifiedSkillValue (defender, defender.getUnitHasSkill (), "US002", attackers,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (1);
		
		// Attack resolutions to choose between - first one that doesn't match (see mocked skill values above, attacker returns -1 for this)
		final AttackResolutionConditionSvr condition1 = new AttackResolutionConditionSvr ();
		condition1.setCombatSide (UnitCombatSideID.ATTACKER);
		condition1.setUnitSkillID ("US001");
		
		final AttackResolutionSvr res1 = new AttackResolutionSvr ();
		res1.getAttackResolutionConditions ().add (condition1);

		// Now one that does match
		final AttackResolutionConditionSvr condition2 = new AttackResolutionConditionSvr ();
		condition2.setCombatSide (UnitCombatSideID.DEFENDER);
		condition2.setUnitSkillID ("US002");
		
		final AttackResolutionSvr res2 = new AttackResolutionSvr ();
		res2.getAttackResolutionConditions ().add (condition2);
		
		final UnitSkillSvr unitAttr = new UnitSkillSvr ();
		unitAttr.getAttackResolutions ().add (res1);
		unitAttr.getAttackResolutions ().add (res2);
		when (db.findUnitSkill ("UA01", "chooseAttackResolution")).thenReturn (unitAttr);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setUnitSkillUtils (unitSkillUtils);

		// Run method
		final AttackResolutionSvr chosen = proc.chooseAttackResolution (attacker, defender, "UA01", players, fow, db);
		
		// Check results
		assertSame (res2, chosen);
	}

	/**
	 * Tests the chooseAttackResolution method when no appropraite attack resolution exists 
	 * @throws Exception If there is a problem
	 */
	@Test(expected=MomException.class)
	public final void testChooseAttackResolution_NotExists () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Set up players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		

		// Units
		final UnitSkillUtils unitSkillUtils = mock (UnitSkillUtils.class);
		
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (1);

		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (2);

		final List<MemoryUnit> attackers = new ArrayList<MemoryUnit> ();
		attackers.add (attacker);
		
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);

		when (unitSkillUtils.getModifiedSkillValue (attacker, attacker.getUnitHasSkill (), "US001", defenders,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);

		when (unitSkillUtils.getModifiedSkillValue (defender, defender.getUnitHasSkill (), "US002", attackers,
			UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH, null, null, players, fow, db)).thenReturn (-1);
		
		// Attack resolutions to choose between - first one that doesn't match (see mocked skill values above, attacker returns -1 for this)
		final AttackResolutionConditionSvr condition1 = new AttackResolutionConditionSvr ();
		condition1.setCombatSide (UnitCombatSideID.ATTACKER);
		condition1.setUnitSkillID ("US001");
		
		final AttackResolutionSvr res1 = new AttackResolutionSvr ();
		res1.getAttackResolutionConditions ().add (condition1);

		// Now another one that doesn't match
		final AttackResolutionConditionSvr condition2 = new AttackResolutionConditionSvr ();
		condition2.setCombatSide (UnitCombatSideID.DEFENDER);
		condition2.setUnitSkillID ("US002");
		
		final AttackResolutionSvr res2 = new AttackResolutionSvr ();
		res2.getAttackResolutionConditions ().add (condition2);
		
		final UnitSkillSvr unitAttr = new UnitSkillSvr ();
		unitAttr.getAttackResolutions ().add (res1);
		unitAttr.getAttackResolutions ().add (res2);
		when (db.findUnitSkill ("UA01", "chooseAttackResolution")).thenReturn (unitAttr);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setUnitSkillUtils (unitSkillUtils);

		// Run method
		proc.chooseAttackResolution (attacker, defender, "UA01", players, fow, db);
	}
	
	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when sorting a valid list
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test
	public final void testSplitAttackResolutionStepsByStepNumber_Valid () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStepSvr> src = new ArrayList<AttackResolutionStepSvr> ();
		for (final int stepNumber : new int [] {1, 1, 2, 3, 3, 3, 4})
		{
			final AttackResolutionStepSvr step = new AttackResolutionStepSvr ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		final List<List<AttackResolutionStepSvr>> dest = proc.splitAttackResolutionStepsByStepNumber (src);
		
		// Check results
		assertEquals (4, dest.size ());
		
		assertEquals (2, dest.get (0).size ());
		assertEquals (1, dest.get (0).get (0).getStepNumber ());
		assertEquals (1, dest.get (0).get (1).getStepNumber ());
		
		assertEquals (1, dest.get (1).size ());
		assertEquals (2, dest.get (1).get (0).getStepNumber ());
		
		assertEquals (3, dest.get (2).size ());
		assertEquals (3, dest.get (2).get (0).getStepNumber ());
		assertEquals (3, dest.get (2).get (1).getStepNumber ());
		assertEquals (3, dest.get (2).get (2).getStepNumber ());
		
		assertEquals (1, dest.get (3).size ());
		assertEquals (4, dest.get (3).get (0).getStepNumber ());
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when the source list isn't correctly sorted
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test(expected=MomException.class)
	public final void testSplitAttackResolutionStepsByStepNumber_OutOfSequence () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStepSvr> src = new ArrayList<AttackResolutionStepSvr> ();
		for (final int stepNumber : new int [] {1, 1, 2, 3, 4, 3})
		{
			final AttackResolutionStepSvr step = new AttackResolutionStepSvr ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.splitAttackResolutionStepsByStepNumber (src);
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when the source list skips a number
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test(expected=MomException.class)
	public final void testSplitAttackResolutionStepsByStepNumber_SkippedSequence () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStepSvr> src = new ArrayList<AttackResolutionStepSvr> ();
		for (final int stepNumber : new int [] {1, 1, 2, 4, 4})
		{
			final AttackResolutionStepSvr step = new AttackResolutionStepSvr ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.splitAttackResolutionStepsByStepNumber (src);
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber when there's a value below 0
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test(expected=MomException.class)
	public final void testSplitAttackResolutionStepsByStepNumber_Zero () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStepSvr> src = new ArrayList<AttackResolutionStepSvr> ();
		for (final int stepNumber : new int [] {0, 1, 1, 2, 3, 3, 3, 4})
		{
			final AttackResolutionStepSvr step = new AttackResolutionStepSvr ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.splitAttackResolutionStepsByStepNumber (src);
	}

	/**
	 * Tests the splitAttackResolutionStepsByStepNumber the list doesn't start with 1
	 * @throws MomException If the steps in the input list aren't in stepNumber order
	 */
	@Test(expected=MomException.class)
	public final void testSplitAttackResolutionStepsByStepNumber_SkippedStart () throws MomException
	{
		// Set up example list
		final List<AttackResolutionStepSvr> src = new ArrayList<AttackResolutionStepSvr> ();
		for (final int stepNumber : new int [] {2, 2, 3, 3, 3, 4})
		{
			final AttackResolutionStepSvr step = new AttackResolutionStepSvr ();
			step.setStepNumber (stepNumber);
			src.add (step);
		}
		
		// Run method
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.splitAttackResolutionStepsByStepNumber (src);
	}
	
	/**
	 * Tests the processAttackResolutionStep method making a ranged attack, i.e. no counter-attack
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Ranged () throws Exception
	{
		// Attack resolution steps
		final AttackResolutionStepSvr attackStep = new AttackResolutionStepSvr ();
		attackStep.setCombatSide (UnitCombatSideID.ATTACKER);
		attackStep.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK);
		
		final List<AttackResolutionStepSvr> steps = new ArrayList<AttackResolutionStepSvr> ();
		steps.add (attackStep);

		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
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
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		when (unitCalc.canMakeRangedAttack (attacker, players, fow, db)).thenReturn (true);
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);
		
		defender.getUnitDamage ().add (defenderDamageTaken);
		when (unitCalc.calculateHitPointsRemaining (defender, players, fow, db)).thenReturn (5);
		
		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// We make 5 hit rolls with 40% chance of each one striking
		final DamageCalculator damageCalc = mock (DamageCalculator.class);
		
		final DamageTypeSvr damageTypeToDefender = new DamageTypeSvr (); 
		
		final AttackDamage potentialDamageToDefender = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK,
			players, fow, db)).thenReturn (potentialDamageToDefender);
		
		// 3 of them actually hit
		when (damageCalc.calculateSingleFigureDamage (defenderWrapper, attackingPlayer, defendingPlayer,
			potentialDamageToDefender, players, fow, db)).thenReturn (3);
		
		// Range penalty
		final ServerUnitCalculations serverUnitCalculations = mock (ServerUnitCalculations.class);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setServerUnitCalculations (serverUnitCalculations);
		proc.setUnitUtils (new UnitUtilsImpl ());
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer,
			steps, null, players, fow, combatMapSize, db);
		
		// Check results
		//assertEquals (0, attacker.getDamageTaken ());
		//assertEquals (3+3, defender.getDamageTaken ());
	}

	/**
	 * Tests the processAttackResolutionStep method making a melee attack, i.e. defender simultaneously counter-attacks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Melee () throws Exception
	{
		// Attack resolution steps
		final AttackResolutionStepSvr attackStep = new AttackResolutionStepSvr ();
		attackStep.setCombatSide (UnitCombatSideID.ATTACKER);
		attackStep.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);

		final AttackResolutionStepSvr counterattackStep = new AttackResolutionStepSvr ();
		counterattackStep.setCombatSide (UnitCombatSideID.DEFENDER);
		counterattackStep.setUnitSkillID (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK);
		
		final List<AttackResolutionStepSvr> steps = new ArrayList<AttackResolutionStepSvr> ();
		steps.add (attackStep);
		steps.add (counterattackStep);

		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
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
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		// Attacker has already taken 2 hits, and can take 6 more
		final UnitDamage attackerDamageTaken = new UnitDamage ();
		attackerDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		attackerDamageTaken.setDamageTaken (2);

		attacker.getUnitDamage ().add (attackerDamageTaken);
		when (unitCalc.calculateHitPointsRemaining (attacker, players, fow, db)).thenReturn (6);
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);
		
		defender.getUnitDamage ().add (defenderDamageTaken);
		when (unitCalc.calculateHitPointsRemaining (defender, players, fow, db)).thenReturn (5);

		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Attacker make 5 hit rolls with 40% chance of each one striking; defender makes 6 hit rolls with 30% chance of each one striking
		final DamageCalculator damageCalc = mock (DamageCalculator.class);
		
		final DamageTypeSvr damageTypeToDefender = new DamageTypeSvr ();
		final DamageTypeSvr damageTypeToAttacker = new DamageTypeSvr ();
		
		final AttackDamage potentialDamageToDefender = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			players, fow, db)).thenReturn (potentialDamageToDefender);

		final AttackDamage potentialDamageToAttacker = new AttackDamage (6, 0, damageTypeToAttacker, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (defenderWrapper, attackerWrapper, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			players, fow, db)).thenReturn (potentialDamageToAttacker);
		
		// 3 of the attacker's hits do damage; 4 of the defender's hits do damage
		when (damageCalc.calculateSingleFigureDamage (defenderWrapper, attackingPlayer, defendingPlayer,
			potentialDamageToDefender, players, fow, db)).thenReturn (3);
		when (damageCalc.calculateSingleFigureDamage (attackerWrapper, attackingPlayer, defendingPlayer,
			potentialDamageToAttacker, players, fow, db)).thenReturn (4);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setUnitUtils (new UnitUtilsImpl ());
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer,
			steps, null, players, fow, combatMapSize, db);
		
		// Check results
		//assertEquals (2+4, attacker.getDamageTaken ());
		//assertEquals (3+3, defender.getDamageTaken ());
	}

	/**
	 * Tests the processAttackResolutionStep method making some skill attacks
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Skills () throws Exception
	{
		// Attack resolution steps
		final List<AttackResolutionStepSvr> steps = new ArrayList<AttackResolutionStepSvr> ();
		for (int n = 1; n <= 4; n++)
		{
			final AttackResolutionStepSvr attackStep = new AttackResolutionStepSvr ();
			attackStep.setCombatSide (UnitCombatSideID.ATTACKER);
			attackStep.setUnitSkillID ("US00" + n);
			steps.add (attackStep);
		}

		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
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
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (101);
		attacker.setOwningPlayerID (defendingPd.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);

		defender.getUnitDamage ().add (defenderDamageTaken);
		when (unitCalc.calculateHitPointsRemaining (defender, players, fow, db)).thenReturn (5);

		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Two of the skills we have and so generate some damage, the other two we don't
		final DamageCalculator damageCalc = mock (DamageCalculator.class);
		
		final DamageTypeSvr damageTypeToDefender = new DamageTypeSvr ();
		final DamageTypeSvr damageTypeToAttacker = new DamageTypeSvr ();
		
		final AttackDamage potentialDamageToDefender1 = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US002",
			players, fow, db)).thenReturn (potentialDamageToDefender1);
		
		final AttackDamage potentialDamageToDefender2 = new AttackDamage (4, 0, damageTypeToAttacker, DamageResolutionTypeID.DOOM, null, null, null, 1);
		when (damageCalc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US004",
			players, fow, db)).thenReturn (potentialDamageToDefender2);

		// 3+4 of them actually hit
		when (damageCalc.calculateResistOrTakeDamage (defenderWrapper, attackingPlayer, defendingPlayer,
			potentialDamageToDefender1, players, fow, db)).thenReturn (3);
		when (damageCalc.calculateDoomDamage (defenderWrapper, attackingPlayer, defendingPlayer,
			potentialDamageToDefender2, players, fow, db)).thenReturn (4);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setUnitUtils (new UnitUtilsImpl ());
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer,
			steps, null, players, fow, combatMapSize, db);
		
		// Check results
		//assertEquals (0, attacker.getDamageTaken ());
		//assertEquals (3+5, defender.getDamageTaken ());		// NB. would be +7, but this is more damage than the unit has HP so it gets reduced to 5
	}

	/**
	 * Tests the processAttackResolutionStep method dealing with damage from a spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testProcessAttackResolutionStep_Spell () throws Exception
	{
		// Attack resolution steps
		final List<AttackResolutionStepSvr> steps = new ArrayList<AttackResolutionStepSvr> ();
		steps.add (null);

		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Combat map
		final CombatMapSize combatMapSize = new CombatMapSize ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
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
		final UnitCalculations unitCalc = mock (UnitCalculations.class);

		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (102);
		defender.setOwningPlayerID (attackingPd.getPlayerID ());
		
		// Defender has already taken 3 hits, and can take 5 more
		final UnitDamage defenderDamageTaken = new UnitDamage ();
		defenderDamageTaken.setDamageType (StoredDamageTypeID.HEALABLE);
		defenderDamageTaken.setDamageTaken (3);

		defender.getUnitDamage ().add (defenderDamageTaken);
		when (unitCalc.calculateHitPointsRemaining (defender, players, fow, db)).thenReturn (5);

		// Wrapper
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Spell does preset damage
		final DamageCalculator damageCalc = mock (DamageCalculator.class);

		final DamageTypeSvr damageTypeToDefender = new DamageTypeSvr ();

		final AttackDamage potentialDamageToDefender = new AttackDamage (5, 1, damageTypeToDefender, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		
		// 3 of them actually hit
		when (damageCalc.calculateSingleFigureDamage (defenderWrapper, attackingPlayer, defendingPlayer,
			potentialDamageToDefender, players, fow, db)).thenReturn (3);
		
		// Set up object to test
		final AttackResolutionProcessingImpl proc = new AttackResolutionProcessingImpl ();
		proc.setDamageCalculator (damageCalc);
		proc.setUnitCalculations (unitCalc);
		proc.setUnitUtils (new UnitUtilsImpl ());
		proc.setUnitServerUtils (new UnitServerUtilsImpl ());
		
		// Run method
		proc.processAttackResolutionStep (null, defenderWrapper, attackingPlayer, defendingPlayer, steps,
			potentialDamageToDefender, players, fow, combatMapSize, db);
		
		// Check results
		//assertEquals (3+3, defender.getDamageTaken ());
	}
}
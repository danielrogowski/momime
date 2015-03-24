package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.UnitAttributeComponent;
import momime.common.utils.UnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

/**
 * Tests the DamageCalculatorImpl class
 */
public final class TestDamageCalculatorImpl
{
	/**
	 * Tests the sendDamageCalculationMessage class with two AI players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testsSndDamageCalculationMessage_TwoAIPlayers () throws Exception
	{
		// Players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (-1);
		attackingPD.setHuman (false);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		final DummyServerToClientConnection defendingConn = new DummyServerToClientConnection ();
		defendingPlayer.setConnection (defendingConn);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		
		// Run test
		final DamageCalculationMessage msg = new DamageCalculationMessage (); 
		calc.sendDamageCalculationMessage (attackingPlayer, defendingPlayer, msg);
		
		// Check results
		assertEquals (0, attackingConn.getMessages ().size ());
		assertEquals (0, defendingConn.getMessages ().size ());
	}
	
	/**
	 * Tests the sendDamageCalculationMessage class with two human players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testsSndDamageCalculationMessage_TwoHumanPlayers () throws Exception
	{
		// Players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (4);
		defendingPD.setHuman (true);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		final DummyServerToClientConnection defendingConn = new DummyServerToClientConnection ();
		defendingPlayer.setConnection (defendingConn);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		
		// Run test
		final DamageCalculationMessage msg = new DamageCalculationMessage (); 
		calc.sendDamageCalculationMessage (attackingPlayer, defendingPlayer, msg);

		// Check results
		assertEquals (1, attackingConn.getMessages ().size ());
		assertSame (msg, attackingConn.getMessages ().get (0));
		
		assertEquals (1, defendingConn.getMessages ().size ());
		assertSame (msg, defendingConn.getMessages ().get (0));
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
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (attacker, players, spells, combatAreaEffects, db)).thenReturn (6);		// Attacker has 6 figures...

		when (unitUtils.getModifiedAttributeValue (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// ..and 3 swords, so 18 hits...

		when (unitUtils.getModifiedAttributeValue (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);	// ..with 40% chance to hit on each
		
		// Set up defender stats
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender is 4 figure unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 shields...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (2);	// ..with 50% chance to block on each

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		
		when (unitCalculations.calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db)).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8,		// Attack rolls, 6 of them are <4
			5, 8, 3, 9,		// First figure is unlucky and only blocks 1 hit, then loses its 2 HP and dies
			1, 5, 8, 2);		// Second figure blocks 2 of the hits, then loses 1 HP
								// So in total, 3 of the dmg went against HP (which is the overall result of the method call)
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setRandomUtils (random);
		
		// Run test
		final DamageCalculationMessage msg = new DamageCalculationMessage ();
		assertEquals (3, calc.calculateDamage (attacker, defender, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			msg, players, spells, combatAreaEffects, db));
		
		// Check the message that got sent to the attacker
		assertEquals (1, attackingMsgs.getMessages ().size ());
		assertSame (msg, attackingMsgs.getMessages ().get (0));
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_AND_DEFENCE_STATISTICS, msg.getMessageType ());
		assertEquals (22, msg.getAttackerUnitURN ().intValue ());
		assertEquals (33, msg.getDefenderUnitURN ().intValue ());
		assertEquals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, msg.getAttackAttributeID ());
		
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
		
		assertEquals (2, msg.getActualBlockedHits ().size ());		// 1st figure blocked 1 hit, 2nd figure blocked 2 hits
		assertEquals (1, msg.getActualBlockedHits ().get (0).intValue ());
		assertEquals (2, msg.getActualBlockedHits ().get (1).intValue ());
	}
}
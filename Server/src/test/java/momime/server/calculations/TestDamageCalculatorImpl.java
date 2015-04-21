package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageTypeID;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.UnitAttributeComponent;
import momime.common.utils.UnitAttributePositiveNegative;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.utils.UnitServerUtils;

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
	public final void testSendDamageCalculationMessage_TwoAIPlayers () throws Exception
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
		final DamageCalculationData data = new DamageCalculationData (); 
		calc.sendDamageCalculationMessage (attackingPlayer, defendingPlayer, data);
		
		// Check results
		assertEquals (0, attackingConn.getMessages ().size ());
		assertEquals (0, defendingConn.getMessages ().size ());
	}
	
	/**
	 * Tests the sendDamageCalculationMessage class with two human players
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testSendDamageCalculationMessage_TwoHumanPlayers () throws Exception
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
		final DamageCalculationData data = new DamageCalculationData (); 
		calc.sendDamageCalculationMessage (attackingPlayer, defendingPlayer, data);

		// Check results
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (data, msg.getBreakdown ());
		
		assertEquals (1, defendingConn.getMessages ().size ());
		assertSame (msg, defendingConn.getMessages ().get (0));
	}
	
	/**
	 * Tests the attackFromUnit method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromUnit () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);

		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		

		// Set up unit
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (22);
		attacker.setOwningPlayerID (attackingPD.getPlayerID ());
		
		// Set up attacker stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (attacker, players, spells, combatAreaEffects, db)).thenReturn (6);		// Attacker has 6 figures...

		when (unitUtils.getModifiedAttributeValue (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// ..and 3 swords, so 18 hits...

		when (unitUtils.getModifiedAttributeValue (attacker, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (1);	// ..with 40% chance to hit on each
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final AttackDamage dmg = calc.attackFromUnit (attacker, attackingPlayer, defendingPlayer,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, players, spells, combatAreaEffects, db);
		
		// Check results
		assertEquals (18, dmg.getPotentialHits ().intValue ());
		assertEquals (1, dmg.getPlusToHit ());
		assertEquals (DamageTypeID.SINGLE_FIGURE, dmg.getDamageType ());

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		
		assertEquals (DamageCalculationAttackData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationAttackData data = (DamageCalculationAttackData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_DATA, data.getMessageType ());
	    assertEquals (attacker.getUnitURN (), data.getAttackerUnitURN ().intValue ());
	    assertEquals (attackingPD.getPlayerID ().intValue (), data.getAttackerPlayerID ());
	    assertNull (data.getAttackSkillID ());
	    assertNull (data.getAttackSpellID ());
	    assertEquals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, data.getAttackAttributeID ());
	    assertEquals (DamageTypeID.SINGLE_FIGURE, data.getDamageType ());
	    assertEquals (6, data.getAttackerFigures ().intValue ());
	    assertEquals (3, data.getAttackStrength ().intValue ());
	    assertEquals (18, data.getPotentialHits ().intValue ());
	}

	/**
	 * Tests the attackFromSpell method on a spell with fixed damage
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromSpell_FixedDamage () throws Exception
	{
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		// Spell details
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		spell.setCombatBaseDamage (12);
		spell.setAttackSpellDamageType (DamageTypeID.DOOM);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		
		// Run test
		final PlayerServerDetails castingPlayer = attackingPlayer;
		final AttackDamage dmg = calc.attackFromSpell (spell, null, castingPlayer, attackingPlayer, defendingPlayer);
		
		// Check results
		assertEquals (12, dmg.getPotentialHits ().intValue ());
		assertEquals (0, dmg.getPlusToHit ());
		assertEquals (DamageTypeID.DOOM, dmg.getDamageType ());

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		
		assertEquals (DamageCalculationAttackData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationAttackData data = (DamageCalculationAttackData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_DATA, data.getMessageType ());
	    assertNull (data.getAttackerUnitURN ());
	    assertEquals (attackingPD.getPlayerID ().intValue (), data.getAttackerPlayerID ());
	    assertNull (data.getAttackSkillID ());
	    assertEquals ("SP001", data.getAttackSpellID ());
	    assertNull (data.getAttackAttributeID ());
	    assertEquals (DamageTypeID.DOOM, data.getDamageType ());
	    assertNull (data.getAttackerFigures ());
	    assertNull (data.getAttackStrength ());
	    assertEquals (12, data.getPotentialHits ().intValue ());
	}
	
	/**
	 * Tests the attackFromSpell method on a spell with variable damage
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromSpell_VariableDamage () throws Exception
	{
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		// Spell details
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		spell.setCombatBaseDamage (12);
		spell.setAttackSpellDamageType (DamageTypeID.DOOM);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		
		// Run test
		final PlayerServerDetails castingPlayer = attackingPlayer;
		final AttackDamage dmg = calc.attackFromSpell (spell, 20, castingPlayer, attackingPlayer, defendingPlayer);
		
		// Check results
		assertEquals (20, dmg.getPotentialHits ().intValue ());
		assertEquals (0, dmg.getPlusToHit ());
		assertEquals (DamageTypeID.DOOM, dmg.getDamageType ());

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		
		assertEquals (DamageCalculationAttackData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationAttackData data = (DamageCalculationAttackData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_DATA, data.getMessageType ());
	    assertNull (data.getAttackerUnitURN ());
	    assertEquals (attackingPD.getPlayerID ().intValue (), data.getAttackerPlayerID ());
	    assertNull (data.getAttackSkillID ());
	    assertEquals ("SP001", data.getAttackSpellID ());
	    assertNull (data.getAttackAttributeID ());
	    assertEquals (DamageTypeID.DOOM, data.getDamageType ());
	    assertNull (data.getAttackerFigures ());
	    assertNull (data.getAttackStrength ());
	    assertEquals (20, data.getPotentialHits ().intValue ());
	}
	
	/**
	 * Tests the calculateSingleFigureDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateSingleFigureDamage () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);
		
		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 shields...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (2);	// ..with 50% chance to block on each

		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8);		// Attack rolls, 6 of them are <4

		// Mock the damage being applied
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (defender, 6, 4, 5, players, spells, combatAreaEffects, db)).thenReturn (3);		// Take 6 hits, each figure has defence 4, with 50% block chance
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		
		// Run test
		assertEquals (3, calc.calculateSingleFigureDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (18, 1, DamageTypeID.SINGLE_FIGURE),
			players, spells, combatAreaEffects, db));
		
		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertEquals (4, data.getChanceToHit ().intValue ());
		assertEquals (72, data.getTenTimesAverageDamage ().intValue ());		// 18 hits * 0.4 chance = 7.2 average hits
		assertEquals (6, data.getActualHits ().intValue ());
		assertEquals (DamageTypeID.SINGLE_FIGURE, data.getDamageType ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertEquals (5, data.getChanceToDefend ().intValue ());
		assertEquals (20, data.getTenTimesAverageBlock ().intValue ());			// 4 shields * 0.5 chance = 2.0 average blocked
		assertEquals (3, data.getFinalHits ());													// 1st figure blocked 1 hit, 2nd figure blocked 2 hits
	}

	/**
	 * Tests the calculateArmourPiercingDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateArmourPiercingDamage () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);
		
		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 shields...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (2);	// ..with 50% chance to block on each

		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8);		// Attack rolls, 6 of them are <4

		// Mock the damage being applied (NB. now 2 instead of 4 defence)
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (defender, 6, 2, 5, players, spells, combatAreaEffects, db)).thenReturn (3);		// Take 6 hits, each figure has defence 2, with 50% block chance
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		
		// Run test
		assertEquals (3, calc.calculateArmourPiercingDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (18, 1, DamageTypeID.ARMOUR_PIERCING),
			players, spells, combatAreaEffects, db));
		
		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertEquals (4, data.getChanceToHit ().intValue ());
		assertEquals (72, data.getTenTimesAverageDamage ().intValue ());		// 18 hits * 0.4 chance = 7.2 average hits
		assertEquals (6, data.getActualHits ().intValue ());
		assertEquals (DamageTypeID.ARMOUR_PIERCING, data.getDamageType ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (2, data.getModifiedDefenceStrength ().intValue ());			// <-- Now halved
		assertEquals (5, data.getChanceToDefend ().intValue ());
		assertEquals (10, data.getTenTimesAverageBlock ().intValue ());			// 2 shields * 0.5 chance = 2.0 average blocked	<-- Now halved
		assertEquals (3, data.getFinalHits ());													// 1st figure blocked 1 hit, 2nd figure blocked 2 hits
	}

	/**
	 * Tests the calculateIllusionaryDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateIllusionaryDamage () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);
		
		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 shields...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (2);	// ..with 50% chance to block on each

		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8);		// Attack rolls, 6 of them are <4

		// Mock the damage being applied (NB. now 0 instead of 4 defence)
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (defender, 6, 0, 5, players, spells, combatAreaEffects, db)).thenReturn (6);		// Take 6 hits, each figure has defence 0, with 50% block chance
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		
		// Run test
		assertEquals (6, calc.calculateIllusionaryDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (18, 1, DamageTypeID.ILLUSIONARY),
			players, spells, combatAreaEffects, db));
		
		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertEquals (4, data.getChanceToHit ().intValue ());
		assertEquals (72, data.getTenTimesAverageDamage ().intValue ());		// 18 hits * 0.4 chance = 7.2 average hits
		assertEquals (6, data.getActualHits ().intValue ());
		assertEquals (DamageTypeID.ILLUSIONARY, data.getDamageType ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (0, data.getModifiedDefenceStrength ().intValue ());			// <-- Now zeroed
		assertEquals (5, data.getChanceToDefend ().intValue ());
		assertEquals (0, data.getTenTimesAverageBlock ().intValue ());				// <-- Now zeroed
		assertEquals (6, data.getFinalHits ());													// Nothing can get blocked
	}
	
	/**
	 * Tests the calculateMultiFigureDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateMultiFigureDamage () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);

		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 shields...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (2);	// ..with 50% chance to block on each

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// Each defending figure normally has 3 hearts...
				
		when (unitCalculations.calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db)).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn
			(3, 7, 1, 2,		// First figure gets hit 3 times...
			6, 7, 8, 9,		// ...blocking none of them, but it was already down to 2 HP so it can't take 3 damage
			1, 2, 3, 4,		// Second figure also gets hit 3 times...
			5, 6, 7, 8,		// ...blocking none of them so loses all its 3 HP
			6, 1, 5, 8,		// Third figure only gets hit once...
			3, 4, 5, 6);		// ...but blocks twice, so it blocks more hits than it receives, and takes no damage
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setRandomUtils (random);
		
		// Run test
		assertEquals (5, calc.calculateMultiFigureDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (4, 1, DamageTypeID.MULTI_FIGURE),
			players, spells, combatAreaEffects, db));

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertEquals (4, data.getChanceToHit ().intValue ());
		assertEquals (48, data.getTenTimesAverageDamage ().intValue ());		// 4 hits * 0.4 chance * 3 alive figures = 4.8 average hits
		assertEquals (7, data.getActualHits ().intValue ());									// 3+3+1, see comments against dice rolls
		assertEquals (DamageTypeID.MULTI_FIGURE, data.getDamageType ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertEquals (5, data.getChanceToDefend ().intValue ());
		assertEquals (20, data.getTenTimesAverageBlock ().intValue ());			// 4 shields * 0.5 chance = 2.0 average blocked
		assertEquals (5, data.getFinalHits ());													// 2+3+0, see comments against dice rolls
	}
	
	/**
	 * Tests the calculateDoomDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDoomDamage () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);

		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...

		// Mock the damage being applied
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (defender, 6, 0, 0, players, spells, combatAreaEffects, db)).thenReturn (6);				// Automatically takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (6, calc.calculateDoomDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (6, 1, DamageTypeID.DOOM),
			players, spells, combatAreaEffects, db));

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertNull (data.getChanceToHit ());
		assertNull (data.getTenTimesAverageDamage ());
		assertEquals (6, data.getActualHits ().intValue ());
		assertEquals (DamageTypeID.DOOM, data.getDamageType ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertNull (data.getUnmodifiedDefenceStrength ());
		assertNull (data.getModifiedDefenceStrength ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (6, data.getFinalHits ());			// Nothing can get blocked
	}
	
	/**
	 * Tests the calculateChanceOfDeathDamage method when we roll that the unit dies
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateChanceOfDeathDamage_Dies () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);

		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...

		// Fix random number generator roll
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (100)).thenReturn (20);		// Dies
		
		// Mock the damage being applied
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (defender, Integer.MAX_VALUE, 0, 0, players, spells, combatAreaEffects, db)).thenReturn (6);		// Takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (6, calc.calculateChanceOfDeathDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (25, 0, DamageTypeID.CHANCE_OF_DEATH),
			players, spells, combatAreaEffects, db));

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertNull (data.getChanceToHit ());
		assertNull (data.getTenTimesAverageDamage ());
		assertEquals (20, data.getActualHits ().intValue ());
		assertEquals (DamageTypeID.CHANCE_OF_DEATH, data.getDamageType ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertNull (data.getUnmodifiedDefenceStrength ());
		assertNull (data.getModifiedDefenceStrength ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (6, data.getFinalHits ());			// Only the actual number of HP the unit actually had is recorded
	}

	/**
	 * Tests the calculateChanceOfDeathDamage method when we roll that the unit lives
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateChanceOfDeathDamage_Lives () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);

		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...

		// Fix random number generator roll
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (100)).thenReturn (80);		// Lives
		
		// Mock the damage being applied
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (defender, Integer.MAX_VALUE, 0, 0, players, spells, combatAreaEffects, db)).thenReturn (6);		// Takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (0, calc.calculateChanceOfDeathDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (25, 0, DamageTypeID.CHANCE_OF_DEATH),
			players, spells, combatAreaEffects, db));

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertNull (data.getChanceToHit ());
		assertNull (data.getTenTimesAverageDamage ());
		assertEquals (80, data.getActualHits ().intValue ());
		assertEquals (DamageTypeID.CHANCE_OF_DEATH, data.getDamageType ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertNull (data.getUnmodifiedDefenceStrength ());
		assertNull (data.getModifiedDefenceStrength ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (0, data.getFinalHits ());			// Takes no damage
	}
	
	/**
	 * Tests the calculateResistOrDieDamage method with no saving throw modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateResistOrDieDamage_NoSavingThrowModifier () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);

		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 resistance...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// Each defending figure normally has 3 hearts...
				
		when (unitCalculations.calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db)).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (5, 4, 3, 2, 1);		// So first two die, other 3 save
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setRandomUtils (random);
		
		// Run test
		assertEquals (5, calc.calculateResistOrDieDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (null, 0, DamageTypeID.RESIST_OR_DIE),
			players, spells, combatAreaEffects, db));

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertNull (data.getChanceToHit ());
		assertNull (data.getTenTimesAverageDamage ());
		assertEquals (2, data.getActualHits ().intValue ());									// 2 figures died
		assertEquals (DamageTypeID.RESIST_OR_DIE, data.getDamageType ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (5, data.getFinalHits ());													// 2+3 for the 2 dead figures (one was already hurt)
	}

	/**
	 * Tests the calculateResistOrDieDamage method with a -2 saving throw modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateResistOrDieDamage_WithSavingThrowModifier () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up players
		final PlayerDescription attackingPD = new PlayerDescription ();
		attackingPD.setPlayerID (3);
		attackingPD.setHuman (true);
		
		final PlayerServerDetails attackingPlayer = new PlayerServerDetails (attackingPD, null, null, null, null);
		
		final DummyServerToClientConnection attackingConn = new DummyServerToClientConnection ();
		attackingPlayer.setConnection (attackingConn);
		
		final PlayerDescription defendingPD = new PlayerDescription ();
		defendingPD.setPlayerID (-2);
		defendingPD.setHuman (false);
		
		final PlayerServerDetails defendingPlayer = new PlayerServerDetails (defendingPD, null, null, null, null);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();		
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (33);

		// Set up defender stats
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (unitCalculations.calculateAliveFigureCount (defender, players, spells, combatAreaEffects, db)).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		
		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (4);	// ..and 4 resistance...

		when (unitUtils.getModifiedAttributeValue (defender, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS,
			UnitAttributeComponent.ALL, UnitAttributePositiveNegative.BOTH, players, spells, combatAreaEffects, db)).thenReturn (3);	// Each defending figure normally has 3 hearts...
				
		when (unitCalculations.calculateHitPointsRemainingOfFirstFigure (defender, players, spells, combatAreaEffects, db)).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (5, 4, 3, 2, 1);		// So first four die, last one saves
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitCalculations (unitCalculations);
		calc.setUnitUtils (unitUtils);
		calc.setRandomUtils (random);
		
		// Run test
		assertEquals (11, calc.calculateResistOrDieDamage (defender, attackingPlayer, defendingPlayer, new AttackDamage (2, 0, DamageTypeID.RESIST_OR_DIE),
			players, spells, combatAreaEffects, db));

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		assertSame (msg, attackingConn.getMessages ().get (0));

		assertEquals (DamageCalculationDefenceData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationDefenceData data = (DamageCalculationDefenceData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.DEFENCE_DATA, data.getMessageType ());
		assertEquals (33, data.getDefenderUnitURN ());
		
		assertNull (data.getChanceToHit ());
		assertNull (data.getTenTimesAverageDamage ());
		assertEquals (4, data.getActualHits ().intValue ());									// 4 figures died
		assertEquals (DamageTypeID.RESIST_OR_DIE, data.getDamageType ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (2, data.getModifiedDefenceStrength ().intValue ());			// Effective resistance of 2
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (11, data.getFinalHits ());													// 2+3+3+3 for the 4 dead figures (one was already hurt)
	}
}
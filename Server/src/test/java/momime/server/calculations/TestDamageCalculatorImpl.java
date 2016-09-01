package momime.server.calculations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.random.RandomUtils;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamagePerFigureID;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.StoredDamageTypeID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.servertoclient.DamageCalculationAttackData;
import momime.common.messages.servertoclient.DamageCalculationData;
import momime.common.messages.servertoclient.DamageCalculationDefenceData;
import momime.common.messages.servertoclient.DamageCalculationMessage;
import momime.common.messages.servertoclient.DamageCalculationMessageTypeID;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.DummyServerToClientConnection;
import momime.server.database.DamageTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.SpellSvr;
import momime.server.database.UnitSkillSvr;
import momime.server.process.AttackResolutionUnit;
import momime.server.utils.UnitServerUtils;

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
	 * Tests the attackFromUnitSkill method on an attack that is multipled up by the number of figures in the attacking unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromUnitSkill_PerFigure () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// The kind of damage inflicted by this skill
		final UnitSkillSvr unitSkill = new UnitSkillSvr ();
		unitSkill.setDamageResolutionTypeID (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE);
		unitSkill.setDamagePerFigure (DamagePerFigureID.PER_FIGURE_COMBINED);
		when (db.findUnitSkill ("US001", "attackFromUnitSkill")).thenReturn (unitSkill);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
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

		// Set up units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (22);
		attacker.setOwningPlayerID (attackingPD.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (23);

		// 2 of the attacker figures are frozen in fear so cannot attack
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		attackerWrapper.setFiguresFrozenInFear (2);

		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);
		
		// Expanded details
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (attacker), anyListOf (ExpandedUnitDetails.class), eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);

		when (xuAttacker.calculateAliveFigureCount ()).thenReturn (6);		// Attacker has 6 figures...
		when (xuAttacker.hasModifiedSkill ("US001")).thenReturn (true);
		when (xuAttacker.getModifiedSkillValue ("US001")).thenReturn (3);	// ..and strength 3 attack per figure, so 18 hits...
		when (xuAttacker.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)).thenReturn (1);	// ..with 40% chance to hit on each
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (defender), anyListOf (ExpandedUnitDetails.class), eq ("US001"), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		// Damage type
		final DamageTypeCalculations damageTypeCalculations = mock (DamageTypeCalculations .class);
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		when (damageTypeCalculations.determineSkillDamageType (xuAttacker, "US001", db)).thenReturn (damageType);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setDamageTypeCalculations (damageTypeCalculations);
		
		// Run test
		final AttackDamage dmg = calc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US001", players, fow, db);
		
		// Check results
		assertEquals (12, dmg.getPotentialHits ().intValue ());
		assertEquals (4, dmg.getChanceToHit ());
		assertEquals (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, dmg.getDamageResolutionTypeID ());

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		
		assertEquals (DamageCalculationAttackData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationAttackData data = (DamageCalculationAttackData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_DATA, data.getMessageType ());
	    assertEquals (attacker.getUnitURN (), data.getAttackerUnitURN ().intValue ());
	    assertEquals (attackingPD.getPlayerID ().intValue (), data.getAttackerPlayerID ());
	    assertEquals ("US001", data.getAttackSkillID ());
	    assertNull (data.getAttackSpellID ());
	    assertEquals (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, data.getDamageResolutionTypeID ());
	    assertEquals (4, data.getAttackerFigures ().intValue ());
	    assertEquals (3, data.getAttackStrength ().intValue ());
	    assertEquals (12, data.getPotentialHits ().intValue ());
	}

	/**
	 * Tests the attackFromUnitSkill method on an attack that remains the same regardless of the number of figures alive in the attacking unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromUnitSkill_PerUnit () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// The kind of damage inflicted by this skill
		final UnitSkillSvr unitSkill = new UnitSkillSvr ();
		unitSkill.setDamageResolutionTypeID (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE);
		unitSkill.setDamagePerFigure (DamagePerFigureID.PER_UNIT);
		when (db.findUnitSkill ("US001", "attackFromUnitSkill")).thenReturn (unitSkill);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
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

		// Set up units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (22);
		attacker.setOwningPlayerID (attackingPD.getPlayerID ());

		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (23);
		
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);

		// 2 of the attacker figures are frozen in fear so cannot attack
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		attackerWrapper.setFiguresFrozenInFear (2);
		
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Expanded details
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (attacker), anyListOf (ExpandedUnitDetails.class), eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);

		when (xuAttacker.calculateAliveFigureCount ()).thenReturn (6);		// Attacker has 6 figures...
		when (xuAttacker.hasModifiedSkill ("US001")).thenReturn (true);
		when (xuAttacker.getModifiedSkillValue ("US001")).thenReturn (3);	// ..and strength 3 attack per figure, so 18 hits...
		when (xuAttacker.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)).thenReturn (1);	// ..with 40% chance to hit on each
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (defender), anyListOf (ExpandedUnitDetails.class), eq ("US001"), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		// Damage type
		final DamageTypeCalculations damageTypeCalculations = mock (DamageTypeCalculations .class);
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		when (damageTypeCalculations.determineSkillDamageType (xuAttacker, "US001", db)).thenReturn (damageType);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setDamageTypeCalculations (damageTypeCalculations);
		
		// Run test
		final AttackDamage dmg = calc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, "US001", players, fow, db);
		
		// Check results
		assertEquals (3, dmg.getPotentialHits ().intValue ());
		assertEquals (4, dmg.getChanceToHit ());
		assertEquals (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, dmg.getDamageResolutionTypeID ());

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		
		assertEquals (DamageCalculationAttackData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationAttackData data = (DamageCalculationAttackData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_DATA, data.getMessageType ());
	    assertEquals (attacker.getUnitURN (), data.getAttackerUnitURN ().intValue ());
	    assertEquals (attackingPD.getPlayerID ().intValue (), data.getAttackerPlayerID ());
	    assertEquals ("US001", data.getAttackSkillID ());
	    assertNull (data.getAttackSpellID ());
	    assertEquals (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, data.getDamageResolutionTypeID ());
	    assertNull (data.getAttackerFigures ());
	    assertNull (data.getAttackStrength ());
	    assertEquals (3, data.getPotentialHits ().intValue ());
	}

	/**
	 * Tests the attackFromUnitSkill method when the attacking unit doesn't have the requested skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromUnitSkill_DontHaveSkill () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Set up units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (22);

		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (23);
		
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);

		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Expanded details
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (attacker), anyListOf (ExpandedUnitDetails.class), eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);

		when (xuAttacker.calculateAliveFigureCount ()).thenReturn (6);		// Attacker has some figures
		when (xuAttacker.hasModifiedSkill ("US001")).thenReturn (false);	// But doesn't have the skill
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (defender), anyListOf (ExpandedUnitDetails.class), eq ("US001"), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final AttackDamage dmg = calc.attackFromUnitSkill (attackerWrapper, defenderWrapper, null, null, "US001", players, fow, db);
		
		// Check results
		assertNull (dmg);
	}

	/**
	 * Tests the attackFromUnitSkill method when every figure of the attacking unit is frozen in fear
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromUnitSkill_FrozenInFear () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// Set up other lists
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Set up units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (22);

		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (23);

		// Wrappers
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		attackerWrapper.setFiguresFrozenInFear (6);
		
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		// Expanded details
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (attacker), anyListOf (ExpandedUnitDetails.class), eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);

		when (xuAttacker.calculateAliveFigureCount ()).thenReturn (6);		// Attacker has some figures
		when (xuAttacker.hasModifiedSkill ("US001")).thenReturn (true);
		when (xuAttacker.getModifiedSkillValue ("US001")).thenReturn (1);	// We have the skill, but can't use since all 6 figures are frozen in fear
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (defender), anyListOf (ExpandedUnitDetails.class), eq ("US001"), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final AttackDamage dmg = calc.attackFromUnitSkill (attackerWrapper, defenderWrapper, null, null, "US001", players, fow, db);
		
		// Check results
		assertNull (dmg);
	}

	/**
	 * Tests the attackFromUnitSkill method when the damage resolution type is modified because we have the Illusionary Attack skill
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromUnitSkill_IllusionaryAttack () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
		// The kind of damage inflicted by this skill
		final UnitSkillSvr unitSkill = new UnitSkillSvr ();
		unitSkill.setDamageResolutionTypeID (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE);
		unitSkill.setDamagePerFigure (DamagePerFigureID.PER_FIGURE_COMBINED);
		when (db.findUnitSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, "attackFromUnitSkill")).thenReturn (unitSkill);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
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

		// Set up units
		final MemoryUnit attacker = new MemoryUnit ();
		attacker.setUnitURN (22);
		attacker.setOwningPlayerID (attackingPD.getPlayerID ());
		
		final MemoryUnit defender = new MemoryUnit ();
		defender.setUnitURN (23);

		// 2 of the attacker figures are frozen in fear so cannot attack
		final AttackResolutionUnit attackerWrapper = new AttackResolutionUnit (attacker);
		attackerWrapper.setFiguresFrozenInFear (2);

		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		defenders.add (defender);
		
		// Expanded details
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final ExpandedUnitDetails xuAttacker = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (attacker), anyListOf (ExpandedUnitDetails.class), eq (null), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuAttacker);

		when (xuAttacker.calculateAliveFigureCount ()).thenReturn (6);		// Attacker has 6 figures...
		when (xuAttacker.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (true);
		when (xuAttacker.hasModifiedSkill (ServerDatabaseValues.UNIT_SKILL_ID_ILLUSIONARY_ATTACK)).thenReturn (true);
		when (xuAttacker.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)).thenReturn (3);	// ..and strength 3 attack per figure, so 18 hits...
		when (xuAttacker.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_HIT)).thenReturn (1);	// ..with 40% chance to hit on each
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (unitUtils.expandUnitDetails (eq (defender), anyListOf (ExpandedUnitDetails.class),
			eq (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK), eq (null), eq (players), eq (fow), eq (db))).thenReturn (xuDefender);
		
		// Damage type
		final DamageTypeCalculations damageTypeCalculations = mock (DamageTypeCalculations .class);
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		when (damageTypeCalculations.determineSkillDamageType (xuAttacker,
			CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, db)).thenReturn (damageType);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitUtils (unitUtils);
		calc.setDamageTypeCalculations (damageTypeCalculations);
		
		// Run test
		final AttackDamage dmg = calc.attackFromUnitSkill (attackerWrapper, defenderWrapper, attackingPlayer, defendingPlayer, CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, players, fow, db);
		
		// Check results
		assertEquals (12, dmg.getPotentialHits ().intValue ());
		assertEquals (4, dmg.getChanceToHit ());
		assertEquals (DamageResolutionTypeID.ILLUSIONARY, dmg.getDamageResolutionTypeID ());

		// Check the message that got sent to the attacker
		assertEquals (1, attackingConn.getMessages ().size ());
		assertEquals (DamageCalculationMessage.class.getName (), attackingConn.getMessages ().get (0).getClass ().getName ());
		final DamageCalculationMessage msg = (DamageCalculationMessage) attackingConn.getMessages ().get (0);
		
		assertEquals (DamageCalculationAttackData.class.getName (), msg.getBreakdown ().getClass ().getName ());
		final DamageCalculationAttackData data = (DamageCalculationAttackData) msg.getBreakdown ();
		
		assertEquals (DamageCalculationMessageTypeID.ATTACK_DATA, data.getMessageType ());
	    assertEquals (attacker.getUnitURN (), data.getAttackerUnitURN ().intValue ());
	    assertEquals (attackingPD.getPlayerID ().intValue (), data.getAttackerPlayerID ());
	    assertEquals (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK, data.getAttackSkillID ());
	    assertNull (data.getAttackSpellID ());
	    assertEquals (DamageResolutionTypeID.ILLUSIONARY, data.getDamageResolutionTypeID ());
	    assertEquals (4, data.getAttackerFigures ().intValue ());
	    assertEquals (3, data.getAttackStrength ().intValue ());
	    assertEquals (12, data.getPotentialHits ().intValue ());
	}
	
	/**
	 * Tests the attackFromSpell method on a spell with fixed damage
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testAttackFromSpell_FixedDamage () throws Exception
	{
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
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
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setStoredDamageTypeID (StoredDamageTypeID.HEALABLE);
		when (db.findDamageType ("DT01", "attackFromSpell")).thenReturn (damageType);

		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		spell.setCombatBaseDamage (12);
		spell.setAttackSpellDamageTypeID ("DT01");
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.DOOM);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		
		// Run test
		final PlayerServerDetails castingPlayer = attackingPlayer;
		final AttackDamage dmg = calc.attackFromSpell (spell, null, castingPlayer, attackingPlayer, defendingPlayer, db);
		
		// Check results
		assertEquals (12, dmg.getPotentialHits ().intValue ());
		assertEquals (3, dmg.getChanceToHit ());
		assertEquals (DamageResolutionTypeID.DOOM, dmg.getDamageResolutionTypeID ());

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
	    assertEquals (StoredDamageTypeID.HEALABLE, data.getStoredDamageTypeID ());
	    assertEquals ("DT01", data.getDamageTypeID ());
	    assertEquals (DamageResolutionTypeID.DOOM, data.getDamageResolutionTypeID ());
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
		// Mock database
		final ServerDatabaseEx db = mock (ServerDatabaseEx.class);
		
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
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		damageType.setStoredDamageTypeID (StoredDamageTypeID.HEALABLE);
		when (db.findDamageType ("DT01", "attackFromSpell")).thenReturn (damageType);
		
		final SpellSvr spell = new SpellSvr ();
		spell.setSpellID ("SP001");
		spell.setCombatBaseDamage (12);
		spell.setAttackSpellDamageTypeID ("DT01");
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.DOOM);
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		
		// Run test
		final PlayerServerDetails castingPlayer = attackingPlayer;
		final AttackDamage dmg = calc.attackFromSpell (spell, 20, castingPlayer, attackingPlayer, defendingPlayer, db);
		
		// Check results
		assertEquals (20, dmg.getPotentialHits ().intValue ());
		assertEquals (3, dmg.getChanceToHit ());
		assertEquals (DamageResolutionTypeID.DOOM, dmg.getDamageResolutionTypeID ());

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
	    assertEquals (StoredDamageTypeID.HEALABLE, data.getStoredDamageTypeID ());
	    assertEquals ("DT01", data.getDamageTypeID ());
	    assertEquals (DamageResolutionTypeID.DOOM, data.getDamageResolutionTypeID ());
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up attack damage
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		final AttackDamage attackDamage = new AttackDamage (18, 1, damageType, DamageResolutionTypeID.SINGLE_FIGURE, null, null, null, 1);
		
		// Set up defender stats
		final DamageTypeCalculations damageTypeCalculations = mock (DamageTypeCalculations .class);
		
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		
		when (xuDefender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (true);
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (4);	// ..and 4 shields...
		when (damageTypeCalculations.getDefenderDefenceStrength (xuDefender, attackDamage, 1)).thenReturn (4);
		
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK)).thenReturn (2);	// ..with 50% chance to block on each

		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8);		// Attack rolls, 6 of them are <4

		// Mock the damage being applied
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, 6, 4, 5)).thenReturn (3);		// Take 6 hits, each figure has defence 4, with 50% block chance
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		calc.setDamageTypeCalculations (damageTypeCalculations);
		
		// Run test
		assertEquals (3, calc.calculateSingleFigureDamage (xuDefender, attackingPlayer, defendingPlayer, attackDamage));
		
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
		assertEquals (DamageResolutionTypeID.SINGLE_FIGURE, data.getDamageResolutionTypeID ());
		
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up attack damage
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		final AttackDamage attackDamage = new AttackDamage (18, 1, damageType, DamageResolutionTypeID.ARMOUR_PIERCING, null, null, null, 1);
		
		// Set up defender stats
		final DamageTypeCalculations damageTypeCalculations = mock (DamageTypeCalculations .class);
		
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...

		when (xuDefender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (true);
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (4);	// ..and 4 shields...
		when (damageTypeCalculations.getDefenderDefenceStrength (xuDefender, attackDamage, 2)).thenReturn (2);		// halved by Armour Piercing

		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK)).thenReturn (2);	// ..with 50% chance to block on each

		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8);		// Attack rolls, 6 of them are <4

		// Mock the damage being applied (NB. now 2 instead of 4 defence)
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, 6, 2, 5)).thenReturn (3);		// Take 6 hits, each figure has defence 2, with 50% block chance
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		calc.setDamageTypeCalculations (damageTypeCalculations);
		
		// Run test
		assertEquals (3, calc.calculateArmourPiercingDamage (xuDefender, attackingPlayer, defendingPlayer, attackDamage));
		
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
		assertEquals (DamageResolutionTypeID.ARMOUR_PIERCING, data.getDamageResolutionTypeID ());
		
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		
		when (xuDefender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (true);
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (4);	// ..and 4 shields...

		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK)).thenReturn (2);	// ..with 50% chance to block on each

		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 2, 6, 7, 3, 6, 7, 4, 2, 5, 7, 9, 2, 4, 3, 6, 6, 8);		// Attack rolls, 6 of them are <4

		// Mock the damage being applied (NB. now 0 instead of 4 defence)
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, 6, 0, 5)).thenReturn (6);		// Take 6 hits, each figure has defence 0, with 50% block chance
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		
		// Run test
		assertEquals (6, calc.calculateIllusionaryDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (18, 1, damageType, DamageResolutionTypeID.ILLUSIONARY, null, null, null, 1)));
		
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
		assertEquals (DamageResolutionTypeID.ILLUSIONARY, data.getDamageResolutionTypeID ());
		
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up attack damage
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		final AttackDamage attackDamage = new AttackDamage (4, 1, damageType, DamageResolutionTypeID.MULTI_FIGURE, null, null, null, 1);
		
		// Set up defender stats
		final UnitUtils unitUtils = mock (UnitUtils.class);
		final DamageTypeCalculations damageTypeCalculations = mock (DamageTypeCalculations .class);
		
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		when (xuDefender.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (true);
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_DEFENCE)).thenReturn (4);	// ..and 4 shields...
		when (damageTypeCalculations.getDefenderDefenceStrength (xuDefender, attackDamage, 1)).thenReturn (4);
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_PLUS_TO_BLOCK)).thenReturn (2);	// ..with 50% chance to block on each
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xuDefender.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
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
		calc.setRandomUtils (random);
		calc.setUnitUtils (unitUtils);
		calc.setDamageTypeCalculations (damageTypeCalculations);
		
		// Run test
		assertEquals (5, calc.calculateMultiFigureDamage (xuDefender, attackingPlayer, defendingPlayer, attackDamage));

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
		assertEquals (DamageResolutionTypeID.MULTI_FIGURE, data.getDamageResolutionTypeID ());
		
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);

		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...

		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, 6, 0, 0)).thenReturn (6);				// Automatically takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (6, calc.calculateDoomDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (6, 1, damageType, DamageResolutionTypeID.DOOM, null, null, null, 1)));

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
		assertEquals (DamageResolutionTypeID.DOOM, data.getDamageResolutionTypeID ());
		
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...

		// Fix random number generator roll
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (100)).thenReturn (20);		// Dies
		
		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, Integer.MAX_VALUE, 0, 0)).thenReturn (6);		// Takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (6, calc.calculateChanceOfDeathDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (25, 0, damageType, DamageResolutionTypeID.CHANCE_OF_DEATH, null, null, null, 1)));

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
		assertEquals (DamageResolutionTypeID.CHANCE_OF_DEATH, data.getDamageResolutionTypeID ());
		
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...

		// Fix random number generator roll
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (100)).thenReturn (80);		// Lives
		
		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, Integer.MAX_VALUE, 0, 0)).thenReturn (6);		// Takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (0, calc.calculateChanceOfDeathDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (25, 0, damageType, DamageResolutionTypeID.CHANCE_OF_DEATH, null, null, null, 1)));

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
		assertEquals (DamageResolutionTypeID.CHANCE_OF_DEATH, data.getDamageResolutionTypeID ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertNull (data.getUnmodifiedDefenceStrength ());
		assertNull (data.getModifiedDefenceStrength ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (0, data.getFinalHits ());			// Takes no damage
	}
	
	/**
	 * Tests the calculateEachFigureResistOrDieDamage method with no saving throw modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateEachFigureResistOrDieDamage_NoSavingThrowModifier () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xuDefender.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (5, 4, 3, 2, 1);		// So first two die, other 3 save
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		assertEquals (5, calc.calculateEachFigureResistOrDieDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (null, 0, damageType, DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE, null, null, null, 1)));

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
		assertEquals (DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE, data.getDamageResolutionTypeID ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (5, data.getFinalHits ());													// 2+3 for the 2 dead figures (one was already hurt)
	}

	/**
	 * Tests the calculateEachFigureResistOrDieDamage method with a -2 saving throw modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateEachFigureResistOrDieDamage_WithSavingThrowModifier () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xuDefender.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (5, 4, 3, 2, 1);		// So first four die, last one saves
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		assertEquals (11, calc.calculateEachFigureResistOrDieDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (2, 0, damageType, DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE, null, null, null, 1)));

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
		assertEquals (DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE, data.getDamageResolutionTypeID ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (2, data.getModifiedDefenceStrength ().intValue ());			// Effective resistance of 2
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (11, data.getFinalHits ());													// 2+3+3+3 for the 4 dead figures (one was already hurt)
	}
	
	/**
	 * Tests the calculateSingleFigureResistOrDieDamage method when the figure survives
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateSingleFigureResistOrDieDamage_Lives () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xuDefender.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (3);		// So it lives

		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		assertEquals (0, calc.calculateSingleFigureResistOrDieDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (null, 0, damageType, DamageResolutionTypeID.SINGLE_FIGURE_RESIST_OR_DIE, null, null, null, 1)));

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
		assertEquals (0, data.getActualHits ().intValue ());									// 0 figures died
		assertEquals (DamageResolutionTypeID.SINGLE_FIGURE_RESIST_OR_DIE, data.getDamageResolutionTypeID ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (0, data.getFinalHits ());													// 0 damage dealt
	}

	/**
	 * Tests the calculateSingleFigureResistOrDieDamage method when the figure dies
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateSingleFigureResistOrDieDamage_Dies () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xuDefender.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (4);		// So it dies

		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitUtils (unitUtils);
		
		// Run test
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		assertEquals (3, calc.calculateSingleFigureResistOrDieDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (null, 0, damageType, DamageResolutionTypeID.SINGLE_FIGURE_RESIST_OR_DIE, null, null, null, 1)));

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
		assertEquals (1, data.getActualHits ().intValue ());									// 0 figures died
		assertEquals (DamageResolutionTypeID.SINGLE_FIGURE_RESIST_OR_DIE, data.getDamageResolutionTypeID ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (3, data.getFinalHits ());													// 3 damage dealt
	}

	/**
	 * Tests the calculateResistOrTakeDamage method with no saving throw modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateResistOrTakeDamage_NoSavingThrowModifier () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
		when (xuDefender.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (5);		// So takes 6 - 4 = 2 damage
		
		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, 2, 0, 0)).thenReturn (2);		// Take 2 hits, with no defence and no blocks
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		
		// Run test
		assertEquals (2, calc.calculateResistOrTakeDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (null, 0, damageType, DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, null, null, null, 1)));

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
		assertEquals (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, data.getDamageResolutionTypeID ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (2, data.getFinalHits ());
	}

	/**
	 * Tests the calculateResistOrTakeDamage method with a -3 saving throw modifier
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateResistOrTakeDamage_WithSavingThrowModifier () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance...

		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS)).thenReturn (3);	// Each defending figure normally has 3 hearts...
				
		when (xuDefender.calculateHitPointsRemainingOfFirstFigure ()).thenReturn (2);	// ...but 1st one is already hurt and only has 2
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (5);		// So takes 6 - 4 + 3 = 5 damage
		
		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, 5, 0, 0)).thenReturn (5);		// Take 5 hits, with no defence and no blocks
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		
		// Run test
		assertEquals (5, calc.calculateResistOrTakeDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (3, 0, damageType, DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, null, null, null, 1)));

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
		assertEquals (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE, data.getDamageResolutionTypeID ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (1, data.getModifiedDefenceStrength ().intValue ());		// 4 with the -3 modifier
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (5, data.getFinalHits ());
	}
	
	/**
	 * Tests the calculateResistanceRollsDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateResistanceRollsDamage () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 6 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance
		
		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 1, 2, 3, 4, 5);		// so 2 die
		
		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, 2, 0, 0)).thenReturn (2);		// Take 5 hits, with no defence and no blocks
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
		calc.setUnitServerUtils (unitServerUtils);
		
		// Run test
		assertEquals (2, calc.calculateResistanceRollsDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (6, 0, damageType, DamageResolutionTypeID.RESISTANCE_ROLLS, null, null, null, 1)));

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
		assertEquals (2, data.getActualHits ().intValue ());
		assertEquals (DamageResolutionTypeID.RESISTANCE_ROLLS, data.getDamageResolutionTypeID ());
		
		assertEquals (5, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (2, data.getFinalHits ());
	}
	
	/**
	 * Tests the calculateDisintegrateDamage method when the unit has low resistance so dies
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDisintegrateDamage_Dies () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (4);	// ..and 4 resistance...
		
		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, Integer.MAX_VALUE, 0, 0)).thenReturn (6);		// Takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (6, calc.calculateDisintegrateDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (null, 0, damageType, DamageResolutionTypeID.DISINTEGRATE, null, null, null, 1)));

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
		assertNull (data.getActualHits ());
		assertEquals (DamageResolutionTypeID.DISINTEGRATE, data.getDamageResolutionTypeID ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertEquals (4, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (6, data.getFinalHits ());			// Only the actual number of HP the unit actually had is recorded
	}

	/**
	 * Tests the calculateDisintegrateDamage method when the unit has high resistance so takes no damage
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateDisintegrateDamage_Lives () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (3);		// Defender has 4 figures unit but 1's dead already...
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (12);	// ..and 12 resistance...
		
		// Mock the damage being applied
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.applyDamage (xuDefender, Integer.MAX_VALUE, 0, 0)).thenReturn (6);		// Takes full dmg of 6 hits
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setUnitServerUtils (unitServerUtils);
	
		// Run test
		assertEquals (0, calc.calculateDisintegrateDamage (xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (2, 0, damageType, DamageResolutionTypeID.DISINTEGRATE, null, null, null, 1)));

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
		assertNull (data.getActualHits ());
		assertEquals (DamageResolutionTypeID.DISINTEGRATE, data.getDamageResolutionTypeID ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertEquals (12, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (10, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (0, data.getFinalHits ());			// Takes no damage
	}
	
	/**
	 * Tests the calculateFearDamage method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateFearDamage () throws Exception
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
		
		// Set up unit
		final MemoryUnit defender = new MemoryUnit ();
		
		final ExpandedUnitDetails xuDefender = mock (ExpandedUnitDetails.class);
		when (xuDefender.getMemoryUnit ()).thenReturn (defender);
		when (xuDefender.getUnitURN ()).thenReturn (33);

		// Wrapper
		final AttackResolutionUnit defenderWrapper = new AttackResolutionUnit (defender);
		defenderWrapper.setFiguresFrozenInFear (2);
		
		// Set up defender stats
		when (xuDefender.calculateAliveFigureCount ()).thenReturn (5);		// Defender has 5 figures unit but 1's dead already + 2 frozen
		when (xuDefender.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (6);	// ..and 6 resistance but -2 modifier

		// Fix random number generator rolls
		final RandomUtils random = mock (RandomUtils.class);
		when (random.nextInt (10)).thenReturn (0, 4, 5);		// so 2 get frozen
		
		// Set up object to test
		final DamageCalculatorImpl calc = new DamageCalculatorImpl ();
		calc.setRandomUtils (random);
	
		// Run test
		final DamageTypeSvr damageType = new DamageTypeSvr ();
		
		calc.calculateFearDamage (defenderWrapper, xuDefender, attackingPlayer, defendingPlayer,
			new AttackDamage (2, 0, damageType, DamageResolutionTypeID.FEAR, null, null, null, 1));
		
		// Check value recorded on server
		assertEquals (4, defenderWrapper.getFiguresFrozenInFear ());

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
		assertEquals (2, data.getActualHits ().intValue ());
		assertEquals (DamageResolutionTypeID.FEAR, data.getDamageResolutionTypeID ());
		
		assertEquals (3, data.getDefenderFigures ());
		assertEquals (6, data.getUnmodifiedDefenceStrength ().intValue ());
		assertEquals (4, data.getModifiedDefenceStrength ().intValue ());
		assertNull (data.getChanceToDefend ());
		assertNull (data.getTenTimesAverageBlock ());
		assertEquals (2, data.getFinalHits ());			// 2 additional figures get frozen
	}
}
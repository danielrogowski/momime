package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.calculations.MomUnitCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.OverlandMapCoordinatesEx;
import momime.common.messages.v0_9_4.MemoryCombatAreaEffect;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.common.utils.UnitUtilsImpl;
import momime.server.ServerTestData;
import momime.server.database.ServerDatabaseEx;

import org.junit.Test;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

/**
 * Tests the CombatAIImpl class
 */
public final class TestCombatAIImpl
{
	/**
	 * Tests the listUnitsToMove method
	 */
	@Test
	public final void testListUnitsToMove ()
	{
		// Combat location
		final OverlandMapCoordinatesEx combatLocation = createCoordinates (20);
		
		// Test unit list
		final List<MemoryUnit> trueUnits = new ArrayList<MemoryUnit> ();
		
		final MemoryUnit notInCombat = new MemoryUnit ();
		notInCombat.setOwningPlayerID (1);
		notInCombat.setStatus (UnitStatusID.ALIVE);
		trueUnits.add (notInCombat);
		
		final MemoryUnit inDifferentCombat = new MemoryUnit ();
		inDifferentCombat.setCombatLocation (createCoordinates (21));
		inDifferentCombat.setOwningPlayerID (1);
		inDifferentCombat.setDoubleCombatMovesLeft (2);
		inDifferentCombat.setStatus (UnitStatusID.ALIVE);
		trueUnits.add (inDifferentCombat);
		
		final MemoryUnit noMovesLeft = new MemoryUnit ();
		noMovesLeft.setCombatLocation (createCoordinates (20));
		noMovesLeft.setOwningPlayerID (1);
		noMovesLeft.setDoubleCombatMovesLeft (0);
		noMovesLeft.setStatus (UnitStatusID.ALIVE);
		trueUnits.add (noMovesLeft);
		
		final MemoryUnit deadUnit = new MemoryUnit ();
		deadUnit.setCombatLocation (createCoordinates (20));
		deadUnit.setOwningPlayerID (1);
		deadUnit.setDoubleCombatMovesLeft (2);
		deadUnit.setStatus (UnitStatusID.DEAD);
		trueUnits.add (deadUnit);

		final MemoryUnit correctUnit = new MemoryUnit ();
		correctUnit.setCombatLocation (createCoordinates (20));
		correctUnit.setOwningPlayerID (1);
		correctUnit.setDoubleCombatMovesLeft (2);
		correctUnit.setStatus (UnitStatusID.ALIVE);
		trueUnits.add (correctUnit);
		
		final MemoryUnit someoneElsesUnit = new MemoryUnit ();
		someoneElsesUnit.setCombatLocation (createCoordinates (20));
		someoneElsesUnit.setOwningPlayerID (2);
		someoneElsesUnit.setDoubleCombatMovesLeft (2);
		someoneElsesUnit.setStatus (UnitStatusID.ALIVE);
		trueUnits.add (someoneElsesUnit);

		// Set up object to test
		final CombatAIImpl ai = new CombatAIImpl ();
		
		// Run method
		final List<MemoryUnit> unitsToMove = ai.listUnitsToMove (combatLocation, 1, trueUnits);
		
		// Check results
		assertEquals (1, unitsToMove.size ());
		assertSame (correctUnit, unitsToMove.get (0));
	}
	
	/**
	 * Tests the calculateUnitCombatAIOrder method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCalculateUnitCombatAIOrder () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up object to test
		final MomUnitCalculations unitCalculations = mock (MomUnitCalculations.class);
		
		final CombatAIImpl ai = new CombatAIImpl ();
		ai.setUnitUtils (new UnitUtilsImpl ());
		ai.setUnitCalculations (unitCalculations);
		
		// Caster with MP remaining
		final MemoryUnit casterWithMP = new MemoryUnit ();
		casterWithMP.setManaRemaining (10);
		assertEquals (1, ai.calculateUnitCombatAIOrder (casterWithMP, players, spells, combatAreaEffects, db));
		
		// Unit with a ranged attack
		final MemoryUnit ranged = new MemoryUnit ();
		ranged.setManaRemaining (9);
		when (unitCalculations.canMakeRangedAttack (ranged, players, spells, combatAreaEffects, db)).thenReturn (true);
		assertEquals (2, ai.calculateUnitCombatAIOrder (ranged, players, spells, combatAreaEffects, db));
		
		// Unit without the caster skill
		final MemoryUnit melee = new MemoryUnit ();
		melee.setUnitID ("UN001");		// Dwarf hero
		assertEquals (3, ai.calculateUnitCombatAIOrder (melee, players, spells, combatAreaEffects, db));
		
		// Caster without MP remaining
		final MemoryUnit casterWithoutMP = new MemoryUnit ();
		casterWithoutMP.setUnitID ("UN003");		// Sage hero
		casterWithoutMP.setManaRemaining (9);
		
		final UnitHasSkill casterSkill = new UnitHasSkill ();
		casterSkill.setUnitSkillID (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_CASTER_HERO);
		casterSkill.setUnitSkillValue (3);
		casterWithoutMP.getUnitHasSkill ().add (casterSkill);
		
		assertEquals (4, ai.calculateUnitCombatAIOrder (casterWithoutMP, players, spells, combatAreaEffects, db));
	}
	
	/**
	 * Tests the evaluateTarget method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEvaluateTarget () throws Exception
	{
		final ServerDatabaseEx db = ServerTestData.loadServerDatabase ();

		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		
		// Set up object to test
		final MomUnitCalculations unitCalculations = mock (MomUnitCalculations.class);
		
		final CombatAIImpl ai = new CombatAIImpl ();
		ai.setUnitUtils (new UnitUtilsImpl ());
		ai.setUnitCalculations (unitCalculations);
		
		// Attacking unit
		final MemoryUnit attacker = new MemoryUnit ();
		
		// Caster with MP remaining
		final MemoryUnit casterWithMP = new MemoryUnit ();
		casterWithMP.setManaRemaining (10);
		assertEquals (3, ai.evaluateTarget (attacker, casterWithMP, players, spells, combatAreaEffects, db));
		
		// Unit with a ranged attack
		final MemoryUnit ranged = new MemoryUnit ();
		ranged.setManaRemaining (9);
		when (unitCalculations.canMakeRangedAttack (ranged, players, spells, combatAreaEffects, db)).thenReturn (true);
		assertEquals (2, ai.evaluateTarget (attacker, ranged, players, spells, combatAreaEffects, db));
		
		// Unit without the caster skill
		final MemoryUnit melee = new MemoryUnit ();
		melee.setUnitID ("UN001");		// Dwarf hero
		assertEquals (1, ai.evaluateTarget (attacker, melee, players, spells, combatAreaEffects, db));
		
		// Caster without MP remaining
		final MemoryUnit casterWithoutMP = new MemoryUnit ();
		casterWithoutMP.setUnitID ("UN003");		// Sage hero
		casterWithoutMP.setManaRemaining (9);
		
		final UnitHasSkill casterSkill = new UnitHasSkill ();
		casterSkill.setUnitSkillID (CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_CASTER_HERO);
		casterSkill.setUnitSkillValue (3);
		casterWithoutMP.getUnitHasSkill ().add (casterSkill);
		
		assertEquals (1, ai.evaluateTarget (attacker, casterWithoutMP, players, spells, combatAreaEffects, db));
	}

	/**
	 * Just to save repeating this a dozen times in the test cases
	 * @param x X coord
	 * @return Coordinates object
	 */
	private final OverlandMapCoordinatesEx createCoordinates (final int x)
	{
		final OverlandMapCoordinatesEx combatLocation = new OverlandMapCoordinatesEx ();
		combatLocation.setX (x);
		combatLocation.setY (10);
		combatLocation.setPlane (1);
		return combatLocation;
	}
}

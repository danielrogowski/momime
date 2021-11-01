package momime.server.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.UnitCombatSideID;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;

/**
 * Tests the CombatAIImpl class
 */
public final class TestCombatAIImpl
{
	/**
	 * Tests the listUnitsToMove method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListUnitsToMove () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Combat location
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);

		// Players
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();

		// Test unit list
		final FogOfWarMemory mem = new FogOfWarMemory ();
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final MemoryUnit notInCombat = new MemoryUnit ();
		notInCombat.setOwningPlayerID (1);
		notInCombat.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (notInCombat);

		final ExpandedUnitDetails xuNotInCombat = mock (ExpandedUnitDetails.class);
		when (xuNotInCombat.getControllingPlayerID ()).thenReturn (1);
		when (expand.expandUnitDetails (notInCombat, null, null, null, players, mem, db)).thenReturn (xuNotInCombat);
		
		final MemoryUnit inDifferentCombat = new MemoryUnit ();
		inDifferentCombat.setCombatLocation (new MapCoordinates3DEx (21, 10, 1));
		inDifferentCombat.setCombatPosition (new MapCoordinates2DEx (5, 6));
		inDifferentCombat.setCombatSide (UnitCombatSideID.ATTACKER);
		inDifferentCombat.setCombatHeading (1);
		inDifferentCombat.setOwningPlayerID (1);
		inDifferentCombat.setDoubleCombatMovesLeft (2);
		inDifferentCombat.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (inDifferentCombat);

		final ExpandedUnitDetails xuInDifferentCombat = mock (ExpandedUnitDetails.class);
		when (xuInDifferentCombat.getControllingPlayerID ()).thenReturn (1);
		when (expand.expandUnitDetails (inDifferentCombat, null, null, null, players, mem, db)).thenReturn (xuInDifferentCombat);
		
		final MemoryUnit noMovesLeft = new MemoryUnit ();
		noMovesLeft.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		noMovesLeft.setCombatPosition (new MapCoordinates2DEx (5, 6));
		noMovesLeft.setCombatSide (UnitCombatSideID.ATTACKER);
		noMovesLeft.setCombatHeading (1);
		noMovesLeft.setOwningPlayerID (1);
		noMovesLeft.setDoubleCombatMovesLeft (0);
		noMovesLeft.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (noMovesLeft);

		final ExpandedUnitDetails xuNoMovesLeft = mock (ExpandedUnitDetails.class);
		when (xuNoMovesLeft.getControllingPlayerID ()).thenReturn (1);
		when (expand.expandUnitDetails (noMovesLeft, null, null, null, players, mem, db)).thenReturn (xuNoMovesLeft);
		
		final MemoryUnit deadUnit = new MemoryUnit ();
		deadUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		deadUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		deadUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		deadUnit.setCombatHeading (1);
		deadUnit.setOwningPlayerID (1);
		deadUnit.setDoubleCombatMovesLeft (2);
		deadUnit.setStatus (UnitStatusID.DEAD);
		mem.getUnit ().add (deadUnit);

		final ExpandedUnitDetails xuDeadUnit = mock (ExpandedUnitDetails.class);
		when (xuDeadUnit.getControllingPlayerID ()).thenReturn (1);
		when (expand.expandUnitDetails (deadUnit, null, null, null, players, mem, db)).thenReturn (xuDeadUnit);
		
		final MemoryUnit landUnitInNavalCombat = new MemoryUnit ();
		landUnitInNavalCombat.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		landUnitInNavalCombat.setCombatSide (UnitCombatSideID.ATTACKER);
		landUnitInNavalCombat.setOwningPlayerID (1);
		landUnitInNavalCombat.setDoubleCombatMovesLeft (2);
		landUnitInNavalCombat.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (landUnitInNavalCombat);

		final ExpandedUnitDetails xuLandUnitInNavalCombat = mock (ExpandedUnitDetails.class);
		when (xuLandUnitInNavalCombat.getControllingPlayerID ()).thenReturn (1);
		when (expand.expandUnitDetails (landUnitInNavalCombat, null, null, null, players, mem, db)).thenReturn (xuLandUnitInNavalCombat);
		
		final MemoryUnit correctUnit = new MemoryUnit ();
		correctUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		correctUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		correctUnit.setCombatSide (UnitCombatSideID.ATTACKER);
		correctUnit.setCombatHeading (1);
		correctUnit.setOwningPlayerID (1);
		correctUnit.setDoubleCombatMovesLeft (2);
		correctUnit.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (correctUnit);
		
		final ExpandedUnitDetails xuCorrectUnit = mock (ExpandedUnitDetails.class);
		when (xuCorrectUnit.getControllingPlayerID ()).thenReturn (1);
		when (expand.expandUnitDetails (correctUnit, null, null, null, players, mem, db)).thenReturn (xuCorrectUnit);
		
		final MemoryUnit someoneElsesUnit = new MemoryUnit ();
		someoneElsesUnit.setCombatLocation (new MapCoordinates3DEx (20, 10, 1));
		someoneElsesUnit.setCombatPosition (new MapCoordinates2DEx (5, 6));
		someoneElsesUnit.setCombatSide (UnitCombatSideID.DEFENDER);
		someoneElsesUnit.setCombatHeading (1);
		someoneElsesUnit.setOwningPlayerID (2);
		someoneElsesUnit.setDoubleCombatMovesLeft (2);
		someoneElsesUnit.setStatus (UnitStatusID.ALIVE);
		mem.getUnit ().add (someoneElsesUnit);

		final ExpandedUnitDetails xuSomeoneElsesUnit = mock (ExpandedUnitDetails.class);
		when (xuSomeoneElsesUnit.getControllingPlayerID ()).thenReturn (2);
		when (expand.expandUnitDetails (someoneElsesUnit, null, null, null, players, mem, db)).thenReturn (xuSomeoneElsesUnit);
		
		// Set up object to test
		final CombatAIImpl ai = new CombatAIImpl ();
		ai.setExpandUnitDetails (expand);
		
		// Run method
		final List<MemoryUnit> unitsToMove = ai.listUnitsToMove (combatLocation, 1, players, mem, db);
		
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
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Other lists
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Sample unit
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final MemoryUnit unit = new MemoryUnit ();

		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit, null, null, null, players, fow, db)).thenReturn (xu);
		when (xu.getMemoryUnit ()).thenReturn (unit);
		
		// Set up object to test
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		
		final CombatAIImpl ai = new CombatAIImpl ();
		ai.setExpandUnitDetails (expand);
		ai.setUnitCalculations (unitCalculations);
		
		// Caster with MP remaining
		when (xu.getManaRemaining ()).thenReturn (10);
		assertEquals (1, ai.calculateUnitCombatAIOrder (xu));
		
		// Unit with a ranged attack
		when (xu.getManaRemaining ()).thenReturn (9);
		when (unitCalculations.canMakeRangedAttack (xu)).thenReturn (true);
		assertEquals (2, ai.calculateUnitCombatAIOrder (xu));
		
		// Unit without the caster skill
		when (unitCalculations.canMakeRangedAttack (xu)).thenReturn (false);
		assertEquals (3, ai.calculateUnitCombatAIOrder (xu));
		
		// Caster without MP remaining
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_CASTER_HERO)).thenReturn (true);
		assertEquals (4, ai.calculateUnitCombatAIOrder (xu));
	}
	
	/**
	 * Tests the evaluateTarget method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testEvaluateTarget () throws Exception
	{
		// Set up object to test
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		
		final CombatAIImpl ai = new CombatAIImpl ();
		ai.setExpandUnitDetails (expand);
		ai.setUnitCalculations (unitCalculations);
		
		// Attacking unit
		final ExpandedUnitDetails attacker = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails defender = mock (ExpandedUnitDetails.class);
		
		// Caster with MP remaining
		when (defender.getManaRemaining ()).thenReturn (10);
		assertEquals (3, ai.evaluateTarget (attacker, defender));
		
		// Unit with a ranged attack
		when (defender.getManaRemaining ()).thenReturn (9);
		when (unitCalculations.canMakeRangedAttack (defender)).thenReturn (true);
		assertEquals (2, ai.evaluateTarget (attacker, defender));
		
		// Unit without the caster skill
		when (unitCalculations.canMakeRangedAttack (defender)).thenReturn (false);
		assertEquals (1, ai.evaluateTarget (attacker, defender));
	}
}
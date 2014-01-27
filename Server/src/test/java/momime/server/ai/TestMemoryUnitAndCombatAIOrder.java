package momime.server.ai;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import momime.common.messages.v0_9_4.MemoryUnit;

import org.junit.Test;

/**
 * Tests the MemoryUnitAndCombatAIOrder class
 */
public final class TestMemoryUnitAndCombatAIOrder
{
	/**
	 * Tests the compare method by using it to sort a real list of units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCompare () throws Exception
	{
		// One of each kind of test unit
		final MemoryUnit casterWithMpLeft = new MemoryUnit ();
		final MemoryUnit unitWithRangedAttack = new MemoryUnit ();
		final MemoryUnit unitWithoutCasterSkill = new MemoryUnit ();
		final MemoryUnit casterWithoutMpLeft = new MemoryUnit ();

		// Put units into the list in the wrong order
		final List<MemoryUnitAndCombatAIOrder> units = new ArrayList<MemoryUnitAndCombatAIOrder> ();
		units.add (new MemoryUnitAndCombatAIOrder (unitWithoutCasterSkill, 3));
		units.add (new MemoryUnitAndCombatAIOrder (casterWithoutMpLeft, 4));
		units.add (new MemoryUnitAndCombatAIOrder (unitWithRangedAttack, 2));
		units.add (new MemoryUnitAndCombatAIOrder (casterWithMpLeft, 1));
		
		// Check results
		Collections.sort (units);
		assertSame (casterWithMpLeft, units.get (0).getUnit ());
		assertSame (unitWithRangedAttack, units.get (1).getUnit ());
		assertSame (unitWithoutCasterSkill, units.get (2).getUnit ());
		assertSame (casterWithoutMpLeft, units.get (3).getUnit ());
	}
}

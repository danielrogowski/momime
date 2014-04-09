package momime.server.process;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import momime.common.messages.v0_9_5.MemoryUnit;

import org.junit.Test;

/**
 * Tests the MemoryUnitAndCombatClass class
 */
public final class TestMemoryUnitAndCombatClass
{
	/**
	 * Tests the compare method by using it to sort a real list of units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCompare () throws Exception
	{
		// One of each kind of test unit
		final MemoryUnit dwarfHero = new MemoryUnit ();
		final MemoryUnit spearmen = new MemoryUnit ();
		final MemoryUnit archerHero = new MemoryUnit ();
		final MemoryUnit bowmen = new MemoryUnit ();
		final MemoryUnit settlers = new MemoryUnit ();

		// Put units into the list in the wrong order
		final List<MemoryUnitAndCombatClass> units = new ArrayList<MemoryUnitAndCombatClass> ();
		units.add (new MemoryUnitAndCombatClass (bowmen, 4));
		units.add (new MemoryUnitAndCombatClass (dwarfHero, 1));
		units.add (new MemoryUnitAndCombatClass (settlers, 5));
		units.add (new MemoryUnitAndCombatClass (archerHero, 3));
		units.add (new MemoryUnitAndCombatClass (spearmen, 2));
		
		// Check results
		Collections.sort (units);
		assertSame (dwarfHero, units.get (0).getUnit ());
		assertSame (spearmen, units.get (1).getUnit ());
		assertSame (archerHero, units.get (2).getUnit ());
		assertSame (bowmen, units.get (3).getUnit ());
		assertSame (settlers, units.get (4).getUnit ());
	}
}

package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.utils.ExpandedUnitDetails;

/**
 * Tests the MemoryUnitAndCombatClass class
 */
@ExtendWith(MockitoExtension.class)
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
		final ExpandedUnitDetails dwarfHero = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails spearmen = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails archerHero = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails bowmen = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails settlers = mock (ExpandedUnitDetails.class);

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
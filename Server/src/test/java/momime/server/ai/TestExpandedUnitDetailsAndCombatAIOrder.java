package momime.server.ai;

import static org.mockito.Mockito.mock;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import momime.common.utils.ExpandedUnitDetails;

/**
 * Tests the ExpandedUnitDetailsAndCombatAIOrder class
 */
public final class TestExpandedUnitDetailsAndCombatAIOrder
{
	/**
	 * Tests the compare method by using it to sort a real list of units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCompare () throws Exception
	{
		// One of each kind of test unit
		final ExpandedUnitDetails casterWithMpLeft = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails unitWithRangedAttack = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails unitWithoutCasterSkill = mock (ExpandedUnitDetails.class);
		final ExpandedUnitDetails casterWithoutMpLeft = mock (ExpandedUnitDetails.class);

		// Put units into the list in the wrong order
		final List<ExpandedUnitDetailsAndCombatAIOrder> units = new ArrayList<ExpandedUnitDetailsAndCombatAIOrder> ();
		units.add (new ExpandedUnitDetailsAndCombatAIOrder (unitWithoutCasterSkill, 3));
		units.add (new ExpandedUnitDetailsAndCombatAIOrder (casterWithoutMpLeft, 4));
		units.add (new ExpandedUnitDetailsAndCombatAIOrder (unitWithRangedAttack, 2));
		units.add (new ExpandedUnitDetailsAndCombatAIOrder (casterWithMpLeft, 1));
		
		// Check results
		Collections.sort (units);
		assertSame (casterWithMpLeft, units.get (0).getUnit ());
		assertSame (unitWithRangedAttack, units.get (1).getUnit ());
		assertSame (unitWithoutCasterSkill, units.get (2).getUnit ());
		assertSame (casterWithoutMpLeft, units.get (3).getUnit ());
	}
}
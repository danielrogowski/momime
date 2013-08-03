package momime.common.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MomScheduledCombat;

import org.junit.Test;

/**
 * Tests the ScheduledCombatUtilsImpl class
 */
public final class TestScheduledCombatUtilsImpl
{
	/**
	 * Tests the findScheduledCombatURN method looking for a combat that exists
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	@Test
	public final void testFindScheduledCombatURN_Exists () throws RecordNotFoundException
	{
		// Make test list
		final List<MomScheduledCombat> combats = new ArrayList<MomScheduledCombat> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomScheduledCombat combat = new MomScheduledCombat ();
			combat.setScheduledCombatURN (n);
			combats.add (combat);
		}
		
		// Run test
		assertEquals (2, new ScheduledCombatUtilsImpl ().findScheduledCombatURN (combats, 2).getScheduledCombatURN ());
	}

	/**
	 * Tests the findScheduledCombatURN method looking for a combat that doesn't exists
	 * @throws RecordNotFoundException If the requested combat URN doesn't exist in the list
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindScheduledCombatURN_NotExists () throws RecordNotFoundException
	{
		// Make test list
		final List<MomScheduledCombat> combats = new ArrayList<MomScheduledCombat> ();
		for (int n = 1; n <= 3; n++)
		{
			final MomScheduledCombat combat = new MomScheduledCombat ();
			combat.setScheduledCombatURN (n);
			combats.add (combat);
		}
		
		// Run test
		new ScheduledCombatUtilsImpl ().findScheduledCombatURN (combats, 4);
	}
}

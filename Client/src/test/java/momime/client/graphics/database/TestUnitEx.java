package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the UnitEx class
 */
public final class TestUnitEx
{
	/**
	 * Tests the findCombatAction method to look for an action that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatAction_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitEx unit = new UnitEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatActionEx action = new UnitCombatActionEx ();
			action.setCombatActionID ("A" + n);
			action.setOverrideActionSoundNumber (n);
			
			unit.getUnitCombatAction ().add (action);
		}
		
		unit.buildMap ();
		
		// Run tests
		assertEquals (2, unit.findCombatAction ("A2", "testFindCombatAction_Exists").getOverrideActionSoundNumber ().intValue ());
	}

	/**
	 * Tests the findCombatAction method to look for an action that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatAction_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitEx unit = new UnitEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatActionEx action = new UnitCombatActionEx ();
			action.setCombatActionID ("A" + n);
			action.setOverrideActionSoundNumber (n);
			
			unit.getUnitCombatAction ().add (action);
		}
		
		unit.buildMap ();
		
		// Run tests
		unit.findCombatAction ("A4", "testFindCombatAction_NotExists");
	}
}
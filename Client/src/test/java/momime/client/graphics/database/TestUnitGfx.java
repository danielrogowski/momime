package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the UnitGfx class
 */
public final class TestUnitGfx
{
	/**
	 * Tests the findCombatAction method to look for an action that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatAction_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitGfx unit = new UnitGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatActionGfx action = new UnitCombatActionGfx ();
			action.setCombatActionID ("A" + n);
			action.setOverrideActionSoundFile ("S" + n);
			
			unit.getUnitCombatAction ().add (action);
		}
		
		unit.buildMap ();
		
		// Run tests
		assertEquals ("S2", unit.findCombatAction ("A2", "testFindCombatAction_Exists").getOverrideActionSoundFile ());
	}

	/**
	 * Tests the findCombatAction method to look for an action that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatAction_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitGfx unit = new UnitGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatActionGfx action = new UnitCombatActionGfx ();
			action.setCombatActionID ("A" + n);
			action.setOverrideActionSoundFile ("S" + n);
			
			unit.getUnitCombatAction ().add (action);
		}
		
		unit.buildMap ();
		
		// Run tests
		unit.findCombatAction ("A4", "testFindCombatAction_NotExists");
	}
}
package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the UnitEx class
 */
@ExtendWith(MockitoExtension.class)
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
			action.setOverrideActionSoundFile ("S" + n);
			
			unit.getUnitCombatAction ().add (action);
		}
		
		unit.buildMaps ();
		
		// Run tests
		assertEquals ("S2", unit.findCombatAction ("A2", "testFindCombatAction_Exists").getOverrideActionSoundFile ());
	}

	/**
	 * Tests the findCombatAction method to look for an action that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatAction_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitEx unit = new UnitEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatActionEx action = new UnitCombatActionEx ();
			action.setCombatActionID ("A" + n);
			action.setOverrideActionSoundFile ("S" + n);
			
			unit.getUnitCombatAction ().add (action);
		}
		
		unit.buildMaps ();
		
		// Run tests
		assertThrows (RecordNotFoundException.class, () ->
		{
			unit.findCombatAction ("A4", "testFindCombatAction_NotExists");
		});
	}
}
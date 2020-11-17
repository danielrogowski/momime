package momime.common.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests the UnitCombatActionEx class
 */
public final class TestUnitCombatActionEx
{
	/**
	 * Tests the findDirection method to look for a direction that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindDirection_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitCombatActionEx action = new UnitCombatActionEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatImage image = new UnitCombatImage ();
			image.setDirection (n);
			image.setUnitCombatImageFile ("L" + n + ".png");
			
			action.getUnitCombatImage ().add (image);
		}
		
		action.buildMap ();
		
		// Run tests
		assertEquals ("L2.png", action.findDirection (2, "testFindDirection_Exists").getUnitCombatImageFile ());
	}

	/**
	 * Tests the findDirection method to look for a direction that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindDirection_NotExists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitCombatActionEx action = new UnitCombatActionEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatImage image = new UnitCombatImage ();
			image.setDirection (n);
			image.setUnitCombatImageFile ("L" + n + ".png");
			
			action.getUnitCombatImage ().add (image);
		}
		
		action.buildMap ();
		
		// Run tests
		action.findDirection (4, "testFindDirection_NotExists");
	}
}
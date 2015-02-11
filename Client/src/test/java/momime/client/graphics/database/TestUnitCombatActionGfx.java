package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the UnitCombatActionGfx class
 */
public final class TestUnitCombatActionGfx
{
	/**
	 * Tests the findDirection method to look for a direction that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindDirection_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitCombatActionGfx action = new UnitCombatActionGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatImageGfx image = new UnitCombatImageGfx ();
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
		final UnitCombatActionGfx action = new UnitCombatActionGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitCombatImageGfx image = new UnitCombatImageGfx ();
			image.setDirection (n);
			image.setUnitCombatImageFile ("L" + n + ".png");
			
			action.getUnitCombatImage ().add (image);
		}
		
		action.buildMap ();
		
		// Run tests
		action.findDirection (4, "testFindDirection_NotExists");
	}
}
package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.client.graphics.database.v0_9_5.RangedAttackTypeWeaponGrade;

import org.junit.Test;

/**
 * Tests the RangedAttackTypeEx class
 */
public final class TestRangedAttackTypeEx
{
	/**
	 * Tests the findWeaponGradeImageFile method
	 */
	@Test
	public final void testFindWeaponGradeImageFile ()
	{
		// Create some dummy entries
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeWeaponGrade image = new RangedAttackTypeWeaponGrade ();
			image.setWeaponGradeNumber (n);
			image.setUnitDisplayRangedImageFile ("Blah" + n + ".png");
			
			rat.getRangedAttackTypeWeaponGrade ().add (image);
		}
		
		rat.buildMap ();
		
		// Run tests
		assertEquals ("Blah2.png", rat.findWeaponGradeImageFile (2));
		assertNull (rat.findWeaponGradeImageFile (4));
	}
}
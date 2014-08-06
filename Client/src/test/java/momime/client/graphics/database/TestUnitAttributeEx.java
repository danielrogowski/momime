package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import momime.client.graphics.database.v0_9_5.UnitAttributeWeaponGrade;

import org.junit.Test;

/**
 * Tests the UnitAttributeEx class
 */
public final class TestUnitAttributeEx
{
	/**
	 * Tests the findWeaponGradeImageFile method
	 */
	@Test
	public final void testFindWeaponGradeImageFile ()
	{
		// Create some dummy entries
		final UnitAttributeEx attr = new UnitAttributeEx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitAttributeWeaponGrade image = new UnitAttributeWeaponGrade ();
			image.setWeaponGradeNumber (n);
			image.setAttributeImageFile ("Blah" + n + ".png");
			
			attr.getUnitAttributeWeaponGrade ().add (image);
		}
		
		attr.buildMap ();
		
		// Run tests
		assertEquals ("Blah2.png", attr.findWeaponGradeImageFile (2));
		assertNull (attr.findWeaponGradeImageFile (4));
	}
}
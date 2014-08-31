package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.client.graphics.database.v0_9_5.UnitAttributeWeaponGrade;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the UnitAttributeEx class
 */
public final class TestUnitAttributeEx
{
	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindWeaponGradeImageFile_Exists () throws RecordNotFoundException
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
		assertEquals ("Blah2.png", attr.findWeaponGradeImageFile (2, "testFindWeaponGradeImageFile_Exists"));
	}

	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWeaponGradeImageFile_NotExists () throws RecordNotFoundException
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
		attr.findWeaponGradeImageFile (4, "testFindWeaponGradeImageFile_NotExists");
	}
}
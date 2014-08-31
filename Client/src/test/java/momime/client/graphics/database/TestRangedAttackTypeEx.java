package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.client.graphics.database.v0_9_5.RangedAttackTypeWeaponGrade;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the RangedAttackTypeEx class
 */
public final class TestRangedAttackTypeEx
{
	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindWeaponGradeImageFile_Exist () throws RecordNotFoundException
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
		assertEquals ("Blah2.png", rat.findWeaponGradeImageFile (2, "testFindWeaponGradeImageFile_Exist"));
	}

	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindWeaponGradeImageFile_NotExists () throws RecordNotFoundException
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
		rat.findWeaponGradeImageFile (4, "testFindWeaponGradeImageFile_NotExists");
	}
}
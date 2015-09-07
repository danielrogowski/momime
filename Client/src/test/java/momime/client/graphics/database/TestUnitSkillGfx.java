package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the UnitSkillGfx class
 */
public final class TestUnitSkillGfx
{
	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindWeaponGradeImageFile_Exists () throws RecordNotFoundException
	{
		// Create some dummy entries
		final UnitSkillGfx attr = new UnitSkillGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillWeaponGradeGfx image = new UnitSkillWeaponGradeGfx ();
			image.setWeaponGradeNumber (n);
			image.setSkillImageFile ("Blah" + n + ".png");
			
			attr.getUnitSkillWeaponGrade ().add (image);
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
		final UnitSkillGfx attr = new UnitSkillGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final UnitSkillWeaponGradeGfx image = new UnitSkillWeaponGradeGfx ();
			image.setWeaponGradeNumber (n);
			image.setSkillImageFile ("Blah" + n + ".png");
			
			attr.getUnitSkillWeaponGrade ().add (image);
		}
		
		attr.buildMap ();
		
		// Run tests
		attr.findWeaponGradeImageFile (4, "testFindWeaponGradeImageFile_NotExists");
	}
}
package momime.common.database;

import static org.junit.Assert.assertEquals;
import momime.common.database.RangedAttackTypeActionID;
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
		
		rat.buildMaps ();
		
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
		
		rat.buildMaps ();
		
		// Run tests
		rat.findWeaponGradeImageFile (4, "testFindWeaponGradeImageFile_NotExists");
	}

	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindCombatImage_Exist () throws RecordNotFoundException
	{
		// Create some dummy entries
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeCombatImage flyImage = new RangedAttackTypeCombatImage ();
			flyImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.FLY);
			flyImage.setDirection (n);
			flyImage.setRangedAttackTypeCombatImageFile ("Fly" + n + ".png");

			final RangedAttackTypeCombatImage strikeImage = new RangedAttackTypeCombatImage ();
			strikeImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.STRIKE);
			strikeImage.setDirection (n);
			strikeImage.setRangedAttackTypeCombatImageFile ("Strike" + n + ".png");
			
			rat.getRangedAttackTypeCombatImage ().add (flyImage);
			rat.getRangedAttackTypeCombatImage ().add (strikeImage);
		}
		
		rat.buildMaps ();
		
		// Run tests
		assertEquals ("Strike2.png", rat.findCombatImage (RangedAttackTypeActionID.STRIKE, 2, "testFindCombatImage_Exist").getRangedAttackTypeCombatImageFile ());
	}

	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that doesn't exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testFindCombatImage_NotExist () throws RecordNotFoundException
	{
		// Create some dummy entries
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeCombatImage flyImage = new RangedAttackTypeCombatImage ();
			flyImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.FLY);
			flyImage.setDirection (n);
			flyImage.setRangedAttackTypeCombatImageFile ("Fly" + n + ".png");

			final RangedAttackTypeCombatImage strikeImage = new RangedAttackTypeCombatImage ();
			strikeImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.STRIKE);
			strikeImage.setDirection (n);
			strikeImage.setRangedAttackTypeCombatImageFile ("Strike" + n + ".png");
			
			rat.getRangedAttackTypeCombatImage ().add (flyImage);
			rat.getRangedAttackTypeCombatImage ().add (strikeImage);
		}
		
		rat.buildMaps ();
		
		// Run tests
		rat.findCombatImage (RangedAttackTypeActionID.STRIKE, 4, "testFindCombatImage_NotExist");
	}
}
package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import momime.client.graphics.database.v0_9_5.RangedAttackTypeActionID;
import momime.common.database.RecordNotFoundException;

import org.junit.Test;

/**
 * Tests the RangedAttackTypeGfx class
 */
public final class TestRangedAttackTypeGfx
{
	/**
	 * Tests the findWeaponGradeImageFile method to look for a weapon grade that does exist
	 * @throws RecordNotFoundException If the record is not found
	 */
	@Test
	public final void testFindWeaponGradeImageFile_Exist () throws RecordNotFoundException
	{
		// Create some dummy entries
		final RangedAttackTypeGfx rat = new RangedAttackTypeGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeWeaponGradeGfx image = new RangedAttackTypeWeaponGradeGfx ();
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
		final RangedAttackTypeGfx rat = new RangedAttackTypeGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeWeaponGradeGfx image = new RangedAttackTypeWeaponGradeGfx ();
			image.setWeaponGradeNumber (n);
			image.setUnitDisplayRangedImageFile ("Blah" + n + ".png");
			
			rat.getRangedAttackTypeWeaponGrade ().add (image);
		}
		
		rat.buildMap ();
		
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
		final RangedAttackTypeGfx rat = new RangedAttackTypeGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeCombatImageGfx flyImage = new RangedAttackTypeCombatImageGfx ();
			flyImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.FLY);
			flyImage.setDirection (n);
			flyImage.setRangedAttackTypeCombatImageFile ("Fly" + n + ".png");

			final RangedAttackTypeCombatImageGfx strikeImage = new RangedAttackTypeCombatImageGfx ();
			strikeImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.STRIKE);
			strikeImage.setDirection (n);
			strikeImage.setRangedAttackTypeCombatImageFile ("Strike" + n + ".png");
			
			rat.getRangedAttackTypeCombatImage ().add (flyImage);
			rat.getRangedAttackTypeCombatImage ().add (strikeImage);
		}
		
		rat.buildMap ();
		
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
		final RangedAttackTypeGfx rat = new RangedAttackTypeGfx ();
		for (int n = 1; n <= 3; n++)
		{
			final RangedAttackTypeCombatImageGfx flyImage = new RangedAttackTypeCombatImageGfx ();
			flyImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.FLY);
			flyImage.setDirection (n);
			flyImage.setRangedAttackTypeCombatImageFile ("Fly" + n + ".png");

			final RangedAttackTypeCombatImageGfx strikeImage = new RangedAttackTypeCombatImageGfx ();
			strikeImage.setRangedAttackTypeActionID (RangedAttackTypeActionID.STRIKE);
			strikeImage.setDirection (n);
			strikeImage.setRangedAttackTypeCombatImageFile ("Strike" + n + ".png");
			
			rat.getRangedAttackTypeCombatImage ().add (flyImage);
			rat.getRangedAttackTypeCombatImage ().add (strikeImage);
		}
		
		rat.buildMap ();
		
		// Run tests
		rat.findCombatImage (RangedAttackTypeActionID.STRIKE, 4, "testFindCombatImage_NotExist");
	}
}
package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.RangedAttackType;
import momime.client.graphics.database.v0_9_5.RangedAttackTypeWeaponGrade;

/**
 * Adds a map over the weapon grades, so we can find their images faster
 */
public final class RangedAttackTypeEx extends RangedAttackType
{
	/** Map of weapon grade numbers to image filenames */
	private Map<Integer, String> weaponGradesMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		weaponGradesMap = new HashMap<Integer, String> ();
		for (final RangedAttackTypeWeaponGrade weaponGrade : getRangedAttackTypeWeaponGrade ())
			weaponGradesMap.put (weaponGrade.getWeaponGradeNumber (), weaponGrade.getUnitDisplayRangedImageFile ());
	}
	
	/**
	 * @param weaponGrade Weapon grade to search for
	 * @return Filename for the image of this weapon grade; or null if no image exists for it
	 */
	public final String findWeaponGradeImageFile (final int weaponGrade)
	{
		return weaponGradesMap.get (weaponGrade);
	}
}
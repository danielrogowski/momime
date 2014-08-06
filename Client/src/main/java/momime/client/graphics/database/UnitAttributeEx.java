package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_5.UnitAttribute;
import momime.client.graphics.database.v0_9_5.UnitAttributeWeaponGrade;

/**
 * Adds a map over the weapon grades, so we can find their images faster
 */
public final class UnitAttributeEx extends UnitAttribute
{
	/** Map of weapon grade numbers to image filenames */
	private Map<Integer, String> weaponGradesMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		weaponGradesMap = new HashMap<Integer, String> ();
		for (final UnitAttributeWeaponGrade weaponGrade : getUnitAttributeWeaponGrade ())
			weaponGradesMap.put (weaponGrade.getWeaponGradeNumber (), weaponGrade.getAttributeImageFile ());
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
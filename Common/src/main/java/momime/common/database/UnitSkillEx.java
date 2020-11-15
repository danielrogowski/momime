package momime.common.database;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds a map over the weapon grades, so we can find their images faster
 */
public final class UnitSkillEx extends UnitSkill
{
	/** Map of weapon grade numbers to image filenames */
	private Map<Integer, String> weaponGradesMap;
	
	/**
	 * Builds the hash map to enable finding records faster
	 */
	public final void buildMap ()
	{
		weaponGradesMap = new HashMap<Integer, String> ();
		for (final UnitSkillWeaponGrade weaponGrade : getUnitSkillWeaponGrade ())
			weaponGradesMap.put (weaponGrade.getWeaponGradeNumber (), weaponGrade.getSkillImageFile ());
	}
	
	/**
	 * @param weaponGrade Weapon grade to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Filename for the image of this weapon grade
	 * @throws RecordNotFoundException If the weapon grade doesn't exist
	 */
	public final String findWeaponGradeImageFile (final int weaponGrade, final String caller) throws RecordNotFoundException
	{
		final String found = weaponGradesMap.get (weaponGrade);

		if (found == null)
			throw new RecordNotFoundException (UnitSkillWeaponGrade.class, weaponGrade, caller);
		
		return found;
	}
}
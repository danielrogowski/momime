package momime.client.graphics.database;

import java.util.HashMap;
import java.util.Map;

import momime.client.graphics.database.v0_9_6.RangedAttackType;
import momime.client.graphics.database.v0_9_6.RangedAttackTypeActionID;
import momime.client.graphics.database.v0_9_6.RangedAttackTypeCombatImage;
import momime.client.graphics.database.v0_9_6.RangedAttackTypeWeaponGrade;
import momime.common.database.RecordNotFoundException;

/**
 * Adds a map over the weapon grades, so we can find their images faster
 */
public final class RangedAttackTypeGfx extends RangedAttackType
{
	/** Map of weapon grade numbers to image filenames */
	private Map<Integer, String> weaponGradesMap;
	
	/** Map of rangedAttackTypeActionID and directions to RAT images, e.g. STRIKE-5 */
	private Map<String, RangedAttackTypeCombatImageGfx> combatImagesMap;
	
	/**
	 * Builds the hash maps to enable finding records faster
	 */
	public final void buildMap ()
	{
		weaponGradesMap = new HashMap<Integer, String> ();
		for (final RangedAttackTypeWeaponGrade weaponGrade : getRangedAttackTypeWeaponGrade ())
			weaponGradesMap.put (weaponGrade.getWeaponGradeNumber (), weaponGrade.getUnitDisplayRangedImageFile ());
		
		combatImagesMap = new HashMap<String, RangedAttackTypeCombatImageGfx> ();
		for (final RangedAttackTypeCombatImage combatImage : getRangedAttackTypeCombatImage ())
			combatImagesMap.put (combatImage.getRangedAttackTypeActionID () + "-" + combatImage.getDirection (), (RangedAttackTypeCombatImageGfx) combatImage);
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
			throw new RecordNotFoundException (RangedAttackTypeWeaponGrade.class, weaponGrade, caller);
		
		return found;
	}

	/**
	 * @param rangedAttackTypeActionID RAT action ID to search for
	 * @param direction Direction to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Filename for the image of this weapon grade
	 * @throws RecordNotFoundException If the weapon grade doesn't exist
	 */
	public final RangedAttackTypeCombatImageGfx findCombatImage (final RangedAttackTypeActionID rangedAttackTypeActionID, final int direction, final String caller)
		throws RecordNotFoundException
	{
		final String key = rangedAttackTypeActionID + "-" + direction;
		final RangedAttackTypeCombatImageGfx found = combatImagesMap.get (key);

		if (found == null)
			throw new RecordNotFoundException (RangedAttackTypeCombatImage.class, key, caller);
		
		return found;
	}
}
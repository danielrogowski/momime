package momime.server.database;

import java.util.List;

import momime.server.database.v0_9_6.UnitAttribute;

/**
 * Empty extension, just so that majority of code doesn't need to reference a package that changes between versions
 */
public final class UnitAttributeSvr extends UnitAttribute
{
	/**
	 * @return List of possible rules by which to resolve attacks based on this attribute
	 */
	@SuppressWarnings ("unchecked")
	public final List<AttackResolutionSvr> getAttackResolutions ()
	{
		return (List<AttackResolutionSvr>) (List<?>) getAttackResolution ();
	}
}